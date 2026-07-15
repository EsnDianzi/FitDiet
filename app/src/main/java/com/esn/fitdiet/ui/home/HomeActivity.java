package com.esn.fitdiet.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.esn.fitdiet.MainApplication;
import com.esn.fitdiet.R;
import com.esn.fitdiet.data.local.entity.DailySummary;
import com.esn.fitdiet.data.local.entity.FoodLog;
import com.esn.fitdiet.data.local.entity.LevelProgress;
import com.esn.fitdiet.data.repository.AppContainer;
import com.esn.fitdiet.databinding.ActivityHomeBinding;
import com.esn.fitdiet.ui.achievements.AchievementsActivity;
import com.esn.fitdiet.ui.battle.BattleActivity;
import com.esn.fitdiet.ui.diet.FoodRecognitionActivity;
import com.esn.fitdiet.ui.main.HomeViewModel;
import com.esn.fitdiet.ui.onboarding.OnboardingActivity;
import com.esn.fitdiet.ui.profile.ProfileActivity;
import com.esn.fitdiet.ui.stats.StatsActivity;
import com.esn.fitdiet.util.AppExecutors;

import java.util.List;
import java.util.Locale;

/**
 * FitDiet 主页（MVVM）。
 *
 * <p>展示当日净热量、摄入/消耗汇总、饮食记录列表、等级进度条，
 * 并通过底部导航按钮串联战斗页与成就页。
 *
 * <p>TC-X-08：首次启动检测 UserProfile 为空 → 自动跳转 OnboardingActivity。
 */
public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private HomeViewModel vm;

    /** 观察者引用，避免 GC 回收导致 LiveData 失活。 */
    private Observer<DailySummary> summaryObserver;
    private Observer<List<FoodLog>> foodObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ── 从 Application 获取依赖容器 ──
        MainApplication app = (MainApplication) getApplication();
        AppContainer di = app.getContainer();

        vm = new HomeViewModel(
                di.getSummaryRepository(),
                di.getFoodRepository(),
                di.getBattleRepository());

        // ── 所有同步 DB 访问放入 diskIO，避免主线程 Room 报错 ──
        AppExecutors.getInstance().diskIO().execute(() -> {
            // TC-X-08：空库首启 → 跳转引导
            boolean isEmpty = app.getContainer().getDatabase().userProfileDao().count() == 0;

            // 等级展示
            LevelProgress lp = di.getBattleRepository().getProgress();
            int level = (lp != null) ? lp.level : 1;
            int totalExp = (lp != null) ? lp.totalExp : 0;
            int expToNext = com.esn.fitdiet.game.LevelSystem.expToNext(level);

            AppExecutors.getInstance().mainThread().execute(() -> {
                if (isEmpty) {
                    Toast.makeText(this, R.string.empty_db_onboarding, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, OnboardingActivity.class));
                    finish();
                    return;
                }

                binding.tvLevel.setText(String.format(Locale.getDefault(),
                        getString(R.string.home_level), level));
                binding.expBar.setMax(expToNext);
                binding.expBar.setProgress(totalExp % expToNext);

                // LiveData 绑定（只在非空库时执行）
                setupObservers();
            });
        });

        // ── 底部导航栏（统一助手，自动高亮当前 tab） ──
        com.esn.fitdiet.ui.common.BottomNavHelper.bind(this, findViewById(R.id.navBar));
    }

    /** 绑定 LiveData 观察者（需在 DB 读取完成后调用） */
    private void setupObservers() {
        summaryObserver = summary -> {
            if (summary != null) {
                binding.tvNetCalories.setText(String.format(Locale.getDefault(),
                        "%.0f kcal", summary.netCalories));
                binding.tvIntake.setText(String.format(Locale.getDefault(),
                        "摄入 %.0f", summary.intakeCalories));
                binding.tvBurned.setText(String.format(Locale.getDefault(),
                        "消耗 %.0f", summary.totalBurned));
            } else {
                binding.tvNetCalories.setText(R.string.home_no_summary);
            }
        };
        vm.todaySummary().observe(this, summaryObserver);

        // ── 当日饮食 LiveData（卡片式） ──
        foodObserver = foods -> {
            LinearLayout listContainer = binding.foodListContainer;
            listContainer.removeAllViews();
            if (foods == null || foods.isEmpty()) {
                TextView empty = new TextView(this);
                empty.setText(R.string.home_empty_food);
                empty.setTextColor(getColor(R.color.apple_text_tertiary));
                empty.setTextSize(14);
                empty.setPadding(16, 16, 16, 16);
                listContainer.addView(empty);
                return;
            }
            for (int i = 0; i < foods.size(); i++) {
                FoodLog f = foods.get(i);
                com.esn.fitdiet.ui.custom.FoodItemView card =
                        new com.esn.fitdiet.ui.custom.FoodItemView(this);
                card.setData(f);
                listContainer.addView(card);
            }
        };
        vm.todayFood().observe(this, foodObserver);

        // ── 兜底生成当日汇总（涉及同步 DAO，放 diskIO） ──
        AppExecutors.getInstance().diskIO().execute(() -> vm.ensureTodaySummary());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
