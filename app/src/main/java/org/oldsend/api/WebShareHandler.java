package org.oldsend.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.oldsend.OldSendService;
import org.oldsend.http.HttpRequest;
import org.oldsend.http.HttpResponse;
import org.oldsend.http.Router;
import org.oldsend.transfer.FileSaver;
import android.os.Environment;

public class WebShareHandler implements Router.Handler {

    private static final String HTML_PAGE = "<!DOCTYPE html>\n" +
        "<html lang=\"zh-CN\">\n" +
        "<head>\n" +
        "<meta charset=\"UTF-8\">\n" +
        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
        "<title>OldSend 网页快传</title>\n" +
        "<style>\n" +
        "body { font-family: -apple-system, sans-serif; background: #f0f2f5; margin: 0; padding: 20px; display: flex; justify-content: center; }\n" +
        ".container { background: white; width: 100%; max-width: 600px; padding: 30px; border-radius: 16px; box-shadow: 0 8px 30px rgba(0,0,0,0.08); }\n" +
        "h1 { color: #1C1B1F; text-align: center; margin-top: 0; font-size: 26px; }\n" +
        ".tabs { display: flex; margin-bottom: 24px; background: #F4F6F8; border-radius: 12px; padding: 4px; }\n" +
        ".tab { flex: 1; text-align: center; padding: 12px; cursor: pointer; color: #666; font-weight: 500; border-radius: 8px; transition: 0.3s; }\n" +
        ".tab.active { background: white; color: #1B5E20; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }\n" +
        ".content { display: none; }\n" +
        ".content.active { display: block; }\n" +
        ".drop-zone { border: 2px dashed #388E3C; border-radius: 12px; padding: 50px 20px; text-align: center; cursor: pointer; background: #F1F8E9; transition: 0.3s; }\n" +
        ".drop-zone:hover { background: #E8F5E9; }\n" +
        "input[type=file] { display: none; }\n" +
        ".progress { display: none; margin-top: 20px; height: 8px; background: #eee; border-radius: 4px; overflow: hidden; }\n" +
        ".bar { height: 100%; width: 0; background: #388E3C; transition: width 0.2s; }\n" +
        ".file-item { display: flex; justify-content: space-between; align-items: center; padding: 16px; background: #F8F9FA; border-radius: 8px; margin-bottom: 12px; }\n" +
        ".btn-dl { background: #388E3C; color: white; text-decoration: none; padding: 8px 16px; border-radius: 20px; font-size: 14px; font-weight: 500; }\n" +
        "</style>\n" +
        "</head>\n" +
        "<body>\n" +
        "<div class=\"container\">\n" +
        "  <h1>OldSend 快传</h1>\n" +
        "  <div class=\"tabs\">\n" +
        "    <div class=\"tab active\" onclick=\"switchTab(0)\">📤 发送到手机</div>\n" +
        "    <div class=\"tab\" onclick=\"switchTab(1)\">📥 从手机下载</div>\n" +
        "  </div>\n" +
        "  <div class=\"content active\" id=\"tab0\">\n" +
        "    <div class=\"drop-zone\" id=\"dropZone\" onclick=\"document.getElementById('fileInput').click()\">\n" +
        "      <h2 style=\"color: #2E7D32; margin: 0 0 10px 0;\">点击或拖拽文件到这里</h2>\n" +
        "      <p style=\"color: #666; margin: 0;\">支持多文件并发传输</p>\n" +
        "    </div>\n" +
        "    <input type=\"file\" id=\"fileInput\" multiple>\n" +
        "    <div class=\"progress\" id=\"progressBox\"><div class=\"bar\" id=\"progressBar\"></div></div>\n" +
        "    <div id=\"status\" style=\"text-align: center; margin-top: 16px; color: #388E3C; font-weight: bold;\"></div>\n" +
        "  </div>\n" +
        "  <div class=\"content\" id=\"tab1\">\n" +
        "    <div id=\"fileList\"><p style=\"text-align:center; color:#999;\">加载中...</p></div>\n" +
        "  </div>\n" +
        "</div>\n" +
        "<script>\n" +
        "function switchTab(idx) {\n" +
        "  document.querySelectorAll('.tab').forEach((t, i) => t.className = i === idx ? 'tab active' : 'tab');\n" +
        "  document.querySelectorAll('.content').forEach((c, i) => c.className = i === idx ? 'content active' : 'content');\n" +
        "  if (idx === 1) loadFiles();\n" +
        "}\n" +
        "const dropZone = document.getElementById('dropZone');\n" +
        "dropZone.addEventListener('dragover', (e) => { e.preventDefault(); dropZone.style.background = '#C8E6C9'; });\n" +
        "dropZone.addEventListener('dragleave', (e) => { e.preventDefault(); dropZone.style.background = '#F1F8E9'; });\n" +
        "dropZone.addEventListener('drop', (e) => {\n" +
        "  e.preventDefault(); dropZone.style.background = '#F1F8E9';\n" +
        "  if (e.dataTransfer.files.length > 0) uploadFiles(e.dataTransfer.files);\n" +
        "});\n" +
        "document.getElementById('fileInput').addEventListener('change', (e) => { if (e.target.files.length > 0) uploadFiles(e.target.files); });\n" +
        "function uploadFiles(files) {\n" +
        "  const progressBox = document.getElementById('progressBox');\n" +
        "  const progressBar = document.getElementById('progressBar');\n" +
        "  const statusDiv = document.getElementById('status');\n" +
        "  progressBox.style.display = 'block';\n" +
        "  let index = 0;\n" +
        "  function next() {\n" +
        "    if (index >= files.length) { statusDiv.innerText = '全部发送成功！'; progressBar.style.width = '100%'; return; }\n" +
        "    const file = files[index];\n" +
        "    statusDiv.innerText = '正在发送: ' + file.name;\n" +
        "    const xhr = new XMLHttpRequest();\n" +
        "    xhr.open('POST', '/web-upload?name=' + encodeURIComponent(file.name), true);\n" +
        "    xhr.setRequestHeader('Content-Type', 'application/octet-stream');\n" +
        "    xhr.upload.onprogress = (e) => { if (e.lengthComputable) progressBar.style.width = Math.round((e.loaded / e.total) * 100) + '%'; };\n" +
        "    xhr.onload = () => { if (xhr.status === 200) { index++; next(); } else { statusDiv.innerText = '发送失败'; } };\n" +
        "    xhr.send(file);\n" +
        "  }\n" +
        "  next();\n" +
        "}\n" +
        "function loadFiles() {\n" +
        "  const list = document.getElementById('fileList');\n" +
        "  fetch('/web-files').then(r => r.json()).then(data => {\n" +
        "    if (data.length === 0) { list.innerHTML = '<p style=\"text-align:center; color:#999;\">暂无可下载文件</p>'; return; }\n" +
        "    list.innerHTML = data.map(f => {\n" +
        "      let sz = f.size > 1024*1024 ? (f.size/1024/1024).toFixed(1)+' MB' : (f.size/1024).toFixed(1)+' KB';\n" +
        "      return '<div class=\"file-item\"><div><div style=\"font-weight:500;color:#333;word-break:break-all;\">' + f.name + '</div><div style=\"font-size:12px;color:#999;margin-top:4px;\">' + sz + '</div></div><a class=\"btn-dl\" href=\"/web-download?name=' + encodeURIComponent(f.name) + '\">下载</a></div>';\n" +
        "    }).join('');\n" +
        "  }).catch(e => { list.innerHTML = '<p style=\"text-align:center; color:#999;\">加载失败</p>'; });\n" +
        "}\n" +
        "</script>\n" +
        "</body>\n" +
        "</html>";

    public HttpResponse handle(HttpRequest req) throws Exception {
        if (!OldSendService.getPrefBooleanGlobal("web_enable", true)) return HttpResponse.status(404);
        if (req.path.equals("/") || req.path.equals("/index.html")) {
            HttpResponse resp = new HttpResponse();
            resp.contentType = "text/html; charset=utf-8";
            resp.body = HTML_PAGE.getBytes("UTF-8");
            return resp;
        }
        if (req.path.equals("/web-upload") && req.method.equalsIgnoreCase("POST")) {
            Map<String, String> q = req.getQueryParams();
            String fileName = q.get("name");
            if (fileName == null) fileName = "web_" + System.currentTimeMillis();
            File target = FileSaver.createTarget(fileName, OldSendService.getPrefBooleanGlobal("save_album", true));
            FileOutputStream fos = new FileOutputStream(target);
            try {
                InputStream in = req.body;
                byte[] buf = new byte[65536];
                long remaining = req.contentLength > 0 ? req.contentLength : Long.MAX_VALUE;
                while (remaining > 0) {
                    int max = (int) Math.min(buf.length, remaining);
                    int n = in.read(buf, 0, max);
                    if (n < 0) break;
                    fos.write(buf, 0, n);
                    remaining -= n;
                }
            } finally { try { fos.close(); } catch (Exception ignored) {} }
            return HttpResponse.json("{\"status\":\"ok\"}");
        }
        if (req.path.equals("/web-files") && req.method.equalsIgnoreCase("GET")) {
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "OldSend");
            JSONArray files = new JSONArray();
            if (dir.exists() && dir.isDirectory()) {
                File[] list = dir.listFiles();
                if (list != null) {
                    for (File f : list) {
                        if (f.isFile()) {
                            JSONObject obj = new JSONObject();
                            obj.put("name", f.getName());
                            obj.put("size", f.length());
                            files.put(obj);
                        }
                    }
                }
            }
            return HttpResponse.json(files.toString());
        }
        if (req.path.equals("/web-download") && req.method.equalsIgnoreCase("GET")) {
            Map<String, String> q = req.getQueryParams();
            String name = q.get("name");
            if (name != null) {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "OldSend");
                File f = new File(dir, name);
                if (f.exists() && f.isFile()) {
                    HttpResponse resp = new HttpResponse();
                    resp.contentType = "application/octet-stream";
                    resp.headers.put("Content-Disposition", "attachment; filename=\"" + f.getName() + "\"");
                    resp.stream = new FileInputStream(f);
                    resp.contentLength = f.length();
                    return resp;
                }
            }
        }
        return HttpResponse.status(404);
    }
}