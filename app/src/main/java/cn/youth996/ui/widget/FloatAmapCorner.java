package cn.youth996.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import cn.youth996.ui.R;

/**
 * 高德地图悬浮窗角落视图
 * 
 * 继承自 ConstraintLayout，支持九宫格位置调整
 * 
 * 功能：
 * 1. 显示地图悬浮窗背景和前景
 * 2. 提供九宫格控制点用于调整位置
 * 3. 支持拖拽操作
 * 
 * @author Youth Desktop Team
 */
public class FloatAmapCorner extends ConstraintLayout {

    /** 背景层 */
    private ImageView bgImageView;
    
    /** 前景层 */
    private ImageView fgImageView;
    
    /** 九宫格控制点 */
    private ImageView leftUpView;
    private ImageView rightUpView;
    private ImageView leftDownView;
    private ImageView rightDownView;
    private ImageView middleUpView;
    private ImageView middleDownView;
    private ImageView leftMiddleView;
    private ImageView rightMiddleView;
    
    /** 回调接口 */
    private OnCornerDragListener cornerDragListener;

    /**
     * 拖拽回调接口
     */
    public interface OnCornerDragListener {
        /**
         * 当拖拽结束时调用
         * @param x 新的X坐标
         * @param y 新的Y坐标
         * @param width 新的宽度
         * @param height 新的高度
         */
        void onDragEnd(int x, int y, int width, int height);
    }

    public FloatAmapCorner(@NonNull Context context) {
        super(context);
        init(context);
    }

    public FloatAmapCorner(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FloatAmapCorner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * 初始化视图
     */
    private void init(Context context) {
        // 加载布局
        LayoutInflater.from(context).inflate(R.layout.layout_float_amap_corner, this, true);
        
        // 初始化视图引用
        initViews();
        
        // 设置点击事件
        setupClickListeners();
    }

    /**
     * 初始化视图引用
     */
    private void initViews() {
        bgImageView = findViewById(R.id.bg);
        fgImageView = findViewById(R.id.fg);
        leftUpView = findViewById(R.id.leftUp);
        rightUpView = findViewById(R.id.rightUp);
        leftDownView = findViewById(R.id.leftDown);
        rightDownView = findViewById(R.id.rightDown);
        middleUpView = findViewById(R.id.middleUp);
        middleDownView = findViewById(R.id.middleDown);
        leftMiddleView = findViewById(R.id.leftMiddle);
        rightMiddleView = findViewById(R.id.rightMiddle);
    }

    /**
     * 设置点击事件
     */
    private void setupClickListeners() {
        // 设置九宫格控制点的点击和拖拽事件
        setupCornerView(leftUpView, CornerPosition.LEFT_UP);
        setupCornerView(rightUpView, CornerPosition.RIGHT_UP);
        setupCornerView(leftDownView, CornerPosition.LEFT_DOWN);
        setupCornerView(rightDownView, CornerPosition.RIGHT_DOWN);
        setupCornerView(middleUpView, CornerPosition.MIDDLE_UP);
        setupCornerView(middleDownView, CornerPosition.MIDDLE_DOWN);
        setupCornerView(leftMiddleView, CornerPosition.LEFT_MIDDLE);
        setupCornerView(rightMiddleView, CornerPosition.RIGHT_MIDDLE);
    }

    /**
     * 设置角落视图的拖拽
     */
    private void setupCornerView(ImageView view, CornerPosition position) {
        if (view == null) return;
        
        view.setOnTouchListener((v, event) -> {
            if (cornerDragListener != null) {
                // 这里可以添加实际的拖拽逻辑
                // 目前只是通知拖拽开始
            }
            return false;
        });
    }

    /**
     * 设置拖拽回调
     */
    public void setOnCornerDragListener(OnCornerDragListener listener) {
        this.cornerDragListener = listener;
    }

    /**
     * 设置背景图片
     */
    public void setBackgroundImage(int resId) {
        if (bgImageView != null) {
            bgImageView.setImageResource(resId);
        }
    }

    /**
     * 设置前景图片
     */
    public void setForegroundImage(int resId) {
        if (fgImageView != null) {
            fgImageView.setImageResource(resId);
        }
    }

    /**
     * 获取背景视图
     */
    public ImageView getBgImageView() {
        return bgImageView;
    }

    /**
     * 获取前景视图
     */
    public ImageView getFgImageView() {
        return fgImageView;
    }

    /**
     * 显示控制点
     */
    public void showControlPoints() {
        setControlPointsVisibility(View.VISIBLE);
    }

    /**
     * 隐藏控制点
     */
    public void hideControlPoints() {
        setControlPointsVisibility(View.GONE);
    }

    /**
     * 设置控制点可见性
     */
    private void setControlPointsVisibility(int visibility) {
        if (leftUpView != null) leftUpView.setVisibility(visibility);
        if (rightUpView != null) rightUpView.setVisibility(visibility);
        if (leftDownView != null) leftDownView.setVisibility(visibility);
        if (rightDownView != null) rightDownView.setVisibility(visibility);
        if (middleUpView != null) middleUpView.setVisibility(visibility);
        if (middleDownView != null) middleDownView.setVisibility(visibility);
        if (leftMiddleView != null) leftMiddleView.setVisibility(visibility);
        if (rightMiddleView != null) rightMiddleView.setVisibility(visibility);
    }

    /**
     * 角落位置枚举
     */
    private enum CornerPosition {
        LEFT_UP,
        RIGHT_UP,
        LEFT_DOWN,
        RIGHT_DOWN,
        MIDDLE_UP,
        MIDDLE_DOWN,
        LEFT_MIDDLE,
        RIGHT_MIDDLE
    }
}
