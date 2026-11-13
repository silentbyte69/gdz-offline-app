package moscow.cuteslavicboy.gdzoffline.api;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import moscow.cuteslavicboy.gdzoffline.util.FilesUtil;

/**
 * Single network entry point. The gdz-ru.com API and its images return 403
 * for browser User-Agents; the okhttp UA is required (verified empirically).
 */
public final class ApiClient {

    public static final String BASE = "https://gdz-ru.com";
    public static final String UA = "okhttp/4.9.1";

    private static final String TAG = "GdzApi";
    private static final int CONNECT_TIMEOUT = 15_000;
    private static final int READ_TIMEOUT = 30_000;
    private static final int MAX_ATTEMPTS = 3;

    private ApiClient() {
    }

    public static String absoluteUrl(String pathOrUrl) {
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            return pathOrUrl;
        }
        return BASE + (pathOrUrl.startsWith("/") ? pathOrUrl : "/" + pathOrUrl);
    }

    public static String getJson(String pathOrUrl) throws IOException {
        HttpURLConnection conn = open(pathOrUrl);
        try {
            int code = conn.getResponseCode();
            if (code != 200) {
                throw new IOException("HTTP " + code + " for " + pathOrUrl);
            }
            try (InputStream in = conn.getInputStream()) {
                return readFully(in);
            }
        } finally {
            conn.disconnect();
        }
    }

    public static String getJsonWithRetry(String pathOrUrl) throws IOException {
        IOException last = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return getJson(pathOrUrl);
            } catch (IOException e) {
                last = e;
                Log.w(TAG, "Attempt " + (attempt + 1) + " failed for " + pathOrUrl + ": " + e.getMessage());
                sleepBackoff(attempt);
            }
        }
        throw last;
    }

    /** Streams a remote file to dest atomically (.tmp + rename). */
    public static void downloadToFile(String url, File dest) throws IOException {
        HttpURLConnection conn = open(url);
        File tmp = new File(dest.getAbsolutePath() + ".tmp");
        try {
            int code = conn.getResponseCode();
            if (code != 200) {
                throw new IOException("HTTP " + code + " for " + url);
            }
            File parent = dest.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Cannot create dir " + parent);
            }
            long total = 0;
            try (InputStream in = conn.getInputStream();
                 OutputStream out = new FileOutputStream(tmp)) {
                byte[] buf = new byte[16 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    total += n;
                }
            }
            if (total == 0) {
                throw new IOException("Empty response for " + url);
            }
            if (!tmp.renameTo(dest)) {
                if (dest.exists() && !dest.delete()) {
                    throw new IOException("Cannot replace " + dest);
                }
                if (!tmp.renameTo(dest)) {
                    throw new IOException("Cannot rename to " + dest);
                }
            }
        } finally {
            tmp.delete();
            conn.disconnect();
        }
    }

    public static void downloadToFileWithRetry(String url, File dest) throws IOException {
        if (FilesUtil.isNonEmptyFile(dest)) return;
        IOException last = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                downloadToFile(url, dest);
                return;
            } catch (IOException e) {
                last = e;
                Log.w(TAG, "Download attempt " + (attempt + 1) + " failed for " + url + ": " + e.getMessage());
                sleepBackoff(attempt);
            }
        }
        throw last;
    }

    private static HttpURLConnection open(String pathOrUrl) throws IOException {
        URL url = new URL(absoluteUrl(pathOrUrl));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", UA);
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setInstanceFollowRedirects(true);
        // Accept-Encoding is intentionally not set: HttpURLConnection adds gzip
        // itself and decodes it transparently.
        return conn;
    }

    private static String readFully(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[16 * 1024];
        int n;
        while ((n = in.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        return bos.toString(StandardCharsets.UTF_8.name());
    }

    private static void sleepBackoff(int attempt) {
        try {
            Thread.sleep(1000L * (long) Math.pow(3, attempt));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}