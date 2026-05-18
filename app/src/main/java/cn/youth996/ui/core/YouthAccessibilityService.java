package cn.youth996.ui.core;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class YouthAccessibilityService extends AccessibilityService {
    private static final String TAG = "YouthAccessibilityService";
    private static YouthAccessibilityService instance;
    
    public interface OnAppChangeListener {
        void onAppChanged(String packageName, String className);
    }
    
    private OnAppChangeListener appChangeListener;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.i(TAG, "═══════════════════════════════════════════════════════════");
        Log.i(TAG, "【无障碍服务】服务已创建 - YouthAccessibilityService");
        Log.i(TAG, "═══════════════════════════════════════════════════════════");
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "【无障碍服务】服务即将销毁");
        instance = null;
        super.onDestroy();
        Log.w(TAG, "【无障碍服务】服务已销毁，实例已清空");
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "【无障碍服务】服务连接成功");

        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            Log.d(TAG, "【无障碍服务】配置服务信息:");
            Log.d(TAG, "  - eventTypes: TYPE_WINDOW_STATE_CHANGED | TYPE_WINDOW_CONTENT_CHANGED | TYPE_VIEW_CLICKED | TYPE_VIEW_LONG_CLICKED | TYPE_VIEW_TEXT_CHANGED");
            Log.d(TAG, "  - feedbackType: FEEDBACK_GENERIC");
            Log.d(TAG, "  - flags: DEFAULT | FLAG_INCLUDE_NOT_IMPORTANT_VIEWS | FLAG_REPORT_VIEW_IDS | FLAG_REQUEST_TOUCH_EXPLORATION_MODE");
            Log.d(TAG, "  - notificationTimeout: 100ms");

            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED |
                    AccessibilityEvent.TYPE_VIEW_CLICKED |
                    AccessibilityEvent.TYPE_VIEW_LONG_CLICKED |
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            info.flags = AccessibilityServiceInfo.DEFAULT |
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS |
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                    AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
            info.notificationTimeout = 100;

            setServiceInfo(info);
            Log.i(TAG, "【无障碍服务】服务配置完成，已准备好接收事件");
        } else {
            Log.e(TAG, "【无障碍服务】服务信息为空，配置失败！");
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            Log.w(TAG, "【无障碍服务】收到空事件");
            return;
        }

        int eventType = event.getEventType();
        String eventTypeName = getEventTypeName(eventType);

        switch (eventType) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                handleWindowStateChanged(event);
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                handleWindowContentChanged(event);
                break;
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                handleViewClicked(event);
                break;
            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                handleViewLongClicked(event);
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                handleViewTextChanged(event);
                break;
            default:
                Log.v(TAG, "【无障碍服务】收到其他类型事件: " + eventTypeName);
                break;
        }
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "⚠️ 【无障碍服务】服务被中断！");
        Log.w(TAG, "⚠️ 【无障碍服务】请检查系统设置，无障碍服务可能被意外停止");
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (event != null) {
            Log.v(TAG, "【无障碍服务】按键事件: keyCode=" + event.getKeyCode() +
                      ", action=" + event.getAction() +
                      ", eventTime=" + event.getEventTime());
        }
        return false;
    }

    private String getEventTypeName(int eventType) {
        switch (eventType) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED: return "WINDOW_STATE_CHANGED";
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED: return "WINDOW_CONTENT_CHANGED";
            case AccessibilityEvent.TYPE_VIEW_CLICKED: return "VIEW_CLICKED";
            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED: return "VIEW_LONG_CLICKED";
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED: return "VIEW_TEXT_CHANGED";
            default: return "UNKNOWN_" + eventType;
        }
    }

    private void handleWindowStateChanged(AccessibilityEvent event) {
        String className = event.getClassName() != null ?
                          event.getClassName().toString() : "null";
        String packageName = event.getPackageName() != null ?
                            event.getPackageName().toString() : "null";

        Log.i(TAG, "┌──────────────────────────────────────────────────────────────");
        Log.i(TAG, "│ 【窗口状态变化】");
        Log.i(TAG, "│   包名: " + packageName);
        Log.i(TAG, "│   类名: " + className);
        Log.i(TAG, "└──────────────────────────────────────────────────────────────");

        if (appChangeListener != null) {
            Log.d(TAG, "【无障碍服务】触发应用切换监听器回调");
            try {
                appChangeListener.onAppChanged(packageName, className);
                Log.d(TAG, "【无障碍服务】应用切换监听器回调执行完成");
            } catch (Exception e) {
                Log.e(TAG, "【无障碍服务】应用切换监听器回调执行失败: " + e.getMessage());
            }
        }
    }

    private void handleWindowContentChanged(AccessibilityEvent event) {
        String packageName = event.getPackageName() != null ?
                            event.getPackageName().toString() : "null";
        Log.v(TAG, "【内容变化】包: " + packageName + ", 子节点数: " + event.getSource().getChildCount());
    }

    private void handleViewClicked(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();
        if (source != null) {
            CharSequence text = source.getText();
            CharSequence contentDesc = source.getContentDescription();
            String nodeInfo = "text=[" + (text != null ? text.toString() : "null") + "], " +
                            "desc=[" + (contentDesc != null ? contentDesc.toString() : "null") + "]";

            Log.d(TAG, "【视图点击】" + nodeInfo);

            if (contentDesc != null && contentDesc.toString().contains("空调")) {
                Log.d(TAG, "【视图点击】检测到空调相关按钮: " + contentDesc);
            }

            source.recycle();
        }
    }

    private void handleViewLongClicked(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();
        if (source != null) {
            CharSequence text = source.getText();
            CharSequence contentDesc = source.getContentDescription();
            String nodeInfo = "text=[" + (text != null ? text.toString() : "null") + "], " +
                            "desc=[" + (contentDesc != null ? contentDesc.toString() : "null") + "]";

            Log.d(TAG, "【视图长按】" + nodeInfo);
            source.recycle();
        }
    }

    private void handleViewTextChanged(AccessibilityEvent event) {
        CharSequence beforeText = event.getBeforeText();
        List<CharSequence> textList = event.getText();
        
        StringBuilder textBuilder = new StringBuilder();
        if (textList != null && !textList.isEmpty()) {
            for (int i = 0; i < textList.size(); i++) {
                if (i > 0) textBuilder.append(", ");
                textBuilder.append(textList.get(i));
            }
        }
        
        String changeInfo = "before=[" + (beforeText != null ? beforeText.toString() : "null") + "], " +
                         "after=[" + textBuilder.toString() + "]";

        Log.v(TAG, "【文本变化】" + changeInfo);
    }

    public void setOnAppChangeListener(OnAppChangeListener listener) {
        this.appChangeListener = listener;
        Log.i(TAG, "【无障碍服务】✓ 已注册应用切换监听器");
        if (listener != null) {
            Log.d(TAG, "【无障碍服务】监听器有效，将接收窗口状态变化事件");
        } else {
            Log.w(TAG, "【无障碍服务】监听器为null，将不会接收任何事件");
        }
    }

    public void removeOnAppChangeListener() {
        Log.i(TAG, "【无障碍服务】正在移除应用切换监听器");
        this.appChangeListener = null;
        Log.i(TAG, "【无障碍服务】✓ 应用切换监听器已移除");
    }

    public static boolean isServiceEnabled() {
        boolean enabled = instance != null;
        Log.d(TAG, "【无障碍服务】服务状态检查: " + (enabled ? "运行中" : "未运行"));
        return enabled;
    }

    public static YouthAccessibilityService getInstance() {
        return instance;
    }

    public AccessibilityNodeInfo getRootInActiveWindow() {
        AccessibilityNodeInfo rootNode = super.getRootInActiveWindow();
        if (rootNode != null) {
            Log.v(TAG, "【无障碍服务】获取到根窗口节点，窗口标题: " +
                      (rootNode.getText() != null ? rootNode.getText().toString() : "无标题"));
        } else {
            Log.w(TAG, "【无障碍服务】⚠️ 无法获取根窗口节点（可能当前没有焦点窗口）");
        }
        return rootNode;
    }

    public boolean findAndClickNodeByText(String text) {
        Log.d(TAG, "═══════════════════════════════════════════════════════════");
        Log.d(TAG, "【查找文本并点击】搜索目标文本: \"" + text + "\"");
        Log.d(TAG, "【查找文本并点击】开始遍历窗口节点树...");

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(TAG, "【查找文本并点击】✗ 失败 - 根窗口节点为空");
            return false;
        }

        try {
            boolean found = findAndClickNode(rootNode, text, true);
            if (found) {
                Log.i(TAG, "【查找文本并点击】✓ 成功 - 找到并点击了包含文本 \"" + text + "\" 的节点");
            } else {
                Log.w(TAG, "【查找文本并点击】✗ 失败 - 未找到包含文本 \"" + text + "\" 的节点");
            }
            return found;
        } finally {
            rootNode.recycle();
            Log.d(TAG, "【查找文本并点击】节点遍历完成");
            Log.d(TAG, "═══════════════════════════════════════════════════════════");
        }
    }

    public boolean findAndClickNodeByContentDescription(String contentDescription) {
        Log.d(TAG, "═══════════════════════════════════════════════════════════");
        Log.d(TAG, "【查找描述并点击】搜索目标描述: \"" + contentDescription + "\"");
        Log.d(TAG, "【查找描述并点击】开始遍历窗口节点树...");

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(TAG, "【查找描述并点击】✗ 失败 - 根窗口节点为空");
            return false;
        }

        try {
            boolean found = findAndClickNodeByDesc(rootNode, contentDescription, true);
            if (found) {
                Log.i(TAG, "【查找描述并点击】✓ 成功 - 找到并点击了包含描述 \"" + contentDescription + "\" 的节点");
            } else {
                Log.w(TAG, "【查找描述并点击】✗ 失败 - 未找到包含描述 \"" + contentDescription + "\" 的节点");
            }
            return found;
        } finally {
            rootNode.recycle();
            Log.d(TAG, "【查找描述并点击】节点遍历完成");
            Log.d(TAG, "═══════════════════════════════════════════════════════════");
        }
    }

    private boolean findAndClickNode(AccessibilityNodeInfo node, String text, boolean recursive) {
        if (node == null) return false;

        int childCount = node.getChildCount();
        Log.v(TAG, "  ├─ 检查节点: text=[" + (node.getText() != null ? node.getText().toString() : "null") +
                  "], childCount=" + childCount);

        CharSequence nodeText = node.getText();
        if (nodeText != null && nodeText.toString().contains(text)) {
            Log.v(TAG, "  │  └─ ✓ 匹配成功! 正在执行点击操作...");
            boolean result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Log.v(TAG, "  │     └─ 点击操作" + (result ? "成功" : "失败"));
            return result;
        }

        if (recursive && childCount > 0) {
            Log.v(TAG, "  │  └─ 遍历 " + childCount + " 个子节点...");
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    try {
                        if (findAndClickNode(child, text, true)) {
                            Log.v(TAG, "  │     └─ 在子节点 " + i + " 中找到目标");
                            return true;
                        }
                    } finally {
                        child.recycle();
                    }
                }
            }
        }

        return false;
    }

    private boolean findAndClickNodeByDesc(AccessibilityNodeInfo node, String contentDescription, boolean recursive) {
        if (node == null) return false;

        int childCount = node.getChildCount();
        CharSequence nodeDesc = node.getContentDescription();
        Log.v(TAG, "  ├─ 检查节点: desc=[" + (nodeDesc != null ? nodeDesc.toString() : "null") +
                  "], childCount=" + childCount);

        if (nodeDesc != null && nodeDesc.toString().contains(contentDescription)) {
            Log.v(TAG, "  │  └─ ✓ 匹配成功! 正在执行点击操作...");
            boolean result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Log.v(TAG, "  │     └─ 点击操作" + (result ? "成功" : "失败"));
            return result;
        }

        if (recursive && childCount > 0) {
            Log.v(TAG, "  │  └─ 遍历 " + childCount + " 个子节点...");
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    try {
                        if (findAndClickNodeByDesc(child, contentDescription, true)) {
                            Log.v(TAG, "  │     └─ 在子节点 " + i + " 中找到目标");
                            return true;
                        }
                    } finally {
                        child.recycle();
                    }
                }
            }
        }

        return false;
    }

    public boolean clickButtonWithId(String viewId) {
        Log.d(TAG, "═══════════════════════════════════════════════════════════");
        Log.d(TAG, "【按ID点击】搜索目标ViewID: \"" + viewId + "\"");

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(TAG, "【按ID点击】✗ 失败 - 根窗口节点为空");
            return false;
        }

        try {
            boolean found = clickNodeByViewId(rootNode, viewId);
            if (found) {
                Log.i(TAG, "【按ID点击】✓ 成功 - 找到并点击了ID为 \"" + viewId + "\" 的节点");
            } else {
                Log.w(TAG, "【按ID点击】✗ 失败 - 未找到ID为 \"" + viewId + "\" 的节点");
            }
            return found;
        } finally {
            rootNode.recycle();
            Log.d(TAG, "═══════════════════════════════════════════════════════════");
        }
    }

    private boolean clickNodeByViewId(AccessibilityNodeInfo node, String viewId) {
        if (node == null) return false;

        String nodeViewId = node.getViewIdResourceName();
        if (nodeViewId != null && nodeViewId.contains(viewId)) {
            Log.v(TAG, "  └─ ✓ 匹配ViewID成功! 正在执行点击操作...");
            boolean result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Log.v(TAG, "     └─ 点击操作" + (result ? "成功" : "失败"));
            return result;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                try {
                    if (clickNodeByViewId(child, viewId)) {
                        return true;
                    }
                } finally {
                    child.recycle();
                }
            }
        }

        return false;
    }
}
