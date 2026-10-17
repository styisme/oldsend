package org.oldsend.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.oldsend.OldSendService;

public class HttpServer {
    private final int port;
    private final Router router;
    private ServerSocket server;
    private volatile boolean running;

    public HttpServer(int port, Router router) {
        this.port = port;
        this.router = router;
    }

    public void start() throws IOException {
        running = true;
        server = new ServerSocket(port);
        Thread t = new Thread(new Runnable() {
            public void run() {
                while (running) {
                    try {
                        final Socket s = server.accept();
                        Thread th = new Thread(new Runnable() {
                            public void run() { handle(s); }
                        }, "http-conn");
                        th.setDaemon(true);
                        th.start();
                    } catch (IOException e) {
                        if (running) OldSendService.log("accept: " + e.getMessage());
                    }
                }
            }
        }, "http-accept");
        t.setDaemon(true);
        t.start();
    }

    private void handle(Socket s) {
        try {
            s.setSoTimeout(300000);
            InputStream in = new BufferedInputStream(s.getInputStream(), 8192);
            OutputStream out = new BufferedOutputStream(s.getOutputStream(), 8192);
            HttpRequest req = HttpRequest.parse(in, s.getInetAddress());
            HttpResponse resp = router.dispatch(req);
            resp.writeTo(out);
            out.flush();
        } catch (Exception e) {
            OldSendService.log("conn error: " + e);
            try {
                OutputStream o = s.getOutputStream();
                o.write("HTTP/1.1 500 Internal Server Error\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes("UTF-8"));
                o.flush();
            } catch (Exception ignored) {}
        } finally {
            try { s.close(); } catch (Exception ignored) {}
        }
    }

    public void stop() {
        running = false;
        try { if (server != null) server.close(); } catch (Exception ignored) {}
    }
}
