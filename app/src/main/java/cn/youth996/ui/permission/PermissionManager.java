package cn.youth996.ui.permission;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.youth996.ui.core.MainActivity;
import cn.youth996.ui.core.YouthAccessibilityService;

public class PermissionManager {
    private static final String TAG = "PermissionManager";
    private static final String PREFS_NAME = "launcher_prefs";
    private static final String KEY_OEM_STATUSBAR_ENABLED = "oem_statusbar_enabled";
    private static final String KEY_ACCESSIBILITY_RESTART_COUNT = "accessibility_restart_count";

    private Context context;
    private Handler handler;
    private AtomicBoolean isInitializing = new AtomicBoolean(false);
    private static PermissionManager instance;

    public interface PermissionCallback {
        void onPermissionGranted(String permission);
        void onPermissionDenied(String permission, String reason);
        void onAccessibilityServiceReady(boolean success);
        void onDesktopRestartComplete();
    }

    public static class AccessibilityHelper {
        public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
            AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (am == null) return false;

            List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_GENERIC
            );

            String serviceName = serviceClass.getName();

            for (AccessibilityServiceInfo service : enabledServices) {
                if (service.getId() != null && service.getId().contains(serviceName)) {
                    return true;
                }
            }
            return false;
        }

        public static boolean isServiceEnabled(Context context, Class<?> serviceClass) {
            AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
            return am != null && am.isEnabled();
        }

        public static boolean isAccessibilityEnabled(Context context, String serviceId) {
            try {
                String enabledServices = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                );

                if (enabledServices == null) return false;

                return enabledServices.contains(serviceId);
            } catch (Exception e) {
                Log.e(TAG, "检查无障碍服务状态失败: " + e.getMessage());
                return false;
            }
        }

        public static void enableAccessibilityService(Context context, Class<?> serviceClass, String packageName) {
            try {
                String serviceId = packageName + "/" + serviceClass.getName();

                Settings.Secure.putString(
                    context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    serviceId
                );

                Settings.Secure.putInt(
                    context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED,
                    1
                );

                Log.d(TAG, "无障碍服务已启用: " + serviceId);
            } catch (Exception e) {
                Log.e(TAG, "启用无障碍服务失败: " + e.getMessage());
            }
        }

        public static void disableAccessibilityService(Context context, String packageName) {
            try {
                Settings.Secure.putInt(
                    context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED,
                    0
                );

                Settings.Secure.putString(
                    context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    ""
                );

                Log.d(TAG, "无障碍服务已禁用");
            } catch (Exception e) {
                Log.e(TAG, "禁用无障碍服务失败: " + e.getMessage());
            }
        }
    }

    public PermissionManager(Context context) {
        this.context = context.getApplicationContext();
        this.handler = new Handler(Looper.getMainLooper());
        instance = this;
    }

    public static PermissionManager getInstance(Context context) {
        if (instance == null) {
            instance = new PermissionManager(context);
        }
        return instance;
    }

    /**
     * 检查悬浮窗权限并请求（参考BYD Launcher启动流程）
     * 在启动时调用此方法检查权限
     */
    public void checkOverlayPermissionAtStartup(Activity activity, int requestCode) {
        Log.d(TAG, "【启动权限检查】检查显示在其他应用的上层权限...");

        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "【启动权限检查】需要悬浮窗权限，正在打开设置页面");
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivityForResult(intent, requestCode);
                Log.i(TAG, "【启动权限检查】已打开悬浮窗权限设置页面");
            } catch (Exception e) {
                Log.e(TAG, "【启动权限检查】无法打开悬浮窗设置页面: " + e.getMessage());
            }
        } else {
            Log.i(TAG, "【启动权限检查】✓ 悬浮窗权限已授予");
            onOverlayPermissionGranted();
        }
    }

    /**
     * 悬浮窗权限授予后的回调处理
     */
    public void onOverlayPermissionResult(int resultCode, int successCode) {
        if (resultCode == successCode) {
            Log.i(TAG, "【悬浮窗权限】用户已授权");
            onOverlayPermissionGranted();
        } else {
            Log.w(TAG, "【悬浮窗权限】用户未授权或授权失败");
        }
    }

    private void onOverlayPermissionGranted() {
        Log.i(TAG, "【悬浮窗权限】权限授予完成");
        // 权限授予后的处理逻辑
        // 可以启动其他初始化流程
    }

    public void requestAllPermissions(PermissionCallback callback) {
        new Thread(() -> {
            try {
                Log.d(TAG, "开始请求权限（仅请求需要用户手动授权的权限）...");

                // 只请求需要用户手动授权的权限：显示在其他应用的上层
                checkAndRequestOverlayPermission(callback);

            } catch (Exception e) {
                Log.e(TAG, "请求权限时发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        }, "PermissionRequest").start();
    }

    private boolean checkAndRequestWriteSettings(PermissionCallback callback) {
        if (!Settings.System.canWrite(context)) {
            Log.d(TAG, "需要WRITE_SETTINGS权限，正在跳转设置...");

            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                Log.w(TAG, "无法打开WRITE_SETTINGS设置页面: " + e.getMessage());
                try {
                    Intent intent = new Intent("android.settings.MANAGE_WRITE_SETTINGS");
                    intent.setData(Uri.parse("package:" + context.getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Exception e2) {
                    Log.e(TAG, "备用方式也失败: " + e2.getMessage());
                }
            }

            if (callback != null) {
                callback.onPermissionDenied("WRITE_SETTINGS", "需要用户授权WRITE_SETTINGS权限");
            }
            return false;
        }

        if (callback != null) {
            callback.onPermissionGranted("WRITE_SETTINGS");
        }
        return true;
    }

    private boolean checkAndRequestOverlayPermission(PermissionCallback callback) {
        if (!Settings.canDrawOverlays(context)) {
            Log.d(TAG, "需要SYSTEM_ALERT_WINDOW权限，正在跳转设置...");
            openOverlayPermissionSettings();

            if (callback != null) {
                callback.onPermissionDenied("SYSTEM_ALERT_WINDOW", "需要用户授权SYSTEM_ALERT_WINDOW权限");
            }
            return false;
        }

        if (callback != null) {
            callback.onPermissionGranted("SYSTEM_ALERT_WINDOW");
        }
        return true;
    }

    private void openOverlayPermissionSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.i(TAG, "已打开'显示在其他应用的上层'权限设置页面");
            Log.i(TAG, "请在此页面开启'允许显示在其他应用的上层'开关");
        } catch (Exception e) {
            Log.e(TAG, "打开权限设置页面失败: " + e.getMessage());
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                Log.w(TAG, "已打开应用设置页面，请手动找到'显示在其他应用的上层'权限并开启");
            } catch (Exception e2) {
                Log.e(TAG, "打开应用设置也失败: " + e2.getMessage());
            }
        }
    }

    private boolean checkAndRequestAccessibilityPermission(PermissionCallback callback) {
        if (!AccessibilityHelper.isServiceEnabled(context, YouthAccessibilityService.class)) {
            Log.d(TAG, "需要无障碍服务权限，正在跳转设置...");

            try {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "无法打开无障碍设置: " + e.getMessage());
            }

            if (callback != null) {
                callback.onPermissionDenied("ACCESSIBILITY_SERVICE", "需要用户启用无障碍服务");
            }
            return false;
        }

        if (callback != null) {
            callback.onPermissionGranted("ACCESSIBILITY_SERVICE");
        }
        return true;
    }

    public void initializeAccessibilityService(PermissionCallback callback) {
        new Thread(() -> {
            try {
                String packageName = context.getPackageName();
                String serviceClassName = YouthAccessibilityService.class.getName();
                String serviceId = packageName + "/" + serviceClassName;

                Log.d(TAG, "初始化无障碍服务: " + serviceId);

                if (AccessibilityHelper.isServiceRunning(context, YouthAccessibilityService.class)) {
                    Log.d(TAG, "无障碍服务正在运行");
                    handler.post(() -> {
                        if (callback != null) {
                            callback.onAccessibilityServiceReady(true);
                        }
                    });
                    return;
                }

                enableAccessibilityWithRetry(serviceId, 3, callback);

            } catch (Exception e) {
                Log.e(TAG, "初始化无障碍服务失败: " + e.getMessage());
                e.printStackTrace();

                handler.post(() -> {
                    if (callback != null) {
                        callback.onAccessibilityServiceReady(false);
                    }
                });
            }
        }, "AccessibilityInit").start();
    }

    private void enableAccessibilityWithRetry(String serviceId, int maxRetries, PermissionCallback callback) {
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                Log.d(TAG, "尝试启用无障碍服务 (第 " + (retryCount + 1) + " 次)...");

                Settings.Secure.putString(
                    context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    serviceId
                );

                Settings.Secure.putInt(
                    context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED,
                    1
                );

                Thread.sleep(500);

                if (AccessibilityHelper.isServiceRunning(context, YouthAccessibilityService.class)) {
                    Log.d(TAG, "无障碍服务启用成功");
                    handler.post(() -> {
                        if (callback != null) {
                            callback.onAccessibilityServiceReady(true);
                        }
                    });
                    return;
                }

                retryCount++;

            } catch (Exception e) {
                Log.e(TAG, "启用无障碍服务时发生错误: " + e.getMessage());
                retryCount++;
            }
        }

        Log.w(TAG, "无障碍服务启用失败，进行重置操作...");

        try {
            Settings.Secure.putInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 0);
            Thread.sleep(300);
            Settings.Secure.putString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, serviceId);
            Thread.sleep(300);
            Settings.Secure.putInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 1);

            Thread.sleep(500);

            if (AccessibilityHelper.isServiceRunning(context, YouthAccessibilityService.class)) {
                Log.d(TAG, "重置后无障碍服务启用成功");
                handler.post(() -> {
                    if (callback != null) {
                        callback.onAccessibilityServiceReady(true);
                    }
                });
            } else {
                Log.e(TAG, "重置后无障碍服务仍然未启用");
                handler.post(() -> {
                    if (callback != null) {
                        callback.onAccessibilityServiceReady(false);
                    }
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "重置无障碍服务失败: " + e.getMessage());
            handler.post(() -> {
                if (callback != null) {
                    callback.onAccessibilityServiceReady(false);
                }
            });
        }
    }

    public void restartDesktop() {
        Log.d(TAG, "正在重启桌面...");

        try {
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);

            Log.d(TAG, "桌面已重启");

        } catch (Exception e) {
            Log.e(TAG, "重启桌面失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void forceRestartAccessibilityService() {
        new Thread(() -> {
            try {
                Log.d(TAG, "强制重启无障碍服务...");

                String packageName = context.getPackageName();
                String serviceClassName = YouthAccessibilityService.class.getName();
                String serviceId = packageName + "/" + serviceClassName;

                for (int round = 0; round < 3; round++) {
                    Log.d(TAG, "无障碍服务重启尝试 (第 " + (round + 1) + " 轮)...");

                    Settings.Secure.putInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 0);
                    Thread.sleep(500);

                    Settings.Secure.putString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, serviceId);
                    Thread.sleep(300);

                    Settings.Secure.putInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 1);

                    for (int i = 0; i < 8; i++) {
                        if (AccessibilityHelper.isServiceRunning(context, YouthAccessibilityService.class)) {
                            Log.d(TAG, "无障碍服务重启成功");
                            return;
                        }
                        Thread.sleep(500);
                    }

                    Log.w(TAG, "第 " + (round + 1) + " 轮重启失败，尝试下一轮...");
                }

                Log.e(TAG, "无障碍服务重启失败次数过多，可能需要手动检查");

            } catch (Exception e) {
                Log.e(TAG, "重启无障碍服务时发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        }, "AccessibilityRestart").start();
    }

    public boolean hasAllRequiredPermissions() {
        return Settings.System.canWrite(context) &&
               Settings.canDrawOverlays(context) &&
               AccessibilityHelper.isServiceEnabled(context, YouthAccessibilityService.class);
    }

    public void monitorAccessibilityService() {
        new Thread(() -> {
            while (true) {
                try {
                    if (!AccessibilityHelper.isServiceRunning(context, YouthAccessibilityService.class)) {
                        Log.w(TAG, "检测到无障碍服务已停止，正在尝试重启...");

                        String packageName = context.getPackageName();
                        String serviceId = packageName + "/" + YouthAccessibilityService.class.getName();

                        AccessibilityHelper.enableAccessibilityService(context, YouthAccessibilityService.class, packageName);

                        Thread.sleep(1000);

                        if (!AccessibilityHelper.isServiceRunning(context, YouthAccessibilityService.class)) {
                            forceRestartAccessibilityService();
                        }
                    }

                    Thread.sleep(5000);

                } catch (Exception e) {
                    Log.e(TAG, "监控无障碍服务时发生错误: " + e.getMessage());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, "AccessibilityMonitor").start();
    }

    public void setupBootCompleted() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isOemStatusbarEnabled = prefs.getBoolean(KEY_OEM_STATUSBAR_ENABLED, false);

        int statusBarDisableFlag = isOemStatusbarEnabled ? 0 : 0x00980200;

        try {
            Object statusBarService = context.getSystemService("statusbar");
            if (statusBarService != null) {
                java.lang.reflect.Method disableMethod = statusBarService.getClass().getMethod("disable", int.class);
                disableMethod.invoke(statusBarService, statusBarDisableFlag);

                if (!isOemStatusbarEnabled) {
                    try {
                        java.lang.reflect.Method disable2Method = statusBarService.getClass().getMethod("disable2", int.class);
                        disable2Method.invoke(statusBarService, 5);
                    } catch (Exception e) {
                        Log.d(TAG, "disable2方法不可用，跳过");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "设置状态栏失败: " + e.getMessage());
        }

        if (!isOemStatusbarEnabled) {
            try {
                Object statusBarService = context.getSystemService("statusbar");
                if (statusBarService != null) {
                    try {
                        java.lang.reflect.Method collapseMethod = statusBarService.getClass().getMethod("collapsePanels");
                        collapseMethod.invoke(statusBarService);
                    } catch (NoSuchMethodException e) {
                        java.lang.reflect.Method collapseMethod = statusBarService.getClass().getMethod("collapse");
                        collapseMethod.invoke(statusBarService);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "收起状态栏失败: " + e.getMessage());
            }
        }

        if (MainActivity.getInstance() == null) {
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }

        if (!AccessibilityHelper.isServiceRunning(context, YouthAccessibilityService.class)) {
            PermissionManager.getInstance(context).initializeAccessibilityService(null);
        }
    }
}
