package com.example.blmusicplayer;

import android.content.Context;
import android.widget.MediaController;
import android.widget.SeekBar;

import de.hdodenhof.circleimageview.CircleImageView;

/*
 * This is demo code to accompany the Mobiletuts+ series:
 * Android SDK: Creating a Music Player
 *
 * Sue Smith - February 2014
 */

public class MusicController {

    private CircleImageView shuffle;
    private CircleImageView prev;
    private CircleImageView playPause;
    private CircleImageView next;
    private CircleImageView replay;
    private SeekBar seekBar;

    public MusicController(CircleImageView sh, CircleImageView pr, CircleImageView pl, CircleImageView ne, CircleImageView re){
        shuffle = sh;
        prev = pr;
        playPause = pl;
        next = ne;
        replay = re;
    }

    public MusicController(CircleImageView pr, CircleImageView pl, CircleImageView ne){
        shuffle = null;
        prev = pr;
        playPause = pl;
        next = ne;
        replay = null;
    }

    public CircleImageView getNext() {
        return next;
    }

    public CircleImageView getShuffle() {
        return shuffle;
    }

    public CircleImageView getPlayPause() {
        return playPause;
    }

    public CircleImageView getPrev() {
        return prev;
    }

    public CircleImageView getReplay() {
        return replay;
    }

}