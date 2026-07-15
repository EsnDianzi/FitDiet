package com.esn.fitdiet.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.esn.fitdiet.data.repository.StatsRepository;

import java.util.List;

/**
 * 体重折线图（自绘、零依赖）。
 * 渲染策略：
 * - 至少 2 个数据点才能画折线
 * - Y 轴自适应体重 min~max，并留 5% 余量
 * - X 轴等距分布，底部标注首/末两个日期
 * - 暗色背景，画布内圆角矩形
 */
public class WeightChartView extends View {

    private static final int COLOR_BG = 0xFF2a2a2c;       // apple_surface
    private static final int COLOR_LINE = 0xFF34C759;      // apple_accent
    private static final int COLOR_POINT = 0xFFFFFFFF;
    private static final int COLOR_TEXT = 0xFF999999;      // apple_text_tertiary
    private static final int COLOR_GRID = 0xFF3a3a3c;      // apple_divider

    private List<StatsRepository.WeightPoint> points;

    private final Paint paintBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintPoint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGrid = new Paint(Paint.ANTI_ALIAS_FLAG);

    public WeightChartView(Context context) {
        super(context);
        init();
    }

    public WeightChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WeightChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paintBg.setColor(COLOR_BG);
        paintBg.setStyle(Paint.Style.FILL);

        paintLine.setColor(COLOR_LINE);
        paintLine.setStyle(Paint.Style.STROKE);
        paintLine.setStrokeWidth(dp(2));
        paintLine.setStrokeJoin(Paint.Join.ROUND);
        paintLine.setStrokeCap(Paint.Cap.ROUND);

        paintPoint.setColor(COLOR_POINT);
        paintPoint.setStyle(Paint.Style.FILL);

        paintText.setColor(COLOR_TEXT);
        paintText.setTextSize(sp(11));

        paintGrid.setColor(COLOR_GRID);
        paintGrid.setStyle(Paint.Style.STROKE);
        paintGrid.setStrokeWidth(dp(0.5f));
    }

    /** 注入体重数据点。传 null/empty 时显示空态文字。 */
    public void setPoints(List<StatsRepository.WeightPoint> pts) {
        this.points = pts;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();

        // 背景圆角矩形
        canvas.drawRoundRect(0, 0, w, h, dp(8), dp(8), paintBg);

        float padL = dp(16);
        float padR = dp(16);
        float padT = dp(20);
        float padB = dp(28); // 留出底部日期文字空间
        float chartW = w - padL - padR;
        float chartH = h - padT - padB;

        if (points == null || points.isEmpty()) {
            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setColor(0xFF999999);
            canvas.drawText("暂无体重数据", w / 2f, h / 2f, paintText);
            return;
        }

        if (points.size() == 1) {
            // 只有 1 个点：画中心点 + 文字
            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setColor(0xFF999999);
            StatsRepository.WeightPoint p = points.get(0);
            canvas.drawCircle(padL + chartW / 2f, padT + chartH / 2f, dp(5), paintPoint);
            String label = p.weightKg + " kg  ·  " + p.date.substring(5); // MM-dd
            canvas.drawText(label, w / 2f, padT + chartH / 2f + dp(28), paintText);
            return;
        }

        // 计算 min/max 体重
        double minW = points.get(0).weightKg;
        double maxW = points.get(0).weightKg;
        for (StatsRepository.WeightPoint p : points) {
            if (p.weightKg < minW) minW = p.weightKg;
            if (p.weightKg > maxW) maxW = p.weightKg;
        }
        // 5% padding 防止折线贴边
        double range = Math.max(0.1, maxW - minW);
        double pad = range * 0.1;
        minW -= pad;
        maxW += pad;
        if (maxW - minW < 0.5) {
            // 体重变化太小时，上下再各加 0.25kg 防止完全平线
            minW -= 0.25;
            maxW += 0.25;
        }

        // Y 轴 3 条横向网格（底部/中间/顶部）
        paintText.setTextAlign(Paint.Align.RIGHT);
        paintText.setColor(COLOR_TEXT);
        for (int i = 0; i <= 2; i++) {
            float y = padT + chartH * (1f - i / 2f);
            canvas.drawLine(padL, y, padL + chartW, y, paintGrid);
            double val = minW + (maxW - minW) * (i / 2f);
            canvas.drawText(String.format("%.1f", val), padL - dp(4), y + dp(4), paintText);
        }

        // 折线路径
        Path path = new Path();
        for (int i = 0; i < points.size(); i++) {
            StatsRepository.WeightPoint p = points.get(i);
            float x = padL + chartW * ((float) i / (points.size() - 1));
            float y = padT + chartH * (float) ((1f - (p.weightKg - minW) / (maxW - minW)));
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        canvas.drawPath(path, paintLine);

        // 数据点 + 标签
        paintText.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < points.size(); i++) {
            StatsRepository.WeightPoint p = points.get(i);
            float x = padL + chartW * ((float) i / (points.size() - 1));
            float y = padT + chartH * (float) ((1f - (p.weightKg - minW) / (maxW - minW)));
            canvas.drawCircle(x, y, dp(4), paintPoint);
        }

        // 首/末两个日期标注在底部
        paintText.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(points.get(0).date.substring(5),
                padL, padT + chartH + dp(18), paintText);
        paintText.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(points.get(points.size() - 1).date.substring(5),
                padL + chartW, padT + chartH + dp(18), paintText);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private float sp(float v) {
        return v * getResources().getDisplayMetrics().scaledDensity;
    }
}
