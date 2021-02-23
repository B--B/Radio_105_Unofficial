package com.bb.radio105;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import org.adblockplus.libadblockplus.android.webview.AdblockWebView;
import org.jetbrains.annotations.NotNull;

public class ZooFragment extends Fragment {

    AdblockWebView mWebView = null;

    @SuppressLint("SetJavaScriptEnabled")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_zoo, container, false);

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

        mWebView = root.findViewById(R.id.webView);
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
                root.findViewById(R.id.loading).setVisibility(View.VISIBLE);
                super.onPageStarted(webView, url, mBitmap);
            }

            @Override
            public void onPageFinished (WebView webView, String url) {
                webView.loadUrl(javaScript);
                root.findViewById(R.id.loading).setVisibility(View.GONE);
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
                        Toast toast = Toast.makeText(getContext(),
                                "Something went wrong, the error code is  " + error.getErrorCode()
                                        + "  Description:  " + error.getDescription().toString(), Toast.LENGTH_LONG);
                        toast.show();
                        break;
                }
            }

            @Deprecated
            @Override
            public void onReceivedError(WebView webView, int errorCode, String description, String failingUrl) {
                webView.loadUrl(Constants.ErrorPagePath);
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
