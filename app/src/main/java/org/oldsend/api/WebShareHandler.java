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
        "body { font-family: -apple-system, sans-serif; background: #f0f2f5; padding: 20px; text-align: center; margin: 0; }\n" +
        ".container { background: white; max-width: 600px; margin: 20px auto; padding: 30px; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); }\n" +
        "h1 { color: #333; font-size: 24px; margin-top: 0; }\n" +
        ".tabs { display: flex; margin-bottom: 20px; border-bottom: 1px solid #eee; }\n" +
        ".tab { flex: 1; padding: 12px; cursor: pointer; font-size: 16px; color: #666; border-bottom: 2px solid transparent; }\n" +
        ".tab.active { color: #2196F3; border-bottom-color: #2196F3; font-weight: bold; }\n" +
        ".content { display: none; }\n" +
        ".content.active { display: block; }\n" +
        ".drop-zone { border: 2px dashed #2196F3; border-radius: 8px; padding: 40px 20px; cursor: pointer; transition: 0.3s; background: #f8fbff; color: #666; }\n" +
        ".drop-zone:hover { background: #e6f2ff; }\n" +
        "input[type=file] { display: none; }\n" +
        ".progress { display: none; margin-top: 20px; height: 8px; background: #eee; border-radius: 4px; overflow: hidden; }\n" +
        ".bar { height: 100%; width: 0; background: #2196F3; transition: width 0.3s; }\n" +
        "#status { margin-top: 15px; font-size: 14px; color: #2196F3; }\n" +
        ".file-list { list-style: none; padding: 0; text-align: left; margin-top: 20px; }\n" +
        ".file-item { display: flex; justify-content: space-between; padding: 12px; border-bottom: 1px solid #eee; align-items: center; }\n" +
        ".file-name { font-size: 14px; color: #333; word-break: break-all; }\n" +
        ".file-size { font-size: 12px; color: #999; margin-left: 10px; }\n" +
        ".btn-dl { background: #4CAF50; color: white; text-decoration: none; padding: 6px 12px; border-radius: 4px; font-size: 12px; }\n" +
        "</style>\n" +
        "</head>\n" +
        "<body>\n" +
        "<div class=\"container\">\n" +
        "<h1>OldSend 快传</h1>\n" +
        "<div class=\"tabs\">\n" +
        "<div class=\"tab active\" onclick=\"switchTab(0)\">发送到手机</div>\n" +
        "<div class=\"tab\" onclick=\"switchTab(1)\">从手机下载</div>\n" +
        "</div>\n" +
        "<div class=\"content active\" id=\"tab0\">\n" +
        "<div class=\"drop-zone\" id=\"dropZone\" onclick=\"document.getElementById('fileInput').click()\">\n" +
        "点击选择文件，或将文件拖拽到这里\n" +
        "</div>\n" +
        "<input type=\"file\" id=\"fileInput\" multiple>\n" +
        "<div class=\"progress\" id=\"progressBox\"><div class=\"bar\" id=\"progressBar\"></div></div>\n" +
        "<div id=\"status\"></div>\n" +
        "</div>\n" +
        "<div class=\"content\" id=\"tab1\">\n" +
        "<ul class=\"file-list\" id=\"fileList\"><li style=\"text-align:center;color:#999;padding:20px;\">加载中...</li></ul>\n" +
        "</div>\n" +
        "</div>\n" +
        "<script>\n" +
        "function switchTab(idx) {\n" +
        "  document.querySelectorAll('.tab').forEach((t, i) => t.className = i === idx ? 'tab active' : 'tab');\n" +
        "  document.querySelectorAll('.content').forEach((c, i) => c.className = i === idx ? 'content active' : 'content');\n" +
        "  if (idx === 1) loadFiles();\n" +
        "}\n" +
        "const fileInput = document.getElementById('fileInput');\n" +
        "const dropZone = document.getElementById('dropZone');\n" +
        "dropZone.addEventListener('dragover', (e) => { e.preventDefault(); dropZone.style.background = '#e6f2ff'; });\n" +
        "dropZone.addEventListener('dragleave', (e) => { e.preventDefault(); dropZone.style.background = '#f8fbff'; });\n" +
        "dropZone.addEventListener('drop', (e) => {\n" +
        "  e.preventDefault(); dropZone.style.background = '#f8fbff';\n" +
        "  if (e.dataTransfer.files.length > 0) uploadFiles(e.dataTransfer.files);\n" +
        "});\n" +
        "fileInput.addEventListener('change', () => { if (fileInput.files.length > 0) uploadFiles(fileInput.files); });\n" +
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
        "    xhr.onerror = () => { statusDiv.innerText = '网络错误'; };\n" +
        "    xhr.send(file);\n" +
        "  }\n" +
        "  next();\n" +
        "}\n" +
        "function loadFiles() {\n" +
        "  const list = document.getElementById('fileList');\n" +
        "  fetch('/web-files').then(r => r.json()).then(data => {\n" +
        "    if (data.length === 0) { list.innerHTML = '<li style=\"text-align:center;color:#999;padding:20px;\">暂无可下载文件</li>'; return; }\n" +
        "    list.innerHTML = data.map(f => {\n" +
        "      let sz = f.size > 1024*1024 ? (f.size/1024/1024).toFixed(1)+' MB' : (f.size/1024).toFixed(1)+' KB';\n" +
        "      return '<li class=\"file-item\"><div><div class=\"file-name\">' + f.name + '</div><div class=\"file-size\">' + sz + '</div></div><a class=\"btn-dl\" href=\"/web-download?name=' + encodeURIComponent(f.name) + '\">下载</a></li>';\n" +
        "    }).join('');\n" +
        "  }).catch(e => { list.innerHTML = '<li style=\"text-align:center;color:#999;padding:20px;\">加载失败</li>'; });\n" +
        "}\n" +
        "</script>\n" +
        "</body>\n" +
        "</html>";

    public HttpResponse handle(HttpRequest req) throws Exception {
        if (!OldSendService.getPrefBooleanGlobal("web_enable", true)) {
            return HttpResponse.status(404);
        }
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
            OldSendService.log("网页上传: " + fileName);
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
            OldSendService.log("网页接收完成: " + fileName);
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
