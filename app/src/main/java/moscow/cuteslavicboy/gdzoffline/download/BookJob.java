package moscow.cuteslavicboy.gdzoffline.download;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BookJob {
    public static final int QUEUED = 0;
    public static final int RUNNING = 1;
    public static final int PAUSED = 2;
    public static final int DONE = 3;
    public static final int DONE_WITH_ERRORS = 4;
    public static final int CANCELLED = 5;

    public static final int PART_ALL = -1;
    public static final int PART_SINGLE = -2;

    public String bookId;
    public String bookUrl;
    public String title;
    public int partIndex;
    public String partTitle;
    public volatile int state = QUEUED;
    public long addedAt;
    public final List<TaskItem> tasks = new ArrayList<>();

    public final AtomicInteger done = new AtomicInteger();
    public final AtomicInteger failed = new AtomicInteger();

    public String key() {
        return key(bookId, partIndex);
    }

    public static String key(String bookId, int partIndex) {
        return bookId + ":" + partIndex;
    }

    public boolean isActive() {
        return state == QUEUED || state == RUNNING || state == PAUSED;
    }

    public boolean isFinished() {
        return state == DONE || state == DONE_WITH_ERRORS || state == CANCELLED;
    }

    public int total() {
        return tasks.size();
    }

    public int progressPercent() {
        int total = total();
        if (total == 0) return 0;
        return (int) ((done.get() + failed.get()) * 100L / total);
    }

    public void recount() {
        int d = 0;
        int f = 0;
        for (TaskItem t : tasks) {
            if (t.status == TaskItem.DONE) d++;
            else if (t.status == TaskItem.FAILED) f++;
        }
        done.set(d);
        failed.set(f);
    }

    /** Immutable-value snapshot for UI/DiffUtil (live jobs are mutable). */
    public BookJob copy() {
        BookJob c = new BookJob();
        c.bookId = bookId;
        c.bookUrl = bookUrl;
        c.title = title;
        c.partIndex = partIndex;
        c.partTitle = partTitle;
        c.state = state;
        c.addedAt = addedAt;
        c.tasks.addAll(tasks);
        c.done.set(done.get());
        c.failed.set(failed.get());
        return c;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("bookId", bookId);
        o.put("bookUrl", bookUrl);
        o.put("title", title);
        o.put("partIndex", partIndex);
        o.put("partTitle", partTitle);
        o.put("state", state);
        o.put("addedAt", addedAt);
        JSONArray arr = new JSONArray();
        for (TaskItem t : tasks) arr.put(t.toJson());
        o.put("tasks", arr);
        return o;
    }

    public static BookJob fromJson(JSONObject o) {
        BookJob job = new BookJob();
        job.bookId = o.optString("bookId");
        job.bookUrl = o.optString("bookUrl");
        job.title = o.optString("title");
        job.partIndex = o.optInt("partIndex", PART_ALL);
        job.partTitle = o.optString("partTitle");
        job.state = o.optInt("state", QUEUED);
        job.addedAt = o.optLong("addedAt");
        JSONArray arr = o.optJSONArray("tasks");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject t = arr.optJSONObject(i);
                if (t != null) job.tasks.add(TaskItem.fromJson(t));
            }
        }
        job.recount();
        return job;
    }
}