package org.oldsend.model;

import java.util.ArrayList;
import java.util.List;

public class HistoryStore {
    public static class Record {
        public String fileName;
        public String path;
        public long size;
        public String time;
        public boolean isReceive;

        public Record(String name, String path, long size, String time, boolean isReceive) {
            this.fileName = name; this.path = path; this.size = size; this.time = time; this.isReceive = isReceive;
        }
    }

    private static final HistoryStore INSTANCE = new HistoryStore();
    public static HistoryStore get() { return INSTANCE; }

    private final List<Record> records = new ArrayList<Record>();

    public void add(Record r) {
        synchronized (records) {
            records.add(0, r);
            if (records.size() > 50) records.remove(records.size() - 1);
        }
    }

    public List<Record> snapshot() {
        synchronized (records) { return new ArrayList<Record>(records); }
    }
}
