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
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashSet;
import java.util.HashMap;

//import org.cougaar.core.util.UID;

/** ContextOfOplanIds is an implementation of Context. It is simply a Set of Oplan IDs. 
 * It can be used when the current oplan under which to operate needs to be referenced.
 * @see Oplan
 * @see Context
 */
public class ContextOfOplanIds 
  extends HashSet implements Context, Collection
{

  public ContextOfOplanIds (Collection ids) {
    for (Iterator iterator = ids.iterator(); iterator.hasNext(); ) {
      Object o = iterator.next();
      if (o instanceof String) add(o);
    }
  }

  /** 
   * Constructor that creates a collection with one and only one oplanID
   */
  public ContextOfOplanIds(String oneOplanId){
    add(oneOplanId);
  }

  /**
   * A constructor that copies the elements of the passed in array into the collection
   */
  public ContextOfOplanIds(String[] arrayOfOplanIDS) {
    for (int i=0; i<arrayOfOplanIDS.length; i++) {
      add(arrayOfOplanIDS[i]);
    }
  }
}
