# Youth桌面 - Android平板桌面UI系统

基于Java开发的Android平板桌面系统，参考叁迪UI桌面的现代化设计风格。

## 功能特性

### 1. 桌面主界面
- 网格布局显示应用图标
- 支持拖拽排列图标
- 长按图标显示选项菜单
- 响应式布局适配不同屏幕尺寸

### 2. 状态栏
- 实时显示当前时间
- 显示当前日期（支持中文格式）
- 毛玻璃视觉效果

### 3. 任务栏
- 主页按钮快速返回桌面
- 应用抽屉按钮
- 平滑的动画过渡效果

### 4. 应用抽屉
- 网格布局展示所有已安装应用
- 搜索功能快速查找应用
- 按应用名称排序
- 流畅的滑入/滑出动画

### 5. 主题系统
- 多种预设主题颜色
- 主题颜色自定义
- 支持壁纸设置
- 明暗主题切换

### 6. 窗口管理
- 最近应用列表管理
- 应用缩略图缓存
- 快速切换应用

### 7. 小部件支持
- 时钟小部件
- 电池状态小部件
- 天气信息小部件
- 快捷设置面板

## 技术栈

- **语言**: Java
- **最低SDK**: Android 7.0 (API 26)
- **目标SDK**: Android 14 (API 34)
- **UI框架**: Android Views + Material Design
- **动画**: Android Property Animations
- **依赖管理**: Gradle

## 项目结构

```
youth-ui/
├── app/
│   ├── src/main/
│   │   ├── java/com/youth/desktop/
│   │   │   ├── MainActivity.java          # 主桌面Activity
│   │   │   ├── AppDrawerActivity.java     # 应用抽屉Activity
│   │   │   ├── AppDrawerAdapter.java      # 应用列表适配器
│   │   │   ├── AppInfo.java               # 应用信息数据类
│   │   │   ├── WindowManagerHelper.java   # 窗口管理辅助类
│   │   │   ├── ThemeManager.java          # 主题管理类
│   │   │   └── WidgetManager.java         # 小部件管理类
│   │   ├── res/
│   │   │   ├── layout/                    # 布局文件
│   │   │   ├── drawable/                  # 图形资源
│   │   │   ├── anim/                     # 动画文件
│   │   │   ├── values/                    # 字符串、颜色、尺寸等资源
│   │   │   └── mipmap/                    # 应用图标
│   │   └── AndroidManifest.xml            # 应用清单文件
│   └── build.gradle                       # 应用模块构建配置
├── build.gradle                           # 项目根目录构建配置
├── settings.gradle                        # Gradle设置
└── gradle.properties                      # Gradle属性配置
```

## 构建说明

### 环境要求
- Android Studio Hedgehog 或更高版本
- JDK 17 或更高版本
- Android SDK Platform 34

### 构建步骤

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd youth-ui
   ```

2. **同步Gradle**
   在Android Studio中点击 "Sync Project with Gradle Files"

3. **构建调试版本**
   ```bash
   ./gradlew assembleDebug
   ```

4. **构建发布版本**
   ```bash
   ./gradlew assembleRelease
   ```

### 安装测试
1. 连接Android设备或启动模拟器
2. 运行 `./gradlew installDebug`
3. 将应用设置为默认桌面（首次启动时会提示）

## 使用说明

### 桌面操作
- **点击图标**: 启动应用
- **长按图标**: 显示应用选项菜单
- **点击主页按钮**: 返回桌面
- **点击应用抽屉按钮**: 打开应用列表

### 应用抽屉
- **浏览应用**: 上下左右滑动查看应用
- **搜索应用**: 点击搜索框输入关键词
- **启动应用**: 点击应用图标
- **关闭抽屉**: 点击关闭按钮或向下滑动

### 主题定制
应用内置主题管理系统，支持：
- 6种预设主题
- 自定义主题颜色
- 壁纸设置

## 设计参考

本项目参考了以下设计理念：

1. **叁迪UI桌面**
   - 现代化界面设计
   - 车机桌面风格
   - 主题定制功能

2. **Material Design 3**
   - 色彩系统
   - 阴影和层次
   - 动画过渡

3. **Windows 11**
   - 圆角设计
   - 毛玻璃效果
   - 居中任务栏

## 开发计划

- [x] 基础桌面框架
- [x] 状态栏和任务栏
- [x] 应用抽屉
- [x] 窗口管理
- [x] 主题系统
- [ ] 小部件系统
- [ ] 文件管理器集成
- [ ] 多用户支持
- [ ] 手势操作支持

## 贡献指南

欢迎提交Issue和Pull Request！

## 许可证

本项目仅供学习和研究使用。

## 联系方式

如有问题，请提交Issue。
