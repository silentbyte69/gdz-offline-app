package moscow.cuteslavicboy.gdzoffline.ui.viewer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;

import moscow.cuteslavicboy.gdzoffline.GdzApp;
import moscow.cuteslavicboy.gdzoffline.R;
import moscow.cuteslavicboy.gdzoffline.util.AppExecutors;

public class ImagePageFragment extends Fragment {

    private static final String ARG_PATHS = "paths";
    private static final long HINT_TIMEOUT_MS = 8000;

    private final Runnable hideHint = () -> {
        if (getView() != null) {
            getView().findViewById(R.id.swipe_hint).setVisibility(View.GONE);
        }
    };

    public static ImagePageFragment newInstance(ArrayList<String> paths) {
        ImagePageFragment fragment = new ImagePageFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_PATHS, paths);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_image_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ArrayList<String> paths = getArguments() != null
                ? getArguments().getStringArrayList(ARG_PATHS) : null;
        if (paths == null) paths = new ArrayList<>();

        ViewPager2 pager = view.findViewById(R.id.image_pager_vertical);
        pager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
        pager.setAdapter(new SolutionPagerAdapter(paths));

        TextView counter = view.findViewById(R.id.page_counter);
        View hint = view.findViewById(R.id.swipe_hint);
        if (paths.size() > 1) {
            counter.setVisibility(View.VISIBLE);
            counter.setText(getString(R.string.page_counter_format, 1, paths.size()));
            final int total = paths.size();
            if (!GdzApp.prefs().isViewerHintSeen()) {
                hint.setVisibility(View.VISIBLE);
                AppExecutors.get().mainDelayed(hideHint, HINT_TIMEOUT_MS);
            }
            pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    counter.setText(getString(R.string.page_counter_format, position + 1, total));
                    if (position > 0) {
                        GdzApp.prefs().setViewerHintSeen();
                        AppExecutors.get().removeMainCallbacks(hideHint);
                        hint.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (GdzApp.prefs().isViewerHintSeen() && getView() != null) {
            getView().findViewById(R.id.swipe_hint).setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        AppExecutors.get().removeMainCallbacks(hideHint);
        super.onDestroyView();
    }
}