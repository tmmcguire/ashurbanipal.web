package net.crsr.ashurbanipal.web.resources;

import java.util.Comparator;

public class DistanceResult implements Comparable<DistanceResult> {
  public final int etext_no;
  public final double distance;
  
  public DistanceResult(int etext_no, double distance) {
    this.etext_no = etext_no;
    this.distance = distance;
  }

  // Default sort: by distance
  @Override
  public int compareTo(DistanceResult other) {
    return Double.compare(this.distance, other.distance);
  }
  
  // Optional sort: by etext_no
  public static class ByEtext implements Comparator<DistanceResult> {
    @Override
    public int compare(DistanceResult left, DistanceResult right) {
      return Integer.compare(left.etext_no, right.etext_no);
    }
  }
  
}