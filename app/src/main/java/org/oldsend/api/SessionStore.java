package org.oldsend.api;

import java.util.HashMap;
import java.util.Map;

public class SessionStore {
    private final Map<String, UploadSession> sessions = new HashMap<String, UploadSession>();

    public UploadSession create(String peerIp) {
        String id = Long.toHexString(System.currentTimeMillis())
                + Long.toHexString(Double.doubleToLongBits(Math.random()));
        UploadSession s = new UploadSession(id, peerIp);
        synchronized (sessions) { sessions.put(id, s); }
        return s;
    }

    public UploadSession get(String id) {
        synchronized (sessions) { return sessions.get(id); }
    }

    public void remove(String id) {
        synchronized (sessions) { sessions.remove(id); }
    }
}
