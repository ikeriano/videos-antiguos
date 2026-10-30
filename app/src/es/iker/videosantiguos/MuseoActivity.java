package es.iker.videosantiguos;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;

/** Portada del museo: una tarjeta por programa. */
public class MuseoActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Ui.DARK);
        ScrollView sv = new ScrollView(this); sv.setBackgroundColor(Ui.BG);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 16); root.setPadding(p, p, p, p); sv.addView(root);
        Button back = Ui.button(this, "⬅️ Volver", Ui.DARK); back.setOnClickListener(v -> finish()); root.addView(back);
        TextView t = Ui.text(this, "🏛️ Museo de la tele", 28, Ui.DARK, true); t.setGravity(Gravity.CENTER); root.addView(t);
        JSONObject d = Museo.get(this);
        TextView s = Ui.text(this, d.optString("intro", "Fichas de programas guardados en la máquina del tiempo."), 16, Ui.TEXT, false); s.setGravity(Gravity.CENTER); root.addView(s);
        JSONArray a = d.optJSONArray("programas");
        for (int i = 0; a != null && i < a.length(); i++) {
            final JSONObject pr = a.optJSONObject(i);
            LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
            c.setPadding(p, p, p, p); c.setBackground(Ui.round((int) Long.parseLong(pr.optString("color", "FFFF8A3D"), 16), Ui.dp(this, 20))); c.setLayoutParams(Ui.lp(this, 8));
            c.setElevation(Ui.dp(this, 3));
            c.addView(Ui.text(this, pr.optString("emoji") + " " + pr.optString("nombre"), 22, 0xFFFFFFFF, true));
            c.addView(Ui.text(this, pr.optString("descripcion"), 15, 0xFFFFFFFF, false));
            JSONArray f = pr.optJSONArray("fichas");
            c.addView(Ui.text(this, "🗂️ " + (f == null ? 0 : f.length()) + " fichas  ·  " + pr.optString("años"), 16, 0xFFFFFFFF, true));
            c.setOnClickListener(v -> startActivity(new Intent(this, FichasActivity.class).putExtra("id", pr.optString("id"))));
            root.addView(c);
        }
        JSONArray pub = d.optJSONArray("publicos");
        if (pub != null && pub.length() > 0) {
            Button pb = Ui.button(this, "📺 Vídeos públicos encontrados (" + pub.length() + ")", Ui.GREEN);
            pb.setOnClickListener(v -> FichaActivity.listaEnlaces(this, "📺 Vídeos públicos", pub));
            root.addView(pb);
        }
        root.addView(Ui.text(this, "Todos los datos vienen de páginas reales guardadas en web.archive.org 😊", 13, Ui.TEXT, false));
        setContentView(sv);
    }
}
