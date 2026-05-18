package cn.youth996.ui.carcontrol;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import cn.youth996.ui.core.YouthAccessibilityService;

import java.util.List;

public class CarControlHelper {
    private static final String TAG = "CarControlHelper";
    private Context context;
    private static CarControlHelper instance;

    public interface CarControlListener {
        void onAcStateChanged(boolean isOn, int temperature, int windLevel);
        void onLightStateChanged(int lightType, boolean isOn);
        void onDoorStateChanged(int doorType, boolean isLocked);
        void onCarControlResult(boolean success, String message);
    }

    private CarControlListener listener;

    private CarControlHelper(Context context) {
        this.context = context.getApplicationContext();
        instance = this;
        Log.i(TAG, "═══════════════════════════════════════════════════════════");
        Log.i(TAG, "【车辆控制辅助】CarControlHelper 已初始化");
        Log.i(TAG, "═══════════════════════════════════════════════════════════");
    }

    public static CarControlHelper getInstance(Context context) {
        if (instance == null) {
            Log.i(TAG, "【车辆控制辅助】实例为空，创建新实例");
            instance = new CarControlHelper(context);
        } else {
            Log.d(TAG, "【车辆控制辅助】返回现有实例");
        }
        return instance;
    }

    public void setListener(CarControlListener listener) {
        this.listener = listener;
        if (listener != null) {
            Log.i(TAG, "【车辆控制辅助】✓ 监听器已注册");
        } else {
            Log.w(TAG, "【车辆控制辅助】监听器已设置为null");
        }
    }

    public boolean isAccessibilityServiceEnabled() {
        Log.d(TAG, "【无障碍检查】检查无障碍服务状态...");

        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) {
            Log.e(TAG, "【无障碍检查】✗ 无法获取AccessibilityManager");
            return false;
        }

        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_GENERIC
        );

        String serviceName = YouthAccessibilityService.class.getName();
        Log.d(TAG, "【无障碍检查】正在查找服务: " + serviceName);
        Log.d(TAG, "【无障碍检查】已启用服务数量: " + enabledServices.size());

        for (AccessibilityServiceInfo service : enabledServices) {
            if (service.getId() != null) {
                Log.d(TAG, "【无障碍检查】发现已启用服务: " + service.getId());
                if (service.getId().contains(serviceName)) {
                    Log.i(TAG, "【无障碍检查】✓ 找到目标无障碍服务: " + service.getId());
                    return true;
                }
            }
        }

        Log.w(TAG, "【无障碍检查】✗ 未找到目标无障碍服务");
        return false;
    }

    public boolean toggleAcPower() {
        Log.i(TAG, "╔═══════════════════════════════════════════════════════════╗");
        Log.i(TAG, "║ 【空调控制】切换空调电源                                     ║");
        Log.i(TAG, "╚═══════════════════════════════════════════════════════════╝");

        if (!isAccessibilityServiceEnabled()) {
            Log.e(TAG, "【空调控制】✗ 无障碍服务未启用，无法控制空调");
            notifyResult(false, "无障碍服务未启用");
            return false;
        }

        try {
            YouthAccessibilityService accessibilityService = YouthAccessibilityService.getInstance();
            if (accessibilityService == null) {
                Log.e(TAG, "【空调控制】✗ 无障碍服务实例为空");
                notifyResult(false, "无障碍服务实例为空");
                return false;
            }

            Log.d(TAG, "【空调控制】尝试查找'空调开关'按钮...");
            boolean clicked = accessibilityService.findAndClickNodeByContentDescription("空调开关");

            if (!clicked) {
                Log.d(TAG, "【空调控制】尝试查找'空调'按钮...");
                clicked = accessibilityService.findAndClickNodeByContentDescription("空调");
            }

            if (!clicked) {
                Log.d(TAG, "【空调控制】尝试查找'空调'文本...");
                clicked = accessibilityService.findAndClickNodeByText("空调");
            }

            if (clicked) {
                Log.i(TAG, "【空调控制】✓ 空调电源切换成功");
                notifyResult(true, "空调电源已切换");
                return true;
            } else {
                Log.e(TAG, "【空调控制】✗ 未找到空调控制按钮");
                notifyResult(false, "未找到空调控制按钮");
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "【空调控制】✗ 切换空调电源失败: " + e.getMessage());
            e.printStackTrace();
            notifyResult(false, "切换失败: " + e.getMessage());
            return false;
        }
    }

    public boolean adjustTemperature(boolean increase) {
        Log.d(TAG, "调整温度: " + (increase ? "升高" : "降低"));

        if (!isAccessibilityServiceEnabled()) {
            Log.w(TAG, "无障碍服务未启用");
            notifyResult(false, "无障碍服务未启用");
            return false;
        }

        try {
            String buttonDesc = increase ? "温度增加" : "温度减少";
            boolean clicked = YouthAccessibilityService.getInstance().findAndClickNodeByContentDescription(buttonDesc);

            if (!clicked) {
                clicked = YouthAccessibilityService.getInstance().findAndClickNodeByContentDescription(increase ? "+" : "-");
            }

            if (!clicked) {
                clicked = YouthAccessibilityService.getInstance().findAndClickNodeByText(increase ? "+" : "-");
            }

            if (clicked) {
                Log.d(TAG, "温度调整成功");
                notifyResult(true, "温度已" + (increase ? "升高" : "降低"));
                return true;
            } else {
                Log.w(TAG, "未找到温度调节按钮");
                notifyResult(false, "未找到温度调节按钮");
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "调整温度失败: " + e.getMessage());
            notifyResult(false, "调整失败: " + e.getMessage());
            return false;
        }
    }

    public boolean adjustWindLevel(boolean increase) {
        Log.d(TAG, "调整风速: " + (increase ? "增强" : "减弱"));

        if (!isAccessibilityServiceEnabled()) {
            notifyResult(false, "无障碍服务未启用");
            return false;
        }

        try {
            String buttonDesc = increase ? "风速增加" : "风速减少";
            boolean clicked = YouthAccessibilityService.getInstance().findAndClickNodeByContentDescription(buttonDesc);

            if (!clicked) {
                clicked = YouthAccessibilityService.getInstance().findAndClickNodeByText("风速");
            }

            if (clicked) {
                Log.d(TAG, "风速调整成功");
                notifyResult(true, "风速已" + (increase ? "增强" : "减弱"));
                return true;
            } else {
                Log.w(TAG, "未找到风速调节按钮");
                notifyResult(false, "未找到风速调节按钮");
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "调整风速失败: " + e.getMessage());
            notifyResult(false, "调整失败: " + e.getMessage());
            return false;
        }
    }

    public boolean controlLight(int lightType, boolean enable) {
        Log.d(TAG, "控制灯光: type=" + lightType + ", enable=" + enable);

        if (!isAccessibilityServiceEnabled()) {
            notifyResult(false, "无障碍服务未启用");
            return false;
        }

        try {
            String lightName = getLightTypeString(lightType);
            boolean clicked = YouthAccessibilityService.getInstance().findAndClickNodeByContentDescription(lightName);

            if (!clicked) {
                clicked = YouthAccessibilityService.getInstance().findAndClickNodeByText(lightName);
            }

            if (clicked) {
                Log.d(TAG, lightName + (enable ? "开启" : "关闭") + "成功");
                notifyResult(true, lightName + "已" + (enable ? "开启" : "关闭"));
                notifyLightStateChanged(lightType, enable);
                return true;
            } else {
                Log.w(TAG, "未找到" + lightName + "控制按钮");
                notifyResult(false, "未找到" + lightName + "控制按钮");
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "控制灯光失败: " + e.getMessage());
            notifyResult(false, "控制失败: " + e.getMessage());
            return false;
        }
    }

    public boolean controlDoorLock(int doorType, boolean lock) {
        Log.d(TAG, "控制车门锁: door=" + doorType + ", lock=" + lock);

        if (!isAccessibilityServiceEnabled()) {
            notifyResult(false, "无障碍服务未启用");
            return false;
        }

        try {
            String doorName = getDoorTypeString(doorType);
            String buttonDesc = lock ? "锁定" : "解锁";

            boolean clicked = YouthAccessibilityService.getInstance().findAndClickNodeByContentDescription(doorName + buttonDesc);

            if (!clicked && doorType == 10) {
                clicked = YouthAccessibilityService.getInstance().findAndClickNodeByContentDescription(buttonDesc);
            }

            if (!clicked) {
                clicked = YouthAccessibilityService.getInstance().findAndClickNodeByText(buttonDesc);
            }

            if (clicked) {
                Log.d(TAG, doorName + buttonDesc + "成功");
                notifyResult(true, doorName + "已" + buttonDesc);
                notifyDoorStateChanged(doorType, lock);
                return true;
            } else {
                Log.w(TAG, "未找到" + doorName + "控制按钮");
                notifyResult(false, "未找到" + doorName + "控制按钮");
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "控制车门锁失败: " + e.getMessage());
            notifyResult(false, "控制失败: " + e.getMessage());
            return false;
        }
    }

    public boolean controlDefrost(int area, boolean enable) {
        Log.d(TAG, "控制除霜: area=" + area + ", enable=" + enable);

        if (!isAccessibilityServiceEnabled()) {
            notifyResult(false, "无障碍服务未启用");
            return false;
        }

        try {
            String areaStr = area == 0 ? "前" : "后";
            String buttonDesc = areaStr + "除霜";

            boolean clicked = YouthAccessibilityService.getInstance().findAndClickNodeByContentDescription(buttonDesc);

            if (!clicked) {
                clicked = YouthAccessibilityService.getInstance().findAndClickNodeByText(buttonDesc);
            }

            if (clicked) {
                Log.d(TAG, buttonDesc + (enable ? "开启" : "关闭") + "成功");
                notifyResult(true, buttonDesc + "已" + (enable ? "开启" : "关闭"));
                return true;
            } else {
                Log.w(TAG, "未找到" + buttonDesc + "控制按钮");
                notifyResult(false, "未找到" + buttonDesc + "控制按钮");
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "控制除霜失败: " + e.getMessage());
            notifyResult(false, "控制失败: " + e.getMessage());
            return false;
        }
    }

    public boolean controlWindDirection(int mode) {
        Log.d(TAG, "控制风向: mode=" + mode);

        if (!isAccessibilityServiceEnabled()) {
            notifyResult(false, "无障碍服务未启用");
            return false;
        }

        try {
            String modeStr = getWindModeString(mode);
            boolean clicked = YouthAccessibilityService.getInstance().findAndClickNodeByContentDescription(modeStr);

            if (!clicked) {
                clicked = YouthAccessibilityService.getInstance().findAndClickNodeByText(modeStr);
            }

            if (clicked) {
                Log.d(TAG, "风向切换到" + modeStr + "成功");
                notifyResult(true, "风向已切换到" + modeStr);
                return true;
            } else {
                Log.w(TAG, "未找到风向控制按钮");
                notifyResult(false, "未找到风向控制按钮");
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "控制风向失败: " + e.getMessage());
            notifyResult(false, "控制失败: " + e.getMessage());
            return false;
        }
    }

    private String getLightTypeString(int type) {
        switch (type) {
            case 0: return "大灯";
            case 1: return "日间行车灯";
            case 2: return "左转向灯";
            case 3: return "右转向灯";
            case 4: return "刹车灯";
            case 5: return "倒车灯";
            case 6: return "前雾灯";
            case 7: return "后雾灯";
            case 8: return "双闪";
            case 9: return "远光灯";
            case 10: return "示廓灯";
            default: return "灯光";
        }
    }

    private String getDoorTypeString(int type) {
        switch (type) {
            case 0: return "左前门";
            case 1: return "右前门";
            case 2: return "左后门";
            case 3: return "右后门";
            case 4: return "后备箱";
            case 5: return "引擎盖";
            case 10: return "全部车门";
            default: return "车门";
        }
    }

    private String getWindModeString(int mode) {
        switch (mode) {
            case 0: return "面部";
            case 1: return "胸部";
            case 2: return "脚部";
            case 3: return "面部+脚部";
            case 4: return "全部";
            default: return "风向";
        }
    }

    private void notifyResult(boolean success, String message) {
        if (listener != null) {
            listener.onCarControlResult(success, message);
        }
    }

    private void notifyLightStateChanged(int lightType, boolean isOn) {
        if (listener != null) {
            listener.onLightStateChanged(lightType, isOn);
        }
    }

    private void notifyDoorStateChanged(int doorType, boolean isLocked) {
        if (listener != null) {
            listener.onDoorStateChanged(doorType, isLocked);
        }
    }
}
