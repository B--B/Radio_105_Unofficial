package com.bb.radio105;

public class Constants {

    // These are the Intent actions that we are prepared to handle. Notice that the fact these
    // constants exist in our class is a mere convenience: what really defines the actions our
    // service can handle are the <action> tags in the <intent-filters> tag for our service in
    // AndroidManifest.xml.
    public static final String ACTION_TOGGLE_PLAYBACK =
            "com.bb.radio105.action.TOGGLE_PLAYBACK";
    public static final String ACTION_PLAY = "com.bb.radio105.action.PLAY";
    public static final String ACTION_PAUSE = "com.bb.radio105.action.PAUSE";
    public static final String ACTION_STOP = "com.bb.radio105.action.STOP";

}
