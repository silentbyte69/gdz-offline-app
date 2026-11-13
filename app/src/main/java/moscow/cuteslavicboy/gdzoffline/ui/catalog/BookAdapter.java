package moscow.cuteslavicboy.gdzoffline.ui.catalog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import moscow.cuteslavicboy.gdzoffline.R;
import moscow.cuteslavicboy.gdzoffline.model.Book;
import moscow.cuteslavicboy.gdzoffline.util.UiUtil;

public class BookAdapter extends ListAdapter<Book, BookAdapter.Holder> {

    public interface Listener {
        void onBook(Book book);
    }

    private static final DiffUtil.ItemCallback<Book> DIFF = new DiffUtil.ItemCallback<Book>() {
        @Override
        public boolean areItemsTheSame(@NonNull Book oldItem, @NonNull Book newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Book oldItem, @NonNull Book newItem) {
            return oldItem.title.equals(newItem.title)
                    && oldItem.isPaid == newItem.isPaid
                    && oldItem.year.equals(newItem.year);
        }
    };

    private final Listener listener;

    public BookAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Book book = getItem(position);
        holder.title.setText(book.title);
        holder.authors.setText(book.authorsLine());
        holder.authors.setVisibility(book.authors.isEmpty() ? View.GONE : View.VISIBLE);
        holder.meta.setText(book.metaLine());
        holder.meta.setVisibility(book.metaLine().isEmpty() ? View.GONE : View.VISIBLE);
        holder.paidBadge.setVisibility(book.isPaid ? View.VISIBLE : View.GONE);
        UiUtil.loadCover(holder.cover, book.coverUrl);
        holder.itemView.setOnClickListener(v -> listener.onBook(book));
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView cover;
        final TextView title;
        final TextView authors;
        final TextView meta;
        final TextView paidBadge;

        Holder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.book_cover);
            title = itemView.findViewById(R.id.book_title);
            authors = itemView.findViewById(R.id.book_authors);
            meta = itemView.findViewById(R.id.book_meta);
            paidBadge = itemView.findViewById(R.id.paid_badge);
        }
    }
}