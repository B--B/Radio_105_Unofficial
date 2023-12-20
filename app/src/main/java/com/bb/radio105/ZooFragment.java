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

import static com.bb.radio105.Constants.ACTION_PAUSE_ZOO;
import static com.bb.radio105.Constants.ACTION_PLAY_ZOO;
import static com.bb.radio105.Constants.ACTION_STOP_ZOO;
import static com.bb.radio105.ZooService.mState;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PictureInPictureParams;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;
import androidx.webkit.WebResourceErrorCompat;
import androidx.webkit.WebViewClientCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import com.bumptech.glide.MemoryCategory;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import timber.log.Timber;

public class ZooFragment extends Fragment implements IPodcastService {

    private WebView mWebView;
    private View root;
    private ProgressBar mProgressBar;
    private RadioServiceBinder mRadioServiceBinder;
    private MediaControllerCompat mMediaControllerCompat;
    static boolean isMediaPlayingPodcast;
    static boolean isVideoInFullscreen;
    static IPodcastService mIPodcastService;
    static String podcastTitle;
    static String podcastSubtitle;
    static String podcastImageUrl;
    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;

    @SuppressLint("SetJavaScriptEnabled")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_zoo, container, false);

        boolean screenOn = Utils.getUserPreferenceBoolean(requireContext(), getString(R.string.screen_on_key), false);
        boolean theme = Utils.getUserPreferenceBoolean(requireContext(), getString(R.string.webviews_themes_key), true);

        if (screenOn) {
            requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        if (!theme) {
            root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.zoo_background));
        }

        // Allow Glide to use more memory
        GlideApp.get(requireContext()).setMemoryCategory(MemoryCategory.HIGH);

        // Playback state interface
        mIPodcastService = this;

        //Acquire wake locks
        mWakeLock = ((PowerManager) requireContext().getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WARNING:ZooServiceWakelock");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mWifiLock = ((WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                    .createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "WARNING:ZooServiceWiFiWakelock");
        } else {
            mWifiLock = ((WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                    .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "WARNING:ZooServiceWiFiWakelock");
        }


        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                if (mWebView.canGoBack()) {
                    mWebView.goBack();
                } else {
                    Navigation.findNavController(root).navigate(R.id.nav_home);
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);

        mWebView = root.findViewById(R.id.webView_zoo);
        mProgressBar = root.findViewById(R.id.loading_zoo);

        final String[] hardwareAcceleration = getResources().getStringArray(R.array.hardware_acceleration_values);
        // The apps theme is decided depending upon the saved preferences on app startup
        String hardwareAccelerationPref = Utils.getUserPreferenceString(requireContext(), getString(R.string.hardware_acceleration_key), getString(R.string.hardware_acceleration_default_value));
        if (hardwareAccelerationPref.equals(hardwareAcceleration[0]))
            mWebView.setLayerType(View.LAYER_TYPE_NONE , null);
        if (hardwareAccelerationPref.equals(hardwareAcceleration[1]))
            mWebView.setLayerType(View.LAYER_TYPE_HARDWARE , null);
        if (hardwareAccelerationPref.equals(hardwareAcceleration[2]))
            mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE , null);

        String url = "https://zoo.105.net";

        mWebView.getSettings().setLoadsImagesAutomatically(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 8.0.0; SM-G960F Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.84 Mobile Safari/537.36");
        mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mWebView.getSettings().setOffscreenPreRaster(true);
        }
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mWebView.addJavascriptInterface(new JSInterfaceZoo(),"JSZOOOUT");
        if (Constants.zooBundle == null) {
            mWebView.loadUrl(url);
        } else {
            mWebView.restoreState(Objects.requireNonNull(Constants.zooBundle.getBundle(Constants.ZOO_STATE)));
        }

        mWebView.setWebViewClient(mWebViewClient);
        mWebView.setWebChromeClient(mWebChromeClient);

        mWebView.setDownloadListener((url1, userAgent, contentDisposition, mimetype, contentLength) -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    Utils.requestStoragePermission(requireActivity(), root);
                }
            }
            Utils.startDownload(requireActivity(), url1);
        });

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Bind music service only if is already running
        if (RadioService.mState != PlaybackStateCompat.STATE_STOPPED) {
            requireContext().bindService(new Intent(getContext(), RadioService.class), mServiceConnection, 0);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (RadioService.mState != PlaybackStateCompat.STATE_STOPPED) {
            // Unbind music service
            requireContext().unbindService(mServiceConnection);
        }
        if (mMediaControllerCompat != null) {
            mMediaControllerCompat = null;
        }
        if (Constants.zooBundle == null) {
            Timber.d("onStop: created new outState bundle!");
            Constants.zooBundle = new Bundle(ClassLoader.getSystemClassLoader());
        }
        final Bundle currentWebViewState = new Bundle(ClassLoader.getSystemClassLoader());
        if (mWebView.saveState(currentWebViewState) == null) {
            Timber.d("onStop: failed to obtain WebView state to save!");
        }
        Constants.zooBundle.putBundle(Constants.ZOO_STATE, currentWebViewState);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mState ==  PlaybackStateCompat.STATE_STOPPED && !isVideoInFullscreen) {
            if (mWebView != null) {
                mWebView.onPause();
                mWebView.pauseTimers();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mState ==  PlaybackStateCompat.STATE_STOPPED && !isVideoInFullscreen) {
            if (mWebView != null) {
                mWebView.onResume();
                mWebView.resumeTimers();
            }
        }
    }

    @Override
    public void onDestroyView() {

        boolean screenOn = Utils.getUserPreferenceBoolean(requireContext(), getString(R.string.screen_on_key), false);
        if (screenOn) {
            requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
        mWakeLock = null;
        mWifiLock = null;
        if (mState !=  PlaybackStateCompat.STATE_STOPPED) {
            Timber.e("Stopping Podcast Service");
            isMediaPlayingPodcast = false;
            stopPodcast();
            podcastTitle = null;
            podcastSubtitle = null;
            podcastImageUrl = null;
        }
        mIPodcastService = null;
        mRadioServiceBinder = null;
        if (mMediaControllerCompat != null) {
            mMediaControllerCompat = null;
        }
        if (mWebView != null) {
            mWebView.removeAllViews();
            mWebView.destroy();
        }
        mProgressBar = null;
        root = null;
        // Restore Glide memory values
        GlideApp.get(requireContext()).setMemoryCategory(MemoryCategory.NORMAL);
        super.onDestroyView();
        mWebView = null;
    }

    private InputStream getBitmapInputStream(Bitmap bitmap, Bitmap.CompressFormat compressFormat) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(compressFormat, 80, byteArrayOutputStream);
        byte[] mByte = byteArrayOutputStream.toByteArray();
        return new ByteArrayInputStream(mByte);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Timber.e("Connection successful");
            mRadioServiceBinder = (RadioServiceBinder) service;
            mMediaControllerCompat = new MediaControllerCompat(getContext(), mRadioServiceBinder.getMediaSessionToken());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Timber.e("Service crashed");
        }
    };

    @Override
    public void playbackState(String playbackState) {
        Timber.e("Playback state changed, new state is %s", playbackState);
        if (playbackState.equals("Play")) {
            mWebView.evaluateJavascript("javascript:(player.play());", null);
            // FIXME: this shit is necessary as 105.net closes the connection if the audio is paused for a few minutes.
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isMediaPlayingPodcast) {
                    Timber.e("!HACK! Trying to start the stream again");
                    mWebView.evaluateJavascript("javascript:(function() { player.pause(); player.play();})()", null);
                }
            }, 100);
        } else {
            mWebView.evaluateJavascript("javascript:(player.pause());", null);
        }
    }

    @SuppressLint("WakelockTimeout")
    class JSInterfaceZoo {
        @JavascriptInterface
        public void mediaZooAction(String mString) {
            Timber.e("isMediaPlayingPodcast is %s", mString);
            isMediaPlayingPodcast = Boolean.parseBoolean(mString);
            if (isMediaPlayingPodcast) {
                if (!mWakeLock.isHeld()) {
                    mWakeLock.acquire();
                }
                if (!mWifiLock.isHeld()) {
                    mWifiLock.acquire();
                }
                if (RadioService.mState != PlaybackStateCompat.STATE_STOPPED) {
                    mMediaControllerCompat.getTransportControls().stop();
                    requireContext().unbindService(mServiceConnection);
                }
                if (mState ==  PlaybackStateCompat.STATE_STOPPED || mState ==  PlaybackStateCompat.STATE_PAUSED) {
                    Timber.e("Received play request from ZooFragment");
                    playPodcast();
                }
            } else {
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
                if (mWifiLock.isHeld()) {
                    mWifiLock.release();
                }
                if (mState ==  PlaybackStateCompat.STATE_PLAYING) {
                    Timber.e("Received pause request from ZooFragment");
                    pausePodcast();
                }
            }
        }

        @JavascriptInterface
        public void getPodcastTitle(String mString) {
            Timber.e("Podcast title is %s", mString);
            podcastTitle = mString;
        }

        @JavascriptInterface
        public void getPodcastSubtitle(String mString) {
            Timber.e("Podcast subtitle is %s", mString);
            podcastSubtitle = mString;
        }

        @JavascriptInterface
        public void getPodcastImage(String mString) {
            // 105 site use an online resizer for dynamically provide an artwork in the correct size. Unfortunately, the
            // artwork fetched have a poor quality. All artworks links have a fixed part "resizer/WIDTH/HEIGHT/true", here
            // the original link sizes will be changed to 480x480, for an higher quality image. If for some reason the
            // replace won't work the original string will be used.
            podcastImageUrl = mString.replaceAll("(resizer/)[^&]*(/true)", "$1480/480$2");
            Timber.e("artUrl changed, new URL is %s", podcastImageUrl);
        }

        @JavascriptInterface
        public void getVideoFullscreenState(String mString) {
            Timber.e("isVideoInFullscreen is %s", mString);
            isVideoInFullscreen = Boolean.parseBoolean(mString);
        }
    }

    private void playPodcast() {
        Intent mIntent = new Intent();
        mIntent.setAction(ACTION_PLAY_ZOO);
        mIntent.setPackage(requireContext().getPackageName());
        requireContext().startService(mIntent);
    }

    private void pausePodcast() {
        Intent mIntent = new Intent();
        mIntent.setAction(ACTION_PAUSE_ZOO);
        mIntent.setPackage(requireContext().getPackageName());
        requireContext().startService(mIntent);
    }

    private void stopPodcast() {
        Intent mIntent = new Intent();
        mIntent.setAction(ACTION_STOP_ZOO);
        mIntent.setPackage(requireContext().getPackageName());
        requireContext().startService(mIntent);
    }

    final WebViewClient mWebViewClient = new WebViewClientCompat() {

        @Override
        public boolean shouldOverrideUrlLoading (@NonNull WebView webView, WebResourceRequest request) {
            if (Objects.requireNonNull(Uri.parse(request.getUrl().toString()).getHost()).contains("zoo.105.net")) {
                return false;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.unsupported_title);
            builder.setMessage(R.string.unsupported_description);
            builder.setNegativeButton(R.string.cancel, null);
            builder.setPositiveButton(R.string.open_browser, (dialog, id) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(String.valueOf(Uri.parse(request.getUrl().toString()))))));
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            return true;
        }

        @Override
        public void onPageStarted(WebView webView, String url, Bitmap mBitmap) {
            webView.setVisibility(View.INVISIBLE);
            mProgressBar.setVisibility(View.VISIBLE);
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            if (mWifiLock.isHeld()) {
                mWifiLock.release();
            }
            if (mState !=  PlaybackStateCompat.STATE_STOPPED) {
                stopPodcast();
                podcastTitle = null;
                podcastSubtitle = null;
                podcastImageUrl = null;
            }
            super.onPageStarted(webView, url, mBitmap);
        }

        @Override
        public void onPageFinished (WebView webView, String url) {
            boolean postCallbackKey = Utils.getUserPreferenceBoolean(requireContext(), getString(R.string.post_callback_key), true);

            final String legacyPodcastService = "javascript:(function() { " +
                    "var audio = document.querySelector('audio'); " +
                    "if (document.body.contains(audio)) { " +
                    "    audio.style.minWidth = '90%';" +
                    "    audio.style.margin= '0 auto';" +
                    "    audio.onplay = function() {" +
                    "            JSZOOOUT.mediaZooAction('true');" +
                    "        };" +
                    "        audio.onpause = function() {" +
                    "            JSZOOOUT.mediaZooAction('false');" +
                    "        };" +
                    "    }" +
                    "    var podcastText = document.getElementsByClassName('titolo_articolo titolo');" +
                    "    if (podcastText.length) {" +
                    "        var text = podcastText[0].textContent;" +
                    "        JSZOOOUT.getPodcastTitle(text);" +
                    "        var image = document.querySelector('[itemprop=thumbnailUrl][content]');" +
                    "        if (document.body.contains(image)) { " +
                    "            JSZOOOUT.getPodcastImage(image.content);" +
                    "        }" +
                    "    }" +
                    "    var podcastSubText = document.getElementsByClassName('sottotitolo_articolo sottotitolo');" +
                    "    if (podcastSubText.length) {" +
                    "        var text = podcastSubText[0].textContent;" +
                    "        JSZOOOUT.getPodcastSubtitle(text);" +
                    "    }" +
                    "    console.log('legacyPodcastService javascript executed');" +
                    "})()";

            final String podcastService = "javascript:(function() { " +
                    "var audio = document.querySelector('audio'); " +
                    "if (document.body.contains(audio)) { " +
                    "    audio.style.minWidth = '90%';" +
                    "    audio.style.margin= '0 auto';" +
                    "    audio.controlsList.remove('nodownload');" +
                    "    audio.onplay = function() {" +
                    "            JSZOOOUT.mediaZooAction('true');" +
                    "        };" +
                    "        audio.onpause = function() {" +
                    "            JSZOOOUT.mediaZooAction('false');" +
                    "        };" +
                    "    }" +
                    "    var podcastText = document.getElementsByClassName('titolo_articolo titolo');" +
                    "    if (podcastText.length) {" +
                    "        var text = podcastText[0].textContent;" +
                    "        JSZOOOUT.getPodcastTitle(text);" +
                    "        var image = document.querySelector('[itemprop=thumbnailUrl][content]');" +
                    "        if (document.body.contains(image)) { " +
                    "            JSZOOOUT.getPodcastImage(image.content);" +
                    "        }" +
                    "    }" +
                    "    var podcastSubText = document.getElementsByClassName('sottotitolo_articolo sottotitolo');" +
                    "    if (podcastSubText.length) {" +
                    "        var text = podcastSubText[0].textContent;" +
                    "        JSZOOOUT.getPodcastSubtitle(text);" +
                    "    }" +
                    "    console.log('podcastService javascript executed');" +
                    "})()";

            final String pipModeEnabled = "javascript:(function() { " +
                    "    function onFullScreen(e) {" +
                    "        var isFullscreenNow = document.webkitFullscreenElement !== null;" +
                    "        JSZOOOUT.getVideoFullscreenState(isFullscreenNow);" +
                    "    }" +
                    "    document.addEventListener('webkitfullscreenchange', onFullScreen);" +
                    "    document.addEventListener('fullscreenchange', onFullScreen);" +
                    "    console.log('pipModeEnabled javascript executed');" +
                    "})()";


            final String lightModeEnabled = "javascript:(function() { " +
                    "document.body.style.backgroundColor = 'transparent';" +
                    "var element = document.getElementsByClassName('row vc_bg_black');" +
                    " if (element.length) { element[0].style.backgroundColor = 'transparent' }; " +
                    "var element = document.getElementsByClassName('row vc_bg_black');" +
                    " if (element.length) { " +
                    "     var element2 = element[0].getElementsByClassName('titolo vc_title');" +
                    "     if (element2.length) { " +
                    "         for (var i = 0; i < element2.length; i++) { " +
                    "             element2[i].style.color = '#121212'; " +
                    "         }" +
                    "     }; " +
                    " }; " +
                    "var element = document.getElementsByClassName('anteprima_slider vc_preview_slider_big_bt vc_txt_m vc_theme_default vc_theme_zoo ');" +
                    " if (element.length) { " +
                    "     for (var i = 0; i < element.length; i++) { " +
                    "         var element2 = element[i].getElementsByClassName('titolo');" +
                    "         if (element2.length) { " +
                    "             for (var i2 = 0; i2 < element2.length; i2++) { " +
                    "                 element2[i2].style.color = '#121212'; " +
                    "             }" +
                    "         }; " +
                    "     }; " +
                    " }; " +
                    "var element = document.getElementsByClassName('anteprima_ipiu anteprima_ipiu_counter_1 vc_box_scenette vc_txt_xs vc_theme_default vc_theme_zoo');" +
                    " if (element.length) { " +
                    "     var element2 = element[0].getElementsByClassName('titolo');" +
                    "     if (element2.length) { " +
                    "         for (var i = 0; i < element2.length; i++) { " +
                    "             element2[i].style.color = '#121212'; " +
                    "         }" +
                    "     }; " +
                    " }; " +
                    "var element = document.getElementsByClassName('anteprima_ipiu anteprima_ipiu_counter_1 vc_box_trends vc_txt_xs vc_theme_default vc_theme_zoo');" +
                    " if (element.length) { " +
                    "     var element2 = element[0].getElementsByClassName('titolo');" +
                    "     if (element2.length) { " +
                    "         for (var i = 0; i < element2.length; i++) { " +
                    "             element2[i].style.color = '#121212'; " +
                    "         }" +
                    "     }; " +
                    " }; " +
                    "var element = document.getElementsByClassName('anteprima_ipiu anteprima_ipiu_counter_1 vc_box_repliche_video vc_txt_xs vc_theme_default vc_theme_zoo null');" +
                    " if (element.length) { " +
                    "     var element2 = element[0].getElementsByClassName('titolo');" +
                    "     if (element2.length) { " +
                    "         for (var i = 0; i < element2.length; i++) { " +
                    "             element2[i].style.color = '#121212'; " +
                    "         }" +
                    "     }; " +
                    " }; " +
                    "var element = document.getElementsByClassName('anteprima_ipiu anteprima_ipiu_counter_1 vc_box_repliche vc_txt_xs vc_theme_default vc_theme_zoo null');" +
                    " if (element.length) { " +
                    "     var element2 = element[0].getElementsByClassName('titolo');" +
                    "     if (element2.length) { " +
                    "         for (var i = 0; i < element2.length; i++) { " +
                    "             element2[i].style.color = '#121212'; " +
                    "         }" +
                    "     }; " +
                    " }; " +
                    "var element = document.getElementsByClassName('articoli_correlati');" +
                    " if (element.length) { " +
                    "     var element2 = element[0].getElementsByClassName('titolo');" +
                    "     if (element2.length) { " +
                    "         for (var i = 0; i < element2.length; i++) { " +
                    "             element2[i].style.color = '#121212'; " +
                    "         }" +
                    "     }; " +
                    " }; " +
                    "var element = document.getElementsByClassName('anteprima_articolo');" +
                    " if (element.length) { " +
                    "     for (var i = 0; i < element.length; i++) { " +
                    "         var element2 = element[i].getElementsByClassName('titolo');" +
                    "         if (element2.length) { " +
                    "             for (var i2 = 0; i2 < element2.length; i2++) { " +
                    "                 element2[i2].style.color = '#121212'; " +
                    "             }" +
                    "         }; " +
                    "     }; " +
                    " }; " +
                    "var element = document.getElementsByClassName('anteprima_ricerca_archivio');" +
                    " if (element.length) { " +
                    "     for (var i = 0; i < element.length; i++) { " +
                    "         var element2 = element[i].getElementsByTagName('a');" +
                    "         if (element2.length) { " +
                    "             for (var i2 = 0; i2 < element2.length; i2++) { " +
                    "                 element2[i2].style.color = '#121212'; " +
                    "             }" +
                    "         }; " +
                    "     }; " +
                    " }; " +
                    "var element = document.getElementsByClassName('intestazione_ricerca_archivio vc_results_found');" +
                    " if (element.length) { " +
                    "     var element2 = element[0].getElementsByTagName('p');" +
                    "     if (element2.length) { " +
                    "         for (var i2 = 0; i2 < element2.length; i2++) { " +
                    "             element2[i2].style.color = '#121212'; " +
                    "         }" +
                    "     }; " +
                    " }; " +
                    "var element = document.getElementsByClassName('sortElementBox');" +
                    " if (element.length) { " +
                    "     var element2 = element[0].getElementsByTagName('a');" +
                    "     if (element2.length) { " +
                    "         for (var i2 = 0; i2 < element2.length; i2++) { " +
                    "             element2[i2].style.color = '#121212'; " +
                    "         }" +
                    "     }; " +
                    " }; " +
                    "var element = document.getElementsByClassName('pagination');" +
                    " if (element.length) { " +
                    "     var element2 = element[0].getElementsByTagName('a');" +
                    "     if (element2.length) { " +
                    "         for (var i2 = 0; i2 < element2.length; i2++) { " +
                    "             element2[i2].style.color = '#121212'; " +
                    "         }" +
                    "     }; " +
                    " }; " +
                    "var element = document.getElementsByClassName('sommario_articolo testoResize');" +
                    " if (element.length) { " +
                    "     var element2 = element[0].getElementsByTagName('p');" +
                    "     if (element2.length) { " +
                    "         for (var i2 = 0; i2 < element2.length; i2++) { " +
                    "             element2[i2].style.color = '#121212'; " +
                    "         }" +
                    "     }; " +
                    " }; " +
                    "var element = document.getElementsByClassName(' link');" +
                    " if (element.length) { " +
                    "     var element2 = element[0].getElementsByTagName('a');" +
                    "     if (element2.length) { " +
                    "         element2[0].style.color = '#121212'; " +
                    "     }; " +
                    " }; " +
                    "var element = document.getElementsByClassName('container vc_bg_dark_grey');" +
                    " if (element.length) { " +
                    "     for (var i = 0; i < element.length; i++) { " +
                    "         element[i].style.backgroundColor = 'transparent'; " +
                    "     }" +
                    " }; " +
                    "var element = document.getElementsByClassName('titolo_articolo titolo');" +
                    " if (element.length) { element[0].style.color = '#121212' }; " +
                    "var element = document.getElementsByClassName('sottotitolo_articolo sottotitolo');" +
                    " if (element.length) { element[0].style.color = '#121212' }; " +
                    "var element = document.getElementsByClassName('data_articolo data');" +
                    " if (element.length) { element[0].style.color = '#121212' }; " +
                    "console.log('lightModeEnabled javascript executed');" +
                    "})()";

            final String darkModeEnabled = "javascript:(function() { " +
                    "document.body.style.backgroundColor = '#121212';" +
                    "var element = document.getElementsByClassName('container vc_bg_white');" +
                    " if (element.length) { element[0].style.backgroundColor = '#121212' }; " +
                    "var element = document.getElementsByClassName('testo_articolo testo testoResize');" +
                    " if (element.length) { " +
                    "     var element2 = element[0].getElementsByTagName('p');" +
                    "     if (element2.length) { " +
                    "         for (var i = 0; i < element.length; i++) { " +
                    "             element[i].style.color = 'white'; " +
                    "         }" +
                    "     }; " +
                    " }; " +
                    "console.log('darkModeEnabled javascript executed');" +
                    "})()";

            final String javaScript = "javascript:(function() { " +
                    "var element = document.getElementsByClassName('player-container vc_mediaelementjs');" +
                    " if (element.length) { element[0].style.width = '100%' }; " +
                    "var element = document.getElementsByClassName('container');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('clear');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('col-xs-12');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('tags vc_article_tag vc_theme_article_zoo');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('col-xs-12 vc_bg_white');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('spacer spacer t_20');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('container vc_bg_black');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('text_edit vc_textedit_box_previews vc_column vc_theme_zoo');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('vc_container_social_button vc_theme_zoo');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('bannervcms banner_rectangle_mobile_320x50_3 ');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('iubenda-cs-container');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('share vc_share_buttons null');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('bannervcms banner_masthead ');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('cerca vc_search_bar');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('spacer spacer t_40');" +
                    " if (element.length) { element[element.length -1].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('text_edit vc_textedit_box_previews vc_theme_zoo');" +
                    " if (element.length) { element[element.length -1].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('anteprima_articolo article_cont vc_preview_small_right_webradio vc_txt_xs vc_theme_default" +
                    " vc_theme_zoo Zoo di 105 vc_section_zoo-radio vc_macro_section_webradio vc_macro_section_canale-105 zooradio scheda cms_article ');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "console.log('Common javascript executed');" +
                    "})()";



            boolean theme = Utils.getUserPreferenceBoolean(requireContext(), getString(R.string.webviews_themes_key), true);
            if (theme) {
                final String[] darkModeValues = getResources().getStringArray(R.array.theme_values);
                // The apps theme is decided depending upon the saved preferences on app startup
                String themePref = PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getString(getString(R.string.theme_key), getString(R.string.theme_default_value));
                // Comparing to see which preference is selected and applying those theme settings
                if (themePref.equals(darkModeValues[0])) {
                    // Check system status and apply colors
                    int nightModeOn = getResources().getConfiguration().uiMode &
                            Configuration.UI_MODE_NIGHT_MASK;
                    switch (nightModeOn) {
                        case Configuration.UI_MODE_NIGHT_YES:
                            webView.evaluateJavascript(darkModeEnabled, null);
                            break;
                        case Configuration.UI_MODE_NIGHT_NO:
                        case Configuration.UI_MODE_NIGHT_UNDEFINED:
                            webView.evaluateJavascript(lightModeEnabled, null);
                            break;
                    }
                }
                if (themePref.equals(darkModeValues[1])) {
                    webView.evaluateJavascript(lightModeEnabled, null);
                }
                if (themePref.equals(darkModeValues[2])) {
                    webView.evaluateJavascript(darkModeEnabled, null);
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                webView.evaluateJavascript(podcastService, null);
            } else {
                webView.evaluateJavascript(legacyPodcastService, null);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                webView.evaluateJavascript(pipModeEnabled, null);
            }
            webView.evaluateJavascript(javaScript, null);
            if (postCallbackKey) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.VISUAL_STATE_CALLBACK)) {
                    final int mPostVisualStateCallbackId = 468;
                    WebViewCompat.postVisualStateCallback(webView, mPostVisualStateCallbackId, requestId -> {
                        if (requestId == mPostVisualStateCallbackId) {
                            if (mProgressBar != null) {
                                mProgressBar.setVisibility(View.INVISIBLE);
                            }
                            webView.setVisibility(View.VISIBLE);
                        }
                    });
                } else {
                    Timber.e("postVisualStateCallback not supported, fallback to handler method");
                    Utils.makeWebViewVisible(webView, mProgressBar);
                }
            } else {
                Utils.makeWebViewVisible(webView, mProgressBar);
            }
            super.onPageFinished(webView, url);
        }

        @Override
        public void onReceivedError(@NonNull WebView webView, @NonNull WebResourceRequest request, @NonNull WebResourceErrorCompat error) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_RESOURCE_ERROR_GET_CODE)) {
                // Ignore some connection errors
                // ERR_FAILED = -1
                // ERR_ADDRESS_UNREACHABLE = -2
                // ERR_CONNECTION_REFUSED = -6
                int errorCode = error.getErrorCode();
                if (errorCode != -1 && errorCode != -2 && errorCode != -6) {
                    webView.loadUrl(Constants.ErrorPagePath);
                }
            }
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view,
                                                          WebResourceRequest request) {

            try {
                String url = request.getUrl().toString();
                ZooFragment mZooFragment = ZooFragment.this;
                Bitmap bitmap;
                if (url.startsWith("https://adv.mediamond.it") || url.startsWith("https://tags.tiqcdn.com/") ||
                        url.endsWith("mediaelement-and-player.min.js") || url.endsWith("cookiecuttr.js") ||
                        url.endsWith("cookie_law.jsp") || url.endsWith("webtrekk_v3.min.js") ||
                        url.endsWith("analytics.js")) {
                    Timber.e("Javascript intercepted: %s", url);
                    return new WebResourceResponse("text/javascript", "UTF-8", new ByteArrayInputStream("// Script Blocked".getBytes(StandardCharsets.UTF_8)));
                } else if (url.toLowerCase(Locale.ROOT).endsWith(".jpg") || url.toLowerCase(Locale.ROOT).endsWith(".jpeg")) {
                    bitmap = GlideApp.with(view).asBitmap().diskCacheStrategy(DiskCacheStrategy.ALL).load(url).submit().get();
                    return new WebResourceResponse("image/jpg", "UTF-8", mZooFragment.getBitmapInputStream(bitmap, Bitmap.CompressFormat.JPEG));
                } else if (url.toLowerCase(Locale.ROOT).endsWith(".png")) {
                    bitmap = GlideApp.with(view).asBitmap().diskCacheStrategy(DiskCacheStrategy.ALL).load(url).submit().get();
                    return new WebResourceResponse("image/png", "UTF-8", mZooFragment.getBitmapInputStream(bitmap, Bitmap.CompressFormat.PNG));
                } else if (url.toLowerCase(Locale.ROOT).endsWith(".webp")) {
                    bitmap = GlideApp.with(view).asBitmap().diskCacheStrategy(DiskCacheStrategy.ALL).load(url).submit().get();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        return new WebResourceResponse("image/webp", "UTF-8", mZooFragment.getBitmapInputStream(bitmap, Bitmap.CompressFormat.WEBP_LOSSY));
                    } else {
                        //noinspection deprecation
                        return new WebResourceResponse("image/webp", "UTF-8", mZooFragment.getBitmapInputStream(bitmap, Bitmap.CompressFormat.WEBP));
                    }
                } else {
                    return super.shouldInterceptRequest(view, request);
                }
            } catch (ExecutionException | InterruptedException e) {
                Timber.e("Image load failed with error %s", e);
                return super.shouldInterceptRequest(view, request);
            }
        }
    };

    final WebChromeClient mWebChromeClient = new WebChromeClient() {

        private View fullScreenView;
        private CustomViewCallback mViewCallback;

        @Override
        public void onProgressChanged(final WebView view, final int newProgress) {
            if (mProgressBar != null) {
                mProgressBar.setProgress(newProgress);
            }
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback mCustomViewCallback) {
            if (fullScreenView != null) {
                onHideCustomView();
                return;
            }
            fullScreenView = view;
            mViewCallback = mCustomViewCallback;
            ((FrameLayout)requireActivity().getWindow().getDecorView()).addView(fullScreenView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            Utils.setUpFullScreen(requireActivity());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                fullScreenView.addOnLayoutChangeListener((v, left, top, right, bottom,
                                                     oldLeft, oldTop, oldRight, oldBottom) -> {
                    if (left != oldLeft || right != oldRight || top != oldTop
                            || bottom != oldBottom) {
                        // The fullScreenView’s bounds changed, update the source hint rect to
                        // reflect its new bounds.
                        final Rect sourceRectHint = new Rect();
                        fullScreenView.getGlobalVisibleRect(sourceRectHint);
                        requireActivity().setPictureInPictureParams(
                                new PictureInPictureParams.Builder()
                                        .setSourceRectHint(sourceRectHint)
                                        .build());
                    }
                });
            }
        }

        @Override
        public void onHideCustomView() {
            ((FrameLayout)requireActivity().getWindow().getDecorView()).removeView(fullScreenView);
            fullScreenView = null;
            Utils.restoreScreen(requireActivity());
            mViewCallback.onCustomViewHidden();
            mViewCallback = null;
        }
    };
}
