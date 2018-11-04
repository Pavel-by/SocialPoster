package com.mairon.socialposter.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.mairon.socialposter.R;
import com.mairon.socialposter.adapter.RVASimpleGroups;
import com.mairon.socialposter.adapter.RVASocialGroups;

import lombok.Getter;
import lombok.experimental.PackagePrivate;
import me.mvdw.recyclerviewmergeadapter.adapter.ViewAdapter;

public abstract class SocialActivity extends AppCompatActivity {

    interface OnLastItemShowedListener {
        void onLastItemShowed();
    }

    private final String                TAG = "SocialActivity";
    @Getter()
    private       RecyclerView          recyclerView;
    private       RVASocialGroups       recyclerAdapterGroups;
    private       RVASimpleGroups       recyclerAdapterWrapper;
    private       RVASimpleGroups.Group groupMain;
    private       RVASimpleGroups.Group groupLoading;
    private       ViewAdapter           recyclerAdapterLoading;
    private       FloatingActionButton  fab;

    private View                     loadingView;
    private OnLastItemShowedListener onLastItemShowedListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social);
        bindViews();
        initRecycler();
    }

    private void bindViews() {
        this.recyclerView = findViewById(R.id.recyclerView);
        this.fab = findViewById(R.id.fab);
    }

    private void initRecycler() {
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        recyclerAdapterGroups = new RVASocialGroups(this);
        recyclerAdapterWrapper = new RVASimpleGroups(this);
        recyclerAdapterLoading = new ViewAdapter();

        loadingView
                = getLayoutInflater().inflate(R.layout.rva_view_item_loading, recyclerView, false);
        recyclerAdapterLoading.addView(loadingView);

        groupMain = new RVASimpleGroups.Group(recyclerAdapterGroups);
        groupLoading = new RVASimpleGroups.Group(recyclerAdapterLoading);
        recyclerAdapterWrapper.add(groupMain);
        recyclerAdapterWrapper.add(groupLoading);

        recyclerView.setAdapter(recyclerAdapterWrapper);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(
                    @NonNull RecyclerView recyclerView,
                    int newState
            )
            {
                if (layoutManager.findLastVisibleItemPosition() ==
                    recyclerAdapterWrapper.getItemCount() - 1)
                {
                    notifyLastItemShowed();
                }
            }
        });
    }

    public void setIsLoading(boolean isLoading) {
        groupLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    public void setEmptyMessage(String text) {
        groupMain.setEmptyText(text);
    }

    private void notifyLastItemShowed() {
        if (onLastItemShowedListener != null)
            onLastItemShowedListener.onLastItemShowed();
    }

    public RVASocialGroups getRecyclerAdapter() {
        return recyclerAdapterGroups;
    }

    public void setOnLastItemShowedListener(OnLastItemShowedListener onLastItemShowedListener) {
        this.onLastItemShowedListener = onLastItemShowedListener;
    }

    public FloatingActionButton getFab() {
        return fab;
    }
}
