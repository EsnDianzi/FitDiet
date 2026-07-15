package com.esn.fitdiet.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * DashScope 通义千问 VL 响应体（成功 200）。
 */
public class QwenVisionResponse {

    @SerializedName("output")
    public Output output;

    @SerializedName("usage")
    public Usage usage;

    @SerializedName("request_id")
    public String requestId;

    public static class Output {
        @SerializedName("choices")
        public List<Choice> choices;
    }

    public static class Choice {
        @SerializedName("message")
        public Message message;

        @SerializedName("finish_reason")
        public String finishReason;
    }

    public static class Message {
        @SerializedName("content")
        public List<Content> content;
    }

    public static class Content {
        @SerializedName("text")
        public String text;
    }

    public static class Usage {
        @SerializedName("input_tokens")
        public int inputTokens;

        @SerializedName("output_tokens")
        public int outputTokens;
    }
}
