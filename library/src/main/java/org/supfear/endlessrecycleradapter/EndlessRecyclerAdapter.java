/*
 * Copyright (c) 2015 Alexander Matveychuk
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.supfear.endlessrecycleradapter;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main and only class for performing endless behavior of the {@link RecyclerView} based lists.
 * It uses {@link ArrayList}.
 * Just extend this class and override required methods.
 * {@link #onCreateView} and {@link #onBindView} methods act like
 * {@link RecyclerView.Adapter#onCreateViewHolder} and {@link RecyclerView.Adapter#onBindViewHolder} respectively.
 *
 * @param <T> type of items to be loaded into list
 * @param <VH> type of ViewHolder. Must extend {@link RecyclerView.ViewHolder}
 */
abstract public class EndlessRecyclerAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int ITEM_TYPE_OTHER = 0;
    public static final int ITEM_TYPE_PENDING = 1;

    public static final int DEFAULT_ITEMS_PER_PAGE = 20;
    private static final String TAG = "EndlessRecyclerAdapter";

    private Context mContext;
    private final int mItemsPerPage;
    private ArrayList<T> mList;
    private AtomicBoolean mKeepAppending = new AtomicBoolean(true);
    private LoadNewDataAsyncTask<T> mLoadDataTask = null;

    /**
     * Calls {@link #EndlessRecyclerAdapter(Context, int, ArrayList)} with default value {@link #DEFAULT_ITEMS_PER_PAGE}
     *
     * @param context context
     * @param objects initial list of items
     */
    public EndlessRecyclerAdapter(Context context, ArrayList<T> objects) {
        this(context, DEFAULT_ITEMS_PER_PAGE, objects);
    }

    /**
     * Constructs the instance with required initial values
     *
     * @param context context
     * @param itemsPerPage items per page to check the end of loading
     * @param objects initial list of items
     */
    public EndlessRecyclerAdapter(Context context, int itemsPerPage, ArrayList<T> objects) {
        mContext = context;
        mItemsPerPage = itemsPerPage;
        mList = objects;
    }

    /**
     * Creates view of item and it's view holder.
     * Just like {@link android.support.v7.widget.RecyclerView.Adapter#onCreateViewHolder(android.view.ViewGroup, int)}.
     *
     * @param parent parent view
     * @param position position
     * @return view holder of declared type {@link VH}
     */
    abstract protected VH onCreateView(ViewGroup parent, int position);

    /**
     * Binds view to given position.
     * Just like {@link android.support.v7.widget.RecyclerView.Adapter#onBindViewHolder(android.support.v7.widget.RecyclerView.ViewHolder, int)}.
     *
     * @param vh view holder created in {@link #onCreateView(android.view.ViewGroup, int)}
     * @param position position of item
     */
    abstract protected void onBindView(VH vh, int position);

    /**
     * Creates pending view to show while loading new portion of data
     *
     * @param parent parent view
     * @return pending view
     */
    abstract protected View onPendingViewCreate(ViewGroup parent);

    /**
     * Loads new portion of data in background.
     *
     * @return new data
     */
    abstract protected Collection<T> loadNewItems();

    /**
     * Will be called if an exception occurred while new portion of data being loaded.
     *
     * @param e occurred exception
     * @return false to stop appending new data, true otherwise
     */
    protected boolean onError(Exception e) {
        return false;
    }

    /**
     * Adds given items to list
     *
     * @param items items
     */
    public void addNewItems(Collection<T> items) {
        int insertPosition = mList.size();
        mList.addAll(items);
        notifyItemRangeInserted(insertPosition, items.size());
    }

    /**
     * Sets {@link #mKeepAppending} value and updates pendingView visibility state
     *
     * @param keepAppending new value
     */
    protected void setKeepAppending(boolean keepAppending) {
        if (mKeepAppending.get() != keepAppending) {
            mKeepAppending.set(keepAppending);
            int pendingViewPosition = mList.size();
            if (keepAppending) {
                notifyItemInserted(pendingViewPosition);
            } else {
                notifyItemRemoved(pendingViewPosition);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mKeepAppending.get() ? mList.size() + 1 : mList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mKeepAppending.get() && position == mList.size()) {
            return ITEM_TYPE_PENDING;
        }
        return ITEM_TYPE_OTHER;
    }

    public T getItem(int position) {
        if (getItemViewType(position) == ITEM_TYPE_PENDING) {
            return null;
        }
        return mList.get(position);
    }

    @Override
    public final RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder: viewType = " + viewType);
        if (viewType == ITEM_TYPE_PENDING) {
            return new PendingViewHolder(parent);
        }
        return onCreateView(parent, viewType);
    }

    @Override
    public final void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: position = " + position);
        if (getItemViewType(position) == ITEM_TYPE_PENDING) {
            Log.d(TAG, "onBindViewHolder: type == PENDING");
            if (mLoadDataTask != null) {
                mLoadDataTask.cancel(true);
            }
            mLoadDataTask = new LoadNewDataAsyncTask<T>(this);
            mLoadDataTask.execute();
            return;
        }
        onBindView((VH) holder, position);
    }

    /**
     * Returns current items count per one page
     *
     * @return items per one page
     */
    public int getItemsPerPage() {
        return mItemsPerPage;
    }

    /**
     * Stops appending new data
     */
    public void stopAppending() {
        setKeepAppending(false);
    }

    /**
     * Restarts appending new data
     */
    public void restartAppending() {
        setKeepAppending(true);
    }

    public Context getContext() {
        return mContext;
    }

    private class PendingViewHolder extends RecyclerView.ViewHolder {

        public PendingViewHolder(ViewGroup parent) {
            super(onPendingViewCreate(parent));
        }

    }

    protected static class LoadNewDataAsyncTask<T> extends AsyncTask<Void, Void, Collection<T>> {

        private EndlessRecyclerAdapter<T, ?> mAdapter;
        private Exception mException = null;

        public LoadNewDataAsyncTask(EndlessRecyclerAdapter<T, ?> adapter) {
            mAdapter = adapter;
        }

        @Override
        protected void onPreExecute() {
            mAdapter.setKeepAppending(true);
        }

        @Override
        protected Collection<T> doInBackground(Void... params) {
            try {
                return mAdapter.loadNewItems();
            } catch (Exception e) {
                mException = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(Collection<T> collection) {
            if (mException != null) {
                mAdapter.setKeepAppending(mAdapter.onError(mException));
            } else {
                mAdapter.addNewItems(collection);
                mAdapter.setKeepAppending(!(collection.size() < mAdapter.getItemsPerPage()));
            }
        }
    }

}