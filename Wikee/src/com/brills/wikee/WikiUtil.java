package com.brills.wikee;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class WikiUtil {
  private final static Pattern pattern_ = Pattern.compile(".*\\.wikipedia\\.org/wiki/(.+)");
  public static String ExtractKeyFromURL(String url) {
    Matcher m = pattern_.matcher(url);
    if (m.find() && m.groupCount() == 1) {
      return m.group(1);
    }
    return "main_page";
  }
}
