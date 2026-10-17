package org.oldsend;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.concurrent.CountDownLatch;

public class NotificationReceiver extends BroadcastReceiver {
    public static final String ACTION_ACCEPT = "org.oldsend.ACCEPT";
    public static final String ACTION_REJECT = "org.oldsend.REJECT";

    public static CountDownLatch waitingLatch = null;
    public static boolean[] waitingResult = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (waitingLatch == null || waitingResult == null) return;
        
        String action = intent.getAction();
        if (ACTION_ACCEPT.equals(action)) {
            waitingResult[0] = true;
            waitingLatch.countDown();
        } else if (ACTION_REJECT.equals(action)) {
            waitingResult[0] = false;
            waitingLatch.countDown();
        }
    }
}
