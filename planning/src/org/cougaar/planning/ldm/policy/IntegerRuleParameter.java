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

package org.cougaar.planning.ldm.policy;

import org.cougaar.planning.ldm.policy.RuleParameter;
import org.cougaar.planning.ldm.policy.RuleParameterIllegalValueException;

/** 
 * @author  ALPINE <alpine-software@bbn.com>
 *
 **/

/**
 * An IntegerRuleParameter is a RuleParameter with specified/protected
 * integer bounds that returns an Integer
 */
public class IntegerRuleParameter implements RuleParameter, java.io.Serializable {
  protected String my_name;
  protected Integer my_value;
  protected int my_min;
  protected int my_max;

  /**
   * Constructor sets min/max values and establishes value as not set
   */
  public IntegerRuleParameter(String param_name, int min, int max, int value)
    throws RuleParameterIllegalValueException
  {
    this(param_name, min, max);
    setValue(new Integer(value));
  }

  public IntegerRuleParameter(String param_name, int min, int max) {
    my_min = min; my_max = max; my_value = null;
    my_name = param_name;
  }

  public IntegerRuleParameter(String param_name)
  { 
    my_value = null;
    my_name = param_name;
  }

  public IntegerRuleParameter() {
  }

  /**
   * Parameter type is INTEGER
   */
  public int ParameterType() { return RuleParameter.INTEGER_PARAMETER; }


  public String getName() {
    return my_name;
  }

  public void  setName(String name) {
    my_name = name;
  }

  public int getMin() {
    return my_min;
  }
    
  public void setMin(int min) {
    my_min = min;
  }

  public int getMax() {
    return my_max;
  }

  public void setMax(int max) {
    my_max = max;
  }

  public void setBounds(int min, int max) {
    if (min > max) {
      throw new java.lang.IllegalArgumentException("min  - " + min + 
                                                   " - must be greater than max - " + max);
    }
    my_min = min; 
    my_max = max;
  }

  public int getLowerBound() {
    return getMin();
  }

  public int getUpperBound() {
    return getMax();
  }

  /**
   * Get parameter value (Integer)
   * @return Object parameter value (Integer). Note : could be null.
   */
  public Object getValue()
  {
    return my_value; 
  }

  public int intValue() {
    return my_value.intValue();
  }

  /**
   * Set parameter value
   * @param Object new_value : must be Integer
   * @throws RuleParameterIllegalValueException
   */
  public void setValue(Object new_value) 
       throws RuleParameterIllegalValueException
  {
    boolean success = false;
    if (new_value instanceof Integer) {
      Integer new_integer = (Integer)new_value;
      if ((new_integer.intValue() >= my_min) && 
	  (new_integer.intValue() <= my_max)) {
	my_value = new_integer;
	success = true;
      }
    }
    if (!success) 
      throw new RuleParameterIllegalValueException
	(RuleParameter.INTEGER_PARAMETER, 
	 "Integer must be between " + my_min + " and " + my_max);
  }

  /**
   * 
   * @param Object test_value : must be Integer
   * @return true if test_value is within the acceptable range
   */
  public boolean inRange(Object test_value)
  {
    if (test_value instanceof Integer) {
      Integer new_integer = (Integer)test_value;
      if ((new_integer.intValue() >= my_min) && 
	  (new_integer.intValue() <= my_max))
	return true;
    }
    return false;
      
  }


  public String toString() 
  {
    return "#<INTEGER_PARAMETER : " + my_value + 
      " [" + my_min + " , " + my_max + "] >";
  }

  public Object clone() {
    IntegerRuleParameter irp = new IntegerRuleParameter(my_name, my_min, my_max);
    try {
      irp.setValue(my_value);
    } catch(RuleParameterIllegalValueException rpive) {}
    return irp;
  }

  public static void Test() 
  {
    IntegerRuleParameter irp = new IntegerRuleParameter("testIntParam", 3, 10);

    if (irp.getValue() != null) {
      System.out.println("Error : Parameter not initialized to null");
    }
    
    try {
      irp.setValue(new Integer(11));
      System.out.println("Error detecting illegal set condition");
    } catch(RuleParameterIllegalValueException rpive) {
    }

    try {
      irp.setValue(new Integer(1));
      System.out.println("Error detecting illegal set condition");
    } catch(RuleParameterIllegalValueException rpive) {
    }

    Integer i4 = new Integer(4);
    try {
      irp.setValue(i4);
    } catch(RuleParameterIllegalValueException rpive) {
      System.out.println("Error detecting legal set condition");
    }

    if(irp.getValue() != i4) {
      System.out.println("Error retrieving value of parameter");
    }

    System.out.println("IRP = " + irp);
    System.out.println("IntegerRuleParameter test complete.");

  }


}
