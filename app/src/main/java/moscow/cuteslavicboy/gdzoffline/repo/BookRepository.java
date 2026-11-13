package moscow.cuteslavicboy.gdzoffline.repo;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

import moscow.cuteslavicboy.gdzoffline.api.ApiClient;
import moscow.cuteslavicboy.gdzoffline.model.Book;
import moscow.cuteslavicboy.gdzoffline.model.BookNode;
import moscow.cuteslavicboy.gdzoffline.storage.Storage;
import moscow.cuteslavicboy.gdzoffline.util.AppExecutors;

public class BookRepository {

    public static class BookResult {
        public Book book;
        public List<BookNode> parts;
        public boolean fromDisk;
    }

    public interface Callback {
        void onResult(BookResult result);

        void onError(String message);
    }

    private static final String TAG = "GdzBook";

    private final Storage storage;

    public BookRepository(Context context) {
        storage = Storage.get(context);
    }

    public void loadBook(Book catalogBook, boolean forceRefresh, Callback callback) {
        AppExecutors.get().networkIO().execute(() -> {
            try {
                if (!forceRefresh && storage.hasBook(catalogBook.id)) {
                    BookResult local = readLocal(catalogBook.id);
                    if (local != null) {
                        AppExecutors.get().main(() -> callback.onResult(local));
                        return;
                    }
                }
                String json = ApiClient.getJsonWithRetry(catalogBook.url);
                JSONObject obj = new JSONObject(json);
                if (!obj.optBoolean("success", false)) {
                    throw new Exception(obj.optString("message", "Ошибка API"));
                }
                Book book = Book.fromJson(obj.optJSONObject("book"));
                if (book.id == null || "null".equals(book.id)) {
                    book.id = catalogBook.id;
                }
                JSONArray rawStructure = obj.optJSONArray("structure");
                List<BookNode> parts = BookNode.listFromJson(rawStructure);

                storage.saveBookMeta(book);
                if (rawStructure != null) {
                    storage.saveStructure(book.id, rawStructure);
                }
                downloadCover(book);

                BookResult result = new BookResult();
                result.book = book;
                result.parts = parts;
                result.fromDisk = false;
                AppExecutors.get().main(() -> callback.onResult(result));
            } catch (Exception e) {
                Log.w(TAG, "Book load failed: " + e.getMessage());
                BookResult local = readLocal(catalogBook.id);
                if (local != null) {
                    AppExecutors.get().main(() -> callback.onResult(local));
                } else {
                    AppExecutors.get().main(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    private BookResult readLocal(String bookId) {
        Book meta = storage.readBookMeta(bookId);
        List<BookNode> parts = storage.readStructure(bookId);
        if (meta == null || parts == null) return null;
        BookResult result = new BookResult();
        result.book = meta;
        result.parts = parts;
        result.fromDisk = true;
        return result;
    }

    private void downloadCover(Book book) {
        if (book.coverUrl == null || book.coverUrl.isEmpty()) return;
        File dest = storage.coverFile(book.id);
        if (dest.isFile()) return;
        try {
            ApiClient.downloadToFileWithRetry(book.coverUrl, dest);
        } catch (Exception e) {
            Log.w(TAG, "Cover download failed (non-fatal): " + e.getMessage());
        }
    }
}