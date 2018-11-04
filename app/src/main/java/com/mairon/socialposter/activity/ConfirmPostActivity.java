package com.mairon.socialposter.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.mairon.socialposter.R;
import com.mairon.socialposter.adapter.RVASimpleGroups;
import com.mairon.socialposter.adapter.RVASocialGroups;
import com.mairon.socialposter.controller.SocialController;
import com.mairon.socialposter.controller.VK;
import com.mairon.socialposter.data.ImageStorage;
import com.mairon.socialposter.model.SocialProfile;
import com.mairon.socialposter.model.vk.VKGroup;

import java.util.ArrayList;
import java.util.List;

import static com.mairon.socialposter.SocialConst.RESULT_POST_CONFIRMED;
import static com.mairon.socialposter.SocialConst.RESULT_POST_NOT_CONFIRMED;

public class ConfirmPostActivity extends AppCompatActivity {

    private final String TAG = "ConfirmPostActivity";
    private       Bitmap BITMAP_NO_AVATAR;

    private VK                   vkHelper;
    private RecyclerView         recyclerView;
    private FloatingActionButton fab;
    private RVASimpleGroups      adapterWrapper;
    private RVASocialGroups      adapterVK;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social);

        initResources();
        initApiHelpers();
        initActionBar();
        bindViews();
        initRecycler();
        initRecyclerItems();

        ((View) this.fab).setVisibility(View.GONE);
    }

    private void initResources() {
        this.BITMAP_NO_AVATAR
                = BitmapFactory.decodeResource(getResources(), R.drawable.no_avatar_icon);
    }

    private void initApiHelpers() {
        this.vkHelper = new VK(this);
    }

    private void bindViews() {
        this.recyclerView = findViewById(R.id.recyclerView);
        this.fab = findViewById(R.id.fab);
    }

    private void initRecycler() {
        this.adapterVK = new RVASocialGroups(this);
        this.adapterWrapper = new RVASimpleGroups(this);

        RVASimpleGroups.Group vkGroup = new RVASimpleGroups.Group(adapterVK);
        vkGroup.setHeader("ВКонтакте");
        vkGroup.setEmptyText("Не выбрано групп для загрузки");
        this.adapterVK.setDeletionEnabled(false);
        this.adapterWrapper.add(vkGroup);

        this.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        this.recyclerView.setAdapter(adapterWrapper);
    }

    private void initActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setIcon(R.mipmap.ic_arrow_back_white);
            actionBar.setHomeAsUpIndicator(R.mipmap.ic_arrow_back_white);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setLogo(R.drawable.vk_icon_light);
            actionBar.setTitle("Подтверждение");
        }
    }

    private void initRecyclerItems() {
        if (vkHelper.isSignedIn()) {
            vkHelper.getGroupsToPost(new SocialController.ResponseListener<ArrayList<VKGroup>>() {
                @Override
                public void onSuccess(final ArrayList<VKGroup> response) {
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
                                    Toast.makeText(ConfirmPostActivity.this, "Не удалось загрузить изобрадение группы", Toast.LENGTH_SHORT)
                                            .show();
                                }
                            });
                        }
                        items.add(item);
                    }
                    adapterVK.setItems(items);
                }

                @Override
                public void onError(SocialController.Error error) {
                    Toast.makeText(ConfirmPostActivity.this, "Не удалось загрузить картинку", Toast.LENGTH_SHORT)
                            .show();
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_confirm_post, menu);
        if (vkHelper.getGroupsToPost().length() == 0) menu.getItem(0).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.post:
                setResult(RESULT_POST_CONFIRMED);
                finish();
                return true;
        }
        return false;
    }

    @Override
    public boolean onSupportNavigateUp() {
        setResult(RESULT_POST_NOT_CONFIRMED);
        finish();
        return true;
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_POST_NOT_CONFIRMED);
        finish();
    }
}
