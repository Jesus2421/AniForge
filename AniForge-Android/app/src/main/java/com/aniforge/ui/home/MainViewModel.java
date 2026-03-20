package com.aniforge.ui.home;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aniforge.api.AnimeFLVClient;
import com.aniforge.model.AnimeInfo;
import com.aniforge.model.EpisodeInfo;
import com.aniforge.model.VideoServer;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel principal — reemplaza el flujo de main.py de Python.
 *
 * En Python el flujo era síncrono (input → buscar → seleccionar → ...).
 * En Android/Fire TV el flujo es reactivo:
 * la UI observa LiveData y el ViewModel hace las llamadas en background.
 *
 * CRÍTICO para Fire TV: NUNCA hacer red en el hilo principal.
 */
public class MainViewModel extends AndroidViewModel {

    private static final String TAG = "MainViewModel";

    private final AnimeFLVClient api = new AnimeFLVClient();
    // Pool de hilos para operaciones de red (equivale al event loop de asyncio en Python)
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    // ─── LiveData que la UI observa ──────────────────────────────────────────
    private final MutableLiveData<List<AnimeInfo>> latestAnimes    = new MutableLiveData<>();
    private final MutableLiveData<List<EpisodeInfo>> latestEpisodes = new MutableLiveData<>();
    private final MutableLiveData<List<AnimeInfo>> searchResults   = new MutableLiveData<>();
    private final MutableLiveData<AnimeInfo> animeDetail           = new MutableLiveData<>();
    private final MutableLiveData<List<VideoServer>> videoServers  = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading               = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage             = new MutableLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
    }

    // ─── Getters de LiveData ─────────────────────────────────────────────────
    public LiveData<List<AnimeInfo>> getLatestAnimes()     { return latestAnimes; }
    public LiveData<List<EpisodeInfo>> getLatestEpisodes() { return latestEpisodes; }
    public LiveData<List<AnimeInfo>> getSearchResults()    { return searchResults; }
    public LiveData<AnimeInfo> getAnimeDetail()            { return animeDetail; }
    public LiveData<List<VideoServer>> getVideoServers()   { return videoServers; }
    public LiveData<Boolean> getIsLoading()                { return isLoading; }
    public LiveData<String> getErrorMessage()              { return errorMessage; }

    // ─────────────────────────────────────────────────────────────────────────
    // loadHome() — carga la pantalla principal con últimos animes y episodios
    // Equivalente al inicio de main.py
    // ─────────────────────────────────────────────────────────────────────────
    public void loadHome() {
        isLoading.postValue(true);
        executor.execute(() -> {
            try {
                List<AnimeInfo> animes = api.getLatestAnimes();
                List<EpisodeInfo> episodes = api.getLatestEpisodes();
                latestAnimes.postValue(animes);
                latestEpisodes.postValue(episodes);
            } catch (Exception e) {
                Log.e(TAG, "Error cargando home", e);
                errorMessage.postValue("Error de conexión: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // search() — equivalente a buscador.buscar_anime(query) de Python
    // ─────────────────────────────────────────────────────────────────────────
    public void search(String query) {
        if (query == null || query.trim().isEmpty()) {
            errorMessage.postValue("La búsqueda no puede estar vacía");
            return;
        }

        isLoading.postValue(true);
        executor.execute(() -> {
            try {
                List<AnimeInfo> results = api.search(query.trim());
                searchResults.postValue(results);
            } catch (Exception e) {
                Log.e(TAG, "Error buscando: " + query, e);
                errorMessage.postValue("Error al buscar: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // loadAnimeDetail() — equivalente a buscador.obtener_episodios(anime.id)
    // ─────────────────────────────────────────────────────────────────────────
    public void loadAnimeDetail(String animeId) {
        isLoading.postValue(true);
        executor.execute(() -> {
            try {
                AnimeInfo info = api.getAnimeInfo(animeId);
                animeDetail.postValue(info);
            } catch (Exception e) {
                Log.e(TAG, "Error cargando detalle: " + animeId, e);
                errorMessage.postValue("Error al cargar el anime: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // loadVideoServers() — equivalente a buscador.obtener_links(anime.id, ep.id)
    // ─────────────────────────────────────────────────────────────────────────
    public void loadVideoServers(String animeId, String episodeId) {
        isLoading.postValue(true);
        executor.execute(() -> {
            try {
                List<VideoServer> servers = api.getVideoServers(animeId, episodeId);
                videoServers.postValue(servers);
            } catch (Exception e) {
                Log.e(TAG, "Error obteniendo servidores", e);
                errorMessage.postValue("No se pudieron obtener los servidores: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
