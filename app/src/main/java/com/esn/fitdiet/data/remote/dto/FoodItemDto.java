package com.esn.fitdiet.data.remote.dto;

/**
 * AI 识别结果中的单条食物，字段允许全部为空，业务层校验。
 */
public class FoodItemDto {
    public String name;     // 食材名
    public double weightG;  // 份量(克)
    public double calories; // 大卡
    public double protein;  // 克
    public double carbs;    // 克
    public double fat;      // 克
    public double fiber;    // 克（可空）

    public FoodItemDto() {
    }

    public FoodItemDto(String name, double weightG, double calories,
                       double protein, double carbs, double fat, double fiber) {
        this.name = name;
        this.weightG = weightG;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.fiber = fiber;
    }

    public boolean hasName() {
        return name != null && !name.trim().isEmpty();
    }

    /**
     * 业务有效性校验：命名食材 + 有效份量。
     * 用于「非食物识别」路由（TC-F3-13）：风景照等返回 {@code name="蓝天"} 但
     * 无营养字段时，份量 {@code weightG<=0} 判定为无效，转入手动补录而非入库。
     */
    public boolean isValid() {
        if (!hasName()) return false;
        return weightG > 0;
    }
}
