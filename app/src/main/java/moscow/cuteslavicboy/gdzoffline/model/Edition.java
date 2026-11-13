package moscow.cuteslavicboy.gdzoffline.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Edition {
    public static final int TYPE_IMAGES = 2;
    public static final int TYPE_VIDEO = 3;

    /** Position in the original editions array; used for on-disk file names. */
    public int index;
    public String title;
    public int type;
    public int publisherId;
    public final List<ImageRef> images = new ArrayList<>();

    public static Edition fromJson(JSONObject o, int index) {
        Edition e = new Edition();
        e.index = index;
        e.title = o.optString("title");
        e.type = o.optInt("type");
        e.publisherId = o.optInt("publisher_id");
        JSONArray images = o.optJSONArray("images");
        if (images != null) {
            for (int i = 0; i < images.length(); i++) {
                JSONObject im = images.optJSONObject(i);
                if (im != null) e.images.add(ImageRef.fromJson(im));
            }
        }
        return e;
    }

    public boolean hasImages() {
        return type == TYPE_IMAGES && !images.isEmpty();
    }
}