package com.bb.radio105;

import static com.bb.radio105.Constants.ACTION_PAUSE;
import static com.bb.radio105.Constants.ACTION_PAUSE_NOTIFICATION;
import static com.bb.radio105.Constants.ACTION_PLAY;
import static com.bb.radio105.Constants.ACTION_PLAY_NOTIFICATION;
import static com.bb.radio105.Constants.ACTION_START;
import static com.bb.radio105.Constants.ACTION_STOP;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import timber.log.Timber;

public class PodcastService extends Service {

    private static final String CHANNEL_ID = "PodcastServiceChannel";
    private Bitmap placeHolder;
    final int NOTIFICATION_ID = 2;
    private NotificationManagerCompat mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder = null;
    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;

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
        // Set the PlaceHolder when service starts
        placeHolder = BitmapFactory.decodeResource(getResources(), R.drawable.ic_radio_105_logo);
        // Set the streaming state
        mState = State.Stopped;
        //Acquire wake locks
        mWakeLock = ((PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WARNING:PodcastServiceWakelock");
        mWifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "WARNING:PodcastServiceWiFiWakelock");
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
    public void onDestroy() {
        // Service is being killed, so make sure we release our resources
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        if (mWifiLock != null && mWifiLock.isHeld()) {
            mWifiLock.release();
        }
        mNotificationBuilder = null;
        mNotificationManager = null;
        placeHolder = null;
        mWakeLock = null;
        mWifiLock = null;
        stopForeground(true);
        stopSelf();
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
        mNotificationBuilder.setContentText(text);
        mNotificationBuilder.clearActions();
        if (mState == State.Playing) {
            mNotificationBuilder.addAction(R.drawable.ic_pause, getString(R.string.pause), mPauseIntent);
        } else {
            mNotificationBuilder.addAction(R.drawable.ic_play, getString(R.string.play), mPlayIntent);
        }
        mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    private void processPlayRequestNotification() {
        //Utils.callJavaScript(mWebView, "player.play");
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

    private void processPlayRequest() {
        if (mState == State.Stopped) {
            mState = State.Playing;
            setUpAsForeground(getString(R.string.playing));
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
}