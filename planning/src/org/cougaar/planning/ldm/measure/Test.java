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
package org.cougaar.planning.ldm.measure;

/** Point test for measure class functionality **/

public class Test {
  public static void main(String arg[]) {
    System.out.println("Constructors:");
    Mass m0 = new Mass(1234.0, Mass.GRAMS);
    System.out.println("  Mass(double, int)="+m0);
    Mass m1 = Mass.newGrams(1234.0);
    System.out.println("  Mass.newGrams(double)="+m1);
    Mass m2 = new Mass("1234 grams");
    System.out.println("  Mass(String withSpace)="+m2);
    Mass m3 = new Mass("1234grams");
    System.out.println("  Mass(String noSpace)="+m3);

    System.out.println("Equals:");
    System.out.println("  all equals(): "+(m0.equals(m1) &&
                                           m1.equals(m2) &&
                                           m2.equals(m3)));

    System.out.println("Common unit:");
    int cu = m0.getCommonUnit();
    System.out.println("  Mass Common unit = "+cu+" ("+m0.getUnitName(cu)+")");
    System.out.println("  m0 = "+m0.getValue(cu));

    System.out.println("Capacity:");
    System.out.println("  rate Capacity: "+
                       new Capacity("Count=20units Duration=2days"));
    System.out.println("  instantaneous Capacity"+
                       new Capacity("Count=10units"));
    System.out.println("Rates:");
    Speed s = new Speed(1.0, Distance.MILES, Duration.HOURS);
    System.out.println(" Getters:");
    System.out.println("  meters/sec = "+s.getMetersPerSecond());
    System.out.println("  miles/hour = "+s.getMilesPerHour());
    System.out.println("  miles/hour = "+s.getValue(Speed.MILES_PER_HOUR));
    System.out.println("  miles/hour = "+s.getValue(Distance.MILES, Duration.HOURS));

    System.out.println("  1.0 == "+(new Speed(1.0, Distance.MILES, Duration.HOURS)).
                       getValue(Distance.MILES, Duration.HOURS));
    System.out.println("  1.0 == "+(Speed.newMilesPerHour(1.0)).
                       getMilesPerHour());
    System.out.println("  1.0 == "+(new Speed(1.0, Speed.MILES_PER_HOUR)).
                       getValue(Speed.MILES_PER_HOUR));

    System.out.println(" constructors:");
    System.out.println("  1: "+new Speed(1.0, Distance.MILES, Duration.HOURS));
    System.out.println("  2: "+new Speed(1.0, Speed.MILES_PER_HOUR));
    System.out.println("  3: "+new Speed(Distance.newMiles(1.0), Duration.newHours(1)));
    System.out.println("  4: "+new Speed("1 milesperhour"));
    System.out.println("  5: "+Speed.newMilesPerHour(1.0));
    System.out.println("  6: "+Speed.newMilesPerHour("1.0"));

    System.out.println();

    Measure m = s;
    System.out.println(" Measure API:");
    System.out.println("  Base unit = 0 = "+m.getUnitName(0));
    System.out.println("  Common unit = "+m.getCommonUnit()+
                       " = "+m.getUnitName(m.getCommonUnit()));
    System.out.println("  Max unit = "+m.getMaxUnit()+
                       " = "+m.getUnitName(m.getMaxUnit()));
    System.out.println("  Base unit value = "+m.getValue(0)+" "+m.getUnitName(0));
    System.out.println("  Common unit value = "+m.getValue(m.getCommonUnit())+
                       " "+m.getUnitName(m.getCommonUnit()));
    System.out.println("  Max unit value = "+m.getValue(m.getMaxUnit())+
                       " "+m.getUnitName(m.getMaxUnit()));
    Derivative d = s;
    System.out.println(" Derivative API:");
    System.out.println("  Numerator Class = "+d.getNumeratorClass());
    System.out.println("  Denominator Class = "+d.getDenominatorClass());
    System.out.println("  Canonical Numerator = "+d.getCanonicalNumerator());
    System.out.println("  CanonicalDenominator = "+d.getCanonicalDenominator());
    System.out.println("  value(miles,hours) = "+
                       d.getValue(Distance.MILES, Duration.HOURS));
    System.out.println("  computeNumerator(30minutes) = "+
                       d.computeNumerator(Duration.newMinutes(30)).getValue(Distance.MILES));
    System.out.println("  computeDenominator(2miles) = "+
                       d.computeDenominator(Distance.newMiles(2)).getValue(Duration.MINUTES));

  }
}
