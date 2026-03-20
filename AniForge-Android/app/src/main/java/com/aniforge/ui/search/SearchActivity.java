package com.aniforge.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aniforge.R;
import com.aniforge.ui.detail.DetailActivity;
import com.aniforge.ui.home.AnimeCardAdapter;
import com.aniforge.ui.home.MainViewModel;

import java.util.ArrayList;

/**
 * Pantalla de búsqueda.
 *
 * FIRE TV: Fire TV tiene un teclado virtual propio que aparece cuando
 * un EditText recibe foco. El usuario puede escribir con el control remoto
 * o con una voz (si está configurado).
 *
 * BUG COMÚN: Si el EditText no tiene android:imeOptions="actionSearch",
 * el botón de confirmar del teclado virtual no lanza la búsqueda.
 */
public class SearchActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private EditText etSearch;
    private ProgressBar progressBar;
    private RecyclerView recyclerResults;
    private TextView tvNoResults;
    private AnimeCardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        setupViews();
        setupViewModel();

        // FIRE TV: mostrar teclado automáticamente al abrir búsqueda
        etSearch.requestFocus();
    }

    private void setupViews() {
        etSearch       = findViewById(R.id.etSearch);
        progressBar    = findViewById(R.id.progressBar);
        recyclerResults = findViewById(R.id.recyclerResults);
        tvNoResults    = findViewById(R.id.tvNoResults);

        // Grid de 5 columnas para resultados
        recyclerResults.setLayoutManager(new GridLayoutManager(this, 5));
        adapter = new AnimeCardAdapter(new ArrayList<>(), anime -> {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(DetailActivity.EXTRA_ANIME_ID, anime.id);
            intent.putExtra(DetailActivity.EXTRA_ANIME_TITLE, anime.title);
            startActivity(intent);
        });
        recyclerResults.setAdapter(adapter);

        // Buscar al presionar Enter o el botón de búsqueda del teclado virtual
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                doSearch();
                return true;
            }
            return false;
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        viewModel.getSearchResults().observe(this, results -> {
            if (results != null && !results.isEmpty()) {
                adapter.updateData(results);
                tvNoResults.setVisibility(View.GONE);
                recyclerResults.setVisibility(View.VISIBLE);
                // Mover foco a resultados para navegar con D-pad
                recyclerResults.requestFocus();
            } else {
                tvNoResults.setVisibility(View.VISIBLE);
                recyclerResults.setVisibility(View.GONE);
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            progressBar.setVisibility(loading != null && loading ? View.VISIBLE : View.GONE);
        });
    }

    private void doSearch() {
        String query = etSearch.getText().toString().trim();
        if (!query.isEmpty()) {
            viewModel.search(query);
        }
    }
}
