package com.esn.fitdiet.ui.custom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * PixelProgressBar 测试（F5 像素格进度条核心）。
 * Robolectric 提供 Context 与 Canvas/Bitmap，验证 setter 状态 + onDraw 不崩溃。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.TIRAMISU)
public class PixelProgressBarTest {

    @Test
    public void setters_updateState() {
        Context ctx = ApplicationProvider.getApplicationContext();
        PixelProgressBar bar = new PixelProgressBar(ctx);
        bar.setMax(100);
        bar.setProgress(40);
        bar.setSegmentCount(8);

        assertEquals(100, bar.getMax());
        assertEquals(40, bar.getProgress());
        assertEquals(8, bar.getSegmentCount());
    }

    @Test
    public void draw_continuous_doesNotCrash() {
        Context ctx = ApplicationProvider.getApplicationContext();
        PixelProgressBar bar = new PixelProgressBar(ctx);
        bar.setMax(100);
        bar.setProgress(60);          // 连续模式
        bar.setSegmentCount(0);

        Bitmap bmp = Bitmap.createBitmap(200, 16, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        bar.onDraw(canvas);             // 不应抛异常
        assertNotNull(bmp);
    }

    @Test
    public void draw_segmented_doesNotCrash() {
        Context ctx = ApplicationProvider.getApplicationContext();
        PixelProgressBar bar = new PixelProgressBar(ctx);
        bar.setMax(100);
        bar.setProgress(75);
        bar.setSegmentCount(8);         // 分段模式

        Bitmap bmp = Bitmap.createBitmap(200, 16, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        bar.onDraw(canvas);             // 分段填充路径
        assertNotNull(bmp);
    }

    @Test
    public void draw_zeroProgress_skipsFill() {
        Context ctx = ApplicationProvider.getApplicationContext();
        PixelProgressBar bar = new PixelProgressBar(ctx);
        bar.setMax(100);
        bar.setProgress(0);          // 0 进度：仅背景

        Bitmap bmp = Bitmap.createBitmap(200, 16, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        bar.onDraw(canvas);             // 不抛异常
        assertNotNull(bmp);
    }
}
