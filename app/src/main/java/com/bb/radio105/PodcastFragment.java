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

import static com.bb.radio105.Constants.ACTION_PAUSE_PODCAST;
import static com.bb.radio105.Constants.ACTION_PLAY_PODCAST;
import static com.bb.radio105.Constants.ACTION_STOP_PODCAST;
import static com.bb.radio105.PodcastService.mState;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PictureInPictureParams;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
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

public class PodcastFragment extends Fragment implements IPodcastService  {

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
    private OnBackPressedCallback mOnBackPressedCallback;

    @SuppressLint("SetJavaScriptEnabled")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_podcast, container, false);

        boolean screenOn = Utils.getUserPreferenceBoolean(requireContext(), getString(R.string.screen_on_key), false);
        boolean theme = Utils.getUserPreferenceBoolean(requireContext(), getString(R.string.webviews_themes_key), true);

        if (screenOn) {
            requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        if (!theme) {
            root.setBackgroundColor(Color.WHITE);
        }


        // Allow Glide to use more memory
        GlideApp.get(requireContext()).setMemoryCategory(MemoryCategory.HIGH);

        // Playback state interface
        mIPodcastService = this;

        mOnBackPressedCallback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                if (mWebView.canGoBack()) {
                    mWebView.goBack();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(mOnBackPressedCallback);

        mWebView = root.findViewById(R.id.webView_podcast);
        mProgressBar = root.findViewById(R.id.loading_podcast);

        final String[] hardwareAcceleration = getResources().getStringArray(R.array.hardware_acceleration_values);
        // The apps theme is decided depending upon the saved preferences on app startup
        String hardwareAccelerationPref = Utils.getUserPreferenceString(requireContext(), getString(R.string.hardware_acceleration_key), getString(R.string.hardware_acceleration_default_value));
        if (hardwareAccelerationPref.equals(hardwareAcceleration[0]))
            mWebView.setLayerType(View.LAYER_TYPE_NONE , null);
        if (hardwareAccelerationPref.equals(hardwareAcceleration[1]))
            mWebView.setLayerType(View.LAYER_TYPE_HARDWARE , null);
        if (hardwareAccelerationPref.equals(hardwareAcceleration[2]))
            mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE , null);

        String url = "https://www.105.net/sezioni/648/programmi";

        mWebView.getSettings().setLoadsImagesAutomatically(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mWebView.getSettings().setOffscreenPreRaster(true);
        }
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mWebView.addJavascriptInterface(new JSInterfacePodcast(),"JSPODCASTOUT");
        if (Constants.podcastBundle == null) {
            mWebView.loadUrl(url);
        } else {
            mWebView.restoreState(Objects.requireNonNull(Constants.podcastBundle.getBundle(Constants.PODCAST_STATE)));
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
        if (Constants.podcastBundle == null) {
            Timber.d("onStop: created new outState bundle!");
            Constants.podcastBundle = new Bundle(ClassLoader.getSystemClassLoader());
        }
        final Bundle currentWebViewState = new Bundle(ClassLoader.getSystemClassLoader());
        if (mWebView.saveState(currentWebViewState) == null) {
            Timber.e("onStop: failed to obtain WebView state to save!");
        }
        Constants.podcastBundle.putBundle(Constants.PODCAST_STATE, currentWebViewState);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mState == PlaybackStateCompat.STATE_STOPPED && !isVideoInFullscreen) {
            if (mWebView != null) {
                mWebView.onPause();
                mWebView.pauseTimers();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mState == PlaybackStateCompat.STATE_STOPPED && !isVideoInFullscreen) {
            if (mWebView != null) {
                mWebView.onResume();
                mWebView.resumeTimers();
            }
        }
    }

    @Override
    public void onDestroyView() {
        boolean pref = Utils.getUserPreferenceBoolean(requireContext(), getString(R.string.screen_on_key), false);
        if (pref) {
            requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        if (mState != PlaybackStateCompat.STATE_STOPPED) {
            Timber.d("Stopping Podcast Service");
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
        mOnBackPressedCallback.remove();
        mOnBackPressedCallback = null;
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
            Timber.i("Connection successful");
            mRadioServiceBinder = (RadioServiceBinder) service;
            mMediaControllerCompat = new MediaControllerCompat(getContext(), mRadioServiceBinder.getMediaSessionToken());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Timber.e("Service crashed");
        }
    };

    class JSInterfacePodcast {
        @JavascriptInterface
        public void mediaPodcastAction(String mString) {
            Timber.i("isMediaPlayingPodcast is %s", mString);
            isMediaPlayingPodcast = Boolean.parseBoolean(mString);
            if (isMediaPlayingPodcast) {
                if (RadioService.mState != PlaybackStateCompat.STATE_STOPPED) {
                    mMediaControllerCompat.getTransportControls().stop();
                    requireContext().unbindService(mServiceConnection);
                }
                if (mState != PlaybackStateCompat.STATE_PLAYING) {
                    Timber.i("Received play request from PodcastService");
                    playPodcast();
                }
            } else {
                if (mState == PlaybackStateCompat.STATE_PLAYING) {
                    Timber.i("Received pause request from PodcastFragment");
                    pausePodcast();
                }
            }
        }

        @JavascriptInterface
        public void getPodcastTitle(String mString) {
            Timber.i("Podcast title is %s", mString);
            podcastTitle = mString;
        }

        @JavascriptInterface
        public void getPodcastSubtitle(String mString) {
            Timber.i("Podcast subtitle is %s", mString);
            podcastSubtitle = mString;
        }

        @JavascriptInterface
        public void getPodcastImage(String mString) {
            // 105 site use an online resizer for dynamically provide an artwork in the correct size. Unfortunately, the
            // artwork fetched have a poor quality. All artworks links have a fixed part "resizer/WIDTH/HEIGHT/true", here
            // the original link sizes will be changed to 480x480, for an higher quality image. If for some reason the
            // replace won't work the original string will be used.
            podcastImageUrl = mString.replaceAll("(resizer/)[^&]*(/true)", "$1480/480$2");
            Timber.i("artUrl changed, new URL is %s", podcastImageUrl);
        }

        @JavascriptInterface
        public void getVideoFullscreenState(String mString) {
            Timber.i("isVideoInFullscreen is %s", mString);
            isVideoInFullscreen = Boolean.parseBoolean(mString);
        }
    }

    @Override
    public void playbackState(String playbackState) {
        Timber.i("Playback state changed, new state is %s", playbackState);
        if (playbackState.equals("Play")) {
            mWebView.evaluateJavascript("javascript:(player.play());", null);
            // FIXME: this shit is necessary as 105.net closes the connection if the audio is paused for a few minutes.
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isMediaPlayingPodcast) {
                    Timber.d("!HACK! Trying to start the stream again");
                    mWebView.evaluateJavascript("javascript:(function() { player.pause(); player.play();})()", null);
                }
            }, 100);
        } else {
            mWebView.evaluateJavascript("javascript:(player.pause());", null);
        }
    }

    private void playPodcast() {
        Intent mIntent = new Intent();
        mIntent.setAction(ACTION_PLAY_PODCAST);
        mIntent.setPackage(requireContext().getPackageName());
        requireContext().startService(mIntent);
    }

    private void pausePodcast() {
        Intent mIntent = new Intent();
        mIntent.setAction(ACTION_PAUSE_PODCAST);
        mIntent.setPackage(requireContext().getPackageName());
        requireContext().startService(mIntent);
    }

    private void stopPodcast() {
        Intent mIntent = new Intent();
        mIntent.setAction(ACTION_STOP_PODCAST);
        mIntent.setPackage(requireContext().getPackageName());
        requireContext().startService(mIntent);
    }

    final  WebViewClient mWebViewClient = new WebViewClientCompat() {

        @Override
        public boolean shouldOverrideUrlLoading (@NonNull WebView webView, WebResourceRequest request) {
            if (Objects.requireNonNull(Uri.parse(request.getUrl().toString()).getHost()).contains("zoo.105.net")) {
                Navigation.findNavController(requireView()).navigate(R.id.nav_zoo);
                return false;
            }
            if (Objects.requireNonNull(Uri.parse(request.getUrl().toString()).getHost()).contains("www.105.net")) {
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
            if (mState != PlaybackStateCompat.STATE_STOPPED) {
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
                    "            JSPODCASTOUT.mediaPodcastAction('true');" +
                    "        };" +
                    "        audio.onpause = function() {" +
                    "            JSPODCASTOUT.mediaPodcastAction('false');" +
                    "        };" +
                    "    }" +
                    "    var podcastText = document.getElementsByClassName('occhiello_articolo');" +
                    "    if (podcastText.length) {" +
                    "        var text = podcastText[0].textContent;" +
                    "        JSPODCASTOUT.getPodcastTitle(text);" +
                    "    }" +
                    "    var podcastSubText = document.getElementsByClassName('titolo_articolo titolo');" +
                    "    if (podcastSubText.length) {" +
                    "        var text = podcastSubText[0].textContent;" +
                    "        JSPODCASTOUT.getPodcastSubtitle(text);" +
                    "        var image = document.querySelector('[alt=' + '' + text + '' + ']') ;" +
                    "        if (document.body.contains(image)) {" +
                    "            JSPODCASTOUT.getPodcastImage(image.src);" +
                    "        }" +
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
                    "            JSPODCASTOUT.mediaPodcastAction('true');" +
                    "        };" +
                    "        audio.onpause = function() {" +
                    "            JSPODCASTOUT.mediaPodcastAction('false');" +
                    "        };" +
                    "    }" +
                    "    var podcastText = document.getElementsByClassName('occhiello_articolo');" +
                    "    if (podcastText.length) {" +
                    "        var text = podcastText[0].textContent;" +
                    "        JSPODCASTOUT.getPodcastTitle(text);" +
                    "    }" +
                    "    var podcastSubText = document.getElementsByClassName('titolo_articolo titolo');" +
                    "    if (podcastSubText.length) {" +
                    "        var text = podcastSubText[0].textContent;" +
                    "        JSPODCASTOUT.getPodcastSubtitle(text);" +
                    "        var image = document.querySelector('[alt=' + '' + text + '' + ']') ;" +
                    "        if (document.body.contains(image)) {" +
                    "            JSPODCASTOUT.getPodcastImage(image.src);" +
                    "        }" +
                    "    }" +
                    "    console.log('podcastService javascript executed');" +
                    "})()";

            final String pipModeEnabled = "javascript:(function() { " +
                    "    function onFullScreen(e) {" +
                    "        var isFullscreenNow = document.webkitFullscreenElement !== null;" +
                    "        JSPODCASTOUT.getVideoFullscreenState(isFullscreenNow);" +
                    "    }" +
                    "    document.addEventListener('webkitfullscreenchange', onFullScreen);" +
                    "    document.addEventListener('fullscreenchange', onFullScreen);" +
                    "    console.log('pipModeEnabled javascript executed');" +
                    "})()";

            final String javaScript = "javascript:(function() { " +
                    "document.body.style.backgroundColor = 'transparent';" +
                    "var element = document.getElementsByClassName('container vc_bg_white');" +
                    " if (element.length) { " +
                    "    for (var i = 0; i < element.length; i++) { " +
                    "        element[i].style.backgroundColor = 'transparent'; " +
                    "    }" +
                    "}; " +
                    "var home = document.createElement('IMG'); " +
                    "home.src = 'images/logos/3/logo_colored.jpg?v=1500902417000'; " +
                    "home.style.marginLeft = '20px';" +
                    "home.style.position = 'absolute';" +
                    "home.style.top = '15px';" +
                    "home.style.width = '40px';" +
                    "home.addEventListener('click', function() { " +
                    "location.href = 'https://www.105.net/sezioni/648/programmi'; });" +
                    "document.body.appendChild(home); " +
                    "var element = document.getElementsByClassName('breadcrumbs_orizzontale vc_breadcrumbs null');" +
                    " if (element.length) { element[0].innerHTML = '' }; " +
                    "var x = window.matchMedia('(max-width: 767px)');" +
                    "if (x.matches) {" +
                    "var element = document.getElementsByClassName('breadcrumbs_orizzontale vc_breadcrumbs null');" +
                    " if (element.length) { element[0].style.display = 'hide' }; } else {" +
                    "var element = document.getElementsByClassName('breadcrumbs_orizzontale vc_breadcrumbs null');" +
                    " if (element.length) { element[0].style.display = 'show' }; } " +
                    "var element = document.getElementsByClassName('rti-privacy-visible');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('bannervcms banner_728x90_leaderboard ');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('container-fluid skin-visible vc_bg_yellow vc_bb6_darkgrey vc_z50 hidden-sm hidden-xs affix-top');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('container-fluid skin-visible vc_bg_yellow vc_z50 hidden-sm hidden-xs');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('player-container vc_mediaelementjs');" +
                    " if (element.length) { element[0].style.width = '100%' }; " +
                    "var element = document.getElementsByClassName('clear');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('navbar-fixed-top hidden-print');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('vc_share_buttons_horizontal');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('spacer t_40');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('col-xs-12');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('text_edit vc_textedit_title_refine_results null');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('vc_search_refine_results_standard');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('social social_buttons');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('vc_cont_article');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('bannervcms banner_masthead_2_970x250 ');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('anteprima_slider vc_preview_slider_dj ghost_container vc_txt_m variant vc_theme_light vc_br_100  null ');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('container-fluid vc_bg_darkgray vc_bt7_yellow vc_z2 vc_hidden_print');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('iubenda-cs-container');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('container-fluid skin-visible vc_bg_yellow vc_bb6_darkgrey vc_z50 hidden-sm hidden-xs affix');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('vc_hidden_print vc_cont_menu_bar');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "var element = document.getElementsByClassName('container-fluid vc_bg_grad_green-blu-tone ghost_container');" +
                    " if (element.length) { element[0].style.display = 'none' }; " +
                    "    console.log('Common javascript executed');" +
                    "})()";

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
                    final int mPostVisualStateCallbackId = 466;
                    WebViewCompat.postVisualStateCallback(webView, mPostVisualStateCallbackId, requestId -> {
                        if (requestId == mPostVisualStateCallbackId) {
                            if (mProgressBar != null) {
                                mProgressBar.setVisibility(View.INVISIBLE);
                            }
                            webView.setVisibility(View.VISIBLE);
                        }
                    });
                } else {
                    Timber.d("postVisualStateCallback not supported, fallback to handler method");
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
                switch (errorCode) {
                    case WebViewClient.ERROR_AUTHENTICATION:
                    case WebViewClient.ERROR_BAD_URL:
                    case WebViewClient.ERROR_FAILED_SSL_HANDSHAKE:
                    case WebViewClient.ERROR_FILE:
                    case WebViewClient.ERROR_FILE_NOT_FOUND:
                    case WebViewClient.ERROR_IO:
                    case WebViewClient.ERROR_PROXY_AUTHENTICATION:
                    case WebViewClient.ERROR_REDIRECT_LOOP:
                    case WebViewClient.ERROR_TIMEOUT:
                    case WebViewClient.ERROR_TOO_MANY_REQUESTS:
                    case WebViewClient.ERROR_UNSAFE_RESOURCE:
                    case WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME:
                    case WebViewClient.ERROR_UNSUPPORTED_SCHEME:
                        webView.loadUrl(Constants.ErrorPagePath);
                        break;
                    case WebViewClient.ERROR_CONNECT:
                    case WebViewClient.ERROR_HOST_LOOKUP:
                    case WebViewClient.ERROR_UNKNOWN:
                        break;
                }
            }
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view,
                                                          WebResourceRequest request) {

            try {
                String url = request.getUrl().toString();
                PodcastFragment mPodcastFragment = PodcastFragment.this;
                Bitmap bitmap;
                if (url.startsWith("https://adv.mediamond.it") || url.startsWith("https://tags.tiqcdn.com/") ||
                        url.endsWith("mediaelement-and-player.min.js") || url.endsWith("cookiecuttr.js") ||
                        url.endsWith("cookie_law.jsp") || url.endsWith("webtrekk_v3.min.js") ||
                        url.endsWith("analytics.js")) {
                    Timber.d("Javascript intercepted: %s", url);
                    return new WebResourceResponse("text/javascript", "UTF-8", new ByteArrayInputStream("// Script Blocked".getBytes(StandardCharsets.UTF_8)));
                } else if (url.toLowerCase(Locale.ROOT).endsWith(".jpg") || url.toLowerCase(Locale.ROOT).endsWith(".jpeg")) {
                    bitmap = GlideApp.with(view).asBitmap().diskCacheStrategy(DiskCacheStrategy.ALL).load(url).submit().get();
                    return new WebResourceResponse("image/jpg", "UTF-8", mPodcastFragment.getBitmapInputStream(bitmap, Bitmap.CompressFormat.JPEG));
                } else if (url.toLowerCase(Locale.ROOT).endsWith(".png")) {
                    bitmap = GlideApp.with(view).asBitmap().diskCacheStrategy(DiskCacheStrategy.ALL).load(url).submit().get();
                    return new WebResourceResponse("image/png", "UTF-8", mPodcastFragment.getBitmapInputStream(bitmap, Bitmap.CompressFormat.PNG));
                } else if (url.toLowerCase(Locale.ROOT).endsWith(".webp")) {
                    bitmap = GlideApp.with(view).asBitmap().diskCacheStrategy(DiskCacheStrategy.ALL).load(url).submit().get();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        return new WebResourceResponse("image/webp", "UTF-8", mPodcastFragment.getBitmapInputStream(bitmap, Bitmap.CompressFormat.WEBP_LOSSY));
                    } else {
                        //noinspection deprecation
                        return new WebResourceResponse("image/webp", "UTF-8", mPodcastFragment.getBitmapInputStream(bitmap, Bitmap.CompressFormat.WEBP));
                    }
                } else {
                    return super.shouldInterceptRequest(view, request);
                }
            } catch (ExecutionException | InterruptedException e) {
                Timber.e("Image load failed with error %s", e);
                return super.shouldInterceptRequest(view, request);
            }
        }

        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            // Disable the on-back press callback if there are no more questions in the
            // WebView to go back to, allowing us to exit the WebView and go back to
            // the fragment.
            mOnBackPressedCallback.setEnabled(view.canGoBack());
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
            ((FrameLayout) requireActivity().getWindow().getDecorView()).addView(fullScreenView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            Utils.setUpFullScreen(requireActivity(), requireActivity().getContentResolver());
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
            ((FrameLayout) requireActivity().getWindow().getDecorView()).removeView(fullScreenView);
            fullScreenView = null;
            Utils.restoreScreen(requireActivity(), requireActivity().getContentResolver());
            mViewCallback.onCustomViewHidden();
            mViewCallback = null;
        }
    };
}
