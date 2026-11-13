package moscow.cuteslavicboy.gdzoffline.storage;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import moscow.cuteslavicboy.gdzoffline.model.Book;
import moscow.cuteslavicboy.gdzoffline.model.BookNode;
import moscow.cuteslavicboy.gdzoffline.model.Edition;
import moscow.cuteslavicboy.gdzoffline.model.TaskInfo;
import moscow.cuteslavicboy.gdzoffline.util.FilesUtil;

/**
 * Durable content storage: getExternalFilesDir(null)/GDZ (not cache).
 * Layout: GDZ/<bookId>/{meta.json, structure.json, cover.jpg, tasks/<taskKey>/{task.json, <ed>_<img>.jpg}}
 */
public class Storage {

    private static Storage instance;

    private final File root;

    private Storage(Context context) {
        File external = context.getExternalFilesDir(null);
        File base = external != null ? external : context.getFilesDir();
        root = new File(base, "GDZ");
        if (!root.exists()) root.mkdirs();
    }

    public static synchronized Storage get(Context context) {
        if (instance == null) {
            instance = new Storage(context.getApplicationContext());
        }
        return instance;
    }

    public File root() {
        return root;
    }

    public File bookDir(String bookId) {
        return new File(root, FilesUtil.sanitizeKey(bookId));
    }

    public File metaFile(String bookId) {
        return new File(bookDir(bookId), "meta.json");
    }

    public File structureFile(String bookId) {
        return new File(bookDir(bookId), "structure.json");
    }

    public File coverFile(String bookId) {
        return new File(bookDir(bookId), "cover.jpg");
    }

    public File tasksDir(String bookId) {
        return new File(bookDir(bookId), "tasks");
    }

    public File taskDir(String bookId, String taskUrl) {
        return new File(tasksDir(bookId), FilesUtil.sanitizeKey(taskUrl));
    }

    public File taskJsonFile(String bookId, String taskUrl) {
        return new File(taskDir(bookId, taskUrl), "task.json");
    }

    public File imageFile(File taskDir, int editionIndex, int imageIndex) {
        return new File(taskDir, editionIndex + "_" + imageIndex + ".jpg");
    }

    public void saveBookMeta(Book book) throws Exception {
        FilesUtil.atomicWrite(metaFile(book.id), book.toJson().toString());
    }

    public Book readBookMeta(String bookId) {
        try {
            File f = metaFile(bookId);
            if (!f.isFile()) return null;
            return Book.fromJson(new JSONObject(FilesUtil.readString(f)));
        } catch (Exception e) {
            return null;
        }
    }

    public void saveStructure(String bookId, JSONArray rawStructure) throws Exception {
        FilesUtil.atomicWrite(structureFile(bookId), rawStructure.toString());
    }

    public List<BookNode> readStructure(String bookId) {
        try {
            File f = structureFile(bookId);
            if (!f.isFile()) return null;
            return BookNode.listFromJson(new JSONArray(FilesUtil.readString(f)));
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasBook(String bookId) {
        return metaFile(bookId).isFile() && structureFile(bookId).isFile();
    }

    /** task.json is written last, so its presence + all image files = complete task. */
    public boolean isTaskComplete(String bookId, String taskUrl, TaskInfo info) {
        File json = taskJsonFile(bookId, taskUrl);
        if (!FilesUtil.isNonEmptyFile(json)) return false;
        File dir = taskDir(bookId, taskUrl);
        for (Edition e : info.editions) {
            if (!e.hasImages()) continue;
            for (int i = 0; i < e.images.size(); i++) {
                if (!FilesUtil.isNonEmptyFile(imageFile(dir, e.index, i))) return false;
            }
        }
        return true;
    }

    /** Cheap set of task keys that have a directory (for UI checkmarks). */
    public Set<String> downloadedTaskKeys(String bookId) {
        Set<String> keys = new HashSet<>();
        File[] dirs = tasksDir(bookId).listFiles();
        if (dirs != null) {
            for (File d : dirs) {
                if (d.isDirectory()) keys.add(d.getName());
            }
        }
        return keys;
    }

    public int downloadedTaskCount(String bookId) {
        File[] dirs = tasksDir(bookId).listFiles();
        if (dirs == null) return 0;
        int count = 0;
        for (File d : dirs) {
            if (d.isDirectory() && new File(d, "task.json").isFile()) count++;
        }
        return count;
    }

    public List<LibraryEntry> listLibraryBooks() {
        List<LibraryEntry> result = new ArrayList<>();
        File[] dirs = root.listFiles();
        if (dirs == null) return result;
        for (File d : dirs) {
            if (!d.isDirectory()) continue;
            File meta = new File(d, "meta.json");
            if (!meta.isFile()) continue;
            try {
                Book book = Book.fromJson(new JSONObject(FilesUtil.readString(meta)));
                LibraryEntry entry = new LibraryEntry();
                entry.book = book;
                entry.downloadedTasks = downloadedTaskCount(book.id);
                entry.sizeBytes = FilesUtil.dirSize(d);
                result.add(entry);
            } catch (Exception ignored) {
            }
        }
        result.sort((a, b) -> a.book.title.compareToIgnoreCase(b.book.title));
        return result;
    }

    public long totalSize() {
        return FilesUtil.dirSize(root);
    }

    public void deleteBook(String bookId) {
        FilesUtil.deleteRecursive(bookDir(bookId));
    }

    /** Local image files for a task, grouped per image-edition (parallel to imageEditions()). */
    public List<List<File>> localEditionFiles(String bookId, String taskUrl, TaskInfo info) {
        File dir = taskDir(bookId, taskUrl);
        List<List<File>> result = new ArrayList<>();
        for (Edition e : info.imageEditions()) {
            List<File> files = new ArrayList<>();
            for (int i = 0; i < e.images.size(); i++) {
                files.add(imageFile(dir, e.index, i));
            }
            result.add(files);
        }
        return result;
    }
}