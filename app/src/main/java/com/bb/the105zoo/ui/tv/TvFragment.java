package com.bb.the105zoo.ui.tv;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bb.the105zoo.R;

public class TvFragment extends Fragment {

    private TvViewModel tvViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        tvViewModel =
                new ViewModelProvider(this).get(TvViewModel.class);
        View root = inflater.inflate(R.layout.fragment_tv, container, false);

        String videoUrl = "https://live2t-radio-mediaset-it.akamaized.net/content/hls_h0_clr_vos/live/channel(ec)/index.m3u8?hdnts=st=1613162433~exp=1613176863~acl=/content/hls_h0_clr_vos/live/channel(ec)*~hmac=aaffca0db29392483c0720e80adbf3f1d30e773e1068063265fa81970cfb982e";
        VideoView videoView = root.findViewById(R.id.videoView);
//Use a media controller so that you can scroll the video contents
//and also to pause, start the video.
        MediaController mediaController = new MediaController(getContext());
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        videoView.setVideoURI(Uri.parse(videoUrl));
        videoView.start();

        return root;
    }
}