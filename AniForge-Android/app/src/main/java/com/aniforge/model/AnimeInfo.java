package com.aniforge.model;

import java.util.List;

/**
 * Equivalente al dataclass AnimeInfo de Python:
 *
 * @dataclass
 * class AnimeInfo:
 *     id: Union[str, int]
 *     title: str
 *     poster: Optional[str] = None
 *     ...
 */
public class AnimeInfo {
    public String id;
    public String title;
    public String poster;
    public String banner;
    public String synopsis;
    public String rating;
    public List<String> genres;
    public String debut;
    public String type;
    public List<EpisodeInfo> episodes;

    public AnimeInfo() {}

    public AnimeInfo(String id, String title) {
        this.id = id;
        this.title = title;
    }

    /** URL completa del poster para cargar con Glide */
    public String getPosterUrl() {
        if (poster == null) return null;
        if (poster.startsWith("http")) return poster;
        return "https://animeflv.net/" + poster;
    }

    @Override
    public String toString() {
        return "AnimeInfo{id='" + id + "', title='" + title + "'}";
    }
}
