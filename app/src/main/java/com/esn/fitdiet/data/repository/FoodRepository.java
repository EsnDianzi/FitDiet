package com.esn.fitdiet.data.repository;

import androidx.lifecycle.LiveData;

import com.esn.fitdiet.data.local.dao.FoodLogDao;
import com.esn.fitdiet.data.local.entity.FoodLog;
import com.esn.fitdiet.data.remote.QwenVisionService;
import com.esn.fitdiet.data.remote.Result;
import com.esn.fitdiet.data.remote.VisionError;
import com.esn.fitdiet.data.remote.dto.FoodItemDto;
import com.esn.fitdiet.domain.model.FoodSource;
import com.esn.fitdiet.domain.model.MealType;
import com.esn.fitdiet.util.AppExecutors;

import java.util.ArrayList;
import java.util.List;

/**
 * 食物仓储：封装 FoodLogDao + QwenVisionService + AppExecutors。
 *
 * <p>核心能力 {@link #recognizeAndSplit}（开发方案 §3.3 / 测试端 TC-F3-01~13）：
 * 调用视觉服务 →（可重试）逐条校验 → 拆分为「待确认（source=AI）」与「手动补录（source=MANUAL）」
 * 两条路径，最终都写入 {@link FoodLog}。降级（无 Key / 401 / 超时 / 整体解析失败 / 全量非食物）
 * 不中断，直接走手动补录。
 */
public class FoodRepository {

    private final FoodLogDao foodLogDao;
    private final QwenVisionService visionService;
    private final AppExecutors executors;
    private final long retryBackoffMs;
    /** 写完 FoodLog 后回调（用于立即重算 DailySummary）。 */
    private Runnable onInsertedHook;

    public FoodRepository(FoodLogDao foodLogDao, QwenVisionService visionService, AppExecutors executors) {
        this(foodLogDao, visionService, executors, 300L);
    }

    public FoodRepository(FoodLogDao foodLogDao, QwenVisionService visionService,
                          AppExecutors executors, long retryBackoffMs) {
        this.foodLogDao = foodLogDao;
        this.visionService = visionService;
        this.executors = executors;
        this.retryBackoffMs = Math.max(0, retryBackoffMs);
    }

    /** 设置写入后回调（SummaryRepository 在此重算 DailySummary）。 */
    public void setOnInsertedHook(Runnable hook) {
        this.onInsertedHook = hook;
    }

    public LiveData<List<FoodLog>> observeByDate(String date) {
        return foodLogDao.observeByDate(date);
    }

    /** 异步插入，写入后回调（用于刷新 DailySummary）。 */
    public void insert(FoodLog log) {
        executors.diskIO().execute(() -> {
            foodLogDao.insert(log);
            if (onInsertedHook != null) onInsertedHook.run();
        });
    }

    /** 异步批量插入，写入后回调。 */
    public void insertAll(List<FoodLog> logs) {
        executors.diskIO().execute(() -> {
            foodLogDao.insertAll(logs);
            if (onInsertedHook != null) onInsertedHook.run();
        });
    }

    /** 同步调用视觉识别（已在调用方异步执行）。 */
    public Result<List<FoodItemDto>> recognize(byte[] imageJpeg, String prompt) {
        return visionService.recognize(imageJpeg, prompt);
    }

    /**
     * 识别并精细拆分（§3.3 建议 2b）。
     *
     * <p>对可重试错误（429 / 5xx / 超时 / 网络）退避重试 1 次；最终按业务校验
     * 将条目拆分为待确认与手动补录两部分，供 UI 展示与落库。
     */
    public SplitResult recognizeAndSplit(byte[] imageJpeg, String prompt) {
        Result<List<FoodItemDto>> r = recognizeWithRetry(imageJpeg, prompt);

        List<FoodItemDto> confirmed = new ArrayList<>(); // 有效 → 待确认（source=AI）
        List<FoodItemDto> manual = new ArrayList<>();    // 无效/非食物 → 手动补录（source=MANUAL）
        if (r.hasData()) {
            for (FoodItemDto it : r.getData()) {
                if (it != null && it.isValid()) {
                    confirmed.add(it);
                } else if (it != null) {
                    manual.add(it);
                }
            }
        }

        // 降级：调用失败，或存在需手动补录的项
        boolean degraded = r.isFailure() || !manual.isEmpty();
        return new SplitResult(confirmed, manual, r.getStatus(), r.getError(), degraded);
    }

    /** 可重试错误退避重试 1 次（§5.5：429/5xx/超时/网络均 retryable）。 */
    private Result<List<FoodItemDto>> recognizeWithRetry(byte[] imageJpeg, String prompt) {
        Result<List<FoodItemDto>> r = visionService.recognize(imageJpeg, prompt);
        if (r.isFailure() && r.getError() != null && r.getError().retryable) {
            sleepQuietly(retryBackoffMs);
            r = visionService.recognize(imageJpeg, prompt);
        }
        return r;
    }

    private static void sleepQuietly(long ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 将 DTO 列表转换为 {@link FoodLog}，统一写入归属日期/餐次/来源。
     * confirmed 项用 {@link FoodSource#AI}，manual 项用 {@link FoodSource#MANUAL}（TC-F3-09）。
     */
    public List<FoodLog> buildLogs(List<FoodItemDto> items, FoodSource source,
                                   String date, MealType meal, String imagePath) {
        List<FoodLog> out = new ArrayList<>();
        if (items == null) return out;
        long now = System.currentTimeMillis();
        for (FoodItemDto it : items) {
            if (it == null) continue;
            FoodLog f = new FoodLog();
            f.date = date;
            f.mealType = meal;
            f.foodName = it.name;
            f.calories = it.calories;
            f.protein = it.protein;
            f.carbs = it.carbs;
            f.fat = it.fat;
            f.fiber = it.fiber;
            f.imagePath = imagePath;
            f.source = source;        // AI 识别 vs 手动补录（PRD 4.3.4）
            f.createdAt = now;
            out.add(f);
        }
        return out;
    }

    /** {@link #recognizeAndSplit} 的拆分结果。 */
    public static final class SplitResult {
        public final List<FoodItemDto> confirmed; // 有效项 → 待确认（source=AI）
        public final List<FoodItemDto> manual;     // 无效/非食物 → 手动补录（source=MANUAL）
        public final Result.Status status;
        public final VisionError error;
        public final boolean degraded;

        public SplitResult(List<FoodItemDto> confirmed, List<FoodItemDto> manual,
                           Result.Status status, VisionError error, boolean degraded) {
            this.confirmed = confirmed;
            this.manual = manual;
            this.status = status;
            this.error = error;
            this.degraded = degraded;
        }
    }

    /** 异步删除。 */
    public void delete(FoodLog log) {
        executors.diskIO().execute(() -> foodLogDao.delete(log));
    }
}
