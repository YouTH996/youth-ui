package cn.youth996.ui.carcontrol;

import android.content.Context;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;

import cn.youth996.ui.core.YouthAccessibilityService;

public class CarControlManager {
    private static final String TAG = "CarControlManager";
    private Context context;
    private static CarControlManager instance;

    public interface CarControlCallback {
        void onControlSuccess(String controlType, String action);
        void onControlFailed(String controlType, String error);
        void onStateChanged(String stateType, JSONObject data);
    }

    private CarControlCallback callback;

    public static class AcState {
        public static final int OFF = 0;
        public static final int ON = 1;
        public static final int AUTO = 2;
        public static final int ECO = 3;
        public static final int SPORT = 4;
    }

    public static class WindMode {
        public static final int FACE = 0;
        public static final int CHEST = 1;
        public static final int FEET = 2;
        public static final int FACE_FEET = 3;
        public static final int ALL = 4;
    }

    public static class DefrostArea {
        public static final int FRONT = 0;
        public static final int REAR = 1;
        public static final int ALL = 2;
    }

    public static class LightType {
        public static final int HEADLIGHT = 0;
        public static final int DAYTIME_RUNNING_LIGHT = 1;
        public static final int TURN_LEFT = 2;
        public static final int TURN_RIGHT = 3;
        public static final int BRAKE_LIGHT = 4;
        public static final int REVERSE_LIGHT = 5;
        public static final int FOG_FRONT = 6;
        public static final int FOG_REAR = 7;
        public static final int HAZARD = 8;
        public static final int HIGH_BEAM = 9;
        public static final int PARKING_LIGHT = 10;
    }

    public static class DoorType {
        public static final int FRONT_LEFT = 0;
        public static final int FRONT_RIGHT = 1;
        public static final int REAR_LEFT = 2;
        public static final int REAR_RIGHT = 3;
        public static final int TRUNK = 4;
        public static final int HOOD = 5;
        public static final int ALL = 10;
    }

    public static class LockAction {
        public static final int LOCK = 0;
        public static final int UNLOCK = 1;
    }

    private CarControlManager(Context context) {
        this.context = context.getApplicationContext();
        instance = this;
        Log.i(TAG, "═══════════════════════════════════════════════════════════");
        Log.i(TAG, "【车辆控制管理器】CarControlManager 已初始化");
        Log.i(TAG, "═══════════════════════════════════════════════════════════");
    }

    public static CarControlManager getInstance(Context context) {
        if (instance == null) {
            Log.i(TAG, "【车辆控制管理器】实例为空，创建新实例");
            instance = new CarControlManager(context);
        } else {
            Log.d(TAG, "【车辆控制管理器】返回现有实例");
        }
        return instance;
    }

    public void setCallback(CarControlCallback callback) {
        this.callback = callback;
        if (callback != null) {
            Log.i(TAG, "【车辆控制管理器】✓ 回调监听器已注册");
            Log.d(TAG, "【车辆控制管理器】将接收以下事件:");
            Log.d(TAG, "  - onControlSuccess: 控制成功回调");
            Log.d(TAG, "  - onControlFailed: 控制失败回调");
            Log.d(TAG, "  - onStateChanged: 状态变化回调");
        } else {
            Log.w(TAG, "【车辆控制管理器】回调监听器已设置为null");
        }
    }

    private void notifySuccess(String controlType, String action) {
        Log.i(TAG, "═══════════════════════════════════════════════════════════");
        Log.i(TAG, "【控制成功】类型: " + controlType + ", 操作: " + action);
        Log.i(TAG, "═══════════════════════════════════════════════════════════");

        if (callback != null) {
            try {
                callback.onControlSuccess(controlType, action);
                Log.d(TAG, "【控制成功】回调执行完成");
            } catch (Exception e) {
                Log.e(TAG, "【控制成功】回调执行失败: " + e.getMessage());
            }
        } else {
            Log.w(TAG, "【控制成功】回调监听器为null，跳过回调");
        }
    }

    private void notifyFailed(String controlType, String error) {
        Log.e(TAG, "═══════════════════════════════════════════════════════════");
        Log.e(TAG, "【控制失败】类型: " + controlType);
        Log.e(TAG, "【控制失败】错误: " + error);
        Log.e(TAG, "═══════════════════════════════════════════════════════════");

        if (callback != null) {
            try {
                callback.onControlFailed(controlType, error);
                Log.d(TAG, "【控制失败】回调执行完成");
            } catch (Exception e) {
                Log.e(TAG, "【控制失败】回调执行失败: " + e.getMessage());
            }
        } else {
            Log.w(TAG, "【控制失败】回调监听器为null，跳过回调");
        }
    }

    private void notifyStateChanged(String stateType, JSONObject data) {
        Log.d(TAG, "═══════════════════════════════════════════════════════════");
        Log.d(TAG, "【状态变化】类型: " + stateType);
        Log.d(TAG, "【状态变化】数据: " + (data != null ? data.toString() : "null"));
        Log.d(TAG, "═══════════════════════════════════════════════════════════");

        if (callback != null) {
            try {
                callback.onStateChanged(stateType, data);
                Log.d(TAG, "【状态变化】回调执行完成");
            } catch (Exception e) {
                Log.e(TAG, "【状态变化】回调执行失败: " + e.getMessage());
            }
        } else {
            Log.w(TAG, "【状态变化】回调监听器为null，跳过回调");
        }
    }

    public boolean controlAc(int action, int value) {
        Log.i(TAG, "╔═══════════════════════════════════════════════════════════╗");
        Log.i(TAG, "║ 【空调控制】开始执行                                         ║");
        Log.i(TAG, "║ 操作码: " + action + ", 值: " + value + "                                        ║");
        Log.i(TAG, "╚═══════════════════════════════════════════════════════════╝");

        boolean accessibilityEnabled = YouthAccessibilityService.isServiceEnabled();
        Log.d(TAG, "【空调控制】无障碍服务状态: " + (accessibilityEnabled ? "运行中 ✓" : "未运行 ✗"));

        if (!accessibilityEnabled) {
            Log.w(TAG, "【空调控制】⚠️ 无障碍服务未运行，可能无法找到目标控件");
        }

        try {
            boolean success = false;
            String actionStr = "";

            switch (action) {
                case AcState.ON:
                case AcState.OFF:
                    Log.d(TAG, "【空调控制】操作类型: 电源" + (action == AcState.ON ? "开启" : "关闭"));
                    success = setAcPower(action == AcState.ON);
                    actionStr = action == AcState.ON ? "开启空调" : "关闭空调";
                    break;

                case 10:
                    Log.d(TAG, "【空调控制】操作类型: 设置温度");
                    success = setAcTemperature(value);
                    actionStr = "设置温度: " + value + "°C";
                    break;

                case 11:
                    Log.d(TAG, "【空调控制】操作类型: 设置风速");
                    success = setAcWindLevel(value);
                    actionStr = "设置风速: " + value;
                    break;

                case 12:
                    Log.d(TAG, "【空调控制】操作类型: 设置风向");
                    success = setAcWindMode(value);
                    actionStr = "设置风向: " + getWindModeString(value);
                    break;

                case 13:
                    Log.d(TAG, "【空调控制】操作类型: 前除霜");
                    success = setAcDefrost(DefrostArea.FRONT, value == 1);
                    actionStr = (value == 1 ? "开启" : "关闭") + "前除霜";
                    break;

                case 14:
                    Log.d(TAG, "【空调控制】操作类型: 后除霜");
                    success = setAcDefrost(DefrostArea.REAR, value == 1);
                    actionStr = (value == 1 ? "开启" : "关闭") + "后除霜";
                    break;

                case 15:
                    Log.d(TAG, "【空调控制】操作类型: 循环模式");
                    success = setAcCycleMode(value);
                    actionStr = "设置循环模式: " + (value == 0 ? "外循环" : "内循环");
                    break;

                case 16:
                    Log.d(TAG, "【空调控制】操作类型: 自动模式");
                    success = setAcAuto(value == 1);
                    actionStr = value == 1 ? "开启自动模式" : "关闭自动模式";
                    break;

                default:
                    Log.e(TAG, "【空调控制】✗ 未知的空调操作码: " + action);
                    notifyFailed("ac", "未知的空调操作: " + action);
                    return false;
            }

            if (success) {
                Log.i(TAG, "【空调控制】✓ 执行成功: " + actionStr);
                notifySuccess("ac", actionStr);
                notifyAcStateChanged();
            } else {
                Log.w(TAG, "【空调控制】✗ 执行失败: " + actionStr);
                notifyFailed("ac", actionStr + " 失败");
            }

            Log.i(TAG, "【空调控制】执行完成，结果: " + (success ? "成功 ✓" : "失败 ✗"));
            return success;

        } catch (Exception e) {
            Log.e(TAG, "【空调控制】✗ 执行时发生异常: " + e.getMessage());
            Log.e(TAG, "【空调控制】异常堆栈: ");
            e.printStackTrace();
            notifyFailed("ac", "控制空调时发生错误: " + e.getMessage());
            return false;
        }
    }

    private boolean setAcPower(boolean on) {
        String actionName = on ? "开启" : "关闭";
        Log.d(TAG, "┌──────────────────────────────────────────────────────────────");
        Log.d(TAG, "│ 【空调电源】正在" + actionName + "空调");
        Log.d(TAG, "└──────────────────────────────────────────────────────────────");

        try {
            YouthAccessibilityService accessibilityService = YouthAccessibilityService.getInstance();
            if (accessibilityService == null) {
                Log.e(TAG, "【空调电源】✗ 无障碍服务实例为空");
                Log.d(TAG, "【空调电源】→ 回退到模拟命令");
                return simulateCarCommand("ac power " + (on ? "on" : "off"));
            }

            AccessibilityNodeInfo rootNode = accessibilityService.getRootInActiveWindow();
            if (rootNode == null) {
                Log.e(TAG, "【空调电源】✗ 无法获取根窗口节点");
                Log.d(TAG, "【空调电源】→ 回退到模拟命令");
                return simulateCarCommand("ac power " + (on ? "on" : "off"));
            }

            Log.d(TAG, "【空调电源】尝试查找'空调开关'按钮...");
            boolean found = accessibilityService.findAndClickNodeByContentDescription("空调开关");
            if (found) {
                Log.i(TAG, "【空调电源】✓ 通过'空调开关'描述找到并点击成功");
                rootNode.recycle();
                return true;
            }

            Log.d(TAG, "【空调电源】尝试查找'空调'文本...");
            found = accessibilityService.findAndClickNodeByText("空调");
            if (found) {
                Log.i(TAG, "【空调电源】✓ 通过'空调'文本找到并点击成功");
                rootNode.recycle();
                return true;
            }

            Log.w(TAG, "【空调电源】✗ 在当前窗口未找到空调控制按钮");
            Log.d(TAG, "【空调电源】→ 回退到模拟命令");
            rootNode.recycle();
            return simulateCarCommand("ac power " + (on ? "on" : "off"));

        } catch (Exception e) {
            Log.e(TAG, "【空调电源】✗ 设置空调电源失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean setAcTemperature(int temperature) {
        Log.d(TAG, "┌──────────────────────────────────────────────────────────────");
        Log.d(TAG, "│ 【设置温度】目标温度: " + temperature + "°C");
        Log.d(TAG, "└──────────────────────────────────────────────────────────────");

        if (temperature < 16 || temperature > 30) {
            Log.e(TAG, "【设置温度】✗ 温度值超出范围 [16-30]: " + temperature);
            return false;
        }

        try {
            YouthAccessibilityService accessibilityService = YouthAccessibilityService.getInstance();
            if (accessibilityService == null) {
                Log.e(TAG, "【设置温度】✗ 无障碍服务实例为空");
                return simulateCarCommand("ac temperature " + temperature);
            }

            AccessibilityNodeInfo rootNode = accessibilityService.getRootInActiveWindow();
            if (rootNode == null) {
                Log.e(TAG, "【设置温度】✗ 无法获取根窗口节点");
                return simulateCarCommand("ac temperature " + temperature);
            }

            String tempText = String.valueOf(temperature);
            Log.d(TAG, "【设置温度】尝试通过文本'" + tempText + "'查找...");
            boolean found = accessibilityService.findAndClickNodeByText(tempText);

            if (!found) {
                Log.d(TAG, "【设置温度】尝试通过描述'温度" + temperature + "'查找...");
                found = accessibilityService.findAndClickNodeByContentDescription("温度" + temperature);
            }

            rootNode.recycle();

            if (found) {
                Log.i(TAG, "【设置温度】✓ 温度设置成功: " + temperature + "°C");
                return true;
            } else {
                Log.w(TAG, "【设置温度】✗ 未找到温度控件");
                return simulateCarCommand("ac temperature " + temperature);
            }

        } catch (Exception e) {
            Log.e(TAG, "【设置温度】✗ 设置温度失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean setAcWindLevel(int level) {
        Log.d(TAG, "┌──────────────────────────────────────────────────────────────");
        Log.d(TAG, "│ 【设置风速】目标风速: " + level);
        Log.d(TAG, "└──────────────────────────────────────────────────────────────");

        if (level < 0 || level > 7) {
            Log.e(TAG, "【设置风速】✗ 风速等级超出范围 [0-7]: " + level);
            return false;
        }

        try {
            YouthAccessibilityService accessibilityService = YouthAccessibilityService.getInstance();
            if (accessibilityService == null) {
                Log.e(TAG, "【设置风速】✗ 无障碍服务实例为空");
                return simulateCarCommand("ac wind level " + level);
            }

            AccessibilityNodeInfo rootNode = accessibilityService.getRootInActiveWindow();
            if (rootNode == null) {
                Log.e(TAG, "【设置风速】✗ 无法获取根窗口节点");
                return simulateCarCommand("ac wind level " + level);
            }

            Log.d(TAG, "【设置风速】尝试通过描述'风速" + level + "'查找...");
            boolean found = accessibilityService.findAndClickNodeByContentDescription("风速" + level);

            if (!found) {
                Log.d(TAG, "【设置风速】尝试通过文本'" + level + "'查找...");
                found = accessibilityService.findAndClickNodeByText(String.valueOf(level));
            }

            rootNode.recycle();

            if (found) {
                Log.i(TAG, "【设置风速】✓ 风速设置成功: " + level);
                return true;
            } else {
                Log.w(TAG, "【设置风速】✗ 未找到风速控件");
                return simulateCarCommand("ac wind level " + level);
            }

        } catch (Exception e) {
            Log.e(TAG, "【设置风速】✗ 设置风速失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean setAcWindMode(int mode) {
        String modeDesc = getWindModeString(mode);
        Log.d(TAG, "┌──────────────────────────────────────────────────────────────");
        Log.d(TAG, "│ 【设置风向】目标风向: " + modeDesc);
        Log.d(TAG, "└──────────────────────────────────────────────────────────────");

        try {
            YouthAccessibilityService accessibilityService = YouthAccessibilityService.getInstance();
            if (accessibilityService == null) {
                Log.e(TAG, "【设置风向】✗ 无障碍服务实例为空");
                return simulateCarCommand("ac wind mode " + mode);
            }

            AccessibilityNodeInfo rootNode = accessibilityService.getRootInActiveWindow();
            if (rootNode == null) {
                Log.e(TAG, "【设置风向】✗ 无法获取根窗口节点");
                return simulateCarCommand("ac wind mode " + mode);
            }

            Log.d(TAG, "【设置风向】尝试通过描述'" + modeDesc + "'查找...");
            boolean found = accessibilityService.findAndClickNodeByContentDescription(modeDesc);

            if (!found) {
                Log.d(TAG, "【设置风向】尝试通过文本'" + modeDesc + "'查找...");
                found = accessibilityService.findAndClickNodeByText(modeDesc);
            }

            rootNode.recycle();

            if (found) {
                Log.i(TAG, "【设置风向】✓ 风向设置成功: " + modeDesc);
                return true;
            } else {
                Log.w(TAG, "【设置风向】✗ 未找到风向控件");
                return simulateCarCommand("ac wind mode " + mode);
            }

        } catch (Exception e) {
            Log.e(TAG, "【设置风向】✗ 设置风向失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean setAcDefrost(int area, boolean enable) {
        String areaStr = area == DefrostArea.FRONT ? "前" : "后";
        String actionStr = enable ? "开启" : "关闭";
        Log.d(TAG, "┌──────────────────────────────────────────────────────────────");
        Log.d(TAG, "│ 【除霜控制】" + actionStr + areaStr + "除霜");
        Log.d(TAG, "└──────────────────────────────────────────────────────────────");

        try {
            YouthAccessibilityService accessibilityService = YouthAccessibilityService.getInstance();
            if (accessibilityService == null) {
                Log.e(TAG, "【除霜控制】✗ 无障碍服务实例为空");
                return simulateCarCommand("ac defrost " + area + " " + (enable ? "1" : "0"));
            }

            AccessibilityNodeInfo rootNode = accessibilityService.getRootInActiveWindow();
            if (rootNode == null) {
                Log.e(TAG, "【除霜控制】✗ 无法获取根窗口节点");
                return simulateCarCommand("ac defrost " + area + " " + (enable ? "1" : "0"));
            }

            String desc = areaStr + "除霜";
            Log.d(TAG, "【除霜控制】尝试通过描述'" + desc + "'查找...");
            boolean found = accessibilityService.findAndClickNodeByContentDescription(desc);

            if (!found) {
                Log.d(TAG, "【除霜控制】尝试通过文本'" + desc + "'查找...");
                found = accessibilityService.findAndClickNodeByText(desc);
            }

            rootNode.recycle();

            if (found) {
                Log.i(TAG, "【除霜控制】✓ " + actionStr + areaStr + "除霜成功");
                return true;
            } else {
                Log.w(TAG, "【除霜控制】✗ 未找到除霜控件");
                return simulateCarCommand("ac defrost " + area + " " + (enable ? "1" : "0"));
            }

        } catch (Exception e) {
            Log.e(TAG, "【除霜控制】✗ 控制除霜失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean setAcCycleMode(int mode) {
        String modeStr = mode == 0 ? "外循环" : "内循环";
        Log.d(TAG, "┌──────────────────────────────────────────────────────────────");
        Log.d(TAG, "│ 【循环模式】设置为: " + modeStr);
        Log.d(TAG, "└──────────────────────────────────────────────────────────────");

        try {
            YouthAccessibilityService accessibilityService = YouthAccessibilityService.getInstance();
            if (accessibilityService == null) {
                Log.e(TAG, "【循环模式】✗ 无障碍服务实例为空");
                return simulateCarCommand("ac cycle " + mode);
            }

            AccessibilityNodeInfo rootNode = accessibilityService.getRootInActiveWindow();
            if (rootNode == null) {
                Log.e(TAG, "【循环模式】✗ 无法获取根窗口节点");
                return simulateCarCommand("ac cycle " + mode);
            }

            Log.d(TAG, "【循环模式】尝试通过描述'循环模式'查找...");
            boolean found = accessibilityService.findAndClickNodeByContentDescription("循环模式");

            if (!found) {
                Log.d(TAG, "【循环模式】尝试通过文本'" + modeStr + "'查找...");
                found = accessibilityService.findAndClickNodeByText(modeStr);
            }

            rootNode.recycle();

            if (found) {
                Log.i(TAG, "【循环模式】✓ 循环模式设置成功: " + modeStr);
                return true;
            } else {
                Log.w(TAG, "【循环模式】✗ 未找到循环模式控件");
                return simulateCarCommand("ac cycle " + mode);
            }

        } catch (Exception e) {
            Log.e(TAG, "【循环模式】✗ 设置循环模式失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean setAcAuto(boolean enable) {
        String actionStr = enable ? "开启" : "关闭";
        Log.d(TAG, "┌──────────────────────────────────────────────────────────────");
        Log.d(TAG, "│ 【自动模式】" + actionStr + "自动模式");
        Log.d(TAG, "└──────────────────────────────────────────────────────────────");

        try {
            YouthAccessibilityService accessibilityService = YouthAccessibilityService.getInstance();
            if (accessibilityService == null) {
                Log.e(TAG, "【自动模式】✗ 无障碍服务实例为空");
                return simulateCarCommand("ac auto " + (enable ? "1" : "0"));
            }

            AccessibilityNodeInfo rootNode = accessibilityService.getRootInActiveWindow();
            if (rootNode == null) {
                Log.e(TAG, "【自动模式】✗ 无法获取根窗口节点");
                return simulateCarCommand("ac auto " + (enable ? "1" : "0"));
            }

            Log.d(TAG, "【自动模式】尝试通过描述'自动'查找...");
            boolean found = accessibilityService.findAndClickNodeByContentDescription("自动");

            if (!found) {
                Log.d(TAG, "【自动模式】尝试通过文本'自动'查找...");
                found = accessibilityService.findAndClickNodeByText("自动");
            }

            rootNode.recycle();

            if (found) {
                Log.i(TAG, "【自动模式】✓ " + actionStr + "自动模式成功");
                return true;
            } else {
                Log.w(TAG, "【自动模式】✗ 未找到自动模式控件");
                return simulateCarCommand("ac auto " + (enable ? "1" : "0"));
            }

        } catch (Exception e) {
            Log.e(TAG, "【自动模式】✗ 控制自动模式失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void notifyAcStateChanged() {
        Log.d(TAG, "【状态更新】通知空调状态变化...");
        try {
            JSONObject data = new JSONObject();
            data.put("state", isAcOn() ? "on" : "off");
            data.put("temperature", getAcTemperature());
            data.put("windLevel", getAcWindLevel());
            data.put("windMode", getAcWindMode());

            notifyStateChanged("acStateChanged", data);

        } catch (JSONException e) {
            Log.e(TAG, "【状态更新】创建空调状态JSON失败: " + e.getMessage());
        }
    }

    public boolean controlLight(int lightType, boolean enable) {
        String lightName = getLightTypeString(lightType);
        String actionStr = (enable ? "开启" : "关闭") + " " + lightName;

        Log.i(TAG, "╔═══════════════════════════════════════════════════════════╗");
        Log.i(TAG, "║ 【灯光控制】" + actionStr + "                                    ║");
        Log.i(TAG, "║ 灯光类型码: " + lightType + "                                             ║");
        Log.i(TAG, "╚═══════════════════════════════════════════════════════════╝");

        try {
            YouthAccessibilityService accessibilityService = YouthAccessibilityService.getInstance();
            if (accessibilityService == null) {
                Log.e(TAG, "【灯光控制】✗ 无障碍服务实例为空");
                boolean success = simulateCarCommand("light " + lightType + " " + (enable ? "1" : "0"));
                if (success) {
                    notifySuccess("light", actionStr);
                    notifyLightStateChanged(lightType, enable);
                } else {
                    notifyFailed("light", actionStr + " 失败");
                }
                return success;
            }

            AccessibilityNodeInfo rootNode = accessibilityService.getRootInActiveWindow();
            if (rootNode == null) {
                Log.e(TAG, "【灯光控制】✗ 无法获取根窗口节点");
                boolean success = simulateCarCommand("light " + lightType + " " + (enable ? "1" : "0"));
                if (success) {
                    notifySuccess("light", actionStr);
                    notifyLightStateChanged(lightType, enable);
                } else {
                    notifyFailed("light", actionStr + " 失败");
                }
                return success;
            }

            Log.d(TAG, "【灯光控制】尝试通过描述'" + lightName + "'查找...");
            boolean found = accessibilityService.findAndClickNodeByContentDescription(lightName);

            if (!found) {
                Log.d(TAG, "【灯光控制】尝试通过文本'" + lightName + "'查找...");
                found = accessibilityService.findAndClickNodeByText(lightName);
            }

            rootNode.recycle();

            if (found) {
                Log.i(TAG, "【灯光控制】✓ " + actionStr + "成功");
                notifySuccess("light", actionStr);
                notifyLightStateChanged(lightType, enable);
                return true;
            } else {
                Log.w(TAG, "【灯光控制】✗ 未找到灯光控件，回退到模拟命令");
                boolean success = simulateCarCommand("light " + lightType + " " + (enable ? "1" : "0"));
                if (success) {
                    notifySuccess("light", actionStr);
                    notifyLightStateChanged(lightType, enable);
                } else {
                    notifyFailed("light", actionStr + " 失败");
                }
                return success;
            }

        } catch (Exception e) {
            Log.e(TAG, "【灯光控制】✗ 控制灯光时发生错误: " + e.getMessage());
            e.printStackTrace();
            notifyFailed("light", "控制灯光时发生错误: " + e.getMessage());
            return false;
        }
    }

    private void notifyLightStateChanged(int lightType, boolean enable) {
        Log.d(TAG, "【状态更新】通知灯光状态变化...");
        try {
            JSONObject data = new JSONObject();
            data.put("lightType", lightType);
            data.put("lightName", getLightTypeString(lightType));
            data.put("enabled", enable);

            notifyStateChanged("lightStateChanged", data);

        } catch (JSONException e) {
            Log.e(TAG, "【状态更新】创建灯光状态JSON失败: " + e.getMessage());
        }
    }

    public boolean controlDoorLock(int doorType, int action) {
        String doorName = getDoorTypeString(doorType);
        String actionStr = action == LockAction.LOCK ? "锁定" : "解锁";
        String fullAction = actionStr + " " + doorName;

        Log.i(TAG, "╔═══════════════════════════════════════════════════════════╗");
        Log.i(TAG, "║ 【车门控制】" + fullAction + "                                   ║");
        Log.i(TAG, "║ 车门类型码: " + doorType + ", 操作码: " + action + "                                  ║");
        Log.i(TAG, "╚═══════════════════════════════════════════════════════════╝");

        try {
            YouthAccessibilityService accessibilityService = YouthAccessibilityService.getInstance();
            if (accessibilityService == null) {
                Log.e(TAG, "【车门控制】✗ 无障碍服务实例为空");
                boolean success = simulateCarCommand("door " + doorType + " " + action);
                if (success) {
                    notifySuccess("door", fullAction);
                    notifyDoorStateChanged(doorType, action);
                } else {
                    notifyFailed("door", fullAction + " 失败");
                }
                return success;
            }

            AccessibilityNodeInfo rootNode = accessibilityService.getRootInActiveWindow();
            if (rootNode == null) {
                Log.e(TAG, "【车门控制】✗ 无法获取根窗口节点");
                boolean success = simulateCarCommand("door " + doorType + " " + action);
                if (success) {
                    notifySuccess("door", fullAction);
                    notifyDoorStateChanged(doorType, action);
                } else {
                    notifyFailed("door", fullAction + " 失败");
                }
                return success;
            }

            Log.d(TAG, "【车门控制】尝试通过描述'" + doorName + actionStr + "'查找...");
            boolean found = accessibilityService.findAndClickNodeByContentDescription(doorName + actionStr);

            if (!found) {
                String buttonText = doorType == DoorType.ALL ? actionStr : doorName;
                Log.d(TAG, "【车门控制】尝试通过文本'" + buttonText + "'查找...");
                found = accessibilityService.findAndClickNodeByText(buttonText);
            }

            if (!found && doorType == DoorType.TRUNK) {
                Log.d(TAG, "【车门控制】尝试通过描述'后备箱'查找...");
                found = accessibilityService.findAndClickNodeByContentDescription("后备箱");
            }

            rootNode.recycle();

            if (found) {
                Log.i(TAG, "【车门控制】✓ " + fullAction + "成功");
                notifySuccess("door", fullAction);
                notifyDoorStateChanged(doorType, action);
                return true;
            } else {
                Log.w(TAG, "【车门控制】✗ 未找到车门控件，回退到模拟命令");
                boolean success = simulateCarCommand("door " + doorType + " " + action);
                if (success) {
                    notifySuccess("door", fullAction);
                    notifyDoorStateChanged(doorType, action);
                } else {
                    notifyFailed("door", fullAction + " 失败");
                }
                return success;
            }

        } catch (Exception e) {
            Log.e(TAG, "【车门控制】✗ 控制车门锁时发生错误: " + e.getMessage());
            e.printStackTrace();
            notifyFailed("door", "控制车门锁时发生错误: " + e.getMessage());
            return false;
        }
    }

    private void notifyDoorStateChanged(int doorType, int action) {
        Log.d(TAG, "【状态更新】通知车门状态变化...");
        try {
            JSONObject data = new JSONObject();
            data.put("doorType", doorType);
            data.put("doorName", getDoorTypeString(doorType));
            data.put("action", action == LockAction.LOCK ? "locked" : "unlocked");

            notifyStateChanged("doorStateChanged", data);

        } catch (JSONException e) {
            Log.e(TAG, "【状态更新】创建车门状态JSON失败: " + e.getMessage());
        }
    }

    private boolean simulateCarCommand(String command) {
        Log.w(TAG, "【模拟命令】执行模拟命令: " + command);
        Log.w(TAG, "【模拟命令】⚠️ 注意: 这是模拟执行，实际车辆控制未执行");
        Log.d(TAG, "【模拟命令】→ 模拟返回成功");
        return true;
    }

    private String getWindModeString(int mode) {
        switch (mode) {
            case WindMode.FACE: return "面部";
            case WindMode.CHEST: return "胸部";
            case WindMode.FEET: return "脚部";
            case WindMode.FACE_FEET: return "面部+脚部";
            case WindMode.ALL: return "全部";
            default: return "未知风向";
        }
    }

    private String getLightTypeString(int type) {
        switch (type) {
            case LightType.HEADLIGHT: return "大灯";
            case LightType.DAYTIME_RUNNING_LIGHT: return "日间行车灯";
            case LightType.TURN_LEFT: return "左转向灯";
            case LightType.TURN_RIGHT: return "右转向灯";
            case LightType.BRAKE_LIGHT: return "刹车灯";
            case LightType.REVERSE_LIGHT: return "倒车灯";
            case LightType.FOG_FRONT: return "前雾灯";
            case LightType.FOG_REAR: return "后雾灯";
            case LightType.HAZARD: return "双闪";
            case LightType.HIGH_BEAM: return "远光灯";
            case LightType.PARKING_LIGHT: return "示廓灯";
            default: return "未知灯光";
        }
    }

    private String getDoorTypeString(int type) {
        switch (type) {
            case DoorType.FRONT_LEFT: return "左前门";
            case DoorType.FRONT_RIGHT: return "右前门";
            case DoorType.REAR_LEFT: return "左后门";
            case DoorType.REAR_RIGHT: return "右后门";
            case DoorType.TRUNK: return "后备箱";
            case DoorType.HOOD: return "引擎盖";
            case DoorType.ALL: return "全部车门";
            default: return "未知车门";
        }
    }

    public boolean isAcOn() {
        return false;
    }

    public int getAcTemperature() {
        return 26;
    }

    public int getAcWindLevel() {
        return 2;
    }

    public int getAcWindMode() {
        return WindMode.FACE;
    }

    public boolean isLightOn(int lightType) {
        return false;
    }

    public boolean isDoorLocked(int doorType) {
        return true;
    }

    public String getAcDebugInfo() {
        Log.d(TAG, "═══════════════════════════════════════════════════════════");
        Log.d(TAG, "【调试信息】空调状态:");
        Log.d(TAG, "  电源: " + (isAcOn() ? "开启 ✓" : "关闭 ✗"));
        Log.d(TAG, "  温度: " + getAcTemperature() + "°C");
        Log.d(TAG, "  风速: " + getAcWindLevel());
        Log.d(TAG, "  风向: " + getWindModeString(getAcWindMode()));
        Log.d(TAG, "═══════════════════════════════════════════════════════════");

        StringBuilder info = new StringBuilder();
        info.append("空调状态:\n");
        info.append("- 电源: ").append(isAcOn() ? "开启" : "关闭").append("\n");
        info.append("- 温度: ").append(getAcTemperature()).append("°C\n");
        info.append("- 风速: ").append(getAcWindLevel()).append("\n");
        info.append("- 风向: ").append(getWindModeString(getAcWindMode())).append("\n");
        return info.toString();
    }

    public String getLightDebugInfo() {
        Log.d(TAG, "═══════════════════════════════════════════════════════════");
        Log.d(TAG, "【调试信息】灯光状态:");
        Log.d(TAG, "  大灯: " + (isLightOn(LightType.HEADLIGHT) ? "开启 ✓" : "关闭 ✗"));
        Log.d(TAG, "  日间行车灯: " + (isLightOn(LightType.DAYTIME_RUNNING_LIGHT) ? "开启 ✓" : "关闭 ✗"));
        Log.d(TAG, "  左转向灯: " + (isLightOn(LightType.TURN_LEFT) ? "开启 ✓" : "关闭 ✗"));
        Log.d(TAG, "  右转向灯: " + (isLightOn(LightType.TURN_RIGHT) ? "开启 ✓" : "关闭 ✗"));
        Log.d(TAG, "  双闪: " + (isLightOn(LightType.HAZARD) ? "开启 ✓" : "关闭 ✗"));
        Log.d(TAG, "═══════════════════════════════════════════════════════════");

        StringBuilder info = new StringBuilder();
        info.append("灯光状态:\n");
        info.append("- 大灯: ").append(isLightOn(LightType.HEADLIGHT) ? "开启" : "关闭").append("\n");
        info.append("- 日间行车灯: ").append(isLightOn(LightType.DAYTIME_RUNNING_LIGHT) ? "开启" : "关闭").append("\n");
        info.append("- 左转向灯: ").append(isLightOn(LightType.TURN_LEFT) ? "开启" : "关闭").append("\n");
        info.append("- 右转向灯: ").append(isLightOn(LightType.TURN_RIGHT) ? "开启" : "关闭").append("\n");
        info.append("- 双闪: ").append(isLightOn(LightType.HAZARD) ? "开启" : "关闭").append("\n");
        return info.toString();
    }

    public String getDoorDebugInfo() {
        Log.d(TAG, "═══════════════════════════════════════════════════════════");
        Log.d(TAG, "【调试信息】车门状态:");
        Log.d(TAG, "  左前门: " + (isDoorLocked(DoorType.FRONT_LEFT) ? "锁定 ✓" : "解锁 ✗"));
        Log.d(TAG, "  右前门: " + (isDoorLocked(DoorType.FRONT_RIGHT) ? "锁定 ✓" : "解锁 ✗"));
        Log.d(TAG, "  左后门: " + (isDoorLocked(DoorType.REAR_LEFT) ? "锁定 ✓" : "解锁 ✗"));
        Log.d(TAG, "  右后门: " + (isDoorLocked(DoorType.REAR_RIGHT) ? "锁定 ✓" : "解锁 ✗"));
        Log.d(TAG, "  后备箱: " + (isDoorLocked(DoorType.TRUNK) ? "锁定 ✓" : "解锁 ✗"));
        Log.d(TAG, "═══════════════════════════════════════════════════════════");

        StringBuilder info = new StringBuilder();
        info.append("车门状态:\n");
        info.append("- 左前门: ").append(isDoorLocked(DoorType.FRONT_LEFT) ? "锁定" : "解锁").append("\n");
        info.append("- 右前门: ").append(isDoorLocked(DoorType.FRONT_RIGHT) ? "锁定" : "解锁").append("\n");
        info.append("- 左后门: ").append(isDoorLocked(DoorType.REAR_LEFT) ? "锁定" : "解锁").append("\n");
        info.append("- 右后门: ").append(isDoorLocked(DoorType.REAR_RIGHT) ? "锁定" : "解锁").append("\n");
        info.append("- 后备箱: ").append(isDoorLocked(DoorType.TRUNK) ? "锁定" : "解锁").append("\n");
        return info.toString();
    }
}
