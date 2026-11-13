package moscow.cuteslavicboy.gdzoffline.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Catalog {
    public final List<IdTitle> classes = new ArrayList<>();
    public final List<IdTitle> subjects = new ArrayList<>();
    public final List<Book> books = new ArrayList<>();

    public static Catalog fromJson(JSONObject o) {
        Catalog c = new Catalog();
        JSONArray classes = o.optJSONArray("classes");
        if (classes != null) {
            for (int i = 0; i < classes.length(); i++) {
                c.classes.add(IdTitle.fromJson(classes.optJSONObject(i)));
            }
        }
        JSONArray subjects = o.optJSONArray("subjects");
        if (subjects != null) {
            for (int i = 0; i < subjects.length(); i++) {
                c.subjects.add(IdTitle.fromJson(subjects.optJSONObject(i)));
            }
        }
        JSONArray books = o.optJSONArray("books");
        if (books != null) {
            for (int i = 0; i < books.length(); i++) {
                JSONObject bo = books.optJSONObject(i);
                if (bo != null) c.books.add(Book.fromJson(bo));
            }
        }
        return c;
    }

    /** classId <= 0 means all grades. */
    public List<Book> booksFor(int classId, int subjectId, boolean includePaid) {
        List<Book> result = new ArrayList<>();
        for (Book b : books) {
            if (!includePaid && b.isPaid) continue;
            if (subjectId > 0 && b.subjectId != subjectId) continue;
            if (classId > 0 && !b.classes.contains(classId)) continue;
            result.add(b);
        }
        return result;
    }

    public List<Book> search(String query, int classId, boolean includePaid) {
        String q = query.toLowerCase(Locale.getDefault()).trim();
        List<Book> result = new ArrayList<>();
        for (Book b : books) {
            if (!includePaid && b.isPaid) continue;
            if (classId > 0 && !b.classes.contains(classId)) continue;
            if (b.title.toLowerCase(Locale.getDefault()).contains(q)) {
                result.add(b);
                continue;
            }
            for (String a : b.authors) {
                if (a.toLowerCase(Locale.getDefault()).contains(q)) {
                    result.add(b);
                    break;
                }
            }
        }
        return result;
    }

    public String subjectTitle(int subjectId) {
        for (IdTitle s : subjects) {
            if (s.id == subjectId) return s.title;
        }
        return "";
    }
}