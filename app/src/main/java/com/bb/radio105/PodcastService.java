package com.bb.radio105;

import static com.bb.radio105.Constants.ACTION_PAUSE;
import static com.bb.radio105.Constants.ACTION_PAUSE_NOTIFICATION;
import static com.bb.radio105.Constants.ACTION_PLAY;
import static com.bb.radio105.Constants.ACTION_PLAY_NOTIFICATION;
import static com.bb.radio105.Constants.ACTION_START;
import static com.bb.radio105.Constants.ACTION_STOP;
import static com.bb.radio105.PodcastFragment.mWebView;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.adblockplus.libadblockplus.android.webview.AdblockWebView;

import timber.log.Timber;

public class PodcastService extends Service {

    private static final String CHANNEL_ID = "PodcastServiceChannel";
    private Bitmap placeHolder;
    final int NOTIFICATION_ID = 2;
    private NotificationManagerCompat mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder = null;
    private boolean serviceCreated = false;
    private PowerManager.WakeLock wakeLock;
    static boolean isPlayingPodcast = false;
    private AdblockWebView mAdblockWebView;

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.i("debug: Creating service");

        mNotificationManager = NotificationManagerCompat.from(this);
        // Set the PlaceHolder when service starts
        placeHolder = BitmapFactory.decodeResource(getResources(), R.drawable.ic_radio_105_logo);

        //Acquire wake lock
        PowerManager mPowerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        this.wakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WARNING:PodcastServiceWakelock");

        mAdblockWebView = mWebView;
    }

    @SuppressLint("WakelockTimeout")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        switch (action) {
            case ACTION_START:
                break;
            case ACTION_PLAY_NOTIFICATION:
                processPlayRequestNotification();
                if (wakeLock != null && !wakeLock.isHeld()) {
                    wakeLock.acquire();
                }
                break;
            case ACTION_PAUSE_NOTIFICATION:
                if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                }
                processPauseRequestNotification();
                break;
            case ACTION_PLAY:
                if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire();
                }
                processPlayRequest();
                break;
            case ACTION_PAUSE:
                if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                }
                processPauseRequest();
                break;
            case ACTION_STOP:
                if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
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
    public void onDestroy() {
        // Service is being killed, so make sure we release our resources
        mAdblockWebView = null;
        mNotificationBuilder = null;
        mNotificationManager = null;
        placeHolder = null;
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        wakeLock = null;
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void setUpAsForeground(String text) {
        // Creating notification channel
        createNotificationChannel();
        Intent intent = new Intent(this, MainActivity.class);

        //Intent for Pause
        Intent pauseIntent = new Intent();
        pauseIntent.setAction(Constants.ACTION_PAUSE_NOTIFICATION);
        PendingIntent mPauseIntent = PendingIntent.getService(this, 101, pauseIntent, 0);

        // Use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        // Building notification here
        mNotificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        mNotificationBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0));
        mNotificationBuilder.setContentText(text);
        mNotificationBuilder.setLargeIcon(placeHolder);
        mNotificationBuilder.setShowWhen(false);
        mNotificationBuilder.setSmallIcon(R.drawable.ic_radio105_notification);
        mNotificationBuilder.setContentTitle(getString(R.string.radio));
        mNotificationBuilder.setContentIntent(pIntent);
        mNotificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        mNotificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        mNotificationBuilder.addAction(R.drawable.ic_pause, getString(R.string.pause), mPauseIntent);
        startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void updateNotification(String text) {
        Intent intent = new Intent(this, MainActivity.class);
        // Use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        //Intent for Play
        Intent playIntent = new Intent();
        playIntent.setAction(Constants.ACTION_PLAY_NOTIFICATION);
        PendingIntent mPlayIntent = PendingIntent.getService(this, 100, playIntent, 0);

        //Intent for Pause
        Intent pauseIntent = new Intent();
        pauseIntent.setAction(Constants.ACTION_PAUSE_NOTIFICATION);
        PendingIntent mPauseIntent = PendingIntent.getService(this, 101, pauseIntent, 0);

        mNotificationBuilder.setContentIntent(pIntent);
        mNotificationBuilder.setContentText(text);
        mNotificationBuilder.clearActions();
        if (PodcastFragment.isMediaPlayingPodcast) {
            mNotificationBuilder.addAction(R.drawable.ic_pause, getString(R.string.pause), mPauseIntent);
        } else {
            mNotificationBuilder.addAction(R.drawable.ic_play, getString(R.string.play), mPlayIntent);
        }
        mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    private void processPlayRequestNotification() {
        Utils.callJavaScript(mAdblockWebView, "player.play");
        updateNotification(getString(R.string.playing));
        isPlayingPodcast = true;
    }

    private void processPauseRequestNotification() {
        Timber.e("Processing pause request from notification");
        Utils.callJavaScript(mAdblockWebView, "player.pause");
        updateNotification(getString(R.string.in_pause));
        isPlayingPodcast = false;
    }

    private void processPlayRequest() {
        if (serviceCreated) {
            updateNotification(getString(R.string.playing));
        } else {
            setUpAsForeground(getString(R.string.playing));
            serviceCreated = true;
        }
        isPlayingPodcast = true;
    }

    private void processPauseRequest() {
        updateNotification(getString(R.string.in_pause));
        isPlayingPodcast = false;
    }

    private void processStopRequest() {
        stopForeground(true);
        PodcastFragment.isMediaPlayingPodcast = false;
        isPlayingPodcast = false;
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
}
