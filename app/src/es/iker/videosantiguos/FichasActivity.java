package es.iker.videosantiguos;

import android.app.*;
import android.content.*;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.json.*;

/** Lista de fichas de un programa (ListView reciclado, fluido en móviles sencillos). */
public class FichasActivity extends Activity {
    final List<JSONObject> todas = new ArrayList<>(), vis = new ArrayList<>();
    BaseAdapter ad; String año = ""; String texto = "";

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Ui.DARK);
        final JSONObject pr = Museo.programa(this, getIntent().getStringExtra("id"));
        if (pr == null) { finish(); return; }
        JSONArray f = pr.optJSONArray("fichas");
        TreeSet<String> años = new TreeSet<>();
        for (int i = 0; f != null && i < f.length(); i++) { todas.add(f.optJSONObject(i)); String y = f.optJSONObject(i).optString("fecha"); if (y.length() >= 4) años.add(y.substring(0, 4)); }
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Ui.BG);
        int p = Ui.dp(this, 10); root.setPadding(p, p, p, 0);
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        Button back = Ui.button(this, "⬅️", Ui.DARK); back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(-2, -2));
        TextView t = Ui.text(this, " " + pr.optString("emoji") + " " + pr.optString("nombre"), 20, Ui.DARK, true);
        top.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(top);
        EditText q = new EditText(this); q.setHint("🔎 Buscar receta, fecha o palabra"); q.setSingleLine(true); q.setTextSize(16);
        q.setBackground(Ui.round(0xFFFFFFFF, Ui.dp(this, 14))); q.setPadding(p, p, p, p); q.setTextColor(Ui.TEXT);
        root.addView(q, Ui.lp(this, 4));
        HorizontalScrollView hs = new HorizontalScrollView(this); hs.setHorizontalScrollBarEnabled(false);
        final LinearLayout chips = new LinearLayout(this); hs.addView(chips);
        List<String> ys = new ArrayList<>(); ys.add("Todos"); ys.addAll(años);
        for (final String y : ys) {
            Button c = Ui.button(this, y, Ui.ORANGE); c.setTextSize(15); c.setMinHeight(Ui.dp(this, 44)); c.setMinimumHeight(Ui.dp(this, 44));
            LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-2, -2); l.setMargins(0, 0, Ui.dp(this, 6), 0);
            c.setOnClickListener(v -> { año = y.equals("Todos") ? "" : y; marcar(chips, (Button) v); filtrar(); });
            chips.addView(c, l);
        }
        root.addView(hs);
        final TextView cuenta = Ui.text(this, "", 14, Ui.TEXT, false); root.addView(cuenta);
        ListView lv = new ListView(this); lv.setDivider(null); lv.setDividerHeight(0); lv.setCacheColorHint(0); lv.setSelector(android.R.color.transparent);
        root.addView(lv, new LinearLayout.LayoutParams(-1, 0, 1));
        ad = new BaseAdapter() {
            public int getCount() { return vis.size(); }
            public Object getItem(int i) { return vis.get(i); }
            public long getItemId(int i) { return i; }
            public View getView(int i, View cv, ViewGroup parent) {
                Holder h;
                if (cv == null) {
                    LinearLayout c = new LinearLayout(FichasActivity.this); c.setGravity(Gravity.CENTER_VERTICAL);
                    int q = Ui.dp(FichasActivity.this, 10); c.setPadding(q, q, q, q);
                    c.setBackground(Ui.round(Ui.CARD, Ui.dp(FichasActivity.this, 16)));
                    AbsListView.LayoutParams l = new AbsListView.LayoutParams(-1, -2); c.setLayoutParams(l);
                    h = new Holder(); h.img = new ImageView(FichasActivity.this); h.img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    c.addView(h.img, new LinearLayout.LayoutParams(Ui.dp(FichasActivity.this, 96), Ui.dp(FichasActivity.this, 64)));
                    LinearLayout tx = new LinearLayout(FichasActivity.this); tx.setOrientation(LinearLayout.VERTICAL); tx.setPadding(q, 0, 0, 0);
                    h.t = Ui.text(FichasActivity.this, "", 16, Ui.DARK, true); h.t.setMaxLines(2); h.t.setEllipsize(TextUtils.TruncateAt.END);
                    h.s = Ui.text(FichasActivity.this, "", 13, Ui.TEXT, false); h.s.setMaxLines(2); h.s.setEllipsize(TextUtils.TruncateAt.END);
                    tx.addView(h.t); tx.addView(h.s); c.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
                    FrameLayout wrap = new FrameLayout(FichasActivity.this); wrap.setPadding(0, Ui.dp(FichasActivity.this, 4), 0, Ui.dp(FichasActivity.this, 4));
                    wrap.addView(c); wrap.setTag(h); cv = wrap;
                } else h = (Holder) cv.getTag();
                JSONObject o = vis.get(i);
                h.t.setText(o.optString("titulo"));
                StringBuilder s = new StringBuilder("📅 " + Museo.fecha(o.optString("fecha")));
                if (o.optInt("partes") > 0) s.append("  ·  🎞️ ").append(o.optInt("partes")).append(o.optInt("partes") == 1 ? " parte" : " partes");
                if (o.optJSONArray("enlaces") != null && o.optJSONArray("enlaces").length() > 0) s.append("  ·  📺 vídeo público");
                h.s.setText(s);
                Museo.imagen(FichasActivity.this, h.img, o.optString("imagen"), Museo.icono(FichasActivity.this));
                return cv;
            }
        };
        lv.setAdapter(ad);
        lv.setOnItemClickListener((pa, v, i, id) -> startActivity(new Intent(this, FichaActivity.class).putExtra("prog", pr.optString("id")).putExtra("ficha", vis.get(i).toString()).putExtra("nombre", pr.optString("nombre"))));
        q.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b2, int c) {}
            public void onTextChanged(CharSequence s, int a, int b2, int c) {}
            public void afterTextChanged(Editable e) { texto = e.toString().toLowerCase(Locale.ROOT).trim(); filtrar(); }
        });
        this.cuenta = cuenta;
        if (chips.getChildCount() > 0) marcar(chips, (Button) chips.getChildAt(0));
        filtrar();
        setContentView(root);
    }
    TextView cuenta;
    static class Holder { ImageView img; TextView t, s; }
    void marcar(LinearLayout chips, Button sel) { for (int i = 0; i < chips.getChildCount(); i++) chips.getChildAt(i).setAlpha(chips.getChildAt(i) == sel ? 1f : 0.55f); }
    void filtrar() {
        vis.clear();
        for (JSONObject o : todas) {
            if (!año.isEmpty() && !o.optString("fecha").startsWith(año)) continue;
            if (!texto.isEmpty() && !(o.optString("titulo") + " " + o.optString("descripcion") + " " + o.optString("fecha") + " " + Museo.fecha(o.optString("fecha"))).toLowerCase(Locale.ROOT).contains(texto)) continue;
            vis.add(o);
        }
        if (ad != null) ad.notifyDataSetChanged();
        if (cuenta != null) cuenta.setText(vis.size() + (vis.size() == 1 ? " ficha" : " fichas") + " · toca una para verla");
    }
}
