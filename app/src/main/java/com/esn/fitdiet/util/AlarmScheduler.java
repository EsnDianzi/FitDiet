package com.esn.fitdiet.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.esn.fitdiet.receiver.AlarmReceiver;

import java.util.Calendar;

/**
 * 每日 20:00 闹钟调度器。
 * 文档 §6.1：API>=31 且已授权 SCHEDULE_EXACT_ALARM 时用 setExactAndAllowWhileIdle，
 * 否则回退 setAndAllowWhileIdle。
 */
public final class AlarmScheduler {

    private static final int REQUEST_CODE = 2000;
    private static final int TRIGGER_HOUR = 20;
    private static final int TRIGGER_MINUTE = 0;

    private AlarmScheduler() { }

    public static void schedule(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, TRIGGER_HOUR);
        cal.set(Calendar.MINUTE, TRIGGER_MINUTE);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // 如果今天 20:00 已过，设为明天 20:00
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        long triggerAt = cal.getTimeInMillis();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+：优先精确闹钟，否则回退
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    public static void cancel(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
    }
}
