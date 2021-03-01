package com.bb.radio105;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PlayerIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case Constants.ACTION_PAUSE:
                HomeFragment.playerStatusListener.onButtonStatusChange("Pause");
                break;
            case Constants.ACTION_PLAY:
                HomeFragment.playerStatusListener.onButtonStatusChange("Play");
                break;
            case Constants.ACTION_STOP:
                HomeFragment.playerStatusListener.onButtonStatusChange("Stop");
                break;
            case Constants.ACTION_PAUSE_NOTIFICATION:
                HomeFragment.playerStatusListener.onButtonStatusChange("Pause_Notification");
                break;
            case Constants.ACTION_PLAY_NOTIFICATION:
                HomeFragment.playerStatusListener.onButtonStatusChange("Play_Notification");
                break;
            case Constants.ACTION_STOP_NOTIFICATION:
                HomeFragment.playerStatusListener.onButtonStatusChange("Stop_Notification");
                break;
            case Constants.ACTION_ERROR:
                HomeFragment.playerStatusListener.onButtonStatusChange("Error");
                break;
            case android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                Intent mIntent = new Intent();
                mIntent.setAction(Constants.ACTION_PAUSE);
                mIntent.setPackage(context.getPackageName());
                context.startService(mIntent);
                break;
        }
    }
}
