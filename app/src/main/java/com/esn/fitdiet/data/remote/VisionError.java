package com.esn.fitdiet.data.remote;

/**
 * 应用内统一错误类型，所有远程/AI 调用失败均归一为此对象。
 * 字段：code / msg / retryable，便于 UI 统一处理错误与重试。
 */
public final class VisionError {

    // HTTP 原样映射段
    public static final int CODE_BAD_REQUEST = 400;
    public static final int CODE_UNAUTHORIZED = 401;
    public static final int CODE_FORBIDDEN = 403;
    public static final int CODE_MODEL_NOT_FOUND = 404;
    public static final int CODE_RATE_LIMITED = 429;
    public static final int CODE_SERVER_ERROR = 500;

    // 本地扩展段
    public static final int CODE_NETWORK_ERROR = 1000;
    public static final int CODE_TIMEOUT = 1001;
    public static final int CODE_PARTIAL_SUCCESS = 1002;
    public static final int CODE_PARSE_ERROR = 1003;

    public final int code;
    public final String msg;
    public final boolean retryable;

    public VisionError(int code, String msg, boolean retryable) {
        this.code = code;
        this.msg = msg;
        this.retryable = retryable;
    }

    public static VisionError badRequest(String msg) {
        return new VisionError(CODE_BAD_REQUEST, msg, false);
    }

    public static VisionError unauthorized(String msg) {
        return new VisionError(CODE_UNAUTHORIZED, msg, false);
    }

    public static VisionError forbidden(String msg) {
        return new VisionError(CODE_FORBIDDEN, msg, false);
    }

    public static VisionError modelNotFound(String msg) {
        return new VisionError(CODE_MODEL_NOT_FOUND, msg, false);
    }

    public static VisionError rateLimited(String msg) {
        return new VisionError(CODE_RATE_LIMITED, msg, true);
    }

    public static VisionError serverError(String msg) {
        return new VisionError(CODE_SERVER_ERROR, msg, true);
    }

    public static VisionError networkError(String msg) {
        return new VisionError(CODE_NETWORK_ERROR, msg, true);
    }

    public static VisionError timeout(String msg) {
        return new VisionError(CODE_TIMEOUT, msg, true);
    }

    public static VisionError partialSuccess(String msg) {
        return new VisionError(CODE_PARTIAL_SUCCESS, msg, true);
    }

    public static VisionError parseError(String msg) {
        return new VisionError(CODE_PARSE_ERROR, msg, false);
    }

    /** 是否属于网络层错误（网络断开/超时），可重试。 */
    public boolean isNetworkError() {
        return code == CODE_NETWORK_ERROR || code == CODE_TIMEOUT;
    }
}
