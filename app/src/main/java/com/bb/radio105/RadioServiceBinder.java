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

import static com.bb.radio105.RadioService.art;
import static com.bb.radio105.RadioService.djString;
import static com.bb.radio105.RadioService.mState;
import static com.bb.radio105.RadioService.mToken;
import static com.bb.radio105.RadioService.titleString;

import android.graphics.Bitmap;
import android.os.Binder;
import android.support.v4.media.session.MediaSessionCompat;

class RadioServiceBinder extends Binder {
    /** methods for clients */
    public MediaSessionCompat.Token getMediaSessionToken() {
        return mToken;
    }

    public Bitmap getArt() {
        return art;
    }

    public String getTitleString() {
        return titleString;
    }

    public String getDjString() {
        return djString;
    }

    public int getPlaybackState() {
        return mState;
    }
}
