package moscow.cuteslavicboy.gdzoffline.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class FilesUtil {

    private FilesUtil() {
    }

    public static void atomicWrite(File dest, String content) throws IOException {
        atomicWrite(dest, content.getBytes(StandardCharsets.UTF_8));
    }

    public static void atomicWrite(File dest, byte[] data) throws IOException {
        File parent = dest.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create dir " + parent);
        }
        File tmp = new File(dest.getAbsolutePath() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(data);
            out.getFD().sync();
        }
        if (tmp.renameTo(dest)) {
            return;
        }
        // renameTo can fail if dest exists on some filesystems
        if (dest.exists() && !dest.delete()) {
            throw new IOException("Cannot replace " + dest);
        }
        if (!tmp.renameTo(dest)) {
            throw new IOException("Cannot rename " + tmp + " -> " + dest);
        }
    }

    public static String readString(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    /** "/186098/2026-1-1-1" -> "186098_2026-1-1-1" */
    public static String sanitizeKey(String url) {
        if (url == null) return "null";
        String s = url.startsWith("/") ? url.substring(1) : url;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '.' || c == '_' || c == '-') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        String result = sb.toString();
        return result.length() > 120 ? result.substring(0, 120) : result;
    }

    public static boolean isNonEmptyFile(File f) {
        return f != null && f.isFile() && f.length() > 0;
    }

    public static void deleteRecursive(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursive(c);
            }
        }
        f.delete();
    }

    public static long dirSize(File dir) {
        if (dir == null || !dir.exists()) return 0;
        if (dir.isFile()) return dir.length();
        long size = 0;
        File[] children = dir.listFiles();
        if (children != null) {
            for (File c : children) size += dirSize(c);
        }
        return size;
    }
}