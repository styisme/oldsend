package org.oldsend;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import android.Manifest;
import android.content.pm.PackageManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.oldsend.discovery.DeviceStore;
import org.oldsend.discovery.MulticastScanner;
import org.oldsend.http.HttpClient;
import org.oldsend.model.Device;
import org.oldsend.transfer.Uploader;
import org.oldsend.ui.DeviceAdapter;

public class MainActivity extends Activity implements OldSendService.LogSink, DeviceStore.Listener {
    private static final int REQ_FILE = 1001;
    private static final int REQ_PERM = 2001;

    private TextView status, logView, selectedFileText;
    private ScrollView logScroll;
    private Button toggleBtn;
    private ListView deviceList;
    private ProgressBar progressBar;
    private DeviceAdapter adapter;
    private Handler handler = new Handler();
    private boolean running = false;
    private List<Uri> pendingUris = new ArrayList<Uri>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(android.view.Window.FEATURE_LEFT_ICON);
        super.onCreate(savedInstanceState);
        getWindow().setFeatureDrawableResource(android.view.Window.FEATURE_LEFT_ICON, R.drawable.ic_launcher);
        setContentView(R.layout.activity_main);
        status = (TextView) findViewById(R.id.status);
        logView = (TextView) findViewById(R.id.log);
        logScroll = (ScrollView) findViewById(R.id.log_scroll);
        toggleBtn = (Button) findViewById(R.id.toggle);
        deviceList = (ListView) findViewById(R.id.devices);
        selectedFileText = (TextView) findViewById(R.id.selected_file);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        adapter = new DeviceAdapter(this);
        deviceList.setAdapter(adapter);

        OldSendService.setSink(this);
        DeviceStore.get().setListener(this);

        toggleBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { toggleService(); }
        });
        findViewById(R.id.scan).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { doScan(); }
        });
        findViewById(R.id.manual).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showManualDialog(); }
        });
        findViewById(R.id.set_name).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showSetNameDialog(); }
        });
        findViewById(R.id.clear_log).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { logView.setText(""); }
        });
        findViewById(R.id.pick_file).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickFile(); }
        });
        findViewById(R.id.send_text).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showSendTextDialog(); }
        });

        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                Device target = adapter.getItem(pos);
                if (pendingUris.isEmpty()) {
                    log("请先点击【选文件】或【发文本】！");
                    return;
                }
                log("准备发送给: " + target.alias + " (" + target.ip + ")");
                List<File> files = new ArrayList<File>();
                for (Uri u : pendingUris) {
                    try {
                        files.add(copyToCache(u, queryDisplayName(u)));
                    } catch (Exception e) {
                        log("读取文件失败: " + e);
                    }
                }
                pendingUris.clear();
                selectedFileText.setText("未选择(可多选)");
                doSend(target, files);
            }
        });

        updateStatus();
        log("OldSend 已就绪");
        log("本机IP: " + OldSendService.getLocalIp());
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) {
                pendingUris.clear();
                pendingUris.add(uri);
                selectedFileText.setText("已分享: " + queryDisplayName(uri));
                log("来自系统分享，请点击设备发送");
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (uris != null) {
                pendingUris.clear();
                pendingUris.addAll(uris);
                selectedFileText.setText("已分享 " + uris.size() + " 个文件");
                log("来自系统分享，共 " + uris.size() + " 个文件，请点击设备发送");
            }
        }
    }

    private void toggleService() {
        if (running) {
            stopService(new Intent(this, OldSendService.class));
            running = false;
            toggleBtn.setText("启动");
        } else {
            startService(new Intent(this, OldSendService.class));
            running = true;
            toggleBtn.setText("停止");
        }
        updateStatus();
    }

    private void updateStatus() {
        String s = (running ? "运行中" : "已停止") + "  IP:" + OldSendService.getLocalIp() + ":53317";
        status.setText(s);
    }

    private void doScan() {
        log("正在扫描...");
        MulticastScanner.scanAsync(new MulticastScanner.Callback() {
            public void onDevice(Device d) { DeviceStore.get().addDevice(d); log("发现: " + d.alias + " @ " + d.ip); }
            public void onFinished() { log("扫描结束，共 " + adapter.getCount() + " 个设备"); }
        });
    }

    private void showManualDialog() {
        final EditText input = new EditText(this);
        input.setHint("192.168.1.100");
        new AlertDialog.Builder(this).setTitle("手动输入设备IP").setView(input)
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) { if (input.getText().length() > 0) probeIp(input.getText().toString().trim()); }
            }).setNegativeButton("取消", null).show();
    }

    private void showSetNameDialog() {
        final EditText input = new EditText(this);
        input.setText(OldSendService.getAlias(this));
        new AlertDialog.Builder(this).setTitle("设置我的设备名").setView(input)
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    String name = input.getText().toString().trim();
                    if (name.length() > 0) {
                        OldSendService.setAlias(MainActivity.this, name);
                        log("设备名已改为: " + name);
                    }
                }
            }).setNegativeButton("取消", null).show();
    }

    private void showSendTextDialog() {
        final EditText input = new EditText(this);
        input.setHint("输入要发送的文字...");
        new AlertDialog.Builder(this).setTitle("发送文本").setView(input)
            .setPositiveButton("发送", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    String text = input.getText().toString();
                    if (text.length() > 0) {
                        try {
                            File f = new File(getCacheDir(), "text_" + System.currentTimeMillis() + ".txt");
                            FileOutputStream fos = new FileOutputStream(f);
                            fos.write(text.getBytes("UTF-8"));
                            fos.close();
                            pendingUris.clear();
                            pendingUris.add(Uri.fromFile(f));
                            selectedFileText.setText("已准备文本: " + text.substring(0, Math.min(10, text.length())) + "...");
                            log("文本已准备，请点击下方设备发送");
                        } catch (Exception e) { log("生成文本失败: " + e); }
                    }
                }
            }).setNegativeButton("取消", null).show();
    }

    private void probeIp(final String ip) {
        log("正在探测 " + ip + "...");
        new Thread(new Runnable() {
            public void run() {
                try {
                    String url = "http://" + ip + ":53317" + Protocol.V2_PREFIX + "/info";
                    JSONObject o = HttpClient.getJson(url, 3000);
                    if (o != null) {
                        Device d = Device.fromJson(o, ip);
                        DeviceStore.get().addDevice(d);
                        log("已添加: " + d.alias + " @ " + ip);
                    } else { log("探测失败: " + ip); }
                } catch (Exception e) { log("探测错误: " + e); }
            }
        }).start();
    }

    private void pickFile() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_PERM);
                return;
            }
        }
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(i, "选择文件"), REQ_FILE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQ_PERM && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickFile();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQ_FILE) return;
        if (resultCode != RESULT_OK || data == null) { log("已取消选择"); return; }
        pendingUris.clear();
        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) pendingUris.add(data.getClipData().getItemAt(i).getUri());
            selectedFileText.setText("已选择 " + count + " 个文件");
        } else if (data.getData() != null) {
            pendingUris.add(data.getData());
            selectedFileText.setText("已选择: " + queryDisplayName(data.getData()));
        }
        log("文件已就绪，请点击下方设备发送");
    }

    private void doSend(final Device device, final List<File> files) {
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        new Thread(new Runnable() {
            public void run() {
                try {
                    Uploader.uploadFiles(device, files, new Uploader.ProgressCallback() {
                        public void onProgress(final String stage, final long sent, final long total) {
                            handler.post(new Runnable() {
                                public void run() {
                                    int pct = total > 0 ? (int)(sent * 100 / total) : 0;
                                    progressBar.setProgress(pct);
                                    status.setText("发送中: " + pct + "% (" + stage + ")");
                                }
                            });
                        }
                        public void onDone(boolean ok, String msg) {
                            log("[" + device.alias + "] " + (ok ? "成功: " : "失败: ") + msg);
                            handler.post(new Runnable() {
                                public void run() {
                                    progressBar.setVisibility(View.GONE);
                                    updateStatus();
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    log("发送错误: " + e);
                    handler.post(new Runnable() { public void run() { progressBar.setVisibility(View.GONE); } });
                }
            }
        }).start();
    }

    private String queryDisplayName(Uri uri) {
        if ("file".equals(uri.getScheme())) return new File(uri.getPath()).getName();
        String name = null;
        Cursor c = null;
        try {
            c = getContentResolver().query(uri, new String[] { "_display_name" }, null, null, null);
            if (c != null && c.moveToFirst()) name = c.getString(0);
        } catch (Exception ignored) {} finally { if (c != null) try { c.close(); } catch (Exception ignored) {} }
        if (name == null) name = uri.getLastPathSegment();
        return name == null ? "file" : name;
    }

    private File copyToCache(Uri uri, String name) throws Exception {
        if ("file".equals(uri.getScheme())) {
            File direct = new File(uri.getPath());
            if (direct.exists() && direct.canRead()) return direct;
        }
        InputStream in = getContentResolver().openInputStream(uri);
        if (in == null) throw new Exception("cannot open");
        File dir = new File(getCacheDir(), "upload");
        if (!dir.exists()) dir.mkdirs();
        String safe = name.replaceAll("[/\\\\:*?\"<>|]", "_");
        if (safe.length() == 0) safe = "file";
        File out = new File(dir, safe);
        FileOutputStream fos = new FileOutputStream(out);
        byte[] buf = new byte[65536]; int n;
        while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
        try { in.close(); } catch (Exception ignored) {}
        try { fos.close(); } catch (Exception ignored) {}
        return out;
    }

    public void log(final String msg) {
        handler.post(new Runnable() {
            public void run() {
                logView.append(msg + "\n");
                if (logScroll != null) logScroll.fullScroll(View.FOCUS_DOWN);
                String text = logView.getText().toString();
                String[] lines = text.split("\n");
                if (lines.length > 300) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = lines.length - 300; i < lines.length; i++) sb.append(lines[i]).append("\n");
                    logView.setText(sb.toString());
                }
            }
        });
    }

    public void onChanged() {
        adapter.setData(DeviceStore.get().snapshot());
        updateStatus();
    }

    @Override protected void onDestroy() { OldSendService.setSink(null); DeviceStore.get().setListener(null); super.onDestroy(); }
}
