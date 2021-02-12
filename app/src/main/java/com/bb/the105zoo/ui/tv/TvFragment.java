package com.bb.the105zoo.ui.tv;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
        final TextView textView = root.findViewById(R.id.text_gallery);
        tvViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }
}