package moscow.cuteslavicboy.gdzoffline.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import moscow.cuteslavicboy.gdzoffline.R;
import moscow.cuteslavicboy.gdzoffline.download.DownloadManager;
import moscow.cuteslavicboy.gdzoffline.ui.catalog.CatalogFragment;
import moscow.cuteslavicboy.gdzoffline.ui.downloads.DownloadsFragment;
import moscow.cuteslavicboy.gdzoffline.ui.library.LibraryFragment;
import moscow.cuteslavicboy.gdzoffline.ui.settings.SettingsFragment;
import moscow.cuteslavicboy.gdzoffline.util.UiUtil;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_TAB = "tab";
    public static final int TAB_CATALOG = 0;
    public static final int TAB_DOWNLOADS = 1;
    public static final int TAB_LIBRARY = 2;
    public static final int TAB_SETTINGS = 3;

    private Fragment catalog;
    private Fragment downloads;
    private Fragment library;
    private Fragment settings;
    private Fragment active;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        UiUtil.applySystemBarInsets(findViewById(R.id.root));
        bottomNav = findViewById(R.id.bottom_nav);

        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState == null) {
            catalog = new CatalogFragment();
            downloads = new DownloadsFragment();
            library = new LibraryFragment();
            settings = new SettingsFragment();
            fm.beginTransaction()
                    .add(R.id.fragment_container, settings, "settings").hide(settings)
                    .add(R.id.fragment_container, library, "library").hide(library)
                    .add(R.id.fragment_container, downloads, "downloads").hide(downloads)
                    .add(R.id.fragment_container, catalog, "catalog")
                    .commit();
            active = catalog;
        } else {
            catalog = fm.findFragmentByTag("catalog");
            downloads = fm.findFragmentByTag("downloads");
            library = fm.findFragmentByTag("library");
            settings = fm.findFragmentByTag("settings");
            active = catalog;
            if (downloads != null && !downloads.isHidden()) active = downloads;
            else if (library != null && !library.isHidden()) active = library;
            else if (settings != null && !settings.isHidden()) active = settings;
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_catalog) switchTab(catalog, TAB_CATALOG);
            else if (id == R.id.nav_downloads) switchTab(downloads, TAB_DOWNLOADS);
            else if (id == R.id.nav_library) switchTab(library, TAB_LIBRARY);
            else if (id == R.id.nav_settings) switchTab(settings, TAB_SETTINGS);
            return true;
        });

        handleTabIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleTabIntent(intent);
    }

    private void handleTabIntent(Intent intent) {
        if (intent == null) return;
        int tab = intent.getIntExtra(EXTRA_TAB, TAB_CATALOG);
        if (tab == TAB_DOWNLOADS) {
            bottomNav.setSelectedItemId(R.id.nav_downloads);
        } else if (tab == TAB_LIBRARY) {
            bottomNav.setSelectedItemId(R.id.nav_library);
        } else if (tab == TAB_SETTINGS) {
            bottomNav.setSelectedItemId(R.id.nav_settings);
        }
    }

    private void switchTab(Fragment target, int tabIndex) {
        if (target == null || target == active) return;
        getSupportFragmentManager().beginTransaction()
                .hide(active)
                .show(target)
                .commitNow();
        active = target;
    }

    @Override
    protected void onResume() {
        super.onResume();
        DownloadManager.get(this).resumePending();
    }
}