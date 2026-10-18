package org.oldsend.model;

import org.json.JSONObject;

public class Device implements java.io.Serializable {
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
        String formatIp = ip;
        if (formatIp == null) formatIp = "";
        
        // 去除可能已经带有的方括号
        if (formatIp.startsWith("[")) formatIp = formatIp.substring(1);
        if (formatIp.endsWith("]")) formatIp = formatIp.substring(0, formatIp.length() - 1);

        // 如果包含冒号，说明是 IPv6，必须用方括号包裹
        if (formatIp.contains(":")) {
            formatIp = "[" + formatIp + "]";
        }
        return (https ? "https" : "http") + "://" + formatIp + ":" + port + path;
    }

    @Override
    public String toString() {
        return alias + " @ " + ip + ":" + port;
    }
}
