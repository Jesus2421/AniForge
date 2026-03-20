package com.aniforge.model;

/**
 * Equivalente a DownloadLinkInfo de Python:
 *
 * @dataclass
 * class DownloadLinkInfo:
 *     server: str
 *     url: str
 */
public class VideoServer {
    public String server;
    public String url;

    public VideoServer() {}

    public VideoServer(String server, String url) {
        this.server = server;
        this.url = url;
    }

    @Override
    public String toString() {
        return "VideoServer{server='" + server + "', url='" + url + "'}";
    }
}
