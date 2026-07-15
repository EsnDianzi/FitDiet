package com.esn.fitdiet.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.esn.fitdiet.worker.DailySummaryWorker;

/**
 * 接收每日 20:00 闹钟，触发 DailySummaryWorker 聚合当日数据。
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build();

        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(DailySummaryWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork("daily-summary", ExistingWorkPolicy.REPLACE, work);
    }
}
