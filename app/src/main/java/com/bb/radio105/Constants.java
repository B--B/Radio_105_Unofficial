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

public class Constants {
    public static final String ACTION_PLAY = "com.bb.radio105.action.PLAY";
    public static final String ACTION_PAUSE = "com.bb.radio105.action.PAUSE";
    public static final String ACTION_STOP = "com.bb.radio105.action.STOP";
    public static final String ACTION_PLAY_NOTIFICATION = "com.bb.radio105.action.PLAY_NOTIFICATION";
    public static final String ACTION_PAUSE_NOTIFICATION = "com.bb.radio105.action.PAUSE_NOTIFICATION";
    public static final String ACTION_STOP_NOTIFICATION = "com.bb.radio105.action.STOP_NOTIFICATION";
    public static final String ACTION_ERROR = "com.bb.radio105.action.ERROR";
    // 404 custom error page
    static final String ErrorPagePath = "file:///android_asset/index.html";
    // Write storage permission
    static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 0;
    // Log TAGS
    /* final static String LOG_TAG = MainActivity.class.getSimpleName(); */
    // The volume we set the media player to when we lose audio focus, but are allowed to reduce
    // the volume instead of stopping playback.
    static final float DUCK_VOLUME = 0.1f;
}
