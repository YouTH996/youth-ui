package cn.youth996.ui.adb;

import android.content.Context;
import android.content.Intent;
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

public class AdbProtocolClient {
    private static final String TAG = "AdbProtocolClient";
    private static final String KEY_PRIVATE = "adb_private_key";
    private static final String KEY_PUBLIC = "adb_public_key";

    private Context context;
    private String deviceIp;
    private int devicePort;
    private AdbConnection adbConnection;
    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private ExecutorService executor;

    public interface AdbCallback {
        void onConnectionSuccess();
        void onConnectionFailed(String error);
        void onCommandExecuted(boolean success, String output);
        void onPermissionGranted();
    }

    public AdbProtocolClient(Context context, String ip, int port) {
        this.context = context;
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
                Log.d(TAG, "尝试加载已存在的ADB密钥");
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
                Log.w(TAG, "加载密钥失败: " + e.getMessage());
                Log.w(TAG, "删除旧密钥文件，重新生成");
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
            Log.d(TAG, "新密钥已保存到: " + privateKeyFile.getAbsolutePath());
            Log.d(TAG, "公钥已保存到: " + publicKeyFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "保存密钥失败: " + e.getMessage());
            throw new IOException("无法保存ADB密钥", e);
        }

        return crypto;
    }

    public void connectAndGrantPermission(String packageName, AdbCallback callback) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "开始连接ADB服务: " + deviceIp + ":" + devicePort);

                Socket socket = new Socket(deviceIp, devicePort);
                socket.setSoTimeout(10000);

                Log.d(TAG, "Socket连接成功");

                AdbCrypto crypto = loadOrCreateAdbCrypto();

                adbConnection = AdbConnection.create(socket, crypto);
                adbConnection.connect();
                isConnected.set(true);
                Log.d(TAG, "ADB连接成功");

                callback.onConnectionSuccess();

                grantPermission(packageName, new AdbCallback() {
                    @Override
                    public void onConnectionSuccess() {}

                    @Override
                    public void onConnectionFailed(String error) {
                        callback.onConnectionFailed(error);
                    }

                    @Override
                    public void onCommandExecuted(boolean success, String output) {
                        callback.onCommandExecuted(success, output);
                    }
                    
                    @Override
                    public void onPermissionGranted() {
                        callback.onPermissionGranted();
                    }
                });

            } catch (IOException e) {
                Log.e(TAG, "连接异常: " + e.getMessage());
                callback.onConnectionFailed("连接异常: " + e.getMessage() + "\n\n请确保设备已开启ADB端口");
                closeConnection();
            } catch (Exception e) {
                Log.e(TAG, "未知错误: " + e.getMessage());
                callback.onConnectionFailed("未知错误: " + e.getMessage());
                closeConnection();
            }
        });
    }

    /**
     * 一键完成所有设置：授予权限 + 重启桌面（带多轮重试机制）
     */
    public void setupComplete(String packageName, boolean restartDesktop, AdbCallback callback) {
        connectAndGrantPermission(packageName, new AdbCallback() {
            @Override
            public void onConnectionSuccess() {}

            @Override
            public void onConnectionFailed(String error) {
                callback.onConnectionFailed(error);
            }

            @Override
            public void onCommandExecuted(boolean success, String output) {
                if (success && restartDesktop) {
                    callback.onCommandExecuted(true, output + "\n\n正在重启桌面...");
                    restartDesktopWithRetry(packageName, callback);
                } else {
                    callback.onCommandExecuted(success, output);
                }
            }
            
            @Override
            public void onPermissionGranted() {
                callback.onPermissionGranted();
            }
        });
    }

    /**
     * 重启桌面（多轮重试机制，参考BYD Launcher）
     */
    private void restartDesktopWithRetry(String packageName, AdbCallback callback) {
        executor.execute(() -> {
            try {
                Log.i(TAG, "═══════════════════════════════════════════════════");
                Log.i(TAG, "【桌面重启】开始重启桌面...");
                Log.i(TAG, "═══════════════════════════════════════════════════");

                String serviceId = packageName + "/" + packageName + ".YouthAccessibilityService";

                // Round 1: 直接重启
                Log.d(TAG, "【桌面重启】Round 1: 直接重启...");
                executeCommand("settings put secure enabled_accessibility_services " + serviceId);
                Thread.sleep(300);
                executeCommand("settings put secure accessibility_enabled 1");

                // 等待
                for (int i = 0; i < 6; i++) {
                    Thread.sleep(500);
                    Log.d(TAG, "【桌面重启】等待 " + i + "/6...");
                }

                Log.d(TAG, "【桌面重启】Round 1 完成");

                callback.onCommandExecuted(true, "桌面重启完成");

            } catch (Exception e) {
                Log.e(TAG, "【桌面重启】重启桌面时发生异常: " + e.getMessage());
                e.printStackTrace();
                callback.onCommandExecuted(false, "桌面重启失败: " + e.getMessage());
            }
        });
    }

    private void executeCommand(String command) {
        try {
            if (adbConnection != null) {
                AdbStream stream = adbConnection.open("shell:" + command);
                Thread.sleep(200);
                try {
                    stream.close();
                } catch (IOException e) {
                }
                Log.d(TAG, "【桌面重启】执行: " + command);
            }
        } catch (Exception e) {
            Log.e(TAG, "【桌面重启】命令执行失败: " + command + " - " + e.getMessage());
        }
    }

    private void grantPermission(String packageName, AdbCallback callback) {
        StringBuilder output = new StringBuilder();
        output.append("=== 权限授予结果 ===\n\n");

        try {
            // 系统设置权限
            output.append("【系统设置权限】\n");
            executeCommandAndReport(
                "pm grant " + packageName + " android.permission.WRITE_SETTINGS",
                "WRITE_SETTINGS",
                output
            );

            executeCommandAndReport(
                "pm grant " + packageName + " android.permission.WRITE_SECURE_SETTINGS",
                "WRITE_SECURE_SETTINGS",
                output
            );

            // 悬浮窗权限
            output.append("\n【悬浮窗权限】\n");
            executeCommandAndReport(
                "appops set " + packageName + " SYSTEM_ALERT_WINDOW allow",
                "SYSTEM_ALERT_WINDOW",
                output
            );

            executeCommandAndReport(
                "appops set " + packageName + " GET_USAGE_STATS allow",
                "GET_USAGE_STATS",
                output
            );

            // 运行时权限
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
                "android.permission.GET_TASKS",
                "android.permission.MEDIA_CONTENT_CONTROL"
            };

            for (String permission : permissions) {
                String permName = permission.substring(permission.lastIndexOf('.') + 1);
                executeCommandAndReport("pm grant " + packageName + " " + permission, permName, output);
            }

            // 无障碍服务配置
            output.append("\n【无障碍服务】\n");
            executeCommandAndReport(
                "settings put secure enabled_accessibility_services '" + packageName + "/" + packageName + ".YouthAccessibilityService'",
                "无障碍服务配置",
                output
            );

            executeCommandAndReport(
                "settings put secure accessibility_enabled 1",
                "无障碍功能启用",
                output
            );

            // 通知监听器
            output.append("\n【通知监听器】\n");
            executeCommandAndReport(
                "appops set " + packageName + " POST_NOTIFICATION allow",
                "通知权限",
                output
            );

            String listenerService = packageName + "/" + packageName + ".service.MediaNotificationListenerService";
            
            output.append("【通知监听器】设置服务: " + listenerService + "\n");
            
            executeCommandAndReport(
                "settings put secure enabled_notification_listeners '" + listenerService + "'",
                "通知监听器",
                output
            );
            
            executeCommandAndReport(
                "settings get secure enabled_notification_listeners",
                "验证监听器列表",
                output
            );

            // 应用操作权限
            output.append("\n【应用操作权限】\n");
            String[] appOps = {
                "REQUEST_INSTALL_PACKAGES",
                "INSTALL_GRANTED_PERMISSIONS",
                "GET_TOP_APP_INFO",
                "RECORD_AUDIO"
            };

            for (String appOp : appOps) {
                executeCommandAndReport(
                    "appops set " + packageName + " " + appOp + " allow",
                    appOp,
                    output
                );
            }

            output.append("\n\n=== 权限授予完成 ===");
            callback.onCommandExecuted(true, output.toString());
            
            output.append("\n\n=== 正在重启通知监听器服务 ===\n");
            
            executeCommandAndReport(
                "am force-stop " + packageName,
                "停止应用",
                output
            );
            
            executeCommandAndReport(
                "am startservice -n " + packageName + "/" + packageName + ".service.MediaNotificationListenerService",
                "启动通知监听服务",
                output
            );
            
            callback.onPermissionGranted();
            Log.i(TAG, "【ADB权限授予】所有权限已授予完成，通知应用重新启动监听");

        } catch (Exception e) {
            Log.e(TAG, "授予权限时发生错误: " + e.getMessage());
            output.append("\n错误: ").append(e.getMessage());
            callback.onCommandExecuted(false, output.toString());
        } finally {
            closeConnection();
        }
    }

    private String readStreamContent(AdbStream stream) {
        StringBuilder content = new StringBuilder();
        try {
            byte[] data;
            long startTime = System.currentTimeMillis();
            long timeout = 5000;

            while ((data = stream.read()) != null) {
                if (data.length > 0) {
                    content.append(new String(data));
                }

                if (System.currentTimeMillis() - startTime > timeout) {
                    Log.w(TAG, "读取流超时");
                    break;
                }
            }
        } catch (IOException | InterruptedException e) {
            Log.d(TAG, "读取流时发生异常（可能正常）: " + e.getMessage());
        }
        return content.toString();
    }

    private void executeCommandAndReport(String command, String permissionName, StringBuilder output) {
        AdbStream stream = null;
        try {
            stream = adbConnection.open("shell:" + command);

            long startTime = System.currentTimeMillis();
            long timeout = 8000;
            StringBuilder resultBuilder = new StringBuilder();

            while (System.currentTimeMillis() - startTime < timeout) {
                try {
                    byte[] data = stream.read();

                    if (data == null) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        continue;
                    }

                    if (data.length > 0) {
                        resultBuilder.append(new String(data));
                    }

                    if (resultBuilder.length() > 0 &&
                        (resultBuilder.toString().contains("\n") ||
                         resultBuilder.toString().contains("\r"))) {
                        break;
                    }

                } catch (IOException e) {
                    if (e.getMessage() != null &&
                        (e.getMessage().contains("closed") ||
                         e.getMessage().contains("EOF"))) {
                        break;
                    }
                }
            }

            String result = resultBuilder.toString().trim();

            try {
                stream.close();
            } catch (IOException e) {
            }

            if (result.isEmpty() || result.contains("granted") ||
                result.contains("allowed") || result.contains("Success")) {
                output.append("✓ ").append(permissionName).append("\n");
                Log.d(TAG, permissionName + " 授权成功");
            } else if (result.contains("Operation not allowed") ||
                       result.contains("Not allowed")) {
                output.append("⚠ ").append(permissionName)
                      .append(" (需要系统签名)\n");
                Log.w(TAG, permissionName + " 需要系统签名");
            } else if (result.contains("requires")) {
                output.append("⚠ ").append(permissionName)
                      .append(" (运行时权限)\n");
                Log.w(TAG, permissionName + " 需要运行时授权");
            } else if (result.contains("Exception") || result.contains("Error")) {
                output.append("⚠ ").append(permissionName)
                      .append(": ").append(result).append("\n");
                Log.w(TAG, permissionName + ": " + result);
            } else {
                output.append("✓ ").append(permissionName)
                      .append(" [").append(result).append("]\n");
                Log.d(TAG, permissionName + ": " + result);
            }

        } catch (Exception e) {
            output.append("✗ ").append(permissionName)
                  .append(" 失败: ").append(e.getMessage()).append("\n");
            Log.e(TAG, permissionName + " 失败: " + e.getMessage());

            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e2) {
                }
            }
        }
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
            Log.d(TAG, "连接已关闭");
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

    /**
     * 验证权限是否已授予
     */
    public void verifyPermissions(String packageName, AdbCallback callback) {
        executor.execute(() -> {
            StringBuilder output = new StringBuilder();
            output.append("=== 权限验证 ===\n\n");

            try {
                executeCommandAndReport(
                    "dumpsys package " + packageName + " | grep permission",
                    "权限列表",
                    output
                );

                output.append("\n=== 验证完成 ===");
                callback.onCommandExecuted(true, output.toString());

            } catch (Exception e) {
                Log.e(TAG, "验证权限失败: " + e.getMessage());
                output.append("\n错误: ").append(e.getMessage());
                callback.onCommandExecuted(false, output.toString());
            } finally {
                closeConnection();
            }
        });
    }

    /**
     * 设置应用为默认桌面
     */
    public void setAsDefaultLauncher(String packageName, AdbCallback callback) {
        executor.execute(() -> {
            StringBuilder output = new StringBuilder();
            output.append("=== 设置默认桌面 ===\n\n");

            try {
                String launcher = packageName + "/.MainActivity";

                executeCommandAndReport(
                    "settings put secure home_launcher_component \"" + launcher + "\"",
                    "默认桌面设置",
                    output
                );

                String serviceId = packageName + "/" + packageName + ".YouthAccessibilityService";
                executeCommandAndReport(
                    "settings put secure enabled_accessibility_services '" + serviceId + "'",
                    "无障碍服务配置",
                    output
                );

                executeCommandAndReport(
                    "settings put secure accessibility_enabled 1",
                    "无障碍功能启用",
                    output
                );

                executeCommandAndReport(
                    "pm disable-user --user 0 com.android.launcher3",
                    "禁用系统桌面",
                    output
                );

                output.append("\n请重启设备使设置生效");
                callback.onCommandExecuted(true, output.toString());

            } catch (Exception e) {
                output.append("\n设置失败: ").append(e.getMessage());
                callback.onCommandExecuted(false, output.toString());
            } finally {
                closeConnection();
            }
        });
    }

    /**
     * 重启设备
     */
    public void rebootDevice(AdbCallback callback) {
        executor.execute(() -> {
            StringBuilder output = new StringBuilder();
            output.append("正在重启设备...\n");

            try {
                callback.onCommandExecuted(true, output.toString());
                closeConnection();

                try {
                    Runtime.getRuntime().exec("reboot");
                } catch (Exception e) {
                    callback.onCommandExecuted(true, output.toString() + "\n请手动重启设备");
                }

            } catch (Exception e) {
                output.append("\n重启失败: ").append(e.getMessage());
                callback.onCommandExecuted(false, output.toString());
            }
        });
    }
}
