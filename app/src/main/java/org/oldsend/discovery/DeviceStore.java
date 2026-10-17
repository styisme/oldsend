package org.oldsend.discovery;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.oldsend.model.Device;

public class DeviceStore {
    private static final DeviceStore INSTANCE = new DeviceStore();
    public static DeviceStore get() { return INSTANCE; }

    private final List<Device> devices = new ArrayList<Device>();
    private Listener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Listener {
        void onChanged();
    }

    public void setListener(Listener l) { listener = l; }

    public void addDevice(Device d) {
        synchronized (devices) {
            for (Iterator<Device> it = devices.iterator(); it.hasNext();) {
                Device e = it.next();
                boolean same = false;
                if (d.fingerprint != null && d.fingerprint.length() > 0
                        && d.fingerprint.equals(e.fingerprint)) {
                    same = true;
                } else if (d.ip != null && d.ip.equals(e.ip)) {
                    same = true;
                }
                if (same) {
                    it.remove();
                    break;
                }
            }
            devices.add(d);
            if (devices.size() > 50) devices.remove(0);
        }
        notifyChanged();
    }

    public List<Device> snapshot() {
        synchronized (devices) {
            return new ArrayList<Device>(devices);
        }
    }

    private void notifyChanged() {
        final Listener l = listener;
        if (l == null) return;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            l.onChanged();
        } else {
            mainHandler.post(new Runnable() {
                public void run() { l.onChanged(); }
            });
        }
    }
}
