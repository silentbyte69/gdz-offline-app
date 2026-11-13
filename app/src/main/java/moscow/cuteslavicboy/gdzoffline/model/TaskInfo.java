package moscow.cuteslavicboy.gdzoffline.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TaskInfo {
    public boolean success;
    public String message;
    public TaskRef task;
    public final List<Edition> editions = new ArrayList<>();

    public static TaskInfo fromJson(JSONObject o) {
        TaskInfo t = new TaskInfo();
        t.success = o.optBoolean("success", false);
        t.message = o.optString("message");
        JSONObject task = o.optJSONObject("task");
        if (task != null) t.task = TaskRef.fromJson(task);
        JSONArray editions = o.optJSONArray("editions");
        if (editions != null) {
            for (int i = 0; i < editions.length(); i++) {
                JSONObject e = editions.optJSONObject(i);
                if (e != null) t.editions.add(Edition.fromJson(e, i));
            }
        }
        return t;
    }

    public List<Edition> imageEditions() {
        List<Edition> result = new ArrayList<>();
        for (Edition e : editions) {
            if (e.hasImages()) result.add(e);
        }
        return result;
    }
}