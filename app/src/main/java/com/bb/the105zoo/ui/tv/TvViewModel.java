package com.bb.the105zoo.ui.tv;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TvViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public TvViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is tv fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}