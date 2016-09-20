package com.lanyuan.supersearch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.handmark.pulltorefresh.library.ILoadingLayout;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final int RESULT_FROM_SETTING = 1;
    public static final int RESULT_SITES = 2;

    FloatingSearchView searchView;
    PullToRefreshListView listView;
    Handler handler;
    BaiduListAdapter adapter;
    List<Baidu> baiduList;
    String q_keyword = "";
    String[] sites;
    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences("Sites_Book", 0);
        shouUpdateInfo();

        initSites();

        searchView = (FloatingSearchView) findViewById(R.id.floating_search_view);
        searchView.setOnSearchListener(searchListener);
        searchView.setOnMenuItemClickListener(menuItemClickListener);

        listView = (PullToRefreshListView) findViewById(R.id.list_v);
        listView.setOnItemClickListener(itemClickListener);

        listView.setMode(PullToRefreshBase.Mode.DISABLED);
        initRefreshView();
        listView.setOnRefreshListener(onRefreshListener);
    }

    private void shouUpdateInfo() {
        String last_version = preferences.getString("last_version","0.0");
        String now_version = preferences.getString("now_version","1.0");
        SharedPreferences.Editor editor = preferences.edit();
        if (!now_version.equals(last_version)){
            AlertDialog.Builder ab = new AlertDialog.Builder(MainActivity.this);
            ab.setTitle("更新");
            ab.setMessage(R.string.update_info);
            ab.setPositiveButton("OK",null);
            ab.show();
            editor.putString("last_version",now_version);
            editor.commit();
        }
    }

    private void initSites() {
        String temp = preferences.getString("sites", "");
        if (temp.equals("")) temp = "baidu.com";
        sites = temp.split(":");
    }

    PullToRefreshBase.OnRefreshListener2 onRefreshListener = new PullToRefreshBase.OnRefreshListener2() {
        @Override
        public void onPullDownToRefresh(PullToRefreshBase refreshView) {

        }

        @Override
        public void onPullUpToRefresh(PullToRefreshBase refreshView) {
            if (!q_keyword.isEmpty()) {
                new getUpdateDataTask().execute();
            } else {
                Snackbar.make(getWindow().getDecorView(), "没有内容", Snackbar.LENGTH_SHORT).show();

            }
        }
    };

    private void initRefreshView() {
        ILoadingLayout layout = listView.getLoadingLayoutProxy(false, true);
        layout.setPullLabel("上拉加载更多");
        layout.setRefreshingLabel("正在加载");
        layout.setReleaseLabel("松开立即加载");
    }

    FloatingSearchView.OnMenuItemClickListener menuItemClickListener = new FloatingSearchView.OnMenuItemClickListener() {
        @Override
        public void onActionMenuItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.setting:
                    startActivityForResult(new Intent(MainActivity.this, SettingActivity.class), RESULT_FROM_SETTING);
                    break;
                case R.id.about:
                    startActivity(new Intent(MainActivity.this,AboutActivity.class));
                    break;
                case R.id.cacel:
                    System.exit(0);
                    break;
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_SITES && requestCode == RESULT_FROM_SETTING) {
            Bundle bundle = data.getBundleExtra("Bundle");
            sites = bundle.getStringArray("SITES");
        }
    }

    FloatingSearchView.OnSearchListener searchListener = new FloatingSearchView.OnSearchListener() {
        @Override
        public void onSuggestionClicked(SearchSuggestion searchSuggestion) {

        }

        @Override
        public void onSearchAction(String currentQuery) {
            q_keyword = currentQuery;
            if (!currentQuery.equals("")){
                getBaiduList(currentQuery);
                listView.setMode(PullToRefreshBase.Mode.PULL_FROM_END);
            }
            else listView.removeAllViews();
        }
    };

    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setData(Uri.parse(baiduList.get(position-1).getReal_url()));
            startActivity(intent);
        }
    };

    private void getBaiduList(final String keyword) {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        baiduList = (List<Baidu>) msg.obj;
                        adapter = new BaiduListAdapter(baiduList, MainActivity.this);
                        listView.setAdapter(adapter);
                }
                super.handleMessage(msg);
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = 1;
                msg.obj = GetBaiduList.getBaiduResult(keyword, sites);
                MainActivity.this.handler.sendMessage(msg);
            }
        }).start();
    }

    private class getUpdateDataTask extends AsyncTask<Void, Void, String[]> {

        @Override
        protected void onPreExecute() {
            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case 1:
                            baiduList.addAll((List<Baidu>) msg.obj);
                            adapter.notifyDataSetChanged();
                            listView.onRefreshComplete();
                    }
                    super.handleMessage(msg);
                }
            };
        }

        @Override
        protected String[] doInBackground(Void... params) {
            Message msg = new Message();
            msg.what = 1;
            msg.obj = GetBaiduList.getBaiduResult(q_keyword, sites);
            MainActivity.this.handler.sendMessage(msg);
            return new String[0];
        }

        @Override
        protected void onPostExecute(String[] strings) {

        }
    }

}
