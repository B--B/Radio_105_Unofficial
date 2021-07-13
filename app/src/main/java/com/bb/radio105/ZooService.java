package com.bb.radio105;

import static android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY;

import static com.bb.radio105.Constants.ACTION_PAUSE_NOTIFICATION_ZOO;
import static com.bb.radio105.Constants.ACTION_PAUSE_ZOO;
import static com.bb.radio105.Constants.ACTION_PLAY_NOTIFICATION_ZOO;
import static com.bb.radio105.Constants.ACTION_PLAY_ZOO;
import static com.bb.radio105.Constants.ACTION_START_ZOO;
import static com.bb.radio105.Constants.ACTION_STOP_ZOO;
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

public class ZooService extends Service {

    private final AudioBecomingNoisyIntentReceiver mAudioBecomingNoisyIntentReceiver = new AudioBecomingNoisyIntentReceiver();

    private static final String CHANNEL_ID = "ZooServiceChannel";
    private Bitmap zooLogo;
    private Bitmap art;
    final int NOTIFICATION_ID = 3;
    private NotificationManagerCompat mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder = null;

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
        zooLogo = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_zoo_logo);
        // Set the streaming state
        mState = State.Stopped;

        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(mAudioBecomingNoisyIntentReceiver, mIntentFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        switch (action) {
            case ACTION_START_ZOO:
                break;
            case ACTION_PLAY_NOTIFICATION_ZOO:
                processPlayRequestNotification();
                break;
            case ACTION_PAUSE_NOTIFICATION_ZOO:
                processPauseRequestNotification();
                break;
            case ACTION_PLAY_ZOO:
                processPlayRequest();
                break;
            case ACTION_PAUSE_ZOO:
                processPauseRequest();
                break;
            case ACTION_STOP_ZOO:
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
        zooLogo = null;
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
        pauseIntent.setAction(Constants.ACTION_PAUSE_NOTIFICATION_ZOO);
        PendingIntent mPauseIntent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_IMMUTABLE);
            mPauseIntent = PendingIntent.getService(this, 101, pauseIntent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);
            mPauseIntent = PendingIntent.getService(this, 101, pauseIntent, 0);
        }

        // Building notification here
        mNotificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        mNotificationBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0));
        mNotificationBuilder.setSubText(text);
        mNotificationBuilder.setShowWhen(false);

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
        if (art != null) {
            mNotificationBuilder.setLargeIcon(art);
        } else {
            mNotificationBuilder.setLargeIcon(zooLogo);
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
        playIntent.setAction(Constants.ACTION_PLAY_NOTIFICATION_ZOO);
        PendingIntent mPlayIntent;

        //Intent for Pause
        Intent pauseIntent = new Intent();
        pauseIntent.setAction(Constants.ACTION_PAUSE_NOTIFICATION_ZOO);
        PendingIntent mPauseIntent;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_IMMUTABLE);
            mPlayIntent = PendingIntent.getService(this, 100, playIntent, PendingIntent.FLAG_IMMUTABLE);
            mPauseIntent = PendingIntent.getService(this, 101, pauseIntent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);
            mPlayIntent = PendingIntent.getService(this, 100, playIntent, 0);
            mPauseIntent = PendingIntent.getService(this, 101, pauseIntent, 0);
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

    @SuppressLint("WakelockTimeout")
    private void processPlayRequestNotification() {
        Timber.e("Processing play request from notification");
        ZooFragment.mIPodcastService.playbackState("Play");
        mState = State.Playing;
        updateNotification(getString(R.string.playing));
    }

    private void processPauseRequestNotification() {
        Timber.e("Processing pause request from notification");
        ZooFragment.mIPodcastService.playbackState("Pause");
        mState = State.Paused;
        updateNotification(getString(R.string.in_pause));
    }

    @SuppressLint("WakelockTimeout")
    private void processPlayRequest() {
        Timber.e("Processing play request");
        if (mState == State.Stopped) {
            mState = State.Playing;
            art = AlbumArtCache.getInstance().getBigImage(ZooFragment.podcastImageUrl.substring(0, 45));
            if (art == null) {
                fetchBitmapFromURL(ZooFragment.podcastImageUrl);
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
                    "Zoo Service Channel",
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
