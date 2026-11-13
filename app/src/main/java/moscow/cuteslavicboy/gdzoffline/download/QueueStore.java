package moscow.cuteslavicboy.gdzoffline.download;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import moscow.cuteslavicboy.gdzoffline.util.FilesUtil;

/** Persists the download queue to filesDir/downloads.json (atomic writes). */
public class QueueStore {

    private static final String TAG = "GdzQueue";
    private static final int MAX_FINISHED_JOBS = 50;

    private final File file;

    public QueueStore(Context context) {
        file = new File(context.getFilesDir(), "downloads.json");
    }

    public synchronized List<BookJob> load() {
        List<BookJob> jobs = new ArrayList<>();
        if (!file.isFile()) return jobs;
        try {
            JSONObject root = new JSONObject(FilesUtil.readString(file));
            JSONArray arr = root.optJSONArray("jobs");
            if (arr == null) return jobs;
            List<BookJob> finished = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                BookJob job = BookJob.fromJson(o);
                for (TaskItem t : job.tasks) {
                    if (t.status == TaskItem.IN_PROGRESS) t.status = TaskItem.PENDING;
                }
                if (job.state == BookJob.RUNNING || job.state == BookJob.QUEUED
                        || job.state == BookJob.PAUSED) {
                    job.state = BookJob.QUEUED;
                    jobs.add(job);
                } else {
                    finished.add(job);
                }
            }
            int from = Math.max(0, finished.size() - MAX_FINISHED_JOBS);
            jobs.addAll(finished.subList(from, finished.size()));
        } catch (Exception e) {
            Log.w(TAG, "Queue load failed: " + e.getMessage());
        }
        return jobs;
    }

    public synchronized void save(List<BookJob> jobs) {
        try {
            JSONObject root = new JSONObject();
            root.put("version", 1);
            JSONArray arr = new JSONArray();
            for (BookJob job : jobs) arr.put(job.toJson());
            root.put("jobs", arr);
            FilesUtil.atomicWrite(file, root.toString());
        } catch (Exception e) {
            Log.w(TAG, "Queue save failed: " + e.getMessage());
        }
    }
}