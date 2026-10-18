package org.oldsend.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.io.InputStream;
import java.util.Map;

import org.json.JSONObject;
import org.oldsend.OldSendService;
import org.oldsend.http.HttpRequest;
import org.oldsend.http.HttpResponse;
import org.oldsend.http.Router;
import org.oldsend.transfer.FileSaver;
import org.oldsend.model.HistoryStore;

public class UploadHandler implements Router.Handler {
    private final SessionStore store;

    public UploadHandler(SessionStore store) { this.store = store; }

    public HttpResponse handle(HttpRequest req) throws Exception {
        if (!"POST".equalsIgnoreCase(req.method)) return HttpResponse.status(405);
        Map<String, String> q = req.getQueryParams();
        String sessionId = q.get("sessionId");
        String fileId = q.get("fileId");
        String token = q.get("token");

        if (sessionId == null || fileId == null || token == null) return HttpResponse.status(400);
        UploadSession session = store.get(sessionId);
        if (session == null) return HttpResponse.status(400);
        UploadSession.FileEntry entry = session.getFile(fileId);
        if (entry == null || !entry.token.equals(token)) return HttpResponse.status(403);

        boolean saveAlbum = OldSendService.getPrefBooleanGlobal("save_album", true);
        File target = FileSaver.createTarget(entry.fileName, saveAlbum);
        OldSendService.log("上传: " + entry.fileName + " -> " + target.getAbsolutePath());

        long written = 0;
        long existingSize = 0;
        if (target.exists()) existingSize = target.length();
        
        // 如果存在并且大小小于总大小，则断点续传（追加模式）
        FileOutputStream fos = new FileOutputStream(target, existingSize > 0);
        try {
            if (existingSize > 0) {
                OldSendService.log("断点续传追加: 已存在 " + existingSize + " 字节");
                written = existingSize;
                if (req.body != null) req.body.skip(existingSize); // 跳过已接收的部分
            }
            InputStream in = req.body;
            byte[] buf = new byte[65536];
            long remaining = req.contentLength > 0 ? req.contentLength : entry.size;
            if (remaining <= 0) remaining = Long.MAX_VALUE;
            while (remaining > 0) {
                int max = (int) Math.min(buf.length, remaining);
                int n = in.read(buf, 0, max);
                if (n < 0) break;
                fos.write(buf, 0, n);
                written += n;
                android.content.Intent intent = new android.content.Intent("org.oldsend.UPDATE_PROGRESS");
                intent.putExtra("current", written);
                intent.putExtra("total", req.contentLength);
                intent.putExtra("fileName", entry.fileName);
                OldSendService.getContext().sendBroadcast(intent);
                remaining -= n;
            }
        } finally { try { fos.close(); } catch (Exception ignored) {} }

        OldSendService.log("接收完成: " + entry.fileName + " (" + written + " 字节)");
        OldSendService.getContext().sendBroadcast(new android.content.Intent("org.oldsend.TRANSFER_DONE").putExtra("ok", true));
        
        // 记录到历史
        String timeStr = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date());
        boolean saveHistory = OldSendService.getPrefBooleanGlobal("save_history", true);
        if (saveHistory) {
            HistoryStore.get().add(OldSendService.getContext(), new HistoryStore.Record(entry.fileName, target.getAbsolutePath(), written, timeStr, true));
        }

        // 如果是纯文本且小于 10KB，自动复制到剪贴板
        if (entry.fileName.toLowerCase().endsWith(".txt") && written < 10 * 1024) {
            try {
                byte[] bytes = new byte[(int) written];
                java.io.FileInputStream fis = new java.io.FileInputStream(target);
                fis.read(bytes);
                fis.close();
                String text = new String(bytes, "UTF-8");
                android.content.ClipboardManager cm = (android.content.ClipboardManager) OldSendService.getContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(android.content.ClipData.newPlainText("OldSend", text));
                OldSendService.log("文本已自动复制到剪贴板");
            } catch (Exception e) {
                OldSendService.log("自动复制失败: " + e);
            }
        }
        
        session.markDone(fileId);
        if (session.allDone()) store.remove(sessionId);

        JSONObject resp = new JSONObject();
        resp.put("sessionId", sessionId);
        return HttpResponse.json(resp.toString());
    }
}
