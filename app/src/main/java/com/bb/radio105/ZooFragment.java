package com.bb.radio105;

import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ZooFragment extends Fragment {
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_zoo, container, false);

        WebView mWebView = root.findViewById(R.id.webView);

        String url = "https://zoo.105.net/sezioni/1080/scenette";

        mWebView.getSettings().setLoadsImagesAutomatically(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mWebView.loadUrl(url);

        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView mWebView, String url, Bitmap mBitmap) {
                mWebView.setVisibility(View.GONE);
                root.findViewById(R.id.loading).setVisibility(View.VISIBLE);
                super.onPageStarted(mWebView, url, mBitmap);
            }

            @Override
            public void onPageFinished (WebView mWebView, String url) {
                root.findViewById(R.id.loading).setVisibility(View.GONE);
                mWebView.setVisibility(View.VISIBLE);
                super.onPageFinished(mWebView, url);
            }
        });
        return root;
    }
}
