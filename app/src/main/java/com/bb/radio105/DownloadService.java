package com.bb.radio105;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;

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

    private int lastUpdate=0;
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
        if (download.equals(Constants.ACTION_START_DOWNLOAD)) {
            // Creating notification channel
            createNotificationChannel();
            // Building the notification
            mNotificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID_DOWNLOAD);
            mNotificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_foreground));
            mNotificationBuilder.setSmallIcon(R.drawable.ic_radio105_notification);
            mNotificationBuilder.setContentTitle(getString(R.string.menu_home));
            mNotificationBuilder.setContentText(getString(R.string.start_download));
            mNotificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
            startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
            // Set up and start the download
            File root = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
                    + Environment.DIRECTORY_DOWNLOADS + "/");
            String urlToDownload = intent.getStringExtra("Url");
            String fileName = intent.getStringExtra("FileName");
            Thread thread = new Thread(() -> {
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
                    int count;
                    while ((count = input.read(data)) != -1) {
                        total += count;

                        progressChange((int) (total * 100) / fileLength);
                        output.write(data, 0, count);
                    }
                    output.flush();
                    output.close();
                    input.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            thread.start();
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    void progressChange(int progress){
        if (lastUpdate != progress) {
            lastUpdate = progress;
            if (progress < 100) {
                mNotificationBuilder.setContentText(getString(R.string.download));
                mNotificationBuilder.setProgress(100, progress,
                        false).setContentInfo(progress+"%");
                mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
            } else {
                mNotificationBuilder.setContentText(getString(R.string.download_complete));
                mNotificationBuilder.setProgress(0, 0, false).setOngoing(false).setContentInfo("");
                mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
                stopForeground(false);
            }
        }
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
}
