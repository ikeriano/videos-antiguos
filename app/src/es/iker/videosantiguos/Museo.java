package es.iker.videosantiguos;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.util.LruCache;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Datos del museo (assets/museo.json) y cargador de imágenes ligero. */
public class Museo {
    static JSONObject data;
    static synchronized JSONObject get(Context c) {
        if (data != null) return data;
        try (InputStream in = c.getAssets().open("museo.json")) {
            ByteArrayOutputStream o = new ByteArrayOutputStream(); byte[] b = new byte[16384]; int n;
            while ((n = in.read(b)) > 0) o.write(b, 0, n);
            data = new JSONObject(o.toString("UTF-8"));
        } catch (Exception e) { data = new JSONObject(); }
        return data;
    }
    static JSONObject programa(Context c, String id) {
        JSONArray a = get(c).optJSONArray("programas");
        for (int i = 0; a != null && i < a.length(); i++) if (id.equals(a.optJSONObject(i).optString("id"))) return a.optJSONObject(i);
        return null;
    }
    static int icono(Context c) { return c.getResources().getIdentifier("icon", "drawable", c.getPackageName()); }
    static final String[] MESES = {"enero","febrero","marzo","abril","mayo","junio","julio","agosto","septiembre","octubre","noviembre","diciembre"};
    static String fecha(String iso) {
        try { String[] p = iso.split("-"); return Integer.parseInt(p[2]) + " de " + MESES[Integer.parseInt(p[1]) - 1] + " de " + p[0]; } catch (Exception e) { return iso; }
    }
    static void abrirFuera(Activity a, String url) {
        try { a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) { Toast.makeText(a, "No encuentro una app para abrirlo 🙂", Toast.LENGTH_SHORT).show(); }
    }

    // ---------- imágenes ----------
    static final LruCache<String, Bitmap> mem = new LruCache<String, Bitmap>(6 * 1024 * 1024) {
        @Override protected int sizeOf(String k, Bitmap b) { return b.getByteCount(); }
    };
    static final ExecutorService pool = Executors.newFixedThreadPool(2);
    static final Handler main = new Handler(Looper.getMainLooper());
    static final Set<String> fallidas = Collections.synchronizedSet(new HashSet<String>());

    /** Carga la imagen en el ImageView (o deja el marcador si falla). */
    static void imagen(final Context c, final ImageView v, final String url, final int marcador) {
        v.setTag(url);
        if (url == null || url.isEmpty() || fallidas.contains(url)) { v.setImageResource(marcador); return; }
        Bitmap b = mem.get(url);
        if (b != null) { v.setImageBitmap(b); return; }
        v.setImageResource(marcador);
        final Context app = c.getApplicationContext();
        pool.execute(() -> {
            if (!url.equals(v.getTag())) return;
            Bitmap r = null;
            for (String u : url.split("\\|")) { if (u.isEmpty() || fallidas.contains(u)) continue; r = cargar(app, u); if (r != null) break; fallidas.add(u); }
            final Bitmap bm = r;
            if (bm != null) mem.put(url, bm); else fallidas.add(url);
            main.post(() -> { if (url.equals(v.getTag()) && bm != null) v.setImageBitmap(bm); });
        });
    }
    static Bitmap cargar(Context c, String url) {
        File dir = new File(c.getCacheDir(), "img"); dir.mkdirs();
        File f = new File(dir, Integer.toHexString(url.hashCode()) + ".jpg");
        try {
            if (!f.exists() || f.length() == 0) {
                HttpURLConnection con = null;
                String u = url;
                for (int r = 0; r < 5; r++) {
                    con = (HttpURLConnection) new URL(u).openConnection();
                    con.setConnectTimeout(15000); con.setReadTimeout(20000); con.setInstanceFollowRedirects(false);
                    con.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 12) VideosAntiguos/1.1");
                    int code = con.getResponseCode();
                    if (code >= 300 && code < 400 && con.getHeaderField("Location") != null) { u = new URL(new URL(u), con.getHeaderField("Location")).toString(); con.disconnect(); continue; }
                    if (code != 200) { con.disconnect(); return null; }
                    break;
                }
                File tmp = new File(dir, f.getName() + ".tmp");
                try (InputStream in = con.getInputStream(); OutputStream o = new FileOutputStream(tmp)) {
                    byte[] b = new byte[8192]; int n, tot = 0;
                    while ((n = in.read(b)) > 0) { o.write(b, 0, n); tot += n; if (tot > 3000000) break; }
                }
                con.disconnect();
                tmp.renameTo(f);
                limpiar(dir);
            }
            BitmapFactory.Options o = new BitmapFactory.Options(); o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getPath(), o);
            int s = 1; while (o.outWidth / (s * 2) >= 360) s *= 2;
            o = new BitmapFactory.Options(); o.inSampleSize = s; o.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap b = BitmapFactory.decodeFile(f.getPath(), o);
            if (b == null) f.delete();
            return b;
        } catch (Throwable t) { return null; }
    }
    /** Mantiene la caché de disco por debajo de ~15 MB. */
    static void limpiar(File dir) {
        File[] fs = dir.listFiles(); if (fs == null) return;
        long tot = 0; for (File x : fs) tot += x.length();
        if (tot < 15000000) return;
        Arrays.sort(fs, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        for (File x : fs) { if (tot < 10000000) break; tot -= x.length(); x.delete(); }
    }
}
