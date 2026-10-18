package org.oldsend;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ReceiveManager {
    public static final String ACTION_ACCEPT = "org.oldsend.RM_ACCEPT";
    public static final String ACTION_REJECT = "org.oldsend.RM_REJECT";

    public interface DialogListener {
        void showConfirmDialog(String senderAlias, int fileCount, CountDownLatch latch, boolean[] result);
    }

    private static DialogListener listener;
    public static void setListener(DialogListener l) { listener = l; }

    public static boolean askUser(final String alias, final int count) {
        if (listener == null) {
            OldSendService.log("后台服务未绑定界面，直接接收");
            return true;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = new boolean[]{false};

        boolean useNotif = OldSendService.getPrefBooleanGlobal("receive_notif", false);
        if (useNotif && android.os.Build.VERSION.SDK_INT >= 16) {
            NotificationReceiver.waitingLatch = latch;
            NotificationReceiver.waitingResult = result;
            OldSendService.showReceiveNotification(alias, count);
            try {
                boolean released = latch.await(30, TimeUnit.SECONDS);
                NotificationReceiver.waitingLatch = null;
                NotificationReceiver.waitingResult = null;
                return released && result[0];
            } catch (InterruptedException e) {
                return false;
            }
        }

        // 如果不是通知栏模式，启动全新的全屏 UI Activity
        Context ctx = OldSendService.getContext();
        Intent intent = new Intent(ctx, TransferActivity.class);
        intent.putExtra("mode", 1);
        intent.putExtra("senderName", alias);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);

        // 注册一个临时广播接收器，等待 Activity 里的按钮点击事件
        final String[] res = new String[]{"wait"};
        BroadcastReceiver br = new BroadcastReceiver() {
            public void onReceive(Context c, Intent intent) {
                if (ACTION_ACCEPT.equals(intent.getAction())) res[0] = "accept";
                if (ACTION_REJECT.equals(intent.getAction())) res[0] = "reject";
            }
        };
        ctx.registerReceiver(br, new IntentFilter(ACTION_ACCEPT));
        ctx.registerReceiver(br, new IntentFilter(ACTION_REJECT));

        try {
            // 等待用户点击，最多等 30 秒
            long start = System.currentTimeMillis();
            while (res[0].equals("wait") && System.currentTimeMillis() - start < 30000) {
                try { Thread.sleep(100); } catch (Exception ignored) {}
            }
        } finally {
            ctx.unregisterReceiver(br);
        }

        return "accept".equals(res[0]);
    }
}