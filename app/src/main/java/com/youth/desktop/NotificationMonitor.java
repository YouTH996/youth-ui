package com.youth.desktop;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationMonitor {
    private static final String TAG = "NotificationMonitor";
    
    private Activity activity;
    private Handler handler;
    private ExecutorService executor;
    
    private String currentPackage = "";
    private String currentTitle = "";
    private String currentArtist = "";
    private Bitmap currentAlbumArt = null;
    private boolean isPlaying = false;
    private boolean isListeningForNotifications = false;
    
    private OnMediaInfoListener listener;
    
    public interface OnMediaInfoListener {
        void onMediaInfoChanged(String packageName, String title, String artist, Bitmap albumArt, boolean isPlaying);
        void onNotificationServiceConnected();
    }
    
    public NotificationMonitor(Activity activity) {
        this.activity = activity;
        this.handler = new Handler(Looper.getMainLooper());
    }
    
    public void setOnMediaInfoListener(OnMediaInfoListener l) {
        this.listener = l;
    }
    
    /**
     * 检查NotificationListenerService权限是否已授权
     * @return true如果已授权，false如果未授权
     */
    public boolean checkNotificationListenerPermission() {
        try {
            ComponentName componentName = new ComponentName(activity, MediaNotificationListenerService.class);
            String enabledListeners = Settings.Secure.getString(
                activity.getContentResolver(),
                "enabled_notification_listeners"
            );
            
            if (enabledListeners != null) {
                boolean isEnabled = enabledListeners.contains(componentName.flattenToString());
                Log.d(TAG, "NotificationListenerService permission check: " + isEnabled);
                return isEnabled;
            }
            
            Log.d(TAG, "NotificationListenerService permission check: false (enabledListeners is null)");
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking notification listener permission: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 打开系统设置页面让用户授权NotificationListenerService权限
     */
    public void requestNotificationListenerPermission() {
        try {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            Toast.makeText(activity, "请在设置中开启'轻语桌面'的通知监听权限", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error opening notification listener settings: " + e.getMessage());
            Toast.makeText(activity, "无法打开权限设置页面，请在系统设置中手动授权", Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 检查并请求NotificationListenerService权限
     * @return true如果已有权限，false如果需要请求权限
     */
    public boolean checkAndRequestNotificationPermission() {
        if (!checkNotificationListenerPermission()) {
            requestNotificationListenerPermission();
            return false;
        }
        return true;
    }
    
    public void startMonitoring() {
        try {
            Log.d(TAG, "=== START MONITORING ===");
            
            if (!checkAndRequestNotificationPermission()) {
                Log.d(TAG, "NotificationListenerService permission not granted, requesting permission...");
                return;
            }
            
            Log.d(TAG, "NotificationListenerService permission granted, starting monitoring...");
            registerBroadcastReceiver();
            
            Toast.makeText(activity, "正在监听音乐播放...", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Broadcast receiver registered");
            
            handler.postDelayed(() -> {
                if (MediaNotificationListenerService.getInstance() != null) {
                    Log.d(TAG, "NotificationListenerService is now available!");
                    if (listener != null) {
                        listener.onNotificationServiceConnected();
                    }
                } else {
                    Log.d(TAG, "NotificationListenerService not yet available, waiting...");
                }
            }, 3000);
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting monitor: " + e.getMessage(), e);
            Toast.makeText(activity, "监听启动失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void registerBroadcastReceiver() {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) return;
                
                String action = intent.getAction();
                Log.d(TAG, "Received broadcast: " + action);
                
                if (action != null && (action.contains("music") || 
                    action.contains("metachanged") || 
                    action.contains("playstate"))) {
                    handleMusicIntent(intent);
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.android.music.metachanged");
        filter.addAction("com.android.music.playstatechanged");
        filter.addAction("com.android.music.musicservicecommand");
        filter.addAction("com.htc.music.metachanged");
        filter.addAction("com.htc.music.playstatechanged");
        filter.addAction("fm.last.android.metachanged");
        filter.addAction("com.spotify.music.playbackstatechanged");
        filter.addAction("com.spotify.music.metadatachanged");
        
        try {
            ContextCompat.registerReceiver(activity, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
            Log.d(TAG, "BroadcastReceiver registered successfully");
            isListeningForNotifications = true;
        } catch (Exception e) {
            Log.e(TAG, "Error registering receiver: " + e.getMessage());
        }
    }
    
    private void handleMusicIntent(Intent intent) {
        try {
            String artist = null;
            String title = null;
            
            if (intent.hasExtra("artist")) {
                artist = intent.getStringExtra("artist");
            }
            if (intent.hasExtra("track")) {
                title = intent.getStringExtra("track");
            }
            if ((title == null || title.isEmpty()) && intent.hasExtra("title")) {
                title = intent.getStringExtra("title");
            }
            if ((artist == null || artist.isEmpty()) && intent.hasExtra("artist")) {
                artist = intent.getStringExtra("artist");
            }
            
            if (title == null || title.isEmpty()) {
                Log.d(TAG, "No music title in intent");
                return;
            }
            
            if (artist == null) artist = "未知艺术家";
            
            currentPackage = "music.broadcast";
            currentTitle = title;
            currentArtist = artist;
            
            Log.d(TAG, "Music from broadcast: " + title + " - " + artist);
            
            updateUI();
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling music intent: " + e.getMessage());
        }
    }
    
    private void updateUI() {
        if (listener != null && !currentTitle.isEmpty()) {
            handler.post(() -> {
                listener.onMediaInfoChanged(currentPackage, currentTitle, currentArtist, 
                                      currentAlbumArt, isPlaying);
            });
        }
    }
    
    public void refreshFromNotificationService() {
        MediaNotificationListenerService service = MediaNotificationListenerService.getInstance();
        if (service != null) {
            String pkg = service.getCurrentPackage();
            String title = service.getCurrentTitle();
            String artist = service.getCurrentArtist();
            Bitmap art = service.getCurrentAlbumArt();
            boolean playing = service.isPlaying();
            
            if (pkg != null && !pkg.isEmpty()) {
                currentPackage = pkg;
                currentTitle = title != null ? title : "";
                currentArtist = artist != null ? artist : "";
                currentAlbumArt = art;
                isPlaying = playing;
                
                updateUI();
                Log.d(TAG, "Refreshed from NotificationService: " + title + " - " + artist);
            }
        }
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
        MediaNotificationListenerService service = MediaNotificationListenerService.getInstance();
        if (service != null) {
            return service.getMediaController();
        }
        return null;
    }
    
    public boolean isListeningForNotifications() {
        return isListeningForNotifications;
    }
    
    public void stopMonitoring() {
        try {
            if (executor != null) {
                executor.shutdownNow();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping monitor: " + e.getMessage());
        }
    }
}
