package cn.youth996.ui.receiver;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import cn.youth996.ui.core.MainActivity;
import cn.youth996.ui.core.YouthAccessibilityService;
import cn.youth996.ui.permission.PermissionManager;
import cn.youth996.ui.service.MediaNotificationListenerService;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    private static final String PREFS_NAME = "launcher_prefs";
    private static final String KEY_OEM_STATUSBAR_ENABLED = "oem_statusbar_enabled";
    private static final String KEY_FIRST_BOOT = "first_boot";

    private static final AtomicBoolean isInitializing = new AtomicBoolean(false);

    public static void requestNotificationListenerSetup(Context context) {
        BootReceiver receiver = new BootReceiver();
        receiver.initializeNotificationListenerService(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
            Log.d(TAG, "接收到启动完成广播");
            handleBootCompleted(context);
        } else if ("android.intent.action.SCREEN_ON".equals(action)) {
            Log.d(TAG, "接收到屏幕亮起广播");
            handleScreenOn(context);
        }
    }

    private void handleBootCompleted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isFirstBoot = prefs.getBoolean(KEY_FIRST_BOOT, true);

        if (isFirstBoot) {
            Log.d(TAG, "首次启动，进行初始化设置...");
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_FIRST_BOOT, false);
            editor.apply();
        }

        setupStatusBar(context);
        launchMainActivity(context);

        if (!PermissionManager.AccessibilityHelper.isServiceRunning(context, YouthAccessibilityService.class)) {
            Log.d(TAG, "无障碍服务未运行，启动初始化...");
            initializeAccessibilityService(context);
        } else {
            Log.d(TAG, "无障碍服务已在运行");
        }

        initializeNotificationListenerService(context);
    }

    private void handleScreenOn(Context context) {
        Log.d(TAG, "屏幕亮起，检查无障碍服务状态...");

        if (!PermissionManager.AccessibilityHelper.isServiceRunning(context, YouthAccessibilityService.class)) {
            Log.w(TAG, "无障碍服务未运行，正在尝试启动...");
            initializeAccessibilityService(context);
        }

        if (MainActivity.getInstance() == null) {
            Log.d(TAG, "MainActivity未运行，正在启动...");
            launchMainActivity(context);
        }

        initializeNotificationListenerService(context);
    }

    private void setupStatusBar(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isOemStatusbarEnabled = prefs.getBoolean(KEY_OEM_STATUSBAR_ENABLED, false);

        int statusBarDisableFlag = isOemStatusbarEnabled ? 0 : 0x00980200;

        try {
            @SuppressLint("WrongConstant")
            Object statusBarService = context.getSystemService("statusbar");
            if (statusBarService != null) {
                java.lang.reflect.Method disableMethod = statusBarService.getClass().getMethod("disable", int.class);
                disableMethod.invoke(statusBarService, statusBarDisableFlag);
                Log.d(TAG, "状态栏标志设置成功");

                if (!isOemStatusbarEnabled) {
                    try {
                        java.lang.reflect.Method disable2Method = statusBarService.getClass().getMethod("disable2", int.class);
                        disable2Method.invoke(statusBarService, 5);
                        Log.d(TAG, "状态栏标志2设置成功");
                    } catch (NoSuchMethodException e) {
                        Log.d(TAG, "disable2方法不可用，跳过");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "设置状态栏失败: " + e.getMessage());
        }

        if (!isOemStatusbarEnabled) {
            try {
                @SuppressLint("WrongConstant")
                Object statusBarService = context.getSystemService("statusbar");
                if (statusBarService != null) {
                    try {
                        java.lang.reflect.Method collapseMethod = statusBarService.getClass().getMethod("collapsePanels");
                        collapseMethod.invoke(statusBarService);
                        Log.d(TAG, "状态栏已收起");
                    } catch (NoSuchMethodException e) {
                        java.lang.reflect.Method collapseMethod = statusBarService.getClass().getMethod("collapse");
                        collapseMethod.invoke(statusBarService);
                        Log.d(TAG, "状态栏已收起");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "收起状态栏失败: " + e.getMessage());
            }
        }
    }

    private void launchMainActivity(Context context) {
        try {
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
            Log.d(TAG, "MainActivity已启动");
        } catch (Exception e) {
            Log.e(TAG, "启动MainActivity失败: " + e.getMessage());
        }
    }

    private void initializeAccessibilityService(Context context) {
        if (isInitializing.compareAndSet(false, true)) {
            new Thread(() -> {
                try {
                    Log.d(TAG, "开始初始化无障碍服务...");

                    String packageName = context.getPackageName();
                    String serviceClassName = YouthAccessibilityService.class.getName();
                    String serviceId = packageName + "/" + serviceClassName;

                    if (PermissionManager.AccessibilityHelper.isServiceRunning(context, YouthAccessibilityService.class)) {
                        Log.d(TAG, "无障碍服务已在运行");
                        isInitializing.set(false);
                        return;
                    }

                    boolean success = enableAccessibilityService(context, serviceId);

                    if (success) {
                        Log.d(TAG, "无障碍服务启用成功");
                    } else {
                        Log.w(TAG, "无障碍服务启用失败，尝试重置...");
                        resetAccessibilityService(context, serviceId);
                    }

                    Thread.sleep(1000);

                    if (!PermissionManager.AccessibilityHelper.isServiceRunning(context, YouthAccessibilityService.class)) {
                        Log.w(TAG, "无障碍服务仍未运行，尝试强制重启...");
                        forceRestartAccessibilityService(context);
                    }

                } catch (Exception e) {
                    Log.e(TAG, "初始化无障碍服务时发生错误: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    isInitializing.set(false);
                }
            }, "AccessibilityInit").start();
        } else {
            Log.d(TAG, "无障碍服务正在初始化中");
        }
    }

    private boolean enableAccessibilityService(Context context, String serviceId) {
        try {
            Log.d(TAG, "启用无障碍服务: " + serviceId);

            android.provider.Settings.Secure.putString(
                context.getContentResolver(),
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                serviceId
            );

            android.provider.Settings.Secure.putInt(
                context.getContentResolver(),
                android.provider.Settings.Secure.ACCESSIBILITY_ENABLED,
                1
            );

            Thread.sleep(500);

            return PermissionManager.AccessibilityHelper.isServiceRunning(context, YouthAccessibilityService.class);

        } catch (Exception e) {
            Log.e(TAG, "启用无障碍服务失败: " + e.getMessage());
            return false;
        }
    }

    private void resetAccessibilityService(Context context, String serviceId) {
        try {
            Log.d(TAG, "重置无障碍服务...");

            for (int round = 0; round < 3; round++) {
                Log.d(TAG, "重置尝试第 " + (round + 1) + " 轮");

                android.provider.Settings.Secure.putInt(
                    context.getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED,
                    0
                );
                Thread.sleep(300);

                android.provider.Settings.Secure.putString(
                    context.getContentResolver(),
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    serviceId
                );
                Thread.sleep(300);

                android.provider.Settings.Secure.putInt(
                    context.getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED,
                    1
                );

                Thread.sleep(500);

                if (PermissionManager.AccessibilityHelper.isServiceRunning(context, YouthAccessibilityService.class)) {
                    Log.d(TAG, "重置后无障碍服务启动成功");
                    return;
                }
            }

            Log.w(TAG, "重置无障碍服务失败");

        } catch (Exception e) {
            Log.e(TAG, "重置无障碍服务失败: " + e.getMessage());
        }
    }

    private void forceRestartAccessibilityService(Context context) {
        try {
            Log.d(TAG, "强制重启无障碍服务...");

            String packageName = context.getPackageName();
            String serviceId = packageName + "/" + YouthAccessibilityService.class.getName();

            for (int attempt = 0; attempt < 6; attempt++) {
                Log.d(TAG, "强制重启尝试 " + (attempt + 1) + "/6");

                android.provider.Settings.Secure.putInt(
                    context.getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED,
                    0
                );
                Thread.sleep(500);

                android.provider.Settings.Secure.putString(
                    context.getContentResolver(),
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    serviceId
                );
                Thread.sleep(300);

                android.provider.Settings.Secure.putInt(
                    context.getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED,
                    1
                );

                for (int i = 0; i < 8; i++) {
                    Thread.sleep(500);
                    if (PermissionManager.AccessibilityHelper.isServiceRunning(context, YouthAccessibilityService.class)) {
                        Log.d(TAG, "强制重启成功");
                        return;
                    }
                }

                Log.w(TAG, "第 " + (attempt + 1) + " 次尝试失败");
            }

            Log.e(TAG, "无障碍服务强制重启失败次数过多，可能需要手动检查");

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    Log.d(TAG, "已打开无障碍服务设置页面");
                } catch (Exception e) {
                    Log.e(TAG, "无法打开无障碍设置: " + e.getMessage());
                }
            }, 2000);

        } catch (Exception e) {
            Log.e(TAG, "强制重启失败: " + e.getMessage());
        }
    }

    private void initializeNotificationListenerService(Context context) {
        new Thread(() -> {
            try {
                Log.i(TAG, "═══════════════════════════════════════════════════════════");
                Log.i(TAG, "【通知监听器】开始初始化通知监听器服务...");
                Log.i(TAG, "═══════════════════════════════════════════════════════════");

                String packageName = context.getPackageName();
                String serviceClassName = "cn.youth996.ui.service.MediaNotificationListenerService";
                String listenerService = packageName + "/" + serviceClassName;

                boolean isEnabled = checkNotificationListenerEnabled(context, listenerService);

                if (!isEnabled && MediaNotificationListenerService.getInstance() != null) {
                    Log.i(TAG, "【通知监听器】✓ MediaNotificationListenerService 正在运行，假设权限已授予");
                    isEnabled = true;
                }

                if (isEnabled) {
                    Log.i(TAG, "【通知监听器】✓ 通知监听器已启用");
                    return;
                }

                Log.w(TAG, "【通知监听器】通知监听器未启用，开始多轮重试机制...");

                boolean success = setupNotificationListenerWithRetry(context, listenerService);

                if (success) {
                    Log.i(TAG, "【通知监听器】✓ 通知监听器设置成功");
                } else {
                    if (MediaNotificationListenerService.getInstance() != null) {
                        Log.i(TAG, "【通知监听器】✓ MediaNotificationListenerService 正在运行，权限应该已授予");
                    } else {
                        Log.w(TAG, "【通知监听器】⚠️ ADB权限已设置，但服务未启动");
                        Log.w(TAG, "【通知监听器】⚠️ 这可能是比亚迪系统限制，需要用户手动授权");
                        
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            try {
                                Log.i(TAG, "【通知监听器】正在打开通知访问权限设置页面...");
                                Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);
                                Log.i(TAG, "【通知监听器】✓ 已打开设置页面，请在列表中启用\"轻语桌面\"");
                            } catch (Exception e) {
                                Log.e(TAG, "【通知监听器】无法打开设置页面: " + e.getMessage());
                                Log.e(TAG, "【通知监听器】请手动打开: 设置 > 应用 > 轻语桌面 > 通知访问权限");
                            }
                        }, 2000);
                    }
                }

                Log.i(TAG, "═══════════════════════════════════════════════════════════");

            } catch (Exception e) {
                Log.e(TAG, "【通知监听器】初始化失败: " + e.getMessage());
                e.printStackTrace();
            }
        }, "NotificationListenerInit").start();
    }

    private boolean checkNotificationListenerEnabled(Context context, String listenerService) {
        try {
            Log.d(TAG, "【通知监听器】通过服务实例检查权限状态...");

            if (MediaNotificationListenerService.getInstance() != null) {
                Log.i(TAG, "【通知监听器】✓ 检查结果: MediaNotificationListenerService 正在运行");
                return true;
            }

            Log.w(TAG, "【通知监听器】✗ 检查结果: MediaNotificationListenerService 未运行");
            Log.w(TAG, "【通知监听器】说明: ADB已设置权限，但服务未启动（需要重启应用）");
            return false;

        } catch (Exception e) {
            Log.e(TAG, "【通知监听器】检查失败: " + e.getMessage());
            return false;
        }
    }

    private boolean setupNotificationListenerWithRetry(Context context, String listenerService) {
        try {
            Log.d(TAG, "【通知监听器】开始多轮重试机制...");

            for (int round = 0; round < 3; round++) {
                Log.d(TAG, "═══════════════════════════════════════════════════════════");
                Log.d(TAG, "【通知监听器】第 " + (round + 1) + " 轮尝试");

                boolean success = false;

                switch (round) {
                    case 0:
                        success = setupNotificationListenerRound1(context, listenerService);
                        break;
                    case 1:
                        success = setupNotificationListenerRound2(context, listenerService);
                        break;
                    case 2:
                        success = setupNotificationListenerRound3(context, listenerService);
                        break;
                }

                if (success) {
                    Log.i(TAG, "【通知监听器】✓ 第 " + (round + 1) + " 轮成功！");
                    return true;
                }

                Log.w(TAG, "【通知监听器】第 " + (round + 1) + " 轮失败，尝试下一轮...");
            }

            return false;

        } catch (Exception e) {
            Log.e(TAG, "【通知监听器】多轮重试失败: " + e.getMessage());
            return false;
        }
    }

    private boolean setupNotificationListenerRound1(Context context, String listenerService) {
        try {
            Log.d(TAG, "【通知监听器-Round1】使用 cmd notification allow_listener + settings 双重设置");
            
            // 方法1：使用 cmd notification allow_listener（比亚迪车机关键命令）
            try {
                Process process = Runtime.getRuntime().exec("cmd notification allow_listener " + listenerService);
                process.waitFor();
                Log.d(TAG, "【通知监听器-Round1】cmd notification allow_listener 执行完成");
            } catch (Exception e) {
                Log.w(TAG, "【通知监听器-Round1】cmd notification 命令失败: " + e.getMessage());
            }
            
            // 方法2：设置 enabled_notification_listeners
            String currentListeners = android.provider.Settings.Secure.getString(
                context.getContentResolver(), 
                "enabled_notification_listeners"
            );
            
            String newListeners;
            if (currentListeners == null || currentListeners.isEmpty()) {
                newListeners = listenerService;
            } else if (currentListeners.contains(listenerService)) {
                newListeners = currentListeners;
            } else {
                newListeners = currentListeners + ":" + listenerService;
            }
            
            android.provider.Settings.Secure.putString(
                context.getContentResolver(),
                "enabled_notification_listeners",
                newListeners
            );
            
            Log.d(TAG, "【通知监听器-Round1】enabled_notification_listeners 设置为: " + newListeners);

            Thread.sleep(500);

            for (int i = 0; i < 6; i++) {
                Thread.sleep(500);
                if (checkNotificationListenerEnabled(context, listenerService)) {
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            Log.e(TAG, "【通知监听器-Round1】失败: " + e.getMessage());
            return false;
        }
    }

    private boolean setupNotificationListenerRound2(Context context, String listenerService) {
        try {
            Log.d(TAG, "【通知监听器-Round2】禁用后重新启用");

            android.provider.Settings.Secure.putString(
                context.getContentResolver(),
                "enabled_notification_listeners",
                ""
            );
            Thread.sleep(300);

            android.provider.Settings.Secure.putString(
                context.getContentResolver(),
                "enabled_notification_listeners",
                listenerService
            );
            Thread.sleep(300);

            for (int i = 0; i < 8; i++) {
                Thread.sleep(500);
                if (checkNotificationListenerEnabled(context, listenerService)) {
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            Log.e(TAG, "【通知监听器-Round2】失败: " + e.getMessage());
            return false;
        }
    }

    private boolean setupNotificationListenerRound3(Context context, String listenerService) {
        try {
            Log.d(TAG, "【通知监听器-Round3】强制停止应用后重新设置（最后手段）");

            try {
                Runtime.getRuntime().exec("cmd notification disallow_listener " + listenerService);
                Log.d(TAG, "【通知监听器-Round3】已禁用监听器");
                Thread.sleep(300);
            } catch (Exception e) {
                Log.w(TAG, "【通知监听器-Round3】disallow_listener 命令失败: " + e.getMessage());
            }

            android.provider.Settings.Secure.putString(
                context.getContentResolver(),
                "enabled_notification_listeners",
                listenerService
            );
            Thread.sleep(200);

            try {
                Runtime.getRuntime().exec("cmd notification allow_listener " + listenerService);
                Log.d(TAG, "【通知监听器-Round3】已启用监听器");
            } catch (Exception e) {
                Log.w(TAG, "【通知监听器-Round3】allow_listener 命令失败: " + e.getMessage());
            }

            for (int i = 0; i < 8; i++) {
                Thread.sleep(500);
                if (checkNotificationListenerEnabled(context, listenerService)) {
                    Log.d(TAG, "【通知监听器-Round3】✓ 监听器已启用");

                    Log.d(TAG, "【通知监听器-Round3】正在重启应用以确保服务启动...");
                    String packageName = context.getPackageName();

                    Runtime.getRuntime().exec("am force-stop " + packageName);
                    Thread.sleep(1000);

                    Intent intent = new Intent(context, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    Log.d(TAG, "【通知监听器-Round3】✓ MainActivity 已重启");

                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            Log.e(TAG, "【通知监听器-Round3】失败: " + e.getMessage());
            return false;
        }
    }
}
