package es.iker.videosantiguos;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class Ui {
    public static final int BG = 0xFFFFF6E9, ORANGE = 0xFFFF8A3D, DARK = 0xFF6D3A12, GREEN = 0xFF43A047, YELLOW = 0xFFFFD54F, CARD = 0xFFFFFFFF, TEXT = 0xFF4E342E;
    static int dp(Context c, int v) { return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f); }
    static GradientDrawable round(int color, float r) { GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(r); return g; }
    static Button button(Context c, String text, int color) {
        Button b = new Button(c); b.setText(text); b.setAllCaps(false); b.setTextSize(19); b.setTextColor(Color.WHITE);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, round(darker(color), dp(c, 18)));
        s.addState(new int[]{}, round(color, dp(c, 18)));
        b.setBackground(s); b.setMinHeight(dp(c, 60)); b.setPadding(dp(c, 16), dp(c, 8), dp(c, 16), dp(c, 8));
        b.setLayoutParams(lp(c, 6));
        return b;
    }
    static int darker(int c) { return Color.rgb(Color.red(c) * 8 / 10, Color.green(c) * 8 / 10, Color.blue(c) * 8 / 10); }
    static LinearLayout.LayoutParams lp(Context c, int m) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, dp(c, m), 0, dp(c, m)); return p; }
    static TextView text(Context c, String t, int size, int color, boolean bold) {
        TextView v = new TextView(c); v.setText(t); v.setTextSize(size); v.setTextColor(color); if (bold) v.setTypeface(Typeface.DEFAULT_BOLD); v.setLayoutParams(lp(c, 4)); return v;
    }

    /** Ask what to do with a video: watch / download / open in browser. */
    static void videoActions(final Activity a, final String orig, final String ts, String title) {
        final String play = Extractor.raw(ts, orig);
        final String ext = Extractor.ext(orig);
        final boolean playable = !(ext.equals("flv") || ext.equals("f4v") || ext.equals("f4m"));
        List<String> opts = new ArrayList<>();
        if (playable) opts.add("▶️  Ver ahora");
        opts.add("⬇️  Descargar");
        opts.add("🌐  Abrir en el navegador");
        opts.add("📋  Copiar enlace");
        final String[] arr = opts.toArray(new String[0]);
        new AlertDialog.Builder(a).setTitle(title + (playable ? "" : "\n(Este formato antiguo se puede descargar 💾)"))
            .setItems(arr, (d, w) -> {
                String o = arr[w];
                if (o.contains("Ver")) a.startActivity(new Intent(a, PlayerActivity.class).putExtra("url", play).putExtra("orig", orig).putExtra("ts", ts));
                else if (o.contains("Descargar")) download(a, play, Extractor.niceName(orig, ts));
                else if (o.contains("navegador")) a.startActivity(new Intent(a, BrowserActivity.class).putExtra("url", play));
                else { ((ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("video", play)); Toast.makeText(a, "Enlace copiado 👍", Toast.LENGTH_SHORT).show(); }
            }).setNegativeButton("Cerrar", null).show();
    }

    static void download(Activity a, String url, String name) {
        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT <= 28 && a.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != 0) {
            a.requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 7);
            Toast.makeText(a, "Dale permiso y vuelve a pulsar Descargar 😊", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
            r.setTitle(name); r.setDescription("Vídeos Antiguos");
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "VideosAntiguos/" + name);
            r.addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 12) VideosAntiguos/1.0");
            String mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(Extractor.ext(name));
            if (mime != null) r.setMimeType(mime);
            if (Build.VERSION.SDK_INT < 29) r.allowScanningByMediaScanner();
            ((DownloadManager) a.getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(r);
            Toast.makeText(a, "¡Descarga empezada! 🎉 Se guarda en Descargas/VideosAntiguos", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(a, "No se pudo empezar la descarga. Prueba otra vez 🙂", Toast.LENGTH_LONG).show();
        }
    }
}
