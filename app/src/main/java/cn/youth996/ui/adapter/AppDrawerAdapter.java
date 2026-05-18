package cn.youth996.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.youth996.ui.R;
import cn.youth996.ui.core.AppInfo;

/**
 * 应用抽屉列表适配器
 *
 * 功能说明：
 * 1. 为RecyclerView提供数据绑定和视图回收机制
 * 2. 管理应用列表项的显示和数据更新
 * 3. 处理应用项的点击和长按事件
 * 4. 支持高效滚动的视图复用
 *
 * RecyclerView适配器工作原理：
 * - onCreateViewHolder: 创建新的视图持有者
 * - onBindViewHolder: 绑定数据到视图
 * - getItemCount: 返回数据项数量
 * - 视图回收: 滚出屏幕的视图会被重新利用
 *
 * @author Youth Desktop Team
 */
public class AppDrawerAdapter extends RecyclerView.Adapter<AppDrawerAdapter.AppViewHolder> {

    // ==================== 成员变量 ====================
    /** 应用数据列表 - 包含要显示的应用信息 */
    private List<AppInfo> apps;

    /** 上下文对象 - 用于启动Activity和获取资源 */
    private Context context;

    /** 布局填充器 - 用于将XML布局转换为View对象 */
    private LayoutInflater inflater;

    /**
     * 构造函数
     *
     * 初始化适配器,设置应用列表和上下文,
     * 并创建布局填充器用于视图创建
     *
     * @param apps 应用信息列表
     * @param context 上下文对象
     */
    public AppDrawerAdapter(List<AppInfo> apps, Context context) {
        this.apps = apps;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    /**
     * 创建视图持有者
     *
     * 当RecyclerView需要显示新项时调用,
     * 加载item_app_drawer布局文件,
     * 创建AppViewHolder实例包装视图
     *
     * @param parent 父视图组
     * @param viewType 视图类型标识
     * @return 创建的视图持有者
     */
    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 使用布局填充器加载列表项布局
        View view = inflater.inflate(R.layout.item_app_drawer, parent, false);
        // 创建并返回视图持有者
        return new AppViewHolder(view);
    }

    /**
     * 绑定数据到视图
     *
     * 当RecyclerView需要显示某项数据时调用,
     * 根据位置获取对应的AppInfo,
     * 调用ViewHolder的bind方法绑定数据
     *
     * @param holder 视图持有者
     * @param position 数据项位置
     */
    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        // 根据位置获取应用信息
        AppInfo appInfo = apps.get(position);
        // 绑定数据到视图
        holder.bind(appInfo);
    }

    /**
     * 获取数据项数量
     *
     * 返回列表中应用项的总数,
     * 用于RecyclerView确定滚动范围
     *
     * @return 应用项数量
     */
    @Override
    public int getItemCount() {
        return apps.size();
    }

    /**
     * 应用视图持有者内部类
     *
     * 负责管理单个应用列表项的视图,
     * 包括图标、标签和事件处理
     */
    class AppViewHolder extends RecyclerView.ViewHolder {

        // ==================== 视图组件 ====================
        /** 应用项容器 - 包含图标和标签的根布局 */
        private LinearLayout container;

        /** 应用图标 - ImageView显示应用图标 */
        private ImageView iconView;

        /** 应用标签 - TextView显示应用名称 */
        private TextView labelView;

        /**
         * 构造函数
         *
         * 从视图层级中查找子视图,
         * 初始化图标、标签等组件引用
         *
         * @param itemView 列表项的根视图
         */
        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            // 查找应用项容器
            container = itemView.findViewById(R.id.app_item_container);
            // 查找应用图标
            iconView = itemView.findViewById(R.id.app_icon);
            // 查找应用标签
            labelView = itemView.findViewById(R.id.app_label);
        }

        /**
         * 绑定应用数据
         *
         * 设置应用图标和名称,
         * 为容器设置点击和长按事件监听
         *
         * @param appInfo 要绑定的应用信息
         */
        public void bind(final AppInfo appInfo) {
            // 设置应用图标
            iconView.setImageDrawable(appInfo.icon);
            // 设置应用名称
            labelView.setText(appInfo.label);

            // 设置点击事件 - 启动应用
            container.setOnClickListener(v -> {
                // 检查Intent是否有效
                if (appInfo.intent != null) {
                    // 使用上下文启动Activity
                    context.startActivity(appInfo.intent);
                }
            });

            // 设置长按事件 - 显示应用信息
            container.setOnLongClickListener(v -> {
                // 显示应用信息对话框
                showAppInfo(appInfo);
                return true;
            });
        }

        /**
         * 显示应用信息对话框
         *
         * 当用户长按应用图标时显示,
         * 包含应用名称、包名和操作选项
         *
         * @param appInfo 要显示信息的应用
         */
        private void showAppInfo(AppInfo appInfo) {
            // 创建AlertDialog构建器
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
            // 设置对话框标题为应用名称
            builder.setTitle(appInfo.label);
            // 设置对话框内容为应用包名
            builder.setMessage("包名: " + appInfo.packageName);
            // 设置确认按钮
            builder.setPositiveButton("打开", (dialog, which) -> {
                // 检查Intent是否有效
                if (appInfo.intent != null) {
                    // 启动应用
                    context.startActivity(appInfo.intent);
                }
            });
            // 设置取消按钮
            builder.setNegativeButton("取消", null);
            // 显示对话框
            builder.show();
        }
    }
}
