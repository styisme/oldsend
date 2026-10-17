package org.oldsend;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

import org.oldsend.api.CancelHandler;
import org.oldsend.api.InfoHandler;
import org.oldsend.api.PrepareUploadHandler;
import org.oldsend.api.RegisterHandler;
import org.oldsend.api.SessionStore;
import org.oldsend.api.UploadHandler;
import org.oldsend.discovery.MulticastListener;
import org.oldsend.http.HttpServer;
import org.oldsend.http.Router;
import org.oldsend.protocol.Identity;

public class OldSendService extends Service {

    public interface LogSink {
        void log(String msg);
    }

    private static LogSink sink;
    public static void setSink(LogSink s) { sink = s; }
    public static void log(String msg) {
        LogSink s = sink;
        if (s != null) s.log(msg);
    }

    private HttpServer httpServer;
    private MulticastListener multicastListener;
    private WifiManager.MulticastLock multicastLock;

    public static String getLocalIp() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (ni.isLoopback() || !ni.isUp()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (!a.isLoopbackAddress() && a.getHostAddress().indexOf(':') < 0) {
                        return a.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "0.0.0.0";
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        Identity.init(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (httpServer != null) return START_STICKY;
        try {
            acquireMulticastLock();

            String alias = Build.MODEL;
            SessionStore store = new SessionStore();

            Router router = new Router();
            router.register(Protocol.V2_PREFIX + "/info", new InfoHandler(alias));
            router.register(Protocol.V2_PREFIX + "/register", new RegisterHandler(alias));
            router.register(Protocol.V2_PREFIX + "/prepare-upload", new PrepareUploadHandler(store));
            router.register(Protocol.V2_PREFIX + "/upload", new UploadHandler(store));
            router.register(Protocol.V2_PREFIX + "/cancel", new CancelHandler(store));

            httpServer = new HttpServer(Protocol.DEFAULT_PORT, router);
            httpServer.start();
            log("HTTP 服务已启动，端口: " + Protocol.DEFAULT_PORT);

            multicastListener = new MulticastListener(Protocol.DEFAULT_PORT, alias);
            multicastListener.start();
            log("组播服务已启动: " + Protocol.MULTICAST_GROUP + ":" + Protocol.DEFAULT_PORT);
            log("本机IP: " + getLocalIp());
        } catch (Exception e) {
            log("启动失败: " + e);
        }
        return START_STICKY;
    }

    private void acquireMulticastLock() {
        if (Build.VERSION.SDK_INT >= 12) {
            try {
                WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                multicastLock = wm.createMulticastLock("oldsend");
                multicastLock.setReferenceCounted(false);
                multicastLock.acquire();
            } catch (Exception e) {
                log("multicast lock: " + e);
            }
        }
    }

    @Override
    public void onDestroy() {
        try { if (httpServer != null) httpServer.stop(); } catch (Exception ignored) {}
        try { if (multicastListener != null) multicastListener.stop(); } catch (Exception ignored) {}
        try { if (multicastLock != null) multicastLock.release(); } catch (Exception ignored) {}
        httpServer = null;
        multicastListener = null;
        log("服务已停止");
        super.onDestroy();
    }
}
