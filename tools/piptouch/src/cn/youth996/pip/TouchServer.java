package cn.youth996.pip;

import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.view.InputEvent;
import android.view.MotionEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 画中画触摸转发服务（以 shell 身份通过 app_process 运行）
 *
 * 桌面 App 把画中画容器上的触摸事件通过本机 TCP socket 发送过来，
 * 本服务将其转换为 MotionEvent 并注入到指定虚拟屏。
 *
 * 协议（每行一条）："action x y"
 *   action: 0=DOWN 1=UP 2=MOVE 3=CANCEL
 *   x, y: 虚拟屏像素坐标（与容器像素一致，虚拟屏与容器等大）
 *
 * 启动方式：
 *   CLASSPATH=/data/local/tmp/pip_touch_server.jar \
 *   app_process /system/bin cn.youth996.pip.TouchServer <displayId> &
 */
public final class TouchServer {

    private static final int PORT = 18927;

    public static void main(String[] args) {
        int displayId = Integer.parseInt(args[0]);
        try {
            // Android 13+ 的 InputManager 构造依赖主线程 Looper，app_process 进程需先准备
            try {
                android.os.Looper.prepareMainLooper();
            } catch (Throwable ignored) {
            }

            // Android 14+ 起 InputManager.getInstance() 依赖 Application 上下文（app_process 里没有），
            // 注入需改用 InputManagerGlobal.getInstance().injectInputEvent()
            Object injector = null;
            Method inject = null;
            try {
                Class<?> globalClass = Class.forName("android.hardware.input.InputManagerGlobal");
                injector = globalClass.getMethod("getInstance").invoke(null);
                inject = globalClass.getMethod("injectInputEvent", InputEvent.class, int.class);
                System.out.println("TOUCH_SERVER_DEBUG injector=InputManagerGlobal");
            } catch (Throwable t) {
                Method getInstance = InputManager.class.getDeclaredMethod("getInstance");
                getInstance.setAccessible(true);
                injector = getInstance.invoke(null);
                inject = InputManager.class.getMethod("injectInputEvent", InputEvent.class, int.class);
                System.out.println("TOUCH_SERVER_DEBUG injector=InputManager(legacy)");
            }
            if (injector == null) {
                System.out.println("TOUCH_SERVER_ERROR injector is null");
                return;
            }

            Method setDisplayId = null;
            java.lang.reflect.Field displayIdField = null;
            try {
                setDisplayId = InputEvent.class.getMethod("setDisplayId", int.class);
            } catch (NoSuchMethodException ignored) {
                // Android 10 以下没有公开/隐藏的 setDisplayId 方法，改反射 mDisplayId 字段
                try {
                    displayIdField = InputEvent.class.getDeclaredField("mDisplayId");
                    displayIdField.setAccessible(true);
                } catch (Throwable t) {
                    System.out.println("TOUCH_SERVER_WARN no displayId field: " + t.getMessage());
                }
            }

            ServerSocket serverSocket = new ServerSocket(PORT);
            boolean canTargetDisplay = setDisplayId != null || displayIdField != null;
            System.out.println("TOUCH_SERVER_READY port=" + PORT + " display=" + displayId
                    + " canTargetDisplay=" + canTargetDisplay);
            while (true) {
                Socket socket = serverSocket.accept();
                if (canTargetDisplay) {
                    handleConnection(socket, injector, inject, setDisplayId, displayIdField, displayId);
                } else {
                    // 无法定向虚拟屏时直接丢弃，避免事件泄漏到主屏误触桌面
                    System.out.println("TOUCH_SERVER_DROP 系统版本不支持定向注入");
                    try { socket.close(); } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable t) {
            System.out.println("TOUCH_SERVER_ERROR " + t);
            t.printStackTrace(System.out);
        }
    }

    private static void handleConnection(Socket socket, Object injector,
                                         Method inject, Method setDisplayId,
                                         java.lang.reflect.Field displayIdField, int displayId) {
        long downTime = 0;
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split(" ");
                if (parts.length != 3) {
                    continue;
                }
                int action = Integer.parseInt(parts[0]);
                float x = Float.parseFloat(parts[1]);
                float y = Float.parseFloat(parts[2]);

                long now = SystemClock.uptimeMillis();
                if (action == MotionEvent.ACTION_DOWN) {
                    downTime = now;
                }
                MotionEvent event = MotionEvent.obtain(downTime, now, action, x, y, 0);
                if (setDisplayId != null) {
                    setDisplayId.invoke(event, displayId);
                } else if (displayIdField != null) {
                    displayIdField.setInt(event, displayId);
                }
                Object result = inject.invoke(injector, event, 0 /* INJECT_INPUT_EVENT_MODE_ASYNC */);
                System.out.println("INJECT action=" + action + " x=" + x + " y=" + y
                        + " target=" + (setDisplayId != null ? "method" : (displayIdField != null ? "field" : "none"))
                        + " result=" + result);
                event.recycle();
            }
            socket.close();
        } catch (Throwable t) {
            System.out.println("TOUCH_SERVER_CONN_ERROR " + t.getMessage());
        }
    }
}
