package org.oldsend;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
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
import android.view.animation.ScaleAnimation;
import android.view.animation.Animation;
import android.view.MotionEvent;
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
    private TextView tabSendText, tabReceiveText, tabSettingsText;
    private View pageSend, pageReceive, pageSettings;

    private BroadcastReceiver progressReceiver;
    private CheckBox cbAutoSave, cbSaveAlbum, cbSaveHistory, cbPin, cbAnim, cbWeb, cbBoot, cbReceiveNotif, cbHttps;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        I18nManager.init(this);
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        status = (TextView) findViewById(R.id.status);
        selectedFileText = (TextView) findViewById(R.id.tv_selected_file);
        toggleBtn = (Button) findViewById(R.id.toggle);
        deviceList = (ListView) findViewById(R.id.devices);
        historyList = (ListView) findViewById(R.id.history_list);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        emptyHint = (TextView) findViewById(R.id.empty_hint);
        
        tabSend = (LinearLayout) findViewById(R.id.tab_send);
        tabReceive = (LinearLayout) findViewById(R.id.tab_receive);
        tabSettings = (LinearLayout) findViewById(R.id.tab_settings);
        
        // 修正获取方式，防止强转崩溃
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

        applyTabPressAnimation(tabSend);
        applyTabPressAnimation(tabReceive);
        applyTabPressAnimation(tabSettings);

        initSettingsUI();

        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                Device target = adapter.getItem(pos);
                if (pendingUris.isEmpty()) { log(I18nManager.get("not_selected")); return; }
                log("Sending to: " + target.alias);
                List<File> files = new ArrayList<File>();
                for (Uri u : pendingUris) {
                    try { files.add(copyToCache(u, queryDisplayName(u))); } catch (Exception e) { log("Read failed"); }
                }
                pendingUris.clear();
                selectedFileText.setText(I18nManager.get("not_selected"));
                doSend(target, files);
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

        updateStatus();
        refreshHistory();
        log("OldSend MD3 Ready");
        log("IP: " + OldSendService.getLocalIp());
        handleIntent(getIntent());
        checkAndRequestPermission();
        applyI18n();
    }

    private String getFirstChar(String str) {
        if (str != null && str.length() > 0) return str.substring(0, 1).toUpperCase();
        return "?";
    }

    // 添加安全设置文字方法
    private void safeSetText(int id, String text) {
        View v = findViewById(id);
        if (v != null && v instanceof TextView) {
            ((TextView) v).setText(text);
        }
    }

    private void applyI18n() {
        if (tabReceiveText != null) tabReceiveText.setText(I18nManager.get("receive"));
        if (tabSettingsText != null) tabSettingsText.setText(I18nManager.get("settings"));
        if (tabSendText != null) tabSendText.setText(I18nManager.get("send"));

        if (toggleBtn != null) toggleBtn.setText(running ? I18nManager.get("stop") : I18nManager.get("start"));
        
        safeSetText(R.id.btn_log, I18nManager.get("log"));
        safeSetText(R.id.scan, I18nManager.get("scan"));
        safeSetText(R.id.tv_select_title, I18nManager.get("select_content"));
        safeSetText(R.id.tv_nearby_title, I18nManager.get("nearby_devices"));
        
        if (emptyHint != null) emptyHint.setText(I18nManager.get("no_device"));
        if (pendingUris.isEmpty() && selectedFileText != null) selectedFileText.setText(I18nManager.get("not_selected"));

        safeSetText(R.id.tv_icon, getFirstChar(I18nManager.get("file")));
        safeSetText(R.id.tv_name, I18nManager.get("file"));
        
        // 手动处理 6 个卡片的文字，避免因为 id 冲突导致替换错误
        View cardFile = findViewById(R.id.card_file);
        if (cardFile != null) {
            ((TextView)cardFile.findViewById(R.id.tv_icon)).setText(getFirstChar(I18nManager.get("file")));
            ((TextView)cardFile.findViewById(R.id.tv_name)).setText(I18nManager.get("file"));
        }
        View cardMedia = findViewById(R.id.card_media);
        if (cardMedia != null) {
            ((TextView)cardMedia.findViewById(R.id.tv_icon)).setText(getFirstChar(I18nManager.get("media")));
            ((TextView)cardMedia.findViewById(R.id.tv_name)).setText(I18nManager.get("media"));
        }
        View cardFolder = findViewById(R.id.card_folder);
        if (cardFolder != null) {
            ((TextView)cardFolder.findViewById(R.id.tv_icon)).setText(getFirstChar(I18nManager.get("folder")));
            ((TextView)cardFolder.findViewById(R.id.tv_name)).setText(I18nManager.get("folder"));
        }
        View cardApp = findViewById(R.id.card_app);
        if (cardApp != null) {
            ((TextView)cardApp.findViewById(R.id.tv_icon)).setText(getFirstChar(I18nManager.get("app")));
            ((TextView)cardApp.findViewById(R.id.tv_name)).setText(I18nManager.get("app"));
        }
        View cardText = findViewById(R.id.card_text);
        if (cardText != null) {
            ((TextView)cardText.findViewById(R.id.tv_icon)).setText(getFirstChar(I18nManager.get("text")));
            ((TextView)cardText.findViewById(R.id.tv_name)).setText(I18nManager.get("text"));
        }
        View cardClip = findViewById(R.id.card_clipboard);
        if (cardClip != null) {
            ((TextView)cardClip.findViewById(R.id.tv_icon)).setText(getFirstChar(I18nManager.get("clipboard")));
            ((TextView)cardClip.findViewById(R.id.tv_name)).setText(I18nManager.get("clipboard"));
        }

        safeSetText(R.id.tv_receive_title, I18nManager.get("receive_history"));
        safeSetText(R.id.tv_set_rx, I18nManager.get("receive"));
        safeSetText(R.id.tv_auto_save, I18nManager.get("auto_save"));
        safeSetText(R.id.tv_save_album, I18nManager.get("save_album"));
        safeSetText(R.id.tv_save_history, I18nManager.get("save_history"));
        safeSetText(R.id.tv_use_pin, I18nManager.get("use_pin"));
        safeSetText(R.id.tv_net, I18nManager.get("network"));
        safeSetText(R.id.tv_web, I18nManager.get("web_enable"));
        safeSetText(R.id.tv_boot, I18nManager.get("auto_start"));
        safeSetText(R.id.tv_notif, I18nManager.get("receive_notif"));
        safeSetText(R.id.tv_port_title, I18nManager.get("port"));
        safeSetText(R.id.tv_timeout_title, I18nManager.get("timeout"));
        safeSetText(R.id.tv_other, I18nManager.get("other"));
        safeSetText(R.id.tv_lang_title, I18nManager.get("language"));
        
        String lang = getSharedPreferences("oldsend", MODE_PRIVATE).getString("app_lang", "system");
        if ("system".equals(lang)) safeSetText(R.id.tv_lang_value, I18nManager.get("follow_system"));
        else if ("zh".equals(lang)) safeSetText(R.id.tv_lang_value, "简体中文");
        else if ("zh_TW".equals(lang)) safeSetText(R.id.tv_lang_value, "繁體中文");
        else if ("en".equals(lang)) safeSetText(R.id.tv_lang_value, "English");
        else if ("ja".equals(lang)) safeSetText(R.id.tv_lang_value, "日本語");
        else if ("ko".equals(lang)) safeSetText(R.id.tv_lang_value, "한국어");
        else if ("de".equals(lang)) safeSetText(R.id.tv_lang_value, "Deutsch");
        else if ("fr".equals(lang)) safeSetText(R.id.tv_lang_value, "Français");
        else if ("es".equals(lang)) safeSetText(R.id.tv_lang_value, "Español");
        else if ("ru".equals(lang)) safeSetText(R.id.tv_lang_value, "Русский");
        else if ("pt".equals(lang)) safeSetText(R.id.tv_lang_value, "Português");

        safeSetText(R.id.tv_anim_title, I18nManager.get("anim"));
        safeSetText(R.id.tv_set_name, I18nManager.get("set_name"));
        safeSetText(R.id.tv_backup, I18nManager.get("backup"));
        safeSetText(R.id.tv_about, I18nManager.get("about"));

        if (tvPin != null) {
            if (cbPin != null && cbPin.isChecked()) {
                tvPin.setText(I18nManager.get("pin_on") + getSharedPreferences("oldsend", MODE_PRIVATE).getString("pin_code", "0000"));
            } else {
                tvPin.setText(I18nManager.get("pin_off"));
            }
        }
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
                log("Permission granted");
            } else {
                log("Permission denied");
            }
        }
    }

    private void initSettingsUI() {
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

        cbAutoSave = (CheckBox) findViewById(R.id.cb_auto_save);
        cbSaveAlbum = (CheckBox) findViewById(R.id.cb_save_album);
        cbSaveHistory = (CheckBox) findViewById(R.id.cb_save_history);
        cbPin = (CheckBox) findViewById(R.id.cb_pin);
        cbAnim = (CheckBox) findViewById(R.id.cb_anim);
        cbWeb = (CheckBox) findViewById(R.id.cb_web);
        cbBoot = (CheckBox) findViewById(R.id.cb_boot);
        cbReceiveNotif = (CheckBox) findViewById(R.id.cb_receive_notif);
        cbHttps = (CheckBox) findViewById(R.id.cb_https);
        tvPin = (TextView) findViewById(R.id.tv_pin);
        tvName = (TextView) findViewById(R.id.tv_name);
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
                    String pin = getSharedPreferences("oldsend", MODE_PRIVATE).getString("pin_code", null);
                    if (pin == null) {
                        pin = String.format("%04d", (int)(Math.random() * 10000));
                        getSharedPreferences("oldsend", MODE_PRIVATE).edit().putString("pin_code", pin).commit();
                    }
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

        View rowLang = findViewById(R.id.row_language);
        if (rowLang != null) rowLang.setOnClickListener(new View.OnClickListener() {
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

        View rowSetName = findViewById(R.id.row_set_name);
        if (rowSetName != null) rowSetName.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showSetNameDialog(); } });
        View rowAbout = findViewById(R.id.row_about);
        if (rowAbout != null) rowAbout.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showAboutDialog(); } });
        View rowBackup = findViewById(R.id.row_backup);
        if (rowBackup != null) rowBackup.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showBackupDialog(); } });
        
        View rowPort = findViewById(R.id.row_port);
        if (rowPort != null) rowPort.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Md3DialogUtils.showInput(MainActivity.this, I18nManager.get("port"), tvPort.getText().toString(), new Md3DialogUtils.OnInputListener() {
                    public void onInput(String text) {
                        try {
                            int val = Integer.parseInt(text);
                            if (val >= 1024 && val <= 65535) {
                                getSharedPreferences("oldsend", MODE_PRIVATE).edit().putInt("port", val).commit();
                                tvPort.setText(String.valueOf(val));
                            }
                        } catch (Exception e) {}
                    }
                }).show();
            }
        });

        View rowTimeout = findViewById(R.id.row_timeout);
        if (rowTimeout != null) rowTimeout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Md3DialogUtils.showInput(MainActivity.this, I18nManager.get("timeout"), tvTimeout.getText().toString(), new Md3DialogUtils.OnInputListener() {
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

    private void savePref(String key, boolean val) {
        getSharedPreferences("oldsend", MODE_PRIVATE).edit().putBoolean(key, val).commit();
    }

    private void switchTab(int index) {
        applyAnimation(pageSend, index == 0);
        applyAnimation(pageReceive, index == 1);
        applyAnimation(pageSettings, index == 2);

        if (tabSend != null) tabSend.setBackgroundResource(index == 0 ? R.drawable.bg_tab_selected : 0);
        if (tabReceive != null) tabReceive.setBackgroundResource(index == 1 ? R.drawable.bg_tab_selected : 0);
        if (tabSettings != null) tabSettings.setBackgroundResource(index == 2 ? R.drawable.bg_tab_selected : 0);

        if (tabSendText != null) tabSendText.setTextColor(index == 0 ? 0xFF2E7D32 : 0xFF49454F);
        if (tabReceiveText != null) tabReceiveText.setTextColor(index == 1 ? 0xFF2E7D32 : 0xFF49454F);
        if (tabSettingsText != null) tabSettingsText.setTextColor(index == 2 ? 0xFF2E7D32 : 0xFF49454F);

        if (index == 1) refreshHistory();
        applyI18n();
    }

    
    private void applyTabPressAnimation(final View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (!getSharedPreferences("oldsend", MODE_PRIVATE).getBoolean("use_anim", true)) {
                    return false; // 如果关了动画，就不执行
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    ScaleAnimation shrink = new ScaleAnimation(1f, 0.92f, 1f, 0.92f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    shrink.setDuration(100);
                    shrink.setFillAfter(true);
                    v.startAnimation(shrink);
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    ScaleAnimation grow = new ScaleAnimation(0.92f, 1f, 0.92f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    grow.setDuration(100);
                    v.startAnimation(grow);
                }
                return false;
            }
        });
    }

    private void applyAnimation(View view, boolean visible) {
        if (view == null) return;
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
                prepareText(text.toString());
            }
        } else {
            Toast.makeText(this, "Clipboard empty", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSendTextDialog() {
        Md3DialogUtils.showInput(this, I18nManager.get("input_text"), "", new Md3DialogUtils.OnInputListener() {
            public void onInput(String text) {
                if (text.length() > 0) prepareText(text);
            }
        }).show();
    }

    private void prepareText(String text) {
        try {
            File f = new File(getCacheDir(), "text_" + System.currentTimeMillis() + ".txt");
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(text.getBytes("UTF-8")); fos.close();
            pendingUris.clear(); pendingUris.add(Uri.fromFile(f));
            if (selectedFileText != null) selectedFileText.setText("Text prepared");
            switchTab(0);
        } catch (Exception e) { log("Prepare text failed"); }
    }

    private void pickFolder() {
        final File root = android.os.Environment.getExternalStorageDirectory();
        showFolderSelector(root);
    }

    private void showFolderSelector(final File dir) {
        final File[] files = dir.listFiles();
        if (files == null) return;

        List<String> folderNames = new ArrayList<String>();
        final List<File> folders = new ArrayList<File>();
        folderNames.add(">> [Send this folder] <<");
        
        for (File f : files) {
            if (f.isDirectory() && !f.isHidden()) {
                folderNames.add("[Folder] " + f.getName());
                folders.add(f);
            }
        }

        String[] items = folderNames.toArray(new String[0]);
        Dialog d = Md3DialogUtils.showList(this, dir.getAbsolutePath(), items, new Md3DialogUtils.OnItemSelectListener() {
            public void onItemSelect(int position) {
                if (position == 0) {
                    List<File> allFiles = new ArrayList<File>();
                    getAllFiles(dir, allFiles);
                    if (allFiles.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Empty folder", Toast.LENGTH_SHORT).show();
                    } else {
                        pendingUris.clear();
                        for (File f : allFiles) pendingUris.add(Uri.fromFile(f));
                        if (selectedFileText != null) selectedFileText.setText("Selected folder: " + dir.getName() + " (" + allFiles.size() + " files)");
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
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                apps.add(app);
                appNames.add(app.loadLabel(pm).toString());
            }
        }

        String[] items = appNames.toArray(new String[0]);
        Dialog d = Md3DialogUtils.showList(this, "Select App (APK)", items, new Md3DialogUtils.OnItemSelectListener() {
            public void onItemSelect(int position) {
                ApplicationInfo app = apps.get(position);
                String apkPath = app.sourceDir;
                pendingUris.clear();
                pendingUris.add(Uri.fromFile(new File(apkPath)));
                if (selectedFileText != null) selectedFileText.setText("App: " + app.loadLabel(getPackageManager()));
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

        logDialog = Md3DialogUtils.createBaseDialog(this, I18nManager.get("log"), scroll, I18nManager.get("close"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) { dialogLogView = null; }
        }, I18nManager.get("clear"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) { logCache.setLength(0); if (dialogLogView != null) dialogLogView.setText(""); }
        });
        logDialog.show();
    }

    private void showSetNameDialog() {
        Md3DialogUtils.showInput(this, I18nManager.get("set_name"), OldSendService.getAlias(this), new Md3DialogUtils.OnInputListener() {
            public void onInput(String text) {
                if (text.length() > 0) { 
                    OldSendService.setAlias(MainActivity.this, text); 
                    if (tvName != null) tvName.setText(text);
                }
            }
        }).show();
    }

    private void showAboutDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_about, null);
        Md3DialogUtils.showCustom(this, "", v, I18nManager.get("confirm"), null).show();
    }
    
    private void showBackupDialog() {
        final String[] items = new String[]{I18nManager.get("backup_export"), I18nManager.get("backup_import")};
        Md3DialogUtils.showList(this, I18nManager.get("backup_title"), items, new Md3DialogUtils.OnItemSelectListener() {
            public void onItemSelect(int pos) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (pos == 0) {
                    try {
                        JSONObject json = new JSONObject();
                        Map<String, ?> all = getSharedPreferences("oldsend", MODE_PRIVATE).getAll();
                        for (Map.Entry<String, ?> entry : all.entrySet()) {
                            json.put(entry.getKey(), entry.getValue());
                        }
                        cm.setPrimaryClip(ClipData.newPlainText("OldSendBackup", json.toString()));
                        Toast.makeText(MainActivity.this, I18nManager.get("export_ok"), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) { Toast.makeText(MainActivity.this, I18nManager.get("err_fail"), Toast.LENGTH_SHORT).show(); }
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
                            Toast.makeText(MainActivity.this, I18nManager.get("import_ok"), Toast.LENGTH_SHORT).show();
                            initSettingsUI();
                            applyI18n();
                        }
                    } catch (Exception e) { Toast.makeText(MainActivity.this, I18nManager.get("err_fail"), Toast.LENGTH_SHORT).show(); }
                }
            }
        }).show();
    }

    private void toggleService() {
        if (running) {
            stopService(new Intent(this, OldSendService.class));
            running = false;
        } else {
            startService(new Intent(this, OldSendService.class));
            running = true;
        }
        updateStatus();
        applyI18n();
    }

    private void updateStatus() {
        if (status != null) {
            String s = (running ? I18nManager.get("running") : I18nManager.get("stopped")) + "  IP:" + OldSendService.getLocalIp() + ":53317";
            status.setText(s);
        }
    }

    private void doScan() {
        log("Scanning...");
        MulticastScanner.scanAsync(new MulticastScanner.Callback() {
            public void onDevice(Device d) { DeviceStore.get().addDevice(d); log("Found: " + d.alias); }
            public void onFinished() { log("Scan finished"); }
        });
    }

    private void pickFile(boolean isMedia) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        if (isMedia) {
            i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*", "audio/*"});
        }
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(i, "Select"), isMedia ? REQ_MEDIA : REQ_FILE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode != REQ_FILE && requestCode != REQ_MEDIA) || resultCode != RESULT_OK || data == null) return;
        pendingUris.clear();
        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) pendingUris.add(data.getClipData().getItemAt(i).getUri());
            if (selectedFileText != null) selectedFileText.setText(count + " files selected");
        } else if (data.getData() != null) {
            pendingUris.add(data.getData());
            if (selectedFileText != null) selectedFileText.setText(queryDisplayName(data.getData()));
        }
    }

    private void doSend(final Device device, final List<File> files) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            public void run() {
                try {
                    Uploader.uploadFiles(device, files, new Uploader.ProgressCallback() {
                        public void onProgress(final String stage, final long sent, final long total) {
                            handler.post(new Runnable() { public void run() { 
                                if (progressBar != null) progressBar.setProgress(total > 0 ? (int)(sent * 100 / total) : 0); 
                            } });
                        }
                        public void onDone(boolean ok, String msg) {
                            log("[" + device.alias + "] " + (ok ? "Success" : "Failed") + ": " + msg);
                            handler.post(new Runnable() { public void run() { 
                                if (progressBar != null) progressBar.setVisibility(View.GONE); 
                            } });
                        }
                    });
                } catch (Exception e) { log("Send error"); }
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
        if (adapter != null) adapter.setData(DeviceStore.get().snapshot()); 
        if (emptyHint != null && adapter != null) emptyHint.setVisibility(adapter.getCount() == 0 ? View.VISIBLE : View.GONE);
        if (deviceList != null) setListViewHeightBasedOnChildren(deviceList);
        updateStatus(); 
    }
    
    private void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) return;
        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }
        android.view.ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    @Override
    public void showConfirmDialog(String senderAlias, int fileCount, final java.util.concurrent.CountDownLatch latch, final boolean[] result) {
        if (isFinishing()) { latch.countDown(); return; }
        Md3DialogUtils.showConfirm(this, I18nManager.get("receive"), senderAlias + " (" + fileCount + ")", I18nManager.get("confirm"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) { result[0] = true; latch.countDown(); }
        }).show();
    }

    private void refreshHistory() {
        List<HistoryStore.Record> list = HistoryStore.get().snapshot();
        if (historyAdapter != null) {
            historyAdapter.clear();
            historyPaths.clear();
            for (HistoryStore.Record r : list) {
                String sizeStr = r.size > 1024*1024 ? (r.size/1024/1024)+"MB" : (r.size/1024)+"KB";
                historyAdapter.add("[" + r.time + "] " + r.fileName + " (" + sizeStr + ")");
                historyPaths.add(r.path);
            }
            historyAdapter.notifyDataSetChanged();
        }
        if (historyList != null) setListViewHeightBasedOnChildren(historyList);
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
                if (selectedFileText != null) selectedFileText.setText(queryDisplayName(uri));
                switchTab(0);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (uris != null) {
                pendingUris.clear(); pendingUris.addAll(uris);
                if (selectedFileText != null) selectedFileText.setText(uris.size() + " files shared");
                switchTab(0);
            }
        }
    }

    @Override protected void onDestroy() { OldSendService.setSink(null); DeviceStore.get().setListener(null); ReceiveManager.setListener(null); super.onDestroy(); }
}
