package moscow.cuteslavicboy.gdzoffline.repo;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONObject;

import java.io.File;

import moscow.cuteslavicboy.gdzoffline.api.ApiClient;
import moscow.cuteslavicboy.gdzoffline.model.Catalog;
import moscow.cuteslavicboy.gdzoffline.util.AppExecutors;
import moscow.cuteslavicboy.gdzoffline.util.FilesUtil;

public class CatalogRepository {

    public static final int STATUS_LOADING = 0;
    public static final int STATUS_READY = 1;
    public static final int STATUS_ERROR = 2;

    private static final String TAG = "GdzCatalog";
    private static final long CACHE_TTL_MS = 24L * 60 * 60 * 1000;
    private static final String CATALOG_PATH = "/full-book-list?country_id=1";

    private static CatalogRepository instance;

    public static class CatalogState {
        public final int status;
        public final Catalog catalog;
        public final boolean fromCache;
        public final String error;

        CatalogState(int status, Catalog catalog, boolean fromCache, String error) {
            this.status = status;
            this.catalog = catalog;
            this.fromCache = fromCache;
            this.error = error;
        }
    }

    private final MutableLiveData<CatalogState> state = new MutableLiveData<>();
    private final File cacheFile;
    private volatile Catalog memory;
    private volatile boolean loading;

    private CatalogRepository(Context context) {
        File dir = new File(context.getFilesDir(), "catalog");
        if (!dir.exists()) dir.mkdirs();
        cacheFile = new File(dir, "full_book_list.json");
    }

    public static synchronized CatalogRepository get(Context context) {
        if (instance == null) {
            instance = new CatalogRepository(context.getApplicationContext());
        }
        return instance;
    }

    public LiveData<CatalogState> getState() {
        return state;
    }

    public Catalog cachedCatalog() {
        return memory;
    }

    public void load(boolean forceRefresh) {
        if (memory != null && !forceRefresh) {
            state.postValue(new CatalogState(STATUS_READY, memory, false, null));
            return;
        }
        synchronized (this) {
            if (loading) return;
            loading = true;
        }
        if (memory == null) {
            state.postValue(new CatalogState(STATUS_LOADING, null, false, null));
        }
        AppExecutors.get().networkIO().execute(() -> {
            try {
                Catalog catalog = null;
                boolean fromCache = false;
                if (!forceRefresh && cacheFresh()) {
                    catalog = parseCache();
                    fromCache = true;
                }
                if (catalog == null) {
                    try {
                        String json = ApiClient.getJsonWithRetry(CATALOG_PATH);
                        FilesUtil.atomicWrite(cacheFile, json);
                        catalog = Catalog.fromJson(new JSONObject(json));
                    } catch (Exception networkError) {
                        Log.w(TAG, "Network catalog load failed: " + networkError.getMessage());
                        catalog = parseCache();
                        fromCache = true;
                        if (catalog == null) {
                            throw networkError;
                        }
                    }
                }
                memory = catalog;
                state.postValue(new CatalogState(STATUS_READY, catalog, fromCache, null));
            } catch (Exception e) {
                Log.e(TAG, "Catalog load failed", e);
                state.postValue(new CatalogState(STATUS_ERROR, null, false, e.getMessage()));
            } finally {
                synchronized (CatalogRepository.this) {
                    loading = false;
                }
            }
        });
    }

    public void clearCache() {
        memory = null;
        AppExecutors.get().diskIO().execute(() -> {
            FilesUtil.deleteRecursive(cacheFile);
        });
    }

    private boolean cacheFresh() {
        return cacheFile.isFile()
                && System.currentTimeMillis() - cacheFile.lastModified() < CACHE_TTL_MS;
    }

    private Catalog parseCache() {
        try {
            if (!cacheFile.isFile()) return null;
            return Catalog.fromJson(new JSONObject(FilesUtil.readString(cacheFile)));
        } catch (Exception e) {
            Log.w(TAG, "Cache parse failed: " + e.getMessage());
            return null;
        }
    }
}