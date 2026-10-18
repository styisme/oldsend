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
import org.oldsend.model.HistoryStore;

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
    
    public static String getPrefStringGlobal(String key, String def) {
        Context ctx = getContext();
        if (ctx == null) return def;
        return ctx.getSharedPreferences("oldsend", Context.MODE_PRIVATE).getString(key, def);
    }
    
    public static String getPin() {
        Context ctx = getContext();
        if (ctx == null) return "0000";
        return ctx.getSharedPreferences("oldsend", Context.MODE_PRIVATE).getString("pin_code", "1234");
    }

    private static Context appContext;
    public static Context getContext() { return appContext; }
    @Override public void onCreate() { super.onCreate(); appContext = this; Identity.init(getApplicationContext()); HistoryStore.get().init(this); startForegroundNotification(); }
    
    public static void showReceiveNotification(String alias, int count) {
        Context ctx = getContext();
        if (ctx == null) return;
        android.app.NotificationManager nm = (android.app.NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        String channelId = "oldsend_receive";
        if (Build.VERSION.SDK_INT >= 26) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, "接收文件", android.app.NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }

        Intent acceptIntent = new Intent(ctx, NotificationReceiver.class);
        acceptIntent.setAction(NotificationReceiver.ACTION_ACCEPT);
        android.app.PendingIntent acceptPi = android.app.PendingIntent.getBroadcast(ctx, 1, acceptIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT);

        Intent rejectIntent = new Intent(ctx, NotificationReceiver.class);
        rejectIntent.setAction(NotificationReceiver.ACTION_REJECT);
        android.app.PendingIntent rejectPi = android.app.PendingIntent.getBroadcast(ctx, 2, rejectIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT);

        android.app.Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            b = new android.app.Notification.Builder(ctx, channelId);
        } else {
            b = new android.app.Notification.Builder(ctx);
        }
        b.setSmallIcon(android.R.drawable.stat_sys_download)
         .setContentTitle("接收文件请求")
         .setContentText("【" + alias + "】请求发送 " + count + " 个文件")
         .setAutoCancel(true)
         .addAction(new android.app.Notification.Action.Builder(android.R.drawable.ic_input_add, "接收", acceptPi).build())
         .addAction(new android.app.Notification.Action.Builder(android.R.drawable.ic_delete, "拒绝", rejectPi).build());

        nm.notify(100, b.build());
    }

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
        // 默认优先返回 IPv4 用于局域网组播
        String v4 = getIpv4();
        if (v4 != null) return v4;
        String v6 = getIpv6();
        if (v6 != null) return v6;
        return "0.0.0.0";
    }

    public static String getIpv4() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (ni.isLoopback() || !ni.isUp()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (!a.isLoopbackAddress() && a.getHostAddress().indexOf(':') < 0) return a.getHostAddress();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static String getIpv6() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (ni.isLoopback() || !ni.isUp()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (!a.isLoopbackAddress() && a.getHostAddress().indexOf(':') >= 0) {
                        String ip = a.getHostAddress();
                        if (!ip.startsWith("fe80")) return ip;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
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
        if (httpServer != null && multicastListener != null) {
            log("服务已在运行中");
            return START_STICKY;
        }
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
            // 兼容 V2
            router.register(Protocol.V2_PREFIX + "/info", new InfoHandler(alias));
            router.register(Protocol.V2_PREFIX + "/register", new RegisterHandler(alias));
            router.register(Protocol.V2_PREFIX + "/prepare-upload", new PrepareUploadHandler(store));
            router.register(Protocol.V2_PREFIX + "/upload", new UploadHandler(store));
            router.register(Protocol.V2_PREFIX + "/cancel", new CancelHandler(store));
            
            // 兼容老设备 V1
            String V1_PREFIX = "/api/localsend/v1";
            router.register(V1_PREFIX + "/info", new InfoHandler(alias));
            router.register(V1_PREFIX + "/register", new RegisterHandler(alias));
            router.register(V1_PREFIX + "/prepare-upload", new PrepareUploadHandler(store));
            router.register(V1_PREFIX + "/upload", new UploadHandler(store));
            router.register(V1_PREFIX + "/cancel", new CancelHandler(store));
            router.register("/", new WebShareHandler());
            router.register("/index.html", new WebShareHandler());
            router.register("/web-upload", new WebShareHandler());

            boolean useHttps = getSharedPreferences("oldsend", MODE_PRIVATE).getBoolean("use_https", false);
            javax.net.ssl.SSLContext sslContext = useHttps ? TlsHelper.getServerSSLContext(this) : null;
            int port = getSharedPreferences("oldsend", MODE_PRIVATE).getInt("port", Protocol.DEFAULT_PORT);
            httpServer = new HttpServer(port, router, sslContext);
            httpServer.start();
            log((useHttps ? "HTTPS" : "HTTP") + " 服务已启动，端口: " + port);

            multicastListener = new MulticastListener(port, alias);
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
