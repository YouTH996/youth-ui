package cn.youth996.ui.core;

import android.graphics.drawable.Drawable;
import android.content.Intent;

/**
 * 应用信息数据模型类
 *
 * 功能说明：
 * 1. 封装应用的基本信息
 * 2. 存储应用图标、名称和启动方式
 * 3. 支持应用的比较和哈希计算
 *
 * 数据字段：
 * - label: 应用显示名称
 * - packageName: 应用包名（唯一标识）
 * - className: 主Activity类名
 * - icon: 应用图标
 * - intent: 启动应用的Intent
 * - isSystemApp: 是否为系统应用
 *
 * 使用场景：
 * - 桌面图标显示
 * - 应用抽屉列表
 * - 最近应用列表
 * - 应用搜索和过滤
 *
 * @author Youth Desktop Team
 */
public class AppInfo {

    // ==================== 数据字段 ====================
    /** 应用显示名称 - 用于在UI上显示 */
    public String label;

    /** 应用包名 - Android应用程序的唯一标识符 */
    public String packageName;

    /** 主Activity类名 - 应用启动界面的完整类名 */
    public String className;

    /** 应用图标 - 可绘制的图标对象 */
    public Drawable icon;

    /** 启动Intent - 用于启动该应用的Intent对象 */
    public Intent intent;

    /** 系统应用标识 - 标记是否为系统预装应用 */
    public boolean isSystemApp = false;

    /**
     * 比较两个对象是否相等
     *
     * 比较规则：
     * 1. 如果是同一个对象引用,返回true
     * 2. 如果对象为null或类型不匹配,返回false
     * 3. 比较包名是否相等
     *
     * @param obj 要比较的对象
     * @return 如果相等返回true,否则返回false
     */
    @Override
    public boolean equals(Object obj) {
        // 如果是同一个对象,直接返回true
        if (this == obj) return true;

        // 如果对象为null或类型不匹配,返回false
        if (obj == null || getClass() != obj.getClass()) return false;

        // 转换为AppInfo类型进行比较
        AppInfo appInfo = (AppInfo) obj;

        // 比较包名,如果包名为null则需要特殊处理
        // 如果当前包名不为空,使用equals比较
        // 如果当前包名为空,则要求另一个也为null才相等
        return packageName != null ? packageName.equals(appInfo.packageName) : appInfo.packageName == null;
    }

    /**
     * 计算对象的哈希码
     *
     * 哈希码基于包名计算,
     * 用于HashMap、HashSet等数据结构
     *
     * 计算规则：
     * - 如果包名不为null,返回包名的哈希码
     * - 如果包名为null,返回0
     *
     * @return 对象的哈希码
     */
    @Override
    public int hashCode() {
        // 如果包名不为null,返回包名的哈希码
        // 如果包名为null,返回0
        return packageName != null ? packageName.hashCode() : 0;
    }
}
