package net.crsr.ashurbanipal.web.resources.utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import net.crsr.ashurbanipal.web.exceptions.BadRequest;
import net.crsr.ashurbanipal.web.exceptions.InternalServerException;
import net.crsr.ashurbanipal.web.resources.FileMetadataLookup;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class AbstractFileRecommendations {
  
  protected final FileMetadataLookup metadataLookup;
  
  abstract public List<ScoredResult> scoredResults(int etext_no);

  protected AbstractFileRecommendations(FileMetadataLookup metadata) {
    this.metadataLookup = metadata;
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public JSONObject getRecommendations(
      @QueryParam("etext_no") Integer etext_no,
      @QueryParam("start") @DefaultValue("0") Integer start,
      @QueryParam("limit") @DefaultValue("20") Integer limit
      ) {
    
    if (etext_no == null) {
      throw new BadRequest("bad request: parameter etext_no required");
    }
    
    try {
      final List<ScoredResult> allRows = scoredResults(etext_no);
      Collections.sort(allRows);
      
      final List<JSONObject> rows = new ArrayList<>(limit);
      final int end = Integer.min(start + limit, allRows.size());
      
      if (start < end) {
        for (ScoredResult distance : allRows.subList(start, end)) {
          final JSONObject row = new JSONObject().put("score", distance.score);
          rows.add(row);
          final JSONObject metadata = metadataLookup.getByEtextNo(distance.etext_no);
          
          @SuppressWarnings("unchecked")
          final Iterator<String> keys = metadata.keys();
          while (keys.hasNext()) {
            String key = keys.next();
            row.put(key, metadata.get(key));
          }
        }
      }

      return new JSONObject().put("rows", rows).put("count", allRows.size());
      
    } catch (JSONException e) {
      throw new InternalServerException(e);
    }
  }

}
