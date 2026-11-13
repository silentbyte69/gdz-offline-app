package moscow.cuteslavicboy.gdzoffline.ui.library;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;

import moscow.cuteslavicboy.gdzoffline.R;
import moscow.cuteslavicboy.gdzoffline.storage.LibraryEntry;
import moscow.cuteslavicboy.gdzoffline.storage.Storage;
import moscow.cuteslavicboy.gdzoffline.util.UiUtil;

public class LibraryAdapter extends ListAdapter<LibraryEntry, LibraryAdapter.Holder> {

    public interface Listener {
        void onOpen(LibraryEntry entry);

        void onDelete(LibraryEntry entry);
    }

    private static final DiffUtil.ItemCallback<LibraryEntry> DIFF =
            new DiffUtil.ItemCallback<LibraryEntry>() {
                @Override
                public boolean areItemsTheSame(@NonNull LibraryEntry a, @NonNull LibraryEntry b) {
                    return a.book.id.equals(b.book.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull LibraryEntry a, @NonNull LibraryEntry b) {
                    return a.downloadedTasks == b.downloadedTasks && a.sizeBytes == b.sizeBytes;
                }
            };

    private final Listener listener;

    public LibraryAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_library_book, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        LibraryEntry entry = getItem(position);
        holder.title.setText(entry.book.title);
        holder.info.setText(holder.itemView.getContext().getString(
                R.string.downloaded_tasks_format, entry.downloadedTasks)
                + " • " + UiUtil.formatSize(entry.sizeBytes));
        File cover = Storage.get(holder.itemView.getContext()).coverFile(entry.book.id);
        UiUtil.loadCover(holder.cover, cover.isFile() ? cover.getAbsolutePath() : entry.book.coverUrl);
        holder.itemView.setOnClickListener(v -> listener.onOpen(entry));
        holder.delete.setOnClickListener(v -> listener.onDelete(entry));
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView cover;
        final TextView title;
        final TextView info;
        final ImageButton delete;

        Holder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.book_cover);
            title = itemView.findViewById(R.id.book_title);
            info = itemView.findViewById(R.id.book_info);
            delete = itemView.findViewById(R.id.book_delete);
        }
    }
}