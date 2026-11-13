package moscow.cuteslavicboy.gdzoffline.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Book {
    public String id;
    public String title;
    public String year;
    public List<String> authors = new ArrayList<>();
    public String publisher;
    public String url;
    public boolean isPaid;
    public List<String> parts = new ArrayList<>();
    public String coverUrl;
    public int subjectId;
    public List<Integer> classes = new ArrayList<>();
    public String subtype;

    public static Book fromJson(JSONObject o) {
        Book b = new Book();
        b.id = String.valueOf(o.opt("id"));
        b.title = o.optString("title");
        b.year = o.optString("year");
        b.authors = stringList(o.optJSONArray("authors"));
        b.publisher = o.optString("publisher");
        b.url = o.optString("url");
        b.isPaid = o.optBoolean("is_paid", false);
        b.parts = stringList(o.optJSONArray("parts"));
        JSONObject cover = o.optJSONObject("cover");
        String coverUrl = cover != null ? cover.optString("url") : o.optString("cover_url");
        b.coverUrl = normalizeCover(coverUrl);
        b.subjectId = o.optInt("subject_id");
        JSONArray classes = o.optJSONArray("classes");
        if (classes != null) {
            for (int i = 0; i < classes.length(); i++) {
                b.classes.add(classes.optInt(i));
            }
        }
        b.subtype = o.optString("subtype");
        return b;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("title", title);
        o.put("year", year);
        o.put("authors", new JSONArray(authors));
        o.put("publisher", publisher);
        o.put("url", url);
        o.put("is_paid", isPaid);
        o.put("parts", new JSONArray(parts));
        o.put("cover_url", coverUrl == null ? "" : coverUrl);
        o.put("subject_id", subjectId);
        o.put("classes", new JSONArray(classes));
        o.put("subtype", subtype);
        return o;
    }

    public String authorsLine() {
        StringBuilder sb = new StringBuilder();
        for (String a : authors) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(a);
        }
        return sb.toString();
    }

    public String metaLine() {
        StringBuilder sb = new StringBuilder();
        if (year != null && !year.isEmpty()) sb.append(year);
        if (publisher != null && !publisher.isEmpty()) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(publisher);
        }
        return sb.toString();
    }

    private static String normalizeCover(String url) {
        if (url == null || url.isEmpty()) return null;
        if (url.startsWith("http://")) return "https://" + url.substring(7);
        return url;
    }

    private static List<String> stringList(JSONArray arr) {
        List<String> list = new ArrayList<>();
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                String s = arr.optString(i);
                if (!s.isEmpty()) list.add(s);
            }
        }
        return list;
    }
}