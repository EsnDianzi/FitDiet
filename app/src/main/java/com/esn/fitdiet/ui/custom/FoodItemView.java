package com.esn.fitdiet.ui.custom;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.esn.fitdiet.R;
import com.esn.fitdiet.data.local.entity.FoodLog;
import com.esn.fitdiet.domain.model.FoodSource;
import com.esn.fitdiet.domain.model.MealType;

import java.util.Locale;

/**
 * 饮食记录卡片视图（Apple 风格）。
 * 使用方式：new FoodItemView(context) 后调用 setData(FoodLog) 即可。
 */
public class FoodItemView extends LinearLayout {

    private final TextView tvMealType, tvSource, tvFoodName, tvCalories;
    private final TextView tvProtein, tvCarbs, tvFat;
    private final View barProtein, barCarbs, barFat;

    public FoodItemView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.item_food_log, this, true);
        tvMealType = findViewById(R.id.tvMealType);
        tvSource = findViewById(R.id.tvSource);
        tvFoodName = findViewById(R.id.tvFoodName);
        tvCalories = findViewById(R.id.tvCalories);
        tvProtein = findViewById(R.id.tvProtein);
        tvCarbs = findViewById(R.id.tvCarbs);
        tvFat = findViewById(R.id.tvFat);
        barProtein = findViewById(R.id.barProtein);
        barCarbs = findViewById(R.id.barCarbs);
        barFat = findViewById(R.id.barFat);
    }

    public void setData(FoodLog f) {
        // 餐次
        tvMealType.setText(mealLabel(f.mealType));
        // 来源
        tvSource.setText(sourceLabel(f.source));
        // 食物名
        tvFoodName.setText(f.foodName != null ? f.foodName : "--");
        // 热量
        tvCalories.setText(String.format(Locale.getDefault(), "%.0f kcal", f.calories));
        // 营养素
        tvProtein.setText(String.format(Locale.getDefault(), "蛋白 %.0fg", f.protein));
        tvCarbs.setText(String.format(Locale.getDefault(), "碳水 %.0fg", f.carbs));
        tvFat.setText(String.format(Locale.getDefault(), "脂肪 %.0fg", f.fat));

        // 营养素分布条（按热量贡献比例）
        // 蛋白 4 kcal/g, 碳水 4 kcal/g, 脂肪 9 kcal/g
        double pCal = f.protein * 4;
        double cCal = f.carbs * 4;
        double fCal = f.fat * 9;
        double total = Math.max(1, pCal + cCal + fCal);

        LinearLayout.LayoutParams lpP = (LinearLayout.LayoutParams) barProtein.getLayoutParams();
        LinearLayout.LayoutParams lpC = (LinearLayout.LayoutParams) barCarbs.getLayoutParams();
        LinearLayout.LayoutParams lpF = (LinearLayout.LayoutParams) barFat.getLayoutParams();
        lpP.weight = (float) (pCal / total);
        lpC.weight = (float) (cCal / total);
        lpF.weight = (float) (fCal / total);
        barProtein.setLayoutParams(lpP);
        barCarbs.setLayoutParams(lpC);
        barFat.setLayoutParams(lpF);
    }

    private String mealLabel(MealType m) {
        return (m == null) ? "其它" : m.getLabel();
    }

    private String sourceLabel(FoodSource s) {
        return (s == null) ? "MANUAL" : s.getLabel();
    }
}
