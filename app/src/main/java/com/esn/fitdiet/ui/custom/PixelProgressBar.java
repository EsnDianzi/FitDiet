package com.esn.fitdiet.ui.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.esn.fitdiet.R;

/**
 * 像素格进度条（血条 / 经验条）。
 * 使用 nearest-neighbor 缩放防止模糊，1px 深色描边。
 *
 * XML 属性（通过 styleable 或直接 setter）：
 * - app:pixelProgress       当前值 (0..max)
 * - app:pixelMax            最大值
 * - app:pixelColor          填充色
 * - app:pixelSegmentCount   分段数（0=连续，>0=分段，每段间留 1dp 缝隙）
 */
public class PixelProgressBar extends View {

    private int max = 100;
    private int progress = 0;
    private int segmentCount = 0;
    private int fillColor = 0xFF4EC96B;   // pixel_green
    private int bgColor = 0xFF1A1C2C;     // pixel_outline
    private int borderColor = 0xFF000000;

    private final Paint fillPaint;
    private final Paint bgPaint;
    private final Rect tempRect;

    public PixelProgressBar(Context context) {
        this(context, null);
    }

    public PixelProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PixelProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setFilterBitmap(false);
        fillPaint.setStyle(Paint.Style.FILL);

        bgPaint = new Paint();
        bgPaint.setFilterBitmap(false);
        bgPaint.setStyle(Paint.Style.FILL);

        tempRect = new Rect();
    }

    public void setMax(int max) {
        this.max = Math.max(1, max);
        invalidate();
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(progress, max));
        invalidate();
    }

    public void setSegmentCount(int count) {
        this.segmentCount = Math.max(0, count);
        invalidate();
    }

    // ===== 测试用 getter =====
    public int getMax() { return max; }
    public int getProgress() { return progress; }
    public int getSegmentCount() { return segmentCount; }

    public void setFillColor(int color) {
        this.fillColor = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // 背景
        bgPaint.setColor(bgColor);
        canvas.drawRect(0, 0, w, h, bgPaint);

        if (progress <= 0) return;

        // 填充区域
        fillPaint.setColor(fillColor);

        if (segmentCount <= 1) {
            // 连续填充
            int fillW = (int) ((float) progress / max * w);
            canvas.drawRect(0, 0, fillW, h, fillPaint);
        } else {
            // 分段填充：每段宽度 = w/segmentCount，段间留 1dp 缝隙
            float dp = getResources().getDisplayMetrics().density;
            int gap = Math.max(1, (int) (1f * dp));
            int segmentW = (w - (segmentCount - 1) * gap) / segmentCount;
            int filledSegments = (int) ((float) progress / max * segmentCount);
            int x = 0;
            for (int i = 0; i < filledSegments; i++) {
                canvas.drawRect(x, 0, x + segmentW, h, fillPaint);
                x += segmentW + gap;
            }
        }

        // 1px 外框描边
        fillPaint.setColor(borderColor);
        fillPaint.setStyle(Paint.Style.STROKE);
        fillPaint.setStrokeWidth(1f);
        canvas.drawRect(0, 0, w, h, fillPaint);
        fillPaint.setStyle(Paint.Style.FILL);
    }
}
