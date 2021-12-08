package com.example.blmusicplayer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


import java.io.IOException;
import java.util.ArrayList;

public class SongListingActivity extends AppCompatActivity {
    private ArrayList<Song> list;
    private MusicService srv;
    private Intent itn;
    private boolean serviceBound = false;
    private boolean paused = false;
    private boolean playbackPaused = false;
    private int flag;
    private ListView listView;
    MusicController controller;
    ImageView button_back;
    EditText Search_Song;
    ArrayList<Song> List_Search_result ;
    boolean playInListSearch = false;
    int currentSong;
    boolean is_playing;
    boolean shuffle;
    boolean replay;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_listing);
        button_back = findViewById(R.id.BackToMain);
        Search_Song = findViewById(R.id.Search_song);

        listView = (ListView)findViewById(R.id.song_list);

        Search_Song.requestFocus();
        Search_Song.setFocusableInTouchMode(false);

        System.out.println(Search_Song.requestFocus());

        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra("bundle");
        currentSong = intent.getIntExtra("current_song", 0);
        is_playing = intent.getBooleanExtra("is_playing", false);
        shuffle = intent.getBooleanExtra("shuffle", false);
        replay = intent.getBooleanExtra("replay", false);
        list = bundle.getParcelableArrayList("list");
        SongAdapter ad = new SongAdapter( list);
        listView.setAdapter(ad);


        Search_Song.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Search_Song.setFocusableInTouchMode(true);
                Search_Song.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(Search_Song, 0);
            }
        });
        Search_Song.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Search_Song();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        Back_to_Main();


    }
    public void songPicked(View view) throws IOException {

        int flag = Integer.parseInt(view.getTag().toString());
        if(playInListSearch == true)
        {
            int dem=-1;
            for ( Song song:list)
            {
                dem++;
                if(song.name == List_Search_result.get(flag).name && song.artist ==List_Search_result.get(flag).artist) break;
            }
            flag = dem;
        }

        Intent intent = new Intent();
        intent.putExtra("current_song",flag);
        intent.putExtra("backBySongPick",1);
        intent.putExtra("backByButton", 0);
        intent.putExtra("is_playing", true);
        intent.putExtra("shuffle", shuffle);
        intent.putExtra("replay", replay);
        //intent.putExtra("check2", 0);
        intent.setClass(view.getContext(),MainActivity.class);
        startActivity(intent);
    }
    public void Search_Song()
    {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(Search_Song.getWindowToken(), 0);
                List_Search_result = new ArrayList<Song>();
                 String key_Search = Search_Song.getText().toString();
                 if(key_Search !=null) {
                     for (Song song : list) {
                         String string = song.name.toLowerCase();
                         String string1 = song.artist.toLowerCase();
                         if (string.contains(key_Search) || string1.contains(key_Search))
                             List_Search_result.add(song);
                     }
                     SongAdapter adapter = new SongAdapter(List_Search_result);
                     listView.setAdapter(adapter);
                 }

    }
    public void Back_to_Main()
    {
        button_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.putExtra("is_playing", is_playing);
                intent.putExtra("current_song", currentSong);
                intent.putExtra("backByButton", 1);
                intent.putExtra("backBySongPicked", 0);
                intent.putExtra("shuffle", shuffle);
                intent.putExtra("replay", replay);
                //intent.putExtra("check2",1);
                //intent.putExtra("List_search", "ListSearch");
                intent.setClass(view.getContext(),MainActivity.class);
                startActivity(intent);
            }
        });
    }
}

