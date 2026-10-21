package cn.youth996.ui.adb;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import com.tananaev.adblib.AdbBase64;
import com.tananaev.adblib.AdbConnection;
import com.tananaev.adblib.AdbCrypto;
import com.tananaev.adblib.AdbStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdbEnhancedClient {
    private static final String TAG = "AdbEnhancedClient";
    private static final String KEY_PRIVATE = "adb_private_key";
    private static final String KEY_PUBLIC = "adb_public_key";
    private static final int DEFAULT_TIMEOUT = 10000;
    private static final int COMMAND_TIMEOUT = 8000;

    private Context context;
    private String deviceIp;
    private int devicePort;
    private AdbConnection adbConnection;
    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private ExecutorService executor;
    private String currentPackageName;

    public interface AdbEnhancedCallback {
        void onConnectionSuccess();
        void onConnectionFailed(String error);
        void onCommandExecuted(boolean success, String output);
        void onPermissionGranted(String permission);
        void onPermissionDenied(String permission, String reason);
    }

    public AdbEnhancedClient(Context context, String ip, int port) {
        this.context = context.getApplicationContext();
        this.deviceIp = ip;
        this.devicePort = port;
        this.executor = Executors.newSingleThreadExecutor();
    }

    private File getKeyFile(String keyName) {
        return new File(context.getFilesDir(), keyName);
    }

    private AdbCrypto loadOrCreateAdbCrypto() throws IOException, NoSuchAlgorithmException {
        File privateKeyFile = getKeyFile(KEY_PRIVATE);
        File publicKeyFile = getKeyFile(KEY_PUBLIC);

        if (privateKeyFile.exists() && publicKeyFile.exists()) {
            try {
                Log.d(TAG, "加载已存在的ADB密钥");
                AdbCrypto crypto = AdbCrypto.loadAdbKeyPair(
                    new AdbBase64() {
                        @Override
                        public String encodeToString(byte[] data) {
                            return Base64.encodeToString(data, Base64.NO_WRAP);
                        }
                    },
                    privateKeyFile,
                    publicKeyFile
                );
                Log.d(TAG, "密钥加载成功");
                return crypto;
            } catch (Exception e) {
                Log.w(TAG, "加载密钥失败，删除旧密钥重新生成");
                privateKeyFile.delete();
                publicKeyFile.delete();
            }
        }

        Log.d(TAG, "生成新的ADB密钥对");
        AdbCrypto crypto = AdbCrypto.generateAdbKeyPair(new AdbBase64() {
            @Override
            public String encodeToString(byte[] data) {
                return Base64.encodeToString(data, Base64.NO_WRAP);
            }
        });

        try {
            crypto.saveAdbKeyPair(privateKeyFile, publicKeyFile);
            Log.d(TAG, "新密钥已保存");
        } catch (Exception e) {
            Log.e(TAG, "保存密钥失败: " + e.getMessage());
            throw new IOException("无法保存ADB密钥", e);
        }

        return crypto;
    }

    public void connect(String packageName, AdbEnhancedCallback callback) {
        this.currentPackageName = packageName;

        executor.execute(() -> {
            try {
                Log.d(TAG, "开始连接ADB: " + deviceIp + ":" + devicePort);

                Socket socket = new Socket(deviceIp, devicePort);
                socket.setSoTimeout(DEFAULT_TIMEOUT);

                AdbCrypto crypto = loadOrCreateAdbCrypto();
                adbConnection = AdbConnection.create(socket, crypto);
                adbConnection.connect();

                isConnected.set(true);
                Log.d(TAG, "ADB连接成功");

                if (callback != null) {
                    callback.onConnectionSuccess();
                }

                grantAllPermissions(packageName, callback);

            } catch (IOException e) {
                Log.e(TAG, "连接失败: " + e.getMessage());
                if (callback != null) {
                    callback.onConnectionFailed("连接失败: " + e.getMessage() + "\n请确保设备已开启ADB端口");
                }
                closeConnection();
            } catch (Exception e) {
                Log.e(TAG, "未知错误: " + e.getMessage());
                if (callback != null) {
                    callback.onConnectionFailed("未知错误: " + e.getMessage());
                }
                closeConnection();
            }
        });
    }

    private void grantAllPermissions(String packageName, AdbEnhancedCallback callback) {
        StringBuilder output = new StringBuilder();
        output.append("=== 开始授予权限 ===\n\n");

        try {
            grantSystemPermissions(packageName, output, callback);
            grantOverlayPermissions(packageName, output, callback);
            grantRuntimePermissions(packageName, output, callback);
            grantAccessibilityPermissions(packageName, output, callback);
            grantNotificationPermissions(packageName, output, callback);
            grantAppOpsPermissions(packageName, output, callback);

            output.append("\n=== 权限授予完成 ===");
            if (callback != null) {
                callback.onCommandExecuted(true, output.toString());
            }

        } catch (Exception e) {
            Log.e(TAG, "授予权限时发生错误: " + e.getMessage());
            output.append("\n错误: ").append(e.getMessage());
            if (callback != null) {
                callback.onCommandExecuted(false, output.toString());
            }
        } finally {
            closeConnection();
        }
    }

    private void grantSystemPermissions(String packageName, StringBuilder output, AdbEnhancedCallback callback) {
        output.append("【系统设置权限】\n");

        executeCommandAndReport(
            "pm grant " + packageName + " android.permission.WRITE_SETTINGS",
            "WRITE_SETTINGS",
            output,
            callback
        );

        executeCommandAndReport(
            "pm grant " + packageName + " android.permission.WRITE_SECURE_SETTINGS",
            "WRITE_SECURE_SETTINGS",
            output,
            callback
        );
    }

    private void grantOverlayPermissions(String packageName, StringBuilder output, AdbEnhancedCallback callback) {
        output.append("\n【悬浮窗权限】\n");

        executeCommandAndReport(
            "appops set " + packageName + " SYSTEM_ALERT_WINDOW allow",
            "SYSTEM_ALERT_WINDOW",
            output,
            callback
        );

        executeCommandAndReport(
            "appops set " + packageName + " GET_USAGE_STATS allow",
            "GET_USAGE_STATS",
            output,
            callback
        );
    }

    private void grantRuntimePermissions(String packageName, StringBuilder output, AdbEnhancedCallback callback) {
        output.append("\n【运行时权限】\n");

        String[] permissions = {
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_PHONE_STATE",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.CHANGE_NETWORK_STATE",
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.permission.WAKE_LOCK",
            "android.permission.READ_LOGS",
            "android.permission.FORCE_STOP_PACKAGES",
            "android.permission.KILL_BACKGROUND_PROCESSES",
            "android.permission.GET_TASKS"
        };

        for (String permission : permissions) {
            executeCommandAndReport(
                "pm grant " + packageName + " " + permission,
                permission.substring(permission.lastIndexOf('.') + 1),
                output,
                callback
            );
        }
    }

    private void grantAccessibilityPermissions(String packageName, StringBuilder output, AdbEnhancedCallback callback) {
        output.append("\n【无障碍服务配置】\n");

        String serviceId = packageName + "/" + packageName + "$YouthAccessibilityService";

        executeCommandAndReport(
            "settings put secure enabled_accessibility_services " + serviceId,
            "无障碍服务配置",
            output,
            callback
        );

        executeCommandAndReport(
            "settings put secure accessibility_enabled 1",
            "无障碍功能启用",
            output,
            callback
        );
    }

    private void grantNotificationPermissions(String packageName, StringBuilder output, AdbEnhancedCallback callback) {
        output.append("\n【通知监听器】\n");

        executeCommandAndReport(
            "appops set " + packageName + " POST_NOTIFICATION allow",
            "通知权限",
            output,
            callback
        );

        String listenerService = packageName + "/" + packageName + "$MediaNotificationListenerService";
        executeCommandAndReport(
            "settings put secure enabled_notification_listeners " + listenerService,
            "通知监听器",
            output,
            callback
        );
    }

    private void grantAppOpsPermissions(String packageName, StringBuilder output, AdbEnhancedCallback callback) {
        output.append("\n【应用操作权限】\n");

        String[] appOps = {
            "REQUEST_INSTALL_PACKAGES",
            "INSTALL_GRANTED_PERMISSIONS",
            "GET_TOP_APP_INFO",
            " RECORD_AUDIO"
        };

        for (String appOp : appOps) {
            executeCommandAndReport(
                "appops set " + packageName + " " + appOp + " allow",
                appOp,
                output,
                callback
            );
        }
    }

    private void executeCommandAndReport(String command, String permissionName,
                                         StringBuilder output, AdbEnhancedCallback callback) {
        try {
            String result = executeCommand(command);

            if (result.contains("granted") || result.contains("Success") || result.isEmpty()) {
                output.append("✓ ").append(permissionName).append("\n");
                Log.d(TAG, permissionName + " 授权成功");
                if (callback != null) {
                    callback.onPermissionGranted(permissionName);
                }
            } else if (result.contains("Operation not allowed")) {
                output.append("⚠ ").append(permissionName).append(" (需要系统签名)\n");
                Log.w(TAG, permissionName + " 需要系统签名");
                if (callback != null) {
                    callback.onPermissionDenied(permissionName, "需要系统签名");
                }
            } else if (result.contains("requires")) {
                output.append("⚠ ").append(permissionName).append(" (运行时权限)\n");
                Log.w(TAG, permissionName + " 需要运行时授权");
                if (callback != null) {
                    callback.onPermissionDenied(permissionName, "需要运行时授权");
                }
            } else if (!result.isEmpty() && !result.contains("Exception")) {
                output.append("✓ ").append(permissionName).append(" [").append(result.trim()).append("]\n");
                Log.d(TAG, permissionName + ": " + result.trim());
            } else {
                output.append("✓ ").append(permissionName).append("\n");
                Log.d(TAG, permissionName + " 执行成功");
            }

        } catch (Exception e) {
            output.append("✗ ").append(permissionName).append(" 失败: ").append(e.getMessage()).append("\n");
            Log.e(TAG, permissionName + " 失败: " + e.getMessage());
            if (callback != null) {
                callback.onPermissionDenied(permissionName, e.getMessage());
            }
        }
    }

    private String executeCommand(String command) throws IOException, InterruptedException {
        if (!isConnected.get()) {
            throw new IOException("ADB未连接");
        }

        AdbStream stream = adbConnection.open("shell:" + command);
        StringBuilder result = new StringBuilder();
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < COMMAND_TIMEOUT) {
            try {
                byte[] data = stream.read();

                if (data == null) {
                    Thread.sleep(50);
                    continue;
                }

                if (data.length > 0) {
                    result.append(new String(data));
                }

                if (result.length() > 0 &&
                    (result.toString().contains("\n") || result.toString().contains("\r"))) {
                    break;
                }

            } catch (IOException e) {
                if (e.getMessage().contains("closed") || e.getMessage().contains("EOF")) {
                    break;
                }
                throw e;
            }
        }

        try {
            stream.close();
        } catch (IOException e) {
        }

        return result.toString().trim();
    }

    public void executeCustomCommand(String command, AdbEnhancedCallback callback) {
        executor.execute(() -> {
            StringBuilder output = new StringBuilder();

            try {
                String result = executeCommand(command);
                output.append("命令执行结果:\n").append(result);

                if (callback != null) {
                    callback.onCommandExecuted(true, output.toString());
                }

            } catch (Exception e) {
                output.append("命令执行失败: ").append(e.getMessage());
                if (callback != null) {
                    callback.onCommandExecuted(false, output.toString());
                }
            } finally {
                closeConnection();
            }
        });
    }

    /**
     * 轻量执行单条 shell 命令（连接 → 执行 → 断开），不走权限授予流程
     *
     * 适用于画中画等只需要 shell 身份执行单条命令的场景（如 am start --display）。
     * 需要设备已开启 ADB TCP 端口（5555）。
     */
    public void runShellCommand(String command, AdbEnhancedCallback callback) {
        executor.execute(() -> {
            try {
                Socket socket = new Socket(deviceIp, devicePort);
                socket.setSoTimeout(DEFAULT_TIMEOUT);
                AdbCrypto crypto = loadOrCreateAdbCrypto();
                adbConnection = AdbConnection.create(socket, crypto);
                adbConnection.connect();
                isConnected.set(true);

                String result = executeCommand(command);
                if (callback != null) {
                    callback.onCommandExecuted(true, result);
                }
            } catch (Exception e) {
                Log.e(TAG, "runShellCommand 失败: " + e.getMessage());
                if (callback != null) {
                    callback.onCommandExecuted(false, e.getMessage());
                }
            } finally {
                closeConnection();
            }
        });
    }

    public void setupComplete(String packageName, boolean reboot, AdbEnhancedCallback callback) {
        connect(packageName, new AdbEnhancedCallback() {
            @Override
            public void onConnectionSuccess() {}

            @Override
            public void onConnectionFailed(String error) {
                if (callback != null) {
                    callback.onConnectionFailed(error);
                }
            }

            @Override
            public void onCommandExecuted(boolean success, String output) {
                if (success && reboot) {
                    if (callback != null) {
                        callback.onCommandExecuted(true, output + "\n\n正在重启设备...");
                    }
                    try {
                        closeConnection();
                        Runtime.getRuntime().exec("reboot");
                    } catch (Exception e) {
                        if (callback != null) {
                            callback.onCommandExecuted(true, output + "\n\n请手动重启设备");
                        }
                    }
                } else {
                    if (callback != null) {
                        callback.onCommandExecuted(success, output);
                    }
                }
            }

            @Override
            public void onPermissionGranted(String permission) {
                if (callback != null) {
                    callback.onPermissionGranted(permission);
                }
            }

            @Override
            public void onPermissionDenied(String permission, String reason) {
                if (callback != null) {
                    callback.onPermissionDenied(permission, reason);
                }
            }
        });
    }

    public void verifyPermissions(String packageName, AdbEnhancedCallback callback) {
        executor.execute(() -> {
            StringBuilder output = new StringBuilder();
            output.append("=== 权限验证 ===\n\n");

            try {
                String result = executeCommand("dumpsys package " + packageName + " | grep permission");
                output.append("权限列表:\n").append(result).append("\n");
                output.append("\n=== 验证完成 ===");

                if (callback != null) {
                    callback.onCommandExecuted(true, output.toString());
                }

            } catch (Exception e) {
                output.append("验证失败: ").append(e.getMessage());
                if (callback != null) {
                    callback.onCommandExecuted(false, output.toString());
                }
            } finally {
                closeConnection();
            }
        });
    }

    public void setAsDefaultLauncher(String packageName, AdbEnhancedCallback callback) {
        executor.execute(() -> {
            StringBuilder output = new StringBuilder();
            output.append("=== 设置默认桌面 ===\n\n");

            try {
                String launcher = packageName + "/.MainActivity";

                executeCommand("settings put secure home_launcher_component \"" + launcher + "\"");
                output.append("✓ 已设置默认桌面\n");

                String serviceId = packageName + "/" + packageName + "$YouthAccessibilityService";
                executeCommand("settings put secure enabled_accessibility_services " + serviceId);
                output.append("✓ 无障碍服务已配置\n");

                executeCommand("settings put secure accessibility_enabled 1");
                output.append("✓ 无障碍功能已启用\n");

                executeCommand("pm disable-user --user 0 com.android.launcher3");
                output.append("✓ 系统桌面已禁用\n");

                output.append("\n请重启设备使设置生效");

                if (callback != null) {
                    callback.onCommandExecuted(true, output.toString());
                }

            } catch (Exception e) {
                output.append("\n设置失败: ").append(e.getMessage());
                if (callback != null) {
                    callback.onCommandExecuted(false, output.toString());
                }
            } finally {
                closeConnection();
            }
        });
    }

    public void rebootDevice(AdbEnhancedCallback callback) {
        executor.execute(() -> {
            try {
                if (callback != null) {
                    callback.onCommandExecuted(true, "正在重启设备...");
                }
                closeConnection();
                Runtime.getRuntime().exec("reboot");
            } catch (Exception e) {
                if (callback != null) {
                    callback.onCommandExecuted(false, "重启失败: " + e.getMessage());
                }
            }
        });
    }

    public void disconnect() {
        closeConnection();
    }

    private void closeConnection() {
        try {
            if (adbConnection != null) {
                adbConnection.close();
                adbConnection = null;
            }
            isConnected.set(false);
            Log.d(TAG, "ADB连接已关闭");
        } catch (IOException e) {
            Log.e(TAG, "关闭连接失败: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return isConnected.get();
    }

    public String getDeviceAddress() {
        return deviceIp + ":" + devicePort;
    }

    public void shutdown() {
        closeConnection();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
