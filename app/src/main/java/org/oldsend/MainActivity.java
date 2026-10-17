package org.oldsend;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.json.JSONObject;
import org.oldsend.discovery.DeviceStore;
import org.oldsend.discovery.MulticastScanner;
import org.oldsend.http.HttpClient;
import org.oldsend.model.Device;
import org.oldsend.transfer.Uploader;
import org.oldsend.ui.DeviceAdapter;

public class MainActivity extends Activity implements OldSendService.LogSink, DeviceStore.Listener {

    private static final int REQ_FILE = 1001;

    private TextView status;
    private TextView logView;
    private ScrollView logScroll;
    private Button toggleBtn;
    private ListView deviceList;
    private DeviceAdapter adapter;
    private Handler handler = new Handler();
    private boolean running = false;
    private Device pendingDevice;
    private Uri pendingFileUri;
    private TextView selectedFileText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = (TextView) findViewById(R.id.status);
        logView = (TextView) findViewById(R.id.log);
        logScroll = (ScrollView) findViewById(R.id.log_scroll);
        toggleBtn = (Button) findViewById(R.id.toggle);
        deviceList = (ListView) findViewById(R.id.devices);
        selectedFileText = (TextView) findViewById(R.id.selected_file);
        findViewById(R.id.pick_file).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickFile(); }
        });
        adapter = new DeviceAdapter(this);
        deviceList.setAdapter(adapter);

        OldSendService.setSink(this);
        DeviceStore.get().setListener(this);

        toggleBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { toggleService(); }
        });

        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                Device target = adapter.getItem(pos);
                if (pendingFileUri == null) {
                    log("请先点击【选择文件】按钮！");
                    return;
                }
                log("准备发送给: " + target.alias + " (" + target.ip + ")");
                sendFile(target, pendingFileUri);
                // 发送后清空已选文件，避免重复误发
                pendingFileUri = null;
                selectedFileText.setText("未选择文件 (选中后点下方设备发送)");
            }
        });

        findViewById(R.id.scan).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { doScan(); }
        });
        findViewById(R.id.manual).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showManualDialog(); }
        });
        findViewById(R.id.clear_log).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { logView.setText(""); }
        });

        updateStatus();
        log("OldSend 已就绪");
        log("本机IP: " + OldSendService.getLocalIp());
        refreshDevices();
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
        String ip = OldSendService.getLocalIp();
        String s = (running ? "运行中" : "已停止") + "  IP:" + ip + ":53317  devices:" + adapter.getCount();
        status.setText(s);
    }

    private void doScan() {
        log("正在扫描...");
        MulticastScanner.scanAsync(new MulticastScanner.Callback() {
            public void onDevice(Device d) {
                DeviceStore.get().addDevice(d);
                log("发现: " + d.alias + " @ " + d.ip);
            }
            public void onFinished() {
                log("扫描结束，共 " + adapter.getCount() + " 个设备");
            }
        });
    }

    private void showManualDialog() {
        final EditText input = new EditText(this);
        input.setHint("192.168.1.100");
        new AlertDialog.Builder(this)
            .setTitle("手动输入设备IP")
            .setView(input)
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    String ip = input.getText().toString().trim();
                    if (ip.length() > 0) probeIp(ip);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void probeIp(final String ip) {
        log("probing " + ip + "...");
        new Thread(new Runnable() {
            public void run() {
                try {
                    String url = "http://" + ip + ":53317" + Protocol.V2_PREFIX + "/info";
                    JSONObject o = HttpClient.getJson(url, 3000);
                    if (o == null) {
                        log("探测失败: " + ip);
                        return;
                    }
                    Device d = Device.fromJson(o, ip);
                    DeviceStore.get().addDevice(d);
                    log("已添加: " + d.alias + " @ " + ip);
                } catch (Exception e) {
                    log("探测错误: " + e);
                }
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQ_FILE) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            log("已取消选择文件");
            return;
        }
        Uri uri = data.getData();
        if (pendingDevice == null) {
            log("未选择设备");
            return;
        }
        sendFile(pendingDevice, uri);
        pendingDevice = null;
    }

    
    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        startActivityForResult(i, REQ_FILE);
    }

    private void sendFile(final Device device, final Uri uri) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    String name = queryDisplayName(uri);
                    File tmp = copyToCache(uri, name);
                    log("正在发送 " + name + " (" + tmp.length() + " 字节) 发送给 " + device.alias);
                    Uploader.upload(device, tmp, new Uploader.ProgressCallback() {
                        public void onProgress(final String stage, final long sent, final long total) {
                            handler.post(new Runnable() {
                                public void run() {
                                    String pct = total > 0 ? (" " + (sent * 100 / total) + "%") : "";
                                    status.setText("正在发送给 " + device.alias + ": " + stage + pct);
                                }
                            });
                        }
                        public void onDone(boolean ok, String msg) {
                            log("[" + device.alias + "] " + (ok ? "成功: " : "失败: ") + msg);
                            handler.post(new Runnable() {
                                public void run() { updateStatus(); }
                            });
                        }
                    });
                } catch (Exception e) {
                    log("发送错误: " + e);
                }
            }
        }).start();
    }

    private String queryDisplayName(Uri uri) {
        String name = null;
        Cursor c = null;
        try {
            c = getContentResolver().query(uri, new String[] { "_display_name" }, null, null, null);
            if (c != null && c.moveToFirst()) name = c.getString(0);
        } catch (Exception ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Exception ignored) {}
        }
        if (name == null) name = uri.getLastPathSegment();
        if (name == null) name = "file_" + System.currentTimeMillis();
        return name;
    }

    private File copyToCache(Uri uri, String name) throws Exception {
        ContentResolver cr = getContentResolver();
        InputStream in = cr.openInputStream(uri);
        if (in == null) throw new Exception("cannot open input");
        File dir = new File(getCacheDir(), "upload");
        if (!dir.exists()) dir.mkdirs();
        String safe = name.replaceAll("[/\\\\:*?\"<>|]", "_");
        if (safe.length() == 0) safe = "file";
        File out = new File(dir, safe);
        FileOutputStream fos = new FileOutputStream(out);
        try {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
        } finally {
            try { in.close(); } catch (Exception ignored) {}
            try { fos.close(); } catch (Exception ignored) {}
        }
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
        refreshDevices();
    }

    private void refreshDevices() {
        adapter.setData(DeviceStore.get().snapshot());
        updateStatus();
    }

    @Override
    protected void onDestroy() {
        OldSendService.setSink(null);
        DeviceStore.get().setListener(null);
        super.onDestroy();
    }
}
