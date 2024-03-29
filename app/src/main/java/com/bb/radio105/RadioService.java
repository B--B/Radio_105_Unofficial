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
import android.app.Service;
import android.content.BroadcastReceiver;
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
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.session.MediaButtonReceiver;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.Calendar;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

import static android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY;
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

public class RadioService extends Service implements OnPreparedListener,
        OnErrorListener, AudioManager.OnAudioFocusChangeListener {

    // Notification metadata
    static String titleString = null;
    static String djString = null;
    private String artUrl = null;
    private Bitmap placeHolder;
    static Bitmap art;
    private Bitmap smallIcon;
    private String artUrlResized;
    private volatile boolean mAudioNoisyReceiverRegistered;
    static boolean fromPauseState = false;
    private boolean fromErrorState = false;

    // Binder given to clients
    private final IBinder mIBinder = new RadioServiceBinder();

    // MediaSession Token
    static MediaSessionCompat.Token mToken;

    // Metadata scheduler
    private ScheduledExecutorService scheduler;

    // The service channel ID
    private static final String CHANNEL_ID = "Radio105ServiceChannel";

    // our media player
    private MediaPlayer mPlayer = null;

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

    // AudioNoisy intent filter
    private final IntentFilter mAudioNoisyIntentFilter = new IntentFilter(ACTION_AUDIO_BECOMING_NOISY);

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
        Timber.i("debug: Creating service");

        // Register a network callback for devices running N and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NetworkUtil.registerNetworkCallback(getApplicationContext());
        }

        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mWifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                    .createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "WARNING:radio105lock");
        } else {
            mWifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                    .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "WARNING:radio105lock");
        }

        mNotificationManager = NotificationManagerCompat.from(this);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Start a new MediaSession
        mSession = new MediaSessionCompat(this, "RadioService");
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_STOP);
        mSession.setPlaybackState(stateBuilder.build());
        mSession.setCallback(mCallback);
        updatePlaybackState(null);

        mSession.setActive(true);
        mToken = mSession.getSessionToken();

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null, getApplicationContext(), MediaButtonReceiver.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mSession.setMediaButtonReceiver(PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, PendingIntent.FLAG_IMMUTABLE));
        } else {
            mSession.setMediaButtonReceiver(PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, 0));
        }

        // Set the PlaceHolder when service starts
        placeHolder = BitmapFactory.decodeResource(getResources(), R.drawable.ic_radio_105_logo);
    }

    /**
     * Called when we receive an Intent. When we receive an intent sent to us via startService(),
     * this is the method that gets called. So here we react appropriately depending on the
     * Intent's action, which specifies what is being requested of us.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mSession, intent);
        return START_NOT_STICKY;
    }

    private void processPlayRequest() {
        mPlayOnFocusGain = true;
        tryToGetAudioFocus();
        registerAudioNoisyReceiver();

        // Get streaming metadata
        getStreamingMetadata();
        if (scheduler == null || scheduler.isTerminated()) {
            // Set the task for retrieving the metadata every hour
            scheduler = Executors.newSingleThreadScheduledExecutor();
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Rome"));
            long initialDelay = millisToNextHourWithDelay(calendar);
            Timber.i("Scheduler will run with an initial delay of %s", initialDelay);
            final long delayBetweenExecutions = 60 * 60 * 1000;
            scheduler.scheduleWithFixedDelay(this::getStreamingMetadata, initialDelay, delayBetweenExecutions, TimeUnit.MILLISECONDS);
        }

        // actually play the song
        if (mState == PlaybackStateCompat.STATE_STOPPED || mState == PlaybackStateCompat.STATE_ERROR) {

            // If we're stopped, just go ahead and start playing
            playNextSong();
        } else if (mState == PlaybackStateCompat.STATE_PAUSED) {
            // If we're paused, just continue playback.
            mState = PlaybackStateCompat.STATE_PLAYING;
            updatePlaybackState(null);
            updateNotification(getString(R.string.playing));
            configAndStartMediaPlayer();
            // Acquire the WiFi lock
            mWifiLock.acquire();
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
            // we can release the Wifi lock, if we're holding it
            if (mWifiLock.isHeld()) mWifiLock.release();
            updateNotification(getString(R.string.in_pause));
            // do not give up audio focus, but unregister AudioNoisyReceiver
            unregisterAudioNoisyReceiver();
        }
        fromPauseState = true;
    }

    private void processStopRequest() {
        if (scheduler != null) {
            scheduler.shutdown();
            Timber.i("Stopped metadata scheduler");
        }
        if (mState == PlaybackStateCompat.STATE_PLAYING || mState == PlaybackStateCompat.STATE_PAUSED || mState == PlaybackStateCompat.STATE_BUFFERING) {
            mState = PlaybackStateCompat.STATE_STOPPED;

            // let go of all resources...
            relaxResources(true);
            giveUpAudioFocus();
            updatePlaybackState(null);
            unregisterAudioNoisyReceiver();
        }
        fromPauseState = false;
    }

    /**
     * Releases resources used by the service for playback. This includes the "foreground service"
     * status and notification, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseForegroundService Indicates whether the foreground service should also be released or not
     */
    private void relaxResources(boolean releaseForegroundService) {
        // stop being a foreground service
        if (releaseForegroundService) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
        }

        // stop and release the Media Player, if it's available
        if (mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }

        // we can also release the Wifi lock, if we're holding it
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
        boolean isUsingHTTPS = Utils.getUserPreferenceBoolean(this, getString(R.string.https_key), true);
        String manualUrl;
        if (isUsingHTTPS) {
            manualUrl = "https://icy.unitedradio.it/Radio105.mp3"; // initialize Uri here
        } else {
            manualUrl = "http://icy.unitedradio.it/Radio105.mp3"; // initialize Uri here
        }
        Timber.i("Starting RadioService with url: %s", manualUrl);

        Thread thread = new Thread(() -> {
            try {
                createMediaPlayerIfNeeded();
                AudioAttributes.Builder b = new AudioAttributes.Builder();
                b.setUsage(AudioAttributes.USAGE_MEDIA);
                mPlayer.setAudioAttributes(b.build());
                mPlayer.setDataSource(manualUrl);

                mState = PlaybackStateCompat.STATE_BUFFERING;
                updatePlaybackState(null);
                if (!fromErrorState) {
                    setUpAsForeground(getString(R.string.loading));
                } else {
                    updateNotification(getString(R.string.loading));
                }

                // starts preparing the media player in the background. When it's done, it will call
                // our OnPreparedListener (that is, the onPrepared() method on this class, since we set
                // the listener to 'this').
                //
                // Until the media player is prepared, we *cannot* call start() on it!
                mPlayer.prepare();

                // Acquire the WiFi lock
                mWifiLock.acquire();

                // Reset fromErrorState boolean
                fromErrorState = false;
            } catch (IOException ex) {
                Timber.e("IOException playing next song: %s", ex.getMessage());
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
    @SuppressLint({"UnspecifiedImmutableFlag", "MissingPermission"})
    private void updateNotification(String text) {
        String artUri = null;
        if (artUrl != null) {
            artUri = artUrl.replaceAll("(resizer/)[^&]*(/true)", "$1800/800$2");
        }
        if (art == null) {
            // use a placeholder art while the remote art is being downloaded
            art = placeHolder;
            smallIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_podcast_logo);
        }
        if (titleString == null) {
            titleString = getString(R.string.radio_105);
        }
        if (djString == null) {
            djString = text;
        }

        if (djString.contains("Marco Mazzoli")) {
            placeHolder = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_zoo_logo);
            mNotificationBuilder.setSmallIcon(R.drawable.ic_zoo_notification);
        } else {
            placeHolder = BitmapFactory.decodeResource(getResources(), R.drawable.ic_radio_105_logo);
            mNotificationBuilder.setSmallIcon(R.drawable.ic_radio105_notification);
        }

        Intent intent = Objects.requireNonNull(getPackageManager()
                        .getLaunchIntentForPackage(getPackageName()))
                .setPackage(null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        // Use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);
        }

        mNotificationBuilder.setContentIntent(pIntent);
        mNotificationBuilder.clearActions();
        if (mState == PlaybackStateCompat.STATE_PLAYING) {
            mNotificationBuilder.setLargeIcon(art);
            mNotificationBuilder.setContentTitle(titleString);
            mNotificationBuilder.setContentText(djString);
            mNotificationBuilder.setSubText(text);
            mNotificationBuilder.addAction(new NotificationCompat.Action(R.drawable.ic_pause, getString(R.string.pause), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE)));
            mNotificationBuilder.addAction(new NotificationCompat.Action(R.drawable.ic_stop, getString(R.string.stop), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP)));
            mSession.setMetadata
                    (new MediaMetadataCompat.Builder()
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, art)
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art)
                            .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, artUri)
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, artUri)
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, smallIcon)
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, titleString)
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, djString)
                            .build()
                    );
        } else if (mState == PlaybackStateCompat.STATE_PAUSED) {
            mNotificationBuilder.setLargeIcon(placeHolder);
            mNotificationBuilder.setContentTitle(getString(R.string.radio));
            mNotificationBuilder.setContentText(text);
            mNotificationBuilder.setSubText(null);
            mNotificationBuilder.addAction(new NotificationCompat.Action(R.drawable.ic_play, getString(R.string.play), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY)));
            mNotificationBuilder.addAction(new NotificationCompat.Action(R.drawable.ic_stop, getString(R.string.stop), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP)));
            mSession.setMetadata
                    (new MediaMetadataCompat.Builder()
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, placeHolder)
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, placeHolder)
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, getString(R.string.radio_105))
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, text)
                            .build()
                    );
        } else {
            mNotificationBuilder.setLargeIcon(placeHolder);
            mNotificationBuilder.setContentTitle(getString(R.string.radio));
            mNotificationBuilder.setContentText(text);
            mNotificationBuilder.setSubText(null);
            mNotificationBuilder.addAction(0, null, null);
            mNotificationBuilder.addAction(0, null, null);
            mSession.setMetadata
                    (new MediaMetadataCompat.Builder()
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, placeHolder)
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, placeHolder)
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, getString(R.string.radio_105))
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, text)
                            .build()
                    );
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
        boolean notificationType = Utils.getUserPreferenceBoolean(this, getString(R.string.notification_type_key), true);

        // Creating notification channel
        createNotificationChannel();

        Intent intent = Objects.requireNonNull(getPackageManager()
                        .getLaunchIntentForPackage(getPackageName()))
                .setPackage(null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        // Use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);
        }

        // Building notification here
        mNotificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        if (notificationType) {
            mNotificationBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1)
                    .setMediaSession(mToken));
        } else {
            mNotificationBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1));
        }
        mNotificationBuilder.setContentText(text);
        mNotificationBuilder.setLargeIcon(placeHolder);
        mNotificationBuilder.setShowWhen(false);
        mNotificationBuilder.setSmallIcon(R.drawable.ic_radio105_notification);
        mNotificationBuilder.setContentTitle(getString(R.string.radio));
        mNotificationBuilder.setContentIntent(pIntent);
        mNotificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        mNotificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        mNotificationBuilder.addAction(0, null, null);
        mNotificationBuilder.addAction(0, null, null);
        mSession.setMetadata
                (new MediaMetadataCompat.Builder()
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, placeHolder)
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, placeHolder)
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
        fromErrorState = true;
        boolean reconnect = Utils.getUserPreferenceBoolean(this, getString(R.string.reconnect_key), true);

        Toast.makeText(getApplicationContext(), getString(R.string.error),
                Toast.LENGTH_SHORT).show();
        Timber.e("Error: what =  %s, extra = %s", what, extra);

        if (reconnect) {
            // Try to restart the streaming immediately if we have a working internet connection
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                // Android below N does not have registerDefaultNetworkCallback
                NetworkUtil.setNetworkConnected();
            }
            if (NetworkUtil.isNetworkConnected) {
                // Reset media player state
                relaxResources(false);
                mState = PlaybackStateCompat.STATE_STOPPED;
                Toast.makeText(getApplicationContext(), getString(R.string.reconnect),
                        Toast.LENGTH_SHORT).show();
                // Start the streaming
                mCallback.onPlay();
            } else {
                // Tell the user that streaming service cannot be recovered
                Toast.makeText(getApplicationContext(), getString(R.string.no_reconnect),
                        Toast.LENGTH_SHORT).show();
                updatePlaybackState(getString(R.string.error_cannot_recover));

                // No internet connection available, release all resources
                relaxResources(true);
                giveUpAudioFocus();
            }
        } else {
            // Tell the user that streaming service cannot be recovered because the option is disabled
            Toast.makeText(getApplicationContext(), getString(R.string.error_no_recover),
                    Toast.LENGTH_SHORT).show();
            updatePlaybackState(getString(R.string.error_no_recover));

            // Service died and reconnect option is disabled, we can release all resources
            relaxResources(true);
            giveUpAudioFocus();
        }
        return true; // true indicates we handled the error
    }

    @Override
    public void onDestroy() {
        // Service is being killed, so make sure we release our resources
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NetworkUtil.unregisterNetworkCallback();
        }
        stopSelf();
        mState = PlaybackStateCompat.STATE_STOPPED;
        relaxResources(true);
        giveUpAudioFocus();
        titleString = null;
        placeHolder = null;
        scheduler = null;
        mToken = null;
        art = null;
        smallIcon = null;
        // Always release the MediaSession to clean up resources
        // and notify associated MediaController(s).
        mSession.release();
        mSession.setActive(false);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mIBinder;
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
        boolean killService = Utils.getUserPreferenceBoolean(this, getString(R.string.service_kill_key), false);
        if (killService) {
            // Stop music service when the option is enabled
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
                    Timber.i("Received metadata: %s", response);
                    Document document = Jsoup.parse(response);
                    Element titleElement = document.selectFirst(".nome");
                    Element djElement = document.selectFirst(".dj_in_onda");
                    Element artElement = document.selectFirst("img");
                    if (titleElement != null) {
                        titleString = titleElement.text();
                    }
                    if (djElement != null) {
                        djString = djElement.text();
                    } else {
                        djString = getString(R.string.blank_line);
                    }
                    if (artElement != null) {
                        // 105 site use an online resizer for dynamically provide an artwork in the correct size. Unfortunately, the
                        // artwork fetched have a poor quality. All artworks links have a fixed part "resizer/WIDTH/HEIGHT/true", here
                        // the original link sizes will be changed to 480x480, for an higher quality image. If for some reason the
                        // replace won't work the original string will be used.
                        artUrl = artElement.absUrl("src");
                        artUrlResized = artUrl.replaceAll("(resizer/)[^&]*(/true)", "$1480/480$2");
                        Timber.i("artUrl changed, new URL is %s", artUrlResized);
                        // Fetch the album art here
                        fetchBitmapFromURL(artUrlResized);
                    }
                },
                error -> {
                    // Handle error
                });
        // Add the request to the RequestQueue.
        requestQueue.add(stringRequest);
    }

    private void fetchBitmapFromURL(String mString) {
        AlbumArtCache.getInstance().fetch(mString, new AlbumArtCache.FetchListener() {
            @Override
            public void onFetched(Bitmap bitmap, Bitmap icon) {
                art = bitmap;
                smallIcon = icon;
                String artUri = mString.replaceAll("(resizer/)[^&]*(/true)", "$1800/800$2");
                mSession.setMetadata
                        (new MediaMetadataCompat.Builder()
                                // set high resolution bitmap in METADATA_KEY_ALBUM_ART. This is used, for
                                // example, on the lockscreen background when the media session is active.
                                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                                // set small version of the album art in the DISPLAY_ICON. This is used on
                                // the MediaDescription and thus it should be small to be serialized if
                                // necessary..
                                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, icon)
                                // set METADATA_KEY_ART, which is used on some android versions for the album
                                // art on the lockscreen background when the media session is active.
                                .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
                                // We also set high definition artworks URI.
                                .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, artUri)
                                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, artUri)
                                .build()
                        );
                // Update metadata only if the stream is playing, the placeHolder is used on PAUSE state
                // and the new metadata will be used when we move on PLAY state
                if (mState == PlaybackStateCompat.STATE_PLAYING) {
                    updateNotification(getString(R.string.playing));
                }
            }
        });
    }

    static long millisToNextHourWithDelay(Calendar calendar) {
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);
        int millis = calendar.get(Calendar.MILLISECOND);
        int secondsToNextHour = 59 - seconds;
        int millisToNextHour = 1000 - millis;
        int minutesToNextHour;
        if (minutes >= 2) {
            minutesToNextHour = 60 - minutes + 2;
        } else {
            minutesToNextHour = 2 - minutes;
        }
        return minutesToNextHour*60*1000 + secondsToNextHour*1000 + millisToNextHour;
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

    private void registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            registerReceiver(mAudioNoisyReceiver, mAudioNoisyIntentFilter);
            mAudioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            unregisterReceiver(mAudioNoisyReceiver);
            mAudioNoisyReceiverRegistered = false;
        }
    }

    // *********  MediaSession.Callback implementation:
    private final MediaSessionCompat.Callback mCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() {
            processPlayRequest();
            if (ZooFragment.isMediaPlayingPodcast) {
                ZooFragment.mIPodcastService.playbackState("Pause");
            }
            if (PodcastFragment.isMediaPlayingPodcast) {
                PodcastFragment.mIPodcastService.playbackState("Pause");
            }
        }

        @Override
        public void onPause() {
            processPauseRequest();
        }

        @Override
        public void onStop() {
            processStopRequest();
        }
    };

    // AudioBecomingNoisy broadcast receiver
    private final BroadcastReceiver mAudioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                boolean noisy = Utils.getUserPreferenceBoolean(context, getString(R.string.noisy_key), true);
                if (noisy) {
                    if (mState == PlaybackStateCompat.STATE_PLAYING) {
                        mCallback.onPause();
                    }
                }
            }
        }
    };
}
