package moscow.cuteslavicboy.gdzoffline.ui.book;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import moscow.cuteslavicboy.gdzoffline.R;
import moscow.cuteslavicboy.gdzoffline.download.BookJob;
import moscow.cuteslavicboy.gdzoffline.download.DownloadManager;
import moscow.cuteslavicboy.gdzoffline.model.Book;
import moscow.cuteslavicboy.gdzoffline.model.BookNode;
import moscow.cuteslavicboy.gdzoffline.model.TaskRef;
import moscow.cuteslavicboy.gdzoffline.storage.Storage;
import moscow.cuteslavicboy.gdzoffline.ui.viewer.TaskActivity;
import moscow.cuteslavicboy.gdzoffline.util.AppExecutors;
import moscow.cuteslavicboy.gdzoffline.util.UiUtil;

public class BookActivity extends AppCompatActivity {

    private static final String EXTRA_BOOK_JSON = "book_json";

    private Book book;
    private BookViewModel viewModel;
    private DownloadManager downloadManager;
    private Storage storage;

    private ImageView cover;
    private TextView title;
    private TextView authors;
    private TextView meta;
    private TextView paidBadge;
    private TextView paidExplanation;
    private ChipGroup partsGroup;
    private View partsScroll;
    private Button downloadButton;
    private View progressSection;
    private ProgressBar jobProgress;
    private TextView jobProgressText;
    private Button cancelButton;
    private RecyclerView structureList;
    private View loadingView;
    private View errorView;

    private StructureAdapter structureAdapter;
    private List<BookNode> parts = new ArrayList<>();
    private int selectedPart = 0;
    private Runnable pendingDownload;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Toast.makeText(this, R.string.notification_permission_rationale,
                            Toast.LENGTH_LONG).show();
                }
                if (pendingDownload != null) {
                    Runnable r = pendingDownload;
                    pendingDownload = null;
                    r.run();
                }
            });

    public static void start(Context context, Book book) {
        try {
            Intent intent = new Intent(context, BookActivity.class);
            intent.putExtra(EXTRA_BOOK_JSON, book.toJson().toString());
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, R.string.error_load, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book);
        setSupportActionBar(findViewById(R.id.toolbar));

        try {
            book = Book.fromJson(new JSONObject(getIntent().getStringExtra(EXTRA_BOOK_JSON)));
        } catch (Exception e) {
            finish();
            return;
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(book.title);
        }
        UiUtil.applySystemBarInsets(findViewById(R.id.root));

        downloadManager = DownloadManager.get(this);
        storage = Storage.get(this);
        viewModel = new ViewModelProvider(this).get(BookViewModel.class);

        cover = findViewById(R.id.book_cover);
        title = findViewById(R.id.book_title);
        authors = findViewById(R.id.book_authors);
        meta = findViewById(R.id.book_meta);
        paidBadge = findViewById(R.id.paid_badge);
        paidExplanation = findViewById(R.id.paid_explanation);
        partsGroup = findViewById(R.id.parts_group);
        partsScroll = findViewById(R.id.parts_scroll);
        downloadButton = findViewById(R.id.download_button);
        progressSection = findViewById(R.id.progress_section);
        jobProgress = findViewById(R.id.job_progress);
        jobProgressText = findViewById(R.id.job_progress_text);
        cancelButton = findViewById(R.id.cancel_button);
        structureList = findViewById(R.id.structure_list);
        loadingView = findViewById(R.id.loading);
        errorView = findViewById(R.id.error_state);

        structureAdapter = new StructureAdapter(new StructureAdapter.Listener() {
            @Override
            public void onTask(TaskRef task) {
                TaskActivity.start(BookActivity.this, book.id, book.title, task);
            }

            @Override
            public void onDownloadTask(TaskRef task) {
                withNotificationPermission(() -> {
                    boolean added = downloadManager.enqueue(book, BookJob.PART_SINGLE,
                            task.displayTitle(), Collections.singletonList(task));
                    Toast.makeText(BookActivity.this,
                            added ? R.string.download_started : R.string.download_already_running,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
        structureList.setLayoutManager(new LinearLayoutManager(this));
        structureList.setAdapter(structureAdapter);

        downloadButton.setOnClickListener(v -> withNotificationPermission(this::enqueueSelected));
        cancelButton.setOnClickListener(v -> {
            BookJob job = downloadManager.activeJobForBook(book.id);
            if (job != null) downloadManager.cancel(job.key());
        });
        findViewById(R.id.error_retry).setOnClickListener(v -> viewModel.refresh());

        viewModel.getState().observe(this, this::onState);
        downloadManager.getJobsLive().observe(this, jobs -> {
            updateJobSection();
            refreshDownloadedKeys();
        });

        viewModel.load(book, false);
    }

    private void onState(BookViewModel.BookState state) {
        loadingView.setVisibility(state.status == BookViewModel.LOADING ? View.VISIBLE : View.GONE);
        errorView.setVisibility(state.status == BookViewModel.ERROR ? View.VISIBLE : View.GONE);
        if (state.status != BookViewModel.READY) return;

        book = state.book;
        parts = asParts(state.parts);
        bindHeader();
        bindParts();
        selectPart(selectedPart < parts.size() ? selectedPart : 0);
        refreshDownloadedKeys();
        updateJobSection();
    }

    private void bindHeader() {
        title.setText(book.title);
        authors.setText(book.authorsLine());
        authors.setVisibility(book.authors.isEmpty() ? View.GONE : View.VISIBLE);
        String metaText = book.metaLine();
        if (book.subtype != null && !book.subtype.isEmpty()) {
            metaText = metaText.isEmpty() ? book.subtype : book.subtype + " • " + metaText;
        }
        meta.setText(metaText);
        paidBadge.setVisibility(book.isPaid ? View.VISIBLE : View.GONE);
        paidExplanation.setVisibility(book.isPaid ? View.VISIBLE : View.GONE);
        downloadButton.setEnabled(!book.isPaid);

        File localCover = storage.coverFile(book.id);
        UiUtil.loadCover(cover, localCover.isFile() ? localCover.getAbsolutePath() : book.coverUrl);
    }

    /**
     * Top-level structure nodes are edition parts only for year/volume editions;
     * for some books (e.g. biology) the top level is sections or task groups.
     */
    private static List<BookNode> asParts(List<BookNode> top) {
        if (looksLikeParts(top)) return top;
        BookNode wrapper = new BookNode();
        wrapper.title = "";
        wrapper.topics.addAll(top);
        return Collections.singletonList(wrapper);
    }

    private static boolean looksLikeParts(List<BookNode> nodes) {
        if (nodes.isEmpty() || nodes.size() > 8) return false;
        for (BookNode n : nodes) {
            String t = n.displayTitle().toLowerCase(java.util.Locale.getDefault());
            boolean partLike = t.contains("изд") || t.contains("часть")
                    || t.matches(".*(19|20)\\d{2}.*");
            if (!partLike) return false;
        }
        return true;
    }

    private void bindParts() {
        partsGroup.removeAllViews();
        if (parts.size() <= 1) {
            partsScroll.setVisibility(View.GONE);
            return;
        }
        partsScroll.setVisibility(View.VISIBLE);
        for (int i = 0; i < parts.size(); i++) {
            Chip chip = new Chip(this);
            chip.setText(parts.get(i).displayTitle());
            chip.setCheckable(true);
            chip.setId(i + 1);
            partsGroup.addView(chip);
        }
        Chip allChip = new Chip(this);
        allChip.setText(R.string.all_parts);
        allChip.setCheckable(true);
        allChip.setId(parts.size() + 1);
        partsGroup.addView(allChip);
        partsGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            selectPart(checkedIds.get(0) - 1);
        });
        partsGroup.check((selectedPart < parts.size() ? selectedPart : 0) + 1);
    }

    private void selectPart(int index) {
        selectedPart = index;
        List<BookNode> roots;
        List<TaskRef> rootTasks;
        if (index >= parts.size()) {
            roots = parts;
            rootTasks = Collections.emptyList();
            downloadButton.setText(R.string.download_all);
        } else {
            BookNode part = parts.get(index);
            roots = part.topics;
            rootTasks = part.tasks;
            downloadButton.setText(R.string.download_part);
        }
        structureAdapter.setData(roots, rootTasks);
    }

    private void enqueueSelected() {
        if (book.isPaid) return;
        List<TaskRef> tasks = new ArrayList<>();
        String partTitle;
        int partIndex;
        if (selectedPart >= parts.size()) {
            for (BookNode part : parts) part.collectTasks(tasks);
            partTitle = getString(R.string.all_parts);
            partIndex = BookJob.PART_ALL;
        } else {
            parts.get(selectedPart).collectTasks(tasks);
            partTitle = parts.get(selectedPart).displayTitle();
            partIndex = selectedPart;
        }
        boolean added = downloadManager.enqueue(book, partIndex, partTitle, tasks);
        Toast.makeText(this,
                added ? R.string.download_started : R.string.download_already_running,
                Toast.LENGTH_SHORT).show();
    }

    private void withNotificationPermission(Runnable action) {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            pendingDownload = action;
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            action.run();
        }
    }

    private void updateJobSection() {
        BookJob job = downloadManager.activeJobForBook(book.id);
        if (job == null) {
            progressSection.setVisibility(View.GONE);
            downloadButton.setEnabled(!book.isPaid);
            return;
        }
        progressSection.setVisibility(View.VISIBLE);
        downloadButton.setEnabled(false);
        jobProgress.setMax(job.total());
        jobProgress.setProgress(job.done.get() + job.failed.get());
        jobProgressText.setText(getString(R.string.job_progress_format,
                job.done.get() + job.failed.get(), job.total(), job.progressPercent()));
    }

    private void refreshDownloadedKeys() {
        AppExecutors.get().diskIO().execute(() -> {
            java.util.Set<String> keys = storage.downloadedTaskKeys(book.id);
            AppExecutors.get().main(() -> structureAdapter.setDownloadedKeys(keys));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDownloadedKeys();
        updateJobSection();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_book, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            viewModel.refresh();
            return true;
        }
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}