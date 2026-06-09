package cn.youth996.ui.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import cn.youth996.ui.R;

/**
 * 悬浮地图服务
 * 
 * 功能说明：
 * 1. 通过AccessibilityService实现悬浮窗功能
 * 2. 支持地图图片的实时显示和更新
 * 3. 提供悬浮窗的显示、隐藏、位置调整等功能
 * 
 * 技术特点：
 * - 使用AccessibilityService实现（参考sandiUI实现方式）
 * - 支持多点触摸拖动
 * - 自动适配不同Android版本
 * - 低内存占用的位图缓存
 * 
 * 使用方式：
 * 1. 在系统设置中开启无障碍服务和悬浮窗权限
 * 2. 发送Intent命令控制悬浮窗
 * 
 * Intent命令格式：
 * - 启动悬浮窗：intent.putExtra("command", "show")
 * - 隐藏悬浮窗：intent.putExtra("command", "hide")
 * - 切换悬浮窗：intent.putExtra("command", "toggle")
 * - 更新地图：intent.putExtra("command", "update_map")
 * 
 * @author Youth Desktop Team
 */
public class FloatingMapService extends AccessibilityService {

    private static final String TAG = "FloatingMapService";
    
    /** 服务实例 */
    private static FloatingMapService instance;
    
    /** 是否正在运行 */
    private static boolean isRunning = false;

    // ==================== 窗口管理 ====================
    /** 窗口管理器 */
    private WindowManager windowManager;
    
    /** 悬浮窗容器 */
    private FrameLayout floatingContainer;
    
    /** 地图显示视图 */
    private ImageView mapImageView;
    
    /** 状态文本视图 */
    private TextView statusTextView;
    
    /** 悬浮窗布局参数 */
    private WindowManager.LayoutParams layoutParams;
    
    /** 是否已添加悬浮窗 */
    private boolean isFloatingWindowAdded = false;

    // ==================== 状态管理 ====================
    /** 主线程Handler */
    private Handler mainHandler;
    
    /** 悬浮窗宽度 */
    private int windowWidth = 800;
    
    /** 悬浮窗高度 */
    private int windowHeight = 600;
    
    /** 悬浮窗X坐标 */
    private int windowX = 100;
    
    /** 悬浮窗Y坐标 */
    private int windowY = 100;
    
    /** 触摸起始X坐标 */
    private float touchStartX;
    
    /** 触摸起始Y坐标 */
    private float touchStartY;
    
    /** 触摸起始时悬浮窗X坐标 */
    private int windowStartX;
    
    /** 触摸起始时悬浮窗Y坐标 */
    private int windowStartY;

    /**
     * 获取服务实例
     *
     * @return FloatingMapService实例，如果服务未运行则返回null
     */
    public static FloatingMapService getInstance() {
        return instance;
    }

    /**
     * 检查服务是否正在运行
     *
     * @return true表示服务正在运行，false表示服务未运行
     */
    public static boolean isServiceRunning() {
        return isRunning;
    }

    // ==================== 服务生命周期 ====================

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        isRunning = true;
        
        mainHandler = new Handler(Looper.getMainLooper());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        Log.i(TAG, "═══════════════════════════════════════════════════════════");
        Log.i(TAG, "【悬浮地图服务】服务已创建 - FloatingMapService");
        Log.i(TAG, "═══════════════════════════════════════════════════════════");
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "【悬浮地图服务】服务即将销毁");
        
        // 移除悬浮窗
        removeFloatingWindow();
        
        instance = null;
        isRunning = false;
        super.onDestroy();
        
        Log.w(TAG, "【悬浮地图服务】服务已销毁，实例已清空");
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "【悬浮地图服务】服务连接成功");

        // 配置无障碍服务信息
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            info.flags = AccessibilityServiceInfo.DEFAULT;
            info.notificationTimeout = 100;
            
            setServiceInfo(info);
            Log.i(TAG, "【悬浮地图服务】服务配置完成");
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }
        
        int eventType = event.getEventType();
        
        // 监听窗口变化事件
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = event.getPackageName() != null ?
                                event.getPackageName().toString() : "null";
            Log.d(TAG, "【窗口变化】包名: " + packageName);
        }
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "⚠️ 【悬浮地图服务】服务被中断");
    }

    // ==================== Intent命令处理 ====================

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getExtras() != null) {
            String command = intent.getStringExtra("command");
            
            if (command != null) {
                Log.i(TAG, "【服务命令】收到命令: " + command);
                
                switch (command) {
                    case "show":
                        showFloatingWindow();
                        break;
                    case "hide":
                        hideFloatingWindow();
                        break;
                    case "toggle":
                        toggleFloatingWindow();
                        break;
                    case "update_map":
                        handleMapUpdate(intent);
                        break;
                    default:
                        Log.w(TAG, "【服务命令】未知命令: " + command);
                        break;
                }
            }
        }
        
        return super.onStartCommand(intent, flags, startId);
    }

    // ==================== 悬浮窗核心功能 ====================

    /**
     * 创建悬浮窗布局参数
     *
     * 根据Android版本选择合适的窗口类型
     */
    private WindowManager.LayoutParams createLayoutParams() {
        layoutParams = new WindowManager.LayoutParams();

        // 根据Android版本设置窗口类型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0+ (API 26+)
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            // Android 7.1及以下
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        // 设置格式和标志
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                           WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                           WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        // 设置位置和尺寸
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.x = windowX;
        layoutParams.y = windowY;
        layoutParams.width = windowWidth;
        layoutParams.height = windowHeight;

        return layoutParams;
    }

    /**
     * 创建悬浮窗视图
     */
    private void createFloatingWindowView() {
        // 创建容器
        floatingContainer = new FrameLayout(this);
        floatingContainer.setBackgroundColor(Color.argb(200, 30, 30, 30));
        
        // 创建地图显示视图
        mapImageView = new ImageView(this);
        mapImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mapImageView.setBackgroundColor(Color.rgb(40, 40, 40));
        
        FrameLayout.LayoutParams mapParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        floatingContainer.addView(mapImageView, mapParams);
        
        // 创建状态文本
        statusTextView = new TextView(this);
        statusTextView.setText("悬浮地图");
        statusTextView.setTextColor(Color.WHITE);
        statusTextView.setTextSize(14);
        statusTextView.setPadding(20, 20, 20, 20);
        
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.gravity = Gravity.TOP | Gravity.START;
        floatingContainer.addView(statusTextView, textParams);
        
        // 设置触摸监听器（支持拖动）
        setupTouchListener();
        
        Log.i(TAG, "【悬浮窗】创建视图完成");
    }

    /**
     * 设置触摸监听器
     */
    private void setupTouchListener() {
        floatingContainer.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 记录触摸起始位置
                    touchStartX = event.getRawX();
                    touchStartY = event.getRawY();
                    windowStartX = layoutParams.x;
                    windowStartY = layoutParams.y;
                    Log.d(TAG, "【触摸】按下: rawX=" + touchStartX + ", rawY=" + touchStartY);
                    return true;
                    
                case MotionEvent.ACTION_MOVE:
                    // 计算偏移量并更新位置
                    int newX = windowStartX + (int) (event.getRawX() - touchStartX);
                    int newY = windowStartY + (int) (event.getRawY() - touchStartY);
                    layoutParams.x = newX;
                    layoutParams.y = newY;
                    windowManager.updateViewLayout(floatingContainer, layoutParams);
                    Log.v(TAG, "【触摸】移动: x=" + newX + ", y=" + newY);
                    return true;
                    
                case MotionEvent.ACTION_UP:
                    // 记录最终位置
                    Log.d(TAG, "【触摸】抬起: 最终位置 x=" + layoutParams.x + ", y=" + layoutParams.y);
                    return true;
            }
            return false;
        });
    }

    /**
     * 添加悬浮窗到窗口
     */
    public void addFloatingWindow() {
        if (isFloatingWindowAdded) {
            Log.w(TAG, "【悬浮窗】悬浮窗已存在");
            return;
        }
        
        try {
            // 创建视图
            createFloatingWindowView();
            
            // 创建布局参数
            WindowManager.LayoutParams params = createLayoutParams();
            
            // 添加到窗口
            windowManager.addView(floatingContainer, params);
            isFloatingWindowAdded = true;
            
            Log.i(TAG, "【悬浮窗】添加成功: 位置(" + windowX + ", " + windowY + 
                  "), 尺寸(" + windowWidth + "x" + windowHeight + ")");
            
        } catch (Exception e) {
            Log.e(TAG, "【悬浮窗】添加失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 移除悬浮窗
     */
    public void removeFloatingWindow() {
        if (isFloatingWindowAdded && floatingContainer != null) {
            try {
                windowManager.removeView(floatingContainer);
                isFloatingWindowAdded = false;
                floatingContainer = null;
                mapImageView = null;
                statusTextView = null;
                Log.i(TAG, "【悬浮窗】移除成功");
            } catch (Exception e) {
                Log.e(TAG, "【悬浮窗】移除失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 显示悬浮窗
     */
    public void showFloatingWindow() {
        if (!isFloatingWindowAdded) {
            addFloatingWindow();
        } else {
            Log.w(TAG, "【悬浮窗】悬浮窗已在显示状态");
        }
    }

    /**
     * 隐藏悬浮窗
     */
    public void hideFloatingWindow() {
        removeFloatingWindow();
    }

    /**
     * 切换悬浮窗显示状态
     */
    public void toggleFloatingWindow() {
        if (isFloatingWindowAdded) {
            hideFloatingWindow();
        } else {
            showFloatingWindow();
        }
    }

    /**
     * 处理地图更新
     */
    private void handleMapUpdate(Intent intent) {
        // 从Intent获取地图数据
        // 这里可以扩展为接收实际的地图数据
        Log.i(TAG, "【地图更新】收到更新请求");
    }

    /**
     * 更新地图图片
     *
     * @param bitmap 地图位图
     */
    public void updateMapImage(Bitmap bitmap) {
        mainHandler.post(() -> {
            if (mapImageView != null && bitmap != null) {
                mapImageView.setImageBitmap(bitmap);
                if (statusTextView != null) {
                    statusTextView.setText("悬浮地图 - 已更新");
                }
                Log.i(TAG, "【地图更新】地图图片已更新");
            }
        });
    }

    /**
     * 设置状态文本
     *
     * @param text 状态文本
     */
    public void setStatusText(String text) {
        mainHandler.post(() -> {
            if (statusTextView != null) {
                statusTextView.setText(text);
            }
        });
    }

    /**
     * 更新悬浮窗位置
     *
     * @param x X坐标
     * @param y Y坐标
     */
    public void updatePosition(int x, int y) {
        windowX = x;
        windowY = y;
        if (isFloatingWindowAdded && layoutParams != null) {
            layoutParams.x = x;
            layoutParams.y = y;
            windowManager.updateViewLayout(floatingContainer, layoutParams);
            Log.i(TAG, "【位置更新】新位置: (" + x + ", " + y + ")");
        }
    }

    /**
     * 更新悬浮窗尺寸
     *
     * @param width 宽度
     * @param height 高度
     */
    public void updateSize(int width, int height) {
        windowWidth = width;
        windowHeight = height;
        if (isFloatingWindowAdded && layoutParams != null) {
            layoutParams.width = width;
            layoutParams.height = height;
            windowManager.updateViewLayout(floatingContainer, layoutParams);
            Log.i(TAG, "【尺寸更新】新尺寸: " + width + "x" + height);
        }
    }

    /**
     * 检查悬浮窗是否正在显示
     */
    public boolean isShowing() {
        return isFloatingWindowAdded;
    }
}
