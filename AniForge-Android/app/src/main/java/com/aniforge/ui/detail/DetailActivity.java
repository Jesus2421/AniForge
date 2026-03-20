package com.aniforge.ui.detail;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.aniforge.R;
import com.aniforge.model.AnimeInfo;
import com.aniforge.model.EpisodeInfo;
import com.aniforge.ui.home.MainViewModel;
import com.aniforge.ui.player.PlayerActivity;

import java.util.ArrayList;

/**
 * Pantalla de detalle del anime.
 * Muestra: poster, sinopsis, géneros, rating y lista de episodios.
 *
 * FIRE TV: La lista de episodios debe ser navegable con D-pad vertical.
 */
public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_ANIME_ID    = "anime_id";
    public static final String EXTRA_ANIME_TITLE = "anime_title";

    private MainViewModel viewModel;
    private ProgressBar progressBar;
    private TextView tvError;
    private ImageView imgBanner;
    private TextView tvTitle, tvSynopsis, tvGenres, tvRating, tvType, tvDebut;
    private RecyclerView recyclerEpisodes;
    private EpisodeAdapter episodeAdapter;

    private String animeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        animeId = getIntent().getStringExtra(EXTRA_ANIME_ID);
        String animeTitle = getIntent().getStringExtra(EXTRA_ANIME_TITLE);

        if (animeId == null) {
            Toast.makeText(this, "Error: ID de anime no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupViews(animeTitle);
        setupViewModel();
        viewModel.loadAnimeDetail(animeId);
    }

    private void setupViews(String initialTitle) {
        progressBar     = findViewById(R.id.progressBar);
        tvError         = findViewById(R.id.tvError);
        imgBanner       = findViewById(R.id.imgBanner);
        tvTitle         = findViewById(R.id.tvTitle);
        tvSynopsis      = findViewById(R.id.tvSynopsis);
        tvGenres        = findViewById(R.id.tvGenres);
        tvRating        = findViewById(R.id.tvRating);
        tvType          = findViewById(R.id.tvType);
        tvDebut         = findViewById(R.id.tvDebut);
        recyclerEpisodes = findViewById(R.id.recyclerEpisodes);

        // Título provisional mientras carga
        if (initialTitle != null) tvTitle.setText(initialTitle);

        // Lista de episodios - LinearLayoutManager vertical para D-pad
        recyclerEpisodes.setLayoutManager(new LinearLayoutManager(this));
        episodeAdapter = new EpisodeAdapter(new ArrayList<>(), episode -> {
            // Al seleccionar episodio con OK del control remoto
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra(PlayerActivity.EXTRA_ANIME_ID, animeId);
            intent.putExtra(PlayerActivity.EXTRA_EPISODE_ID, episode.id);
            intent.putExtra(PlayerActivity.EXTRA_EPISODE_NUMBER, episode.number);
            startActivity(intent);
        });
        recyclerEpisodes.setAdapter(episodeAdapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        viewModel.getAnimeDetail().observe(this, anime -> {
            if (anime != null) renderAnimeDetail(anime);
        });

        viewModel.getIsLoading().observe(this, loading -> {
            progressBar.setVisibility(loading != null && loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                tvError.setText(error);
                tvError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void renderAnimeDetail(AnimeInfo anime) {
        tvTitle.setText(anime.title);
        tvSynopsis.setText(anime.synopsis != null ? anime.synopsis : "Sin sinopsis disponible");
        tvRating.setText(anime.rating != null ? "★ " + anime.rating : "Sin valoración");
        tvType.setText(anime.type != null ? anime.type : "");
        tvDebut.setText(anime.debut != null ? anime.debut : "");

        if (anime.genres != null && !anime.genres.isEmpty()) {
            tvGenres.setText(String.join(" · ", anime.genres));
        }

        if (anime.banner != null && !anime.banner.isEmpty()) {
            Glide.with(this)
                .load(anime.banner)
                .centerCrop()
                .into(imgBanner);
        }

        if (anime.episodes != null && !anime.episodes.isEmpty()) {
            episodeAdapter.updateData(anime.episodes);
            // FIRE TV: mover foco al primer episodio automáticamente
            recyclerEpisodes.requestFocus();
        }
    }
}
