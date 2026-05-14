package com.youth.desktop;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
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
    private LinearLayout homeButton;
    private TextView acTempText;
    private LinearLayout cardContainer;
    private HorizontalScrollView cardScrollView;
    private CardManager cardManager;
    private MusicPlayerManager musicPlayerManager;
    
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
        statusBar = findViewById(R.id.status_bar);
        taskbar = findViewById(R.id.taskbar);

        homeButton = findViewById(R.id.btn_home);
        acTempText = findViewById(R.id.ac_temp_text);

        // 图标网格已移除
        // iconGrid.setColumnCount(DESKTOP_COLUMNS);
        // iconGrid.setRowCount(DESKTOP_ROWS);
        
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
        
        TextView textLyric1 = findViewById(R.id.text_lyric_1);
        TextView textLyric2 = findViewById(R.id.text_lyric_2);
        TextView textLyricCurrent = findViewById(R.id.text_lyric_current_center);
        TextView textLyric4 = findViewById(R.id.text_lyric_4);
        TextView textLyric5 = findViewById(R.id.text_lyric_5);
        
        Log.d("MainActivity", "Creating MusicPlayerManager...");
        musicPlayerManager = new MusicPlayerManager(this);
        musicPlayerManager.bindViews(imgAlbumCoverCenter, imgMusicSource, textMusicSource,
                                     textSongTitle, textSongArtist, btnPlayPauseCenter);
        musicPlayerManager.bindLyricsViews(textLyric1, textLyric2, textLyricCurrent, textLyric4, textLyric5);
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
            public void onLyricsUpdate(String lyric1, String lyric2, String currentLyric, String lyric4, String lyric5, int index) {
                Log.d("MainActivity", "=== LYRICS UPDATE: index=" + index + ", current=" + currentLyric);
                runOnUiThread(() -> {
                    musicPlayerManager.updateLyrics(lyric1, lyric2, currentLyric, lyric4, lyric5);
                });
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
            Toast.makeText(this, "打开导航应用", Toast.LENGTH_SHORT).show();
        });
        
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
        
        // 设置图标
        loadImageFromAssets("webui/img/navbar/sys_nav_bar_apps.png", imgSettings);
        
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
        super.onBackPressed();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
