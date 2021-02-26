package com.bb.radio105;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class DownloadService extends Service {

    NotificationManagerCompat mNotificationManager;
    NotificationCompat.Builder mNotificationBuilder = null;
    private static final String CHANNEL_ID_DOWNLOAD = "Radio105PodcastChannel";
    private final int NOTIFICATION_ID = 2;

    @Override
    public void onCreate() {
        mNotificationManager = NotificationManagerCompat.from(this);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String download = intent.getAction();
        switch (download) {
            case Constants.ACTION_START_DOWNLOAD:
                //Intent for Stop
                Intent stopIntent = new Intent();
                stopIntent.setAction(Constants.ACTION_STOP_DOWNLOAD);
                PendingIntent mStopIntent = PendingIntent.getService(this, 110, stopIntent, 0);
                // Creating notification channel
                createNotificationChannel();
                // Building the notification
                mNotificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID_DOWNLOAD);
                mNotificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_foreground));
                mNotificationBuilder.setSmallIcon(R.drawable.ic_radio105_notification);
                mNotificationBuilder.setContentTitle(getString(R.string.menu_home));
                mNotificationBuilder.setContentText(getString(R.string.start_download));
                mNotificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
                mNotificationBuilder.addAction(R.drawable.ic_stop, getString(R.string.stop), mStopIntent);
                startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
                // Set up and start the download
                File root = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
                        + Environment.DIRECTORY_DOWNLOADS + "/");
                String urlToDownload = intent.getStringExtra("Url");
                String fileName = intent.getStringExtra("FileName");
                @SuppressLint("RestrictedApi") Thread thread = new Thread(() -> {
                    try  {
                        URL url = new URL(urlToDownload);
                        URLConnection connection = url.openConnection();
                        connection.connect();
                        int fileLength = connection.getContentLength();

                        // Download the file
                        InputStream input = new BufferedInputStream(url.openStream());
                        OutputStream output = new FileOutputStream(new File(root.getPath(), fileName));

                        byte[] data = new byte[1024];

                        long total = 0;
                        int count, tmpPercentage = 0;
                        while ((count = input.read(data)) != -1) {
                            total += count;
                            output.write(data, 0, count);
                            int percentage = (int) ((total * 100) / fileLength);
                            if (percentage > tmpPercentage) {
                                mNotificationBuilder.setContentText(percentage + "%");
                                mNotificationBuilder.setProgress(100, percentage, false);
                                mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
                                tmpPercentage = percentage;
                            }
                        }
                        output.flush();
                        output.close();
                        input.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mNotificationBuilder.setContentText(getString(R.string.download_complete));
                    mNotificationBuilder.setProgress(0, 0, false).setOngoing(false).setContentInfo("");
                    mNotificationBuilder.mActions.clear();
                    mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
                    stopForeground(false);
                });
                thread.start();
                break;
            case Constants.ACTION_STOP_DOWNLOAD:
                stopForegroundService();
                break;
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID_DOWNLOAD,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void stopForegroundService()
    {
        stopForeground(true);
        stopSelf();
    }
}
