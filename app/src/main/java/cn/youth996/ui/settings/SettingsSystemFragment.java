package cn.youth996.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.youth996.ui.R;
import cn.youth996.ui.adb.AdbProtocolClient;
import cn.youth996.ui.core.FloatingWindowManager;
import cn.youth996.ui.receiver.BootReceiver;

public class SettingsSystemFragment extends Fragment {

    private static final String PREFS_NAME = "system_settings";
    private static final String KEY_SHOW_STATUSBAR = "show_statusbar";
    private static final String KEY_SHOW_NAVBAR = "show_navbar";
    private static final String KEY_KEEP_SCREEN_ON = "keep_screen_on";
    private static final String KEY_WIFI = "wifi";
    private static final String KEY_BLUETOOTH = "bluetooth";
    private static final String KEY_MOBILE_DATA = "mobile_data";
    private static final String KEY_TOUCH_SOUND = "touch_sound";
    private static final String KEY_VIBRATION = "vibration";

    private Switch switchShowStatusbar;
    private Switch switchShowNavbar;
    private Switch switchKeepScreenOn;
    private Switch switchWifi;
    private Switch switchBluetooth;
    private Switch switchMobileData;
    private Switch switchTouchSound;
    private Switch switchVibration;
    private LinearLayout layoutAdbPermission;
    private LinearLayout layoutSetHome;
    private LinearLayout layoutFloatMapApp;
    private ImageView imgFloatMapIcon;
    private TextView textFloatMapPackage;

    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_system, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getContext() != null) {
            prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        }

        initViews(view);
        if (prefs != null) {
            loadSettings();
        }
        setupListeners();
    }

    private void initViews(View view) {
        switchShowStatusbar = view.findViewById(R.id.switch_show_statusbar);
        switchShowNavbar = view.findViewById(R.id.switch_show_navbar);
        switchKeepScreenOn = view.findViewById(R.id.switch_keep_screen_on);
        switchWifi = view.findViewById(R.id.switch_wifi);
        switchBluetooth = view.findViewById(R.id.switch_bluetooth);
        switchMobileData = view.findViewById(R.id.switch_mobile_data);
        switchTouchSound = view.findViewById(R.id.switch_touch_sound);
        switchVibration = view.findViewById(R.id.switch_vibration);
        layoutAdbPermission = view.findViewById(R.id.layout_adb_permission);
        layoutSetHome = view.findViewById(R.id.layout_set_home);
        layoutFloatMapApp = view.findViewById(R.id.layout_float_map_app);
        imgFloatMapIcon = view.findViewById(R.id.img_float_map_icon);
        textFloatMapPackage = view.findViewById(R.id.text_float_map_package);
    }

    private void loadSettings() {
        if (prefs == null) return;

        boolean showStatusbar = prefs.getBoolean(KEY_SHOW_STATUSBAR, true);
        boolean showNavbar = prefs.getBoolean(KEY_SHOW_NAVBAR, true);
        boolean keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, false);
        boolean wifi = prefs.getBoolean(KEY_WIFI, true);
        boolean bluetooth = prefs.getBoolean(KEY_BLUETOOTH, true);
        boolean mobileData = prefs.getBoolean(KEY_MOBILE_DATA, true);
        boolean touchSound = prefs.getBoolean(KEY_TOUCH_SOUND, true);
        boolean vibration = prefs.getBoolean(KEY_VIBRATION, true);

        if (switchShowStatusbar != null) switchShowStatusbar.setChecked(showStatusbar);
        if (switchShowNavbar != null) switchShowNavbar.setChecked(showNavbar);
        if (switchKeepScreenOn != null) switchKeepScreenOn.setChecked(keepScreenOn);
        if (switchWifi != null) switchWifi.setChecked(wifi);
        if (switchBluetooth != null) switchBluetooth.setChecked(bluetooth);
        if (switchMobileData != null) switchMobileData.setChecked(mobileData);
        if (switchTouchSound != null) switchTouchSound.setChecked(touchSound);
        if (switchVibration != null) switchVibration.setChecked(vibration);

        refreshFloatMapApp();
    }

    private void setupListeners() {
        if (switchShowStatusbar != null) {
            switchShowStatusbar.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (prefs != null) {
                    prefs.edit().putBoolean(KEY_SHOW_STATUSBAR, isChecked).apply();
                }
                if (getContext() != null) {
                    Toast.makeText(requireContext(), isChecked ? "已显示状态栏" : "已隐藏状态栏", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (switchShowNavbar != null) {
            switchShowNavbar.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (prefs != null) {
                    prefs.edit().putBoolean(KEY_SHOW_NAVBAR, isChecked).apply();
                }
                if (getContext() != null) {
                    Toast.makeText(requireContext(), isChecked ? "已显示导航栏" : "已隐藏导航栏", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (switchKeepScreenOn != null) {
            switchKeepScreenOn.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (prefs != null) {
                    prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, isChecked).apply();
                }
                if (getContext() != null) {
                    Toast.makeText(requireContext(), isChecked ? "已开启屏幕常亮" : "已关闭屏幕常亮", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (switchWifi != null) {
            switchWifi.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (prefs != null) {
                    prefs.edit().putBoolean(KEY_WIFI, isChecked).apply();
                }
                if (getContext() != null) {
                    Toast.makeText(requireContext(), isChecked ? "WiFi 已开启" : "WiFi 已关闭", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (switchBluetooth != null) {
            switchBluetooth.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (prefs != null) {
                    prefs.edit().putBoolean(KEY_BLUETOOTH, isChecked).apply();
                }
                if (getContext() != null) {
                    Toast.makeText(requireContext(), isChecked ? "蓝牙 已开启" : "蓝牙 已关闭", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (switchMobileData != null) {
            switchMobileData.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (prefs != null) {
                    prefs.edit().putBoolean(KEY_MOBILE_DATA, isChecked).apply();
                }
                if (getContext() != null) {
                    Toast.makeText(requireContext(), isChecked ? "移动数据 已开启" : "移动数据 已关闭", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (switchTouchSound != null) {
            switchTouchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (prefs != null) {
                    prefs.edit().putBoolean(KEY_TOUCH_SOUND, isChecked).apply();
                }
                if (getContext() != null) {
                    Toast.makeText(requireContext(), isChecked ? "触摸声音 已开启" : "触摸声音 已关闭", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (switchVibration != null) {
            switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (prefs != null) {
                    prefs.edit().putBoolean(KEY_VIBRATION, isChecked).apply();
                }
                if (getContext() != null) {
                    Toast.makeText(requireContext(), isChecked ? "振动反馈 已开启" : "振动反馈 已关闭", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (layoutAdbPermission != null) {
            layoutAdbPermission.setOnClickListener(v -> showAdbPermissionDialog());
        }

        if (layoutSetHome != null) {
            layoutSetHome.setOnClickListener(v -> setAsHomeLauncher());
        }

        if (layoutFloatMapApp != null) {
            layoutFloatMapApp.setOnClickListener(v -> showMapAppPickerDialog());
        }
    }

    /**
     * 回显当前选择的悬浮地图应用（图标 + 包名）
     */
    private void refreshFloatMapApp() {
        if (getContext() == null || textFloatMapPackage == null) return;

        String mapPackage = FloatingWindowManager.getMapPackage(requireContext());
        textFloatMapPackage.setText(mapPackage);

        if (imgFloatMapIcon != null) {
            try {
                PackageManager pm = requireContext().getPackageManager();
                imgFloatMapIcon.setImageDrawable(pm.getApplicationIcon(mapPackage));
            } catch (Exception e) {
                imgFloatMapIcon.setImageDrawable(null);
            }
        }
    }

    /**
     * 弹出已安装应用列表，选择作为悬浮地图的应用
     */
    private void showMapAppPickerDialog() {
        if (getContext() == null) return;

        PackageManager pm = requireContext().getPackageManager();
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        java.util.List<android.content.pm.ResolveInfo> resolveInfos = pm.queryIntentActivities(launcherIntent, 0);

        java.util.List<android.content.pm.ResolveInfo> apps = new java.util.ArrayList<>();
        for (android.content.pm.ResolveInfo info : resolveInfos) {
            // 排除自身
            if (!info.activityInfo.packageName.equals(requireContext().getPackageName())) {
                apps.add(info);
            }
        }
        apps.sort((a, b) -> a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));

        String currentPackage = FloatingWindowManager.getMapPackage(requireContext());
        int checkedIndex = -1;
        for (int i = 0; i < apps.size(); i++) {
            if (apps.get(i).activityInfo.packageName.equals(currentPackage)) {
                checkedIndex = i;
                break;
            }
        }

        android.widget.ArrayAdapter<android.content.pm.ResolveInfo> adapter =
            new android.widget.ArrayAdapter<android.content.pm.ResolveInfo>(requireContext(), R.layout.item_app_choice, apps) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    View itemView = convertView;
                    if (itemView == null) {
                        itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_app_choice, parent, false);
                    }
                    android.content.pm.ResolveInfo info = getItem(position);
                    if (info != null) {
                        ((ImageView) itemView.findViewById(R.id.img_app_icon)).setImageDrawable(info.loadIcon(pm));
                        ((TextView) itemView.findViewById(R.id.text_app_name)).setText(info.loadLabel(pm));
                        ((TextView) itemView.findViewById(R.id.text_app_package)).setText(info.activityInfo.packageName);
                    }
                    return itemView;
                }
            };

        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("选择悬浮地图应用")
            .setSingleChoiceItems(adapter, checkedIndex, (dialog, which) -> {
                String packageName = apps.get(which).activityInfo.packageName;
                FloatingWindowManager.setMapPackage(requireContext(), packageName);
                refreshFloatMapApp();
                Toast.makeText(requireContext(), "已设置悬浮地图: " + packageName, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void showAdbPermissionDialog() {
        if (getContext() == null) return;

        String packageName = requireContext().getPackageName();
        String localIp = getLocalIpAddress();
        
        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("ADB授权")
            .setMessage("选择授权方式：")
            .setPositiveButton("自动执行授权", (dialog, which) -> executeAdbGrant())
            .setNeutralButton("查看连接命令", (dialog, which) -> showAdbCommands(localIp, packageName))
            .setNegativeButton("取消", null)
            .show();
    }

    private void showAdbCommands(String localIp, String packageName) {
        String adbCommands = "【1. 设备连接】\n" +
            "adb connect " + localIp + ":5555\n\n" +
            "【2. 授予权限】\n" +
            "adb shell pm grant " + packageName + " android.permission.WRITE_SETTINGS\n\n" +
            "adb shell appops set " + packageName + " SYSTEM_ALERT_WINDOW allow";

        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("ADB命令")
            .setMessage(adbCommands)
            .setPositiveButton("复制连接命令", (dialog, which) -> {
                copyToClipboard("adb connect " + localIp + ":5555");
                Toast.makeText(requireContext(), "已复制", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("关闭", null)
            .show();
    }

    private void executeAdbGrant() {
        if (getContext() == null) return;
        
        String packageName = requireContext().getPackageName();
        String deviceIp = getLocalIpAddress();
        
        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("ADB协议授权")
            .setMessage("将通过ADB协议直接连接车机端口并授予权限\n\n" +
                       "车机地址: " + deviceIp + ":5555\n\n" +
                       "请确保：\n" +
                       "✓ 车机已开启ADB端口\n" +
                       "✓ 设备IP正确")
            .setPositiveButton("开始连接", (dialog, which) -> {
                Toast.makeText(requireContext(), "正在连接ADB服务...", Toast.LENGTH_SHORT).show();
                
                AdbProtocolClient adbClient = new AdbProtocolClient(requireContext(), deviceIp, 5555);
                adbClient.connectAndGrantPermission(packageName, new AdbProtocolClient.AdbCallback() {
                    @Override
                    public void onConnectionSuccess() {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "连接成功，正在授权...", Toast.LENGTH_SHORT).show();
                        });
                    }
                    
                    @Override
                    public void onConnectionFailed(String error) {
                        requireActivity().runOnUiThread(() -> {
                            new android.app.AlertDialog.Builder(requireContext())
                                .setTitle("连接失败")
                                .setMessage(error)
                                .setPositiveButton("确定", null)
                                .show();
                        });
                    }
                    
                    @Override
                    public void onCommandExecuted(boolean success, String output) {
                        requireActivity().runOnUiThread(() -> {
                            new android.app.AlertDialog.Builder(requireContext())
                                .setTitle(success ? "授权完成" : "授权结果")
                                .setMessage(output)
                                .setPositiveButton("确定", null)
                                .show();
                        });
                    }
                    
                    @Override
                    public void onPermissionGranted() {
                        requireActivity().runOnUiThread(() -> {
                            try {
                                android.widget.Toast.makeText(requireContext(), 
                                    "✓ 权限已授予，正在重新启动音乐监听...", 
                                    android.widget.Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                android.util.Log.w("SettingsSystemFragment", "Toast显示失败: " + e.getMessage());
                            }
                            
                            android.os.Handler mainHandler = new android.os.Handler(requireContext().getMainLooper());
                            mainHandler.postDelayed(() -> {
                                restartMusicListening();
                            }, 1000);
                        });
                    }
                });
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void copyToClipboard(String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
            requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("ADB", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
    }

    private String getLocalIpAddress() {
        try {
            java.net.NetworkInterface networkInterface = null;
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            
            while (interfaces.hasMoreElements()) {
                networkInterface = interfaces.nextElement();
                java.util.Enumeration<java.net.InetAddress> addresses = networkInterface.getInetAddresses();
                
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress inetAddress = addresses.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        String ip = inetAddress.getHostAddress();
                        if (ip != null && ip.length() > 0 && !ip.contains(":")) {
                            if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                                return ip;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "192.168.1.100";
    }
    
    /**
     * 重新启动音乐监听
     * ADB 权限授予后调用
     */
    private void restartMusicListening() {
        try {
            android.util.Log.i("SettingsSystemFragment", "═══════════════════════════════════════════════════════════");
            android.util.Log.i("SettingsSystemFragment", "【音乐监听重启】正在重新启动音乐监听...");
            android.util.Log.i("SettingsSystemFragment", "═══════════════════════════════════════════════════════════");
            
            BootReceiver.requestNotificationListenerSetup(requireContext());
            
            android.os.Handler mainHandler = new android.os.Handler(requireContext().getMainLooper());
            mainHandler.postDelayed(() -> {
                try {
                    android.app.Activity activity = requireActivity();
                    if (activity instanceof cn.youth996.ui.core.MainActivity) {
                        cn.youth996.ui.core.MainActivity mainActivity = (cn.youth996.ui.core.MainActivity) activity;
                        mainActivity.restartMusicMonitoring();
                        android.util.Log.i("SettingsSystemFragment", "【音乐监听重启】✓ 音乐监听已重新启动");
                    } else {
                        android.util.Log.w("SettingsSystemFragment", "【音乐监听重启】当前不是 MainActivity，无法重启音乐监听");
                    }
                } catch (Exception e) {
                    android.util.Log.e("SettingsSystemFragment", "【音乐监听重启】重启失败: " + e.getMessage());
                }
            }, 2000);
            
        } catch (Exception e) {
            android.util.Log.e("SettingsSystemFragment", "【音乐监听重启】失败: " + e.getMessage());
        }
    }

    private void setAsHomeLauncher() {
        if (getContext() == null) return;

        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + requireContext().getPackageName()));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.addCategory(Intent.CATEGORY_LAUNCHER);

                PackageManager pm = requireContext().getPackageManager();
                var resolveInfo = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);

                if (resolveInfo != null && resolveInfo.activityInfo != null) {
                    intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + resolveInfo.activityInfo.packageName));
                }
            }

            startActivity(intent);
            Toast.makeText(requireContext(), "请在设置中将Youth桌面设为默认桌面", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                startActivity(intent);
                Toast.makeText(requireContext(), "请选择Youth桌面作为默认桌面", Toast.LENGTH_LONG).show();
            } catch (Exception ex) {
                Toast.makeText(requireContext(), "无法打开设置，请手动设置", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
