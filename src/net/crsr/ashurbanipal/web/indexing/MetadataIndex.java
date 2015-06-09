/*
 * ashurbanipal.web: Java Servlet-based interface to Ashurbanipal data
 * Copyright 2015 Tommy M. McGuire
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package net.crsr.ashurbanipal.web.indexing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.crsr.ashurbanipal.web.resources.utilities.ScoredResult;

import org.json.JSONException;
import org.json.JSONObject;

public class MetadataIndex {

  private static final Map<String,List<ScoredResult>> index = new HashMap<>();
  
  public MetadataIndex(Set<Entry<Integer,JSONObject>> metadataLookup) {
    try {
      for (Entry<Integer,JSONObject> entry : metadataLookup) {
        final Integer etext_no = entry.getKey();
        final JSONObject metadata = entry.getValue();
        addWords(etext_no, metadata.getString("title"), 3);
        addWords(etext_no, metadata.getString("author"), 2);
        addWords(etext_no, metadata.getString("subject"), 1);
      }

      final Comparator<ScoredResult> comparator = new ScoredResult.ByEtext();
      for (List<ScoredResult> postings : index.values()) {
        postings.sort(comparator);
        unique(postings);
      }
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static List<String> stringToWords(String s) {
    final StringBuilder sb = new StringBuilder(s);
    int cur = sb.indexOf("'");
    while (cur >= 0) {
      int toRemove = Character.charCount(sb.codePointAt(cur));
      sb.delete(cur, cur + toRemove);
      cur = sb.indexOf("'", cur);
    }
    final List<String> words = new ArrayList<>();
    for (String word : sb.toString().split("[^\\p{IsAlphabetic}\\d]+")) {
      words.add( word.toLowerCase() );
    }
    return words;
  }
  
  public List<ScoredResult> getEntries(String keys) {
    final List<String> keywords = stringToWords(keys);
    List<ScoredResult> results = new ArrayList<>();
    int i = 0;
    while (i < keywords.size() && results.isEmpty()) {
      results.addAll( index.getOrDefault(keywords.get(i++), Collections.<ScoredResult> emptyList()) );
    }
    for (String word : keywords.subList(i, keywords.size())) {
      results = mergePostings(results, index.getOrDefault(word, Collections.<ScoredResult> emptyList()));
    }
    return results;
  }

  private List<ScoredResult> mergePostings(List<ScoredResult> results, final List<ScoredResult> postings) {
    final int pSize = postings.size();
    int p = 0;
    final int rSize = results.size();
    int r = 0;
    final List<ScoredResult> newResults = new ArrayList<>(Integer.max(rSize, pSize));
    while (r < rSize && p < pSize) {
      final ScoredResult rResult = results.get(r);
      final ScoredResult pResult = postings.get(p);
      if (rResult.etext_no < pResult.etext_no) {
        ++r;
      } else if (pResult.etext_no < rResult.etext_no) {
        ++p;
      } else {
        newResults.add( new ScoredResult(pResult.etext_no, pResult.score + rResult.score) );
        ++r;
        ++p;
      }
    }
    return newResults;
  }
  
  private void unique(List<ScoredResult> results) {
    int i = 0;
    while (i < results.size()) {
      final ScoredResult current = results.get(i);
      int j = i+1;
      while (j < results.size() && results.get(j).etext_no == current.etext_no) {
        ++j;
      }
      if (j != i + 1) {
        final List<ScoredResult> sublist = results.subList(i, j);
        double score = 0.0;
        for (ScoredResult elt : sublist) {
          score += elt.score;
        }
        sublist.clear();
        sublist.add(new ScoredResult(current.etext_no, score));
      }
      ++i;
    }
  }

  private void addWords(final Integer etext_no, final String string, int weight) {
    for (String word : stringToWords(string)) {
      List<ScoredResult> postings = index.get(word);
      if (postings == null) {
        postings = new ArrayList<>();
        index.put(word, postings);
      }
      postings.add(new ScoredResult(etext_no, weight));
    }
  }

}
