package cn.youth996.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class BatteryView extends View {
    
    private Paint borderPaint;
    private Paint fillPaint;
    private Paint textPaint;
    private RectF batteryRect;
    
    private int batteryLevel = 85;
    private int range = 120;
    private static final int BORDER_WIDTH = 3;
    
    public BatteryView(Context context) {
        super(context);
        init();
    }
    
    public BatteryView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public BatteryView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(BORDER_WIDTH);
        borderPaint.setColor(Color.WHITE);
        
        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.parseColor("#00FF88")); // 亮绿色
        
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(28);
        
        batteryRect = new RectF();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int width = getWidth();
        int height = getHeight();
        
        int batteryWidth = 60;
        int batteryHeight = 30;
        int tipWidth = 4;
        int tipHeight = 10;
        
        int batteryLeft = 0;
        int batteryTop = (height - batteryHeight) / 2;
        
        float fillPercent = batteryLevel / 100f;
        int fillWidth = (int) ((batteryWidth - BORDER_WIDTH * 2) * fillPercent);
        
        batteryRect.set(batteryLeft, batteryTop, batteryLeft + batteryWidth, batteryTop + batteryHeight);
        canvas.drawRoundRect(batteryRect, 4, 4, borderPaint);
        
        if (fillWidth > 0) {
            RectF fillRect = new RectF(
                batteryLeft + BORDER_WIDTH,
                batteryTop + BORDER_WIDTH,
                batteryLeft + BORDER_WIDTH + fillWidth,
                batteryTop + batteryHeight - BORDER_WIDTH
            );
            canvas.drawRoundRect(fillRect, 2, 2, fillPaint);
        }
        
        int tipLeft = batteryLeft + batteryWidth;
        int tipTop = batteryTop + (batteryHeight - tipHeight) / 2;
        RectF tipRect = new RectF(tipLeft, tipTop, tipLeft + tipWidth, tipTop + tipHeight);
        canvas.drawRoundRect(tipRect, 2, 2, borderPaint);
        
        String rangeText = range + "km";
        float textX = batteryLeft + batteryWidth + tipWidth + 10;
        float textY = height / 2 + textPaint.getTextSize() / 3;
        canvas.drawText(rangeText, textX, textY, textPaint);
        
        String percentText = batteryLevel + "%";
        float percentTextSize = 18;
        textPaint.setTextSize(percentTextSize);
        float percentX = batteryLeft + (batteryWidth - textPaint.measureText(percentText)) / 2;
        float percentY = batteryTop + (batteryHeight + percentTextSize) / 2 - 2;
        
        textPaint.setColor(Color.WHITE);
        canvas.drawText(percentText, percentX, percentY, textPaint);
        textPaint.setColor(Color.WHITE);
    }
    
    public void setBatteryLevel(int level) {
        this.batteryLevel = Math.max(0, Math.min(100, level));
        invalidate();
    }
    
    public int getBatteryLevel() {
        return batteryLevel;
    }
    
    public void setRange(int range) {
        this.range = range;
        invalidate();
    }
    
    public int getRange() {
        return range;
    }
}
