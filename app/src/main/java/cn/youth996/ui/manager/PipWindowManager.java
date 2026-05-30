package cn.youth996.ui.manager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import cn.youth996.ui.R;

/**
 * 高德地图悬浮窗管理器
 * 使用 ImageView 显示静态地图预览，点击打开高德地图应用
 */
public class PipWindowManager {
    private static final String TAG = "PipWindowManager";

    private Activity activity;
    private WindowManager windowManager;
    private FrameLayout floatingView;
    private ImageView previewImage;
    private TextView hintText;

    private boolean isFloatingWindowVisible = false;
    private int screenWidth;
    private int screenHeight;
    private boolean isPortrait = true;

    // 悬浮窗位置和大小
    private int floatingWidth = 400;
    private int floatingHeight = 300;
    private int floatingX = 20;
    private int floatingY = 100;

    private OnPipWindowListener listener;

    public interface OnPipWindowListener {
        void onPipWindowCreated();
        void onPipWindowRemoved();
        void onNavigationRequested();
    }

    public PipWindowManager(Activity activity) {
        this.activity = activity;
        this.windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        
        updateScreenMetrics();
    }

    /**
     * 更新屏幕尺寸
     */
    private void updateScreenMetrics() {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        
        // 根据屏幕方向调整悬浮窗大小
        isPortrait = screenHeight > screenWidth;
        if (isPortrait) {
            floatingWidth = Math.min(400, screenWidth - 40);
            floatingHeight = (int) (floatingWidth * 0.75);
            floatingX = 20;
            floatingY = 100;
        } else {
            floatingWidth = (int) (screenWidth * 0.25);
            floatingHeight = (int) (screenHeight * 0.4);
            floatingX = 20;
            floatingY = (screenHeight - floatingHeight) / 2;
        }
        
        Log.d(TAG, "屏幕尺寸: " + screenWidth + "x" + screenHeight + ", 方向: " + (isPortrait ? "竖屏" : "横屏"));
        Log.d(TAG, "悬浮窗大小: " + floatingWidth + "x" + floatingHeight);
    }

    public void setOnPipWindowListener(OnPipWindowListener listener) {
        this.listener = listener;
    }

    /**
     * 创建悬浮窗
     */
    public void createFloatingWindow() {
        if (activity == null || activity.isFinishing()) {
            Log.w(TAG, "Activity 已结束，无法创建悬浮窗");
            return;
        }

        if (floatingView != null) {
            Log.d(TAG, "悬浮窗已存在");
            return;
        }

        try {
            activity.runOnUiThread(() -> {
                try {
                    // 使用布局文件创建悬浮窗
                    LayoutInflater inflater = LayoutInflater.from(activity);
                    floatingView = (FrameLayout) inflater.inflate(
                            R.layout.layout_pip_float_window, 
                            null,
                            false
                    );

                    previewImage = floatingView.findViewById(R.id.img_pip_preview);
                    hintText = floatingView.findViewById(R.id.text_pip_hint);

                    // 设置点击事件 - 打开高德地图应用
                    floatingView.setOnClickListener(v -> {
                        Log.d(TAG, "悬浮窗被点击");
                        if (listener != null) {
                            listener.onNavigationRequested();
                        }
                    });

                    // 设置悬浮窗参数
                    WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                            floatingWidth,
                            floatingHeight,
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                            android.graphics.PixelFormat.TRANSLUCENT
                    );

                    params.gravity = Gravity.TOP | Gravity.START;
                    params.x = floatingX;
                    params.y = floatingY;

                    // 添加到窗口管理器
                    windowManager.addView(floatingView, params);
                    isFloatingWindowVisible = true;
                    Log.d(TAG, "悬浮窗已创建");

                    if (listener != null) {
                        listener.onPipWindowCreated();
                    }

                } catch (Exception e) {
                    Log.e(TAG, "创建悬浮窗失败: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "创建悬浮窗失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 显示悬浮窗
     */
    public void showFloatingWindow() {
        if (floatingView != null && !isFloatingWindowVisible) {
            try {
                floatingView.setVisibility(View.VISIBLE);
                isFloatingWindowVisible = true;
                Log.d(TAG, "悬浮窗已显示");
            } catch (Exception e) {
                Log.e(TAG, "显示悬浮窗失败: " + e.getMessage());
            }
        } else if (floatingView == null) {
            createFloatingWindow();
        }
    }

    /**
     * 隐藏悬浮窗
     */
    public void hideFloatingWindow() {
        if (floatingView != null && isFloatingWindowVisible) {
            try {
                floatingView.setVisibility(View.GONE);
                isFloatingWindowVisible = false;
                Log.d(TAG, "悬浮窗已隐藏");
            } catch (Exception e) {
                Log.e(TAG, "隐藏悬浮窗失败: " + e.getMessage());
            }
        }
    }

    /**
     * 移除悬浮窗
     */
    public void removeFloatingWindow() {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        try {
            activity.runOnUiThread(() -> {
                if (floatingView != null) {
                    try {
                        windowManager.removeView(floatingView);
                        floatingView = null;
                        previewImage = null;
                        hintText = null;
                        isFloatingWindowVisible = false;
                        Log.d(TAG, "悬浮窗已移除");

                        if (listener != null) {
                            listener.onPipWindowRemoved();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "移除悬浮窗失败: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "移除悬浮窗失败: " + e.getMessage());
        }
    }

    /**
     * 更新悬浮窗位置（屏幕方向变化时调用）
     */
    public void updatePositionForOrientation() {
        updateScreenMetrics();
        
        if (floatingView != null) {
            try {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) floatingView.getLayoutParams();
                params.width = floatingWidth;
                params.height = floatingHeight;
                params.x = floatingX;
                params.y = floatingY;
                windowManager.updateViewLayout(floatingView, params);
                Log.d(TAG, "悬浮窗位置已更新: " + floatingX + ", " + floatingY + " 大小: " + floatingWidth + "x" + floatingHeight);
            } catch (Exception e) {
                Log.e(TAG, "更新悬浮窗位置失败: " + e.getMessage());
            }
        }
    }

    /**
     * 检查悬浮窗是否可见
     */
    public boolean isVisible() {
        return isFloatingWindowVisible && floatingView != null;
    }

    /**
     * 检查悬浮窗是否已创建
     */
    public boolean isCreated() {
        return floatingView != null;
    }

    /**
     * 释放资源
     */
    public void release() {
        removeFloatingWindow();
        activity = null;
    }
}
