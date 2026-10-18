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

    private static final String HTML_PAGE = "<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n    <title>OldSend Web Share</title>\n    <style>\n        :root {\n            --bg: #f4f6f8; --card-bg: #ffffff; --text: #1c1b1f; --text-sec: #666666;\n            --primary: #388e3c; --primary-light: #c8e6c9; --border: #e0e0e0;\n            --shadow: 0 4px 20px rgba(0,0,0,0.05);\n        }\n        @media (prefers-color-scheme: dark) {\n            :root {\n                --bg: #121212; --card-bg: #1e1e1e; --text: #e0e0e0; --text-sec: #aaaaaa;\n                --primary: #4caf50; --primary-light: #2c3e35; --border: #3e3e3e;\n                --shadow: 0 4px 20px rgba(0,0,0,0.3);\n            }\n        }\n        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif; }\n        body { background-color: var(--bg); color: var(--text); display: flex; justify-content: center; min-height: 100vh; padding: 20px; }\n        .container { width: 100%; max-width: 800px; }\n        .header { text-align: center; margin-bottom: 30px; }\n        .header h1 { font-size: 28px; color: var(--primary); margin-bottom: 8px; }\n        .header p { font-size: 14px; color: var(--text-sec); }\n        .tabs { display: flex; background: var(--card-bg); border-radius: 12px; padding: 4px; margin-bottom: 20px; box-shadow: var(--shadow); }\n        .tab { flex: 1; text-align: center; padding: 12px; border-radius: 8px; cursor: pointer; font-weight: 500; color: var(--text-sec); transition: all 0.3s; }\n        .tab.active { background: var(--primary-light); color: var(--primary); }\n        .card { background: var(--card-bg); border-radius: 16px; padding: 24px; box-shadow: var(--shadow); border: 1px solid var(--border); display: none; }\n        .card.active { display: block; animation: fadeIn 0.3s; }\n        @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }\n        .drop-zone { border: 2px dashed var(--primary); border-radius: 12px; padding: 60px 20px; text-align: center; cursor: pointer; transition: background 0.3s; background: var(--bg); }\n        .drop-zone.dragover { background: var(--primary-light); }\n        .drop-zone h3 { font-size: 20px; margin-bottom: 8px; color: var(--primary); }\n        .drop-zone p { font-size: 14px; color: var(--text-sec); }\n        .file-list { margin-top: 20px; }\n        .file-item { display: flex; align-items: center; background: var(--bg); padding: 12px; border-radius: 8px; margin-bottom: 8px; }\n        .file-icon { width: 40px; height: 40px; background: var(--primary-light); border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 20px; margin-right: 12px; }\n        .file-info { flex: 1; overflow: hidden; }\n        .file-name { font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }\n        .file-meta { font-size: 12px; color: var(--text-sec); margin-top: 4px; }\n        .progress-bar { width: 100%; height: 4px; background: var(--border); border-radius: 2px; margin-top: 8px; overflow: hidden; }\n        .progress-fill { height: 100%; background: var(--primary); width: 0%; transition: width 0.2s; }\n        .btn { background: var(--primary); color: white; border: none; padding: 10px 20px; border-radius: 20px; cursor: pointer; font-weight: 500; transition: opacity 0.3s; }\n        .btn:hover { opacity: 0.8; }\n        .btn-outline { background: transparent; color: var(--primary); border: 1px solid var(--primary); padding: 8px 16px; border-radius: 8px; cursor: pointer; text-decoration: none; font-size: 14px; }\n        textarea { width: 100%; min-height: 150px; padding: 12px; border-radius: 8px; border: 1px solid var(--border); background: var(--bg); color: var(--text); resize: vertical; margin-bottom: 16px; font-size: 14px; }\n        textarea:focus { outline: none; border-color: var(--primary); }\n        .toast { position: fixed; bottom: 20px; left: 50%; transform: translateX(-50%); background: #333; color: #fff; padding: 10px 20px; border-radius: 20px; font-size: 14px; opacity: 0; transition: opacity 0.3s; pointer-events: none; z-index: 1000; }\n        .toast.show { opacity: 1; }\n    </style>\n</head>\n<body>\n    <div class=\"container\">\n        <div class=\"header\">\n            <h1 id=\"device-name\">OldSend Web</h1>\n            <p id=\"device-info\">\u6b63\u5728\u52a0\u8f7d...</p>\n        </div>\n        \n        <div class=\"tabs\">\n            <div class=\"tab active\" onclick=\"switchTab('upload')\">\ud83d\udce4 \u53d1\u9001\u5230\u624b\u673a</div>\n            <div class=\"tab\" onclick=\"switchTab('download')\">\ud83d\udce5 \u4ece\u624b\u673a\u4e0b\u8f7d</div>\n            <div class=\"tab\" onclick=\"switchTab('text')\">\ud83d\udcdd \u53d1\u9001\u6587\u672c</div>\n        </div>\n\n        <div id=\"tab-upload\" class=\"card active\">\n            <div class=\"drop-zone\" id=\"drop-zone\" onclick=\"document.getElementById('file-input').click()\">\n                <h3>\u70b9\u51fb\u6216\u62d6\u62fd\u6587\u4ef6\u5230\u8fd9\u91cc</h3>\n                <p>\u652f\u6301\u591a\u6587\u4ef6\u5e76\u53d1\u3001\u76f4\u63a5\u7c98\u8d34\u622a\u56fe</p>\n            </div>\n            <input type=\"file\" id=\"file-input\" multiple style=\"display: none;\" onchange=\"handleFiles(this.files)\">\n            <div class=\"file-list\" id=\"upload-list\"></div>\n        </div>\n\n        <div id=\"tab-download\" class=\"card\">\n            <div class=\"file-list\" id=\"download-list\">\n                <p style=\"text-align:center; color:var(--text-sec);\">\u52a0\u8f7d\u4e2d...</p>\n            </div>\n        </div>\n\n        <div id=\"tab-text\" class=\"card\">\n            <textarea id=\"text-input\" placeholder=\"\u5728\u6b64\u8f93\u5165\u6587\u672c\u3001\u7f51\u5740\uff0c\u53d1\u9001\u540e\u76f4\u63a5\u5b58\u5165\u624b\u673a\u526a\u8d34\u677f...\"></textarea>\n            <button class=\"btn\" onclick=\"sendText()\">\u53d1\u9001\u5230\u624b\u673a</button>\n        </div>\n    </div>\n\n    <div class=\"toast\" id=\"toast\"></div>\n\n    <script>\n        fetch('/web/info').then(r => r.json()).then(data => {\n            document.getElementById('device-name').innerText = data.alias;\n            document.getElementById('device-info').innerText = data.deviceModel + ' | ' + data.ip;\n        }).catch(() => {});\n\n        function switchTab(tab) {\n            document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));\n            document.querySelectorAll('.card').forEach(c => c.classList.remove('active'));\n            document.querySelectorAll('.tab')[['upload', 'download', 'text'].indexOf(tab)].classList.add('active');\n            document.getElementById('tab-' + tab).classList.add('active');\n            if (tab === 'download') loadFiles();\n        }\n\n        function showToast(msg) {\n            const toast = document.getElementById('toast');\n            toast.innerText = msg; toast.classList.add('show');\n            setTimeout(() => toast.classList.remove('show'), 2000);\n        }\n\n        const dropZone = document.getElementById('drop-zone');\n        dropZone.addEventListener('dragover', (e) => { e.preventDefault(); dropZone.classList.add('dragover'); });\n        dropZone.addEventListener('dragleave', (e) => { e.preventDefault(); dropZone.classList.remove('dragover'); });\n        dropZone.addEventListener('drop', (e) => {\n            e.preventDefault(); dropZone.classList.remove('dragover');\n            if (e.dataTransfer.files.length > 0) handleFiles(e.dataTransfer.files);\n        });\n\n        document.addEventListener('paste', (e) => {\n            const items = e.clipboardData.items;\n            const files = [];\n            for (let i = 0; i < items.length; i++) {\n                if (items[i].type.indexOf('image') !== -1) {\n                    const file = items[i].getAsFile();\n                    if(file) files.push(file);\n                }\n            }\n            if (files.length > 0) { handleFiles(files); showToast('\u5df2\u4ece\u526a\u8d34\u677f\u83b7\u53d6\u56fe\u7247'); switchTab('upload'); }\n        });\n\n        function getIcon(name) {\n            name = name.toLowerCase();\n            if (name.match(/\\.(jpg|jpeg|png|gif|webp)$/)) return '\ud83d\uddbc\ufe0f';\n            if (name.match(/\\.(mp4|mkv|avi|mov)$/)) return '\ud83c\udfac';\n            if (name.match(/\\.(mp3|wav|flac)$/)) return '\ud83c\udfb5';\n            if (name.match(/\\.(zip|rar|7z|tar|gz)$/)) return '\ud83d\udce6';\n            if (name.match(/\\.(apk)$/)) return '\ud83e\udd16';\n            if (name.match(/\\.(pdf)$/)) return '\ud83d\udcd5';\n            if (name.match(/\\.(txt|md)$/)) return '\ud83d\udcdd';\n            return '\ud83d\udcc4';\n        }\n\n        function formatSize(bytes) {\n            if (bytes === 0) return '0 B';\n            const k = 1024, sizes = ['B', 'KB', 'MB', 'GB'];\n            const i = Math.floor(Math.log(bytes) / Math.log(k));\n            return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];\n        }\n\n        async function handleFiles(files) {\n            if (!files || files.length === 0) return;\n            const uploadList = document.getElementById('upload-list');\n            const CONCURRENCY = 3;\n            let index = 0;\n\n            async function uploadWorker() {\n                while (index < files.length) {\n                    const i = index++;\n                    const file = files[i];\n                    const itemDiv = document.createElement('div');\n                    itemDiv.className = 'file-item';\n                    itemDiv.innerHTML = `\n                        <div class=\"file-icon\">${getIcon(file.name)}</div>\n                        <div class=\"file-info\">\n                            <div class=\"file-name\">${file.name}</div>\n                            <div class=\"file-meta\">${formatSize(file.size)}</div>\n                            <div class=\"progress-bar\"><div class=\"progress-fill\" id=\"prog-${i}\"></div></div>\n                        </div>\n                        <div id=\"status-${i}\" style=\"font-size:12px; font-weight:bold; color:var(--text-sec);\">\u7b49\u5f85</div>\n                    `;\n                    uploadList.appendChild(itemDiv);\n                    \n                    await new Promise((resolve) => {\n                        const xhr = new XMLHttpRequest();\n                        xhr.open('POST', '/web/upload?name=' + encodeURIComponent(file.name), true);\n                        xhr.setRequestHeader('Content-Type', 'application/octet-stream');\n                        xhr.upload.onprogress = (e) => {\n                            if (e.lengthComputable) {\n                                const pct = Math.round((e.loaded / e.total) * 100);\n                                document.getElementById(`prog-${i}`).style.width = pct + '%';\n                                document.getElementById(`status-${i}`).innerText = pct + '%';\n                            }\n                        };\n                        xhr.onload = () => {\n                            if (xhr.status === 200) {\n                                document.getElementById(`prog-${i}`).style.width = '100%';\n                                document.getElementById(`status-${i}`).innerText = '\u5b8c\u6210';\n                                document.getElementById(`status-${i}`).style.color = 'var(--primary)';\n                            } else { document.getElementById(`status-${i}`).innerText = '\u5931\u8d25'; }\n                            resolve();\n                        };\n                        xhr.onerror = () => { document.getElementById(`status-${i}`).innerText = '\u9519\u8bef'; resolve(); };\n                        xhr.send(file);\n                    });\n                }\n            }\n\n            const workers = [];\n            for (let w = 0; w < Math.min(CONCURRENCY, files.length); w++) workers.push(uploadWorker());\n            await Promise.all(workers);\n            showToast('\u6240\u6709\u6587\u4ef6\u53d1\u9001\u5b8c\u6210\uff01');\n        }\n\n        function loadFiles() {\n            const list = document.getElementById('download-list');\n            list.innerHTML = '<p style=\"text-align:center; color:var(--text-sec);\">\u52a0\u8f7d\u4e2d...</p>';\n            fetch('/web/files').then(r => r.json()).then(data => {\n                if (data.length === 0) { list.innerHTML = '<p style=\"text-align:center; color:var(--text-sec);\">\u6682\u65e0\u53ef\u4e0b\u8f7d\u6587\u4ef6</p>'; return; }\n                list.innerHTML = '';\n                data.forEach(f => {\n                    const div = document.createElement('div');\n                    div.className = 'file-item';\n                    div.innerHTML = `\n                        <div class=\"file-icon\">${getIcon(f.name)}</div>\n                        <div class=\"file-info\">\n                            <div class=\"file-name\">${f.name}</div>\n                            <div class=\"file-meta\">${formatSize(f.size)}</div>\n                        </div>\n                        <a href=\"/web/download?name=${encodeURIComponent(f.name)}\" class=\"btn-outline\">\u4e0b\u8f7d</a>\n                    `;\n                    list.appendChild(div);\n                });\n            }).catch(() => { list.innerHTML = '<p style=\"text-align:center; color:var(--text-sec);\">\u52a0\u8f7d\u5931\u8d25</p>'; });\n        }\n\n        function sendText() {\n            const text = document.getElementById('text-input').value;\n            if (!text) return showToast('\u8bf7\u8f93\u5165\u6587\u672c');\n            fetch('/web/text', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ text: text }) })\n            .then(r => {\n                if (r.ok) { showToast('\u6587\u672c\u5df2\u53d1\u9001\u5230\u624b\u673a\u526a\u8d34\u677f'); document.getElementById('text-input').value = ''; }\n                else showToast('\u53d1\u9001\u5931\u8d25');\n            }).catch(() => showToast('\u7f51\u7edc\u9519\u8bef'));\n        }\n    </script>\n</body>\n</html>";

    public HttpResponse handle(HttpRequest req) throws Exception {
        if (!OldSendService.getPrefBooleanGlobal("web_enable", true)) return HttpResponse.status(404);
        
        if (req.path.equals("/") || req.path.equals("/index.html")) {
            HttpResponse resp = new HttpResponse();
            resp.contentType = "text/html; charset=utf-8";
            resp.body = HTML_PAGE.getBytes("UTF-8");
            return resp;
        }

        if (req.path.equals("/web/info") && req.method.equalsIgnoreCase("GET")) {
            JSONObject info = new JSONObject();
            info.put("alias", OldSendService.getAlias(null));
            info.put("deviceModel", "Android " + android.os.Build.VERSION.RELEASE);
            info.put("ip", OldSendService.getLocalIp());
            return HttpResponse.json(info.toString());
        }
        
        if (req.path.equals("/web/upload") && req.method.equalsIgnoreCase("POST")) {
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
            } finally {
                try { fos.close(); } catch (Exception ignored) {}
            }
            OldSendService.log("Web 上传完成: " + fileName);
            return HttpResponse.json("{\"status\":\"ok\"}");
        }
        
        if (req.path.equals("/web/files") && req.method.equalsIgnoreCase("GET")) {
            JSONArray arr = new JSONArray();
            File[] dirs = {
                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "OldSend"),
                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "OldSend"),
                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "OldSend")
            };
            for (File dir : dirs) {
                if (dir.exists() && dir.isDirectory()) {
                    File[] list = dir.listFiles();
                    if (list != null) {
                        for (File f : list) {
                            if (f.isFile()) {
                                JSONObject obj = new JSONObject();
                                obj.put("name", f.getName());
                                obj.put("size", f.length());
                                arr.put(obj);
                            }
                        }
                    }
                }
            }
            return HttpResponse.json(arr.toString());
        }
        
        if (req.path.equals("/web/download") && req.method.equalsIgnoreCase("GET")) {
            Map<String, String> q = req.getQueryParams();
            String name = q.get("name");
            if (name != null) {
                File[] dirs = {
                    new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "OldSend"),
                    new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "OldSend"),
                    new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "OldSend")
                };
                for (File dir : dirs) {
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
        }
        
        if (req.path.equals("/web/text") && req.method.equalsIgnoreCase("POST")) {
            String body = RegisterHandler.readAll(req);
            try {
                JSONObject obj = new JSONObject(body);
                String text = obj.optString("text", "");
                if (!text.isEmpty()) {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager) OldSendService.getContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("OldSend", text));
                }
            } catch (Exception e) {}
            return HttpResponse.json("{\"status\":\"ok\"}");
        }
        
        return HttpResponse.status(404);
    }
}
