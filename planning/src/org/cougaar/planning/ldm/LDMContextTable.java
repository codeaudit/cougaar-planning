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

package org.cougaar.planning.ldm;

import java.util.HashMap;
import java.util.Map;

import org.cougaar.core.mts.MessageAddress;

/**
 * <b>PRIVATE</b> registry of (agent -to- LDMServesPlugin) mapping,
 * for Asset prototype serialization.
 * <p>
 * Asset deserialization can use the ClusterContext to figure out
 * the address of the agent being [de]serialized.  Beyond that, an
 * Asset needs to deserialize its prototype and bind to the agent's
 * LDM.  This class allows the Asset to find the appropriate
 * LDMServesPlugin.
 * <p>
 * See <b>bug 1576</b> and <b>bug 1659</b> for future refactoring
 * plans.  This implementation is a temporary hack!
 * <p>
 * The only valid clients:
 *
 * @see org.cougaar.planning.ldm.asset.Asset
 * @see org.cougaar.planning.ldm.PlanningDomain
 */
public final class LDMContextTable {
  
  private static final Map table = new HashMap();

  /** @see org.cougaar.planning.ldm.PlanningDomain */
  static void setLDM(MessageAddress agentAddr, LDMServesPlugin ldm) {
    synchronized (table) {
      Object o = table.get(agentAddr);
      if (o instanceof LDMServesPlugin.Delegator) {
        LDMServesPlugin.Delegator delegator = (LDMServesPlugin.Delegator) o;
        delegator.setLDM(ldm);
      }
      table.put(agentAddr, ldm);
    }
  }

  /** @see org.cougaar.planning.ldm.asset.Asset */
  public static LDMServesPlugin getLDM(MessageAddress agentAddr) {
    synchronized (table) {
      LDMServesPlugin result = (LDMServesPlugin) table.get(agentAddr);
      if (result == null) {
        result = new LDMServesPlugin.Delegator();
        table.put(agentAddr, result);
      }
      return result;
    }
  }

}
