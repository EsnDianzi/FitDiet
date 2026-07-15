package com.esn.fitdiet.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/**
 * DashScope 通义千问 VL 请求体。
 */
public class QwenVisionRequest {

    @SerializedName("model")
    public String model;

    @SerializedName("input")
    public Input input;

    @SerializedName("parameters")
    public Parameters parameters;

    public QwenVisionRequest(String model, String imageBase64, String prompt) {
        this.model = model;
        this.input = new Input();
        this.input.messages = new ArrayList<>();

        Message message = new Message();
        message.role = "user";
        message.content = new ArrayList<>();

        Content image = new Content();
        image.image = imageBase64;
        Content text = new Content();
        text.text = prompt;

        message.content.add(image);
        message.content.add(text);
        this.input.messages.add(message);

        this.parameters = new Parameters();
        this.parameters.resultFormat = "message";
    }

    public static class Input {
        @SerializedName("messages")
        public List<Message> messages;
    }

    public static class Message {
        @SerializedName("role")
        public String role;
        @SerializedName("content")
        public List<Content> content;
    }

    public static class Content {
        @SerializedName("image")
        public String image;
        @SerializedName("text")
        public String text;
    }

    public static class Parameters {
        @SerializedName("result_format")
        public String resultFormat;
    }
}
