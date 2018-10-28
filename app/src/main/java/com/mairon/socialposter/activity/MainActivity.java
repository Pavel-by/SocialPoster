package com.mairon.socialposter.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.mairon.socialposter.ImageStorage;
import com.mairon.socialposter.R;
import com.mairon.socialposter.controller.Facebook;
import com.mairon.socialposter.controller.SocialController;
import com.mairon.socialposter.controller.VK;
import com.mairon.socialposter.model.SocialPost;
import com.mairon.socialposter.model.SocialProfile;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.api.VKError;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    private final int ICON_VK        = R.drawable.vk_icon;
    private final int ICON_FACEBOOK  = R.drawable.facebook_icon;
    private final int ICON_TWITTER   = R.drawable.twitter_icon;
    private final int ICON_NO_AVATAR = R.drawable.no_avatar_icon;

    private final int ICON_BORDER_COLOR_ACTIVE   = R.color.socialIconBorderColorActive;
    private final int ICON_BORDER_COLOR_INACTIVE = R.color.socialIconBorderColorInactive;

    private VK                            vkHelper;
    private VKCallback<VKAccessToken>     vkLoginCallback;
    private Facebook                      facebookHelper;
    private FacebookCallback<LoginResult> facebookLoginCallback;

    private CircleImageView imageViewVK;
    private CircleImageView imageViewFacebook;
    private CircleImageView imageViewTwitter;
    private ProgressBar     progressBarVK;
    private ProgressBar     progressBarFacebook;
    private ProgressBar     progressBarTwitter;
    private EditText        editTextMessage;
    private Button          buttonSend;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        bindViews();
        bindViewsListeners();
        initApiHelpers();

        updateVKInfo();
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data
    )
    {
        if (vkHelper.onActivityResult(requestCode, resultCode, data, vkLoginCallback)
            || facebookHelper.onActivityResult(requestCode, resultCode, data))
        {
            return;
        }
    }

    private void initApiHelpers() {
        this.vkLoginCallback = new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                updateVKInfo();
            }

            @Override
            public void onError(VKError error) {
                updateVKInfo();
                switch (error.errorCode) {
                    case VKError.VK_REQUEST_HTTP_FAILED: {
                        Toast.makeText(MainActivity.this, "Проверьте ваше подключение к интернету", Toast.LENGTH_SHORT)
                                .show();
                        break;
                    }
                    case VKError.VK_REQUEST_NOT_PREPARED: {
                        Toast.makeText(MainActivity.this, "Системная ошибка", Toast.LENGTH_SHORT)
                                .show();
                    }
                    case VKError.VK_JSON_FAILED: {
                        Toast.makeText(MainActivity.this, "Ошибка при обработке ответа с сервера", Toast.LENGTH_SHORT)
                                .show();
                    }
                    case VKError.VK_API_ERROR: {
                        Toast.makeText(MainActivity.this, error.errorMessage, Toast.LENGTH_SHORT)
                                .show();
                    }
                }
            }
        };
        this.facebookLoginCallback = new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                updateFacebookInfo();
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(MainActivity.this, error.getLocalizedMessage(), Toast.LENGTH_SHORT)
                        .show();
            }
        };

        this.vkHelper = new VK(this);
        this.facebookHelper = new Facebook(this, facebookLoginCallback);

        if (facebookHelper.isSignedIn()) updateFacebookInfo();
    }

    private void bindViews() {
        this.imageViewVK = findViewById(R.id.socialIconVK);
        this.imageViewFacebook = findViewById(R.id.socialIconFacebook);
        this.imageViewTwitter = findViewById(R.id.socialIconTwitter);
        this.progressBarVK = findViewById(R.id.progressBarVK);
        this.progressBarFacebook = findViewById(R.id.progressBarFacebook);
        this.progressBarTwitter = findViewById(R.id.progressBarTwitter);
        this.editTextMessage = findViewById(R.id.editTextMessage);
        this.buttonSend = findViewById(R.id.buttonSend);
    }

    private void bindViewsListeners() {
        imageViewVK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateVKInfo();
                if (!vkHelper.isSignedIn()) vkHelper.signIn();
                else showVKProfileInfo();
            }
        });
        imageViewFacebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateFacebookInfo();
                if (!facebookHelper.isSignedIn()) facebookHelper.signIn();
                else showFacebookProfileInfo();
            }
        });
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SocialPost post = new SocialPost();
                post.setText(editTextMessage.getText().toString());
                if (vkHelper.isSignedIn()) {
                    vkHelper.post(post, new SocialController.ResponseListener<SocialPost>() {
                        @Override
                        public void onSuccess(SocialPost responsePost) {
                            Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(SocialController.Error error) {
                            Toast.makeText(
                                    MainActivity.this,
                                    error.getCode() + " " + error.getMessage(), Toast.LENGTH_SHORT
                            ).show();
                        }
                    });
                }
                if (facebookHelper.isSignedIn()) {
                    facebookHelper.post(post, new SocialController.ResponseListener<SocialPost>() {
                        @Override
                        public void onSuccess(SocialPost response) {
                            Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(SocialController.Error error) {
                            Toast.makeText(
                                    MainActivity.this,
                                    error.getCode() + " " + error.getMessage(), Toast.LENGTH_SHORT
                            ).show();
                        }
                    });
                }
            }
        });
    }

    private void updateVKInfo() {
        if (!vkHelper.isSignedIn()) {
            imageViewVK.setBorderColor(getResources().getColor(ICON_BORDER_COLOR_INACTIVE));
            imageViewVK.setImageResource(ICON_VK);
            setIsVKLoading(false);
        } else {
            setIsVKLoading(true);
            vkHelper.getUserData(new SocialController.ResponseListener<SocialProfile>() {
                @Override
                public void onSuccess(final SocialProfile responseProfile) {
                    if (responseProfile.getImage() == null) {
                        ImageStorage.get(responseProfile.getImageUrl(), new ImageStorage.DownloadListener() {
                            @Override
                            public void onSuccess(Bitmap image) {
                                responseProfile.setImage(image);
                                imageViewVK.setImageBitmap(image);
                                imageViewVK.setBorderColor(getResources().getColor(ICON_BORDER_COLOR_ACTIVE));
                                setIsVKLoading(false);
                            }

                            @Override
                            public void onError() {
                                Toast.makeText(MainActivity.this, "Ошибка при загрузке аватарки", Toast.LENGTH_SHORT)
                                        .show();
                                imageViewVK.setBorderColor(getResources().getColor(ICON_BORDER_COLOR_ACTIVE));
                                imageViewVK.setImageResource(ICON_NO_AVATAR);
                                setIsVKLoading(false);
                            }
                        });
                    } else {
                        imageViewVK.setImageBitmap(responseProfile.getImage());
                        imageViewVK.setBorderColor(getResources().getColor(ICON_BORDER_COLOR_ACTIVE));
                        setIsVKLoading(false);
                    }
                }

                @Override
                public void onError(SocialController.Error error) {
                    Toast.makeText(MainActivity.this,
                                   error.getCode() + ": " + error.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                    imageViewVK.setBorderColor(getResources().getColor(ICON_BORDER_COLOR_INACTIVE));
                    imageViewVK.setImageResource(ICON_VK);
                    setIsVKLoading(false);
                }
            });
        }
    }

    private void setIsVKLoading(boolean isVKLoading) {
        if (isVKLoading) {
            imageViewVK.setVisibility(View.GONE);
            progressBarVK.setVisibility(View.VISIBLE);
        } else {
            imageViewVK.setVisibility(View.VISIBLE);
            progressBarVK.setVisibility(View.GONE);
        }
    }

    private void showVKProfileInfo() {
        vkHelper.getUserData(new SocialController.ResponseListener<SocialProfile>() {
            @Override
            public void onSuccess(final SocialProfile profile) {
                final View root = getLayoutInflater().inflate(R.layout.layout_social_profile, null);
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("ВКонтакте")
                        .setIcon(R.drawable.vk_icon_light)
                        .setView(root)
                        .setNegativeButton("Закрыть", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                    DialogInterface dialogInterface,
                                    int i
                            )
                            {
                                dialogInterface.dismiss();
                            }
                        });
                final AlertDialog alertDialog = builder.create();
                final ImageView profileIcon = root.findViewById(R.id.profileIcon);
                final TextView value = root.findViewById(R.id.textValue);
                final TextView hint = root.findViewById(R.id.textHint);
                profileIcon.setImageResource(ICON_NO_AVATAR);
                value.setText(profile.getLastName() + " " + profile.getFirstName());
                hint.setVisibility(View.GONE);
                root.findViewById(R.id.buttonLogout).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        vkHelper.signOut();
                        updateVKInfo();
                        alertDialog.dismiss();
                    }
                });
                alertDialog.show();
                if (profile.getImageUrl() != null) {
                    if (profile.getImage() != null) {
                        profileIcon.setImageBitmap(profile.getImage());
                    } else {
                        ImageStorage.get(profile.getImageUrl(), new ImageStorage.DownloadListener() {
                            @Override
                            public void onSuccess(Bitmap image) {
                                profile.setImage(image);
                                profileIcon.setImageBitmap(image);
                            }

                            @Override
                            public void onError() {
                                Toast.makeText(MainActivity.this, "Ошибка при загрузке аватарки", Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
                    }
                }
            }

            @Override
            public void onError(SocialController.Error error) {
                Toast.makeText(MainActivity.this, "Ошибка при загрузке профиля", Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    private void updateFacebookInfo() {
        if (!facebookHelper.isSignedIn()) {
            imageViewFacebook.setImageResource(ICON_FACEBOOK);
            imageViewFacebook.setBorderColor(getResources().getColor(ICON_BORDER_COLOR_INACTIVE));
            setIsFacebookLoading(false);
        } else {
            setIsFacebookLoading(true);
            facebookHelper.getUserData(new SocialController.ResponseListener<SocialProfile>() {
                @Override
                public void onSuccess(final SocialProfile responseProfile) {
                    if (responseProfile.getImage() == null) {
                        if (responseProfile.getImageUrl() != null) {
                            ImageStorage.get(responseProfile.getImageUrl(), new ImageStorage.DownloadListener() {
                                @Override
                                public void onSuccess(Bitmap image) {
                                    responseProfile.setImage(image);
                                    imageViewFacebook.setImageBitmap(image);
                                    imageViewFacebook.setBorderColor(getResources().getColor(ICON_BORDER_COLOR_ACTIVE));
                                    setIsFacebookLoading(false);
                                }

                                @Override
                                public void onError() {
                                    Toast.makeText(MainActivity.this, "Ошибка при загрузке аватарки", Toast.LENGTH_SHORT)
                                            .show();
                                    imageViewFacebook.setBorderColor(getResources().getColor(ICON_BORDER_COLOR_ACTIVE));
                                    imageViewFacebook.setImageResource(ICON_NO_AVATAR);
                                    setIsFacebookLoading(false);
                                }
                            });
                        } else {
                            imageViewFacebook.setImageResource(ICON_NO_AVATAR);
                            imageViewFacebook.setBorderColor(getResources().getColor(ICON_BORDER_COLOR_ACTIVE));
                            setIsFacebookLoading(false);
                        }
                    } else {
                        imageViewFacebook.setImageBitmap(responseProfile.getImage());
                        imageViewFacebook.setBorderColor(getResources().getColor(ICON_BORDER_COLOR_ACTIVE));
                        setIsFacebookLoading(false);
                    }
                }

                @Override
                public void onError(SocialController.Error error) {
                    Toast.makeText(MainActivity.this,
                                   error.getCode() + ": " + error.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                    imageViewVK.setBorderColor(getResources().getColor(ICON_BORDER_COLOR_INACTIVE));
                    imageViewVK.setImageResource(ICON_VK);
                    setIsFacebookLoading(false);
                }
            });
        }
    }

    private void setIsFacebookLoading(boolean isFacebookLoading) {
        if (isFacebookLoading) {
            progressBarFacebook.setVisibility(View.VISIBLE);
            imageViewFacebook.setVisibility(View.GONE);
        } else {
            progressBarFacebook.setVisibility(View.GONE);
            imageViewFacebook.setVisibility(View.VISIBLE);
        }
    }

    private void showFacebookProfileInfo() {
        facebookHelper.getUserData(new SocialController.ResponseListener<SocialProfile>() {
            @Override
            public void onSuccess(final SocialProfile profile) {
                final View root = getLayoutInflater().inflate(R.layout.layout_social_profile, null);
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Facebook")
                        .setIcon(R.drawable.facebook_light_icon)
                        .setView(root)
                        .setNegativeButton("Закрыть", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                    DialogInterface dialogInterface,
                                    int i
                            )
                            {
                                dialogInterface.dismiss();
                            }
                        });
                final AlertDialog alertDialog = builder.create();
                final ImageView profileIcon = root.findViewById(R.id.profileIcon);
                final TextView value = root.findViewById(R.id.textValue);
                final TextView hint = root.findViewById(R.id.textHint);
                profileIcon.setImageResource(ICON_NO_AVATAR);
                value.setText(profile.getLastName() + " " + profile.getFirstName());
                Log.e(TAG, value.getText().toString());
                hint.setVisibility(View.GONE);
                root.findViewById(R.id.buttonLogout).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        facebookHelper.signOut();
                        updateFacebookInfo();
                        alertDialog.dismiss();
                    }
                });
                alertDialog.show();
                if (profile.getImageUrl() != null) {
                    if (profile.getImage() != null) {
                        profileIcon.setImageBitmap(profile.getImage());
                    } else {
                        ImageStorage.get(profile.getImageUrl(), new ImageStorage.DownloadListener() {
                            @Override
                            public void onSuccess(Bitmap image) {
                                profile.setImage(image);
                                profileIcon.setImageBitmap(image);
                            }

                            @Override
                            public void onError() {
                                Toast.makeText(MainActivity.this, "Ошибка при загрузке аватарки", Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
                    }
                }
            }

            @Override
            public void onError(SocialController.Error error) {
                Toast.makeText(MainActivity.this, "Ошибка при загрузке профиля", Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }
}
