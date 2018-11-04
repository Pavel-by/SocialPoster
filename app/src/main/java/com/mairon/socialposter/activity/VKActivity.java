package com.mairon.socialposter.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.mairon.socialposter.data.ImageStorage;
import com.mairon.socialposter.R;
import com.mairon.socialposter.adapter.RVASocialGroups;
import com.mairon.socialposter.controller.SocialController;
import com.mairon.socialposter.controller.VK;
import com.mairon.socialposter.model.SocialProfile;
import com.mairon.socialposter.model.vk.VKGroup;

import java.util.ArrayList;
import java.util.List;

import static com.mairon.socialposter.SocialConst.REQUEST_VK_CHOOSE_GROUP;
import static com.mairon.socialposter.SocialConst.RESULT_VK_SIGN_OUT;

public class VKActivity extends SocialActivity {

    private final String TAG = "VKActivity";

    private Bitmap BITMAP_NO_AVATAR;
    public final static int RESULT_CODE_NOT_SIGNED_IN = 100000;

    private VK vkHelper;
    private ArrayList<VKGroup> groupsToPost;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vkHelper = new VK(this);
        if (!vkHelper.isSignedIn()) {
            setResult(RESULT_CODE_NOT_SIGNED_IN);
            finish();
        } else {
            initImages();
            initActionBar();
            getRecyclerAdapter().setOnItemDeleteListener(new RVASocialGroups.OnItemDeleteListener() {
                @Override
                public boolean onItemDelete(
                        RVASocialGroups adapter,
                        int index
                )
                {
                    groupsToPost.remove(index);
                    vkHelper.setGroupsToPost(groupsToPost);
                    return true;
                }
            });
            reloadRecyclerItems();
            getFab().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startChooseGroupActivity();
                }
            });
        }
    }

    private void initImages() {
        this.BITMAP_NO_AVATAR = BitmapFactory.decodeResource(getResources(), R.drawable.no_avatar_icon);
    }

    private void initActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setIcon(R.mipmap.ic_arrow_back_white);
            actionBar.setHomeAsUpIndicator(R.mipmap.ic_arrow_back_white);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setLogo(R.drawable.vk_icon_light);
            actionBar.setTitle("Куда постить");
            vkHelper.getUserData(new SocialController.ResponseListener<SocialProfile>() {
                @Override
                public void onSuccess(SocialProfile response) {
                    actionBar.setSubtitle(response.getLastName() + " " + response.getFirstName());
                }

                @Override
                public void onError(SocialController.Error error) {
                    Toast.makeText(VKActivity.this, "Не удалось получить данные профиля", Toast.LENGTH_SHORT)
                            .show();
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_vk, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.signOut:
                saveGroupsToPost();
                vkHelper.signOut();
                setResult(RESULT_VK_SIGN_OUT);
                finish();
                return true;
        }
        return false;
    }

    private void reloadRecyclerItems() {
        setEmptyMessage("Нет выбранных групп");
        setIsLoading(true);
        getRecyclerAdapter().removeAll();
        vkHelper.getGroupsToPost(new SocialController.ResponseListener<ArrayList<VKGroup>>() {
            @Override
            public void onSuccess(ArrayList<VKGroup> response) {
                groupsToPost = response;
                List<RVASocialGroups.Item> items = new ArrayList<>();
                for (final VKGroup group : response) {
                    final RVASocialGroups.Item item = new RVASocialGroups.Item(group);
                    if (group.getImage() == null) {
                        item.setImage(BITMAP_NO_AVATAR);
                        ImageStorage.get(group.getImageUrl(), new ImageStorage.DownloadListener() {
                            @Override
                            public void onSuccess(Bitmap image) {
                                group.setImage(image);
                                item.setImage(image);
                            }

                            @Override
                            public void onError() {
                                Toast.makeText(VKActivity.this, "Не удалось загрузить изобрадение группы", Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
                    }
                    items.add(item);
                }
                getRecyclerAdapter().setItems(items);
                setIsLoading(false);
            }

            @Override
            public void onError(SocialController.Error error) {
                Toast.makeText(VKActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                setIsLoading(false);
            }
        });
    }

    private void startChooseGroupActivity() {
        Intent intent = new Intent(this, VKChooseGroupActivity.class);
        intent.putExtra("exclude", vkHelper.getGroupsToPost());
        startActivityForResult(intent, REQUEST_VK_CHOOSE_GROUP);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveGroupsToPost();
    }

    private void saveGroupsToPost() {
        if (vkHelper.isSignedIn()) vkHelper.setGroupsToPost(groupsToPost);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data
    )
    {
        if (requestCode == REQUEST_VK_CHOOSE_GROUP) {
            if (resultCode == RESULT_OK) {
                if (data != null)
                    Log.e(TAG, ""+data.getIntExtra(VKChooseGroupActivity.GROUP_ID, VK.INVALID_GROUP_ID));
                    vkHelper.addGroupToPost(
                            data.getIntExtra(VKChooseGroupActivity.GROUP_ID, VK.INVALID_GROUP_ID)
                    );
                    reloadRecyclerItems();
            }
        }
    }
}
