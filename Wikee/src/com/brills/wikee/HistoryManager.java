package com.brills.wikee;

import java.util.ArrayList;
import java.util.HashSet;

// Linear History (no dup) and session independent
class HistoryManager {
  private ArrayList<String> history_;
  private HashSet<String> hhistory_;
  
  public HistoryManager() {
    history_ = new ArrayList<String>();
    hhistory_ = new HashSet<String>();
  }

  public void Append(String key) {
    PreprocessKey(key);
    if (hhistory_.add(key)) {
      history_.add(key);
    }
  }
  
  public ArrayList<String> GetAsList() {
    return history_;
  }
  
  private void PreprocessKey(String key) {
    key = key.replace('_', ' ');
  }
}
