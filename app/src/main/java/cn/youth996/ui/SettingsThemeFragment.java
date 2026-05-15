package cn.youth996.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsThemeFragment extends Fragment {

    private static final String PREFS_NAME = "theme_settings";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_ACCENT_COLOR = "accent_color";
    private static final String KEY_WALLPAPER_INDEX = "wallpaper_index";

    private ImageView imgCurrentWallpaper;
    private RadioGroup radioGroupThemeMode;
    private RadioButton radioLight;
    private RadioButton radioDark;
    private Button btnSystemWallpapers;
    private Button btnGalleryWallpapers;

    private SharedPreferences prefs;
    private int currentWallpaperIndex = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_theme, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getContext() != null) {
            prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        }

        initViews(view);
        if (prefs != null) {
            loadSettings();
        }
        setupListeners();
    }

    private void initViews(View view) {
        imgCurrentWallpaper = view.findViewById(R.id.img_current_wallpaper);
        radioGroupThemeMode = view.findViewById(R.id.radio_group_theme_mode);
        radioLight = view.findViewById(R.id.radio_light);
        radioDark = view.findViewById(R.id.radio_dark);
        btnSystemWallpapers = view.findViewById(R.id.btn_system_wallpapers);
        btnGalleryWallpapers = view.findViewById(R.id.btn_gallery_wallpapers);
    }

    private void loadSettings() {
        int themeMode = prefs.getInt(KEY_THEME_MODE, 0);
        currentWallpaperIndex = prefs.getInt(KEY_WALLPAPER_INDEX, 0);

        if (radioLight != null && radioDark != null) {
            if (themeMode == 0) {
                radioLight.setChecked(true);
            } else {
                radioDark.setChecked(true);
            }
        }

        updateWallpaperPreview();
    }

    private void setupListeners() {
        if (radioGroupThemeMode != null) {
            radioGroupThemeMode.setOnCheckedChangeListener((group, checkedId) -> {
                if (prefs != null) {
                    int mode = (checkedId == R.id.radio_light) ? 0 : 1;
                    prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
                    if (getContext() != null) {
                        String themeName = (mode == 0) ? "浅色模式" : "深色模式";
                        Toast.makeText(requireContext(), "已切换到" + themeName, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (btnSystemWallpapers != null) {
            btnSystemWallpapers.setOnClickListener(v -> {
                if (getContext() != null) {
                    Toast.makeText(requireContext(), "打开系统壁纸选择器", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnGalleryWallpapers != null) {
            btnGalleryWallpapers.setOnClickListener(v -> {
                if (getContext() != null) {
                    Toast.makeText(requireContext(), "打开相册选择壁纸", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateWallpaperPreview() {
        try {
            String wallpaperPath = "webui/wallpaper/" + (currentWallpaperIndex + 1) + ".jpg";
            if (imgCurrentWallpaper != null && getContext() != null) {
                android.graphics.drawable.Drawable wallpaperDrawable =
                    getResources().getDrawable(R.drawable.wallpaper_preview_background_dark);
                imgCurrentWallpaper.setImageDrawable(wallpaperDrawable);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
