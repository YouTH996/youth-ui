package com.youth.desktop;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.session.MediaController;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MusicPlayerManager {
    private static final String TAG = "MusicPlayerManager";
    private static final long REFRESH_INTERVAL = 2000;
    
    private Activity activity;
    private Handler handler;
    private NotificationMonitor notificationMonitor;
    private Runnable refreshRunnable;
    
    private ImageView imgAlbumCover;
    private ImageView imgMusicSource;
    private TextView textMusicSource;
    private TextView textSongTitle;
    private TextView textSongArtist;
    private TextView textSongLyrics;
    private ImageView btnPlayPause;
    
    private String currentPackageName = "";
    private String currentTitle = "";
    private String currentArtist = "";
    private Bitmap currentAlbumArt = null;
    private boolean isPlaying = false;
    
    private OnMusicUpdateListener listener;
    
    public interface OnMusicUpdateListener {
        void onMusicUpdate(String packageName, String title, String artist, Bitmap albumArt, boolean isPlaying);
        void onPlaybackStateChanged(boolean isPlaying);
    }
    
    public MusicPlayerManager(Activity activity) {
        this.activity = activity;
        this.handler = new Handler(Looper.getMainLooper());
    }
    
    public void setOnMusicUpdateListener(OnMusicUpdateListener listener) {
        this.listener = listener;
    }
    
    public void bindViews(ImageView albumCover, ImageView musicSource, TextView sourceText, 
                         TextView titleText, TextView artistText, TextView lyricsText,
                         ImageView playPauseBtn) {
        this.imgAlbumCover = albumCover;
        this.imgMusicSource = musicSource;
        this.textMusicSource = sourceText;
        this.textSongTitle = titleText;
        this.textSongArtist = artistText;
        this.textSongLyrics = lyricsText;
        this.btnPlayPause = playPauseBtn;
    }
    
    public void startListening() {
        try {
            notificationMonitor = new NotificationMonitor(activity);
            notificationMonitor.setOnMediaInfoListener(new NotificationMonitor.OnMediaInfoListener() {
                @Override
                public void onMediaInfoChanged(String packageName, String title, String artist, Bitmap albumArt, boolean playing) {
                    handler.post(() -> {
                        if (!packageName.isEmpty()) {
                            currentPackageName = packageName;
                            currentTitle = title;
                            currentArtist = artist;
                            currentAlbumArt = albumArt;
                            isPlaying = playing;
                            
                            updateMusicUI();
                        }
                    });
                }
                
                @Override
                public void onNotificationServiceConnected() {
                    Log.d(TAG, "NotificationListenerService is now connected!");
                    Toast.makeText(activity, "已连接到通知服务", Toast.LENGTH_SHORT).show();
                }
            });
            
            notificationMonitor.startMonitoring();
            
            startPeriodicRefresh();
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting music listener: " + e.getMessage());
            Toast.makeText(activity, "启动音乐监听失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void startPeriodicRefresh() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                notificationMonitor.refreshFromNotificationService();
                handler.postDelayed(this, REFRESH_INTERVAL);
            }
        };
        handler.postDelayed(refreshRunnable, 1000);
    }
    
    private void updateMusicUI() {
        if (textSongTitle != null) {
            textSongTitle.setText(currentTitle);
        }
        
        if (textSongArtist != null) {
            textSongArtist.setText(currentArtist);
        }
        
        if (imgAlbumCover != null && currentAlbumArt != null) {
            imgAlbumCover.setImageBitmap(currentAlbumArt);
        }
        
        if (textMusicSource != null && !currentPackageName.isEmpty()) {
            String appName = getAppName(currentPackageName);
            textMusicSource.setText(appName);
        }
        
        if (imgMusicSource != null && !currentPackageName.isEmpty()) {
            Drawable appIcon = getAppIcon(currentPackageName);
            if (appIcon != null) {
                imgMusicSource.setImageDrawable(appIcon);
            }
        }
        
        if (textSongLyrics != null) {
            textSongLyrics.setText("♪ " + currentArtist + " - " + currentTitle);
        }
        
        if (btnPlayPause != null) {
            btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        }
        
        if (listener != null) {
            listener.onMusicUpdate(currentPackageName, currentTitle, currentArtist, 
                                   currentAlbumArt, isPlaying);
            listener.onPlaybackStateChanged(isPlaying);
        }
    }
    
    private String getAppName(String packageName) {
        try {
            if (packageName.contains(".")) {
                PackageManager pm = activity.getPackageManager();
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                return pm.getApplicationLabel(appInfo).toString();
            }
            return packageName;
        } catch (Exception e) {
            return packageName;
        }
    }
    
    private Drawable getAppIcon(String packageName) {
        try {
            if (packageName.contains(".")) {
                PackageManager pm = activity.getPackageManager();
                return pm.getApplicationIcon(packageName);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    public void playPause() {
        MediaController controller = notificationMonitor.getMediaController();
        if (controller != null) {
            try {
                if (isPlaying) {
                    controller.getTransportControls().pause();
                } else {
                    controller.getTransportControls().play();
                }
                return;
            } catch (Exception e) {
                Log.e(TAG, "Error controlling playback: " + e.getMessage());
            }
        }
        
        Toast.makeText(activity, "无法控制播放，请在音乐应用中操作", Toast.LENGTH_SHORT).show();
    }
    
    public void nextTrack() {
        MediaController controller = notificationMonitor.getMediaController();
        if (controller != null) {
            try {
                controller.getTransportControls().skipToNext();
                return;
            } catch (Exception e) {
                Log.e(TAG, "Error skipping to next: " + e.getMessage());
            }
        }
        
        Toast.makeText(activity, "无法控制播放，请在音乐应用中操作", Toast.LENGTH_SHORT).show();
    }
    
    public void previousTrack() {
        MediaController controller = notificationMonitor.getMediaController();
        if (controller != null) {
            try {
                controller.getTransportControls().skipToPrevious();
                return;
            } catch (Exception e) {
                Log.e(TAG, "Error skipping to previous: " + e.getMessage());
            }
        }
        
        Toast.makeText(activity, "无法控制播放，请在音乐应用中操作", Toast.LENGTH_SHORT).show();
    }
    
    public void openMusicApp() {
        if (currentPackageName != null && !currentPackageName.isEmpty() && !currentPackageName.equals("music.broadcast")) {
            try {
                Intent launchIntent = activity.getPackageManager().getLaunchIntentForPackage(currentPackageName);
                if (launchIntent != null) {
                    activity.startActivity(launchIntent);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error opening music app: " + e.getMessage());
                Toast.makeText(activity, "无法打开音乐应用", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    public boolean hasActiveMusic() {
        return currentPackageName != null && !currentPackageName.isEmpty();
    }
    
    public String getCurrentPackageName() {
        return currentPackageName;
    }
    
    public boolean isCurrentlyPlaying() {
        return isPlaying;
    }
    
    public void stopListening() {
        if (refreshRunnable != null) {
            handler.removeCallbacks(refreshRunnable);
        }
        if (notificationMonitor != null) {
            notificationMonitor.stopMonitoring();
        }
    }
}
