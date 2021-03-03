package com.bb.radio105;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
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
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.adblockplus.libadblockplus.android.webview.AdblockWebView;

import java.util.Objects;

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
        } else {
            requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // Custom Colors
        final String[] darkModeValues = getResources().getStringArray(R.array.theme_values);
        // The apps theme is decided depending upon the saved preferences on app startup
        String themePref = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(getString(R.string.theme_key), getString(R.string.theme_default_value));
        // Comparing to see which preference is selected and applying those theme settings
        if (themePref.equals(darkModeValues[0])) {
            // Check system status and apply colors
            int nightModeOn = requireContext().getResources().getConfiguration().uiMode &
                    Configuration.UI_MODE_NIGHT_MASK;
            switch (nightModeOn) {
                case Configuration.UI_MODE_NIGHT_YES:
                    setZooDarkColors();
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                case Configuration.UI_MODE_NIGHT_UNDEFINED:
                    setZooLightColors();
                    break;
            }
        }
        if (themePref.equals(darkModeValues[1])) {
            setZooLightColors();
        }
        if (themePref.equals(darkModeValues[2])) {
            setZooDarkColors();
        }

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
                " if (element.length) { element[0].style.display = 'none' }; " + "})()";

        mWebView.getSettings().setLoadsImagesAutomatically(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
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
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Utils.startDownload(requireActivity(), url1);
            } else {
                Utils.requestStoragePermission(requireActivity(), root);
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        final String[] darkModeValues = getResources().getStringArray(R.array.theme_values);
        // The apps theme is decided depending upon the saved preferences on app startup
        String pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(getString(R.string.theme_key), getString(R.string.theme_default_value));
        // Comparing to see which preference is selected and applying those theme settings
        if (pref.equals(darkModeValues[0])) {
            // Check system status and restore colors
            int nightModeOn = requireContext().getResources().getConfiguration().uiMode &
                    Configuration.UI_MODE_NIGHT_MASK;
            switch (nightModeOn) {
                case Configuration.UI_MODE_NIGHT_YES:
                    setStockDarkColors();
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                case Configuration.UI_MODE_NIGHT_UNDEFINED:
                    setStockLightColors();
                    break;
            }
        }
        if (pref.equals(darkModeValues[1])) {
            setStockLightColors();
        }
        if (pref.equals(darkModeValues[2])) {
            setStockDarkColors();
        }

        mWebView.destroy();
        root = null;
        super.onDestroyView();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // BEGIN_INCLUDE(onRequestPermissionsResult)
        if (requestCode == Constants.PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE) {
            // Request for storage permission.
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted.
                Snackbar.make(root, R.string.storage_permission_granted,
                        Snackbar.LENGTH_SHORT)
                        .show();
            } else {
                // Permission request was denied.
                Snackbar.make(root, R.string.storage_permission_denied,
                        Snackbar.LENGTH_SHORT)
                        .show();
            }
        }
        // END_INCLUDE(onRequestPermissionsResult)
    }

    private void setZooLightColors() {
        requireActivity().getWindow().setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.zoo_300));
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar())
                .setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(requireContext(), R.color.zoo_500)));
        NavigationView mNavigationView = requireActivity().findViewById(R.id.nav_view);
        View header = mNavigationView.getHeaderView(0);
        LinearLayout mLinearLayout = header.findViewById(R.id.nav_header);
        mLinearLayout.setBackgroundResource(R.drawable.side_nav_bar_zoo);
        ColorStateList mColorStateList = new ColorStateList(
                new int[][] {
                        new int[] {-android.R.attr.state_checked},
                        new int[] { android.R.attr.state_checked}
                },
                new int[] {
                        ContextCompat.getColor(requireContext(), R.color.black),
                        ContextCompat.getColor(requireContext(), R.color.zoo_500),
                }
        );
        mNavigationView.setItemIconTintList(mColorStateList);
        mNavigationView.setItemTextColor(mColorStateList);
    }

    private void setZooDarkColors() {
        requireActivity().getWindow().setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.zoo_200));
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar())
                .setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(requireContext(), R.color.zoo_500)));
        NavigationView mNavigationView = requireActivity().findViewById(R.id.nav_view);
        View header = mNavigationView.getHeaderView(0);
        LinearLayout mLinearLayout = header.findViewById(R.id.nav_header);
        mLinearLayout.setBackgroundResource(R.drawable.side_nav_bar_zoo);
        ColorStateList mColorStateList = new ColorStateList(
                new int[][] {
                        new int[] {-android.R.attr.state_checked},
                        new int[] { android.R.attr.state_checked}
                },
                new int[] {
                        ContextCompat.getColor(requireContext(), R.color.white),
                        ContextCompat.getColor(requireContext(), R.color.zoo_500),
                }
        );
        mNavigationView.setItemIconTintList(mColorStateList);
        mNavigationView.setItemTextColor(mColorStateList);
    }

    private void setStockLightColors() {
        requireActivity().getWindow().setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.orange_900));
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar())
                .setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(requireContext(), R.color.yellow_700)));
        NavigationView mNavigationView = requireActivity().findViewById(R.id.nav_view);
        View header = mNavigationView.getHeaderView(0);
        LinearLayout mLinearLayout = header.findViewById(R.id.nav_header);
        mLinearLayout.setBackgroundResource(R.drawable.side_nav_bar);
        ColorStateList mColorStateList = new ColorStateList(
                new int[][] {
                        new int[] {-android.R.attr.state_checked},
                        new int[] { android.R.attr.state_checked}
                },
                new int[] {
                        ContextCompat.getColor(requireContext(), R.color.black),
                        ContextCompat.getColor(requireContext(), R.color.yellow_700),
                }
        );
        mNavigationView.setItemIconTintList(mColorStateList);
        mNavigationView.setItemTextColor(mColorStateList);
    }

    private void setStockDarkColors() {
        requireActivity().getWindow().setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.yellow_700));
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar())
                .setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(requireContext(), R.color.yellow_200)));
        NavigationView mNavigationView = requireActivity().findViewById(R.id.nav_view);
        View header = mNavigationView.getHeaderView(0);
        LinearLayout mLinearLayout = header.findViewById(R.id.nav_header);
        mLinearLayout.setBackgroundResource(R.drawable.side_nav_bar);
        ColorStateList mColorStateList = new ColorStateList(
                new int[][] {
                        new int[] {-android.R.attr.state_checked},
                        new int[] { android.R.attr.state_checked}
                },
                new int[] {
                        ContextCompat.getColor(requireContext(), R.color.white),
                        ContextCompat.getColor(requireContext(), R.color.yellow_200)
                }
        );
        mNavigationView.setItemIconTintList(mColorStateList);
        mNavigationView.setItemTextColor(mColorStateList);
    }
}
