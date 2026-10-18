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
import android.content.ClipboardManager;

public class WebShareHandler implements Router.Handler {

    private static final String HTML_PAGE = "<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n    <title>OldSend \u7ec8\u6781\u5c40\u57df\u7f51</title>\n    <style>\n        :root { --bg: #f4f6f8; --card-bg: #ffffff; --text: #1c1b1f; --text-sec: #666; --primary: #388e3c; --primary-light: #c8e6c9; --border: #e0e0e0; --shadow: 0 4px 20px rgba(0,0,0,0.05); }\n        @media (prefers-color-scheme: dark) { :root { --bg: #121212; --card-bg: #1e1e1e; --text: #e0e0e0; --text-sec: #aaa; --primary: #4caf50; --primary-light: #2c3e35; --border: #3e3e3e; --shadow: 0 4px 20px rgba(0,0,0,0.3); } }\n        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, sans-serif; }\n        body { background: var(--bg); color: var(--text); padding: 20px; display: flex; justify-content: center; }\n        .container { width: 100%; max-width: 800px; }\n        .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }\n        .header h1 { color: var(--primary); font-size: 24px; }\n        .clipboard-box { background: var(--card-bg); padding: 12px 20px; border-radius: 12px; display: flex; align-items: center; gap: 10px; box-shadow: var(--shadow); cursor: pointer; border: 1px solid var(--border); }\n        .clipboard-text { font-size: 14px; color: var(--text-sec); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 300px; }\n        .tabs { display: flex; background: var(--card-bg); border-radius: 12px; padding: 4px; margin-bottom: 20px; box-shadow: var(--shadow); }\n        .tab { flex: 1; text-align: center; padding: 12px; border-radius: 8px; cursor: pointer; font-weight: bold; color: var(--text-sec); transition: 0.3s; }\n        .tab.active { background: var(--primary-light); color: var(--primary); }\n        .card { background: var(--card-bg); border-radius: 16px; padding: 24px; box-shadow: var(--shadow); border: 1px solid var(--border); display: none; }\n        .card.active { display: block; animation: fadeIn 0.3s; }\n        .drop-zone { border: 2px dashed var(--primary); border-radius: 12px; padding: 60px 20px; text-align: center; cursor: pointer; background: var(--bg); }\n        .drop-zone.dragover { background: var(--primary-light); }\n        .file-item { display: flex; align-items: center; background: var(--bg); padding: 12px; border-radius: 8px; margin-top: 8px; }\n        .file-icon { width: 40px; height: 40px; background: var(--primary-light); border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 20px; margin-right: 12px; }\n        .progress-bar { width: 100%; height: 4px; background: var(--border); border-radius: 2px; margin-top: 8px; overflow: hidden; }\n        .progress-fill { height: 100%; background: var(--primary); width: 0%; transition: width 0.2s; }\n        .btn { background: var(--primary); color: white; border: none; padding: 10px 20px; border-radius: 20px; cursor: pointer; }\n        textarea { width: 100%; min-height: 120px; padding: 12px; border-radius: 8px; border: 1px solid var(--border); background: var(--bg); color: var(--text); margin-bottom: 12px; }\n        .toast { position: fixed; bottom: 20px; left: 50%; transform: translateX(-50%); background: #333; color: #fff; padding: 10px 20px; border-radius: 20px; opacity: 0; transition: 0.3s; pointer-events: none; }\n        .toast.show { opacity: 1; }\n    </style>\n</head>\n<body>\n    <div class=\"container\">\n        <div class=\"header\">\n            <h1 id=\"device-name\">OldSend</h1>\n            <div class=\"clipboard-box\" onclick=\"syncClipboard()\">\n                <span style=\"font-size:18px;\">\ud83d\udccb</span>\n                <div class=\"clipboard-text\" id=\"clipboard-display\">\u70b9\u51fb\u540c\u6b65\u624b\u673a\u526a\u8d34\u677f</div>\n            </div>\n        </div>\n        \n        <div class=\"tabs\">\n            <div class=\"tab active\" onclick=\"switchTab('upload')\">\ud83d\udce4 \u53d1\u9001</div>\n            <div class=\"tab\" onclick=\"switchTab('download')\">\ud83d\udce5 \u4e0b\u8f7d</div>\n            <div class=\"tab\" onclick=\"switchTab('text')\">\ud83d\udcdd \u6587\u672c</div>\n        </div>\n\n        <div id=\"tab-upload\" class=\"card active\">\n            <div class=\"drop-zone\" id=\"drop-zone\" onclick=\"document.getElementById('file-input').click()\">\n                <h3>\u70b9\u51fb / \u62d6\u62fd / \u7c98\u8d34 / \u62d6\u5165\u6587\u672c</h3>\n                <p>\u652f\u6301\u591a\u6587\u4ef6\u5e76\u53d1\u3001\u6587\u672c\u81ea\u52a8\u53d8TXT</p>\n            </div>\n            <input type=\"file\" id=\"file-input\" multiple style=\"display: none;\" onchange=\"handleFiles(this.files)\">\n            <div id=\"upload-list\"></div>\n        </div>\n\n        <div id=\"tab-download\" class=\"card\">\n            <div id=\"download-list\"><p>\u52a0\u8f7d\u4e2d...</p></div>\n        </div>\n\n        <div id=\"tab-text\" class=\"card\">\n            <textarea id=\"text-input\" placeholder=\"\u53d1\u9001\u6587\u672c\u5230\u624b\u673a\u526a\u8d34\u677f...\"></textarea>\n            <button class=\"btn\" onclick=\"sendText()\">\u53d1\u9001</button>\n        </div>\n    </div>\n    <div class=\"toast\" id=\"toast\"></div>\n\n    <script>\n        // \u521d\u59cb\u5316\u8bbe\u5907\u4fe1\u606f\n        fetch('/web/info').then(r=>r.json()).then(d=>document.getElementById('device-name').innerText = d.alias);\n\n        // \u526a\u8d34\u677f\u540c\u6b65\u8f6e\u8be2 (\u6bcf5\u79d2\u83b7\u53d6\u4e00\u6b21\u624b\u673a\u526a\u8d34\u677f)\n        let lastClip = \"\";\n        setInterval(() => {\n            fetch('/web/clipboard').then(r=>r.text()).then(txt => {\n                if (txt && txt !== \"null\" && txt !== lastClip) {\n                    lastClip = txt;\n                    document.getElementById('clipboard-display').innerText = \"\u624b\u673a: \" + txt;\n                }\n            });\n        }, 5000);\n\n        function syncClipboard() {\n            if (lastClip) {\n                navigator.clipboard.writeText(lastClip).then(()=>showToast('\u5df2\u590d\u5236\u624b\u673a\u526a\u8d34\u677f\u5185\u5bb9'));\n            }\n        }\n\n        function switchTab(tab) {\n            document.querySelectorAll('.tab').forEach(t=>t.classList.remove('active'));\n            document.querySelectorAll('.card').forEach(c=>c.classList.remove('active'));\n            document.querySelectorAll('.tab')[['upload','download','text'].indexOf(tab)].classList.add('active');\n            document.getElementById('tab-'+tab).classList.add('active');\n            if(tab==='download') loadFiles();\n        }\n\n        function showToast(msg) { const t=document.getElementById('toast'); t.innerText=msg; t.classList.add('show'); setTimeout(()=>t.classList.remove('show'),2000); }\n\n        const dropZone = document.getElementById('drop-zone');\n        dropZone.addEventListener('dragover', (e) => { e.preventDefault(); dropZone.classList.add('dragover'); });\n        dropZone.addEventListener('dragleave', (e) => { e.preventDefault(); dropZone.classList.remove('dragover'); });\n        dropZone.addEventListener('drop', (e) => {\n            e.preventDefault(); dropZone.classList.remove('dragover');\n            \n            // \u5904\u7406\u62d6\u62fd\u6587\u4ef6\n            if (e.dataTransfer.files.length > 0) { handleFiles(e.dataTransfer.files); return; }\n            \n            // \u5904\u7406\u62d6\u62fd\u6587\u672c (\u81ea\u52a8\u751f\u6210 txt)\n            const text = e.dataTransfer.getData('text/plain');\n            if (text) {\n                const file = new File([text], \"\u62d6\u62fd\u6587\u672c_\" + Date.now() + \".txt\", {type: \"text/plain\"});\n                handleFiles([file]);\n                showToast('\u5df2\u5c06\u6587\u672c\u8f6c\u4e3a TXT \u53d1\u9001');\n            }\n        });\n\n        document.addEventListener('paste', (e) => {\n            const items = e.clipboardData.items;\n            for (let i = 0; i < items.length; i++) {\n                if (items[i].type.indexOf('image') !== -1) {\n                    const file = items[i].getAsFile();\n                    if(file) { handleFiles([file]); showToast('\u5df2\u7c98\u8d34\u56fe\u7247'); }\n                }\n            }\n        });\n\n        function getIcon(name) {\n            name = name.toLowerCase();\n            if (name.match(/\\.(jpg|jpeg|png|gif|webp)$/)) return '\ud83d\uddbc\ufe0f';\n            if (name.match(/\\.(mp4|mkv|avi)$/)) return '\ud83c\udfac';\n            if (name.match(/\\.(mp3|wav)$/)) return '\ud83c\udfb5';\n            if (name.match(/\\.(zip|rar|7z)$/)) return '\ud83d\udce6';\n            if (name.match(/\\.(apk)$/)) return '\ud83e\udd16';\n            if (name.match(/\\.(txt|md)$/)) return '\ud83d\udcdd';\n            return '\ud83d\udcc4';\n        }\n        function formatSize(b) { if(!b) return '0 B'; let k=1024,s=['B','KB','MB','GB'],i=Math.floor(Math.log(b)/Math.log(k)); return parseFloat((b/Math.pow(k,i)).toFixed(1))+' '+s[i]; }\n\n        async function handleFiles(files) {\n            if(!files || files.length===0) return;\n            const list = document.getElementById('upload-list');\n            let index = 0;\n            async function worker() {\n                while(index < files.length) {\n                    const i = index++;\n                    const f = files[i];\n                    const div = document.createElement('div'); div.className = 'file-item';\n                    div.innerHTML = `<div class=\"file-icon\">${getIcon(f.name)}</div><div style=\"flex:1; overflow:hidden;\"><div style=\"font-weight:bold; white-space:nowrap; text-overflow:ellipsis; overflow:hidden;\">${f.name}</div><div style=\"font-size:12px; color:var(--text-sec);\">${formatSize(f.size)}</div><div class=\"progress-bar\"><div class=\"progress-fill\" id=\"p-${i}\"></div></div></div><div id=\"s-${i}\" style=\"font-size:12px; font-weight:bold; color:var(--text-sec); margin-left:10px;\">\u7b49\u5f85</div>`;\n                    list.appendChild(div);\n                    \n                    await new Promise(resolve => {\n                        const xhr = new XMLHttpRequest();\n                        xhr.open('POST', '/web/upload?name=' + encodeURIComponent(f.name), true);\n                        xhr.upload.onprogress = e => { if(e.lengthComputable) { const p = Math.round((e.loaded/e.total)*100); document.getElementById('p-'+i).style.width = p+'%'; document.getElementById('s-'+i).innerText = p+'%'; } };\n                        xhr.onload = () => { document.getElementById('s-'+i).innerText = xhr.status===200?'\u5b8c\u6210':'\u5931\u8d25'; resolve(); };\n                        xhr.onerror = () => { document.getElementById('s-'+i).innerText = '\u51fa\u9519'; resolve(); };\n                        xhr.send(f);\n                    });\n                }\n            }\n            const ws = []; for(let w=0;w<Math.min(3, files.length);w++) ws.push(worker());\n            await Promise.all(ws);\n            showToast('\u4f20\u8f93\u5b8c\u6210\uff01');\n        }\n\n        function loadFiles() {\n            const list = document.getElementById('download-list');\n            list.innerHTML = '<p>\u52a0\u8f7d\u4e2d...</p>';\n            fetch('/web/files').then(r=>r.json()).then(data => {\n                if(data.length===0) { list.innerHTML = '<p>\u6682\u65e0\u6587\u4ef6</p>'; return; }\n                list.innerHTML = '';\n                data.forEach(f => {\n                    const div = document.createElement('div'); div.className = 'file-item';\n                    div.innerHTML = `<div class=\"file-icon\">${getIcon(f.name)}</div><div style=\"flex:1; overflow:hidden;\"><div style=\"font-weight:bold; white-space:nowrap; text-overflow:ellipsis; overflow:hidden;\">${f.name}</div><div style=\"font-size:12px; color:var(--text-sec);\">${formatSize(f.size)}</div></div><a href=\"/web/download?name=${encodeURIComponent(f.name)}\" class=\"btn\" style=\"text-decoration:none;\">\u4e0b\u8f7d</a>`;\n                    list.appendChild(div);\n                });\n            });\n        }\n\n        function sendText() {\n            const text = document.getElementById('text-input').value;\n            if(!text) return showToast('\u8bf7\u8f93\u5165\u6587\u672c');\n            fetch('/web/text', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ text }) })\n            .then(r => { if(r.ok) { showToast('\u5df2\u53d1\u9001\u5230\u624b\u673a\u526a\u8d34\u677f'); document.getElementById('text-input').value = ''; } });\n        }\n    </script>\n</body>\n</html>";

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
            return HttpResponse.json(info.toString());
        }
        
        // 剪贴板双向同步 GET
        if (req.path.equals("/web/clipboard") && req.method.equalsIgnoreCase("GET")) {
            try {
                ClipboardManager cm = (ClipboardManager) OldSendService.getContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                if (cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                    CharSequence text = cm.getPrimaryClip().getItemAt(0).coerceToText(OldSendService.getContext());
                    HttpResponse resp = new HttpResponse();
                    resp.contentType = "text/plain; charset=utf-8";
                    resp.body = (text != null ? text.toString() : "").getBytes("UTF-8");
                    return resp;
                }
            } catch (Exception e) {}
            return HttpResponse.json("null");
        }

        // 剪贴板双向同步 POST
        if (req.path.equals("/web/text") && req.method.equalsIgnoreCase("POST")) {
            String body = RegisterHandler.readAll(req);
            try {
                JSONObject obj = new JSONObject(body);
                String text = obj.optString("text", "");
                if (!text.isEmpty()) {
                    ClipboardManager cm = (ClipboardManager) OldSendService.getContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("OldSend", text));
                }
            } catch (Exception e) {}
            return HttpResponse.json("{\"status\":\"ok\"}");
        }
        
        // 文件上传接口
        if (req.path.equals("/web/upload") && req.method.equalsIgnoreCase("POST")) {
            Map<String, String> q = req.getQueryParams();
            String fileName = q.get("name");
            if (fileName == null) fileName = "web_" + System.currentTimeMillis();
            
            String filterStr = OldSendService.getPrefStringGlobal("filter_ext", "exe,bat,cmd,vbs,sh");
            if (filterStr.length() > 0) {
                String lower = fileName.toLowerCase();
                String[] exts = filterStr.split(",");
                for (String ext : exts) {
                    if (lower.endsWith("." + ext.trim())) {
                        OldSendService.log("Web拦截自定义后缀: " + fileName);
                        return HttpResponse.status(403);
                    }
                }
            }

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
            return HttpResponse.json("{\"status\":\"ok\"}");
        }
        
        // 反向索要文件列表
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
                    if (list != null) for (File f : list) if (f.isFile()) {
                        JSONObject obj = new JSONObject();
                        obj.put("name", f.getName()); obj.put("size", f.length()); arr.put(obj);
                    }
                }
            }
            return HttpResponse.json(arr.toString());
        }
        
        // 下载文件接口
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
        return HttpResponse.status(404);
    }
}
