package com.bb.radio105;

import android.graphics.Bitmap;
import android.support.v4.media.session.MediaSessionCompat;

interface IMusicService {
    MediaSessionCompat.Token getMediaSessionToken();
    Bitmap getArt();
    String getTitleString();
    String getDjString();
    int getPlaybackState();
    // void pauseStreaming();
}
