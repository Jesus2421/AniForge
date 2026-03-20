package com.aniforge.model;

/**
 * Equivalente al dataclass EpisodeInfo de Python:
 *
 * @dataclass
 * class EpisodeInfo:
 *     id: Union[str, int]
 *     anime: str
 *     image_preview: Optional[str] = None
 */
public class EpisodeInfo {
    public String id;
    public String anime;
    public String imagePreview;
    // Número de episodio calculado desde el índice (como en Episode.from_api de main.py)
    public int number;

    public EpisodeInfo() {}

    public EpisodeInfo(String id, String anime, String imagePreview, int number) {
        this.id = id;
        this.anime = anime;
        this.imagePreview = imagePreview;
        this.number = number;
    }

    @Override
    public String toString() {
        return "EpisodeInfo{id='" + id + "', anime='" + anime + "', number=" + number + "}";
    }
}
