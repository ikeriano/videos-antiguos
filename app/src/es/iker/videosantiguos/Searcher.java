package es.iker.videosantiguos;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** On-device search engine. Plain Java (HttpURLConnection). Run on a background thread. */
public class Searcher {
    public static class Result {
        public String orig, ts, mime, status, size, source;
        public String playUrl() { return Extractor.raw(ts, orig); }
        public String name() { return Extractor.niceName(orig, ts); }
    }
    public interface Listener { void progress(String msg); void found(Result r); }

    final Listener l; volatile boolean cancelled;
    final LinkedHashMap<String, Result> results = new LinkedHashMap<>();
    final Set<String> fetchedConfigs = new HashSet<>();
    public Searcher(Listener l) { this.l = l; }
    public void cancel() { cancelled = true; }

    public static String get(String url, int maxBytes) throws IOException {
        IOException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try { return getOnce(url, maxBytes); }
            catch (FileNotFoundException e) { throw e; }
            catch (IOException e) { last = e; try { Thread.sleep(1500L * (attempt + 1)); } catch (InterruptedException ie) { break; } }
        }
        throw last;
    }
    static String getOnce(String url, int maxBytes) throws IOException {
        String u = url;
        for (int hop = 0; hop < 8; hop++) {
            HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
            c.setConnectTimeout(30000); c.setReadTimeout(60000);
            c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Mobile Safari/537.36 VideosAntiguos/1.0");
            c.setRequestProperty("Accept-Encoding", "gzip");
            int code = c.getResponseCode();
            if (code >= 300 && code < 400) { String loc = c.getHeaderField("Location"); c.disconnect(); if (loc == null) throw new IOException("redirect"); u = new URL(new URL(u), loc).toString(); continue; }
            if (code == 404 || code == 403 || code == 410) { c.disconnect(); throw new FileNotFoundException("HTTP " + code); }
            if (code >= 400) { c.disconnect(); throw new IOException("HTTP " + code); }
            InputStream in = c.getInputStream();
            if ("gzip".equalsIgnoreCase(c.getContentEncoding())) in = new GZIPInputStream(in);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] b = new byte[16384]; int n;
            while ((n = in.read(b)) > 0 && bo.size() < maxBytes) bo.write(b, 0, n);
            in.close(); c.disconnect();
            return bo.toString("UTF-8");
        }
        throw new IOException("too many redirects");
    }

    void add(String orig, String ts, String mime, String status, String size, String source) {
        if (!Extractor.isMediaUrl(orig) && (mime == null || !mime.matches("(?i)(video|audio)/.*"))) return;
        Result r = results.get(orig);
        if (r != null) { if (r.size == null) r.size = size; return; }
        r = new Result(); r.orig = orig; r.ts = ts; r.mime = mime; r.status = status; r.size = size; r.source = source;
        results.put(orig, r);
        l.found(r);
    }

    static String enc(String s) { try { return URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { return s; } }
    static final String CDX = "https://web.archive.org/cdx/search/cdx?output=json&limit=200&collapse=urlkey&url=";

    List<String[]> cdx(String query) {
        try { return Extractor.parseCdx(get(CDX + query, 2_000_000)); } catch (Exception e) { return new ArrayList<>(); }
    }

    public List<Result> run(String input) {
        String orig = Extractor.original(input);
        String ts = Extractor.timestamp(input);
        if (ts == null) ts = "2013";
        // 1. page
        l.progress("Buscando en la máquina del tiempo... ⏳");
        String html = null;
        try { html = get(Extractor.toRaw(input), 4_000_000); }
        catch (Exception e) { l.progress("La página tarda en responder, sigo buscando por otros caminos... 😊"); }
        if (html != null && !cancelled) scan(html, orig, ts, 0);
        if (cancelled) return list();
        // 2. CDX prefix (same folder)
        l.progress("Mirando el archivo de vídeos guardados... 📼");
        String pre = Extractor.prefix(orig);
        for (String[] r : cdx(enc(pre) + "&matchType=prefix&filter=mimetype:video/.*")) add(r[1], r[0], r[2], r[3], Extractor.prettySize(r[4]), "Archivo (misma carpeta)");
        if (cancelled) return list();
        String host = Extractor.host(orig); if (host == null) host = "";
        String slug = Extractor.slug(orig), kw = Extractor.keyword(slug);
        boolean a3 = host.contains("antena3") || host.contains("atresplayer") || host.contains("hogarutil");
        // 3. host-wide by programme keyword
        if (kw != null) {
            l.progress("Buscando vídeos de \u00ab" + kw + "\u00bb en toda la web... 🔎");
            List<String> domains = new ArrayList<>();
            String base = host.replaceFirst("^(www|m)\\.", "");
            domains.add(base);
            if (a3 && !domains.contains("antena3.com")) domains.add("antena3.com");
            if (a3 && !domains.contains("atresplayer.com")) domains.add("atresplayer.com");
            if (a3) domains.add("atresmedia.com");
            for (String d : domains) {
                if (cancelled) break;
                for (String[] r : cdx(enc(d) + "&matchType=domain&filter=mimetype:video/.*&filter=" + enc("original:.*" + kw + ".*"))) add(r[1], r[0], r[2], r[3], Extractor.prettySize(r[4]), "Archivo de " + d);
                for (String[] r : cdx(enc(d) + "&matchType=domain&filter=" + enc("original:.*" + kw + ".*\\.(mp4|flv|m3u8|f4m|mp3)$"))) add(r[1], r[0], r[2], r[3], Extractor.prettySize(r[4]), "Archivo de " + d);
            }
        }
        // 4. atresplayer episode id
        String id = Extractor.atresId(orig);
        if (id != null && !cancelled) {
            l.progress("Buscando el episodio en Atresplayer... 🎬");
            for (String d : new String[]{"api.atresplayer.com", "atresplayer.com"}) {
                for (String[] r : cdx(enc(d) + "&matchType=domain&filter=" + enc("original:.*" + id + ".*"))) {
                    if (cancelled) break;
                    if (Extractor.isMediaUrl(r[1]) || r[2].startsWith("video/")) add(r[1], r[0], r[2], r[3], Extractor.prettySize(r[4]), "Atresplayer");
                    else if (r[2].contains("json") || r[2].contains("xml") || r[1].contains("/player/") || r[1].contains("api.")) fetchConfig(r[1], r[0], 1);
                }
            }
        }
        return list();
    }

    void scan(String text, String baseOrig, String ts, int depth) {
        for (String m : Extractor.extractMedia(text, baseOrig)) add(m, ts, null, "Encontrado en la página", null, depth == 0 ? "Página" : "Configuración del reproductor");
        if (depth >= 2) return;
        for (String c : Extractor.extractConfigs(text, baseOrig)) { if (cancelled) return; fetchConfig(c, ts, depth + 1); }
    }
    void fetchConfig(String orig, String ts, int depth) {
        if (fetchedConfigs.size() > 25 || !fetchedConfigs.add(orig)) return;
        l.progress("Abriendo la configuración del reproductor... 🔧");
        try { scan(get(Extractor.raw(ts, orig), 2_000_000), orig, ts, depth); } catch (Exception e) { }
    }
    List<Result> list() { return new ArrayList<>(results.values()); }
}
