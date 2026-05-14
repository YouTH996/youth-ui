package com.youth.desktop;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsCardsFragment extends Fragment {

    private static final String PREFS_NAME = "cards_settings";
    private static final String KEY_SHOW_CARDS = "show_cards";
    private static final String KEY_CARD_CORNER_RADIUS = "card_corner_radius";
    private static final String KEY_CARD_ALPHA = "card_alpha";
    private static final String KEY_CARD_SHADOW = "card_shadow";

    private Switch switchShowCards;
    private Switch switchCardShadow;
    private SeekBar seekbarCardCornerRadius;
    private SeekBar seekbarCardAlpha;

    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_cards, container, false);
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
        switchShowCards = view.findViewById(R.id.switch_show_cards);
        switchCardShadow = view.findViewById(R.id.switch_card_shadow);
        seekbarCardCornerRadius = view.findViewById(R.id.seekbar_card_corner_radius);
        seekbarCardAlpha = view.findViewById(R.id.seekbar_card_alpha);
    }

    private void loadSettings() {
        if (prefs == null) return;

        boolean showCards = prefs.getBoolean(KEY_SHOW_CARDS, true);
        boolean cardShadow = prefs.getBoolean(KEY_CARD_SHADOW, true);
        int cornerRadius = prefs.getInt(KEY_CARD_CORNER_RADIUS, 15);
        int cardAlpha = prefs.getInt(KEY_CARD_ALPHA, 80);

        if (switchShowCards != null) switchShowCards.setChecked(showCards);
        if (switchCardShadow != null) switchCardShadow.setChecked(cardShadow);
        if (seekbarCardCornerRadius != null) seekbarCardCornerRadius.setProgress(cornerRadius);
        if (seekbarCardAlpha != null) seekbarCardAlpha.setProgress(cardAlpha);
    }

    private void setupListeners() {
        if (switchShowCards != null) {
            switchShowCards.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (prefs != null) {
                    prefs.edit().putBoolean(KEY_SHOW_CARDS, isChecked).apply();
                }
                if (getContext() != null) {
                    Toast.makeText(requireContext(), isChecked ? "已显示桌面卡片" : "已隐藏桌面卡片", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (switchCardShadow != null) {
            switchCardShadow.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (prefs != null) {
                    prefs.edit().putBoolean(KEY_CARD_SHADOW, isChecked).apply();
                }
                if (getContext() != null) {
                    Toast.makeText(requireContext(), isChecked ? "已开启卡片阴影" : "已关闭卡片阴影", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (seekbarCardCornerRadius != null) {
            seekbarCardCornerRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && prefs != null) {
                        prefs.edit().putInt(KEY_CARD_CORNER_RADIUS, progress).apply();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (getContext() != null) {
                        Toast.makeText(requireContext(), "卡片圆角已更新", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (seekbarCardAlpha != null) {
            seekbarCardAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && prefs != null) {
                        prefs.edit().putInt(KEY_CARD_ALPHA, progress).apply();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (getContext() != null) {
                        Toast.makeText(requireContext(), "卡片透明度已更新", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}
