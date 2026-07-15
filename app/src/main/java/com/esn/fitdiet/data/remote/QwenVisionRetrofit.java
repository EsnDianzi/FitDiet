package com.esn.fitdiet.data.remote;

import android.util.Base64;
import com.esn.fitdiet.data.remote.dto.FoodItemDto;
import com.esn.fitdiet.data.remote.dto.QwenVisionRequest;
import com.esn.fitdiet.data.remote.dto.QwenVisionResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * 通义千问 VL 的 Retrofit 实现。
 * 将 DashScope 线格式归一为 Result<T>（code / msg / retryable）。
 */
public class QwenVisionRetrofit implements QwenVisionService {

    private static final String BASE_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/";
    private static final String MODEL = "qwen-vl-max";

    private final Api api;
    private final String apiKey;
    private static final Gson gson = new Gson();

    public QwenVisionRetrofit(String apiKey) {
        this.apiKey = apiKey;
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .build()))
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.api = retrofit.create(Api.class);
    }

    private interface Api {
        @POST("generation")
        Call<QwenVisionResponse> recognize(@Body QwenVisionRequest req);
    }

    @Override
    public Result<List<FoodItemDto>> recognize(byte[] imageJpeg, String prompt) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return Result.fail(VisionError.unauthorized("未配置 DashScope API Key"));
        }

        String base64 = "data:image/jpeg;base64,"
                + Base64.encodeToString(imageJpeg, Base64.NO_WRAP);
        QwenVisionRequest request = new QwenVisionRequest(MODEL, base64, prompt);

        try {
            Response<QwenVisionResponse> response = api.recognize(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                return parseBody(response.body());
            } else {
                return mapHttpError(response.code(), response.message());
            }
        } catch (SocketTimeoutException e) {
            return Result.fail(VisionError.timeout(e.getMessage()));
        } catch (IOException e) {
            return Result.fail(VisionError.networkError(e.getMessage()));
        }
    }

    /**
     * 解析模型响应正文。先剥离噪声文字（§3.3 / TC-F3-02），再逐条校验。
     * 成功返回全部条目（含无效项），由上层 {@code recognizeAndSplit} 路由待确认/手动补录。
     */
    static Result<List<FoodItemDto>> parseBody(QwenVisionResponse body) {
        if (body.output == null
                || body.output.choices == null
                || body.output.choices.isEmpty()
                || body.output.choices.get(0).message == null
                || body.output.choices.get(0).message.content == null
                || body.output.choices.get(0).message.content.isEmpty()) {
            return Result.fail(VisionError.parseError("响应结构不完整"));
        }

        String text = body.output.choices.get(0).message.content.get(0).text;
        if (text == null || text.trim().isEmpty()) {
            return Result.fail(VisionError.parseError("模型返回空文本"));
        }

        // §3.3：从噪声文字中抽取 JSON 数组
        String json = JsonExtractor.extractJsonArray(text);
        if (json == null) {
            return Result.fail(VisionError.parseError("未找到 JSON 数组"));
        }

        try {
            Type listType = new TypeToken<List<FoodItemDto>>() {}.getType();
            List<FoodItemDto> items = gson.fromJson(json, listType);
            if (items == null || items.isEmpty()) {
                return Result.fail(VisionError.parseError("未识别到食物"));
            }

            List<FoodItemDto> valid = new ArrayList<>();
            List<FoodItemDto> invalid = new ArrayList<>();
            for (FoodItemDto item : items) {
                if (item != null && item.isValid()) {
                    valid.add(item);
                } else {
                    invalid.add(item);
                }
            }

            if (valid.isEmpty()) {
                // 全部无效/非食物（如风景照）：返回全部条目，上层整批路由手动补录
                return Result.ok(items);
            }

            if (!invalid.isEmpty()) {
                // 部分成功：data 携带全部条目，status=PARTIAL 触发失败项手动补录
                return Result.partial(items, VisionError.partialSuccess(
                        "部分项解析失败：成功 " + valid.size() + "/" + items.size()));
            }

            return Result.ok(valid);
        } catch (JsonSyntaxException e) {
            return Result.fail(VisionError.parseError(e.getMessage()));
        }
    }

    private Result<List<FoodItemDto>> mapHttpError(int code, String msg) {
        switch (code) {
            case 400:
                return Result.fail(VisionError.badRequest(msg));
            case 401:
                return Result.fail(VisionError.unauthorized(msg));
            case 403:
                return Result.fail(VisionError.forbidden(msg));
            case 404:
                return Result.fail(VisionError.modelNotFound(msg));
            case 429:
                return Result.fail(VisionError.rateLimited(msg));
            default:
                if (code >= 500) {
                    return Result.fail(VisionError.serverError(msg));
                }
                return Result.fail(VisionError.serverError("HTTP " + code + " " + msg));
        }
    }
}
