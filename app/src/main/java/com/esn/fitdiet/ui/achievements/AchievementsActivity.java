package com.esn.fitdiet.ui.achievements;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.esn.fitdiet.MainApplication;
import com.esn.fitdiet.R;
import com.esn.fitdiet.data.local.entity.LevelProgress;
import com.esn.fitdiet.data.repository.AppContainer;
import com.esn.fitdiet.data.repository.StatsRepository;
import com.esn.fitdiet.databinding.ActivityAchievementsBinding;
import com.esn.fitdiet.util.AppExecutors;

import java.util.List;

/**
 * 成就页（F6 成就系统 UI）。
 *
 * <p>展示 6 个成就卡片：lv1 / lv3 / lv5 / streak7 / kill100 / muscle6。
 * 每个卡片显示标题、描述、解锁状态（绿色=已解锁，灰色=未解锁）。
 * 数据来源于 {@link StatsRepository#computeAchievements(LevelProgress)}。
 */
public class AchievementsActivity extends AppCompatActivity {

    private ActivityAchievementsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAchievementsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 获取等级进度
        MainApplication app = (MainApplication) getApplication();
        AppContainer di = app.getContainer();
        StatsRepository statsRepo = di.getStatsRepository();

        // getProgress() 是同步 DB 查询，Room 禁止主线程
        AppExecutors.getInstance().diskIO().execute(() -> {
            LevelProgress lp = statsRepo.getProgress();
            List<StatsRepository.Achievement> achievements = statsRepo.computeAchievements(lp);

            AppExecutors.getInstance().mainThread().execute(() -> {
                LinearLayout container = binding.achievementContainer;
                for (StatsRepository.Achievement a : achievements) {
                    container.addView(buildAchievementCard(a));
                }
            });
        });

        binding.btnBack.setOnClickListener(v -> finish());
    }

    /**
     * 构建单个成就卡片。
     *
     * @param a 成就对象（id/title/unlocked/desc）
     * @return 带样式的卡片 LinearLayout
     */
    private LinearLayout buildAchievementCard(StatsRepository.Achievement a) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(20, 16, 20, 16);
        card.setBackgroundColor(a.unlocked
                ? getColor(R.color.apple_surface)
                : getColor(R.color.apple_surface_alt));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 1);
        card.setLayoutParams(params);

        // 状态图标
        TextView icon = new TextView(this);
        icon.setText(a.unlocked ? "✓" : "—");
        icon.setTextSize(18);
        icon.setTextColor(a.unlocked
                ? getColor(R.color.apple_accent)
                : getColor(R.color.apple_text_tertiary));
        icon.setGravity(Gravity.CENTER);
        icon.setPadding(0, 0, 16, 0);
        card.addView(icon);

        // 文字区域
        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText(a.title);
        title.setTextSize(16);
        title.setLetterSpacing(-0.01f);
        title.setTextColor(a.unlocked
                ? getColor(R.color.apple_text_primary)
                : getColor(R.color.apple_text_tertiary));
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        textCol.addView(title);

        TextView desc = new TextView(this);
        desc.setText(a.desc);
        desc.setTextSize(12);
        desc.setLetterSpacing(-0.01f);
        desc.setTextColor(a.unlocked
                ? getColor(R.color.apple_text_secondary)
                : getColor(R.color.apple_text_tertiary));
        desc.setPadding(0, 4, 0, 0);
        textCol.addView(desc);

        card.addView(textCol);

        return card;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
