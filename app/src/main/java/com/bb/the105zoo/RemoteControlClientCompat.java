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

package com.bb.the105zoo;

import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * RemoteControlClient enables exposing information meant to be consumed by remote controls capable
 * of displaying metadata, artwork and media transport control buttons. A remote control client
 * object is associated with a media button event receiver. This event receiver must have been
 * previously registered with
 * {@link android.media.AudioManager#registerMediaButtonEventReceiver(android.content.ComponentName)}
 * before the RemoteControlClient can be registered through
 * {@link android.media.AudioManager#registerRemoteControlClient(android.media.RemoteControlClient)}.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class RemoteControlClientCompat {

    private static final String TAG = "RemoteControlCompat";

    private static Class sRemoteControlClientClass;

    // RCC short for RemoteControlClient
    private static Method sRCCEditMetadataMethod;
    private static Method sRCCSetPlayStateMethod;
    private static Method sRCCSetTransportControlFlags;

    private static boolean sHasRemoteControlAPIs = false;

    static {
        try {
            ClassLoader classLoader = RemoteControlClientCompat.class.getClassLoader();
            sRemoteControlClientClass = getActualRemoteControlClientClass(classLoader);
            // dynamically populate the playstate and flag values in case they change
            // in future versions.
            for (Field field : RemoteControlClientCompat.class.getFields()) {
                try {
                    Field realField = sRemoteControlClientClass.getField(field.getName());
                    Object realValue = realField.get(null);
                    field.set(null, realValue);
                } catch (NoSuchFieldException e) {
                    Log.w(TAG, "Could not get real field: " + field.getName());
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Error trying to pull field value for: " + field.getName()
                            + " " + e.getMessage());
                } catch (IllegalAccessException e) {
                    Log.w(TAG, "Error trying to pull field value for: " + field.getName()
                            + " " + e.getMessage());
                }
            }

            // get the required public methods on RemoteControlClient
            sRCCSetPlayStateMethod = sRemoteControlClientClass.getMethod("setPlaybackState",
                    int.class);
            sRCCSetTransportControlFlags = sRemoteControlClientClass.getMethod(
                    "setTransportControlFlags", int.class);

            sHasRemoteControlAPIs = true;
        } catch (ClassNotFoundException e) {
            // Silently fail when running on an OS before ICS.
        } catch (NoSuchMethodException e) {
            // Silently fail when running on an OS before ICS.
        } catch (IllegalArgumentException e) {
            // Silently fail when running on an OS before ICS.
        } catch (SecurityException e) {
            // Silently fail when running on an OS before ICS.
        }
    }

    public static Class getActualRemoteControlClientClass(ClassLoader classLoader)
            throws ClassNotFoundException {
        return classLoader.loadClass("android.media.RemoteControlClient");
    }

    private Object mActualRemoteControlClient;

    public RemoteControlClientCompat(PendingIntent pendingIntent) {
        if (!sHasRemoteControlAPIs) {
            return;
        }
        try {
            mActualRemoteControlClient =
                    sRemoteControlClientClass.getConstructor(PendingIntent.class)
                            .newInstance(pendingIntent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteControlClientCompat(PendingIntent pendingIntent, Looper looper) {
        if (!sHasRemoteControlAPIs) {
            return;
        }

        try {
            mActualRemoteControlClient =
                    sRemoteControlClientClass.getConstructor(PendingIntent.class, Looper.class)
                            .newInstance(pendingIntent, looper);
        } catch (Exception e) {
            Log.e(TAG, "Error creating new instance of " + sRemoteControlClientClass.getName(), e);
        }
    }

    /**
     * Sets the current playback state.
     * @param state The current playback state, one of the following values:
     *       {@link android.media.RemoteControlClient#PLAYSTATE_STOPPED},
     *       {@link android.media.RemoteControlClient#PLAYSTATE_PAUSED},
     *       {@link android.media.RemoteControlClient#PLAYSTATE_PLAYING},
     *       {@link android.media.RemoteControlClient#PLAYSTATE_FAST_FORWARDING},
     *       {@link android.media.RemoteControlClient#PLAYSTATE_REWINDING},
     *       {@link android.media.RemoteControlClient#PLAYSTATE_SKIPPING_FORWARDS},
     *       {@link android.media.RemoteControlClient#PLAYSTATE_SKIPPING_BACKWARDS},
     *       {@link android.media.RemoteControlClient#PLAYSTATE_BUFFERING},
     *       {@link android.media.RemoteControlClient#PLAYSTATE_ERROR}.
     */
    public void setPlaybackState(int state) {
        if (sHasRemoteControlAPIs) {
            try {
                sRCCSetPlayStateMethod.invoke(mActualRemoteControlClient, state);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Sets the flags for the media transport control buttons that this client supports.
     * @param transportControlFlags A combination of the following flags:
     *      {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_PREVIOUS},
     *      {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_REWIND},
     *      {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_PLAY},
     *      {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_PLAY_PAUSE},
     *      {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_PAUSE},
     *      {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_STOP},
     *      {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_FAST_FORWARD},
     *      {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_NEXT}
     */
    public void setTransportControlFlags(int transportControlFlags) {
        if (sHasRemoteControlAPIs) {
            try {
                sRCCSetTransportControlFlags.invoke(mActualRemoteControlClient,
                        transportControlFlags);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public final Object getActualRemoteControlClientObject() {
        return mActualRemoteControlClient;
    }
}
