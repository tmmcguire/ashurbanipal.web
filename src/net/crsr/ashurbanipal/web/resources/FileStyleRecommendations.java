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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Path;

import net.crsr.ashurbanipal.web.exceptions.ResultNotFound;
import net.crsr.ashurbanipal.web.resources.utilities.AbstractFileRecommendations;
import net.crsr.ashurbanipal.web.resources.utilities.ScoredResult;

import org.apache.wink.common.annotations.Workspace;

@Path("/file/style")
@Workspace(workspaceTitle="Text Metrics", collectionTitle="Text style")
public class FileStyleRecommendations extends AbstractFileRecommendations {
  
  private static final String POS_DATA = "net/crsr/ashurbanipal/web/resources/data/gutenberg.pos";

  final private Map<Integer,Integer> etextToRow = new HashMap<>();
  final private Map<Integer,Integer> rowToEtext = new HashMap<>();
  // Use unboxed data structures for speed.
  final private double[][] posMatrix;

  public FileStyleRecommendations(FileMetadataLookup metadata) {
    super(metadata);
    BufferedReader br = null;
    try {

      br = new BufferedReader(new InputStreamReader(FileStyleRecommendations.class.getClassLoader().getResourceAsStream(POS_DATA)));

      // Read part of speech data into a temporary structure.
      final List<List<String>> temporary = new ArrayList<>();
      final Set<Integer> seenEtext = new HashSet<>();
      String line = br.readLine();
      while (line != null) {
        final List<String> list = Arrays.asList(line.split("\t"));
        final int etext_no = Integer.parseInt(list.get(0));
        if (!seenEtext.contains(etext_no)) {
          // Only retain the first occurrence of an etext. This data needs cleaning.
          seenEtext.add(etext_no);
          temporary.add(list);
        }
        line = br.readLine();
      }
     
      // Create the permanent part of speech data structures.
      posMatrix = new double[temporary.size()][temporary.get(0).size() - 1];
      for (int i = 0; i < temporary.size(); ++i) {
        final int etext_no = Integer.parseUnsignedInt(temporary.get(i).get(0));
        etextToRow.put(etext_no, i);
        rowToEtext.put(i, etext_no);
        
        for (int j = 1; j < temporary.get(i).size(); ++j) {
          posMatrix[i][j-1] = Double.parseDouble(temporary.get(i).get(j));
        }
      }
      
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      if (br != null) { try { br.close(); } catch (IOException e) { /* ignore */ } }
    }
  }
  
  // Compute the Euclidian distance between the selected text row and the data for each text.
  public List<ScoredResult> scoredResults(int etext_no) {
    final Integer row = etextToRow.get(etext_no);
    if (row == null) {
      throw new ResultNotFound("No style data found for Etext #" + etext_no);
    }

    final double[] distances = new double[posMatrix.length];
    
    for (int i = 0; i < posMatrix.length; ++i) {
      distances[i] = 0.0;
      for (int j = 0; j < posMatrix[0].length; ++j) {
        final double x = posMatrix[row][j] - posMatrix[i][j];
        distances[i] += x * x;
      }
      distances[i] = Math.sqrt(distances[i]); // Necessary? No.
    }
    
    final List<ScoredResult> results = new ArrayList<>(distances.length);
    for (int i = 0; i < distances.length; ++i) {
      results.add(new ScoredResult(rowToEtext.get(i), distances[i]));
    }
    return results;
  }
  
}
