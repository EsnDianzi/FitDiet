package com.esn.fitdiet.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.esn.fitdiet.data.local.AppDatabase;
import com.esn.fitdiet.data.local.entity.DailySummary;
import com.esn.fitdiet.data.repository.SummaryRepository;
import com.esn.fitdiet.util.AppExecutors;
import com.esn.fitdiet.util.DateUtil;

/**
 * 聚合当日 FoodLog 与 ExerciseLog，生成或更新 DailySummary。
 * 由 AlarmReceiver（每日 20:00）或 SummaryIntegrityChecker（回填）触发。
 *
 * <p>聚合逻辑收敛于 {@link SummaryRepository#generateForDate}（方案 §1.3 / §3.4），
 * Worker 仅负责触发与线程环境。
 */
public class DailySummaryWorker extends Worker {

    private static final String TAG = "DailySummaryWorker";

    public DailySummaryWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            String date = DateUtil.today();
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            SummaryRepository repo = new SummaryRepository(
                    db.dailySummaryDao(), db.foodLogDao(), db.exerciseLogDao(),
                    db.userProfileDao(), AppExecutors.getInstance());

            DailySummary summary = repo.generateForDate(date);
            Log.d(TAG, "summary generated for " + date + " bmr=" + summary.bmrCalories);
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "failed", e);
            return Result.retry();
        }
    }
}
