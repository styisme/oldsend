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
        // 过滤虚拟网卡
        if (d.ip != null && d.ip.startsWith("172.17.")) {
            return;
        }
        
        boolean changed = false;
        synchronized (devices) {
            Iterator<Device> it = devices.iterator();
            while (it.hasNext()) {
                Device e = it.next();
                boolean sameIp = d.ip != null && d.ip.equals(e.ip) && d.port == e.port;
                boolean sameFp = d.fingerprint != null && d.fingerprint.length() > 0 && d.fingerprint.equals(e.fingerprint);
                
                // 只要 IP 端口相同，或者指纹相同，就认为是同一个设备，直接移除旧记录
                if (sameIp || sameFp) {
                    it.remove();
                    changed = true;
                }
            }
            devices.add(d);
            if (devices.size() > 50) {
                devices.remove(0);
            }
            changed = true;
        }
        
        if (changed) notifyChanged();
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