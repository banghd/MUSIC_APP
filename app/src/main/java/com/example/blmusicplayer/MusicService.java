package com.example.blmusicplayer;



import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class MusicService extends Service implements  MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener{
    public MediaPlayer player;
    private ArrayList<Song> songlist;
    private int curpos;
    private final IBinder binder = new MusicBinder();
    private boolean iscomplete = false;
    public boolean shuffle = false;
    public boolean replay = false;

    NotificationManager notificationManager;
    NotificationCompat.Builder builder;
    int NOTIFICATION_ID = 02022000;
    String CHANNEL_ID = "02022000";

    RemoteViews notificationLayout;


    public final String ACTION_PLAY_PAUSE = "play_pause";
    public final String ACTION_PREVIOUS = "previous";
    public final String ACTION_NEXT = "next";
    public final String ACTION_CANCEL = "cancel";

    public void onCreate(){
        super.onCreate();
        curpos = 0;
        player = new MediaPlayer();
        initPlayer();
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("action");
            switch (action){
                case ACTION_PLAY_PAUSE:{
                    if(isPng()) {
                        pausePlayer();
                        setNotification();
                    }
                    else {
                        go();
                        setNotification();
                    }
                }
                break;
                case ACTION_NEXT:{
                    try{
                        playNext();
                    }
                    catch (IOException e){

                    }
                }
                break;
                case ACTION_PREVIOUS:{
                    try{
                        playPrev();
                    }
                    catch (IOException e){

                    }
                }
                break;
                case ACTION_CANCEL:{
                    notificationManager.cancelAll();
                }
                break;
            }
        }
    };


    public void setNotification(){
        notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "18082000";
            String Description = "18082000";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mChannel.setDescription(Description);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            mChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(mChannel);
        }
        registerReceiver(broadcastReceiver, new IntentFilter("TRACK_BACKS"));
        notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_layout);
        updateNotificationLayout();

        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("whatever")
                .setContentText("whatever")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setCustomContentView(notificationLayout);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public void updateNotificationLayout(){
        notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_layout);

        Intent playPauseIntent = new Intent(this, NotificationReceiver.class);
        playPauseIntent.setAction(ACTION_PLAY_PAUSE);
        PendingIntent playPausePendingIntent = PendingIntent.getBroadcast(this, 0, playPauseIntent, 0);
        notificationLayout.setOnClickPendingIntent(R.id.notification_play_pause, playPausePendingIntent);

        Intent prevIntent = new Intent(this, NotificationReceiver.class);
        prevIntent.setAction(ACTION_PREVIOUS);
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this, 0, prevIntent, 0);
        notificationLayout.setOnClickPendingIntent(R.id.notification_prev, prevPendingIntent);

        Intent nextIntent = new Intent(this, NotificationReceiver.class);
        nextIntent.setAction(ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 0, nextIntent, 0);
        notificationLayout.setOnClickPendingIntent(R.id.notification_next, nextPendingIntent);

        Intent cancelIntent = new Intent(this, NotificationReceiver.class);
        cancelIntent.setAction(ACTION_CANCEL);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(this, 0, cancelIntent, 0);
        notificationLayout.setOnClickPendingIntent(R.id.notification_cancel, cancelPendingIntent);

        if(getPosn() != 0) {
            if (isPng()) {
                notificationLayout.setImageViewResource(R.id.notification_play_pause, R.drawable.play);
            } else {
                notificationLayout.setImageViewResource(R.id.notification_play_pause, R.drawable.pause);
            }
        }
        boolean flag = false;
        if(curpos == -1){
            curpos = 0;
            flag= true;
        }
        notificationLayout.setTextViewText(R.id.notification_song_title, songlist.get(curpos).name);
        notificationLayout.setTextViewText(R.id.notification_song_artist, songlist.get(curpos).artist);
        Bitmap thumbnail = BitmapFactory.decodeFile(songlist.get(curpos).thumbnail);
        if(thumbnail == null){
            notificationLayout.setImageViewResource(R.id.notification_thumbnail, R.drawable.replace_thumbnail);
        }
        else{
            notificationLayout.setImageViewBitmap(R.id.notification_thumbnail, thumbnail);
        }
        if(flag){
            curpos = -1;
            flag = false;
        }

    }



    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent){
        player.stop();
        player.release();
        return false;
    }

    public void initPlayer(){
        player.reset();
        player.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        //player.reset();
    }

    public void setSongList(ArrayList<Song> songs){
        songlist = songs;
        curpos = -1;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.reset();
        if(replay){

        }
        else if(shuffle){
            Random  ran = new Random();
            curpos = ran.nextInt(songlist.size());
        }
        else{
            curpos++;
            if (curpos == songlist.size())
                curpos = 0;
        }
        try {
            startPlaying();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void startPlaying() throws IOException {
        player.reset();
        Song playSong = songlist.get(curpos);
        long currSong = playSong.id;
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);

        try{
            player.setDataSource(getApplicationContext(), trackUri);
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
        player.prepare();
        setNotification();
    }

    public void playPrev() throws IOException {
        if(shuffle)
        {
            Random  ran = new Random();
            curpos = ran.nextInt(songlist.size());
        }
        else {
            curpos--;
            if (curpos < 0)
                curpos = songlist.size() - 1;
        }
        startPlaying();
    }

    public void playNext() throws IOException {
        if(shuffle)
        {
            Random  ran = new Random();
            curpos = ran.nextInt(songlist.size());
        }
        else {
            curpos++;
            if (curpos == songlist.size())
                curpos = 0;
        }
        startPlaying();
    }
    public int getCurpos() { return this.curpos; }
    public void setCurpos(int pos){
        curpos = pos;
    }

    public int getPosn(){
        return player.getCurrentPosition();
    }

    public int getDur(){
        return player.getDuration();
    }

    public boolean isPng(){
        return player.isPlaying();
    }

    public int pausePlayer(){
        player.pause();
        setNotification();
        return curpos;
    }

    public void seek(int posn){
        player.seekTo(posn);
    }

    public int go(){
        player.start();
        setNotification();
        return  curpos;
    }
    public  void setShuffle()
    {
        if(shuffle == false) shuffle=true;
        else shuffle =false;
    }
    public void setReplay()
    {
        if(replay == false) replay = true;
        else  replay = false;
    }
}
