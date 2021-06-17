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

/*
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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Fragment to display links to social networks and direct messages to program on air.
 * <p>
 * Tries to open links in respective app or falls back to web links or other
 * means if the user doesn't have the apps installed. Currently provides support
 * for Facebook, Instagram. Twitch, TikTok, YouTube and Twitter. Links
 * into the Play Store for recommendation and sharing with friends are commented for now.
 *
 */

public class SocialFragment extends Fragment {
    private static final String ARG_CONTACT_EMAIL_ADDRESS = "diretta@105.net";
    private static final String ARG_FACEBOOK_PAGE = "Radio105";
    private static final String ARG_TWITTER_PROFILE = "Radio105";
    private static final String ARG_INSTAGRAM_PROFILE = "radio105";
    private static final String ARG_TWITCH_CHANNEL = "radio_105";
    private static final String ARG_TIKTOK_PROFILE = "@Radio105";
    private static final String ARG_YOUTUBE_PROFILE = "UCcm6KpwkAsyZ5U4LtCGjBcA";
    private static final String ARG_PHONE_NUMBER = "393424115105";
    private static final String ARG_TELEGRAM_ID = "1416935972";

    // Social networks links
    private TextView followTitle;
    private TextView followTwitter;
    private TextView openFacebookGroup;
    private TextView followInstagram;
    private TextView twitchChannel;
    private TextView followTikTok;
    private TextView youtubeChannel;
    // Feedback links
    private TextView contactTitle;
    private TextView sendEmail;
    private TextView whatsappMessage;
    private TextView telegramMessage;
    private TextView smsMessage;

    private View root;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_social, container, false);

        // Stock Colors
        MainActivity.updateColorsInterface.onUpdate(false);

        followTitle = root.findViewById(R.id.follow_title);
        followTwitter = root.findViewById(R.id.follow_twitter);
        openFacebookGroup = root.findViewById(R.id.open_facebook_group);
        followInstagram = root.findViewById(R.id.follow_instagram);
        twitchChannel = root.findViewById(R.id.twitch_account);
        followTikTok = root.findViewById(R.id.follow_tiktok);
        youtubeChannel = root.findViewById(R.id.youtube_channel);

        contactTitle = root.findViewById(R.id.contact_title);
        sendEmail = root.findViewById(R.id.send_mail);
        whatsappMessage = root.findViewById(R.id.whatsapp_message);
        telegramMessage = root.findViewById(R.id.telegram_message);
        smsMessage = root.findViewById(R.id.sms_message);

        return root;
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
        followTwitter.setOnClickListener(view2 -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("twitter://user?screen_name=" + ARG_TWITTER_PROFILE)));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/#!/" + ARG_TWITTER_PROFILE)));
            }
        });

        followInstagram.setVisibility(View.VISIBLE);
        followInstagram.setOnClickListener(view3 -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/_u/" + ARG_INSTAGRAM_PROFILE))));

        twitchChannel.setVisibility(View.VISIBLE);
        twitchChannel.setOnClickListener(view4 -> {
            try {
                requireContext().getPackageManager().getPackageInfo("com.zhiliaoapp.musically", 0);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.twitch.com/_u/" + ARG_TWITCH_CHANNEL)));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.twitch.com/" + ARG_TWITCH_CHANNEL)));
            }
        });

        followTikTok.setVisibility(View.VISIBLE);
        followTikTok.setOnClickListener(view5 -> {
            try {
                requireContext().getPackageManager().getPackageInfo("com.zhiliaoapp.musically", 0);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://vm.tiktok.com/" + ARG_TIKTOK_PROFILE)));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://vm.tiktok.com/" + ARG_TIKTOK_PROFILE)));
            }
        });

        youtubeChannel.setVisibility(View.VISIBLE);
        youtubeChannel.setOnClickListener(view6 -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube.com/channel/" + ARG_YOUTUBE_PROFILE)));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/channel/" + ARG_YOUTUBE_PROFILE)));
            }
        });

        contactTitle.setVisibility(View.VISIBLE);

        sendEmail.setVisibility(View.VISIBLE);
        sendEmail.setOnClickListener(view9 -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto", ARG_CONTACT_EMAIL_ADDRESS, null))
                    .putExtra(Intent.EXTRA_EMAIL, ARG_CONTACT_EMAIL_ADDRESS);
            startActivity(Intent.createChooser(emailIntent, view9.getContext().getString(R.string.send_email)));
        });

        whatsappMessage.setVisibility(View.VISIBLE);
        whatsappMessage.setOnClickListener(view10 -> {
            try {
                requireContext().getPackageManager().getPackageInfo("com.whatsapp", 0);
                startActivity(new Intent(Intent.ACTION_SEND, Uri.parse("https://wa.me/" + ARG_PHONE_NUMBER)));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + ARG_PHONE_NUMBER)));
            }
        });

        telegramMessage.setVisibility(View.VISIBLE);
        telegramMessage.setOnClickListener(view11 -> {
            /* Open Telegram chat in browser as there' s no way to open a Telegram chat without a username,
            and Radio 105 Telegram account does not have a username.
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=" + ARG_TELEGRAM_ID)));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://web.telegram.org/#/im?p=u" + ARG_TELEGRAM_ID)));
            } */
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://web.telegram.org/#/im?p=u" + ARG_TELEGRAM_ID)));
        });

        smsMessage.setVisibility(View.VISIBLE);
        smsMessage.setOnClickListener(view12 -> startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + ARG_PHONE_NUMBER))));
    }

    public void onDestroyView() {
        root = null;
        super.onDestroyView();
    }
}
