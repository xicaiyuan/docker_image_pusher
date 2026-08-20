package com.wetype.voicebubble;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/**
 * Dependency-free floating mic button. The home state looks like a compact system control;
 * docked state becomes a translucent target so the real WeChat mic remains visually readable.
 */
public class MicBubbleView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean docked = false;
    private boolean pressedVisual = false;

    public MicBubbleView(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setStyle(Paint.Style.STROKE);
    }

    public void setDocked(boolean value) {
        docked = value;
        invalidate();
    }

    public void setPressedVisual(boolean value) {
        pressedVisual = value;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float r = Math.min(w, h) / 2f;

        float pressScale = pressedVisual ? 0.91f : 1f;
        canvas.save();
        canvas.scale(pressScale, pressScale, cx, cy);

        if (docked) {
            fill.clearShadowLayer();
            fill.setStyle(Paint.Style.FILL);
            fill.setShader(new RadialGradient(cx, cy, r,
                    new int[]{0x1206B6D4, 0x2A14B8A6, 0x5514B8A6},
                    new float[]{0f, 0.72f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, r * 0.96f, fill);
            fill.setShader(null);

            stroke.setColor(0xBFFFFFFF);
            stroke.setStrokeWidth(Math.max(2f, w * 0.035f));
            canvas.drawCircle(cx, cy, r * 0.82f, stroke);
            drawMic(canvas, cx, cy, w, h, 0xEFFFFFFF);
            canvas.restore();
            return;
        }

        fill.setStyle(Paint.Style.FILL);
        fill.setColor(0x220EA5E9);
        canvas.drawCircle(cx, cy, r * 0.98f, fill);

        fill.setShadowLayer(Ui.dp(getContext(), 9), 0, Ui.dp(getContext(), 4), 0x42000000);
        fill.setShader(new LinearGradient(0, 0, w, h,
                new int[]{Color.rgb(14, 165, 233), Color.rgb(6, 182, 212), Color.rgb(20, 184, 166)},
                null, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r * 0.84f, fill);
        fill.setShader(null);
        fill.clearShadowLayer();

        stroke.setColor(0x55FFFFFF);
        stroke.setStrokeWidth(Math.max(1.5f, w * 0.022f));
        canvas.drawCircle(cx, cy, r * 0.73f, stroke);

        drawMic(canvas, cx, cy, w, h, Color.WHITE);
        canvas.restore();
    }

    private void drawMic(Canvas canvas, float cx, float cy, float w, float h, int color) {
        float bodyW = w * 0.19f;
        float bodyH = h * 0.30f;
        RectF body = new RectF(cx - bodyW / 2f, cy - h * 0.20f,
                cx + bodyW / 2f, cy - h * 0.20f + bodyH);
        fill.setColor(color);
        fill.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(body, bodyW / 2f, bodyW / 2f, fill);

        stroke.setColor(color);
        stroke.setStrokeWidth(Math.max(2.6f, w * 0.052f));
        RectF arc = new RectF(cx - w * 0.19f, cy - h * 0.10f,
                cx + w * 0.19f, cy + h * 0.19f);
        canvas.drawArc(arc, 0, 180, false, stroke);
        canvas.drawLine(cx, cy + h * 0.18f, cx, cy + h * 0.29f, stroke);
        canvas.drawLine(cx - w * 0.11f, cy + h * 0.29f,
                cx + w * 0.11f, cy + h * 0.29f, stroke);
    }
}
