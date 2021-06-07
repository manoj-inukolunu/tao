package me.tao.common;

public class Interval {

  int begin;
  int end;

  public Interval(int begin, int end) {
    this.begin = begin;
    this.end = end;
  }

  public boolean overlapsFully(Interval other) {
    return other.end >= this.end;
  }

}
