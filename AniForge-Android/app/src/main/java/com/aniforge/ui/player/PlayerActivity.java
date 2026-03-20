package com.aniforge.ui.player;

import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.aniforge.R;
import com.aniforge.model.VideoServer;
import com.aniforge.ui.home.MainViewModel;

import java.util.List;

/**
 * Reproductor de video con ExoPlayer.
 *
 * FIRE TV: El control remoto tiene botones de Play/Pause, que ExoPlayer
 * maneja automáticamente si PlayerView tiene el foco.
 *
 * BUG DETECTADO: En Python no había manejo de errores de red al reproducir.
 * Aquí usamos el listener de ExoPlayer para capturar errores y mostrar
 * al usuario la opción de intentar con otro servidor.
 */
public class PlayerActivity extends AppCompatActivity {

    public static final String EXTRA_ANIME_ID       = "anime_id";
    public static final String EXTRA_EPISODE_ID     = "episode_id";
    public static final String EXTRA_EPISODE_NUMBER = "episode_number";

    private MainViewModel viewModel;
    private ExoPlayer player;
    private PlayerView playerView;
    private ProgressBar progressBar;
    private TextView tvServerInfo;

    private String animeId;
    private String episodeId;
    private int episodeNumber;
    private List<VideoServer> servers;
    private int currentServerIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        animeId       = getIntent().getStringExtra(EXTRA_ANIME_ID);
        episodeId     = getIntent().getStringExtra(EXTRA_EPISODE_ID);
        episodeNumber = getIntent().getIntExtra(EXTRA_EPISODE_NUMBER, 1);

        if (animeId == null || episodeId == null) {
            Toast.makeText(this, "Error: datos del episodio incompletos", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupViews();
        setupPlayer();
        setupViewModel();

        // Cargar servidores de video
        viewModel.loadVideoServers(animeId, episodeId);
    }

    private void setupViews() {
        playerView   = findViewById(R.id.playerView);
        progressBar  = findViewById(R.id.progressBar);
        tvServerInfo = findViewById(R.id.tvServerInfo);
    }

    private void setupPlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        // FIRE TV: el PlayerView debe tener foco para recibir comandos del control remoto
        playerView.requestFocus();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_BUFFERING) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                // BUG CORREGIDO: En Python no había fallback entre servidores.
                // Aquí intentamos automáticamente con el siguiente servidor disponible.
                Toast.makeText(PlayerActivity.this,
                    "Error en servidor actual. Intentando con el siguiente...",
                    Toast.LENGTH_SHORT).show();
                tryNextServer();
            }
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        viewModel.getVideoServers().observe(this, serverList -> {
            if (serverList != null && !serverList.isEmpty()) {
                servers = serverList;
                currentServerIndex = 0;
                playCurrentServer();
            } else {
                Toast.makeText(this, "No se encontraron servidores de video", Toast.LENGTH_LONG).show();
                finish();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void playCurrentServer() {
        if (servers == null || currentServerIndex >= servers.size()) {
            Toast.makeText(this, "No hay más servidores disponibles", Toast.LENGTH_LONG).show();
            return;
        }

        VideoServer server = servers.get(currentServerIndex);
        tvServerInfo.setText("Servidor: " + server.server +
            " (" + (currentServerIndex + 1) + "/" + servers.size() + ")");

        try {
            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(server.url));
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();
        } catch (Exception e) {
            Toast.makeText(this, "URL inválida en servidor " + server.server, Toast.LENGTH_SHORT).show();
            tryNextServer();
        }
    }

    private void tryNextServer() {
        currentServerIndex++;
        playCurrentServer();
    }

    /**
     * FIRE TV: Manejar teclas del control remoto para el reproductor.
     * Play/Pause, adelantar y retroceder.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (player == null) return super.onKeyDown(keyCode, event);

        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (player.isPlaying()) player.pause();
                else player.play();
                return true;

            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                // Adelantar 10 segundos
                player.seekTo(player.getCurrentPosition() + 10_000);
                return true;

            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                // Retroceder 10 segundos
                player.seekTo(Math.max(0, player.getCurrentPosition() - 10_000));
                return true;

            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) player.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
