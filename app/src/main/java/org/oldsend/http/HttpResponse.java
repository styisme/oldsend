package org.oldsend.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HttpResponse {
    public int status = 200;
    public String contentType = "application/json; charset=utf-8";
    public byte[] body = new byte[0];
    public Map<String, String> headers = new HashMap<String, String>();

    public static HttpResponse json(String s) {
        HttpResponse r = new HttpResponse();
        try { r.body = s.getBytes("UTF-8"); } catch (Exception e) {}
        return r;
    }

    public static HttpResponse status(int code) {
        HttpResponse r = new HttpResponse();
        r.status = code;
        return r;
    }

    public void writeTo(OutputStream out) throws IOException {
        out.write(("HTTP/1.1 " + status + " " + reasonOf(status) + "\r\n").getBytes("UTF-8"));
        out.write(("Content-Type: " + contentType + "\r\n").getBytes("UTF-8"));
        out.write(("Content-Length: " + body.length + "\r\n").getBytes("UTF-8"));
        Iterator<Map.Entry<String, String>> it = headers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            out.write((e.getKey() + ": " + e.getValue() + "\r\n").getBytes("UTF-8"));
        }
        out.write("Connection: close\r\n\r\n".getBytes("UTF-8"));
        out.write(body);
    }

    private static String reasonOf(int status) {
        if (status == 200) return "OK";
        if (status == 204) return "No Content";
        if (status == 400) return "Bad Request";
        if (status == 403) return "Forbidden";
        if (status == 404) return "Not Found";
        if (status == 405) return "Method Not Allowed";
        if (status == 500) return "Internal Server Error";
        if (status == 501) return "Not Implemented";
        return "Unknown";
    }
}
