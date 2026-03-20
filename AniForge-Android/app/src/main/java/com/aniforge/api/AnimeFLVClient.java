package com.aniforge.api;

import android.util.Log;

import com.aniforge.model.AnimeInfo;
import com.aniforge.model.EpisodeInfo;
import com.aniforge.model.VideoServer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Port completo de animeflv.py a Java.
 *
 * Python usaba cloudscraper + BeautifulSoup.
 * Aquí usamos OkHttp + Jsoup, que son sus equivalentes en Android.
 *
 * TODOS los métodos hacen I/O de red → llamar siempre en un hilo secundario
 * (AsyncTask, ExecutorService o ViewModel + LiveData).
 */
public class AnimeFLVClient {

    private static final String TAG = "AnimeFLVClient";

    // Equivalente a las constantes BASE_URL, etc. de Python
    private static final String BASE_URL              = "https://animeflv.net";
    private static final String BROWSE_URL            = "https://animeflv.net/browse";
    private static final String ANIME_VIDEO_URL       = "https://animeflv.net/ver/";
    private static final String ANIME_URL             = "https://animeflv.net/anime/";
    private static final String BASE_EPISODE_IMG_URL  = "https://cdn.animeflv.net/screenshots/";

    // Headers para simular un navegador real (equivale a cloudscraper en Python)
    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/124.0.0.0 Safari/537.36";

    private final OkHttpClient client;

    public AnimeFLVClient() {
        client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            // Reintentos automáticos (cloudscraper los hacía implícitamente)
            .retryOnConnectionFailure(true)
            .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTTP helper (equivalente a self._scraper.get() en Python)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Hace GET a la URL y devuelve el cuerpo como String.
     * Lanza IOException si falla la red o el servidor devuelve error.
     *
     * BUG CRÍTICO DETECTADO: En Python no se verificaba response.ok.
     * Aquí sí verificamos el código HTTP.
     */
    private String fetchHtml(String url) throws IOException {
        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
            .header("Referer", BASE_URL)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + " para URL: " + url);
            }
            if (response.body() == null) {
                throw new IOException("Cuerpo de respuesta vacío para: " + url);
            }
            return response.body().string();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // search() — equivalente a AnimeFLV.search() en Python
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Busca animes por query.
     * Puerto directo de:
     *   def search(self, query: str = None, page: int = None) -> List[AnimeInfo]
     */
    public List<AnimeInfo> search(String query, Integer page) throws IOException, AnimeFLVParseException {
        StringBuilder urlBuilder = new StringBuilder(BROWSE_URL);
        boolean hasParam = false;

        if (query != null && !query.isEmpty()) {
            urlBuilder.append("?q=").append(java.net.URLEncoder.encode(query, "UTF-8"));
            hasParam = true;
        }
        if (page != null) {
            urlBuilder.append(hasParam ? "&" : "?").append("page=").append(page);
        }

        String html = fetchHtml(urlBuilder.toString());
        Document doc = Jsoup.parse(html);

        // Equivalente a: soup.select("div.Container ul.ListAnimes li article")
        Elements elements = doc.select("div.Container ul.ListAnimes li article");

        if (elements.isEmpty()) {
            // BUG EN PYTHON: solo lanzaba error si elements era None, no si estaba vacío
            // Aquí manejamos el caso vacío correctamente
            return new ArrayList<>();
        }

        return processAnimeListInfo(elements);
    }

    /** Sobrecarga sin paginación */
    public List<AnimeInfo> search(String query) throws IOException, AnimeFLVParseException {
        return search(query, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAnimeInfo() — equivalente a AnimeFLV.get_anime_info() en Python
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Obtiene toda la información de un anime por su ID.
     * Puerto directo de:
     *   def get_anime_info(self, id: str) -> AnimeInfo
     *
     * BUG DETECTADO EN PYTHON: La URL era f"{ANIME_URL}/{id}" con doble slash
     * porque ANIME_URL ya termina en "/". Aquí lo corregimos.
     */
    public AnimeInfo getAnimeInfo(String id) throws IOException, AnimeFLVParseException {
        // CORRECCIÓN: Python tenía ANIME_URL + "/" + id → doble slash. Lo corregimos.
        String url = ANIME_URL + id;
        String html = fetchHtml(url);
        Document doc = Jsoup.parse(html);

        AnimeInfo info = new AnimeInfo();
        info.id = id;

        try {
            // Equivalente a los selectores CSS de BeautifulSoup en Python
            Element titleEl = doc.selectFirst(
                "body div.Wrapper div.Body div div.Ficha.fchlt div.Container h1.Title");
            info.title = titleEl != null ? titleEl.text() : "";

            Element posterEl = doc.selectFirst(
                "body div div div div div aside div.AnimeCover div.Image figure img");
            if (posterEl != null) {
                String src = posterEl.attr("src");
                info.poster = src.startsWith("http") ? src : BASE_URL + "/" + src;
                info.banner = info.poster.replace("covers", "banners");
            }

            Element synopsisEl = doc.selectFirst(
                "body div div div div div main section div.Description p");
            info.synopsis = synopsisEl != null ? synopsisEl.text().trim() : "";

            Element ratingEl = doc.selectFirst(
                "body div div div.Ficha.fchlt div.Container div.vtshr div.Votes span#votes_prmd");
            info.rating = ratingEl != null ? ratingEl.text() : null;

            Element debutEl = doc.selectFirst(
                "body div.Wrapper div.Body div div.Container div.BX.Row.BFluid.Sp20 aside.SidebarA.BFixed p.AnmStts");
            info.debut = debutEl != null ? debutEl.text() : null;

            Element typeEl = doc.selectFirst(
                "body div.Wrapper div.Body div div.Ficha.fchlt div.Container span.Type");
            info.type = typeEl != null ? typeEl.text() : null;

            // Géneros
            List<String> genres = new ArrayList<>();
            for (Element genreEl : doc.select("main.Main section.WdgtCn nav.Nvgnrs a")) {
                String href = genreEl.attr("href");
                if (href.contains("=")) {
                    genres.add(href.split("=")[1]);
                }
            }
            info.genres = genres;

            // Episodios — extraídos desde los scripts JS embebidos
            // Equivalente al bloque "var anime_info" y "var episodes" de Python
            List<EpisodeInfo> episodes = new ArrayList<>();
            String animeThumbnailsId = null;

            for (Element script : doc.select("script")) {
                String content = script.html();

                if (content.contains("var anime_info = [")) {
                    try {
                        String raw = content.split("var anime_info = ")[1].split(";")[0].trim();
                        JSONArray arr = new JSONArray(raw);
                        animeThumbnailsId = arr.getString(0);
                    } catch (Exception e) {
                        Log.w(TAG, "No se pudo parsear anime_info: " + e.getMessage());
                    }
                }

                if (content.contains("var episodes = [")) {
                    try {
                        String raw = content.split("var episodes = ")[1].split(";")[0].trim();
                        JSONArray arr = new JSONArray(raw);
                        int epNumber = 1;
                        for (int i = 0; i < arr.length(); i++) {
                            JSONArray epArr = arr.getJSONArray(i);
                            String epId = epArr.getString(0);
                            String preview = (animeThumbnailsId != null)
                                ? BASE_EPISODE_IMG_URL + animeThumbnailsId + "/" + epId + "/th_3.jpg"
                                : null;
                            episodes.add(new EpisodeInfo(epId, id, preview, epNumber++));
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "No se pudo parsear episodes: " + e.getMessage());
                    }
                }
            }
            info.episodes = episodes;

        } catch (Exception e) {
            throw new AnimeFLVParseException("Error parseando info del anime: " + e.getMessage(), e);
        }

        return info;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getVideoServers() — equivalente a AnimeFLV.get_video_servers() en Python
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Obtiene los servidores de video para un episodio.
     * Puerto de:
     *   def get_video_servers(self, id: str, episode: int, ...) -> List[Dict]
     *
     * BUG DETECTADO EN PYTHON: Si el script no contenía "var videos = {",
     * se devolvía lista vacía sin notificar al caller. Aquí lo logueamos.
     */
    public List<VideoServer> getVideoServers(String animeId, String episodeId)
            throws IOException, AnimeFLVParseException {

        String url = ANIME_VIDEO_URL + animeId + "-" + episodeId;
        String html = fetchHtml(url);
        Document doc = Jsoup.parse(html);

        List<VideoServer> servers = new ArrayList<>();

        for (Element script : doc.select("script")) {
            String content = script.html();
            if (content.contains("var videos = {")) {
                try {
                    String videosRaw = content.split("var videos = ")[1].split(";")[0].trim();
                    JSONObject videos = new JSONObject(videosRaw);

                    // SUB = subtitulado
                    if (videos.has("SUB")) {
                        JSONArray subServers = videos.getJSONArray("SUB");
                        for (int i = 0; i < subServers.length(); i++) {
                            JSONObject s = subServers.getJSONObject(i);
                            servers.add(new VideoServer(
                                s.optString("title", "Servidor " + (i+1)),
                                s.optString("url", "")
                            ));
                        }
                    }

                    // LAT = doblado latino
                    if (videos.has("LAT")) {
                        JSONArray latServers = videos.getJSONArray("LAT");
                        for (int i = 0; i < latServers.length(); i++) {
                            JSONObject s = latServers.getJSONObject(i);
                            servers.add(new VideoServer(
                                s.optString("title", "LAT " + (i+1)),
                                s.optString("url", "")
                            ));
                        }
                    }

                } catch (Exception e) {
                    throw new AnimeFLVParseException("Error parseando servidores de video", e);
                }
                break;
            }
        }

        if (servers.isEmpty()) {
            Log.w(TAG, "No se encontraron servidores para " + animeId + " ep " + episodeId);
        }

        return servers;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getLatestEpisodes() — equivalente a AnimeFLV.get_latest_episodes()
    // ─────────────────────────────────────────────────────────────────────────

    public List<EpisodeInfo> getLatestEpisodes() throws IOException, AnimeFLVParseException {
        String html = fetchHtml(BASE_URL);
        Document doc = Jsoup.parse(html);

        List<EpisodeInfo> ret = new ArrayList<>();
        Elements elements = doc.select("ul.ListEpisodios li a");

        for (Element el : elements) {
            try {
                String href = el.attr("href"); // /ver/nanatsu-no-taizai-1
                int lastDash = href.lastIndexOf("-");
                String epId = href.substring(lastDash + 1);
                String animePart = href.substring(0, lastDash).replace("/ver/", "");

                Element imgEl = el.selectFirst("span.Image img");
                String imgSrc = imgEl != null ? BASE_URL + imgEl.attr("src") : null;

                ret.add(new EpisodeInfo(epId, animePart, imgSrc, Integer.parseInt(epId)));
            } catch (Exception e) {
                throw new AnimeFLVParseException("Error parseando episodio reciente", e);
            }
        }

        return ret;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getLatestAnimes() — equivalente a AnimeFLV.get_latest_animes()
    // ─────────────────────────────────────────────────────────────────────────

    public List<AnimeInfo> getLatestAnimes() throws IOException, AnimeFLVParseException {
        String html = fetchHtml(BASE_URL);
        Document doc = Jsoup.parse(html);
        Elements elements = doc.select("ul.ListAnimes li article");
        return processAnimeListInfo(elements);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper interno: _process_anime_list_info() de Python
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Procesa una lista de elementos article y los convierte en AnimeInfo.
     * Port de _process_anime_list_info() de Python.
     *
     * BUG DETECTADO EN PYTHON: Si un elemento fallaba, lanzaba excepción
     * y perdía todos los demás. Aquí logueamos el error y continuamos.
     */
    private List<AnimeInfo> processAnimeListInfo(Elements elements) throws AnimeFLVParseException {
        List<AnimeInfo> ret = new ArrayList<>();

        for (Element el : elements) {
            try {
                AnimeInfo anime = new AnimeInfo();

                // ID: equivalente a removeprefix(href[1:], "anime/")
                Element btnEl = el.selectFirst("div.Description a.Button");
                if (btnEl != null) {
                    String href = btnEl.attr("href");
                    // href = "/anime/nanatsu-no-taizai"
                    anime.id = href.replace("/anime/", "").replace("/", "");
                }

                Element titleEl = el.selectFirst("a h3");
                anime.title = titleEl != null ? titleEl.text() : "";

                // Poster — con fallback a data-cfsrc (como en Python)
                Element imgEl = el.selectFirst("a div.Image figure img");
                if (imgEl != null) {
                    String src = imgEl.attr("src");
                    if (src.isEmpty()) src = imgEl.attr("data-cfsrc");
                    anime.poster = src.startsWith("http") ? src : BASE_URL + src;
                    anime.banner = anime.poster.replace("covers", "banners").trim();
                }

                Element typeEl = el.selectFirst("div.Description p span.Type");
                anime.type = typeEl != null ? typeEl.text() : null;

                Elements pTags = el.select("div.Description p");
                if (pTags.size() > 1) {
                    anime.synopsis = pTags.get(1).text().trim();
                }

                Element ratingEl = el.selectFirst("div.Description p span.Vts");
                anime.rating = ratingEl != null ? ratingEl.text() : null;

                Element estrenoEl = el.selectFirst("a span.Estreno");
                anime.debut = estrenoEl != null ? estrenoEl.text().toLowerCase() : null;

                ret.add(anime);

            } catch (Exception e) {
                // BUG CORREGIDO: Python hacía raise y perdía todo. Aquí seguimos con el resto.
                Log.e(TAG, "Error procesando elemento de anime, se omite: " + e.getMessage());
            }
        }

        return ret;
    }
}
