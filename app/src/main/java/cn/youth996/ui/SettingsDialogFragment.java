package cn.youth996.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class SettingsDialogFragment extends DialogFragment {

    private RadioGroup radioGroupTabs;
    private RadioButton radioBasic;
    private RadioButton radioTheme;
    private RadioButton radioCards;
    private RadioButton radioSystem;
    private RadioButton radioAbout;
    private ImageButton btnClose;
    private FrameLayout contentContainer;

    private Fragment currentFragment;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupListeners();
        showContent(0);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                dpToPx(800),
                dpToPx(350)
            );
        }
    }

    private void initViews(View view) {
        radioGroupTabs = view.findViewById(R.id.radio_group_tabs);
        radioBasic = view.findViewById(R.id.radio_basic);
        radioTheme = view.findViewById(R.id.radio_theme);
        radioCards = view.findViewById(R.id.radio_cards);
        radioSystem = view.findViewById(R.id.radio_system);
        radioAbout = view.findViewById(R.id.radio_about);
        btnClose = view.findViewById(R.id.btn_close);
        contentContainer = view.findViewById(R.id.content_container);
    }

    private void setupListeners() {
        if (radioGroupTabs != null) {
            radioGroupTabs.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.radio_basic) {
                    showContent(0);
                } else if (checkedId == R.id.radio_theme) {
                    showContent(1);
                } else if (checkedId == R.id.radio_cards) {
                    showContent(2);
                } else if (checkedId == R.id.radio_system) {
                    showContent(3);
                } else if (checkedId == R.id.radio_about) {
                    showContent(4);
                }
            });
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }
    }

    private void showContent(int index) {
        Fragment fragment;
        String tag;

        switch (index) {
            case 0:
                fragment = new SettingsBasicFragment();
                tag = "basic";
                break;
            case 1:
                fragment = new SettingsThemeFragment();
                tag = "theme";
                break;
            case 2:
                fragment = new SettingsCardsFragment();
                tag = "cards";
                break;
            case 3:
                fragment = new SettingsSystemFragment();
                tag = "system";
                break;
            case 4:
            default:
                fragment = new SettingsAboutFragment();
                tag = "about";
                break;
        }

        if (currentFragment != null && currentFragment.getTag() != null && 
            currentFragment.getTag().equals(tag)) {
            return;
        }

        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }

        Fragment existingFragment = getChildFragmentManager().findFragmentByTag(tag);
        if (existingFragment != null) {
            transaction.show(existingFragment);
            currentFragment = existingFragment;
        } else {
            transaction.add(R.id.content_container, fragment, tag);
            currentFragment = fragment;
        }

        transaction.commit();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
