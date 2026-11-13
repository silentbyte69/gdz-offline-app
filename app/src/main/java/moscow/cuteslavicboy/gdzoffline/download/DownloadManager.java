package moscow.cuteslavicboy.gdzoffline.download;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import moscow.cuteslavicboy.gdzoffline.model.Book;
import moscow.cuteslavicboy.gdzoffline.model.TaskRef;
import moscow.cuteslavicboy.gdzoffline.repo.TaskRepository;

public class DownloadManager {

    private static final String TAG = "GdzDownload";
    private static final int WORKER_COUNT = 4;
    private static final long WORKER_DELAY_MS = 200;
    private static final long NOTIFY_INTERVAL_MS = 500;
    private static final long PERSIST_INTERVAL_MS = 5000;

    private static DownloadManager instance;

    private final Context context;
    private final QueueStore queueStore;
    private final TaskRepository taskRepository;

    private final LinkedHashMap<String, BookJob> jobs = new LinkedHashMap<>();
    private final MutableLiveData<List<BookJob>> jobsLive = new MutableLiveData<>();

    private final ExecutorService workers = Executors.newFixedThreadPool(WORKER_COUNT);
    private final AtomicInteger runningWorkers = new AtomicInteger();
    private final ScheduledExecutorService persistScheduler = Executors.newSingleThreadScheduledExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Object lock = new Object();
    private volatile boolean networkPaused;
    private volatile boolean dirty;
    private boolean notifyPending;

    public static synchronized DownloadManager get(Context context) {
        if (instance == null) {
            instance = new DownloadManager(context.getApplicationContext());
        }
        return instance;
    }

    private DownloadManager(Context context) {
        this.context = context;
        this.queueStore = new QueueStore(context);
        this.taskRepository = new TaskRepository(context);
        for (BookJob job : queueStore.load()) {
            jobs.put(job.key(), job);
        }
        postSnapshot();
        registerNetworkCallback();
        persistScheduler.scheduleWithFixedDelay(() -> {
            if (dirty) persistNow();
        }, PERSIST_INTERVAL_MS, PERSIST_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public LiveData<List<BookJob>> getJobsLive() {
        return jobsLive;
    }

    public List<BookJob> snapshot() {
        synchronized (lock) {
            List<BookJob> list = new ArrayList<>(jobs.size());
            for (BookJob job : jobs.values()) {
                list.add(job.copy());
            }
            Collections.reverse(list);
            return list;
        }
    }

    /** Returns false if an active job for the same book+part already exists. */
    public boolean enqueue(Book book, int partIndex, String partTitle, List<TaskRef> taskRefs) {
        if (taskRefs == null || taskRefs.isEmpty()) return false;
        String key = BookJob.key(book.id, partIndex);
        synchronized (lock) {
            BookJob existing = jobs.get(key);
            if (existing != null && existing.isActive()) return false;
            BookJob job = new BookJob();
            job.bookId = book.id;
            job.bookUrl = book.url;
            job.title = book.title;
            job.partIndex = partIndex;
            job.partTitle = partTitle;
            job.addedAt = System.currentTimeMillis();
            for (TaskRef ref : taskRefs) {
                job.tasks.add(new TaskItem(ref.url));
            }
            jobs.remove(key);
            jobs.put(key, job);
        }
        persistNow();
        notifyJobs();
        startWorkers();
        startService();
        return true;
    }

    public void cancel(String key) {
        synchronized (lock) {
            BookJob job = jobs.get(key);
            if (job != null && job.isActive()) {
                job.state = BookJob.CANCELLED;
            }
        }
        persistNow();
        notifyJobs();
    }

    public void cancelAll() {
        synchronized (lock) {
            for (BookJob job : jobs.values()) {
                if (job.isActive()) job.state = BookJob.CANCELLED;
            }
        }
        persistNow();
        notifyJobs();
    }

    public void retryFailed(String key) {
        synchronized (lock) {
            BookJob job = jobs.get(key);
            if (job == null) return;
            for (TaskItem t : job.tasks) {
                if (t.status == TaskItem.FAILED) t.status = TaskItem.PENDING;
            }
            job.recount();
            if (job.done.get() + job.failed.get() < job.total()) {
                job.state = BookJob.QUEUED;
            }
        }
        persistNow();
        notifyJobs();
        startWorkers();
        startService();
    }

    public void clearFinished() {
        synchronized (lock) {
            jobs.values().removeIf(BookJob::isFinished);
        }
        persistNow();
        notifyJobs();
    }

    public boolean hasPendingJobs() {
        synchronized (lock) {
            for (BookJob job : jobs.values()) {
                if (job.isActive()) return true;
            }
        }
        return false;
    }

    public BookJob activeJobForBook(String bookId) {
        synchronized (lock) {
            for (BookJob job : jobs.values()) {
                if (job.bookId.equals(bookId) && job.isActive()) return job;
            }
        }
        return null;
    }

    /** Called by the service when the system dataSync budget is exhausted. */
    public void pauseAllForTimeout() {
        synchronized (lock) {
            for (BookJob job : jobs.values()) {
                if (job.isActive()) job.state = BookJob.PAUSED;
            }
        }
        persistNow();
        notifyJobs();
    }

    public void startService() {
        try {
            Intent intent = new Intent(context, DownloadService.class);
            intent.setAction(DownloadService.ACTION_START);
            context.startForegroundService(intent);
        } catch (Exception e) {
            Log.w(TAG, "Cannot start download service: " + e.getMessage());
        }
    }

    /** Starts workers for persisted jobs after process/service restart. */
    public void ensureWorkers() {
        startWorkers();
    }

    public void resumePending() {
        startWorkers();
        if (hasPendingJobs()) {
            startService();
        }
    }

    private void startWorkers() {
        synchronized (lock) {
            while (runningWorkers.get() < WORKER_COUNT && hasPendingLocked()) {
                runningWorkers.incrementAndGet();
                workers.execute(this::workerLoop);
            }
        }
    }

    private boolean hasPendingLocked() {
        for (BookJob job : jobs.values()) {
            if (job.state != BookJob.QUEUED && job.state != BookJob.RUNNING) continue;
            for (TaskItem t : job.tasks) {
                if (t.status == TaskItem.PENDING) return true;
            }
        }
        return false;
    }

    private void workerLoop() {
        try {
            while (true) {
                if (networkPaused) {
                    Thread.sleep(1000);
                    synchronized (lock) {
                        if (!hasPendingLocked()) break;
                    }
                    continue;
                }
                BookJob job = null;
                TaskItem item = null;
                synchronized (lock) {
                    for (BookJob j : jobs.values()) {
                        if (j.state != BookJob.QUEUED && j.state != BookJob.RUNNING) continue;
                        for (TaskItem t : j.tasks) {
                            if (t.status == TaskItem.PENDING) {
                                job = j;
                                item = t;
                                item.status = TaskItem.IN_PROGRESS;
                                break;
                            }
                        }
                        if (item != null) break;
                    }
                    if (item == null) break;
                    job.state = BookJob.RUNNING;
                }
                processTask(job, item);
                Thread.sleep(WORKER_DELAY_MS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            runningWorkers.decrementAndGet();
            persistNow();
            notifyJobs();
        }
    }

    private void processTask(BookJob job, TaskItem item) {
        TaskRef ref = new TaskRef();
        ref.url = item.url;
        try {
            TaskRepository.TaskResult result = taskRepository.loadTask(job.bookId, ref);
            item.status = result.error == null ? TaskItem.DONE : TaskItem.FAILED;
            if (result.error != null) {
                Log.w(TAG, "Task failed " + item.url + ": " + result.error);
            }
        } catch (Exception e) {
            if (networkPaused) {
                // Network dropped mid-task: keep it pending for auto-resume.
                item.status = TaskItem.PENDING;
                return;
            }
            item.status = TaskItem.FAILED;
            Log.w(TAG, "Task error " + item.url + ": " + e.getMessage());
        }
        if (item.status == TaskItem.DONE) job.done.incrementAndGet();
        else job.failed.incrementAndGet();

        boolean finished = false;
        synchronized (lock) {
            if (job.isActive() && job.done.get() + job.failed.get() >= job.total()) {
                job.state = job.failed.get() > 0 ? BookJob.DONE_WITH_ERRORS : BookJob.DONE;
                finished = true;
            }
        }
        dirty = true;
        if (finished) persistNow();
        notifyJobs();
    }

    private void registerNetworkCallback() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;
        try {
            cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    if (networkPaused) {
                        networkPaused = false;
                        synchronized (lock) {
                            for (BookJob job : jobs.values()) {
                                if (job.state == BookJob.PAUSED) job.state = BookJob.QUEUED;
                            }
                        }
                        notifyJobs();
                        startWorkers();
                    }
                }

                @Override
                public void onLost(@NonNull Network network) {
                    networkPaused = true;
                    synchronized (lock) {
                        for (BookJob job : jobs.values()) {
                            if (job.state == BookJob.RUNNING || job.state == BookJob.QUEUED) {
                                job.state = BookJob.PAUSED;
                            }
                        }
                    }
                    persistNow();
                    notifyJobs();
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "Network callback registration failed: " + e.getMessage());
        }
    }

    private void notifyJobs() {
        mainHandler.post(() -> {
            synchronized (DownloadManager.this) {
                if (notifyPending) return;
                notifyPending = true;
            }
            mainHandler.postDelayed(() -> {
                synchronized (DownloadManager.this) {
                    notifyPending = false;
                }
                postSnapshot();
            }, NOTIFY_INTERVAL_MS);
        });
    }

    private void postSnapshot() {
        jobsLive.postValue(snapshot());
    }

    private void persistNow() {
        dirty = false;
        queueStore.save(snapshot());
    }
}