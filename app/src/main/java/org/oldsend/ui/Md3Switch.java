package org.oldsend.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.CheckBox;

public class Md3Switch extends CheckBox {
    private Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint thumbShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF trackRect = new RectF();
    private float animProgress = 0f;

    public Md3Switch(Context context) {
        super(context);
        init();
    }

    public Md3Switch(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        trackPaint.setStyle(Paint.Style.FILL);
        thumbShadowPaint.setStyle(Paint.Style.FILL);
        thumbShadowPaint.setColor(0x33000000); 
        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setColor(Color.WHITE);
        setClickable(true);
        setFocusable(true);
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);
        float target = checked ? 1f : 0f;
        if (animProgress != target) {
            if (Build.VERSION.SDK_INT >= 11) {
                ValueAnimator animator = ValueAnimator.ofFloat(animProgress, target);
                animator.setDuration(200);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        animProgress = (float) animation.getAnimatedValue();
                        invalidate();
                    }
                });
                animator.start();
            } else {
                animProgress = target;
                invalidate();
            }
        }
    }

    @Override
    public boolean performClick() {
        toggle();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        float radius = height / 2f;
        trackRect.set(0, 0, width, height);
        int colorOff = 0xFFE0E0E0;
        int colorOn = 0xFF388E3C;
        int currentColor = evaluateColor(animProgress, colorOff, colorOn);
        trackPaint.setColor(currentColor);
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint);

        float thumbRadius = radius - 5f; 
        float minX = radius;
        float maxX = width - radius;
        float thumbX = minX + (maxX - minX) * animProgress;
        
        canvas.drawCircle(thumbX, radius + 1f, thumbRadius + 1f, thumbShadowPaint);
        canvas.drawCircle(thumbX, radius, thumbRadius, thumbPaint);
    }

    private int evaluateColor(float fraction, int startValue, int endValue) {
        int startA = (startValue >> 24) & 0xff;
        int startR = (startValue >> 16) & 0xff;
        int startG = (startValue >> 8) & 0xff;
        int startB = startValue & 0xff;
        int endA = (endValue >> 24) & 0xff;
        int endR = (endValue >> 16) & 0xff;
        int endG = (endValue >> 8) & 0xff;
        int endB = endValue & 0xff;
        return ((startA + (int)(fraction * (endA - startA))) << 24) |
               ((startR + (int)(fraction * (endR - startR))) << 16) |
               ((startG + (int)(fraction * (endG - startG))) << 8) |
               ((startB + (int)(fraction * (endB - startB))));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animProgress = isChecked() ? 1f : 0f;
        invalidate();
    }
}
