package es.iker.videosantiguos;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

public class PlayerActivity extends Activity {
    VideoView v; TextView msg; int pos;
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        final String url = getIntent().getStringExtra("url"), orig = getIntent().getStringExtra("orig"), ts = getIntent().getStringExtra("ts");
        FrameLayout f = new FrameLayout(this); f.setBackgroundColor(Color.BLACK);
        v = new VideoView(this);
        f.addView(v, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        msg = Ui.text(this, "Cargando el vídeo... 🍿", 20, Color.WHITE, true); msg.setGravity(Gravity.CENTER);
        f.addView(msg, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        Button dl = Ui.button(this, "⬇️ Descargar", Ui.ORANGE);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END); lp.setMargins(16, 16, 16, 16);
        f.addView(dl, lp);
        dl.setOnClickListener(x -> Ui.download(this, url, Extractor.niceName(orig != null ? orig : url, ts)));
        setContentView(f);
        MediaController mc = new MediaController(this); mc.setAnchorView(v); v.setMediaController(mc);
        v.setOnPreparedListener(mp -> { msg.setVisibility(View.GONE); if (pos > 0) v.seekTo(pos); v.start(); });
        v.setOnErrorListener((mp, w, e) -> { msg.setVisibility(View.VISIBLE); msg.setText("Este vídeo no se puede ver aquí, ¡pero puedes descargarlo! 💾"); return true; });
        if (b != null) pos = b.getInt("pos");
        v.setVideoURI(android.net.Uri.parse(url));
    }
    @Override protected void onSaveInstanceState(Bundle o) { super.onSaveInstanceState(o); o.putInt("pos", v.getCurrentPosition()); }
    @Override protected void onPause() { super.onPause(); pos = v.getCurrentPosition(); v.pause(); }
}
