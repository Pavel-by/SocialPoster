package com.mairon.socialposter.adapter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.mairon.socialposter.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

public class RVAImages extends RecyclerView.Adapter<RVAImages.ImageViewHolder> {

    private final String TAG = "RVAImages";

    public interface OnItemClickListener {
        void onItemClick(
                View v,
                int position
        );
    }

    @Getter
    private Activity activity;

    @Getter
    private List<Item> items = new ArrayList<>();
    private Item.OnChangeListener   itemOnChangeListener;
    @Getter
    @Setter
    private OnItemClickListener onItemClickListener;

    public RVAImages(Activity activity) {
        this.activity = activity;
        itemOnChangeListener = new Item.OnChangeListener() {
            @Override
            public void onChange(Item item) {
                int position = items.indexOf(item);
                if (position >= 0)
                    notifyItemChanged(position);
            }
        };
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(
            @NonNull ViewGroup viewGroup,
            int i
    )
    {
        return new ImageViewHolder(activity.getLayoutInflater()
                                           .inflate(R.layout.rva_item_image, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(
            @NonNull ImageViewHolder holder,
            int i
    )
    {
        final Item item = items.get(i);
        holder.imageView.setImageBitmap(item.getImage());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchOnItemClick(view, items.indexOf(item));
            }
        });
        holder.closeMask.setVisibility(item.isMaskVisible() ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }


    public void setItems(Collection<Item> items) {
        removeAll();
        addItems(items);
    }

    public void addItem(Item item) {
        addItem(item, items.size());
        /*registerCallbackOnItem(item);
        this.items.add(item);
        notifyItemInserted(items.size() - 1);*/
    }

    public void addItem(Item item, int position) {
        try {
            this.items.add(position, item);
            registerCallbackOnItem(item);
            notifyItemInserted(position);
            Log.e(TAG, "Item added");
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getLocalizedMessage());
        }
    }

    public void addItems(Collection<Item> itemsCollection) {
        for (Item item : itemsCollection) {
            registerCallbackOnItem(item);
        }
        this.items.addAll(itemsCollection);
        notifyItemRangeInserted(this.items.size() - itemsCollection.size(), itemsCollection.size());
    }

    public Item remove(int position) {
        Item item = items.remove(position);
        unregisterCallbackOnItem(item);
        notifyItemRemoved(position);
        return item;
    }

    public boolean remove(Item item) {
        int position = items.indexOf(item);
        if (items.remove(item)) {
            unregisterCallbackOnItem(item);
            notifyItemRemoved(position);
            return true;
        }
        return false;
    }

    public void removeAll() {
        for (Item item : items) {
            unregisterCallbackOnItem(item);
        }
        int count = items.size();
        this.items.clear();
        notifyItemRangeRemoved(0, count);
    }


    private void registerCallbackOnItem(Item item) {
        item.setOnChangeListener(itemOnChangeListener);
    }

    private void unregisterCallbackOnItem(Item item) {
        item.setOnChangeListener(null);
    }

    private void dispatchOnItemClick(View v, int i) {
        if (onItemClickListener != null)
            onItemClickListener.onItemClick(v, i);
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageView;
        private ImageView closeMask;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            this.imageView = itemView.findViewById(R.id.imageView);
            this.closeMask = itemView.findViewById(R.id.closeMask);
        }
    }

    @NoArgsConstructor
    public static class Item {

        interface OnChangeListener {
            void onChange(Item item);
        }

        @Getter
        private Bitmap image;
        @Getter(AccessLevel.PRIVATE)
        @Setter(AccessLevel.PRIVATE)
        private OnChangeListener onChangeListener;
        @Getter
        private boolean isMaskVisible = true;

        public Item(Bitmap image) {
            this.image = image;
        }

        public Item(Bitmap image, boolean isMaskVisible) {
            this.image = image;
            this.isMaskVisible = isMaskVisible;
        }

        public void setImage(Bitmap image) {
            this.image = image;
            notifyChanged();
        }

        public void setMaskVisible(boolean isMaskVisible) {
            this.isMaskVisible = isMaskVisible;
            notifyChanged();
        }

        private void notifyChanged() {
            if (this.onChangeListener != null)
                onChangeListener.onChange(this);
        }
    }
}
