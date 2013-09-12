package com.brills.wikee;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;



class ScrollListenedWebView extends WebView {
  static public interface OnScrollChangedListener {
    void onScrollChanged(ScrollListenedWebView view, 
                         int x, int y, int oldx, int oldy);
  }
  
  public ScrollListenedWebView(Context context) {
    super(context);
  }
  
  public ScrollListenedWebView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }
  
  public ScrollListenedWebView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }
  
  public void setOnScrollChangedListener(OnScrollChangedListener listener) {
    this.on_scroll_changed_listener = listener;
  }
  
  @Override
  protected void onScrollChanged(int l, int t, int oldl, int oldt) {
    super.onScrollChanged(l, t, oldl, oldt);
    if (on_scroll_changed_listener != null) {
      on_scroll_changed_listener.onScrollChanged(this, l, t, oldl, oldt);
    }
  }
  
  private OnScrollChangedListener on_scroll_changed_listener = null;
}
