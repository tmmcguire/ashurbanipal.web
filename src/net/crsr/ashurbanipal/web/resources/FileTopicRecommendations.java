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

package net.crsr.ashurbanipal.web.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import net.crsr.ashurbanipal.web.exceptions.ResultNotFound;
import net.crsr.ashurbanipal.web.resources.utilities.AbstractFileRecommendations;
import net.crsr.ashurbanipal.web.resources.utilities.ScoredResult;

import org.apache.wink.common.annotations.Workspace;

@Path("/file/topic")
@Workspace(workspaceTitle="Text Metrics", collectionTitle="Text topics")
public class FileTopicRecommendations extends AbstractFileRecommendations {

  private static final String TOPIC_DATA = "net/crsr/ashurbanipal/web/resources/data/gutenberg.nouns";

  final private Map<Integer,BigInteger> topicSets = new HashMap<>();
  
  public FileTopicRecommendations(FileMetadataLookup metadataLookup) {
    super(metadataLookup);
    BufferedReader br = null;
    try {

      br = new BufferedReader(new InputStreamReader(FileTopicRecommendations.class.getClassLoader().getResourceAsStream(TOPIC_DATA)));

      String line = br.readLine();
      while (line != null) {
        final List<String> list = Arrays.asList(line.split("\t"));
        final int etext_no = Integer.parseInt(list.get(0));
        if (!topicSets.containsKey(etext_no)) {
          // Only retain the first occurrence of an etext. This data needs cleaning.
          BigInteger bitSet = BigInteger.valueOf(0);
          for (String bit : list.subList(1, list.size())) {
            bitSet = bitSet.setBit( Integer.valueOf(bit) );
          }
          topicSets.put(etext_no, bitSet);
        }
        line = br.readLine();
      }
      
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      if (br != null) { try { br.close(); } catch (IOException e) { /* ignore */ } }
    }
  }
  
  // Uses the Jaccard distance between the two topic sets.
  public List<ScoredResult> scoredResults(int etext_no) {
    final List<ScoredResult> results = new ArrayList<>(topicSets.size());
    final BigInteger thisBitSet = topicSets.get(etext_no);
    if (thisBitSet == null) {
      throw new ResultNotFound("No topic data found for Etext #" + etext_no);
    }
    for (Map.Entry<Integer,BigInteger> entry : topicSets.entrySet()) {
      final BigInteger otherBitSet = entry.getValue();
      if (otherBitSet != null) {
        final double intersect = thisBitSet.and(otherBitSet).bitCount();
        final double union = thisBitSet.or(otherBitSet).bitCount();
        results.add(new ScoredResult(entry.getKey(), (union - intersect) / union));
      }
    }
    return results;
  }
  
}
