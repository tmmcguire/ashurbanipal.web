package net.crsr.ashurbanipal.web.indexing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.crsr.ashurbanipal.web.resources.DistanceResult;

import org.json.JSONException;
import org.json.JSONObject;

public class MetadataIndex {

  private static final Map<String,List<DistanceResult>> index = new HashMap<>();
  
  public MetadataIndex(Set<Entry<Integer,JSONObject>> metadataLookup) {
    try {
      for (Entry<Integer,JSONObject> entry : metadataLookup) {
        final Integer etext_no = entry.getKey();
        final JSONObject metadata = entry.getValue();
        final String title = metadata.getString("title");
        final String author = metadata.getString("author");
        final String subject = metadata.getString("subject");

        addWords(etext_no, title, 3);
        addWords(etext_no, author, 2);
        addWords(etext_no, subject, 1);
      }

      final Comparator<DistanceResult> comparator = new DistanceResult.ByEtext();
      for (List<DistanceResult> postings : index.values()) {
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
  
  public List<DistanceResult> getEntries(String keys) {
    final List<String> keywords = stringToWords(keys);
    List<DistanceResult> results = new ArrayList<>();
    int i = 0;
    while (i < keywords.size() && results.isEmpty()) {
      results.addAll( index.getOrDefault(keywords.get(i++), Collections.<DistanceResult> emptyList()) );
    }
    for (String word : keywords.subList(i, keywords.size())) {
      final List<DistanceResult> postings = index.getOrDefault(word, Collections.<DistanceResult> emptyList());
      if (postings != null) {
        results = mergePostings(results, postings);
      }
    }
    return results;
  }

  private List<DistanceResult> mergePostings(List<DistanceResult> results, final List<DistanceResult> postings) {
    final int pSize = postings.size();
    final int rSize = results.size();
    final List<DistanceResult> newResults = new ArrayList<>(rSize + pSize);
    int r = 0;
    int p = 0;
    while (r < rSize && p < pSize) {
      final DistanceResult rResult = results.get(r);
      final DistanceResult pResult = postings.get(p);
      if (rResult.etext_no < pResult.etext_no) {
        ++r;
      } else if (pResult.etext_no < rResult.etext_no) {
        ++p;
      } else {
        newResults.add( new DistanceResult(pResult.etext_no, pResult.distance + rResult.distance) );
        ++r;
        ++p;
      }
    }
    results = newResults;
    return results;
  }
  
  private void unique(List<DistanceResult> results) {
    int i = 0;
    while (i < results.size()) {
      int j = i;
      DistanceResult current = results.get(i);
      while (j < results.size() && results.get(j).etext_no == current.etext_no) {
        ++j;
      }
      if (j != i + 1) {
        final List<DistanceResult> sublist = results.subList(i, j);
        final Integer etext_no = sublist.get(0).etext_no;
        double distance = 0.0;
        for (DistanceResult elt : sublist) {
          distance += elt.distance;
        }
        sublist.clear();
        sublist.add(new DistanceResult(etext_no, distance));
      }
      i = j;
    }
  }

  private void addWords(final Integer etext_no, final String string, int weight) {
    for (String word : stringToWords(string)) {
      List<DistanceResult> postings = index.get(word);
      if (postings == null) {
        postings = new ArrayList<>();
        index.put(word, postings);
      }
      postings.add(new DistanceResult(etext_no, weight));
    }
  }

}
