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

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.crsr.ashurbanipal.web.AshurbanipalWeb;
import net.crsr.ashurbanipal.web.exceptions.InternalServerException;

import org.apache.wink.common.annotations.Workspace;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/file/topic")
@Workspace(workspaceTitle="Text Metrics", collectionTitle="Text topics")
public class FileTopicRecommendations {

  private static final Logger log = LoggerFactory.getLogger(FileTopicRecommendations.class);
  
  private static final String TOPIC_DATA = "net/crsr/ashurbanipal/web/resources/data/gutenberg.nouns";

  final private Map<Integer,BigInteger> topicSets = new HashMap<>();
  
  public FileTopicRecommendations() {
    BufferedReader br = null;
    try {

      br = new BufferedReader(new InputStreamReader(FileStyleRecommendations.class.getClassLoader().getResourceAsStream(TOPIC_DATA)));

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

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public JSONObject getRecommendations(
      @QueryParam("etext_no") Integer etext_no,
      @QueryParam("start") @DefaultValue("0") Integer start,
      @QueryParam("limit") @DefaultValue("20") Integer limit
      ) {
    if (etext_no == null) {
      final String message = "bad request: parameter etext_no=\"search-term\" required";
      log.info(message);
      throw new WebApplicationException(Response.status(BAD_REQUEST).entity(message).build());
    }
    
    try {
      final List<TopicResult> allRows = topicDistances(etext_no);
      Collections.sort(allRows);
      
      final List<JSONObject> rows = new ArrayList<>(limit);
      for (TopicResult distance : allRows.subList(start, start + limit)) {
        final JSONObject row = new JSONObject().put("dist", distance.distance);
        rows.add(row);
        final JSONObject metadata = AshurbanipalWeb.METADATA_LOOKUP.getByEtextNo(distance.etext_no);
        for (String key : JSONObject.getNames(metadata)) {
          row.put(key, metadata.get(key));
        }
      }

      return new JSONObject().put("rows", rows);
    } catch (JSONException e) {
      throw new InternalServerException(e);
    }
  }
  
  // Uses the Jaccard distance between the two topic sets.
  public List<TopicResult> topicDistances(int etext_no) {
    final BigInteger thisBitSet = topicSets.get(etext_no);
    final List<TopicResult> results = new ArrayList<>(topicSets.size());
    for (Map.Entry<Integer,BigInteger> entry : topicSets.entrySet()) {
      final double intersect = thisBitSet.and(entry.getValue()).bitCount();
      final double union = thisBitSet.or(entry.getValue()).bitCount();
      results.add(new TopicResult(entry.getKey(), (union - intersect) / union));
    }
    return results;
  }
  
  public static class TopicResult implements Comparable<TopicResult> {
    public final int etext_no;
    public final double distance;
    
    public TopicResult(int etext_no, double distance) {
      this.etext_no = etext_no;
      this.distance = distance;
    }

    @Override
    public int compareTo(TopicResult other) {
      return Double.compare(this.distance, other.distance);
    }
  }
  
}