package cn.youth996.ui.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.net.Uri;
import android.util.Log;
import android.app.Activity;
import android.os.Build;

import cn.youth996.ui.service.FloatingMapService;

/**
 * 悬浮窗控制管理器
 * 
 * 功能说明：
 * 1. 提供悬浮窗服务的启动、停止和控制接口
 * 2. 处理悬浮窗权限检查和请求
 * 3. 封装Intent命令发送逻辑
 * 4. 支持通过广播命令控制高德地图悬浮窗
 * 
 * 使用方式：
 * 
 * 【方式一：使用高德地图悬浮窗（推荐）】
 * 1. 检查高德地图是否安装：FloatingWindowManager.isAmapInstalled(context)
 * 2. 显示悬浮窗：FloatingWindowManager.showAmapFloatMap(context, x, y, width, height)
 * 3. 关闭悬浮窗：FloatingWindowManager.hideAmapFloatMap(context)
 * 
 * 【方式二：使用自定义悬浮窗】
 * 1. 首先检查权限：FloatingWindowManager.hasOverlayPermission(context)
 * 2. 如无权限则请求：FloatingWindowManager.requestOverlayPermission(activity)
 * 3. 启动服务：FloatingWindowManager.startService(context)
 * 4. 控制悬浮窗：FloatingWindowManager.show(context) / hide(context) / toggle(context)
 * 
 * @author Youth Desktop Team
 */
public class FloatingWindowManager {

    private static final String TAG = "FloatingWindowManager";
    
    /** 服务包名 */
    private static final String SERVICE_PACKAGE = "cn.youth996.ui";
    
    /** 服务类名 */
    private static final String SERVICE_CLASS = "cn.youth996.ui.service.FloatingMapService";

    /** 高德地图包名 */
    private static final String AMAP_PACKAGE = "com.autonavi.amapauto";
    
    /** 显示悬浮窗广播 Action */
    private static final String ACTION_SHOW_MAP = "com.autonavi.plus.showmap";
    
    /** 关闭悬浮窗广播 Action */
    private static final String ACTION_CLOSE_MAP = "com.autonavi.plus.closemap";

    /**
     * 检查是否有悬浮窗权限
     *
     * Android 6.0及以上需要用户授权
     *
     * @param context 上下文对象
     * @return true表示有权限，false表示无权限
     */
    public static boolean hasOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasPermission = Settings.canDrawOverlays(context);
            Log.i(TAG, "悬浮窗权限检查: " + (hasPermission ? "已授权" : "未授权"));
            return hasPermission;
        }
        return true;
    }

    /**
     * 请求悬浮窗权限
     *
     * 打开系统设置页面让用户授权
     *
     * @param activity Activity实例
     */
    public static void requestOverlayPermission(Activity activity) {
        if (!hasOverlayPermission(activity)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
            Log.i(TAG, "已打开悬浮窗权限设置页面");
        } else {
            Log.i(TAG, "悬浮窗权限已授权，无需请求");
        }
    }

    // ==================== 高德地图悬浮窗控制（推荐方式） ====================

    /**
     * 显示高德地图悬浮窗（通过广播命令）
     *
     * 这是推荐的方式，直接让高德地图显示其内置的悬浮窗
     *
     * @param context 上下文对象
     * @param x      X坐标（像素）
     * @param y      Y坐标（像素）
     * @param width  宽度（像素）
     * @param height 高度（像素）
     */
    public static void showAmapFloatMap(Context context, int x, int y, int width, int height) {
        Log.i(TAG, "【高德悬浮窗】显示悬浮窗: x=" + x + ", y=" + y + 
              ", width=" + width + ", height=" + height);
        
        try {
            Intent intent = new Intent(ACTION_SHOW_MAP);
            intent.setPackage(AMAP_PACKAGE);
            
            // 设置悬浮窗位置和尺寸
            intent.putExtra("x", x);
            intent.putExtra("y", y);
            intent.putExtra("w", width);
            intent.putExtra("h", height);
            
            context.sendBroadcast(intent);
            
            Log.i(TAG, "【高德悬浮窗】显示悬浮窗广播已发送");
        } catch (Exception e) {
            Log.e(TAG, "【高德悬浮窗】显示悬浮窗失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 显示高德地图悬浮窗（使用默认位置）
     *
     * 默认显示在屏幕右上角区域
     *
     * @param context 上下文对象
     */
    public static void showAmapFloatMap(Context context) {
        // 获取屏幕尺寸
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        
        // 默认显示在右上角
        int x = (int) (screenWidth * 0.05);  // 5% 边距
        int y = (int) (screenHeight * 0.05); // 5% 边距
        int width = (int) (screenWidth * 0.45); // 45% 宽度
        int height = (int) (screenHeight * 0.35); // 35% 高度
        
        showAmapFloatMap(context, x, y, width, height);
    }

    /**
     * 显示高德地图悬浮窗（使用屏幕百分比）
     *
     * @param context        上下文对象
     * @param xPercent      X坐标百分比 (0-100)
     * @param yPercent      Y坐标百分比 (0-100)
     * @param widthPercent  宽度百分比 (0-100)
     * @param heightPercent 高度百分比 (0-100)
     */
    public static void showAmapFloatMapPercent(Context context, int xPercent, int yPercent, 
                                            int widthPercent, int heightPercent) {
        // 获取屏幕尺寸
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        
        int x = (screenWidth * xPercent) / 100;
        int y = (screenHeight * yPercent) / 100;
        int width = (screenWidth * widthPercent) / 100;
        int height = (screenHeight * heightPercent) / 100;
        
        showAmapFloatMap(context, x, y, width, height);
    }

    /**
     * 关闭高德地图悬浮窗（通过广播命令）
     *
     * 发送广播命令给高德地图，让它关闭悬浮窗
     *
     * @param context 上下文对象
     */
    public static void hideAmapFloatMap(Context context) {
        Log.i(TAG, "【高德悬浮窗】关闭悬浮窗");
        
        try {
            Intent intent = new Intent(ACTION_CLOSE_MAP);
            intent.setPackage(AMAP_PACKAGE);
            
            context.sendBroadcast(intent);
            
            Log.i(TAG, "【高德悬浮窗】关闭悬浮窗广播已发送");
        } catch (Exception e) {
            Log.e(TAG, "【高德悬浮窗】关闭悬浮窗失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 切换高德地图悬浮窗显示状态
     *
     * @param context          上下文对象
     * @param currentlyShowing 当前是否正在显示
     */
    public static void toggleAmapFloatMap(Context context, boolean currentlyShowing) {
        if (currentlyShowing) {
            hideAmapFloatMap(context);
        } else {
            showAmapFloatMap(context);
        }
    }

    /**
     * 检查高德地图是否已安装
     *
     * @param context 上下文对象
     * @return true表示已安装，false表示未安装
     */
    public static boolean isAmapInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(AMAP_PACKAGE, 0);
            Log.i(TAG, "【高德悬浮窗】高德地图已安装: " + AMAP_PACKAGE);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "【高德悬浮窗】高德地图未安装: " + AMAP_PACKAGE);
            return false;
        }
    }

    // ==================== 自定义悬浮窗服务控制 ====================

    // ==================== 使用 FloatAmapCornerWindow 的悬浮窗控制 ====================

    /**
     * 显示自定义悬浮窗（使用布丁UI架构）
     *
     * 使用 FloatAmapCornerWindow 实现悬浮窗
     *
     * @param context 上下文对象
     * @param x      X坐标
     * @param y      Y坐标
     * @param width  宽度
     * @param height 高度
     */
    public static void showCustomFloatWindow(Context context, int x, int y, int width, int height) {
        Log.i(TAG, "【自定义悬浮窗】显示悬浮窗: x=" + x + ", y=" + y + 
              ", width=" + width + ", height=" + height);

        try {
            FloatAmapCornerWindow window = FloatAmapCornerWindow.getInstance(context);
            window.show(context, x, y, width, height);
            Log.i(TAG, "【自定义悬浮窗】悬浮窗已显示");
        } catch (Exception e) {
            Log.e(TAG, "【自定义悬浮窗】显示悬浮窗失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 显示自定义悬浮窗（使用默认位置）
     *
     * @param context 上下文对象
     */
    public static void showCustomFloatWindow(Context context) {
        showCustomFloatWindow(context, -1, -1, -1, -1);
    }

    /**
     * 隐藏自定义悬浮窗
     *
     * @param context 上下文对象
     */
    public static void hideCustomFloatWindow(Context context) {
        Log.i(TAG, "【自定义悬浮窗】隐藏悬浮窗");

        try {
            FloatAmapCornerWindow window = FloatAmapCornerWindow.getInstance(context);
            window.hide();
            Log.i(TAG, "【自定义悬浮窗】悬浮窗已隐藏");
        } catch (Exception e) {
            Log.e(TAG, "【自定义悬浮窗】隐藏悬浮窗失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 更新自定义悬浮窗位置
     *
     * @param context 上下文对象
     * @param x X坐标
     * @param y Y坐标
     */
    public static void updateCustomFloatPosition(Context context, int x, int y) {
        Log.i(TAG, "【自定义悬浮窗】更新位置: x=" + x + ", y=" + y);

        try {
            FloatAmapCornerWindow window = FloatAmapCornerWindow.getInstance(context);
            window.updatePosition(x, y);
            Log.i(TAG, "【自定义悬浮窗】位置已更新");
        } catch (Exception e) {
            Log.e(TAG, "【自定义悬浮窗】更新位置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 更新自定义悬浮窗尺寸
     *
     * @param context 上下文对象
     * @param width  宽度
     * @param height 高度
     */
    public static void updateCustomFloatSize(Context context, int width, int height) {
        Log.i(TAG, "【自定义悬浮窗】更新尺寸: width=" + width + ", height=" + height);

        try {
            FloatAmapCornerWindow window = FloatAmapCornerWindow.getInstance(context);
            window.updateSize(width, height);
            Log.i(TAG, "【自定义悬浮窗】尺寸已更新");
        } catch (Exception e) {
            Log.e(TAG, "【自定义悬浮窗】更新尺寸失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 显示自定义悬浮窗的控制点
     *
     * @param context 上下文对象
     */
    public static void showCustomFloatControlPoints(Context context) {
        Log.i(TAG, "【自定义悬浮窗】显示控制点");

        try {
            FloatAmapCornerWindow window = FloatAmapCornerWindow.getInstance(context);
            window.showControlPoints();
            Log.i(TAG, "【自定义悬浮窗】控制点已显示");
        } catch (Exception e) {
            Log.e(TAG, "【自定义悬浮窗】显示控制点失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 隐藏自定义悬浮窗的控制点
     *
     * @param context 上下文对象
     */
    public static void hideCustomFloatControlPoints(Context context) {
        Log.i(TAG, "【自定义悬浮窗】隐藏控制点");

        try {
            FloatAmapCornerWindow window = FloatAmapCornerWindow.getInstance(context);
            window.hideControlPoints();
            Log.i(TAG, "【自定义悬浮窗】控制点已隐藏");
        } catch (Exception e) {
            Log.e(TAG, "【自定义悬浮窗】隐藏控制点失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 检查自定义悬浮窗是否正在显示
     *
     * @param context 上下文对象
     * @return true表示正在显示，false表示未显示
     */
    public static boolean isCustomFloatWindowShowing(Context context) {
        try {
            FloatAmapCornerWindow window = FloatAmapCornerWindow.getInstance(context);
            return window.isShowing();
        } catch (Exception e) {
            Log.e(TAG, "【自定义悬浮窗】检查显示状态失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 销毁自定义悬浮窗实例
     */
    public static void destroyCustomFloatWindow() {
        Log.i(TAG, "【自定义悬浮窗】销毁实例");
        FloatAmapCornerWindow.destroy();
    }

    // ==================== 使用 Service 的悬浮窗控制 ====================

    /**
     * 启动悬浮地图服务
     *
     * @param context 上下文对象
     */
    public static void startService(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(SERVICE_PACKAGE, SERVICE_CLASS));
        
        try {
            context.startService(intent);
            Log.i(TAG, "悬浮地图服务启动成功");
        } catch (Exception e) {
            Log.e(TAG, "悬浮地图服务启动失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 停止悬浮地图服务
     *
     * @param context 上下文对象
     */
    public static void stopService(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(SERVICE_PACKAGE, SERVICE_CLASS));
        
        try {
            context.stopService(intent);
            Log.i(TAG, "悬浮地图服务停止成功");
        } catch (Exception e) {
            Log.e(TAG, "悬浮地图服务停止失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 显示悬浮窗（自定义服务）
     *
     * @param context 上下文对象
     */
    public static void show(Context context) {
        Intent intent = new Intent(context, FloatingMapService.class);
        intent.putExtra("command", "show");
        context.startService(intent);
        Log.i(TAG, "发送显示悬浮窗命令");
    }

    /**
     * 隐藏悬浮窗（自定义服务）
     *
     * @param context 上下文对象
     */
    public static void hide(Context context) {
        Intent intent = new Intent(context, FloatingMapService.class);
        intent.putExtra("command", "hide");
        context.startService(intent);
        Log.i(TAG, "发送隐藏悬浮窗命令");
    }

    /**
     * 切换悬浮窗显示状态（自定义服务）
     *
     * @param context 上下文对象
     */
    public static void toggle(Context context) {
        Intent intent = new Intent(context, FloatingMapService.class);
        intent.putExtra("command", "toggle");
        context.startService(intent);
        Log.i(TAG, "发送切换悬浮窗命令");
    }

    /**
     * 更新地图（自定义服务）
     *
     * @param context 上下文对象
     */
    public static void updateMap(Context context) {
        Intent intent = new Intent(context, FloatingMapService.class);
        intent.putExtra("command", "update_map");
        context.startService(intent);
        Log.i(TAG, "发送更新地图命令");
    }

    /**
     * 显示悬浮窗（自定义服务，带参数）
     *
     * @param context 上下文对象
     * @param x X坐标
     * @param y Y坐标
     * @param width 宽度
     * @param height 高度
     */
    public static void showWithParams(Context context, int x, int y, int width, int height) {
        Intent intent = new Intent(context, FloatingMapService.class);
        intent.putExtra("command", "show");
        intent.putExtra("x", x);
        intent.putExtra("y", y);
        intent.putExtra("width", width);
        intent.putExtra("height", height);
        context.startService(intent);
        Log.i(TAG, "发送显示悬浮窗命令（带参数）: 位置(" + x + ", " + y + "), 尺寸(" + width + "x" + height + ")");
    }
}
