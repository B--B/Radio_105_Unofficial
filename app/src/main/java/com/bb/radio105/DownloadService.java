package com.bb.radio105;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class DownloadService extends IntentService {

    private int lastUpdate=0;
    NotificationManagerCompat mNotificationManager;
    NotificationCompat.Builder mNotificationBuilder = null;
    private static final String CHANNEL_ID_DOWNLOAD = "Radio105PodcastChannel";
    private final int NOTIFICATION_ID = 2;

    public DownloadService() {
        super("com.bb.radio105.DownloadService");
    }

    @Override
    public void onCreate() {
        mNotificationManager = NotificationManagerCompat.from(this);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        // Creating notification channel
        createNotificationChannel();
        // Building notification here
        mNotificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID_DOWNLOAD);
        mNotificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_foreground));
        mNotificationBuilder.setSmallIcon(R.drawable.ic_radio105_notification);
        mNotificationBuilder.setContentTitle("Radio 105");
        mNotificationBuilder.setContentText("Starting Download");
        mNotificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        // Launch notification
        startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
        return START_NOT_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        File root = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
                + Environment.DIRECTORY_DOWNLOADS + "/");
        String urlToDownload = intent.getStringExtra("Url");
        String fileName = intent.getStringExtra("FileName");
        try {
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

                progressChange((int)(total * 100) / fileLength);
                output.write(data, 0, count);
            }
            output.flush();
            output.close();
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void progressChange(int progress){
        if (lastUpdate != progress) {
            lastUpdate = progress;
            if (progress < 100) {
                mNotificationBuilder.setContentText("Downloading");
                mNotificationBuilder.setProgress(100, progress,
                        false).setContentInfo(progress+"%");
                mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
            } else {
                // This is completely unuseful, as DownloadService is destroyed
                // when onHandleIntent finish
                Uri download = Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
                        + Environment.DIRECTORY_DOWNLOADS + "/");
                Intent intent1 = new Intent(Intent.ACTION_VIEW);
                intent1.setData(download);
                // Use System.currentTimeMillis() to have a unique ID for the pending intent
                PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent1, PendingIntent.FLAG_UPDATE_CURRENT);
                mNotificationBuilder.setContentText("Download complete");
                mNotificationBuilder.setContentIntent(pIntent);
                mNotificationBuilder.setProgress(0, 0, false).setOngoing(false).setContentInfo("");
                mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
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
