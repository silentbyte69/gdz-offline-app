package moscow.cuteslavicboy.gdzoffline.ui.downloads;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import moscow.cuteslavicboy.gdzoffline.R;
import moscow.cuteslavicboy.gdzoffline.download.BookJob;
import moscow.cuteslavicboy.gdzoffline.download.DownloadManager;

public class DownloadsFragment extends Fragment {

    private DownloadManager manager;
    private DownloadJobAdapter adapter;
    private View emptyState;
    private View actionsRow;
    private RecyclerView recycler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_downloads, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        manager = DownloadManager.get(requireContext());
        recycler = view.findViewById(R.id.recycler);
        emptyState = view.findViewById(R.id.empty_state);
        actionsRow = view.findViewById(R.id.actions_row);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new DownloadJobAdapter(new DownloadJobAdapter.Listener() {
            @Override
            public void onCancel(BookJob job) {
                manager.cancel(job.key());
            }

            @Override
            public void onRetry(BookJob job) {
                manager.retryFailed(job.key());
            }
        });
        recycler.setAdapter(adapter);

        view.findViewById(R.id.cancel_all_button).setOnClickListener(v -> manager.cancelAll());
        view.findViewById(R.id.clear_finished_button).setOnClickListener(v -> manager.clearFinished());

        manager.getJobsLive().observe(getViewLifecycleOwner(), this::onJobs);
    }

    private void onJobs(List<BookJob> jobs) {
        adapter.submitList(jobs);
        boolean empty = jobs.isEmpty();
        recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        actionsRow.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}