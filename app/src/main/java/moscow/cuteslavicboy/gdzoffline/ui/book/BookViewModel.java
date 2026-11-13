package moscow.cuteslavicboy.gdzoffline.ui.book;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import moscow.cuteslavicboy.gdzoffline.GdzApp;
import moscow.cuteslavicboy.gdzoffline.model.Book;
import moscow.cuteslavicboy.gdzoffline.model.BookNode;
import moscow.cuteslavicboy.gdzoffline.repo.BookRepository;

public class BookViewModel extends ViewModel {

    public static final int LOADING = 0;
    public static final int READY = 1;
    public static final int ERROR = 2;

    public static class BookState {
        public int status = LOADING;
        public Book book;
        public List<BookNode> parts;
        public boolean fromDisk;
        public String error;
    }

    private final MutableLiveData<BookState> state = new MutableLiveData<>();
    private final BookRepository repository = new BookRepository(GdzApp.get());
    private Book current;

    public LiveData<BookState> getState() {
        return state;
    }

    public void load(Book book, boolean forceRefresh) {
        current = book;
        BookState loading = new BookState();
        loading.status = LOADING;
        state.setValue(loading);
        repository.loadBook(book, forceRefresh, new BookRepository.Callback() {
            @Override
            public void onResult(BookRepository.BookResult result) {
                BookState s = new BookState();
                s.status = READY;
                s.book = result.book;
                s.parts = result.parts;
                s.fromDisk = result.fromDisk;
                state.postValue(s);
            }

            @Override
            public void onError(String message) {
                BookState s = new BookState();
                s.status = ERROR;
                s.error = message;
                state.postValue(s);
            }
        });
    }

    public void refresh() {
        if (current != null) load(current, true);
    }
}