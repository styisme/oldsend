package org.oldsend;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import org.oldsend.api.*;
import org.oldsend.api.WebShareHandler;
import org.oldsend.discovery.MulticastListener;
import org.oldsend.http.HttpServer;
import org.oldsend.http.Router;
import org.oldsend.protocol.Identity;

public class OldSendService extends Service {
    public interface LogSink { void log(String msg); }
    private static LogSink sink;
    public static void setSink(LogSink s) { sink = s; }
    public static void log(String msg) { LogSink s = sink; if (s != null) s.log(msg); }

    private HttpServer httpServer;
    private MulticastListener multicastListener;
    private WifiManager.MulticastLock multicastLock;

    public static boolean getPrefBooleanGlobal(String key, boolean def) {
        Context ctx = getContext();
        if (ctx == null) return def;
        return ctx.getSharedPreferences("oldsend", Context.MODE_PRIVATE).getBoolean(key, def);
    }
    
    public static String getPin() {
        Context ctx = getContext();
        if (ctx == null) return "0000";
        return ctx.getSharedPreferences("oldsend", Context.MODE_PRIVATE).getString("pin_code", "1234");
    }

    private static Context appContext;
    public static Context getContext() { return appContext; }
    @Override public void onCreate() { super.onCreate(); appContext = this; Identity.init(getApplicationContext()); startForegroundNotification(); }
    
    public static String getAlias(Context ctx) {
        if (ctx == null) return Build.MODEL;
        SharedPreferences sp = ctx.getSharedPreferences("oldsend", Context.MODE_PRIVATE);
        return sp.getString("alias", Build.MODEL);
    }

    public static void setAlias(Context ctx, String name) {
        SharedPreferences sp = ctx.getSharedPreferences("oldsend", Context.MODE_PRIVATE);
        sp.edit().putString("alias", name).commit();
    }

    public static String getLocalIp() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (ni.isLoopback() || !ni.isUp()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (!a.isLoopbackAddress() && a.getHostAddress().indexOf(':') < 0) return a.getHostAddress();
                }
            }
        } catch (Exception ignored) {}
        return "0.0.0.0";
    }

    @Override public IBinder onBind(Intent intent) { return null; }
    
    private void startForegroundNotification() {
        String channelId = "oldsend_channel";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(channelId, "OldSend后台服务", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this);
        }
        builder.setContentTitle("OldSend 正在运行")
               .setContentText("局域网文件传输已就绪，点击返回")
               .setSmallIcon(android.R.drawable.stat_sys_download_done)
               .setContentIntent(pi);

        startForeground(1, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (httpServer != null) return START_STICKY;
        try {
            if (Build.VERSION.SDK_INT >= 12) {
                try {
                    WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                    multicastLock = wm.createMulticastLock("oldsend");
                    multicastLock.setReferenceCounted(false);
                    multicastLock.acquire();
                } catch (Exception e) { log("multicast lock: " + e); }
            }
            String alias = getAlias(this);
            SessionStore store = new SessionStore();
            Router router = new Router();
            router.register(Protocol.V2_PREFIX + "/info", new InfoHandler(alias));
            router.register(Protocol.V2_PREFIX + "/register", new RegisterHandler(alias));
            router.register(Protocol.V2_PREFIX + "/prepare-upload", new PrepareUploadHandler(store));
            router.register(Protocol.V2_PREFIX + "/upload", new UploadHandler(store));
            router.register(Protocol.V2_PREFIX + "/cancel", new CancelHandler(store));
            router.register("/", new WebShareHandler());
            router.register("/index.html", new WebShareHandler());
            router.register("/web-upload", new WebShareHandler());

            boolean useHttps = getSharedPreferences("oldsend", MODE_PRIVATE).getBoolean("use_https", false);
            javax.net.ssl.SSLContext sslContext = useHttps ? TlsHelper.getServerSSLContext(this) : null;
            httpServer = new HttpServer(Protocol.DEFAULT_PORT, router, sslContext);
            httpServer.start();
            log((useHttps ? "HTTPS" : "HTTP") + " 服务已启动，端口: " + Protocol.DEFAULT_PORT);

            multicastListener = new MulticastListener(Protocol.DEFAULT_PORT, alias);
            multicastListener.start();
            log("组播服务已启动: " + Protocol.MULTICAST_GROUP + ":" + Protocol.DEFAULT_PORT);
            log("本机IP: " + getLocalIp());
        } catch (Exception e) { log("启动失败: " + e); }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try { if (httpServer != null) httpServer.stop(); } catch (Exception ignored) {}
        try { if (multicastListener != null) multicastListener.stop(); } catch (Exception ignored) {}
        try { if (multicastLock != null) multicastLock.release(); } catch (Exception ignored) {}
        httpServer = null; multicastListener = null;
        log("服务已停止");
        super.onDestroy();
    }
}
