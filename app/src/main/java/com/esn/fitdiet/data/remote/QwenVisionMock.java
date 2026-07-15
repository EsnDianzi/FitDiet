package com.esn.fitdiet.data.remote;

import com.esn.fitdiet.data.remote.dto.FoodItemDto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * QwenVisionService 的 Mock 实现，用于单元测试与无 Key 环境。
 */
public class QwenVisionMock implements QwenVisionService {

    public static final String STUB_OK = "ok";
    public static final String STUB_PARTIAL = "partial";
    public static final String STUB_EMPTY = "empty";
    public static final String STUB_UNAUTHORIZED = "unauthorized";
    public static final String STUB_NETWORK_ERROR = "network_error";
    public static final String STUB_HUGE_JSON = "huge";

    // —— 边界 / 错误模式桩 ——
    /** 非食物识别（风景照）：返回带名称但无营养字段的条目，应路由手动补录（TC-F3-13）。 */
    public static final String STUB_NON_FOOD = "non_food";
    /** 限流后成功：首次 429，重试 1 次后返回正常结果（TC-F3-05 正例）。 */
    public static final String STUB_RATE_LIMITED_THEN_OK = "ratelimit_then_ok";
    /** 持续限流：始终 429，重试 1 次后仍失败 → 手动（TC-F3-05 反例）。 */
    public static final String STUB_RATE_LIMITED_PERSISTENT = "ratelimit_persistent";

    private int callCount = 0;
    private final String behavior;

    public QwenVisionMock() {
        this(STUB_OK);
    }

    public QwenVisionMock(String behavior) {
        this.behavior = behavior;
    }

    @Override
    public Result<List<FoodItemDto>> recognize(byte[] imageJpeg, String prompt) {
        switch (behavior) {
            case STUB_OK:
                return Result.ok(Arrays.asList(
                        newItem("米饭", 150, 174, 4, 38, 0.4, 1),
                        newItem("鸡胸肉", 100, 165, 31, 0, 3.6, 0)));
            case STUB_PARTIAL:
                // 3 项中 1 项缺字段：data 携带全部条目，上层拆分出手动补录项（TC-F3-10）
                List<FoodItemDto> items = new ArrayList<>();
                items.add(newItem("米饭", 150, 174, 4, 38, 0.4, 1));
                items.add(newItem("西兰花", 120, 43, 3, 9, 0.5, 4));
                items.add(new FoodItemDto()); // 缺字段 → 无效
                return Result.partial(items, VisionError.partialSuccess("成功 2/3"));
            case STUB_EMPTY:
                return Result.fail(VisionError.parseError("未识别到食物"));
            case STUB_UNAUTHORIZED:
                return Result.fail(VisionError.unauthorized("Mock：API Key 未配置"));
            case STUB_NETWORK_ERROR:
                return Result.fail(VisionError.networkError("Mock：网络连接失败"));
            case STUB_HUGE_JSON:
                return Result.ok(hugeList());
            case STUB_NON_FOOD:
                // 风景照：有名称但无营养/份量，业务校验判定无效 → 手动补录
                return Result.ok(Collections.singletonList(newItem("蓝天", 0, 0, 0, 0, 0, 0)));
            case STUB_RATE_LIMITED_THEN_OK:
                callCount++;
                if (callCount == 1) {
                    return Result.fail(VisionError.rateLimited("Mock：触发限流"));
                }
                return Result.ok(Arrays.asList(
                        newItem("米饭", 150, 174, 4, 38, 0.4, 1),
                        newItem("鸡胸肉", 100, 165, 31, 0, 3.6, 0)));
            case STUB_RATE_LIMITED_PERSISTENT:
                return Result.fail(VisionError.rateLimited("Mock：持续限流"));
            default:
                return Result.ok(Collections.singletonList(newItem("米饭", 150, 174, 4, 38, 0.4, 1)));
        }
    }

    private FoodItemDto newItem(String name, double weightG, double calories,
                                double protein, double carbs, double fat, double fiber) {
        return new FoodItemDto(name, weightG, calories, protein, carbs, fat, fiber);
    }

    private List<FoodItemDto> hugeList() {
        List<FoodItemDto> list = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            list.add(newItem("食物" + i, 100, 150 + i, 5, 20, 3, 1));
        }
        return list;
    }
}
