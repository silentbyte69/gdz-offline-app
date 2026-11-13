package moscow.cuteslavicboy.gdzoffline;

import android.app.Application;

import moscow.cuteslavicboy.gdzoffline.download.DownloadManager;
import moscow.cuteslavicboy.gdzoffline.repo.CatalogRepository;
import moscow.cuteslavicboy.gdzoffline.storage.Storage;
import moscow.cuteslavicboy.gdzoffline.util.Prefs;

public class GdzApp extends Application {

    private static GdzApp instance;
    private Prefs prefs;

    public static GdzApp get() {
        return instance;
    }

    public static Prefs prefs() {
        return instance.prefs;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        prefs = new Prefs(this);
        prefs.applyTheme();
        Storage.get(this);
        CatalogRepository.get(this);
        DownloadManager.get(this);
    }
}