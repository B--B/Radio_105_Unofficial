package com.bb.radio105;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import org.adblockplus.libadblockplus.android.webview.AdblockWebView;

public class PodcastFragment extends Fragment {

    AdblockWebView mWebView = null;
    private View root;

    @SuppressLint("SetJavaScriptEnabled")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_podcast, container, false);

        boolean pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.screen_on_key), false);
        if (pref) {
            requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // Stock Colors
        MainActivity.updateColorsInterface.onUpdate(false);

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
        String url = "https://www.105.net/sezioni/995/podcast";
        final String javaScript = "javascript:(function() { " +
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
                " if (element.length) { element[0].style.display = 'none' }; " + "})()";

        mWebView.getSettings().setLoadsImagesAutomatically(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mWebView.loadUrl(url);

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
                root.findViewById(R.id.loading_podcast).setVisibility(View.VISIBLE);
                super.onPageStarted(webView, url, mBitmap);
            }

            @Override
            public void onPageFinished (WebView webView, String url) {
                webView.loadUrl(javaScript);
                root.findViewById(R.id.loading_podcast).setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                super.onPageFinished(webView, url);
            }

            @RequiresApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView webView, WebResourceRequest request, WebResourceError error) {
                // Ignore some connection errors
                // ERR_FAILED = -1
                // ERR_CONNECTION_REFUSED = -6  --> needed for people with AD Blocker
                switch (error.getErrorCode()) {
                    case -1:
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

            /* @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view,
                                                              WebResourceRequest request) {
                Log.e(Constants.LOG_TAG, "in WebView client. isMainFrame:"+request.isForMainFrame() +": " + request.getUrl());
                return super.shouldInterceptRequest(view, request);
            } */
        });

        mWebView.setWebChromeClient(new WebChromeClient() {

            private View fullScreenView;
            private ViewGroup mViewGroup;
            private WebChromeClient.CustomViewCallback mViewCallback;

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

        mWebView.setDownloadListener((url1, userAgent, contentDisposition, mimetype, contentLength) -> {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Utils.requestStoragePermission(requireActivity(), root);
            }
            Utils.startDownload(requireActivity(), url1);
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        boolean pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.screen_on_key), false);
        if (pref) {
            requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        mWebView.destroy();
        root = null;
        super.onDestroyView();
    }
}
