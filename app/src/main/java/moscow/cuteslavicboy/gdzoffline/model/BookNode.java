package moscow.cuteslavicboy.gdzoffline.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Recursive structure node: edition part / topic / subtopic. */
public class BookNode {
    public String title;
    public String titleShort;
    public String description;
    public final List<TaskRef> tasks = new ArrayList<>();
    public final List<BookNode> topics = new ArrayList<>();

    public static BookNode fromJson(JSONObject o) {
        BookNode n = new BookNode();
        n.title = o.optString("title");
        n.titleShort = o.optString("title_short");
        n.description = o.optString("description");
        JSONArray tasks = o.optJSONArray("tasks");
        if (tasks != null) {
            for (int i = 0; i < tasks.length(); i++) {
                JSONObject t = tasks.optJSONObject(i);
                if (t != null) n.tasks.add(TaskRef.fromJson(t));
            }
        }
        JSONArray topics = o.optJSONArray("topics");
        if (topics != null) {
            for (int i = 0; i < topics.length(); i++) {
                JSONObject t = topics.optJSONObject(i);
                if (t != null) n.topics.add(BookNode.fromJson(t));
            }
        }
        return n;
    }

    public static List<BookNode> listFromJson(JSONArray arr) {
        List<BookNode> list = new ArrayList<>();
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) list.add(BookNode.fromJson(o));
            }
        }
        return list;
    }

    public void collectTasks(List<TaskRef> out) {
        out.addAll(tasks);
        for (BookNode t : topics) {
            t.collectTasks(out);
        }
    }

    public int countTasks() {
        int count = tasks.size();
        for (BookNode t : topics) count += t.countTasks();
        return count;
    }

    public String displayTitle() {
        return title != null && !title.isEmpty() ? title : titleShort;
    }
}