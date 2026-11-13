package moscow.cuteslavicboy.gdzoffline.repo;

import android.content.Context;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

import moscow.cuteslavicboy.gdzoffline.api.ApiClient;
import moscow.cuteslavicboy.gdzoffline.model.Edition;
import moscow.cuteslavicboy.gdzoffline.model.TaskInfo;
import moscow.cuteslavicboy.gdzoffline.model.TaskRef;
import moscow.cuteslavicboy.gdzoffline.storage.Storage;
import moscow.cuteslavicboy.gdzoffline.util.AppExecutors;
import moscow.cuteslavicboy.gdzoffline.util.FilesUtil;

/**
 * Local-first task fetch/store path shared by the viewer and download workers.
 * task.json is written LAST and acts as the completeness marker.
 */
public class TaskRepository {

    public static class TaskResult {
        public TaskInfo info;
        public List<List<File>> editionFiles;
        public boolean fromDisk;
        public String error;
        public boolean paid;

        static TaskResult ok(TaskInfo info, List<List<File>> files, boolean fromDisk) {
            TaskResult r = new TaskResult();
            r.info = info;
            r.editionFiles = files;
            r.fromDisk = fromDisk;
            return r;
        }

        static TaskResult fail(String error, boolean paid) {
            TaskResult r = new TaskResult();
            r.error = error;
            r.paid = paid;
            return r;
        }
    }

    public interface Callback {
        void onResult(TaskResult result);

        void onError(String message);
    }

    private final Storage storage;

    public TaskRepository(Context context) {
        storage = Storage.get(context);
    }

    /** Synchronous; call from a background thread. Throws IOException on network failure. */
    public TaskResult loadTask(String bookId, TaskRef ref) throws IOException {
        File taskJson = storage.taskJsonFile(bookId, ref.url);
        if (FilesUtil.isNonEmptyFile(taskJson)) {
            try {
                TaskInfo local = TaskInfo.fromJson(new JSONObject(FilesUtil.readString(taskJson)));
                if (local.success && storage.isTaskComplete(bookId, ref.url, local)) {
                    return TaskResult.ok(local, storage.localEditionFiles(bookId, ref.url, local), true);
                }
            } catch (Exception ignored) {
                // corrupted local copy -> refetch
            }
        }

        String json = ApiClient.getJsonWithRetry(ref.url);
        JSONObject obj;
        try {
            obj = new JSONObject(json);
        } catch (Exception e) {
            throw new IOException("Bad JSON for " + ref.url);
        }
        if (!obj.optBoolean("success", false)) {
            String message = obj.optString("message", "Ошибка API");
            return TaskResult.fail(message, message.contains("Токен"));
        }
        TaskInfo info = TaskInfo.fromJson(obj);

        File dir = storage.taskDir(bookId, ref.url);
        for (Edition e : info.editions) {
            if (!e.hasImages()) continue;
            for (int i = 0; i < e.images.size(); i++) {
                File dest = storage.imageFile(dir, e.index, i);
                ApiClient.downloadToFileWithRetry(e.images.get(i).url, dest);
            }
        }
        FilesUtil.atomicWrite(taskJson, json);
        return TaskResult.ok(info, storage.localEditionFiles(bookId, ref.url, info), false);
    }

    public void loadTaskAsync(String bookId, TaskRef ref, Callback callback) {
        AppExecutors.get().networkIO().execute(() -> {
            try {
                TaskResult result = loadTask(bookId, ref);
                AppExecutors.get().main(() -> callback.onResult(result));
            } catch (Exception e) {
                AppExecutors.get().main(() -> callback.onError(e.getMessage()));
            }
        });
    }
}