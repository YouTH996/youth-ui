package cn.youth996.ui.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 画中画（真内嵌）地图管理器
 *
 * 与悬浮模式的区别：
 *   - 悬浮：地图 App 自己以 overlay 小窗显示在最上层（广播遥控）
 *   - 画中画：桌面创建 VirtualDisplay，把地图 App 启动到虚拟屏中，
 *     画面经 ImageReader 逐帧拷贝绘制到桌面卡片内的 PipFrameView
 *
 * 关键设计（Android 15 实测约束）：
 *   - MediaProjection 一次授权只允许创建"一个"虚拟屏（重复创建抛异常），
 *     且虚拟屏直接绑窗口 Surface 时，宿主页面退后台 Surface 销毁后画面永久黑屏；
 *     因此虚拟屏输出绑定到与窗口无关的 ImageReader，自己拷贝帧绘制，
 *     桌面退后台/回前台画面都不受影响
 *   - getMediaProjection 之前必须先运行 mediaProjection 类型前台服务（API 34+）
 *   - 地图入口 Activity 内部跳转会把任务带到主屏，用 am display move-stack 拉回
 *   - 触摸由 shell 侧 TouchServer 定向注入虚拟屏（Android 10+）
 *
 * @author Youth Desktop Team
 */
public class PipVirtualDisplayManager {

    private static final String TAG = "PipVirtualDisplay";
    private static final String DISPLAY_NAME = "youth_pip_map";

    /** MediaProjection 屏幕捕获授权请求码 */
    public static final int REQUEST_MEDIA_PROJECTION = 1002;

    /** 触摸服务相关常量 */
    private static final String TOUCH_JAR_ASSET = "pip_touch_server.jar";
    private static final String TOUCH_JAR_REMOTE = "/data/local/tmp/pip_touch_server.jar";
    private static final String TOUCH_LOG_REMOTE = "/data/local/tmp/pip_touch.log";
    private static final int TOUCH_PORT = 18927;

    /** 精确杀触摸服务：排除当前 shell 自身（命令行含 TouchServer 字样，pkill -f 会误杀自己） */
    private static final String KILL_TOUCH_SERVER_CMD =
            "for p in $(pgrep -f 'cn.youth996.pip.TouchServer'); do [ $p -ne $$ ] && kill $p 2>/dev/null; done";

    private static MediaProjection mediaProjection;
    private static VirtualDisplay virtualDisplay;
    private static ImageReader imageReader;
    private static PipFrameView frameView;
    private static FrameLayout hostContainer;
    private static boolean showing = false;

    /** 授权/服务就绪流程中的挂起状态 */
    private static boolean consentInProgress = false;
    private static Activity pendingActivity;
    private static FrameLayout pendingContainer;
    private static int pendingResultCode;
    private static Intent pendingResultData;
    private static String mapPackageName;

    private static HandlerThread frameThread;
    private static Handler frameHandler;

    private static final Object touchLock = new Object();
    private static Socket touchSocket;
    private static OutputStream touchOut;
    private static final ExecutorService touchExecutor = Executors.newSingleThreadExecutor();

    /**
     * 画中画当前是否正在显示
     */
    public static boolean isShowing() {
        return showing;
    }

    /**
     * 是否已获得屏幕捕获授权（获得后才可创建画中画虚拟屏）
     */
    public static boolean hasProjection() {
        return mediaProjection != null;
    }

    /**
     * 当前系统是否支持画中画触摸（Android 10+ 才能定向注入虚拟屏）
     */
    public static boolean isTouchSupported() {
        return Build.VERSION.SDK_INT >= 29;
    }

    /**
     * 在指定容器中以内嵌画中画方式显示地图 App
     *
     * 若尚未获得屏幕捕获授权，会先发起系统授权弹窗，
     * 授权结果通过 {@link #onProjectionResult(Activity, int, Intent)} 回传后继续。
     *
     * @param activity  宿主 Activity（用于启动地图 Activity 到虚拟屏）
     * @param container 承载画面的容器（内部会加入一个 PipFrameView）
     * @return true 表示显示流程已发起（含等待授权）
     */
    public static boolean showPipMap(Activity activity, FrameLayout container) {
        if (showing) {
            Log.d(TAG, "画中画已在显示，忽略重复调用");
            return true;
        }
        String mapPackage = FloatingWindowManager.getMapPackage(activity);
        Intent launchIntent = activity.getPackageManager().getLaunchIntentForPackage(mapPackage);
        if (launchIntent == null) {
            Log.w(TAG, "未找到地图应用: " + mapPackage);
            return false;
        }

        if (mediaProjection == null) {
            if (consentInProgress) {
                Log.d(TAG, "【画中画】授权流程进行中，等待回调");
                return true;
            }
            Log.i(TAG, "【画中画】尚未获得屏幕捕获授权，发起授权请求");
            consentInProgress = true;
            pendingActivity = activity;
            pendingContainer = container;
            MediaProjectionManager mpm =
                    (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            activity.startActivityForResult(mpm.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
            return true;
        }

        hostContainer = container;
        frameView = new PipFrameView(activity);
        container.addView(frameView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (virtualDisplay == null) {
            createDisplayAndLaunch(activity, launchIntent);
        } else {
            // 虚拟屏和地图任务都还在，直接恢复显示即可
            int displayId = virtualDisplay.getDisplay().getDisplayId();
            Log.i(TAG, "【画中画】复用已有虚拟屏: displayId=" + displayId);
            runShell(activity, buildMoveTaskCommand(displayId));
            if (isTouchSupported()) {
                setupTouchForwarding(activity, displayId);
            }
        }

        showing = true;
        Log.i(TAG, "【画中画】已发起内嵌显示: " + mapPackage);
        return true;
    }

    /**
     * 处理屏幕捕获授权结果
     *
     * 授权成功后先启动 mediaProjection 类型前台服务，待服务就绪回调
     * {@link #onProjectionServiceReady(Context)} 再获取 MediaProjection。
     * （Android 14+ 强制要求；实测部分 Android 12 定制 ROM 同样强制，故所有版本统一走此路径）
     *
     * @return true 表示流程已受理（等待前台服务就绪）
     */
    public static boolean onProjectionResult(Activity activity, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            pendingResultCode = resultCode;
            pendingResultData = data;
            Log.i(TAG, "【画中画】授权成功，启动前台服务后获取 MediaProjection");
            androidx.core.content.ContextCompat.startForegroundService(activity,
                    new Intent(activity, cn.youth996.ui.service.PipProjectionService.class));
            // consentInProgress 保持 true，等待 onProjectionServiceReady
            return true;
        }
        Log.w(TAG, "【画中画】屏幕捕获授权被拒绝");
        consentInProgress = false;
        pendingActivity = null;
        pendingContainer = null;
        return false;
    }

    /**
     * 前台服务就绪回调（由 PipProjectionService 调用），此时才可获取 MediaProjection
     */
    public static void onProjectionServiceReady(Context context) {
        obtainProjection(context);
        consentInProgress = false;
        if (mediaProjection != null && pendingActivity != null && pendingContainer != null) {
            showPipMap(pendingActivity, pendingContainer);
            pendingActivity.runOnUiThread(() ->
                    android.widget.Toast.makeText(pendingActivity, "画中画地图已显示",
                            android.widget.Toast.LENGTH_SHORT).show());
        } else if (mediaProjection == null && pendingActivity != null) {
            pendingActivity.runOnUiThread(() ->
                    android.widget.Toast.makeText(pendingActivity, "画中画启动失败，请重试",
                            android.widget.Toast.LENGTH_SHORT).show());
        }
    }

    private static void obtainProjection(Context context) {
        try {
            MediaProjectionManager mpm =
                    (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            mediaProjection = mpm.getMediaProjection(pendingResultCode, pendingResultData);
            // Android 14+ 要求开始捕获前必须注册回调
            mediaProjection.registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    Log.w(TAG, "【画中画】MediaProjection 已停止");
                    mediaProjection = null;
                    releaseDisplay();
                    showing = false;
                }
            }, new Handler(android.os.Looper.getMainLooper()));
            Log.i(TAG, "【画中画】屏幕捕获授权成功");
        } catch (Exception e) {
            Log.e(TAG, "【画中画】获取 MediaProjection 失败: " + e.getMessage());
            mediaProjection = null;
        }
    }

    /**
     * 关闭画中画：移除画面视图、断开触摸通道、停止触摸服务
     *
     * 注意：不释放虚拟屏/前台服务/MediaProjection——Android 14+ 授权会话单次有效，
     * 释放后下次开启需重新弹授权框；地图任务保留在虚拟屏上，再次开启直接恢复
     */
    public static void hidePipMap(Context context) {
        if (!showing) {
            return;
        }
        closeTouchChannel();
        if (isTouchSupported() && context != null) {
            runShell(context, KILL_TOUCH_SERVER_CMD + "; echo PIP_TOUCH_KILLED");
        }
        if (hostContainer != null && frameView != null) {
            hostContainer.removeView(frameView);
        }
        frameView = null;
        hostContainer = null;
        showing = false;
        Log.i(TAG, "【画中画】已关闭");
    }

    /**
     * 创建虚拟屏（输出到 ImageReader）并启动地图
     */
    private static void createDisplayAndLaunch(Activity activity, Intent launchIntent) {
        try {
            int width = hostContainer.getWidth();
            int height = hostContainer.getHeight();
            if (width <= 0 || height <= 0) {
                width = 1030;
                height = 635;
            }
            int densityDpi = activity.getResources().getDisplayMetrics().densityDpi;

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
            startFramePump();

            virtualDisplay = mediaProjection.createVirtualDisplay(
                    DISPLAY_NAME, width, height, densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    imageReader.getSurface(), null, null);
            if (virtualDisplay == null) {
                Log.e(TAG, "【画中画】创建虚拟屏失败");
                return;
            }
            int displayId = virtualDisplay.getDisplay().getDisplayId();
            Log.i(TAG, "【画中画】虚拟屏已创建: displayId=" + displayId + ", " + width + "x" + height);

            launchOnDisplayViaShell(activity, launchIntent, displayId);

            if (isTouchSupported()) {
                setupTouchForwarding(activity, displayId);
            } else {
                // Android 9 及以下无法把触摸定向注入虚拟屏，点击回退为打开完整地图
                Log.w(TAG, "【画中画】系统版本过低(SDK " + Build.VERSION.SDK_INT + ")，触摸不可用");
                frameView.setOnClickListener(v -> activity.startActivity(launchIntent));
            }
        } catch (Exception e) {
            Log.e(TAG, "【画中画】启动失败: " + e.getMessage());
            // 授权实例可能已失效（non-current），置空让下次开启重新授权
            mediaProjection = null;
            releaseDisplay();
        }
    }

    /**
     * 启动帧拷贝线程：ImageReader → Bitmap → PipFrameView
     */
    private static void startFramePump() {
        stopFramePump();
        frameThread = new HandlerThread("pip-frame");
        frameThread.start();
        frameHandler = new Handler(frameThread.getLooper());
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                if (image == null) {
                    return;
                }
                Image.Plane plane = image.getPlanes()[0];
                ByteBuffer buffer = plane.getBuffer();
                int pixelStride = plane.getPixelStride();
                int rowStride = plane.getRowStride();
                int paddedWidth = rowStride / pixelStride;
                PipFrameView view = frameView;
                if (view != null && buffer.remaining() > 0) {
                    view.updateFrame(buffer, paddedWidth, image.getWidth(), image.getHeight());
                }
            } catch (Exception e) {
                Log.w(TAG, "【画中画】帧拷贝异常: " + e.getMessage());
            } finally {
                if (image != null) {
                    image.close();
                }
            }
        }, frameHandler);
    }

    private static void stopFramePump() {
        if (frameThread != null) {
            frameThread.quitSafely();
            frameThread = null;
            frameHandler = null;
        }
    }

    /**
     * 通过 ADB shell 通道把地图 Activity 启动到指定虚拟屏
     */
    private static void launchOnDisplayViaShell(Activity activity, Intent launchIntent, int displayId) {
        android.content.ComponentName componentName = launchIntent.getComponent();
        if (componentName == null) {
            Log.e(TAG, "【画中画】地图启动 Intent 缺少组件名，无法通过 shell 启动");
            return;
        }
        String component = componentName.flattenToShortString();
        mapPackageName = component.substring(0, component.indexOf('/'));
        // am start 的输出重定向掉：ADB 通道读到首个换行就会关闭流，提前掐死后续看守循环
        String command = "am start --display " + displayId + " -n " + component + " > /dev/null 2>&1"
                + "; " + buildMoveTaskCommand(displayId);
        Log.i(TAG, "【画中画】shell 启动: " + component + " -> display " + displayId);
        runShell(activity, command);
    }

    /**
     * 轮询看守地图任务：地图入口 Activity 会先落在虚拟屏，其内部跳转（约1-3秒后）
     * 可能把整个任务带到主屏（Android 10+ 常见），因此持续观察并在偏离时拉回
     */
    private static String buildMoveTaskCommand(int displayId) {
        return "for i in 1 2 3 4 5 6 7 8 9 10 11 12; do sleep 1"
                + "; SID=$(am stack list | grep -B2 '" + mapPackageName + "/' | grep RootTask | tail -1"
                + " | sed 's/.*id=\\([0-9]*\\).*/\\1/')"
                + "; if [ -n \"$SID\" ]; then"
                + " CUR=$(am stack list | grep \"RootTask id=$SID \" | grep -o 'displayId=[0-9]*' | sed 's/displayId=//')"
                + "; if [ \"$CUR\" != \"" + displayId + "\" ]; then am display move-stack $SID " + displayId
                + " > /dev/null 2>&1; echo PIP_MOVED_$SID; fi"
                + "; fi; done; echo PIP_WATCH_DONE";
    }

    /**
     * 部署并启动触摸转发服务，随后把触摸事件转发给它
     */
    private static void setupTouchForwarding(Activity activity, int displayId) {
        frameView.setOnTouchListener((v, event) -> {
            forwardTouch(event);
            return true;
        });
        deployTouchServer(activity, displayId);
    }

    /**
     * 推送触摸服务 jar 并以指定虚拟屏启动（重复调用会先杀旧服务）
     *
     * jar 经 base64 分块追加写入：整条命令塞 4KB base64 会被部分 adbd 拒绝
     * （Stream open actively rejected），分块推送实测可靠
     */
    private static void deployTouchServer(Activity activity, int displayId) {
        touchExecutor.execute(() -> {
            try {
                byte[] jarBytes = readAsset(activity, TOUCH_JAR_ASSET);
                String base64 = Base64.encodeToString(jarBytes, Base64.NO_WRAP);
                // 分块写入 b64 临时文件
                final int chunkSize = 900;
                for (int i = 0; i < base64.length(); i += chunkSize) {
                    String part = base64.substring(i, Math.min(i + chunkSize, base64.length()));
                    String redirect = (i == 0) ? ">" : ">>";
                    runShellSync(activity,
                            "echo -n " + part + " " + redirect + " /data/local/tmp/pip_touch.b64; echo OK");
                }
                // 解码 → 杀旧服务 → 后台启动新服务
                // 注意不能用 pkill -f TouchServer：命令行里含 TouchServer 字样会误杀 shell 自身
                runShellSync(activity, "base64 -d /data/local/tmp/pip_touch.b64 > " + TOUCH_JAR_REMOTE
                        + "; rm -f /data/local/tmp/pip_touch.b64"
                        + "; " + KILL_TOUCH_SERVER_CMD
                        + "; CLASSPATH=" + TOUCH_JAR_REMOTE
                        + " nohup app_process /system/bin cn.youth996.pip.TouchServer " + displayId
                        + " > " + TOUCH_LOG_REMOTE + " 2>&1 & echo PIP_TOUCH_STARTED");
                Log.i(TAG, "【画中画】触摸服务已下发启动, display=" + displayId);
            } catch (Exception e) {
                Log.e(TAG, "【画中画】触摸服务部署失败: " + e.getMessage());
            }
        });
    }

    /**
     * 同步执行 shell 命令（在 touchExecutor 后台线程调用，最多等 15 秒）
     */
    private static String runShellSync(Context context, String command) throws Exception {
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        final String[] output = new String[1];
        cn.youth996.ui.adb.AdbEnhancedClient adbClient =
                new cn.youth996.ui.adb.AdbEnhancedClient(context, "127.0.0.1", 5555);
        adbClient.runShellCommand(command, new cn.youth996.ui.adb.AdbEnhancedClient.AdbEnhancedCallback() {
            @Override
            public void onConnectionSuccess() {}

            @Override
            public void onConnectionFailed(String error) {
                Log.e(TAG, "【画中画】ADB 连接失败: " + error);
                latch.countDown();
            }

            @Override
            public void onCommandExecuted(boolean success, String out) {
                output[0] = out;
                latch.countDown();
            }

            @Override
            public void onPermissionGranted(String permission) {}

            @Override
            public void onPermissionDenied(String permission, String reason) {}
        });
        latch.await(15, java.util.concurrent.TimeUnit.SECONDS);
        return output[0];
    }

    /**
     * 把触摸事件转发给 shell 侧触摸服务（协议：每行 "action x y"）
     */
    private static void forwardTouch(MotionEvent event) {
        int action = event.getActionMasked();
        if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_MOVE
                && action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL) {
            return;
        }
        final String line = action + " " + event.getX() + " " + event.getY() + "\n";
        touchExecutor.execute(() -> writeTouchLine(line));
    }

    private static void writeTouchLine(String line) {
        synchronized (touchLock) {
            for (int attempt = 0; attempt < 2; attempt++) {
                try {
                    if (touchSocket == null || touchSocket.isClosed() || !touchSocket.isConnected()) {
                        touchSocket = new Socket();
                        touchSocket.connect(new InetSocketAddress("127.0.0.1", TOUCH_PORT), 1500);
                        touchOut = touchSocket.getOutputStream();
                    }
                    touchOut.write(line.getBytes("UTF-8"));
                    touchOut.flush();
                    return;
                } catch (Exception e) {
                    closeTouchChannel();
                }
            }
        }
    }

    private static void closeTouchChannel() {
        synchronized (touchLock) {
            try {
                if (touchSocket != null) touchSocket.close();
            } catch (Exception ignored) {
            }
            touchSocket = null;
            touchOut = null;
        }
    }

    private static byte[] readAsset(Context context, String name) throws Exception {
        InputStream in = context.getAssets().open(name);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
        in.close();
        return out.toByteArray();
    }

    /**
     * 通过 ADB shell 通道（本机回环 5555）执行单条命令
     */
    private static void runShell(Context context, String command) {
        cn.youth996.ui.adb.AdbEnhancedClient adbClient =
                new cn.youth996.ui.adb.AdbEnhancedClient(context, "127.0.0.1", 5555);
        adbClient.runShellCommand(command, new cn.youth996.ui.adb.AdbEnhancedClient.AdbEnhancedCallback() {
            @Override
            public void onConnectionSuccess() {}

            @Override
            public void onConnectionFailed(String error) {
                Log.e(TAG, "【画中画】ADB 连接失败（需先开启 ADB 端口）: " + error);
            }

            @Override
            public void onCommandExecuted(boolean success, String output) {
                Log.i(TAG, "【画中画】shell 结果: success=" + success + ", output=" + output);
            }

            @Override
            public void onPermissionGranted(String permission) {}

            @Override
            public void onPermissionDenied(String permission, String reason) {}
        });
    }

    private static void releaseDisplay() {
        if (virtualDisplay != null) {
            try {
                virtualDisplay.release();
            } catch (Exception ignored) {
            }
            virtualDisplay = null;
        }
        if (imageReader != null) {
            try {
                imageReader.close();
            } catch (Exception ignored) {
            }
            imageReader = null;
        }
        stopFramePump();
    }

    /**
     * 画中画画面视图：持有最新一帧 Bitmap 并绘制
     */
    static class PipFrameView extends View {

        private Bitmap frameBuffer;
        private int frameWidth;
        private int frameHeight;
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final Rect srcRect = new Rect();
        private final Rect dstRect = new Rect();

        PipFrameView(Context context) {
            super(context);
        }

        /**
         * 在帧拷贝线程调用：把 ImageReader 的像素拷贝进可复用 Bitmap
         */
        synchronized void updateFrame(ByteBuffer buffer, int paddedWidth, int width, int height) {
            if (frameBuffer == null || frameWidth != width || frameHeight != height) {
                frameWidth = width;
                frameHeight = height;
                frameBuffer = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888);
            }
            buffer.rewind();
            frameBuffer.copyPixelsFromBuffer(buffer);
            postInvalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            Bitmap bmp;
            synchronized (this) {
                bmp = frameBuffer;
                if (bmp == null) {
                    return;
                }
                srcRect.set(0, 0, frameWidth, frameHeight);
            }
            dstRect.set(0, 0, getWidth(), getHeight());
            canvas.drawBitmap(bmp, srcRect, dstRect, paint);
        }
    }
}
