package net.crsr.ashurbanipal.web.indexing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.crsr.ashurbanipal.web.resources.utilities.ScoredResult;

import org.json.JSONException;
import org.json.JSONObject;

// A simple spelling corrector index search, based on Peter Norvig's How to Write a Spelling Corrector (http://norvig.com/spell-correct.html).
//
// This does not work very well, although slightly better than the raw StringIndex.
public class SpellingIndex extends StringIndex {
  
  final private Map<String,Integer> model = new HashMap<String,Integer>();
  
  public SpellingIndex(Map<Integer,JSONObject> metadataLookup) throws JSONException {
    super(metadataLookup);
    
    // Train spelling model with word frequencies
    for (Entry<Integer,JSONObject> entry : metadataLookup.entrySet()) {
      for (String key : Arrays.asList("title", "author", "subject")) {
        for (String word : stringToWords(entry.getValue().getString(key))) {
          model.put(word, model.getOrDefault(word, 1) + 1);
        }
      }
    }
  }
  
  public List<ScoredResult> getEntries(String keys) {
    List<ScoredResult> results = new ArrayList<>();
    final List<String> words = stringToWords(keys);
    for (String word : words) {
      if (model.containsKey(word)) {
        results = acceptOrMergePostings(results, word);
      } else {
        final Set<String> edits1 = edits1(word);
        final Set<String> knownEdits1 = known(edits1, model);
        if (!knownEdits1.isEmpty()) {
          results = acceptOrMergePostings(results, max(knownEdits1));
        } else {
          final Set<String> knownEdits2 = edits2(edits1, model);
          if (!knownEdits2.isEmpty()) {
            results = acceptOrMergePostings(results, max(knownEdits2));
          }
        }
      }
      // An alternative implementation: simply collect the 0, 1, or 2 step edits and pick the most likely.
      // Doesn't work either.
      //    Set<String> edits = new HashSet<>();
      //    edits.add(word);
      //    final Set<String> edits1 = edits1(word);
      //    edits.addAll( known(edits1, model) );
      //    edits.addAll( edits2(edits1, model) );
      //    results = acceptOrMergePostings(results, max(edits));
    }
    return results;
  }
  
  private String max(Set<String> words) {
    return Collections.max(words, new StringProbabilityComparator());
  }
  
  private class StringProbabilityComparator implements Comparator<String> {
    @Override
    public int compare(String left, String right) {
      return Integer.compare(model.getOrDefault(left, 1), model.getOrDefault(right, 1));
    }
  }

  private static Set<String> edits1(String word) {
    final List<Pair> splits = splits(word);
    final Set<String> edits = new HashSet<>();
    edits.addAll(deletes(splits));
    edits.addAll(transposes(splits));
    edits.addAll(replaces(splits));
    edits.addAll(inserts(splits));
    return edits;
  }
  
  private static Set<String> edits2(Set<String> edits1, Map<String,?> knownWords) {
    final Set<String> edits = new HashSet<>();
    for (String word1 : edits1) {
      for (String word2 : edits1(word1)) {
        if (knownWords.containsKey(word2)) {
          edits.add(word2);
        }
      }
    }
    return edits;
  }

  // Split word in all possible locations.
  private static List<Pair> splits(String word) {
    final List<Pair> splits = new ArrayList<>();
    for (int i = 0; i < word.length() + 1; ++i) {
      final String left  = i > 0 && i <= word.length() ? word.substring(0, i) : "";
      final String right = i <= word.length()          ? word.substring(i)    : "";
      splits.add(new Pair(left,right));
    }
    return splits;
  }
  
  // Return all possible words with one character deleted.
  private static List<String> deletes(List<Pair> splits) {
    final List<String> deletes = new ArrayList<>();
    for (Pair pair : splits) {
      if (!pair.right.isEmpty()) {
        deletes.add( pair.left + pair.right.substring(1) );
      }
    }
    return deletes;
  }
  
  private static List<String> transposes(List<Pair> splits) {
    final List<String> transposes = new ArrayList<>();
    for (Pair pair : splits) {
      if (pair.right.length() > 1) {
        transposes.add( pair.left + pair.right.charAt(1) + pair.right.charAt(0) + pair.right.substring(2) );
      }
    }
    return transposes;
  }

  // Additional letters are used by replaces and inserts.
  private static final String alphabet = "abcdefghijklmnopqrstuvwxyz";
  private static final char[] characters = new char[alphabet.length()];
  static {
    alphabet.getChars(0, alphabet.length(), characters, 0);
  }
  
  private static List<String> replaces(List<Pair> splits) {
    final List<String> replaces = new ArrayList<>();
    for (Pair pair : splits) {
      if (!pair.right.isEmpty()) {
        for (char ch : characters) {
          replaces.add( pair.left + ch + pair.right.substring(1) );
        }
      }
    }
    return replaces;
  }
  
  private static List<String> inserts(List<Pair> splits) {
    final List<String> inserts = new ArrayList<>();
    for (Pair pair : splits) {
      for (char ch : characters) {
        inserts.add( pair.left + ch + pair.right );
      }
    }
    return inserts;
  }
  
  private static Set<String> known(Set<String> words, Map<String,?> knownWords) {
    final Set<String> known = new HashSet<>();
    for (String word : words) {
      if (knownWords.containsKey(word)) {
        known.add(word);
      }
    }
    return known;
  }
  
  private static class Pair {
    public final String left;
    public final String right;
    
    public Pair(String left, String right) {
      this.left = left;
      this.right = right;
    }
    
    public String toString() {
      return String.format("(%s,%s)", left, right);
    }
  }
}
