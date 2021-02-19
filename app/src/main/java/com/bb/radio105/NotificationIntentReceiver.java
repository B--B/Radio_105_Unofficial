package com.bb.radio105;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case Constants.ACTION_PAUSE:
                HomeFragment.notificationStatusListener.onButtonStatusChange("Pause");
                break;
            case Constants.ACTION_PLAY:
                HomeFragment.notificationStatusListener.onButtonStatusChange("Play");
                break;
            case Constants.ACTION_STOP:
                HomeFragment.notificationStatusListener.onButtonStatusChange("Stop");
                break;
            case Constants.ACTION_PAUSE_NOTIFICATION:
                HomeFragment.notificationStatusListener.onButtonStatusChange("Pause_Notification");
                break;
            case Constants.ACTION_PLAY_NOTIFICATION:
                HomeFragment.notificationStatusListener.onButtonStatusChange("Play_Notification");
                break;
            case Constants.ACTION_STOP_NOTIFICATION:
                HomeFragment.notificationStatusListener.onButtonStatusChange("Stop_Notification");
                break;
        }
    }
}
