package com.bb.radio105;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import androidx.core.app.NotificationCompat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class DownloadService extends IntentService {

    static final int UPDATE_PROGRESS = 8344;
    private int lastUpdate=0;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    private static final String CHANNEL_ID_DOWNLOAD = "Radio105PodcastChannel";
    private final int NOTIFICATION_ID = 2;

    public DownloadService() {

        super("com.bb.radio105.DownloadService");


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        mNotificationManager = (NotificationManager)  getSystemService(NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID_DOWNLOAD);
        mBuilder.setSmallIcon(R.drawable.ic_radio105_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_foreground))
                .setContentTitle("Radio 105 Podcast")
                .setContentText("Downloading")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true);
        startForeground(NOTIFICATION_ID, mBuilder.build());
        return START_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        File root = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
                + Environment.DIRECTORY_DOWNLOADS + "/");
        Bundle b = new Bundle();
        b = intent.getExtras();
        Object urlToDownload = b.getString("Url");
        Object fileName = b.getString("FileName");
        // ResultReceiver receiver = (ResultReceiver) intent.getParcelableExtra("receiver");
        try {
            URL url = new URL(urlToDownload.toString());
            URLConnection connection = url.openConnection();
            connection.connect();
            // this will be useful so that you can show a typical 0-100% progress bar
            int fileLength = connection.getContentLength();

            // download the file
            InputStream input = new BufferedInputStream(url.openStream());
            OutputStream output = new FileOutputStream(new File(root.getPath(), fileName.toString()));

            byte[] data = new byte[1024];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                total += count;

                progressChange((int)(total * 100) / fileLength);
                // publishing the progress....
                //   Bundle resultData = new Bundle();
                //   resultData.putInt("progress" ,(int) (total * 100 / fileLength));
                //   receiver.send(UPDATE_PROGRESS, resultData);
                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Bundle resultData = new Bundle();
        //  resultData.putInt("progress" ,100);
        //  receiver.send(UPDATE_PROGRESS, resultData);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    void progressChange(int progress){


        if (lastUpdate != progress) {
            lastUpdate = progress;
            // not.contentView.setProgressBar(R.id.status_progress,
            // 100,Integer.valueOf(progress[0]), false);
            // inform the progress bar of updates in progress
            // nm.notify(42, not);
            if (progress < 100) {
                mBuilder.setProgress(100, progress,
                        false).setContentInfo(progress+"%");
                mNotificationManager.notify(12, mBuilder.build());
                Intent i = new Intent("com.bb.radio105.MainActivity").putExtra("Downloading",progress+"%");
                this.sendBroadcast(i);
            } else {
                mBuilder.setContentText("Download complete")
                        // Removes the progress bar
                        .setProgress(0, 0, false).setOngoing(false).setContentInfo("");

                mNotificationManager.notify(12, mBuilder.build());

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
