/*
 * Copyright (C) 2020 ayoubrem
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

    public static void checkNetworkInfo(Context context, final OnConnectionStatusChange onConnectionStatusChange) {

        mConnectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            NetworkCapabilities capabilities = mConnectivityManager.getNetworkCapabilities(mConnectivityManager.getActiveNetwork());
            if (capabilities == null) {
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
        } else {
            // For android version below Nougat api 24
            NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
            onConnectionStatusChange.onChange(networkInfo!= null && networkInfo.isConnectedOrConnecting());
        }
    }

    public static void unregisterNetworkCallback() {
        if (mConnectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
                mNetworkCallback = null;
            }
            mConnectivityManager = null;
        }
    }

    interface OnConnectionStatusChange{
        void onChange(boolean type);
    }
}
