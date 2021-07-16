package com.bb.radio105;

import static android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY;

import static com.bb.radio105.Constants.ACTION_PAUSE_NOTIFICATION_PODCAST;
import static com.bb.radio105.Constants.ACTION_PAUSE_PODCAST;
import static com.bb.radio105.Constants.ACTION_PLAY_NOTIFICATION_PODCAST;
import static com.bb.radio105.Constants.ACTION_PLAY_PODCAST;
import static com.bb.radio105.Constants.ACTION_STOP_PODCAST;
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
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import timber.log.Timber;

public class PodcastService extends Service {

    private final AudioBecomingNoisyIntentReceiver mAudioBecomingNoisyIntentReceiver = new AudioBecomingNoisyIntentReceiver();

    private static final String CHANNEL_ID = "PodcastServiceChannel";
    private Bitmap podcastLogo;
    final int NOTIFICATION_ID = 2;
    private NotificationManagerCompat mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder = null;
    private Bitmap art;

    enum State {
        Stopped,
        Playing,
        Paused
    }

    static State mState = State.Stopped;

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.i("debug: Creating service");

        mNotificationManager = NotificationManagerCompat.from(this);

        // Set the PlaceHolders when service starts
        podcastLogo = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_podcast_logo);
        // Set the streaming state
        mState = State.Stopped;

        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(mAudioBecomingNoisyIntentReceiver, mIntentFilter);
    }

    @SuppressLint("WakelockTimeout")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        switch (action) {
            case ACTION_PLAY_NOTIFICATION_PODCAST:
                processPlayRequestNotification();
                break;
            case ACTION_PAUSE_NOTIFICATION_PODCAST:
                processPauseRequestNotification();
                break;
            case ACTION_PLAY_PODCAST:
                processPlayRequest();
                break;
            case ACTION_PAUSE_PODCAST:
                processPauseRequest();
                break;
            case ACTION_STOP_PODCAST:
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
    public void onTaskRemoved(Intent rootIntent) {
        // Service is being killed, so make sure we release our resources
        if (mState != State.Stopped) {
            processStopRequest();
        }
        stopSelf();
        zooBundle = null;
        podcastBundle = null;
    }

    @Override
    public void onDestroy() {
        // Service is being killed, so make sure we release our resources
        processStopRequest();
        stopSelf();
        unregisterReceiver(mAudioBecomingNoisyIntentReceiver);
        mNotificationBuilder = null;
        mNotificationManager = null;
        podcastLogo = null;
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void setUpAsForeground(String text) {
        // Creating notification channel
        createNotificationChannel();

        Intent intent = getPackageManager()
                .getLaunchIntentForPackage(getPackageName())
                .setPackage(null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        // Use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent;

        //Intent for Pause
        Intent pauseIntent = new Intent();
        pauseIntent.setAction(ACTION_PAUSE_NOTIFICATION_PODCAST);
        PendingIntent mPauseIntent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_IMMUTABLE);
            mPauseIntent = PendingIntent.getService(this, 111, pauseIntent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);
            mPauseIntent = PendingIntent.getService(this, 111, pauseIntent, 0);
        }

        // Building notification here
        mNotificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        mNotificationBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0));
        mNotificationBuilder.setSubText(text);
        mNotificationBuilder.setShowWhen(false);

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
        if (art != null) {
            mNotificationBuilder.setLargeIcon(art);
        } else {
            mNotificationBuilder.setLargeIcon(podcastLogo);
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

        //Intent for Play
        Intent playIntent = new Intent();
        playIntent.setAction(ACTION_PLAY_NOTIFICATION_PODCAST);
        PendingIntent mPlayIntent;

        //Intent for Pause
        Intent pauseIntent = new Intent();
        pauseIntent.setAction(ACTION_PAUSE_NOTIFICATION_PODCAST);
        PendingIntent mPauseIntent;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_IMMUTABLE);
            mPlayIntent = PendingIntent.getService(this, 110, playIntent, PendingIntent.FLAG_IMMUTABLE);
            mPauseIntent = PendingIntent.getService(this, 111, pauseIntent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);
            mPlayIntent = PendingIntent.getService(this, 110, playIntent, 0);
            mPauseIntent = PendingIntent.getService(this, 111, pauseIntent, 0);
        }

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
        Timber.e("Processing play request from notification");
        PodcastFragment.mIPodcastService.playbackState("Play");
        mState = State.Playing;
        updateNotification(getString(R.string.playing));
    }

    private void processPauseRequestNotification() {
        Timber.e("Processing pause request from notification");
        PodcastFragment.mIPodcastService.playbackState("Pause");
        mState = State.Paused;
        updateNotification(getString(R.string.in_pause));
    }

    @SuppressLint("WakelockTimeout")
    private void processPlayRequest() {
        Timber.e("Processing play request");
        if (mState == State.Stopped) {
            mState = State.Playing;
            art = AlbumArtCache.getInstance().getBigImage(PodcastFragment.podcastImageUrl.substring(0, 45));
            if (art == null) {
                fetchBitmapFromURL(PodcastFragment.podcastImageUrl);
            }
            setUpAsForeground(getString(R.string.playing));
        } else {
            mState = State.Playing;
            updateNotification(getString(R.string.playing));
        }
    }

    private void processPauseRequest() {
        Timber.e("Processing pause request");
        mState = State.Paused;
        updateNotification(getString(R.string.in_pause));
    }

    private void processStopRequest() {
        Timber.e("Processing stop request");
        if (mState != State.Stopped) {
            mState = State.Stopped;
            stopForeground(true);
        }
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

    private void fetchBitmapFromURL(String mString) {
        AlbumArtCache.getInstance().fetch(mString, new AlbumArtCache.FetchListener() {
            @Override
            public void onFetched(Bitmap bitmap, Bitmap icon) {
                art = bitmap;
                mNotificationBuilder.setLargeIcon(bitmap);
                mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
            }
        });
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
}
