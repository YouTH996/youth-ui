# 车机卡片功能开发说明

## 功能概述

在导航栏（状态栏）上方添加了一个水平滚动的卡片容器，包含以下5种车机卡片：

### 1. 导航卡片 (card_navigation.xml)
- 显示导航状态和目的地
- 显示距离、预计时间和剩余距离
- 点击可打开导航应用

### 2. 音乐卡片 (card_music.xml)
- 显示专辑封面、歌曲名和艺术家
- 播放进度条和播放控制按钮（上一首、播放/暂停、下一首）
- 模拟音乐播放进度

### 3. 车辆快捷控制卡片 (card_vehicle_control.xml)
- 解锁车辆
- 车窗控制
- 打开后备箱
- 寻车功能

### 4. 胎压卡片 (card_tire_pressure.xml)
- 显示四个轮胎的压力值（左前、右前、左后、右后）
- 显示胎温
- 根据压力值显示不同的颜色（正常-青色，低压-红色，高压-橙色）

### 5. 常用软件卡片 (card_frequent_apps.xml)
- 显示4个常用应用的图标和名称
- 点击可快速启动应用

## 技术实现

### 布局结构
- 在 `activity_main.xml` 中添加了 `HorizontalScrollView` 容器
- 使用 `LinearLayout` 作为卡片容器，采用水平方向排列
- 每张卡片宽度280dp，高度100dp

### 卡片管理
- 创建 `CardManager` 类统一管理所有卡片
- 提供公开方法更新卡片数据：
  - `updateNavigationInfo()` - 更新导航信息
  - `updateMusicInfo()` - 更新音乐信息
  - `updateTirePressure()` - 更新胎压数据
  - `updateFrequentApps()` - 更新常用应用

### 样式设计
- 卡片背景使用半透明深色（#CC1A1A2E）
- 圆角16dp，轻微阴影效果
- 强调色使用青色（#00D4FF）
- 支持暗黑科技风格主题

## 文件清单

### 新增布局文件
- `res/layout/card_navigation.xml`
- `res/layout/card_music.xml`
- `res/layout/card_vehicle_control.xml`
- `res/layout/card_tire_pressure.xml`
- `res/layout/card_frequent_apps.xml`

### 新增drawable资源
- `res/drawable/card_background.xml` - 卡片背景
- `res/drawable/card_control_button_background.xml` - 控制按钮背景
- `res/drawable/card_app_button_background.xml` - 应用按钮背景
- `res/drawable/album_cover_background.xml` - 专辑封面背景
- `res/drawable/ic_nav_default.xml` - 导航图标
- `res/drawable/ic_music_default.xml` - 音乐图标
- `res/drawable/ic_play.xml` - 播放图标
- `res/drawable/ic_pause.xml` - 暂停图标
- `res/drawable/ic_previous.xml` - 上一首图标
- `res/drawable/ic_next.xml` - 下一首图标
- `res/drawable/ic_unlock.xml` - 解锁图标
- `res/drawable/ic_window.xml` - 车窗图标
- `res/drawable/ic_trunk.xml` - 后备箱图标
- `res/drawable/ic_find_car.xml` - 寻车图标

### 新增颜色
- `res/values/colors.xml` 中添加了卡片系统颜色

### 新增类
- `CardManager.java` - 卡片管理器

### 修改文件
- `activity_main.xml` - 添加卡片容器
- `MainActivity.java` - 集成卡片管理器
- `colors.xml` - 添加卡片颜色

## 使用说明

卡片系统在应用启动时自动初始化和显示。用户可以通过左右滑动来浏览不同的卡片。点击卡片或卡片上的按钮可以触发相应的功能。

### 更新卡片数据示例

```java
// 更新导航信息
cardManager.updateNavigationInfo("公司", 12.5f, 25, 8.2f);

// 更新音乐信息
cardManager.updateMusicInfo("演员", "薛之谦", "webui/img/widget/music_cover.jpg");

// 更新胎压数据
cardManager.updateTirePressure(2.3f, 2.4f, 2.3f, 2.5f, 45.0f);

// 更新常用应用
List<AppInfo> frequentApps = new ArrayList<>();
// 添加应用信息...
cardManager.updateFrequentApps(frequentApps);
```

## 后续优化建议

1. 添加真实数据源，从系统服务获取导航、音乐、胎压等真实数据
2. 实现卡片的动态显示/隐藏，根据当前场景自动调整
3. 添加卡片动画效果，如滑动渐入、数据更新时的动画
4. 支持卡片定制化，用户可以选择显示哪些卡片
5. 优化触摸反馈，提升用户体验
6. 添加卡片之间的交互，如从音乐卡片直接跳转到音乐应用
