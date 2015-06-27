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

import net.crsr.ashurbanipal.web.exceptions.BadRequest;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringEncoder;
import org.apache.commons.codec.language.Nysiis;
import org.json.JSONObject;

public class SoundexIndex extends AbstractIndex {
  
  // Soundex encoders from commons-codec:

  // Some, like RefinedSoundex and DoubleMetaphone don't work on non-ASCII characters.
  // Others, like Caverphone and DaitchMokotoffSoundex seem to produce odd results.
  // Others are slower, although except for startup time the difference is unimportant.
  // BeiderMorseEncoder is just bad all the way around.
  
  // Two that seem to work well after a little experimentation are Nysiis and Metaphone.
  private static final StringEncoder soundex = new Nysiis();
//  private static final StringEncoder soundex = new Metaphone();

  public SoundexIndex(Map<Integer,JSONObject> metadataLookup) {
    super(metadataLookup);
    System.out.println(index.size());
  }

  @Override
  protected List<String> stringToWords(String s) {
    try {
      final List<String> results = new ArrayList<>();
      for (String word : s.toLowerCase().split("[^\\p{IsAlphabetic}\\d]+")) {
        final String encoding = soundex.encode(word);
        if (encoding != null && !encoding.isEmpty()) {
          results.add(encoding);
        }
      }
      return results;
    } catch (EncoderException e) {
      throw new BadRequest(e);
    }
  }
}
