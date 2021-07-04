package com.bb.radio105;

import static com.bb.radio105.MusicService.art;
import static com.bb.radio105.MusicService.djString;
import static com.bb.radio105.MusicService.mState;
import static com.bb.radio105.MusicService.mToken;
import static com.bb.radio105.MusicService.titleString;

import android.graphics.Bitmap;
import android.os.Binder;
import android.support.v4.media.session.MediaSessionCompat;

class MusicServiceBinder extends Binder implements IMusicService {
    /** methods for clients */
    @Override
    public MediaSessionCompat.Token getMediaSessionToken() {
        return mToken;
    }

    @Override
    public Bitmap getArt() {
        return art;
    }

    @Override
    public String getTitleString() {
        return titleString;
    }

    @Override
    public String getDjString() {
        return djString;
    }

    @Override
    public int getPlaybackState() {
        return mState;
    }

   // @Override
   // public void pauseStreaming() {
   //     processPauseRequest();
   // }
}
