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

    /** 默认悬浮地图包名（高德地图车机版） */
    public static final String DEFAULT_MAP_PACKAGE = "com.autonavi.amapauto.chenmo";

    /** 悬浮地图设置 SharedPreferences 文件名 */
    private static final String MAP_PREFS_NAME = "map_settings";

    /** 悬浮地图包名 key */
    private static final String KEY_MAP_PACKAGE = "float_map_package";

    /** 显示悬浮窗广播 Action */
    private static final String ACTION_SHOW_MAP = "com.autonavi.plus.showmap";
    
    /** 关闭悬浮窗广播 Action */
    private static final String ACTION_CLOSE_MAP = "com.autonavi.plus.closemap";

    /**
     * 获取当前配置的悬浮地图包名
     *
     * @param context 上下文对象
     * @return 用户选择的悬浮地图包名，未设置时返回默认高德车机版包名
     */
    public static String getMapPackage(Context context) {
        return context.getSharedPreferences(MAP_PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_MAP_PACKAGE, DEFAULT_MAP_PACKAGE);
    }

    /**
     * 保存悬浮地图包名
     *
     * @param context     上下文对象
     * @param packageName 悬浮地图应用包名
     */
    public static void setMapPackage(Context context, String packageName) {
        context.getSharedPreferences(MAP_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_MAP_PACKAGE, packageName)
                .apply();
    }

    /** 高德悬浮窗当前是否正在显示（由本应用控制） */
    private static boolean isAmapFloatShowing = false;

    /** 用户是否开启了悬浮地图功能（用于前后台切换时自动恢复显示） */
    private static boolean isAmapFloatEnabled = false;

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
            intent.setPackage(getMapPackage(context));
            //region debug-point float-map-show-pkg
            Log.i(TAG, "【debug】show setPackage=" + getMapPackage(context)
                    + " isShowing=" + isAmapFloatShowing + " isEnabled=" + isAmapFloatEnabled);
            //endregion

            // 设置悬浮窗位置和尺寸
            intent.putExtra("x", x);
            intent.putExtra("y", y);
            intent.putExtra("w", width);
            intent.putExtra("h", height);
            
            context.sendBroadcast(intent);
            isAmapFloatShowing = true;
            isAmapFloatEnabled = true;
            
            Log.i(TAG, "【高德悬浮窗】显示悬浮窗广播已发送");
        } catch (Exception e) {
            Log.e(TAG, "【高德悬浮窗】显示悬浮窗失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 显示高德地图悬浮窗（使用默认位置）
     *
     * 默认显示在屏幕左侧中部，尺寸较大便于查看
     *
     * @param context 上下文对象
     */
    public static void showAmapFloatMap(Context context) {
        // 获取屏幕尺寸
        float density = context.getResources().getDisplayMetrics().density;
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;

        // 顶部留出真实 statusBar 高度（系统可能切了刘海/挖孔，固定 32dp 不准）
        int systemStatusBarPx = getSystemStatusBarHeightPx(context);

        // 尺寸与删除前的「左侧导航卡片」完全对齐：
        //   - 顶部 = statusBar(真实高度) + centerArea_marginTop(8dp)
        //   - 高度 = 屏幕 70%（比 80% 更小，避开下方 taskbar 安全区）
        //   - 左边 = centerArea_padding_h(16dp)
        //   - 右边与音乐卡(300dp)对齐：宽度 = 屏宽 - 左边 - 音乐卡(300dp) - 间隙(8dp) - 右边距(16dp)
        int topMarginPx = systemStatusBarPx + (int) (16 * density);
        int y = topMarginPx;
        int x = (int) (20 * density);                              // 16dp 左边距
        int musicCardWidthPx = (int) (300 * density);              // 音乐卡片宽度 300dp
        int gapPx = (int) (8 * density);                           // 中间间隙 8dp
        int rightPaddingPx = (int) (16 * density);                 // 右边距 16dp
        int width = screenWidth - x - musicCardWidthPx - gapPx - rightPaddingPx;
        int height = (int) (screenHeight * 0.68f);                 // 70% 屏幕高度（与原导航卡大致一致且更紧凑）

        showAmapFloatMap(context, x, y, width, height);
    }

    /**
     * 获取系统 statusBar 高度（像素）。优先用 system 资源的 status_bar_height，
     * 失败时 fallback 到 24dp。
     */
    private static int getSystemStatusBarHeightPx(Context context) {
        try {
            int resId = context.getResources().getIdentifier(
                    "status_bar_height", "dimen", "android");
            if (resId > 0) {
                return context.getResources().getDimensionPixelSize(resId);
            }
        } catch (Throwable ignored) {
        }
        return (int) (24 * context.getResources().getDisplayMetrics().density);
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
     * 关闭高德地图悬浮窗（用户主动关闭）
     *
     * 发送广播命令给高德地图，让它关闭悬浮窗，
     * 同时标记用户已关闭悬浮地图功能，回到桌面时不会自动恢复。
     *
     * @param context 上下文对象
     */
    public static void hideAmapFloatMap(Context context) {
        Log.i(TAG, "【高德悬浮窗】关闭悬浮窗（用户主动）");
        
        try {
            Intent intent = new Intent(ACTION_CLOSE_MAP);
            intent.setPackage(getMapPackage(context));
            //region debug-point float-map-hide-pkg
            Log.i(TAG, "【debug】hide setPackage=" + getMapPackage(context));
            //endregion

            context.sendBroadcast(intent);
            isAmapFloatShowing = false;
            isAmapFloatEnabled = false;
            
            Log.i(TAG, "【高德悬浮窗】关闭悬浮窗广播已发送");
        } catch (Exception e) {
            Log.e(TAG, "【高德悬浮窗】关闭悬浮窗失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 暂停高德地图悬浮窗（应用进入后台时自动隐藏）
     *
     * 仅隐藏悬浮窗，不清除用户开启状态，
     * 回到桌面时可通过 {@link #resumeAmapFloatMap(Context)} 自动恢复。
     *
     * @param context 上下文对象
     */
    public static void pauseAmapFloatMap(Context context) {
        if (!isAmapFloatEnabled) {
            Log.d(TAG, "【高德悬浮窗】用户未开启悬浮地图，无需暂停");
            return;
        }
        if (!isAmapFloatShowing) {
            Log.d(TAG, "【高德悬浮窗】悬浮窗当前未显示，无需暂停");
            return;
        }
        Log.i(TAG, "【高德悬浮窗】应用进入后台，自动隐藏悬浮窗");
        
        try {
            Intent intent = new Intent(ACTION_CLOSE_MAP);
            intent.setPackage(getMapPackage(context));
            //region debug-point float-map-pause-pkg
            Log.i(TAG, "【debug】pause setPackage=" + getMapPackage(context));
            //endregion
            context.sendBroadcast(intent);
            isAmapFloatShowing = false;
            Log.i(TAG, "【高德悬浮窗】自动隐藏广播已发送");
        } catch (Exception e) {
            Log.e(TAG, "【高德悬浮窗】自动隐藏失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 恢复高德地图悬浮窗（应用回到前台/桌面时自动显示）
     *
     * 仅当用户之前开启了悬浮地图功能时才恢复显示。
     *
     * @param context 上下文对象
     */
    public static void resumeAmapFloatMap(Context context) {
        if (!isAmapFloatEnabled) {
            Log.d(TAG, "【高德悬浮窗】用户未开启悬浮地图，无需恢复");
            return;
        }
        if (isAmapFloatShowing) {
            Log.d(TAG, "【高德悬浮窗】悬浮窗已在显示，无需恢复");
            return;
        }
        Log.i(TAG, "【高德悬浮窗】应用回到桌面，自动恢复显示悬浮窗");
        showAmapFloatMap(context);
    }

    /**
     * 查询用户是否开启了悬浮地图功能
     *
     * @return true 表示用户已开启悬浮地图
     */
    public static boolean isAmapFloatEnabled() {
        return isAmapFloatEnabled;
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
     * 切换高德地图悬浮窗显示状态（使用内部记录的状态）
     *
     * @param context 上下文对象
     */
    public static void toggleAmapFloatMap(Context context) {
        toggleAmapFloatMap(context, isAmapFloatShowing);
    }

    /**
     * 查询高德地图悬浮窗当前是否正在显示
     *
     * 注意：此处仅记录本应用通过 showAmapFloatMap / hideAmapFloatMap
     * 触发的状态，不代表高德地图自身悬浮窗的实时状态。
     *
     * @return true 表示正在显示
     */
    public static boolean isAmapFloatShowing() {
        return isAmapFloatShowing;
    }

    /**
     * 检查高德地图是否已安装
     *
     * @param context 上下文对象
     * @return true表示已安装，false表示未安装
     */
    public static boolean isAmapInstalled(Context context) {
        String mapPackage = getMapPackage(context);
        try {
            context.getPackageManager().getPackageInfo(mapPackage, 0);
            Log.i(TAG, "【悬浮地图】地图应用已安装: " + mapPackage);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "【悬浮地图】地图应用未安装: " + mapPackage);
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
