package com.brills.wikee;

import java.util.ArrayList;

import com.example.wikee.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

class HistoryListAdapter extends ArrayAdapter<String> {
  private final Context context_;
  private final ArrayList<String> history_;
  private final int res_;

  public HistoryListAdapter(Context context, int res, ArrayList<String> history) {
    super(context, res, history);
    history_ = history;
    context_ = context;
    res_ = res;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    LayoutInflater inflater =
        (LayoutInflater) context_.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View row_view = inflater.inflate(res_, parent, false);
    TextView history_text = (TextView) row_view.findViewById(R.id.SessionHistoryText);
    history_text.setText(history_.get(position));
    return row_view;
  }
}
