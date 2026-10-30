package es.iker.videosantiguos;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.*;

public class MainActivity extends Activity {
    static final String[][] DEFAULT_FAVS = {
        {"Karlos Arguiñano 7 feb 2013", "https://web.archive.org/web/20130211045623/http://www.antena3.com/videos/karlos-arguinano/2013-febrero-7-2013020700029.html"},
        {"Karlos Arguiñano (portada programa)", "https://web.archive.org/web/20130212042936/http://www.antena3.com/programas/karlos-arguinano"},
        {"Karlos Arguiñano vídeos", "https://web.archive.org/web/20130209075256/http://www.antena3.com/videos/karlos-arguinano.html"},
        {"Ahora Caigo especial 10 años", "https://web.archive.org/web/20220517210544/https://www.atresplayer.com/antena3/programas/ahora-caigo/temporada-1/01-08-21-especial-10-anos_60ee0bdd6584a872911240a6/"},
        {"Hogarutil Arguiñano", "https://web.archive.org/web/20130126123522/http://www.hogarutil.com/tv/programas/karlos-arguinano-cocina/index.html?201301"},
    };
    EditText input; LinearLayout favBox, results; TextView status; Button searchBtn, stopBtn; ProgressBar spin;
    Searcher current; final Handler h = new Handler(Looper.getMainLooper()); int count; String lastUrl;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Ui.DARK);
        ScrollView sv = new ScrollView(this); sv.setBackgroundColor(Ui.BG); sv.setFillViewport(true);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 16); root.setPadding(p, p, p, p);
        sv.addView(root);
        TextView title = Ui.text(this, "📺 Vídeos Antiguos", 30, Ui.DARK, true); title.setGravity(Gravity.CENTER); root.addView(title);
        TextView sub = Ui.text(this, "¡Hola Iker! Pega un enlace de la Wayback Machine y busco los vídeos por ti 😊", 17, Ui.TEXT, false); sub.setGravity(Gravity.CENTER); root.addView(sub);
        Button museo = Ui.button(this, "🏛️ Museo: Arguiñano, Ahora Caigo y más", 0xFF8E24AA);
        museo.setOnClickListener(v -> startActivity(new Intent(this, MuseoActivity.class)));
        root.addView(museo);
        input = new EditText(this); input.setHint("Pega aquí el enlace (web.archive.org/...)"); input.setTextSize(17); input.setSingleLine(true);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_URI); input.setBackground(Ui.round(Color.WHITE, Ui.dp(this, 14)));
        input.setPadding(p, p, p, p); input.setLayoutParams(Ui.lp(this, 8)); input.setTextColor(Ui.TEXT);
        root.addView(input);
        LinearLayout row = new LinearLayout(this);
        Button paste = Ui.button(this, "📋 Pegar", Ui.DARK);
        searchBtn = Ui.button(this, "🔎 Buscar vídeos", Ui.ORANGE);
        row.addView(paste, new LinearLayout.LayoutParams(0, -2, 1)); View gap = new View(this); row.addView(gap, new LinearLayout.LayoutParams(Ui.dp(this, 8), 1));
        row.addView(searchBtn, new LinearLayout.LayoutParams(0, -2, 2));
        root.addView(row);
        Button browse = Ui.button(this, "🌐 Abrir en el navegador", Ui.GREEN); root.addView(browse);
        LinearLayout srow = new LinearLayout(this); srow.setGravity(Gravity.CENTER_VERTICAL);
        spin = new ProgressBar(this); spin.setVisibility(View.GONE); srow.addView(spin, new LinearLayout.LayoutParams(Ui.dp(this, 36), Ui.dp(this, 36)));
        status = Ui.text(this, "", 17, Ui.DARK, true); srow.addView(status, new LinearLayout.LayoutParams(0, -2, 1));
        stopBtn = Ui.button(this, "Parar", Ui.DARK); stopBtn.setVisibility(View.GONE); srow.addView(stopBtn, new LinearLayout.LayoutParams(-2, -2));
        root.addView(srow);
        results = new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL); root.addView(results);
        TextView ft = Ui.text(this, "⭐ Mis favoritos", 22, Ui.DARK, true); ft.setPadding(0, Ui.dp(this, 14), 0, 0); root.addView(ft);
        favBox = new LinearLayout(this); favBox.setOrientation(LinearLayout.VERTICAL); root.addView(favBox);
        Button addFav = Ui.button(this, "➕ Guardar enlace en favoritos", Ui.YELLOW); addFav.setTextColor(Ui.DARK); root.addView(addFav);
        setContentView(sv);

        paste.setOnClickListener(v -> { ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); if (cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) { CharSequence t = cm.getPrimaryClip().getItemAt(0).coerceToText(this); if (t != null) input.setText(t.toString().trim()); } });
        searchBtn.setOnClickListener(v -> search(input.getText().toString().trim()));
        stopBtn.setOnClickListener(v -> { if (current != null) current.cancel(); });
        browse.setOnClickListener(v -> openBrowser(input.getText().toString().trim()));
        addFav.setOnClickListener(v -> askAddFav());
        showFavs();
        Intent i = getIntent();
        if (Intent.ACTION_SEND.equals(i.getAction()) && i.getStringExtra(Intent.EXTRA_TEXT) != null) input.setText(i.getStringExtra(Intent.EXTRA_TEXT));
        buscarDesde(i);
    }
    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); setIntent(i); buscarDesde(i); }
    void buscarDesde(Intent i) {
        final String u = i.getStringExtra("buscar");
        if (u == null) return;
        i.removeExtra("buscar"); input.setText(u); h.post(() -> search(u));
    }

    void openBrowser(String u) { startActivityForResult(new Intent(this, BrowserActivity.class).putExtra("url", u.isEmpty() ? "https://web.archive.org/" : u), 1); }
    @Override protected void onActivityResult(int rq, int rc, Intent d) { if (rq == 1 && rc == RESULT_OK && d != null && d.getStringExtra("url") != null) { input.setText(d.getStringExtra("url")); search(d.getStringExtra("url")); } }

    // ---------- favourites ----------
    JSONArray loadFavs() {
        SharedPreferences sp = getSharedPreferences("favs", MODE_PRIVATE);
        try { if (sp.contains("list")) return new JSONArray(sp.getString("list", "[]")); } catch (Exception e) {}
        JSONArray a = new JSONArray();
        try { for (String[] f : DEFAULT_FAVS) a.put(new JSONObject().put("n", f[0]).put("u", f[1])); } catch (Exception e) {}
        saveFavs(a); return a;
    }
    void saveFavs(JSONArray a) { getSharedPreferences("favs", MODE_PRIVATE).edit().putString("list", a.toString()).apply(); }
    void showFavs() {
        favBox.removeAllViews();
        final JSONArray a = loadFavs();
        for (int i = 0; i < a.length(); i++) {
            final int idx = i; final JSONObject o = a.optJSONObject(i); if (o == null) continue;
            Button b = Ui.button(this, "⭐ " + o.optString("n"), 0xFFFFA65C);
            b.setTextSize(17); b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            b.setOnClickListener(v -> { input.setText(o.optString("u")); search(o.optString("u")); });
            b.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this).setTitle(o.optString("n")).setItems(new String[]{"🌐 Abrir en el navegador", "🗑️ Quitar de favoritos"}, (d, w) -> {
                    if (w == 0) openBrowser(o.optString("u"));
                    else { JSONArray n = new JSONArray(); for (int j = 0; j < a.length(); j++) if (j != idx) n.put(a.opt(j)); saveFavs(n); showFavs(); Toast.makeText(this, "Quitado 👍", Toast.LENGTH_SHORT).show(); }
                }).show();
                return true;
            });
            favBox.addView(b);
        }
        TextView hint = Ui.text(this, "Consejo: mantén pulsado un favorito para quitarlo o abrirlo en el navegador.", 14, Ui.TEXT, false);
        favBox.addView(hint);
    }
    void askAddFav() {
        final String u = input.getText().toString().trim();
        if (u.isEmpty()) { Toast.makeText(this, "Primero pega un enlace arriba 😊", Toast.LENGTH_SHORT).show(); return; }
        final EditText name = new EditText(this); name.setHint("Nombre (ej: Arguiñano marzo 2013)");
        new AlertDialog.Builder(this).setTitle("Guardar en favoritos ⭐").setView(name).setPositiveButton("Guardar", (d, w) -> {
            String n = name.getText().toString().trim(); if (n.isEmpty()) n = Extractor.prettyDate(Extractor.timestamp(u)) + " · " + Extractor.host(Extractor.original(u));
            JSONArray a = loadFavs(); try { a.put(new JSONObject().put("n", n).put("u", u)); } catch (Exception e) {}
            saveFavs(a); showFavs(); Toast.makeText(this, "¡Guardado! ⭐", Toast.LENGTH_SHORT).show();
        }).setNegativeButton("Cancelar", null).show();
    }

    // ---------- search ----------
    void search(final String url) {
        if (url.isEmpty()) { Toast.makeText(this, "Pega un enlace primero 😊", Toast.LENGTH_SHORT).show(); return; }
        if (current != null) current.cancel();
        lastUrl = url; count = 0; results.removeAllViews();
        ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(), 0);
        spin.setVisibility(View.VISIBLE); stopBtn.setVisibility(View.VISIBLE); searchBtn.setEnabled(false);
        status.setText("Buscando en la máquina del tiempo... ⏳");
        final Searcher s = new Searcher(new Searcher.Listener() {
            public void progress(final String m) { h.post(() -> { if (current != null && url.equals(lastUrl)) status.setText(m); }); }
            public void found(final Searcher.Result r) { h.post(() -> { if (url.equals(lastUrl)) addResult(r); }); }
        });
        current = s;
        new Thread(() -> {
            try { s.run(url); } catch (Throwable t) { }
            h.post(() -> { if (current == s) done(); });
        }).start();
    }
    void done() {
        spin.setVisibility(View.GONE); stopBtn.setVisibility(View.GONE); searchBtn.setEnabled(true); current = null;
        if (count > 0) { status.setText("¡Genial! He encontrado " + count + (count == 1 ? " vídeo 🎉" : " vídeos 🎉")); return; }
        status.setText("");
        LinearLayout card = card();
        card.addView(Ui.text(this, "🌟 Esta vez no he encontrado vídeos guardados", 19, Ui.DARK, true));
        card.addView(Ui.text(this, "¡No pasa nada! Prueba con otra fecha o captura de la misma página, o ábrela en el navegador y pulsa el reproductor: yo vigilo y te aviso si aparece un vídeo 😊", 16, Ui.TEXT, false));
        Button b = Ui.button(this, "🌐 Abrir la página en el navegador", Ui.GREEN);
        b.setOnClickListener(v -> openBrowser(lastUrl)); card.addView(b);
        final String cal = "https://web.archive.org/web/*/" + Extractor.original(lastUrl);
        Button c = Ui.button(this, "📅 Ver otras fechas", Ui.ORANGE);
        c.setOnClickListener(v -> openBrowser(cal)); card.addView(c);
        results.addView(card);
    }
    LinearLayout card() {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 14); c.setPadding(p, p, p, p); c.setBackground(Ui.round(Ui.CARD, Ui.dp(this, 18))); c.setLayoutParams(Ui.lp(this, 6));
        if (Build.VERSION.SDK_INT >= 21) c.setElevation(Ui.dp(this, 2));
        return c;
    }
    void addResult(final Searcher.Result r) {
        count++;
        LinearLayout c = card();
        String ext = Extractor.ext(r.orig).toUpperCase();
        c.addView(Ui.text(this, "🎞️ " + r.name(), 17, Ui.DARK, true));
        StringBuilder info = new StringBuilder("📅 " + Extractor.prettyDate(r.ts) + "   🏷️ " + ext);
        if (r.size != null) info.append("   💾 ~").append(r.size);
        info.append("\n✅ ").append(r.status != null && r.status.matches("\\d+") ? "Estado " + r.status : r.status).append(" · ").append(r.source);
        c.addView(Ui.text(this, info.toString(), 14, Ui.TEXT, false));
        c.setOnClickListener(v -> Ui.videoActions(this, r.orig, r.ts, r.name()));
        results.addView(c);
    }
}
