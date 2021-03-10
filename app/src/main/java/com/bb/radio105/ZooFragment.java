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

public class ZooFragment extends Fragment {

    AdblockWebView mWebView = null;
    private View root;

    @SuppressLint("SetJavaScriptEnabled")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_zoo, container, false);

        boolean pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.screen_on_key), false);
        if (pref) {
            requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // Custom Colors
        MainActivity.updateColorsInterface.onUpdate(true);

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
        String url = "https://zoo.105.net";
        // TODO: Even if it's working this mess must be absolutely cleaned now that ads are gone
        final String javaScript = "javascript:(function() { " +
                "var element = document.getElementsByClassName('container');" +
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
                "var element = document.getElementById('adv-gpt-masthead-leaderboard-container1');" +
                " if (element.length) { element[0].style.display = 'none' }; " +
                "var element = document.getElementById('google_ads_iframe_/4758/altri_radiomediaset_radio105/altre_1__container__');" +
                " if (element.length) { element[0].style.display = 'none' }; " +
                "var element = document.getElementById('div-gpt-320x50');" +
                " if (element.length) { element[0].style.display = 'none' }; " +
                "var element = document.getElementById('google_ads_iframe_/4758/altri_radiomediaset_radio105/altre_2__container__');" +
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
                root.findViewById(R.id.loading_zoo).setVisibility(View.VISIBLE);
                super.onPageStarted(webView, url, mBitmap);
            }

            @Override
            public void onPageFinished (WebView webView, String url) {
                webView.loadUrl(javaScript);
                root.findViewById(R.id.loading_zoo).setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                super.onPageFinished(webView, url);
            }

            @RequiresApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView webView, WebResourceRequest request, WebResourceError error) {
                // Ignore some connection errors
                // ERR_FAILED = -1
                // ERR_CONNECTION_REFUSED = -6 --> needed for people with AD Blocker
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
                Log.e(Constants.LOG_TAG, "in WebView client. isMainFrame:" + request.isForMainFrame() + ": " + request.getUrl());
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
