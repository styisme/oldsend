package org.oldsend;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            boolean autoStart = context.getSharedPreferences("oldsend", Context.MODE_PRIVATE).getBoolean("auto_start", false);
            if (autoStart) {
                context.startService(new Intent(context, OldSendService.class));
            }
        }
    }
}
