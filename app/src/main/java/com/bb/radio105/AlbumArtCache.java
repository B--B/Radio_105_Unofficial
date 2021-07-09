package com.bb.radio105;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import java.io.IOException;

import timber.log.Timber;

final class AlbumArtCache {

    private static final int MAX_ALBUM_ART_CACHE_SIZE = 12*1024*1024;  // 12 MB
    private static final int MAX_ART_WIDTH = 800;  // pixels
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
        // Holds no more than MAX_ALBUM_ART_CACHE_SIZE bytes, bounded by maxmemory/4 and
        // Integer.MAX_VALUE:
        int maxSize = Math.min(MAX_ALBUM_ART_CACHE_SIZE,
                (int) (Math.min(Integer.MAX_VALUE, Runtime.getRuntime().maxMemory()/4)));
        mCache = new LruCache<String, Bitmap[]>(maxSize) {
            @Override
            protected int sizeOf(String key, Bitmap[] value) {
                return value[BIG_BITMAP_INDEX].getByteCount()
                        + value[ICON_BITMAP_INDEX].getByteCount();
            }
        };
    }

    public Bitmap getBigImage(String artUrl) {
        Bitmap[] result = mCache.get(artUrl);
        return result == null ? null : result[BIG_BITMAP_INDEX];
    }

    public Bitmap getIconImage(String artUrl) {
        Bitmap[] result = mCache.get(artUrl);
        return result == null ? null : result[ICON_BITMAP_INDEX];
    }

    public void fetch(final String artUrl, final FetchListener listener) {
        // 105 site use an online resizer for dynamically provide an artwork in the correct size. Unfortunately, the
        // artwork fetched have a poor quality. All artworks links have a fixed part "resizer/WIDTH/HEIGHT/true", here
        // the original link sizes will be changed to 800x800, for an higher quality image. If for some reason the
        // replace won't work the original string will be used.
        String artUrlResized = artUrl.replaceAll("(resizer/)[^&]*(/true)", "$1800/800$2");
        Timber.e("artUrl changed, new URL is %s", artUrlResized);
        Bitmap[] bitmap = mCache.get(artUrlResized);
        if (bitmap != null) {
            Timber.e("getOrFetch: album art is in cache, using it %s", artUrlResized);
            listener.onFetched(artUrlResized, bitmap[BIG_BITMAP_INDEX], bitmap[ICON_BITMAP_INDEX]);
            return;
        }

        Timber.e("getOrFetch: starting thread to fetch %s", artUrlResized);

            Thread thread = new Thread(() -> {
                Bitmap[] bitmaps;
                try {
                    Bitmap bitmap2 = BitmapHelper.fetchAndRescaleBitmap(artUrlResized,
                            MAX_ART_WIDTH, MAX_ART_HEIGHT);
                    Bitmap icon = BitmapHelper.scaleBitmap(bitmap2,
                            MAX_ART_WIDTH_ICON, MAX_ART_HEIGHT_ICON);
                    bitmaps = new Bitmap[] {bitmap2, icon};
                    mCache.put(artUrlResized, bitmaps);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        listener.onFetched(artUrlResized,
                                bitmaps[BIG_BITMAP_INDEX], bitmaps[ICON_BITMAP_INDEX]);
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            thread.start();
    }

    public static abstract class FetchListener {
        public abstract void onFetched(String artUrl, Bitmap bigImage, Bitmap iconImage);
        public void onError(String artUrl, Exception e) {
            Timber.e("AlbumArtFetchListener: error while downloading %s", artUrl);
        }
    }
}
