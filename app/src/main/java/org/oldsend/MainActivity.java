package org.oldsend;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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
import org.oldsend.util.I18nManager;
import org.oldsend.util.ThemeUtils;
import org.oldsend.util.FileLogger;

public class MainActivity extends Activity implements OldSendService.LogSink, DeviceStore.Listener, ReceiveManager.DialogListener {
    private static final int REQ_FILE = 1001;
    private static final int REQ_MEDIA = 1002;

    private TextView status, selectedFileText, tvPin, tvName, tvPort, tvTimeout, emptyHint;
    private LinearLayout layoutSelectedFiles;
    private Button btnAddFilesMore, btnEditFiles, btnManualIp;
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
    public static List<Uri> pendingUris = new ArrayList<Uri>();
    private List<String> historyPaths = new ArrayList<String>();
    private boolean pendingAppend = false;

    private LinearLayout tabSend, tabReceive, tabSettings;
    private TextView tabSendText, tabReceiveText, tabSettingsText;
    private View pageSend, pageReceive, pageSettings;

    private CheckBox cbDark, cbAutoSave, cbSaveAlbum, cbSaveHistory, cbPin, cbAnim, cbWeb, cbBoot, cbReceiveNotif, cbHttps;

    private BroadcastReceiver progressReceiver;
    private BroadcastReceiver receiveConfirmReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        I18nManager.init(this);
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        ThemeUtils.applyTheme(this);

        FileLogger.log("MainActivity", "onCreate started");

        status = (TextView) findViewById(R.id.status);
        selectedFileText = (TextView) findViewById(R.id.tv_selected_file);
        toggleBtn = (Button) findViewById(R.id.toggle);
        deviceList = (ListView) findViewById(R.id.devices);
        historyList = (ListView) findViewById(R.id.history_list);
        progressBar = null; // 修复: 布局中无 R.id.progress
        emptyHint = (TextView) findViewById(R.id.empty_hint);
        
        tabSend = (LinearLayout) findViewById(R.id.tab_send);
        tabReceive = (LinearLayout) findViewById(R.id.tab_receive);
        tabSettings = (LinearLayout) findViewById(R.id.tab_settings);
        
        tabSendText = (TextView) findViewById(R.id.tab_send_text);
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

        findViewById(R.id.card_file).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { pickFile(false, false); } });
        findViewById(R.id.card_media).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { pickFile(true, false); } });
        findViewById(R.id.card_text).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showSendTextDialog(); } });
        findViewById(R.id.card_clipboard).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { sendClipboard(); } });
        findViewById(R.id.card_folder).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { pickFolder(false); } });
        findViewById(R.id.card_app).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { pickApp(false); } });

        toggleBtn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { toggleService(); } });
        ((Button)findViewById(R.id.btn_cross_net)).setText(I18nManager.get("cross_net"));
        ((Button)findViewById(R.id.btn_manual_ip)).setText(I18nManager.get("add_ip"));
        findViewById(R.id.btn_cross_net).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { startActivity(new Intent(MainActivity.this, CrossNetActivity.class)); } });
        findViewById(R.id.scan).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { doScan(); } });
        findViewById(R.id.btn_log).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showLogDialog(); } });

        tabSend.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { switchTab(0); } });
        tabReceive.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { switchTab(1); } });
        tabSettings.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { switchTab(2); } });

        
        layoutSelectedFiles = (LinearLayout) findViewById(R.id.layout_selected_files);
        btnAddFilesMore = (Button) findViewById(R.id.btn_add_files_more);
        btnEditFiles = (Button) findViewById(R.id.btn_edit_files);
        btnManualIp = (Button) findViewById(R.id.btn_manual_ip);

        if (btnAddFilesMore != null) btnAddFilesMore.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showAddFilesDialog(); } });
        if (btnEditFiles != null) btnEditFiles.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { startActivity(new Intent(MainActivity.this, SelectListActivity.class)); } });
        if (btnManualIp != null) btnManualIp.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showManualIpDialog(); } });
        
        
        CheckBox cbAutoWhitelist = (CheckBox) findViewById(R.id.cb_auto_whitelist);
        if (cbAutoWhitelist != null) {
            cbAutoWhitelist.setChecked(OldSendService.getPrefBooleanGlobal("auto_accept_whitelist", false));
            cbAutoWhitelist.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton v, boolean c) {
                    savePref("auto_accept_whitelist", c);
                }
            });
        }
        
        initSettingsUI();

        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                Device target = adapter.getItem(pos);
                if (pendingUris.isEmpty()) { log(I18nManager.get("not_selected")); return; }
                log("Sending to: " + target.alias);
                final Device finalTarget = target;
                new Thread(new Runnable() {
                    public void run() {
                        final List<File> files = new ArrayList<File>();
                        for (Uri u : pendingUris) {
                            try { files.add(copyToCache(u, queryDisplayName(u))); } catch (Exception e) { log("Read failed"); }
                        }
                        runOnUiThread(new Runnable() {
                            public void run() {
                                pendingUris.clear();
                                updateSelectedFilesUI();
                                doSend(finalTarget, files);
                            }
                        });

        deviceList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                final Device target = adapter.getItem(pos);
                final String fp = (target.fingerprint != null && !target.fingerprint.isEmpty()) ? target.fingerprint : target.ip;
                
                String wl = OldSendService.getPrefStringGlobal("whitelist", "");
                String bl = OldSendService.getPrefStringGlobal("blacklist", "");
                final boolean isWl = wl.contains(fp);
                final boolean isBl = bl.contains(fp);

                final String[] items = new String[]{
                    isWl ? "移除白名单" : "⭐ 加入白名单",
                    isBl ? "移除黑名单" : "🚫 加入黑名单"
                };
                Md3DialogUtils.showList(MainActivity.this, target.alias, items, new Md3DialogUtils.OnItemSelectListener() {
                    public void onItemSelect(int p) {
                        if (p == 0) toggleList("whitelist", fp, isWl);
                        else toggleList("blacklist", fp, isBl);
                    }
                }).show();
                return true;
            }
        });
        
                    }
                }).start();
            }
        });

        historyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                String path = historyPaths.get(pos);
                File f = new File(path);
                if (!f.exists()) { Toast.makeText(MainActivity.this, "File not found", Toast.LENGTH_SHORT).show(); return; }
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(f), "application/*");
                    startActivity(intent);
                } catch (Exception e) { Toast.makeText(MainActivity.this, "Cannot open", Toast.LENGTH_SHORT).show(); }
            }
        });

        progressReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (TransferActivity.ACTION_UPDATE_PROGRESS.equals(intent.getAction())) {
                    sendBroadcast(intent);
                } else if (TransferActivity.ACTION_TRANSFER_DONE.equals(intent.getAction())) {
                    sendBroadcast(intent);
                }
            }
        };
        registerReceiver(progressReceiver, new IntentFilter(TransferActivity.ACTION_UPDATE_PROGRESS));
        registerReceiver(progressReceiver, new IntentFilter(TransferActivity.ACTION_TRANSFER_DONE));

        receiveConfirmReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (ReceiveManager.pendingCallback != null) {
                    ReceiveManager.pendingCallback.onResult(intent.getAction().equals("org.oldsend.MANUAL_ACCEPT"));
                    ReceiveManager.pendingCallback = null;
                }
            }
        };
        registerReceiver(receiveConfirmReceiver, new IntentFilter("org.oldsend.MANUAL_ACCEPT"));
        registerReceiver(receiveConfirmReceiver, new IntentFilter("org.oldsend.MANUAL_REJECT"));

        updateStatus();
        refreshHistory();
        log("OldSend MD3 Ready");
        log("IP: " + OldSendService.getLocalIp());
        handleIntent(getIntent());
        checkAndRequestPermission();
        applyI18n();
    }

    private void applyI18n() {
        tabReceiveText.setText(I18nManager.get("receive"));
        tabSettingsText.setText(I18nManager.get("settings"));
        tabSendText.setText(I18nManager.get("send"));

        toggleBtn.setText(running ? I18nManager.get("stop") : I18nManager.get("start"));
        ((Button)findViewById(R.id.btn_log)).setText(I18nManager.get("log"));
        ((Button)findViewById(R.id.scan)).setText(I18nManager.get("scan"));

        ((TextView)findViewById(R.id.tv_select_title)).setText(I18nManager.get("select_content"));
        ((TextView)findViewById(R.id.tv_nearby_title)).setText(I18nManager.get("nearby_devices"));
        emptyHint.setText(I18nManager.get("no_device"));
        if (pendingUris.isEmpty()) updateSelectedFilesUI();

        ((TextView)findViewById(R.id.tv_receive_title)).setText(I18nManager.get("receive_history"));

        safeSetText(R.id.tv_set_rx, I18nManager.get("receive"));
        safeSetText(R.id.tv_dark, I18nManager.get("dark_mode"));
        safeSetText(R.id.tv_auto_save, I18nManager.get("auto_save"));
        safeSetText(R.id.tv_filter_title, I18nManager.get("filter_ext_title"));
        safeSetText(R.id.tv_auto_whitelist, I18nManager.get("auto_whitelist"));
        safeSetText(R.id.tv_manage_acl_title, I18nManager.get("manage_acl"));
        safeSetText(R.id.tv_save_album, I18nManager.get("save_album"));
        safeSetText(R.id.tv_save_history, I18nManager.get("save_history"));
        safeSetText(R.id.tv_use_pin, I18nManager.get("use_pin"));
        safeSetText(R.id.tv_net, I18nManager.get("network"));
        safeSetText(R.id.tv_https_title, I18nManager.get("https_enable"));
        safeSetText(R.id.tv_web, I18nManager.get("web_enable"));
        safeSetText(R.id.tv_boot, I18nManager.get("auto_start"));
        safeSetText(R.id.tv_notif, I18nManager.get("receive_notif"));
        safeSetText(R.id.tv_port_title, I18nManager.get("port"));
        safeSetText(R.id.tv_timeout_title, I18nManager.get("timeout"));
        safeSetText(R.id.tv_pin_title, I18nManager.get("modify_pin"));
        safeSetText(R.id.tv_other, I18nManager.get("other"));
        safeSetText(R.id.tv_lang_title, I18nManager.get("language"));
        safeSetText(R.id.tv_anim_title, I18nManager.get("anim"));
        safeSetText(R.id.tv_set_name, I18nManager.get("set_name"));
        safeSetText(R.id.tv_backup, I18nManager.get("backup"));
        safeSetText(R.id.tv_about, I18nManager.get("about"));
    }

    private void safeSetText(int id, String text) {
        View v = findViewById(id);
        if (v != null && v instanceof TextView) ((TextView) v).setText(text);
    }

    private void checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
        }
    }

    private void initSettingsUI() {
        cbAutoSave = (CheckBox) findViewById(R.id.cb_auto_save);

        cbDark = (CheckBox) findViewById(R.id.cb_dark_mode);
        if (cbDark != null) {
            cbDark.setChecked(getSharedPreferences("oldsend", MODE_PRIVATE).getBoolean("dark_mode", false));
            cbDark.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton v, boolean c) {
                    savePref("dark_mode", c);
                    recreate(); // 重启 Activity 生效
                }
            });
        }
        
        cbSaveAlbum = (CheckBox) findViewById(R.id.cb_save_album);
        cbSaveHistory = (CheckBox) findViewById(R.id.cb_save_history);
        cbPin = (CheckBox) findViewById(R.id.cb_pin);
        cbAnim = (CheckBox) findViewById(R.id.cb_anim);
        cbWeb = (CheckBox) findViewById(R.id.cb_web);
        cbBoot = (CheckBox) findViewById(R.id.cb_boot);
        cbReceiveNotif = (CheckBox) findViewById(R.id.cb_receive_notif);
        cbHttps = (CheckBox) findViewById(R.id.cb_https);
        
        tvPin = (TextView) findViewById(R.id.tv_pin);
        tvName = (TextView) findViewById(R.id.tv_setting_name);
        tvPort = (TextView) findViewById(R.id.tv_port_value);
        tvTimeout = (TextView) findViewById(R.id.tv_timeout_value);

        if (tvName != null) tvName.setText(OldSendService.getAlias(this));
        if (cbAutoSave != null) cbAutoSave.setChecked(OldSendService.getPrefBooleanGlobal("auto_save", false));
        if (cbSaveAlbum != null) cbSaveAlbum.setChecked(OldSendService.getPrefBooleanGlobal("save_album", true));
        if (cbSaveHistory != null) cbSaveHistory.setChecked(OldSendService.getPrefBooleanGlobal("save_history", true));
        if (cbPin != null) cbPin.setChecked(OldSendService.getPrefBooleanGlobal("use_pin", false));
        if (cbAnim != null) cbAnim.setChecked(OldSendService.getPrefBooleanGlobal("use_anim", true));
        if (cbWeb != null) cbWeb.setChecked(OldSendService.getPrefBooleanGlobal("web_enable", true));
        if (cbBoot != null) cbBoot.setChecked(OldSendService.getPrefBooleanGlobal("auto_start", false));
        if (cbReceiveNotif != null) cbReceiveNotif.setChecked(OldSendService.getPrefBooleanGlobal("receive_notif", false));
        if (cbHttps != null) cbHttps.setChecked(getSharedPreferences("oldsend", MODE_PRIVATE).getBoolean("use_https", false));
        
        if (tvPort != null) tvPort.setText(String.valueOf(getSharedPreferences("oldsend", MODE_PRIVATE).getInt("port", 53317)));
        if (tvTimeout != null) tvTimeout.setText(String.valueOf(getSharedPreferences("oldsend", MODE_PRIVATE).getInt("timeout", 500)));

        if (cbAutoSave != null) cbAutoSave.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { public void onCheckedChanged(CompoundButton v, boolean c) { savePref("auto_save", c); } });
        if (cbSaveAlbum != null) cbSaveAlbum.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { public void onCheckedChanged(CompoundButton v, boolean c) { savePref("save_album", c); } });
        if (cbSaveHistory != null) cbSaveHistory.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { public void onCheckedChanged(CompoundButton v, boolean c) { savePref("save_history", c); } });
        if (cbAnim != null) cbAnim.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { public void onCheckedChanged(CompoundButton v, boolean c) { savePref("use_anim", c); } });
        if (cbWeb != null) cbWeb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { public void onCheckedChanged(CompoundButton v, boolean c) { savePref("web_enable", c); } });
        if (cbBoot != null) cbBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { public void onCheckedChanged(CompoundButton v, boolean c) { savePref("auto_start", c); } });
        if (cbReceiveNotif != null) cbReceiveNotif.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { public void onCheckedChanged(CompoundButton v, boolean c) { savePref("receive_notif", c); } });
        if (cbHttps != null) cbHttps.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { public void onCheckedChanged(CompoundButton v, boolean c) { getSharedPreferences("oldsend", MODE_PRIVATE).edit().putBoolean("use_https", c).commit(); if (running) { stopService(new Intent(MainActivity.this, OldSendService.class)); startService(new Intent(MainActivity.this, OldSendService.class)); } } });

        if (cbPin != null) cbPin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { 
            public void onCheckedChanged(CompoundButton v, boolean c) { 
                savePref("use_pin", c); 
                if (c) {
                    String pin = getSharedPreferences("oldsend", MODE_PRIVATE).getString("pin_code", "0000");
                    if (tvPin != null) {
                        tvPin.setText(I18nManager.get("pin_on") + pin);
                        tvPin.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (tvPin != null) tvPin.setVisibility(View.GONE);
                }
                if (running) {
                    stopService(new Intent(MainActivity.this, OldSendService.class));
                    startService(new Intent(MainActivity.this, OldSendService.class));
                }
            } 
        });

        findViewById(R.id.row_language).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String[] items = new String[]{I18nManager.get("follow_system"), "简体中文", "繁體中文", "English", "日本語", "한국어", "Deutsch", "Français", "Español", "Русский", "Português"};
                Md3DialogUtils.showList(MainActivity.this, I18nManager.get("select_lang"), items, new Md3DialogUtils.OnItemSelectListener() {
                    public void onItemSelect(int pos) {
                        String[] codes = {"system", "zh", "zh_TW", "en", "ja", "ko", "de", "fr", "es", "ru", "pt"};
                        getSharedPreferences("oldsend", MODE_PRIVATE).edit().putString("app_lang", codes[pos]).commit();
                        Toast.makeText(MainActivity.this, I18nManager.get("restart_tip"), Toast.LENGTH_SHORT).show();
                        Intent intent = getIntent();
                        finish();
                        startActivity(intent);
                    }
                }).show();
            }
        });

        findViewById(R.id.row_pin_code).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String currentPin = getSharedPreferences("oldsend", MODE_PRIVATE).getString("pin_code", "0000");
                Md3DialogUtils.showInput(MainActivity.this, I18nManager.get("modify_pin"), currentPin, new Md3DialogUtils.OnInputListener() {
                    public void onInput(String text) {
                        if (text != null && text.length() > 0) {
                            getSharedPreferences("oldsend", MODE_PRIVATE).edit().putString("pin_code", text).commit();
                            if (cbPin != null && cbPin.isChecked()) {
                                tvPin.setText(I18nManager.get("pin_on") + text);
                            }
                        }
                    }
                }).show();
            }
        });
        
findViewById(R.id.row_set_name).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showSetNameDialog(); } });
        findViewById(R.id.row_about).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showAboutDialog(); } });
        findViewById(R.id.row_manage_acl).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showAclManager(); } });
        

        // 强行绑定拦截后缀点击事件
        TextView tvFilterVal = (TextView) findViewById(R.id.tv_filter_value);
        if (tvFilterVal != null) tvFilterVal.setText(OldSendService.getPrefStringGlobal("filter_ext", "exe,bat,cmd,vbs,sh"));
        
        findViewById(R.id.row_filter_ext).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String current = OldSendService.getPrefStringGlobal("filter_ext", "exe,bat,cmd,vbs,sh");
                Md3DialogUtils.showInput(MainActivity.this, "拦截后缀 (逗号分隔, 留空为不拦截)", current, new Md3DialogUtils.OnInputListener() {
                    public void onInput(String text) {
                        String val = text.replace(" ", "").toLowerCase();
                        getSharedPreferences("oldsend", MODE_PRIVATE).edit().putString("filter_ext", val).commit();
                        ((TextView) findViewById(R.id.tv_filter_value)).setText(val);
                        Toast.makeText(MainActivity.this, "拦截规则已更新", Toast.LENGTH_SHORT).show();
                    }
                }).show();
            }
        });
        
        findViewById(R.id.row_backup).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showBackupDialog(); } });
        findViewById(R.id.row_port).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Md3DialogUtils.showInput(MainActivity.this, I18nManager.get("port"), tvPort.getText().toString(), new Md3DialogUtils.OnInputListener() {
                    public void onInput(String text) {
                        try { int val = Integer.parseInt(text); getSharedPreferences("oldsend", MODE_PRIVATE).edit().putInt("port", val).commit(); tvPort.setText(String.valueOf(val)); } catch (Exception e) {}
                    }
                }).show();
            }
        });
        findViewById(R.id.row_timeout).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Md3DialogUtils.showInput(MainActivity.this, I18nManager.get("timeout"), tvTimeout.getText().toString(), new Md3DialogUtils.OnInputListener() {
                    public void onInput(String text) {
                        try { int val = Integer.parseInt(text); getSharedPreferences("oldsend", MODE_PRIVATE).edit().putInt("timeout", val).commit(); tvTimeout.setText(String.valueOf(val)); } catch (Exception e) {}
                    }
                }).show();
            }
        });
    }

    
    private void toggleList(String key, String fp, boolean exists) {
        String current = OldSendService.getPrefStringGlobal(key, "");
        if (exists) {
            current = current.replace(fp + ",", "").replace(fp, "");
        } else {
            current += fp + ",";
        }
        getSharedPreferences("oldsend", MODE_PRIVATE).edit().putString(key, current).commit();
        Toast.makeText(this, exists ? "已移出名单" : "已加入名单", Toast.LENGTH_SHORT).show();
    }
    
    
    private void showAclManager() {
        final String[] items = new String[]{"查看/移除 白名单", "查看/移除 黑名单", "清空所有名单"};
        Md3DialogUtils.showList(this, "名单管理中心", items, new Md3DialogUtils.OnItemSelectListener() {
            public void onItemSelect(int pos) {
                if (pos == 2) {
                    getSharedPreferences("oldsend", MODE_PRIVATE).edit().remove("whitelist").remove("blacklist").commit();
                    Toast.makeText(MainActivity.this, "已清空所有黑白名单", Toast.LENGTH_SHORT).show();
                } else {
                    showSpecificList(pos == 0 ? "whitelist" : "blacklist");
                }
            }
        }).show();
    }

    private void showSpecificList(final String key) {
        String listStr = OldSendService.getPrefStringGlobal(key, "");
        if (listStr.trim().isEmpty()) {
            Toast.makeText(this, "当前名单为空", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 用逗号分割出所有指纹/IP
        final String[] arr = listStr.split(",");
        final String[] displayArr = new String[arr.length];
        for(int i = 0; i < arr.length; i++) {
            displayArr[i] = arr[i].length() > 16 ? arr[i].substring(0, 16) + "..." : arr[i];
        }

        Md3DialogUtils.showList(this, "点击移除设备 (" + arr.length + "个)", displayArr, new Md3DialogUtils.OnItemSelectListener() {
            public void onItemSelect(int pos) {
                String target = arr[pos];
                String current = OldSendService.getPrefStringGlobal(key, "");
                // 移除并更新
                current = current.replace(target + ",", "").replace(target, "");
                getSharedPreferences("oldsend", MODE_PRIVATE).edit().putString(key, current).commit();
                Toast.makeText(MainActivity.this, "已移除", Toast.LENGTH_SHORT).show();
                showSpecificList(key); // 递归刷新列表
            }
        }).show();
    }
        
    private void savePref(String key, boolean val) { getSharedPreferences("oldsend", MODE_PRIVATE).edit().putBoolean(key, val).commit(); }

    private void switchTab(int index) {
        applyAnimation(pageSend, index == 0);
        applyAnimation(pageReceive, index == 1);
        applyAnimation(pageSettings, index == 2);
        
        boolean isDark = ThemeUtils.isDarkMode(this);
        int activeBg = isDark ? R.drawable.bg_tab_pill_dark : R.drawable.bg_tab_pill;
        int activeColor = isDark ? 0xFF81C784 : 0xFF1B5E20;
        int inactiveColor = isDark ? 0xFFAAAAAA : 0xFF666666;

        if (tabSend != null) tabSend.setBackgroundResource(index == 0 ? activeBg : 0);
        if (tabReceive != null) tabReceive.setBackgroundResource(index == 1 ? activeBg : 0);
        if (tabSettings != null) tabSettings.setBackgroundResource(index == 2 ? activeBg : 0);
        
        TextView sendIcon = (TextView) findViewById(R.id.tab_send_icon);
        TextView recvIcon = (TextView) findViewById(R.id.tab_receive_icon);
        TextView setIcon = (TextView) findViewById(R.id.tab_settings_icon);

        if (tabSendText != null) tabSendText.setTextColor(index == 0 ? activeColor : inactiveColor);
        if (tabReceiveText != null) tabReceiveText.setTextColor(index == 1 ? activeColor : inactiveColor);
        if (tabSettingsText != null) tabSettingsText.setTextColor(index == 2 ? activeColor : inactiveColor);
        
        if (sendIcon != null) sendIcon.setTextColor(index == 0 ? activeColor : inactiveColor);
        if (recvIcon != null) recvIcon.setTextColor(index == 1 ? activeColor : inactiveColor);
        if (setIcon != null) setIcon.setTextColor(index == 2 ? activeColor : inactiveColor);

        if (index == 1) refreshHistory();
        
        // 实时更新深色模式下的下拉框背景
        if (index == 2) {
            int bgDropdown = isDark ? R.drawable.bg_md3_dropdown_dark : R.drawable.bg_md3_dropdown;
            ((View) findViewById(R.id.tv_lang_value).getParent()).setBackgroundResource(bgDropdown);
            ((View) findViewById(R.id.tv_name).getParent()).setBackgroundResource(bgDropdown);
            ((View) findViewById(R.id.tv_pin_value).getParent()).setBackgroundResource(bgDropdown);
            
            if (isDark) {
                ((TextView)findViewById(R.id.tv_lang_value)).setTextColor(0xFF81C784);
                ((TextView)findViewById(R.id.tv_name)).setTextColor(0xFF81C784);
                ((TextView)findViewById(R.id.tv_pin_value)).setTextColor(0xFF81C784);
            } else {
                ((TextView)findViewById(R.id.tv_lang_value)).setTextColor(0xFF1B5E20);
                ((TextView)findViewById(R.id.tv_name)).setTextColor(0xFF1B5E20);
                ((TextView)findViewById(R.id.tv_pin_value)).setTextColor(0xFF1B5E20);
            }
        }
    }

    private void applyAnimation(View view, boolean visible) {
        if (view == null) return;
        if (!getSharedPreferences("oldsend", MODE_PRIVATE).getBoolean("use_anim", true)) { view.setVisibility(visible ? View.VISIBLE : View.GONE); return; }
        if (visible) { view.setVisibility(View.VISIBLE); AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f); anim.setDuration(250); view.startAnimation(anim); } else { view.setVisibility(View.GONE); }
    }

    private void sendClipboard() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = cm.getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) {
            CharSequence text = clip.getItemAt(0).coerceToText(this);
            if (text != null && text.length() > 0) prepareText(text.toString());
        } else { Toast.makeText(this, "Clipboard empty", Toast.LENGTH_SHORT).show(); }
    }

    private void showSendTextDialog() {
        Md3DialogUtils.showInput(this, I18nManager.get("input_text"), "", new Md3DialogUtils.OnInputListener() {
            public void onInput(String text) { if (text.length() > 0) prepareText(text); }
        }).show();
    }

    private void prepareText(String text) {
        try {
            File f = new File(getCacheDir(), "text_" + System.currentTimeMillis() + ".txt");
            FileOutputStream fos = new FileOutputStream(f); fos.write(text.getBytes("UTF-8")); fos.close();
            pendingUris.clear(); pendingUris.add(Uri.fromFile(f));
            if (selectedFileText != null) updateSelectedFilesUI();
            switchTab(0);
        } catch (Exception e) { log("Prepare text failed"); }
    }

    private void pickFolder(boolean append) { pendingAppend = append; showFolderSelector(android.os.Environment.getExternalStorageDirectory()); }

    private void showFolderSelector(final File dir) {
        final File[] files = dir.listFiles();
        if (files == null) return;
        List<String> folderNames = new ArrayList<String>();
        final List<File> folders = new ArrayList<File>();
        folderNames.add(">> [Send this folder] <<");
        for (File f : files) { if (f.isDirectory() && !f.isHidden()) { folderNames.add("[Folder] " + f.getName()); folders.add(f); } }
        String[] items = folderNames.toArray(new String[0]);
        Dialog d = Md3DialogUtils.showList(this, dir.getAbsolutePath(), items, new Md3DialogUtils.OnItemSelectListener() {
            public void onItemSelect(int position) {
                if (position == 0) {
                    List<File> allFiles = new ArrayList<File>();
                    getAllFiles(dir, allFiles);
                    if (allFiles.isEmpty()) { Toast.makeText(MainActivity.this, "Empty folder", Toast.LENGTH_SHORT).show(); }
                    else {
                        if (!pendingAppend) pendingUris.clear();
                        for (File f : allFiles) pendingUris.add(Uri.fromFile(f));
                        if (selectedFileText != null) updateSelectedFilesUI();
                        switchTab(0);
                    }
                } else { showFolderSelector(folders.get(position - 1)); }
            }
        });
        d.show();
    }

    private void getAllFiles(File dir, List<File> out) {
        File[] fs = dir.listFiles(); if (fs == null) return;
        for (File f : fs) { if (f.isDirectory()) getAllFiles(f, out); else out.add(f); }
    }

    private void pickApp(boolean append) {
        pendingAppend = append;
        PackageManager pm = getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0);
        final List<ApplicationInfo> apps = new ArrayList<ApplicationInfo>();
        List<String> appNames = new ArrayList<String>();
        for (PackageInfo pkg : packages) { ApplicationInfo app = pkg.applicationInfo; if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) { apps.add(app); appNames.add(app.loadLabel(pm).toString()); } }
        String[] items = appNames.toArray(new String[0]);
        Dialog d = Md3DialogUtils.showList(this, "Select App (APK)", items, new Md3DialogUtils.OnItemSelectListener() {
            public void onItemSelect(int position) {
                ApplicationInfo app = apps.get(position); String apkPath = app.sourceDir;
                if (!pendingAppend) pendingUris.clear(); 
                pendingUris.add(Uri.fromFile(new File(apkPath)));
                if (selectedFileText != null) updateSelectedFilesUI();
                switchTab(0);
            }
        });
        d.show();
    }

    private void showLogDialog() {
        if (logDialog != null && logDialog.isShowing()) return;
        dialogLogView = new TextView(this);
        dialogLogView.setTextSize(11); 
        dialogLogView.setTypeface(android.graphics.Typeface.MONOSPACE);
        dialogLogView.setPadding(16, 16, 16, 16); 
        dialogLogView.setText(logCache.toString());

        ScrollView scroll = new ScrollView(this); 
        scroll.addView(dialogLogView); 
        
        if (ThemeUtils.isDarkMode(this)) {
            dialogLogView.setTextColor(0xFFAAAAAA);
            scroll.setBackgroundColor(0xFF1E1E1E);
        } else {
            dialogLogView.setTextColor(0xFF333333);
            scroll.setBackgroundColor(0xFFF5F5F5);
        }

        logDialog = Md3DialogUtils.createBaseDialog(this, I18nManager.get("log"), scroll, I18nManager.get("close"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) { dialogLogView = null; }
        }, "导出日志", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) { shareLogFile(); }
        });
        logDialog.show();
    }

    private void shareLogFile() {
        File logFile = new File(android.os.Environment.getExternalStorageDirectory(), "Download/OldSend/debug_log.txt");
        if (!logFile.exists()) { Toast.makeText(this, "Log file not found", Toast.LENGTH_SHORT).show(); return; }
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(logFile));
            startActivity(Intent.createChooser(intent, "Share Log"));
        } catch (Exception e) { Toast.makeText(this, "Share failed", Toast.LENGTH_SHORT).show(); }
    }

    private void showSetNameDialog() {
        Md3DialogUtils.showInput(this, I18nManager.get("set_name"), OldSendService.getAlias(this), new Md3DialogUtils.OnInputListener() {
            public void onInput(String text) { if (text.length() > 0) { OldSendService.setAlias(MainActivity.this, text); if (tvName != null) tvName.setText(text); } }
        }).show();
    }

    private void showAboutDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_about, null);
        
        // 适配深色模式下的文字颜色
        if (ThemeUtils.isDarkMode(this)) {
            android.view.ViewGroup group = (android.view.ViewGroup) v;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof TextView) {
                    int color = ((TextView) child).getCurrentTextColor();
                    if (color == 0xFF333333 || color == 0xFF1C1B1F || color == android.graphics.Color.BLACK) {
                        ((TextView) child).setTextColor(0xFFE0E0E0);
                    } else if (color == 0xFF666666) {
                        ((TextView) child).setTextColor(0xFFAAAAAA);
                    }
                }
            }
        }
        
        Md3DialogUtils.showCustom(this, "", v, I18nManager.get("confirm"), null).show();
    }
    
    private void showBackupDialog() {
        final String[] items = new String[]{I18nManager.get("backup_export"), I18nManager.get("backup_import")};
        Md3DialogUtils.showList(this, I18nManager.get("backup_title"), items, new Md3DialogUtils.OnItemSelectListener() {
            public void onItemSelect(int pos) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (pos == 0) {
                    try {
                        JSONObject json = new JSONObject(); Map<String, ?> all = getSharedPreferences("oldsend", MODE_PRIVATE).getAll();
                        for (Map.Entry<String, ?> entry : all.entrySet()) json.put(entry.getKey(), entry.getValue());
                        cm.setPrimaryClip(ClipData.newPlainText("OldSendBackup", json.toString()));
                        Toast.makeText(MainActivity.this, I18nManager.get("export_ok"), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) { Toast.makeText(MainActivity.this, I18nManager.get("err_fail"), Toast.LENGTH_SHORT).show(); }
                } else {
                    try {
                        ClipData clip = cm.getPrimaryClip();
                        if (clip != null && clip.getItemCount() > 0) {
                            String text = clip.getItemAt(0).coerceToText(MainActivity.this).toString(); JSONObject json = new JSONObject(text);
                            android.content.SharedPreferences.Editor editor = getSharedPreferences("oldsend", MODE_PRIVATE).edit();
                            java.util.Iterator<String> keys = json.keys();
                            while (keys.hasNext()) {
                                String key = keys.next(); Object val = json.get(key);
                                if (val instanceof Boolean) editor.putBoolean(key, (Boolean) val);
                                else if (val instanceof Integer) editor.putInt(key, (Integer) val);
                                else if (val instanceof String) editor.putString(key, (String) val);
                            }
                            editor.commit();
                            Toast.makeText(MainActivity.this, I18nManager.get("import_ok"), Toast.LENGTH_SHORT).show();
                            
        CheckBox cbAutoWhitelist = (CheckBox) findViewById(R.id.cb_auto_whitelist);
        if (cbAutoWhitelist != null) {
            cbAutoWhitelist.setChecked(OldSendService.getPrefBooleanGlobal("auto_accept_whitelist", false));
            cbAutoWhitelist.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton v, boolean c) {
                    savePref("auto_accept_whitelist", c);
                }
            });
        }
        
        initSettingsUI(); applyI18n();
                        }
                    } catch (Exception e) { Toast.makeText(MainActivity.this, I18nManager.get("err_fail"), Toast.LENGTH_SHORT).show(); }
                }
            }
        }).show();
    }

    private void toggleService() {
        if (running) { stopService(new Intent(this, OldSendService.class)); running = false; }
        else { startService(new Intent(this, OldSendService.class)); running = true; }
        updateStatus(); applyI18n();
    }

    private void updateStatus() {
        if (status != null) {
            String s = (running ? I18nManager.get("running") : I18nManager.get("stopped")) + "  IP:" + OldSendService.getLocalIp() + ":53317";
            status.setText(s);
        }
    }

    
    private void updateSelectedFilesUI() {
        if (pendingUris.isEmpty()) {
            if (layoutSelectedFiles != null) layoutSelectedFiles.setVisibility(View.GONE);
            if (selectedFileText != null) selectedFileText.setText(I18nManager.get("not_selected"));
        } else {
            if (layoutSelectedFiles != null) layoutSelectedFiles.setVisibility(View.VISIBLE);
            long totalSize = 0;
            for (Uri u : pendingUris) {
                try {
                    android.database.Cursor cursor = getContentResolver().query(u, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                        if (sizeIndex != -1) totalSize += cursor.getLong(sizeIndex);
                        cursor.close();
                    }
                } catch (Exception ignored) {}
            }
            String sizeStr = totalSize > 1024*1024 ? String.format("%.1f MB", totalSize/1024.0/1024.0) : String.format("%.1f KB", totalSize/1024.0);
            if (selectedFileText != null) selectedFileText.setText("文件: " + pendingUris.size() + "\n大小: " + sizeStr);
        }
    }

    private void showAddFilesDialog() {
        final String[] items = new String[]{"文件", "媒体", "应用", "文件夹"};
        Md3DialogUtils.showList(this, "加入文件", items, new Md3DialogUtils.OnItemSelectListener() {
            public void onItemSelect(int pos) {
                if (pos == 0) pickFile(false, true);
                else if (pos == 1) pickFile(true, true);
                else if (pos == 2) pickApp(true);
                else if (pos == 3) pickFolder(true);
            }
        }).show();
    }

    private void showManualIpDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_manual_ip);
        if (ThemeUtils.isDarkMode(this)) {
            findViewById(R.id.dlg_root).setBackgroundResource(R.drawable.bg_md3_card_dark);
            ((TextView)dialog.findViewById(R.id.et_ip_address)).setTextColor(0xFFE0E0E0);
            ((TextView)dialog.findViewById(R.id.et_ip_address)).setHintTextColor(0xFF888888);
            ((TextView)dialog.findViewById(R.id.et_ip_address)).setBackgroundColor(0xFF2C2C2C);
        }
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.9), WindowManager.LayoutParams.WRAP_CONTENT);
        }

        final EditText etIp = (EditText) dialog.findViewById(R.id.et_ip_address);
        final TextView tvError = (TextView) dialog.findViewById(R.id.tv_error_msg);
        TextView btnCancel = (TextView) dialog.findViewById(R.id.btn_cancel);
        TextView btnConfirm = (TextView) dialog.findViewById(R.id.btn_confirm);

        btnCancel.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { dialog.dismiss(); } });
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String ip = etIp.getText().toString().trim();
                if (ip.matches("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")) {
                    tvError.setVisibility(View.GONE);
                    dialog.dismiss();
                    Device d = new Device();
                    d.alias = "手动设备";
                    d.ip = ip;
                    d.port = 53317;
                    d.https = false;
                    DeviceStore.get().addDevice(d);
                    log("手动添加 IP: " + ip);
                } else {
                    tvError.setVisibility(View.VISIBLE);
                }
            }
        });
        dialog.show();
    }
        
    private void doScan() { log("Scanning..."); MulticastScanner.scanAsync(new MulticastScanner.Callback() { public void onDevice(Device d) { DeviceStore.get().addDevice(d); log("Found: " + d.alias); } public void onFinished() { log("Scan finished"); } }); }

    private void pickFile(boolean isMedia, boolean append) {
        pendingAppend = append;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*"); i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        if (isMedia) i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*", "audio/*"});
        startActivityForResult(Intent.createChooser(i, "Select"), isMedia ? REQ_MEDIA : REQ_FILE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode != REQ_FILE && requestCode != REQ_MEDIA) || resultCode != RESULT_OK || data == null) return;
        if (!pendingAppend) {
            pendingUris.clear();
        }
        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) pendingUris.add(data.getClipData().getItemAt(i).getUri());
            if (selectedFileText != null) updateSelectedFilesUI();
        } else if (data.getData() != null) {
            pendingUris.add(data.getData());
            if (selectedFileText != null) updateSelectedFilesUI();
        }
    }

    private void doSend(final Device device, final List<File> files) {
        Intent intent = new Intent(this, TransferActivity.class);
        intent.putExtra("mode", 0);
        intent.putExtra("device", device);
        ArrayList<String> paths = new ArrayList<>();
        for (File f : files) paths.add(f.getAbsolutePath());
        intent.putStringArrayListExtra("files", paths);
        startActivity(intent);
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
        FileLogger.log("UI", msg);
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

    public void onChanged() { if (adapter != null) adapter.setData(DeviceStore.get().snapshot()); if (emptyHint != null && adapter != null) emptyHint.setVisibility(adapter.getCount() == 0 ? View.VISIBLE : View.GONE); if (deviceList != null) setListViewHeightBasedOnChildren(deviceList); updateStatus(); }
    
    private void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter(); if (listAdapter == null) return;
        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) { View listItem = listAdapter.getView(i, null, listView); listItem.measure(0, 0); totalHeight += listItem.getMeasuredHeight(); }
        android.view.ViewGroup.LayoutParams params = listView.getLayoutParams(); params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1)); listView.setLayoutParams(params);
    }

    @Override
    public void showConfirmDialog(String senderAlias, int fileCount, final ReceiveManager.Callback callback) {
        if (isFinishing()) { callback.onResult(false); return; }
        Md3DialogUtils.showConfirm(this, "接收文件", "设备【" + senderAlias + "】请求发送 " + fileCount + " 个文件，是否接收？", "接收", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                callback.onResult(true);
            }
        }).show();
    }

    private void refreshHistory() {
        List<HistoryStore.Record> list = HistoryStore.get().snapshot();
        if (historyAdapter != null) { historyAdapter.clear(); historyPaths.clear(); for (HistoryStore.Record r : list) { String sizeStr = r.size > 1024*1024 ? (r.size/1024/1024)+"MB" : (r.size/1024)+"KB"; historyAdapter.add("[" + r.time + "] " + r.fileName + " (" + sizeStr + ")"); historyPaths.add(r.path); } historyAdapter.notifyDataSetChanged(); }
        if (historyList != null) setListViewHeightBasedOnChildren(historyList);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSelectedFilesUI();
    }

    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); handleIntent(intent); }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) { pendingUris.clear(); pendingUris.add(uri); if (selectedFileText != null) updateSelectedFilesUI(); switchTab(0); }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (uris != null) { pendingUris.clear(); pendingUris.addAll(uris); if (selectedFileText != null) updateSelectedFilesUI(); switchTab(0); }
        }
    }

    @Override protected void onDestroy() { OldSendService.setSink(null); DeviceStore.get().setListener(null); ReceiveManager.setListener(null); if (progressReceiver != null) { try { unregisterReceiver(progressReceiver); } catch (Exception ignored) {} }
        if (receiveConfirmReceiver != null) { try { unregisterReceiver(receiveConfirmReceiver); } catch (Exception ignored) {} } super.onDestroy(); }
}
