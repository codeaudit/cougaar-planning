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
import java.util.HashMap;

/**
 * Verb is the action part of a Task.
 *
 */

public class Verb implements Serializable {
  private String name;
	
  /** Constructor takes a String that represents the verb.
   * @note pre-11.0 this was deprecated and public - now Verb.get(String) should be used.
   */
  protected Verb(String v) {
    if (v == null) throw new IllegalArgumentException();
    name = v.intern();
  }
	
  /** @return String toString returns the String that represents the verb */
  public final String toString() {
    return name;
  }
	
  /** Capabilities are equal IFF they encapsulate the same string
   */
  public final boolean equals(Object v) {
    // use == since verb strings are interned
    return (this == v || 
            (v instanceof Verb && name == ((Verb)v).name) ||
            (v instanceof String && name.equals((String) v))
            );
  }
  
  /** convenience method for verb testing */
  public final boolean equals(String v) {
    return ( name==v || name.equals(v));
  }

  public final int hashCode()
  {
    return name.hashCode();
  }

  // replace with an interned variation
  protected Object readResolve() {
    return getVerb(name);
  }

  // 
  // verb cache
  //

  private static final HashMap verbs = new HashMap(29);
  	
  /** older alias for Verb.get() 
   * @deprecated Use Verb.get(String)
   **/
  public static Verb getVerb(String vs) {
    return get(vs);
  }
  /** Verb factory method.  Constructs or returns cached verb instances
   * matching the requested paramater.
   * Note that this will only construct and/or return direct instances of
   * Verb and never any subclass.
   **/
  public static Verb get(String vs) {
    synchronized (verbs) {
      Verb v = (Verb) verbs.get(vs);
      if (v != null) 
        return v;
      else {
        vs = vs.intern();
        v = new Verb(vs);
        verbs.put(vs, v);
        return v;
      }
    }
  }
}
