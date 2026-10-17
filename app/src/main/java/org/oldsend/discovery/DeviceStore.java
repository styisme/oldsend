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
        // 过滤掉 172.17.x.x 这种虚拟网卡 IP，避免发送失败
        if (d.ip != null && d.ip.startsWith("172.17.")) {
            return;
        }
        synchronized (devices) {
            for (Iterator<Device> it = devices.iterator(); it.hasNext();) {
                Device e = it.next();
                boolean same = false;
                if (d.fingerprint != null && d.fingerprint.length() > 0
                        && d.fingerprint.equals(e.fingerprint)) {
                    // 如果是同一个设备（指纹相同），但新 IP 是 192.168 等真实局域网段，则更新
                    if (d.ip != null && (d.ip.startsWith("192.168.") || d.ip.startsWith("10."))) {
                        same = true;
                        break;
                    }
                } else if (d.ip != null && d.ip.equals(e.ip)) {
                    same = true;
                    break;
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
