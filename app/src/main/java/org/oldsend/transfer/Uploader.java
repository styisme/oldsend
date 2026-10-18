package org.oldsend.transfer;

import java.io.File;
import android.content.Intent;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.json.JSONObject;
import org.oldsend.OldSendService;
import org.oldsend.Protocol;
import org.oldsend.http.HttpClient;
import org.oldsend.model.Device;
import org.oldsend.protocol.Identity;

public class Uploader {
    public interface ProgressCallback {
        void onProgress(String stage, long sent, long total);
        void onDone(boolean ok, String msg);
    }

    public static void uploadFiles(final Device target, final List<File> files, final ProgressCallback cb) {
        uploadFiles(target, files, null, cb);
    }

    public static void uploadFiles(final Device target, final List<File> files, final String pin, final ProgressCallback cb) {
        new Thread(new Runnable() {
            public void run() {
                try { doUpload(target, files, pin, cb); } catch (Exception e) { if (cb != null) cb.onDone(false, e.toString()); }
            }
        }).start();
    }

    static void doUpload(Device target, List<File> files, String pin, ProgressCallback cb) throws Exception {
        if (target.https) { cb.onDone(false, "目标开启了HTTPS，请在对方关闭加密"); return; }
        if (files.isEmpty()) return;

        JSONObject info = buildInfo();
        JSONObject filesJson = new JSONObject();
        long totalSize = 0;
        String[] fileIds = new String[files.size()];

        for (int i = 0; i < files.size(); i++) {
            File f = files.get(i);
            String id = UUID.randomUUID().toString().replace("-", "");
            fileIds[i] = id;
            totalSize += f.length();
            JSONObject fileDto = new JSONObject();
            fileDto.put("id", id); fileDto.put("fileName", f.getName());
            fileDto.put("size", f.length()); fileDto.put("fileType", guessMime(f.getName()));
            filesJson.put(id, fileDto);
        }

        JSONObject prepareBody = new JSONObject();
        prepareBody.put("info", info); prepareBody.put("files", filesJson);

        cb.onProgress("准备上传", 0, totalSize);
        String prepareUrl = target.url(Protocol.V2_PREFIX + "/prepare-upload");
        if (pin != null && pin.length() > 0) prepareUrl += "?pin=" + pin;
        JSONObject resp = HttpClient.postJson(prepareUrl, prepareBody, 15000);
        if (resp == null) { cb.onDone(false, "对方拒绝接收"); return; }
        String sessionId = resp.optString("sessionId", null);
        JSONObject respFiles = resp.optJSONObject("files");
        if (sessionId == null || respFiles == null) { cb.onDone(false, "响应无效"); return; }

        final AtomicLong totalSent = new AtomicLong(0);
        ExecutorService pool = Executors.newFixedThreadPool(3); // 3线程并发
        final boolean[] hasError = new boolean[]{false};
        
        for (int i = 0; i < files.size(); i++) {
            final File f = files.get(i);
            final String fileId = fileIds[i];
            final String token = respFiles.optString(fileId, null);
            if (token == null) continue;
            final String uploadUrl = target.url(Protocol.V2_PREFIX + "/upload") + "?sessionId=" + enc(sessionId) + "&fileId=" + enc(fileId) + "&token=" + enc(token);
            
            pool.execute(new Runnable() {
                public void run() {
                    try {
                        long size = f.length();
                        java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(uploadUrl).openConnection();
                        c.setConnectTimeout(15000); c.setReadTimeout(300000);
                        c.setRequestMethod("POST"); c.setDoOutput(true);
                        c.setRequestProperty("Content-Type", "application/octet-stream");
                        c.setFixedLengthStreamingMode((int) size);
                        java.io.OutputStream os = c.getOutputStream();
                        java.io.FileInputStream fis = new java.io.FileInputStream(f);
                        byte[] buf = new byte[65536];
                        int n;
                        while ((n = fis.read(buf)) > 0) {
                            os.write(buf, 0, n);
                            long current = totalSent.addAndGet(n);
                            if (cb != null) cb.onProgress("发送中", current, totalSize);
                        }
                        os.flush(); fis.close(); os.close();
                        int code = c.getResponseCode();
                        if (code < 200 || code >= 300) hasError[0] = true;
                        c.disconnect();
                    } catch (Exception e) {
                        hasError[0] = true;
                    }
                }
            });
        }
        
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.HOURS);
        
        if (hasError[0]) {
            cb.onDone(false, "部分文件上传失败");
        } else {
            cb.onDone(true, "发送成功");
            OldSendService.getContext().sendBroadcast(new android.content.Intent("org.oldsend.TRANSFER_DONE").putExtra("ok", true));
        }
            OldSendService.getContext().sendBroadcast(new Intent("org.oldsend.TRANSFER_DONE").putExtra("ok", true));
    }

    static boolean uploadFile(String url, File file, final long baseSent, final long totalSize, final ProgressCallback cb) throws Exception {
        long size = file.length();
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000); c.setReadTimeout(300000);
        c.setRequestMethod("POST"); c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/octet-stream");
        c.setFixedLengthStreamingMode((int) size);
        try {
            OutputStream os = c.getOutputStream();
            FileInputStream fis = new FileInputStream(file);
            byte[] buf = new byte[65536];
            int n;
            long fileSent = 0;
            while ((n = fis.read(buf)) > 0) {
                os.write(buf, 0, n); fileSent += n;
                if (cb != null) cb.onProgress("发送中", baseSent + fileSent, totalSize);
            }
            os.flush(); fis.close(); os.close();
        } catch (Exception e) { c.disconnect(); throw e; }
        try { return c.getResponseCode() >= 200 && c.getResponseCode() < 300; }
        finally { c.disconnect(); }
    }

    static JSONObject buildInfo() throws Exception {
        JSONObject o = new JSONObject();
        o.put("alias", OldSendService.getAlias(null));
        o.put("version", Protocol.VERSION); o.put("deviceModel", "Android " + android.os.Build.VERSION.RELEASE);
        o.put("deviceType", "mobile"); o.put("fingerprint", Identity.getFingerprint());
        o.put("port", Protocol.DEFAULT_PORT); o.put("protocol", "http"); o.put("download", false);
        return o;
    }

    static String guessMime(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".mp4")) return "video/mp4";
        return "application/octet-stream";
    }

    static String enc(String s) { try { return java.net.URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { return s; } }
}
