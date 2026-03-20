package com.aniforge.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.aniforge.R;
import com.aniforge.model.AnimeInfo;

import java.util.List;

/**
 * Adapter de tarjetas de anime.
 *
 * CRÍTICO PARA FIRE TV:
 * - Cada item debe ser focusable para navegación D-pad
 * - El estado "focused" debe tener feedback visual claro
 *   (borde brillante, escala aumentada, etc.)
 */
public class AnimeCardAdapter extends RecyclerView.Adapter<AnimeCardAdapter.ViewHolder> {

    public interface OnAnimeClickListener {
        void onAnimeClick(AnimeInfo anime);
    }

    private List<AnimeInfo> animes;
    private final OnAnimeClickListener listener;

    public AnimeCardAdapter(List<AnimeInfo> animes, OnAnimeClickListener listener) {
        this.animes = animes;
        this.listener = listener;
    }

    public void updateData(List<AnimeInfo> newAnimes) {
        this.animes = newAnimes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_anime_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AnimeInfo anime = animes.get(position);
        holder.bind(anime, listener);
    }

    @Override
    public int getItemCount() {
        return animes != null ? animes.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgPoster;
        private final TextView tvTitle;
        private final TextView tvType;
        private final TextView tvRating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPoster = itemView.findViewById(R.id.imgPoster);
            tvTitle   = itemView.findViewById(R.id.tvTitle);
            tvType    = itemView.findViewById(R.id.tvType);
            tvRating  = itemView.findViewById(R.id.tvRating);
        }

        public void bind(AnimeInfo anime, OnAnimeClickListener listener) {
            tvTitle.setText(anime.title != null ? anime.title : "Sin título");
            tvType.setText(anime.type != null ? anime.type : "");
            tvRating.setText(anime.rating != null ? "★ " + anime.rating : "");

            // Cargar imagen con Glide
            if (anime.getPosterUrl() != null) {
                Glide.with(itemView.getContext())
                    .load(anime.getPosterUrl())
                    .placeholder(R.drawable.placeholder_anime)
                    .error(R.drawable.placeholder_anime)
                    .centerCrop()
                    .into(imgPoster);
            } else {
                imgPoster.setImageResource(R.drawable.placeholder_anime);
            }

            // Click y foco para D-pad
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onAnimeClick(anime);
            });

            // CRÍTICO PARA FIRE TV: feedback visual al recibir foco con D-pad
            itemView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    // Escalar y elevar para indicar selección con D-pad
                    v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(150).start();
                    v.setElevation(12f);
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                    v.setElevation(2f);
                }
            });
        }
    }
}
