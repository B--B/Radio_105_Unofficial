package com.bb.radio105;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.jetbrains.annotations.NotNull;

public class ZooFragment extends Fragment {
    WebView mWebView = null;

    @SuppressLint("SetJavaScriptEnabled")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_zoo, container, false);

        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                if (mWebView.canGoBack()) {
                    mWebView.goBack();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);

        mWebView = root.findViewById(R.id.webView);
        String url = "https://zoo.105.net";
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
                " if (element.length) { element[0].style.display = 'none' }; " + "})()";

        mWebView.getSettings().setLoadsImagesAutomatically(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        if (savedInstanceState != null) {
            // Restore last state for checked position.
            mWebView.restoreState(savedInstanceState);
        } else  {
            mWebView.loadUrl(url);
        }

        mWebView.setWebViewClient(new WebViewClient() {

            public boolean shouldOverrideUrlLoading (WebView view, WebResourceRequest request) {
                return Uri.parse(request.getUrl().toString()).getHost().contains("www.105.net");
            }

            @Override
            public void onPageStarted(WebView mWebView, String url, Bitmap mBitmap) {
                mWebView.setVisibility(View.GONE);
                root.findViewById(R.id.loading).setVisibility(View.VISIBLE);
                super.onPageStarted(mWebView, url, mBitmap);
            }

            @Override
            public void onPageFinished (WebView mWebView, String url) {
                mWebView.loadUrl(javaScript);
                root.findViewById(R.id.loading).setVisibility(View.GONE);
                mWebView.setVisibility(View.VISIBLE);
                super.onPageFinished(mWebView, url);
            }
        });
        return root;
    }

    @Override
    public void onStart() {
        boolean pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.screen_on_key), false);
        if (pref) {
            requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        super.onStart();
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle savedInstanceState) {
        mWebView.saveState(savedInstanceState);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        mWebView.destroy();
        super.onDestroy();
    }
}
