package moscow.cuteslavicboy.gdzoffline.download;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.lifecycle.Observer;

import java.util.List;

import moscow.cuteslavicboy.gdzoffline.R;
import moscow.cuteslavicboy.gdzoffline.ui.MainActivity;

public class DownloadService extends Service {

    public static final String ACTION_START = "moscow.cuteslavicboy.gdzoffline.START";
    public static final String ACTION_CANCEL_ALL = "moscow.cuteslavicboy.gdzoffline.CANCEL_ALL";
    public static final String CHANNEL_ID = "downloads";

    private static final String TAG = "GdzService";
    private static final int NOTIF_ID = 1;

    private DownloadManager manager;
    private PowerManager.WakeLock wakeLock;
    private boolean foregroundStarted;

    private final Observer<List<BookJob>> observer = jobs -> {
        boolean anyActive = false;
        int done = 0;
        int total = 0;
        boolean paused = false;
        for (BookJob job : jobs) {
            if (job.isActive()) {
                anyActive = true;
                done += job.done.get() + job.failed.get();
                total += job.total();
                if (job.state == BookJob.PAUSED) paused = true;
            }
        }
        if (!anyActive) {
            if (foregroundStarted) {
                stopForegroundCompat();
            }
            stopSelf();
            return;
        }
        Notification notification = buildNotification(done, total, paused);
        try {
            ServiceCompat.startForeground(this, NOTIF_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            foregroundStarted = true;
        } catch (Exception e) {
            Log.w(TAG, "startForeground failed: " + e.getMessage());
            stopSelf();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        manager = DownloadManager.get(this);
        createChannel();
        // Must enter foreground before any stopSelf() path: the LiveData observer
        // below dispatches synchronously and may stop the service immediately.
        try {
            ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(0, 0, false),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            foregroundStarted = true;
        } catch (Exception e) {
            Log.w(TAG, "startForeground failed: " + e.getMessage());
            stopSelf();
            return;
        }
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "gdz:downloads");
            wakeLock.acquire();
        }
        manager.getJobsLive().observeForever(observer);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (!foregroundStarted) {
            try {
                ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(0, 0, false),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
                foregroundStarted = true;
            } catch (Exception e) {
                Log.w(TAG, "startForeground failed: " + e.getMessage());
                stopSelf();
                return START_NOT_STICKY;
            }
        }
        if (intent != null && ACTION_CANCEL_ALL.equals(intent.getAction())) {
            manager.cancelAll();
        }
        manager.ensureWorkers();
        return START_STICKY;
    }

    @Override
    public void onTimeout(int startId) {
        handleTimeout();
    }

    @Override
    public void onTimeout(int startId, int fgsType) {
        handleTimeout();
    }

    private void handleTimeout() {
        Log.w(TAG, "dataSync timeout reached, pausing queue");
        manager.pauseAllForTimeout();
        stopForegroundCompat();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        manager.getJobsLive().removeObserver(observer);
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void stopForegroundCompat() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
    }

    private void createChannel() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notif_channel_downloads),
                    NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(int done, int total, boolean paused) {
        Intent contentIntent = new Intent(this, MainActivity.class);
        contentIntent.putExtra(MainActivity.EXTRA_TAB, MainActivity.TAB_DOWNLOADS);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentPi = PendingIntent.getActivity(this, 0, contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent cancelIntent = new Intent(this, DownloadService.class);
        cancelIntent.setAction(ACTION_CANCEL_ALL);
        PendingIntent cancelPi = PendingIntent.getService(this, 1, cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String text;
        if (paused) {
            text = getString(R.string.notif_paused);
        } else if (total > 0) {
            int percent = (int) (done * 100L / total);
            text = getString(R.string.notif_progress_format, done, total, percent);
        } else {
            text = getString(R.string.loading);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_downloads)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(contentPi)
                .addAction(R.drawable.ic_cancel, getString(R.string.cancel_all), cancelPi)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        if (total > 0) {
            builder.setProgress(total, done, false);
        } else {
            builder.setProgress(0, 0, true);
        }
        return builder.build();
    }
}