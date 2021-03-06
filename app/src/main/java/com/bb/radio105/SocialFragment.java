/*
 * Copyright 2017 Sascha Peilicke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bb.radio105;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Fragment to display links to the Play Store and social networks.
 * <p>
 * Tries to open links in respective app or falls back to web links or other
 * means if the user doesn't have the apps installed. Currently provides support
 * for Facebook, Google+ and Twitter. Provides links into the Play Store for
 * recommendation and sharing with friends.
 */
public class SocialFragment extends Fragment {
    public static final String ARG_APPLICATION_ID = BuildConfig.APPLICATION_ID;
    public static final String ARG_CONTACT_EMAIL_ADDRESS = "diretta@105.net";
    public static final String ARG_FACEBOOK_PAGE = "Radio105";
    public static final String ARG_TWITTER_PROFILE = "Radio105";

    // Social networks links
    private TextView followTitle;
    private TextView followTwitter;
    private TextView openFacebookGroup;
    // Recommendation links
    private TextView rateOnPlayStore;
    // Feedback links
    private TextView contactTitle;
    private TextView provideFeedback;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_social, container, false);
        followTitle = view.findViewById(R.id.follow_title);
        followTwitter = view.findViewById(R.id.follow_twitter);
        openFacebookGroup = view.findViewById(R.id.open_facebook_group);

        rateOnPlayStore = view.findViewById(R.id.rate_play_store);

        contactTitle = view.findViewById(R.id.contact_title);
        provideFeedback = view.findViewById(R.id.provide_feedback);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        followTitle.setVisibility(View.VISIBLE);

        openFacebookGroup.setVisibility(View.VISIBLE);
        openFacebookGroup.setOnClickListener(view1 -> {
            try {
                requireContext().getPackageManager().getPackageInfo("com.facebook.katana", 0);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/" + ARG_FACEBOOK_PAGE)));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/" + ARG_FACEBOOK_PAGE)));
            }
        });

        followTwitter.setVisibility(View.VISIBLE);
        followTwitter.setOnClickListener(view12 -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("twitter://user?screen_name=" + ARG_TWITTER_PROFILE)));
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/#!/" + ARG_TWITTER_PROFILE)));
            }
        });

        rateOnPlayStore.setOnClickListener(view13 -> {
            // To count with Play market back stack, After pressing back button,
            // to taken back to our application, we need to add following flags to intent.
            int flags = Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
            if (Build.VERSION.SDK_INT >= 21) {
                flags |= Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
            } else {
                //noinspection deprecation
                flags |= Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET;
            }
            Intent goToMarket = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + ARG_APPLICATION_ID))
                    .addFlags(flags);
            try {
                startActivity(goToMarket);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=" + ARG_APPLICATION_ID)));
            }
        });

        contactTitle.setVisibility(View.VISIBLE);

        final String emailSubject;
        emailSubject = getString(R.string.feedback);
        final String emailText;
        emailText = getString(R.string.i_love_your_app);

        provideFeedback.setVisibility(View.VISIBLE);
        provideFeedback.setOnClickListener(view14 -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto", ARG_CONTACT_EMAIL_ADDRESS, null))
                    .putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                    .putExtra(Intent.EXTRA_TEXT, emailText)
                    .putExtra(Intent.EXTRA_EMAIL, ARG_CONTACT_EMAIL_ADDRESS);
            startActivity(Intent.createChooser(emailIntent, view14.getContext().getString(R.string.send_email)));
        });
    }
}
