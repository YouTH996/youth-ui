package cn.youth996.ui.example;

import android.content.Context;
import android.util.Log;

import cn.youth996.ui.adb.AdbEnhancedClient;
import cn.youth996.ui.carcontrol.CarControlManager;
import cn.youth996.ui.carcontrol.CarControlHelper;
import cn.youth996.ui.permission.PermissionManager;

public class README {

    public static final String TAG = "YouthUI_Features";

    public static void printFeatureOverview() {
        Log.d(TAG, "=== 轻语桌面 (Youth Desktop) 功能概览 ===");
        Log.d(TAG, "");
        Log.d(TAG, "1. ADB连接功能");
        Log.d(TAG, "   - AdbProtocolClient: 基础的ADB协议客户端");
        Log.d(TAG, "   - AdbEnhancedClient: 增强的ADB客户端，支持完整的权限授予流程");
        Log.d(TAG, "");
        Log.d(TAG, "2. 权限获取与管理");
        Log.d(TAG, "   - PermissionManager: 统一管理所有权限请求");
        Log.d(TAG, "   - 自动检测并请求 WRITE_SETTINGS 权限");
        Log.d(TAG, "   - 自动检测并请求 SYSTEM_ALERT_WINDOW 权限");
        Log.d(TAG, "   - 自动配置无障碍服务");
        Log.d(TAG, "   - 支持权限状态监控和自动恢复");
        Log.d(TAG, "");
        Log.d(TAG, "3. 车辆控制");
        Log.d(TAG, "   - CarControlManager: 车辆控制管理器");
        Log.d(TAG, "   - 空调控制: 开关、温度、风速、风向、除霜");
        Log.d(TAG, "   - 灯光控制: 大灯、转向灯、雾灯、双闪等");
        Log.d(TAG, "   - 车门控制: 锁门、解锁各车门和后备箱");
        Log.d(TAG, "");
        Log.d(TAG, "4. 自动启动和保活");
        Log.d(TAG, "   - BootReceiver: 启动接收器");
        Log.d(TAG, "   - 设备启动时自动初始化");
        Log.d(TAG, "   - 无障碍服务自动保活");
        Log.d(TAG, "   - 屏幕亮起时自动检查状态");
        Log.d(TAG, "");
        Log.d(TAG, "=== 使用示例 ===");
    }

    public static void example_ADB_Connection(Context context) {
        Log.d(TAG, "=== ADB连接示例 ===");

        AdbEnhancedClient client = new AdbEnhancedClient(context, "192.168.1.100", 5555);

        client.connect("cn.youth996.ui", new AdbEnhancedClient.AdbEnhancedCallback() {
            @Override
            public void onConnectionSuccess() {
                Log.d(TAG, "ADB连接成功");
            }

            @Override
            public void onConnectionFailed(String error) {
                Log.e(TAG, "ADB连接失败: " + error);
            }

            @Override
            public void onCommandExecuted(boolean success, String output) {
                Log.d(TAG, "命令执行: " + (success ? "成功" : "失败"));
                Log.d(TAG, "输出: " + output);
            }

            @Override
            public void onPermissionGranted(String permission) {
                Log.d(TAG, "权限已授予: " + permission);
            }

            @Override
            public void onPermissionDenied(String permission, String reason) {
                Log.w(TAG, "权限被拒绝: " + permission + " - " + reason);
            }
        });
    }

    public static void example_AC_Control(Context context) {
        Log.d(TAG, "=== 空调控制示例 ===");

        CarControlManager manager = CarControlManager.getInstance(context);

        manager.controlAc(CarControlManager.AcState.ON, 0);

        manager.controlAc(10, 24);

        manager.controlAc(11, 3);

        manager.controlAc(12, CarControlManager.WindMode.FACE_FEET);

        manager.controlAc(13, 1);

        manager.controlAc(14, 1);
    }

    public static void example_Light_Control(Context context) {
        Log.d(TAG, "=== 灯光控制示例 ===");

        CarControlManager manager = CarControlManager.getInstance(context);

        manager.controlLight(CarControlManager.LightType.HEADLIGHT, true);

        manager.controlLight(CarControlManager.LightType.HAZARD, true);

        manager.controlLight(CarControlManager.LightType.TURN_LEFT, true);
    }

    public static void example_Door_Control(Context context) {
        Log.d(TAG, "=== 车门控制示例 ===");

        CarControlManager manager = CarControlManager.getInstance(context);

        manager.controlDoorLock(CarControlManager.DoorType.ALL, CarControlManager.LockAction.LOCK);

        manager.controlDoorLock(CarControlManager.DoorType.TRUNK, CarControlManager.LockAction.UNLOCK);

        manager.controlDoorLock(CarControlManager.DoorType.FRONT_LEFT, CarControlManager.LockAction.UNLOCK);
    }

    public static void example_Permission_Management(Context context) {
        Log.d(TAG, "=== 权限管理示例 ===");

        PermissionManager manager = PermissionManager.getInstance(context);

        manager.requestAllPermissions(new PermissionManager.PermissionCallback() {
            @Override
            public void onPermissionGranted(String permission) {
                Log.d(TAG, "已授予权限: " + permission);
            }

            @Override
            public void onPermissionDenied(String permission, String reason) {
                Log.w(TAG, "权限被拒绝: " + permission + " - " + reason);
            }

            @Override
            public void onAccessibilityServiceReady(boolean success) {
                Log.d(TAG, "无障碍服务: " + (success ? "就绪" : "未就绪"));
            }

            @Override
            public void onDesktopRestartComplete() {
                Log.d(TAG, "桌面重启完成");
            }
        });
    }

    public static void example_Accessibility_Monitor(Context context) {
        Log.d(TAG, "=== 无障碍服务监控示例 ===");

        PermissionManager manager = PermissionManager.getInstance(context);

        manager.initializeAccessibilityService(new PermissionManager.PermissionCallback() {
            @Override
            public void onPermissionGranted(String permission) {
            }

            @Override
            public void onPermissionDenied(String permission, String reason) {
            }

            @Override
            public void onAccessibilityServiceReady(boolean success) {
                if (success) {
                    Log.d(TAG, "无障碍服务已就绪");
                    manager.monitorAccessibilityService();
                } else {
                    Log.e(TAG, "无障碍服务未就绪");
                }
            }

            @Override
            public void onDesktopRestartComplete() {
            }
        });
    }

    public static void example_CarControlHelper(Context context) {
        Log.d(TAG, "=== CarControlHelper 示例 ===");

        CarControlHelper helper = CarControlHelper.getInstance(context);

        helper.setListener(new CarControlHelper.CarControlListener() {
            @Override
            public void onAcStateChanged(boolean isOn, int temperature, int windLevel) {
                Log.d(TAG, "空调状态变化 - 开启: " + isOn + ", 温度: " + temperature + "°C, 风速: " + windLevel);
            }

            @Override
            public void onLightStateChanged(int lightType, boolean isOn) {
                Log.d(TAG, "灯光状态变化 - 类型: " + lightType + ", 开启: " + isOn);
            }

            @Override
            public void onDoorStateChanged(int doorType, boolean isLocked) {
                Log.d(TAG, "车门状态变化 - 类型: " + doorType + ", 锁定: " + isLocked);
            }

            @Override
            public void onCarControlResult(boolean success, String message) {
                Log.d(TAG, "控制结果 - " + (success ? "成功" : "失败") + ": " + message);
            }
        });

        helper.toggleAcPower();

        helper.adjustTemperature(true);

        helper.adjustWindLevel(false);
    }

    public static void example_Debug_Info(Context context) {
        Log.d(TAG, "=== 调试信息示例 ===");

        CarControlManager manager = CarControlManager.getInstance(context);

        Log.d(TAG, "\n" + manager.getAcDebugInfo());
        Log.d(TAG, "\n" + manager.getLightDebugInfo());
        Log.d(TAG, "\n" + manager.getDoorDebugInfo());
    }
}
