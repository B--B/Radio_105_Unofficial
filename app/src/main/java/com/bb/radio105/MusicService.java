/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2021 Zanin Marco (B--B)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bb.radio105;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.preference.PreferenceManager;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

import static android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY;
import static com.bb.radio105.Constants.ACTION_ERROR;
import static com.bb.radio105.Constants.ACTION_PAUSE;
import static com.bb.radio105.Constants.ACTION_PLAY;
import static com.bb.radio105.Constants.ACTION_STOP;
import static com.bb.radio105.Constants.VOLUME_DUCK;
import static com.bb.radio105.Constants.VOLUME_NORMAL;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;

/**
 * Service that handles media playback.
 */

public class MusicService extends MediaBrowserServiceCompat implements OnPreparedListener,
        OnErrorListener, AudioManager.OnAudioFocusChangeListener {

    private final PlayerIntentReceiver playerIntentReceiver = new PlayerIntentReceiver();

    // The notification color
//    private int mNotificationColor;

    // Notification metadata
    String titleString = null;
    String djString = null;
    String artUrl = null;
    private LruCache<String, Bitmap> mAlbumArtCache;
    private static final int MAX_ALBUM_ART_CACHE_SIZE = 1024*1024;
    Bitmap art;

    // SparseArray for notification actions
    private final SparseArray<PendingIntent> mIntents = new SparseArray<>();

    // The tag we put on debug messages
    private final static String TAG = "Radio105Player";
    private static final String CHANNEL_ID = "Radio105ServiceChannel";

    // our media player
    static MediaPlayer mPlayer = null;

    // Current local media player state
    static int mState = PlaybackStateCompat.STATE_STOPPED;

    // do we have audio focus?
    enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }

    // Type of audio focus we have:
    private AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

    private AudioManager mAudioManager;
    private boolean mPlayOnFocusGain;
    private AudioFocusRequest mFocusRequest;

    // Wifi lock that we hold when streaming files from the internet, in order to prevent the
    // device from shutting off the Wifi radio
    private WifiLock mWifiLock;

    // The ID we use for the notification (the onscreen alert that appears at the notification
    // area at the top of the screen as an icon -- and as text as well if the user expands the
    // notification area).
    final int NOTIFICATION_ID = 1;

    NotificationManagerCompat mNotificationManager;

    NotificationCompat.Builder mNotificationBuilder = null;

    // Media Session
    private MediaSessionCompat mSession;
    private PlaybackStateCompat.Builder stateBuilder;

    /**
     * Makes sure the media player exists and has been reset. This will create the media player
     * if needed, or reset the existing media player if one already exists.
     */
    private void createMediaPlayerIfNeeded() {
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();

            // Make sure the media player will acquire a wake-lock while playing. If we don't do
            // that, the CPU might go to sleep while the song is playing, causing playback to stop.
            //
            // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
            // permission in AndroidManifest.xml.
            mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            // we want the media player to notify us when it's ready preparing, and when it's done
            // playing:
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnErrorListener(this);
        } else
            mPlayer.reset();
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.tag(TAG).i("debug: Creating service");

        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        mWifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "radio105lock");

//        mNotificationColor = getNotificationColor();
        mNotificationManager = NotificationManagerCompat.from(this);

        String pkg = getPackageName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mIntents.put(R.drawable.ic_pause, PendingIntent.getForegroundService(this, 100,
                    new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            mIntents.put(R.drawable.ic_play, PendingIntent.getForegroundService(this, 100,
                    new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            mIntents.put(R.drawable.ic_stop, PendingIntent.getForegroundService(this, 100,
                    new Intent(ACTION_STOP).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        } else {
            mIntents.put(R.drawable.ic_pause, PendingIntent.getService(this, 100,
                    new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT));
            mIntents.put(R.drawable.ic_play, PendingIntent.getService(this, 100,
                    new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT));
            mIntents.put(R.drawable.ic_stop, PendingIntent.getService(this, 100,
                    new Intent(ACTION_STOP).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT));
        }

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Start a new MediaSession
        mSession = new MediaSessionCompat(this, "MusicService");
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE);
        mSession.setPlaybackState(stateBuilder.build());
        mSession.setCallback(mCallback);
        setSessionToken(mSession.getSessionToken());
        updatePlaybackState(null);

        mSession.setActive(true);

        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(playerIntentReceiver, mIntentFilter);

        // simple album art cache that holds no more than
        // MAX_ALBUM_ART_CACHE_SIZE bytes:
        mAlbumArtCache = new LruCache<String, Bitmap>(MAX_ALBUM_ART_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };

        // Get streaming metadata when service starts
        getStreamingMetadata();

        NetworkUtil.checkNetworkInfo(this, type -> {
            boolean pref1 = PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean(getString(R.string.network_change_key), true);
            if (pref1) {
                if (mState == PlaybackStateCompat.STATE_PLAYING) {
                    if (type) {
                        // Restart the stream. Don't use MediaSession callback as we are
                        // already onPlay and the stream will restart in a few moments
                        recoverStream();
                    }
                }
            }
        });
    }

    /**
     * Called when we receive an Intent. When we receive an intent sent to us via startService(),
     * this is the method that gets called. So here we react appropriately depending on the
     * Intent's action, which specifies what is being requested of us.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        switch (action) {
            case ACTION_PLAY:
                processPlayRequest();
                break;
            case ACTION_PAUSE:
                processPauseRequest();
                break;
            case ACTION_STOP:
                processStopRequest();
                break;
        }

        return START_NOT_STICKY; // Means we started the service, but don't want it to
        // restart in case it's killed.
    }

    private void processPlayRequest() {
        mPlayOnFocusGain = true;
        tryToGetAudioFocus();

        // actually play the song
        if (mState == PlaybackStateCompat.STATE_STOPPED) {
            // If we're stopped, just go ahead to the next song and start playing
            playNextSong();
        } else if (mState == PlaybackStateCompat.STATE_PAUSED) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            mState = PlaybackStateCompat.STATE_PLAYING;
            updatePlaybackState(null);
            updateNotification(getString(R.string.playing));
            configAndStartMediaPlayer();
        }
        if (!mSession.isActive()) {
            mSession.setActive(true);
        }
    }

    private void processPauseRequest() {
        if (mState == PlaybackStateCompat.STATE_PLAYING) {
            // Pause media player and cancel the 'foreground service' state.
            mState = PlaybackStateCompat.STATE_PAUSED;
            updatePlaybackState(null);
            mPlayer.pause();
            relaxResources();
            updateNotification(getString(R.string.in_pause));
            // do not give up audio focus
        }
    }

    private void processStopRequest() {
        if (mState == PlaybackStateCompat.STATE_PLAYING || mState == PlaybackStateCompat.STATE_PAUSED) {
            mState = PlaybackStateCompat.STATE_STOPPED;

            // let go of all resources...
            relaxResources(true);
            giveUpAudioFocus();
            updatePlaybackState(null);

            // service is no longer necessary. Will be started again if needed.
            stopSelf();
        }
    }

    /**
     * Releases resources used by the service for playback. This includes the "foreground service"
     * status and notification, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also be released or not
     */
    private void relaxResources(boolean releaseMediaPlayer) {
        // stop being a foreground service
        stopForeground(true);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }

        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock.isHeld()) mWifiLock.release();
    }

    private void relaxResources() {
        // we can release the Wifi lock, if we're holding it
        if (mWifiLock.isHeld()) mWifiLock.release();
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it. This
     * method starts/restarts the MediaPlayer respecting the current audio focus state. So if
     * we have focus, it will play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is allowed by the
     * current focus settings. This method assumes mPlayer != null, so if you are calling it,
     * you have to do so from a context where you are sure this is the case.
     */
    private void configAndStartMediaPlayer() {
        if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause, even if mState
            // is State.Playing. But we stay in the Playing state so that we know we have to resume
            // playback once we get the focus back.
            if (mPlayer.isPlaying()) mPlayer.pause();
            return;
        } else if (mAudioFocus == AudioFocus.NoFocusCanDuck) {
            mPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK); // we'll be relatively quiet
        } else {
            mPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL); // we can be loud again
        }
        // If we were playing when we lost focus, we need to resume playing.
        if (mPlayOnFocusGain) {
            if (!mPlayer.isPlaying()) {
                mPlayer.start();
            }
            mPlayOnFocusGain = false;
            mState = PlaybackStateCompat.STATE_PLAYING;
        }
    }

    /**
     * Starts playing the next song. If manualUrl is null, the next song will be randomly selected
     * from our Media Retriever (that is, it will be a random song in the user's device). If
     * manualUrl is non-null, then it specifies the URL or path to the song that will be played
     * next.
     */
    private void playNextSong() {
        mState = PlaybackStateCompat.STATE_STOPPED;
        relaxResources(false); // release everything except MediaPlayer
        String manualUrl = "http://icy.unitedradio.it/Radio105.mp3"; // initialize Uri here

        Thread thread = new Thread(() -> {
            try {
                createMediaPlayerIfNeeded();
                AudioAttributes.Builder b = new AudioAttributes.Builder();
                b.setUsage(AudioAttributes.USAGE_MEDIA);
                mPlayer.setAudioAttributes(b.build());
                mPlayer.setDataSource(manualUrl);

                mState = PlaybackStateCompat.STATE_BUFFERING;
                updatePlaybackState(null);
                setUpAsForeground(getString(R.string.loading));

                // starts preparing the media player in the background. When it's done, it will call
                // our OnPreparedListener (that is, the onPrepared() method on this class, since we set
                // the listener to 'this').
                //
                // Until the media player is prepared, we *cannot* call start() on it!
                mPlayer.prepare();

                // If we are streaming from the internet, we want to hold a Wifi lock, which prevents
                // the Wifi radio from going to sleep while the song is playing. If, on the other hand,
                // we are *not* streaming, we want to release the lock if we were holding it before.
                mWifiLock.acquire();
                if (mWifiLock.isHeld()) mWifiLock.release();
            } catch (IOException ex) {
                Timber.tag("MusicService").e("IOException playing next song: %s", ex.getMessage());
                updatePlaybackState(ex.getMessage());
            }
        });
        thread.start();
    }

    private void recoverStream() {
        mState = PlaybackStateCompat.STATE_STOPPED;
        updateNotification(getString(R.string.recovering));
        mPlayOnFocusGain = true;
        tryToGetAudioFocus();
        String manualUrl = "http://icy.unitedradio.it/Radio105.mp3"; // initialize Uri here

        Thread thread = new Thread(() -> {
            try {
                createMediaPlayerIfNeeded();
                AudioAttributes.Builder b = new AudioAttributes.Builder();
                b.setUsage(AudioAttributes.USAGE_MEDIA);
                mPlayer.setAudioAttributes(b.build());
                mPlayer.setDataSource(manualUrl);

                mState = PlaybackStateCompat.STATE_BUFFERING;
                updatePlaybackState(null);

                // starts preparing the media player in the background. When it's done, it will call
                // our OnPreparedListener (that is, the onPrepared() method on this class, since we set
                // the listener to 'this').
                //
                // Until the media player is prepared, we *cannot* call start() on it!
                mPlayer.prepare();

                // If we are streaming from the internet, we want to hold a Wifi lock, which prevents
                // the Wifi radio from going to sleep while the song is playing. If, on the other hand,
                // we are *not* streaming, we want to release the lock if we were holding it before.
                mWifiLock.acquire();
                if (mWifiLock.isHeld()) mWifiLock.release();
            } catch (IOException ex) {
                Timber.tag("MusicService").e("IOException playing next song: %s", ex.getMessage());
                updatePlaybackState(ex.getMessage());
            }
        });
        thread.start();
    }

    /**
     * Called when media player is done preparing.
     */
    public void onPrepared(MediaPlayer player) {
        // The media player is done preparing. That means we can start playing!
        mState = PlaybackStateCompat.STATE_PLAYING;
        // Start the foreground service here, notification colors will be wrong when the stream
        // starts from stopped state
        updatePlaybackState(null);
        updateNotification(getString(R.string.playing));
        configAndStartMediaPlayer();
    }

    /**
     * Updates the notification.
     */
    @SuppressLint({"UnspecifiedImmutableFlag"})
    private void updateNotification(String text) {
        boolean pref = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.notification_type_key), true);

        art = mAlbumArtCache.get(artUrl);
        if (art == null) {
            // use a placeholder art while the remote art is being downloaded
            art = BitmapFactory.decodeResource(getResources(), R.drawable.ic_radio_105_logo);
        }

        Intent intent = new Intent(this, MainActivity.class);
        // Use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        mNotificationBuilder.setContentIntent(pIntent);
        if (mState == PlaybackStateCompat.STATE_PLAYING) {
            mSession.setMetadata
                    (new MediaMetadataCompat.Builder()
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art)
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, titleString)
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, djString)
                            .build()
                    );
        } else {
            mSession.setMetadata
                    (new MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, getString(R.string.radio_105))
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, text)
                            .build()
                    );
        }
        mNotificationBuilder.clearActions();
        if (mState == PlaybackStateCompat.STATE_PLAYING) {
            mNotificationBuilder.addAction(R.drawable.ic_pause, getString(R.string.pause), mIntents.get(R.drawable.ic_pause));
            mNotificationBuilder.addAction(R.drawable.ic_stop, getString(R.string.stop), mIntents.get(R.drawable.ic_stop));
        } else if (mState == PlaybackStateCompat.STATE_PAUSED) {
            mNotificationBuilder.addAction(R.drawable.ic_play, getString(R.string.play), mIntents.get(R.drawable.ic_play));
            mNotificationBuilder.addAction(R.drawable.ic_stop, getString(R.string.stop), mIntents.get(R.drawable.ic_stop));
        } else if (mState == PlaybackStateCompat.STATE_STOPPED) {
            mNotificationBuilder.addAction(0, null, null);
            mNotificationBuilder.addAction(0, null, null);
        }
        if (!pref) {
            mNotificationBuilder.setContentText(text);
        }
        mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    /**
     * Configures service as a foreground service. A foreground service is a service that's doing
     * something the user is actively aware of (such as playing music), and must appear to the
     * user as a notification. That's why we create the notification here.
     */


    @SuppressLint("UnspecifiedImmutableFlag")
    private void setUpAsForeground(String text) {
        boolean pref = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.notification_type_key), true);

        // Get streaming metadata
        getStreamingMetadata();

        // Fetch the album art
        if (artUrl != null) {
            fetchBitmapFromURLThread(artUrl);
        }

        art = BitmapFactory.decodeResource(getResources(), R.drawable.ic_radio_105_logo);

        // Creating notification channel
        createNotificationChannel();
        Intent intent = new Intent(this, MainActivity.class);

        // Use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        // Building notification here
        mNotificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        if (pref) {
            mNotificationBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1)
                    .setMediaSession(mSession.getSessionToken()));
//            mNotificationBuilder.setColor(mNotificationColor);
        } else {
            mNotificationBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1));
            mNotificationBuilder.setContentText(text);
        }
        mNotificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_foreground));
        mNotificationBuilder.setSmallIcon(R.drawable.ic_radio105_notification);
        mNotificationBuilder.setContentTitle(getString(R.string.radio));
        mNotificationBuilder.setContentIntent(pIntent);
        mNotificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        mNotificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        mNotificationBuilder.addAction(0, null, null);
        mNotificationBuilder.addAction(0, null, null);
        mSession.setMetadata
                (new MediaMetadataCompat.Builder()
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, getString(R.string.radio_105))
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, text)
                        .build()
                );
        // Launch notification
        startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    /**
     * Called when there's an error playing media. When this happens, the media player goes to
     * the Error state. We warn the user about the error and reset the media player.
     */
    public boolean onError(MediaPlayer mp, int what, int extra) {
        boolean pref = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.reconnect_key), true);

        Toast.makeText(getApplicationContext(), getString(R.string.error),
                Toast.LENGTH_SHORT).show();
        Timber.tag(TAG).e("Error: what=" + what + ", extra=" + extra);

        mState = PlaybackStateCompat.STATE_STOPPED;
        relaxResources(true);
        giveUpAudioFocus();

        Intent mIntent = new Intent();
        mIntent.setAction(ACTION_ERROR);
        mIntent.setPackage(getPackageName());

        if (pref) {
            // Try to restart the service immediately if we have a working internet connection
            if (isDeviceOnline()) {
                Toast.makeText(getApplicationContext(), getString(R.string.reconnect),
                        Toast.LENGTH_SHORT).show();
                // Send the intent for buttons change
                Intent reconnect = new Intent();
                reconnect.setAction(ACTION_PLAY);
                reconnect.setPackage(getPackageName());
                sendBroadcast(reconnect);
                // Start the streaming
                mCallback.onPlay();
            } else {
                // Tell the user that streaming service cannot be recovered
                Toast.makeText(getApplicationContext(), getString(R.string.no_reconnect),
                        Toast.LENGTH_SHORT).show();
                // Send the error intent
                sendBroadcast(mIntent);
            }
        } else {
            sendBroadcast(mIntent);
        }
        return true; // true indicates we handled the error
    }

    @Override
    public void onDestroy() {
        // Service is being killed, so make sure we release our resources
        mState = PlaybackStateCompat.STATE_STOPPED;
        relaxResources(true);
        giveUpAudioFocus();
        NetworkUtil.unregisterNetworkCallback();
        unregisterReceiver(playerIntentReceiver);
        titleString = null;
        djString = null;
        artUrl = null;
        art = null;
        // Always release the MediaSession to clean up resources
        // and notify associated MediaController(s).
        mSession.release();
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        // Clients can connect, but this BrowserRoot is an empty hierarchy
        // so onLoadChildren returns nothing. This disables the ability to browse for content.
        return new BrowserRoot(getString(R.string.app_name), null);
    }

    @Override
    public void onLoadChildren(@NonNull @NotNull String parentId, @NonNull @NotNull MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> result) {
        //  Browsing not allowed
        if (TextUtils.equals(getString(R.string.app_name), parentId)) {
            result.sendResult(null);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        boolean pref = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.service_kill_key), false);
        if (pref) {
            // Stop music service when the user enabled the option
            mCallback.onStop();
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // We have gained focus:
            mAudioFocus = AudioFocus.Focused;
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // We have lost focus. If we can duck (low playback volume), we can keep playing.
            // Otherwise, we need to pause the playback.
            boolean canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;
            // If we are playing, we need to reset media player by calling configMediaPlayerState
            // with mAudioFocus properly set.
            if (mState == PlaybackStateCompat.STATE_PLAYING && !canDuck) {
                // If we don't have audio focus and can't duck, we save the information that
                // we were playing, so that we can resume playback once we get the focus back.
                mPlayOnFocusGain = true;
            }
        }
        configAndStartMediaPlayer();
    }

    private boolean isDeviceOnline() {
        // Try to connect to CloudFlare DNS socket, return true if success
        final AtomicBoolean deviceOnline = new AtomicBoolean(false);
        Thread thread = new Thread(() -> {
            try {
                int timeout = 1500;
                Socket sock = new Socket();
                SocketAddress mSocketAddress = new InetSocketAddress("1.1.1.1", 53);
                sock.connect(mSocketAddress, timeout);
                sock.close();
                deviceOnline.set(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return deviceOnline.get();
    }

    private void getStreamingMetadata() {
        RequestQueue requestQueue;
        // Instantiate the cache
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap
        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());
        // Instantiate the RequestQueue with the cache and network.
        requestQueue = new RequestQueue(cache, network);
        // Start the queue
        requestQueue.start();

        String url ="https://www.105.net/custom_widget/finelco/onair_105.jsp?ajax=true";
        // Formulate the request and handle the response.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    Log.e("Radio105Metadata","Received metadata: " + response);
                    Document document = Jsoup.parse(response);
                    Element titleElement = document.selectFirst(".nome");
                    Element djElement = document.selectFirst(".dj_in_onda");
                    Element artElement = document.selectFirst("img");
                    titleString = titleElement.text();
                    djString = djElement.text();
                    artUrl = artElement.absUrl("src");
                    Log.e("Radio105Metadata","Received title: " + titleString);
                    Log.e("Radio105Metadata","Received dj name: " + djString);
                    Log.e("Radio105Metadata","Received bitmap url: " + artUrl);
                },
                error -> {
                    // Handle error
                });
        // Add the request to the RequestQueue.
        requestQueue.add(stringRequest);
    }

    void fetchBitmapFromURLThread(final String source) {
        new Thread(() -> {
            Bitmap bitmap;
            try {
                bitmap = BitmapHelper.fetchAndRescaleBitmap(source,
                        BitmapHelper.MEDIA_ART_WIDTH, BitmapHelper.MEDIA_ART_HEIGHT);
                mAlbumArtCache.put(source, bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Try to get the system audio focus.
     */
    void tryToGetAudioFocus() {
        if (mAudioFocus != AudioFocus.Focused) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mFocusRequest = (new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(
                                new AudioAttributes.Builder()
                                        .setUsage(AudioAttributes.USAGE_MEDIA)
                                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                        .build()
                        )
                        .build()
                );
                if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager.requestAudioFocus(mFocusRequest)) {
                    mAudioFocus = AudioFocus.Focused;
                }
            } else {
                int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN);
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    mAudioFocus = AudioFocus.Focused;
                }
            }
        }
    }

    /**
     * Give up the audio focus.
     */
    void giveUpAudioFocus() {
        if (mAudioFocus == AudioFocus.Focused) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager.abandonAudioFocusRequest(mFocusRequest)) {
                    mAudioFocus = AudioFocus.NoFocusNoDuck;
                }
            } else {
                if (mAudioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    mAudioFocus = AudioFocus.NoFocusNoDuck;
                }
            }
        }
    }

/*
    protected int getNotificationColor() {
        int notificationColor = 0;
        String packageName = getPackageName();
        try {
            Context packageContext = createPackageContext(packageName, 0);
            ApplicationInfo applicationInfo =
                    getPackageManager().getApplicationInfo(packageName, 0);
            packageContext.setTheme(applicationInfo.theme);
            Resources.Theme theme = packageContext.getTheme();
            TypedArray ta = theme.obtainStyledAttributes(
                    new int[]{android.R.attr.colorPrimary});
            notificationColor = ta.getColor(0, Color.DKGRAY);
            ta.recycle();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return notificationColor;
    }
*/

    /**
     * Update the current media player state, optionally showing an error message.
     *
     * @param error if not null, error message to present to the user.
     *
     */
    private void updatePlaybackState(String error) {
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        if (mPlayer != null && mPlayer.isPlaying()) {
            position = mPlayer.getCurrentPosition();
        }
//        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
//                .setActions(getAvailableActions());
//        setCustomAction(stateBuilder);
        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(PlaybackStateCompat.ERROR_CODE_UNKNOWN_ERROR, error);
            mState = PlaybackStateCompat.STATE_ERROR;
        }
        stateBuilder.setState(mState, position, 1.0f, SystemClock.elapsedRealtime());
        mSession.setPlaybackState(stateBuilder.build());
    }

    // *********  MediaSession.Callback implementation:
    private final MediaSessionCompat.Callback mCallback = new MediaSessionCompat.Callback() {

        @Override
        public void onPlay() {
            Intent mIntent = new Intent();
            mIntent.setAction(ACTION_PLAY);
            mIntent.setPackage(getPackageName());
            startService(mIntent);
        }

        @Override
        public void onPause() {
            Intent mIntent = new Intent();
            mIntent.setAction(ACTION_PAUSE);
            mIntent.setPackage(getPackageName());
            startService(mIntent);
        }

        @Override
        public void onStop() {
            Intent mIntent = new Intent();
            mIntent.setAction(ACTION_STOP);
            mIntent.setPackage(getPackageName());
            startService(mIntent);
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
//            Log.e("Radio105Streaming", "Received intent extras: " + mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT).toString());
            KeyEvent mKeyEvent = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (mKeyEvent.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                mCallback.onPause();
            }
            return super.onMediaButtonEvent(mediaButtonEvent);
        }
    };
}
