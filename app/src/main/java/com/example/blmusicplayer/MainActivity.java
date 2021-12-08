package com.example.blmusicplayer;

import android.Manifest;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;
//import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;



import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements MediaController.MediaPlayerControl, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,MediaPlayer.OnCompletionListener{

    private ArrayList <Song> list;
    private  static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    private MusicService srv;
    private Intent itn;
    private boolean serviceBound=false;

    MusicController controller;
    ImageView listButton;
    CircleImageView main_screen_shuffle_button;
    CircleImageView main_screen_replay_button;
    int flag ;
    boolean is_playing = false;
    int backBySongPicked=0, backByButton=0;
    private boolean checkComplete;
    boolean replay = false ;
    boolean shuffle = false;
    
    SeekBar seekBar;
    Handler seekBarHandler = new Handler();
    Runnable r;
    TextView duration;
    TextView progress;
    int seekBarDuration = 0;
    int seekBarProgress = 0;
    public final String ACTION_PLAY_PAUSE = "play_pause";
    public final String ACTION_PREVIOUS = "previous";
    public final String ACTION_NEXT = "next";

    public int curSong;

    NotificationManager notificationManager;
    NotificationCompat.Builder builder;
    int NOTIFICATION_ID = 18082000;
    String CHANNEL_ID = "18082000";

    RemoteViews notificationLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
            while (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED){
            }
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        list = new ArrayList<Song>();
        getSongs();

        listButton = findViewById(R.id.playing_list_button);
        listButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList("list", list);
                intent.putExtra("bundle",bundle);
                intent.putExtra("current_song", curSong);
                intent.putExtra("is_playing", isPlaying());
                intent.putExtra("shuffle", shuffle);
                intent.putExtra("replay", replay);
                intent.setClass(view.getContext(), SongListingActivity.class);
                startActivity(intent);
            }
        });


        Intent intent = getIntent();
        if(intent!= null) {
            curSong = intent.getIntExtra("current_song", 0);
            backBySongPicked = intent.getIntExtra("backBySongPick",0);
            backByButton = intent.getIntExtra("backByButton",0);
            is_playing = intent.getBooleanExtra("is_playing", false);
            shuffle = intent.getBooleanExtra("shuffle", false);
            replay = intent.getBooleanExtra("replay", false);
        }

        registerReceiver(broadcastReceiver, new IntentFilter("TRACK_BACKS"));

        setController();
        duration = findViewById(R.id.duration);
        progress = findViewById(R.id.progress);
        seekBar = findViewById(R.id.main_screen_seek_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(srv.player != null && fromUser){
                    srv.player.seekTo(progress*1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String s = intent.getStringExtra("action");
            switch (s){
                case ACTION_PLAY_PAUSE: {
                    //updateMainScreen();
                    if(isPlaying()){
                        controller.getPlayPause().setImageResource(R.drawable.pause);
                    }
                    else{
                        controller.getPlayPause().setImageResource(R.drawable.play);
                    }
                    break;
                }
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if(srv != null)
            updateMainScreen();
        if(itn==null){
            itn = new Intent(this, MusicService.class);
            bindService(itn, musicConnection, Context.BIND_AUTO_CREATE);
            startService(itn);
        }
    }


    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
                MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
                //get service
                srv = binder.getService();
                //pass list
                srv.setSongList(list);
                srv.replay = replay;
                srv.shuffle = shuffle;
                serviceBound = true;
                //initSeekBar();

            if (backBySongPicked == 1 ) {
                srv.setCurpos(curSong);
                try {
                    srv.startPlaying();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if(backByButton ==1){
                srv.setCurpos(curSong);
            }
            updateMainScreen();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    public void initSeekBar(){
        r = new Runnable() {
            @Override
            public void run() {
                if(srv.player != null) {
                    if(curSong != srv.getCurpos()){
                        curSong = srv.getCurpos();
                        updateMainScreen();
                    }
                    if(getDuration() != 0){
                        seekBarDuration = getDuration()/1000;
                    }
                    if(getCurrentPosition() != 0){
                        seekBarProgress = getCurrentPosition()/1000;
                    }
                    seekBar.setMax(seekBarDuration);
                    seekBar.setProgress(seekBarProgress);
                    duration.setText(secondToMinute(seekBarDuration));
                    progress.setText(secondToMinute(seekBarProgress));
                }
                seekBarHandler.postDelayed(r, 1000);
            }
        };
        seekBarHandler.postDelayed(r, 1000);
    }

    public String secondToMinute(int second){
        if(second%60 < 10){
            return second / 60 + ":" + "0" + second % 60;
        }
        else {
            return second / 60 + ":" + second % 60;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //getSongs();
                }
            }
        }
    }

    public void getSongs(){
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if(musicCursor!=null && musicCursor.moveToFirst()){
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumIdColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            //add songs to list
            do {
                int albumId = musicCursor.getInt(albumIdColumn);
                int id = musicCursor.getInt(idColumn);
                String title = musicCursor.getString(titleColumn);
                String artist = musicCursor.getString(artistColumn);

                Cursor cursor = getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        new String[] {MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                        MediaStore.Audio.Albums._ID+ "=?",
                        new String[] {String.valueOf(albumId)},
                        null);
                if (cursor.moveToFirst()) {
                    String thumbnail = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                    Song a = new Song(id, title, artist, thumbnail);
                    list.add(a);
                }
            }
            while (musicCursor.moveToNext());
        }
        Collections.sort(list, new Comparator<Song>(){
            public int compare(Song a, Song b){
                return a.name.compareTo(b.name);
            }
        });
    }

    public void setController(){
        CircleImageView sh = findViewById(R.id.main_screen_shuffle_button);
        CircleImageView pr = findViewById(R.id.main_screen_prev_button);
        CircleImageView pl = findViewById(R.id.main_screen_play_pause_button);
        CircleImageView ne = findViewById(R.id.main_screen_next_button);
        CircleImageView re = findViewById(R.id.main_screen_replay_button);
        controller = new MusicController(sh, pr, pl, ne, re);
        controller.getPrev().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    srv.playPrev();
                    curSong = srv.getCurpos();
                    is_playing = true;
                    updateMainScreen();
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
        });
        controller.getNext().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if(srv.getCurpos() == -1){
                        srv.setCurpos(0);
                    }
                    if(replay){
                        srv.setCurpos(curSong+1);
                    }
                    srv.playNext();
                    curSong = srv.getCurpos();
                    is_playing = true;
                    updateMainScreen();
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
        });
        controller.getPlayPause().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPlaying()){
                    int i = srv.pausePlayer();
                    controller.getPlayPause().setImageResource(R.drawable.pause);
                }
                else{
                    srv.go();
                    controller.getPlayPause().setImageResource(R.drawable.play);
                }
            }
        });

        main_screen_shuffle_button = findViewById(R.id.main_screen_shuffle_button);
        main_screen_shuffle_button.setBorderColor(Color.WHITE);
        main_screen_shuffle_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                srv.setShuffle();
                shuffle = srv.shuffle;
                if(shuffle){
                    main_screen_shuffle_button.setBorderWidth(4);
                }
                else{
                    main_screen_shuffle_button.setBorderWidth(0);
                }
            }
        });

        main_screen_replay_button = findViewById(R.id.main_screen_replay_button);
        main_screen_replay_button.setBorderColor(Color.WHITE);
        main_screen_replay_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                srv.setReplay();
                replay = srv.replay;
                if(replay){
                    main_screen_replay_button.setImageResource(R.drawable.replay_1);
                }
                else{
                    main_screen_replay_button.setImageResource(R.drawable.replay);
                }
            }
        });
    }

    public void updateMainScreen(){
        Song currentSong;
        if(srv != null ){
            controller.getPlayPause().setImageResource(R.drawable.play);
            if(srv.getCurpos() == -1) {
                currentSong = list.get(0);
                controller.getPlayPause().setImageResource(R.drawable.pause);
            }
            else {
                currentSong = list.get(srv.getCurpos());
            }
        }
        else{
            currentSong = list.get(curSong);
            if(isPlaying()){
                controller.getPlayPause().setImageResource(R.drawable.play);
            }
            else{
                controller.getPlayPause().setImageResource(R.drawable.pause);
            }
        }
        TextView t = findViewById(R.id.main_screen_song_title);
        t.setText(currentSong.name);
        t = findViewById(R.id.main_screen_song_artist);
        t.setText(currentSong.artist);
        ImageView i = findViewById(R.id.main_screen_song_thumbnail);
        Bitmap thumbnail = BitmapFactory.decodeFile(currentSong.thumbnail);
        if (thumbnail == null) {
            i.setImageResource(R.drawable.replace_thumbnail);
        } else {
            i.setImageBitmap(thumbnail);
        }
        if(shuffle){
            main_screen_shuffle_button.setBorderWidth(4);
        }
        else{
            main_screen_shuffle_button.setBorderWidth(0);
        }

        if(replay){
            main_screen_replay_button.setImageResource(R.drawable.replay_1);
        }
        else{
            main_screen_replay_button.setImageResource(R.drawable.replay);
        }
        initSeekBar();
    }


    @Override
    public void start() {
        int pls = srv.go();
    }

    @Override
    public void pause() {
        int pls = srv.pausePlayer();
    }

    @Override
    public void seekTo(int pos) {
        srv.seek(pos);
    }

    @Override
    public int getDuration() {
        if(srv!=null && serviceBound && srv.isPng())
            return srv.getDur();
        else return 0;
    }

    @Override
    public int getCurrentPosition() {
        if(srv!=null && serviceBound && srv.isPng())
            return srv.getPosn();
        else return 0;
    }

    @Override
    public boolean isPlaying() {
        if( serviceBound)
        {
            return srv.isPng();
        }
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onStop() {
        //controller.hide();
        super.onStop();
    }
    public void setReplay()
    {
        if(replay==false) replay = true;
        else replay = false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.reset();
        updateMainScreen();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }
}


