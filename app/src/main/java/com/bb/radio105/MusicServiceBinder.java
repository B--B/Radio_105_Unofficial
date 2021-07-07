package com.bb.radio105;

import static com.bb.radio105.MusicService.art;
import static com.bb.radio105.MusicService.djString;
import static com.bb.radio105.MusicService.mState;
import static com.bb.radio105.MusicService.mToken;
import static com.bb.radio105.MusicService.titleString;

import android.graphics.Bitmap;
import android.os.Binder;
import android.support.v4.media.session.MediaSessionCompat;

class MusicServiceBinder extends Binder {
    /** methods for clients */
    public MediaSessionCompat.Token getMediaSessionToken() {
        return mToken;
    }

    public Bitmap getArt() {
        return art;
    }

    public String getTitleString() {
        return titleString;
    }

    public String getDjString() {
        return djString;
    }

    public int getPlaybackState() {
        return mState;
    }
}
