package moscow.cuteslavicboy.gdzoffline.ui.catalog;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import moscow.cuteslavicboy.gdzoffline.GdzApp;
import moscow.cuteslavicboy.gdzoffline.R;
import moscow.cuteslavicboy.gdzoffline.model.Book;
import moscow.cuteslavicboy.gdzoffline.model.IdTitle;
import moscow.cuteslavicboy.gdzoffline.repo.CatalogRepository;
import moscow.cuteslavicboy.gdzoffline.ui.book.BookActivity;
import moscow.cuteslavicboy.gdzoffline.util.AppExecutors;

public class CatalogFragment extends Fragment {

    private static final int LEVEL_SUBJECTS = 0;
    private static final int LEVEL_BOOKS = 1;
    private static final long SEARCH_DEBOUNCE_MS = 300;

    private CatalogViewModel viewModel;
    private RecyclerView recycler;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progress;
    private TextView breadcrumb;
    private TextView offlineBanner;
    private View errorState;
    private View emptyState;
    private TextInputEditText searchInput;
    private AutoCompleteTextView gradeDropdown;

    private SubjectAdapter subjectAdapter;
    private BookAdapter bookAdapter;

    private int level = LEVEL_SUBJECTS;
    private int gradeId = 0;
    private int subjectId = 0;
    private String subjectTitle = "";
    private String query = "";
    private boolean gradeAdapterReady;

    private final Runnable searchRunnable = this::refreshCurrent;
    private final OnBackPressedCallback backCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            if (!query.isEmpty()) {
                searchInput.setText("");
            } else if (level == LEVEL_BOOKS) {
                level = LEVEL_SUBJECTS;
                subjectId = 0;
                subjectTitle = "";
                refreshCurrent();
            }
            updateBackCallback();
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_catalog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CatalogViewModel.class);

        recycler = view.findViewById(R.id.recycler);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        progress = view.findViewById(R.id.progress);
        breadcrumb = view.findViewById(R.id.breadcrumb);
        offlineBanner = view.findViewById(R.id.offline_banner);
        errorState = view.findViewById(R.id.error_state);
        emptyState = view.findViewById(R.id.empty_state);
        searchInput = view.findViewById(R.id.search_input);
        gradeDropdown = view.findViewById(R.id.grade_dropdown);

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        subjectAdapter = new SubjectAdapter(row -> {
            level = LEVEL_BOOKS;
            subjectId = row.id;
            subjectTitle = row.title;
            refreshCurrent();
            updateBackCallback();
        });
        bookAdapter = new BookAdapter(book ->
                BookActivity.start(requireContext(), book));
        recycler.setAdapter(subjectAdapter);

        swipeRefresh.setOnRefreshListener(() -> viewModel.loadCatalog(true));

        view.findViewById(R.id.error_retry).setOnClickListener(v -> viewModel.loadCatalog(true));

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = s.toString().trim();
                AppExecutors.get().removeMainCallbacks(searchRunnable);
                AppExecutors.get().mainDelayed(searchRunnable, SEARCH_DEBOUNCE_MS);
                updateBackCallback();
            }
        });

        gradeDropdown.setOnItemClickListener((parent, v, position, id) -> {
            gradeId = position == 0 ? 0 : position;
            refreshCurrent();
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(), backCallback);

        viewModel.getCatalogState().observe(getViewLifecycleOwner(), this::onCatalogState);
        viewModel.getSubjects().observe(getViewLifecycleOwner(), rows -> {
            if (level == LEVEL_SUBJECTS && query.isEmpty()) {
                subjectAdapter.submit(rows);
                showList(!rows.isEmpty());
            }
        });
        viewModel.getBooks().observe(getViewLifecycleOwner(), books -> {
            if (level == LEVEL_BOOKS || !query.isEmpty()) {
                bookAdapter.submitList(books);
                showList(!books.isEmpty());
            }
        });

        viewModel.loadCatalog(false);
    }

    private void onCatalogState(CatalogRepository.CatalogState state) {
        swipeRefresh.setRefreshing(false);
        switch (state.status) {
            case CatalogRepository.STATUS_LOADING:
                progress.setVisibility(View.VISIBLE);
                errorState.setVisibility(View.GONE);
                break;
            case CatalogRepository.STATUS_READY:
                progress.setVisibility(View.GONE);
                errorState.setVisibility(View.GONE);
                offlineBanner.setVisibility(state.fromCache ? View.VISIBLE : View.GONE);
                setupGradeAdapter(state.catalog.classes);
                refreshCurrent();
                break;
            case CatalogRepository.STATUS_ERROR:
                progress.setVisibility(View.GONE);
                recycler.setVisibility(View.GONE);
                emptyState.setVisibility(View.GONE);
                errorState.setVisibility(View.VISIBLE);
                ((TextView) errorState.findViewById(R.id.error_message))
                        .setText(R.string.error_load);
                break;
        }
    }

    private void setupGradeAdapter(List<IdTitle> classes) {
        if (gradeAdapterReady) return;
        gradeAdapterReady = true;
        List<String> items = new ArrayList<>();
        items.add(getString(R.string.all_grades));
        for (IdTitle c : classes) {
            items.add(getString(R.string.grade_format, c.id));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, items);
        gradeDropdown.setAdapter(adapter);
        gradeDropdown.setText(items.get(gradeId == 0 ? 0 : gradeId), false);
    }

    private void refreshCurrent() {
        if (viewModel.getCatalogState().getValue() == null
                || viewModel.getCatalogState().getValue().status != CatalogRepository.STATUS_READY) {
            return;
        }
        boolean includePaid = GdzApp.prefs().showPaid();
        if (!query.isEmpty()) {
            recycler.setAdapter(bookAdapter);
            breadcrumb.setText(R.string.catalog_search_results);
            viewModel.filterBooks(gradeId, 0, query, includePaid);
        } else if (level == LEVEL_SUBJECTS) {
            recycler.setAdapter(subjectAdapter);
            breadcrumb.setText(R.string.subjects_title);
            viewModel.filterSubjects(gradeId, includePaid);
        } else {
            recycler.setAdapter(bookAdapter);
            String crumb = subjectTitle;
            if (gradeId > 0) {
                crumb = getString(R.string.grade_format, gradeId) + " • " + subjectTitle;
            }
            breadcrumb.setText(crumb);
            viewModel.filterBooks(gradeId, subjectId, null, includePaid);
        }
    }

    private void showList(boolean hasItems) {
        progress.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
        recycler.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(hasItems ? View.GONE : View.VISIBLE);
    }

    private void updateBackCallback() {
        backCallback.setEnabled(!query.isEmpty() || level == LEVEL_BOOKS);
    }

    @Override
    public void onDestroyView() {
        AppExecutors.get().removeMainCallbacks(searchRunnable);
        super.onDestroyView();
    }
}