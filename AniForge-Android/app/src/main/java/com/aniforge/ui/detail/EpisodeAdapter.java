package com.aniforge.ui.detail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.aniforge.R;
import com.aniforge.model.EpisodeInfo;

import java.util.List;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {

    public interface OnEpisodeClickListener {
        void onEpisodeClick(EpisodeInfo episode);
    }

    private List<EpisodeInfo> episodes;
    private final OnEpisodeClickListener listener;

    public EpisodeAdapter(List<EpisodeInfo> episodes, OnEpisodeClickListener listener) {
        this.episodes = episodes;
        this.listener = listener;
    }

    public void updateData(List<EpisodeInfo> newEpisodes) {
        this.episodes = newEpisodes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_episode, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(episodes.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return episodes != null ? episodes.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgPreview;
        private final TextView tvEpisodeNumber;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPreview      = itemView.findViewById(R.id.imgEpisodePreview);
            tvEpisodeNumber = itemView.findViewById(R.id.tvEpisodeNumber);
        }

        public void bind(EpisodeInfo episode, OnEpisodeClickListener listener) {
            tvEpisodeNumber.setText("Episodio " + episode.number);

            if (episode.imagePreview != null) {
                Glide.with(itemView.getContext())
                    .load(episode.imagePreview)
                    .placeholder(R.drawable.placeholder_episode)
                    .centerCrop()
                    .into(imgPreview);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onEpisodeClick(episode);
            });

            // FIRE TV: feedback visual al navegar con D-pad
            itemView.setOnFocusChangeListener((v, hasFocus) -> {
                v.setBackgroundResource(hasFocus
                    ? R.drawable.bg_episode_focused
                    : R.drawable.bg_episode_normal);
            });
        }
    }
}
