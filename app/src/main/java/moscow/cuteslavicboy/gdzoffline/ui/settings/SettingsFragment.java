package moscow.cuteslavicboy.gdzoffline.ui.settings;

import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;

import moscow.cuteslavicboy.gdzoffline.GdzApp;
import moscow.cuteslavicboy.gdzoffline.R;
import moscow.cuteslavicboy.gdzoffline.repo.CatalogRepository;
import moscow.cuteslavicboy.gdzoffline.util.Prefs;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Prefs prefs = GdzApp.prefs();

        RadioGroup themeGroup = view.findViewById(R.id.theme_group);
        int checkedId;
        switch (prefs.getTheme()) {
            case Prefs.THEME_LIGHT:
                checkedId = R.id.theme_light;
                break;
            case Prefs.THEME_DARK:
                checkedId = R.id.theme_dark;
                break;
            default:
                checkedId = R.id.theme_system;
                break;
        }
        themeGroup.check(checkedId);
        themeGroup.setOnCheckedChangeListener((group, id) -> {
            int theme;
            if (id == R.id.theme_light) theme = Prefs.THEME_LIGHT;
            else if (id == R.id.theme_dark) theme = Prefs.THEME_DARK;
            else theme = Prefs.THEME_SYSTEM;
            if (theme != prefs.getTheme()) {
                prefs.setTheme(theme);
                prefs.applyTheme();
            }
        });

        TextView aboutAuthor = view.findViewById(R.id.about_author);
        aboutAuthor.setText(getString(R.string.about_author_format,
                getString(R.string.author_name)));

        MaterialSwitch showPaid = view.findViewById(R.id.show_paid_switch);
        showPaid.setChecked(prefs.showPaid());
        showPaid.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.setShowPaid(isChecked));

        view.findViewById(R.id.clear_cache_button).setOnClickListener(v -> {
            CatalogRepository.get(requireContext()).clearCache();
            Toast.makeText(requireContext(), R.string.cache_cleared, Toast.LENGTH_SHORT).show();
        });

        TextView version = view.findViewById(R.id.about_version);
        try {
            PackageInfo info = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            version.setText(getString(R.string.version_format, info.versionName));
        } catch (Exception e) {
            version.setText(R.string.app_name);
        }
    }
}