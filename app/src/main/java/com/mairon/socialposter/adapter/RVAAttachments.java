package com.mairon.socialposter.adapter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;

import com.mairon.socialposter.R;
import com.mairon.socialposter.model.SocialAttachment;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 *
 */
public class RVAAttachments extends RVAImages {

    private final String TAG = "RVAAttachments";

    private final Bitmap ICON_FILE;
    private final Bitmap ICON_ADD;
    private final Item   ITEM_ADD;

    @Getter
    private List<SocialAttachment> attachments = new ArrayList<>();
    @Setter
    @Getter
    private View.OnClickListener   onAddButtonClickListener;
    @Getter
    private boolean                isAddingAllowed = true;

    public RVAAttachments(Activity activity) {
        super(activity);
        this.ICON_FILE
                = BitmapFactory.decodeResource(activity.getResources(), R.mipmap.ic_text_left_gray);
        this.ICON_ADD
                = BitmapFactory.decodeResource(activity.getResources(), R.mipmap.ic_add_gray_full);
        this.ITEM_ADD = new Item(ICON_ADD, false);
        initItemClickListener();
    }

    public RVAAttachments(
            Activity activity,
            View.OnClickListener onAddButtonClickListener
    )
    {
        super(activity);
        this.ICON_FILE
                = BitmapFactory.decodeResource(activity.getResources(), R.mipmap.ic_text_left_gray);
        this.ICON_ADD
                = BitmapFactory.decodeResource(activity.getResources(), R.mipmap.ic_add_gray_full);
        this.ITEM_ADD = new Item(ICON_ADD, false);
        this.onAddButtonClickListener = onAddButtonClickListener;
        initItemClickListener();
    }

    private void initItemClickListener() {
        super.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(
                    View v,
                    int position
            )
            {
                if (isAddingAllowed() && position == getItemCount() - 1)
                    onAddButtonClickListener.onClick(v);
                else {
                    attachments.remove(position);
                    RVAAttachments.super.remove(position);
                }
            }
        });
    }

    public void addAttachment(SocialAttachment attachment) {
        this.attachments.add(attachment);
        super.addItem(generateItem(attachment), isAddingAllowed() ? getItemCount() - 1 : getItemCount());
    }

    public void setAttachments(List<SocialAttachment> attachments) {
        this.attachments = attachments;
        List<Item> items = new ArrayList<>();
        for (SocialAttachment attachment : attachments) {
            items.add(generateItem(attachment));
        }
        if (isAddingAllowed()) items.add(ITEM_ADD);
        super.setItems(items);
    }

    private Item generateItem(SocialAttachment attachment) {
        Bitmap image;
        switch (attachment.getType()) {
            case FILE:
                image = ICON_FILE;
                break;
            case PHOTO:
                image = attachment.getPhoto();
                break;
            default:
                image = ICON_FILE;
        }
        return new Item(image);
    }

    public void removeAttachment(SocialAttachment attachment) {
        int index = attachments.indexOf(attachment);
        if (index >= 0) {
            attachments.remove(index);
            super.remove(index);
        }
    }

    public void clearAttachments() {
        this.attachments.clear();
        super.setItems(new ArrayList<Item>(){{add(ITEM_ADD);}});
    }

    public void setAddingAllowed(boolean isAddingAllowed) {
        if (this.isAddingAllowed == isAddingAllowed) return;
        if (isAddingAllowed) {
            super.remove(ITEM_ADD);
        } else {
            super.addItem(ITEM_ADD);
        }
    }
}
