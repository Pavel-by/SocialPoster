package com.mairon.socialposter.controller;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.CallSuper;
import android.text.TextUtils;

import com.mairon.socialposter.model.SocialPost;
import com.mairon.socialposter.model.SocialProfile;
import com.mairon.socialposter.model.vk.VKPost;
import com.mairon.socialposter.model.vk.VKProfile;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKUsersArray;

import org.json.JSONObject;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

public class VK implements SocialController {

    private Activity activity;
    private String[] scopes         = {
            VKScope.WALL
    };
    private String   userDataFields = TextUtils.join(",", new String[]{
            VKApiUserFull.FIELD_PHOTO_100
    });
    private VKProfile profile = new VKProfile();

    public VK(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void signIn() {
        profile.clear();
        VKSdk.login(activity, scopes);
    }

    @Override
    public void signOut() {
        profile.clear();
        VKSdk.logout();
    }

    @Override
    public boolean isSignedIn() {
        return VKSdk.isLoggedIn();
    }

    @Override
    public void getUserData(final ResponseListener<SocialProfile> responseListener) {
        if (!profile.isExpired()) {
            responseListener.onSuccess(profile);
        } else {
            VKRequest request = new VKRequest("users.get", VKParameters.from(
                    VKApiConst.FIELDS, userDataFields
            ));
            request.executeWithListener(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    try {
                        JSONObject json = response.json.getJSONArray("response").getJSONObject(0);
                        profile.setExpired(false);
                        profile.setID(json.getInt("id"));
                        profile.setFirstName(json.getString("first_name"));
                        profile.setLastName(json.getString("last_name"));
                        profile.setImageUrl(json.getString(VKApiUserFull.FIELD_PHOTO_100));
                        responseListener.onSuccess(profile);
                    } catch (Exception e) {
                        responseListener.onError(new Error(0, "Ошибка при обработке ответа сервера"));
                    }
                }

                @Override
                public void onError(VKError error) {
                    responseListener.onError(new Error(error.errorCode, error.errorMessage));
                }
            });
        }
    }

    @Override
    public void post(
            final SocialPost post,
            final ResponseListener<SocialPost> responseListener
    )
    {
        VKRequest request = new VKRequest("wall.post", VKParameters.from(
                VKApiConst.MESSAGE, post.getText()
        ));
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                try {
                    JSONObject json = response.json.getJSONObject("response");
                    VKPost responsePost = new VKPost();
                    responsePost.setID(json.getInt("post_id"));
                    responsePost.setText(post.getText());
                    responseListener.onSuccess(responsePost);
                } catch (Exception e) {
                    responseListener.onError(new Error(0, "Ошибка при обработке ответа сервера"));
                }
            }

            @Override
            public void onError(VKError error) {
                responseListener.onError(new Error(error.errorCode, error.errorMessage));
            }
        });
    }

    public boolean onActivityResult(
            int requestCode,
            int resultCode,
            Intent data,
            VKCallback<VKAccessToken> listener
    )
    {
        return VKSdk.onActivityResult(requestCode, resultCode, data, listener);
    }
}
