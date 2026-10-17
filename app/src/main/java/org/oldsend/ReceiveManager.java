package org.oldsend;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ReceiveManager {
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
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            public void run() {
                if (listener != null) {
                    listener.showConfirmDialog(alias, count, latch, result);
                } else {
                    latch.countDown();
                }
            }
        });

        try {
            // 等待用户点击，最多等 30 秒
            boolean released = latch.await(30, TimeUnit.SECONDS);
            return released && result[0];
        } catch (InterruptedException e) {
            return false;
        }
    }
}
