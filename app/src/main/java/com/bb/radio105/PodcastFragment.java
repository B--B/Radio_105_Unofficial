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

import static com.bb.radio105.PodcastService.State.Paused;
import static com.bb.radio105.PodcastService.State.Playing;
import static com.bb.radio105.PodcastService.State.Stopped;
import static com.bb.radio105.PodcastService.mState;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.MemoryCategory;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.adblockplus.libadblockplus.android.settings.AdblockHelper;
import org.adblockplus.libadblockplus.android.webview.AdblockWebView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import timber.log.Timber;

public class PodcastFragment extends Fragment implements IPodcastService {

    private AdblockWebView mWebView;
    private View root;
    private ProgressBar mProgressBar;
    private MusicServiceBinder mMusicServiceBinder;
    private MediaControllerCompat mMediaControllerCompat;
    private Intent startPodcastService;
    static boolean isMediaPlayingPodcast;
    static IPodcastService mIPodcastService;
    static String podcastTitle;
    static String podcastSubtitle;
    static String podcastImageUrl;

    @SuppressLint("SetJavaScriptEnabled")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_podcast, container, false);

        boolean pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.screen_on_key), false);
        if (pref) {
            requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // Allow Glide to use more memory
        Glide.get(requireContext()).setMemoryCategory(MemoryCategory.HIGH);

        // Stock Colors
        MainActivity.updateColorsInterface.onUpdate(false);
        // Playback state interface
        mIPodcastService = this;

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

        mWebView = root.findViewById(R.id.webView_podcast);
        String url = "https://www.105.net/sezioni/648/programmi";
        String javaScript = "javascript:(function() { " +
                "var home = document.createElement('IMG'); " +
                "home.src = 'images/logos/3/logo_colored.jpg?v=1500902417000'; " +
                "home.style.marginLeft = '5%';" +
                "home.style.position = 'absolute';" +
                "home.style.top = '0.5%';" +
                "home.style.height = '8%';" +
                "home.addEventListener('click', function() { " +
                "location.href = 'https://www.105.net/sezioni/648/programmi'; });" +
                "document.body.appendChild(home); " +
                "var audio = document.querySelector('audio'); " +
                "if (document.body.contains(audio)) { audio.style.minWidth = '90%'; audio.style.margin= '0 auto'; audio.controlsList.remove('nodownload');" +
                "    audio.onplay = function() {" +
                "        JSPODCASTOUT.mediaPodcastAction('true');" +
                "    };" +
                "    audio.onpause = function() {" +
                "        JSPODCASTOUT.mediaPodcastAction('false');" +
                "    };" +
                "};" +
                "var podcastText = document.getElementsByClassName('occhiello_articolo');" +
                " if (podcastText.length) { var text = podcastText[0].textContent; " +
                "JSPODCASTOUT.getPodcastTitle(text); };" +
                "var podcastSubText = document.getElementsByClassName('titolo_articolo titolo');" +
                " if (podcastSubText.length) { var text = podcastSubText[0].textContent; " +
                "JSPODCASTOUT.getPodcastSubtitle(text); " +
                "var image = document.querySelector('[title=' + CSS.escape(text) + ']') ;" +
                " if (document.body.contains(image)) { JSPODCASTOUT.getPodcastImage(image.src); };" +
                "};" +
                "var element = document.getElementsByClassName('player-container vc_mediaelementjs');" +
                " if (element.length) { element[0].style.width = '100%' }; " +
                "var element = document.getElementsByClassName('clear');" +
                " if (element.length) { element[0].style.display = 'none' }; " +
                "var element = document.getElementsByClassName('navbar-fixed-top hidden-print');" +
                " if (element.length) { element[0].style.display = 'none' }; " +
                "var element = document.getElementsByClassName('container vc_bg_white');" +
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
                "var element = document.getElementsByClassName('container-fluid vc_bg_grad_green-blu-tone ghost_container');" +
                " if (element.length) { element[0].style.display = 'none' }; " + "})()";

        mWebView.getSettings().setLoadsImagesAutomatically(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mWebView.setProvider(AdblockHelper.get().getProvider());
        mWebView.setSiteKeysConfiguration(AdblockHelper.get().getSiteKeysConfiguration());
        mWebView.addJavascriptInterface(new JSInterfacePodcast(),"JSPODCASTOUT");
        if (Constants.podcastBundle == null) {
            mWebView.loadUrl(url);
        } else {
            mWebView.restoreState(Constants.podcastBundle.getBundle(Constants.PODCAST_STATE));
        }

        mProgressBar = root.findViewById(R.id.loading_podcast);

        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading (WebView webView, WebResourceRequest request) {
                if (Uri.parse(request.getUrl().toString()).getHost().contains("zoo.105.net")) {
                    Navigation.findNavController(requireView()).navigate(R.id.nav_zoo);
                    return false;
                }
                if (Uri.parse(request.getUrl().toString()).getHost().contains("www.105.net")) {
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
                webView.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.VISIBLE);
                if (mState != Stopped) {
                    stopPodcast();
                    podcastTitle = null;
                    podcastSubtitle = null;
                    podcastImageUrl = null;
                }
                super.onPageStarted(webView, url, mBitmap);
            }

            @Override
            public void onPageFinished (WebView webView, String url) {
                webView.loadUrl(javaScript);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (mProgressBar != null) {
                        mProgressBar.setVisibility(View.GONE);
                    }
                    webView.setVisibility(View.VISIBLE);
                }, 200);
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
                    PodcastFragment mPodcastFragment = PodcastFragment.this;
                    Bitmap bitmap;
                    if (url.endsWith("mediaelement-and-player.min.js")) {
                        return new WebResourceResponse("text/javascript", "UTF-8", new ByteArrayInputStream("// Script Blocked".getBytes(StandardCharsets.UTF_8)));
                    } else if (url.toLowerCase(Locale.ROOT).endsWith(".jpg") || url.toLowerCase(Locale.ROOT).endsWith(".jpeg")) {
                        bitmap = Glide.with(view).asBitmap().diskCacheStrategy(DiskCacheStrategy.ALL).load(url).submit().get();
                        return new WebResourceResponse("image/jpg", "UTF-8", mPodcastFragment.getBitmapInputStream(bitmap, Bitmap.CompressFormat.JPEG));
                    } else if (url.toLowerCase(Locale.ROOT).endsWith(".png")) {
                        bitmap = Glide.with(view).asBitmap().diskCacheStrategy(DiskCacheStrategy.ALL).load(url).submit().get();
                        return new WebResourceResponse("image/png", "UTF-8", mPodcastFragment.getBitmapInputStream(bitmap, Bitmap.CompressFormat.PNG));
                    } else if (url.toLowerCase(Locale.ROOT).endsWith(".webp")) {
                        bitmap = Glide.with(view).asBitmap().diskCacheStrategy(DiskCacheStrategy.ALL).load(url).submit().get();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            return new WebResourceResponse("image/webp", "UTF-8", mPodcastFragment.getBitmapInputStream(bitmap, Bitmap.CompressFormat.WEBP_LOSSY));
                        } else {
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
        });

        mWebView.setWebChromeClient(new WebChromeClient() {

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
        });

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Bind music service
        requireContext().bindService(new Intent(getContext(), MusicService.class), mServiceConnection, 0);
        // Start podcast service
        startPodcastService = new Intent(getContext(), PodcastService.class);
        startPodcastService.setAction("com.bb.radio105.action.START_PODCAST");
        requireContext().startService(startPodcastService);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Unbind music service
        requireContext().unbindService(mServiceConnection);
        if (mMediaControllerCompat != null) {
            mMediaControllerCompat = null;
        }
        if (mState == Stopped) {
            requireContext().stopService(startPodcastService);
        }
        if (Constants.podcastBundle == null) {
            Timber.d("onStop: created new outState bundle!");
            Constants.podcastBundle = new Bundle(ClassLoader.getSystemClassLoader());
        }
        final Bundle currentWebViewState = new Bundle(ClassLoader.getSystemClassLoader());
        if (mWebView.saveState(currentWebViewState) == null) {
            Timber.d("onStop: failed to obtain WebView state to save!");
        }
        Constants.podcastBundle.putBundle(Constants.PODCAST_STATE, currentWebViewState);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mState != Playing) {
            if (mWebView != null) {
                mWebView.onPause();
                mWebView.pauseTimers();
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onResume() {
        super.onResume();
        if (mState != Playing) {
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
        if (mState != Stopped) {
            Timber.e("Stopping Podcast Service");
            isMediaPlayingPodcast = false;
            stopPodcast();
            podcastTitle = null;
            podcastSubtitle = null;
            podcastImageUrl = null;
            requireContext().stopService(startPodcastService);
        }
        mIPodcastService = null;
        mMusicServiceBinder = null;
        if (mMediaControllerCompat != null) {
            mMediaControllerCompat = null;
        }
        if (mWebView != null) {
            mWebView.removeAllViews();
            mWebView.dispose(null);
            mWebView.destroy();
        }
        mProgressBar = null;
        root = null;
        // Restore Glide memory values
        Glide.get(requireContext()).setMemoryCategory(MemoryCategory.NORMAL);
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
            mMusicServiceBinder = (MusicServiceBinder) service;
            mMediaControllerCompat = new MediaControllerCompat(getContext(), mMusicServiceBinder.getMediaSessionToken());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Timber.e("Service crashed");
        }
    };

    class JSInterfacePodcast {
        @JavascriptInterface
        public void mediaPodcastAction(String mString) {
            Timber.e("isMediaPlayingPodcast is %s", mString);
            isMediaPlayingPodcast = Boolean.parseBoolean(mString);
            if (isMediaPlayingPodcast) {
                if (mMusicServiceBinder.getPlaybackState() == PlaybackStateCompat.STATE_PLAYING) {
                    mMediaControllerCompat.getTransportControls().pause();
                }
                if (mState == Stopped || mState == Paused) {
                    Timber.e("Received play request from PodcastService");
                    playPodcast();
                }
            } else {
                if (mState == Playing) {
                    Timber.e("Received pause request from PodcastFragment");
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

    @Override
    public void playbackState(String playbackState) {
        if (playbackState.equals("Play")) {
            Utils.callJavaScript(mWebView, "player.play");
        } else {
            Utils.callJavaScript(mWebView, "player.pause");
        }
    }

    @Override
    public void duckRequest(Boolean mustDuck) {
        if (mustDuck) {
            mWebView.evaluateJavascript("javascript:(function() { " +
                    "var audio = document.querySelector('audio'); " +
                    "if (document.body.contains(audio)) { " +
                    "    audio.volume = 0.2;};" +
                    "})()", null);
        } else {
            mWebView.evaluateJavascript("javascript:(function() { " +
                    "var audio = document.querySelector('audio'); " +
                    "if (document.body.contains(audio)) { " +
                    "    audio.volume = 1.0;};" +
                    "})()", null);
        }
    }

    private void playPodcast() {
        Intent mIntent = new Intent();
        mIntent.setAction("com.bb.radio105.action.PLAY_PODCAST");
        mIntent.setPackage(requireContext().getPackageName());
        requireContext().startService(mIntent);
    }

    private void pausePodcast() {
        Intent mIntent = new Intent();
        mIntent.setAction("com.bb.radio105.action.PAUSE_PODCAST");
        mIntent.setPackage(requireContext().getPackageName());
        requireContext().startService(mIntent);
    }

    private void stopPodcast() {
        Intent mIntent = new Intent();
        mIntent.setAction("com.bb.radio105.action.STOP_PODCAST");
        mIntent.setPackage(requireContext().getPackageName());
        requireContext().startService(mIntent);
    }
}
