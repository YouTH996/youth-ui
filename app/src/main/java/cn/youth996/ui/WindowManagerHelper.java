package cn.youth996.ui;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.LruCache;

import java.util.ArrayList;
import java.util.List;

/**
 * 窗口管理辅助类
 *
 * 功能说明：
 * 1. 管理最近运行的应用列表
 * 2. 提供应用缩略图缓存功能
 * 3. 支持窗口切换和任务管理
 * 4. 实现内存优化的图片缓存
 *
 * 技术特性：
 * - 使用LruCache实现高效的内存缓存
 * - 自动适配不同Android版本的API
 * - 支持应用图标的快速获取和转换
 *
 * 缓存策略：
 * - 使用应用包名作为缓存Key
 * - 缓存大小限制为最大可用内存的1/8
 * - 支持手动清除缓存
 *
 * @author Youth Desktop Team
 */
public class WindowManagerHelper {

    // ==================== 系统服务 ====================
    /** 上下文对象 - 用于访问系统服务和资源 */
    private Context context;

    /** 缩略图缓存 - LruCache用于缓存应用缩略图 */
    private LruCache<String, Bitmap> thumbnailCache;

    /** 活动管理器 - 用于获取运行中的应用信息 */
    private ActivityManager activityManager;

    /** 包管理器 - 用于获取应用信息和图标 */
    private PackageManager packageManager;

    /**
     * 构造函数
     *
     * 初始化窗口管理辅助类,
     * 获取系统服务,
     * 创建缩略图缓存
     *
     * @param context 上下文对象
     */
    public WindowManagerHelper(Context context) {
        this.context = context;

        // 获取活动管理器服务
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        // 获取包管理器服务
        this.packageManager = context.getPackageManager();

        // 计算最大可用内存（单位：KB）
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // 设置缓存大小为最大内存的1/8
        int cacheSize = maxMemory / 8;

        // 创建LruCache缓存实例
        thumbnailCache = new LruCache<String, Bitmap>(cacheSize) {
            /**
             * 计算单个缓存项的大小
             *
             * @param key 缓存键
             * @param bitmap 位图对象
             * @return 位图占用的内存大小（KB）
             */
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // 返回位图的字节数除以1024得到KB
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    /**
     * 获取最近运行的应用列表
     *
     * 查询系统中的最近任务,
     * 根据Android版本使用不同的API,
     * 返回包含应用包名和启动Intent的列表
     *
     * Android 5.0+适配：
     * - 使用getAppTasks()获取任务列表
     * - 从AppTask获取RecentTaskInfo
     *
     * Android 5.0以前：
     * - 使用getRecentTasks()直接获取
     *
     * @return 最近运行的应用信息列表
     */
    public List<RunningAppInfo> getRunningApps() {
        // 创建返回列表
        List<RunningAppInfo> runningApps = new ArrayList<>();

        // 判断Android版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Android 5.0及以上版本
            // 获取应用任务列表
            List<ActivityManager.AppTask> appTasks = activityManager.getAppTasks();

            // 遍历每个应用任务
            for (ActivityManager.AppTask task : appTasks) {
                // 获取任务信息
                ActivityManager.RecentTaskInfo taskInfo = task.getTaskInfo();

                // 检查任务信息是否有效
                if (taskInfo != null && taskInfo.baseIntent != null) {
                    // 创建应用信息对象
                    RunningAppInfo info = new RunningAppInfo();

                    // 获取包名
                    if (taskInfo.baseIntent.getComponent() != null) {
                        info.packageName = taskInfo.baseIntent.getComponent().getPackageName();
                    } else {
                        info.packageName = "";
                    }

                    // 设置启动Intent
                    info.intent = taskInfo.baseIntent;

                    // 添加到列表
                    runningApps.add(info);
                }
            }
        } else {
            // Android 5.0以前版本
            // 获取最近的20个任务
            List<ActivityManager.RecentTaskInfo> recentTasks = activityManager.getRecentTasks(20,
                ActivityManager.RECENT_IGNORE_UNAVAILABLE);

            // 遍历任务列表
            for (ActivityManager.RecentTaskInfo taskInfo : recentTasks) {
                // 检查任务信息有效性
                if (taskInfo.baseIntent != null && taskInfo.baseIntent.getComponent() != null) {
                    // 创建应用信息对象
                    RunningAppInfo info = new RunningAppInfo();
                    // 获取包名
                    info.packageName = taskInfo.baseIntent.getComponent().getPackageName();
                    // 设置启动Intent
                    info.intent = taskInfo.baseIntent;
                    // 添加到列表
                    runningApps.add(info);
                }
            }
        }

        // 返回运行应用列表
        return runningApps;
    }

    /**
     * 获取应用缩略图
     *
     * 首先检查缓存中是否存在,
     * 如果存在则直接返回,
     * 不存在则从包管理器获取图标并转换为位图,
     * 最后存入缓存
     *
     * 缓存机制：
     * - Key格式：包名 + "_thumbnail"
     * - 使用LruCache自动管理内存
     *
     * @param packageName 应用包名
     * @return 应用缩略图位图
     */
    public Bitmap getAppThumbnail(String packageName) {
        // 创建缓存键
        String key = packageName + "_thumbnail";

        // 尝试从缓存获取
        Bitmap cached = thumbnailCache.get(key);
        if (cached != null) {
            // 缓存命中,直接返回
            return cached;
        }

        try {
            // 获取应用图标
            Drawable icon = packageManager.getApplicationIcon(packageName);
            // 将Drawable转换为Bitmap
            Bitmap bitmap = drawableToBitmap(icon);
            // 存入缓存
            thumbnailCache.put(key, bitmap);
            // 返回位图
            return bitmap;
        } catch (PackageManager.NameNotFoundException e) {
            // 应用未找到,返回默认缩略图
            return createDefaultThumbnail();
        }
    }

    /**
     * 将Drawable转换为Bitmap
     *
     * 处理Drawable到Bitmap的转换,
     * 处理无效尺寸的特殊情况,
     * 使用Canvas绘制Drawable内容
     *
     * @param drawable 要转换的Drawable对象
     * @return 转换后的Bitmap对象
     */
    private Bitmap drawableToBitmap(Drawable drawable) {
        // 检查Drawable尺寸是否有效
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            // 返回一个1x1的透明位图
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        }

        // 使用Drawable的固有尺寸创建位图
        Bitmap bitmap = Bitmap.createBitmap(
            drawable.getIntrinsicWidth(),     // 宽度
            drawable.getIntrinsicHeight(),    // 高度
            Bitmap.Config.ARGB_8888           // 32位ARGB格式
        );

        // 创建画布
        Canvas canvas = new Canvas(bitmap);
        // 设置Drawable的绘制区域
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        // 在画布上绘制Drawable
        drawable.draw(canvas);

        // 返回绘制的位图
        return bitmap;
    }

    /**
     * 创建默认缩略图
     *
     * 当无法获取应用图标时调用,
     * 创建一个带有"APP"文字的灰色位图,
     * 作为默认占位图
     *
     * @return 默认缩略图
     */
    private Bitmap createDefaultThumbnail() {
        // 创建72x72的灰色位图
        Bitmap bitmap = Bitmap.createBitmap(72, 72, Bitmap.Config.ARGB_8888);
        // 创建画布
        Canvas canvas = new Canvas(bitmap);

        // 绘制灰色背景
        canvas.drawColor(Color.GRAY);

        // 创建画笔用于绘制文字
        Paint paint = new Paint();
        // 设置文字颜色为白色
        paint.setColor(Color.WHITE);
        // 设置文字大小为24
        paint.setTextSize(24);
        // 设置文字居中对齐
        paint.setTextAlign(Paint.Align.CENTER);

        // 绘制"APP"文字（居中位置）
        canvas.drawText("APP", 36, 45, paint);

        // 返回默认缩略图
        return bitmap;
    }

    /**
     * 清除缩略图缓存
     *
     * 调用LruCache的evictAll方法,
     * 清空所有缓存的缩略图,
     * 释放内存空间
     */
    public void clearThumbnailCache() {
        // 清空所有缓存
        thumbnailCache.evictAll();
    }

    /**
     * 运行中的应用信息内部类
     *
     * 用于封装最近运行应用的数据,
     * 包含包名、启动Intent和缩略图
     */
    public static class RunningAppInfo {
        /** 应用包名 */
        public String packageName;

        /** 启动Intent */
        public Intent intent;

        /** 应用缩略图 */
        public Bitmap thumbnail;
    }
}
