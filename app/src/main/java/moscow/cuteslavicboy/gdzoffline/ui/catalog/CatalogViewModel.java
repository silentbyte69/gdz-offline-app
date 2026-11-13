package moscow.cuteslavicboy.gdzoffline.ui.catalog;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import moscow.cuteslavicboy.gdzoffline.GdzApp;
import moscow.cuteslavicboy.gdzoffline.model.Book;
import moscow.cuteslavicboy.gdzoffline.model.Catalog;
import moscow.cuteslavicboy.gdzoffline.model.IdTitle;
import moscow.cuteslavicboy.gdzoffline.repo.CatalogRepository;
import moscow.cuteslavicboy.gdzoffline.util.AppExecutors;

public class CatalogViewModel extends ViewModel {

    public static class SubjectRow {
        public final int id;
        public final String title;
        public final int count;

        SubjectRow(int id, String title, int count) {
            this.id = id;
            this.title = title;
            this.count = count;
        }
    }

    private final CatalogRepository repository = CatalogRepository.get(GdzApp.get());
    private final MutableLiveData<List<Book>> books = new MutableLiveData<>();
    private final MutableLiveData<List<SubjectRow>> subjects = new MutableLiveData<>();

    public LiveData<CatalogRepository.CatalogState> getCatalogState() {
        return repository.getState();
    }

    public LiveData<List<Book>> getBooks() {
        return books;
    }

    public LiveData<List<SubjectRow>> getSubjects() {
        return subjects;
    }

    public void loadCatalog(boolean forceRefresh) {
        repository.load(forceRefresh);
    }

    public void clearCache() {
        repository.clearCache();
    }

    public void filterSubjects(int classId, boolean includePaid) {
        Catalog catalog = repository.cachedCatalog();
        if (catalog == null) return;
        AppExecutors.get().networkIO().execute(() -> {
            List<SubjectRow> rows = new ArrayList<>();
            for (IdTitle subject : catalog.subjects) {
                int count = catalog.booksFor(classId, subject.id, includePaid).size();
                if (count > 0) {
                    rows.add(new SubjectRow(subject.id, subject.title, count));
                }
            }
            subjects.postValue(rows);
        });
    }

    public void filterBooks(int classId, int subjectId, String query, boolean includePaid) {
        Catalog catalog = repository.cachedCatalog();
        if (catalog == null) return;
        AppExecutors.get().networkIO().execute(() -> {
            List<Book> result;
            if (query != null && !query.trim().isEmpty()) {
                result = catalog.search(query, classId, includePaid);
            } else {
                result = catalog.booksFor(classId, subjectId, includePaid);
            }
            books.postValue(result);
        });
    }
}