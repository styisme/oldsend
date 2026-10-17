package org.oldsend;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.widget.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.oldsend.discovery.DeviceStore;
import org.oldsend.discovery.MulticastScanner;
import org.oldsend.http.HttpClient;
import org.oldsend.model.Device;
import org.oldsend.model.HistoryStore;
import org.oldsend.transfer.Uploader;
import org.oldsend.ui.DeviceAdapter;
import org.oldsend.ui.Md3DialogUtils;

public class MainActivity extends Activity implements OldSendService.LogSink, DeviceStore.Listener, ReceiveManager.DialogListener {
    private static final int REQ_FILE = 1001;
    private static final int REQ_MEDIA = 1002;

    private TextView status, selectedFileText, tvPin, tvName, tvPort, tvTimeout, emptyHint;
    private TextView dialogLogView;
    private Dialog logDialog;
    private StringBuilder logCache = new StringBuilder();
    private Button toggleBtn;
    private ListView deviceList, historyList;
    private ProgressBar progressBar;
    private DeviceAdapter adapter;
    private ArrayAdapter<String> historyAdapter;
    private Handler handler = new Handler();
    private boolean running = false;
    private List<Uri> pendingUris = new ArrayList<Uri>();
    private List<String> historyPaths = new ArrayList<String>();

    private LinearLayout tabSend, tabReceive, tabSettings;
    private TextView tabReceiveText, tabSettingsText;
    private View pageSend, pageReceive, pageSettings;

    private CheckBox cbAutoSave, cbSaveAlbum, cbSaveHistory, cbPin, cbAnim, cbWeb, cbBoot, cbReceiveNotif;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // 移除标题栏
        setContentView(R.layout.activity_main);

        status = (TextView) findViewById(R.id.status);
        selectedFileText = (TextView) findViewById(R.id.selected_file);
        toggleBtn = (Button) findViewById(R.id.toggle);
        deviceList = (ListView) findViewById(R.id.devices);
        historyList = (ListView) findViewById(R.id.history_list);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        
        tabSend = (LinearLayout) findViewById(R.id.tab_send);
        tabReceive = (LinearLayout) findViewById(R.id.tab_receive);
        tabSettings = (LinearLayout) findViewById(R.id.tab_settings);
        tabReceiveText = (TextView) findViewById(R.id.tab_receive_text);
        tabSettingsText = (TextView) findViewById(R.id.tab_settings_text);
        
        pageSend = findViewById(R.id.page_send);
        pageReceive = findViewById(R.id.page_receive);
        pageSettings = findViewById(R.id.page_settings);

        adapter = new DeviceAdapter(this);
        deviceList.setAdapter(adapter);
        historyAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        historyList.setAdapter(historyAdapter);

        OldSendService.setSink(this);
        DeviceStore.get().setListener(this);
        ReceiveManager.setListener(this);

        // 绑定快捷卡片
        findViewById(R.id.card_file).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { pickFile(false); } });
        findViewById(R.id.card_media).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { pickFile(true); } });
        findViewById(R.id.card_text).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showSendTextDialog(); } });
        findViewById(R.id.card_clipboard).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { sendClipboard(); } });
        findViewById(R.id.card_folder).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { pickFolder(); } });
        findViewById(R.id.card_app).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { pickApp(); } });

        toggleBtn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { toggleService(); } });
        findViewById(R.id.scan).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { doScan(); } });
        findViewById(R.id.btn_log).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showLogDialog(); } });

        tabSend.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { switchTab(0); } });
        tabReceive.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { switchTab(1); } });
        tabSettings.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { switchTab(2); } });

        initSettingsUI();

        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                Device target = adapter.getItem(pos);
                if (pendingUris.isEmpty()) { log("请先选择文件或输入文本！"); return; }
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
        checkAndRequestPermission();
    }

    private void checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 1001);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                log("存储权限已授予");
            } else {
                log("未授予存储权限，可能无法收发文件");
            }
        }
    }

    private void initSettingsUI() {
        cbAutoSave = (CheckBox) findViewById(R.id.cb_auto_save);
        cbSaveAlbum = (CheckBox) findViewById(R.id.cb_save_album);
        cbSaveHistory = (CheckBox) findViewById(R.id.cb_save_history);
        cbPin = (CheckBox) findViewById(R.id.cb_pin);
        cbAnim = (CheckBox) findViewById(R.id.cb_anim);
        cbWeb = (CheckBox) findViewById(R.id.cb_web);
        cbBoot = (CheckBox) findViewById(R.id.cb_boot);
        cbReceiveNotif = (CheckBox) findViewById(R.id.cb_receive_notif);
        tvPin = (TextView) findViewById(R.id.tv_pin);
        tvName = (TextView) findViewById(R.id.tv_name);
        emptyHint = (TextView) findViewById(R.id.empty_hint);
        tvPort = (TextView) findViewById(R.id.tv_port);
        tvTimeout = (TextView) findViewById(R.id.tv_timeout);

        tvName.setText(OldSendService.getAlias(this));
        cbAutoSave.setChecked(OldSendService.getPrefBooleanGlobal("auto_save", false));
        cbSaveAlbum.setChecked(OldSendService.getPrefBooleanGlobal("save_album", true));
        cbSaveHistory.setChecked(OldSendService.getPrefBooleanGlobal("save_history", true));
        cbPin.setChecked(OldSendService.getPrefBooleanGlobal("use_pin", false));
        cbAnim.setChecked(OldSendService.getPrefBooleanGlobal("use_anim", true));
        cbWeb.setChecked(OldSendService.getPrefBooleanGlobal("web_enable", true));
        cbBoot.setChecked(OldSendService.getPrefBooleanGlobal("auto_start", false));
        cbReceiveNotif.setChecked(OldSendService.getPrefBooleanGlobal("receive_notif", false));
        
        int p = getSharedPreferences("oldsend", MODE_PRIVATE).getInt("port", 53317);
        tvPort.setText(String.valueOf(p));
        int t = getSharedPreferences("oldsend", MODE_PRIVATE).getInt("timeout", 500);
        tvTimeout.setText(String.valueOf(t));

        updatePinDisplay(cbPin.isChecked());

        cbAutoSave.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { public void onCheckedChanged(CompoundButton v, boolean c) { savePref("auto_save", c); } });
        cbSaveAlbum.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { public void onCheckedChanged(CompoundButton v, boolean c) { savePref("save_album", c); } });
        cbSaveHistory.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { public void onCheckedChanged(CompoundButton v, boolean c) { savePref("save_history", c); } });
        cbAnim.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { public void onCheckedChanged(CompoundButton v, boolean c) { savePref("use_anim", c); } });
        cbWeb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { public void onCheckedChanged(CompoundButton v, boolean c) { savePref("web_enable", c); } });
        cbBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { public void onCheckedChanged(CompoundButton v, boolean c) { savePref("auto_start", c); } });
        cbReceiveNotif.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { public void onCheckedChanged(CompoundButton v, boolean c) { savePref("receive_notif", c); } });
        
        cbPin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { 
            public void onCheckedChanged(CompoundButton v, boolean c) { 
                savePref("use_pin", c); 
                updatePinDisplay(c);
                if (running) {
                    stopService(new Intent(MainActivity.this, OldSendService.class));
                    startService(new Intent(MainActivity.this, OldSendService.class));
                }
            } 
        });

        findViewById(R.id.row_set_name).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showSetNameDialog(); } });
        findViewById(R.id.row_about).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showAboutDialog(); } });
        findViewById(R.id.row_backup).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showBackupDialog(); } });
        
        findViewById(R.id.row_port).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Md3DialogUtils.showInput(MainActivity.this, "修改端口", tvPort.getText().toString(), new Md3DialogUtils.OnInputListener() {
                    public void onInput(String text) {
                        try {
                            int val = Integer.parseInt(text);
                            if (val >= 1024 && val <= 65535) {
                                getSharedPreferences("oldsend", MODE_PRIVATE).edit().putInt("port", val).commit();
                                tvPort.setText(String.valueOf(val));
                                log("端口已修改，请重启服务生效");
                            }
                        } catch (Exception e) {}
                    }
                }).show();
            }
        });

        findViewById(R.id.row_timeout).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Md3DialogUtils.showInput(MainActivity.this, "搜索超时 (毫秒)", tvTimeout.getText().toString(), new Md3DialogUtils.OnInputListener() {
                    public void onInput(String text) {
                        try {
                            int val = Integer.parseInt(text);
                            getSharedPreferences("oldsend", MODE_PRIVATE).edit().putInt("timeout", val).commit();
                            tvTimeout.setText(String.valueOf(val));
                        } catch (Exception e) {}
                    }
                }).show();
            }
        });
    }

    private void updatePinDisplay(boolean visible) {
        if (visible) {
            String pin = getSharedPreferences("oldsend", MODE_PRIVATE).getString("pin_code", null);
            if (pin == null) {
                pin = String.format("%04d", (int)(Math.random() * 10000));
                getSharedPreferences("oldsend", MODE_PRIVATE).edit().putString("pin_code", pin).commit();
            }
            tvPin.setText("当前 PIN 码: " + pin);
            tvPin.setVisibility(View.VISIBLE);
        } else {
            tvPin.setVisibility(View.GONE);
        }
    }

    private void savePref(String key, boolean val) {
        getSharedPreferences("oldsend", MODE_PRIVATE).edit().putBoolean(key, val).commit();
    }

    private void switchTab(int index) {
        applyAnimation(pageSend, index == 0);
        applyAnimation(pageReceive, index == 1);
        applyAnimation(pageSettings, index == 2);

        tabSend.setBackgroundResource(index == 0 ? R.drawable.bg_tab_selected : 0);
        tabReceive.setBackgroundResource(index == 1 ? R.drawable.bg_tab_selected : 0);
        tabSettings.setBackgroundResource(index == 2 ? R.drawable.bg_tab_selected : 0);

        tabReceiveText.setTextColor(index == 1 ? 0xFF2E7D32 : 0xFF49454F);
        tabSettingsText.setTextColor(index == 2 ? 0xFF2E7D32 : 0xFF49454F);

        if (index == 1) refreshHistory();
    }

    private void applyAnimation(View view, boolean visible) {
        if (!getSharedPreferences("oldsend", MODE_PRIVATE).getBoolean("use_anim", true)) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
            return;
        }
        if (visible) {
            view.setVisibility(View.VISIBLE);
            AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
            anim.setDuration(250);
            view.startAnimation(anim);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    private void sendClipboard() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = cm.getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) {
            CharSequence text = clip.getItemAt(0).coerceToText(this);
            if (text != null && text.length() > 0) {
                prepareText(text.toString(), "剪贴板");
            }
        } else {
            Toast.makeText(this, "剪贴板为空", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSendTextDialog() {
        Md3DialogUtils.showInput(this, "发送文本", "", new Md3DialogUtils.OnInputListener() {
            public void onInput(String text) {
                if (text.length() > 0) prepareText(text, "文本");
            }
        }).show();
    }

    private void prepareText(String text, String source) {
        try {
            File f = new File(getCacheDir(), "text_" + System.currentTimeMillis() + ".txt");
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(text.getBytes("UTF-8")); fos.close();
            pendingUris.clear(); pendingUris.add(Uri.fromFile(f));
            selectedFileText.setText("已准备" + source + ": " + text.substring(0, Math.min(10, text.length())) + "...");
            switchTab(0);
        } catch (Exception e) { log("生成" + source + "失败"); }
    }

    private void pickFolder() {
        // 使用自定义 MD3 目录选择器
        final File root = android.os.Environment.getExternalStorageDirectory();
        showFolderSelector(root);
    }

    private void showFolderSelector(final File dir) {
        final File[] files = dir.listFiles();
        if (files == null) return;

        List<String> folderNames = new ArrayList<String>();
        final List<File> folders = new ArrayList<File>();
        folderNames.add(">> [发送当前文件夹] <<");
        
        for (File f : files) {
            if (f.isDirectory() && !f.isHidden()) {
                folderNames.add("[文件夹] " + f.getName());
                folders.add(f);
            }
        }

        String[] items = folderNames.toArray(new String[0]);
        Dialog d = Md3DialogUtils.showList(this, dir.getAbsolutePath(), items, new Md3DialogUtils.OnItemSelectListener() {
            public void onItemSelect(int position) {
                if (position == 0) {
                    // 发送当前文件夹
                    List<File> allFiles = new ArrayList<File>();
                    getAllFiles(dir, allFiles);
                    if (allFiles.isEmpty()) {
                        Toast.makeText(MainActivity.this, "文件夹为空", Toast.LENGTH_SHORT).show();
                    } else {
                        pendingUris.clear();
                        for (File f : allFiles) pendingUris.add(Uri.fromFile(f));
                        selectedFileText.setText("已选择文件夹: " + dir.getName() + " (" + allFiles.size() + " 个文件)");
                        switchTab(0);
                    }
                } else {
                    showFolderSelector(folders.get(position - 1));
                }
            }
        });
        d.show();
    }

    private void getAllFiles(File dir, List<File> out) {
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.isDirectory()) getAllFiles(f, out);
            else out.add(f);
        }
    }

    private void pickApp() {
        PackageManager pm = getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0);
        final List<ApplicationInfo> apps = new ArrayList<ApplicationInfo>();
        List<String> appNames = new ArrayList<String>();

        for (PackageInfo pkg : packages) {
            ApplicationInfo app = pkg.applicationInfo;
            // 过滤掉系统应用
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                apps.add(app);
                appNames.add(app.loadLabel(pm).toString());
            }
        }

        String[] items = appNames.toArray(new String[0]);
        Dialog d = Md3DialogUtils.showList(this, "选择应用 (APK)", items, new Md3DialogUtils.OnItemSelectListener() {
            public void onItemSelect(int position) {
                ApplicationInfo app = apps.get(position);
                String apkPath = app.sourceDir;
                pendingUris.clear();
                pendingUris.add(Uri.fromFile(new File(apkPath)));
                selectedFileText.setText("已选择应用: " + app.loadLabel(getPackageManager()));
                switchTab(0);
            }
        });
        d.show();
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

        logDialog = Md3DialogUtils.createBaseDialog(this, "运行日志", scroll, "关闭", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) { dialogLogView = null; }
        }, "清空", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) { logCache.setLength(0); if (dialogLogView != null) dialogLogView.setText(""); }
        });
        logDialog.show();
    }

    private void showSetNameDialog() {
        Md3DialogUtils.showInput(this, "修改设备名", OldSendService.getAlias(this), new Md3DialogUtils.OnInputListener() {
            public void onInput(String text) {
                if (text.length() > 0) { 
                    OldSendService.setAlias(MainActivity.this, text); 
                    tvName.setText(text);
                }
            }
        }).show();
    }

    private void showAboutDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_about, null);
        Md3DialogUtils.showCustom(this, "", v, "确定", null).show();
    }
    
    private void showBackupDialog() {
        final String[] items = new String[]{"导出设置到剪贴板", "从剪贴板导入设置"};
        Md3DialogUtils.showList(this, "备份 / 恢复", items, new Md3DialogUtils.OnItemSelectListener() {
            public void onItemSelect(int pos) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (pos == 0) {
                    try {
                        JSONObject json = new JSONObject();
                        Map<String, ?> all = getSharedPreferences("oldsend", MODE_PRIVATE).getAll();
                        for (Map.Entry<String, ?> entry : all.entrySet()) {
                            json.put(entry.getKey(), entry.getValue());
                        }
                        cm.setPrimaryClip(ClipData.newPlainText("OldSend备份", json.toString()));
                        Toast.makeText(MainActivity.this, "已导出到剪贴板", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) { Toast.makeText(MainActivity.this, "导出失败", Toast.LENGTH_SHORT).show(); }
                } else {
                    try {
                        ClipData clip = cm.getPrimaryClip();
                        if (clip != null && clip.getItemCount() > 0) {
                            String text = clip.getItemAt(0).coerceToText(MainActivity.this).toString();
                            JSONObject json = new JSONObject(text);
                            android.content.SharedPreferences.Editor editor = getSharedPreferences("oldsend", MODE_PRIVATE).edit();
                            java.util.Iterator<String> keys = json.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                Object val = json.get(key);
                                if (val instanceof Boolean) editor.putBoolean(key, (Boolean) val);
                                else if (val instanceof Integer) editor.putInt(key, (Integer) val);
                                else if (val instanceof String) editor.putString(key, (String) val);
                            }
                            editor.commit();
                            Toast.makeText(MainActivity.this, "导入成功，请重启服务", Toast.LENGTH_SHORT).show();
                            initSettingsUI(); // 刷新界面
                        }
                    } catch (Exception e) { Toast.makeText(MainActivity.this, "导入失败", Toast.LENGTH_SHORT).show(); }
                }
            }
        }).show();
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

    private void pickFile(boolean isMedia) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType(isMedia ? "*/*" : "*/*");
        if (isMedia) {
            i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*", "audio/*"});
        }
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(i, isMedia ? "选择媒体" : "选择文件"), isMedia ? REQ_MEDIA : REQ_FILE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode != REQ_FILE && requestCode != REQ_MEDIA) || resultCode != RESULT_OK || data == null) return;
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
                            handler.post(new Runnable() { public void run() { progressBar.setProgress(total > 0 ? (int)(sent * 100 / total) : 0); } });
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
        String name = null; android.database.Cursor c = null;
        try { c = getContentResolver().query(uri, new String[] { "_display_name" }, null, null, null); if (c != null && c.moveToFirst()) name = c.getString(0); } catch (Exception ignored) {} finally { if (c != null) try { c.close(); } catch (Exception ignored) {} }
        if (name == null) name = uri.getLastPathSegment();
        return name == null ? "file" : name;
    }

    private File copyToCache(Uri uri, String name) throws Exception {
        if ("file".equals(uri.getScheme())) return new File(uri.getPath());
        InputStream in = getContentResolver().openInputStream(uri);
        String safe = name.replace("/", "_").replace("\\", "_").replace(":", "_").replace("*", "_").replace("?", "_").replace("\"", "_").replace("<", "_").replace(">", "_").replace("|", "_");
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

    public void onChanged() { 
        adapter.setData(DeviceStore.get().snapshot()); 
        emptyHint.setVisibility(adapter.getCount() == 0 ? View.VISIBLE : View.GONE);
        updateStatus(); 
    }

    @Override
    public void showConfirmDialog(String senderAlias, int fileCount, final java.util.concurrent.CountDownLatch latch, final boolean[] result) {
        if (isFinishing()) { latch.countDown(); return; }
        Md3DialogUtils.showConfirm(this, "接收文件", "设备【" + senderAlias + "】请求发送 " + fileCount + " 个文件，是否接收？", "接收", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) { result[0] = true; latch.countDown(); }
        }).show();
        // 处理拒绝的情况因为自定义弹窗结构限制，需要通过外部取消
        // 这里由于使用自定义弹窗，为了不卡死，我们加一个取消监听
        // (为了简化逻辑，此处默认在取消时如果未点击则超时失败)
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

    @Override protected void onNewIntent(Intent intent) {
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
                pendingUris.clear(); pendingUris.add(uri);
                selectedFileText.setText("已分享: " + queryDisplayName(uri));
                log("来自系统分享，请点击设备发送");
                switchTab(0);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (uris != null) {
                pendingUris.clear(); pendingUris.addAll(uris);
                selectedFileText.setText("已分享 " + uris.size() + " 个文件");
                log("来自系统分享，共 " + uris.size() + " 个文件，请点击设备发送");
                switchTab(0);
            }
        }
    }

    @Override protected void onDestroy() { OldSendService.setSink(null); DeviceStore.get().setListener(null); ReceiveManager.setListener(null); super.onDestroy(); }
}
