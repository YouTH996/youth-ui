package com.youth.desktop;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 应用抽屉Activity - 显示所有已安装应用的界面
 *
 * 功能说明：
 * 1. 以全屏形式展示系统中所有可启动的应用
 * 2. 提供搜索功能,支持按应用名称过滤
 * 3. 使用RecyclerView网格布局显示应用列表
 * 4. 提供平滑的滑入/滑出动画效果
 * 5. 支持点击应用图标启动应用
 * 6. 支持长按查看应用详情
 *
 * 界面布局：
 * - 顶部：搜索栏和操作按钮（返回、关闭）
 * - 主体：应用列表（5列网格布局）
 *
 * @author Youth Desktop Team
 */
public class AppDrawerActivity extends AppCompatActivity {

    // ==================== 视图组件声明 ====================
    /** 抽屉容器 - 整个应用抽屉的根布局 */
    private LinearLayout drawerContainer;

    /** 应用列表视图 - 使用RecyclerView显示应用网格 */
    private RecyclerView appsRecyclerView;

    /** 搜索框 - 用于输入关键词搜索应用 */
    private EditText searchBox;

    /** 关闭按钮 - 点击关闭应用抽屉 */
    private ImageButton closeButton;

    /** 主页按钮 - 点击返回系统桌面 */
    private ImageButton homeButton;

    // ==================== 数据管理 ====================
    /** 所有应用列表 - 包含系统中所有可启动的应用 */
    private List<AppInfo> allApps = new ArrayList<>();

    /** 筛选后的应用列表 - 根据搜索条件过滤后的应用 */
    private List<AppInfo> filteredApps = new ArrayList<>();

    /** 应用列表适配器 - 用于RecyclerView的数据绑定 */
    private AppDrawerAdapter adapter;

    /**
     * Activity创建时的初始化方法
     *
     * 执行流程：
     * 1. 设置窗口标志位,允许触摸事件穿透
     * 2. 调用父类onCreate方法
     * 3. 设置内容布局为activity_app_drawer
     * 4. 初始化视图组件
     * 5. 加载所有应用
     * 6. 设置RecyclerView
     * 7. 设置点击事件
     * 8. 播放滑入动画
     *
     * @param savedInstanceState 包含Activity之前保存状态的Bundle对象
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 获取Window对象
        Window window = getWindow();
        // 设置布局标志,使内容填充整个屏幕
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        );
        // 设置非触摸模式标志,允许触摸事件穿透到下层
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        );

        // 设置内容布局
        setContentView(R.layout.activity_app_drawer);

        // 初始化视图组件
        initViews();
        // 加载所有应用
        loadApps();
        // 设置RecyclerView
        setupRecyclerView();
        // 设置点击事件
        setupClickListeners();
        // 播放滑入动画
        animateIn();
    }

    /**
     * 初始化视图组件
     *
     * 从布局文件中获取所有需要操作的视图组件,
     * 包括抽屉容器、应用列表、搜索框和按钮等
     */
    private void initViews() {
        // 获取抽屉容器
        drawerContainer = findViewById(R.id.drawer_container);
        // 获取应用列表RecyclerView
        appsRecyclerView = findViewById(R.id.apps_recycler_view);
        // 获取搜索框
        searchBox = findViewById(R.id.search_box);
        // 获取关闭按钮
        closeButton = findViewById(R.id.close_button);
        // 获取主页按钮
        homeButton = findViewById(R.id.home_button);
    }

    /**
     * 加载所有已安装的应用
     *
     * 查询系统中所有LAUNCHER类型的Activity,
     * 创建AppInfo对象保存应用信息,
     * 并按应用名称排序
     *
     * 过滤规则：
     * - 排除当前应用自身（桌面应用）
     * - 只保留有启动图标的应用
     */
    private void loadApps() {
        // 创建查询Intent,查找所有LAUNCHER类型的Activity
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        // 获取包管理器
        PackageManager pm = getPackageManager();
        // 查询所有匹配Intent的应用
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);

        // 遍历所有查询到的应用
        for (ResolveInfo resolveInfo : resolveInfos) {
            // 排除当前桌面应用自身,避免显示自己
            if (resolveInfo.activityInfo.packageName.equals(getPackageName())) {
                continue;
            }

            // 创建新的AppInfo对象
            AppInfo appInfo = new AppInfo();
            // 设置应用名称
            appInfo.label = resolveInfo.loadLabel(pm).toString();
            // 设置应用包名
            appInfo.packageName = resolveInfo.activityInfo.packageName;
            // 设置Activity类名
            appInfo.className = resolveInfo.activityInfo.name;
            // 设置应用图标
            appInfo.icon = resolveInfo.loadIcon(pm);
            // 设置启动Intent
            appInfo.intent = pm.getLaunchIntentForPackage(appInfo.packageName);

            // 添加到所有应用列表
            allApps.add(appInfo);
        }

        // 按应用名称排序(忽略大小写)
        Collections.sort(allApps, (a1, a2) -> a1.label.compareToIgnoreCase(a2.label));

        // 初始时筛选列表包含所有应用
        filteredApps.addAll(allApps);
    }

    /**
     * 设置RecyclerView
     *
     * 创建适配器并设置到RecyclerView,
     * 配置LayoutManager为5列网格布局,
     * 实现应用列表的网格显示
     */
    private void setupRecyclerView() {
        // 创建适配器
        adapter = new AppDrawerAdapter(filteredApps, this);
        // 设置网格布局管理器（5列）
        appsRecyclerView.setLayoutManager(new GridLayoutManager(this, 5));
        // 设置适配器
        appsRecyclerView.setAdapter(adapter);
    }

    /**
     * 设置点击事件监听器
     *
     * 为关闭按钮、主页按钮和搜索框设置点击事件,
     * 实现关闭抽屉、返回主页和搜索应用的功能
     */
    private void setupClickListeners() {
        // 设置关闭按钮点击事件 - 关闭抽屉
        closeButton.setOnClickListener(v -> animateOut());

        // 设置主页按钮点击事件 - 返回主页
        homeButton.setOnClickListener(v -> {
            // 播放退出动画
            animateOut();
            // 延迟300ms后返回主页
            new android.os.Handler().postDelayed(() -> {
                // 创建返回主屏幕的Intent
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // 启动主屏幕Activity
                startActivity(intent);
            }, 300);
        });

        // 设置搜索框焦点变化监听
        searchBox.setOnFocusChangeListener((v, hasFocus) -> {
            // 当失去焦点时,如果搜索框为空则显示所有应用
            if (!hasFocus) {
                String query = searchBox.getText().toString();
                filterApps(query);
            }
        });
    }

    /**
     * 根据搜索关键词过滤应用列表
     *
     * 清空筛选列表,
     * 根据关键词匹配应用名称,
     * 支持大小写不敏感的模糊搜索,
     * 刷新适配器更新显示
     *
     * @param query 搜索关键词
     */
    private void filterApps(String query) {
        // 清空筛选列表
        filteredApps.clear();

        // 如果关键词为空或null,显示所有应用
        if (query == null || query.isEmpty()) {
            filteredApps.addAll(allApps);
        } else {
            // 遍历所有应用,查找名称包含关键词的应用
            for (AppInfo app : allApps) {
                // 转换为小写进行不区分大小写的匹配
                if (app.label.toLowerCase().contains(query.toLowerCase())) {
                    filteredApps.add(app);
                }
            }
        }

        // 通知适配器数据已改变,刷新列表
        adapter.notifyDataSetChanged();
    }

    /**
     * 播放滑入动画
     *
     * 应用抽屉从屏幕底部滑入,
     * 同时透明度从0渐变到1,
     * 使用300毫秒的减速插值器实现平滑效果
     *
     * 动画效果：
     * - Alpha动画：0.0 → 1.0（淡入）
     * - Y轴位移：高度 → 0（滑入）
     */
    private void animateIn() {
        // 初始状态设置
        drawerContainer.setAlpha(0f);
        drawerContainer.setTranslationY(drawerContainer.getHeight());

        // 创建动画集合
        AnimatorSet animatorSet = new AnimatorSet();
        // 创建透明度动画（淡入）
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(drawerContainer, "alpha", 0f, 1f);
        // 创建Y轴位移动画（滑入）
        ObjectAnimator translateAnim = ObjectAnimator.ofFloat(drawerContainer, "translationY",
            drawerContainer.getHeight(), 0f);

        // 同时播放两个动画
        animatorSet.playTogether(alphaAnim, translateAnim);
        // 设置动画时长为300毫秒
        animatorSet.setDuration(300);
        // 设置减速插值器,开始快结束慢
        animatorSet.setInterpolator(new android.view.animation.DecelerateInterpolator());
        // 开始播放动画
        animatorSet.start();
    }

    /**
     * 播放滑出动画
     *
     * 应用抽屉向屏幕底部滑出,
     * 同时透明度从1渐变到0,
     * 使用300毫秒的加速插值器,
     * 动画结束后关闭Activity
     *
     * 动画效果：
     * - Alpha动画：1.0 → 0.0（淡出）
     * - Y轴位移：0 → 高度（滑出）
     */
    private void animateOut() {
        // 创建动画集合
        AnimatorSet animatorSet = new AnimatorSet();
        // 创建透明度动画（淡出）
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(drawerContainer, "alpha", 1f, 0f);
        // 创建Y轴位移动画（滑出）
        ObjectAnimator translateAnim = ObjectAnimator.ofFloat(drawerContainer, "translationY",
            0f, drawerContainer.getHeight());

        // 同时播放两个动画
        animatorSet.playTogether(alphaAnim, translateAnim);
        // 设置动画时长为300毫秒
        animatorSet.setDuration(300);
        // 设置加速插值器,开始慢结束快
        animatorSet.setInterpolator(new android.view.animation.AccelerateInterpolator());

        // 设置动画监听器
        animatorSet.addListener(new AnimatorListenerAdapter() {
            /**
             * 动画结束时调用
             *
             * 关闭Activity并取消切换动画,
             * 实现流畅的抽屉关闭效果
             *
             * @param animation 结束的动画对象
             */
            @Override
            public void onAnimationEnd(Animator animation) {
                // 关闭Activity
                finish();
                // 取消Activity切换动画
                overridePendingTransition(0, 0);
            }
        });

        // 开始播放动画
        animatorSet.start();
    }

    /**
     * 处理返回键事件
     *
     * 按下返回键时,
     * 播放退出动画而非直接关闭,
     * 实现平滑的关闭效果
     */
    @Override
    public void onBackPressed() {
        // 播放退出动画
        animateOut();
    }
}
