package moscow.cuteslavicboy.gdzoffline.model;

import org.json.JSONObject;

public class IdTitle {
    public int id;
    public String title;

    public static IdTitle fromJson(JSONObject o) {
        IdTitle it = new IdTitle();
        it.id = o.optInt("id");
        it.title = o.optString("title");
        return it;
    }
}