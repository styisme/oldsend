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
import org.oldsend.model.HistoryStore;
import org.oldsend.transfer.Uploader;
import org.oldsend.ui.DeviceAdapter;

public class MainActivity extends Activity implements OldSendService.LogSink, DeviceStore.Listener, ReceiveManager.DialogListener {
    private static final int REQ_FILE = 1001;
    private static final int REQ_PERM = 2001;

    private TextView status, selectedFileText;
    private TextView dialogLogView;
    private AlertDialog logDialog;
    private StringBuilder logCache = new StringBuilder();
    private ScrollView logScroll;
    private Button toggleBtn;
    private ListView deviceList, historyList;
    private ProgressBar progressBar;
    private DeviceAdapter adapter;
    private ArrayAdapter<String> historyAdapter;
    private Handler handler = new Handler();
    private boolean running = false;
    private List<Uri> pendingUris = new ArrayList<Uri>();
    private List<String> historyPaths = new ArrayList<String>();

    private LinearLayout tabSend, tabReceive;
    private TextView tabReceiveText;
    private View pageSend, pageReceive;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(android.view.Window.FEATURE_LEFT_ICON);
        getWindow().setFeatureDrawableResource(android.view.Window.FEATURE_LEFT_ICON, R.drawable.ic_launcher);
        setContentView(R.layout.activity_main);

        status = (TextView) findViewById(R.id.status);
        
        
        toggleBtn = (Button) findViewById(R.id.toggle);
        deviceList = (ListView) findViewById(R.id.devices);
        historyList = (ListView) findViewById(R.id.history_list);
        selectedFileText = (TextView) findViewById(R.id.selected_file);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        
        tabSend = (LinearLayout) findViewById(R.id.tab_send);
        tabReceive = (LinearLayout) findViewById(R.id.tab_receive);
        tabReceiveText = (TextView) findViewById(R.id.tab_receive_text);
        pageSend = findViewById(R.id.page_send);
        pageReceive = findViewById(R.id.page_receive);

        adapter = new DeviceAdapter(this);
        deviceList.setAdapter(adapter);
        historyAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        historyList.setAdapter(historyAdapter);

        OldSendService.setSink(this);
        DeviceStore.get().setListener(this);
        ReceiveManager.setListener(this);

        toggleBtn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { toggleService(); } });
        findViewById(R.id.scan).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { doScan(); } });
        findViewById(R.id.manual).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showManualDialog(); } });
        findViewById(R.id.set_name).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showSetNameDialog(); } });
        findViewById(R.id.btn_log).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showLogDialog(); } });
        findViewById(R.id.pick_file).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { pickFile(); } });
        findViewById(R.id.send_text).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showSendTextDialog(); } });

        tabSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { switchTab(true); }
        });
        tabReceive.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { switchTab(false); }
        });

        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                Device target = adapter.getItem(pos);
                if (pendingUris.isEmpty()) { log("请先选文件或发文本！"); return; }
                log("发送给: " + target.alias);
                List<File> files = new ArrayList<File>();
                for (Uri u : pendingUris) {
                    try { files.add(copyToCache(u, queryDisplayName(u))); } catch (Exception e) { log("读取失败"); }
                }
                pendingUris.clear();
                selectedFileText.setText("未选择文件");
                doSend(target, files);
            }
        });

        historyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                String path = historyPaths.get(pos);
                File f = new File(path);
                if (!f.exists()) { Toast.makeText(MainActivity.this, "文件已不存在", Toast.LENGTH_SHORT).show(); return; }
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(f), "application/*");
                    startActivity(intent);
                } catch (Exception e) { Toast.makeText(MainActivity.this, "无法打开此文件", Toast.LENGTH_SHORT).show(); }
            }
        });

        updateStatus();
        refreshHistory();
        log("OldSend MD3 已就绪");
        log("本机IP: " + OldSendService.getLocalIp());
        handleIntent(getIntent());
    }

    private void switchTab(boolean isSend) {
        if (isSend) {
            pageSend.setVisibility(View.VISIBLE);
            pageReceive.setVisibility(View.GONE);
            tabSend.setBackgroundResource(R.drawable.bg_tab_selected);
            tabReceive.setBackgroundResource(0);
            tabReceiveText.setTextColor(0xFF49454F);
        } else {
            pageSend.setVisibility(View.GONE);
            pageReceive.setVisibility(View.VISIBLE);
            tabReceive.setBackgroundResource(R.drawable.bg_tab_selected);
            tabSend.setBackgroundResource(0);
            tabReceiveText.setTextColor(0xFF2E7D32);
            refreshHistory();
        }
    }

    private void refreshHistory() {
        List<HistoryStore.Record> list = HistoryStore.get().snapshot();
        historyAdapter.clear();
        historyPaths.clear();
        for (HistoryStore.Record r : list) {
            String sizeStr = r.size > 1024*1024 ? (r.size/1024/1024)+"MB" : (r.size/1024)+"KB";
            historyAdapter.add("[" + r.time + "] " + r.fileName + " (" + sizeStr + ")");
            historyPaths.add(r.path);
        }
        historyAdapter.notifyDataSetChanged();
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
            public void onDevice(Device d) { DeviceStore.get().addDevice(d); log("发现: " + d.alias); }
            public void onFinished() { log("扫描结束"); }
        });
    }

    private void showManualDialog() {
        final EditText input = new EditText(this);
        input.setHint("192.168.1.100");
        new AlertDialog.Builder(this).setTitle("手动输入IP").setView(input)
            .setPositiveButton("确定", new DialogInterface.OnClickListener() { public void onClick(DialogInterface d, int w) { probeIp(input.getText().toString().trim()); } })
            .setNegativeButton("取消", null).show();
    }

    private void showSetNameDialog() {
        final EditText input = new EditText(this);
        input.setText(OldSendService.getAlias(this));
        new AlertDialog.Builder(this).setTitle("修改设备名").setView(input)
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) { if (input.getText().length() > 0) OldSendService.setAlias(MainActivity.this, input.getText().toString()); }
            }).setNegativeButton("取消", null).show();
    }

    private void showSendTextDialog() {
        final EditText input = new EditText(this);
        input.setHint("输入文字...");
        new AlertDialog.Builder(this).setTitle("发送文本").setView(input)
            .setPositiveButton("发送", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    String text = input.getText().toString();
                    if (text.length() > 0) {
                        try {
                            File f = new File(getCacheDir(), "text_" + System.currentTimeMillis() + ".txt");
                            FileOutputStream fos = new FileOutputStream(f);
                            fos.write(text.getBytes("UTF-8")); fos.close();
                            pendingUris.clear(); pendingUris.add(Uri.fromFile(f));
                            selectedFileText.setText("已准备文本");
                        } catch (Exception e) { log("生成失败"); }
                    }
                }
            }).setNegativeButton("取消", null).show();
    }

    private void probeIp(final String ip) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    JSONObject o = HttpClient.getJson("http://" + ip + ":53317" + Protocol.V2_PREFIX + "/info", 3000);
                    if (o != null) DeviceStore.get().addDevice(Device.fromJson(o, ip));
                } catch (Exception e) { log("探测失败"); }
            }
        }).start();
    }

    private void pickFile() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_PERM);
            return;
        }
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*"); i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(i, "选择文件"), REQ_FILE);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQ_PERM && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) pickFile();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQ_FILE || resultCode != RESULT_OK || data == null) return;
        pendingUris.clear();
        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) pendingUris.add(data.getClipData().getItemAt(i).getUri());
            selectedFileText.setText("已选择 " + count + " 个文件");
        } else if (data.getData() != null) {
            pendingUris.add(data.getData());
            selectedFileText.setText("已选择: " + queryDisplayName(data.getData()));
        }
    }

    private void doSend(final Device device, final List<File> files) {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            public void run() {
                try {
                    Uploader.uploadFiles(device, files, new Uploader.ProgressCallback() {
                        public void onProgress(final String stage, final long sent, final long total) {
                            handler.post(new Runnable() { public void run() {
                                progressBar.setProgress(total > 0 ? (int)(sent * 100 / total) : 0);
                            }});
                        }
                        public void onDone(boolean ok, String msg) {
                            log("[" + device.alias + "] " + (ok ? "成功" : "失败") + ": " + msg);
                            handler.post(new Runnable() { public void run() { progressBar.setVisibility(View.GONE); } });
                        }
                    });
                } catch (Exception e) { log("发送错误"); }
            }
        }).start();
    }

    private String queryDisplayName(Uri uri) {
        if ("file".equals(uri.getScheme())) return new File(uri.getPath()).getName();
        String name = null; Cursor c = null;
        try { c = getContentResolver().query(uri, new String[] { "_display_name" }, null, null, null); if (c != null && c.moveToFirst()) name = c.getString(0); } catch (Exception ignored) {} finally { if (c != null) try { c.close(); } catch (Exception ignored) {} }
        if (name == null) name = uri.getLastPathSegment();
        return name == null ? "file" : name;
    }

    private File copyToCache(Uri uri, String name) throws Exception {
        if ("file".equals(uri.getScheme())) return new File(uri.getPath());
        InputStream in = getContentResolver().openInputStream(uri);
String safe = name;
        safe = safe.replace("/", "_").replace("\\", "_").replace(":", "_")
                   .replace("*", "_").replace("?", "_").replace("\"", "_")
                   .replace("<", "_").replace(">", "_").replace("|", "_");
        File out = new File(getCacheDir(), safe);
        FileOutputStream fos = new FileOutputStream(out);
        byte[] buf = new byte[65536]; int n;
        while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
        in.close(); fos.close();
        return out;
    }

    public void log(final String msg) {
        handler.post(new Runnable() {
            public void run() {
                logCache.append(msg).append("\n");
                if (logCache.length() > 5000) logCache.delete(0, logCache.length() - 5000);
                if (dialogLogView != null) {
                    dialogLogView.setText(logCache.toString());
                    if (logDialog != null && logDialog.isShowing()) {
                        // 保持滚动到底部
                        android.text.Layout layout = dialogLogView.getLayout();
                        if (layout != null) {
                            int scrollAmount = layout.getLineTop(dialogLogView.getLineCount()) - dialogLogView.getHeight();
                            if (scrollAmount > 0) dialogLogView.scrollTo(0, scrollAmount);
                        }
                    }
                }
            }
        });
    }

    private void showLogDialog() {
        if (logDialog != null && logDialog.isShowing()) return;
        dialogLogView = new TextView(this);
        dialogLogView.setTextSize(11);
        dialogLogView.setTextColor(0xFF333333);
        dialogLogView.setTypeface(android.graphics.Typeface.MONOSPACE);
        dialogLogView.setPadding(16, 16, 16, 16);
        dialogLogView.setText(logCache.toString());

        ScrollView scroll = new ScrollView(this);
        scroll.addView(dialogLogView);
        scroll.setBackgroundColor(0xFFF5F5F5);

        logDialog = new AlertDialog.Builder(this)
            .setTitle("运行日志")
            .setView(scroll)
            .setPositiveButton("关闭", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) { dialogLogView = null; }
            })
            .setNegativeButton("清空", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    logCache.setLength(0);
                    if (dialogLogView != null) dialogLogView.setText("");
                }
            })
            .create();
        logDialog.show();
    }

    public void onChanged() { adapter.setData(DeviceStore.get().snapshot()); updateStatus(); }

    @Override
    public void showConfirmDialog(String senderAlias, int fileCount, final java.util.concurrent.CountDownLatch latch, final boolean[] result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("接收文件");
        builder.setMessage("设备【" + senderAlias + "】请求发送 " + fileCount + " 个文件，是否接收？");
        builder.setCancelable(false);
        builder.setPositiveButton("接收", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                result[0] = true;
                latch.countDown();
            }
        });
        builder.setNegativeButton("拒绝", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                result[0] = false;
                latch.countDown();
            }
        });
        builder.show();
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

    @Override protected void onDestroy() { OldSendService.setSink(null); DeviceStore.get().setListener(null); ReceiveManager.setListener(null); super.onDestroy(); }
}
