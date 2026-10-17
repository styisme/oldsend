package org.oldsend.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.CheckBox;

public class Md3Switch extends CheckBox {
    private Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF trackRect = new RectF();

    public Md3Switch(Context context) {
        super(context);
        init();
    }

    public Md3Switch(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Md3Switch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        trackPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setColor(Color.WHITE);
        thumbPaint.setShadowLayer(2f, 0f, 2f, 0x40000000);
        // 强制开启软件渲染以支持阴影（针对低版本兼容）
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 彻底移除原生的绘制
        // super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        float radius = height / 2f;

        // 画背景 Track
        trackRect.set(0, 0, width, height);
        if (isChecked()) {
            trackPaint.setColor(0xFF388E3C); // 选中时的深绿色
        } else {
            trackPaint.setColor(0xFFE0E0E0); // 未选中时的浅灰色
        }
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint);

        // 画滑块 Thumb
        float thumbRadius = radius - 4f; // 留出内边距
        float thumbX;
        if (isChecked()) {
            thumbX = width - radius;
        } else {
            thumbX = radius;
        }
        canvas.drawCircle(thumbX, radius, thumbRadius, thumbPaint);
    }
}
