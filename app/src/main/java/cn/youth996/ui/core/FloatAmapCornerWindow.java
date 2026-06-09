package cn.youth996.ui.core;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.WindowManager;

import cn.youth996.ui.widget.FloatAmapCorner;

/**
 * 高德地图悬浮窗窗口管理器
 * 
 * 负责创建和管理悬浮窗的 WindowManager 和 LayoutParams
 * 
 * 参考布丁UI的 FloatAmapCornerWindow 实现
 * 
 * @author Youth Desktop Team
 */
public class FloatAmapCornerWindow {

    private static final String TAG = "FloatAmapCornerWindow";

    /** 单例实例 */
    private static FloatAmapCornerWindow instance;

    /** WindowManager */
    private WindowManager windowManager;

    /** LayoutParams */
    private WindowManager.LayoutParams layoutParams;

    /** 悬浮窗视图 */
    private FloatAmapCorner floatAmapCorner;

    /** 是否已添加视图 */
    private boolean isViewAdded = false;

    /**
     * 私有构造函数（单例模式）
     */
    private FloatAmapCornerWindow(Context context) {
        // 获取 WindowManager
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        
        // 创建 LayoutParams
        layoutParams = new WindowManager.LayoutParams();
        
        // 配置 LayoutParams
        configLayoutParams();
    }

    /**
     * 获取单例实例
     */
    public static synchronized FloatAmapCornerWindow getInstance(Context context) {
        if (instance == null) {
            instance = new FloatAmapCornerWindow(context);
        }
        return instance;
    }

    /**
     * 配置 LayoutParams
     */
    private void configLayoutParams() {
        // 设置窗口类型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            Log.i(TAG, "使用 TYPE_APPLICATION_OVERLAY (2038)");
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
            Log.i(TAG, "使用 TYPE_PHONE");
        }

        // 设置格式
        layoutParams.format = android.graphics.PixelFormat.TRANSLUCENT;

        // 设置标志位
        // 264 = FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL | FLAG_WATCH_OUTSIDE_TOUCH
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                           WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                           WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

        // 设置对齐方式 - 居中
        layoutParams.gravity = android.view.Gravity.CENTER;

        // 设置尺寸
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        Log.i(TAG, "LayoutParams 配置完成: type=" + layoutParams.type + 
              ", flags=" + layoutParams.flags + 
              ", gravity=" + layoutParams.gravity);
    }

    /**
     * 显示悬浮窗
     *
     * @param context 上下文
     * @param x      X坐标
     * @param y      Y坐标
     * @param width  宽度
     * @param height 高度
     */
    public void show(Context context, int x, int y, int width, int height) {
        Log.i(TAG, "显示悬浮窗: x=" + x + ", y=" + y + 
              ", width=" + width + ", height=" + height);

        try {
            // 设置位置和尺寸
            layoutParams.x = x;
            layoutParams.y = y;
            layoutParams.width = width;
            layoutParams.height = height;

            if (!isViewAdded) {
                // 创建悬浮窗视图
                floatAmapCorner = new FloatAmapCorner(context);
                
                // 添加视图
                windowManager.addView(floatAmapCorner, layoutParams);
                isViewAdded = true;
                
                Log.i(TAG, "悬浮窗视图已添加");
            } else {
                // 更新视图
                windowManager.updateViewLayout(floatAmapCorner, layoutParams);
                Log.i(TAG, "悬浮窗视图已更新");
            }
        } catch (Exception e) {
            Log.e(TAG, "显示悬浮窗失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 显示悬浮窗（使用默认位置）
     *
     * @param context 上下文
     */
    public void show(Context context) {
        // 获取屏幕尺寸
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        
        // 默认显示在右上角
        int x = (int) (screenWidth * 0.5);  // 居中偏右
        int y = (int) (screenHeight * 0.1); // 10% 高度
        int width = (int) (screenWidth * 0.45); // 45% 宽度
        int height = (int) (screenHeight * 0.35); // 35% 高度
        
        show(context, x, y, width, height);
    }

    /**
     * 隐藏悬浮窗
     */
    public void hide() {
        Log.i(TAG, "隐藏悬浮窗");

        try {
            if (isViewAdded && floatAmapCorner != null) {
                windowManager.removeView(floatAmapCorner);
                floatAmapCorner = null;
                isViewAdded = false;
                Log.i(TAG, "悬浮窗视图已移除");
            }
        } catch (Exception e) {
            Log.e(TAG, "隐藏悬浮窗失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 更新悬浮窗位置
     *
     * @param x X坐标
     * @param y Y坐标
     */
    public void updatePosition(int x, int y) {
        Log.i(TAG, "更新位置: x=" + x + ", y=" + y);

        try {
            if (isViewAdded) {
                layoutParams.x = x;
                layoutParams.y = y;
                windowManager.updateViewLayout(floatAmapCorner, layoutParams);
                Log.i(TAG, "位置已更新");
            }
        } catch (Exception e) {
            Log.e(TAG, "更新位置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 更新悬浮窗尺寸
     *
     * @param width  宽度
     * @param height 高度
     */
    public void updateSize(int width, int height) {
        Log.i(TAG, "更新尺寸: width=" + width + ", height=" + height);

        try {
            if (isViewAdded) {
                layoutParams.width = width;
                layoutParams.height = height;
                windowManager.updateViewLayout(floatAmapCorner, layoutParams);
                Log.i(TAG, "尺寸已更新");
            }
        } catch (Exception e) {
            Log.e(TAG, "更新尺寸失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取悬浮窗视图
     */
    public FloatAmapCorner getFloatAmapCorner() {
        return floatAmapCorner;
    }

    /**
     * 是否正在显示
     */
    public boolean isShowing() {
        return isViewAdded;
    }

    /**
     * 显示控制点
     */
    public void showControlPoints() {
        if (floatAmapCorner != null) {
            floatAmapCorner.showControlPoints();
        }
    }

    /**
     * 隐藏控制点
     */
    public void hideControlPoints() {
        if (floatAmapCorner != null) {
            floatAmapCorner.hideControlPoints();
        }
    }

    /**
     * 获取 WindowManager
     */
    public WindowManager getWindowManager() {
        return windowManager;
    }

    /**
     * 获取 LayoutParams
     */
    public WindowManager.LayoutParams getLayoutParams() {
        return layoutParams;
    }

    /**
     * 销毁实例
     */
    public static void destroy() {
        if (instance != null && instance.isViewAdded) {
            instance.hide();
        }
        instance = null;
        Log.i(TAG, "FloatAmapCornerWindow 实例已销毁");
    }
}
