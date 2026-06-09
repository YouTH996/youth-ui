package cn.youth996.ui.core;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 高德地图悬浮窗控制器
 * 
 * 功能说明：
 * 通过发送广播命令控制高德地图车机版的悬浮窗显示/隐藏
 * 
 * 关键发现（基于布丁UI反编译）：
 * - 显示悬浮窗: Intent Action = "com.autonavi.plus.showmap"
 * - 关闭悬浮窗: Intent Action = "com.autonavi.plus.closemap"
 * - 目标包名: "com.autonavi.amapauto" (高德地图车机版)
 * 
 * 使用方式：
 * 1. 直接调用 showFloatMap() / hideFloatMap()
 * 2. 或通过 FloatingWindowManager 调用
 * 
 * @author Youth Desktop Team
 */
public class AmapFloatController {

    private static final String TAG = "AmapFloatController";
    
    /** 高德地图包名 */
    private static final String AMAP_PACKAGE = "com.autonavi.amapauto";
    
    /** 显示悬浮窗广播 Action */
    private static final String ACTION_SHOW_MAP = "com.autonavi.plus.showmap";
    
    /** 关闭悬浮窗广播 Action */
    private static final String ACTION_CLOSE_MAP = "com.autonavi.plus.closemap";
    
    /** 单例实例 */
    private static AmapFloatController instance;
    
    /** 上下文对象 */
    private Context context;

    /**
     * 私有构造函数（单例模式）
     */
    private AmapFloatController(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * 获取单例实例
     *
     * @param context 上下文对象
     * @return AmapFloatController实例
     */
    public static synchronized AmapFloatController getInstance(Context context) {
        if (instance == null) {
            instance = new AmapFloatController(context);
        }
        return instance;
    }

    /**
     * 显示高德地图悬浮窗
     *
     * 发送广播命令给高德地图，让它在指定位置显示悬浮窗
     *
     * @param x      X坐标（像素）
     * @param y      Y坐标（像素）
     * @param width  宽度（像素）
     * @param height 高度（像素）
     */
    public void showFloatMap(int x, int y, int width, int height) {
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
     * 默认显示在屏幕右上角
     */
    public void showFloatMap() {
        // 默认位置和尺寸
        showFloatMap(100, 100, 600, 400);
    }

    /**
     * 显示高德地图悬浮窗（使用屏幕百分比）
     *
     * @param xPercent      X坐标百分比 (0-100)
     * @param yPercent      Y坐标百分比 (0-100)
     * @param widthPercent  宽度百分比 (0-100)
     * @param heightPercent 高度百分比 (0-100)
     */
    public void showFloatMapPercent(int xPercent, int yPercent, 
                                   int widthPercent, int heightPercent) {
        // 获取屏幕尺寸
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        
        int x = (screenWidth * xPercent) / 100;
        int y = (screenHeight * yPercent) / 100;
        int width = (screenWidth * widthPercent) / 100;
        int height = (screenHeight * heightPercent) / 100;
        
        showFloatMap(x, y, width, height);
    }

    /**
     * 关闭高德地图悬浮窗
     *
     * 发送广播命令给高德地图，让它关闭悬浮窗
     */
    public void hideFloatMap() {
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
     * @param currentlyShowing 当前是否正在显示
     */
    public void toggleFloatMap(boolean currentlyShowing) {
        if (currentlyShowing) {
            hideFloatMap();
        } else {
            showFloatMap();
        }
    }

    /**
     * 检查高德地图是否已安装
     *
     * @return true表示已安装，false表示未安装
     */
    public boolean isAmapInstalled() {
        try {
            context.getPackageManager().getPackageInfo(AMAP_PACKAGE, 0);
            Log.i(TAG, "【高德悬浮窗】高德地图已安装: " + AMAP_PACKAGE);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "【高德悬浮窗】高德地图未安装: " + AMAP_PACKAGE);
            return false;
        }
    }

    /**
     * 获取高德地图包名
     *
     * @return 高德地图包名
     */
    public String getAmapPackageName() {
        return AMAP_PACKAGE;
    }

    /**
     * 获取显示悬浮窗的广播 Action
     *
     * @return 广播 Action
     */
    public String getShowMapAction() {
        return ACTION_SHOW_MAP;
    }

    /**
     * 获取关闭悬浮窗的广播 Action
     *
     * @return 广播 Action
     */
    public String getCloseMapAction() {
        return ACTION_CLOSE_MAP;
    }
}
