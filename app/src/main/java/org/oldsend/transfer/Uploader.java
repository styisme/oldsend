package org.oldsend.transfer;

import java.io.File;
import java.util.UUID;

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

    public static void upload(final Device target, final File file, final ProgressCallback cb) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    doUpload(target, file, cb);
                } catch (Exception e) {
                    if (cb != null) cb.onDone(false, e.toString());
                }
            }
        }, "upload-" + file.getName()).start();
    }

    static void doUpload(final Device target, final File file, final ProgressCallback cb) throws Exception {
        if (target.https) {
            cb.onDone(false, "target requires HTTPS, not supported");
            return;
        }
        long size = file.length();
        if (size > Integer.MAX_VALUE) {
            cb.onDone(false, "file > 2GB not supported");
            return;
        }
        String fileId = UUID.randomUUID().toString().replace("-", "");

        JSONObject info = buildInfo();
        JSONObject fileDto = new JSONObject();
        fileDto.put("id", fileId);
        fileDto.put("fileName", file.getName());
        fileDto.put("size", size);
        fileDto.put("fileType", guessMime(file.getName()));

        JSONObject files = new JSONObject();
        files.put(fileId, fileDto);

        JSONObject prepareBody = new JSONObject();
        prepareBody.put("info", info);
        prepareBody.put("files", files);

        if (cb != null) cb.onProgress("prepare-upload", 0, size);
        String prepareUrl = target.url(Protocol.V2_PREFIX + "/prepare-upload");
        OldSendService.log("POST " + prepareUrl);
        JSONObject resp = HttpClient.postJson(prepareUrl, prepareBody, 15000);
        if (resp == null) {
            cb.onDone(false, "prepare-upload rejected");
            return;
        }
        String sessionId = resp.optString("sessionId", null);
        JSONObject respFiles = resp.optJSONObject("files");
        String token = respFiles != null ? respFiles.optString(fileId, null) : null;
        if (sessionId == null || token == null) {
            cb.onDone(false, "invalid prepare-upload response");
            return;
        }

        String uploadUrl = target.url(Protocol.V2_PREFIX + "/upload")
                + "?sessionId=" + enc(sessionId)
                + "&fileId=" + enc(fileId)
                + "&token=" + enc(token);
        OldSendService.log("POST " + uploadUrl + " (" + size + " bytes)");

        boolean ok = HttpClient.uploadFile(uploadUrl, file, new HttpClient.ProgressListener() {
            public void onProgress(final long sent, final long total) {
                if (cb != null) cb.onProgress("uploading", sent, total);
            }
        });
        if (ok) {
            cb.onDone(true, "sent " + file.getName() + " (" + size + " bytes)");
        } else {
            cb.onDone(false, "upload rejected");
        }
    }

    static JSONObject buildInfo() throws Exception {
        JSONObject o = new JSONObject();
        o.put("alias", android.os.Build.MODEL);
        o.put("version", Protocol.VERSION);
        o.put("deviceModel", "Android " + android.os.Build.VERSION.RELEASE);
        o.put("deviceType", "mobile");
        o.put("fingerprint", Identity.getFingerprint());
        o.put("port", Protocol.DEFAULT_PORT);
        o.put("protocol", "http");
        o.put("download", false);
        return o;
    }

    static String guessMime(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".mov")) return "video/quicktime";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".zip")) return "application/zip";
        if (lower.endsWith(".apk")) return "application/vnd.android.package-archive";
        return "application/octet-stream";
    }

    static String enc(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }
}
