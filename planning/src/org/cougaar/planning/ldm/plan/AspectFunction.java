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

import java.io.Serializable;

/**
 * An AspectValue with a map of quantity versus time instead of a value.
 *
 */
 
public class AspectFunction extends TypedAspectValue {
  private PiecewiseLinear plFunction;

  protected AspectFunction(int type, PiecewiseLinear newFunction) {
    super(type);
    plFunction = newFunction;
  }

  private static boolean hack_warnedUser; // FIXME big hack!

  public static AspectValue create(int type, Object o) {
    if (o instanceof PiecewiseLinear) {
      return new AspectFunction(type, (PiecewiseLinear) o);
    } else {
      throw new IllegalArgumentException(
          "Cannot construct an AspectFunction from "+
          (o==null?"null":(o.getClass().getName()+": "+o)));
    }
  }

  public final double doubleValue() {
    throw new IllegalArgumentException("AspectFunctions do not have numeric values");
  }
  public final long longValue() {
    throw new IllegalArgumentException("AspectFunctions do not have numeric values");
  }
  public final float floatValue() {
    throw new IllegalArgumentException("AspectFunctions do not have numeric values");
  }
  public final int intValue() {
    throw new IllegalArgumentException("AspectFunctions do not have numeric values");
  }

  /** The function associated with the AspectValue.
   * @note functionValue is the preferred method.
    */
  public final PiecewiseLinear getFunctionValue() { return plFunction;}

  /** The function associated with the AspectValue. */
  public final PiecewiseLinear functionValue() { return plFunction;}

  public int hashCode() {
    return getType()+plFunction.hashCode();
  }

  public String toString() {
    return plFunction+"["+getType()+"]";
  }

  public boolean nearlyEquals(Object o) {
    return
      (o instanceof AspectValue &&
       this.equals((AspectValue) o));
  }

  public boolean equals(AspectValue v) {
    if (v instanceof AspectFunction) {
      AspectFunction loc_v = (AspectFunction)v;
      return (loc_v.getAspectType() == getType() &&
              loc_v.getFunctionValue() == getFunctionValue());
    } else {
      return false;
    }
  }
}
