package com.bb.radio105;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import androidx.annotation.NonNull;

public class NetworkUtil {

    static ConnectivityManager mConnectivityManager;
    static ConnectivityManager.NetworkCallback mNetworkCallback;

    public static void checkNetworkInfo(Context context, final OnConnectionStatusChange onConnectionStatusChange){

        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            NetworkCapabilities capabilities = mConnectivityManager.getNetworkCapabilities(mConnectivityManager.getActiveNetwork());
            if (capabilities == null){
                onConnectionStatusChange.onChange(false);
            }
            mNetworkCallback = (new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    onConnectionStatusChange.onChange(true);
                }
                @Override
                public void onLost(@NonNull Network network) {
                    onConnectionStatusChange.onChange(false);
                }
            });
            mConnectivityManager.registerDefaultNetworkCallback(mNetworkCallback);
        }
        //for android version below Nougat api 24
        else {
            NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
            onConnectionStatusChange.onChange(networkInfo!= null && networkInfo.isConnectedOrConnecting());
        }
    }

    public static void unregisterNetworkCallback() {
        if (mConnectivityManager != null) {
            mConnectivityManager.unregisterNetworkCallback((ConnectivityManager.NetworkCallback) mNetworkCallback);
            mNetworkCallback = null;
            mConnectivityManager = null;
        }
    }

    interface OnConnectionStatusChange{
        void onChange(boolean type);
    }
}
