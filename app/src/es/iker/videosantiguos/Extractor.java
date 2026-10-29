package es.iker.videosantiguos;

import java.net.URI;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.*;

/** Pure Java helpers (no Android) so they can be tested on the JVM. */
public class Extractor {
    public static final String WB = "https://web.archive.org/web/";
    static final Pattern WAYBACK = Pattern.compile("^(?:(?:https?:)?//)?(?:web\\.)?archive\\.org/web/(\\d{1,14})([a-z]{2}_)?/+(.+)$", Pattern.CASE_INSENSITIVE);
    static final String MEDIA_EXT = "mp4|m4v|flv|f4v|m3u8|f4m|mp3|webm|3gp|mov";
    static final Pattern MEDIA = Pattern.compile("(?:https?:)?//[^\\s\"'<>()\\\\]+?\\.(?:" + MEDIA_EXT + ")(?:\\?[^\\s\"'<>]*)?(?=[\\s\"'<>&;,)\\]}]|$)", Pattern.CASE_INSENSITIVE);
    static final Pattern REL_MEDIA = Pattern.compile("[\"'=]((?:/|\\.\\./|[\\w-]+/)[^\\s\"'<>:]*?\\.(?:" + MEDIA_EXT + "))(?:\\?[^\\s\"'<>]*)?[\"'&]", Pattern.CASE_INSENSITIVE);
    static final Pattern CONFIG = Pattern.compile("(?:https?:)?(?://)[^\\s\"'<>()]*?\\.(?:xml|json|smil)(?:\\?[^\\s\"'<>]*)?(?=[\\s\"'<>&;,)]|$)", Pattern.CASE_INSENSITIVE);
    static final Pattern PARAM = Pattern.compile("(?:urlVideo|xml|file|config|configUrl|playlist|data-(?:src|video|url|xml|config|file|mp4|media)[\\w-]*|src|video|url|mediaUrl|contentUrl)\\s*[=:]\\s*[\"']?([^\"'\\s<>&,}]+)", Pattern.CASE_INSENSITIVE);
    static final Pattern HEX24 = Pattern.compile("(?<![0-9a-f])([0-9a-f]{24})(?![0-9a-f])");
    static final Pattern CDX_ROW = Pattern.compile("\\[\"([^\"]*)\",\\s*\"([^\"]*)\",\\s*\"([^\"]*)\",\\s*\"([^\"]*)\",\\s*\"([^\"]*)\",\\s*\"([^\"]*)\",\\s*\"([^\"]*)\"\\]");

    public static boolean isWayback(String u) { return u != null && WAYBACK.matcher(u.trim()).matches(); }
    public static String timestamp(String u) { Matcher m = WAYBACK.matcher(u.trim()); return m.matches() ? m.group(1) : null; }

    /** Original URL (without Wayback prefix), with scheme. */
    public static String original(String u) {
        u = u.trim();
        for (int i = 0; i < 4; i++) { Matcher m = WAYBACK.matcher(u); if (!m.matches()) break; u = m.group(3); }
        if (u.startsWith("//")) u = "http:" + u;
        u = u.replaceFirst("^(https?):/(?!/)", "$1://");
        if (!u.matches("(?i)^https?://.*")) u = "http://" + u;
        return u;
    }

    /** Raw (unmodified original bytes) Wayback URL: inserts id_ after the timestamp. */
    public static String toRaw(String u) {
        String ts = timestamp(u);
        if (ts == null) ts = "2013";
        return WB + ts + "id_/" + original(u);
    }
    public static String raw(String ts, String orig) { return WB + (ts == null ? "2013" : ts) + "id_/" + orig; }
    public static String normal(String ts, String orig) { return WB + (ts == null ? "2013" : ts) + "/" + orig; }

    static String unescape(String s) {
        s = s.replace("\\/", "/").replace("\\u002F", "/").replace("\\u002f", "/").replace("&amp;", "&").replace("\\u0026", "&");
        if (s.matches("(?i).*%2F.*") || s.matches("(?i)^https?%3A.*")) { try { s = URLDecoder.decode(s, "UTF-8"); } catch (Exception e) {} }
        return s;
    }

    static String resolve(String base, String u) {
        try {
            u = unescape(u.trim());
            if (isWayback(u) || u.startsWith("/web/")) return original(u.startsWith("/web/") ? "https://web.archive.org" + u : u);
            if (u.startsWith("//")) return "http:" + u;
            if (u.matches("(?i)^https?://.*")) return u;
            return new URI(base).resolve(u.replace(" ", "%20")).toString();
        } catch (Exception e) { return null; }
    }

    public static boolean isMediaUrl(String u) { return u != null && u.toLowerCase(Locale.ROOT).replaceAll("[?#].*$", "").matches(".*\\.(" + MEDIA_EXT + ")$"); }
    public static boolean isConfigUrl(String u) { return u != null && u.toLowerCase(Locale.ROOT).replaceAll("[?#].*$", "").matches(".*\\.(xml|json|smil)$"); }
    static boolean junkConfig(String u) {
        String l = u.toLowerCase(Locale.ROOT);
        return l.contains("crossdomain.xml") || l.contains("sitemap") || l.contains("/rss") || l.contains("oembed") || l.contains("manifest.json") || l.contains("opensearch") || l.contains("w3.org") || l.contains("facebook") || l.contains("twitter") || l.contains("google");
    }

    /** Media URLs (original form) found in a page/config. */
    public static LinkedHashSet<String> extractMedia(String text, String baseOrig) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (text == null) return out;
        String t = text.replace("\\/", "/").replace("&amp;", "&");
        Matcher m = MEDIA.matcher(t);
        while (m.find()) add(out, resolve(baseOrig, m.group()), true);
        m = REL_MEDIA.matcher(t);
        while (m.find()) add(out, resolve(baseOrig, m.group(1)), true);
        m = PARAM.matcher(t);
        while (m.find()) { String r = resolve(baseOrig, m.group(1)); if (r != null && isMediaUrl(r)) add(out, r, true); }
        // urls inside other urls (e.g. player.swf?file=http%3A...mp4)
        Matcher e = Pattern.compile("(?i)https?%3A%2F%2F[^\"'\\s&<>]+").matcher(t);
        while (e.find()) { String r = resolve(baseOrig, e.group()); if (isMediaUrl(r)) add(out, r, true); }
        return out;
    }

    /** Player config candidates (.xml/.json, flashvars xml=, urlVideo, data-*). */
    public static LinkedHashSet<String> extractConfigs(String text, String baseOrig) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (text == null) return out;
        String t = text.replace("\\/", "/").replace("&amp;", "&");
        Matcher m = CONFIG.matcher(t);
        while (m.find()) { String r = resolve(baseOrig, m.group()); if (r != null && !junkConfig(r)) add(out, r, false); }
        m = PARAM.matcher(t);
        while (m.find()) {
            String v = m.group(1), r = resolve(baseOrig, v);
            if (r == null || junkConfig(r) || isMediaUrl(r)) continue;
            String key = m.group().toLowerCase(Locale.ROOT);
            if (isConfigUrl(r) || key.startsWith("xml") || key.startsWith("urlvideo") || key.startsWith("config") || key.startsWith("playlist") || key.startsWith("data-xml") || key.startsWith("data-config")) add(out, r, false);
        }
        return out;
    }

    static void add(Set<String> s, String u, boolean media) {
        if (u == null || u.length() < 10 || u.length() > 1500) return;
        if (!u.matches("(?i)^https?://[^/]+\\..+")) return;
        if (media && !isMediaUrl(u)) return;
        s.add(u);
    }

    /** Programme slug from antena3 / atresplayer / hogarutil URLs, e.g. karlos-arguinano, ahora-caigo. */
    public static String slug(String orig) {
        String p = orig.replaceFirst("(?i)^https?://[^/]+", "").replaceAll("[?#].*", "");
        String[] seg = p.split("/");
        for (int i = 0; i < seg.length - 1; i++) {
            if (seg[i].matches("videos|programas|series|tv")) {
                String s = seg[i + 1].replaceAll("\\.html?$", "");
                if (s.equals("programas") && i + 2 < seg.length) s = seg[i + 2].replaceAll("\\.html?$", "");
                if (s.length() > 2) return s;
            }
        }
        if (seg.length > 1) { String s = seg[seg.length - 1].replaceAll("\\.html?$", ""); if (s.matches("[a-z][a-z-]{3,}") && !s.equals("index")) return s; }
        return null;
    }
    /** Most specific word of the slug (e.g. 'arguinano' from 'karlos-arguinano-cocina'). */
    public static String keyword(String slug) {
        if (slug == null) return null;
        String best = null;
        for (String w : slug.split("-")) if (w.length() >= 5 && !w.matches("videos|programa|programas|cocina|temporada|capitulo") && (best == null || w.length() > best.length())) best = w;
        return best == null ? slug : best;
    }
    public static String atresId(String orig) {
        if (!orig.toLowerCase(Locale.ROOT).contains("atresplayer")) return null;
        Matcher m = HEX24.matcher(orig); String last = null; while (m.find()) last = m.group(1); return last;
    }
    public static String host(String orig) { try { return new URI(orig).getHost(); } catch (Exception e) { return null; } }
    /** host + directory path prefix for CDX prefix search. */
    public static String prefix(String orig) {
        String s = orig.replaceFirst("(?i)^https?://", "").replaceAll("[?#].*", "");
        int i = s.lastIndexOf('/');
        if (i > 0 && i < s.length() - 1) s = s.substring(0, i + 1);
        return s;
    }

    /** CDX json rows -> {timestamp, original, mimetype, status, length}. */
    public static List<String[]> parseCdx(String json) {
        List<String[]> out = new ArrayList<>();
        if (json == null) return out;
        Matcher m = CDX_ROW.matcher(json);
        while (m.find()) { if (m.group(2).equals("timestamp")) continue; out.add(new String[]{m.group(2), m.group(3), m.group(4), m.group(5), m.group(7)}); }
        return out;
    }

    public static String niceName(String orig, String ts) {
        String f = orig.replaceAll("[?#].*", "");
        f = f.substring(f.lastIndexOf('/') + 1);
        try { f = URLDecoder.decode(f, "UTF-8"); } catch (Exception e) {}
        f = f.replaceAll("[^\\w.\\-áéíóúñÁÉÍÓÚÑ]+", "_");
        if (f.length() > 80) f = f.substring(f.length() - 80);
        String d = ts != null && ts.length() >= 8 ? ts.substring(0, 8) + "_" : "";
        if (f.isEmpty()) f = "video.mp4";
        return d + f;
    }
    public static String prettyDate(String ts) {
        if (ts == null || ts.length() < 8) return "fecha desconocida";
        String[] mes = {"ene","feb","mar","abr","may","jun","jul","ago","sep","oct","nov","dic"};
        try { int mo = Integer.parseInt(ts.substring(4, 6)); return Integer.parseInt(ts.substring(6, 8)) + " " + mes[mo - 1] + " " + ts.substring(0, 4); } catch (Exception e) { return ts; }
    }
    public static String prettySize(String len) {
        try { long l = Long.parseLong(len); if (l <= 0) return null; if (l > 1048576) return String.format(Locale.ROOT, "%.1f MB", l / 1048576.0); return (l / 1024) + " KB"; } catch (Exception e) { return null; }
    }
    public static String ext(String u) { String l = u.toLowerCase(Locale.ROOT).replaceAll("[?#].*", ""); int i = l.lastIndexOf('.'); return i < 0 ? "" : l.substring(i + 1); }
}
