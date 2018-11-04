package com.mairon.socialposter.controller;

import android.app.Activity;
import android.content.Intent;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.Profile;
import com.facebook.ProfileManager;
import com.facebook.internal.Utility;
import com.facebook.login.LoginBehavior;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.mairon.socialposter.model.SocialPost;
import com.mairon.socialposter.model.SocialProfile;
import com.mairon.socialposter.model.facebook.FacebookProfile;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Facebook implements SocialController {

    private final String TAG = "FacebookController";

    public final static int PICTURE_WIDTH  = 100;
    public final static int PICTURE_HEIGHT = 100;

    private CallbackManager    callbackManager;
    private Collection<String> permissions   = Arrays.asList(
        "publish_pages"
    );
    private Activity           activity;
    private FacebookProfile    storedProfile = new FacebookProfile();

    public Facebook(
            Activity activity,
            FacebookCallback<LoginResult> loginCallback
    )
    {
        this.activity = activity;
        this.callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, loginCallback);
    }

    @Override
    public void signIn() {
        storedProfile.setExpired(true);
        LoginManager.getInstance().logInWithPublishPermissions(activity, permissions);
    }

    @Override
    public boolean isSignedIn() {
        storedProfile.setExpired(true);
        AccessToken token = AccessToken.getCurrentAccessToken();
        return token != null && !token.isExpired();
    }

    @Override
    public void signOut() {
        LoginManager.getInstance().logOut();
    }

    @Override
    public void getUserData(final ResponseListener<SocialProfile> responseListener) {
        if (!isSignedIn()) {
            responseListener.onError(new Error(0, "Необходимо авторизоваться перед получением данных профиля."));
        } else if (!storedProfile.isExpired()) {
            responseListener.onSuccess(storedProfile);
        } else {
            fetchProfile(new ResponseListener<Profile>() {
                @Override
                public void onSuccess(Profile responseProfile) {

                    storedProfile.setExpired(false);
                    storedProfile.setID(responseProfile.getId());
                    storedProfile.setFirstName(responseProfile.getFirstName());
                    storedProfile.setLastName(responseProfile.getLastName());
                    if (storedProfile.getImageUrl() != null
                        && !storedProfile.getImageUrl().equals(
                            responseProfile.getProfilePictureUri(PICTURE_WIDTH, PICTURE_HEIGHT)
                                    .toString())
                            )
                    {
                        storedProfile.setImageUrl(responseProfile.getProfilePictureUri(PICTURE_WIDTH, PICTURE_HEIGHT)
                                                          .toString());
                        storedProfile.setImage(null);
                    }
                    Log.e(TAG, storedProfile.toString());
                    responseListener.onSuccess(storedProfile);
                }

                @Override
                public void onError(Error error) {
                    responseListener.onError(error);
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
        Bundle params = new Bundle();
        params.putString("message", post.getText());
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/me/feed",
                params,
                HttpMethod.POST,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        Log.e(TAG, "POST RESPONSE: " + response.toString());
                        responseListener.onSuccess(post);
                    }
                }
        ).executeAsync();
    }

    public boolean onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    )
    {
        return callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void fetchProfile(final ResponseListener<Profile> listener) {
        Utility.getGraphMeRequestWithCacheAsync(AccessToken.getCurrentAccessToken().getToken(),
                                                new Utility.GraphMeRequestWithCacheCallback() {
                                                    @Override
                                                    public void onSuccess(JSONObject userInfo) {
                                                        String id = userInfo.optString("id");
                                                        if (id == null) {
                                                            return;
                                                        }
                                                        String link = userInfo.optString("link");
                                                        Profile profile = new Profile(
                                                                id,
                                                                userInfo.optString("first_name"),
                                                                userInfo.optString("middle_name"),
                                                                userInfo.optString("last_name"),
                                                                userInfo.optString("name"),
                                                                link !=
                                                                null ? Uri.parse(link) : null
                                                        );
                                                        Profile.setCurrentProfile(profile);
                                                        listener.onSuccess(profile);
                                                    }

                                                    @Override
                                                    public void onFailure(FacebookException error) {
                                                        Toast.makeText(activity, "Не удалось загрузить данные профиля", Toast.LENGTH_SHORT)
                                                                .show();
                                                        listener.onError(new Error(0, error.getLocalizedMessage()));
                                                    }
                                                });
    }
}
