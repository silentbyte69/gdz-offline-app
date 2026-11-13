package moscow.cuteslavicboy.gdzoffline.ui.book;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import moscow.cuteslavicboy.gdzoffline.R;
import moscow.cuteslavicboy.gdzoffline.model.BookNode;
import moscow.cuteslavicboy.gdzoffline.model.TaskRef;

/** Flattened expandable tree: parts/topics/tasks in a single RecyclerView. */
public class StructureAdapter extends RecyclerView.Adapter<StructureAdapter.Holder> {

    public interface Listener {
        void onTask(TaskRef task);

        void onDownloadTask(TaskRef task);
    }

    private static final int TYPE_NODE = 0;
    private static final int TYPE_TASK = 1;

    private static class Row {
        int type;
        BookNode node;
        TaskRef task;
        int depth;
        String path;
        boolean expandable;
    }

    private final Listener listener;
    private final List<Row> rows = new ArrayList<>();
    private final Set<String> expanded = new HashSet<>();
    private Set<String> downloadedKeys = new HashSet<>();

    private List<BookNode> roots = new ArrayList<>();
    private List<TaskRef> rootTasks = new ArrayList<>();

    public StructureAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setData(List<BookNode> roots, List<TaskRef> rootTasks) {
        this.roots = roots;
        this.rootTasks = rootTasks;
        expanded.clear();
        for (int i = 0; i < roots.size(); i++) {
            expanded.add(String.valueOf(i));
        }
        rebuild();
    }

    public void setDownloadedKeys(Set<String> keys) {
        downloadedKeys = keys;
        notifyDataSetChanged();
    }

    private void rebuild() {
        rows.clear();
        for (int i = 0; i < roots.size(); i++) {
            addNode(roots.get(i), 0, String.valueOf(i));
        }
        for (int i = 0; i < rootTasks.size(); i++) {
            addTask(rootTasks.get(i), 0, "t" + i);
        }
        notifyDataSetChanged();
    }

    private void addNode(BookNode node, int depth, String path) {
        Row row = new Row();
        row.type = TYPE_NODE;
        row.node = node;
        row.depth = depth;
        row.path = path;
        row.expandable = !node.topics.isEmpty() || !node.tasks.isEmpty();
        rows.add(row);
        if (expanded.contains(path)) {
            for (int i = 0; i < node.topics.size(); i++) {
                addNode(node.topics.get(i), depth + 1, path + "/" + i);
            }
            for (int i = 0; i < node.tasks.size(); i++) {
                addTask(node.tasks.get(i), depth + 1, path + "/t" + i);
            }
        }
    }

    private void addTask(TaskRef task, int depth, String path) {
        Row row = new Row();
        row.type = TYPE_TASK;
        row.task = task;
        row.depth = depth;
        row.path = path;
        rows.add(row);
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tree_node, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Row row = rows.get(position);
        int pad = (int) (holder.itemView.getContext().getResources().getDisplayMetrics().density
                * (16 + row.depth * 20));
        holder.itemView.setPadding(pad, holder.itemView.getPaddingTop(),
                holder.itemView.getPaddingRight(), holder.itemView.getPaddingBottom());

        if (row.type == TYPE_NODE) {
            holder.title.setText(row.node.displayTitle());
            holder.title.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.arrow.setVisibility(row.expandable ? View.VISIBLE : View.INVISIBLE);
            holder.arrow.setImageResource(expanded.contains(row.path)
                    ? R.drawable.ic_collapse : R.drawable.ic_expand);
            holder.action.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(v -> {
                if (!row.expandable) return;
                if (expanded.contains(row.path)) expanded.remove(row.path);
                else expanded.add(row.path);
                rebuild();
            });
        } else {
            holder.title.setText(row.task.displayTitle());
            holder.title.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.arrow.setVisibility(View.INVISIBLE);
            holder.action.setVisibility(View.VISIBLE);
            boolean downloaded = downloadedKeys.contains(row.task.key());
            holder.action.setImageResource(downloaded ? R.drawable.ic_check : R.drawable.ic_download);
            holder.action.setContentDescription(holder.itemView.getContext()
                    .getString(R.string.download_task));
            holder.itemView.setOnClickListener(v -> listener.onTask(row.task));
            holder.action.setOnClickListener(v -> listener.onDownloadTask(row.task));
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView arrow;
        final TextView title;
        final ImageView action;

        Holder(@NonNull View itemView) {
            super(itemView);
            arrow = itemView.findViewById(R.id.node_arrow);
            title = itemView.findViewById(R.id.node_title);
            action = itemView.findViewById(R.id.node_action);
        }
    }
}