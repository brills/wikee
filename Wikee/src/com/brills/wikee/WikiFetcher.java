package com.brills.wikee;

import java.io.IOException;
import java.net.URLEncoder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import android.net.Uri;

class WikiFetcher {
  private static final String WikiURLFormat = "http://%s.m.wikipedia.org/wiki/";
  private static final String DefaultLang = "en";

  private void RewriteHTML(Document doc) {
    Elements wiki_header = doc.select(".header");
    wiki_header.attr("style", "Display: none;");
  }

  private String ErrorPage() {
    // TODO(zhuo):
    // fill this
    return "Error";
  }

  public String FetchWiki(String keyword, String lang) {
    if (lang.isEmpty()) {
      lang = DefaultLang;
    }
    try {
      Document doc =
          Jsoup.connect(String.format(WikiURLFormat, lang) + Uri.encode(keyword)).get();
      RewriteHTML(doc);
      return doc.outerHtml();
    } catch (Exception e) {
      e.printStackTrace();
      return ErrorPage();
    }
  }
}
