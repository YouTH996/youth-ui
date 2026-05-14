package com.youth.desktop;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class YouthAccessibilityService extends android.accessibilityservice.AccessibilityService {
    private static final String TAG = "YouthAccessibilityService";
    private static YouthAccessibilityService instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "无障碍服务已创建");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.d(TAG, "无障碍服务已销毁");
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "无障碍服务已连接");

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
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
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        Log.d(TAG, "收到无障碍事件: " + event.getEventType() +
                   " 包名: " + event.getPackageName());

        switch (event.getEventType()) {
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
        }
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "无障碍服务被中断");
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        return false;
    }

    private void handleWindowStateChanged(AccessibilityEvent event) {
        String className = event.getClassName() != null ?
                          event.getClassName().toString() : "";
        String packageName = event.getPackageName() != null ?
                            event.getPackageName().toString() : "";

        Log.d(TAG, "窗口状态变化 - 类名: " + className +
                   " 包名: " + packageName);
    }

    private void handleWindowContentChanged(AccessibilityEvent event) {
        Log.d(TAG, "窗口内容变化");
    }

    private void handleViewClicked(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();
        if (source != null) {
            CharSequence text = source.getText();
            CharSequence contentDesc = source.getContentDescription();
            Log.d(TAG, "点击视图 - 文本: " + text + " 描述: " + contentDesc);
            source.recycle();
        }
    }

    private void handleViewLongClicked(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();
        if (source != null) {
            CharSequence text = source.getText();
            CharSequence contentDesc = source.getContentDescription();
            Log.d(TAG, "长按视图 - 文本: " + text + " 描述: " + contentDesc);
            source.recycle();
        }
    }

    public static boolean isServiceEnabled() {
        return instance != null;
    }

    public static YouthAccessibilityService getInstance() {
        return instance;
    }

    public AccessibilityNodeInfo getRootInActiveWindow() {
        return super.getRootInActiveWindow();
    }

    public boolean findAndClickNodeByText(String text) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return false;

        boolean found = findAndClickNode(rootNode, text, true);
        rootNode.recycle();
        return found;
    }

    public boolean findAndClickNodeByContentDescription(String contentDescription) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return false;

        boolean found = findAndClickNodeByDesc(rootNode, contentDescription, true);
        rootNode.recycle();
        return found;
    }

    private boolean findAndClickNode(AccessibilityNodeInfo node, String text, boolean recursive) {
        if (node == null) return false;

        CharSequence nodeText = node.getText();
        if (nodeText != null && nodeText.toString().contains(text)) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }

        if (recursive) {
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    if (findAndClickNode(child, text, true)) {
                        child.recycle();
                        return true;
                    }
                    child.recycle();
                }
            }
        }

        return false;
    }

    private boolean findAndClickNodeByDesc(AccessibilityNodeInfo node, String contentDescription, boolean recursive) {
        if (node == null) return false;

        CharSequence nodeDesc = node.getContentDescription();
        if (nodeDesc != null && nodeDesc.toString().contains(contentDescription)) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }

        if (recursive) {
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    if (findAndClickNodeByDesc(child, contentDescription, true)) {
                        child.recycle();
                        return true;
                    }
                    child.recycle();
                }
            }
        }

        return false;
    }

    public boolean clickButtonWithId(String viewId) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return false;

        boolean found = clickNodeByViewId(rootNode, viewId);
        rootNode.recycle();
        return found;
    }

    private boolean clickNodeByViewId(AccessibilityNodeInfo node, String viewId) {
        if (node == null) return false;

        String nodeViewId = node.getViewIdResourceName();
        if (nodeViewId != null && nodeViewId.contains(viewId)) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (clickNodeByViewId(child, viewId)) {
                    child.recycle();
                    return true;
                }
                child.recycle();
            }
        }

        return false;
    }
}
