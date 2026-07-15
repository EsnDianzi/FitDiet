package com.esn.fitdiet.ui.diet;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.esn.fitdiet.MainApplication;
import com.esn.fitdiet.R;
import com.esn.fitdiet.data.local.entity.FoodLog;
import com.esn.fitdiet.data.remote.Result;
import com.esn.fitdiet.data.remote.VisionError;
import com.esn.fitdiet.data.remote.dto.FoodItemDto;
import com.esn.fitdiet.data.repository.FoodRepository;
import com.esn.fitdiet.domain.model.FoodSource;
import com.esn.fitdiet.domain.model.MealType;
import com.esn.fitdiet.util.AppExecutors;
import com.esn.fitdiet.util.DateUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 食物识别页：拍照/选图 → 视觉识别（Mock 默认）→ 确认 → 写 FoodLog。
 *
 * <p>降级路径（TC-X-01 / TC-X-05）：
 * <ul>
 *   <li>网络错误（networkError） → Toast + 显示手动录入表单</li>
 *   <li>API Key 缺失（401） → Toast + 显示手动录入表单</li>
 *   <li>部分成功 → 成功项 AI 写入，失败项提示手动补录</li>
 * </ul>
 *
 * <p>手动录入表单包含食物名 + 重量输入，保存时创建 source=MANUAL 的 FoodLog。
 */
public class FoodRecognitionActivity extends AppCompatActivity {

    private Button btnCapture, btnManual;
    private TextView tvResult;
    /** 手动录入表单容器（默认隐藏） */
    private LinearLayout manualForm;
    private EditText etFoodName, etWeightG;
    private Spinner spMealType, spFoodPreset;
    private Button btnSaveManual;

    /** 内置食材表：名称 → {kcal/100g, 蛋白g/100g, 碳水g/100g, 脂肪g/100g} */
    private static final Map<String, double[]> FOOD_TABLE = new HashMap<>();
    static {
        FOOD_TABLE.put("米饭（熟）",       new double[]{116, 2.6, 25.9, 0.3});
        FOOD_TABLE.put("馒头",             new double[]{223, 7.0, 44.2, 1.1});
        FOOD_TABLE.put("面条（煮）",       new double[]{110, 3.6, 22.0, 0.5});
        FOOD_TABLE.put("鸡胸肉（熟）",     new double[]{165, 31.0, 0.0, 3.6});
        FOOD_TABLE.put("鸡蛋（煮）",       new double[]{155, 12.6, 1.1, 10.6});
        FOOD_TABLE.put("牛肉（瘦）",       new double[]{190, 26.0, 0.0, 9.0});
        FOOD_TABLE.put("三文鱼",           new double[]{208, 20.0, 0.0, 13.0});
        FOOD_TABLE.put("豆腐",             new double[]{76,  8.1, 1.9, 4.8});
        FOOD_TABLE.put("西兰花",           new double[]{34,  2.8, 7.0, 0.4});
        FOOD_TABLE.put("菠菜",             new double[]{23,  2.9, 3.6, 0.4});
        FOOD_TABLE.put("苹果",             new double[]{52,  0.3, 14.0, 0.2});
        FOOD_TABLE.put("香蕉",             new double[]{89,  1.1, 23.0, 0.3});
        FOOD_TABLE.put("牛奶（全脂）",     new double[]{61,  3.2, 4.8, 3.3});
        FOOD_TABLE.put("酸奶（原味）",     new double[]{61,  3.5, 4.7, 3.3});
        FOOD_TABLE.put("全麦面包",         new double[]{247, 13.0, 41.0, 3.4});
        FOOD_TABLE.put("红薯",             new double[]{86,  1.6, 20.1, 0.1});
        FOOD_TABLE.put("土豆",             new double[]{77,  2.0, 17.5, 0.1});
        FOOD_TABLE.put("鸡腿（熟）",       new double[]{181, 26.0, 0.0, 8.0});
        FOOD_TABLE.put("虾仁",             new double[]{99,  24.0, 0.2, 0.3});
        FOOD_TABLE.put("燕麦片",           new double[]{367, 13.5, 66.3, 6.5});
        FOOD_TABLE.put("杏仁",             new double[]{579, 21.2, 21.6, 49.9});
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_recognition);

        tvResult = findViewById(R.id.tvResult);
        btnCapture = findViewById(R.id.btnCapture);
        btnManual = findViewById(R.id.btnManual);
        manualForm = findViewById(R.id.manualForm);
        etFoodName = findViewById(R.id.etFoodName);
        etWeightG = findViewById(R.id.etWeightG);
        spMealType = findViewById(R.id.spMealType);
        spFoodPreset = findViewById(R.id.spFoodPreset);
        btnSaveManual = findViewById(R.id.btnSaveManual);

        // ── 餐次选择 Spinner ──
        String[] mealLabels = {"早餐", "午餐", "晚餐", "加餐"};
        spMealType.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, mealLabels));
        spMealType.setSelection(1); // 默认午餐

        // ── 内置食材表 Spinner ──
        String[] presetNames = FOOD_TABLE.keySet().toArray(new String[0]);
        java.util.Arrays.sort(presetNames);
        ArrayAdapter<String> presetAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, presetNames);
        spFoodPreset.setAdapter(presetAdapter);
        // 选择食材时自动填充名称
        spFoodPreset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selected = (String) parent.getItemAtPosition(pos);
                etFoodName.setText(selected);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        btnCapture.setOnClickListener(v -> runMockRecognition());

        btnManual.setOnClickListener(v -> showManualForm());

        // 绑定底部导航栏
        com.esn.fitdiet.ui.common.BottomNavHelper.bind(this, findViewById(R.id.navBar));

        // 手动保存按钮
        btnSaveManual.setOnClickListener(v -> saveManualEntry());
    }

    /** 运行识别（Mock / 真实 API） */
    private void runMockRecognition() {
        tvResult.setText("识别中...");
        AppExecutors executors = AppExecutors.getInstance();
        executors.diskIO().execute(() -> {
            MainApplication app = (MainApplication) getApplication();
            FoodRepository repo = app.getContainer().getFoodRepository();

            // 传空图片 + 默认 prompt 触发 Mock
            Result<List<FoodItemDto>> result = repo.recognize(new byte[0], "识别食物");

            runOnUiThread(() -> handleRecognitionResult(result, repo));
        });
    }

    /**
     * 处理识别结果，包含降级路径（TC-X-01 / TC-X-05）。
     *
     * @param result 识别结果（SUCCESS/PARTIAL/FAILURE）
     * @param repo   食物仓储（用于写入成功项）
     */
    private void handleRecognitionResult(Result<List<FoodItemDto>> result, FoodRepository repo) {
        // ── TC-X-05：API Key 缺失（401） → 手动录入 ──
        if (result.isFailure() && result.getError() != null
                && result.getError().code == 401) {
            Toast.makeText(this, R.string.no_api_key, Toast.LENGTH_LONG).show();
            showManualForm();
            return;
        }

        // ── TC-X-01：网络错误 → 手动录入 ──
        if (result.isFailure() && result.getError() != null
                && result.getError().isNetworkError()) {
            Toast.makeText(this, R.string.no_network, Toast.LENGTH_LONG).show();
            showManualForm();
            return;
        }

        if (result.isSuccess() && result.getData() != null) {
            // 全部成功：展示结果 + AI 写入
            StringBuilder sb = new StringBuilder("识别结果：\n");
            for (FoodItemDto item : result.getData()) {
                if (item.isValid()) {
                    sb.append("• ").append(item.name)
                            .append(" ").append(item.weightG).append("g")
                            .append(" (").append((int) item.calories).append("kcal)\n");
                }
            }
            tvResult.setText(sb.toString());

            // 写入 AI 识别的有效条目
            writeAiLogs(repo, result.getData());

        } else if (result.isPartial()) {
            // 部分成功：展示结果 + 提示手动补录
            StringBuilder sb = new StringBuilder("部分识别成功：\n");
            if (result.getData() != null) {
                for (FoodItemDto item : result.getData()) {
                    if (item.isValid()) {
                        sb.append("• ").append(item.name)
                                .append(" ").append(item.weightG).append("g\n");
                    }
                }
            }
            sb.append("\n").append(getString(R.string.ai_partial_success));
            tvResult.setText(sb.toString());
            Toast.makeText(this, R.string.ai_partial_success, Toast.LENGTH_LONG).show();

            // 写入有效条目，无效项由用户手动补录
            if (result.getData() != null) {
                writeAiLogs(repo, result.getData());
            }

        } else {
            // 完全失败：显示错误 + 降级手动
            tvResult.setText("识别失败：\n"
                    + (result.getError() != null ? result.getError().msg : "未知错误"));
            Toast.makeText(this, R.string.no_network, Toast.LENGTH_LONG).show();
            showManualForm();
        }
    }

    /** 将 AI 识别的有效条目写入 Room（source=AI） */
    private void writeAiLogs(FoodRepository repo, List<FoodItemDto> items) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            for (FoodItemDto item : items) {
                if (!item.isValid()) continue;
                FoodLog log = new FoodLog();
                log.date = DateUtil.today();
                log.mealType = MealType.LUNCH; // 默认午餐
                log.foodName = item.name;
                log.calories = item.calories;
                log.protein = item.protein;
                log.carbs = item.carbs;
                log.fat = item.fat;
                log.fiber = item.fiber;
                log.source = FoodSource.AI;
                log.createdAt = System.currentTimeMillis();
                repo.insert(log);
            }
        });
    }

    /** 显示手动录入表单（TC-X-01/05 降级目标） */
    private void showManualForm() {
        manualForm.setVisibility(View.VISIBLE);
        btnCapture.setEnabled(false);
    }

    /** 保存手动录入条目（source=MANUAL），使用内置食材表数据。 */
    private void saveManualEntry() {
        String name = etFoodName.getText().toString().trim();
        String weightStr = etWeightG.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "请输入食物名称", Toast.LENGTH_SHORT).show();
            return;
        }
        double weightG;
        try {
            weightG = Double.parseDouble(weightStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效重量", Toast.LENGTH_SHORT).show();
            return;
        }
        if (weightG <= 0) {
            Toast.makeText(this, "重量必须大于 0", Toast.LENGTH_SHORT).show();
            return;
        }

        // 从食材表查找营养数据，找不到用 1.5 kcal/g 估算
        double[] data = FOOD_TABLE.get(name);
        double kcalPer100, protPer100, carbPer100, fatPer100;
        if (data != null) {
            kcalPer100 = data[0]; protPer100 = data[1]; carbPer100 = data[2]; fatPer100 = data[3];
        } else {
            kcalPer100 = 150; protPer100 = 8; carbPer100 = 15; fatPer100 = 4;
        }
        double factor = weightG / 100.0;

        // 餐次映射
        int mealIdx = spMealType.getSelectedItemPosition();
        MealType mealType;
        switch (mealIdx) {
            case 0: mealType = MealType.BREAKFAST; break;
            case 2: mealType = MealType.DINNER; break;
            case 3: mealType = MealType.SNACK; break;
            default: mealType = MealType.LUNCH;
        }

        FoodLog log = new FoodLog();
        log.date = DateUtil.today();
        log.mealType = mealType;
        log.foodName = name;
        log.calories = kcalPer100 * factor;
        log.protein = protPer100 * factor;
        log.carbs = carbPer100 * factor;
        log.fat = fatPer100 * factor;
        log.fiber = 0;
        log.source = FoodSource.MANUAL;
        log.createdAt = System.currentTimeMillis();

        MainApplication app = (MainApplication) getApplication();
        AppExecutors.getInstance().diskIO().execute(() -> {
            app.getContainer().getFoodRepository().insert(log);

            runOnUiThread(() -> {
                Toast.makeText(this, "已保存: " + name + " " + (int) weightG + "g  (" +
                        (int)(kcalPer100 * factor) + "kcal)", Toast.LENGTH_SHORT).show();

                // 清空表单
                etFoodName.setText("");
                etWeightG.setText("");
                manualForm.setVisibility(View.GONE);
                btnCapture.setEnabled(true);
                tvResult.setText("手动录入: " + name + " " + (int) weightG + "g");
            });
        });
    }
}
