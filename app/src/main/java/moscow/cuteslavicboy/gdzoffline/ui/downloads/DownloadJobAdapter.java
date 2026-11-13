package moscow.cuteslavicboy.gdzoffline.ui.downloads;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import moscow.cuteslavicboy.gdzoffline.R;
import moscow.cuteslavicboy.gdzoffline.download.BookJob;

public class DownloadJobAdapter extends ListAdapter<BookJob, DownloadJobAdapter.Holder> {

    public interface Listener {
        void onCancel(BookJob job);

        void onRetry(BookJob job);
    }

    private static final DiffUtil.ItemCallback<BookJob> DIFF = new DiffUtil.ItemCallback<BookJob>() {
        @Override
        public boolean areItemsTheSame(@NonNull BookJob a, @NonNull BookJob b) {
            return a.key().equals(b.key()) && a.addedAt == b.addedAt;
        }

        @Override
        public boolean areContentsTheSame(@NonNull BookJob a, @NonNull BookJob b) {
            return a.state == b.state
                    && a.done.get() == b.done.get()
                    && a.failed.get() == b.failed.get();
        }
    };

    private final Listener listener;

    public DownloadJobAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_download_job, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        BookJob job = getItem(position);
        holder.title.setText(job.title);
        holder.part.setText(job.partTitle == null || job.partTitle.isEmpty()
                ? holder.itemView.getContext().getString(R.string.all_parts) : job.partTitle);
        int processed = job.done.get() + job.failed.get();
        holder.progress.setMax(job.total());
        holder.progress.setProgress(processed);
        holder.progressText.setText(holder.itemView.getContext().getString(
                R.string.job_progress_format, processed, job.total(), job.progressPercent()));

        String stateText;
        switch (job.state) {
            case BookJob.QUEUED:
                stateText = holder.itemView.getContext().getString(R.string.state_queued);
                break;
            case BookJob.RUNNING:
                stateText = holder.itemView.getContext().getString(R.string.state_running);
                break;
            case BookJob.PAUSED:
                stateText = holder.itemView.getContext().getString(R.string.state_paused);
                break;
            case BookJob.DONE:
                stateText = holder.itemView.getContext().getString(R.string.state_done);
                break;
            case BookJob.DONE_WITH_ERRORS:
                stateText = holder.itemView.getContext()
                        .getString(R.string.state_done_errors, job.failed.get());
                break;
            default:
                stateText = holder.itemView.getContext().getString(R.string.state_cancelled);
                break;
        }
        holder.state.setText(stateText);

        holder.cancel.setVisibility(job.isActive() ? View.VISIBLE : View.GONE);
        holder.cancel.setOnClickListener(v -> listener.onCancel(job));
        holder.retry.setVisibility(job.state == BookJob.DONE_WITH_ERRORS ? View.VISIBLE : View.GONE);
        holder.retry.setOnClickListener(v -> listener.onRetry(job));
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView part;
        final ProgressBar progress;
        final TextView progressText;
        final TextView state;
        final Button cancel;
        final Button retry;

        Holder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.job_title);
            part = itemView.findViewById(R.id.job_part);
            progress = itemView.findViewById(R.id.job_progress);
            progressText = itemView.findViewById(R.id.job_progress_text);
            state = itemView.findViewById(R.id.job_state);
            cancel = itemView.findViewById(R.id.job_cancel);
            retry = itemView.findViewById(R.id.job_retry);
        }
    }
}