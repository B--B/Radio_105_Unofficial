/*
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

import static android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY;

import static com.bb.radio105.Constants.ACTION_PAUSE_NOTIFICATION_ZOO;
import static com.bb.radio105.Constants.ACTION_PAUSE_ZOO;
import static com.bb.radio105.Constants.ACTION_PLAY_NOTIFICATION_ZOO;
import static com.bb.radio105.Constants.ACTION_PLAY_ZOO;
import static com.bb.radio105.Constants.ACTION_STOP_ZOO;
import static com.bb.radio105.Constants.podcastBundle;
import static com.bb.radio105.Constants.zooBundle;
import static com.bb.radio105.ZooFragment.podcastImageUrl;
import static com.bb.radio105.ZooFragment.podcastSubtitle;
import static com.bb.radio105.ZooFragment.podcastTitle;

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
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import timber.log.Timber;

public class ZooService extends Service {

    private static final String CHANNEL_ID = "ZooServiceChannel";
    private Bitmap zooLogo;
    private Bitmap art;
    final int NOTIFICATION_ID = 3;
    private NotificationManagerCompat mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder = null;
    private volatile boolean mAudioNoisyReceiverRegistered;
    // Media Session
    private MediaSessionCompat mSession;
    private PlaybackStateCompat.Builder stateBuilder;

    // Current local media player state
    static int mState = PlaybackStateCompat.STATE_STOPPED;

    // AudioNoisy intent filter
    private final IntentFilter mAudioNoisyIntentFilter = new IntentFilter(ACTION_AUDIO_BECOMING_NOISY);

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.i("debug: Creating service");

        mNotificationManager = NotificationManagerCompat.from(this);

        // Set the PlaceHolders when service starts
        zooLogo = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_zoo_logo);
        // Set the streaming state
        mState = PlaybackStateCompat.STATE_STOPPED;

        // Start a new MediaSession
        mSession = new MediaSessionCompat(this, "PodcastService");
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE);
        mSession.setPlaybackState(stateBuilder.build());
        mSession.setCallback(mCallback);
        updatePlaybackState();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        switch (action) {
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
        if (mState != PlaybackStateCompat.STATE_STOPPED) {
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
        mNotificationBuilder = null;
        mNotificationManager = null;
        zooLogo = null;
        mSession.release();
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void setUpAsForeground(String text) {
        boolean notificationType = Utils.getUserPreferenceBoolean(this, getString(R.string.notification_type_key), true);

        // Creating notification channel
        createNotificationChannel();

        Intent intent = getPackageManager()
                .getLaunchIntentForPackage(getPackageName())
                .setPackage(null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        PendingIntent pIntent;

        //Intent for Pause
        Intent pauseIntent = new Intent();
        pauseIntent.setAction(Constants.ACTION_PAUSE_NOTIFICATION_ZOO);
        PendingIntent mPauseIntent;

        // Use System.currentTimeMillis() to have a unique ID for the pending intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_IMMUTABLE);
            mPauseIntent = PendingIntent.getService(this, 101, pauseIntent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);
            mPauseIntent = PendingIntent.getService(this, 101, pauseIntent, 0);
        }

        updatePlaybackState();

        // Building notification here
        mNotificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        if (notificationType) {
            mNotificationBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0)
                    .setMediaSession(mSession.getSessionToken()));
        } else {
            mNotificationBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0));
        }
        mNotificationBuilder.setSubText(text);
        mNotificationBuilder.setShowWhen(false);

        if (podcastTitle == null) {
            podcastTitle = getString(R.string.zoo_service);
        }
        if (podcastSubtitle == null) {
            podcastSubtitle = getString(R.string.zoo_service);
        }
        if (art == null) {
            art = zooLogo;
        }

        mNotificationBuilder.setSmallIcon(R.drawable.ic_zoo_notification);
        mNotificationBuilder.setContentTitle(podcastTitle);
        mNotificationBuilder.setContentText(podcastSubtitle);
        mNotificationBuilder.setLargeIcon(art);
        mNotificationBuilder.setContentIntent(pIntent);
        mNotificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        mNotificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        mNotificationBuilder.addAction(R.drawable.ic_pause, getString(R.string.pause), mPauseIntent);
        mSession.setMetadata
                (new MediaMetadataCompat.Builder()
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, art)
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art)
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, podcastTitle)
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, podcastSubtitle)
                        .build()
                );
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

        updatePlaybackState();

        mNotificationBuilder.setContentIntent(pIntent);
        mNotificationBuilder.setSubText(text);
        mNotificationBuilder.clearActions();
        if (mState == PlaybackStateCompat.STATE_PLAYING) {
            mNotificationBuilder.setLargeIcon(art);
            mNotificationBuilder.addAction(R.drawable.ic_pause, getString(R.string.pause), mPauseIntent);
            mSession.setMetadata
                    (new MediaMetadataCompat.Builder()
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, art)
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art)
                            .build()
                    );
        } else {
            mNotificationBuilder.setLargeIcon(zooLogo);
            mNotificationBuilder.addAction(R.drawable.ic_play, getString(R.string.play), mPlayIntent);
            mSession.setMetadata
                    (new MediaMetadataCompat.Builder()
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, zooLogo)
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, zooLogo)
                            .build()
                    );
        }
        mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    private void processPlayRequestNotification() {
        Timber.e("Processing play request from notification");
        registerAudioNoisyReceiver();
        ZooFragment.mIPodcastService.playbackState("Play");
        mState = PlaybackStateCompat.STATE_PLAYING;
        updateNotification(getString(R.string.playing));
    }

    private void processPauseRequestNotification() {
        Timber.e("Processing pause request from notification");
        ZooFragment.mIPodcastService.playbackState("Pause");
        mState = PlaybackStateCompat.STATE_PAUSED;
        updateNotification(getString(R.string.in_pause));
        unregisterAudioNoisyReceiver();
    }

    private void processPlayRequest() {
        Timber.e("Processing play request");
        registerAudioNoisyReceiver();
        if (mState == PlaybackStateCompat.STATE_STOPPED) {
            mSession.setActive(true);
            mState = PlaybackStateCompat.STATE_PLAYING;
            if (podcastImageUrl != null) {
                fetchBitmapFromURL( podcastImageUrl );
            }
            setUpAsForeground(getString(R.string.playing));
        } else {
            mState = PlaybackStateCompat.STATE_PLAYING;
            updateNotification(getString(R.string.playing));
        }
    }

    private void processPauseRequest() {
        Timber.e("Processing pause request");
        mState = PlaybackStateCompat.STATE_PAUSED;
        updateNotification(getString(R.string.in_pause));
        unregisterAudioNoisyReceiver();
    }

    private void processStopRequest() {
        Timber.e("Processing stop request");
        if (mState != PlaybackStateCompat.STATE_STOPPED) {
            mState = PlaybackStateCompat.STATE_STOPPED;
            updatePlaybackState();
            stopForeground(true);
            mSession.setActive(false);
        }
        unregisterAudioNoisyReceiver();
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
                String artUri = mString.replaceAll("(resizer/)[^&]*(/true)", "$1800/800$2");
                mNotificationBuilder.setLargeIcon(bitmap);
                mSession.setMetadata
                        (new MediaMetadataCompat.Builder()
                                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, icon)
                                .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
                                .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, artUri)
                                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, artUri)
                                .build()
                        );
                mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
            }
        });
    }

    // AudioBecomingNoisy broadcast receiver
    private final BroadcastReceiver mAudioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                boolean noisy = Utils.getUserPreferenceBoolean(context, getString(R.string.noisy_key), true);
                if (noisy) {
                    if (mState == PlaybackStateCompat.STATE_PLAYING) {
                        processPauseRequestNotification();
                    }
                }
            }
        }
    };

    private void updatePlaybackState() {
        stateBuilder.setState(mState, 0, 1.0f, SystemClock.elapsedRealtime());
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
            Intent mIntent = new Intent();
            mIntent.setAction(ACTION_PLAY_NOTIFICATION_ZOO);
            mIntent.setPackage(getPackageName());
            startService(mIntent);
        }

        @Override
        public void onPause() {
            Intent mIntent = new Intent();
            mIntent.setAction(ACTION_PAUSE_NOTIFICATION_ZOO);
            mIntent.setPackage(getPackageName());
            startService(mIntent);
        }
    };
}
