package org.oldsend.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    public String method;
    public String path;
    public String query;
    public Map<String, String> headers = new HashMap<String, String>();
    public InputStream body;
    public long contentLength;
    public boolean chunked;
    public InetAddress remoteAddress;

    public static HttpRequest parse(InputStream in, InetAddress remote) throws IOException {
        HttpRequest r = new HttpRequest();
        r.remoteAddress = remote;

        String line = readLine(in);
        if (line == null || line.length() == 0) throw new IOException("empty request");
        String[] parts = line.split(" ");
        if (parts.length < 2) throw new IOException("bad request line");
        r.method = parts[0];
        String url = parts[1];
        int q = url.indexOf('?');
        if (q >= 0) {
            r.path = url.substring(0, q);
            r.query = url.substring(q + 1);
        } else {
            r.path = url;
            r.query = "";
        }

        while ((line = readLine(in)) != null && line.length() > 0) {
            int i = line.indexOf(':');
            if (i > 0) {
                r.headers.put(line.substring(0, i).trim().toLowerCase(),
                              line.substring(i + 1).trim());
            }
        }

        String cl = r.headers.get("content-length");
        if (cl != null) {
            try { r.contentLength = Long.parseLong(cl); } catch (Exception e) { r.contentLength = 0; }
        }
        String te = r.headers.get("transfer-encoding");
        r.chunked = (te != null && te.toLowerCase().indexOf("chunked") >= 0);
        r.body = in;
        return r;
    }

    public static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder(128);
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') {
                int len = sb.length();
                if (len > 0 && sb.charAt(len - 1) == '\r') sb.setLength(len - 1);
                return sb.toString();
            }
            sb.append((char) c);
            if (sb.length() > 8192) throw new IOException("line too long");
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    public Map<String, String> getQueryParams() {
        Map<String, String> m = new HashMap<String, String>();
        if (query == null || query.length() == 0) return m;
        String[] pairs = query.split("&");
        for (int i = 0; i < pairs.length; i++) {
            String p = pairs[i];
            int j = p.indexOf('=');
            try {
                if (j > 0) {
                    m.put(URLDecoder.decode(p.substring(0, j), "UTF-8"),
                          URLDecoder.decode(p.substring(j + 1), "UTF-8"));
                } else {
                    m.put(URLDecoder.decode(p, "UTF-8"), "");
                }
            } catch (Exception ignored) {}
        }
        return m;
    }
}
