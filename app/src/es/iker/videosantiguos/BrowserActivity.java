package es.iker.videosantiguos;

import android.app.*;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.util.*;

public class BrowserActivity extends Activity {
    WebView w; Button found; ProgressBar bar;
    final LinkedHashSet<String> media = new LinkedHashSet<>();
    final Handler h = new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Ui.BG);
        LinearLayout top = new LinearLayout(this); top.setPadding(Ui.dp(this, 6), 0, Ui.dp(this, 6), 0);
        Button back = Ui.button(this, "⬅️", Ui.DARK);
        found = Ui.button(this, "🎬 Vídeos detectados (0)", Ui.GREEN);
        Button search = Ui.button(this, "🔎", Ui.ORANGE);
        top.addView(back, new LinearLayout.LayoutParams(-2, -2)); top.addView(found, new LinearLayout.LayoutParams(0, -2, 1)); top.addView(search, new LinearLayout.LayoutParams(-2, -2));
        root.addView(top);
        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); bar.setMax(100);
        root.addView(bar, new LinearLayout.LayoutParams(-1, Ui.dp(this, 6)));
        w = new WebView(this);
        root.addView(w, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setLoadWithOverviewMode(true); s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true); s.setDisplayZoomControls(false); s.setMediaPlaybackRequiresUserGesture(false);
        if (Build.VERSION.SDK_INT >= 21) s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        w.setWebChromeClient(new WebChromeClient() { @Override public void onProgressChanged(WebView v, int p) { bar.setProgress(p); bar.setVisibility(p >= 100 ? View.GONE : View.VISIBLE); } });
        w.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, String url) { check(url); return null; }
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) { check(r.getUrl().toString()); return null; }
            @Override public void onLoadResource(WebView v, String url) { check(url); }
            @Override public boolean shouldOverrideUrlLoading(WebView v, String url) {
                if (Extractor.isMediaUrl(url)) { check(url); openMedia(url); return true; }
                return false;
            }
        });
        w.setDownloadListener((url, ua, cd, mime, len) -> { check(url); openMedia(url); });
        back.setOnClickListener(x -> { if (w.canGoBack()) w.goBack(); else finish(); });
        found.setOnClickListener(x -> showFound());
        search.setOnClickListener(x -> { setResult(RESULT_OK, new android.content.Intent().putExtra("url", w.getUrl())); finish(); });
        String url = getIntent().getStringExtra("url");
        if (url == null) url = "https://web.archive.org/";
        if (!url.matches("(?i)^https?://.*")) url = "https://web.archive.org/web/2013/" + url;
        w.loadUrl(url);
    }

    void check(final String url) {
        if (url == null || !Extractor.isMediaUrl(url)) return;
        h.post(() -> { if (media.add(url)) { found.setText("🎬 Vídeos detectados (" + media.size() + ")"); Toast.makeText(this, "¡He visto un vídeo! 🎉", Toast.LENGTH_SHORT).show(); } });
    }
    void openMedia(String url) {
        String ts = Extractor.timestamp(url); if (ts == null) ts = Extractor.timestamp(w.getUrl() == null ? "" : w.getUrl());
        Ui.videoActions(this, Extractor.original(url), ts, Extractor.niceName(Extractor.original(url), ts));
    }
    void showFound() {
        if (media.isEmpty()) { new AlertDialog.Builder(this).setTitle("Todavía no hay vídeos 🙂").setMessage("Navega por la página y pulsa el reproductor. Cuando vea un vídeo, aparecerá aquí.").setPositiveButton("Vale", null).show(); return; }
        final String[] arr = media.toArray(new String[0]);
        String[] names = new String[arr.length];
        for (int i = 0; i < arr.length; i++) names[i] = "🎞️ " + Extractor.niceName(Extractor.original(arr[i]), Extractor.timestamp(arr[i]));
        new AlertDialog.Builder(this).setTitle("Vídeos detectados").setItems(names, (d, i) -> openMedia(arr[i])).setNegativeButton("Cerrar", null).show();
    }
    @Override public void onBackPressed() { if (w.canGoBack()) w.goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy() { w.destroy(); super.onDestroy(); }
}
