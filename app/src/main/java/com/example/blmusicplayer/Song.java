package com.example.blmusicplayer;

import android.os.Parcel;
import android.os.Parcelable;

public  class Song implements Parcelable {
    public String name;
    public String artist;
    public long id;
    public String thumbnail;


    public Song(long id, String name, String artist, String thumbnail){
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.thumbnail = thumbnail;
    }
    public Song(long id, String name, String artist){
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.thumbnail = null;
    }

    protected Song(Parcel in) {
        name = in.readString();
        artist = in.readString();
        id = in.readLong();
        thumbnail = in.readString();
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

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(artist);
        dest.writeLong(id);
        dest.writeString(thumbnail);
    }
}
