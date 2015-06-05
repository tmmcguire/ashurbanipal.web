package net.crsr.ashurbanipal.web.resources;

public class DistanceResult implements Comparable<DistanceResult> {
  public final int etext_no;
  public final double distance;
  
  public DistanceResult(int etext_no, double distance) {
    this.etext_no = etext_no;
    this.distance = distance;
  }

  @Override
  public int compareTo(DistanceResult other) {
    return Double.compare(this.distance, other.distance);
  }
}