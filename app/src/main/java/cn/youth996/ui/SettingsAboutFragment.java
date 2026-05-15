package cn.youth996.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsAboutFragment extends Fragment {

    private TextView textVersion;
    private Button btnFeedback;
    private Button btnRate;
    private Button btnPrivacy;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupVersion();
        setupListeners();
    }

    private void initViews(View view) {
        textVersion = view.findViewById(R.id.text_version);
        btnFeedback = view.findViewById(R.id.btn_feedback);
        btnRate = view.findViewById(R.id.btn_rate);
        btnPrivacy = view.findViewById(R.id.btn_privacy);
    }

    private void setupVersion() {
        try {
            if (getContext() != null) {
                String packageName = requireContext().getPackageName();
                android.content.pm.PackageInfo packageInfo =
                    requireContext().getPackageManager().getPackageInfo(packageName, 0);
                String versionName = packageInfo.versionName;
                if (textVersion != null) {
                    textVersion.setText("版本 " + versionName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (textVersion != null) {
                textVersion.setText("版本 1.0.0");
            }
        }
    }

    private void setupListeners() {
        if (btnFeedback != null) {
            btnFeedback.setOnClickListener(v -> {
                if (getContext() != null) {
                    Toast.makeText(requireContext(), "感谢您的反馈！", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnRate != null) {
            btnRate.setOnClickListener(v -> {
                if (getContext() != null) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("market://details?id=" + requireContext().getPackageName()));
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "无法打开应用市场", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (btnPrivacy != null) {
            btnPrivacy.setOnClickListener(v -> {
                if (getContext() != null) {
                    Toast.makeText(requireContext(), "隐私政策", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
