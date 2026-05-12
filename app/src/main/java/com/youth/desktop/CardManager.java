package com.youth.desktop;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
    private int currentProgress = 35;
    private Handler progressHandler;
    private Runnable progressRunnable;
    
    private List<AppInfo> frequentApps;
    
    public CardManager(Activity activity, LinearLayout cardContainer, HorizontalScrollView cardScrollView) {
        this.activity = activity;
        this.cardContainer = cardContainer;
        this.cardScrollView = cardScrollView;
        this.handler = new Handler(Looper.getMainLooper());
        this.progressHandler = new Handler(Looper.getMainLooper());
        this.frequentApps = new ArrayList<>();
    }
    
    public void initializeCards() {
        LayoutInflater inflater = LayoutInflater.from(activity);
        
        navigationCard = inflater.inflate(R.layout.card_navigation, cardContainer, false);
        musicCard = inflater.inflate(R.layout.card_music, cardContainer, false);
        vehicleControlCard = inflater.inflate(R.layout.card_vehicle_control, cardContainer, false);
        tirePressureCard = inflater.inflate(R.layout.card_tire_pressure, cardContainer, false);
        frequentAppsCard = inflater.inflate(R.layout.card_frequent_apps, cardContainer, false);
        
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
        
        startMusicProgressSimulation();
    }
    
    private void setupNavigationCard() {
        ImageView imgNavIcon = navigationCard.findViewById(R.id.img_nav_icon);
        TextView textNavStatus = navigationCard.findViewById(R.id.text_nav_status);
        TextView textNavDestination = navigationCard.findViewById(R.id.text_nav_destination);
        TextView textNavDistance = navigationCard.findViewById(R.id.text_nav_distance);
        TextView textNavEta = navigationCard.findViewById(R.id.text_nav_eta);
        TextView textNavRemaining = navigationCard.findViewById(R.id.text_nav_remaining);
        
        try {
            InputStream is = activity.getAssets().open("webui/img/navbar/sys_nav_bar_home.png");
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            imgNavIcon.setImageBitmap(bitmap);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        textNavStatus.setText("导航中");
        textNavDestination.setText("公司");
        textNavDistance.setText("12.5");
        textNavEta.setText("25");
        textNavRemaining.setText("8.2");
        
        navigationCard.setOnClickListener(v -> {
            Toast.makeText(activity, "打开导航应用", Toast.LENGTH_SHORT).show();
        });
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
            if (isPlaying) {
                btnPlayPause.setImageResource(R.drawable.ic_pause);
                startMusicProgressSimulation();
            } else {
                btnPlayPause.setImageResource(R.drawable.ic_play);
                stopMusicProgressSimulation();
            }
        });
        
        btnNext.setOnClickListener(v -> {
            Toast.makeText(activity, "下一首", Toast.LENGTH_SHORT).show();
        });
        
        musicCard.setOnClickListener(v -> {
            Toast.makeText(activity, "打开音乐应用", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void setupVehicleControlCard() {
        View btnUnlock = vehicleControlCard.findViewById(R.id.btn_unlock);
        View btnWindow = vehicleControlCard.findViewById(R.id.btn_window);
        View btnTrunk = vehicleControlCard.findViewById(R.id.btn_trunk);
        View btnFindCar = vehicleControlCard.findViewById(R.id.btn_find_car);
        
        btnUnlock.setOnClickListener(v -> {
            Toast.makeText(activity, "解锁车辆", Toast.LENGTH_SHORT).show();
        });
        
        btnWindow.setOnClickListener(v -> {
            Toast.makeText(activity, "车窗控制", Toast.LENGTH_SHORT).show();
        });
        
        btnTrunk.setOnClickListener(v -> {
            Toast.makeText(activity, "打开后备箱", Toast.LENGTH_SHORT).show();
        });
        
        btnFindCar.setOnClickListener(v -> {
            Toast.makeText(activity, "寻车功能已启动", Toast.LENGTH_SHORT).show();
        });
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
        TextView textTireTemp = tirePressureCard.findViewById(R.id.text_tire_temp);
        
        textTireFl.setText("2.3");
        textTireFr.setText("2.4");
        textTireRl.setText("2.3");
        textTireRr.setText("2.5");
        textTireTemp.setText("胎温: 45°C");
        
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
        LinearLayout btnApp4 = frequentAppsCard.findViewById(R.id.btn_app_4);
        
        TextView textApp1 = frequentAppsCard.findViewById(R.id.text_app_1);
        TextView textApp2 = frequentAppsCard.findViewById(R.id.text_app_2);
        TextView textApp3 = frequentAppsCard.findViewById(R.id.text_app_3);
        TextView textApp4 = frequentAppsCard.findViewById(R.id.text_app_4);
        
        textApp1.setText("导航");
        textApp2.setText("音乐");
        textApp3.setText("电话");
        textApp4.setText("设置");
        
        btnApp1.setOnClickListener(v -> {
            Toast.makeText(activity, "打开导航", Toast.LENGTH_SHORT).show();
        });
        
        btnApp2.setOnClickListener(v -> {
            Toast.makeText(activity, "打开音乐", Toast.LENGTH_SHORT).show();
        });
        
        btnApp3.setOnClickListener(v -> {
            Toast.makeText(activity, "打开电话", Toast.LENGTH_SHORT).show();
        });
        
        btnApp4.setOnClickListener(v -> {
            Toast.makeText(activity, "打开设置", Toast.LENGTH_SHORT).show();
        });
    }
    
    public void updateNavigationInfo(String destination, float distance, int eta, float remaining) {
        if (navigationCard != null) {
            TextView textNavDestination = navigationCard.findViewById(R.id.text_nav_destination);
            TextView textNavDistance = navigationCard.findViewById(R.id.text_nav_distance);
            TextView textNavEta = navigationCard.findViewById(R.id.text_nav_eta);
            TextView textNavRemaining = navigationCard.findViewById(R.id.text_nav_remaining);
            
            textNavDestination.setText(destination);
            textNavDistance.setText(String.format("%.1f", distance));
            textNavEta.setText(String.valueOf(eta));
            textNavRemaining.setText(String.format("%.1f", remaining));
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
    
    public void updateTirePressure(float fl, float fr, float rl, float rr, float temp) {
        if (tirePressureCard != null) {
            TextView textTireFl = tirePressureCard.findViewById(R.id.text_tire_fl);
            TextView textTireFr = tirePressureCard.findViewById(R.id.text_tire_fr);
            TextView textTireRl = tirePressureCard.findViewById(R.id.text_tire_rl);
            TextView textTireRr = tirePressureCard.findViewById(R.id.text_tire_rr);
            TextView textTireTemp = tirePressureCard.findViewById(R.id.text_tire_temp);
            
            textTireFl.setText(String.format("%.1f", fl));
            textTireFr.setText(String.format("%.1f", fr));
            textTireRl.setText(String.format("%.1f", rl));
            textTireRr.setText(String.format("%.1f", rr));
            textTireTemp.setText(String.format("胎温: %.0f°C", temp));
            
            updateTirePressureColor(textTireFl, fl);
            updateTirePressureColor(textTireFr, fr);
            updateTirePressureColor(textTireRl, rl);
            updateTirePressureColor(textTireRr, rr);
        }
    }
    
    public void updateFrequentApps(List<AppInfo> apps) {
        if (frequentAppsCard != null && apps != null && apps.size() >= 4) {
            this.frequentApps = apps;
            
            ImageView imgApp1 = frequentAppsCard.findViewById(R.id.img_app_1);
            ImageView imgApp2 = frequentAppsCard.findViewById(R.id.img_app_2);
            ImageView imgApp3 = frequentAppsCard.findViewById(R.id.img_app_3);
            ImageView imgApp4 = frequentAppsCard.findViewById(R.id.img_app_4);
            
            TextView textApp1 = frequentAppsCard.findViewById(R.id.text_app_1);
            TextView textApp2 = frequentAppsCard.findViewById(R.id.text_app_2);
            TextView textApp3 = frequentAppsCard.findViewById(R.id.text_app_3);
            TextView textApp4 = frequentAppsCard.findViewById(R.id.text_app_4);
            
            try {
                if (apps.get(0) != null && apps.get(0).label != null) {
                    textApp1.setText(apps.get(0).label);
                }
                if (apps.get(1) != null && apps.get(1).label != null) {
                    textApp2.setText(apps.get(1).label);
                }
                if (apps.get(2) != null && apps.get(2).label != null) {
                    textApp3.setText(apps.get(2).label);
                }
                if (apps.get(3) != null && apps.get(3).label != null) {
                    textApp4.setText(apps.get(3).label);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void startMusicProgressSimulation() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    currentProgress++;
                    if (currentProgress > 100) {
                        currentProgress = 0;
                    }
                    
                    SeekBar seekBar = musicCard.findViewById(R.id.seekbar_progress);
                    if (seekBar != null) {
                        seekBar.setProgress(currentProgress);
                    }
                    
                    TextView textCurrentTime = musicCard.findViewById(R.id.text_current_time);
                    if (textCurrentTime != null) {
                        int currentSeconds = (int) (currentProgress * 2.25);
                        int minutes = currentSeconds / 60;
                        int seconds = currentSeconds % 60;
                        textCurrentTime.setText(String.format("%d:%02d", minutes, seconds));
                    }
                    
                    progressHandler.postDelayed(this, 1000);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }
    
    private void stopMusicProgressSimulation() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
    }
    
    public void onDestroy() {
        stopMusicProgressSimulation();
    }
}
