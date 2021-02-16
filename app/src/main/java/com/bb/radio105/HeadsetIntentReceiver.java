/*   
 * Copyright (C) 2011 The Android Open Source Project
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
import android.widget.Toast;

import androidx.preference.PreferenceManager;

/**
 * Receives broadcasted intents. In particular, we are interested in the
 * android.media.AUDIO_BECOMING_NOISY and android.intent.action.MEDIA_BUTTON intents, which is
 * broadcast, for example, when the user disconnects the headphones. This class works because we are
 * declaring it in a &lt;receiver&gt; tag in AndroidManifest.xml.
 */
public class HeadsetIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean pref = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.noisy_key), true);
        boolean pref1 = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.headset_key), true);
        if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
            if (pref) {
                Intent mIntent = new Intent();
                mIntent.setAction("com.bb.radio105.action.PAUSE");
                mIntent.setPackage(context.getPackageName());
                context.startService(mIntent);
            }
//            We receive this intent + extra state every time the app is open, not so useful for us
//        } else if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
//            int state = intent.getIntExtra("state", -1);
//            switch (state) {
//                case 0:
//                    Headphones disconnected
//                    break;
//                case 1:
//                    Headphones connected
//                    break;
//                default:
//                    Status Unknown
//            }
        }
    }
}
