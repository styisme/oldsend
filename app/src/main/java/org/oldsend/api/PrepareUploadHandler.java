package org.oldsend.api;

import java.util.Iterator;
import org.json.JSONObject;
import org.oldsend.OldSendService;
import org.oldsend.ReceiveManager;
import org.oldsend.http.HttpRequest;
import org.oldsend.http.HttpResponse;
import org.oldsend.http.Router;

public class PrepareUploadHandler implements Router.Handler {
    private final SessionStore store;

    public PrepareUploadHandler(SessionStore store) { this.store = store; }

    public HttpResponse handle(HttpRequest req) throws Exception {
        if (!"POST".equalsIgnoreCase(req.method)) return HttpResponse.status(405);
        String body = RegisterHandler.readAll(req);
        JSONObject o = new JSONObject(body);
        JSONObject files = o.optJSONObject("files");
        if (files == null) files = new JSONObject();

        String senderAlias = "未知设备";
        JSONObject info = o.optJSONObject("info");
        if (info != null) senderAlias = info.optString("alias", "未知设备");

        int fileCount = files.length();
        OldSendService.log("收到请求: " + senderAlias + " 请求发送 " + fileCount + " 个文件");

        // 弹窗询问用户是否接收
        boolean accepted = ReceiveManager.askUser(senderAlias, fileCount);
        if (!accepted) {
            OldSendService.log("已拒绝 " + senderAlias + " 的发送请求");
            return HttpResponse.status(403);
        }

        UploadSession session = store.create(req.remoteAddress.getHostAddress());
        JSONObject respFiles = new JSONObject();
        Iterator<?> it = files.keys();
        while (it.hasNext()) {
            String fileId = (String) it.next();
            JSONObject f = files.getJSONObject(fileId);
            String fileName = f.optString("fileName", fileId);
            long size = f.optLong("size", -1);
            String token = Long.toHexString(System.nanoTime() ^ (long) (Math.random() * 1e18));
            session.addFile(fileId, fileName, size, token);
            respFiles.put(fileId, token);
        }

        JSONObject resp = new JSONObject();
        resp.put("sessionId", session.sessionId);
        resp.put("files", respFiles);
        return HttpResponse.json(resp.toString());
    }
}
