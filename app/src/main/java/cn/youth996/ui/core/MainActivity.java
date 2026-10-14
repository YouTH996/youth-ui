package cn.youth996.ui.core;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import cn.youth996.ui.R;
import cn.youth996.ui.manager.CardManager;
import cn.youth996.ui.manager.MusicPlayerManager;
import cn.youth996.ui.manager.NotificationMonitor;
import cn.youth996.ui.service.MediaNotificationListenerService;
import cn.youth996.ui.settings.SettingsDialogFragment;
import cn.youth996.ui.view.BatteryView;

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
    
    private static final String TAG = "MainActivity";
    private static MainActivity instance;

    // ==================== 视图组件声明 ====================
    private FrameLayout desktopContainer;
    private LinearLayout statusBar;
    private LinearLayout taskbar;
    private GridLayout iconGrid;
    private LinearLayout homeButton;
    private TextView acTempText;
    private LinearLayout cardContainer;
    private HorizontalScrollView cardScrollView;
    private CardManager cardManager;
    private MusicPlayerManager musicPlayerManager;
    private LinearLayout desktopMainLayout;
    private LinearLayout centerArea;
    
    // ImageView 组件声明
    private ImageView imgBack;
    private ImageView imgHome;
    private ImageView imgRecent;
    private ImageView imgAcPower;
    private ImageView imgAcMinus;
    private ImageView imgAcPlus;
    private ImageView imgDefrostFront;
    private ImageView imgDefrostRear;
    private ImageView imgAirDirection;
    private ImageView imgSettings;
    
    // 状态栏组件声明
    private ImageView imgVoiceAssistant;
    private ImageView imgWifi;
    private ImageView imgBluetooth;
    private ImageView img4gSignal;
    private ImageView imgDrivingRecorder;
    private TextView textGearP;
    private TextView textGearR;
    private TextView textGearN;
    private TextView textGearD;
    private TextView textTimeOnly;
    private BatteryView batteryView;
    
    // 导航预览组件声明
    private View navModeContainer;
    private View drivingModeContainer;
    private View pipModeContainer;
    private ImageView imgPipIcon;
    private TextView textPipStatus;
    private FrameLayout pipPreviewFrame;
    
    // 高德地图悬浮窗检测
    private static final String AMAP_PACKAGE = "com.autonavi.amapauto.chenmo";
    private static final String AMAP_STANDALONE_PACKAGE = "com.autonavi.map";
    private static final String AMAP_FLOAT_WINDOW_ACTIVITY = "com.autonavi.auto.ui.SplashActivity";
    private boolean isAutoNaviFloating = false;
    private boolean isAutoNaviPipMode = false;
    private Runnable navMonitorRunnable;
    
    // 画中画模式常量
    private static final int MODE_DRIVING = 0;
    private static final int MODE_NAVIGATION = 1;
    private static final int MODE_PIP = 2;
    private int currentNavMode = MODE_DRIVING;

    // ==================== 空调状态 ====================
    private boolean isAcOn = false;
    private int currentTemp = 26;
    private static final int MIN_TEMP = 17;
    private static final int MAX_TEMP = 33;

    // 除霜和风向状态
    private boolean isFrontDefrostOn = false;
    private boolean isRearDefrostOn = false;
    private int airDirection = 0; // 0:面部 1:胸部 2:脚部 3:全部
    
    // 档位状态 (0=P, 1=N, 2=D)
    private int currentGear = 0;
    
    // 状态栏图标状态
    private boolean isWifiOn = true;
    private boolean isBluetoothOn = true;
    private boolean is4gSignalOn = true;
    private boolean isDrivingRecorderOn = true;
    
    // 壁纸相关
    private String[] wallpaperPaths = {
        "webui/wallpaper/1.jpg",
        "webui/wallpaper/2.jpg",
        "webui/wallpaper/3.jpg",
        "webui/wallpaper/4.jpg",
        "webui/wallpaper/5.jpg",
        "webui/wallpaper/6.jpg",
        "webui/wallpaper/7.jpg",
        "webui/wallpaper/8.jpg",
        "webui/wallpaper/9.jpg",
        "webui/wallpaper/10.jpg"
    };
    private int currentWallpaperIndex = 0;
    private ImageView wallpaperImageView;
    private GestureDetector gestureDetector;

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

        instance = this;
        
        // 隐藏系统导航栏和状态栏
        hideSystemUI();
        
        setContentView(R.layout.activity_main);

        initViews();
        initCardManager();
        initDefaultPinnedApps();
        loadInstalledApps();
        setupTimeUpdater();
        setupClickListeners();
        updateAcTempDisplay();

        // 启动悬浮窗权限检查（参考BYD Launcher）
        checkOverlayPermission();

        // 初始化导航监控
//        initNavigationMonitor();
        // 检查并请求权限
//        checkAndRequestPermissions();

        // 动态计算外层主布局的底部安全区，避免中间卡片与底部小卡片/taskbar 重叠
        adjustMainLayoutBottomSafeArea();
        // 给中间主区域一个最小高度（占屏幕 50%），保证中间两张主卡足够大
        applyCenterAreaMinHeight();
    }

    /**
     * 动态计算并设置桌面主布局（外层 vertical LinearLayout）的底部 padding。
     * 预留空间 = taskbar 实际高度 + 小卡片容器距屏底距离 + 小卡片容器本身高度。
     * 这样无论屏幕尺寸如何变化，中间两张主卡片都会恰好显示在
     * "状态栏" 与 "小卡片+taskbar" 之间的可用空间里。
     */
    private void adjustMainLayoutBottomSafeArea() {
        if (desktopMainLayout == null || taskbar == null || cardScrollView == null) {
            return;
        }
        // 等小卡片容器首次布局完成后再测量（此时 CardManager 已设置好 minHeight）
        cardScrollView.post(() -> cardScrollView.post(this::applyBottomSafeArea));
    }

    /**
     * 实际把"taskbar + 小卡片 + 间距"的总和写到外层 LinearLayout 的 paddingBottom 上。
     * 该方法在 cardScrollView 二次 post 后被调用，确保 minHeight 已生效。
     */
    private void applyBottomSafeArea() {
        try {
            int taskbarHeight = taskbar.getHeight();
            if (taskbarHeight <= 0) {
                taskbarHeight = (int) (getResources().getDimension(R.dimen.taskbar_height));
            }
            int cardScrollMarginBottomPx = (int) (getResources().getDimension(R.dimen.card_container_margin_bottom));
            int cardScrollHeight = cardScrollView.getHeight();
            int extraPad = (int) (4 * getResources().getDisplayMetrics().density);
            int total = taskbarHeight + cardScrollMarginBottomPx + cardScrollHeight + extraPad;
            desktopMainLayout.setPadding(
                    desktopMainLayout.getPaddingLeft(),
                    desktopMainLayout.getPaddingTop(),
                    desktopMainLayout.getPaddingRight(),
                    total
            );
            android.util.Log.i(TAG, "【布局适配】动态底部安全区 px=" + total +
                    " (taskbar=" + taskbarHeight +
                    ", cardScrollMargin=" + cardScrollMarginBottomPx +
                    ", cardScrollHeight=" + cardScrollHeight + ")");
        } catch (Exception e) {
            android.util.Log.e(TAG, "【布局适配】计算底部安全区失败: " + e.getMessage());
        }
    }

    /**
     * 强制设置中间主区域（左侧导航卡 + 右侧音乐卡）高度 = 屏幕高度的 80%，
     * 让两张主卡在桌面布局中占绝对主体。
     * 并把外层 LinearLayout 的 paddingBottom 同步增大，
     * 保证底部小卡片容器和 taskbar 始终在中间区域下方。
     */
    private void applyCenterAreaMinHeight() {
        if (centerArea == null) {
            return;
        }
        try {
            float density = getResources().getDisplayMetrics().density;
            int screenHeightPx = getResources().getDisplayMetrics().heightPixels;
            int targetHeightPx = (int) (screenHeightPx * 0.80f);
            // 强制覆盖 wrap_content：直接设 layoutParams.height
            android.view.ViewGroup.LayoutParams lp = centerArea.getLayoutParams();
            lp.height = targetHeightPx;
            centerArea.setLayoutParams(lp);
            // 同步外层 paddingBottom = statusBar + centerArea + 8dp 缓冲
            centerArea.post(() -> {
                int realHeight = centerArea.getHeight();
                int statusBarHeight = statusBar != null ? statusBar.getHeight() : 0;
                int desiredSafe = realHeight + statusBarHeight + (int) (8 * density);
                if (desktopMainLayout != null) {
                    desktopMainLayout.setPadding(
                            desktopMainLayout.getPaddingLeft(),
                            desktopMainLayout.getPaddingTop(),
                            desktopMainLayout.getPaddingRight(),
                            desiredSafe
                    );
                    android.util.Log.i(TAG, "【布局适配】centerArea 强制高度 px=" + realHeight +
                            "（屏幕 80%），外层 paddingBottom=" + desiredSafe);
                }
            });
        } catch (Exception e) {
            android.util.Log.e(TAG, "【布局适配】中间区域高度设置失败: " + e.getMessage());
        }
    }

    /**
     * 检查悬浮窗权限（参考BYD Launcher启动流程）
     */
    private void checkOverlayPermission() {
        if (!android.provider.Settings.canDrawOverlays(this)) {
            android.util.Log.w(TAG, "【悬浮窗权限】需要悬浮窗权限，正在打开设置页面");
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                android.util.Log.i(TAG, "【悬浮窗权限】已打开悬浮窗权限设置页面");
            } catch (Exception e) {
                android.util.Log.e(TAG, "【悬浮窗权限】无法打开设置页面: " + e.getMessage());
            }
        } else {
            android.util.Log.i(TAG, "【悬浮窗权限】✓ 悬浮窗权限已授予");
            onOverlayPermissionGranted();
        }
    }

    private static final int REQUEST_OVERLAY_PERMISSION = 1001;

    /**
     * 悬浮窗权限授予结果处理
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (resultCode == RESULT_OK) {
                android.util.Log.i(TAG, "【悬浮窗权限】用户已授权");
                onOverlayPermissionGranted();
            } else {
                android.util.Log.w(TAG, "【悬浮窗权限】用户未授权");
            }
        }
    }

    /**
     * 悬浮窗权限授予后的回调
     */
    private void onOverlayPermissionGranted() {
        android.util.Log.i(TAG, "【悬浮窗权限】权限授予完成");
        // 可以继续其他初始化流程
    }
    
    /**
     * 隐藏系统导航栏和状态栏
     */
    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android 11 (API 30) 及以上使用 WindowInsetsController
            WindowInsetsController controller = decorView.getWindowInsetsController();
            if (controller != null) {
                controller.hide(android.view.WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            // Android 10 (API 29) 及以下使用 setSystemUiVisibility
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
        
        // 设置窗口标志,确保内容延伸到系统栏区域
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
    }
    
    private void initCardManager() {
        cardContainer = findViewById(R.id.card_container);
        cardScrollView = findViewById(R.id.card_scroll_view);
        
        if (cardContainer != null && cardScrollView != null) {
            cardManager = new CardManager(this, cardContainer, cardScrollView);
            cardManager.initializeCards();
        }
    }

    private void initViews() {
        desktopContainer = findViewById(R.id.desktop_container);
        desktopMainLayout = findViewById(R.id.desktop_main_layout);
        centerArea = findViewById(R.id.center_area);
        statusBar = findViewById(R.id.status_bar);
        taskbar = findViewById(R.id.taskbar);

        homeButton = findViewById(R.id.btn_home);
        acTempText = findViewById(R.id.ac_temp_text);

        // 图标网格已移除
        // iconGrid.setColumnCount(DESKTOP_COLUMNS);
        // iconGrid.setRowCount(DESKTOP_ROWS);
        
        // 初始化导航预览组件（需要在initCenterArea之前）
        navModeContainer = findViewById(R.id.nav_mode_container);
        drivingModeContainer = findViewById(R.id.driving_mode_container);
        pipModeContainer = findViewById(R.id.pip_mode_container);
        imgPipIcon = findViewById(R.id.img_pip_icon);
        textPipStatus = findViewById(R.id.text_pip_status);
        pipPreviewFrame = findViewById(R.id.pip_preview_frame);
        
        // 初始化中间区域组件
        initCenterArea();
        
        // 初始化 ImageView 组件
        imgBack = findViewById(R.id.img_back);
        imgHome = findViewById(R.id.img_home);
        imgRecent = findViewById(R.id.img_recent);
        imgAcPower = findViewById(R.id.img_ac_power);
        imgAcMinus = findViewById(R.id.img_ac_minus);
        imgAcPlus = findViewById(R.id.img_ac_plus);
        imgDefrostFront = findViewById(R.id.img_defrost_front);
        imgDefrostRear = findViewById(R.id.img_defrost_rear);
        imgAirDirection = findViewById(R.id.img_air_direction);
        imgSettings = findViewById(R.id.img_settings);
        
        // 初始化状态栏组件
        imgVoiceAssistant = findViewById(R.id.img_voice_assistant);
        imgWifi = findViewById(R.id.img_wifi);
        imgBluetooth = findViewById(R.id.img_bluetooth);
        img4gSignal = findViewById(R.id.img_4g_signal);
        imgDrivingRecorder = findViewById(R.id.img_driving_recorder);
        textGearP = findViewById(R.id.text_gear_p);
        textGearR = findViewById(R.id.text_gear_r);
        textGearN = findViewById(R.id.text_gear_n);
        textGearD = findViewById(R.id.text_gear_d);
        textTimeOnly = findViewById(R.id.text_time_only);
        batteryView = findViewById(R.id.battery_view);
        
        // 设置电池初始值
        if (batteryView != null) {
            batteryView.setBatteryLevel(85);
            batteryView.setRange(120);
        }
        
        // 从 assets 加载图片
        loadIconsFromAssets();
        loadStatusBarIcons();
        updateGearDisplay();
        updateTimeOnly();
        
        // 设置导航栏按钮文字为白色
        setNavigationBarTextColorWhite();
        
        // 初始化壁纸
        initWallpaper();
    }
    
    /**
     * 初始化中间区域：导航预览和音乐播放卡片
     */
    private void initCenterArea() {
        Log.d("MainActivity", "=== INIT CENTER AREA ===");
        
        ImageView imgNavPreview = findViewById(R.id.img_nav_preview);
        ImageView imgAlbumCoverCenter = findViewById(R.id.img_album_cover_center);
        ImageView btnPreviousCenter = findViewById(R.id.btn_previous_center);
        ImageView btnPlayPauseCenter = findViewById(R.id.btn_play_pause_center);
        ImageView btnNextCenter = findViewById(R.id.btn_next_center);
        View navPreviewContainer = findViewById(R.id.nav_preview_container);
        View musicPlayerCard = findViewById(R.id.music_player_card);
        
        ImageView imgMusicSource = findViewById(R.id.img_music_source_icon);
        TextView textMusicSource = findViewById(R.id.text_music_source);
        TextView textSongTitle = findViewById(R.id.text_song_title_center);
        TextView textSongArtist = findViewById(R.id.text_song_artist_center);
        
        ViewGroup layoutLyrics = findViewById(R.id.layout_lyrics_center);

        // 状态栏灵动岛歌词
        View islandContainer = findViewById(R.id.island_lyric_container);
        ImageView imgIslandWave = findViewById(R.id.img_island_wave);
        TextView textIslandTitle = findViewById(R.id.text_island_title);
        TextView textIslandLyric = findViewById(R.id.text_island_lyric);

        Log.d("MainActivity", "Creating MusicPlayerManager...");
        musicPlayerManager = new MusicPlayerManager(this);
        musicPlayerManager.bindViews(imgAlbumCoverCenter, imgMusicSource, textMusicSource,
                                     textSongTitle, textSongArtist, btnPlayPauseCenter);
        musicPlayerManager.bindLyricsViews(layoutLyrics);
        musicPlayerManager.bindIslandViews(islandContainer, imgIslandWave, textIslandTitle, textIslandLyric);
        Log.d("MainActivity", "MusicPlayerManager created, setting listener...");
        
        musicPlayerManager.setOnMusicUpdateListener(new MusicPlayerManager.OnMusicUpdateListener() {
            @Override
            public void onMusicUpdate(String packageName, String title, String artist, Bitmap albumArt, boolean isPlaying) {
//                Log.d("MainActivity", "=== MUSIC UPDATE: " + title + " - " + artist);
                updateMusicPlayerUI(title, artist, isPlaying);
            }
            
            @Override
            public void onPlaybackStateChanged(boolean isPlaying) {
//                Log.d("MainActivity", "Playback state changed: " + isPlaying);
                btnPlayPauseCenter.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
            }
        });
        
        Log.d("MainActivity", "Starting music listening...");
        musicPlayerManager.startListening();
        Log.d("MainActivity", "Music listening started");
        
//        initLyricsAccessibilityService();
        setupMusicControls();
        setupLyricsListener();
    }
    
    private void setupLyricsListener() {
        MediaNotificationListenerService.setLyricsListener(new MediaNotificationListenerService.OnLyricsUpdateListener() {
            @Override
            public void onLyricsUpdate(List<String> lyrics, int index) {
                runOnUiThread(() -> musicPlayerManager.updateLyrics(lyrics, index));
            }
        });
    }
    
    private void setupMusicControls() {
        ImageView imgNavPreview = findViewById(R.id.img_nav_preview);
        ImageView imgAlbumCoverCenter = findViewById(R.id.img_album_cover_center);
        View navPreviewContainer = findViewById(R.id.nav_preview_container);
        ImageView btnPreviousCenter = findViewById(R.id.btn_previous_center);
        ImageView btnPlayPauseCenter = findViewById(R.id.btn_play_pause_center);
        ImageView btnNextCenter = findViewById(R.id.btn_next_center);
        View musicPlayerCard = findViewById(R.id.music_player_card);
        
        try {
            InputStream is = getAssets().open("webui/img/widget/automap_logo.png");
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            imgNavPreview.setImageBitmap(bitmap);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        try {
            InputStream is = getAssets().open("webui/img/widget/music_cover.jpg");
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            imgAlbumCoverCenter.setImageBitmap(bitmap);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        navPreviewContainer.setOnClickListener(v -> {
            switch (currentNavMode) {
                case MODE_DRIVING:
                    Toast.makeText(this, "打开导航应用", Toast.LENGTH_SHORT).show();
                    launchAmapNavigation();
                    break;
                case MODE_NAVIGATION:
                    Toast.makeText(this, "打开高德地图", Toast.LENGTH_SHORT).show();
                    launchAmapNavigation();
                    break;
                case MODE_PIP:
                    Toast.makeText(this, "切换到高德地图", Toast.LENGTH_SHORT).show();
                    bringAmapToFront();
                    break;
            }
        });
        
        if (pipPreviewFrame != null) {
            pipPreviewFrame.setOnClickListener(v -> {
                if (currentNavMode == MODE_PIP) {
                    Toast.makeText(this, "切换到高德地图", Toast.LENGTH_SHORT).show();
                    bringAmapToFront();
                }
            });
        }
        
        btnPreviousCenter.setOnClickListener(v -> {
            if (musicPlayerManager.hasActiveMusic()) {
                musicPlayerManager.previousTrack();
            } else {
                Toast.makeText(this, "上一首", Toast.LENGTH_SHORT).show();
            }
        });
        
        btnPlayPauseCenter.setOnClickListener(v -> {
            if (musicPlayerManager.hasActiveMusic()) {
                musicPlayerManager.playPause();
            } else {
                Toast.makeText(this, "播放/暂停", Toast.LENGTH_SHORT).show();
            }
        });
        
        btnNextCenter.setOnClickListener(v -> {
            if (musicPlayerManager.hasActiveMusic()) {
                musicPlayerManager.nextTrack();
            } else {
                Toast.makeText(this, "下一首", Toast.LENGTH_SHORT).show();
            }
        });
        
        musicPlayerCard.setOnClickListener(v -> {
            if (musicPlayerManager.hasActiveMusic()) {
                musicPlayerManager.openMusicApp();
            } else {
                Toast.makeText(this, "打开音乐应用", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateMusicPlayerUI(String title, String artist, boolean isPlaying) {
        // 不再替换歌词显示
    }
    
    /**
     * 设置导航栏按钮文字颜色为白色
     */
    private void setNavigationBarTextColorWhite() {
        int whiteColor = Color.WHITE;
        
        // 获取导航栏中的所有 TextView 并设置文字颜色
        findTextViewsInLinearLayout(taskbar, whiteColor);
        
        // 获取 btn_home 中的文字
        View btnHome = findViewById(R.id.btn_home);
        if (btnHome != null && btnHome instanceof LinearLayout) {
            LinearLayout btnHomeLayout = (LinearLayout) btnHome;
            for (int i = 0; i < btnHomeLayout.getChildCount(); i++) {
                View child = btnHomeLayout.getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(whiteColor);
                }
            }
        }
        
        // 获取 btn_settings 中的文字
        View btnSettings = findViewById(R.id.btn_desktop_settings);
        if (btnSettings != null && btnSettings instanceof LinearLayout) {
            LinearLayout btnSettingsLayout = (LinearLayout) btnSettings;
            for (int i = 0; i < btnSettingsLayout.getChildCount(); i++) {
                View child = btnSettingsLayout.getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(whiteColor);
                }
            }
        }
    }
    
    /**
     * 递归查找 LinearLayout 中的所有 TextView
     */
    private void findTextViewsInLinearLayout(ViewGroup parent, int color) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(color);
            } else if (child instanceof ViewGroup) {
                findTextViewsInLinearLayout((ViewGroup) child, color);
            }
        }
    }
    
    /**
     * 检查并请求必要的权限
     */
    private void checkAndRequestPermissions() {
        if (!hasUsageStatsPermission()) {
            Log.w(TAG, "需要使用统计权限");
            requestUsageStatsPermission();
        }
        
        if (!isNotificationListenerEnabled()) {
            Log.w(TAG, "需要通知监听器权限");
            requestNotificationListenerPermission();
        }
    }
    
    /**
     * 检查使用统计权限
     */
    private boolean hasUsageStatsPermission() {
        try {
            android.app.AppOpsManager appOps = (android.app.AppOpsManager) 
                getSystemService(Context.APP_OPS_SERVICE);
            
            if (appOps != null) {
                int mode = appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    getPackageName()
                );
                
                return mode == android.app.AppOpsManager.MODE_ALLOWED;
            }
        } catch (Exception e) {
            Log.e(TAG, "检查使用统计权限时出错: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * 请求使用统计权限
     */
    private void requestUsageStatsPermission() {
        Toast.makeText(this, "需要授权使用统计权限以检测导航应用", Toast.LENGTH_LONG).show();
        
        new android.os.Handler().postDelayed(() -> {
            try {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "无法打开使用统计设置: " + e.getMessage());
                try {
                    Intent intent = new Intent(Settings.ACTION_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    Toast.makeText(this, "请在设置中开启'使用与应用访问权限'", Toast.LENGTH_LONG).show();
                } catch (Exception e2) {
                    Toast.makeText(this, "请手动在设置中授权使用统计权限", Toast.LENGTH_LONG).show();
                }
            }
        }, 1000);
    }
    
    /**
     * 检查通知监听器是否已启用
     */
    private boolean isNotificationListenerEnabled() {
        try {
            String flat = Settings.Secure.getString(
                getContentResolver(),
                "enabled_notification_listeners"
            );
            
            if (flat != null && flat.contains(getPackageName())) {
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "检查通知监听器状态失败: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * 请求通知监听器权限
     */
    private void requestNotificationListenerPermission() {
        Toast.makeText(this, "需要授权通知监听权限以读取通知信息", Toast.LENGTH_LONG).show();
        
        new android.os.Handler().postDelayed(() -> {
            try {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "无法打开通知监听器设置: " + e.getMessage());
                Toast.makeText(this, "请手动在设置中开启'通知访问权限'", Toast.LENGTH_LONG).show();
            }
        }, 1000);
    }
    
    /**
     * 初始化导航监测
     */
    private void initNavigationMonitor() {
        registerAccessibilityListener();
        
        navMonitorRunnable = new Runnable() {
            @Override
            public void run() {
                checkAutoNaviFloating();
                if (handler != null) {
                    handler.postDelayed(this, 2000);
                }
            }
        };
        
        startNavMonitor();
    }
    
    /**
     * 注册无障碍服务监听器
     */
    private void registerAccessibilityListener() {
        if (YouthAccessibilityService.isServiceEnabled()) {
            YouthAccessibilityService accessibilityService = YouthAccessibilityService.getInstance();
            if (accessibilityService != null) {
                accessibilityService.setOnAppChangeListener(new YouthAccessibilityService.OnAppChangeListener() {
                    @Override
                    public void onAppChanged(String packageName, String className) {
                        Log.d(TAG, "检测到应用切换: " + packageName);
                        
                        if (packageName.equals(AMAP_PACKAGE) || packageName.equals(AMAP_STANDALONE_PACKAGE)) {
                            Log.d(TAG, "高德地图已切换到前台");
                            isAutoNaviFloating = true;
                            checkAmapPipMode(className);
                        } else {
                            if (isAutoNaviFloating) {
                                Log.d(TAG, "高德地图已切换到后台");
                                isAutoNaviFloating = false;
                                isAutoNaviPipMode = false;
                            }
                        }
                    }
                });
                Log.d(TAG, "无障碍服务监听器已注册");
            }
        } else {
            Log.w(TAG, "无障碍服务未启用");
        }
    }
    
    /**
     * 启动导航监测
     */
    private void startNavMonitor() {
        if (handler != null && navMonitorRunnable != null) {
            handler.post(navMonitorRunnable);
        }
    }
    
    /**
     * 停止导航监测
     */
    private void stopNavMonitor() {
        if (handler != null && navMonitorRunnable != null) {
            handler.removeCallbacks(navMonitorRunnable);
        }
    }
    
    /**
     * 检测高德地图是否以悬浮窗模式运行
     */
    private void checkAutoNaviFloating() {
        String topPackage = getTopPackageName();
        
        boolean isAmapRunning = topPackage != null && 
            (topPackage.equals(AMAP_PACKAGE) || topPackage.equals(AMAP_STANDALONE_PACKAGE));
        
        if (isAmapRunning != isAutoNaviFloating || isAmapRunning) {
            isAutoNaviFloating = isAmapRunning;
            
            runOnUiThread(() -> {
                if (isAmapRunning) {
                    checkAmapPipMode();
                }
            });
        }
    }
    
    /**
     * 检测高德地图是否为画中画模式
     */
    private void checkAmapPipMode() {
        try {
            android.app.ActivityManager activityManager = 
                (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
            
            if (activityManager != null) {
                List<android.app.ActivityManager.RunningTaskInfo> runningTasks = 
                    activityManager.getRunningTasks(10);
                
                boolean foundAmap = false;
                boolean isPipMode = false;
                
                for (android.app.ActivityManager.RunningTaskInfo taskInfo : runningTasks) {
                    if (taskInfo.topActivity != null) {
                        String packageName = taskInfo.topActivity.getPackageName();
                        String activityName = taskInfo.topActivity.getClassName();
                        
                        if (packageName.equals(AMAP_PACKAGE) || 
                            packageName.equals(AMAP_STANDALONE_PACKAGE)) {
                            
                            foundAmap = true;
                            
                            if (activityName.contains("pip") || 
                                activityName.contains("Pip") ||
                                activityName.contains("PIP") ||
                                activityName.contains("picture_in_picture") ||
                                activityName.contains("float") ||
                                activityName.contains("Float") ||
                                activityName.contains("小窗") ||
                                activityName.contains("悬浮")) {
                                
                                isPipMode = true;
                                break;
                            }
                        }
                    }
                }
                
                if (foundAmap) {
                    if (isPipMode && !isAutoNaviPipMode) {
                        isAutoNaviPipMode = true;
                        switchToPipMode();
                        Log.d(TAG, "检测到高德地图画中画模式");
                    } else if (!isPipMode) {
                        if (isAutoNaviPipMode) {
                            isAutoNaviPipMode = false;
                            switchToNavigationMode();
                            Log.d(TAG, "检测到高德地图普通模式");
                        }
                    }
                } else {
                    if (isAutoNaviPipMode) {
                        isAutoNaviPipMode = false;
                        switchToNavigationMode();
                        Log.d(TAG, "高德地图未运行");
                    }
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "无法检测画中画模式: " + e.getMessage());
            switchToNavigationMode();
        } catch (Exception e) {
            Log.e(TAG, "检测画中画模式时出错: " + e.getMessage());
            switchToNavigationMode();
        }
    }
    
    /**
     * 根据Activity类名检测高德地图是否为画中画模式（无障碍服务版本）
     */
    private void checkAmapPipMode(String className) {
        if (className == null || className.isEmpty()) {
            Log.d(TAG, "Activity类名为空");
            return;
        }
        
        Log.d(TAG, "检测画中画模式: " + className);
        
        if (className.contains("pip") || 
            className.contains("Pip") ||
            className.contains("PIP") ||
            className.contains("picture_in_picture") ||
            className.contains("float") ||
            className.contains("Float") ||
            className.contains("小窗") ||
            className.contains("悬浮")) {
            
            if (!isAutoNaviPipMode) {
                isAutoNaviPipMode = true;
                runOnUiThread(() -> switchToPipMode());
                Log.d(TAG, "检测到高德地图画中画模式（无障碍）");
            }
        } else {
            if (isAutoNaviPipMode) {
                isAutoNaviPipMode = false;
                runOnUiThread(() -> switchToNavigationMode());
                Log.d(TAG, "高德地图切换到普通模式（无障碍）");
            } else {
                runOnUiThread(() -> switchToNavigationMode());
                Log.d(TAG, "高德地图已启动（无障碍）");
            }
        }
    }
    
    /**
     * 获取前台应用的包名
     */
    private String getTopPackageName() {
        // 直接检查高德地图包名是否在运行
        if (isPackageRunning(AMAP_PACKAGE)) {
            Log.d(TAG, "高德地图车机版正在运行: " + AMAP_PACKAGE);
            return AMAP_PACKAGE;
        }
        
        if (isPackageRunning(AMAP_STANDALONE_PACKAGE)) {
            Log.d(TAG, "高德地图独立版正在运行: " + AMAP_STANDALONE_PACKAGE);
            return AMAP_STANDALONE_PACKAGE;
        }
        
        // 如果高德地图不在运行，返回null
        return null;
    }
    
    /**
     * 检查指定包名的应用是否正在运行
     */
    private boolean isPackageRunning(String packageName) {
        try {
            ActivityManager activityManager = 
                (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            
            if (activityManager == null) {
                Log.w(TAG, "ActivityManager服务不可用");
                return false;
            }
            
            // 方法1: 获取所有运行中的进程
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = 
                activityManager.getRunningAppProcesses();
            
            if (runningProcesses != null) {
                for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                    if (processInfo.processName != null && 
                        processInfo.processName.equals(packageName)) {
                        Log.d(TAG, "从进程列表找到应用: " + packageName);
                        return true;
                    }
                }
            }
            
            // 方法2: 获取运行中的任务（前台任务）
            try {
                List<ActivityManager.RunningTaskInfo> runningTasks = 
                    activityManager.getRunningTasks(100);
                
                if (runningTasks != null) {
                    for (ActivityManager.RunningTaskInfo taskInfo : runningTasks) {
                        if (taskInfo.topActivity != null && 
                            taskInfo.topActivity.getPackageName() != null &&
                            taskInfo.topActivity.getPackageName().equals(packageName)) {
                            Log.d(TAG, "从前台任务找到应用: " + packageName);
                            return true;
                        }
                        
                        if (taskInfo.baseActivity != null && 
                            taskInfo.baseActivity.getPackageName() != null &&
                            taskInfo.baseActivity.getPackageName().equals(packageName)) {
                            Log.d(TAG, "从基础Activity找到应用: " + packageName);
                            return true;
                        }
                    }
                }
            } catch (SecurityException e) {
                Log.w(TAG, "无法获取任务列表: " + e.getMessage());
            }
            
            // 方法3: 使用PackageManager检查应用是否已安装（不能检测运行状态，但可以确认应用存在）
            try {
                PackageManager pm = getPackageManager();
                pm.getPackageInfo(packageName, 0);
                Log.d(TAG, "应用已安装: " + packageName);
            } catch (PackageManager.NameNotFoundException e) {
                Log.d(TAG, "应用未安装: " + packageName);
                return false;
            }
            
            Log.d(TAG, "应用未运行: " + packageName);
            return false;
            
        } catch (SecurityException e) {
            Log.w(TAG, "权限不足: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "检测应用状态失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 切换到导航模式（高德地图悬浮窗模式）
     */
    private void switchToNavigationMode() {
        if (currentNavMode == MODE_NAVIGATION) {
            return;
        }
        
        if (navModeContainer != null && drivingModeContainer != null && pipModeContainer != null) {
            navModeContainer.setVisibility(View.VISIBLE);
            drivingModeContainer.setVisibility(View.GONE);
            pipModeContainer.setVisibility(View.GONE);
            
            currentNavMode = MODE_NAVIGATION;
        }
    }
    
    /**
     * 切换到画中画模式（高德地图画中画模式）
     */
    private void switchToPipMode() {
        if (currentNavMode == MODE_PIP) {
            return;
        }
        
        if (navModeContainer != null && drivingModeContainer != null && pipModeContainer != null) {
            navModeContainer.setVisibility(View.GONE);
            drivingModeContainer.setVisibility(View.GONE);
            pipModeContainer.setVisibility(View.VISIBLE);
            
            if (textPipStatus != null) {
                textPipStatus.setText("高德地图画中画");
            }
            
            currentNavMode = MODE_PIP;
            
            Log.d(TAG, "已切换到画中画模式");
        }
    }
    
    /**
     * 启动高德地图导航
     */
    private void launchAmapNavigation() {
        try {
            PackageManager pm = getPackageManager();
            
            Intent intent = pm.getLaunchIntentForPackage(AMAP_PACKAGE);
            
            if (intent == null) {
                intent = pm.getLaunchIntentForPackage(AMAP_STANDALONE_PACKAGE);
            }
            
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                Log.d(TAG, "已启动高德地图");
            } else {
                Toast.makeText(this, "未找到高德地图应用", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "未找到高德地图应用");
            }
        } catch (Exception e) {
            Log.e(TAG, "启动高德地图失败: " + e.getMessage());
            Toast.makeText(this, "启动高德地图失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 将高德地图切换到前台
     */
    private void bringAmapToFront() {
        try {
            android.app.ActivityManager activityManager = 
                (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
            
            if (activityManager != null) {
                List<android.app.ActivityManager.RunningTaskInfo> runningTasks = 
                    activityManager.getRunningTasks(1);
                
                for (android.app.ActivityManager.RunningTaskInfo taskInfo : runningTasks) {
                    if (taskInfo.topActivity != null) {
                        String packageName = taskInfo.topActivity.getPackageName();
                        
                        if (packageName.equals(AMAP_PACKAGE) || 
                            packageName.equals(AMAP_STANDALONE_PACKAGE)) {
                            
                            activityManager.moveTaskToFront(
                                taskInfo.id, 
                                android.app.ActivityManager.MOVE_TASK_WITH_HOME
                            );
                            Log.d(TAG, "已将高德地图切换到前台");
                            return;
                        }
                    }
                }
                
                launchAmapNavigation();
            }
        } catch (SecurityException e) {
            Log.w(TAG, "无法切换高德地图到前台: " + e.getMessage());
            launchAmapNavigation();
        } catch (Exception e) {
            Log.e(TAG, "切换高德地图到前台失败: " + e.getMessage());
            launchAmapNavigation();
        }
    }
    
    /**
     * 初始化壁纸
     */
    private void initWallpaper() {
        // 创建壁纸 ImageView
        wallpaperImageView = new ImageView(this);
        wallpaperImageView.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
        wallpaperImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        
        // 将壁纸添加到容器底部
        desktopContainer.addView(wallpaperImageView, 0);
        
        // 设置初始壁纸
        setWallpaper(currentWallpaperIndex);
        
        // 设置手势监听
        setupWallpaperGestureListener();
    }
    
    /**
     * 设置壁纸
     */
    private void setWallpaper(int index) {
        if (wallpaperImageView == null || index < 0 || index >= wallpaperPaths.length) {
            return;
        }
        
        try {
            InputStream inputStream = getAssets().open(wallpaperPaths[index]);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            wallpaperImageView.setImageBitmap(bitmap);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 设置手势监听支持左右滑动切换壁纸
     */
    private void setupWallpaperGestureListener() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) {
                    return false;
                }
                
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();
                
                // 判断是否为水平滑动
                if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > 100) {
                    if (diffX > 0) {
                        // 向右滑动 - 显示上一个壁纸
                        previousWallpaper();
                    } else {
                        // 向左滑动 - 显示下一个壁纸
                        nextWallpaper();
                    }
                    return true;
                }
                return false;
            }
        });
        
        desktopContainer.setOnTouchListener((v, event) -> {
            return gestureDetector.onTouchEvent(event);
        });
    }
    
    /**
     * 切换到下一个壁纸
     */
    private void nextWallpaper() {
        currentWallpaperIndex = (currentWallpaperIndex + 1) % wallpaperPaths.length;
        setWallpaper(currentWallpaperIndex);
        Toast.makeText(this, "壁纸 " + (currentWallpaperIndex + 1) + "/" + wallpaperPaths.length, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 切换到上一个壁纸
     */
    private void previousWallpaper() {
        currentWallpaperIndex = (currentWallpaperIndex - 1 + wallpaperPaths.length) % wallpaperPaths.length;
        setWallpaper(currentWallpaperIndex);
        Toast.makeText(this, "壁纸 " + (currentWallpaperIndex + 1) + "/" + wallpaperPaths.length, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 从 assets 目录加载图标图片并设置到对应的 ImageView
     */
    private void loadIconsFromAssets() {
        // 导航按钮图标
        loadImageFromAssets("webui/img/navbar/sys_nav_bar_back.png", imgBack);
        loadImageFromAssets("webui/img/navbar/sys_nav_bar_home.png", imgHome);
        loadImageFromAssets("webui/img/navbar/sys_nav_bar_recents.png", imgRecent);
        
        // 空调控制图标
        loadImageFromAssets("webui/img/navbar/ac_bar_power0.png", imgAcPower);
        loadImageFromAssets("webui/img/navbar/air_control_jian.png", imgAcMinus);
        loadImageFromAssets("webui/img/navbar/air_control_jia.png", imgAcPlus);
        loadImageFromAssets("webui/img/navbar/ac_bar_front_defrost0.png", imgDefrostFront);
        
        // 风向图标 - 初始化时设置默认状态
        updateAirDirectionIcon();
        
        // 设置图标（使用汽车 logo 图标）
        loadImageFromAssets("webui/img/navbar/sys_nav_bar_car.png", imgSettings);

        // 显示应用图标（九宫格）
        ImageView imgShowApps = findViewById(R.id.img_show_apps);
        if (imgShowApps != null) {
            loadImageFromAssets("webui/img/navbar/sys_nav_bar_apps.png", imgShowApps);
        }

        // 后除霜图标 - 使用前除霜图标作为备用
        loadImageFromAssets("webui/img/navbar/ac_bar_front_defrost0.png", imgDefrostRear);
    }
    
    /**
     * 更新风向图标，根据当前风向状态显示对应的图片
     * 风向状态: 0-7 对应 ac_bar_wind_mode_0 到 ac_bar_wind_mode_7
     */
    private void updateAirDirectionIcon() {
        String assetPath = String.format("webui/img/navbar/ac_bar_wind_mode_%d.png", airDirection);
        loadImageFromAssets(assetPath, imgAirDirection);
    }
    
    /**
     * 从 assets 目录加载状态栏图标
     */
    private void loadStatusBarIcons() {
        // 加载语音助手图标
        loadImageFromAssets("webui/img/statusbar/statusbar_intelligent_voice.png", imgVoiceAssistant);
        
        // 加载 WiFi 图标
        loadImageFromAssets("webui/img/statusbar/statusbar_wifi_4.png", imgWifi);
        
        // 加载蓝牙图标
        updateBluetoothIcon();
        
        // 加载 4G 信号图标
        loadImageFromAssets("webui/img/statusbar/statusbar_data_signal_level_4.png", img4gSignal);
        
        // 加载行车记录仪图标
        updateDrivingRecorderIcon();
    }
    
    /**
     * 更新蓝牙图标状态
     */
    private void updateBluetoothIcon() {
        if (isBluetoothOn) {
            loadImageFromAssets("webui/img/statusbar/statusbar_bluetooth1.png", imgBluetooth);
        } else {
            loadImageFromAssets("webui/img/statusbar/statusbar_bluetooth0.png", imgBluetooth);
        }
    }
    
    /**
     * 更新行车记录仪图标状态
     */
    private void updateDrivingRecorderIcon() {
        if (isDrivingRecorderOn) {
            loadImageFromAssets("webui/img/statusbar/statusbar_driving_recorder_1.png", imgDrivingRecorder);
        } else {
            loadImageFromAssets("webui/img/statusbar/statusbar_driving_recorder_0.png", imgDrivingRecorder);
        }
    }
    
    /**
     * 更新档位显示，当前档位显示绿色，其他档位显示灰色
     */
    private void updateGearDisplay() {
        int greenColor = Color.parseColor("#00FF88"); // 亮绿色，更好的可见性
        int semiTransparentWhite = Color.parseColor("#B0FFFFFF"); // 半透明白色
        
        textGearP.setTextColor(currentGear == 0 ? greenColor : semiTransparentWhite);
        textGearR.setTextColor(currentGear == 1 ? greenColor : semiTransparentWhite);
        textGearN.setTextColor(currentGear == 2 ? greenColor : semiTransparentWhite);
        textGearD.setTextColor(currentGear == 3 ? greenColor : semiTransparentWhite);
    }
    
    /**
     * 更新仅显示时间（时分格式）
     */
    private void updateTimeOnly() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        if (textTimeOnly != null) {
            textTimeOnly.setText(timeFormat.format(new Date()));
        }
    }
    
    /**
     * 从 assets 目录加载图片并设置到 ImageView
     * @param assetPath assets 目录下的图片路径
     * @param imageView 要设置图片的 ImageView
     */
    private void loadImageFromAssets(String assetPath, ImageView imageView) {
        if (imageView == null) {
            return;
        }
        
        try {
            InputStream inputStream = getAssets().open(assetPath);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            imageView.setImageBitmap(bitmap);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        // displayDesktopIcons(); // 图标网格已移除
        
        if (cardManager != null) {
            cardManager.setInstalledApps(installedApps);
        }
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

        Date currentDate = new Date();
        if (textTimeOnly != null) {
            textTimeOnly.setText(timeFormat.format(currentDate));
        }
    }

    private void setupClickListeners() {
        // 语音助手按钮
        if (imgVoiceAssistant != null) {
            imgVoiceAssistant.setOnClickListener(v -> {
                Toast.makeText(this, "唤醒小迪语音", Toast.LENGTH_SHORT).show();
            });
        }
        
        // 档位显示区域 - 点击切换档位
        View gearDisplay = findViewById(R.id.gear_display);
        if (gearDisplay != null) {
            gearDisplay.setOnClickListener(v -> {
                currentGear = (currentGear + 1) % 4;
                updateGearDisplay();
                String[] gears = {"P档", "R档", "N档", "D档"};
                Toast.makeText(this, gears[currentGear], Toast.LENGTH_SHORT).show();
            });
        }
        
        // WiFi 图标 - 点击切换状态
        if (imgWifi != null) {
            imgWifi.setOnClickListener(v -> {
                isWifiOn = !isWifiOn;
                if (isWifiOn) {
                    loadImageFromAssets("webui/img/statusbar/statusbar_wifi_4.png", imgWifi);
                    Toast.makeText(this, "WiFi 已开启", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "WiFi 已关闭", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // 蓝牙图标 - 点击切换状态
        if (imgBluetooth != null) {
            imgBluetooth.setOnClickListener(v -> {
                isBluetoothOn = !isBluetoothOn;
                updateBluetoothIcon();
                String message = isBluetoothOn ? "蓝牙已开启" : "蓝牙已关闭";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            });
        }
        
        // 4G 信号图标 - 点击切换状态
        if (img4gSignal != null) {
            img4gSignal.setOnClickListener(v -> {
                is4gSignalOn = !is4gSignalOn;
                String message = is4gSignalOn ? "4G信号已开启" : "4G信号已关闭";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            });
        }
        
        // 行车记录仪图标 - 点击切换状态
        if (imgDrivingRecorder != null) {
            imgDrivingRecorder.setOnClickListener(v -> {
                isDrivingRecorderOn = !isDrivingRecorderOn;
                updateDrivingRecorderIcon();
                String message = isDrivingRecorderOn ? "行车记录仪已开启" : "行车记录仪已关闭";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            });
        }
        
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
                airDirection = (airDirection + 1) % 8;
                updateAirDirectionIcon();
                String[] directions = {"模式1", "模式2", "模式3", "模式4", "模式5", "模式6", "模式7", "模式8"};
                Toast.makeText(this, "当前风向: " + directions[airDirection], Toast.LENGTH_SHORT).show();
            });
        }

        // 桌面设置
        View btnDesktopSettings = findViewById(R.id.btn_desktop_settings);
        if (btnDesktopSettings != null) {
            btnDesktopSettings.setOnClickListener(v -> showSettingsDialog());
        }

        // 显示应用弹窗（点击启动 AppDrawerActivity）
        View btnShowApps = findViewById(R.id.btn_show_apps);
        if (btnShowApps != null) {
            btnShowApps.setOnClickListener(v -> {
                try {
                    Log.i(TAG, "【应用抽屉】启动 AppDrawerActivity");
                    Intent drawerIntent = new Intent(this, cn.youth996.ui.ui.AppDrawerActivity.class);
                    drawerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(drawerIntent);
                } catch (Exception e) {
                    Log.e(TAG, "启动 AppDrawerActivity 失败: " + e.getMessage(), e);
                    Toast.makeText(this, "无法打开应用列表", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // ==================== 悬浮地图状态切换按钮 ====================
        // 单个按钮：根据当前是否显示悬浮地图，切换图标和点击行为
        //   - 未显示（默认） → 显示地图图标，点击 → 开启悬浮地图
        //   - 已显示         → 显示关闭图标，点击 → 关闭悬浮地图
        View btnFloatingMap = findViewById(R.id.btn_floating_map);
        if (btnFloatingMap != null) {
            btnFloatingMap.setOnClickListener(v -> {
                if (cn.youth996.ui.core.FloatingWindowManager.isAmapFloatShowing()) {
                    // 当前已显示 → 点击关闭
                    Log.i(TAG, "【悬浮地图】点击关闭悬浮");
                    cn.youth996.ui.core.FloatingWindowManager.hideAmapFloatMap(this);
                    Toast.makeText(this, "悬浮地图已关闭", Toast.LENGTH_SHORT).show();
                } else {
                    // 当前未显示 → 检查安装后开启
                    Log.i(TAG, "【悬浮地图】点击开启悬浮");
                    if (!cn.youth996.ui.core.FloatingWindowManager.isAmapInstalled(this)) {
                        Log.w(TAG, "【悬浮地图】高德地图未安装");
                        Toast.makeText(this, "请先安装高德地图车机版", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    cn.youth996.ui.core.FloatingWindowManager.showAmapFloatMap(this);
                    Toast.makeText(this, "悬浮地图已显示", Toast.LENGTH_SHORT).show();
                }
                refreshFloatingMapButtonIcon();
            });
        }

        // 初始状态刷新按钮图标
        refreshFloatingMapButtonIcon();
    }

    /**
     * 根据当前悬浮地图显示状态，刷新 taskbar 中状态按钮的图标。
     *   - 未显示 → 地图图标（开启悬浮）
     *   - 已显示 → 关闭图标（关闭悬浮）
     */
    private void refreshFloatingMapButtonIcon() {
        ImageView img = findViewById(R.id.img_floating_state_icon);
        if (img == null) return;
        if (cn.youth996.ui.core.FloatingWindowManager.isAmapFloatShowing()) {
            img.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        } else {
            img.setImageResource(android.R.drawable.ic_dialog_map);
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
        SettingsDialogFragment settingsDialog = new SettingsDialogFragment();
        settingsDialog.show(getSupportFragmentManager(), "settings");
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
        hideSystemUI();
        updateTimeDisplay();
        startNavMonitor();
        
        // 回到桌面时，如果用户开启了悬浮地图，自动恢复显示
        cn.youth996.ui.core.FloatingWindowManager.resumeAmapFloatMap(this);
        
        handler.postDelayed(() -> {
            checkNotificationPermissionOnResume();
        }, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 切换到其他应用时，自动隐藏悬浮地图（保留用户开启状态，回桌面会自动恢复）
        cn.youth996.ui.core.FloatingWindowManager.pauseAmapFloatMap(this);
    }
    
    /**
     * 在 onResume 时检查通知监听器权限
     * 当从设置页面返回或 ADB 授权后自动重新监听
     */
    private void checkNotificationPermissionOnResume() {
        if (musicPlayerManager != null) {
            android.util.Log.i(TAG, "【权限恢复检查】正在检查通知监听器权限状态...");
            
            NotificationMonitor monitor = getNotificationMonitor();
            if (monitor != null) {
                boolean hasPermission = monitor.checkNotificationListenerPermission();
                
                if (hasPermission) {
                    android.util.Log.i(TAG, "【权限恢复检查】✓ 权限已授予");
                    if (!monitor.isListeningForNotifications()) {
                        android.util.Log.i(TAG, "【权限恢复检查】正在重新启动监听...");
                        monitor.restartMonitoring();
                    }
                } else {
                    android.util.Log.w(TAG, "【权限恢复检查】✗ 权限未授予，需要通过 ADB 授权");
                }
            }
        }
    }
    
    /**
     * 获取 NotificationMonitor 实例
     */
    private NotificationMonitor getNotificationMonitor() {
        if (musicPlayerManager != null) {
            return musicPlayerManager.getNotificationMonitor();
        }
        return null;
    }
    
    /**
     * 重新启动音乐监听
     * ADB 权限授予后由 SettingsSystemFragment 调用
     */
    public void restartMusicMonitoring() {
        android.util.Log.i(TAG, "【音乐监听重启】开始重启音乐监听...");
        
        if (musicPlayerManager != null) {
            musicPlayerManager.restartListening();
            android.util.Log.i(TAG, "【音乐监听重启】✓ 音乐监听已重新启动");
        } else {
            android.util.Log.w(TAG, "【音乐监听重启】✗ MusicPlayerManager 未初始化");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && timeUpdateRunnable != null) {
            handler.removeCallbacks(timeUpdateRunnable);
        }
        stopNavMonitor();
        instance = null;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public static MainActivity getInstance() {
        return instance;
    }
}
