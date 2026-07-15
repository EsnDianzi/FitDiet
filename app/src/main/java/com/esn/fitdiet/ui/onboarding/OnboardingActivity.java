package com.esn.fitdiet.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.esn.fitdiet.MainApplication;
import com.esn.fitdiet.R;
import com.esn.fitdiet.data.local.entity.UserProfile;
import com.esn.fitdiet.data.repository.NutritionRepository;
import com.esn.fitdiet.domain.model.ActivityLevel;
import com.esn.fitdiet.domain.model.Equipment;
import com.esn.fitdiet.domain.model.Gender;
import com.esn.fitdiet.domain.model.Goal;
import com.esn.fitdiet.domain.model.NutritionTarget;
import com.esn.fitdiet.ui.home.HomeActivity;
import com.esn.fitdiet.util.AppExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 引导建档页（PRD §4.1）。
 *
 * <p>完整采集：年龄/身高/体重/性别/核心目标/促睾开关/活动水平/可用器械。
 * 点击"计算营养目标"后实时展示 BMR/TDEE/目标热量/三大营养素。
 * 用户确认后写入 UserProfile 并跳转主页。
 *
 * <p>促睾开关仅对男性可见（女性脂肪分派走目标百分比，与促睾无关）。
 */
public class OnboardingActivity extends AppCompatActivity {

    // 基础信息
    private EditText etAge, etHeight, etWeight;
    // 性别
    private RadioGroup rgGender;
    // 目标
    private RadioGroup rgGoal;
    // 促睾
    private LinearLayout layoutTestosterone;
    private Switch swTestosterone;
    // 活动水平
    private Spinner spActivity;
    // 器械
    private LinearLayout equipContainer;
    private final List<CheckBox> equipCheckBoxes = new ArrayList<>();
    // 结果
    private TextView tvResult;
    private Button btnCalculate, btnSave;

    /** 暂存计算结果供确认保存使用 */
    private NutritionTarget computedTarget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        // ── 绑定视图 ──
        etAge = findViewById(R.id.etAge);
        etHeight = findViewById(R.id.etHeight);
        etWeight = findViewById(R.id.etWeight);
        rgGender = findViewById(R.id.rgGender);
        rgGoal = findViewById(R.id.rgGoal);
        layoutTestosterone = findViewById(R.id.layoutTestosterone);
        swTestosterone = findViewById(R.id.swTestosterone);
        spActivity = findViewById(R.id.spActivity);
        equipContainer = findViewById(R.id.equipContainer);
        tvResult = findViewById(R.id.tvResult);
        btnCalculate = findViewById(R.id.btnCalculate);
        btnSave = findViewById(R.id.btnSave);

        // ── 活动水平 Spinner ──
        String[] activityLabels = new String[ActivityLevel.values().length];
        for (int i = 0; i < ActivityLevel.values().length; i++) {
            activityLabels[i] = ActivityLevel.values()[i].getLabel();
        }
        spActivity.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, activityLabels));
        spActivity.setSelection(1); // 默认"轻度"

        // ── 器械多选 CheckBox ──
        for (Equipment e : Equipment.values()) {
            CheckBox cb = new CheckBox(this);
            cb.setText(e.getLabel());
            cb.setTextColor(getColor(R.color.apple_text_primary));
            cb.setTextSize(15);
            cb.setButtonTintList(android.content.res.ColorStateList.valueOf(
                    getColor(R.color.apple_accent)));
            cb.setPadding(0, 8, 0, 8);
            cb.setTag(e);
            equipCheckBoxes.add(cb);
            equipContainer.addView(cb);
        }
        // 默认勾选"徒手"
        if (!equipCheckBoxes.isEmpty()) equipCheckBoxes.get(0).setChecked(true);

        // ── 性别切换时显示/隐藏促睾开关 ──
        rgGender.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isMale = checkedId == R.id.rbMale;
            layoutTestosterone.setVisibility(isMale ? View.VISIBLE : View.GONE);
            if (!isMale) swTestosterone.setChecked(false);
        });

        // ── 计算按钮 ──
        btnCalculate.setOnClickListener(v -> calculateAndShow());

        // ── 保存按钮 ──
        btnSave.setOnClickListener(v -> saveAndFinish());
    }

    /** 校验输入 → 计算营养目标 → 展示结果区 + 显示保存按钮。 */
    private void calculateAndShow() {
        String ageStr = etAge.getText().toString().trim();
        String heightStr = etHeight.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();

        if (TextUtils.isEmpty(ageStr) || TextUtils.isEmpty(heightStr) || TextUtils.isEmpty(weightStr)) {
            Toast.makeText(this, "请填写所有基础信息", Toast.LENGTH_SHORT).show();
            return;
        }

        int age;
        double height, weight;
        try {
            age = Integer.parseInt(ageStr);
            height = Double.parseDouble(heightStr);
            weight = Double.parseDouble(weightStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效数字", Toast.LENGTH_SHORT).show();
            return;
        }

        // 基础校验
        if (age < 10 || age > 120) {
            Toast.makeText(this, "年龄需在 10~120 之间", Toast.LENGTH_SHORT).show();
            return;
        }
        if (height < 100 || height > 250) {
            Toast.makeText(this, "身高需在 100~250 cm 之间", Toast.LENGTH_SHORT).show();
            return;
        }
        if (weight < 30 || weight > 300) {
            Toast.makeText(this, "体重需在 30~300 kg 之间", Toast.LENGTH_SHORT).show();
            return;
        }

        // 读取 UI 输入
        Gender gender = (rgGender.getCheckedRadioButtonId() == R.id.rbMale)
                ? Gender.MALE : Gender.FEMALE;

        Goal goal;
        int checkedGoalId = rgGoal.getCheckedRadioButtonId();
        if (checkedGoalId == R.id.rbCut) goal = Goal.CUT;
        else if (checkedGoalId == R.id.rbBulk) goal = Goal.BULK;
        else if (checkedGoalId == R.id.rbMaintain) goal = Goal.MAINTAIN;
        else goal = Goal.RECOMP;

        boolean boostTest = swTestosterone.isChecked() && gender == Gender.MALE;

        int activityIdx = spActivity.getSelectedItemPosition();
        ActivityLevel activityLevel = ActivityLevel.values()[activityIdx];

        // ── 计算 ──
        computedTarget = com.esn.fitdiet.domain.calculator.NutritionCalculator.compute(
                gender, age, height, weight, activityLevel, goal, boostTest);

        // ── 展示结果 ──
        String resultText = String.format(Locale.getDefault(),
                "━━ 营养目标 ━━\n" +
                "BMR: %.0f kcal\n" +
                "TDEE: %.0f kcal\n" +
                "目标热量: %.0f kcal/日\n\n" +
                "蛋白质: %.0f g (%.0f kcal)\n" +
                "脂肪: %.0f g (%.0f kcal)\n" +
                "碳水: %.0f g (%.0f kcal)\n\n" +
                "目标: %s  |  活动: %s",
                computedTarget.getBmr(), computedTarget.getTdee(), computedTarget.getTargetCalories(),
                computedTarget.getProteinG(), computedTarget.getProteinG() * 4,
                computedTarget.getFatG(), computedTarget.getFatG() * 9,
                computedTarget.getCarbsG(), computedTarget.getCarbsG() * 4,
                goal.getLabel(), activityLevel.getLabel());

        tvResult.setText(resultText);
        tvResult.setVisibility(View.VISIBLE);
        btnSave.setVisibility(View.VISIBLE);
    }

    /** 采集所有输入 → 构建 UserProfile → 落库 → 跳转主页。 */
    private void saveAndFinish() {
        if (computedTarget == null) {
            Toast.makeText(this, "请先计算营养目标", Toast.LENGTH_SHORT).show();
            return;
        }

        double weight = Double.parseDouble(etWeight.getText().toString().trim());
        double height = Double.parseDouble(etHeight.getText().toString().trim());
        int age = Integer.parseInt(etAge.getText().toString().trim());

        Gender gender = (rgGender.getCheckedRadioButtonId() == R.id.rbMale)
                ? Gender.MALE : Gender.FEMALE;

        Goal goal;
        int checkedGoalId = rgGoal.getCheckedRadioButtonId();
        if (checkedGoalId == R.id.rbCut) goal = Goal.CUT;
        else if (checkedGoalId == R.id.rbBulk) goal = Goal.BULK;
        else if (checkedGoalId == R.id.rbMaintain) goal = Goal.MAINTAIN;
        else goal = Goal.RECOMP;

        boolean boostTest = swTestosterone.isChecked() && gender == Gender.MALE;
        int activityIdx = spActivity.getSelectedItemPosition();
        ActivityLevel activityLevel = ActivityLevel.values()[activityIdx];

        List<Equipment> equipmentList = new ArrayList<>();
        for (CheckBox cb : equipCheckBoxes) {
            if (cb.isChecked()) equipmentList.add((Equipment) cb.getTag());
        }

        // ── 构建档案 ──
        UserProfile profile = new UserProfile();
        profile.id = 1;
        profile.age = age;
        profile.heightCm = height;
        profile.weightKg = weight;
        profile.gender = gender;
        profile.goal = goal;
        profile.boostTestosterone = boostTest;
        profile.activityLevel = activityLevel;
        profile.equipment = equipmentList;
        profile.bmr = computedTarget.getBmr();
        profile.tdee = computedTarget.getTdee();
        profile.targetCalories = computedTarget.getTargetCalories();
        profile.proteinTarget = computedTarget.getProteinG();
        profile.carbsTarget = computedTarget.getCarbsG();
        profile.fatTarget = computedTarget.getFatG();
        profile.createdAt = System.currentTimeMillis();
        profile.updatedAt = System.currentTimeMillis();

        AppExecutors.getInstance().diskIO().execute(() -> {
            MainApplication app = (MainApplication) getApplication();
            app.getContainer().getDatabase().userProfileDao().insert(profile);

            runOnUiThread(() -> {
                Toast.makeText(this, "建档成功！", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            });
        });
    }
}
