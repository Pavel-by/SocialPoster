package com.mairon.socialposter.controller;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;

import com.mairon.socialposter.model.SocialPost;
import com.mairon.socialposter.model.SocialProfile;

import org.json.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

public interface SocialController {

    interface ResponseListener<V> {
        void onSuccess(V response);
        void onError(Error error);
    }

    void signIn();
    boolean isSignedIn();
    void signOut();
    void getUserData(ResponseListener<SocialProfile> responseListener);
    void post(SocialPost post, ResponseListener<SocialPost> responseListener);

    @Data
    @AllArgsConstructor
    @ToString
    class Error {
        private int code;
        private String message;
    }
}
