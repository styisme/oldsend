package org.oldsend.http;

import java.util.HashMap;
import java.util.Map;

import org.oldsend.OldSendService;

public class Router {
    public interface Handler {
        HttpResponse handle(HttpRequest req) throws Exception;
    }

    private final Map<String, Handler> routes = new HashMap<String, Handler>();

    public void register(String path, Handler h) {
        routes.put(path, h);
    }

    public HttpResponse dispatch(HttpRequest req) {
        Handler h = routes.get(req.path);
        if (h == null) {
            OldSendService.log("404 " + req.method + " " + req.path);
            return HttpResponse.status(404);
        }
        try {
            return h.handle(req);
        } catch (Exception e) {
            OldSendService.log("handler error " + req.path + ": " + e);
            return HttpResponse.status(500);
        }
    }
}
