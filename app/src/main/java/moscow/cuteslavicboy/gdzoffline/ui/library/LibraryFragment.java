package moscow.cuteslavicboy.gdzoffline.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import moscow.cuteslavicboy.gdzoffline.R;
import moscow.cuteslavicboy.gdzoffline.download.BookJob;
import moscow.cuteslavicboy.gdzoffline.download.DownloadManager;
import moscow.cuteslavicboy.gdzoffline.storage.LibraryEntry;
import moscow.cuteslavicboy.gdzoffline.storage.Storage;
import moscow.cuteslavicboy.gdzoffline.ui.book.BookActivity;
import moscow.cuteslavicboy.gdzoffline.util.AppExecutors;
import moscow.cuteslavicboy.gdzoffline.util.UiUtil;

public class LibraryFragment extends Fragment {

    private Storage storage;
    private DownloadManager manager;
    private LibraryAdapter adapter;
    private RecyclerView recycler;
    private View emptyState;
    private TextView usage;
    private SwipeRefreshLayout swipeRefresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        storage = Storage.get(requireContext());
        manager = DownloadManager.get(requireContext());
        recycler = view.findViewById(R.id.recycler);
        emptyState = view.findViewById(R.id.empty_state);
        usage = view.findViewById(R.id.storage_usage);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new LibraryAdapter(new LibraryAdapter.Listener() {
            @Override
            public void onOpen(LibraryEntry entry) {
                BookActivity.start(requireContext(), entry.book);
            }

            @Override
            public void onDelete(LibraryEntry entry) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setMessage(getString(R.string.delete_book_confirm, entry.book.title))
                        .setPositiveButton(R.string.delete, (d, w) -> deleteBook(entry))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });
        recycler.setAdapter(adapter);
        swipeRefresh.setOnRefreshListener(this::loadLibrary);
        loadLibrary();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLibrary();
    }

    private void loadLibrary() {
        AppExecutors.get().diskIO().execute(() -> {
            List<LibraryEntry> entries = storage.listLibraryBooks();
            long total = storage.totalSize();
            AppExecutors.get().main(() -> {
                if (getView() == null) return;
                swipeRefresh.setRefreshing(false);
                adapter.submitList(entries);
                boolean empty = entries.isEmpty();
                recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
                emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                usage.setText(getString(R.string.storage_usage_format, UiUtil.formatSize(total)));
            });
        });
    }

    private void deleteBook(LibraryEntry entry) {
        for (BookJob job : manager.snapshot()) {
            if (job.bookId.equals(entry.book.id) && job.isActive()) {
                manager.cancel(job.key());
            }
        }
        AppExecutors.get().diskIO().execute(() -> {
            storage.deleteBook(entry.book.id);
            AppExecutors.get().main(() -> {
                Toast.makeText(requireContext(), R.string.book_deleted, Toast.LENGTH_SHORT).show();
                loadLibrary();
            });
        });
    }
}