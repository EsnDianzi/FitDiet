package com.esn.fitdiet.data.remote;

/**
 * 应用内统一返回格式（三态）：SUCCESS / FAILURE / PARTIAL。
 * 所有远程/AI 调用、Repository 对外暴露均使用此类型。
 *
 * @param <T> 成功/部分成功时携带的数据类型
 */
public final class Result<T> {

    public enum Status {
        SUCCESS, FAILURE, PARTIAL
    }

    private final T data;
    private final VisionError error;
    private final Status status;

    private Result(T data, VisionError error, Status status) {
        this.data = data;
        this.error = error;
        this.status = status;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(data, null, Status.SUCCESS);
    }

    public static <T> Result<T> fail(VisionError error) {
        return new Result<>(null, error, Status.FAILURE);
    }

    public static <T> Result<T> partial(T data, VisionError error) {
        return new Result<>(data, error, Status.PARTIAL);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isFailure() {
        return status == Status.FAILURE;
    }

    public boolean isPartial() {
        return status == Status.PARTIAL;
    }

    public Status getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }

    public VisionError getError() {
        return error;
    }

    public boolean hasData() {
        return data != null;
    }
}
