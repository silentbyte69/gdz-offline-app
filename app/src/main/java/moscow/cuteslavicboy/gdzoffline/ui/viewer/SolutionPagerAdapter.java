package moscow.cuteslavicboy.gdzoffline.ui.viewer;

import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

import moscow.cuteslavicboy.gdzoffline.R;

/** One full-screen zoomable solution image per page. */
public class SolutionPagerAdapter extends RecyclerView.Adapter<SolutionPagerAdapter.Holder> {

    private final List<String> paths;

    public SolutionPagerAdapter(List<String> paths) {
        this.paths = paths;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ZoomableImageView image = new ZoomableImageView(parent.getContext());
        image.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return new Holder(image);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ZoomableImageView image = (ZoomableImageView) holder.itemView;
        Glide.with(image)
                .load(new File(paths.get(position)))
                .placeholder(R.drawable.placeholder_cover)
                .into(image);
    }

    @Override
    public int getItemCount() {
        return paths.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        Holder(@NonNull ZoomableImageView itemView) {
            super(itemView);
        }
    }
}