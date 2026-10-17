package org.oldsend.api;

import org.json.JSONObject;
import org.oldsend.Protocol;
import org.oldsend.http.HttpRequest;
import org.oldsend.http.HttpResponse;
import org.oldsend.http.Router;
import org.oldsend.protocol.Identity;

public class InfoHandler implements Router.Handler {
    private final String alias;

    public InfoHandler(String alias) { this.alias = alias; }

    public HttpResponse handle(HttpRequest req) throws Exception {
        return HttpResponse.json(buildSelfJson(alias));
    }

    public static String buildSelfJson(String alias) {
        try {
            JSONObject o = new JSONObject();
            o.put("alias", alias);
            o.put("version", Protocol.VERSION);
            o.put("deviceModel", "Android " + android.os.Build.VERSION.RELEASE);
            o.put("deviceType", "mobile");
            o.put("fingerprint", Identity.getFingerprint());
            o.put("port", Protocol.DEFAULT_PORT);
            o.put("protocol", "http");
            o.put("download", false);
            return o.toString();
        } catch (Exception e) {
            return "{}";
        }
    }
}
