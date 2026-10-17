package org.oldsend.discovery;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.json.JSONObject;
import org.oldsend.OldSendService;
import org.oldsend.Protocol;
import org.oldsend.model.Device;
import org.oldsend.protocol.Identity;

public class MulticastScanner {

    public interface Callback {
        void onDevice(Device d);
        void onFinished();
    }

    public static void scanAsync(final Callback cb) {
        new Thread(new Runnable() {
            public void run() { doScan(cb); }
        }, "multicast-scan").start();
    }

    static void doScan(Callback cb) {
        DatagramSocket sock = null;
        try {
            sock = new DatagramSocket();
            sock.setSoTimeout(1000);
            sock.setBroadcast(true);

            JSONObject self = new JSONObject();
            self.put("alias", android.os.Build.MODEL);
            self.put("version", Protocol.VERSION);
            self.put("deviceModel", "Android " + android.os.Build.VERSION.RELEASE);
            self.put("deviceType", "mobile");
            self.put("fingerprint", Identity.getFingerprint());
            self.put("port", Protocol.DEFAULT_PORT);
            self.put("protocol", "http");
            self.put("download", false);

            byte[] data = self.toString().getBytes("UTF-8");
            InetAddress group = InetAddress.getByName(Protocol.MULTICAST_GROUP);
            DatagramPacket out = new DatagramPacket(data, data.length, group, Protocol.DEFAULT_PORT);
            sock.send(out);

            long deadline = System.currentTimeMillis() + 600;
            byte[] buf = new byte[65535];
            while (System.currentTimeMillis() < deadline) {
                try {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    sock.receive(p);
                    String body = new String(p.getData(), 0, p.getLength(), "UTF-8").trim();
                    if (body.length() == 0 || body.startsWith("[")) continue;
                    JSONObject o = new JSONObject(body);
                    Device d = Device.fromJson(o, p.getAddress().getHostAddress());
                    if (cb != null) cb.onDevice(d);
                } catch (java.net.SocketTimeoutException ste) {
                    break;
                } catch (Exception e) {
                    OldSendService.log("scan recv: " + e);
                }
            }
        } catch (Exception e) {
            OldSendService.log("scan error: " + e);
        } finally {
            try { if (sock != null) sock.close(); } catch (Exception ignored) {}
            if (cb != null) cb.onFinished();
        }
    }
}
