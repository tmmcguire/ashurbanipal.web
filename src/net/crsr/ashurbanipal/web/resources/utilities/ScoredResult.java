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

package net.crsr.ashurbanipal.web.resources.utilities;

import java.util.Comparator;

public class ScoredResult implements Comparable<ScoredResult> {
  public final int etext_no;
  public final double score;
  
  public ScoredResult(int etext_no, double score) {
    this.etext_no = etext_no;
    this.score = score;
  }

  // Default sort: by distance
  @Override
  public int compareTo(ScoredResult other) {
    final int compare = Double.compare(this.score, other.score);
    return compare != 0 ? compare : Integer.compare(this.etext_no, other.etext_no);
  }
  
  // Optional sort: by etext_no
  public static class ByEtext implements Comparator<ScoredResult> {
    @Override
    public int compare(ScoredResult left, ScoredResult right) {
      return Integer.compare(left.etext_no, right.etext_no);
    }
  }
  public static class Inverse implements Comparator<ScoredResult> {
    @Override
    public int compare(ScoredResult left, ScoredResult right) {
      final int compare = Double.compare(right.score, left.score);
      return compare != 0 ? compare : Integer.compare(left.etext_no, right.etext_no);
    }
  }
  
}