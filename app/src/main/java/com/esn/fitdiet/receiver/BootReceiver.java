package com.esn.fitdiet.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.esn.fitdiet.util.AlarmScheduler;

/**
 * 监听 BOOT_COMPLETED 和 MY_PACKAGE_REPLACED，重设每日 20:00 闹钟。
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            AlarmScheduler.schedule(context);
        }
    }
}
