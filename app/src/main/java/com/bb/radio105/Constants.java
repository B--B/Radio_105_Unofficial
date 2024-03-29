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

import android.os.Bundle;

public class Constants {
    // Podcast Service Actions
    static final String ACTION_PAUSE_NOTIFICATION_PODCAST = "com.bb.radio105.action.PAUSE_NOTIFICATION_PODCAST";
    static final String ACTION_PAUSE_PODCAST = "com.bb.radio105.action.PAUSE_PODCAST";
    static final String ACTION_PLAY_NOTIFICATION_PODCAST = "com.bb.radio105.action.PLAY_NOTIFICATION_PODCAST";
    static final String ACTION_PLAY_PODCAST = "com.bb.radio105.action.PLAY_PODCAST";
    static final String ACTION_STOP_PODCAST = "com.bb.radio105.action.STOP_PODCAST";
    // Zoo Service Actions
    static final String ACTION_PAUSE_NOTIFICATION_ZOO = "com.bb.radio105.action.PAUSE_NOTIFICATION_ZOO";
    static final String ACTION_PAUSE_ZOO = "com.bb.radio105.action.PAUSE_ZOO";
    static final String ACTION_PLAY_NOTIFICATION_ZOO = "com.bb.radio105.action.PLAY_NOTIFICATION_ZOO";
    static final String ACTION_PLAY_ZOO = "com.bb.radio105.action.PLAY_ZOO";
    static final String ACTION_STOP_ZOO = "com.bb.radio105.action.STOP_ZOO";
    // 404 custom error page
    static final String ErrorPagePath = "file:///android_asset/index.html";
    // Write storage permission
    static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 0;
    // Post notification permission
    static final int PERMISSION_REQUEST_POST_NOTIFICATIONS = 1;
    // Log TAGS
    /* final static String LOG_TAG = MainActivity.class.getSimpleName(); */
    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce the volume instead of stopping playback.
    static final float VOLUME_DUCK = 0.2f;
    // The volume we set the media player when we have audio focus.
    static final float VOLUME_NORMAL = 1.0f;
    // WebView Constants
    static final String PODCAST_STATE = "podcast_state";
    static Bundle podcastBundle;
    static final String ZOO_STATE = "zoo_state";
    static Bundle zooBundle;
}
