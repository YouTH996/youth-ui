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
    private TextView textLyric1;
    private TextView textLyric2;
    private TextView textLyricCurrent;
    private TextView textLyric4;
    private TextView textLyric5;
    private ImageView btnPlayPause;
    
    private String currentPackageName = "";
    private String currentTitle = "";
    private String currentArtist = "";
    private Bitmap currentAlbumArt = null;
    private boolean isPlaying = false;
    private int currentLyricIndex = 0;
    private String[] currentLyrics = {};
    private long currentPosition = 0;
    private Runnable lyricsUpdateRunnable;
    
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
                         TextView titleText, TextView artistText,
                         ImageView playPauseBtn) {
        this.imgAlbumCover = albumCover;
        this.imgMusicSource = musicSource;
        this.textMusicSource = sourceText;
        this.textSongTitle = titleText;
        this.textSongArtist = artistText;
        this.btnPlayPause = playPauseBtn;
    }
    
    public void bindLyricsViews(TextView lyric1, TextView lyric2, TextView lyricCurrent, TextView lyric4, TextView lyric5) {
        this.textLyric1 = lyric1;
        this.textLyric2 = lyric2;
        this.textLyricCurrent = lyricCurrent;
        this.textLyric4 = lyric4;
        this.textLyric5 = lyric5;
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
        
        updateLyricsDisplay();
        
        if (btnPlayPause != null) {
            btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        }
        
        if (isPlaying) {
            startLyricsUpdateTimer();
        } else {
            stopLyricsUpdateTimer();
        }
        
        if (listener != null) {
            listener.onMusicUpdate(currentPackageName, currentTitle, currentArtist, 
                                   currentAlbumArt, isPlaying);
            listener.onPlaybackStateChanged(isPlaying);
        }
    }
    
    private void stopLyricsUpdateTimer() {
        if (lyricsUpdateRunnable != null) {
            handler.removeCallbacks(lyricsUpdateRunnable);
            lyricsUpdateRunnable = null;
        }
    }
    
    private void updateLyricsDisplay() {
        if (currentLyrics.length == 0) return;
        
        if (textLyric1 != null) {
            if (currentLyricIndex >= 2) {
                textLyric1.setText(removeTimeTag(currentLyrics[currentLyricIndex - 2]));
            } else {
                textLyric1.setText("");
            }
        }
        
        if (textLyric2 != null) {
            if (currentLyricIndex >= 1) {
                textLyric2.setText(removeTimeTag(currentLyrics[currentLyricIndex - 1]));
            } else {
                textLyric2.setText("");
            }
        }
        
        if (textLyricCurrent != null) {
            if (currentLyricIndex < currentLyrics.length) {
                textLyricCurrent.setText(removeTimeTag(currentLyrics[currentLyricIndex]));
            } else {
                textLyricCurrent.setText("♪ " + currentArtist + " - " + currentTitle);
            }
        }
        
        if (textLyric4 != null) {
            if (currentLyricIndex < currentLyrics.length - 1) {
                textLyric4.setText(removeTimeTag(currentLyrics[currentLyricIndex + 1]));
            } else {
                textLyric4.setText("");
            }
        }
        
        if (textLyric5 != null) {
            if (currentLyricIndex < currentLyrics.length - 2) {
                textLyric5.setText(removeTimeTag(currentLyrics[currentLyricIndex + 2]));
            } else {
                textLyric5.setText("");
            }
        }
    }
    
    private String removeTimeTag(String lyric) {
        if (lyric == null) return "";
        return lyric.replaceAll("\\[\\d{2}:\\d{2}\\.\\d{2,}\\]", "").trim();
    }
    
    public void updateLyrics(String[] lyrics) {
        if (lyrics != null && lyrics.length > 0) {
            this.currentLyrics = lyrics;
            this.currentLyricIndex = 0;
            updateLyricsDisplay();
            startLyricsUpdateTimer();
        }
    }
    
    public void updateLyrics(String prevLyric, String currentLyric, String nextLyric) {
        if (textLyric1 != null) textLyric1.setText(prevLyric);
        if (textLyric2 != null) textLyric2.setText(currentLyric);
        if (textLyricCurrent != null) textLyricCurrent.setText(currentLyric);
        if (textLyric4 != null) textLyric4.setText(nextLyric);
        if (textLyric5 != null) textLyric5.setText(nextLyric);
    }
    
    private void startLyricsUpdateTimer() {
        if (lyricsUpdateRunnable != null) {
            handler.removeCallbacks(lyricsUpdateRunnable);
        }
        
        lyricsUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateCurrentLyricPosition();
                handler.postDelayed(this, 500);
            }
        };
        handler.post(lyricsUpdateRunnable);
    }
    
    private void updateCurrentLyricPosition() {
        if (notificationMonitor == null || !isPlaying || currentLyrics.length == 0) {
            return;
        }
        
        try {
            MediaController controller = notificationMonitor.getMediaController();
            if (controller != null) {
                android.media.session.PlaybackState state = controller.getPlaybackState();
                if (state != null) {
                    long position = state.getPosition();
                    if (position != currentPosition) {
                        currentPosition = position;
                        updateLyricIndexByPosition(position);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting position: " + e.getMessage());
        }
    }
    
    private void updateLyricIndexByPosition(long positionMs) {
        if (currentLyrics.length == 0) return;
        
        int newIndex = 0;
        for (int i = 0; i < currentLyrics.length; i++) {
            if (getLyricTimeMs(currentLyrics[i]) <= positionMs) {
                newIndex = i;
            } else {
                break;
            }
        }
        
        if (newIndex != currentLyricIndex) {
            currentLyricIndex = newIndex;
            updateLyricsDisplay();
            Log.d(TAG, "Lyric updated to index: " + currentLyricIndex + " - " + removeTimeTag(currentLyrics[currentLyricIndex]));
        }
    }
    
    private long getLyricTimeMs(String lyric) {
        try {
            if (lyric == null || lyric.length() < 10) return 0;
            
            int bracketStart = lyric.indexOf('[');
            int bracketEnd = lyric.indexOf(']');
            
            if (bracketStart >= 0 && bracketEnd > bracketStart) {
                String timeStr = lyric.substring(bracketStart + 1, bracketEnd);
                String[] parts = timeStr.split(":");
                if (parts.length == 2) {
                    int minutes = Integer.parseInt(parts[0]);
                    String[] secParts = parts[1].split("\\.");
                    int seconds = Integer.parseInt(secParts[0]);
                    int millis = 0;
                    if (secParts.length > 1) {
                        millis = Integer.parseInt(secParts[1]);
                        if (secParts[1].length() == 2) {
                            millis *= 10;
                        }
                    }
                    return (minutes * 60 + seconds) * 1000L + millis;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing lyric time: " + e.getMessage());
        }
        return 0;
    }
    
    public void highlightLyric(int index) {
        if (index >= 0 && index < currentLyrics.length) {
            this.currentLyricIndex = index;
            updateLyricsDisplay();
        }
    }
    
    public void updateCurrentLyrics(String lyrics) {
        if (lyrics != null && !lyrics.isEmpty()) {
            this.currentLyrics = new String[]{lyrics};
            this.currentLyricIndex = 0;
            updateLyricsDisplay();
            Log.d(TAG, "Updated lyrics: " + lyrics);
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
