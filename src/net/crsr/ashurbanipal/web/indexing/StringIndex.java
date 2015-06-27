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
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

public class StringIndex extends AbstractIndex {

  public StringIndex(Map<Integer,JSONObject> metadataLookup) {
    super(metadataLookup);
  }

  protected List<String> stringToWords(String s) {
    final StringBuilder sb = new StringBuilder(s);
    int cur = sb.indexOf("'");
    while (cur >= 0) {
      int toRemove = Character.charCount(sb.codePointAt(cur));
      sb.delete(cur, cur + toRemove);
      cur = sb.indexOf("'", cur);
    }
    final List<String> results = new ArrayList<>();
    for (String word : sb.toString().split("[^\\p{IsAlphabetic}\\d]+")) {
      results.add( word.toLowerCase() );
    }
    return results;
  }

}
