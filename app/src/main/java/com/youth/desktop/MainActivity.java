package com.youth.desktop;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 主桌面Activity - Youth桌面系统的主入口
 *
 * 功能说明：
 * 1. 管理桌面图标网格布局
 * 2. 显示状态栏(时间和日期)
 * 3. 管理任务栏(导航、空调控制、桌面设置)
 * 4. 处理应用启动和图标管理
 * 5. 实时更新时间显示
 * 6. 模拟空调控制功能
 *
 * @author Youth Desktop Team
 */
public class MainActivity extends AppCompatActivity {

    // ==================== 视图组件声明 ====================
    private FrameLayout desktopContainer;
    private LinearLayout statusBar;
    private LinearLayout taskbar;
    private GridLayout iconGrid;
    private TextView timeText;
    private TextView dateText;
    private LinearLayout homeButton;
    private TextView acTempText;

    // ==================== 空调状态 ====================
    private boolean isAcOn = false;
    private int currentTemp = 26;
    private static final int MIN_TEMP = 17;
    private static final int MAX_TEMP = 33;

    // 除霜和风向状态
    private boolean isFrontDefrostOn = false;
    private boolean isRearDefrostOn = false;
    private int airDirection = 0; // 0:面部 1:胸部 2:脚部 3:全部

    // ==================== 数据和处理器 ====================
    private Handler handler;
    private Runnable timeUpdateRunnable;

    // ==================== 应用数据管理 ====================
    private Set<String> pinnedApps = new HashSet<>();
    private List<AppInfo> installedApps = new ArrayList<>();
    private List<AppInfo> desktopIcons = new ArrayList<>();

    // ==================== 布局配置常量 ====================
    private static final int DESKTOP_COLUMNS = 8;
    private static final int DESKTOP_ROWS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initDefaultPinnedApps();
        loadInstalledApps();
        setupTimeUpdater();
        setupClickListeners();
        updateAcTempDisplay();
    }

    private void initViews() {
        desktopContainer = findViewById(R.id.desktop_container);
        statusBar = findViewById(R.id.status_bar);
        taskbar = findViewById(R.id.taskbar);
        iconGrid = findViewById(R.id.icon_grid);

        timeText = findViewById(R.id.time_text);
        dateText = findViewById(R.id.date_text);
        homeButton = findViewById(R.id.btn_home);
        acTempText = findViewById(R.id.ac_temp_text);

        iconGrid.setColumnCount(DESKTOP_COLUMNS);
        iconGrid.setRowCount(DESKTOP_ROWS);
    }

    private void initDefaultPinnedApps() {
        pinnedApps.add("com.android.settings");
        pinnedApps.add("com.android.browser");
        pinnedApps.add("com.android.gallery3d");
        pinnedApps.add("com.android.music");
        pinnedApps.add("com.android.camera2");
        pinnedApps.add("com.android.contacts");
        pinnedApps.add("com.android.calendar");
        pinnedApps.add("com.android.email");
    }

    private void loadInstalledApps() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        PackageManager pm = getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);

        for (ResolveInfo resolveInfo : resolveInfos) {
            AppInfo appInfo = new AppInfo();
            appInfo.label = resolveInfo.loadLabel(pm).toString();
            appInfo.packageName = resolveInfo.activityInfo.packageName;
            appInfo.className = resolveInfo.activityInfo.name;
            appInfo.icon = resolveInfo.loadIcon(pm);
            appInfo.intent = pm.getLaunchIntentForPackage(appInfo.packageName);

            installedApps.add(appInfo);

            if (pinnedApps.contains(appInfo.packageName)) {
                desktopIcons.add(appInfo);
            }
        }

        Collections.sort(desktopIcons, (a1, a2) -> a1.label.compareToIgnoreCase(a2.label));
        displayDesktopIcons();
    }

    private void displayDesktopIcons() {
        iconGrid.removeAllViews();

        int count = 0;
        int maxIcons = DESKTOP_COLUMNS * DESKTOP_ROWS;

        for (AppInfo appInfo : desktopIcons) {
            if (count >= maxIcons) break;

            View iconView = createIconView(appInfo);

            int row = count / DESKTOP_COLUMNS;
            int col = count % DESKTOP_COLUMNS;

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(row, 1, GridLayout.CENTER);
            params.columnSpec = GridLayout.spec(col, 1, GridLayout.CENTER);
            params.setGravity(Gravity.CENTER);
            params.bottomMargin = dpToPx(32);
            params.rightMargin = dpToPx(16);

            iconGrid.addView(iconView, params);
            count++;
        }
    }

    private View createIconView(final AppInfo appInfo) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        container.setBackgroundResource(R.drawable.icon_background_selector);

        ImageView iconView = new ImageView(this);
        iconView.setImageDrawable(appInfo.icon);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
            dpToPx(56), dpToPx(56)
        );
        iconView.setLayoutParams(iconParams);

        TextView labelView = new TextView(this);
        labelView.setText(appInfo.label);
        labelView.setTextSize(12);
        labelView.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
        labelView.setMaxLines(2);
        labelView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        labelView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        labelParams.topMargin = dpToPx(8);
        labelView.setLayoutParams(labelParams);

        container.addView(iconView);
        container.addView(labelView);

        container.setOnClickListener(v -> launchApp(appInfo));
        container.setOnLongClickListener(v -> {
            showIconOptions(appInfo);
            return true;
        });

        return container;
    }

    private void launchApp(AppInfo appInfo) {
        if (appInfo.intent != null) {
            startActivity(appInfo.intent);
            addToRecentApps(appInfo);
        }
    }

    private void showIconOptions(AppInfo appInfo) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(appInfo.label);
        builder.setItems(new String[]{"打开", "从桌面移除"}, (dialog, which) -> {
            if (which == 0) {
                launchApp(appInfo);
            } else if (which == 1) {
                desktopIcons.remove(appInfo);
                displayDesktopIcons();
            }
        });
        builder.show();
    }

    private void addToRecentApps(AppInfo appInfo) {
        updateTaskbar();
    }

    private void setupTimeUpdater() {
        handler = new Handler();
        timeUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateTimeDisplay();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(timeUpdateRunnable);
    }

    private void updateTimeDisplay() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM月dd日 E", Locale.CHINESE);

        Date currentDate = new Date();
        timeText.setText(timeFormat.format(currentDate));
        dateText.setText(dateFormat.format(currentDate));
    }

    private void setupClickListeners() {
        // 返回按钮
        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // 首页按钮
        if (homeButton != null) {
            homeButton.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            });
        }

        // 最近任务按钮
        View btnRecent = findViewById(R.id.btn_recent);
        if (btnRecent != null) {
            btnRecent.setOnClickListener(v -> {
                Toast.makeText(this, "打开最近任务", Toast.LENGTH_SHORT).show();
                try {
                    Intent recentIntent = new Intent();
                    recentIntent.setClassName("com.android.systemui",
                        "com.android.systemui.recents.RecentsActivity");
                    recentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(recentIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "无法打开最近任务", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 空调开关
        View btnAcPower = findViewById(R.id.btn_ac_power);
        if (btnAcPower != null) {
            btnAcPower.setOnClickListener(v -> {
                isAcOn = !isAcOn;
                String message = isAcOn ? "空调已开启" : "空调已关闭";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                updateAcTempDisplay();
            });
        }

        // 温度减小
        View btnAcMinus = findViewById(R.id.btn_ac_minus);
        if (btnAcMinus != null) {
            btnAcMinus.setOnClickListener(v -> {
                if (isAcOn && currentTemp > MIN_TEMP) {
                    currentTemp--;
                    updateAcTempDisplay();
                    Toast.makeText(this, "温度: " + currentTemp + "°C", Toast.LENGTH_SHORT).show();
                } else if (!isAcOn) {
                    Toast.makeText(this, "请先开启空调", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "已达到最低温度", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 温度增大
        View btnAcPlus = findViewById(R.id.btn_ac_plus);
        if (btnAcPlus != null) {
            btnAcPlus.setOnClickListener(v -> {
                if (isAcOn && currentTemp < MAX_TEMP) {
                    currentTemp++;
                    updateAcTempDisplay();
                    Toast.makeText(this, "温度: " + currentTemp + "°C", Toast.LENGTH_SHORT).show();
                } else if (!isAcOn) {
                    Toast.makeText(this, "请先开启空调", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "已达到最高温度", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 前挡风除霜
        View btnDefrostFront = findViewById(R.id.btn_defrost_front);
        if (btnDefrostFront != null) {
            btnDefrostFront.setOnClickListener(v -> {
                isFrontDefrostOn = !isFrontDefrostOn;
                String message = isFrontDefrostOn ? "前挡风除霜已开启" : "前挡风除霜已关闭";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                // 视觉效果反馈
                btnDefrostFront.setAlpha(isFrontDefrostOn ? 1.0f : 0.5f);
            });
        }

        // 后挡风玻璃除霜
        View btnDefrostRear = findViewById(R.id.btn_defrost_rear);
        if (btnDefrostRear != null) {
            btnDefrostRear.setOnClickListener(v -> {
                isRearDefrostOn = !isRearDefrostOn;
                String message = isRearDefrostOn ? "后挡风玻璃除霜已开启" : "后挡风玻璃除霜已关闭";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                // 视觉效果反馈
                btnDefrostRear.setAlpha(isRearDefrostOn ? 1.0f : 0.5f);
            });
        }

        // 空调吹风角度调节
        View btnAirDirection = findViewById(R.id.btn_air_direction);
        if (btnAirDirection != null) {
            btnAirDirection.setOnClickListener(v -> {
                airDirection = (airDirection + 1) % 4;
                String[] directions = {"面部吹风", "胸部吹风", "脚部吹风", "全部吹风"};
                Toast.makeText(this, "当前模式: " + directions[airDirection], Toast.LENGTH_SHORT).show();
            });
        }

        // 桌面设置
        View btnDesktopSettings = findViewById(R.id.btn_desktop_settings);
        if (btnDesktopSettings != null) {
            btnDesktopSettings.setOnClickListener(v -> showSettingsDialog());
        }
    }

    private void updateAcTempDisplay() {
        if (acTempText != null) {
            if (isAcOn) {
                acTempText.setText(currentTemp + "°C");
                acTempText.setTextColor(getResources().getColor(R.color.primary, getTheme()));
            } else {
                acTempText.setText("--°C");
                acTempText.setTextColor(getResources().getColor(R.color.text_hint, getTheme()));
            }
        }
    }

    private void showSettingsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("桌面设置");
        builder.setItems(new String[]{"更换壁纸", "主题设置", "桌面布局", "关于"}, (dialog, which) -> {
            switch (which) {
                case 0:
                    Toast.makeText(this, "更换壁纸功能", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    Toast.makeText(this, "主题设置功能", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    Toast.makeText(this, "桌面布局功能", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    showAboutDialog();
                    break;
            }
        });
        builder.show();
    }

    private void showAboutDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("关于 Youth桌面");
        builder.setMessage("Youth桌面 v1.0\n\n参考叁迪UI设计的现代化桌面系统\n\n功能特点:\n• 桌面图标管理\n• 应用抽屉\n• 空调控制面板\n• 主题定制\n• 车机风格界面");
        builder.setPositiveButton("确定", null);
        builder.show();
    }

    private void updateTaskbar() {
        if (homeButton != null) {
            homeButton.setSelected(true);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTimeDisplay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && timeUpdateRunnable != null) {
            handler.removeCallbacks(timeUpdateRunnable);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
