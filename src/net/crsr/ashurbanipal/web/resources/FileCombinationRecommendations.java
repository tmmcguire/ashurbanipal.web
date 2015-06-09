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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.Path;

import net.crsr.ashurbanipal.web.resources.utilities.AbstractFileRecommendations;
import net.crsr.ashurbanipal.web.resources.utilities.ScoredResult;

import org.apache.wink.common.annotations.Workspace;

@Path("/file/combination")
@Workspace(workspaceTitle="Text Metrics", collectionTitle="Text topics")
public class FileCombinationRecommendations extends AbstractFileRecommendations {
  
  private final FileStyleRecommendations styleRecommendations;
  private final FileTopicRecommendations topicRecommendations;
  
  public FileCombinationRecommendations(
      FileMetadataLookup metadataLookup,
      FileStyleRecommendations styleRecommendations,
      FileTopicRecommendations topicRecommendations
      ) {
    super(metadataLookup);
    this.styleRecommendations = styleRecommendations;
    this.topicRecommendations = topicRecommendations;
  }

  // Merge the two scored results, with the resulting score being the two primary scores multiplied.
  public List<ScoredResult> scoredResults(int etext_no) {
    final ScoredResult.ByEtext byEtextComparator = new ScoredResult.ByEtext();
    final List<ScoredResult> styleDistances = styleRecommendations.scoredResults(etext_no);
    Collections.sort(styleDistances, byEtextComparator);
    int s = 0;
    final List<ScoredResult> topicDistances = topicRecommendations.scoredResults(etext_no);
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
