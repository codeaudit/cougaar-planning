/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.planning.ldm.plan;

/**
 * A piecewise linear function of some quantity versus time
 */

public class PiecewiseLinear implements java.io.Serializable, Cloneable {

  Point[] points;

  /** An endpoint in a piecewise linear function */
  public static class Point {
    private long time;
    private float value;

    /**
     * The x coordinate of the point is the time (a long), and the
     * y coordinate is the value of the function
     */
    public Point (long time, float value) {
      this.time = time;
      this.value = value;
    }

    public long getTime()  { return time; }
    public float getValue()  { return value; }
  }

  /** Constructor that includes an array of points */
  public PiecewiseLinear (Point[] points) {
    setPoints (points);
  }

  /** Constructor without any initial points */
  public PiecewiseLinear()  {}

  /** Accessor to array of point */
  public Point[] getPoints()  { return points; }

  /**
   * Set the array of points to a new value<br>
   * Will sort the points by increasing time
   */
  public void setPoints (Point[] points) {
    this.points = new Point [points.length];
    System.arraycopy (points, 0, this.points, 0, points.length);
    java.util.Arrays.sort (this.points, 0, points.length,
      new java.util.Comparator() {
        public int compare (Object o1, Object o2) {
          long time1 = ((Point) o1).getTime();
          long time2 = ((Point) o2).getTime();
          return (time1 < time2) ? -1 : ((time1 > time2) ? 1 : 0);
        }});
  }

  /** make a deep copy */
  public Object clone() {
    return new PiecewiseLinear (points);
  }

  /** do the interpolation to find the value of the function at a
   * given time */
  public float valueAtTime (long time) {
    if (time <= points[0].getTime())
      return points[0].getValue();
    for (int i = 1; i < points.length; i++) {
      if (time <= points[i].getTime()) {
        long diff = points[i].getTime() - points[i-1].getTime();
        if (diff == 0)
          return (points[i].getValue() + points[i-1].getValue()) / 2.0f;
        return (points[i-1].getValue() * (points[i].getTime() - time) +
                points[i].getValue() * (time - points[i-1].getTime())) /
               diff;
      }
    }
    return points[points.length - 1].getValue();
  }

}
