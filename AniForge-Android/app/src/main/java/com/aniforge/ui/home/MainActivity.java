package com.aniforge.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aniforge.R;
import com.aniforge.model.AnimeInfo;
import com.aniforge.ui.detail.DetailActivity;
import com.aniforge.ui.search.SearchActivity;

import java.util.ArrayList;

/**
 * Pantalla principal de AniForge.
 *
 * CRÍTICO PARA FIRE TV:
 * - Todos los elementos interactivos deben tener android:focusable="true"
 * - La navegación D-pad (arriba/abajo/izquierda/derecha) debe funcionar sin touch
 * - El botón "Menu" del control remoto abre búsqueda
 */
public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private RecyclerView recyclerLatestAnimes;
    private RecyclerView recyclerLatestEpisodes;
    private ProgressBar progressBar;
    private TextView tvError;
    private AnimeCardAdapter animeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupViews();
        setupViewModel();
        viewModel.loadHome();
    }

    private void setupViews() {
        progressBar = findViewById(R.id.progressBar);
        tvError = findViewById(R.id.tvError);
        recyclerLatestAnimes = findViewById(R.id.recyclerLatestAnimes);
        recyclerLatestEpisodes = findViewById(R.id.recyclerLatestEpisodes);

        // Grid de 5 columnas — típico para pantallas de TV 1080p
        GridLayoutManager layoutManager = new GridLayoutManager(this, 5);
        recyclerLatestAnimes.setLayoutManager(layoutManager);

        animeAdapter = new AnimeCardAdapter(new ArrayList<>(), anime -> {
            // Al seleccionar un anime con OK/Enter del control remoto
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(DetailActivity.EXTRA_ANIME_ID, anime.id);
            intent.putExtra(DetailActivity.EXTRA_ANIME_TITLE, anime.title);
            startActivity(intent);
        });
        recyclerLatestAnimes.setAdapter(animeAdapter);

        // Botón de búsqueda — el primer elemento enfocable al inicio
        View btnSearch = findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(v -> openSearch());
        // CRÍTICO: solicitar foco inicial para que el D-pad funcione desde el primer momento
        btnSearch.requestFocus();
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        viewModel.getLatestAnimes().observe(this, animes -> {
            if (animes != null && !animes.isEmpty()) {
                animeAdapter.updateData(animes);
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            progressBar.setVisibility(loading != null && loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                tvError.setText(error);
                tvError.setVisibility(View.VISIBLE);
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openSearch() {
        startActivity(new Intent(this, SearchActivity.class));
    }

    /**
     * CRÍTICO PARA FIRE TV: Manejar teclas del control remoto.
     * El botón de búsqueda (lupa) en el control Fire TV envía KEYCODE_SEARCH.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_SEARCH:
            case KeyEvent.KEYCODE_MENU:
                openSearch();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }
}
