package com.mairon.socialposter.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.mairon.socialposter.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView.Adapter для списков, где требуется сгруппировать несколько RecyclerView, объединив в
 * один
 */
public class RVASimpleGroups extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final String TAG = "RVASimpleGroups";

    private final static int itemViewTypeEmptyMessage_Header = -1;

    private ArrayList<Group> groups;
    private Activity context;
    private RecyclerView recycler;
    private int mUnusedViewType = 0;

    private class AdapterDataObserver extends RecyclerView.AdapterDataObserver {

        private Group group;

        AdapterDataObserver(Group group) {
            this.group = group;
        }

        @Override
        public void onChanged() {
            RVASimpleGroups.this.notifyDataSetChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            GroupHelper helper = new GroupHelper(group);
            RVASimpleGroups.this.notifyItemRangeChanged(helper.globalFirstItemOffset + positionStart, itemCount);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            GroupHelper helper = new GroupHelper(group);
            RVASimpleGroups.this.notifyItemRangeInserted(positionStart + helper.globalFirstItemOffset, itemCount);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            GroupHelper helper = new GroupHelper(group);
            RVASimpleGroups.this.notifyItemRangeRemoved(positionStart + helper.globalFirstItemOffset, itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            GroupHelper helper = new GroupHelper(group);
            RVASimpleGroups.this.notifyItemRangeChanged(helper.globalFirstItemOffset, helper.group.getItemCount());
        }
    };

    public RVASimpleGroups(Activity context) {
        this(context, new ArrayList<Group>());
    }

    public RVASimpleGroups(Activity context, ArrayList<Group> items) {
        super();
        this.groups = items;
        this.context = context;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recycler = recyclerView;
    }

    /**
     * Возвращает ТОЛЬКО количество групп
     * @return количество групп
     */
    public int getRawItemCount() {
        return groups.size();
    }

    @Override
    public int getItemCount() {
        int count = 0;
        for (Group g : groups) {
            count += g.getItemCount();
        }
        return count;
        //return groups.size();
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        GroupHelper helper = new GroupHelper(position);
        if (getItemViewType(position) == itemViewTypeEmptyMessage_Header) {
            ViewHolderMessage holderMessage = (ViewHolderMessage) holder;

            if (helper.group.getHeader() != null && helper.group.getHeader().length() > 0) {
                holderMessage.header.setVisibility(View.VISIBLE);
                holderMessage.header.setText(helper.group.getHeader());
            } else {
                holderMessage.header.setVisibility(View.GONE);
            }

            if ( helper.group.isAdapterEmpty()
                    && (helper.group.getEmptyText() != null && helper.group.getEmptyText().length() > 0
                    || helper.group.getButtonEmptyText() != null && helper.group.getButtonEmptyText().length() > 0) ) {
                holderMessage.empty.setVisibility(View.VISIBLE);
                if (helper.group.getEmptyText() != null && helper.group.getEmptyText().length() > 0) {
                    holderMessage.messageEmpty.setVisibility(View.VISIBLE);
                    holderMessage.messageEmpty.setText(helper.group.getEmptyText());
                } else {
                    holderMessage.messageEmpty.setVisibility(View.GONE);
                }

                if (helper.group.getButtonEmptyText() != null && helper.group.getButtonEmptyText().length() > 0) {
                    holderMessage.buttonEmpty.setVisibility(View.VISIBLE);
                    holderMessage.buttonEmpty.setText(helper.group.getButtonEmptyText());
                    holderMessage.buttonEmpty.setOnClickListener(helper.group.getButtonEmptyListener());
                } else {
                    holderMessage.buttonEmpty.setVisibility(View.GONE);
                }
            } else {
                holderMessage.empty.setVisibility(View.GONE);
            }
        } else {
            helper.group.onBindViewHolder(holder, helper.groupItemPosition);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == itemViewTypeEmptyMessage_Header) {
            return new ViewHolderMessage(
                    LayoutInflater.from(context)
                            .inflate(R.layout.comp_rva_simple_group_message_and_header, parent, false)
            );
        } else {
            for (Group group : groups) {
                int localViewType = group.mViewTypesMap.get(viewType, -1);
                if (localViewType >= 0) {
                    return group.onCreateViewHolder(parent, localViewType);
                }
            }
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        GroupHelper helper = new GroupHelper(position);
        Group group = helper.group;
        int localViewType = group.getItemViewType(position - helper.globalFirstItemOffset);
        int localViewTypeIndex = group.mViewTypesMap.indexOfValue(localViewType);

        if (localViewType == itemViewTypeEmptyMessage_Header) {
            //If now we should show empty message that doesn't apply to adapter
            return localViewType;
        }

        if (localViewTypeIndex >= 0) {
            //if we already attach this localType to globalType
            return group.mViewTypesMap.keyAt(localViewTypeIndex);
        }

        group.mViewTypesMap.put(mUnusedViewType, localViewType);
        return mUnusedViewType++;
    }

    public static class ViewHolderMessage extends RecyclerView.ViewHolder {
        public TextView messageEmpty;
        public Button buttonEmpty;
        public TextView header;
        public View empty;


        public ViewHolderMessage(View v) {
            super(v);

            this.empty = v.findViewById(R.id.empty);
            this.header = v.findViewById(R.id.header);
            this.buttonEmpty = v.findViewById(R.id.buttonEmpty);
            this.messageEmpty = v.findViewById(R.id.messageEmpty);
        }
    }

    /**
     * Установить новый массив элементов. Вызов обновления (notifyDataSetChanged()) не требуется.
     *
     * @param groups Массив элементов
     */
    public void setGroups(ArrayList<Group> groups) {
        for (int i = 0; i < groups.size(); i++) {
            groups.get(i).setOnChangeListener(new AdapterDataObserver(groups.get(i)));
        }

        this.groups = groups;
        if (this.recycler == null) return;
        this.recycler.post(new Runnable() {
            @Override
            public void run() {
                RVASimpleGroups.this.notifyDataSetChanged();
            }
        });
    }

    /**
     * Добавить еще одну группу. Обновление адаптера не требуется.
     *
     * @param group Массив Item-ов
     */
    public void add(Group group) {
        group.setOnChangeListener(new AdapterDataObserver(group));
        this.groups.add(group);
        if (this.recycler == null) return;
        this.recycler.post(new Runnable() {
            @Override
            public void run() {
                RVASimpleGroups.this.notifyItemInserted(groups.size() - 1);
            }
        });
    }

    /**
     * Получить группу
     *
     * @param pos Позиция
     * @return Объект группы
     */
    public Group get(int pos) {
        return groups.get(pos);
    }

    /**
     * Удалить группы
     */
    public void clear() {
        this.groups.clear();
    }

    /**
     * This class helps in calculation values relatively one group and groupAdapter
     */
    private class GroupHelper {

        int globalFirstItemOffset = -1;
        Group group;
        int groupIndex = -1;

        int groupItemPosition = -1;
        int globalItemPosition = -1;

        GroupHelper(int globalItemPosition) {
            int offset = 0;
            int groupIndex = 0;

            while (groupIndex < groups.size() && offset + groups.get(groupIndex).getItemCount() < globalItemPosition + 1) {
                offset += groups.get(groupIndex).getItemCount();
                groupIndex += 1;
            }

            this.globalFirstItemOffset = offset;
            this.groupIndex = groupIndex;
            this.group = groups.get(groupIndex);
            this.groupItemPosition = globalItemPosition - offset;
            this.globalItemPosition = globalItemPosition;
        }

        GroupHelper(Group group) {
            this.groupIndex = groups.indexOf(group);

            int offset = 0;
            for (int i = 0; i < groupIndex; i++) {
                offset += groups.get(i).getItemCount();
            }
            this.globalFirstItemOffset = offset;
            this.group = group;
        }
    }

    /**
     * Группа
     */
    public static class Group {

        private SparseIntArray mViewTypesMap = new SparseIntArray();

        private interface OnChangeListener {
            void onChange(Group group);
        }

        private String header = "";
        private int visibility = View.VISIBLE;

        private RecyclerView.Adapter adapter;

        public RecyclerView.Adapter getAdapter() {
            return adapter;
        }

        private String messageEmptyText = "";
        private String buttonEmptyText;
        private View.OnClickListener buttonEmptyListener;
        private RecyclerView.AdapterDataObserver mListener;

        private RecyclerView.AdapterDataObserver adapterDataObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                if (mListener != null) mListener.onChanged();
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                if (mListener != null) mListener.onItemRangeChanged(positionStart + 1, itemCount);
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                if (mListener == null) return;
                mListener.onItemRangeInserted(positionStart + 1, itemCount);
                if (adapter.getItemCount() == itemCount) {
                    mListener.onItemRangeChanged(0, 1);
                }
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                if (mListener == null) return;
                if (adapter.getItemCount() == 0) {
                    /// if adapter is empty it is means that on removing step all items were deleted
                    /// and we should show empty message, so we should keep one item to message
                    mListener.onItemRangeRemoved(positionStart + 1, itemCount);
                    mListener.onItemRangeChanged(0, 1);
                } else {
                    mListener.onItemRangeRemoved(positionStart + 1, itemCount);
                }
            }

            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                if (mListener != null) mListener.onItemRangeMoved(fromPosition + 1, toPosition + 1, itemCount);
            }
        };

        public Group(RecyclerView.Adapter adapter) {
            this(adapter, "");
        }

        public Group(RecyclerView.Adapter adapter, String header) {
            super();
            this.adapter = adapter;
            this.adapter.registerAdapterDataObserver(adapterDataObserver);
            this.header = header;
        }

        private void setOnChangeListener(RecyclerView.AdapterDataObserver mListener) {
            this.mListener = mListener;
        }

        /**
         * Получить текущее значение видимости группы
         *
         * @return Видима ли группа
         */
        public int getVisibility() {
            return visibility;
        }

        /**
         * Установить видимость группы
         *
         * @param visibility Видима ли группа
         */
        public void setVisibility(int visibility) {
            this.visibility = visibility;
            if (mListener != null) {
                mListener.onChanged();
            }
        }

        /**
         * Получить заголовок группы
         *
         * @return Заголовок
         */
        public String getHeader() {
            return header;
        }

        /**
         * Установить заголовок группы
         *
         * @param header Заголовок
         */
        public void setHeader(String header) {
            this.header = header;
        }

        public boolean isAdapterEmpty() {
            return adapter.getItemCount() == 0;
        }

        /**
         * Установить текст, который будет выводиться при пустом recyclerview
         *
         * @param text Текст, который будет выводиться при пустом recyclerview
         */
        public void setEmptyText(String text) {
            this.messageEmptyText = text;
            dispatchEmptyDataChanged();
        }

        /**
         * Получить текст, который будет выводиться при пустом recyclerview
         *
         * @return Текст, который будет выводиться при пустом recyclerview
         */
        public String getEmptyText() {
            return messageEmptyText;
        }

        public String getButtonEmptyText() {
            return buttonEmptyText;
        }

        public void setButtonEmptyText(String buttonEmptyText) {
            this.buttonEmptyText = buttonEmptyText;
            dispatchEmptyDataChanged();
        }

        public View.OnClickListener getButtonEmptyListener() {
            return buttonEmptyListener;
        }

        public void setButtonEmptyListener(View.OnClickListener buttonEmptyListener) {
            this.buttonEmptyListener = buttonEmptyListener;
            dispatchEmptyDataChanged();
        }

        /**
         * Установить параметры кнопки, отображающейся при пустом recyclerView
         *
         * @param text     Текст кнопки
         * @param listener Слушатель нажатия кнопки
         */
        public void setEmptyButton(String text, View.OnClickListener listener) {
            this.buttonEmptyListener = listener;
            this.buttonEmptyText = text;
            dispatchEmptyDataChanged();
        }

        private int getItemCount() {
            if (getVisibility() == View.GONE) {
                return 0;
            }
            return adapter.getItemCount() + 1;
        }

        private int getItemViewType(int position) {
            return position == 0 ? RVASimpleGroups.itemViewTypeEmptyMessage_Header : adapter.getItemViewType(position - 1);
        }

        private void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            adapter.onBindViewHolder(holder, position - 1);
        }

        private RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return adapter.onCreateViewHolder(parent, viewType);
        }

        private void dispatchEmptyDataChanged() {
            if (adapter.getItemCount() == 0 && mListener != null) {
                mListener.onItemRangeChanged(0, 1);
            }
        }
    }
}