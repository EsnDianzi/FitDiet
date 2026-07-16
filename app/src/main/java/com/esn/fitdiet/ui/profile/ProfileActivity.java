package com.esn.fitdiet.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.esn.fitdiet.MainApplication;
import com.esn.fitdiet.R;
import com.esn.fitdiet.data.local.entity.UserProfile;
import com.esn.fitdiet.databinding.ActivityProfileBinding;
import com.esn.fitdiet.ui.onboarding.OnboardingActivity;
import com.esn.fitdiet.util.AppExecutors;

import java.util.Locale;

/**
 * 个人设置页（PRD §6 "我的" Tab）。
 *
 * <p>展示当前档案摘要（基础信息 / 目标 / 活动 / 营养目标），
 * 提供编辑档案入口（跳转 OnboardingActivity，会预填已有数据）、
 * API Key 设置、关于信息。
 */
public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 阻止 EditText 自动抢占焦点
        binding.getRoot().requestFocus();

        // ── 加载当前档案 ──
        loadProfileSummary();

        // ── 保存 API Key ──
        binding.btnSaveKey.setOnClickListener(v -> {
            String key = binding.etApiKey.getText().toString().trim();
            if (!TextUtils.isEmpty(key)) {
                getSharedPreferences("fitdiet_prefs", MODE_PRIVATE)
                        .edit().putString("api_key", key).apply();
                Toast.makeText(this, "API Key 已保存", Toast.LENGTH_SHORT).show();
            }
        });

        // ── 编辑档案 ──
        binding.btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(this, OnboardingActivity.class)));

        binding.btnBack.setOnClickListener(v -> finish());

        // 绑定底部导航栏
        com.esn.fitdiet.ui.common.BottomNavHelper.bind(this, findViewById(R.id.navBar));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从编辑页返回时刷新数据
        loadProfileSummary();
    }

    /** 加载当前档案摘要展示 */
    private void loadProfileSummary() {
        MainApplication app = (MainApplication) getApplication();
        AppExecutors.getInstance().diskIO().execute(() -> {
            UserProfile p = app.getContainer().getDatabase().userProfileDao().get();
            AppExecutors.getInstance().mainThread().execute(() -> {
                if (p == null) {
                    binding.tvAge.setText("--");
                    binding.tvGender.setText("--");
                    binding.tvHeight.setText("--");
                    binding.tvWeight.setText("--");
                    binding.tvGoal.setText("--");
                    binding.tvActivity.setText("--");
                    binding.tvBoost.setText("--");
                    binding.tvBmr.setText("--");
                    binding.tvTdee.setText("--");
                    binding.tvTargetCalories.setText("-- kcal");
                    binding.tvProtein.setText("-- g");
                    binding.tvFat.setText("-- g");
                    binding.tvCarbs.setText("-- g");
                    return;
                }

                // 基础信息
                binding.tvAge.setText(String.valueOf(p.age));
                binding.tvGender.setText(p.gender == null ? "--" : p.gender.getLabel());
                binding.tvHeight.setText(String.format(Locale.getDefault(), "%.0f cm", p.heightCm));
                binding.tvWeight.setText(String.format(Locale.getDefault(), "%.1f kg", p.weightKg));

                // 目标 & 活动
                binding.tvGoal.setText(p.goal == null ? "--" : p.goal.getLabel());
                binding.tvActivity.setText(p.activityLevel == null ? "--" : p.activityLevel.getLabel());
                binding.tvBoost.setText(p.boostTestosterone ? "已开启" : "未开启");

                // 营养目标
                binding.tvBmr.setText(String.format(Locale.getDefault(), "%.0f kcal", p.bmr));
                binding.tvTdee.setText(String.format(Locale.getDefault(), "%.0f kcal", p.tdee));
                binding.tvTargetCalories.setText(String.format(Locale.getDefault(),
                        "%.0f kcal/日", p.targetCalories));
                binding.tvProtein.setText(String.format(Locale.getDefault(),
                        "%.0f g", p.proteinTarget));
                binding.tvFat.setText(String.format(Locale.getDefault(),
                        "%.0f g", p.fatTarget));
                binding.tvCarbs.setText(String.format(Locale.getDefault(),
                        "%.0f g", p.carbsTarget));
            });
        });
    }
}
