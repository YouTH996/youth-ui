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
    
    private String getEncodedPrivateKey(AdbCrypto crypto) throws Exception {
        Field keyPairField = AdbCrypto.class.getDeclaredField("keyPair");
        keyPairField.setAccessible(true);
        java.security.KeyPair keyPair = (java.security.KeyPair) keyPairField.get(crypto);
        
        return Base64.encodeToString(keyPair.getPrivate().getEncoded(), Base64.NO_WRAP);
    }
    
    private String getEncodedPublicKey(AdbCrypto crypto) throws Exception {
        Field keyPairField = AdbCrypto.class.getDeclaredField("keyPair");
        keyPairField.setAccessible(true);
        java.security.KeyPair keyPair = (java.security.KeyPair) keyPairField.get(crypto);
        
        return Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.NO_WRAP);
    }
    
    private AdbCrypto loadOrCreateAdbCrypto() throws IOException, NoSuchAlgorithmException {
        File privateKeyFile = getKeyFile(KEY_PRIVATE);
        File publicKeyFile = getKeyFile(KEY_PUBLIC);
        
        try {
            if (privateKeyFile.exists() && publicKeyFile.exists()) {
                Log.d(TAG, "加载已存在的ADB密钥");
                return AdbCrypto.loadAdbKeyPair(
                    new AdbBase64() {
                        @Override
                        public String encodeToString(byte[] data) {
                            return Base64.encodeToString(data, Base64.NO_WRAP);
                        }
                    },
                    privateKeyFile,
                    publicKeyFile
                );
            }
        } catch (Exception e) {
            Log.w(TAG, "加载密钥失败，将生成新密钥: " + e.getMessage());
            if (privateKeyFile.exists()) {
                privateKeyFile.delete();
            }
            if (publicKeyFile.exists()) {
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
            String privateKeyBase64 = getEncodedPrivateKey(crypto);
            String publicKeyBase64 = getEncodedPublicKey(crypto);
            
            try (FileOutputStream fos = new FileOutputStream(privateKeyFile)) {
                fos.write(privateKeyBase64.getBytes());
                fos.flush();
            }
            try (FileOutputStream fos = new FileOutputStream(publicKeyFile)) {
                fos.write(publicKeyBase64.getBytes());
                fos.flush();
            }
            Log.d(TAG, "新密钥已保存");
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
                isConnected = true;
                Log.d(TAG, "ADB连接成功");
                
                callback.onConnectionSuccess();
                grantPermission(packageName, callback);
                
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
    
    private String readStreamContent(AdbStream stream) {
        StringBuilder content = new StringBuilder();
        try {
            byte[] data;
            while ((data = stream.read()) != null) {
                content.append(new String(data));
            }
        } catch (IOException | InterruptedException e) {
            Log.d(TAG, "读取流时发生异常（可能正常）: " + e.getMessage());
        }
        return content.toString();
    }
    
    private void executeCommand(String command, String successMessage, String errorPrefix, StringBuilder output) {
        try {
            AdbStream stream = adbConnection.open("shell:" + command);
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            String result = readStreamContent(stream).trim();
            
            try {
                stream.close();
            } catch (IOException e) {
                Log.d(TAG, "关闭流时异常（可能正常）: " + e.getMessage());
            }
            
            if (result.isEmpty()) {
                output.append("✓ ").append(successMessage).append("\n");
                Log.d(TAG, successMessage);
            } else {
                output.append("✗ ").append(errorPrefix).append(": ").append(result).append("\n");
                Log.w(TAG, errorPrefix + ": " + result);
            }
        } catch (Exception e) {
            output.append("✗ ").append(errorPrefix).append(" 失败: ").append(e.getMessage()).append("\n");
            Log.e(TAG, errorPrefix + " 失败: " + e.getMessage());
        }
    }
    
    private void grantPermission(String packageName, AdbCallback callback) {
        StringBuilder output = new StringBuilder();
        output.append("=== 权限授予结果 ===\n\n");
        
        try {
            executeCommand(
                "pm grant " + packageName + " android.permission.WRITE_SETTINGS",
                "WRITE_SETTINGS 权限授予成功",
                "WRITE_SETTINGS",
                output
            );
            
            executeCommand(
                "appops set " + packageName + " SYSTEM_ALERT_WINDOW allow",
                "SYSTEM_ALERT_WINDOW 权限授予成功",
                "SYSTEM_ALERT_WINDOW",
                output
            );
            
            executeCommand(
                "pm grant " + packageName + " android.permission.WRITE_SECURE_SETTINGS",
                "WRITE_SECURE_SETTINGS 权限授予成功",
                "WRITE_SECURE_SETTINGS",
                output
            );
            
            executeCommand(
                "settings put secure enabled_accessibility_services " + packageName + "/" + packageName + ".YouthAccessibilityService",
                "无障碍服务配置成功",
                "无障碍服务配置",
                output
            );
            
            executeCommand(
                "settings put secure accessibility_enabled 1",
                "无障碍功能已启用",
                "无障碍功能启用",
                output
            );
            
            output.append("\n=== 完成 ===");
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
}
