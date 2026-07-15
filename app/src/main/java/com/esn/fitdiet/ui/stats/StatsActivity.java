package com.esn.fitdiet.ui.stats;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.esn.fitdiet.MainApplication;
import com.esn.fitdiet.R;
import com.esn.fitdiet.data.local.entity.ExerciseLog;
import com.esn.fitdiet.data.local.entity.LevelProgress;
import com.esn.fitdiet.data.repository.StatsRepository;
import com.esn.fitdiet.databinding.ActivityStatsBinding;
import com.esn.fitdiet.ui.custom.WeightChartView;
import com.esn.fitdiet.util.AppExecutors;

import java.util.List;
import java.util.Locale;

/**
 * 统计页（F6 趋势 + 训练历史 + 体重曲线）。
 *
 * <p>关键指标：
 * <ul>
 *   <li>本周净热量缺口：本周一（含）→ 今天（含）所有 DailySummary.netCalories 之和</li>
 *   <li>连续打卡天数（LevelProgress）</li>
 *   <li>体重曲线：自绘 WeightChartView，按 DailySummary.weightKg 画折线</li>
 *   <li>训练历史：最近 10 条 ExerciseLog</li>
 * </ul>
 */
public class StatsActivity extends AppCompatActivity {

    private ActivityStatsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStatsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MainApplication app = (MainApplication) getApplication();
        StatsRepository repo = app.getContainer().getStatsRepository();

        // ── 一次性同步：本周缺口、连续打卡、体重点、训练历史 ──
        AppExecutors.getInstance().diskIO().execute(() -> {
            double deficit = repo.weeklyDeficit();
            LevelProgress lp = repo.getProgress();
            int streak = (lp != null) ? lp.streakDays : 0;
            List<StatsRepository.WeightPoint> weightPts = repo.getWeightPoints();
            List<ExerciseLog> recentExercises = repo.getRecentExercise(10);

            AppExecutors.getInstance().mainThread().execute(() -> {
                // 本周缺口（负数=缺口，正数=盈余）
                String deficitSign = deficit <= 0 ? "" : "+";
                binding.tvWeeklyDeficit.setText(String.format(Locale.getDefault(),
                        "%s%.0f kcal", deficitSign, deficit));
                // 颜色：缺口=accent 绿，盈余=warning 橙
                binding.tvWeeklyDeficit.setTextColor(deficit <= 0
                        ? getColor(R.color.apple_accent)
                        : getColor(R.color.apple_warning));

                // 连续打卡
                binding.tvStreak.setText(String.format(Locale.getDefault(),
                        "%d 天", streak));

                // 体重折线
                ((WeightChartView) binding.weightChart).setPoints(weightPts);

                // 训练历史
                renderExerciseHistory(recentExercises);
            });
        });

        binding.btnBack.setOnClickListener(v -> finish());

        // 绑定底部导航栏
        com.esn.fitdiet.ui.common.BottomNavHelper.bind(this, findViewById(R.id.navBar));
    }

    /** 渲染训练历史列表。空列表显示提示语。 */
    private void renderExerciseHistory(List<ExerciseLog> logs) {
        LinearLayout container = binding.exerciseContainer;
        container.removeAllViews();

        if (logs == null || logs.isEmpty()) {
            binding.tvEmptyExercise.setVisibility(View.VISIBLE);
            return;
        }
        binding.tvEmptyExercise.setVisibility(View.GONE);

        for (int i = 0; i < logs.size(); i++) {
            ExerciseLog log = logs.get(i);
            TextView row = new TextView(this);
            row.setText(formatExerciseRow(log));
            row.setTextColor(getColor(R.color.apple_text_primary));
            row.setTextSize(14);
            row.setLetterSpacing(-0.01f);
            row.setPadding(16, 12, 16, 12);
            // 连续卡片组：首/末项带圆角，中间行用纯色
            boolean isFirst = (i == 0);
            boolean isLast = (i == logs.size() - 1);
            int bgRes;
            if (isFirst && isLast) bgRes = R.drawable.bg_apple_card;
            else if (isFirst) bgRes = R.drawable.bg_apple_card_top;
            else if (isLast) bgRes = R.drawable.bg_apple_card_bottom;
            else {
                row.setBackgroundColor(getColor(R.color.apple_surface));
                row.setPadding(16, 12, 16, 12);
                container.addView(row);
                continue;
            }
            row.setBackgroundResource(bgRes);
            container.addView(row);
        }
    }

    private String formatExerciseRow(ExerciseLog log) {
        // "07-15  胸  4 组  20 min  120 kcal"
        String date = (log.date != null && log.date.length() >= 10)
                ? log.date.substring(5) : log.date;
        StringBuilder sb = new StringBuilder();
        sb.append(date).append("  ");
        sb.append(log.muscleGroup != null ? log.muscleGroup : log.monsterName);
        sb.append("  ").append(log.completedRounds).append("/").append(log.totalRounds).append(" 组");
        sb.append("  ").append(log.durationMin).append(" min");
        sb.append("  ").append(String.format(Locale.getDefault(), "%.0f kcal", log.caloriesBurned));
        return sb.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
