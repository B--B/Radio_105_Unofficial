package com.bb.the105zoo.ui.podcast;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bb.the105zoo.R;

public class PodcastFragment extends Fragment {

    private PodcastViewModel podcastViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        podcastViewModel =
                new ViewModelProvider(this).get(PodcastViewModel.class);
        View root = inflater.inflate(R.layout.fragment_podcast, container, false);
        final TextView textView = root.findViewById(R.id.text_slideshow);
        podcastViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }
}