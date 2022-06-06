package com.soumya_music.mplayer.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Song implements Parcelable {
    private String title;
    private String author;
    private String songUrl;
    private int id;

    public Song(String title, String author, String songUrl, int id) {
        this.title = title;
        this.author = author;
        this.songUrl = songUrl;
        this.id = id;
    }

    protected Song(Parcel in) {
        title = in.readString();
        author = in.readString();
        songUrl = in.readString();
        id = in.readInt();
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSongUrl() {
        return songUrl;
    }

    public void setSongUrl(String songUrl) {
        this.songUrl = songUrl;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(title);
        parcel.writeString(author);
        parcel.writeString(songUrl);
        parcel.writeInt(id);
    }
}
