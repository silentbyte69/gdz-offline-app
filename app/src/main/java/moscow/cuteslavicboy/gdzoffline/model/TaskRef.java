package moscow.cuteslavicboy.gdzoffline.model;

import org.json.JSONException;
import org.json.JSONObject;

import moscow.cuteslavicboy.gdzoffline.util.FilesUtil;

public class TaskRef {
    public String title;
    public String titleShort;
    public String description;
    public String youtubeVideoId;
    public String url;

    public static TaskRef fromJson(JSONObject o) {
        TaskRef t = new TaskRef();
        t.title = o.optString("title");
        t.titleShort = o.optString("title_short");
        t.description = o.optString("description");
        t.youtubeVideoId = o.optString("youtube_video_id");
        t.url = o.optString("url");
        return t;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("title", title);
        o.put("title_short", titleShort);
        o.put("description", description);
        o.put("youtube_video_id", youtubeVideoId);
        o.put("url", url);
        return o;
    }

    public String key() {
        return FilesUtil.sanitizeKey(url);
    }

    public String displayTitle() {
        return title != null && !title.isEmpty() ? title : titleShort;
    }
}