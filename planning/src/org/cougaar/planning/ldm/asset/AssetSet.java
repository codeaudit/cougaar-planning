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

package org.cougaar.planning.ldm.asset;

import java.util.Map;
import org.cougaar.core.blackboard.*;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.util.KeyedSet;

/**
 * AssetSet is a custom container which maintains a hashtable-like
 * association between asset's item id and the asset object.  
 **/

public class AssetSet 
extends KeyedSet
{
  protected Object getKey(Object o) {
    return ((Asset) o).getKey();
  }

  // special methods for Asset searches

  public Asset findAsset(Asset asset) {
    Object key = getKey(asset);
    if (key == null) return null;
    return (Asset) inner.get(key);
  }

  public Asset findAsset(String key) {
    return (Asset) inner.get(key);
  }

}




