package org.oldsend;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.oldsend.model.Device;
import org.oldsend.transfer.Uploader;
import org.oldsend.util.I18nManager;
import org.oldsend.util.ThemeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransferActivity extends Activity {

    public static final String ACTION_UPDATE_PROGRESS = "org.oldsend.UPDATE_PROGRESS";
    public static final String ACTION_TRANSFER_DONE = "org.oldsend.TRANSFER_DONE";

    private LinearLayout layoutConfirm, layoutProgress;
    private TextView tvSenderName, tvTitle, tvSaveDir, tvFileName, tvTotalTitle, tvSpeedInfo, tvFileIcon, tvFileStatus;
    private ProgressBar pbFile, pbTotal;
    private Button btnAccept, btnReject, btnCancel, btnDone;

    private int mode;
    private Device targetDevice;
    private ArrayList<String> filePaths;
    private long totalSize = 0;
    private long startTime;
    private boolean isDone = false;
    private Handler handler = new Handler();

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isDone) return;
            if (ACTION_UPDATE_PROGRESS.equals(intent.getAction())) {
                long current = intent.getLongExtra("current", 0);
                long total = intent.getLongExtra("total", 1);
                String fName = intent.getStringExtra("fileName");
                updateProgress(current, total, fName);
            } else if (ACTION_TRANSFER_DONE.equals(intent.getAction())) {
                boolean ok = intent.getBooleanExtra("ok", false);
                transferDone(ok);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);
        ThemeUtils.applyTheme(this);

        if (ThemeUtils.isDarkMode(this)) {
            findViewById(R.id.root_transfer).setBackgroundColor(0xFF121212);
        }

        layoutConfirm = (LinearLayout) findViewById(R.id.layout_confirm);
        layoutProgress = (LinearLayout) findViewById(R.id.layout_progress);
        tvSenderName = (TextView) findViewById(R.id.tv_sender_name);
        tvTitle = (TextView) findViewById(R.id.tv_title);
        tvSaveDir = (TextView) findViewById(R.id.tv_save_dir);
        tvFileIcon = (TextView) findViewById(R.id.tv_file_icon);
        tvFileName = (TextView) findViewById(R.id.tv_file_name);
        tvFileStatus = (TextView) findViewById(R.id.tv_file_status);
        tvTotalTitle = (TextView) findViewById(R.id.tv_total_title);
        tvSpeedInfo = (TextView) findViewById(R.id.tv_speed_info);
        pbFile = (ProgressBar) findViewById(R.id.pb_file);
        pbTotal = (ProgressBar) findViewById(R.id.pb_total);
        
        btnAccept = (Button) findViewById(R.id.btn_accept);
        btnReject = (Button) findViewById(R.id.btn_reject);
        btnCancel = (Button) findViewById(R.id.btn_cancel);
        btnDone = (Button) findViewById(R.id.btn_done);

        btnCancel.setText("✕ " + I18nManager.get("cancel"));
        btnDone.setText("✔ " + I18nManager.get("completed"));

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        mode = intent.getIntExtra("mode", 0);
        isDone = false;

        if (mode == 1) {
            String senderName = intent.getStringExtra("senderName");
            tvSenderName.setText(senderName != null ? senderName : "Unknown Device");
            layoutConfirm.setVisibility(View.VISIBLE);
            layoutProgress.setVisibility(View.GONE);
            
            btnAccept.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    sendBroadcast(new Intent("org.oldsend.MANUAL_ACCEPT"));
                    layoutConfirm.setVisibility(View.GONE);
                    layoutProgress.setVisibility(View.VISIBLE);
                    tvTitle.setText("正在接收文件");
                    tvSaveDir.setVisibility(View.VISIBLE);
                    tvSaveDir.setText("保存目录: /storage/emulated/0/Download/OldSend");
                    startTime = System.currentTimeMillis();
                }
            });
            
            btnReject.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    sendBroadcast(new Intent("org.oldsend.MANUAL_REJECT"));
                    finish();
                }
            });
        } else {
            layoutConfirm.setVisibility(View.GONE);
            layoutProgress.setVisibility(View.VISIBLE);
            tvTitle.setText("正在发送文件");
            
            targetDevice = (Device) intent.getSerializableExtra("device");
            filePaths = intent.getStringArrayListExtra("files");
            
            if (filePaths != null && !filePaths.isEmpty()) {
                String firstFile = new File(filePaths.get(0)).getName();
                tvFileIcon.setText(getIconForFile(firstFile));
                if (ThemeUtils.isDarkMode(this)) tvFileIcon.setBackgroundColor(0xFF2C3E35);
                else tvFileIcon.setBackgroundColor(0xFFC8E6C9);
            }
            
            startUpload();
        }
        
        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        btnDone.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
    }

    private String getIconForFile(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".flac")) return "🎵";
        if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".avi")) return "🎬";
        if (lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".jpeg")) return "🖼️";
        if (lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".7z")) return "📦";
        if (lower.endsWith(".apk")) return "🤖";
        if (lower.endsWith(".pdf")) return "📕";
        if (lower.endsWith(".txt") || lower.endsWith(".md")) return "📝";
        return "📄";
    }

    private void startUpload() {
        if (filePaths == null || filePaths.isEmpty()) return;
        
        final List<File> files = new ArrayList<File>();
        for (String path : filePaths) {
            files.add(new File(path));
            totalSize += new File(path).length();
        }
        
        startTime = System.currentTimeMillis();
        
        new Thread(new Runnable() {
            public void run() {
                try {
                    Uploader.uploadFiles(targetDevice, files, new Uploader.ProgressCallback() {
                        public void onProgress(final String stage, final long sent, final long total) {
                            handler.post(new Runnable() { public void run() {
                                if (!isDone) {
                                    long accumulated = 0;
                                    String currentName = files.get(0).getName();
                                    for (File f : files) {
                                        accumulated += f.length();
                                        if (sent <= accumulated) {
                                            currentName = f.getName();
                                            break;
                                        }
                                    }
                                    updateProgress(sent, total, currentName);
                                }
                            }});
                        }
                        public void onDone(final boolean ok, final String msg) {
                            handler.post(new Runnable() { public void run() { 
                                if (!isDone) {
                                    if (msg != null && msg.contains("PIN_REQUIRED")) {
                                        showPinDialog();
                                    } else {
                                        transferDone(ok); 
                                    }
                                }
                            }});
                        }
                    });
                } catch (Exception e) {
                    final String errMsg = e.getMessage();
                    handler.post(new Runnable() { public void run() {
                        Toast.makeText(TransferActivity.this, "Error: " + errMsg, Toast.LENGTH_SHORT).show();
                        finish();
                    }});
                }
            }
        }).start();
    }

    private void showPinDialog() {
        org.oldsend.ui.Md3DialogUtils.showInput(this, "请输入接收方 PIN 码", "", new org.oldsend.ui.Md3DialogUtils.OnInputListener() {
            public void onInput(String pin) {
                if (pin != null && pin.length() > 0) {
                    startUpload(); 
                } else {
                    finish();
                }
            }
        }).show();
    }

    private void updateProgress(long current, long total, String fileName) {
        if (isDone) return;
        long elapsed = System.currentTimeMillis() - startTime;
        double speed = current / (elapsed / 1000.0 + 0.1);
        String speedStr = speed > 1024 * 1024 ? String.format(Locale.US, "%.1f MB/s", speed / 1024 / 1024) : String.format(Locale.US, "%.1f KB/s", speed / 1024);
        
        tvFileIcon.setText(getIconForFile(fileName));
        tvFileName.setText(fileName + " (" + formatSize(total) + ")");
        
        if (total > 0) {
            int pct = (int) (current * 100 / total);
            if (pct > 100) pct = 100;
            pbTotal.setProgress(pct);
            pbFile.setProgress(pct);
            
            long remain = (long)((total - current) / (speed + 1));
            String timeStr = String.format(Locale.US, "%02d:%02d", remain / 60, remain % 60);
            
            tvTotalTitle.setText("总进度 (" + timeStr + ")");
            tvSpeedInfo.setText("文件: " + (mode==1?"0":"0") + " / 1\n大小: " + formatSize(current) + " / " + formatSize(total) + "\n速度: " + speedStr);
        }
    }

    private void transferDone(boolean ok) {
        if (isDone) return;
        isDone = true;

        try {
            android.os.Vibrator v = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) v.vibrate(100);
        } catch (Exception ignored) {}

        btnCancel.setVisibility(View.GONE);
        btnDone.setVisibility(View.VISIBLE);
        
        if (ok) {
            tvTitle.setText(mode == 1 ? "接收完成" : "发送完成");
            tvTotalTitle.setText("已完成");
            pbTotal.setProgress(100);
            pbFile.setProgress(100);
            tvFileStatus.setVisibility(View.VISIBLE);
            
            if (mode == 1) {
                tvFileStatus.setText("已保存到相册");
                tvSaveDir.setText("保存目录: /storage/emulated/0/Download/OldSend");
                tvFileStatus.setTextColor(0xFF388E3C);
            } else {
                tvFileStatus.setText("完成");
                tvFileStatus.setTextColor(0xFF388E3C);
            }
        } else {
            tvTitle.setText("传输失败");
            tvTotalTitle.setText("已中断");
            tvFileStatus.setText("失败");
            tvFileStatus.setTextColor(0xFFD32F2F);
            tvFileStatus.setVisibility(View.VISIBLE);
        }
    }

    private String formatSize(long size) {
        if (size > 1024 * 1024) return String.format(Locale.US, "%.1f MB", size / 1024.0 / 1024.0);
        return String.format(Locale.US, "%.1f KB", size / 1024.0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATE_PROGRESS);
        filter.addAction(ACTION_TRANSFER_DONE);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }
}
