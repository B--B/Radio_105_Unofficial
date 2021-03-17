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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.preference.PreferenceManager;

import static com.bb.radio105.Constants.ACTION_ERROR;
import static com.bb.radio105.Constants.ACTION_PAUSE;
import static com.bb.radio105.Constants.ACTION_PLAY;
import static com.bb.radio105.Constants.ACTION_STOP;

public class PlayerIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case ACTION_PAUSE:
                HomeFragment.playerStatusListener.onButtonStatusChange("Pause");
                break;
            case ACTION_PLAY:
                HomeFragment.playerStatusListener.onButtonStatusChange("Play");
                break;
            case ACTION_STOP:
                HomeFragment.playerStatusListener.onButtonStatusChange("Stop");
                break;
            case ACTION_ERROR:
                HomeFragment.playerStatusListener.onButtonStatusChange("Error");
                break;
            case android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                boolean pref = PreferenceManager.getDefaultSharedPreferences(context)
                        .getBoolean(context.getString(R.string.noisy_key), true);
                if (pref) {
                    Intent mIntent = new Intent();
                    mIntent.setAction(ACTION_PAUSE);
                    mIntent.setPackage(context.getPackageName());
                    context.startService(mIntent);
                }
                break;
        }
    }
}
