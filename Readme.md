# Radio 105 Unofficial App

<br />
<br />

<img src="images/Screenshot_1.png" width="200"> <img src="images/Screenshot_8.png" width="200">

<br />
<br />

I've decided to start the development of this app tired of the bugs in the official app, especially with newer android version.

<br />
<br />

**Features:**

* Completely open source
* Compatible with all devices running android Lollipop and above
* No Ads
* No external libraries except [AdBlockPlus](https://github.com/adblockplus/libadblockplus-android) webview for Podcats and The 105 Zoo sections

<br />
<br />

**Permissions:**

* INTERNET
* ACCESS_NETWORK_STATE and ACCESS_WIFI_STATE: needed by recover stream option and for properly set the partial WiFi wakelock
* FOREGROUND_SERVICE: needed by the Radio streamig service. Without this Android will kill the streaming service after a few minutes when the screen is off
* WAKE_LOCK: needed by the radio streaming service when running. Without this there's the possibility that Android turns off the WiFi when the screen is off even if the streaming service is running and using it
* WRITE_EXTERNAL_STORAGE: this permission is NOT granted by default, will be asked only when the user tries to download a podcast from podcast or the 105 zoo sections

<br />
<br />

**Sections:**

1. Radio: the place where the radio streaming can be started and controlled <img src="images/Screenshot_1.png" align="right" height="75" ><img src="images/Screenshot_2.png" align="right" height="75" />
    * When the stream starts a notification with multimedia commands will be created
    * The stream can be stopped, paused and restarted (if persistent notification option is enabled) even on secure lockscreen

2. 105 TV: a simple fragment that stream 105 Tv channel from [Mediaset Play](https://www.mediasetplay.mediaset.it/) <img src="images/Screenshot_3.png" align="right" height="75" >
    * Automatically enable fullscreen when in portrat mode
    * Screen does not turns off when the user is in this section

3. Podcast: the [105.net](https://105.net) podcast section <img src="images/Screenshot_4.png" align="right" height="75" >
    * All ad and banners completely removed
    * Podcast can be listened and downloaded (write on storage permission is required for download)
    * No cookies
    * If for some reason a link can't be opened, the app will prompt the user to open it in an external browser

4. The 105 Zoo: the complete [zoo.105.net](https://zoo.105.net) site <img src="images/Screenshot_5.png" align="right" height="75" >
    * All ad and banners completely removed
    * Podcast can be listened and downloaded (write on storage permission is required for download)
    * No cookies
    * If for some reason a link can't be opened, the app will prompt the user to open it in an external browser

5. Social <a href="images/Screenshot_6.png"><img src="images/Screenshot_6.png" align="right" height="75" ></a>
    * Provide links for all Radio 105 social accounts
    * Provide links for send a message to the program on air

6. Settings: <a href="images/Screenshot_7.png"><img src="images/Screenshot_7.png" align="right" height="75" ></a>
    * Theme: choose between light, dark or system theme *Default: system*
    * Stop streaming setting: allow system to stop streaming when app is removed from recent tasks *Default: disabled*
    * Audio devices: pause streaming when an audio device (headset, BT, etc.) is disconnected *Default: enabled*
    * Persistent notification: when pausing the radio streaming, the icon will persist in status bar *Default: disabled*
    * Screen on: keep the screen on when in podcast and the 105 zoo sections *Default: disabled*
    * Reconnect stream: restart the radio stream after an error if internet is available  *Default: enabled*
    * Network change: restart the radio stream immediately when the device switch from mobile network to WiFi and vice versa  *Default: enabled*

