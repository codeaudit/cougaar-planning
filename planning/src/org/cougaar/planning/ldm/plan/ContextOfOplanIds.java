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

/** ContextOfOplanIds is an implementation of Context. It is simply a collection of Oplan IDs. 
 * It can be used when the current oplan under which to operate needs to be referenced.
 * @see Oplan
 * @see Context
 */
public class ContextOfOplanIds 
  extends AbstractCollection  implements Context, Collection
{

  String[] oplanIds;

  public ContextOfOplanIds (Collection ids) {
    int IDcount=0;
    // Count the number of elements in the collection that are actually oplan IDs
    for (Iterator iterator = ids.iterator(); iterator.hasNext();) {
      Object o = iterator.next();
      if (o instanceof String) 
        IDcount++;
    }
    // Now go through again and add the IDs to the array
    this.oplanIds = new String[IDcount];
    int i = 0;
    for (Iterator iterator = ids.iterator(); iterator.hasNext(); ) {
      Object o = iterator.next();
      if (o instanceof String) 
	this.oplanIds[i] = (String)o;
    }
  }

  /** 
   * Constructor that creates a collection with one and only one oplanID
   */
  public ContextOfOplanIds(String oneOplanID){
    oplanIds = new String[1];
    oplanIds[0] = oneOplanID;
  }

  /**
   * A constructor that copies the elements of the passed in array into the collection
   */
  public ContextOfOplanIds(String[] arrayOfOplanIDS) {
    oplanIds = new String[arrayOfOplanIDS.length];
    for (int i=0; i<arrayOfOplanIDS.length; i++) {
      oplanIds[i] = arrayOfOplanIDS[i];
    }
  }

  public Iterator iterator() {
    return new OplanIDIterator(oplanIds);
  }

  /** @return the number of elements in the collection */
  public int size() {
    return oplanIds.length;
  }

  public boolean contains(Object o) {
    if (o instanceof String) {
      String inthere = (String)o;
      for (int i = 0; i < oplanIds.length; i++) {
	if (oplanIds[i].equals(inthere))
	  return true;
      }
    }
    return false;
  }

  /** 
   * @return true if this collection contains all of the elements in other
   */
  public boolean containsAll(Collection other) {
    boolean found = false;
    for (Iterator otherIterator = other.iterator(); otherIterator.hasNext();) {
      Object o=otherIterator.next();
      if (!(o instanceof String))
	return false;
      String otherOplanID = (String)o;
      found = false;
      for (int i=0; i<oplanIds.length; i++) {
	if (otherOplanID.equals(oplanIds[i])) {
	  found = true;
	  break;
	}
      }
      if (!found) 
	return false;
    }
    return true;
  }

  /**
   * @return an array of Object containing OplanIDs in the collection 
   **/
  public Object[] toArray() {
    return toArray(new String[oplanIds.length]);
  }

  /**
   * @return an array of OplanIDs with all the OplanIDs in the collection
   * @param array an array of OplanIds. A new array will be allocated if array size is incorrect.
   * 
   * @exception Throws ArrayStoreException if array is not an array of Strings
   **/
  public Object[] toArray(Object[] array) {
    if (!(array instanceof String[]))
      throw new ArrayStoreException("array must be an array of String");

    if (array.length != oplanIds.length) {
      array = new String[oplanIds.length];
    }
    System.arraycopy(oplanIds, 0, array, 0, oplanIds.length);
    return array;
  }
  /**
   * Simple accessor for n-th uid. Avoids consing iterators or arrays.
   **/
  public String get(int i) {
    return oplanIds[i];
  }

  public static class OplanIDIterator implements Iterator {
    String[] oplanIdArray;
    int place = 0;
    OplanIDIterator(String[] oplanIds) {
      oplanIdArray = oplanIds;
    }
    public boolean hasNext() {
      if (place < oplanIdArray.length)
	return true;
      return false;
    }
    public Object next() {
      return oplanIdArray[place++];
    }
    /**
     * @exception always throws UnsupportedOperationException
     */
    public void remove() {
      throw new UnsupportedOperationException("ContextOfOplanIds is immutable collection");
    }
  }
  public String toString() {
    String output = "[ContextOfOplanIds ";
    for (int i=0; i <oplanIds.length; i++) {
      output += (String)oplanIds[i];
      output += " ";
    }
    output += "]";
    return output;
  }


}
