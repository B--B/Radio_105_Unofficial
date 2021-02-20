package com.bb.radio105;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class PodcastFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_podcast, container, false);

        WebView mWebView = root.findViewById(R.id.webView);

        String url = "https://www.105.net/sezioni/995/podcast";

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
                mWebView.loadUrl("javascript:(function() { " +
                        "var element = document.getElementsByClassName('navbar-fixed-top hidden-print');" +
                        " if (element.length) { element[0].style.display = 'none' }; " +
                        "var element = document.getElementsByClassName('container vc_bg_white');" +
                        " if (element.length) { element[0].style.display = 'none' }; " +
                        "var element = document.getElementsByClassName('col-xs-12 vc_p0');" +
                        " if (element.length) { element[0].style.display = 'none' }; " +
                        "var element = document.getElementsByClassName('container-fluid vc_bg_darkgray vc_bt7_yellow vc_z2 vc_hidden_print');" +
                        " if (element.length) { element[0].style.display = 'none' }; " +
                        "var element = document.getElementsByClassName('container-fluid vc_bg_color_program');" +
                        " if (element.length) { element[0].style.display = 'none' }; " +
                        "var element = document.getElementsByClassName('container-fluid vc_bg_white vc_pl_pr_0_mobile');" +
                        " if (element.length) { element[0].style.display = 'none' }; " +
                        "var element = document.getElementsByClassName('vc_search_refine_results_standard');" +
                        " if (element.length) { element[0].style.display = 'none' }; " +
                        "var element = document.getElementsByClassName('text_edit vc_textedit_title_refine_results null');" +
                        " if (element.length) { element[0].style.display = 'none' }; " +
                        "var element = document.getElementsByClassName('anteprima_articolo article_cont vc_preview_medium_bt_podcast vc_txt_m variant vc_theme_light vc_br_60 null Zoo di 105 vc_section_lo-zoo-di-105-home vc_macro_section_canale-lo-zoo-di-105 homezoo scheda cms_article ');" +
                        " if (element.length) { element[0].style.display = 'none' }; " +
                        "var element = document.getElementsByClassName('social social_buttons');" +
                        " if (element.length) { element[0].style.display = 'none' }; " + "})()");
                root.findViewById(R.id.loading).setVisibility(View.GONE);
                mWebView.setVisibility(View.VISIBLE);
                super.onPageFinished(mWebView, url);
            }
        });
        return root;
    }
}
