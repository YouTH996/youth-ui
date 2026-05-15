package cn.youth996.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 主题管理类
 *
 * 功能说明：
 * 1. 管理桌面主题配置
 * 2. 存储和读取壁纸设置
 * 3. 管理主题模式和强调色
 * 4. 提供预设主题方案
 * 5. 管理图标样式偏好
 *
 * 数据持久化：
 * - 使用SharedPreferences存储主题配置
 * - 壁纸使用URI存储
 * - 主题颜色使用整数值存储
 *
 * 预设主题：
 * - 默认蓝色
 * - 清新绿色
 * - 热情红色
 * - 优雅紫色
 * - 活力橙色
 * - 科技青色
 *
 * @author Youth Desktop Team
 */
public class ThemeManager {

    // ==================== 存储键常量 ====================
    /** 存储文件名 - SharedPreferences文件名 */
    private static final String PREFS_NAME = "theme_prefs";

    /** 壁纸URI键 - 存储壁纸图片的URI */
    private static final String KEY_WALLPAPER_URI = "wallpaper_uri";

    /** 主题模式键 - 存储当前主题模式 */
    private static final String KEY_THEME_MODE = "theme_mode";

    /** 强调色键 - 存储主题强调色 */
    private static final String KEY_ACCENT_COLOR = "accent_color";

    /** 图标样式键 - 存储图标样式偏好 */
    private static final String KEY_ICON_STYLE = "icon_style";

    // ==================== 成员变量 ====================
    /** 上下文对象 - 用于访问SharedPreferences */
    private Context context;

    /** 偏好设置 - SharedPreferences实例 */
    private SharedPreferences prefs;

    /**
     * 构造函数
     *
     * 初始化主题管理器,
     * 创建SharedPreferences实例
     *
     * @param context 上下文对象
     */
    public ThemeManager(Context context) {
        this.context = context;
        // 使用私有模式创建SharedPreferences
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 设置壁纸
     *
     * 将壁纸URI保存到SharedPreferences,
     * 使用Uri的toString()方法转换为字符串存储
     *
     * @param uri 壁纸图片的URI
     */
    public void setWallpaper(Uri uri) {
        // 将URI转换为字符串并存储
        prefs.edit().putString(KEY_WALLPAPER_URI, uri.toString()).apply();
    }

    /**
     * 获取壁纸URI
     *
     * 从SharedPreferences读取壁纸URI字符串,
     * 转换为Uri对象返回
     *
     * @return 壁纸URI,如果未设置则返回null
     */
    public Uri getWallpaperUri() {
        // 读取URI字符串
        String uriString = prefs.getString(KEY_WALLPAPER_URI, null);
        // 如果不为空则转换为Uri返回
        return uriString != null ? Uri.parse(uriString) : null;
    }

    /**
     * 获取壁纸位图
     *
     * 获取壁纸URI后,
     * 打开输入流读取图片,
     * 解码为Bitmap对象
     *
     * @return 壁纸位图,如果获取失败返回null
     */
    public Bitmap getWallpaperBitmap() {
        // 获取壁纸URI
        Uri uri = getWallpaperUri();
        // 如果URI为空则返回null
        if (uri == null) {
            return null;
        }

        try {
            // 打开内容解析器获取输入流
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            // 解码为Bitmap并返回
            return BitmapFactory.decodeStream(inputStream);
        } catch (FileNotFoundException e) {
            // 文件未找到,返回null
            return null;
        }
    }

    /**
     * 设置主题模式
     *
     * 保存主题模式到SharedPreferences,
     * 模式包括：浅色模式、深色模式、自动模式
     *
     * @param mode 主题模式（0-浅色,1-深色,2-自动）
     */
    public void setThemeMode(int mode) {
        // 保存主题模式
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
    }

    /**
     * 获取主题模式
     *
     * 从SharedPreferences读取主题模式,
     * 默认为浅色模式
     *
     * @return 当前主题模式
     */
    public int getThemeMode() {
        // 读取主题模式,默认为浅色模式
        return prefs.getInt(KEY_THEME_MODE, ThemeMode.LIGHT);
    }

    /**
     * 设置强调色
     *
     * 保存主题强调色到SharedPreferences,
     * 强调色用于状态栏、按钮等主要强调元素
     *
     * @param color ARGB格式的颜色值
     */
    public void setAccentColor(int color) {
        // 保存强调色
        prefs.edit().putInt(KEY_ACCENT_COLOR, color).apply();
    }

    /**
     * 获取强调色
     *
     * 从SharedPreferences读取强调色,
     * 默认为蓝色（#2196F3）
     *
     * @return 当前强调色（ARGB格式）
     */
    public int getAccentColor() {
        // 读取强调色,默认为蓝色
        return prefs.getInt(KEY_ACCENT_COLOR, 0xFF2196F3);
    }

    /**
     * 设置图标样式
     *
     * 保存图标样式偏好,
     * 样式包括：圆角、方形、自适应
     *
     * @param style 图标样式（0-圆角,1-方形,2-自适应）
     */
    public void setIconStyle(int style) {
        // 保存图标样式
        prefs.edit().putInt(KEY_ICON_STYLE, style).apply();
    }

    /**
     * 获取图标样式
     *
     * 从SharedPreferences读取图标样式,
     * 默认为圆角样式
     *
     * @return 当前图标样式
     */
    public int getIconStyle() {
        // 读取图标样式,默认为圆角
        return prefs.getInt(KEY_ICON_STYLE, IconStyle.ROUND);
    }

    /**
     * 获取预设主题列表
     *
     * 返回系统预设的6种主题方案,
     * 每种主题包含名称、主色和背景色
     *
     * 预设主题：
     * 1. 默认蓝色 - #2196F3
     * 2. 清新绿色 - #4CAF50
     * 3. 热情红色 - #F44336
     * 4. 优雅紫色 - #9C27B0
     * 5. 活力橙色 - #FF9800
     * 6. 科技青色 - #00BCD4
     *
     * @return 预设主题列表
     */
    public List<ThemePreset> getPresetThemes() {
        // 创建预设主题列表
        List<ThemePreset> presets = new ArrayList<>();

        // 添加默认蓝色主题
        presets.add(new ThemePreset("默认蓝色", 0xFF2196F3, 0xFFE3F2FD));
        // 添加清新绿色主题
        presets.add(new ThemePreset("清新绿色", 0xFF4CAF50, 0xFFE8F5E9));
        // 添加热情红色主题
        presets.add(new ThemePreset("热情红色", 0xFFF44336, 0xFFFFEBEE));
        // 添加优雅紫色主题
        presets.add(new ThemePreset("优雅紫色", 0xFF9C27B0, 0xFFF3E5F5));
        // 添加活力橙色主题
        presets.add(new ThemePreset("活力橙色", 0xFFFF9800, 0xFFFFF3E0));
        // 添加科技青色主题
        presets.add(new ThemePreset("科技青色", 0xFF00BCD4, 0xFFE0F7FA));

        // 返回预设列表
        return presets;
    }

    /**
     * 应用预设主题
     *
     * 将预设主题的颜色应用到当前主题
     *
     * @param preset 要应用的预设主题
     */
    public void applyPreset(ThemePreset preset) {
        // 设置预设的主色
        setAccentColor(preset.primaryColor);
    }

    /**
     * 预设主题数据模型
     *
     * 封装预设主题的配置信息,
     * 包括主题名称、主色和背景色
     */
    public static class ThemePreset {
        /** 主题名称 */
        public String name;

        /** 主色 - 用于状态栏、按钮等 */
        public int primaryColor;

        /** 背景色 - 用于背景和卡片 */
        public int backgroundColor;

        /**
         * 构造函数
         *
         * 创建预设主题实例
         *
         * @param name 主题名称
         * @param primaryColor 主色（ARGB格式）
         * @param backgroundColor 背景色（ARGB格式）
         */
        public ThemePreset(String name, int primaryColor, int backgroundColor) {
            this.name = name;
            this.primaryColor = primaryColor;
            this.backgroundColor = backgroundColor;
        }
    }

    /**
     * 主题模式常量
     *
     * 定义三种主题模式
     */
    public static class ThemeMode {
        /** 浅色模式 */
        public static final int LIGHT = 0;

        /** 深色模式 */
        public static final int DARK = 1;

        /** 自动模式 - 根据系统设置自动切换 */
        public static final int AUTO = 2;
    }

    /**
     * 图标样式常量
     *
     * 定义三种图标样式
     */
    public static class IconStyle {
        /** 圆角图标 */
        public static final int ROUND = 0;

        /** 方形图标 */
        public static final int SQUARE = 1;

        /** 自适应图标 - 根据系统配置自动选择 */
        public static final int ADAPTIVE = 2;
    }
}
