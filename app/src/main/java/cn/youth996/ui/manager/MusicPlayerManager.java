package cn.youth996.ui.manager;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.session.MediaController;
import android.os.Handler;
import android.os.Looper;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import cn.youth996.ui.R;

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
    private ViewGroup lyricsContainer;
    private List<String> lyricTexts = new ArrayList<>();
    private int lastRenderedIndex = -1;
    private int lastRenderedMaxLines = -1;
    private List<String> lastRenderedLyrics = null;
    private ImageView btnPlayPause;

    // 状态栏灵动岛歌词
    private View islandContainer;
    private ImageView islandWave;
    private TextView islandTitle;
    private TextView islandLyric;
    private Bitmap waveIdleFrame;
    private Bitmap[] waveFrames;
    private int waveFrameIndex = 0;
    private Runnable waveAnimRunnable;
    
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
    
    public void bindLyricsViews(ViewGroup container) {
        this.lyricsContainer = container;
        if (container != null) {
            // 布局完成或高度变化后，重新按可用高度计算可显示的歌词行数
            container.getViewTreeObserver().addOnGlobalLayoutListener(this::updateLyricsDisplay);
        }
    }
    
    /**
     * 绑定状态栏灵动岛视图：波形动画图标 + 固定歌名 + 当前歌词
     */
    public void bindIslandViews(View container, ImageView wave, TextView title, TextView lyric) {
        this.islandContainer = container;
        this.islandWave = wave;
        this.islandTitle = title;
        this.islandLyric = lyric;
        if (lyric != null) {
            lyric.setSelected(true); // 触发跑马灯滚动
        }
        loadWaveFrames();
    }

    private void loadWaveFrames() {
        try {
            waveIdleFrame = BitmapFactory.decodeStream(
                    activity.getAssets().open("webui/img/island/Y1.png"));
            waveFrames = new Bitmap[5];
            for (int i = 0; i < waveFrames.length; i++) {
                waveFrames[i] = BitmapFactory.decodeStream(
                        activity.getAssets().open("webui/img/island/Y" + (11 + i) + ".png"));
            }
            if (islandWave != null) {
                islandWave.setImageBitmap(waveIdleFrame);
            }
        } catch (Exception e) {
            Log.e(TAG, "加载灵动岛波形帧失败: " + e.getMessage());
        }
    }

    private void updateIslandLyric(String text) {
        setTextIfChanged(islandLyric, text == null ? "" : text);
        updateIslandVisibility();
    }

    /**
     * 仅在文本变化时才 setText，避免跑马灯/单行 TextView 反复触发
     * StaticLayout 的 "maxLineHeight should not be -1" 警告
     */
    private void setTextIfChanged(TextView tv, String text) {
        if (tv == null) return;
        CharSequence old = tv.getText();
        if (old == null || !old.toString().equals(text)) {
            tv.setText(text);
        }
    }

    private void updateIslandVisibility() {
        if (islandContainer == null) return;
        boolean hasTitle = currentTitle != null && !currentTitle.isEmpty();
        boolean hasLyric = islandLyric != null && islandLyric.getText().length() > 0;
        islandContainer.setVisibility(hasTitle || hasLyric ? View.VISIBLE : View.GONE);
    }

    private void startWaveAnimation() {
        if (islandWave == null || waveFrames == null || waveAnimRunnable != null) return;
        waveAnimRunnable = new Runnable() {
            @Override
            public void run() {
                islandWave.setImageBitmap(waveFrames[waveFrameIndex]);
                waveFrameIndex = (waveFrameIndex + 1) % waveFrames.length;
                handler.postDelayed(this, 150);
            }
        };
        handler.post(waveAnimRunnable);
    }

    private void stopWaveAnimation() {
        if (waveAnimRunnable != null) {
            handler.removeCallbacks(waveAnimRunnable);
            waveAnimRunnable = null;
        }
        waveFrameIndex = 0;
        if (islandWave != null && waveIdleFrame != null) {
            islandWave.setImageBitmap(waveIdleFrame);
        }
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
        setTextIfChanged(textSongTitle, currentTitle);
        setTextIfChanged(islandTitle, currentTitle);
        updateIslandVisibility();

        setTextIfChanged(textSongArtist, currentArtist);
        
        if (imgAlbumCover != null && currentAlbumArt != null) {
            imgAlbumCover.setImageBitmap(currentAlbumArt);
        }
        
        if (textMusicSource != null && !currentPackageName.isEmpty()) {
            String appName = getAppName(currentPackageName);
            setTextIfChanged(textMusicSource, appName);
        }
        
        if (imgMusicSource != null && !currentPackageName.isEmpty()) {
            Drawable appIcon = getAppIcon(currentPackageName);
            if (appIcon != null) {
                imgMusicSource.setImageDrawable(appIcon);
            }
        }
        
        if (btnPlayPause != null) {
            btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        }
        
        if (isPlaying) {
            startLyricsUpdateTimer();
            startWaveAnimation();
        } else {
            stopLyricsUpdateTimer();
            stopWaveAnimation();
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
        if (lyricsContainer == null) return;

        if (lyricTexts.isEmpty()) {
            lyricsContainer.removeAllViews();
            lastRenderedIndex = -1;
            lastRenderedLyrics = null;
            updateIslandLyric(null);
            return;
        }

        if (currentLyricIndex < 0 || currentLyricIndex >= lyricTexts.size()) {
            return;
        }

        updateIslandLyric(lyricTexts.get(currentLyricIndex));

        int maxLines = computeMaxLyricLines();
        // 歌词索引和行数都未变化时跳过重绘，避免播放进度轮询导致的频繁重建
        if (maxLines == lastRenderedMaxLines && currentLyricIndex == lastRenderedIndex
                && lyricTexts == lastRenderedLyrics) {
            return;
        }
        lastRenderedMaxLines = maxLines;
        lastRenderedIndex = currentLyricIndex;
        lastRenderedLyrics = lyricTexts;

        renderLyricLines(maxLines);
    }

    /**
     * 根据歌词容器剩余高度计算可显示的行数（保证奇数，当前行居中）
     */
    private int computeMaxLyricLines() {
        if (lyricsContainer == null) return 5;

        int availableHeight = lyricsContainer.getHeight()
                - lyricsContainer.getPaddingTop() - lyricsContainer.getPaddingBottom();
        if (availableHeight <= 0) return 5;

        float scaledDensity = activity.getResources().getDisplayMetrics().scaledDensity;
        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(11 * scaledDensity);
        Paint.FontMetrics fm = paint.getFontMetrics();
        int lineHeight = (int) Math.ceil(fm.descent - fm.ascent + fm.leading)
                + Math.round(8 * scaledDensity);
        if (lineHeight <= 0) return 5;

        int maxLines = availableHeight / lineHeight;
        if (maxLines < 3) maxLines = 3;
        if (maxLines % 2 == 0) maxLines--;
        return maxLines;
    }

    /**
     * 以当前歌词为中心，按 maxLines 动态创建歌词 TextView
     */
    private void renderLyricLines(int maxLines) {
        lyricsContainer.removeAllViews();

        int accentColor = ContextCompat.getColor(activity, R.color.dark_tech_accent);
        int secondaryColor = ContextCompat.getColor(activity, R.color.dark_tech_text_secondary);
        Typeface mediumTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL);

        int half = maxLines / 2;
        for (int i = currentLyricIndex - half; i <= currentLyricIndex + half; i++) {
            TextView tv = new TextView(activity);
            tv.setText(i >= 0 && i < lyricTexts.size() ? lyricTexts.get(i) : "");
            tv.setGravity(Gravity.CENTER);
            tv.setSingleLine(true);
            tv.setEllipsize(TextUtils.TruncateAt.END);

            int distance = Math.abs(i - currentLyricIndex);
            if (distance == 0) {
                tv.setTextSize(14);
                tv.setTextColor(accentColor);
                tv.setTypeface(mediumTypeface);
                int padding = dpToPx(4);
                tv.setPadding(0, padding, 0, padding);
            } else {
                tv.setTextSize(11);
                tv.setTextColor(secondaryColor);
                tv.setAlpha(Math.max(0.15f, 0.75f - 0.15f * distance));
            }
            lyricsContainer.addView(tv);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * activity.getResources().getDisplayMetrics().density);
    }
    
    private String removeTimeTag(String lyric) {
        if (lyric == null) return "";
        return lyric.replaceAll("\\[\\d{2}:\\d{2}\\.\\d{2,}\\]", "").trim();
    }
    
    public void updateLyrics(String[] lyrics) {
        if (lyrics != null && lyrics.length > 0) {
            this.currentLyrics = lyrics;
            List<String> texts = new ArrayList<>(lyrics.length);
            for (String line : lyrics) {
                texts.add(removeTimeTag(line));
            }
            this.lyricTexts = texts;
            this.currentLyricIndex = 0;
            updateLyricsDisplay();
            startLyricsUpdateTimer();
        }
    }

    /**
     * 由外部歌词服务驱动：传入完整歌词文本（已去除时间标签）和当前行索引
     */
    public void updateLyrics(List<String> lyrics, int index) {
        if (lyrics == null || lyrics.isEmpty()) return;
        this.lyricTexts = lyrics;
        // 置空内置歌词源，避免内置定时器与外部驱动互相覆盖
        this.currentLyrics = new String[0];
        this.currentLyricIndex = index;
        updateLyricsDisplay();
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
        if (index >= 0 && index < lyricTexts.size()) {
            this.currentLyricIndex = index;
            updateLyricsDisplay();
        }
    }

    public void updateCurrentLyrics(String lyrics) {
        if (lyrics != null && !lyrics.isEmpty()) {
            this.currentLyrics = new String[]{lyrics};
            this.lyricTexts = new ArrayList<>();
            this.lyricTexts.add(removeTimeTag(lyrics));
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
        stopWaveAnimation();
        if (refreshRunnable != null) {
            handler.removeCallbacks(refreshRunnable);
        }
        if (notificationMonitor != null) {
            notificationMonitor.stopMonitoring();
        }
    }
    
    /**
     * 获取 NotificationMonitor 实例
     * 用于外部检查权限状态
     */
    public NotificationMonitor getNotificationMonitor() {
        return notificationMonitor;
    }
    
    /**
     * 重新启动音乐监听（ADB 设置权限后调用）
     */
    public void restartListening() {
        stopListening();
        
        handler.postDelayed(() -> {
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
                    android.widget.Toast.makeText(activity, "已重新连接到通知服务", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
            
            notificationMonitor.checkPermissionAndStart();
            startPeriodicRefresh();
        }, 500);
    }
}
