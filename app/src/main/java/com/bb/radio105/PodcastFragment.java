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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
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
import android.view.ViewParent;
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
import androidx.core.app.ActivityCompat;
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

public class PodcastFragment extends Fragment {

    static AdblockWebView mWebView = null;
    private View root;
    private ProgressBar mProgressBar;
    private IMusicService mMusicServiceBinder;
    private MediaControllerCompat mMediaControllerCompat;
    private Intent startPodcastService;
    static boolean isMediaPlayingPodcast;

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

        // WebView and Chrome clients
        PodcastWebViewClient mPodcastWebViewClient = new PodcastWebViewClient();
        PodcastWebChromeClient mPodcastWebChromeClient = new PodcastWebChromeClient();

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
        mProgressBar = root.findViewById(R.id.loading_podcast);
        String url = "https://www.105.net/sezioni/648/programmi";

        mWebView.getSettings().setLoadsImagesAutomatically(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setDatabaseEnabled(true);
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mWebView.setProvider(AdblockHelper.get().getProvider());
        mWebView.setSiteKeysConfiguration(AdblockHelper.get().getSiteKeysConfiguration());
        mWebView.setWebViewClient(mPodcastWebViewClient);
        mWebView.setWebChromeClient(mPodcastWebChromeClient);
        mWebView.addJavascriptInterface(new JSInterfacePodcast(),"JSPODCASTOUT");
        if (Constants.podcastBundle == null) {
            mWebView.loadUrl(url);
        } else {
            mWebView.restoreState(Constants.podcastBundle.getBundle(Constants.PODCAST_STATE));
        }

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
        // Bind music service
        requireContext().bindService(new Intent(getContext(), MusicService.class), mServiceConnection, 0);
        // Start podcast service
        startPodcastService = new Intent(getContext(), PodcastService.class);
        startPodcastService.setAction("com.bb.radio105.action.START");
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
        if (Constants.podcastBundle == null) {
            Timber.d("onStop: created new outState bundle!");
            Constants.podcastBundle = new Bundle(ClassLoader.getSystemClassLoader());
        }
        final Bundle currentWebViewState = new Bundle(ClassLoader.getSystemClassLoader());
        if (mWebView.saveState(currentWebViewState) == null) {
            Timber.d("onStop: failed to obtain WebView state to save!");
        }
        Constants.podcastBundle.putBundle(Constants.PODCAST_STATE, currentWebViewState);
        if (mState == Stopped) {
            requireContext().stopService(startPodcastService);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mState != Playing) {
            if (mWebView != null) {
                mWebView.getSettings().setJavaScriptEnabled(false);
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
                mWebView.getSettings().setJavaScriptEnabled(true);
                mWebView.onResume();
                mWebView.resumeTimers();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        boolean pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.screen_on_key), false);
        if (pref) {
            requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        if (mState != Stopped) {
            Timber.e("Stopping Podcast Service");
            mState = Stopped;
            requireContext().stopService(startPodcastService);
        }
        // Restore Glide memory values
        Glide.get(requireContext()).setMemoryCategory(MemoryCategory.NORMAL);

        mMusicServiceBinder = null;
        if (mMediaControllerCompat != null) {
            mMediaControllerCompat = null;
        }
        mProgressBar = null;
        AdblockHelper.get().getProvider().release();
        ViewParent parent = mWebView.getParent();
        if (parent != null) {
            ((ViewGroup) parent).removeView(mWebView);
        }
        mWebView.stopLoading();
        mWebView.clearHistory();
        mWebView.removeJavascriptInterface("JSPODCASTOUT");
        mWebView.loadUrl("about:blank");
        mWebView.getSettings().setJavaScriptEnabled(false);
        mWebView.removeAllViews();
        mWebView.destroy();
        mWebView = null;
        root = null;
    }

    private InputStream getBitmapInputStream(Bitmap bitmap, Bitmap.CompressFormat compressFormat) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(compressFormat, 80, byteArrayOutputStream);
        byte[] mByte = byteArrayOutputStream.toByteArray();
        return new ByteArrayInputStream(mByte);
    }

    private final class PodcastWebChromeClient extends WebChromeClient {
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
    }

    private final class PodcastWebViewClient extends WebViewClient {
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
            mProgressBar.setVisibility(View.VISIBLE);
            webView.setVisibility(View.GONE);
            if (mState != Stopped) {
                stopPodcast();
            }
            super.onPageStarted(webView, url, mBitmap);
        }

        @Override
        public void onPageFinished (WebView webView, String url) {
            String javaScript = "javascript:(function() { " +
                    "var audio = document.querySelector('audio'); " +
                    "if (document.body.contains(audio)) { audio.style.minWidth = '90%'; audio.style.margin= '0 auto'; audio.controlsList.remove('nodownload');" +
                    "    audio.onplay = function() {" +
                    "        JSPODCASTOUT.mediaPodcastAction('true');" +
                    "    };" +
                    "    audio.onpause = function() {" +
                    "        JSPODCASTOUT.mediaPodcastAction('false');" +
                    "    };" +
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
                e.printStackTrace();
            }
            return super.shouldInterceptRequest(view, request);
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Timber.e("Connection successful");
            mMusicServiceBinder = (IMusicService) service;
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
                    Timber.e("Received play request for PodcastService");
                    playPodcast();
                }
            } else {
                if (mState == Playing) {
                    pausePodcast();
                }
            }
        }
    }

    private void playPodcast() {
        Intent mIntent = new Intent();
        mIntent.setAction("com.bb.radio105.action.PLAY");
        mIntent.setPackage(requireContext().getPackageName());
        requireContext().startService(mIntent);
    }

    private void pausePodcast() {
        Intent mIntent = new Intent();
        mIntent.setAction("com.bb.radio105.action.PAUSE");
        mIntent.setPackage(requireContext().getPackageName());
        requireContext().startService(mIntent);
    }

    private void stopPodcast() {
        Intent mIntent = new Intent();
        mIntent.setAction("com.bb.radio105.action.STOP");
        mIntent.setPackage(requireContext().getPackageName());
        requireContext().startService(mIntent);
    }
}
