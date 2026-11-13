package moscow.cuteslavicboy.gdzoffline.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class Prefs {

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    private static final String FILE = "settings";
    private static final String KEY_THEME = "theme";
    private static final String KEY_SHOW_PAID = "show_paid";
    private static final String KEY_VIEWER_HINT_SEEN = "viewer_swipe_hint_seen";

    private final SharedPreferences sp;

    public Prefs(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public int getTheme() {
        return sp.getInt(KEY_THEME, THEME_SYSTEM);
    }

    public void setTheme(int theme) {
        sp.edit().putInt(KEY_THEME, theme).apply();
    }

    public boolean showPaid() {
        return sp.getBoolean(KEY_SHOW_PAID, true);
    }

    public void setShowPaid(boolean show) {
        sp.edit().putBoolean(KEY_SHOW_PAID, show).apply();
    }

    public boolean isViewerHintSeen() {
        return sp.getBoolean(KEY_VIEWER_HINT_SEEN, false);
    }

    public void setViewerHintSeen() {
        sp.edit().putBoolean(KEY_VIEWER_HINT_SEEN, true).apply();
    }

    public void applyTheme() {
        switch (getTheme()) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}