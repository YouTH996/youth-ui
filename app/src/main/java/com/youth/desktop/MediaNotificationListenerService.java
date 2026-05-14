package com.youth.desktop;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import java.util.List;
import java.util.regex.Pattern;

import kotlin.Suppress;

public class MediaNotificationListenerService extends NotificationListenerService {
    private static final String TAG = "MediaNotificationService";
    private static MediaNotificationListenerService instance = null;
    private static final int POLL_INTERVAL = 500;
    
    private MediaSessionManager mediaSessionManager;
    private MediaController mediaController;
    private MediaController.Callback mediaCallback;
    private Handler handler;
    private Runnable pollRunnable;
    
    private String currentPackage = "";
    private String currentTitle = "";
    private String currentArtist = "";
    private Bitmap currentAlbumArt = null;
    private boolean isPlaying = false;
    
    public interface OnMediaInfoListener {
        void onMediaInfoChanged(String packageName, String title, String artist, Bitmap albumArt, boolean isPlaying);
    }
    
    private static OnMediaInfoListener listener;
    
    public static void setListener(OnMediaInfoListener l) {
        listener = l;
    }
    
    public static void clearListener() {
        listener = null;
    }
    
    public static MediaNotificationListenerService getInstance() {
        return instance;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "MediaNotificationListenerService created");
        
        try {
            mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (mediaSessionManager != null) {
                Log.d(TAG, "MediaSessionManager initialized");
                setupMediaCallback();
                startPolling();
            } else {
                Log.e(TAG, "MediaSessionManager is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing: " + e.getMessage());
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        stopPolling();
        Log.d(TAG, "MediaNotificationListenerService destroyed");
    }
    
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        
        if (isMusicApp(packageName)) {
            refreshMediaSessions();
        }
    }
    
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        
        if (isMusicApp(packageName) && packageName.equals(currentPackage)) {
            currentPackage = "";
            currentTitle = "";
            currentArtist = "";
            currentAlbumArt = null;
            isPlaying = false;
            
            if (listener != null) {
                listener.onMediaInfoChanged("", "", "", null, false);
            }
        }
    }
    
    private void setupMediaCallback() {
        mediaCallback = new MediaController.Callback() {
            @Override
            public void onMetadataChanged(MediaMetadata metadata) {
                updateFromMetadata(metadata);
            }
            
            @Override
            public void onPlaybackStateChanged(PlaybackState state) {
                if (state != null) {
                    isPlaying = (state.getState() == PlaybackState.STATE_PLAYING);
                    notifyUpdate();
                }
            }
            
            @Override
            public void onSessionDestroyed() {
                if (mediaController != null) {
                    mediaController.unregisterCallback(mediaCallback);
                    mediaController = null;
                }
                currentPackage = "";
                notifyUpdate();
            }
        };
    }
    
    private void startPolling() {
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                refreshMediaSessions();
                handler.postDelayed(pollRunnable, POLL_INTERVAL);
            }
        };
        handler.post(pollRunnable);
    }
    
    private void stopPolling() {
        if (handler != null && pollRunnable != null) {
            handler.removeCallbacks(pollRunnable);
        }
    }
    
    private void refreshMediaSessions() {
        if (mediaSessionManager == null) {
            Log.w(TAG, "MediaSessionManager is null, skipping refresh");
            updateFromNotification();
            return;
        }
        
        try {
            ComponentName componentName = new ComponentName(this, MediaNotificationListenerService.class);
            Log.d(TAG, "Requesting active sessions...");
            List<MediaController> controllers = mediaSessionManager.getActiveSessions(componentName);
            
            Log.d(TAG, "Found " + controllers.size() + " active sessions");
            
            MediaController newController = null;
            for (MediaController controller : controllers) {
                String pkg = controller.getPackageName();
                Log.d(TAG, "Checking session: " + pkg);
                if (isMusicApp(pkg)) {
                    newController = controller;
                    Log.d(TAG, "Found music app: " + pkg);
                    break;
                }
            }
            
            if (newController != null && newController != mediaController) {
                if (mediaController != null) {
                    mediaController.unregisterCallback(mediaCallback);
                }
                
                mediaController = newController;
                currentPackage = mediaController.getPackageName();
                
                mediaController.registerCallback(mediaCallback);
                
                Log.d(TAG, "Attaching to music app: " + currentPackage);
                updateFromMediaController();
            } else if (newController != null && mediaController != null) {
                updateFromMediaController();
            } else if (newController == null) {
                Log.d(TAG, "No music app found, trying notifications");
                updateFromNotification();
            }
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception - permission not granted: " + e.getMessage());
            updateFromNotification();
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing media sessions: " + e.getMessage());
            updateFromNotification();
        }
    }
    
    private void updateFromMediaController() {
        if (mediaController == null) return;
        
        try {
            MediaMetadata metadata = mediaController.getMetadata();
            if (metadata != null) {
                updateFromMetadata(metadata);
            }
            
            PlaybackState state = mediaController.getPlaybackState();
            if (state != null) {
                isPlaying = (state.getState() == PlaybackState.STATE_PLAYING);
                notifyUpdate();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating from media controller: " + e.getMessage());
        }
    }
    
    private void updateFromMetadata(MediaMetadata metadata) {
        if (metadata == null) return;
        
        String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        
        if (title == null) title = "未知歌曲";
        if (artist == null) artist = "未知艺术家";
        
        Bitmap albumArt = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        }
        
        if (albumArt == null) {
            albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
        }
        
        if (title != null && !title.equals(currentTitle)) {
            currentTitle = title;
        }
        
        if (artist != null && !artist.equals(currentArtist)) {
            currentArtist = artist;
        }
        
        if (albumArt != null) {
            currentAlbumArt = albumArt;
        }
        
        notifyUpdate();
    }
    
    private void updateFromNotification() {
        try {
            StatusBarNotification[] notifications = getActiveNotifications();
            
            for (StatusBarNotification sbn : notifications) {
                if (isMusicApp(sbn.getPackageName())) {
                    Notification notification = sbn.getNotification();
                    if (notification == null) continue;
                    
                    Bundle extras = notification.extras;
                    if (extras == null) continue;
                    
                    String title = extras.getString(Notification.EXTRA_TITLE);
                    String text = extras.getString(Notification.EXTRA_TEXT);
                    String subText = extras.getString(Notification.EXTRA_SUB_TEXT);
                    
                    if (title == null || title.isEmpty()) {
                        title = extras.getString(Notification.EXTRA_TITLE_BIG);
                    }
                    
                    if (title == null || title.isEmpty()) {
                        continue;
                    }
                    
                    String artist = extractArtist(text, subText);
                    
                    Bitmap largeIcon = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        largeIcon = extras.getParcelable(Notification.EXTRA_LARGE_ICON, Bitmap.class);
                    } else {
                        @Suppress(names = "DEPRECATION")
                        Object icon = extras.getParcelable(Notification.EXTRA_LARGE_ICON);
                        if (icon instanceof Bitmap) {
                            largeIcon = (Bitmap) icon;
                        }
                    }
                    
                    boolean playing = isPlaybackActive(text, notification);
                    
                    if (!sbn.getPackageName().equals(currentPackage) || 
                        !title.equals(currentTitle) || 
                        !artist.equals(currentArtist)) {
                        
                        currentPackage = sbn.getPackageName();
                        currentTitle = title;
                        currentArtist = artist;
                        currentAlbumArt = largeIcon;
                        isPlaying = playing;
                        
                        notifyUpdate();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating from notification: " + e.getMessage());
        }
    }
    
    private String extractArtist(String text, String subText) {
        if (subText != null && !subText.isEmpty()) {
            return subText;
        }
        
        if (text == null || text.isEmpty()) {
            return "未知艺术家";
        }
        
        if (text.contains("-")) {
            String[] parts = text.split("-", 2);
            if (parts.length > 1) {
                return parts[0].trim();
            }
        } else if (text.contains("·")) {
            String[] parts = text.split("·", 2);
            if (parts.length > 1) {
                return parts[0].trim();
            }
        }
        
        return text;
    }
    
    private boolean isPlaybackActive(String text, Notification notification) {
        if (text == null) return false;
        
        String lower = text.toLowerCase();
        
        if (lower.contains("paused") || lower.contains("已暂停") || 
            lower.contains("⏸") || lower.contains("❚❚")) {
            return false;
        }
        
        if (lower.contains("playing") || lower.contains("正在播放") || 
            lower.contains("▶") || lower.contains("♪") || 
            lower.contains("🎵") || lower.contains("play")) {
            return true;
        }
        
        return true;
    }
    
    private void notifyUpdate() {
        if (listener != null && !currentPackage.isEmpty()) {
            listener.onMediaInfoChanged(currentPackage, currentTitle, currentArtist, 
                                      currentAlbumArt, isPlaying);
        }
    }
    
    private boolean isMusicApp(String packageName) {
        if (packageName == null) return false;
        
        packageName = packageName.toLowerCase();
        
        String[] musicPackages = {
            "netease", "cloudmusic", "music",
            "qqmusic", "tencent.qqmusic",
            "kugou", "kugou.player",
            "kuwo", "kuwo.player",
            "baidu.music",
            "player", "media", "audio"
        };
        
        for (String musicPackage : musicPackages) {
            if (packageName.contains(musicPackage)) {
                return true;
            }
        }
        
        return false;
    }
    
    public String getCurrentPackage() {
        return currentPackage;
    }
    
    public String getCurrentTitle() {
        return currentTitle;
    }
    
    public String getCurrentArtist() {
        return currentArtist;
    }
    
    public Bitmap getCurrentAlbumArt() {
        return currentAlbumArt;
    }
    
    public boolean isPlaying() {
        return isPlaying;
    }
    
    public MediaController getMediaController() {
        return mediaController;
    }
}
