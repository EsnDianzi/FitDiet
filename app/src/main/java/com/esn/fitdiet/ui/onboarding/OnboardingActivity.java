package com.esn.fitdiet.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
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
import com.esn.fitdiet.data.local.entity.DailySummary;
import com.esn.fitdiet.data.local.entity.UserProfile;
import com.esn.fitdiet.data.repository.NutritionRepository;
import com.esn.fitdiet.domain.model.ActivityLevel;
import com.esn.fitdiet.domain.model.Equipment;
import com.esn.fitdiet.domain.model.Gender;
import com.esn.fitdiet.domain.model.Goal;
import com.esn.fitdiet.domain.model.NutritionTarget;
import com.esn.fitdiet.ui.home.HomeActivity;
import com.esn.fitdiet.util.AppExecutors;
import com.esn.fitdiet.util.DateUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 引导建档 / 编辑档案页（PRD §4.1）。
 *
 * <p>完整采集：年龄/身高/体重/性别/核心目标/促睾开关/活动水平/可用器械。
 * 点击"计算营养目标"后实时展示 BMR/TDEE/目标热量/三大营养素。
 *
 * <p>支持编辑：进入时如果数据库已有 UserProfile，自动预填所有字段。
 * 保存时 update 而非 insert（保留 createdAt），并把体重写入当日 DailySummary.weightKg
 * 作为体重曲线的首个数据点。
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
    // 标题
    private TextView tvTitle;

    /** 暂存计算结果供确认保存使用 */
    private NutritionTarget computedTarget;
    /** 已存在的档案（编辑模式），用于判断是 insert 还是 update */
    private UserProfile existingProfile;
    /** 标题：建档/编辑 */
    private boolean isEditMode;

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
        tvTitle = findViewById(R.id.tvTitle);

        // ── 活动水平 Spinner ──
        String[] activityLabels = new String[ActivityLevel.values().length];
        for (int i = 0; i < ActivityLevel.values().length; i++) {
            activityLabels[i] = ActivityLevel.values()[i].getLabel();
        }
        spActivity.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, activityLabels));

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

        // ── 加载已有档案（编辑模式） ──
        loadExistingProfile();
    }

    /** 加载已有档案 → 预填 UI。 */
    private void loadExistingProfile() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            MainApplication app = (MainApplication) getApplication();
            existingProfile = app.getContainer().getDatabase().userProfileDao().get();
            isEditMode = (existingProfile != null);

            AppExecutors.getInstance().mainThread().execute(() -> {
                if (isEditMode) {
                    populateFromProfile(existingProfile);
                } else {
                    // 新建：默认勾选徒手 + 默认"轻度"活动
                    if (!equipCheckBoxes.isEmpty()) equipCheckBoxes.get(0).setChecked(true);
                    spActivity.setSelection(1);
                }
            });
        });
    }

    /** 把已有档案的数据预填到 UI。 */
    private void populateFromProfile(UserProfile p) {
        tvTitle.setText("编辑档案");

        etAge.setText(String.valueOf(p.age));
        etHeight.setText(String.valueOf(p.heightCm));
        etWeight.setText(String.valueOf(p.weightKg));

        rgGender.check(p.gender == Gender.MALE ? R.id.rbMale : R.id.rbFemale);

        int goalId;
        switch (p.goal) {
            case CUT: goalId = R.id.rbCut; break;
            case BULK: goalId = R.id.rbBulk; break;
            case MAINTAIN: goalId = R.id.rbMaintain; break;
            default: goalId = R.id.rbRecomp;
        }
        rgGoal.check(goalId);

        swTestosterone.setChecked(p.boostTestosterone);

        int activityIdx = 0;
        for (int i = 0; i < ActivityLevel.values().length; i++) {
            if (ActivityLevel.values()[i] == p.activityLevel) {
                activityIdx = i;
                break;
            }
        }
        spActivity.setSelection(activityIdx);

        // 器械：先清空所有勾选
        for (CheckBox cb : equipCheckBoxes) {
            cb.setOnCheckedChangeListener(null);
            cb.setChecked(false);
        }
        // 再勾上已选
        if (p.equipment != null) {
            for (Equipment e : p.equipment) {
                for (CheckBox cb : equipCheckBoxes) {
                    if (cb.getTag() == e) {
                        cb.setChecked(true);
                        break;
                    }
                }
            }
        }
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

        // ── 展示结果（卡片式分组布局） ──
        renderResultCard(goal, activityLevel);

        btnSave.setVisibility(View.VISIBLE);
        btnSave.setText(isEditMode ? "保存修改" : "确认保存");
    }

    /** 用结构化视图（不靠 TextView 拼接）展示营养目标。 */
    private void renderResultCard(Goal goal, ActivityLevel activityLevel) {
        tvResult.setVisibility(View.VISIBLE);
        tvResult.setText("");

        // 移除旧的子视图（重新填充）
        tvResult.setBackgroundResource(R.drawable.bg_apple_card);

        // 由于 tvResult 是 TextView，用代码构造一个带 bullet 字符的格式化文本
        StringBuilder sb = new StringBuilder();
        sb.append("● 基础代谢  ").append((int) computedTarget.getBmr()).append(" kcal\n");
        sb.append("● 每日总消耗  ").append((int) computedTarget.getTdee()).append(" kcal\n");
        sb.append("● 目标摄入  ").append((int) computedTarget.getTargetCalories()).append(" kcal/日\n");
        sb.append("\n");
        sb.append("● 蛋白质  ").append((int) computedTarget.getProteinG()).append(" g  ·  ")
                .append((int) computedTarget.getProteinG() * 4).append(" kcal\n");
        sb.append("● 脂肪  ").append((int) computedTarget.getFatG()).append(" g  ·  ")
                .append((int) computedTarget.getFatG() * 9).append(" kcal\n");
        sb.append("● 碳水  ").append((int) computedTarget.getCarbsG()).append(" g  ·  ")
                .append((int) computedTarget.getCarbsG() * 4).append(" kcal\n");
        sb.append("\n");
        sb.append(String.format(Locale.getDefault(), "%s · %s",
                goal.getLabel(), activityLevel.getLabel()));
        tvResult.setText(sb.toString());
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

        // ── 构建档案（编辑模式保留 createdAt） ──
        UserProfile profile;
        if (isEditMode && existingProfile != null) {
            profile = existingProfile;
        } else {
            profile = new UserProfile();
            profile.id = 1;
            profile.createdAt = System.currentTimeMillis();
        }

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
        profile.updatedAt = System.currentTimeMillis();

        final double finalWeight = weight;
        AppExecutors.getInstance().diskIO().execute(() -> {
            MainApplication app = (MainApplication) getApplication();
            NutritionRepository repo = app.getContainer().getNutritionRepository();
            if (isEditMode) {
                repo.updateProfile(profile);
            } else {
                repo.saveProfile(profile);
            }

            // 把当前体重作为今日（建档日）体重曲线的首个数据点
            // 用 SummaryRepository 确保今日汇总存在，再回填体重
            app.getContainer().getSummaryRepository().ensureForDate(DateUtil.today());
            DailySummary today = app.getContainer().getDatabase().dailySummaryDao().getByDate(DateUtil.today());
            if (today != null) {
                today.weightKg = finalWeight;
                today.updatedAt = System.currentTimeMillis();
                app.getContainer().getDatabase().dailySummaryDao().insert(today);
            }

            runOnUiThread(() -> {
                Toast.makeText(this,
                        isEditMode ? "档案已更新" : "建档成功！",
                        Toast.LENGTH_SHORT).show();
                // 编辑模式回上一页；建档模式跳主页
                if (isEditMode) {
                    finish();
                } else {
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                }
            });
        });
    }
}
