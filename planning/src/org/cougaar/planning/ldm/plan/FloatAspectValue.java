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

/** An AspectValue implementation which stores a float.
 */
 
public class FloatAspectValue extends TypedAspectValue {
  private float value;

  // zeros cache 
  private static final int ZEROS = 20;
  private static final AspectValue zero[] = new AspectValue[ZEROS];
  static {
    // dumb, but we'll not worry about it (minimal excess AV creation)
    for (int i=0;i<ZEROS;i++) {
      zero[i] = new FloatAspectValue(i,0.0f);
    }
  }

  protected FloatAspectValue(int type, float value) {
    super(type);
    if (Float.isNaN(value) || Float.isInfinite(value))
      throw new IllegalArgumentException("The value of a FloatAspectValue must be a finite, non-NaN");
    this.value = value;
  }

  public static AspectValue create(int type, Object o) {
    float value;
    if (o instanceof Number) {
      value = ((Number)o).floatValue();
    } else if (o instanceof AspectValue) {
      value = ((AspectValue)o).floatValue();
    } else {
      throw new IllegalArgumentException("Cannot construct a FloatAspectValue from "+o);
    }
    return create(type, value);
  }

  public static AspectValue create(int type, float value) {
    if (value == 0.0 &&
        type>=0 && type<ZEROS ) {
      return zero[type];
    }
    return new FloatAspectValue(type,value);
  }

  public final double doubleValue() {
    return (double) value;
  }
  public final long longValue() {
    return Math.round(value);
  }
  public final float floatValue() {
    return value;
  }
  public final int intValue() {
    return (int) Math.round(value);
  }

  public boolean equals(Object v) {
    if (v instanceof FloatAspectValue) {
      return (getType() == ((AspectValue)v).getType() &&
              floatValue() == ((AspectValue)v).floatValue());
    } else {
      return false;
    }
  }

  public int hashCode() {
    return getType()+((int)(floatValue()*128));
  }

  public String toString() {
    return Float.toString(floatValue())+"["+getType()+"]";
  }


}


