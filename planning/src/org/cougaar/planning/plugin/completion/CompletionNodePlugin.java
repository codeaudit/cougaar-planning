/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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

package org.cougaar.planning.plugin.completion;

import java.util.Collections;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import org.cougaar.core.agent.AgentContainer;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.component.Container;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.UnaryPredicate;

/**
 * This plugin gathers and integrates completion information from
 * agents in a node to determine the "completion" of the current
 * tasks. It continually determines the worst laggard in the node and
 * forwards that one laggard to the society root.
 **/

public class CompletionNodePlugin extends CompletionSourcePlugin {
  private IncrementalSubscription targetRelaySubscription;
  private AgentContainer agentContainer;
  private Map filters = new WeakHashMap();
  private Laggard worstLaggard = null;

  public void load() {
    super.load();

    NodeControlService ncs = (NodeControlService)
      getServiceBroker().getService(
          this, NodeControlService.class, null);
    if (ncs != null) {
      agentContainer = ncs.getRootContainer();
      getServiceBroker().releaseService(
          this, NodeControlService.class, ncs);
    }
  }

  public void setupSubscriptions() {
    targetRelaySubscription = (IncrementalSubscription)
      blackboard.subscribe(targetRelayPredicate);
    super.setupSubscriptions();
  }

  public void execute() {
    if (targetRelaySubscription.hasChanged()) {
      checkPersistenceNeeded(targetRelaySubscription);
      if (logger.isDebugEnabled()) {
        Collection newRelays = targetRelaySubscription.getAddedCollection();
        if (!newRelays.isEmpty()) {
          for (Iterator i = newRelays.iterator(); i.hasNext(); ) {
            CompletionRelay relay = (CompletionRelay) i.next();
            logger.debug("New target: " + relay.getSource());
            if (worstLaggard != null) {
              sendResponseLaggard(relay, worstLaggard);
            }
          }
        }
      }
    }
    super.execute();
  }

  protected Set getTargetNames() {
    // get local agent addresses
    Set addrs;
    if (agentContainer == null) {
      if (logger.isErrorEnabled()) {
        logger.error(
            "Unable to list local agents on node "+
            getMessageAddress());
      }
      addrs = Collections.EMPTY_SET;
    } else {
      addrs = agentContainer.getAgentAddresses();
    }
    // flatten to names, which the parent then converts back.
    // we could fix parent to ask for "getTargetAddresses()"
    Set names = new HashSet(addrs.size());
    for (Iterator i = addrs.iterator(); i.hasNext(); ) {
      MessageAddress a = (MessageAddress) i.next();
      names.add(a.getAddress());
    }
    return names;
  }

  private void sendResponseLaggard(CompletionRelay relay, Laggard newLaggard) {
    if (logger.isDebugEnabled()) {
      logger.debug("Send response to "
                   + relay.getSource()
                   + ": "
                   + newLaggard);
    }
    relay.setResponseLaggard(newLaggard);
    blackboard.publishChange(relay);
  }

  protected void handleNewLaggard(Laggard newLaggard) {
    worstLaggard = newLaggard;
    if (targetRelaySubscription.size() > 0) {
      for (Iterator i = targetRelaySubscription.iterator(); i.hasNext(); ) {
        CompletionRelay relay = (CompletionRelay) i.next();
        LaggardFilter filter = (LaggardFilter) filters.get(relay);
        if (filter == null) {
          filter = new LaggardFilter();
          filters.put(relay, filter);
        }
        if (filter.filter(newLaggard)) {
          sendResponseLaggard(relay, newLaggard);
        } else {
          if (logger.isDebugEnabled()) logger.debug("No new response to " + relay.getSource());
        }
      }
    } else {
      if (logger.isDebugEnabled()) logger.debug("No relays");
    }
  }
}
      
