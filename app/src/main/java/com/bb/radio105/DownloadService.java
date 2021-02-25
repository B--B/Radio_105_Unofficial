package com.bb.radio105;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
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

import timber.log.Timber;

public class DownloadService extends IntentService {

    static final int UPDATE_PROGRESS = 8344;
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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("onStartCommand");
        Timber.d("Start building notification");
        // Creating notification channel
        createNotificationChannel();
        Intent intent1 = new Intent(this, MainActivity.class);
        // Use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent1, PendingIntent.FLAG_UPDATE_CURRENT);
        // Building notification here
        mNotificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID_DOWNLOAD);
        mNotificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_foreground));
        mNotificationBuilder.setSmallIcon(R.drawable.ic_radio105_notification);
        mNotificationBuilder.setContentTitle("Radio 105");
        mNotificationBuilder.setContentText("Downloading");
        mNotificationBuilder.setContentIntent(pIntent);
        mNotificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        // Launch notification
        startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
        return START_NOT_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        File root = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
                + Environment.DIRECTORY_DOWNLOADS + "/");
        Bundle b = new Bundle();
        b = intent.getExtras();
        Object urlToDownload = b.getString("Url");
        Object fileName = b.getString("FileName");
        Timber.d(urlToDownload.toString());
        Timber.d(fileName.toString());
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
                mNotificationBuilder.setProgress(100, progress,
                        false).setContentInfo(progress+"%");
                mNotificationManager.notify(12, mNotificationBuilder.build());
                Intent i = new Intent("com.bb.radio105.MainActivity").putExtra("Downloading",progress+"%");
                this.sendBroadcast(i);
            } else {
                mNotificationBuilder.setContentText("Download complete")
                        // Removes the progress bar
                        .setProgress(0, 0, false).setOngoing(false).setContentInfo("");

                mNotificationManager.notify(12, mNotificationBuilder.build());

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
