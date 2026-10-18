package org.oldsend;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.oldsend.util.I18nManager;
import org.oldsend.util.ThemeUtils;
import org.oldsend.ui.Md3DialogUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import android.util.Base64;

public class CrossNetActivity extends Activity {
    private static final String NTFY_URL = "https://ntfy.sh/oldsend_cross_";
    private TextView tvStatus, tvPinDisplay, tvFileInfo;
    private EditText etPin;
    private LinearLayout layoutSelectedFiles;
    private Handler handler = new Handler();
    private boolean isTransferring = false;
    private List<Uri> pendingUris = new ArrayList<Uri>();
    private boolean pendingAppend = false;
    private String currentPin = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cross_net);
        ThemeUtils.applyTheme(this);

        if (ThemeUtils.isDarkMode(this)) {
            findViewById(R.id.root_cross_net).setBackgroundColor(0xFF121212);
            ((TextView)findViewById(R.id.btn_cn_back)).setTextColor(0xFFE0E0E0);
        }

        tvStatus = (TextView) findViewById(R.id.tv_cn_status);
        tvPinDisplay = (TextView) findViewById(R.id.tv_cn_pin_display);
        tvFileInfo = (TextView) findViewById(R.id.tv_cn_file_info);
        etPin = (EditText) findViewById(R.id.et_cn_pin);
        layoutSelectedFiles = (LinearLayout) findViewById(R.id.layout_cn_selected_files);

        findViewById(R.id.btn_cn_back).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        findViewById(R.id.btn_cn_add_files).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pendingAppend = true;
                pickFile();
            }
        });

        findViewById(R.id.btn_cn_edit_files).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // 通过全局静态变量把数据传给管理页
                MainActivity.pendingUris = pendingUris;
                startActivity(new Intent(CrossNetActivity.this, SelectListActivity.class));
            }
        });

        findViewById(R.id.btn_cn_generate).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isTransferring) return;
                if (pendingUris.isEmpty()) {
                    pendingAppend = false;
                    pickFile();
                } else {
                    generatePinAndSend();
                }
            }
        });

        findViewById(R.id.btn_cn_receive).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isTransferring) return;
                String pin = etPin.getText().toString().trim();
                if (pin.length() != 6) {
                    Toast.makeText(CrossNetActivity.this, "请输入6位纯数字密码", Toast.LENGTH_SHORT).show();
                    return;
                }
                startReceive(pin);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pendingUris != MainActivity.pendingUris && MainActivity.pendingUris != null) {
            pendingUris = MainActivity.pendingUris; // 同步从管理页修改后的数据
        }
        updateUI();
    }

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(i, "选择文件"), 2001);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2001 && resultCode == RESULT_OK && data != null) {
            if (!pendingAppend) pendingUris.clear();
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) pendingUris.add(data.getClipData().getItemAt(i).getUri());
            } else if (data.getData() != null) {
                pendingUris.add(data.getData());
            }
            updateUI();
        }
    }

    private void updateUI() {
        if (pendingUris.isEmpty()) {
            layoutSelectedFiles.setVisibility(View.GONE);
            findViewById(R.id.btn_cn_generate).setText("选择文件并生成 6 位密码");
        } else {
            layoutSelectedFiles.setVisibility(View.VISIBLE);
            long totalSize = 0;
            for (Uri u : pendingUris) {
                try {
                    Cursor cursor = getContentResolver().query(u, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                        if (sizeIndex != -1) totalSize += cursor.getLong(sizeIndex);
                        cursor.close();
                    }
                } catch (Exception ignored) {}
            }
            String sizeStr = totalSize > 1024*1024 ? String.format("%.1f MB", totalSize/1024.0/1024.0) : String.format("%.1f KB", totalSize/1024.0);
            tvFileInfo.setText("已选择 " + pendingUris.size() + " 个文件, 总计 " + sizeStr);
            findViewById(R.id.btn_cn_generate).setText("生成密码并开始发送");
        }
    }

    private void generatePinAndSend() {
        int pin = 100000 + new Random().nextInt(900000);
        currentPin = String.valueOf(pin);
        tvPinDisplay.setText("密码: " + currentPin);
        tvPinDisplay.setVisibility(View.VISIBLE);
        startSend(currentPin);
    }

    private void startSend(final String pin) {
        isTransferring = true;
        tvStatus.setText("正在打包文件...");
        
        new Thread(new Runnable() {
            public void run() {
                try {
                    // 1. 将所有文件打包到临时 ZIP
                    File tempZip = new File(getCacheDir(), "crossnet_" + pin + ".zip");
                    FileOutputStream fos = new FileOutputStream(tempZip);
                    ZipOutputStream zos = new ZipOutputStream(fos);
                    byte[] buf = new byte[8192];
                    
                    for (Uri uri : pendingUris) {
                        InputStream is = getContentResolver().openInputStream(uri);
                        String name = queryDisplayName(uri);
                        zos.putNextEntry(new ZipEntry(name));
                        int n;
                        while ((n = is.read(buf)) > 0) zos.write(buf, 0, n);
                        is.close();
                        zos.closeEntry();
                    }
                    zos.close();
                    fos.close();
                    
                    // 2. 读取 ZIP，转 Base64 切片发送
                    handler.post(new Runnable() { public void run() { tvStatus.setText("打包完成，正在建立极客通道..."); } });
                    
                    FileInputStream fis = new FileInputStream(tempZip);
                    byte[] chunk = new byte[3000]; // 留点余量给 Base64 和 JSON
                    int n;
                    long totalSent = 0;
                    
                    while ((n = fis.read(chunk)) > 0) {
                        byte[] actualChunk = new byte[n];
                        System.arraycopy(chunk, 0, actualChunk, 0, n);
                        String b64 = Base64.encodeToString(actualChunk, Base64.NO_WRAP);
                        
                        java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(NTFY_URL + pin).openConnection();
                        c.setRequestMethod("POST");
                        c.setDoOutput(true);
                        c.setRequestProperty("Content-Type", "text/plain");
                        java.io.OutputStream os = c.getOutputStream();
                        os.write(b64.getBytes("UTF-8"));
                        os.flush();
                        os.close();
                        
                        totalSent += n;
                        final long finalSent = totalSent;
                        handler.post(new Runnable() { public void run() { tvStatus.setText("正在发送... 已发送 " + (finalSent/1024) + " KB"); } });
                        Thread.sleep(100); // 限速防封
                    }
                    fis.close();
                    
                    // 发送结束标记
                    java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(NTFY_URL + pin).openConnection();
                    c.setRequestMethod("POST");
                    c.setDoOutput(true);
                    c.getOutputStream().write("__END__".getBytes("UTF-8"));
                    
                    tempZip.delete();
                    
                    handler.post(new Runnable() {
                        public void run() { 
                            tvStatus.setText("发送完成！等待对方接收完毕。"); 
                            isTransferring = false; 
                        }
                    });
                } catch (final Exception e) {
                    handler.post(new Runnable() {
                        public void run() { tvStatus.setText("发送失败: " + e.getMessage()); isTransferring = false; }
                    });
                }
            }
        }).start();
    }

    private void startReceive(final String pin) {
        isTransferring = true;
        tvStatus.setText("正在建立连接...");
        
        new Thread(new Runnable() {
            public void run() {
                try {
                    String pollUrl = NTFY_URL + pin + "/json?poll=1";
                    File tempZip = new File(getCacheDir(), "recv_crossnet_" + pin + ".zip");
                    FileOutputStream fos = new FileOutputStream(tempZip);
                    
                    boolean finished = false;
                    long lastTime = 0;
                    
                    while (!finished) {
                        java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(pollUrl).openConnection();
                        c.setConnectTimeout(15000);
                        c.setReadTimeout(15000);
                        java.io.InputStream is = c.getInputStream();
                        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, "UTF-8"));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.trim().length() > 0) {
                                org.json.JSONObject o = new org.json.JSONObject(line);
                                long time = o.optLong("time", 0);
                                if (time > lastTime) {
                                    String msg = o.optString("message", "");
                                    if ("__END__".equals(msg)) {
                                        finished = true;
                                        break;
                                    }
                                    if (!msg.isEmpty()) {
                                        byte[] data = Base64.decode(msg, Base64.NO_WRAP);
                                        fos.write(data);
                                        final long size = fos.getChannel().size();
                                        handler.post(new Runnable() { public void run() { tvStatus.setText("接收中... 已接收 " + (size/1024) + " KB"); } });
                                    }
                                    lastTime = time;
                                }
                            }
                        }
                        reader.close(); is.close();
                        if (!finished) Thread.sleep(1000);
                    }
                    fos.close();
                    
                    // 解压 ZIP 到 Download 目录
                    handler.post(new Runnable() { public void run() { tvStatus.setText("接收完成，正在解压..."); } });
                    File downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                    ZipInputStream zis = new ZipInputStream(new FileInputStream(tempZip));
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        File outFile = new File(downloadDir, entry.getName());
                        FileOutputStream out = new FileOutputStream(outFile);
                        byte[] b = new byte[8192];
                        int len;
                        while ((len = zis.read(b)) > 0) out.write(b, 0, len);
                        out.close();
                    }
                    zis.close();
                    tempZip.delete();
                    
                    handler.post(new Runnable() {
                        public void run() { 
                            tvStatus.setText("接收完成！已解压到 Download 目录"); 
                            isTransferring = false; 
                        }
                    });
                } catch (final Exception e) {
                    handler.post(new Runnable() {
                        public void run() { tvStatus.setText("接收失败: " + e.getMessage()); isTransferring = false; }
                    });
                }
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
}