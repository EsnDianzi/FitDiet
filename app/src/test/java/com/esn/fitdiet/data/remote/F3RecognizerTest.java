package com.esn.fitdiet.data.remote;

import com.esn.fitdiet.data.remote.dto.FoodItemDto;
import com.esn.fitdiet.data.remote.dto.QwenVisionResponse;
import com.esn.fitdiet.data.repository.FoodRepository;
import com.esn.fitdiet.data.local.entity.FoodLog;
import com.esn.fitdiet.domain.model.FoodSource;
import com.esn.fitdiet.domain.model.MealType;
import com.esn.fitdiet.util.AppExecutors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * F3 拍照识别 AI 调用层测试（TC-F3-01 ~ TC-F3-13）。
 *
 * <p>Robolectric 提供主线程 {@code Looper}，使 {@link AppExecutors} 可实例化（与 BattleIntegrationTest 同模式）。
 * 通过不同 {@link QwenVisionMock} 桩 + 解析工具单测覆盖成功 / 噪声 / 缺字段 / 错误码 / 限流重试 / 非食物等全路径。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class F3RecognizerTest {

    private static FoodRepository repoWith(QwenVisionService svc) {
        // 退避 0ms 避免测试真实 sleep；dao 传 null（recognizeAndSplit/buildLogs 不触达）
        return new FoodRepository(null, svc, AppExecutors.getInstance(), 0L);
    }

    private static final String DATE = "2026-07-10";
    private static final MealType MEAL = MealType.LUNCH;

    // ===== TC-F3-01 成功识别 =====
    @Test
    public void f3_01_success() {
        FoodRepository repo = repoWith(new QwenVisionMock(QwenVisionMock.STUB_OK));
        FoodRepository.SplitResult r = repo.recognizeAndSplit(new byte[0], "识别食物");
        assertTrue("isSuccess", r.status == Result.Status.SUCCESS);
        assertEquals("2 项待确认", 2, r.confirmed.size());
        assertTrue("无手动项", r.manual.isEmpty());
        assertFalse("未降级", r.degraded);
    }

    // ===== TC-F3-02 返回文本含噪声（剥离 JSON 数组） =====
    @Test
    public void f3_02_noiseStripped() {
        String noisy = "以下是识别结果：\n"
                + "[{\"name\":\"米饭\",\"weightG\":150,\"calories\":174},"
                + "{\"name\":\"鸡胸肉\",\"weightG\":100,\"calories\":165}]\n以上为结果。";
        String json = JsonExtractor.extractJsonArray(noisy);
        assertNotNull("抽到 JSON 数组", json);
        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]"));

        List<FoodItemDto> parsed = new Gson().fromJson(json, listType());
        assertEquals("解析出 2 项", 2, parsed.size());
        assertEquals("米饭", parsed.get(0).name);
    }

    @Test
    public void f3_02_retrofitParseHandlesNoise() {
        QwenVisionResponse resp = noisyResponse(
                "说明：\n[{\"name\":\"米饭\",\"weightG\":150,\"calories\":174}]");
        Result<List<FoodItemDto>> r = QwenVisionRetrofit.parseBody(resp);
        assertTrue("isSuccess", r.isSuccess());
        assertEquals(1, r.getData().size());
        assertEquals("米饭", r.getData().get(0).name);
    }

    // ===== TC-F3-03 单条缺字段（fiber 默认 0） =====
    @Test
    public void f3_03_missingFieldDefaults() {
        String json = "[{\"name\":\"米饭\",\"weightG\":150,\"calories\":174}]";
        List<FoodItemDto> items = new Gson().fromJson(json, listType());
        assertEquals(1, items.size());
        assertEquals(0.0, items.get(0).fiber, 0.0001); // fiber 默认 0
    }

    // ===== TC-F3-04 401 错误 =====
    @Test
    public void f3_04_unauthorized401() {
        FoodRepository repo = repoWith(new QwenVisionMock(QwenVisionMock.STUB_UNAUTHORIZED));
        FoodRepository.SplitResult r = repo.recognizeAndSplit(new byte[0], "");
        assertTrue("isFailure", r.status == Result.Status.FAILURE);
        assertEquals(401, r.error.code);
        assertTrue("降级", r.degraded);
    }

    // ===== TC-F3-05 429 限流退避重试 =====
    @Test
    public void f3_05_rateLimited_thenRetrySuccess() {
        FoodRepository repo = repoWith(new QwenVisionMock(QwenVisionMock.STUB_RATE_LIMITED_THEN_OK));
        FoodRepository.SplitResult r = repo.recognizeAndSplit(new byte[0], "");
        assertTrue("重试后成功", r.status == Result.Status.SUCCESS);
        assertEquals(2, r.confirmed.size());
        assertFalse("未降级", r.degraded);
    }

    @Test
    public void f3_05_rateLimited_persistent_fallbackManual() {
        FoodRepository repo = repoWith(new QwenVisionMock(QwenVisionMock.STUB_RATE_LIMITED_PERSISTENT));
        FoodRepository.SplitResult r = repo.recognizeAndSplit(new byte[0], "");
        assertTrue("重试仍失败", r.status == Result.Status.FAILURE);
        assertEquals(429, r.error.code);
        assertTrue("降级手动", r.degraded);
    }

    // ===== TC-F3-06 网络错误 =====
    @Test
    public void f3_06_networkError1000() {
        FoodRepository repo = repoWith(new QwenVisionMock(QwenVisionMock.STUB_NETWORK_ERROR));
        FoodRepository.SplitResult r = repo.recognizeAndSplit(new byte[0], "");
        assertTrue("isFailure", r.status == Result.Status.FAILURE);
        assertEquals(1000, r.error.code);
        assertTrue("降级", r.degraded);
    }

    // ===== TC-F3-07 解析失败 =====
    @Test
    public void f3_07_parseError1003() {
        FoodRepository repo = repoWith(new QwenVisionMock(QwenVisionMock.STUB_EMPTY));
        FoodRepository.SplitResult r = repo.recognizeAndSplit(new byte[0], "");
        assertTrue("isFailure", r.status == Result.Status.FAILURE);
        assertEquals(1003, r.error.code);
    }

    // ===== TC-F3-08 无 Key（不走网络，直接 unauthorized） =====
    @Test
    public void f3_08_noKey_unauthorized() {
        QwenVisionRetrofit retrofit = new QwenVisionRetrofit("");
        Result<List<FoodItemDto>> r = retrofit.recognize(new byte[0], "");
        assertTrue("isFailure", r.isFailure());
        assertEquals(401, r.getError().code);
        assertFalse("不可重试（需补全 Key）", r.getError().retryable);
    }

    // ===== TC-F3-09 source 标记 AI / MANUAL =====
    @Test
    public void f3_09_sourceMarker() {
        FoodRepository repo = repoWith(new QwenVisionMock(QwenVisionMock.STUB_OK));
        FoodRepository.SplitResult r = repo.recognizeAndSplit(new byte[0], "");
        List<FoodLog> aiLogs = repo.buildLogs(r.confirmed, FoodSource.AI, DATE, MEAL, null);
        assertEquals("2 条 AI 日志", 2, aiLogs.size());
        for (FoodLog f : aiLogs) assertEquals("source=AI", FoodSource.AI, f.source);

        // 手动补录路径
        FoodRepository repo2 = repoWith(new QwenVisionMock(QwenVisionMock.STUB_NON_FOOD));
        FoodRepository.SplitResult r2 = repo2.recognizeAndSplit(new byte[0], "");
        List<FoodLog> manualLogs = repo2.buildLogs(r2.manual, FoodSource.MANUAL, DATE, MEAL, null);
        assertEquals("1 条手动日志", 1, manualLogs.size());
        assertEquals("source=MANUAL", FoodSource.MANUAL, manualLogs.get(0).source);
    }

    // ===== TC-F3-10 部分成功（失败项转手动补录，不整体丢弃） =====
    @Test
    public void f3_10_partial_keepsFailedForManual() {
        FoodRepository repo = repoWith(new QwenVisionMock(QwenVisionMock.STUB_PARTIAL));
        FoodRepository.SplitResult r = repo.recognizeAndSplit(new byte[0], "");
        assertTrue("isPartial", r.status == Result.Status.PARTIAL);
        assertEquals(1002, r.error.code);
        assertEquals("2 项待确认", 2, r.confirmed.size());
        assertEquals("1 项转手动", 1, r.manual.size());     // 缺字段项未整体丢弃
        assertTrue("标记降级", r.degraded);
    }

    // ===== TC-F3-11 超大 JSON 不 OOM =====
    @Test
    public void f3_11_hugeJson_noOOM() {
        FoodRepository repo = repoWith(new QwenVisionMock(QwenVisionMock.STUB_HUGE_JSON));
        FoodRepository.SplitResult r = repo.recognizeAndSplit(new byte[0], "");
        assertTrue("isSuccess", r.status == Result.Status.SUCCESS);
        assertEquals("200 项", 200, r.confirmed.size());
    }

    // ===== TC-F3-12 空数组 → 手动流程（不崩溃） =====
    @Test
    public void f3_12_emptyArray_noCrash() {
        FoodRepository repo = repoWith(new QwenVisionMock(QwenVisionMock.STUB_EMPTY));
        FoodRepository.SplitResult r = repo.recognizeAndSplit(new byte[0], "");
        // 空数组 → 解析失败 → 降级手动，不抛异常
        assertTrue(r.degraded);
        assertTrue(r.manual.isEmpty() && r.confirmed.isEmpty());
    }

    // ===== TC-F3-13 非食物识别（风景照 → 业务校验路由手动） =====
    @Test
    public void f3_13_nonFood_routesManual() {
        FoodRepository repo = repoWith(new QwenVisionMock(QwenVisionMock.STUB_NON_FOOD));
        FoodRepository.SplitResult r = repo.recognizeAndSplit(new byte[0], "");
        // 解析成功但条目无营养字段 → 全部转入手动补录
        assertTrue("无待确认项", r.confirmed.isEmpty());
        assertEquals("1 项非食物转手动", 1, r.manual.size());
        assertEquals("蓝天", r.manual.get(0).name);
        assertTrue("整体降级", r.degraded);
    }

    // ===== 噪声剥离边界（JsonExtractor 纯函数） =====
    @Test
    public void jsonExtractor_noBracket_returnsNull() {
        assertNull(JsonExtractor.extractJsonArray("没有任何数组的纯文本"));
    }

    @Test
    public void jsonExtractor_nestedArray() {
        String text = "前缀[{\"name\":\"a\",\"tags\":[\"x\",\"y\"]}]后缀";
        String json = JsonExtractor.extractJsonArray(text);
        assertNotNull(json);
        List<FoodItemDto> items = new Gson().fromJson(json, listType());
        assertEquals(1, items.size());
        assertEquals("a", items.get(0).name);
    }

    // ===== 工具方法 =====
    private static Type listType() {
        return new TypeToken<List<FoodItemDto>>() {}.getType();
    }

    private static QwenVisionResponse noisyResponse(String text) {
        QwenVisionResponse.Output out = new QwenVisionResponse.Output();
        QwenVisionResponse.Choice ch = new QwenVisionResponse.Choice();
        QwenVisionResponse.Message msg = new QwenVisionResponse.Message();
        QwenVisionResponse.Content ct = new QwenVisionResponse.Content();
        ct.text = text;
        msg.content = Arrays.asList(ct);
        ch.message = msg;
        out.choices = new ArrayList<>(Arrays.asList(ch));
        QwenVisionResponse resp = new QwenVisionResponse();
        resp.output = out;
        return resp;
    }
}
