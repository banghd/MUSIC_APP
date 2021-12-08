package com.example.blmusicplayer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;



import java.util.ArrayList;

public class SongAdapter extends BaseAdapter {

    private ArrayList<Song> songs;

    public SongAdapter(ArrayList<Song> theSongs){
        songs=theSongs;
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int position) {
        return songs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return songs.get(position).id;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View viewProduct;
        if (convertView == null) {
            viewProduct = View.inflate(parent.getContext(), R.layout.song, null);
        } else viewProduct = convertView;

        //Bind sữ liệu phần tử vào View
        Song s = (Song) getItem(position);
        viewProduct.setTag(position);
        ((TextView) viewProduct.findViewById(R.id.song_title)).setText(s.name);
        ((TextView) viewProduct.findViewById(R.id.song_artist)).setText(s.artist);
        Bitmap thumbnail= BitmapFactory.decodeFile(s.thumbnail);
        if(thumbnail == null){
            ((ImageView) viewProduct.findViewById(R.id.song_thumbnail)).setImageResource(R.drawable.replace_thumbnail);
        }
        else {
            ((ImageView) viewProduct.findViewById(R.id.song_thumbnail)).setImageBitmap(thumbnail);
        }
        return viewProduct;
    }
}
