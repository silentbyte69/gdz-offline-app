package moscow.cuteslavicboy.gdzoffline.download;

import org.json.JSONException;
import org.json.JSONObject;

public class TaskItem {
    public static final int PENDING = 0;
    public static final int DONE = 1;
    public static final int FAILED = 2;
    public static final int IN_PROGRESS = 3;

    public String url;
    public volatile int status = PENDING;

    public TaskItem(String url) {
        this.url = url;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("url", url);
        o.put("status", status);
        return o;
    }

    public static TaskItem fromJson(JSONObject o) {
        TaskItem item = new TaskItem(o.optString("url"));
        item.status = o.optInt("status", PENDING);
        return item;
    }
}