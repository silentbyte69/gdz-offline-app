package moscow.cuteslavicboy.gdzoffline.ui.catalog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import moscow.cuteslavicboy.gdzoffline.R;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.Holder> {

    public interface Listener {
        void onSubject(CatalogViewModel.SubjectRow row);
    }

    private final Listener listener;
    private final List<CatalogViewModel.SubjectRow> rows = new ArrayList<>();

    public SubjectAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<CatalogViewModel.SubjectRow> newRows) {
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subject, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        CatalogViewModel.SubjectRow row = rows.get(position);
        holder.title.setText(row.title);
        holder.count.setText(holder.itemView.getContext()
                .getString(R.string.books_count_format, row.count));
        holder.itemView.setOnClickListener(v -> listener.onSubject(row));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView count;

        Holder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.subject_title);
            count = itemView.findViewById(R.id.subject_count);
        }
    }
}