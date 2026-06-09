package cn.youth996.ui.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import cn.youth996.ui.core.FloatAmapCornerWindow;

/**
 * 高德地图悬浮窗角落服务
 * 
 * 参考布丁UI的 FloatAmapCornerService 实现
 * 提供悬浮窗的显示、隐藏、位置调整等功能
 * 
 * @author Youth Desktop Team
 */
public class FloatAmapCornerService extends Service {

    private static final String TAG = "FloatAmapCornerService";

    /** 单例实例 */
    private static FloatAmapCornerService instance;

    /** 悬浮窗窗口管理器 */
    private FloatAmapCornerWindow floatAmapCornerWindow;

    /** 是否正在运行 */
    private boolean isRunning = false;

    /**
     * 获取单例实例
     */
    public static FloatAmapCornerService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.i(TAG, "服务 onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "服务 onStartCommand");

        if (intent == null) {
            Log.w(TAG, "Intent 为空");
            return START_NOT_STICKY;
        }

        // 获取命令
        String command = intent.getStringExtra("command");
        Log.i(TAG, "收到命令: " + command);

        if (command == null) {
            // 默认显示悬浮窗
            showFloatWindow();
        } else {
            switch (command) {
                case "show":
                    showFloatWindow(intent);
                    break;
                case "hide":
                    hideFloatWindow();
                    break;
                case "toggle":
                    toggleFloatWindow();
                    break;
                case "update_position":
                    updatePosition(intent);
                    break;
                case "update_size":
                    updateSize(intent);
                    break;
                case "show_control_points":
                    showControlPoints();
                    break;
                case "hide_control_points":
                    hideControlPoints();
                    break;
                default:
                    Log.w(TAG, "未知命令: " + command);
                    break;
            }
        }

        return START_STICKY;
    }

    /**
     * 显示悬浮窗（带参数）
     */
    private void showFloatWindow(Intent intent) {
        int x = intent.getIntExtra("x", -1);
        int y = intent.getIntExtra("y", -1);
        int width = intent.getIntExtra("width", -1);
        int height = intent.getIntExtra("height", -1);

        Log.i(TAG, "显示悬浮窗: x=" + x + ", y=" + y + 
              ", width=" + width + ", height=" + height);

        // 获取或创建窗口管理器
        if (floatAmapCornerWindow == null) {
            floatAmapCornerWindow = FloatAmapCornerWindow.getInstance(this);
        }

        if (x >= 0 && y >= 0 && width > 0 && height > 0) {
            // 使用指定位置和尺寸
            floatAmapCornerWindow.show(this, x, y, width, height);
        } else {
            // 使用默认位置
            floatAmapCornerWindow.show(this);
        }

        isRunning = true;
        Log.i(TAG, "悬浮窗已显示");
    }

    /**
     * 显示悬浮窗（使用默认位置）
     */
    private void showFloatWindow() {
        showFloatWindow(null);
    }

    /**
     * 隐藏悬浮窗
     */
    private void hideFloatWindow() {
        Log.i(TAG, "隐藏悬浮窗");

        if (floatAmapCornerWindow != null) {
            floatAmapCornerWindow.hide();
            isRunning = false;
            Log.i(TAG, "悬浮窗已隐藏");
        }
    }

    /**
     * 切换悬浮窗显示状态
     */
    private void toggleFloatWindow() {
        Log.i(TAG, "切换悬浮窗状态");

        if (floatAmapCornerWindow == null) {
            floatAmapCornerWindow = FloatAmapCornerWindow.getInstance(this);
        }

        if (floatAmapCornerWindow.isShowing()) {
            hideFloatWindow();
        } else {
            showFloatWindow();
        }
    }

    /**
     * 更新悬浮窗位置
     */
    private void updatePosition(Intent intent) {
        int x = intent.getIntExtra("x", 0);
        int y = intent.getIntExtra("y", 0);

        Log.i(TAG, "更新位置: x=" + x + ", y=" + y);

        if (floatAmapCornerWindow != null && floatAmapCornerWindow.isShowing()) {
            floatAmapCornerWindow.updatePosition(x, y);
            Log.i(TAG, "位置已更新");
        }
    }

    /**
     * 更新悬浮窗尺寸
     */
    private void updateSize(Intent intent) {
        int width = intent.getIntExtra("width", -1);
        int height = intent.getIntExtra("height", -1);

        Log.i(TAG, "更新尺寸: width=" + width + ", height=" + height);

        if (floatAmapCornerWindow != null && floatAmapCornerWindow.isShowing()) {
            if (width > 0 && height > 0) {
                floatAmapCornerWindow.updateSize(width, height);
                Log.i(TAG, "尺寸已更新");
            }
        }
    }

    /**
     * 显示控制点
     */
    private void showControlPoints() {
        Log.i(TAG, "显示控制点");

        if (floatAmapCornerWindow != null) {
            floatAmapCornerWindow.showControlPoints();
            Log.i(TAG, "控制点已显示");
        }
    }

    /**
     * 隐藏控制点
     */
    private void hideControlPoints() {
        Log.i(TAG, "隐藏控制点");

        if (floatAmapCornerWindow != null) {
            floatAmapCornerWindow.hideControlPoints();
            Log.i(TAG, "控制点已隐藏");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "服务 onDestroy");

        // 隐藏悬浮窗
        if (floatAmapCornerWindow != null) {
            floatAmapCornerWindow.hide();
            floatAmapCornerWindow = null;
        }

        instance = null;
        isRunning = false;
        Log.i(TAG, "服务已销毁");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 检查服务是否正在运行
     */
    public static boolean isServiceRunning() {
        return instance != null && instance.isRunning;
    }
}
