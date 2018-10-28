package com.mairon.socialposter.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.MenuRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.mairon.socialposter.R;

public abstract class SocialActivity extends AppCompatActivity {

    private final String TAG = "SocialActivity";



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
