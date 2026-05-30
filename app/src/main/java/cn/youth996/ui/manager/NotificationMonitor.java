package cn.youth996.ui.manager;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.session.MediaController;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.concurrent.ExecutorService;

import cn.youth996.ui.service.MediaNotificationListenerService;

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
     * 由于比亚迪系统限制，我们通过服务实例来判断权限是否已授予
     * @return true如果已授权，false如果未授权
     */
    public boolean checkNotificationListenerPermission() {
        try {
            ComponentName componentName = new ComponentName(activity, MediaNotificationListenerService.class);
            String serviceString = componentName.flattenToString();
            
            android.util.Log.i(TAG, "═══════════════════════════════════════════════════════════");
            android.util.Log.i(TAG, "【通知权限检查】服务名称: " + serviceString);
            
            if (MediaNotificationListenerService.getInstance() != null) {
                android.util.Log.i(TAG, "【通知权限检查】✓ MediaNotificationListenerService 正在运行，权限已授予");
                android.util.Log.i(TAG, "═══════════════════════════════════════════════════════════");
                return true;
            }
            
            android.util.Log.w(TAG, "【通知权限检查】✗ MediaNotificationListenerService 未运行");
            android.util.Log.w(TAG, "【通知权限检查】提示: 请在设置 > 应用 > 轻语桌面中手动开启通知访问权限");
            android.util.Log.i(TAG, "═══════════════════════════════════════════════════════════");
            return false;
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "【通知权限检查】检查失败: " + e.getMessage());
            return false;
        }
    }

    
    /**
     * 检查并请求NotificationListenerService权限
     * @return true如果已有权限，false如果需要请求权限
     */
    public boolean checkAndRequestNotificationPermission() {
        boolean hasPermission = checkNotificationListenerPermission();
        
        Log.d(TAG, "═══════════════════════════════════════════════════════════");
        Log.d(TAG, "【通知权限检查】权限状态: " + (hasPermission ? "✓ 已授权" : "✗ 未授权"));
        
        if (!hasPermission) {
            Log.d(TAG, "【通知权限检查】需要用户授权才能监听音乐播放");
        } else {
            Log.d(TAG, "【通知权限检查】✓ 通知监听器权限已授予");
        }
        
        Log.d(TAG, "═══════════════════════════════════════════════════════════");
        return hasPermission;
    }
    
    /**
     * 重新启动监听（ADB设置权限后调用）
     * 重新检查权限并启动监听
     */
    public void restartMonitoring() {
        Log.i(TAG, "═══════════════════════════════════════════════════════════");
        Log.i(TAG, "【重启监听】正在重新检查权限并启动监听...");
        
        stopMonitoring();
        
        android.os.Handler mainHandler = new android.os.Handler(activity.getMainLooper());
        mainHandler.postDelayed(() -> {
            boolean hasPermission = checkNotificationListenerPermission();
            
            if (hasPermission) {
                Log.i(TAG, "【重启监听】✓ 权限已授予，正在启动监听...");
                startMonitoring();
            } else {
                Log.w(TAG, "【重启监听】✗ 权限仍未授予，请通过 ADB 授权");
            }
            
            Log.i(TAG, "═══════════════════════════════════════════════════════════");
        }, 500);
    }
    
    /**
     * 检查权限并立即启动监听（不弹出设置）
     * 用于 ADB 设置权限后的检查
     */
    public boolean checkPermissionAndStart() {
        boolean hasPermission = checkNotificationListenerPermission();
        
        Log.d(TAG, "【权限检查】当前权限状态: " + (hasPermission ? "✓ 已授权" : "✗ 未授权"));
        
        if (hasPermission) {
            if (!isListeningForNotifications) {
                Log.d(TAG, "【权限检查】权限已授予，正在启动监听...");
                registerBroadcastReceiver();
                
                android.os.Handler mainHandler = new android.os.Handler(activity.getMainLooper());
                mainHandler.postDelayed(() -> {
                    if (MediaNotificationListenerService.getInstance() != null) {
                        Log.d(TAG, "【权限检查】✓ MediaNotificationListenerService 已连接");
                        if (listener != null) {
                            listener.onNotificationServiceConnected();
                        }
                    } else {
                        Log.d(TAG, "【权限检查】MediaNotificationListenerService 尚未就绪，等待中...");
                    }
                }, 3000);
            }
            return true;
        } else {
            Log.w(TAG, "【权限检查】✗ 权限未授予，无法监听");
            return false;
        }
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
            }
        }
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
