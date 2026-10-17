package org.oldsend.api;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.json.JSONObject;
import org.oldsend.OldSendService;
import org.oldsend.discovery.DeviceStore;
import org.oldsend.http.HttpRequest;
import org.oldsend.http.HttpResponse;
import org.oldsend.http.Router;
import org.oldsend.model.Device;

public class RegisterHandler implements Router.Handler {
    private final String alias;

    public RegisterHandler(String alias) { this.alias = alias; }

    public HttpResponse handle(HttpRequest req) throws Exception {
        if ("POST".equalsIgnoreCase(req.method)) {
            try {
                String body = readAll(req);
                if (body.length() > 0) {
                    JSONObject o = new JSONObject(body);
                    String peerIp = req.remoteAddress.getHostAddress();
                    Device d = Device.fromJson(o, peerIp);
                    DeviceStore.get().addDevice(d);
                    OldSendService.log("register: " + d.alias + " @ " + peerIp);
                }
            } catch (Exception e) {
                OldSendService.log("register parse: " + e);
            }
        }
        return HttpResponse.json(InfoHandler.buildSelfJson(alias));
    }

    public static String readAll(HttpRequest req) throws Exception {
        if (req.chunked) return readChunked(req.body);
        if (req.contentLength <= 0) return "";
        byte[] buf = new byte[(int) req.contentLength];
        int off = 0;
        while (off < buf.length) {
            int n = req.body.read(buf, off, buf.length - off);
            if (n < 0) break;
            off += n;
        }
        return new String(buf, 0, off, "UTF-8");
    }

    static String readChunked(InputStream in) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        while (true) {
            String line = HttpRequest.readLine(in);
            if (line == null) break;
            int size = Integer.parseInt(line.trim(), 16);
            if (size == 0) {
                HttpRequest.readLine(in);
                break;
            }
            byte[] buf = new byte[size];
            int off = 0;
            while (off < size) {
                int n = in.read(buf, off, size - off);
                if (n < 0) break;
                off += n;
            }
            bo.write(buf, 0, off);
            HttpRequest.readLine(in);
        }
        return new String(bo.toByteArray(), "UTF-8");
    }
}
