package org.oldsend.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UploadSession {
    public static class FileEntry {
        public final String fileId;
        public final String fileName;
        public final long size;
        public final String token;
        public FileEntry(String id, String name, long s, String t) {
            fileId = id; fileName = name; size = s; token = t;
        }
    }

    public final String sessionId;
    public final String peerIp;
    private final Map<String, FileEntry> files = new HashMap<String, FileEntry>();
    private final Set<String> done = new HashSet<String>();

    public UploadSession(String id, String peer) {
        this.sessionId = id;
        this.peerIp = peer;
    }

    public void addFile(String fileId, String fileName, long size, String token) {
        files.put(fileId, new FileEntry(fileId, fileName, size, token));
    }

    public FileEntry getFile(String fileId) { return files.get(fileId); }

    public void markDone(String fileId) {
        synchronized (done) { done.add(fileId); }
    }

    public boolean allDone() {
        synchronized (done) { return done.size() >= files.size(); }
    }
}
