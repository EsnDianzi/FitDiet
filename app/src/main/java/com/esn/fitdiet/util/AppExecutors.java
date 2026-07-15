package com.esn.fitdiet.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 应用全局线程池（Java ExecutorService，无协程）。
 *
 * diskIO  : 供 Room / 网络 IO 使用（4 线程缓存池）
 * mainThread : 通过主线程 Handler post Runnable
 * singleThread : 串行写入（避免 Room 并发冲突）
 */
public final class AppExecutors {

    private final ExecutorService diskIO;
    private final Executor mainThread;
    private final ExecutorService singleThread;

    private static volatile AppExecutors INSTANCE;

    private AppExecutors() {
        this.diskIO = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "FitDiet-diskIO");
            t.setDaemon(true);
            return t;
        });
        this.singleThread = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "FitDiet-single");
            t.setDaemon(true);
            return t;
        });
        Handler mainHandler = new Handler(Looper.getMainLooper());
        this.mainThread = mainHandler::post;
    }

    public static AppExecutors getInstance() {
        if (INSTANCE == null) {
            synchronized (AppExecutors.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AppExecutors();
                }
            }
        }
        return INSTANCE;
    }

    public ExecutorService diskIO() { return diskIO; }
    public Executor mainThread() { return mainThread; }
    public ExecutorService singleThread() { return singleThread; }
}
