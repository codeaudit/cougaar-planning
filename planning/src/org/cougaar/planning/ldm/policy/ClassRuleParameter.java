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
 * A ClassRuleParameter is a RuleParameter with specified/protected
 * Java interface/class that returns an Class that implements that interface
 * or extends that class
 */
public class ClassRuleParameter implements RuleParameter, java.io.Serializable {
  protected String my_name;
  protected Class my_interface = Object.class;
  protected Class my_value = Object.class;

  /**
   * Constructor sets class interface and establishes value as not set
   */
  public ClassRuleParameter(String param_name, Class iface)
  { 
    my_interface = iface; my_value = iface;
    my_name = param_name;
  }


  public ClassRuleParameter(String param_name)
  { 
    my_name = param_name;
  }

  public ClassRuleParameter() {
  }

  /**
   * Parameter type is CLASS
   */
  public int ParameterType() { return RuleParameter.CLASS_PARAMETER; }


  public Class getInterface() {
    return my_interface;
  }

  public void setInterface(Class iface)
  { 
    my_interface = iface; 
  }

  public void setInterface(String iface) { 
    try {
    my_interface = Class.forName(iface);
    } catch (Exception e) {
      System.out.println("Couldn't create class " + iface + e);
    } 
  }

  public String getName() {
    return my_name;
  }

  public void  setName(String name) {
    my_name = name;
  }

  /**
   * Get parameter value (Class)
   * @return Object parameter value (Class). Note : could be null.
   */
  public Object getValue()
  {
    return my_value; 
  }

  public void setValue(String iface) { 
    try {
    my_interface = Class.forName(iface);
    } catch (Exception e) {
      System.out.println("Couldn't create class " + iface + e);
    } 
  }

  /**
   * Set parameter value
   * @param Object new_value : must be Class that implements/extends 
   * given class
   * @throws RuleParameterIllegalValueException
   */
  public void setValue(Object new_value) 
       throws RuleParameterIllegalValueException
  {
    boolean success = false;
    if (new_value instanceof Class) {
      Class new_class = (Class)new_value;
      if (my_interface.isAssignableFrom(new_class)) {
	my_value = new_class;
	success = true;
      }
    }
    if (!success) 
      throw new RuleParameterIllegalValueException
	(RuleParameter.CLASS_PARAMETER, 
	 "Class must extend/implement " + my_interface);
  }

  /**
   * @param Object test_value : must be Class
   * @return true if Object isAssignableFrom Class specified in constructor,
   * false otherwise
   */
  public boolean inRange(Object test_value)
  {
    if (test_value instanceof Class) {
      Class new_class = (Class)test_value;
      if (my_interface.isAssignableFrom(new_class)) {
	return true;
      }
    }
    return false;
  }

  public String toString() 
  {
    return "#<CLASS_PARAMETER : " + my_value + 
      " [" + my_interface + "] >";
  }


  public Object clone() {
    ClassRuleParameter crp = new ClassRuleParameter(my_name, my_interface);
    try{
      crp.setValue(my_value);
    }catch (RuleParameterIllegalValueException rpive) {}
    return crp;
  }

  private interface CRP_Interface {}
  private class CRP_Derived implements CRP_Interface {}

  public static void Test() 
  {
    ClassRuleParameter crp = new ClassRuleParameter("testClassParam", 
						    CRP_Interface.class);

    if (crp.getValue() != null) {
      System.out.println("Error : Parameter not initialized to null");
    }
    
    try {
      crp.setValue(Integer.class);
      System.out.println("Error detecting illegal set condition");
    } catch(RuleParameterIllegalValueException rpive) {
    }

    try {
      crp.setValue(CRP_Derived.class);
    } catch(RuleParameterIllegalValueException rpive) {
      System.out.println("Error detecting legal set condition");
    }

    if(crp.getValue() != CRP_Derived.class) {
      System.out.println("Error retrieving value of parameter");
    }

    System.out.println("CRP = " + crp);
    System.out.println("ClassRuleParameter test complete.");

  }
}



