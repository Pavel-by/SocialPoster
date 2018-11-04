package com.mairon.socialposter.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.mairon.socialposter.PermissionHelper;
import com.mairon.socialposter.adapter.RVAAttachments;
import com.mairon.socialposter.data.ImageStorage;
import com.mairon.socialposter.R;
import com.mairon.socialposter.adapter.RVAImages;
import com.mairon.socialposter.controller.Facebook;
import com.mairon.socialposter.controller.SocialController;
import com.mairon.socialposter.controller.VK;
import com.mairon.socialposter.data.FileHelper;
import com.mairon.socialposter.model.SocialAttachment;
import com.mairon.socialposter.model.SocialPost;
import com.mairon.socialposter.model.SocialProfile;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.api.VKError;

import java.io.FileNotFoundException;
import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.mairon.socialposter.SocialConst.REQUEST_CAMERA;
import static com.mairon.socialposter.SocialConst.REQUEST_CONFIRM_POST;
import static com.mairon.socialposter.SocialConst.REQUEST_FILE;
import static com.mairon.socialposter.SocialConst.REQUEST_GALLERY;
import static com.mairon.socialposter.SocialConst.REQUEST_PERMISSION_ACCESS_NETWORK_STATE;
import static com.mairon.socialposter.SocialConst.REQUEST_PERMISSION_INTERNET;
import static com.mairon.socialposter.SocialConst.REQUEST_PERMISSION_READ_EXTERNAL_STORAGE;
import static com.mairon.socialposter.SocialConst.REQUEST_VK;
import static com.mairon.socialposter.SocialConst.RESULT_POST_CONFIRMED;
import static com.mairon.socialposter.SocialConst.RESULT_VK_SIGN_OUT;

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
    private AlertDialog dialogLoading;
    private AlertDialog     dialogPickAttachment;
    private RecyclerView    recyclerSelectedImages;
    private RVAAttachments  adapterAttachments;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        bindViews();
        initRecyclerSelectedImages();
        bindViewsListeners();
        initApiHelpers();
        initDialogs();

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
        switch (requestCode) {
            case REQUEST_GALLERY:
                if (resultCode == RESULT_OK) {
                    try {

                        final Uri imageUri = data.getData();
                        final InputStream
                                imageStream = getContentResolver().openInputStream(imageUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        adapterAttachments.addAttachment(new SocialAttachment(selectedImage));
                        dialogPickAttachment.hide();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case REQUEST_CAMERA:
                if (resultCode == RESULT_OK) {
                    try {
                        Bitmap image = (Bitmap) data.getExtras().get("data");
                        adapterAttachments.addAttachment(new SocialAttachment(image));
                        dialogPickAttachment.hide();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case REQUEST_FILE:
                if (resultCode == RESULT_OK) {
                    if (data == null) {
                        return;
                    }

                    Uri selectedFileUri = data.getData();
                    String filePath = FileHelper.getPath(this, selectedFileUri);

                    if (filePath != null && !filePath.equals("")) {
                        adapterAttachments.addAttachment(new SocialAttachment(filePath));
                    } else {
                        Toast.makeText(this, "Не удалось получить файл", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case REQUEST_VK:
                if (resultCode == RESULT_VK_SIGN_OUT) {
                    updateVKInfo();
                }
                break;
            case REQUEST_CONFIRM_POST:
                if (resultCode == RESULT_POST_CONFIRMED) {
                    post();
                }
                break;
            default:
                Toast.makeText(this, "Не удалось определить тап результата", Toast.LENGTH_SHORT)
                        .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    )
    {
        switch (requestCode) {
            case REQUEST_PERMISSION_INTERNET:
            case REQUEST_PERMISSION_ACCESS_NETWORK_STATE:
            case REQUEST_PERMISSION_READ_EXTERNAL_STORAGE:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    dialogPickAttachment.hide();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions,
                                                 grantResults);
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

    private void initDialogs() {
        AlertDialog.Builder dialogBuilder;
        //Диалог выбора приложений
        dialogBuilder = new AlertDialog.Builder(this);

        String[] items = {"Выбрать из галереи", "Сделать фото", "Выбрать документ"};
        dialogBuilder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(
                    DialogInterface dialog,
                    int which
            )
            {
                switch (which) {
                    case 0:
                        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                        photoPickerIntent.setType("image/*");
                        startActivityForResult(photoPickerIntent, REQUEST_GALLERY);
                        break;
                    case 1:
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(cameraIntent, REQUEST_CAMERA);
                        break;
                    case 2:
                        if (!PermissionHelper.hasPermissions(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                            PermissionHelper.requestPermission(
                                    MainActivity.this,
                                    REQUEST_PERMISSION_READ_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                            );
                        } else {
                            Intent intent = new Intent();
                            intent.setType("*/*");
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            startActivityForResult(Intent.createChooser(intent, "Выберите файл"), REQUEST_FILE);
                        }
                }
            }
        })
                .setCancelable(true);
        dialogPickAttachment = dialogBuilder.create();

        //Диалог загрузки
        dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(R.layout.rva_view_item_loading)
                .setCancelable(false);
        dialogLoading = dialogBuilder.create();
    }

    private void bindViews() {
        this.imageViewVK = findViewById(R.id.socialIconVK);
        this.imageViewFacebook = findViewById(R.id.socialIconFacebook);
        this.progressBarVK = findViewById(R.id.progressBarVK);
        this.progressBarFacebook = findViewById(R.id.progressBarFacebook);
        this.editTextMessage = findViewById(R.id.editTextMessage);
        this.buttonSend = findViewById(R.id.buttonSend);
        this.recyclerSelectedImages = findViewById(R.id.recyclerSelectedImages);
    }

    private void initRecyclerSelectedImages() {
        this.adapterAttachments = new RVAAttachments(this, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPickAttachmentDialog();
            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        adapterAttachments.addItem(
                new RVAImages.Item(
                        BitmapFactory.decodeResource(getResources(), R.mipmap.ic_add_gray_full),
                        false
                )
        );

        this.recyclerSelectedImages.setAdapter(adapterAttachments);
        this.recyclerSelectedImages.setLayoutManager(layoutManager);
    }

    private void bindViewsListeners() {
        imageViewVK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateVKInfo();
                if (!vkHelper.isSignedIn()) vkHelper.signIn();
                else startActivityVK();
            }
        });
        imageViewFacebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Coming soon", Toast.LENGTH_SHORT).show();
                /*updateFacebookInfo();
                if (!facebookHelper.isSignedIn()) facebookHelper.signIn();
                else showFacebookProfileInfo();*/
            }
        });
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (canPost()) startConfirmPostActivity();
                else {
                    if (!vkHelper.isSignedIn())
                        showResultDialog("Ошибка", "Необходимо привязать аккаунт");
                    else
                        showResultDialog("Ошибка", "Слишком мало данных для поста");
                }
                //------------FACEBOOK----------------
                /*if (facebookHelper.isSignedIn()) {
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
                }*/
            }
        });
    }

    private void updateVKInfo() {
        if (!vkHelper.isSignedIn()) {
            imageViewVK.setBorderColor(getResources().getColor(ICON_BORDER_COLOR_INACTIVE));
            imageViewVK.setImageResource(ICON_VK);
            setIsVKLoading(false);
        } else {
            Log.e(TAG, "START LOADING");
            setIsVKLoading(true);
            vkHelper.getUserData(new SocialController.ResponseListener<SocialProfile>() {
                @Override
                public void onSuccess(final SocialProfile responseProfile) {
                    Log.e(TAG, "GOT DATA");
                    if (responseProfile.getImage() == null) {
                        ImageStorage.get(responseProfile.getImageUrl(), new ImageStorage.DownloadListener() {
                            @Override
                            public void onSuccess(Bitmap image) {
                                Log.e(TAG, "SET IMAGE");
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

    /*private void showVKProfileInfo() {
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
    }*/

    private boolean canPost() {
        return vkHelper.isSignedIn()
               && (adapterAttachments.getAttachments().size() > 0 || editTextMessage.getText().length() > 0);
    }

    private void startActivityVK() {
        startActivityForResult(new Intent(this, VKActivity.class), REQUEST_VK);
    }

    private void startConfirmPostActivity() {
        startActivityForResult(new Intent(this, ConfirmPostActivity.class), REQUEST_CONFIRM_POST);
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

    private void showPickAttachmentDialog() {

        dialogPickAttachment.show();
    }

    private void showResultDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                            DialogInterface dialogInterface,
                            int i
                    )
                    {
                        dialogInterface.dismiss();
                    }
                });
        builder.create().show();
    }

    private void showLoadingDialog() {
        dialogLoading.show();
    }

    private void hideDialogLoading() {
        dialogLoading.hide();
    }

    private void post() {
        if (vkHelper.isSignedIn()) {
            showLoadingDialog();
            SocialPost post = new SocialPost();
            post.setText(editTextMessage.getText().toString());
            post.setAttachments(adapterAttachments.getAttachments());
            if (vkHelper.isSignedIn()) {
                vkHelper.post(post, new SocialController.ResponseListener<SocialPost>() {
                    @Override
                    public void onSuccess(SocialPost responsePost) {
                        hideDialogLoading();
                        adapterAttachments.clearAttachments();
                        editTextMessage.setText("");
                        showResultDialog("Успешно", "Посты были опубликованы");
                    }

                    @Override
                    public void onError(SocialController.Error error) {
                        hideDialogLoading();
                        showResultDialog("Ошибка", "При публикации постов произошла ошибка");
                    }
                });
            }
        }
        else {
            showResultDialog("Ошибка", "Необходимо войти в аккаунт");
        }
    }
}
