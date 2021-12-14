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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.MemoryCategory;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import timber.log.Timber;

public class ZooFragment extends Fragment implements IPodcastService {

    private WebView mWebView;
    private View root;
    private ProgressBar mProgressBar;
    private RadioServiceBinder mRadioServiceBinder;
    private MediaControllerCompat mMediaControllerCompat;
    static boolean isMediaPlayingPodcast;
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

        boolean pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.screen_on_key), false);
        if (pref) {
            requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // Allow Glide to use more memory
        GlideApp.get(requireContext()).setMemoryCategory(MemoryCategory.HIGH);

        // Custom Colors
        MainActivity.updateColorsInterface.onUpdate(true);
        MainActivity.isZooColor = true;

        // Playback state interface
        mIPodcastService = this;

        //Acquire wake locks
        mWakeLock = ((PowerManager) requireContext().getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WARNING:ZooServiceWakelock");
        mWifiLock = ((WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "WARNING:ZooServiceWiFiWakelock");


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

        String url = "https://zoo.105.net";

        boolean hardwareAcceleration = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.hardware_acceleration_key), false);
        if (!hardwareAcceleration) {
            mWebView.setLayerType(WebView.LAYER_TYPE_NONE , null);
        } else {
            mWebView.setLayerType(WebView.LAYER_TYPE_HARDWARE , null);
        }
        mWebView.getSettings().setLoadsImagesAutomatically(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mWebView.addJavascriptInterface(new JSInterfaceZoo(),"JSZOOOUT");
        if (Constants.zooBundle == null) {
            mWebView.loadUrl(url);
        } else {
            mWebView.restoreState(Constants.zooBundle.getBundle(Constants.ZOO_STATE));
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
        if (mState ==  PlaybackStateCompat.STATE_STOPPED) {
            if (mWebView != null) {
                mWebView.onPause();
                mWebView.pauseTimers();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mState ==  PlaybackStateCompat.STATE_STOPPED) {
            if (mWebView != null) {
                mWebView.onResume();
                mWebView.resumeTimers();
            }
        }
    }

    @Override
    public void onDestroyView() {
        boolean pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.screen_on_key), false);
        if (pref) {
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
            podcastImageUrl = mString.replaceAll("(resizer/)[^&]*(/true)", "$1480/480$2");
            Timber.e("artUrl changed, new URL is %s", podcastImageUrl);
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

    final WebViewClient mWebViewClient = new WebViewClient() {

        @Override
        public boolean shouldOverrideUrlLoading (WebView webView, WebResourceRequest request) {
            if (Uri.parse(request.getUrl().toString()).getHost().contains("zoo.105.net")) {
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

        // Suppress lint warns as:
        // 1 - WebViewCompat cannot be used with AdBlockWebView and postVisualStateCallback is used only if user enables it
        // 2 - NewApi is a false positive, postCallbackKey is false and switch does not appear with API < M
        @SuppressLint({"WebViewApiAvailability", "NewApi"})
        @Override
        public void onPageFinished (WebView webView, String url) {
            SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
            boolean postCallbackKey = mSharedPreferences.getBoolean("post_callback_key", false);

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
                    " }; " + "})()";

            final String javaScript = "javascript:(function() { " +
                    "var audio = document.querySelector('audio'); " +
                    "if (document.body.contains(audio)) { audio.style.minWidth = '90%'; audio.style.margin= '0 auto'; audio.controlsList.remove('nodownload'); " +
                    "    audio.onplay = function() {" +
                    "        JSZOOOUT.mediaZooAction('true');" +
                    "    };" +
                    "    audio.onpause = function() {" +
                    "        JSZOOOUT.mediaZooAction('false');" +
                    "    };" +
                    "};" +
                    "var podcastText = document.getElementsByClassName('titolo_articolo titolo');" +
                    " if (podcastText.length) { var text = podcastText[0].textContent; " +
                    "JSZOOOUT.getPodcastTitle(text); " +
                    "var image = document.querySelector('[title=' + CSS.escape(text) + ']') ;" +
                    " if (document.body.contains(image)) { JSZOOOUT.getPodcastImage(image.src); };" +
                    "};" +
                    "var podcastSubText = document.getElementsByClassName('sottotitolo_articolo sottotitolo');" +
                    " if (podcastSubText.length) { var text = podcastSubText[0].textContent; " +
                    "JSZOOOUT.getPodcastSubtitle(text); };" +
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
                    " if (element.length) { element[0].style.display = 'none' }; " + "})()";

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

            webView.evaluateJavascript(javaScript, null);
            if (postCallbackKey) {
                webView.postVisualStateCallback(getId(), new WebView.VisualStateCallback() {
                    @Override
                    public void onComplete(long requestId) {
                        if (mProgressBar != null) {
                            mProgressBar.setVisibility(View.INVISIBLE);
                        }
                        webView.setVisibility(View.VISIBLE);
                    }
                });
            } else {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (mProgressBar != null) {
                        mProgressBar.setVisibility(View.INVISIBLE);
                    }
                    webView.setVisibility(View.VISIBLE);
                }, 200);
            }
            super.onPageFinished(webView, url);
        }

        @RequiresApi(Build.VERSION_CODES.M)
        @Override
        public void onReceivedError(WebView webView, WebResourceRequest request, WebResourceError error) {
            // Ignore some connection errors
            // ERR_FAILED = -1
            // ERR_ADDRESS_UNREACHABLE = -2
            // ERR_CONNECTION_REFUSED = -6
            switch (error.getErrorCode()) {
                case -1:
                case -2:
                case -6:
                    break;
                default:
                    webView.loadUrl(Constants.ErrorPagePath);
                    break;
            }
        }

        @Deprecated
        @Override
        public void onReceivedError(WebView webView, int errorCode, String description, String failingUrl) {
            webView.loadUrl(Constants.ErrorPagePath);
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view,
                                                          WebResourceRequest request) {

            try {
                String url = request.getUrl().toString();
                ZooFragment mZooFragment = ZooFragment.this;
                Bitmap bitmap;
                if (url.startsWith("https://adv.mediamond.it") || url.endsWith("mediaelement-and-player.min.js")) {
                    Timber.e("Intercepted javascript: %s", url);
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
        private ViewGroup mViewGroup;
        private WebChromeClient.CustomViewCallback mViewCallback;

        @Override
        public void onProgressChanged(final WebView view, final int newProgress) {
            if (mProgressBar != null) {
                mProgressBar.setProgress(newProgress);
            }
        }

        @Override
        public void onShowCustomView(View view, WebChromeClient.CustomViewCallback mCustomViewCallback) {
            if (fullScreenView != null) {
                mCustomViewCallback.onCustomViewHidden();
                return;
            }

            ViewGroup.LayoutParams layoutParams =
                    new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT);
            mViewGroup = (ViewGroup) root.getRootView();
            fullScreenView = view;

            mViewCallback = mCustomViewCallback;
            mViewGroup.addView(fullScreenView, layoutParams);
        }

        @Override
        public void onHideCustomView() {
            if (fullScreenView != null) {
                mViewGroup.removeView(fullScreenView);
                fullScreenView = null;
                mViewCallback.onCustomViewHidden();
            }
        }
    };
}
