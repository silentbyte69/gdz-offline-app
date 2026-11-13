package moscow.cuteslavicboy.gdzoffline.ui.viewer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONObject;

import java.io.File;
import java.util.List;

import moscow.cuteslavicboy.gdzoffline.R;
import moscow.cuteslavicboy.gdzoffline.model.Edition;
import moscow.cuteslavicboy.gdzoffline.model.TaskRef;
import moscow.cuteslavicboy.gdzoffline.repo.TaskRepository;
import moscow.cuteslavicboy.gdzoffline.util.UiUtil;

public class TaskActivity extends AppCompatActivity {

    private static final String EXTRA_BOOK_ID = "book_id";
    private static final String EXTRA_BOOK_TITLE = "book_title";
    private static final String EXTRA_TASK_JSON = "task_json";

    private String bookId;
    private TaskRef taskRef;
    private TaskViewModel viewModel;

    private TabLayout tabs;
    private ViewPager2 pager;
    private ProgressBar progress;
    private TextView message;
    private TextView offlineBadge;
    private View retryButton;

    public static void start(Context context, String bookId, String bookTitle, TaskRef task) {
        try {
            Intent intent = new Intent(context, TaskActivity.class);
            intent.putExtra(EXTRA_BOOK_ID, bookId);
            intent.putExtra(EXTRA_BOOK_TITLE, bookTitle);
            intent.putExtra(EXTRA_TASK_JSON, task.toJson().toString());
            context.startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);
        setSupportActionBar(findViewById(R.id.toolbar));
        UiUtil.applySystemBarInsets(findViewById(R.id.root));

        bookId = getIntent().getStringExtra(EXTRA_BOOK_ID);
        String bookTitle = getIntent().getStringExtra(EXTRA_BOOK_TITLE);
        try {
            taskRef = TaskRef.fromJson(new JSONObject(getIntent().getStringExtra(EXTRA_TASK_JSON)));
        } catch (Exception e) {
            finish();
            return;
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getString(R.string.task_title_format, taskRef.displayTitle()));
            actionBar.setSubtitle(bookTitle);
        }

        tabs = findViewById(R.id.edition_tabs);
        pager = findViewById(R.id.image_pager);
        progress = findViewById(R.id.progress);
        message = findViewById(R.id.message);
        offlineBadge = findViewById(R.id.offline_badge);
        retryButton = findViewById(R.id.retry_button);
        retryButton.setOnClickListener(v -> viewModel.load(bookId, taskRef));

        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        viewModel.getState().observe(this, this::onState);
        viewModel.load(bookId, taskRef);
    }

    private void onState(TaskViewModel.TaskState state) {
        progress.setVisibility(state.status == TaskViewModel.LOADING ? View.VISIBLE : View.GONE);
        if (state.status == TaskViewModel.LOADING) {
            pager.setVisibility(View.GONE);
            tabs.setVisibility(View.GONE);
            message.setVisibility(View.GONE);
            retryButton.setVisibility(View.GONE);
            offlineBadge.setVisibility(View.GONE);
            return;
        }
        if (state.status == TaskViewModel.ERROR) {
            showMessage(getString(R.string.task_load_error), true);
            return;
        }
        TaskRepository.TaskResult result = state.result;
        if (result.error != null) {
            showMessage(result.paid ? getString(R.string.task_paid_message) : result.error,
                    !result.paid);
            return;
        }
        List<Edition> editions = result.info.imageEditions();
        if (editions.isEmpty()) {
            showMessage(getString(R.string.task_video_only), false);
            return;
        }
        message.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
        offlineBadge.setVisibility(result.fromDisk ? View.VISIBLE : View.GONE);
        pager.setVisibility(View.VISIBLE);

        ImagePagerAdapter adapter = new ImagePagerAdapter(this, editions, result.editionFiles);
        pager.setAdapter(adapter);
        if (editions.size() > 1) {
            tabs.setVisibility(View.VISIBLE);
            new TabLayoutMediator(tabs, pager,
                    (tab, position) -> tab.setText(editions.get(position).title)).attach();
        } else {
            tabs.setVisibility(View.GONE);
        }
    }

    private void showMessage(String text, boolean showRetry) {
        pager.setVisibility(View.GONE);
        tabs.setVisibility(View.GONE);
        offlineBadge.setVisibility(View.GONE);
        message.setVisibility(View.VISIBLE);
        message.setText(text);
        retryButton.setVisibility(showRetry ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}