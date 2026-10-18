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

public class TransferActivity extends Activity {

    public static final String ACTION_UPDATE_PROGRESS = "org.oldsend.UPDATE_PROGRESS";
    public static final String ACTION_TRANSFER_DONE = "org.oldsend.TRANSFER_DONE";

    private LinearLayout layoutConfirm, layoutProgress;
    private TextView tvSenderName, tvTitle, tvSaveDir, tvFileName, tvTotalTitle, tvSpeedInfo;
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

        layoutConfirm = (LinearLayout) findViewById(R.id.layout_confirm);
        layoutProgress = (LinearLayout) findViewById(R.id.layout_progress);
        tvSenderName = (TextView) findViewById(R.id.tv_sender_name);
        tvTitle = (TextView) findViewById(R.id.tv_title);
        tvSaveDir = (TextView) findViewById(R.id.tv_save_dir);
        tvFileName = (TextView) findViewById(R.id.tv_file_name);
        tvTotalTitle = (TextView) findViewById(R.id.tv_total_title);
        tvSpeedInfo = (TextView) findViewById(R.id.tv_speed_info);
        pbFile = (ProgressBar) findViewById(R.id.pb_file);
        pbTotal = (ProgressBar) findViewById(R.id.pb_total);
        
        btnAccept = (Button) findViewById(R.id.btn_accept);
        btnReject = (Button) findViewById(R.id.btn_reject);
        btnCancel = (Button) findViewById(R.id.btn_cancel);
        btnDone = (Button) findViewById(R.id.btn_done);

        btnCancel.setText(I18nManager.get("cancel")); // 确保按钮文字跟随系统

        Intent intent = getIntent();
        handleIntent(intent);
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
            tvSenderName.setText(senderName);
            layoutConfirm.setVisibility(View.VISIBLE);
            layoutProgress.setVisibility(View.GONE);
            
            btnAccept.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    sendBroadcast(new Intent(ReceiveManager.ACTION_ACCEPT));
                    layoutConfirm.setVisibility(View.GONE);
                    layoutProgress.setVisibility(View.VISIBLE);
                    tvTitle.setText(I18nManager.get("receiving_title"));
                    tvSaveDir.setVisibility(View.VISIBLE);
                    startTime = System.currentTimeMillis();
                }
            });
            
            btnReject.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    sendBroadcast(new Intent(ReceiveManager.ACTION_REJECT));
                    finish();
                }
            });
        } else {
            layoutConfirm.setVisibility(View.GONE);
            layoutProgress.setVisibility(View.VISIBLE);
            tvTitle.setText(I18nManager.get("sending_title"));
            
            targetDevice = (Device) intent.getSerializableExtra("device");
            filePaths = intent.getStringArrayListExtra("files");
            startUpload();
        }
        
        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        btnDone.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
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
                                if (!isDone) updateProgress(sent, total, files.get(0).getName());
                            }});
                        }
                        public void onDone(final boolean ok, String msg) {
                            handler.post(new Runnable() { public void run() { 
                                if (!isDone) transferDone(ok); 
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

    private void updateProgress(long current, long total, String fileName) {
        long elapsed = System.currentTimeMillis() - startTime;
        double speed = current / (elapsed / 1000.0 + 0.1);
        String speedStr = speed > 1024 * 1024 ? String.format("%.1f MB/s", speed / 1024 / 1024) : String.format("%.1f KB/s", speed / 1024);
        
        tvFileName.setText(fileName);
        
        String sizeLabel = I18nManager.get("size");
        String speedLabel = I18nManager.get("speed");
        String progressLabel = I18nManager.get("total_progress");
        String receivedLabel = I18nManager.get("received");

        if (total > 0) {
            int pct = (int) (current * 100 / total);
            if (pct > 100) pct = 100;
            pbTotal.setProgress(pct);
            pbFile.setProgress(pct);
            tvTotalTitle.setText(progressLabel + " (" + pct + "%)");
            tvSpeedInfo.setText(sizeLabel + ": " + formatSize(current) + " / " + formatSize(total) + "\n" + speedLabel + ": " + speedStr);
        } else {
            pbTotal.setProgress(0);
            pbFile.setProgress(0);
            tvTotalTitle.setText(progressLabel + "...");
            tvSpeedInfo.setText(receivedLabel + ": " + formatSize(current) + "\n" + speedLabel + ": " + speedStr);
        }
    }

    private void transferDone(boolean ok) {
        if (isDone) return;
        isDone = true;

        btnCancel.setVisibility(View.GONE);
        btnDone.setVisibility(View.VISIBLE);
        
        if (ok) {
            tvTitle.setText(I18nManager.get("completed"));
            tvTotalTitle.setText(I18nManager.get("completed"));
            pbTotal.setProgress(100);
            pbFile.setProgress(100);
            if (mode == 1) {
                tvFileName.setText(I18nManager.get("save_to_dir"));
            }
        } else {
            tvTitle.setText(I18nManager.get("failed"));
            tvTotalTitle.setText(I18nManager.get("failed"));
            pbTotal.setProgress(0);
            pbFile.setProgress(0);
        }
    }

    private String formatSize(long size) {
        if (size > 1024 * 1024) return String.format("%.1f MB", size / 1024.0 / 1024.0);
        return String.format("%.1f KB", size / 1024.0);
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
