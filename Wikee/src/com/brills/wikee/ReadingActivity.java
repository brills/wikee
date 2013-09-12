package com.brills.wikee;

import com.example.wikee.R;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import android.os.AsyncTask;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;

public class ReadingActivity extends Activity {

  private HistoryManager history_manager_ = null;
  private SessionManager session_manager_ = null;
  private HistoryListAdapter history_list_adapter_ = null;

  private class FetchWikiTask extends AsyncTask<String, Integer, String> {
    private static final String BaseURLPattern = "http://%s.m.wikipedia.org/";
    private String BaseURL = "http://en.m.wikipedia.org";

    @Override
    protected void onPreExecute() {
      setProgressBarIndeterminateVisibility(true);
    }

    @Override
    protected String doInBackground(String... params) {
      String result;
      if (params.length == 2) {
        BaseURL = String.format(BaseURLPattern, params[1]);
        result = new WikiFetcher().FetchWiki(params[0], params[1]);
        publishProgress(100);
      } else {
        result = new WikiFetcher().FetchWiki(params[0], "");
        publishProgress(100);
      }
      return result;
    }

    @Override
    protected void onPostExecute(String result) {
      ((WebView) findViewById(R.id.WikiView)).loadDataWithBaseURL(BaseURL, result, "text/html",
          null, null);
      setProgressBarIndeterminateVisibility(false);
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ConfigActionBar();
    setContentView(R.layout.activity_reading);

    history_manager_ = new HistoryManager();
    ConfigSlidingMenu();
    ConfigWikiView();


    handleIntent(getIntent());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
    SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
    searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(
        getApplicationContext(), ReadingActivity.class)));
    return super.onCreateOptionsMenu(menu);
  }

  private void ConfigActionBar() {
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    ActionBar actionBar = getActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
  }

  private void handleIntent(Intent intent) {
    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
      String query = intent.getStringExtra(SearchManager.QUERY); 
      // session_manager_ = new SessionManager(query, this);
      ProcessQuery(query);
    }
  }

  private void ProcessQuery(String query) {
    if (history_list_adapter_ != null) {
      history_list_adapter_.notifyDataSetChanged();
    }
      history_manager_.Append(query);
      // session_manager_.NewQueryInSession(query);
      new FetchWikiTask().execute(query);
  }

  private void ConfigSlidingMenu() {
    final SlidingMenu menu = new SlidingMenu(this);
    menu.setMode(SlidingMenu.LEFT);
    menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
    menu.setShadowWidthRes(R.dimen.slidingmenu_shadow_width);
    menu.setShadowDrawable(R.drawable.slidingmenu_shadow);
    menu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
    //menu.setBehindOffset(100);
    menu.setFadeDegree(1.0f);
    menu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
    // Set the real menu
    menu.setMenu(R.layout.reading_sliding_menu);
    ListView history_list_view = (ListView) findViewById(R.id.SessionHistoryListView);
    history_list_adapter_ = new HistoryListAdapter(this, R.layout.session_history_list_item,
      history_manager_.GetAsList());
    history_list_view.setAdapter(history_list_adapter_);
    history_list_view.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ProcessQuery(history_manager_.GetAsList().get(position));
        menu.toggle();
      }
    });
  }

  @SuppressLint("SetJavaScriptEnabled")
  private void ConfigWikiView() {
    ScrollListenedWebView wiki_view = (ScrollListenedWebView) findViewById(R.id.WikiView);
    WebSettings settings = wiki_view.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setSupportZoom(false);
    wiki_view.setWebViewClient(new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        ProcessQuery(WikiUtil.ExtractKeyFromURL(url));
        return true;
      }
    });
  }
}
