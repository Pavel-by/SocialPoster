package com.mairon.socialposter.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mairon.socialposter.R;
import com.mairon.socialposter.model.SocialGroup;
import com.mairon.socialposter.model.vk.VKGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class RVASocialGroups extends RecyclerView.Adapter<RVASocialGroups.SocialGroupViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(
                View v,
                int position
        );
    }

    public interface OnItemLongClickListener {
        boolean OnItemLongClick(
                View v,
                int position
        );
    }

    public interface OnItemDeleteListener {
        boolean onItemDelete(
                RVASocialGroups adapter,
                int index
        );
    }

    private final String TAG                    = "RVASocialGroups";
    private final int    BACKGROUND_CLICKABLE   = R.drawable.list_item_background;
    private final int    BACKGROUND_TRANSPARENT = R.color.transparent;

    private Activity context;

    private ArrayList<Item>         items             = new ArrayList<>();
    @Getter
    @Setter
    private OnItemClickListener     onItemClickListener;
    @Getter
    @Setter
    private OnItemLongClickListener onItemLongClickListener;
    private Item.OnChangeListener   itemOnChangeListener;
    @Getter
    @Setter
    private OnItemDeleteListener    onItemDeleteListener;
    @Getter
    private boolean                 isDeletionEnabled = true;

    public RVASocialGroups(Activity context) {
        this.context = context;
        this.itemOnChangeListener = new Item.OnChangeListener() {
            @Override
            public void onChange(Item item) {
                int position = items.indexOf(item);
                if (position >= 0)
                    notifyItemChanged(position);
            }
        };
    }

    @Override
    public SocialGroupViewHolder onCreateViewHolder(
            ViewGroup parent,
            int viewType
    )
    {
        return new SocialGroupViewHolder(
                context.getLayoutInflater().inflate(R.layout.rva_item_social_group, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(
            SocialGroupViewHolder holder,
            int position
    )
    {
        final Item item = items.get(position);
        holder.image.setImageBitmap(item.getImage());
        holder.value.setText(item.getValue());
        holder.hint.setText(item.getHint());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onItemClickListener != null)
                    onItemClickListener.onItemClick(view, items.indexOf(item));
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (onItemLongClickListener != null)
                    return onItemLongClickListener.OnItemLongClick(view, items.indexOf(item));
                return false;
            }
        });
        holder.buttonDelete.setVisibility(isDeletionEnabled ? View.VISIBLE : View.GONE);
        holder.buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (processItemDelete(items.indexOf(item)))
                    view.setOnClickListener(null);
            }
        });
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
        registerCallbackOnItem(item);
        this.items.add(item);
        notifyItemInserted(items.size() - 1);
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

    public List<Item> getItems() {
        return items;
    }

    public void setDeletionEnabled(boolean deletionEnabled) {
        if (deletionEnabled != isDeletionEnabled) {
            this.isDeletionEnabled = deletionEnabled;
            notifyDataSetChanged();
        }
    }

    private void registerCallbackOnItem(Item item) {
        item.setOnChangeListener(itemOnChangeListener);
    }

    private void unregisterCallbackOnItem(Item item) {
        item.setOnChangeListener(null);
    }

    private boolean processItemDelete(int index) {
        if (onItemDeleteListener == null || onItemDeleteListener.onItemDelete(this, index)) {
            unregisterCallbackOnItem(items.get(index));
            items.remove(index);
            notifyItemRemoved(index);
            return true;
        }
        return false;
    }

    public static class SocialGroupViewHolder extends RecyclerView.ViewHolder {
        private CircleImageView image;
        private TextView        value;
        private TextView        hint;
        private View            buttonDelete;

        public SocialGroupViewHolder(View itemView) {
            super(itemView);
            this.image = itemView.findViewById(R.id.image);
            this.value = itemView.findViewById(R.id.value);
            this.hint = itemView.findViewById(R.id.hint);
            this.buttonDelete = itemView.findViewById(R.id.iconDelete);
        }
    }

    @NoArgsConstructor
    public static class Item {

        interface OnChangeListener {
            void onChange(Item item);
        }

        @Getter(AccessLevel.PRIVATE)
        @Setter(AccessLevel.PRIVATE)
        private OnChangeListener onChangeListener;
        @Getter
        @Setter
        private Object id;
        @Getter
        private Bitmap           image;
        @Getter
        private String           value;
        @Getter
        private String           hint;

        public Item(SocialGroup group) {
            this.image = group.getImage();
            this.value = group.getName();
            this.hint = Integer.toString(group.getMembersCount()) + " участника(ов)";
        }

        public void setImage(Bitmap image) {
            this.image = image;
            notifyChanged();
        }

        public void setValue(String value) {
            this.value = value;
            notifyChanged();
        }

        public void setHint(String hint) {
            this.hint = hint;
            notifyChanged();
        }

        private void notifyChanged() {
            if (this.onChangeListener != null)
                onChangeListener.onChange(this);
        }
    }
}
