package org.oldsend.api;

import java.util.Map;

import org.oldsend.http.HttpRequest;
import org.oldsend.http.HttpResponse;
import org.oldsend.http.Router;

public class CancelHandler implements Router.Handler {
    private final SessionStore store;

    public CancelHandler(SessionStore store) { this.store = store; }

    public HttpResponse handle(HttpRequest req) throws Exception {
        Map<String, String> q = req.getQueryParams();
        String sessionId = q.get("sessionId");
        if (sessionId != null) store.remove(sessionId);
        return HttpResponse.json("{}");
    }
}
