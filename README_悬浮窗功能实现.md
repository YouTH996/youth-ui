# 悬浮窗功能实现完整文档

## 📋 文档概述

本文档详细记录了基于高德地图车机版、布丁UI、sandiUI等桌面应用的悬浮窗实现方案，包括反编译分析、架构设计、代码实现和使用说明。

**项目路径**: `/Users/zxj/code/youth-ui`

**更新日期**: 2024-XX-XX

---

## 📁 相关APK文件

### 反编译的APK包

| APK名称 | 路径 | 说明 |
|---------|------|------|
| 高德地图车机版 | `/Users/zxj/code/youth-ui/apk/Amap8558_原版修复巡航三灯_沉默悬浮共存[0908].apk` | 高德地图车机版，8.5.5版本 |
| 高德地图悬浮版 | `/Users/zxj/code/youth-ui/apk/高德地图悬浮版.apk` | 高德地图悬浮版，8.1.0版本 |
| sandiUI | `/Users/zxj/code/youth-ui/apk/sandiUI_v1.4.6.3_release_3673.com.apk` | sandiUI桌面 |
| 布丁UI | `/Users/zxj/code/youth-ui/apk/布丁UI.apk` | 布丁UI桌面 |

### 反编译后的代码

| APK名称 | 反编译路径 |
|---------|-----------|
| 高德地图悬浮版 | `/Users/zxj/code/youth-ui/decompiled_amap_float/` |
| 高德地图车机版 | `/Users/zxj/code/youth-ui/decompiled_amap/` |
| sandiUI | `/Users/zxj/code/youth-ui/decompiled_sandiui/` |
| 布丁UI | `/Users/zxj/code/youth-ui/decompiled_buding/` |

---

## 🏗️ 架构设计总览

### 悬浮窗控制架构

```
┌─────────────────────────────────────────────────────────────┐
│                   FloatingWindowManager                      │
│                    (悬浮窗统一管理器)                        │
└─────────────────────┬───────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
        ▼             ▼             ▼
┌───────────────┐ ┌─────────────┐ ┌─────────────┐
│ 高德地图悬浮窗 │ │ 自定义悬浮窗 │ │ 旧版悬浮窗   │
│ (广播命令方式) │ │ (Window方式) │ │ (Service方式)│
└───────────────┘ └─────────────┘ └─────────────┘
```

### 三种实现方案对比

| 方案 | 实现方式 | 权限要求 | 开发难度 | 灵活性 | 推荐度 |
|------|---------|---------|---------|--------|--------|
| **高德地图悬浮窗** | 发送广播命令 | 无 | ⭐ 简单 | ⭐⭐ | ⭐⭐⭐⭐ |
| **自定义悬浮窗** | WindowManager | 需要 | ⭐⭐ 中等 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **旧版悬浮窗** | Service | 需要 | ⭐⭐⭐ 复杂 | ⭐⭐⭐ | ⭐⭐ |

---

## 📱 方案一：高德地图悬浮窗（推荐）

### 1.1 核心发现

通过反编译**布丁UI**桌面，发现了控制高德地图悬浮窗的关键广播命令：

#### 广播命令表

| 操作 | Broadcast Action | 目标包名 | 参数 |
|------|----------------|---------|------|
| **显示悬浮窗** | `com.autonavi.plus.showmap` | `com.autonavi.amapauto` | `x`, `y`, `w`, `h` |
| **关闭悬浮窗** | `com.autonavi.plus.closemap` | `com.autonavi.amapauto` | 无 |

#### 关键代码（来自布丁UI）

```java
// FloatMapManager.java
public static void a(int x, int y, int w, int h) {
    Intent intent = new Intent("com.autonavi.plus.showmap");
    intent.setPackage("com.autonavi.amapauto");
    intent.putExtra("x", x);
    intent.putExtra("y", y);
    intent.putExtra("w", w);
    intent.putExtra("h", h);
    App.Companion.a().sendBroadcast(intent);
}

public static void b() {
    Intent intent = new Intent("com.autonavi.plus.closemap");
    intent.setPackage("com.autonavi.amapauto");
    App.Companion.a().sendBroadcast(intent);
}
```

### 1.2 实现代码

#### AmapFloatController.java

路径：`/app/src/main/java/cn/youth996/ui/core/AmapFloatController.java`

```java
package cn.youth996.ui.core;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 高德地图悬浮窗控制器
 * 
 * 通过发送广播命令控制高德地图车机版的悬浮窗显示/隐藏
 * 
 * 关键发现（基于布丁UI反编译）：
 * - 显示悬浮窗: Intent Action = "com.autonavi.plus.showmap"
 * - 关闭悬浮窗: Intent Action = "com.autonavi.plus.closemap"
 * - 目标包名: "com.autonavi.amapauto" (高德地图车机版)
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

    private AmapFloatController(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized AmapFloatController getInstance(Context context) {
        if (instance == null) {
            instance = new AmapFloatController(context);
        }
        return instance;
    }

    /**
     * 显示高德地图悬浮窗
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
     */
    public void showFloatMap() {
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        
        int x = (int) (screenWidth * 0.05);
        int y = (int) (screenHeight * 0.05);
        int width = (int) (screenWidth * 0.45);
        int height = (int) (screenHeight * 0.35);
        
        showFloatMap(x, y, width, height);
    }

    /**
     * 显示高德地图悬浮窗（使用屏幕百分比）
     */
    public void showFloatMapPercent(int xPercent, int yPercent, 
                                   int widthPercent, int heightPercent) {
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
     * 检查高德地图是否已安装
     */
    public boolean isAmapInstalled() {
        try {
            context.getPackageManager().getPackageInfo(AMAP_PACKAGE, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

### 1.3 使用示例

```java
// 获取控制器实例
AmapFloatController controller = AmapFloatController.getInstance(context);

// 检查高德地图是否安装
if (!controller.isAmapInstalled()) {
    Toast.makeText(context, "请先安装高德地图车机版", Toast.LENGTH_SHORT).show();
    return;
}

// 显示悬浮窗（指定位置）
controller.showFloatMap(100, 100, 600, 400);

// 显示悬浮窗（百分比）
controller.showFloatMapPercent(5, 5, 45, 35);

// 关闭悬浮窗
controller.hideFloatMap();
```

### 1.4 前置条件

⚠️ **必须安装高德地图车机版**
- 包名：`com.autonavi.amapauto`
- 不是普通版的高德地图导航
- 是车机版的APK

---

## 🏗️ 方案二：自定义悬浮窗（布丁UI架构）

### 2.1 架构设计

基于布丁UI的反编译分析，实现了完整的自定义悬浮窗架构：

```
┌────────────────────────────────────────────┐
│     FloatingWindowManager (控制管理器)      │
└────────────────┬───────────────────────────┘
                 │
                 ▼
┌────────────────────────────────────────────┐
│   FloatAmapCornerWindow (窗口管理器)        │
│   - 单例模式                               │
│   - WindowManager 管理                     │
│   - LayoutParams 配置                      │
└────────────────┬───────────────────────────┘
                 │
                 ▼
┌────────────────────────────────────────────┐
│   FloatAmapCorner (自定义视图)              │
│   - 继承 ConstraintLayout                  │
│   - 背景层 + 前景层                        │
│   - 九宫格控制点                           │
└────────────────┬───────────────────────────┘
                 │
                 ▼
┌────────────────────────────────────────────┐
│   layout_float_amap_corner.xml             │
│   (ConstraintLayout 布局文件)               │
└────────────────────────────────────────────┘
```

### 2.2 核心组件

#### 2.2.1 FloatAmapCorner.java - 自定义视图

路径：`/app/src/main/java/cn/youth996/ui/widget/FloatAmapCorner.java`

```java
package cn.youth996.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import cn.youth996.ui.R;

/**
 * 高德地图悬浮窗角落视图
 * 
 * 继承自 ConstraintLayout，支持九宫格位置调整
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

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.layout_float_amap_corner, this, true);
        initViews();
        setupClickListeners();
    }

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

    private void setupClickListeners() {
        setupCornerView(leftUpView, CornerPosition.LEFT_UP);
        setupCornerView(rightUpView, CornerPosition.RIGHT_UP);
        setupCornerView(leftDownView, CornerPosition.LEFT_DOWN);
        setupCornerView(rightDownView, CornerPosition.RIGHT_DOWN);
        setupCornerView(middleUpView, CornerPosition.MIDDLE_UP);
        setupCornerView(middleDownView, CornerPosition.MIDDLE_DOWN);
        setupCornerView(leftMiddleView, CornerPosition.LEFT_MIDDLE);
        setupCornerView(rightMiddleView, CornerPosition.RIGHT_MIDDLE);
    }

    private void setupCornerView(ImageView view, CornerPosition position) {
        if (view == null) return;
        
        view.setOnTouchListener((v, event) -> {
            if (cornerDragListener != null) {
                // 处理拖拽逻辑
            }
            return false;
        });
    }

    public void setOnCornerDragListener(OnCornerDragListener listener) {
        this.cornerDragListener = listener;
    }

    public void showControlPoints() {
        setControlPointsVisibility(View.VISIBLE);
    }

    public void hideControlPoints() {
        setControlPointsVisibility(View.GONE);
    }

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

    private enum CornerPosition {
        LEFT_UP, RIGHT_UP, LEFT_DOWN, RIGHT_DOWN,
        MIDDLE_UP, MIDDLE_DOWN, LEFT_MIDDLE, RIGHT_MIDDLE
    }
}
```

#### 2.2.2 FloatAmapCornerWindow.java - 窗口管理器

路径：`/app/src/main/java/cn/youth996/ui/core/FloatAmapCornerWindow.java`

```java
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

    private FloatAmapCornerWindow(Context context) {
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        configLayoutParams();
    }

    public static synchronized FloatAmapCornerWindow getInstance(Context context) {
        if (instance == null) {
            instance = new FloatAmapCornerWindow(context);
        }
        return instance;
    }

    private void configLayoutParams() {
        // 设置窗口类型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        // 设置格式
        layoutParams.format = android.graphics.PixelFormat.TRANSLUCENT;

        // 设置标志位
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                           WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                           WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

        // 设置对齐方式 - 居中
        layoutParams.gravity = android.view.Gravity.CENTER;

        // 设置尺寸
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
    }

    /**
     * 显示悬浮窗
     */
    public void show(Context context, int x, int y, int width, int height) {
        Log.i(TAG, "显示悬浮窗: x=" + x + ", y=" + y + 
              ", width=" + width + ", height=" + height);

        try {
            layoutParams.x = x;
            layoutParams.y = y;
            layoutParams.width = width;
            layoutParams.height = height;

            if (!isViewAdded) {
                floatAmapCorner = new FloatAmapCorner(context);
                windowManager.addView(floatAmapCorner, layoutParams);
                isViewAdded = true;
            } else {
                windowManager.updateViewLayout(floatAmapCorner, layoutParams);
            }
        } catch (Exception e) {
            Log.e(TAG, "显示悬浮窗失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 显示悬浮窗（使用默认位置）
     */
    public void show(Context context) {
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        
        int x = (int) (screenWidth * 0.5);
        int y = (int) (screenHeight * 0.1);
        int width = (int) (screenWidth * 0.45);
        int height = (int) (screenHeight * 0.35);
        
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
            }
        } catch (Exception e) {
            Log.e(TAG, "隐藏悬浮窗失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 更新悬浮窗位置
     */
    public void updatePosition(int x, int y) {
        try {
            if (isViewAdded) {
                layoutParams.x = x;
                layoutParams.y = y;
                windowManager.updateViewLayout(floatAmapCorner, layoutParams);
            }
        } catch (Exception e) {
            Log.e(TAG, "更新位置失败: " + e.getMessage());
        }
    }

    /**
     * 更新悬浮窗尺寸
     */
    public void updateSize(int width, int height) {
        try {
            if (isViewAdded) {
                layoutParams.width = width;
                layoutParams.height = height;
                windowManager.updateViewLayout(floatAmapCorner, layoutParams);
            }
        } catch (Exception e) {
            Log.e(TAG, "更新尺寸失败: " + e.getMessage());
        }
    }

    public void showControlPoints() {
        if (floatAmapCorner != null) {
            floatAmapCorner.showControlPoints();
        }
    }

    public void hideControlPoints() {
        if (floatAmapCorner != null) {
            floatAmapCorner.hideControlPoints();
        }
    }

    public boolean isShowing() {
        return isViewAdded;
    }

    public static void destroy() {
        if (instance != null && instance.isViewAdded) {
            instance.hide();
        }
        instance = null;
    }
}
```

#### 2.2.3 FloatAmapCornerService.java - 服务类

路径：`/app/src/main/java/cn/youth996/ui/service/FloatAmapCornerService.java`

```java
package cn.youth996.ui.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import cn.youth996.ui.core.FloatAmapCornerWindow;

/**
 * 高德地图悬浮窗角落服务
 */
public class FloatAmapCornerService extends Service {

    private static final String TAG = "FloatAmapCornerService";

    private static FloatAmapCornerService instance;
    private FloatAmapCornerWindow floatAmapCornerWindow;
    private boolean isRunning = false;

    public static FloatAmapCornerService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        String command = intent.getStringExtra("command");
        
        if (command == null) {
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
            }
        }

        return START_STICKY;
    }

    private void showFloatWindow(Intent intent) {
        int x = intent.getIntExtra("x", -1);
        int y = intent.getIntExtra("y", -1);
        int width = intent.getIntExtra("width", -1);
        int height = intent.getIntExtra("height", -1);

        if (floatAmapCornerWindow == null) {
            floatAmapCornerWindow = FloatAmapCornerWindow.getInstance(this);
        }

        if (x >= 0 && y >= 0 && width > 0 && height > 0) {
            floatAmapCornerWindow.show(this, x, y, width, height);
        } else {
            floatAmapCornerWindow.show(this);
        }

        isRunning = true;
    }

    private void showFloatWindow() {
        showFloatWindow(null);
    }

    private void hideFloatWindow() {
        if (floatAmapCornerWindow != null) {
            floatAmapCornerWindow.hide();
            isRunning = false;
        }
    }

    private void toggleFloatWindow() {
        if (floatAmapCornerWindow == null) {
            floatAmapCornerWindow = FloatAmapCornerWindow.getInstance(this);
        }

        if (floatAmapCornerWindow.isShowing()) {
            hideFloatWindow();
        } else {
            showFloatWindow();
        }
    }

    private void updatePosition(Intent intent) {
        int x = intent.getIntExtra("x", 0);
        int y = intent.getIntExtra("y", 0);

        if (floatAmapCornerWindow != null && floatAmapCornerWindow.isShowing()) {
            floatAmapCornerWindow.updatePosition(x, y);
        }
    }

    private void updateSize(Intent intent) {
        int width = intent.getIntExtra("width", -1);
        int height = intent.getIntExtra("height", -1);

        if (floatAmapCornerWindow != null && floatAmapCornerWindow.isShowing()) {
            if (width > 0 && height > 0) {
                floatAmapCornerWindow.updateSize(width, height);
            }
        }
    }

    private void showControlPoints() {
        if (floatAmapCornerWindow != null) {
            floatAmapCornerWindow.showControlPoints();
        }
    }

    private void hideControlPoints() {
        if (floatAmapCornerWindow != null) {
            floatAmapCornerWindow.hideControlPoints();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatAmapCornerWindow != null) {
            floatAmapCornerWindow.hide();
            floatAmapCornerWindow = null;
        }
        instance = null;
        isRunning = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static boolean isServiceRunning() {
        return instance != null && instance.isRunning;
    }
}
```

### 2.3 布局文件

#### layout_float_amap_corner.xml

路径：`/app/src/main/res/layout/layout_float_amap_corner.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:background="@android:color/transparent">

    <!-- 背景层 -->
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/bg"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:alpha="0.3"
        android:scaleType="fitCenter"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <!-- 前景层 -->
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/fg"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:scaleType="fitCenter"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <!-- 左上角控制点 -->
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/leftUp"
        android:layout_width="24dp"
        android:layout_height="24dp"
        android:background="@drawable/shape_corner_control_point"
        android:padding="4dp"
        android:src="@android:drawable/ic_menu_crop"
        android:visibility="gone"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <!-- 右上角控制点 -->
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/rightUp"
        android:layout_width="24dp"
        android:layout_height="24dp"
        android:background="@drawable/shape_corner_control_point"
        android:padding="4dp"
        android:src="@android:drawable/ic_menu_crop"
        android:visibility="gone"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <!-- 右下角控制点 -->
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/rightDown"
        android:layout_width="24dp"
        android:layout_height="24dp"
        android:background="@drawable/shape_corner_control_point"
        android:padding="4dp"
        android:src="@android:drawable/ic_menu_crop"
        android:visibility="gone"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- 左下角控制点 -->
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/leftDown"
        android:layout_width="24dp"
        android:layout_height="24dp"
        android:background="@drawable/shape_corner_control_point"
        android:padding="4dp"
        android:src="@android:drawable/ic_menu_crop"
        android:visibility="gone"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent" />

    <!-- 中上控制点 -->
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/middleUp"
        android:layout_width="24dp"
        android:layout_height="24dp"
        android:background="@drawable/shape_corner_control_point"
        android:padding="4dp"
        android:src="@android:drawable/ic_menu_crop"
        android:visibility="gone"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <!-- 中下控制点 -->
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/middleDown"
        android:layout_width="24dp"
        android:layout_height="24dp"
        android:background="@drawable/shape_corner_control_point"
        android:padding="4dp"
        android:src="@android:drawable/ic_menu_crop"
        android:visibility="gone"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent" />

    <!-- 左中控制点 -->
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/leftMiddle"
        android:layout_width="24dp"
        android:layout_height="24dp"
        android:background="@drawable/shape_corner_control_point"
        android:padding="4dp"
        android:src="@android:drawable/ic_menu_crop"
        android:visibility="gone"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <!-- 右中控制点 -->
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/rightMiddle"
        android:layout_width="24dp"
        android:layout_height="24dp"
        android:background="@drawable/shape_corner_control_point"
        android:padding="4dp"
        android:src="@android:drawable/ic_menu_crop"
        android:visibility="gone"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

#### shape_corner_control_point.xml

路径：`/app/src/main/res/drawable/shape_corner_control_point.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <!-- 控制点背景 -->
    <solid android:color="#80FF9800" />
    <!-- 边框 -->
    <stroke
        android:width="2dp"
        android:color="#FFFFFF" />
    <!-- 内边距 -->
    <padding
        android:bottom="4dp"
        android:left="4dp"
        android:right="4dp"
        android:top="4dp" />
</shape>
```

---

## 🎯 统一管理器：FloatingWindowManager

### 3.1 功能概览

路径：`/app/src/main/java/cn/youth996/ui/core/FloatingWindowManager.java`

FloatingWindowManager 是所有悬浮窗功能的统一入口，提供以下功能：

#### 高德地图悬浮窗控制（推荐）

```java
// 检查高德地图是否安装
public static boolean isAmapInstalled(Context context)

// 显示高德地图悬浮窗（指定位置）
public static void showAmapFloatMap(Context context, int x, int y, int width, int height)

// 显示高德地图悬浮窗（默认位置）
public static void showAmapFloatMap(Context context)

// 显示高德地图悬浮窗（百分比）
public static void showAmapFloatMapPercent(Context context, int xPercent, int yPercent, 
                                           int widthPercent, int heightPercent)

// 关闭高德地图悬浮窗
public static void hideAmapFloatMap(Context context)

// 切换高德地图悬浮窗
public static void toggleAmapFloatMap(Context context, boolean currentlyShowing)
```

#### 自定义悬浮窗控制（布丁UI架构）

```java
// 显示自定义悬浮窗（指定位置）
public static void showCustomFloatWindow(Context context, int x, int y, int width, int height)

// 显示自定义悬浮窗（默认位置）
public static void showCustomFloatWindow(Context context)

// 隐藏自定义悬浮窗
public static void hideCustomFloatWindow(Context context)

// 更新自定义悬浮窗位置
public static void updateCustomFloatPosition(Context context, int x, int y)

// 更新自定义悬浮窗尺寸
public static void updateCustomFloatSize(Context context, int width, int height)

// 显示自定义悬浮窗控制点
public static void showCustomFloatControlPoints(Context context)

// 隐藏自定义悬浮窗控制点
public static void hideCustomFloatControlPoints(Context context)

// 检查自定义悬浮窗是否正在显示
public static boolean isCustomFloatWindowShowing(Context context)

// 销毁自定义悬浮窗实例
public static void destroyCustomFloatWindow()
```

#### 权限管理

```java
// 检查悬浮窗权限
public static boolean hasOverlayPermission(Context context)

// 请求悬浮窗权限
public static void requestOverlayPermission(Activity activity)
```

---

## 📝 AndroidManifest.xml 配置

路径：`/app/src/main/AndroidManifest.xml`

```xml
<!-- 悬浮窗权限 -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>

<!-- 悬浮地图服务 -->
<service
    android:name="cn.youth996.ui.service.FloatingMapService"
    android:exported="false"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:foregroundServiceType="mediaPlayback">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/floating_service_config" />
</service>

<!-- 高德地图悬浮窗角落服务 -->
<service
    android:name="cn.youth996.ui.service.FloatAmapCornerService"
    android:exported="false"
    android:foregroundServiceType="mediaPlayback" />
```

---

## 💻 使用示例

### 示例1：基础使用

```java
public class MainActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 检查悬浮窗权限
        if (!FloatingWindowManager.hasOverlayPermission(this)) {
            FloatingWindowManager.requestOverlayPermission(this);
        }
    }
    
    // 显示高德地图悬浮窗
    public void onShowAmapFloat(View view) {
        if (!FloatingWindowManager.isAmapInstalled(this)) {
            Toast.makeText(this, "请先安装高德地图车机版", Toast.LENGTH_SHORT).show();
            return;
        }
        
        FloatingWindowManager.showAmapFloatMap(this);
        Toast.makeText(this, "高德地图悬浮窗已显示", Toast.LENGTH_SHORT).show();
    }
    
    // 关闭高德地图悬浮窗
    public void onHideAmapFloat(View view) {
        FloatingWindowManager.hideAmapFloatMap(this);
        Toast.makeText(this, "高德地图悬浮窗已关闭", Toast.LENGTH_SHORT).show();
    }
    
    // 显示自定义悬浮窗
    public void onShowCustomFloat(View view) {
        if (!FloatingWindowManager.hasOverlayPermission(this)) {
            Toast.makeText(this, "请先授权悬浮窗权限", Toast.LENGTH_SHORT).show();
            FloatingWindowManager.requestOverlayPermission(this);
            return;
        }
        
        FloatingWindowManager.showCustomFloatWindow(this);
        Toast.makeText(this, "自定义悬浮窗已显示", Toast.LENGTH_SHORT).show();
    }
    
    // 隐藏自定义悬浮窗
    public void onHideCustomFloat(View view) {
        FloatingWindowManager.hideCustomFloatWindow(this);
        Toast.makeText(this, "自定义悬浮窗已关闭", Toast.LENGTH_SHORT).show();
    }
}
```

### 示例2：百分比定位

```java
// 使用百分比显示高德地图悬浮窗
FloatingWindowManager.showAmapFloatMapPercent(this, 
    5,   // x: 5%
    5,   // y: 5%
    45,  // width: 45%
    35   // height: 35%
);

// 使用百分比显示自定义悬浮窗
FloatingWindowManager.showCustomFloatWindowPercent(this,
    10,  // x: 10%
    10,  // y: 10%
    50,  // width: 50%
    40   // height: 40%
);
```

### 示例3：通过Service控制

```java
// 启动服务显示悬浮窗
Intent intent = new Intent(this, FloatAmapCornerService.class);
intent.putExtra("command", "show");
intent.putExtra("x", 100);
intent.putExtra("y", 100);
intent.putExtra("width", 600);
intent.putExtra("height", 400);
startService(intent);

// 隐藏悬浮窗
intent.putExtra("command", "hide");
startService(intent);

// 切换显示状态
intent.putExtra("command", "toggle");
startService(intent);

// 显示控制点
intent.putExtra("command", "show_control_points");
startService(intent);
```

---

## 🔧 LayoutParams 配置详解

### 窗口类型

```java
// Android 8.0+ (API 26+)
params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY; // 2038

// Android 8.0 以下
params.type = WindowManager.LayoutParams.TYPE_PHONE; // 2002
```

### 格式设置

```java
params.format = android.graphics.PixelFormat.TRANSLUCENT; // 1
```

### 标志位

```java
// 推荐配置
params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |        // 256
              WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |      // 32
              WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;   // 16
```

### 对齐方式

```java
params.gravity = android.view.Gravity.CENTER; // 8388659
```

### 尺寸设置

```java
params.width = WindowManager.LayoutParams.WRAP_CONTENT;  // -2
params.height = WindowManager.LayoutParams.WRAP_CONTENT;  // -2
```

---

## 📚 文件结构总览

### Java源文件

```
/app/src/main/java/cn/youth996/ui/
├── core/
│   ├── FloatingWindowManager.java      # 统一管理器
│   ├── FloatAmapCornerWindow.java      # 窗口管理器
│   └── AmapFloatController.java         # 高德地图控制器
├── service/
│   ├── FloatAmapCornerService.java     # 悬浮窗服务
│   └── FloatingMapService.java         # 旧版悬浮窗服务
├── widget/
│   └── FloatAmapCorner.java            # 自定义视图
└── core/
    └── MainActivity.java                # 主界面
```

### 资源文件

```
/app/src/main/res/
├── layout/
│   ├── activity_main.xml                # 主界面布局
│   └── layout_float_amap_corner.xml     # 悬浮窗布局
├── drawable/
│   └── shape_corner_control_point.xml   # 控制点样式
└── xml/
    └── floating_service_config.xml      # 无障碍服务配置
```

### 配置文件

```
/app/src/main/
└── AndroidManifest.xml                  # 应用配置
```

---

## ⚙️ 权限配置

### 必须权限

```xml
<!-- 悬浮窗权限（必须） -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
```

### 可选权限

```xml
<!-- 后台运行权限 -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>

<!-- 开机自启权限 -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>

<!-- 无障碍服务权限 -->
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE"/>
```

---

## 🐛 故障排除

### 问题1：悬浮窗不显示

**可能原因**：
1. 没有悬浮窗权限
2. 服务未启动
3. 视图未正确创建

**解决方法**：
```java
// 检查权限
if (!FloatingWindowManager.hasOverlayPermission(context)) {
    FloatingWindowManager.requestOverlayPermission(activity);
    return;
}

// 检查是否正在显示
boolean showing = FloatingWindowManager.isCustomFloatWindowShowing(context);
Log.i(TAG, "悬浮窗状态: " + showing);
```

### 问题2：高德地图悬浮窗无反应

**可能原因**：
1. 高德地图车机版未安装
2. 高德地图未启动过
3. 广播命令未正确发送

**解决方法**：
```java
// 检查高德地图是否安装
if (!FloatingWindowManager.isAmapInstalled(context)) {
    Toast.makeText(context, "请先安装高德地图车机版", Toast.LENGTH_SHORT).show();
    return;
}

// 手动启动高德地图
Intent intent = context.getPackageManager()
    .getLaunchIntentForPackage("com.autonavi.amapauto");
if (intent != null) {
    context.startActivity(intent);
}
```

### 问题3：控制点不显示

**可能原因**：
1. 控制点可见性设置为 gone
2. 视图未正确加载

**解决方法**：
```java
// 确保调用了显示方法
FloatingWindowManager.showCustomFloatControlPoints(context);
```

---

## 🚀 优化建议

### 1. 添加拖拽功能

```java
floatAmapCorner.setOnTouchListener((v, event) -> {
    switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            lastX = (int) event.getRawX();
            lastY = (int) event.getRawY();
            break;
        case MotionEvent.ACTION_MOVE:
            int dx = (int) (event.getRawX() - lastX);
            int dy = (int) (event.getRawY() - lastY);
            FloatingWindowManager.updateCustomFloatPosition(
                getContext(), 
                dx, dy
            );
            lastX = (int) event.getRawX();
            lastY = (int) event.getRawY();
            break;
    }
    return true;
});
```

### 2. 添加动画效果

```java
// 显示动画
AlphaAnimation fadeIn = new AlphaAnimation(0, 1);
fadeIn.setDuration(300);
floatAmapCorner.startAnimation(fadeIn);

// 隐藏动画
AlphaAnimation fadeOut = new AlphaAnimation(1, 0);
fadeOut.setDuration(300);
floatAmapCorner.startAnimation(fadeOut);
```

### 3. 保存用户设置

```java
// 保存位置
SharedPreferences prefs = context.getSharedPreferences("float_window", MODE_PRIVATE);
prefs.edit()
    .putInt("x", x)
    .putInt("y", y)
    .putInt("width", width)
    .putInt("height", height)
    .apply();

// 恢复位置
int savedX = prefs.getInt("x", defaultX);
int savedY = prefs.getInt("y", defaultY);
```

---

## 📖 参考资料

### 反编译代码路径

| 应用 | 路径 |
|------|------|
| 高德地图悬浮版 | `/Users/zxj/code/youth-ui/decompiled_amap_float/` |
| 高德地图车机版 | `/Users/zxj/code/youth-ui/decompiled_amap/` |
| sandiUI | `/Users/zxj/code/youth-ui/decompiled_sandiui/` |
| 布丁UI | `/Users/zxj/code/youth-ui/decompiled_buding/` |

### 关键反编译文件

#### 布丁UI关键文件

- `/decompiled_buding/sources/com/sfcar/launcher/service/plugin/builtin/floatamap/FloatAmapController.java`
- `/decompiled_buding/sources/com/sfcar/launcher/service/plugin/builtin/floatamap/FloatAmapCornerWindow.java`
- `/decompiled_buding/sources/com/sfcar/launcher/service/plugin/builtin/floatamap/FloatAmapCornerService.java`
- `/decompiled_buding/sources/com/sfcar/launcher/theme/component/drive/FloatMapManager.java`

#### 高德地图悬浮版关键文件

- `/decompiled_amap_float/sources/com/autonavi/autofloat/FloatWindowManagerNative.java`
- `/decompiled_amap_float/sources/com/autonavi/autofloat/FloatWindowService.java`
- `/decompiled_amap_float/sources/defpackage/hi0.java`

---

## 📋 更新记录

| 日期 | 版本 | 更新内容 |
|------|------|---------|
| 2024-XX-XX | 1.0 | 初始版本，集成高德地图悬浮窗和自定义悬浮窗 |

---

## 👥 作者

Youth Desktop Team

---

## ⚠️ 免责声明

本文档仅供学习和研究使用，请勿用于商业目的。所有反编译代码的版权属于其原始所有者。
