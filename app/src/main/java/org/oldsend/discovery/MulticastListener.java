package org.oldsend.discovery;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;

import org.json.JSONObject;
import org.oldsend.OldSendService;
import org.oldsend.Protocol;
import org.oldsend.api.InfoHandler;
import org.oldsend.model.Device;

public class MulticastListener {
    private final int port;
    private final String alias;
    private MulticastSocket socket;
    private volatile boolean running;

    public MulticastListener(int port, String alias) {
        this.port = port;
        this.alias = alias;
    }

    public void start() throws Exception {
        socket = new MulticastSocket(null);
        // 关键：允许重用端口
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(port));

        InetAddress group = InetAddress.getByName(Protocol.MULTICAST_GROUP);
        socket.joinGroup(group);
        running = true;
        Thread t = new Thread(new Runnable() {
            public void run() { loop(); }
        }, "multicast-listen");
        t.setDaemon(true);
        t.start();
    }

    private void loop() {
        byte[] buf = new byte[65535];
        while (running) {
            try {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                socket.receive(p);
                handle(p);
            } catch (Exception e) {
                if (running) OldSendService.log("multicast recv: " + e.getMessage());
            }
        }
    }

    private void handle(DatagramPacket p) {
        try {
            String body = new String(p.getData(), 0, p.getLength(), "UTF-8").trim();
            if (body.length() == 0 || body.startsWith("[")) return;
            JSONObject o = new JSONObject(body);
            String peerIp = p.getAddress().getHostAddress();
            Device d = Device.fromJson(o, peerIp);
            DeviceStore.get().addDevice(d);
            OldSendService.log("multicast from " + peerIp + " (" + d.alias + ")");

            JSONObject self = new JSONObject(InfoHandler.buildSelfJson(alias));
            byte[] data = self.toString().getBytes("UTF-8");
            DatagramPacket out = new DatagramPacket(data, data.length, p.getAddress(), p.getPort());
            socket.send(out);
        } catch (Exception e) {
            OldSendService.log("multicast handle: " + e);
        }
    }

    public void stop() {
        running = false;
        try { socket.leaveGroup(InetAddress.getByName(Protocol.MULTICAST_GROUP)); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }
}
