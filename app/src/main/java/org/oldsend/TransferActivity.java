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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TransferActivity extends Activity {

    public static final String ACTION_UPDATE_PROGRESS = "org.oldsend.UPDATE_PROGRESS";
    public static final String ACTION_TRANSFER_DONE = "org.oldsend.TRANSFER_DONE";

    private LinearLayout layoutConfirm, layoutProgress;
    private TextView tvSenderName, tvSenderType, tvTitle, tvSaveDir, tvFileName, tvTotalTitle, tvSpeedInfo;
    private ProgressBar pbFile, pbTotal;
    private Button btnAccept, btnReject, btnCancel, btnDone;

    private int mode; // 0 = SEND, 1 = RECEIVE
    private Device targetDevice;
    private ArrayList<String> filePaths;
    private long totalSize = 0;
    private long startTime;
    private Handler handler = new Handler();

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
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

        layoutConfirm = (LinearLayout) findViewById(R.id.layout_confirm);
        layoutProgress = (LinearLayout) findViewById(R.id.layout_progress);
        tvSenderName = (TextView) findViewById(R.id.tv_sender_name);
        tvSenderType = (TextView) findViewById(R.id.tv_sender_type);
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

        Intent intent = getIntent();
        mode = intent.getIntExtra("mode", 0);

        if (mode == 1) {
            String senderName = intent.getStringExtra("senderName");
            tvSenderName.setText(senderName);
            layoutConfirm.setVisibility(View.VISIBLE);
            
            btnAccept.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    sendBroadcast(new Intent(ReceiveManager.ACTION_ACCEPT));
                    layoutConfirm.setVisibility(View.GONE);
                    layoutProgress.setVisibility(View.VISIBLE);
                    tvTitle.setText("正在接收文件");
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

            btnCancel.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { finish(); }
            });
            
        } else {
            layoutConfirm.setVisibility(View.GONE);
            layoutProgress.setVisibility(View.VISIBLE);
            tvTitle.setText("正在发送文件");
            
            targetDevice = (Device) intent.getSerializableExtra("device");
            filePaths = intent.getStringArrayListExtra("files");
            
            startUpload();
            
            btnCancel.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { finish(); }
            });
        }
        
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
                            // 这里的进度其实底层已经发了广播，但如果底层没发，这里可以补发
                        }
                        public void onDone(boolean ok, String msg) {
                            handler.post(new Runnable() { public void run() { transferDone(ok); } });
                        }
                    });
                } catch (Exception e) {
                    handler.post(new Runnable() { public void run() {
                        Toast.makeText(TransferActivity.this, "发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }});
                }
            }
        }).start();
    }

    private void updateProgress(long current, long total, String fileName) {
        if (total <= 0) total = 1;
        int pct = (int) (current * 100 / total);
        pbTotal.setProgress(pct);
        pbFile.setProgress(pct);
        
        long elapsed = System.currentTimeMillis() - startTime;
        double speed = current / (elapsed / 1000.0 + 0.1);
        String speedStr = speed > 1024 * 1024 ? String.format("%.1f MB/s", speed / 1024 / 1024) : String.format("%.1f KB/s", speed / 1024);
        
        tvTotalTitle.setText("总进度 (" + pct + "%)");
        tvFileName.setText(fileName);
        tvSpeedInfo.setText("大小: " + formatSize(current) + " / " + formatSize(total) + "\n速度: " + speedStr);
    }

    private void transferDone(boolean ok) {
        btnCancel.setVisibility(View.GONE);
        btnDone.setVisibility(View.VISIBLE);
        tvTotalTitle.setText(ok ? "已完成" : "已失败");
        pbTotal.setProgress(100);
        pbFile.setProgress(100);
        if (ok && mode == 1) {
            tvFileName.setText("已保存到相册/下载目录");
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
