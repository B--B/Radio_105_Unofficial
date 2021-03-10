/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

/**
 * Service that handles media playback.
 */

public class MusicService extends Service implements OnCompletionListener, OnPreparedListener,
        OnErrorListener {

    // The tag we put on debug messages
    final static String TAG = "Radio105Player";
    private static final String CHANNEL_ID = "Radio105ServiceChannel";

    // Intent receiver for ACTION_AUDIO_BECOMING_NOISY
    private final PlayerIntentReceiver playerIntentReceiver = new PlayerIntentReceiver();

    // our media player
    static MediaPlayer mPlayer = null;

    // indicates the state our service:
    enum State {
        Stopped,    // media player is stopped and not prepared to play
        Preparing,  // media player is preparing...
        Playing,    // playback active (media player ready!). (but the media player may actually be
        // paused in this state if we don't have audio focus. But we stay in this state
        // so that we know we have to resume playback once we get focus back)
        Paused      // playback paused (media player ready!)
    }

    static State mState = State.Stopped;

    // title of the song we are currently playing
    final String mSongTitle = "Radio 105 Streaming";

    // Wifi lock that we hold when streaming files from the internet, in order to prevent the
    // device from shutting off the Wifi radio
    WifiLock mWifiLock;

    // The ID we use for the notification (the onscreen alert that appears at the notification
    // area at the top of the screen as an icon -- and as text as well if the user expands the
    // notification area).
    final int NOTIFICATION_ID = 1;

    NotificationManagerCompat mNotificationManager;

    NotificationCompat.Builder mNotificationBuilder = null;

    /**
     * Makes sure the media player exists and has been reset. This will create the media player
     * if needed, or reset the existing media player if one already exists.
     */
    void createMediaPlayerIfNeeded() {
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
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnErrorListener(this);
        }
        else
            mPlayer.reset();
    }

    @Override
    public void onCreate() {
        Timber.tag(TAG).i("debug: Creating service");

        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        mWifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "radio105lock");

        mNotificationManager = NotificationManagerCompat.from(this);

        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(playerIntentReceiver, mIntentFilter);

        NetworkUtil.checkNetworkInfo(this, type -> {{
            boolean pref1 = PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean(getString(R.string.network_change_key), true);
            if (pref1) {
                if (MusicService.mState == MusicService.State.Playing) {
                    if (type)
                        // Restart the stream
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
            case Constants.ACTION_PLAY:
                processPlayRequest();
                sendBroadcast(intent);
                break;
            case Constants.ACTION_PLAY_NOTIFICATION:
                processPlayRequestNotification();
                sendBroadcast(intent);
                break;
            case Constants.ACTION_PAUSE:
            case Constants.ACTION_PAUSE_NOTIFICATION:
                processPauseRequest();
                sendBroadcast(intent);
                break;
            case Constants.ACTION_STOP:
            case Constants.ACTION_STOP_NOTIFICATION:
                processStopRequest();
                sendBroadcast(intent);
                break;
        }

        return START_NOT_STICKY; // Means we started the service, but don't want it to
        // restart in case it's killed.
    }

    void processPlayRequest() {
        // actually play the song

        if (mState == State.Stopped) {
            // If we're stopped, just go ahead to the next song and start playing
            playNextSong();
        }
        else if (mState == State.Paused) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            mState = State.Playing;
            setUpAsForeground(mSongTitle + getString(R.string.playing));
            configAndStartMediaPlayer();
        }
    }

    void processPlayRequestNotification() {
        mState = State.Playing;
        updateNotification(mSongTitle + getString(R.string.playing));
        configAndStartMediaPlayer();
    }

    void processPauseRequest() {
        if (mState == State.Playing) {
            boolean pref = PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean(getString(R.string.notification_key), false);
            // Pause media player and cancel the 'foreground service' state.
            mState = State.Paused;
            mPlayer.pause();
            if (!pref) {
                relaxResources(false); // while paused, we always retain the MediaPlayer
            } else {
                updateNotification(mSongTitle + getString(R.string.in_pause));
                relaxResources();
            }
            // do not give up audio focus
        }
    }

    void processStopRequest() {
        processStopRequest(false);
    }

    void processStopRequest(boolean force) {
        if (mState == State.Playing || mState == State.Paused || force) {
            mState = State.Stopped;

            // let go of all resources...
            relaxResources(true);

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
    void relaxResources(boolean releaseMediaPlayer) {
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

    void relaxResources() {
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
    void configAndStartMediaPlayer() {
        if (!mPlayer.isPlaying()) mPlayer.start();
    }


    /**
     * Starts playing the next song. If manualUrl is null, the next song will be randomly selected
     * from our Media Retriever (that is, it will be a random song in the user's device). If
     * manualUrl is non-null, then it specifies the URL or path to the song that will be played
     * next.
     */
    void playNextSong() {
        mState = State.Stopped;
        relaxResources(false); // release everything except MediaPlayer
        String manualUrl = "http://icy.unitedradio.it/Radio105.mp3"; // initialize Uri here

        Thread thread = new Thread(() -> {
            try {
                createMediaPlayerIfNeeded();
                AudioAttributes.Builder b = new AudioAttributes.Builder();
                b.setUsage(AudioAttributes.USAGE_MEDIA);
                mPlayer.setAudioAttributes(b.build());
                mPlayer.setDataSource(manualUrl);

                mState = State.Preparing;
                setUpAsForeground(mSongTitle + getString(R.string.loading));

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
                ex.printStackTrace();
            }
        });
        thread.start();
    }

    void recoverStream() {
        mState = State.Stopped;
        String manualUrl = "http://icy.unitedradio.it/Radio105.mp3"; // initialize Uri here

        Thread thread = new Thread(() -> {
            try {
                createMediaPlayerIfNeeded();
                AudioAttributes.Builder b = new AudioAttributes.Builder();
                b.setUsage(AudioAttributes.USAGE_MEDIA);
                mPlayer.setAudioAttributes(b.build());
                mPlayer.setDataSource(manualUrl);

                mState = State.Preparing;
                setUpAsForeground(mSongTitle + getString(R.string.loading));

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
                ex.printStackTrace();
            }
        });
        thread.start();
    }

    /** Called when media player is done playing current song. */
    public void onCompletion(MediaPlayer player) {
        // The media player finished playing the current song, so we go ahead and start the next.
        playNextSong();
    }

    /** Called when media player is done preparing. */
    public void onPrepared(MediaPlayer player) {
        // The media player is done preparing. That means we can start playing!
        mState = State.Playing;
        updateNotification(mSongTitle + getString(R.string.playing));
        configAndStartMediaPlayer();
    }

    /** Updates the notification. */
    @SuppressLint("RestrictedApi")
    void updateNotification(String text) {
        boolean pref = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.notification_key), false);
        Intent intent = new Intent(this, MainActivity.class);
        // Use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mNotificationBuilder.setContentText(text);
        mNotificationBuilder.setContentIntent(pIntent);
        if (pref) {
            //Intent for Play
            Intent playIntent = new Intent();
            playIntent.setAction(Constants.ACTION_PLAY_NOTIFICATION);
            PendingIntent mPlayIntent = PendingIntent.getService(this, 100, playIntent, 0);

            //Intent for Pause
            Intent pauseIntent = new Intent();
            pauseIntent.setAction(Constants.ACTION_PAUSE_NOTIFICATION);
            PendingIntent mPauseIntent = PendingIntent.getService(this, 101, pauseIntent, 0);

            //Intent for Stop
            Intent stopIntent = new Intent();
            stopIntent.setAction(Constants.ACTION_STOP_NOTIFICATION);
            PendingIntent mStopIntent = PendingIntent.getService(this, 102, stopIntent, 0);

            mNotificationBuilder.mActions.clear();
            if (mState == State.Playing) {
                mNotificationBuilder.addAction(R.drawable.ic_pause, getString(R.string.pause), mPauseIntent);
                mNotificationBuilder.addAction(R.drawable.ic_stop, getString(R.string.stop), mStopIntent);
            } else if (mState == State.Paused) {
                mNotificationBuilder.addAction(R.drawable.ic_play, getString(R.string.play), mPlayIntent);
                mNotificationBuilder.addAction(R.drawable.ic_stop, getString(R.string.stop), mStopIntent);
            }
        }
        mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    /**
     * Configures service as a foreground service. A foreground service is a service that's doing
     * something the user is actively aware of (such as playing music), and must appear to the
     * user as a notification. That's why we create the notification here.
     */


    void setUpAsForeground(String text) {
        //Intent for Pause
        Intent pauseIntent = new Intent();
        pauseIntent.setAction(Constants.ACTION_PAUSE_NOTIFICATION);
        PendingIntent mPauseIntent = PendingIntent.getService(this, 101, pauseIntent, 0);

        //Intent for Stop
        Intent stopIntent = new Intent();
        stopIntent.setAction(Constants.ACTION_STOP_NOTIFICATION);
        PendingIntent mStopIntent = PendingIntent.getService(this, 102, stopIntent, 0);

        // Creating notification channel
        createNotificationChannel();
        Intent intent = new Intent(this, MainActivity.class);
        // Use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // Building notification here
        mNotificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        mNotificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_foreground));
        mNotificationBuilder.setSmallIcon(R.drawable.ic_radio105_notification);
        mNotificationBuilder.setContentTitle(getString(R.string.radio));
        mNotificationBuilder.setContentText(text);
        mNotificationBuilder.setContentIntent(pIntent);
        mNotificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        mNotificationBuilder.addAction(R.drawable.ic_pause, getString(R.string.pause), mPauseIntent);
        mNotificationBuilder.addAction(R.drawable.ic_stop, getString(R.string.stop), mStopIntent);
        // Launch notification
        startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    /**
     * Called when there's an error playing media. When this happens, the media player goes to
     * the Error state. We warn the user about the error and reset the media player.
     */
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(getApplicationContext(), getString(R.string.error),
                Toast.LENGTH_SHORT).show();
        Timber.tag(TAG).e("Error: what=" + what + ", extra=" + extra);

        mState = State.Stopped;
        relaxResources(true);

        Intent mIntent = new Intent();
        mIntent.setAction(Constants.ACTION_ERROR);
        mIntent.setPackage(getPackageName());
        sendBroadcast(mIntent);

        boolean pref = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.reconnect_key), true);
        if (pref) {
            // Try to restart the service immediately if we have a working internet connection
            if (isDeviceOnline()) {
                Toast.makeText(getApplicationContext(), getString(R.string.reconnect),
                        Toast.LENGTH_SHORT).show();
                // Send the intent for buttons change
                Intent reconnect = new Intent();
                reconnect.setAction(Constants.ACTION_PLAY);
                reconnect.setPackage(getPackageName());
                sendBroadcast(reconnect);
                // Start the streaming
                processPlayRequest();
            } else {
                // Tell the user that streaming service cannot be recovered
                Toast.makeText(getApplicationContext(), getString(R.string.no_reconnect),
                        Toast.LENGTH_SHORT).show();
            }
        }
        return true; // true indicates we handled the error
    }

    @Override
    public void onDestroy() {
        // Service is being killed, so make sure we release our resources
        mState = State.Stopped;
        relaxResources(true);
        unregisterReceiver(playerIntentReceiver);
        NetworkUtil.unregisterNetworkCallback();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
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
            mState = State.Stopped;
            relaxResources(true);
            stopSelf();
        }
        super.onTaskRemoved(rootIntent);
    }

    public boolean isDeviceOnline() {
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
}
