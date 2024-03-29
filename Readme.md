<p align="center">
    <a href="https://play.google.com/store/apps/details?id=com.bb.radio105" alt="Radio 105 Unofficial App">
        <img src="images/Logo.png" width="300"/></a>
</p>

<br />

<p align="center">
    <a href="https://github.com/B--B/Radio_105_Unofficial/releases" alt="Releases">
        <img src="https://badgen.net/github/releases/B--B/Radio_105_Unofficial" /></a>
    <a href="#download" alt="Latest Release">
        <img src="https://badgen.net/github/release/B--B/Radio_105_Unofficial" /></a>
    <a href="https://github.com/B--B/Radio_105_Unofficial/tags" alt="Tags">
        <img src="https://badgen.net/github/tag/B--B/Radio_105_Unofficial" /></a>
    <a href="https://github.com/B--B/Radio_105_Unofficial/commits/main" alt="Commits">
        <img src="https://badgen.net/github/commits/B--B/Radio_105_Unofficial/main" /></a>
    <a href="https://github.com/B--B/Radio_105_Unofficial/pulse" alt="Last Commit">
        <img src="https://badgen.net/github/last-commit/B--B/Radio_105_Unofficial/main" /></a>
    <a href="#license" alt="License">
        <img src="https://badgen.net/github/license/B--B/Radio_105_Unofficial" /></a>
</p>

<br />
<br />

#### Screenshots

<p align="center">
<img src="images/Screenshot_1.png" width="180">
<img src="images/Screenshot_8.png" width="180">
<img src="images/Screenshot_9.png" width="180">
<img src="images/Screenshot_11.png" width="180">
</p>

<br />
<br />

#### Material You on Android 12

<p align="center">
<img src="images/Material_you_1.png" height="125">
<img src="images/Material_you_2.png" height="125">
<img src="images/Material_you_3.png" height="125">
<img src="images/Material_you_4.png" height="125">
<img src="images/Material_you_5.png" height="125">
<img src="images/Material_you_6.png" height="125">
</p>

### Features:

* Completely open source
* Compatible with all devices running android Lollipop and above
* No Ads
* No external libraries except ~~[AdBlockPlus](https://gitlab.com/eyeo/distpartners/libadblockplus-android) for Podcast and The 105 Zoo sections,~~ [jsoup](https://jsoup.org/) for parsing radio streaming metadata, [Glide](https://github.com/bumptech/glide) for WebView image caching, [LeakCanary](https://github.com/square/leakcanary) (only in Debug builds) for memory leaks detection and [Timber](https://github.com/JakeWharton/timber) (only in Debug builds) for logging
* The radio streaming can be controlled with wearable devices or Android Auto
* Can be installed on android TV --> Tested with Android TV emulator
* Compatible with wearable devices --> Tested with Wear OS emulator and with Xiaomi Band 4 and 5
* Fully compatible with Android 14 and [Material you](https://material.io/blog/announcing-material-you)
* The version downloadable from Play Store use the new [Android App Bundle](https://developer.android.com/platform/technology/app-bundle) format

<br />
<br />

### Permissions:

* INTERNET
* ACCESS_NETWORK_STATE and ACCESS_WIFI_STATE: needed by recover stream option and for properly set the partial WiFi wakelock
* FOREGROUND_SERVICE: needed by the Radio streaming service. Without this Android will kill the streaming service after a few minutes when the screen is off
* WAKE_LOCK: needed by the radio streaming service when running. Without this there's the possibility that Android turns off the WiFi or send the CPU in deep sleep state when the screen is off even if the streaming service is running
* WRITE_EXTERNAL_STORAGE: this permission is NOT granted by default and will be asked only when the user tries to download a podcast from podcast or the 105 zoo sections. On Android versions >= 10 the permission is NOT required, the app will use the new scoped storage model implemented in Android 10. More info [here](https://developer.android.com/about/versions/11/privacy/storage)
* POST_NOTIFICATIONS: this permission is needed only when using the Android standard notification type instead the multimedia notification type, it's disabled by default and user will be prompted to grant the permission when Android standard notification type in settings is enabled

<br />
<br />

### Sections:

1. Radio: the place where the radio streaming can be started and controlled <img src="images/Screenshot_1.png" align="right" height="75" ><img src="images/Screenshot_2.png" align="right" height="75" />
    * When the stream starts a notification with multimedia commands will be created
    * The stream can be stopped, paused and restarted even on secure lockscreen and with wearable devices

2. 105 TV: a simple fragment that stream 105 Tv channel from [Mediaset Play](https://www.mediasetplay.mediaset.it/) <img src="images/Screenshot_3.png" align="right" height="75" >
    * Automatically enable fullscreen when in landscape mode
    * Screen does not turns off when the user is in this section
    * PiP mode is supported for devices running at least android Oreo

3. Podcast: the [105.net](https://105.net) programs section <img src="images/Screenshot_4.png" align="right" height="75" >
    * All ad and banners completely removed
    * Podcast can be listened and downloaded (write on storage permission is required for download on Android versions < 10)
    * Starting from version 1.7.0 podcast can be listened even with the screen off or with the app in background, and a notification with play/pause button is displayed. The feature works fine, but there are some limitation at the moment:
        * ~~It's not a MediaSession, this means standard notification and no multimedia controls on wearable devices~~ --> Added in v1.7.1
        * Streaming service will stop if app is removed from recent apps list
    * No cookies
    * If for some reason a link can't be opened, the app will prompt the user to open it in an external browser
    * PiP mode is supported for fullscreen videos in devices running at least android Oreo
    * For devices running android L and M there are some limitations due to the really old WebView version:
        * Download feature is not available
        * Some videos may not work well, or not work at all
        * ~~The artwork in notification service doesn't work, a placeholder will be displayed instead~~

4. The 105 Zoo: the complete [zoo.105.net](https://zoo.105.net) site <img src="images/Screenshot_5.png" align="right" height="75" >
    * All ad and banners completely removed
    * Podcast can be listened and downloaded (write on storage permission is required for download on Android versions < 10)
    * Starting from version 1.7.0 podcast can be listened even with the screen off or with the app in background, and a notification with play/pause button is displayed. The feature works fine, but there are some limitation at the moment:
        * ~~It's not a MediaSession, this means standard notification and no multimedia controls on wearable devices~~ --> Added in v1.7.1
        * Streaming service will stop if app is removed from recent apps list
    * No cookies
    * If for some reason a link can't be opened, the app will prompt the user to open it in an external browser
    * PiP mode is supported for fullscreen videos in devices running at least android Oreo
    * For devices running android L and M there are some limitations due to the really old WebView version:
        * Download feature is not available
        * Some videos may not work well, or not work at all
        * ~~The artwork in notification service doesn't work, a placeholder will be displayed instead~~

5. Social <img src="images/Screenshot_6.png" align="right" height="75" >
    * Provide links for all Radio 105 social accounts
    * Provide links for send a message to the program on air

6. Settings: the app options, some of them are available only for certain devices <img src="images/Screenshot_7.png" align="right" height="75" >
    * Instructions for properly run the radio streaming service on devices with MiUi/EMUI roms | **Only in MiUi and EMUI devices**
    * Theme: choose between light, dark or system theme *Default: system*
    * Notification type: allow user to choose between a standard or a multimedia notification during the audio streaming *Default: media notification* | **Removed in Android TV devices**
    * WebView Themes: allow users to enable or disable Podcast and The 105 Zoo sections theming *Default: enabled*
    * Screen on: keep the screen on when in podcast and the 105 zoo sections *Default: disabled* | **Removed in Android TV devices**
    * ~~WebView postVisualStateCallback: use a postVisualStateCallback for set visibility when page is loaded in Podcast/Zoo sections. According to Android Studio documentation this method may NOT be compatible with all devices, but is faster and do not cause graphical glitches *Default: disabled for potential incompatibility with some devices, but everyone should give it a try and keep it enabled if works properly*~~ --> This option has been deprecated, enabled by default and moved under **Experimental Options** after the migration to WebVieWCompat
    * Stop streaming setting: allow system to stop streaming when app is removed from recent tasks *Default: disabled* | **Removed in Android TV and in MiUi/EMUI devices**
    * Audio devices: pause streaming when an audio device (headset, BT, etc.) is disconnected *Default: enabled*
    * HTTPS/HTTP connection: switch between HTTPS/HTTP connection for Radio streaming service *Default: HTTPS*
    * Reconnect stream: restart the radio stream after an error or a network change if internet is available  *Default: enabled*
    * Experimental Options: this section may contain option in unsafe/not working/testing phase **Hidden by default --> On <ins>Developer Fragment</ins> tap 7 times on app version TextView**

7. Developer Info <img src="images/Screenshot_10.png" align="right" height="75" >
    * Provide developer contact, bug report and sources link on GitHub
    * Provide link to Play Store page and share/vote app buttons
    * Show the version number of the installed app

<br />
<br />

## Download:

<p align="center">
    <a href='https://github.com/B--B/Radio_105_Unofficial/releases/latest'>
        <img alt='Get it on GitHub' src='https://badgen.net/badge/icon/Get%20it%20on%20GitHub/grey/?scale=2&icon=github&label'/></a>
    <a href='https://play.google.com/store/apps/details?id=com.bb.radio105'>
        <img alt='Get it on Play Store' src='https://badgen.net/badge/icon/Get%20it%20on%20Play%20Store/black/?scale=2&icon=googleplay&label'/></a>

<br />
<br />
<b>Packages Size</b><br>
<a>APK: ~5.9Mb</a> <b>|</b> <a>AAB: ~5.4Mb</a>
</p>

Please note that signing keys are different, this means that you need to uninstall Play Store version if you want to install GitHub version. Usually updates on GitHub are pushed a couple of days before Play Store releases (this is due to Google app review times), but keep in mind that using GitHub version will stop updates from Play Store, and you have to update manually the app from GitHub everytime a new version is released.

<br />
<br />

## Build Instructions

1. [Android Studio Preview](https://developer.android.com/studio/preview) is suggested, stable version can be used for compiling too, but build.gradle should be modified as stable version does not support the gradle plugin version in use. Installation info [here](https://developer.android.com/studio/install).

2. Download the repository zip and uncompress it or clone the repository with `git clone https://github.com/B--B/Radio_105_Unofficial.git`

3. Open Android Studio and in **Project** section click on **Open** and select the repository folder

4. Let Android Studio download and setup all dependencies, then in the menu bar, click **Build --> Generate Signed Bundle/APK**.

5. Choose **APK** and click **Next**

6. Create a signing key for signing the apk, click on **Create New...**, provide all required info then click **OK** --> This step is required only the first time

7. Once you have the signing key, put your keystore password and click on **Next**

8. Select *debug* or *release* package and click **Finish**

9. APK will be generated in folder `Radio_105_Unofficial/app/release/` for *release* APKs, and in `Radio_105_Unofficial/app/debug/` folder for *debug* APKs.

<br />
<br />

## License

    Copyright (C) 2021 B--B

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
