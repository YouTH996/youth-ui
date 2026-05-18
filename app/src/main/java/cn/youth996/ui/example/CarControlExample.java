package cn.youth996.ui.example;

import android.content.Context;

import cn.youth996.ui.adb.AdbEnhancedClient;
import cn.youth996.ui.carcontrol.CarControlManager;

public class CarControlExample {

    public static void exampleUsage(Context context) {
        CarControlManager carControlManager = CarControlManager.getInstance(context);

        carControlManager.setCallback(new CarControlManager.CarControlCallback() {
            @Override
            public void onControlSuccess(String controlType, String action) {
                android.util.Log.d("CarControl", controlType + ": " + action + " 成功");
            }

            @Override
            public void onControlFailed(String controlType, String error) {
                android.util.Log.e("CarControl", controlType + " 失败: " + error);
            }

            @Override
            public void onStateChanged(String stateType, org.json.JSONObject data) {
                android.util.Log.d("CarControl", "状态变化: " + stateType + " -> " + data.toString());
            }
        });

        carControlManager.controlAc(CarControlManager.AcState.ON, 0);

        carControlManager.controlAc(10, 24);

        carControlManager.controlAc(11, 3);

        carControlManager.controlAc(12, CarControlManager.WindMode.FACE);

        carControlManager.controlAc(13, 1);

        carControlManager.controlAc(14, 1);

        carControlManager.controlLight(CarControlManager.LightType.HEADLIGHT, true);

        carControlManager.controlLight(CarControlManager.LightType.HAZARD, true);

        carControlManager.controlDoorLock(CarControlManager.DoorType.ALL, CarControlManager.LockAction.LOCK);

        carControlManager.controlDoorLock(CarControlManager.DoorType.TRUNK, CarControlManager.LockAction.UNLOCK);
    }

    public static void exampleAdbConnection(Context context) {
        AdbEnhancedClient adbClient = new AdbEnhancedClient(context, "192.168.1.100", 5555);

        adbClient.connect("cn.youth996.ui", new AdbEnhancedClient.AdbEnhancedCallback() {
            @Override
            public void onConnectionSuccess() {
                android.util.Log.d("AdbExample", "ADB连接成功");
            }

            @Override
            public void onConnectionFailed(String error) {
                android.util.Log.e("AdbExample", "ADB连接失败: " + error);
            }

            @Override
            public void onCommandExecuted(boolean success, String output) {
                android.util.Log.d("AdbExample", "命令执行结果: " + output);
            }

            @Override
            public void onPermissionGranted(String permission) {
                android.util.Log.d("AdbExample", "权限已授予: " + permission);
            }

            @Override
            public void onPermissionDenied(String permission, String reason) {
                android.util.Log.w("AdbExample", "权限被拒绝: " + permission + " - " + reason);
            }
        });
    }
}
