package com.bb.the105zoo.ui.podcast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PodcastViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public PodcastViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is podcast fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}