package cn.youth996.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.youth996.ui.R;

public class SettingsBasicFragment extends Fragment {

    private static final String PREFS_NAME = "basic_settings";
    private static final String KEY_ICON_SIZE = "icon_size";
    private static final String KEY_GRID_COLUMNS = "grid_columns";
    private static final String KEY_SHOW_APP_NAME = "show_app_name";
    private static final String KEY_SHOW_BADGE = "show_badge";
    private static final String KEY_DESKTOP_LAYOUT = "desktop_layout";
    private static final String KEY_AUTO_START = "auto_start";
    private static final String KEY_TIME_FORMAT = "time_format";

    private Spinner spinnerIconSize;
    private Spinner spinnerGridColumns;
    private Spinner spinnerDesktopLayout;
    private Spinner spinnerTimeFormat;
    private Switch switchShowAppName;
    private Switch switchShowBadge;
    private Switch switchAutoStart;

    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_basic, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getContext() != null) {
            prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }

        initViews(view);
        if (prefs != null) {
            loadSettings();
        }
        setupListeners();
    }

    private void initViews(View view) {
        spinnerIconSize = view.findViewById(R.id.spinner_icon_size);
        spinnerGridColumns = view.findViewById(R.id.spinner_grid_columns);
        spinnerDesktopLayout = view.findViewById(R.id.spinner_desktop_layout);
        spinnerTimeFormat = view.findViewById(R.id.spinner_time_format);
        switchShowAppName = view.findViewById(R.id.switch_show_app_name);
        switchShowBadge = view.findViewById(R.id.switch_show_badge);
        switchAutoStart = view.findViewById(R.id.switch_auto_start);
    }

    private void loadSettings() {
        int iconSize = prefs.getInt(KEY_ICON_SIZE, 1);
        int gridColumns = prefs.getInt(KEY_GRID_COLUMNS, 2);
        int desktopLayout = prefs.getInt(KEY_DESKTOP_LAYOUT, 0);
        int timeFormat = prefs.getInt(KEY_TIME_FORMAT, 1);
        boolean showAppName = prefs.getBoolean(KEY_SHOW_APP_NAME, true);
        boolean showBadge = prefs.getBoolean(KEY_SHOW_BADGE, true);
        boolean autoStart = prefs.getBoolean(KEY_AUTO_START, false);

        if (spinnerIconSize != null) spinnerIconSize.setSelection(iconSize);
        if (spinnerGridColumns != null) spinnerGridColumns.setSelection(gridColumns);
        if (spinnerDesktopLayout != null) spinnerDesktopLayout.setSelection(desktopLayout);
        if (spinnerTimeFormat != null) spinnerTimeFormat.setSelection(timeFormat);
        if (switchShowAppName != null) switchShowAppName.setChecked(showAppName);
        if (switchShowBadge != null) switchShowBadge.setChecked(showBadge);
        if (switchAutoStart != null) switchAutoStart.setChecked(autoStart);
    }

    private void setupListeners() {
        if (spinnerIconSize != null) {
            spinnerIconSize.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (prefs != null) {
                        prefs.edit().putInt(KEY_ICON_SIZE, position).apply();
                    }
                    if (getContext() != null) {
                        Toast.makeText(requireContext(), "图标大小已更改", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }

        if (spinnerGridColumns != null) {
            spinnerGridColumns.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (prefs != null) {
                        prefs.edit().putInt(KEY_GRID_COLUMNS, position).apply();
                    }
                    if (getContext() != null) {
                        Toast.makeText(requireContext(), "桌面列数已更改", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }

        if (spinnerDesktopLayout != null) {
            spinnerDesktopLayout.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (prefs != null) {
                        prefs.edit().putInt(KEY_DESKTOP_LAYOUT, position).apply();
                    }
                    if (getContext() != null) {
                        Toast.makeText(requireContext(), "桌面布局已更改", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }

        if (spinnerTimeFormat != null) {
            spinnerTimeFormat.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (prefs != null) {
                        prefs.edit().putInt(KEY_TIME_FORMAT, position).apply();
                    }
                    if (getContext() != null) {
                        String format = position == 0 ? "12小时" : "24小时";
                        Toast.makeText(requireContext(), "时钟格式已更改为" + format, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }

        if (switchShowAppName != null) {
            switchShowAppName.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (prefs != null) {
                    prefs.edit().putBoolean(KEY_SHOW_APP_NAME, isChecked).apply();
                }
                if (getContext() != null) {
                    Toast.makeText(requireContext(), isChecked ? "显示应用名称" : "隐藏应用名称", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (switchShowBadge != null) {
            switchShowBadge.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (prefs != null) {
                    prefs.edit().putBoolean(KEY_SHOW_BADGE, isChecked).apply();
                }
                if (getContext() != null) {
                    Toast.makeText(requireContext(), isChecked ? "显示应用角标" : "隐藏应用角标", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (switchAutoStart != null) {
            switchAutoStart.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (prefs != null) {
                    prefs.edit().putBoolean(KEY_AUTO_START, isChecked).apply();
                }
                if (getContext() != null) {
                    Toast.makeText(requireContext(), isChecked ? "已开启开机自启" : "已关闭开机自启", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
