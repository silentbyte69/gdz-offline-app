package moscow.cuteslavicboy.gdzoffline.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppExecutors {

    private static final AppExecutors INSTANCE = new AppExecutors();

    private final ExecutorService diskIO = Executors.newSingleThreadExecutor();
    private final ExecutorService networkIO = Executors.newFixedThreadPool(2);
    private final Handler mainThread = new Handler(Looper.getMainLooper());

    public static AppExecutors get() {
        return INSTANCE;
    }

    public ExecutorService diskIO() {
        return diskIO;
    }

    public ExecutorService networkIO() {
        return networkIO;
    }

    public void main(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            mainThread.post(r);
        }
    }

    public void mainDelayed(Runnable r, long delayMs) {
        mainThread.postDelayed(r, delayMs);
    }

    public void removeMainCallbacks(Runnable r) {
        mainThread.removeCallbacks(r);
    }
}