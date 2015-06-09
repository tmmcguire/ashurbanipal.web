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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.crsr.ashurbanipal.web.exceptions.InternalServerException;
import net.crsr.ashurbanipal.web.exceptions.ResultNotFound;
import net.crsr.ashurbanipal.web.indexing.MetadataIndex;
import net.crsr.ashurbanipal.web.resources.utilities.ScoredResult;

import org.apache.wink.common.annotations.Workspace;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/file/lookup")
@Workspace(workspaceTitle="Text Lookup", collectionTitle="Author, Title, Subject query")
public class FileMetadataLookup {

  private static final String METADATA = "net/crsr/ashurbanipal/web/resources/data/gutenberg.metadata";

  private static final Logger log = LoggerFactory.getLogger(FileMetadataLookup.class);
  
  final Map<Integer,JSONObject> metadata = new HashMap<>();
  final MetadataIndex index;

  public FileMetadataLookup() {
    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader(FileMetadataLookup.class.getClassLoader().getResourceAsStream(METADATA)));

      String line = br.readLine();
      final String[] columns = line.split("\t");

      line = br.readLine();
      while (line != null) {
        final String[] values = line.split("\t");
        final int etext_no = Integer.parseInt(values[0]);
        if (!metadata.containsKey(etext_no)) {
          // Only retain the first occurrence of an etext. This data needs cleaning.
          final JSONObject record = new JSONObject().put("etext_no", etext_no);
          metadata.put(etext_no, record);
          for (int i = 1; i < values.length; ++i) {
            record.put(columns[i], values[i]);
          }
        }
        line = br.readLine();
      }
      
      index = new MetadataIndex(metadata.entrySet());
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    } finally {
      if (br != null) { try { br.close(); } catch (IOException e) { /* ignore */ } }
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public JSONObject getQuery(
      @QueryParam("query") String searchTerm,
      @QueryParam("start") @DefaultValue("0") Integer start,
      @QueryParam("limit") @DefaultValue("20") Integer limit) {
    try {
      if (searchTerm == null) {
        final String message = "bad request: parameter query=\"search-term\" required";
        log.info(message);
        throw new WebApplicationException(Response.status(BAD_REQUEST).entity(message).build());
      }

      final List<ScoredResult> allRows = index.getEntries(searchTerm);
      Collections.sort(allRows, new ScoredResult.Inverse());
      
      final List<JSONObject> rows = new ArrayList<>(limit);
      final int end = Integer.min(start + limit, allRows.size());
      for (ScoredResult distance : allRows.subList(start, end)) {
        final JSONObject row = new JSONObject().put("score", distance.score);
        rows.add(row);
        final JSONObject metadata = getByEtextNo(distance.etext_no);
        for (String key : JSONObject.getNames(metadata)) {
          row.put(key, metadata.get(key));
        }
      }

      final JSONObject results = new JSONObject();
      results.put("count", allRows.size());
      results.put("rows", rows);
      return results;
    } catch (JSONException e) {
      throw new InternalServerException(e);
    }
  }
  
  @Path("/{etext_no}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public JSONObject getByEtextNo(@PathParam("etext_no") Integer etext_no) {
    if (etext_no == null) {
      final String message = "bad request: etext_no required";
      log.info(message);
      throw new WebApplicationException(Response.status(BAD_REQUEST).entity(message).build());
    }
    final JSONObject result = metadata.get(etext_no);
    if (result != null) {
      return result;
    } else {
      throw new ResultNotFound("Unrecognized etext_no: " + etext_no);
    }
  }
  
  public Set<Entry<Integer,JSONObject>> entrySet() {
    return metadata.entrySet();
  }
}
