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

public class WebShareHandler implements Router.Handler {

    // 网页界面的 HTML 代码
    private static final String HTML_PAGE = "<!DOCTYPE html>\n" +
        "<html lang=\"zh-CN\">\n" +
        "<head>\n" +
        "<meta charset=\"UTF-8\">\n" +
        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
        "<title>OldSend 网页传输</title>\n" +
        "<style>\n" +
        "body { font-family: -apple-system, sans-serif; background: #f0f2f5; padding: 20px; text-align: center; }\n" +
        ".box { background: white; max-width: 500px; margin: 0 auto; padding: 30px; border-radius: 10px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }\n" +
        "h1 { color: #333; font-size: 24px; margin-bottom: 5px; }\n" +
        "p { color: #666; font-size: 14px; margin-bottom: 20px; }\n" +
        ".drop-zone { border: 2px dashed #007bff; border-radius: 10px; padding: 40px 20px; cursor: pointer; transition: 0.3s; }\n" +
        ".drop-zone:hover { background: #e6f2ff; }\n" +
        "input[type=file] { display: none; }\n" +
        ".progress { display: none; margin-top: 20px; height: 20px; background: #eee; border-radius: 10px; overflow: hidden; }\n" +
        ".bar { height: 100%; width: 0; background: #28a745; transition: width 0.3s; }\n" +
        "#status { margin-top: 15px; font-size: 14px; color: #007bff; }\n" +
        "</style>\n" +
        "</head>\n" +
        "<body>\n" +
        "<div class=\"box\">\n" +
        "<h1>OldSend 快传</h1>\n" +
        "<p>将文件发送到手机</p>\n" +
        "<div class=\"drop-zone\" id=\"dropZone\" onclick=\"document.getElementById('fileInput').click()\">\n" +
        "  点击选择文件，或将文件拖拽到这里\n" +
        "</div>\n" +
        "<input type=\"file\" id=\"fileInput\" multiple>\n" +
        "<div class=\"progress\" id=\"progressBox\"><div class=\"bar\" id=\"progressBar\"></div></div>\n" +
        "<div id=\"status\"></div>\n" +
        "</div>\n" +
        "<script>\n" +
        "const fileInput = document.getElementById('fileInput');\n" +
        "const statusDiv = document.getElementById('status');\n" +
        "const progressBox = document.getElementById('progressBox');\n" +
        "const progressBar = document.getElementById('progressBar');\n" +
        "const dropZone = document.getElementById('dropZone');\n" +
        "\n" +
        "dropZone.addEventListener('dragover', (e) => { e.preventDefault(); dropZone.style.background = '#e6f2ff'; });\n" +
        "dropZone.addEventListener('dragleave', (e) => { e.preventDefault(); dropZone.style.background = 'transparent'; });\n" +
        "dropZone.addEventListener('drop', (e) => {\n" +
        "  e.preventDefault();\n" +
        "  dropZone.style.background = 'transparent';\n" +
        "  if (e.dataTransfer.files.length > 0) uploadFiles(e.dataTransfer.files);\n" +
        "});\n" +
        "\n" +
        "fileInput.addEventListener('change', () => {\n" +
        "  if (fileInput.files.length > 0) uploadFiles(fileInput.files);\n" +
        "});\n" +
        "\n" +
        "function uploadFiles(files) {\n" +
        "  progressBox.style.display = 'block';\n" +
        "  let index = 0;\n" +
        "  function next() {\n" +
        "    if (index >= files.length) {\n" +
        "      statusDiv.innerText = '全部发送成功！';\n" +
        "      progressBar.style.width = '100%';\n" +
        "      return;\n" +
        "    }\n" +
        "    const file = files[index];\n" +
        "    statusDiv.innerText = '正在发送: ' + file.name;\n" +
        "    const xhr = new XMLHttpRequest();\n" +
        "    xhr.open('POST', '/web-upload?name=' + encodeURIComponent(file.name), true);\n" +
        "    xhr.setRequestHeader('Content-Type', 'application/octet-stream');\n" +
        "    xhr.upload.onprogress = (e) => {\n" +
        "      if (e.lengthComputable) {\n" +
        "        const pct = Math.round((e.loaded / e.total) * 100);\n" +
        "        progressBar.style.width = pct + '%';\n" +
        "      }\n" +
        "    };\n" +
        "    xhr.onload = () => {\n" +
        "      if (xhr.status === 200) { index++; next(); }\n" +
        "      else { statusDiv.innerText = '发送失败，请重试'; }\n" +
        "    };\n" +
        "    xhr.onerror = () => { statusDiv.innerText = '网络错误'; };\n" +
        "    xhr.send(file);\n" +
        "  }\n" +
        "  next();\n" +
        "}\n" +
        "</script>\n" +
        "</body>\n" +
        "</html>";

    public HttpResponse handle(HttpRequest req) throws Exception {
        // 1. 浏览器请求首页，返回 HTML
        if (req.path.equals("/") || req.path.equals("/index.html")) {
            HttpResponse resp = new HttpResponse();
            resp.contentType = "text/html; charset=utf-8";
            resp.body = HTML_PAGE.getBytes("UTF-8");
            return resp;
        }
        
        // 2. 网页 POST 上传文件（自己定义的极简协议）
        if (req.path.equals("/web-upload") && req.method.equalsIgnoreCase("POST")) {
            Map<String, String> q = req.getQueryParams();
            String fileName = q.get("name");
            if (fileName == null) fileName = "web_file_" + System.currentTimeMillis();

            File target = FileSaver.createTarget(fileName);
            OldSendService.log("网页上传: " + fileName + " (" + req.contentLength + " bytes)");

            long written = 0;
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
                    written += n;
                    remaining -= n;
                }
            } finally {
                try { fos.close(); } catch (Exception ignored) {}
            }

            OldSendService.log("网页接收完成: " + fileName + " (" + written + " bytes)");
            JSONObject resp = new JSONObject();
            resp.put("status", "ok");
            return HttpResponse.json(resp.toString());
        }

        return HttpResponse.status(404);
    }
}
