package org.oldsend.http;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Security;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;

import org.json.JSONObject;

public class HttpClient {

    static {
        try {
            if (android.os.Build.VERSION.SDK_INT < 24) {
                try {
                    Security.insertProviderAt(new org.conscrypt.OpenSSLProvider(), 1);
                } catch (Throwable ignored) {}
            }
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) { return true; }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public interface ProgressListener {
        void onProgress(long sent, long total);
    }

    public static JSONObject getJson(String url, int timeoutMs) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(timeoutMs);
        c.setReadTimeout(timeoutMs);
        c.setRequestMethod("GET");
        try {
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) return null;
            InputStream in = c.getInputStream();
            String s = readAll(in);
            in.close();
            if (s == null || s.length() == 0) return new JSONObject();
            return new JSONObject(s);
        } finally {
            c.disconnect();
        }
    }

    public static JSONObject postJson(String url, JSONObject body, int timeoutMs) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(timeoutMs);
        c.setReadTimeout(timeoutMs);
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        byte[] data = body.toString().getBytes("UTF-8");
        c.setFixedLengthStreamingMode(data.length);
        try {
            OutputStream os = c.getOutputStream();
            os.write(data);
            os.flush();
            os.close();
        } catch (IOException e) {
            c.disconnect();
            throw e;
        }
        try {
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) return null;
            InputStream in = c.getInputStream();
            String s = readAll(in);
            in.close();
            if (s == null || s.length() == 0) return new JSONObject();
            return new JSONObject(s);
        } finally {
            c.disconnect();
        }
    }

    public static boolean uploadFile(String url, File file, ProgressListener listener) throws Exception {
        long size = file.length();
        if (size > Integer.MAX_VALUE) throw new IOException("file too large for API 10 (>2GB)");

        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(300000);
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/octet-stream");
        c.setFixedLengthStreamingMode((int) size);

        try {
            OutputStream os = c.getOutputStream();
            FileInputStream fis = new FileInputStream(file);
            byte[] buf = new byte[65536];
            long sent = 0;
            try {
                int n;
                while ((n = fis.read(buf)) > 0) {
                    os.write(buf, 0, n);
                    sent += n;
                    if (listener != null) listener.onProgress(sent, size);
                }
                os.flush();
            } finally {
                try { fis.close(); } catch (Exception ignored) {}
                try { os.close(); } catch (Exception ignored) {}
            }
        } catch (IOException e) {
            c.disconnect();
            throw e;
        }

        try {
            int code = c.getResponseCode();
            return code >= 200 && code < 300;
        } finally {
            c.disconnect();
        }
    }

    public static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        return new String(bo.toByteArray(), "UTF-8");
    }
}
