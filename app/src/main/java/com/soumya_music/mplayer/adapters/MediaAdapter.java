package com.soumya_music.mplayer.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.soumya_music.mplayer.R;
import com.soumya_music.mplayer.databinding.SongListCardBinding;
import com.soumya_music.mplayer.models.Song;

import java.util.List;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.ViewHolder> {

    List<Song> songList;
    Context context;
    OnClickListener onClickListener;

    public MediaAdapter(List<Song> songList, Context context, OnClickListener onClickListener) {
        this.songList = songList;
        this.context = context;
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public MediaAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_list_card,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaAdapter.ViewHolder holder, int position) {
        Song model = songList.get(position);
        Log.e("model ::",String.valueOf(model));
        holder.author.setText(model.getAuthor());
        holder.tvSongs.setText(model.getTitle());
        holder.itemView.setOnClickListener(view -> onClickListener.onClick(position,model));
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView author,tvSongs;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            author = itemView.findViewById(R.id.author);
            tvSongs = itemView.findViewById(R.id.tv_songs);
        }
    }
    public interface OnClickListener{
        void onClick(int position, Song item);
    }
}
