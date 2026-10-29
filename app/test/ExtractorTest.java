import es.iker.videosantiguos.*;
import java.util.*;
public class ExtractorTest {
    static int fail = 0, ok = 0;
    static void eq(Object a, Object b, String n) { if (Objects.equals(a, b)) ok++; else { fail++; System.out.println("FAIL " + n + "\n  got:  " + a + "\n  want: " + b); } }
    static void has(Collection<String> c, String x, String n) { if (c.contains(x)) ok++; else { fail++; System.out.println("FAIL " + n + " missing " + x + " in " + c); } }
    public static void main(String[] a) {
        String u1 = "https://web.archive.org/web/20130211045623/http://www.antena3.com/videos/karlos-arguinano/2013-febrero-7-2013020700029.html";
        eq(Extractor.toRaw(u1), "https://web.archive.org/web/20130211045623id_/http://www.antena3.com/videos/karlos-arguinano/2013-febrero-7-2013020700029.html", "raw1");
        eq(Extractor.toRaw("https://web.archive.org/web/20130211045623im_/http://x.com/a.mp4"), "https://web.archive.org/web/20130211045623id_/http://x.com/a.mp4", "raw im_");
        eq(Extractor.toRaw("web.archive.org/web/2013/http://x.com/"), "https://web.archive.org/web/2013id_/http://x.com/", "raw noscheme");
        eq(Extractor.toRaw("http://www.antena3.com/programas/karlos-arguinano"), "https://web.archive.org/web/2013id_/http://www.antena3.com/programas/karlos-arguinano", "raw plain");
        eq(Extractor.original("https://web.archive.org/web/20130211045623/http:/www.antena3.com/x"), "http://www.antena3.com/x", "single slash");
        eq(Extractor.slug(Extractor.original(u1)), "karlos-arguinano", "slug1");
        eq(Extractor.slug("http://www.antena3.com/videos/karlos-arguinano.html"), "karlos-arguinano", "slug2");
        eq(Extractor.slug("https://www.atresplayer.com/antena3/programas/ahora-caigo/temporada-1/x_60ee0bdd6584a872911240a6/"), "ahora-caigo", "slug3");
        eq(Extractor.keyword("karlos-arguinano-cocina"), "arguinano", "kw");
        eq(Extractor.atresId("https://www.atresplayer.com/antena3/programas/ahora-caigo/temporada-1/01-08-21-especial-10-anos_60ee0bdd6584a872911240a6/"), "60ee0bdd6584a872911240a6", "atresid");
        eq(Extractor.prefix("http://www.antena3.com/videos/karlos-arguinano/2013.html"), "www.antena3.com/videos/karlos-arguinano/", "prefix");
        String base = "http://www.antena3.com/videos/karlos-arguinano/2013-febrero-7.html";
        String html = "<html><script>var urlVideo = 'http://desprogresiva.antena3.com/mp_series1/2013/02/07/00029/001.mp4';\n"
          + "var cfg = {\"file\":\"http:\\/\\/cdn.antena3.com\\/clips\\/arg_7feb.flv\"};</script>"
          + "<object><param name=\"flashvars\" value=\"xml=http://www.antena3.com/videoxml/karlos-arguinano/2013020700029.xml&autoplay=true\"></object>"
          + "<div data-video-config=\"/modulos/player/config.json?id=29\" data-src=\"/media/clip2.m3u8\"></div>"
          + "<a href=\"https://web.archive.org/web/20130211045623/http://www.antena3.com/media/old.mp3\">mp3</a>"
          + "<embed src=\"player.swf?file=http%3A%2F%2Fvod.a3.com%2Fv%2Fenc.mp4&x=1\">"
          + "<link href=\"http://www.antena3.com/crossdomain.xml\"></html>";
        Set<String> m = Extractor.extractMedia(html, base);
        System.out.println("media: " + m);
        has(m, "http://desprogresiva.antena3.com/mp_series1/2013/02/07/00029/001.mp4", "mp4 var");
        has(m, "http://cdn.antena3.com/clips/arg_7feb.flv", "escaped flv");
        has(m, "http://www.antena3.com/media/clip2.m3u8", "relative m3u8");
        has(m, "http://www.antena3.com/media/old.mp3", "wayback-prefixed mp3");
        has(m, "http://vod.a3.com/v/enc.mp4", "urlencoded mp4");
        Set<String> c = Extractor.extractConfigs(html, base);
        System.out.println("configs: " + c);
        has(c, "http://www.antena3.com/videoxml/karlos-arguinano/2013020700029.xml", "xml= flashvars");
        has(c, "http://www.antena3.com/modulos/player/config.json?id=29", "data-* json");
        eq(c.contains("http://www.antena3.com/crossdomain.xml"), false, "junk filtered");
        String xml = "<video><archivo><![CDATA[mp_series1/2013/02/07/00029/000.mp4]]></archivo><url>http://deslocal.antena3.com/a/b/high.mp4</url><urlHttpVideo>http://desprogresiva.antena3.com/</urlHttpVideo></video>";
        has(Extractor.extractMedia(xml, "http://www.antena3.com/videoxml/x.xml"), "http://deslocal.antena3.com/a/b/high.mp4", "xml media");
        String json = "{\"sources\":[{\"src\":\"https://geodeswowa3player.akamaized.net/vcg/_definst_/assets4/2021/07/30/X/hls/main.m3u8\",\"type\":\"application/vnd.apple.mpegurl\"}]}";
        has(Extractor.extractMedia(json, "https://api.atresplayer.com/player/v1/episode/60ee0bdd6584a872911240a6"), "https://geodeswowa3player.akamaized.net/vcg/_definst_/assets4/2021/07/30/X/hls/main.m3u8", "json m3u8");
        String cdx = "[[\"urlkey\",\"timestamp\",\"original\",\"mimetype\",\"statuscode\",\"digest\",\"length\"],\n[\"com,antena3)/v/a.mp4\", \"20130211045623\", \"http://www.antena3.com/v/a.mp4\", \"video/mp4\", \"200\", \"ABC\", \"10485760\"]]";
        List<String[]> rows = Extractor.parseCdx(cdx);
        eq(rows.size(), 1, "cdx rows"); eq(rows.get(0)[1], "http://www.antena3.com/v/a.mp4", "cdx orig"); eq(Extractor.prettySize(rows.get(0)[4]), "10.0 MB", "size");
        eq(Extractor.niceName("http://x.com/a/b/001.mp4?x=1", "20130211045623"), "20130211_001.mp4", "name");
        eq(Extractor.prettyDate("20130207120000"), "7 feb 2013", "date");
        System.out.println(ok + " OK, " + fail + " FAIL"); System.exit(fail == 0 ? 0 : 1);
    }
}
