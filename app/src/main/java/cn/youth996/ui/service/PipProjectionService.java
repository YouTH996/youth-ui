package cn.youth996.ui.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import cn.youth996.ui.core.PipVirtualDisplayManager;

/**
 * 画中画屏幕捕获前台服务
 *
 * Android 14（API 34）起，getMediaProjection 之前必须先运行一个
 * foregroundServiceType=mediaProjection 的前台服务，否则抛 SecurityException。
 * 本服务仅用于满足该要求：启动后即进入前台并回调画中画管理器继续授权流程。
 *
 * @author Youth Desktop Team
 */
public class PipProjectionService extends Service {

    private static final String TAG = "PipProjectionService";
    private static final String CHANNEL_ID = "pip_projection";
    private static final int NOTIFICATION_ID = 42;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, "画中画", NotificationManager.IMPORTANCE_MIN));
        }
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("画中画运行中")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build();

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        Log.i(TAG, "【画中画】前台服务已就绪");

        // 通知管理器：前台服务已就绪，可以继续 getMediaProjection
        PipVirtualDisplayManager.onProjectionServiceReady(this);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
