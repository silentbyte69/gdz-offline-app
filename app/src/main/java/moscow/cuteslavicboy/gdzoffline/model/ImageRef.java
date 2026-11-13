package moscow.cuteslavicboy.gdzoffline.model;

import org.json.JSONObject;

public class ImageRef {
    public String title;
    public String url;

    public static ImageRef fromJson(JSONObject o) {
        ImageRef r = new ImageRef();
        r.title = o.optString("title");
        r.url = o.optString("url");
        return r;
    }
}