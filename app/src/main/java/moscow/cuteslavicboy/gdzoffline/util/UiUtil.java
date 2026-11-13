package moscow.cuteslavicboy.gdzoffline.util;

import android.view.View;
import android.widget.ImageView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

import java.io.File;
import java.util.Locale;

import moscow.cuteslavicboy.gdzoffline.R;
import moscow.cuteslavicboy.gdzoffline.api.ApiClient;

public final class UiUtil {

    private UiUtil() {
    }

    /** targetSdk 35+ forces edge-to-edge; pad the root so content clears system bars. */
    public static void applySystemBarInsets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " Б";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.getDefault(), "%.1f КБ", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.getDefault(), "%.1f МБ", mb);
        return String.format(Locale.getDefault(), "%.2f ГБ", mb / 1024.0);
    }

    /** Loads a remote cover (with the required UA header) or a local file. */
    public static void loadCover(ImageView view, String urlOrPath) {
        if (urlOrPath == null || urlOrPath.isEmpty()) {
            Glide.with(view).clear(view);
            view.setImageResource(R.drawable.placeholder_cover);
            return;
        }
        Object source;
        if (urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://")) {
            source = new GlideUrl(urlOrPath, new LazyHeaders.Builder()
                    .addHeader("User-Agent", ApiClient.UA)
                    .build());
        } else {
            source = new File(urlOrPath);
        }
        Glide.with(view)
                .load(source)
                .placeholder(R.drawable.placeholder_cover)
                .error(R.drawable.placeholder_cover)
                .centerCrop()
                .into(view);
    }
}