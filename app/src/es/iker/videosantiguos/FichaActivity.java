package es.iker.videosantiguos;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;

/** Ficha de un episodio: portada, fecha, recetas y botones. */
public class FichaActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Ui.DARK);
        final JSONObject o;
        try { o = new JSONObject(getIntent().getStringExtra("ficha")); } catch (Exception e) { finish(); return; }
        ScrollView sv = new ScrollView(this); sv.setBackgroundColor(Ui.BG);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 16); root.setPadding(p, p, p, p); sv.addView(root);
        Button back = Ui.button(this, "⬅️ Volver", Ui.DARK); back.setOnClickListener(v -> finish()); root.addView(back);
        ImageView img = new ImageView(this); img.setScaleType(ImageView.ScaleType.CENTER_CROP); img.setBackground(Ui.round(0xFFFFE0B2, Ui.dp(this, 16)));
        img.setClipToOutline(true);
        LinearLayout.LayoutParams il = new LinearLayout.LayoutParams(-1, Ui.dp(this, 200)); il.setMargins(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        root.addView(img, il);
        Museo.imagen(this, img, o.optString("imagen"), Museo.icono(this));
        root.addView(Ui.text(this, getIntent().getStringExtra("nombre"), 15, Ui.ORANGE, true));
        root.addView(Ui.text(this, o.optString("titulo"), 22, Ui.DARK, true));
        StringBuilder s = new StringBuilder("📅 Emitido el " + Museo.fecha(o.optString("fecha")));
        if (o.optInt("partes") > 0) s.append("\n🎞️ ").append(o.optInt("partes")).append(o.optInt("partes") == 1 ? " parte" : " partes de vídeo");
        root.addView(Ui.text(this, s.toString(), 17, Ui.TEXT, false));
        if (!o.optString("descripcion").isEmpty()) root.addView(Ui.text(this, o.optString("descripcion"), 16, Ui.TEXT, false));
        JSONArray rec = o.optJSONArray("recetas");
        if (rec != null && rec.length() > 0) {
            StringBuilder r = new StringBuilder("🍳 Recetas:");
            for (int i = 0; i < rec.length(); i++) r.append("\n• ").append(rec.optString(i));
            root.addView(Ui.text(this, r.toString(), 17, Ui.DARK, false));
        }
        final JSONArray pags = o.optJSONArray("paginas");
        if (pags != null && pags.length() > 0) {
            Button g = Ui.button(this, "🌐 Ver página guardada", Ui.GREEN);
            g.setOnClickListener(v -> {
                if (pags.length() == 1) abrir(pags.optJSONObject(0).optString("u"));
                else { String[] n = new String[pags.length()]; for (int i = 0; i < n.length; i++) n[i] = pags.optJSONObject(i).optString("t");
                    new AlertDialog.Builder(this).setTitle("¿Qué página?").setItems(n, (d, w) -> abrir(pags.optJSONObject(w).optString("u"))).show(); }
            });
            root.addView(g);
            Button bs = Ui.button(this, "🔎 Buscar vídeo", Ui.ORANGE);
            bs.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class).putExtra("buscar", pags.optJSONObject(0).optString("u"))));
            root.addView(bs);
        }
        final JSONArray en = o.optJSONArray("enlaces");
        if (en != null && en.length() > 0) {
            Button y = Ui.button(this, "📺 Ver en YouTube/otros (" + en.length() + ")", 0xFFE53935);
            y.setOnClickListener(v -> listaEnlaces(this, "📺 Vídeos públicos", en));
            root.addView(y);
        }
        root.addView(Ui.text(this, "💡 Las fichas vienen de páginas reales guardadas en la Wayback Machine.", 13, Ui.TEXT, false));
        setContentView(sv);
    }
    void abrir(String u) { startActivity(new Intent(this, BrowserActivity.class).putExtra("url", u)); }

    static void listaEnlaces(final Activity a, String titulo, final JSONArray en) {
        String[] n = new String[en.length()];
        for (int i = 0; i < n.length; i++) { JSONObject e = en.optJSONObject(i); n[i] = e.optString("t") + (e.optString("info").isEmpty() ? "" : "\n   " + e.optString("info")); }
        new AlertDialog.Builder(a).setTitle(titulo).setItems(n, (d, w) -> Museo.abrirFuera(a, en.optJSONObject(w).optString("u"))).setNegativeButton("Cerrar", null).show();
    }
}
