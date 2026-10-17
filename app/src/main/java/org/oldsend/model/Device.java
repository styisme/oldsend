package org.oldsend.model;

import org.json.JSONObject;

public class Device {
    public String alias;
    public String version;
    public String deviceModel;
    public String deviceType;
    public String fingerprint;
    public String ip;
    public int port;
    public boolean https;
    public boolean download;

    public static Device fromJson(JSONObject o, String ip) {
        Device d = new Device();
        d.ip = ip;
        d.alias = o.optString("alias", "unknown");
        d.version = o.optString("version", "");
        d.deviceModel = o.optString("deviceModel", "");
        d.deviceType = o.optString("deviceType", "mobile");
        d.fingerprint = o.optString("fingerprint", "");
        d.port = o.optInt("port", 53317);
        String proto = o.optString("protocol", "http");
        d.https = "https".equalsIgnoreCase(proto);
        d.download = o.optBoolean("download", false);
        return d;
    }

    public String url(String path) {
        return (https ? "https" : "http") + "://" + ip + ":" + port + path;
    }

    @Override
    public String toString() {
        return alias + " @ " + ip + ":" + port;
    }
}
