package org.oldsend;

import android.os.Handler;
import android.os.Looper;
import org.oldsend.util.FileLogger;

public class ReceiveManager {
    public interface DialogListener {
        void showConfirmDialog(String senderAlias, int fileCount, final Callback callback);
    }

    public interface Callback {
        void onResult(boolean accepted);
    }

    private static DialogListener listener;
    public static void setListener(DialogListener l) { listener = l; }

    public static boolean askUser(final String alias, final int count) {
        FileLogger.log("ReceiveManager", "Ask user to receive from: " + alias);
        if (listener == null) return true;

        final Object lock = new Object();
        final boolean[] result = new boolean[]{false};
        final boolean[] waiting = new boolean[]{true};

        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            public void run() {
                if (listener != null) {
                    listener.showConfirmDialog(alias, count, new Callback() {
                        public void onResult(boolean accepted) {
                            synchronized (lock) {
                                result[0] = accepted;
                                waiting[0] = false;
                                lock.notifyAll();
                            }
                        }
                    });
                } else {
                    synchronized (lock) {
                        result[0] = true;
                        waiting[0] = false;
                        lock.notifyAll();
                    }
                }
            }
        });

        synchronized (lock) {
            try {
                long start = System.currentTimeMillis();
                while (waiting[0]) {
                    lock.wait(30000);
                    if (System.currentTimeMillis() - start > 30000) break;
                }
            } catch (Exception ignored) {}
        }
        return result[0];
    }
}
