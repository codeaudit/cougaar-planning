/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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
/** Immutable implementation of Longitude.
 **/

// should be machine generated
package org.cougaar.planning.ldm.measure;

public final class Longitude extends AbstractMeasure
{
  private final static double upperBound = 180.0;
  private final static double lowerBound = -180.0;
  private final static double wrapUpperBound = 360.0;
  private final static double wrapLowerBound = -360.0;
	
	
  //no real conversions for now
  private static final Conversion DEGREES_TO_DEGREES = new Conversion() {
      public double convert(double from) { return from; }};

  // basic unit is Degrees
  private double theValue;

  // private constructor
  private Longitude(double v) throws ValueRangeException {
    theValue = wrap(v);
  }

  public Longitude(String s) {
    int i = indexOfType(s);
    if (i < 0) throw new UnknownUnitException();
    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();
    String u = s.substring(i).trim().toLowerCase();
    if (u.equals("degrees")) 
      theValue=n;
    else 
      throw new UnknownUnitException();
  }

  public int getCommonUnit() { return 0; }
  public int getMaxUnit() { return 0; }
  public String getUnitName(int i) { 
    if (i ==0) return "degrees";
    else throw new IllegalArgumentException();
  }


  // TypeNamed factory methods
  public static Longitude newDegrees(double v) {
    return new Longitude(v);
  }
  public static Longitude newDegrees(String s) {
    return new Longitude((Double.valueOf(s).doubleValue()));
  }

  // TypeNamed factory methods
  public static Longitude newLongitude(double v) {
    return new Longitude(v);
  }
  public static Longitude newLongitude(String s) {
    return new Longitude((Double.valueOf(s).doubleValue()));
  }
  

  // Index Typed factory methods
  // ***No real conversions for now
  private static final Conversion convFactor[]={
    //conversions to base units
    DEGREES_TO_DEGREES,
    //RADIANS_TO_DEGREES,
    // conversions from base units
    DEGREES_TO_DEGREES
    //DEGREES_TO_RADIANS
  };
  // indexes into factor array
  public final static int DEGREES = 0;
  //public final static int RADIANS = 1;
  private final static int MAXUNIT = 0;

  // Index Typed factory methods
  public static Longitude newLongitude(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT) 
      return new Longitude(convFactor[unit].convert(v));
    else
      throw new UnknownUnitException();
  }

  public static Longitude newLongitude(String s, int unit) {
    if (unit >= 0 && unit <= MAXUNIT) 
      return new Longitude(convFactor[unit].convert(Double.valueOf(s).doubleValue()));
    else 
      throw new UnknownUnitException();
  }

  // Support for AbstractMeasure-level constructor
  public static AbstractMeasure newMeasure(String s, int unit) {
    return newLongitude(s, unit);
  }
  public static AbstractMeasure newMeasure(double v, int unit) {
    return newLongitude(v, unit);
  }

  // Unit-based Reader methods
  public double getDegrees() {
    return (theValue);
  }
  //public double getRADIANS() {
  //return (DEGREES_TO_RADIANS.convert(theValue));
  //}

  public double getValue(int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return convFactor[MAXUNIT+1+unit].convert(theValue);
    else
      throw new UnknownUnitException();
  }

  public static Conversion getConversion(final int from, final int to) {
    if (from >= 0 && from <= MAXUNIT &&
        to >= 0 && to <= MAXUNIT ) {
      return new Conversion() {
          public double convert(double value) {
            return convFactor[MAXUNIT+1+to].convert(convFactor[from].convert(value));
          }
        };
    } else
      throw new UnknownUnitException();
  }

  public boolean equals(Object o) {
    return ( o instanceof Longitude &&
             theValue == ((Longitude) o).getDegrees());
  }
  public String toString() {
    return Double.toString(theValue) + "o";
  }
  private static final double hashFactor = (Integer.MAX_VALUE / 180.0);
  public int hashCode() {
    return (int) (theValue*hashFactor);
  }
  
  /** convert an arbitrary value to be in the range of -180 (open) to +180 (closed) **/
  private static double wrap( double v ) {
    v = v % 360.0;              // to (-359.99999, 359.99999)
    if (v <= -180.0) {          // to (-179.00000, 359.99999)
      v += 360.0;
    } else if (v > 180.0) {     // to (-179.00000, 180.00000)
      v -= 360.0;
    }
    return v;
  }
  
  /*
  private static void check(double x, double z) {
    double y = wrap(x);
    if (y != z) {
      System.err.println("wrap("+x+") = "+y+" not "+z);
    }
  }
  public static void main(String arg[]) {
    check(0.0, 0.0);
    check(180.0, 180.0);
    check(-180.0, 180.0);
    check(270.0, -90.0);
    check(-90.0, -90.0);
    check(-90.0-(360.0*100), -90.0);
    check(90.0+(360.0*100), 90.0);
  }
  */
  
}
