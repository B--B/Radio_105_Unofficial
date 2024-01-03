/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import java.io.IOException;

import timber.log.Timber;

final class AlbumArtCache {

    private static final int MAX_ALBUM_ART_CACHE_SIZE = 12*1024*1024;  // 12 MB
    private static final int MAX_ART_WIDTH = 480;  // pixels
    private static final int MAX_ART_HEIGHT = 480;  // pixels

    // Resolution reasonable for carrying around as an icon (generally in
    // MediaDescription.getIconBitmap). This should not be bigger than necessary, because
    // the MediaDescription object should be lightweight. If you set it too high and try to
    // serialize the MediaDescription, you may get FAILED BINDER TRANSACTION errors.
    private static final int MAX_ART_WIDTH_ICON = 196;  // pixels
    private static final int MAX_ART_HEIGHT_ICON = 196;  // pixels
    private static final int BIG_BITMAP_INDEX = 0;
    private static final int ICON_BITMAP_INDEX = 1;

    private final LruCache<String, Bitmap[]> mCache;
    private static final AlbumArtCache sInstance = new AlbumArtCache();

    public static AlbumArtCache getInstance() {
        return sInstance;
    }

    private AlbumArtCache() {
        super();
        // Holds no more than MAX_ALBUM_ART_CACHE_SIZE bytes, bounded by max memory/4 and
        // Integer.MAX_VALUE:
        int maxSize = Math.min(MAX_ALBUM_ART_CACHE_SIZE,
                (int) (Math.min(Integer.MAX_VALUE, Runtime.getRuntime().maxMemory()/4)));
        mCache = new LruCache<>(maxSize) {
            @Override
            protected int sizeOf(String key, Bitmap[] value) {
                return value[BIG_BITMAP_INDEX].getByteCount()
                        + value[ICON_BITMAP_INDEX].getByteCount();
            }
        };
    }

    /* These functions are not needed at the moment
    public Bitmap getBigImage(String artUrl) {
        Bitmap[] result = mCache.get(artUrl);
        return result == null ? null : result[BIG_BITMAP_INDEX];
    }

    public Bitmap getIconImage(String artUrl) {
        Bitmap[] result = mCache.get(artUrl);
        return result == null ? null : result[ICON_BITMAP_INDEX];
    }
    */

    public void fetch(final String artUrl, final FetchListener listener) {
        Bitmap[] bitmap = mCache.get(artUrl.substring(0, 70));
        if (bitmap != null) {
            Timber.e("getOrFetch: album art is in cache, using it %s", artUrl);
            listener.onFetched(bitmap[BIG_BITMAP_INDEX], bitmap[ICON_BITMAP_INDEX]);
            return;
        }

        Timber.e("getOrFetch: starting thread to fetch %s", artUrl);

            Thread thread = new Thread(() -> {
                Bitmap[] bitmaps;
                try {
                    Bitmap bitmap2 = BitmapHelper.fetchAndRescaleBitmap(artUrl,
                            MAX_ART_WIDTH, MAX_ART_HEIGHT);
                    Bitmap icon = BitmapHelper.scaleBitmap(bitmap2,
                            MAX_ART_WIDTH_ICON, MAX_ART_HEIGHT_ICON);
                    bitmaps = new Bitmap[] {bitmap2, icon};

                    mCache.put(artUrl.substring(0, 70), bitmaps);
                    new Handler(Looper.getMainLooper()).post(() -> listener.onFetched(
                            bitmaps[BIG_BITMAP_INDEX], bitmaps[ICON_BITMAP_INDEX]));
                } catch (IOException e) {
                    Timber.e(e, "getOrFetch: error fetching bitmap");
                }
            });
            thread.start();
    }

    public static abstract class FetchListener {
        public abstract void onFetched(Bitmap bigImage, Bitmap iconImage);
    }
}
