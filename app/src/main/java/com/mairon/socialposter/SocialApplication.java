package com.mairon.socialposter;

import android.app.Application;

import com.twitter.sdk.android.core.Twitter;
import com.vk.sdk.VKSdk;

public class SocialApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        VKSdk.initialize(getApplicationContext());
    }
}
