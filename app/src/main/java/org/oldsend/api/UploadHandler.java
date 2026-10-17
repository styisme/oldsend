package org.oldsend.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;

import org.json.JSONObject;
import org.oldsend.OldSendService;
import org.oldsend.http.HttpRequest;
import org.oldsend.http.HttpResponse;
import org.oldsend.http.Router;
import org.oldsend.transfer.FileSaver;

public class UploadHandler implements Router.Handler {
    private final SessionStore store;

    public UploadHandler(SessionStore store) { this.store = store; }

    public HttpResponse handle(HttpRequest req) throws Exception {
        if (!"POST".equalsIgnoreCase(req.method)) return HttpResponse.status(405);
        Map<String, String> q = req.getQueryParams();
        String sessionId = q.get("sessionId");
        String fileId = q.get("fileId");
        String token = q.get("token");

        if (sessionId == null || fileId == null || token == null) {
            return HttpResponse.status(400);
        }
        UploadSession session = store.get(sessionId);
        if (session == null) {
            OldSendService.log("upload: unknown session " + sessionId);
            return HttpResponse.status(400);
        }
        UploadSession.FileEntry entry = session.getFile(fileId);
        if (entry == null || !entry.token.equals(token)) {
            return HttpResponse.status(403);
        }

        File target = FileSaver.createTarget(entry.fileName);
        OldSendService.log("upload " + entry.fileName + " (" + entry.size
                + " bytes) -> " + target.getAbsolutePath());

        long written = 0;
        FileOutputStream fos = new FileOutputStream(target);
        try {
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
                remaining -= n;
            }
        } finally {
            try { fos.close(); } catch (Exception ignored) {}
        }

        OldSendService.log("received " + entry.fileName + " (" + written + " bytes)");
        session.markDone(fileId);
        if (session.allDone()) store.remove(sessionId);

        JSONObject resp = new JSONObject();
        resp.put("sessionId", sessionId);
        resp.put("fileId", fileId);
        return HttpResponse.json(resp.toString());
    }
}
