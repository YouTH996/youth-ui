package cn.youth996.ui.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
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
    private List<LyricLine> currentLyrics = new ArrayList<>();
    private long currentPosition = 0;
    
    public interface OnMediaInfoListener {
        void onMediaInfoChanged(String packageName, String title, String artist, Bitmap albumArt, boolean isPlaying);
    }
    
    public interface OnLyricsUpdateListener {
        void onLyricsUpdate(String lyric1, String lyric2, String currentLyric, String lyric4, String lyric5, int index);
    }
    
    private static OnMediaInfoListener listener;
    private static OnLyricsUpdateListener lyricsListener;
    private int currentLyricIndex = 0;
    
    public static class LyricLine {
        public long time;
        public String text;
        
        public LyricLine(long time, String text) {
            this.time = time;
            this.text = text;
        }
    }
    
    public static void setListener(OnMediaInfoListener l) {
        listener = l;
    }
    
    public static void clearListener() {
        listener = null;
    }
    
    public static void setLyricsListener(OnLyricsUpdateListener l) {
        lyricsListener = l;
    }
    
    public static void clearLyricsListener() {
        lyricsListener = null;
    }
    
    public static MediaNotificationListenerService getInstance() {
        return instance;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.i(TAG, "═══════════════════════════════════════════════════════════");
        Log.i(TAG, "【MediaNotificationListenerService】服务已创建");
        
        try {
            mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (mediaSessionManager != null) {
                Log.i(TAG, "【MediaNotificationListenerService】MediaSessionManager 初始化成功");
                setupMediaCallback();
                startPolling();
                Log.i(TAG, "【MediaNotificationListenerService】开始轮询音乐应用");
            } else {
                Log.e(TAG, "【MediaNotificationListenerService】MediaSessionManager 为空");
            }
        } catch (Exception e) {
            Log.e(TAG, "【MediaNotificationListenerService】初始化错误: " + e.getMessage());
        }
        
        Log.i(TAG, "═══════════════════════════════════════════════════════════");
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
            Log.w(TAG, "【媒体会话】MediaSessionManager 为空，使用通知方式");
            updateFromNotification();
            return;
        }
        
        try {
            ComponentName componentName = new ComponentName(this, MediaNotificationListenerService.class);
            List<MediaController> controllers = mediaSessionManager.getActiveSessions(componentName);
            
            Log.d(TAG, "【媒体会话】检测到 " + controllers.size() + " 个活跃的媒体会话");
            
            MediaController newController = null;
            for (MediaController controller : controllers) {
                String pkg = controller.getPackageName();
                Log.d(TAG, "【媒体会话】检测到应用: " + pkg + " (音乐应用: " + isMusicApp(pkg) + ")");
                if (isMusicApp(pkg)) {
                    newController = controller;
                    break;
                }
            }
            
            if (newController != null && newController != mediaController) {
                if (mediaController != null) {
                    mediaController.unregisterCallback(mediaCallback);
                }
                
                mediaController = newController;
                currentPackage = mediaController.getPackageName();
                Log.i(TAG, "【媒体会话】✓ 连接到音乐应用: " + currentPackage);
                
                mediaController.registerCallback(mediaCallback);
                
                updateFromMediaController();
            } else if (newController != null && mediaController != null) {
                updateFromMediaController();
            } else if (newController == null) {
                Log.d(TAG, "【媒体会话】未检测到音乐应用，尝试从通知获取");
                updateFromNotification();
            }
            
        } catch (SecurityException e) {
            Log.e(TAG, "【媒体会话】安全异常 - 权限未授予: " + e.getMessage());
            updateFromNotification();
        } catch (Exception e) {
            Log.e(TAG, "【媒体会话】刷新媒体会话错误: " + e.getMessage());
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
            Log.e(TAG, "Error updating from media controller: " + e.getMessage(), e);
        }
    }
    
    private void updateFromMetadata(MediaMetadata metadata) {
        if (metadata == null) return;
        
        String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        try {
            Bundle bundle = getMediaMetadataBundle(metadata);
            if (bundle != null) {
                String lyrics = null;
                
                if (bundle.containsKey("android.media.metadata.LYRIC")) {
                    lyrics = bundle.getString("android.media.metadata.LYRIC");
                } else if (bundle.containsKey("android.media.metadata.LYRICS")) {
                    lyrics = bundle.getString("android.media.metadata.LYRICS");
                }
                
                if (lyrics != null && !lyrics.isEmpty()) {
                    parseAndNotifyLyrics(lyrics);
                } else {
                    Log.d(TAG, "No LYRIC found in bundle");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting LYRICS: " + e.getMessage(), e);
        }

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
    
    private void parseAndNotifyLyrics(String lyrics) {
        if (lyrics == null || lyrics.isEmpty()) {
            return;
        }
        
        currentLyrics.clear();
        Pattern pattern = Pattern.compile("\\[(\\d+):(\\d+)\\.(\\d+)\\](.*)");
        String[] lines = lyrics.split("\n");
        
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                long min = Long.parseLong(matcher.group(1));
                long sec = Long.parseLong(matcher.group(2));
                long ms = Long.parseLong(matcher.group(3));
                String text = matcher.group(4).trim();
                
                long time = min * 60 * 1000 + sec * 1000 + ms * 10;
                
                if (!text.isEmpty()) {
                    currentLyrics.add(new LyricLine(time, text));
                }
            }
        }
        
        if (!currentLyrics.isEmpty()) {
            startLyricsSync();
        }
    }
    
    private void startLyricsSync() {
        handler.removeCallbacks(lyricsSyncRunnable);
        handler.post(lyricsSyncRunnable);
    }
    
    private Runnable lyricsSyncRunnable = new Runnable() {
        @Override
        public void run() {
            updateLyricsPosition();
            handler.postDelayed(this, 300);
        }
    };
    
    private void updateLyricsPosition() {
        if (mediaController == null || currentLyrics.isEmpty()) {
            return;
        }
        
        try {
            PlaybackState state = mediaController.getPlaybackState();
            if (state != null) {
                long position = state.getPosition();
                if (position != currentPosition) {
                    currentPosition = position;
                    
                    int index = findCurrentLyricIndex(position);
                    if (index >= 0 && lyricsListener != null) {
                        String lyric1 = index >= 2 ? currentLyrics.get(index - 2).text : "";
                        String lyric2 = index >= 1 ? currentLyrics.get(index - 1).text : "";
                        String currentLyric = currentLyrics.get(index).text;
                        String lyric4 = index < currentLyrics.size() - 1 ? currentLyrics.get(index + 1).text : "";
                        String lyric5 = index < currentLyrics.size() - 2 ? currentLyrics.get(index + 2).text : "";
                        
                        lyricsListener.onLyricsUpdate(lyric1, lyric2, currentLyric, lyric4, lyric5, index);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting position: " + e.getMessage());
        }
    }
    
    private int findCurrentLyricIndex(long position) {
        if (currentLyrics.isEmpty()) {
            return -1;
        }
        
        for (int i = currentLyrics.size() - 1; i >= 0; i--) {
            if (position >= currentLyrics.get(i).time) {
                return i;
            }
        }
        
        return 0;
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
    
    private Bundle getMediaMetadataBundle(MediaMetadata metadata) {
        try {
            @SuppressLint("SoonBlockedPrivateApi")
            Field field = MediaMetadata.class.getDeclaredField("mBundle");
            field.setAccessible(true);
            return (Bundle) field.get(metadata);
        } catch (Exception e) {
            Log.e(TAG, "get bundle error", e);
        }
        return null;
    }
}
