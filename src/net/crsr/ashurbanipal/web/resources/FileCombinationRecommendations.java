package net.crsr.ashurbanipal.web.resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import net.crsr.ashurbanipal.web.exceptions.BadRequest;
import net.crsr.ashurbanipal.web.exceptions.InternalServerException;
import net.crsr.ashurbanipal.web.resources.utilities.ScoredResult;

import org.apache.wink.common.annotations.Workspace;
import org.json.JSONException;
import org.json.JSONObject;

@Path("/file/combination")
@Workspace(workspaceTitle="Text Metrics", collectionTitle="Text topics")
public class FileCombinationRecommendations {
  
  private final FileMetadataLookup metadataLookup;
  private final FileStyleRecommendations styleRecommendations;
  private final FileTopicRecommendations topicRecommendations;
  
  public FileCombinationRecommendations(
      FileMetadataLookup metadataLookup,
      FileStyleRecommendations styleRecommendations,
      FileTopicRecommendations topicRecommendations
      ) {
    this.metadataLookup = metadataLookup;
    this.styleRecommendations = styleRecommendations;
    this.topicRecommendations = topicRecommendations;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public JSONObject getTextList(
      @QueryParam("etext_no") Integer etext_no,
      @QueryParam("start") @DefaultValue("0") Integer start,
      @QueryParam("limit") @DefaultValue("20") Integer limit) {
    if (etext_no == null) {
      throw new BadRequest("bad request: parameter etext_no required");
    }
    
    try {
      final List<ScoredResult> allRows = combinedDistances(etext_no);
      Collections.sort(allRows);
      
      final List<JSONObject> rows = new ArrayList<>(limit);
      for (ScoredResult distance : allRows.subList(start, start + limit)) {
        final JSONObject row = new JSONObject().put("score", distance.score);
        rows.add(row);
        final JSONObject metadata = metadataLookup.getByEtextNo(distance.etext_no);
        for (String key : JSONObject.getNames(metadata)) {
          row.put(key, metadata.get(key));
        }
      }

      return new JSONObject().put("rows", rows);
    } catch (JSONException e) {
      throw new InternalServerException(e);
    }
    
  }

  public List<ScoredResult> combinedDistances(int etext_no) {
    final ScoredResult.ByEtext byEtextComparator = new ScoredResult.ByEtext();
    final List<ScoredResult> styleDistances = styleRecommendations.euclidianDistances(etext_no);
    Collections.sort(styleDistances, byEtextComparator);
    int s = 0;
    final List<ScoredResult> topicDistances = topicRecommendations.topicDistances(etext_no);
    Collections.sort(topicDistances, byEtextComparator);
    int t = 0;
    final List<ScoredResult> results = new ArrayList<>();
    while (s < styleDistances.size() && t < topicDistances.size()) {
      final ScoredResult styleDistance = styleDistances.get(s);
      final ScoredResult topicDistance = topicDistances.get(t);
      if (styleDistance.etext_no < topicDistance.etext_no) {
        ++s;
      } else if (topicDistance.etext_no < styleDistance.etext_no) {
        ++t;
      } else {
        results.add( new ScoredResult(styleDistance.etext_no, styleDistance.score * topicDistance.score) );
        ++s;
        ++t;
      }
    }
    return results;
  }

}
