package cn.youth996.ui.manager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.youth996.ui.R;
import cn.youth996.ui.core.AppInfo;
import cn.youth996.ui.core.FloatingWindowManager;

public class CardManager {

    private Activity activity;
    private LinearLayout cardContainer;
    private HorizontalScrollView cardScrollView;
    private Handler handler;
    
    private View navigationCard;
    private View musicCard;
    private View vehicleControlCard;
    private View tirePressureCard;
    private View frequentAppsCard;
    
    private boolean isPlaying = false;
    
    private List<AppInfo> frequentApps;
    private List<AppInfo> installedApps;
    private SharedPreferences prefs;
    
    private static final String PREFS_NAME = "frequent_apps_prefs";
    private static final String KEY_FREQUENT_APPS = "frequent_apps";
    private static final int MAX_FREQUENT_APPS = 3;
    
    public CardManager(Activity activity, LinearLayout cardContainer, HorizontalScrollView cardScrollView) {
        this.activity = activity;
        this.cardContainer = cardContainer;
        this.cardScrollView = cardScrollView;
        this.handler = new Handler(Looper.getMainLooper());
        this.frequentApps = new ArrayList<>();
        this.installedApps = new ArrayList<>();
        this.prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public void setInstalledApps(List<AppInfo> apps) {
        this.installedApps = apps;
        loadFrequentApps();
        updateFrequentAppsDisplay();
    }
    
    public void initializeCards() {
        LayoutInflater inflater = LayoutInflater.from(activity);
        
        navigationCard = inflater.inflate(R.layout.card_navigation, cardContainer, false);
        musicCard = inflater.inflate(R.layout.card_music, cardContainer, false);
        vehicleControlCard = inflater.inflate(R.layout.card_vehicle_control, cardContainer, false);
        tirePressureCard = inflater.inflate(R.layout.card_tire_pressure, cardContainer, false);
        frequentAppsCard = inflater.inflate(R.layout.card_frequent_apps, cardContainer, false);
        
        // 底部小卡片（倒数第二排）高度：因中间主卡调整为屏幕 80%，
        // 这里压缩到屏幕 5%，兜底 60~100dp，给中间主卡让出空间。
        float density = activity.getResources().getDisplayMetrics().density;
        float screenHeightDp = activity.getResources().getDisplayMetrics().heightPixels / density;
        int targetCardHeightDp = (int) (screenHeightDp * 0.13f);
        targetCardHeightDp = Math.max(60, Math.min(100, targetCardHeightDp));
        int targetCardHeightPx = (int) (targetCardHeightDp * density);
        int buttonSizeDp = Math.max(48, targetCardHeightDp - 24);
        int buttonSizePx = (int) (buttonSizeDp * density);
        int iconSizeDp = (int) (buttonSizeDp * 0.7f);
        int iconSizePx = (int) (iconSizeDp * density);
        int cardPaddingDp = Math.max(10, (int) (buttonSizeDp * 0.22f));
        int cardPaddingPx = (int) (cardPaddingDp * density);
        for (View card : new View[]{navigationCard, musicCard, vehicleControlCard,
                tirePressureCard, frequentAppsCard}) {
            card.getLayoutParams().height = targetCardHeightPx;
            card.setPadding(cardPaddingPx, cardPaddingPx, cardPaddingPx, cardPaddingPx);
            // 胎压卡不参与图标尺寸重写：它的汽车俯视图需要保持自身比例，
            // 被 scaleCardInnerViews 二次缩放后会变得过小。
            if (card != tirePressureCard) {
                scaleCardInnerViews(card, buttonSizePx, iconSizePx, density);
            }
        }
        
        cardContainer.addView(navigationCard);
        cardContainer.addView(musicCard);
        cardContainer.addView(vehicleControlCard);
        cardContainer.addView(tirePressureCard);
        cardContainer.addView(frequentAppsCard);
        
        setupNavigationCard();
        setupMusicCard();
        setupVehicleControlCard();
        setupTirePressureCard();
        setupFrequentAppsCard();
    }
    
    private void setupNavigationCard() {
        ImageView imgNavIcon = navigationCard.findViewById(R.id.img_nav_icon);
        ImageView imgNavHome = navigationCard.findViewById(R.id.img_nav_home);
        ImageView imgNavCompany = navigationCard.findViewById(R.id.img_nav_company);
        View btnNavIcon = navigationCard.findViewById(R.id.btn_nav_icon);
        View btnNavHome = navigationCard.findViewById(R.id.btn_nav_home);
        View btnNavCompany = navigationCard.findViewById(R.id.btn_nav_company);

        try {
            InputStream is = activity.getAssets().open("webui/img/widget/automap_logo.png");
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            imgNavIcon.setImageBitmap(bitmap);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            InputStream is = activity.getAssets().open("webui/img/widget/nav_btn_home.png");
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            imgNavHome.setImageBitmap(bitmap);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        try {
            InputStream is = activity.getAssets().open("webui/img/widget/nav_btn_gongsi.png");
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            imgNavCompany.setImageBitmap(bitmap);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        btnNavIcon.setOnClickListener(v -> openMapApp());

        btnNavHome.setOnClickListener(v -> naviGoHome());

        btnNavCompany.setOnClickListener(v -> naviGoCompany());

        navigationCard.setOnClickListener(v -> {
        });
    }

    // 高德车机版遥控协议（均在 com.autonavi.amapauto 上实测验证）：
    // 回家：标准广播 AUTONAVI_STANDARD_BROADCAST_RECV，KEY_TYPE=10040
    // 去公司：桌面小部件广播 <包名>.AUTO_WIDGET_GO_COMPANY（标准协议无对应 KEY_TYPE）
    // 两者在地址已设置时直接发起导航，未设置时打开地址设置页
    private static final String AMAP_BROADCAST_ACTION = "AUTONAVI_STANDARD_BROADCAST_RECV";
    private static final int AMAP_KEY_TYPE_GO_HOME = 10040;
    private static final String AMAP_WIDGET_ACTION_GO_COMPANY = ".AUTO_WIDGET_GO_COMPANY";

    /** 打开设置中配置的默认地图应用 */
    private void openMapApp() {
        String pkg = FloatingWindowManager.getMapPackage(activity);
        try {
            Intent launchIntent = activity.getPackageManager().getLaunchIntentForPackage(pkg);
            if (launchIntent == null) {
                Toast.makeText(activity, "未安装地图应用: " + pkg, Toast.LENGTH_SHORT).show();
                return;
            }
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(launchIntent);
        } catch (Exception e) {
            Toast.makeText(activity, "打开地图失败", Toast.LENGTH_SHORT).show();
        }
    }

    /** 导航回家：高德车机版标准广播协议 KEY_TYPE=10040 */
    private void naviGoHome() {
        String pkg = FloatingWindowManager.getMapPackage(activity);
        try {
            Intent intent = new Intent(AMAP_BROADCAST_ACTION);
            intent.setPackage(pkg);
            intent.putExtra("KEY_TYPE", AMAP_KEY_TYPE_GO_HOME);
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            activity.sendBroadcast(intent);
            Toast.makeText(activity, "开始导航回家", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(activity, "发送导航指令失败", Toast.LENGTH_SHORT).show();
        }
    }

    /** 导航去公司：高德车机版小部件广播（实测有效，未设公司地址时会打开设置页） */
    private void naviGoCompany() {
        String pkg = FloatingWindowManager.getMapPackage(activity);
        try {
            Intent intent = new Intent(pkg + AMAP_WIDGET_ACTION_GO_COMPANY);
            intent.setPackage(pkg);
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            activity.sendBroadcast(intent);
            Toast.makeText(activity, "开始导航去公司", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(activity, "发送导航指令失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupMusicCard() {
        ImageView imgAlbumCover = musicCard.findViewById(R.id.img_album_cover);
        TextView textSongTitle = musicCard.findViewById(R.id.text_song_title);
        TextView textSongArtist = musicCard.findViewById(R.id.text_song_artist);
        ImageView btnPrevious = musicCard.findViewById(R.id.btn_previous);
        ImageView btnPlayPause = musicCard.findViewById(R.id.btn_play_pause);
        ImageView btnNext = musicCard.findViewById(R.id.btn_next);
        
        try {
            InputStream is = activity.getAssets().open("webui/img/widget/music_cover.jpg");
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            imgAlbumCover.setImageBitmap(bitmap);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        textSongTitle.setText("演员");
        textSongArtist.setText("薛之谦");
        
        btnPrevious.setOnClickListener(v -> {
            Toast.makeText(activity, "上一首", Toast.LENGTH_SHORT).show();
        });
        
        btnPlayPause.setOnClickListener(v -> {
            isPlaying = !isPlaying;
            btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        });
        
        btnNext.setOnClickListener(v -> {
            Toast.makeText(activity, "下一首", Toast.LENGTH_SHORT).show();
        });
        
        musicCard.setOnClickListener(v -> {
            Toast.makeText(activity, "打开音乐应用", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void setupVehicleControlCard() {
        ImageView imgCar = vehicleControlCard.findViewById(R.id.img_car);
        ImageView imgLockUnlock = vehicleControlCard.findViewById(R.id.img_lock_unlock);
        ImageView imgTrunk = vehicleControlCard.findViewById(R.id.img_trunk);
        View btnLockUnlock = vehicleControlCard.findViewById(R.id.btn_lock_unlock);
        View btnTrunk = vehicleControlCard.findViewById(R.id.btn_trunk);
        
        try {
            InputStream is = activity.getAssets().open("webui/img/widget/byd_song.png");
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            imgCar.setImageBitmap(bitmap);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        try {
            InputStream is = activity.getAssets().open("webui/img/widget/car_lock2.png");
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            imgLockUnlock.setImageBitmap(bitmap);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        try {
            InputStream is = activity.getAssets().open("webui/img/widget/control_trunck0.png");
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            imgTrunk.setImageBitmap(bitmap);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        btnLockUnlock.setOnClickListener(v -> {
            try {
                boolean isLocked = imgLockUnlock.getTag() != null && (boolean) imgLockUnlock.getTag();
                
                if (isLocked) {
                    InputStream is = activity.getAssets().open("webui/img/widget/car_lock2.png");
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    imgLockUnlock.setImageBitmap(bitmap);
                    is.close();
                    Toast.makeText(activity, "车辆已解锁", Toast.LENGTH_SHORT).show();
                } else {
                    InputStream is = activity.getAssets().open("webui/img/widget/car_lock1.png");
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    imgLockUnlock.setImageBitmap(bitmap);
                    is.close();
                    Toast.makeText(activity, "车辆已上锁", Toast.LENGTH_SHORT).show();
                }
                
                imgLockUnlock.setTag(!isLocked);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        
        btnTrunk.setOnClickListener(v -> {
            try {
                boolean isTrunkOpen = imgTrunk.getTag() != null && (boolean) imgTrunk.getTag();
                
                if (isTrunkOpen) {
                    InputStream is = activity.getAssets().open("webui/img/widget/control_trunck0.png");
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    imgTrunk.setImageBitmap(bitmap);
                    is.close();
                    Toast.makeText(activity, "后备箱已关闭", Toast.LENGTH_SHORT).show();
                } else {
                    InputStream is = activity.getAssets().open("webui/img/widget/control_trunck1.png");
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    imgTrunk.setImageBitmap(bitmap);
                    is.close();
                    Toast.makeText(activity, "后备箱已打开", Toast.LENGTH_SHORT).show();
                }
                
                imgTrunk.setTag(!isTrunkOpen);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        
        imgLockUnlock.setTag(false);
        imgTrunk.setTag(false);
    }
    
    private void setupTirePressureCard() {
        ImageView imgCarTy = tirePressureCard.findViewById(R.id.img_car_ty);
        
        try {
            InputStream is = activity.getAssets().open("webui/img/widget/car_ty.png");
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            imgCarTy.setImageBitmap(bitmap);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        TextView textTireFl = tirePressureCard.findViewById(R.id.text_tire_fl);
        TextView textTireFr = tirePressureCard.findViewById(R.id.text_tire_fr);
        TextView textTireRl = tirePressureCard.findViewById(R.id.text_tire_rl);
        TextView textTireRr = tirePressureCard.findViewById(R.id.text_tire_rr);

        textTireFl.setText("2.3");
        textTireFr.setText("2.4");
        textTireRl.setText("2.3");
        textTireRr.setText("2.5");
        
        updateTirePressureColor(textTireFl, 2.3f);
        updateTirePressureColor(textTireFr, 2.4f);
        updateTirePressureColor(textTireRl, 2.3f);
        updateTirePressureColor(textTireRr, 2.5f);
        
        tirePressureCard.setOnClickListener(v -> {
            Toast.makeText(activity, "打开胎压详情", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void updateTirePressureColor(TextView textView, float pressure) {
        if (pressure < 2.0f) {
            textView.setTextColor(activity.getResources().getColor(R.color.card_error));
        } else if (pressure > 2.7f) {
            textView.setTextColor(activity.getResources().getColor(R.color.card_warning));
        } else {
            textView.setTextColor(activity.getResources().getColor(R.color.card_accent));
        }
    }
    
    private void setupFrequentAppsCard() {
        LinearLayout btnApp1 = frequentAppsCard.findViewById(R.id.btn_app_1);
        LinearLayout btnApp2 = frequentAppsCard.findViewById(R.id.btn_app_2);
        LinearLayout btnApp3 = frequentAppsCard.findViewById(R.id.btn_app_3);
        LinearLayout btnManageApps = frequentAppsCard.findViewById(R.id.btn_manage_apps);
        
        TextView textApp1 = frequentAppsCard.findViewById(R.id.text_app_1);
        TextView textApp2 = frequentAppsCard.findViewById(R.id.text_app_2);
        TextView textApp3 = frequentAppsCard.findViewById(R.id.text_app_3);
        
        btnApp1.setOnClickListener(v -> {
            if (!frequentApps.isEmpty()) {
                launchApp(frequentApps.get(0));
            }
        });
        
        btnApp2.setOnClickListener(v -> {
            if (frequentApps.size() > 1) {
                launchApp(frequentApps.get(1));
            }
        });
        
        btnApp3.setOnClickListener(v -> {
            if (frequentApps.size() > 2) {
                launchApp(frequentApps.get(2));
            }
        });
        
        btnApp1.setOnLongClickListener(v -> {
            if (!frequentApps.isEmpty()) {
                removeFrequentApp(0);
            }
            return true;
        });
        
        btnApp2.setOnLongClickListener(v -> {
            if (frequentApps.size() > 1) {
                removeFrequentApp(1);
            }
            return true;
        });
        
        btnApp3.setOnLongClickListener(v -> {
            if (frequentApps.size() > 2) {
                removeFrequentApp(2);
            }
            return true;
        });
        
        btnManageApps.setOnClickListener(v -> {
            showAppSelectionDialog();
        });
    }
    
    private void loadFrequentApps() {
        frequentApps.clear();
        Set<String> savedPackages = prefs.getStringSet(KEY_FREQUENT_APPS, new HashSet<>());
        
        for (String packageName : savedPackages) {
            for (AppInfo app : installedApps) {
                if (app.packageName.equals(packageName)) {
                    frequentApps.add(app);
                    break;
                }
            }
        }
    }
    
    private void saveFrequentApps() {
        Set<String> packageNames = new HashSet<>();
        for (AppInfo app : frequentApps) {
            packageNames.add(app.packageName);
        }
        prefs.edit().putStringSet(KEY_FREQUENT_APPS, packageNames).apply();
    }
    
    /**
     * 遍历卡片内部所有子 View，把固定 72dp 按钮和 48dp 图标按比例放大，
     * 使卡片整体视觉高度和元素大小都跟着目标高度变化。
     */
    private void scaleCardInnerViews(View root, int buttonSizePx, int iconSizePx, float density) {
        if (root == null) return;
        // 阈值：原值是 72dp / 48dp，对应 px 取决于 dpr。比较 px 更稳。
        int btnThreshold = (int) (65 * density);   // 65dp 及以上视为按钮
        int iconThreshold = (int) (40 * density);  // 40~64dp 视为图标
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                scaleCardInnerViews(vg.getChildAt(i), buttonSizePx, iconSizePx, density);
            }
        } else {
            android.view.ViewGroup.LayoutParams lp = root.getLayoutParams();
            if (lp == null) return;
            int curW = lp.width;
            int curH = lp.height;
            // 改固定 72dp 的按钮/容器
            if (curH >= btnThreshold && curH < btnThreshold + (int) (20 * density)
                    && curW == curH) {
                lp.width = buttonSizePx;
                lp.height = buttonSizePx;
                root.setLayoutParams(lp);
                // 如果是 ImageView，缩小到图标尺寸
                if (root instanceof ImageView) {
                    android.view.ViewGroup.LayoutParams imgLp = root.getLayoutParams();
                    imgLp.width = iconSizePx;
                    imgLp.height = iconSizePx;
                    root.setLayoutParams(imgLp);
                }
            }
            // 改固定 48dp 的图标
            if (root instanceof ImageView) {
                if (curH >= iconThreshold && curH < btnThreshold) {
                    lp.width = iconSizePx;
                    lp.height = iconSizePx;
                    root.setLayoutParams(lp);
                }
            }
        }
    }

    private void updateFrequentAppsDisplay() {
        ImageView imgApp1 = frequentAppsCard.findViewById(R.id.img_app_1);
        ImageView imgApp2 = frequentAppsCard.findViewById(R.id.img_app_2);
        ImageView imgApp3 = frequentAppsCard.findViewById(R.id.img_app_3);
        TextView textApp1 = frequentAppsCard.findViewById(R.id.text_app_1);
        TextView textApp2 = frequentAppsCard.findViewById(R.id.text_app_2);
        TextView textApp3 = frequentAppsCard.findViewById(R.id.text_app_3);
        
        if (frequentApps.size() > 0) {
            AppInfo app1 = frequentApps.get(0);
            imgApp1.setImageDrawable(app1.icon);
            textApp1.setText(app1.label);
        } else {
            imgApp1.setImageResource(android.R.drawable.ic_menu_help);
            textApp1.setText("");
        }
        
        if (frequentApps.size() > 1) {
            AppInfo app2 = frequentApps.get(1);
            imgApp2.setImageDrawable(app2.icon);
            textApp2.setText(app2.label);
        } else {
            imgApp2.setImageResource(android.R.drawable.ic_menu_help);
            textApp2.setText("");
        }
        
        if (frequentApps.size() > 2) {
            AppInfo app3 = frequentApps.get(2);
            imgApp3.setImageDrawable(app3.icon);
            textApp3.setText(app3.label);
        } else {
            imgApp3.setImageResource(android.R.drawable.ic_menu_help);
            textApp3.setText("");
        }
    }
    
    private void showAppSelectionDialog() {
        if (installedApps.isEmpty()) {
            Toast.makeText(activity, "没有可用的应用", Toast.LENGTH_SHORT).show();
            return;
        }
        
        List<AppInfo> availableApps = new ArrayList<>();
        for (AppInfo app : installedApps) {
            if (!frequentApps.contains(app)) {
                availableApps.add(app);
            }
        }
        
        if (availableApps.isEmpty()) {
            Toast.makeText(activity, "所有应用都已添加", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] appNames = new String[availableApps.size()];
        for (int i = 0; i < availableApps.size(); i++) {
            appNames[i] = availableApps.get(i).label;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("选择要添加的应用");
        builder.setItems(appNames, (dialog, which) -> {
            AppInfo selectedApp = availableApps.get(which);
            addFrequentApp(selectedApp);
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    private void addFrequentApp(AppInfo app) {
        if (frequentApps.size() >= MAX_FREQUENT_APPS) {
            Toast.makeText(activity, "常用应用已满，请先移除一个", Toast.LENGTH_SHORT).show();
            return;
        }
        
        frequentApps.add(app);
        saveFrequentApps();
        updateFrequentAppsDisplay();
        Toast.makeText(activity, "已添加 " + app.label + " 到常用应用", Toast.LENGTH_SHORT).show();
    }
    
    private void launchApp(AppInfo app) {
        if (app.intent != null) {
            activity.startActivity(app.intent);
        }
    }
    
    private void removeFrequentApp(int index) {
        if (index >= 0 && index < frequentApps.size()) {
            AppInfo app = frequentApps.get(index);
            frequentApps.remove(index);
            saveFrequentApps();
            updateFrequentAppsDisplay();
            Toast.makeText(activity, "已从常用应用移除 " + app.label, Toast.LENGTH_SHORT).show();
        }
    }
    
    public void updateMusicInfo(String title, String artist, String albumPath) {
        if (musicCard != null) {
            TextView textSongTitle = musicCard.findViewById(R.id.text_song_title);
            TextView textSongArtist = musicCard.findViewById(R.id.text_song_artist);
            ImageView imgAlbumCover = musicCard.findViewById(R.id.img_album_cover);

            textSongTitle.setText(title);
            textSongArtist.setText(artist);

            if (albumPath != null && !albumPath.isEmpty()) {
                try {
                    InputStream is = activity.getAssets().open(albumPath);
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    imgAlbumCover.setImageBitmap(bitmap);
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 用真实的媒体数据更新音乐卡片（来自 MusicPlayerManager 的通知监听）
     */
    public void updateMusicInfo(String title, String artist, Bitmap albumArt) {
        if (musicCard == null) return;
        TextView textSongTitle = musicCard.findViewById(R.id.text_song_title);
        TextView textSongArtist = musicCard.findViewById(R.id.text_song_artist);
        ImageView imgAlbumCover = musicCard.findViewById(R.id.img_album_cover);

        if (textSongTitle != null) textSongTitle.setText(title);
        if (textSongArtist != null) textSongArtist.setText(artist);
        if (imgAlbumCover != null && albumArt != null) {
            imgAlbumCover.setImageBitmap(albumArt);
        }
    }

    /**
     * 同步音乐卡片的播放/暂停按钮状态
     */
    public void updateMusicPlayState(boolean playing) {
        if (musicCard == null) return;
        ImageView btnPlayPause = musicCard.findViewById(R.id.btn_play_pause);
        if (btnPlayPause != null) {
            btnPlayPause.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        }
    }

    /**
     * 将音乐卡片的控制按钮绑定到真实的 MusicPlayerManager
     */
    public void bindMusicCardControls(MusicPlayerManager manager) {
        if (musicCard == null || manager == null) return;

        // 清除写死的演示数据，等待真实媒体信息填充
        TextView textSongTitle = musicCard.findViewById(R.id.text_song_title);
        TextView textSongArtist = musicCard.findViewById(R.id.text_song_artist);
        if (textSongTitle != null) textSongTitle.setText("");
        if (textSongArtist != null) textSongArtist.setText("");

        View btnPrevious = musicCard.findViewById(R.id.btn_previous);
        View btnPlayPause = musicCard.findViewById(R.id.btn_play_pause);
        View btnNext = musicCard.findViewById(R.id.btn_next);

        if (btnPrevious != null) {
            btnPrevious.setOnClickListener(v -> manager.previousTrack());
        }
        if (btnPlayPause != null) {
            btnPlayPause.setOnClickListener(v -> manager.playPause());
        }
        if (btnNext != null) {
            btnNext.setOnClickListener(v -> manager.nextTrack());
        }
        musicCard.setOnClickListener(v -> manager.openMusicApp());
    }
    
    public void updateTirePressure(float fl, float fr, float rl, float rr) {
        if (tirePressureCard != null) {
            TextView textTireFl = tirePressureCard.findViewById(R.id.text_tire_fl);
            TextView textTireFr = tirePressureCard.findViewById(R.id.text_tire_fr);
            TextView textTireRl = tirePressureCard.findViewById(R.id.text_tire_rl);
            TextView textTireRr = tirePressureCard.findViewById(R.id.text_tire_rr);

            textTireFl.setText(String.format("%.1f", fl));
            textTireFr.setText(String.format("%.1f", fr));
            textTireRl.setText(String.format("%.1f", rl));
            textTireRr.setText(String.format("%.1f", rr));

            updateTirePressureColor(textTireFl, fl);
            updateTirePressureColor(textTireFr, fr);
            updateTirePressureColor(textTireRl, rl);
            updateTirePressureColor(textTireRr, rr);
        }
    }
}
