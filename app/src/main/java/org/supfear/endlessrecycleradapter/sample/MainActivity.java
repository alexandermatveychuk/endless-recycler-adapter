package org.supfear.endlessrecycleradapter.sample;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.supfear.endlessrecycleradapter.EndlessRecyclerAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    RecyclerView recyclerView;
    Adapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = (RecyclerView) findViewById(R.id.recycleView);
        recyclerView.setHasFixedSize(true);

        // example of endless grid view based on RecyclerView wih pending view centered
        final GridLayoutManager lm = new GridLayoutManager(this, 2);
        lm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (mAdapter.getItemViewType(position) == Adapter.ITEM_TYPE_PENDING) {
                    return lm.getSpanCount();
                }
                return 1;
            }
        });

        // example of usual endless list based on RecyclerView
//        LinearLayoutManager lm = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(lm);
        mAdapter = new Adapter(this, Adapter.createPage(0, 20), 0);
        recyclerView.setAdapter(mAdapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class Item {
        public String name;

        public Item(String name) {
            this.name = name;
        }
    }

    public static class Adapter extends EndlessRecyclerAdapter<Item, Adapter.ViewHolder> {

        private static final String TAG = "Adapter";
        private int mLastPage = -1;

        protected Adapter(Context context, ArrayList<Item> objects, int lastPage) {
            super(context, objects);
            mLastPage = lastPage;
        }

        @Override
        protected ViewHolder onCreateView(ViewGroup parent, int position) {
            View v = LayoutInflater.from(getContext()).inflate(R.layout.item, parent, false);
            return new ViewHolder(v);
        }

        @Override
        protected void onBindView(ViewHolder viewHolder, int position) {
            Item item = getItem(position);
            viewHolder.name.setText(item.name);
        }

        @Override
        protected View onPendingViewCreate(ViewGroup parent) {
            return LayoutInflater.from(getContext()).inflate(R.layout.pending, parent, false);
        }

        @Override
        protected Collection<Item> loadNewItems() {
            Log.d(TAG, "loadNewItems: " + mLastPage);
            try {
                Thread.sleep(1000, 0);
            } catch (InterruptedException e) {
                Log.d(TAG, "exception", e);
            }
            List<Item> newList = createPage(++mLastPage, getItemsPerPage());
            if (mLastPage == 10) {
                newList.remove(newList.size() - 1);
            }
            return newList;
        }

        public static ArrayList<Item> createPage(int page, int pageSize) {
            ArrayList<Item> list = new ArrayList<>();
            for (int i = 0; i < pageSize; i++) {
                list.add(new Item("Item " + i + " (page " + page + ")"));
            }
            return list;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            public TextView name;

            public ViewHolder(View itemView) {
                super(itemView);
                name = (TextView) itemView.findViewById(R.id.name);
            }
        }
    }
}
