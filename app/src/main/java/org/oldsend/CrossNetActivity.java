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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.oldsend.util.I18nManager;
import org.oldsend.util.ThemeUtils;

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
    private TextView tvStatus, tvPinDisplay, tvFileInfo, tvIpv6Info, tvStunInfo;
    private EditText etPin, etDirectIp;
    private CheckBox cbUseDirect;
    private LinearLayout layoutSelectedFiles, layoutDirectIp;
    private Button btnGen, btnRecv, btnStunProbe, btnUpnpPunch;
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

        // 初始化所有 UI 变量，确保不漏掉任何一个
        tvStatus = (TextView) findViewById(R.id.tv_cn_status);
        tvPinDisplay = (TextView) findViewById(R.id.tv_cn_pin_display);
        tvFileInfo = (TextView) findViewById(R.id.tv_cn_file_info);
        tvIpv6Info = (TextView) findViewById(R.id.tv_ipv6_info);
        tvStunInfo = (TextView) findViewById(R.id.tv_stun_info);
        
        etPin = (EditText) findViewById(R.id.et_cn_pin);
        etDirectIp = (EditText) findViewById(R.id.et_direct_ip);
        
        layoutSelectedFiles = (LinearLayout) findViewById(R.id.layout_cn_selected_files);
        layoutDirectIp = (LinearLayout) findViewById(R.id.layout_direct_ip);
        
        cbUseDirect = (CheckBox) findViewById(R.id.cb_use_direct);
        btnGen = (Button) findViewById(R.id.btn_cn_generate);
        btnRecv = (Button) findViewById(R.id.btn_cn_receive);
        btnStunProbe = (Button) findViewById(R.id.btn_stun_probe);
        btnUpnpPunch = (Button) findViewById(R.id.btn_upnp_punch);

        // 设置多语言
        ((TextView)findViewById(R.id.tv_cross_net_title)).setText(I18nManager.get("cross_net_title"));
        ((TextView)findViewById(R.id.tv_cross_net_desc)).setText(I18nManager.get("cross_net_desc"));
        ((TextView)findViewById(R.id.tv_send_title)).setText(I18nManager.get("send_file_title"));
        ((TextView)findViewById(R.id.tv_recv_title)).setText(I18nManager.get("recv_file_title"));
        btnGen.setText(I18nManager.get("gen_pin_btn"));
        btnRecv.setText(I18nManager.get("connect_recv_btn"));
        etPin.setHint(I18nManager.get("input_pin_hint"));
        cbUseDirect.setText(I18nManager.get("direct_mode"));
        btnUpnpPunch.setText(I18nManager.get("upnp_punch"));
        
        String ipv6 = OldSendService.getIpv6();
        if (ipv6 != null) {
            tvIpv6Info.setText(String.format(I18nManager.get("ipv6_local"), ipv6));
        } else {
            tvIpv6Info.setText(I18nManager.get("ipv6_none"));
        }

        // 绑定各种点击事件
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
                MainActivity.pendingUris = pendingUris;
                startActivity(new Intent(CrossNetActivity.this, SelectListActivity.class));
            }
        });

        cbUseDirect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                layoutDirectIp.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                if (isChecked) {
                    tvPinDisplay.setVisibility(View.GONE);
                    btnGen.setText("直连发送已选文件");
                } else {
                    btnGen.setText("生成密码并开始发送");
                }
            }
        });

        btnGen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isTransferring) return;
                if (pendingUris.isEmpty()) {
                    pendingAppend = false;
                    pickFile();
                } else {
                    if (cbUseDirect.isChecked()) {
                        String ipStr = etDirectIp.getText().toString().trim();
                        if (ipStr.isEmpty()) {
                            Toast.makeText(CrossNetActivity.this, "请输入对方公网IP和端口", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        startDirectSend(ipStr);
                    } else {
                        generatePinAndSend();
                    }
                }
            }
        });

        btnRecv.setOnClickListener(new View.OnClickListener() {
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

        btnStunProbe.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tvStunInfo.setText("STUN 探测中...");
                new Thread(new Runnable() {
                    public void run() {
                        final String result = org.oldsend.util.StunClient.getPublicAddress();
                        handler.post(new Runnable() {
                            public void run() {
                                if (result.contains("失败") || result.contains("未知")) {
                                    tvStunInfo.setText("NAT 穿透失败 (对称型NAT)");
                                    tvStunInfo.setTextColor(0xFFD32F2F);
                                } else {
                                    tvStunInfo.setText("NAT公网地址: " + result);
                                    tvStunInfo.setTextColor(0xFF388E3C);
                                    etDirectIp.setText(result);
                                }
                            }
                        });
                    }
                }).start();
            }
        });

        btnUpnpPunch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                isTransferring = true;
                tvStatus.setText("正在向路由器申请 UPnP 端口映射...");
                new Thread(new Runnable() {
                    public void run() {
                        final String result = requestUPnP();
                        handler.post(new Runnable() {
                            public void run() {
                                isTransferring = false;
                                tvStatus.setText(result);
                                if (result.contains("成功")) {
                                    cbUseDirect.setChecked(true);
                                }
                            }
                        });
                    }
                }).start();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MainActivity.pendingUris != null && MainActivity.pendingUris != pendingUris) {
            pendingUris = new ArrayList<Uri>(MainActivity.pendingUris);
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
        if (pendingUris == null || pendingUris.isEmpty()) {
            layoutSelectedFiles.setVisibility(View.GONE);
            btnGen.setText("选择文件并生成密码");
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
            btnGen.setText("生成密码并开始发送");
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
                    
                    handler.post(new Runnable() { public void run() { tvStatus.setText("打包完成，正在建立极客通道..."); } });
                    
                    FileInputStream fis = new FileInputStream(tempZip);
                    byte[] chunk = new byte[3000];
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
                        OutputStream os = c.getOutputStream();
                        os.write(b64.getBytes("UTF-8"));
                        os.flush();
                        os.close();
                        
                        totalSent += n;
                        final long finalSent = totalSent;
                        handler.post(new Runnable() { public void run() { tvStatus.setText("正在发送... 已发送 " + (finalSent/1024) + " KB"); } });
                        Thread.sleep(100);
                    }
                    fis.close();
                    
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

    private void startDirectSend(String ipStr) {
        String cleanIp = ipStr.trim();
        int port = 53317;
        
        // 如果用户点了自己头像，或者填了本机 IP，直接拦截
        if (cleanIp.contains(OldSendService.getLocalIp())) {
            tvStatus.setText("直连失败: 无法通过公网IP直连本机，请使用另一台设备。");
            return;
        }

        // 暴力清洗：去除所有可能存在的方括号
        cleanIp = cleanIp.replace("[", "").replace("]", "");

        // 检查是否带端口 (如果是 IPv6，最后一个冒号后面可能跟着端口)
        int lastColon = cleanIp.lastIndexOf(':');
        if (lastColon != -1) {
            String possiblePort = cleanIp.substring(lastColon + 1);
            // 如果最后一段是纯数字，且长度小于 6 位，说明是端口
            if (possiblePort.matches("\\d{1,5}")) {
                try {
                    port = Integer.parseInt(possiblePort);
                    cleanIp = cleanIp.substring(0, lastColon); // 剥离端口
                } catch (Exception ignored) {}
            }
        }
        
        // 最终 cleanIp 是纯净的 IP（绝对无括号），port 是合法数字
        final String ip = cleanIp;
        final int finalPort = port;
        
        final org.oldsend.model.Device target = new org.oldsend.model.Device();
        target.alias = "公网设备";
        target.ip = ip;
        target.port = finalPort;
        target.https = false;

        isTransferring = true;
        tvStatus.setText("正在读取文件并准备直连...");

        new Thread(new Runnable() {
            public void run() {
                final List<File> files = new ArrayList<File>();
                try {
                    for (Uri uri : pendingUris) {
                        File out = new File(getCacheDir(), queryDisplayName(uri));
                        InputStream is = getContentResolver().openInputStream(uri);
                        FileOutputStream fos = new FileOutputStream(out);
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
                        is.close(); fos.close();
                        files.add(out);
                    }
                } catch (Exception e) {
                    handler.post(new Runnable() { public void run() {
                        Toast.makeText(CrossNetActivity.this, "读取文件失败", Toast.LENGTH_SHORT).show();
                        isTransferring = false;
                        tvStatus.setText("准备失败");
                    }});
                    return;
                }

                handler.post(new Runnable() { public void run() {
                    tvStatus.setText("正在通过公网直连发送...");
                }});

                org.oldsend.transfer.Uploader.uploadFiles(target, files, new org.oldsend.transfer.Uploader.ProgressCallback() {
                    public void onProgress(final String stage, final long sent, final long total) {
                        handler.post(new Runnable() { public void run() {
                            if (total > 0) {
                                int pct = (int)(sent * 100 / total);
                                tvStatus.setText("极速发送中... " + pct + "% (" + (sent/1024) + "/" + (total/1024) + " KB)");
                            }
                        }});
                    }
                    public void onDone(final boolean ok, final String msg) {
                        handler.post(new Runnable() { public void run() {
                            isTransferring = false;
                            tvStatus.setText(ok ? "公网极速直连发送完成！" : "直连失败: " + msg);
                        }});
                    }
                });
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
                        InputStream is = c.getInputStream();
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
        String name = null; Cursor c = null;
        try { c = getContentResolver().query(uri, new String[] { "_display_name" }, null, null, null); if (c != null && c.moveToFirst()) name = c.getString(0); } catch (Exception ignored) {} finally { if (c != null) try { c.close(); } catch (Exception ignored) {} }
        if (name == null) name = uri.getLastPathSegment();
        return name == null ? "file" : name;
    }

    // 极其硬核的纯手写 UPnP SSDP 搜索与端口映射
    private String requestUPnP() {
        try {
            java.net.DatagramSocket ssdpSock = new java.net.DatagramSocket();
            ssdpSock.setSoTimeout(2000);
            String mSearch = "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 3\r\n" +
                    "ST: urn:schemas-upnp-org:device:InternetGatewayDevice:1\r\n\r\n";
            byte[] data = mSearch.getBytes("UTF-8");
            java.net.DatagramPacket packet = new java.net.DatagramPacket(data, data.length,
                    java.net.InetAddress.getByName("239.255.255.250"), 1900);
            ssdpSock.send(packet);

            byte[] buf = new byte[2048];
            java.net.DatagramPacket recv = new java.net.DatagramPacket(buf, buf.length);
            String location = null;
            try {
                ssdpSock.receive(recv);
                String response = new String(recv.getData(), 0, recv.getLength(), "UTF-8");
                for (String line : response.split("\r\n")) {
                    if (line.toUpperCase().startsWith("LOCATION:")) {
                        location = line.substring(9).trim();
                        break;
                    }
                }
            } catch (Exception e) {
                return "获取失败：路由器可能未开启 UPnP。";
            }
            ssdpSock.close();

            if (location == null) return "获取失败：未找到支持 UPnP 的路由器。";

            java.net.URL url = new java.net.URL(location);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            InputStream in = conn.getInputStream();
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in));
            String line;
            String controlUrl = null;
            String serviceType = null;
            
            while ((line = reader.readLine()) != null) {
                if (line.contains("<controlURL>") && line.contains("WANIPConnection")) {
                    int start = line.indexOf("<controlURL>") + 12;
                    int end = line.indexOf("</controlURL>");
                    if (start > 12 && end > start) {
                        controlUrl = line.substring(start, end);
                    }
                }
                if (line.contains("<serviceType>") && line.contains("WANIPConnection")) {
                    int start = line.indexOf("<serviceType>") + 13;
                    int end = line.indexOf("</serviceType>");
                    if (start > 13 && end > start) {
                        serviceType = line.substring(start, end);
                    }
                }
            }
            reader.close();

            if (controlUrl == null || serviceType == null) {
                return "映射失败：路由器 UPnP 接口异常。";
            }
            
            if (!controlUrl.startsWith("http")) {
                java.net.URL base = new java.net.URL(location);
                controlUrl = base.getProtocol() + "://" + base.getHost() + (base.getPort() != -1 ? ":" + base.getPort() : "") + (controlUrl.startsWith("/") ? "" : "/") + controlUrl;
            }

            String localIp = OldSendService.getLocalIp();
            String soapBody = "<?xml version=\"1.0\"?>\n" +
                    "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                    "<s:Body>\n" +
                    "<u:AddPortMapping xmlns:u=\"" + serviceType + "\">\n" +
                    "<NewRemoteHost></NewRemoteHost>\n" +
                    "<NewExternalPort>53317</NewExternalPort>\n" +
                    "<NewProtocol>TCP</NewProtocol>\n" +
                    "<NewInternalPort>53317</NewInternalPort>\n" +
                    "<NewInternalClient>" + localIp + "</NewInternalClient>\n" +
                    "<NewEnabled>1</NewEnabled>\n" +
                    "<NewPortMappingDescription>OldSend</NewPortMappingDescription>\n" +
                    "<NewLeaseDuration>0</NewLeaseDuration>\n" +
                    "</u:AddPortMapping>\n" +
                    "</s:Body>\n" +
                    "</s:Envelope>";

            java.net.HttpURLConnection soapConn = (java.net.HttpURLConnection) new java.net.URL(controlUrl).openConnection();
            soapConn.setRequestMethod("POST");
            soapConn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
            soapConn.setRequestProperty("SOAPAction", "\"" + serviceType + "#AddPortMapping\"");
            soapConn.setDoOutput(true);
            soapConn.getOutputStream().write(soapBody.getBytes("UTF-8"));
            
            int code = soapConn.getResponseCode();
            if (code == 200) {
                String pubIp = getPublicIP();
                return "穿透成功！你的公网地址是: " + pubIp + ":53317\n请让对方填入此地址。";
            } else {
                return "穿透失败：路由器拒绝映射请求(" + code + ")。";
            }
        } catch (Exception e) {
            return "穿透异常: " + e.getMessage();
        }
    }

    private String getPublicIP() {
        try {
            java.net.URL url = new java.net.URL("https://api.ipify.org");
            java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(url.openStream()));
            String ip = in.readLine();
            in.close();
            return ip;
        } catch (Exception e) {
            return "未知公网IP";
        }
    }
}
