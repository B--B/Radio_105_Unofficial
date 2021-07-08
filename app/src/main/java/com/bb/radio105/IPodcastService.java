package com.bb.radio105;

interface IPodcastService {
    void playbackState(String playbackState);
    void duckRequest(Boolean mustDuck);
}
