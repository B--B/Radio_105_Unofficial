package com.bb.radio105;

import static android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY;
import static com.bb.radio105.Constants.ACTION_PAUSE;
import static com.bb.radio105.Constants.ACTION_PAUSE_NOTIFICATION;
import static com.bb.radio105.Constants.ACTION_PLAY;
import static com.bb.radio105.Constants.ACTION_PLAY_NOTIFICATION;
import static com.bb.radio105.Constants.ACTION_START;
import static com.bb.radio105.Constants.ACTION_STOP;
import static com.bb.radio105.Constants.podcastBundle;
import static com.bb.radio105.Constants.zooBundle;

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
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.LruCache;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import java.io.IOException;

import timber.log.Timber;

public class PodcastService extends Service implements AudioManager.OnAudioFocusChangeListener {

    private final AudioBecomingNoisyIntentReceiver mAudioBecomingNoisyIntentReceiver = new AudioBecomingNoisyIntentReceiver();

    private static final String CHANNEL_ID = "PodcastServiceChannel";
    private Bitmap podcastLogo;
    private Bitmap zooLogo;
    final int NOTIFICATION_ID = 2;
    private NotificationManagerCompat mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder = null;
    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;
    private LruCache<String, Bitmap> mAlbumArtCache;
    private static final int MAX_ALBUM_ART_CACHE_SIZE = 1024*1024;

    enum State {
        Stopped,
        Playing,
        Paused
    }

    static State mState = State.Stopped;

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

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.i("debug: Creating service");

        mNotificationManager = NotificationManagerCompat.from(this);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Set the PlaceHolders when service starts
        podcastLogo = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_podcast_logo);
        zooLogo = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_zoo_logo);
        // Set the streaming state
        mState = State.Stopped;
        //Acquire wake locks
        mWakeLock = ((PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WARNING:PodcastServiceWakelock");
        mWifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "WARNING:PodcastServiceWiFiWakelock");

        // simple album art cache that holds no more than
        // MAX_ALBUM_ART_CACHE_SIZE bytes:
        mAlbumArtCache = new LruCache<String, Bitmap>(MAX_ALBUM_ART_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };

        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(mAudioBecomingNoisyIntentReceiver, mIntentFilter);
    }

    @SuppressLint("WakelockTimeout")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        switch (action) {
            case ACTION_START:
                // Request audio focus here
                tryToGetAudioFocus();
                break;
            case ACTION_PLAY_NOTIFICATION:
                processPlayRequestNotification();
                mWakeLock.acquire();
                mWifiLock.acquire();
                break;
            case ACTION_PAUSE_NOTIFICATION:
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
                if (mWifiLock.isHeld()) {
                    mWifiLock.release();
                }
                processPauseRequestNotification();
                break;
            case ACTION_PLAY:
                mWakeLock.acquire();
                mWifiLock.acquire();
                processPlayRequest();
                break;
            case ACTION_PAUSE:
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
                if (mWifiLock.isHeld()) {
                    mWifiLock.release();
                }
                processPauseRequest();
                break;
            case ACTION_STOP:
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
                if (mWifiLock.isHeld()) {
                    mWifiLock.release();
                }
                processStopRequest();
                break;
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
            if (mState == State.Playing && !canDuck) {
                // If we don't have audio focus and can't duck, we save the information that
                // we were playing, so that we can resume playback once we get the focus back.
                mPlayOnFocusGain = true;
            }
        }
        handleFocusRequest();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        zooBundle = null;
        podcastBundle = null;
    }

    @Override
    public void onDestroy() {
        // Service is being killed, so make sure we release our resources
        giveUpAudioFocus();
        processStopRequest();
        stopSelf();
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        if (mWifiLock != null && mWifiLock.isHeld()) {
            mWifiLock.release();
        }
        unregisterReceiver(mAudioBecomingNoisyIntentReceiver);
        mNotificationBuilder = null;
        mNotificationManager = null;
        podcastLogo = null;
        zooLogo = null;
        mWakeLock = null;
        mWifiLock = null;
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void setUpAsForeground(String text) {
        // Creating notification channel
        createNotificationChannel();

        //Intent for Pause
        Intent pauseIntent = new Intent();
        pauseIntent.setAction(Constants.ACTION_PAUSE_NOTIFICATION);
        PendingIntent mPauseIntent = PendingIntent.getService(this, 101, pauseIntent, 0);

        Intent intent = getPackageManager()
                .getLaunchIntentForPackage(getPackageName())
                .setPackage(null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        // Use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent;
        pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

        // Building notification here
        mNotificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        mNotificationBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0));
        mNotificationBuilder.setSubText(text);
        mNotificationBuilder.setShowWhen(false);
        if (ZooFragment.zooService) {
            mNotificationBuilder.setLargeIcon(zooLogo);
            mNotificationBuilder.setSmallIcon(R.drawable.ic_zoo_notification);
            if (ZooFragment.podcastTitle != null) {
                mNotificationBuilder.setContentTitle(ZooFragment.podcastTitle);
            } else {
                mNotificationBuilder.setContentTitle(getString(R.string.zoo_service));
            }
            if (ZooFragment.podcastSubtitle != null) {
                mNotificationBuilder.setContentText(ZooFragment.podcastSubtitle);
            } else {
                mNotificationBuilder.setContentText(getString(R.string.zoo_service));
            }
        } else {
            mNotificationBuilder.setLargeIcon(podcastLogo);
            mNotificationBuilder.setSmallIcon(R.drawable.ic_radio105_notification);
            if (PodcastFragment.podcastTitle != null) {
                mNotificationBuilder.setContentTitle(PodcastFragment.podcastTitle);
            } else {
                mNotificationBuilder.setContentTitle(getString(R.string.podcast_service));
            }
            if (PodcastFragment.podcastSubtitle != null) {
                mNotificationBuilder.setContentText(PodcastFragment.podcastSubtitle);
            } else {
                mNotificationBuilder.setContentText(getString(R.string.podcast_service));
            }
        }
        mNotificationBuilder.setContentIntent(pIntent);
        mNotificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        mNotificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        mNotificationBuilder.addAction(R.drawable.ic_pause, getString(R.string.pause), mPauseIntent);
        startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void updateNotification(String text) {

        Intent intent = getPackageManager()
                .getLaunchIntentForPackage(getPackageName())
                .setPackage(null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        // Use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent;
        pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

        //Intent for Play
        Intent playIntent = new Intent();
        playIntent.setAction(Constants.ACTION_PLAY_NOTIFICATION);
        PendingIntent mPlayIntent = PendingIntent.getService(this, 100, playIntent, 0);

        //Intent for Pause
        Intent pauseIntent = new Intent();
        pauseIntent.setAction(Constants.ACTION_PAUSE_NOTIFICATION);
        PendingIntent mPauseIntent = PendingIntent.getService(this, 101, pauseIntent, 0);

        mNotificationBuilder.setContentIntent(pIntent);
        mNotificationBuilder.setSubText(text);
        mNotificationBuilder.clearActions();
        if (mState == State.Playing) {
            mNotificationBuilder.addAction(R.drawable.ic_pause, getString(R.string.pause), mPauseIntent);
        } else {
            mNotificationBuilder.addAction(R.drawable.ic_play, getString(R.string.play), mPlayIntent);
        }
        mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    private void processPlayRequestNotification() {
        mPlayOnFocusGain = true;
        mState = State.Playing;
        updateNotification(getString(R.string.playing));
        if (ZooFragment.zooService) {
            ZooFragment.mIPodcastService.playbackState("Play");
        } else {
            PodcastFragment.mIPodcastService.playbackState("Play");
        }
    }

    private void processPauseRequestNotification() {
        Timber.e("Processing pause request from notification");
        // Utils.callJavaScript(mWebView, "player.pause");
        mState = State.Paused;
        updateNotification(getString(R.string.in_pause));
        if (ZooFragment.zooService) {
            ZooFragment.mIPodcastService.playbackState("Pause");
        } else {
            PodcastFragment.mIPodcastService.playbackState("Pause");
        }
    }

    private void processDuckPauseRequest() {
        Timber.e("Processing pause request from notification");
        // Utils.callJavaScript(mWebView, "player.pause");
        mState = State.Playing;
        updateNotification(getString(R.string.in_pause));
        if (ZooFragment.zooService) {
            ZooFragment.mIPodcastService.playbackState("Pause");
        } else {
            PodcastFragment.mIPodcastService.playbackState("Pause");
        }
    }

    private void processPlayRequest() {
        mPlayOnFocusGain = true;
        if (mState == State.Stopped) {
            mState = State.Playing;
            setUpAsForeground(getString(R.string.playing));
            if (ZooFragment.zooService) {
                fetchBitmapFromURLThread(ZooFragment.podcastImageUrl);
            } else {
                fetchBitmapFromURLThread(PodcastFragment.podcastImageUrl);
            }
        } else {
            mState = State.Playing;
            updateNotification(getString(R.string.playing));
        }
    }

    private void processPauseRequest() {
        mState = State.Paused;
        updateNotification(getString(R.string.in_pause));
    }

    private void processStopRequest() {
        mState = State.Stopped;
        giveUpAudioFocus();
        stopForeground(true);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Podcast Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    void fetchBitmapFromURLThread(final String source) {
        Thread thread = new Thread(() -> {
            try {
                Bitmap bitmap;
                bitmap = BitmapHelper.fetchAndRescaleBitmap(source,
                        BitmapHelper.MEDIA_ART_WIDTH, BitmapHelper.MEDIA_ART_HEIGHT);
                mAlbumArtCache.put(source, bitmap);
                new Handler(Looper.getMainLooper()).post(() -> {
                    mNotificationBuilder.setLargeIcon(bitmap);
                    mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    // AudioBecomingNoisy broadcast receiver
    class AudioBecomingNoisyIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                boolean pref = PreferenceManager.getDefaultSharedPreferences(context)
                        .getBoolean(context.getString(R.string.noisy_key), true);
                if (pref) {
                    if (mState == State.Playing) {
                        processPauseRequestNotification();
                    }
                }
            }
        }
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

    private void handleFocusRequest() {
        if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause, even if mState
            // is State.Playing. But we stay in the Playing state so that we know we have to resume
            // playback once we get the focus back.
            if (mState == State.Playing) processDuckPauseRequest();
            return;
        } else if (mAudioFocus == AudioFocus.NoFocusCanDuck) {
            if (ZooFragment.zooService) {
                ZooFragment.mIPodcastService.duckRequest(true);
            } else {
                PodcastFragment.mIPodcastService.duckRequest(true);
            }
        } else {
            if (ZooFragment.zooService) {
                ZooFragment.mIPodcastService.duckRequest(false);
            } else {
                PodcastFragment.mIPodcastService.duckRequest(false);
            }
        }
        // If we were playing when we lost focus, we need to resume playing.
        if (mPlayOnFocusGain) {
            if (mState != State.Playing) {
                processPlayRequestNotification();
            }
            mPlayOnFocusGain = false;
        }
    }
}
