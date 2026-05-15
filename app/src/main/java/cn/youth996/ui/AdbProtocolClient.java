package cn.youth996.ui;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import com.tananaev.adblib.AdbBase64;
import com.tananaev.adblib.AdbConnection;
import com.tananaev.adblib.AdbCrypto;
import com.tananaev.adblib.AdbStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdbProtocolClient {
    private static final String TAG = "AdbProtocolClient";
    private static final String KEY_PRIVATE = "adb_private_key";
    private static final String KEY_PUBLIC = "adb_public_key";
    
    private Context context;
    private String deviceIp;
    private int devicePort;
    private AdbConnection adbConnection;
    private boolean isConnected = false;
    private ExecutorService executor;
    
    public interface AdbCallback {
        void onConnectionSuccess();
        void onConnectionFailed(String error);
        void onCommandExecuted(boolean success, String output);
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
                boolean deleted1 = privateKeyFile.delete();
                boolean deleted2 = publicKeyFile.delete();
                Log.d(TAG, "删除结果: private=" + deleted1 + ", public=" + deleted2);
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
            e.printStackTrace();
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
                isConnected = true;
                Log.d(TAG, "ADB连接成功");
                
                callback.onConnectionSuccess();
                
                // 步骤1：授予所有权限
                grantPermission(packageName, new AdbCallback() {
                    @Override
                    public void onConnectionSuccess() {}
                    
                    @Override
                    public void onConnectionFailed(String error) {
                        callback.onConnectionFailed(error);
                    }
                    
                    @Override
                    public void onCommandExecuted(boolean success, String output) {
                        // 步骤2：验证权限
                        verifyPermissions(packageName, new AdbCallback() {
                            @Override
                            public void onConnectionSuccess() {}
                            
                            @Override
                            public void onConnectionFailed(String error) {
                                callback.onCommandExecuted(success, output + "\n\n" + error);
                            }
                            
                            @Override
                            public void onCommandExecuted(boolean vSuccess, String vOutput) {
                                // 步骤3：设置默认桌面
                                setAsDefaultLauncher(packageName, new AdbCallback() {
                                    @Override
                                    public void onConnectionSuccess() {}
                                    
                                    @Override
                                    public void onConnectionFailed(String error) {
                                        callback.onCommandExecuted(success && vSuccess, 
                                            output + "\n\n" + vOutput + "\n\n" + error);
                                    }
                                    
                                    @Override
                                    public void onCommandExecuted(boolean lSuccess, String lOutput) {
                                        // 汇总所有结果
                                        String finalOutput = output + "\n\n" + vOutput + "\n\n" + lOutput;
                                        callback.onCommandExecuted(success && vSuccess && lSuccess, finalOutput);
                                    }
                                });
                            }
                        });
                    }
                });
                
            } catch (IOException e) {
                Log.e(TAG, "连接异常: " + e.getMessage());
                callback.onConnectionFailed("连接异常: " + e.getMessage() + "\n\n" +
                    "请确保设备已开启ADB端口");
                closeConnection();
            } catch (Exception e) {
                Log.e(TAG, "未知错误: " + e.getMessage());
                callback.onConnectionFailed("未知错误: " + e.getMessage());
                closeConnection();
            }
        });
    }
    
    /**
     * 一键完成所有设置：授予权限 + 设置默认桌面 + 重启设备
     */
    public void setupComplete(String packageName, boolean reboot, AdbCallback callback) {
        connectAndGrantPermission(packageName, new AdbCallback() {
            @Override
            public void onConnectionSuccess() {}
            
            @Override
            public void onConnectionFailed(String error) {
                callback.onConnectionFailed(error);
            }
            
            @Override
            public void onCommandExecuted(boolean success, String output) {
                if (success && reboot) {
                    callback.onCommandExecuted(true, output + "\n\n正在重启设备...");
                    try {
                        closeConnection();
                        Runtime.getRuntime().exec("reboot");
                    } catch (Exception e) {
                        callback.onCommandExecuted(true, output + "\n\n请手动重启设备");
                    }
                } else {
                    callback.onCommandExecuted(success, output);
                }
            }
        });
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
    
    private void executeCommand(String command, String permissionName, StringBuilder output) {
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
    
    private void grantPermission(String packageName, AdbCallback callback) {
        StringBuilder output = new StringBuilder();
        output.append("=== 权限授予结果 ===\n\n");
        
        try {
            // === 1. 系统设置权限 ===
            output.append("【系统设置权限】\n");
            executeCommand(
                "pm grant " + packageName + " android.permission.WRITE_SETTINGS",
                "WRITE_SETTINGS",
                output
            );
            
            executeCommand(
                "pm grant " + packageName + " android.permission.WRITE_SECURE_SETTINGS",
                "WRITE_SECURE_SETTINGS",
                output
            );
            
            // === 2. 悬浮窗权限 ===
            output.append("\n【悬浮窗权限】\n");
            executeCommand(
                "appops set " + packageName + " SYSTEM_ALERT_WINDOW allow",
                "SYSTEM_ALERT_WINDOW",
                output
            );
            
            executeCommand(
                "appops set " + packageName + " GET_USAGE_STATS allow",
                "PACKAGE_USAGE_STATS (appops)",
                output
            );
            
            // === 3. 运行时权限 ===
            output.append("\n【运行时权限】\n");
            executeCommand(
                "pm grant " + packageName + " android.permission.ACCESS_COARSE_LOCATION",
                "ACCESS_COARSE_LOCATION",
                output
            );
            
            executeCommand(
                "pm grant " + packageName + " android.permission.ACCESS_FINE_LOCATION",
                "ACCESS_FINE_LOCATION",
                output
            );
            
            executeCommand(
                "pm grant " + packageName + " android.permission.READ_PHONE_STATE",
                "READ_PHONE_STATE",
                output
            );
            
            executeCommand(
                "pm grant " + packageName + " android.permission.READ_EXTERNAL_STORAGE",
                "READ_EXTERNAL_STORAGE",
                output
            );
            
            executeCommand(
                "pm grant " + packageName + " android.permission.WRITE_EXTERNAL_STORAGE",
                "WRITE_EXTERNAL_STORAGE",
                output
            );
            
            executeCommand(
                "pm grant " + packageName + " android.permission.BLUETOOTH",
                "BLUETOOTH",
                output
            );
            
            executeCommand(
                "pm grant " + packageName + " android.permission.BLUETOOTH_ADMIN",
                "BLUETOOTH_ADMIN",
                output
            );
            
            executeCommand(
                "pm grant " + packageName + " android.permission.INTERNET",
                "INTERNET",
                output
            );
            
            executeCommand(
                "pm grant " + packageName + " android.permission.ACCESS_NETWORK_STATE",
                "ACCESS_NETWORK_STATE",
                output
            );
            
            executeCommand(
                "pm grant " + packageName + " android.permission.ACCESS_WIFI_STATE",
                "ACCESS_WIFI_STATE",
                output
            );
            
            executeCommand(
                "pm grant " + packageName + " android.permission.CHANGE_NETWORK_STATE",
                "CHANGE_NETWORK_STATE",
                output
            );
            
            executeCommand(
                "pm grant " + packageName + " android.permission.RECEIVE_BOOT_COMPLETED",
                "RECEIVE_BOOT_COMPLETED",
                output
            );
            
            executeCommand(
                "pm grant " + packageName + " android.permission.WAKE_LOCK",
                "WAKE_LOCK",
                output
            );
            
            executeCommand(
                "pm grant " + packageName + " android.permission.READ_LOGS",
                "READ_LOGS",
                output
            );
            
            executeCommand(
                "pm grant " + packageName + " android.permission.FORCE_STOP_PACKAGES",
                "FORCE_STOP_PACKAGES",
                output
            );
            
            executeCommand(
                "pm grant " + packageName + " android.permission.KILL_BACKGROUND_PROCESSES",
                "KILL_BACKGROUND_PROCESSES",
                output
            );
            
            executeCommand(
                "pm grant " + packageName + " android.permission.GET_TASKS",
                "GET_TASKS",
                output
            );
            
            // === 4. 无障碍服务配置 ===
            output.append("\n【无障碍服务】\n");
            executeCommand(
                "settings put secure enabled_accessibility_services " + packageName + "/" + packageName + ".YouthAccessibilityService",
                "无障碍服务已配置",
                output
            );
            
            executeCommand(
                "settings put secure accessibility_enabled 1",
                "无障碍功能已启用",
                output
            );
            
            // === 5. 通知监听器权限 ===
            output.append("\n【通知监听器】\n");
            executeCommand(
                "appops set " + packageName + " POST_NOTIFICATION allow",
                "通知权限 (AppOps)",
                output
            );
            
            executeCommand(
                "settings put secure enabled_notification_listeners " + packageName + "/" + packageName + "$MediaNotificationListenerService",
                "通知监听器服务",
                output
            );
            
            // === 6. 应用操作权限 ===
            output.append("\n【应用操作权限】\n");
            executeCommand(
                "appops set " + packageName + " REQUEST_INSTALL_PACKAGES allow",
                "REQUEST_INSTALL_PACKAGES",
                output
            );
            
            executeCommand(
                "appops set " + packageName + " INSTALL_GRANTED_PERMISSIONS allow",
                "INSTALL_GRANTED_PERMISSIONS",
                output
            );
            
            // === 7. 自定义Provider权限 ===
            output.append("\n【自定义Provider】\n");
            executeCommand(
                "appops set " + packageName + " GET_TOP_APP_INFO allow",
                "GET_TOP_APP_INFO",
                output
            );
            
            // === 8. 音频焦点 ===
            output.append("\n【音频权限】\n");
            executeCommand(
                "appops set " + packageName + " RECORD_AUDIO allow",
                "RECORD_AUDIO",
                output
            );
            
            // === 9. 设置默认桌面 ===
            output.append("\n【默认桌面设置】\n");
            String homeLauncher = "cn.youth996.ui/.MainActivity";
            executeCommand(
                "settings put secure home_launcher_component \"" + homeLauncher + "\"",
                "已设置轻语桌面为默认桌面",
                output
            );
            
            // === 10. 锁屏设置 ===
            output.append("\n【锁屏设置】\n");
            executeCommand(
                "settings put secure lock_screen_owner_info_enabled 0",
                "锁屏信息已禁用",
                output
            );
            
            output.append("\n\n=== 全部权限授予完成 ===");
            callback.onCommandExecuted(true, output.toString());
            
        } catch (Exception e) {
            Log.e(TAG, "授予权限失败: " + e.getMessage());
            output.append("\n错误: ").append(e.getMessage());
            callback.onCommandExecuted(false, output.toString());
        } finally {
            closeConnection();
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
            isConnected = false;
            Log.d(TAG, "连接已关闭");
        } catch (IOException e) {
            Log.e(TAG, "关闭连接失败: " + e.getMessage());
        }
    }
    
    public boolean isConnected() {
        return isConnected;
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
     * 验证所有权限是否已授予
     */
    public void verifyPermissions(String packageName, AdbCallback callback) {
        executor.execute(() -> {
            StringBuilder output = new StringBuilder();
            output.append("=== 权限验证结果 ===\n\n");
            
            try {
                executeCommand(
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
     * 创建桌面快捷方式
     */
    public void createShortcut(String packageName, AdbCallback callback) {
        executor.execute(() -> {
            StringBuilder output = new StringBuilder();
            output.append("=== 创建桌面快捷方式 ===\n\n");
            
            try {
                String launcherActivity = getMainActivity(packageName);
                
                if (launcherActivity != null) {
                    String intentUri = "intent:#Intent;action=android.intent.action.MAIN;" +
                                     "category=android.intent.category.LAUNCHER;" +
                                     "component=" + packageName + "/" + launcherActivity +
                                     ";end";
                    
                    executeCommand(
                        "am start -a android.intent.action.CREATE_SHORTCUT " +
                        "-d \"" + intentUri + "\" " +
                        "--user 0",
                        "桌面快捷方式",
                        output
                    );
                } else {
                    output.append("⚠ 无法获取主Activity\n");
                }
                
                output.append("\n=== 完成 ===");
                callback.onCommandExecuted(true, output.toString());
                
            } catch (Exception e) {
                Log.e(TAG, "创建快捷方式失败: " + e.getMessage());
                output.append("\n错误: ").append(e.getMessage());
                callback.onCommandExecuted(false, output.toString());
            } finally {
                closeConnection();
            }
        });
    }
    
    /**
     * 获取应用的主Activity名称
     */
    private String getMainActivity(String packageName) {
        try {
            AdbStream stream = adbConnection.open("shell:dumpsys package " + packageName + 
                                                  " | grep -A 1 MAIN");
            Thread.sleep(300);
            String result = readStreamContent(stream);
            
            if (result.contains("/.")) {
                int start = result.indexOf("/.");
                int end = result.indexOf("}", start);
                if (end > start) {
                    return result.substring(start + 2, end).trim();
                }
            } else if (result.contains("/")) {
                int start = result.indexOf("/");
                int end = result.indexOf(" ", start);
                if (end > start) {
                    return result.substring(start + 1, end).trim();
                }
            }
            
            stream.close();
        } catch (Exception e) {
            Log.w(TAG, "获取MainActivity失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 设置应用为默认桌面
     */
    public void setAsDefaultLauncher(String packageName, AdbCallback callback) {
        executor.execute(() -> {
            StringBuilder output = new StringBuilder();
            output.append("=== 设置默认桌面 ===\n\n");
            
            try {
                String homeLauncher = packageName + "/.MainActivity";
                
                executeCommand(
                    "settings put secure home_launcher_component \"" + homeLauncher + "\"",
                    "默认桌面设置",
                    output
                );
                
                executeCommand(
                    "settings put secure enabled_accessibility_services " + packageName + "/" + packageName + "$MainActivity",
                    "无障碍服务配置",
                    output
                );
                
                executeCommand(
                    "settings put secure accessibility_enabled 1",
                    "无障碍功能启用",
                    output
                );
                
                executeCommand(
                    "pm disable-user --user 0 com.android.launcher3",
                    "禁用系统桌面",
                    output
                );
                
                output.append("\n=== 完成 ===\n");
                output.append("请重启设备以使设置生效");
                callback.onCommandExecuted(true, output.toString());
                
            } catch (Exception e) {
                Log.e(TAG, "设置默认桌面失败: " + e.getMessage());
                output.append("\n错误: ").append(e.getMessage());
                callback.onCommandExecuted(false, output.toString());
            } finally {
                closeConnection();
            }
        });
    }
    
    /**
     * 重启设备使设置生效
     */
    public void rebootDevice(AdbCallback callback) {
        executor.execute(() -> {
            StringBuilder output = new StringBuilder();
            output.append("正在重启设备...\n");
            
            try {
                callback.onCommandExecuted(true, output.toString());
                closeConnection();
                
                Runtime.getRuntime().exec("reboot");
                
            } catch (Exception e) {
                Log.e(TAG, "重启设备失败: " + e.getMessage());
                output.append("\n错误: ").append(e.getMessage());
                callback.onCommandExecuted(false, output.toString());
            }
        });
    }
}
