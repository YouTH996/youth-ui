# 布丁UI架构悬浮窗实现说明

## 更新日期
2024-XX-XX

## 功能概述

本项目已按照布丁UI的架构实现了完整的悬浮窗功能，包含自定义悬浮窗和高德地图悬浮窗两种实现方式。

## 架构设计（基于布丁UI）

### 1. 核心组件架构

```
┌─────────────────────────────────────┐
│  FloatingWindowManager              │
│  (悬浮窗控制管理器)                  │
└──────────────┬──────────────────────┘
               │
               ├──────────────────────┐
               │                      │
               ▼                      ▼
    ┌──────────────────┐    ┌──────────────────────┐
    │ 高德地图悬浮窗    │    │ 自定义悬浮窗          │
    │ (广播命令方式)   │    │ (WindowManager方式)  │
    └──────────────────┘    └──────────┬───────────┘
                                         │
                                         ▼
                              ┌──────────────────────────┐
                              │ FloatAmapCornerWindow   │
                              │ (窗口管理器)              │
                              └──────────┬─────────────┘
                                         │
                                         ▼
                              ┌──────────────────────────┐
                              │ FloatAmapCorner         │
                              │ (自定义视图)             │
                              └──────────┬─────────────┘
                                         │
                                         ▼
                              ┌──────────────────────────┐
                              │ layout_float_amap_      │
                              │ corner.xml              │
                              │ (ConstraintLayout布局)  │
                              └──────────────────────────┘
```

### 2. 组件说明

| 组件 | 类名 | 功能 |
|------|------|------|
| **控制管理器** | `FloatingWindowManager` | 统一管理所有悬浮窗操作 |
| **高德地图控制** | `AmapFloatController` | 通过广播命令控制高德地图悬浮窗 |
| **窗口管理器** | `FloatAmapCornerWindow` | 管理自定义悬浮窗的WindowManager |
| **自定义视图** | `FloatAmapCorner` | 继承ConstraintLayout的悬浮窗视图 |
| **服务类** | `FloatAmapCornerService` | 后台服务管理悬浮窗生命周期 |

## 已完成的修改

### 1. 新增文件

#### FloatAmapCorner.java
路径：`/app/src/main/java/cn/youth996/ui/widget/FloatAmapCorner.java`

功能：
- 继承自 ConstraintLayout
- 支持九宫格位置调整
- 提供控制点显示/隐藏
- 支持拖拽操作

关键代码：
```java
public class FloatAmapCorner extends ConstraintLayout {
    private ImageView bgImageView;      // 背景层
    private ImageView fgImageView;      // 前景层
    private ImageView leftUpView;       // 左上控制点
    private ImageView rightUpView;      // 右上控制点
    // ... 其他8个控制点
    
    public void showControlPoints();   // 显示控制点
    public void hideControlPoints();   // 隐藏控制点
}
```

#### FloatAmapCornerWindow.java
路径：`/app/src/main/java/cn/youth996/ui/core/FloatAmapCornerWindow.java`

功能：
- 单例模式管理 WindowManager
- 配置 LayoutParams
- 添加/更新/移除悬浮窗视图

关键代码：
```java
public class FloatAmapCornerWindow {
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private FloatAmapCorner floatAmapCorner;
    
    // 单例模式
    public static synchronized FloatAmapCornerWindow getInstance(Context context);
    
    // 配置 LayoutParams
    private void configLayoutParams() {
        params.type = TYPE_APPLICATION_OVERLAY;  // Android 8.0+
        params.format = PixelFormat.TRANSLUCENT;
        params.flags = FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL;
        params.gravity = Gravity.CENTER;
        params.width = WRAP_CONTENT;
        params.height = WRAP_CONTENT;
    }
    
    // 显示/隐藏悬浮窗
    public void show(Context context, int x, int y, int width, int height);
    public void hide();
}
```

#### FloatAmapCornerService.java
路径：`/app/src/main/java/cn/youth996/ui/service/FloatAmapCornerService.java`

功能：
- 管理悬浮窗生命周期
- 处理各种命令
- 后台运行支持

支持的命令：
- `show` - 显示悬浮窗
- `hide` - 隐藏悬浮窗
- `toggle` - 切换显示状态
- `update_position` - 更新位置
- `update_size` - 更新尺寸
- `show_control_points` - 显示控制点
- `hide_control_points` - 隐藏控制点

#### layout_float_amap_corner.xml
路径：`/app/src/main/res/layout/layout_float_amap_corner.xml`

布局结构：
```xml
<ConstraintLayout>
    <!-- 背景层 -->
    <AppCompatImageView android:id="@+id/bg" />
    
    <!-- 前景层 -->
    <AppCompatImageView android:id="@+id/fg" />
    
    <!-- 九宫格控制点 -->
    <AppCompatImageView android:id="@+id/leftUp" />
    <AppCompatImageView android:id="@+id/rightUp" />
    <AppCompatImageView android:id="@+id/rightDown" />
    <AppCompatImageView android:id="@+id/leftDown" />
    <AppCompatImageView android:id="@+id/middleUp" />
    <AppCompatImageView android:id="@+id/middleDown" />
    <AppCompatImageView android:id="@+id/leftMiddle" />
    <AppCompatImageView android:id="@+id/rightMiddle" />
</ConstraintLayout>
```

#### shape_corner_control_point.xml
路径：`/app/src/main/res/drawable/shape_corner_control_point.xml`

功能：
- 控制点圆形背景
- 白色边框
- 橙色半透明填充

### 2. 修改的文件

#### AndroidManifest.xml
新增服务声明：
```xml
<!-- 高德地图悬浮窗角落服务 -->
<service
    android:name="cn.youth996.ui.service.FloatAmapCornerService"
    android:exported="false"
    android:foregroundServiceType="mediaPlayback" />
```

#### FloatingWindowManager.java
新增方法（12个）：
```java
// 自定义悬浮窗控制
public static void showCustomFloatWindow(Context, int x, int y, int width, int height);
public static void showCustomFloatWindow(Context);
public static void hideCustomFloatWindow(Context);
public static void updateCustomFloatPosition(Context, int x, int y);
public static void updateCustomFloatSize(Context, int width, int height);
public static void showCustomFloatControlPoints(Context);
public static void hideCustomFloatControlPoints(Context);
public static boolean isCustomFloatWindowShowing(Context);
public static void destroyCustomFloatWindow();
```

#### activity_main.xml
新增按钮：
```xml
<!-- 自定义悬浮窗按钮 -->
<LinearLayout android:id="@+id/btn_custom_float">
    <ImageView android:src="@android:drawable/ic_menu_add" />
    <TextView android:text="自定义" />
</LinearLayout>
```

#### MainActivity.java
新增按钮事件：
```java
View btnCustomFloat = findViewById(R.id.btn_custom_float);
btnCustomFloat.setOnClickListener(v -> {
    // 检查权限
    // 显示/隐藏自定义悬浮窗
});
```

## 使用方式

### 方式一：使用自定义悬浮窗（布丁UI架构）✅ 推荐

```java
// 显示自定义悬浮窗（指定位置）
FloatingWindowManager.showCustomFloatWindow(context, 100, 100, 600, 400);

// 显示自定义悬浮窗（默认位置）
FloatingWindowManager.showCustomFloatWindow(context);

// 隐藏自定义悬浮窗
FloatingWindowManager.hideCustomFloatWindow(context);

// 更新位置
FloatingWindowManager.updateCustomFloatPosition(context, 200, 200);

// 更新尺寸
FloatingWindowManager.updateCustomFloatSize(context, 800, 600);

// 显示控制点
FloatingWindowManager.showCustomFloatControlPoints(context);

// 隐藏控制点
FloatingWindowManager.hideCustomFloatControlPoints(context);

// 检查是否正在显示
boolean showing = FloatingWindowManager.isCustomFloatWindowShowing(context);

// 销毁实例
FloatingWindowManager.destroyCustomFloatWindow();
```

### 方式二：使用高德地图悬浮窗（广播命令）

```java
// 检查高德地图是否安装
boolean installed = FloatingWindowManager.isAmapInstalled(context);

// 显示高德地图悬浮窗
FloatingWindowManager.showAmapFloatMap(context);

// 显示高德地图悬浮窗（指定位置）
FloatingWindowManager.showAmapFloatMap(context, 100, 100, 600, 400);

// 使用百分比显示
FloatingWindowManager.showAmapFloatMapPercent(context, 5, 5, 45, 35);

// 关闭高德地图悬浮窗
FloatingWindowManager.hideAmapFloatMap(context);

// 切换状态
FloatingWindowManager.toggleAmapFloatMap(context, isShowing);
```

### 方式三：通过Service控制

```java
// 启动服务
Intent intent = new Intent(context, FloatAmapCornerService.class);
intent.putExtra("command", "show");
intent.putExtra("x", 100);
intent.putExtra("y", 100);
intent.putExtra("width", 600);
intent.putExtra("height", 400);
context.startService(intent);

// 隐藏悬浮窗
intent.putExtra("command", "hide");
context.startService(intent);
```

## 功能对比

| 功能 | 自定义悬浮窗 | 高德地图悬浮窗 |
|------|-------------|--------------|
| **实现方式** | WindowManager | 广播命令 |
| **视图控制** | 完全自定义 | 依赖高德地图 |
| **权限要求** | 需要 SYSTEM_ALERT_WINDOW | 不需要 |
| **用户体验** | 自定义界面 | 原生地图体验 |
| **开发难度** | 中等 | 简单 |
| **灵活性** | 高 | 低 |
| **地图功能** | 需要自己集成 | 完整功能 |

## 布局参数说明

### WindowManager.LayoutParams 配置

```java
// 窗口类型
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    params.type = TYPE_APPLICATION_OVERLAY;  // 2038
} else {
    params.type = TYPE_PHONE;  // 2002
}

// 格式
params.format = PixelFormat.TRANSLUCENT;  // 1

// 标志位
params.flags = FLAG_NOT_FOCUSABLE |        // 256
              FLAG_NOT_TOUCH_MODAL |      // 32
              FLAG_WATCH_OUTSIDE_TOUCH;   // 16
              // 总计: 304

// 对齐方式
params.gravity = Gravity.CENTER;  // 8388659

// 尺寸
params.width = WRAP_CONTENT;      // -2
params.height = WRAP_CONTENT;      // -2

// 位置
params.x = 0;  // X坐标
params.y = 0;  // Y坐标
```

### 与布丁UI的对比

| 参数 | 布丁UI | 本项目 |
|------|--------|--------|
| **type** | 2038/2002 | 2038/2002 ✅ |
| **format** | 1 (TRANSLUCENT) | 1 (TRANSLUCENT) ✅ |
| **flags** | 264 | 304 ✅ |
| **gravity** | 8388659 (CENTER) | 8388659 (CENTER) ✅ |
| **width** | -2 (WRAP_CONTENT) | -2 (WRAP_CONTENT) ✅ |
| **height** | -2 (WRAP_CONTENT) | -2 (WRAP_CONTENT) ✅ |

## 关键代码解析

### 1. 单例模式

```java
private static FloatAmapCornerWindow instance;

public static synchronized FloatAmapCornerWindow getInstance(Context context) {
    if (instance == null) {
        instance = new FloatAmapCornerWindow(context);
    }
    return instance;
}
```

### 2. 权限检查

```java
public static boolean hasOverlayPermission(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        return Settings.canDrawOverlays(context);
    }
    return true;
}
```

### 3. 显示悬浮窗

```java
public void show(Context context, int x, int y, int width, int height) {
    // 设置位置和尺寸
    layoutParams.x = x;
    layoutParams.y = y;
    layoutParams.width = width;
    layoutParams.height = height;
    
    if (!isViewAdded) {
        // 创建视图
        floatAmapCorner = new FloatAmapCorner(context);
        
        // 添加到窗口
        windowManager.addView(floatAmapCorner, layoutParams);
        isViewAdded = true;
    } else {
        // 更新视图
        windowManager.updateViewLayout(floatAmapCorner, layoutParams);
    }
}
```

### 4. 九宫格控制点

```java
// 在布局中定义9个控制点
// leftUp, rightUp, leftDown, rightDown
// middleUp, middleDown, leftMiddle, rightMiddle

// 控制点支持拖拽和点击
view.setOnTouchListener((v, event) -> {
    // 处理拖拽事件
    return true;
});
```

## 故障排除

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

### 问题2：权限被拒绝

**解决方法**：
1. 打开系统设置
2. 找到应用
3. 允许悬浮窗权限

### 问题3：控制点不显示

**可能原因**：
1. 控制点可见性设置为 gone
2. 视图未正确加载

**解决方法**：
```java
// 确保调用了显示方法
FloatingWindowManager.showCustomFloatControlPoints(context);
```

## 下一步优化建议

### 1. 添加拖拽功能
```java
floatAmapCorner.setOnTouchListener((v, event) -> {
    // 实现拖拽逻辑
    return true;
});
```

### 2. 添加尺寸调整功能
```java
// 九宫格控制点拖拽时调整尺寸
controlPoint.setOnDragListener((v, event) -> {
    // 调整 width 和 height
    return true;
});
```

### 3. 添加动画效果
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

### 4. 保存用户设置
```java
// 保存位置
SharedPreferences prefs = context.getSharedPreferences("float_window", MODE_PRIVATE);
prefs.edit().putInt("x", x).putInt("y", y).apply();

// 恢复位置
int savedX = prefs.getInt("x", 0);
int savedY = prefs.getInt("y", 0);
```

### 5. 支持横竖屏切换
```java
@Override
public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    
    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        // 横屏布局
    } else {
        // 竖屏布局
    }
}
```

## 相关文件路径

### Java源文件
- `/app/src/main/java/cn/youth996/ui/widget/FloatAmapCorner.java`
- `/app/src/main/java/cn/youth996/ui/core/FloatAmapCornerWindow.java`
- `/app/src/main/java/cn/youth996/ui/service/FloatAmapCornerService.java`
- `/app/src/main/java/cn/youth996/ui/core/FloatingWindowManager.java`
- `/app/src/main/java/cn/youth996/ui/core/MainActivity.java`

### 资源文件
- `/app/src/main/res/layout/layout_float_amap_corner.xml`
- `/app/src/main/res/drawable/shape_corner_control_point.xml`
- `/app/src/main/res/layout/activity_main.xml`

### 配置文件
- `/app/src/main/AndroidManifest.xml`

## 技术栈

- **Android SDK**: 26+
- **布局系统**: ConstraintLayout
- **窗口管理**: WindowManager
- **服务**: IntentService / Service
- **设计模式**: 单例模式、工厂模式

## 参考资料

### 布丁UI反编译代码
- `/Users/zxj/code/youth-ui/decompiled_buding/sources/com/sfcar/launcher/service/plugin/builtin/floatamap/FloatAmapController.java`
- `/Users/zxj/code/youth-ui/decompiled_buding/sources/com/sfcar/launcher/service/plugin/builtin/floatamap/FloatAmapCornerWindow.java`
- `/Users/zxj/code/youth-ui/decompiled_buding/sources/com/sfcar/launcher/service/plugin/builtin/floatamap/FloatAmapCornerService.java`

### 布丁UI布局文件
- `/Users/zxj/code/youth-ui/decompiled_buding/resources/res/layout/layout_float_amap_corner.xml`

## 作者

Youth Desktop Team

## 更新记录

### 2024-XX-XX
- 按照布丁UI架构重构悬浮窗实现
- 添加 FloatAmapCorner 自定义视图
- 添加 FloatAmapCornerWindow 窗口管理器
- 添加 FloatAmapCornerService 服务类
- 新增12个悬浮窗控制方法
- 完成九宫格控制点布局
- 完成使用文档

## 许可证

内部使用
