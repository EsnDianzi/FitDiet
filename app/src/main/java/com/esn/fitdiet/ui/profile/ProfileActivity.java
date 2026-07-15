package com.esn.fitdiet.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.esn.fitdiet.MainApplication;
import com.esn.fitdiet.R;
import com.esn.fitdiet.data.local.entity.UserProfile;
import com.esn.fitdiet.data.repository.NutritionRepository;
import com.esn.fitdiet.domain.model.NutritionTarget;
import com.esn.fitdiet.ui.onboarding.OnboardingActivity;
import com.esn.fitdiet.util.AppExecutors;

import java.util.Locale;

/**
 * 个人设置页（PRD §6 "我的" Tab）。
 *
 * <p>展示当前档案摘要（BMR/TDEE/目标热量/三大营养素），
 * 提供编辑档案入口（跳转 OnboardingActivity）、API Key 设置、关于信息。
 */
public class ProfileActivity extends AppCompatActivity {

    private TextView tvProfileSummary;
    private EditText etApiKey;
    private Button btnSaveKey, btnEditProfile, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvProfileSummary = findViewById(R.id.tvProfileSummary);
        etApiKey = findViewById(R.id.etApiKey);
        btnSaveKey = findViewById(R.id.btnSaveKey);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnBack = findViewById(R.id.btnBack);

        // ── 加载当前档案 ──
        loadProfileSummary();

        // ── 保存 API Key ──
        btnSaveKey.setOnClickListener(v -> {
            String key = etApiKey.getText().toString().trim();
            if (!TextUtils.isEmpty(key)) {
                getSharedPreferences("fitdiet_prefs", MODE_PRIVATE)
                        .edit().putString("api_key", key).apply();
                Toast.makeText(this, "API Key 已保存", Toast.LENGTH_SHORT).show();
            }
        });

        // ── 编辑档案 ──
        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(this, OnboardingActivity.class)));

        btnBack.setOnClickListener(v -> finish());
    }

    /** 加载当前档案摘要展示 */
    private void loadProfileSummary() {
        MainApplication app = (MainApplication) getApplication();
        // get() 是同步 DB 查询，Room 禁止主线程——放进 diskIO
        AppExecutors.getInstance().diskIO().execute(() -> {
            UserProfile p = app.getContainer().getDatabase().userProfileDao().get();
            AppExecutors.getInstance().mainThread().execute(() -> {
                if (p == null) {
                    tvProfileSummary.setText("尚未建档，请先完成引导设置");
                    return;
                }

                String summary = String.format(Locale.getDefault(),
                        "━━ 个人档案 ━━\n" +
                        "性别: %s  |  年龄: %d\n" +
                        "身高: %.0f cm  |  体重: %.1f kg\n" +
                        "目标: %s  |  活动: %s\n" +
                        "促睾: %s\n\n" +
                        "BMR: %.0f kcal  |  TDEE: %.0f kcal\n" +
                        "目标热量: %.0f kcal/日\n" +
                        "蛋白: %.0f g  |  脂肪: %.0f g  |  碳水: %.0f g\n\n" +
                        "FitDiet v1.0  |  com.esn.fitdiet",
                        p.gender.name(), p.age,
                        p.heightCm, p.weightKg,
                        p.goal.getLabel(), p.activityLevel.getLabel(),
                        p.boostTestosterone ? "开" : "关",
                        p.bmr, p.tdee,
                        p.targetCalories,
                        p.proteinTarget, p.fatTarget, p.carbsTarget);

                tvProfileSummary.setText(summary);
            });
        });
    }
}
