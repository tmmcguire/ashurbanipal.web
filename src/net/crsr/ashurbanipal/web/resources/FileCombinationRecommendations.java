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

import net.crsr.ashurbanipal.web.AshurbanipalWeb;
import net.crsr.ashurbanipal.web.exceptions.BadRequest;
import net.crsr.ashurbanipal.web.exceptions.InternalServerException;

import org.apache.wink.common.annotations.Workspace;
import org.json.JSONException;
import org.json.JSONObject;

@Path("/file/combination")
@Workspace(workspaceTitle="Text Metrics", collectionTitle="Text topics")
public class FileCombinationRecommendations {

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
      final List<DistanceResult> allRows = combinedDistances(etext_no);
      Collections.sort(allRows);
      
      final List<JSONObject> rows = new ArrayList<>(limit);
      for (DistanceResult distance : allRows.subList(start, start + limit)) {
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

  public List<DistanceResult> combinedDistances(int etext_no) {
    final DistanceResult.ByEtext byEtextComparator = new DistanceResult.ByEtext();
    final List<DistanceResult> styleDistances = AshurbanipalWeb.STYLE_RECOMMENDATIONS.euclidianDistances(etext_no);
    Collections.sort(styleDistances, byEtextComparator);
    int s = 0;
    final List<DistanceResult> topicDistances = AshurbanipalWeb.TOPIC_RECOMMENDATIONS.topicDistances(etext_no);
    Collections.sort(topicDistances, byEtextComparator);
    int t = 0;
    final List<DistanceResult> results = new ArrayList<>();
    while (s < styleDistances.size() && t < topicDistances.size()) {
      final DistanceResult styleDistance = styleDistances.get(s);
      final DistanceResult topicDistance = topicDistances.get(t);
      if (styleDistance.etext_no < topicDistance.etext_no) {
        ++s;
      } else if (topicDistance.etext_no < styleDistance.etext_no) {
        ++t;
      } else {
        results.add( new DistanceResult(styleDistance.etext_no, styleDistance.distance * topicDistance.distance) );
        ++s;
        ++t;
      }
    }
    return results;
  }

}
