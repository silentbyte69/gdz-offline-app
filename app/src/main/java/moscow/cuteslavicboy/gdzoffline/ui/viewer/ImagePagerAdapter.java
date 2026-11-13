package moscow.cuteslavicboy.gdzoffline.ui.viewer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import moscow.cuteslavicboy.gdzoffline.model.Edition;

public class ImagePagerAdapter extends FragmentStateAdapter {

    private final List<Edition> editions;
    private final List<List<File>> editionFiles;

    public ImagePagerAdapter(@NonNull FragmentActivity activity,
                             List<Edition> editions, List<List<File>> editionFiles) {
        super(activity);
        this.editions = editions;
        this.editionFiles = editionFiles;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        ArrayList<String> paths = new ArrayList<>();
        for (File f : editionFiles.get(position)) {
            paths.add(f.getAbsolutePath());
        }
        return ImagePageFragment.newInstance(paths);
    }

    @Override
    public int getItemCount() {
        return editions.size();
    }
}