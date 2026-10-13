package cn.youth996.ui.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Keep;

/**
 * 悬浮窗管理辅助类
 * 
 * 功能说明：
 * 1. 管理悬浮窗的创建、显示、隐藏和销毁
 * 2. 处理悬浮窗权限检查和请求
 * 3. 提供统一的悬浮窗配置接口
 * 
 * 使用方式：
 * - 首先检查悬浮窗权限
 * - 然后创建悬浮窗并添加视图
 * - 通过服务控制悬浮窗的显示/隐藏
 * 
 * @author Youth Desktop Team
 */
@Keep
public class FloatingWindowHelper {

    private static final String TAG = "FloatingWindowHelper";
    
    /** 单例实例 */
    private static FloatingWindowHelper instance;
    
    /** 上下文对象 */
    private Context context;
    
    /** 窗口管理器 */
    private WindowManager windowManager;
    
    /** 悬浮窗布局参数 */
    private WindowManager.LayoutParams layoutParams;
    
    /** 当前是否已添加悬浮窗 */
    private boolean isFloatingWindowAdded = false;
    
    /** 悬浮窗视图 */
    private View floatingView;
    
    /** 悬浮窗宽度（像素） */
    private int floatingWidth = 600;
    
    /** 悬浮窗高度（像素） */
    private int floatingHeight = 400;
    
    /** 悬浮窗初始X位置 */
    private int floatingX = 100;
    
    /** 悬浮窗初始Y位置 */
    private int floatingY = 100;

    /**
     * 私有构造函数（单例模式）
     */
    private FloatingWindowHelper(Context context) {
        this.context = context.getApplicationContext();
        this.windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
    }

    /**
     * 获取单例实例
     *
     * @param context 上下文对象
     * @return FloatingWindowHelper实例
     */
    public static synchronized FloatingWindowHelper getInstance(Context context) {
        if (instance == null) {
            instance = new FloatingWindowHelper(context);
        }
        return instance;
    }

    /**
     * 检查是否有悬浮窗权限
     *
     * Android 6.0及以上需要用户授权
     * Android 6.0以下系统自动授权
     *
     * @return true表示有权限，false表示无权限
     */
    public boolean hasOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }

    /**
     * 请求悬浮窗权限
     *
     * 打开系统设置页面让用户授权
     *
     * @param activity Activity实例（用于启动设置页面）
     */
    public void requestOverlayPermission(Activity activity) {
        if (!hasOverlayPermission()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            activity.startActivity(intent);
            Log.i(TAG, "已打开悬浮窗权限设置页面");
        } else {
            Log.i(TAG, "悬浮窗权限已授权");
        }
    }

    /**
     * 创建悬浮窗布局参数
     *
     * 根据Android版本选择合适的窗口类型
     * - Android 8.0+ 使用 TYPE_APPLICATION_OVERLAY
     * - Android 7.0及以下 使用 TYPE_PHONE
     *
     * @return WindowManager.LayoutParams实例
     */
    public WindowManager.LayoutParams createLayoutParams() {
        layoutParams = new WindowManager.LayoutParams();

        // 设置窗口类型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0+ 使用 TYPE_APPLICATION_OVERLAY
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            // Android 7.0及以下使用 TYPE_PHONE
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        // 设置像素格式
        layoutParams.format = PixelFormat.TRANSLUCENT;

        // 设置窗口标志
        // FLAG_NOT_FOCUSABLE - 不获取焦点，点击事件传递到下层窗口
        // FLAG_NOT_TOUCH_MODAL - 允许触摸事件穿透到下层窗口
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                           WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

        // 设置位置和尺寸
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.x = floatingX;
        layoutParams.y = floatingY;
        layoutParams.width = floatingWidth;
        layoutParams.height = floatingHeight;

        Log.i(TAG, "创建悬浮窗布局参数: type=" + layoutParams.type +
              ", x=" + layoutParams.x + ", y=" + layoutParams.y +
              ", width=" + layoutParams.width + ", height=" + layoutParams.height);

        return layoutParams;
    }

    /**
     * 添加悬浮窗
     *
     * 将指定的视图添加到窗口管理器中显示
     *
     * @param view 要显示的悬浮窗视图
     * @return true表示添加成功，false表示添加失败
     */
    public boolean addFloatingWindow(View view) {
        if (!hasOverlayPermission()) {
            Log.e(TAG, "添加悬浮窗失败：没有悬浮窗权限");
            return false;
        }

        if (isFloatingWindowAdded) {
            Log.w(TAG, "悬浮窗已存在，先移除再添加");
            removeFloatingWindow();
        }

        if (view == null) {
            Log.e(TAG, "添加悬浮窗失败：视图为空");
            return false;
        }

        try {
            floatingView = view;
            WindowManager.LayoutParams params = createLayoutParams();
            windowManager.addView(floatingView, params);
            isFloatingWindowAdded = true;
            Log.i(TAG, "悬浮窗添加成功");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "添加悬浮窗失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 移除悬浮窗
     *
     * 将悬浮窗从窗口管理器中移除
     */
    public void removeFloatingWindow() {
        if (isFloatingWindowAdded && floatingView != null) {
            try {
                windowManager.removeView(floatingView);
                Log.i(TAG, "悬浮窗移除成功");
            } catch (Exception e) {
                Log.e(TAG, "移除悬浮窗失败: " + e.getMessage());
                e.printStackTrace();
            } finally {
                isFloatingWindowAdded = false;
                floatingView = null;
            }
        } else {
            Log.w(TAG, "悬浮窗未添加，无需移除");
        }
    }

    /**
     * 更新悬浮窗位置
     *
     * @param x 新的X坐标
     * @param y 新的Y坐标
     */
    public void updatePosition(int x, int y) {
        if (isFloatingWindowAdded && layoutParams != null) {
            floatingX = x;
            floatingY = y;
            layoutParams.x = x;
            layoutParams.y = y;
            windowManager.updateViewLayout(floatingView, layoutParams);
            Log.d(TAG, "更新悬浮窗位置: x=" + x + ", y=" + y);
        }
    }

    /**
     * 更新悬浮窗尺寸
     *
     * @param width 新的宽度
     * @param height 新的高度
     */
    public void updateSize(int width, int height) {
        if (isFloatingWindowAdded && layoutParams != null) {
            floatingWidth = width;
            floatingHeight = height;
            layoutParams.width = width;
            layoutParams.height = height;
            windowManager.updateViewLayout(floatingView, layoutParams);
            Log.d(TAG, "更新悬浮窗尺寸: width=" + width + ", height=" + height);
        }
    }

    /**
     * 显示悬浮窗
     *
     * 如果悬浮窗已添加但被隐藏，可以调用此方法显示
     */
    public void showFloatingWindow() {
        if (floatingView != null && !isFloatingWindowAdded) {
            try {
                WindowManager.LayoutParams params = createLayoutParams();
                windowManager.addView(floatingView, params);
                isFloatingWindowAdded = true;
                Log.i(TAG, "悬浮窗显示成功");
            } catch (Exception e) {
                Log.e(TAG, "显示悬浮窗失败: " + e.getMessage());
                e.printStackTrace();
            }
        } else if (isFloatingWindowAdded) {
            Log.w(TAG, "悬浮窗已在显示状态");
        } else {
            Log.w(TAG, "悬浮窗未创建，无法显示");
        }
    }

    /**
     * 隐藏悬浮窗
     *
     * 将悬浮窗从窗口中移除（不销毁，下次可以重新显示）
     */
    public void hideFloatingWindow() {
        if (isFloatingWindowAdded && floatingView != null) {
            try {
                windowManager.removeView(floatingView);
                isFloatingWindowAdded = false;
                Log.i(TAG, "悬浮窗隐藏成功");
            } catch (Exception e) {
                Log.e(TAG, "隐藏悬浮窗失败: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "悬浮窗未添加或已隐藏");
        }
    }

    /**
     * 切换悬浮窗显示状态
     *
     * @return 切换后的显示状态，true为显示，false为隐藏
     */
    public boolean toggleFloatingWindow() {
        if (isFloatingWindowAdded) {
            hideFloatingWindow();
            return false;
        } else {
            showFloatingWindow();
            return true;
        }
    }

    /**
     * 检查悬浮窗是否正在显示
     *
     * @return true表示正在显示，false表示未显示
     */
    public boolean isFloatingWindowShowing() {
        return isFloatingWindowAdded;
    }

    /**
     * 获取悬浮窗视图
     *
     * @return 当前的悬浮窗视图，如果未创建则返回null
     */
    public View getFloatingView() {
        return floatingView;
    }

    /**
     * 设置悬浮窗初始位置
     *
     * @param x X坐标
     * @param y Y坐标
     */
    public void setInitialPosition(int x, int y) {
        this.floatingX = x;
        this.floatingY = y;
        if (layoutParams != null) {
            layoutParams.x = x;
            layoutParams.y = y;
        }
    }

    /**
     * 设置悬浮窗默认尺寸
     *
     * @param width 宽度
     * @param height 高度
     */
    public void setDefaultSize(int width, int height) {
        this.floatingWidth = width;
        this.floatingHeight = height;
        if (layoutParams != null) {
            layoutParams.width = width;
            layoutParams.height = height;
        }
    }

    /**
     * 销毁悬浮窗助手
     *
     * 移除悬浮窗并清空实例
     */
    public void destroy() {
        removeFloatingWindow();
        instance = null;
        Log.i(TAG, "悬浮窗助手已销毁");
    }
}
