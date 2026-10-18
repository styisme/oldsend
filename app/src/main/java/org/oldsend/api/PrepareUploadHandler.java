package org.oldsend.api;

import java.util.Iterator;
import java.util.Map;
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
        
        // 1. 验证 PIN
        boolean usePin = OldSendService.getPrefBooleanGlobal("use_pin", false);
        String expectedPin = OldSendService.getPin();
        if (usePin) {
            Map<String, String> q = req.getQueryParams();
            String clientPin = q.get("pin");
            if (clientPin == null || !clientPin.equals(expectedPin)) {
                HttpResponse res = HttpResponse.status(401);
                res.headers.put("X-LocalSend-Pin", "true");
                OldSendService.log("拦截：对方未提供正确的 PIN");
                return res;
            }
        }

        String body = RegisterHandler.readAll(req);
        JSONObject o = new JSONObject(body);
        JSONObject files = o.optJSONObject("files");
        if (files == null) files = new JSONObject();

        String senderAlias = "未知设备";
        String senderFingerprint = req.remoteAddress.getHostAddress(); // 默认用 IP 兜底

        JSONObject info = o.optJSONObject("info");
        if (info != null) {
            senderAlias = info.optString("alias", "未知设备");
            String f = info.optString("fingerprint", "");
            if (!f.isEmpty()) senderFingerprint = f;
        }
        
        String blacklist = OldSendService.getPrefStringGlobal("blacklist", "");
        if (blacklist.contains(senderFingerprint)) {
            OldSendService.log("黑名单拦截: " + senderAlias);
            return HttpResponse.status(403);
        }

        int fileCount = files.length();
        OldSendService.log("收到请求: " + senderAlias + " 请求发送 " + fileCount + " 个文件");

        // 2. 检查是否开启了自动保存 (跳过弹窗)
        boolean autoSave = OldSendService.getPrefBooleanGlobal("auto_save", false);
        boolean autoWhitelist = OldSendService.getPrefBooleanGlobal("auto_accept_whitelist", false);
        String whitelist = OldSendService.getPrefStringGlobal("whitelist", "");
        
        if (!autoSave && autoWhitelist && whitelist.contains(senderFingerprint)) {
            autoSave = true;
            OldSendService.log("白名单自动放行: " + senderAlias);
        }
        boolean accepted = true;
        if (!autoSave) {
            accepted = ReceiveManager.askUser(senderAlias, fileCount);
        } else {
            OldSendService.log("自动保存已开启，直接接收");
        }

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
            String filterStr = OldSendService.getPrefStringGlobal("filter_ext", "exe,bat,cmd,vbs,sh");
            if (filterStr.length() > 0) {
                String lower = fileName.toLowerCase();
                String[] exts = filterStr.split(",");
                for (String ext : exts) {
                    if (lower.endsWith("." + ext.trim())) {
                        OldSendService.log("拦截自定义后缀文件: " + fileName);
                        return HttpResponse.status(403);
                    }
                }
            }
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
