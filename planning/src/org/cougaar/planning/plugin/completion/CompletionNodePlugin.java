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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.WeakHashMap;
import org.cougaar.core.agent.AgentContainer;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.component.Container;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.node.NodeBusyService;
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
  private Map filters = new WeakHashMap();
  private Laggard worstLaggard = null;
  private NodeControlService ncs;
  private NodeBusyService nbs;
  private static final Class[] requiredServices = {
    NodeControlService.class,
    NodeBusyService.class,
  };

  public CompletionNodePlugin() {
    super(requiredServices);
  }

  public void load() {
    super.load();
  }

  public void unload() {
    if (haveServices()) {
      getServiceBroker().releaseService(this, NodeControlService.class, ncs);
      getServiceBroker().releaseService(this, NodeBusyService.class, nbs);
    }
    super.unload();
  }

  protected boolean haveServices() {
    if (nbs != null && ncs != null) return true;
    if (super.haveServices()) {
      ncs = (NodeControlService)
        getServiceBroker().getService(this, NodeControlService.class, null);
      nbs = (NodeBusyService)
        getServiceBroker().getService(this, NodeBusyService.class, null);
      return true;
    }
    return false;
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

  protected Set getTargets() {
    // get local agent addresses
    AgentContainer agentContainer = ncs.getRootContainer();
    if (agentContainer == null) {
      if (logger.isErrorEnabled()) {
        logger.error(
            "Unable to list local agents on node "+
            getMessageAddress());
      }
      return Collections.EMPTY_SET;
    } else {
      return agentContainer.getAgentAddresses();
    }
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

  // Adjust the set of laggards to be sure all busy agents appear to be incomplete
  protected boolean adjustLaggards(SortedSet laggards) {
    Set targets = new HashSet(getTargets());
    List newLaggards = new ArrayList(targets.size());
    for (Iterator i = laggards.iterator(); i.hasNext(); ) {
      Laggard laggard = (Laggard) i.next();
      MessageAddress target = laggard.getAgent();
      targets.remove(target);
      if (laggard.isLaggard()) continue; // Already laggard, don't care if busy
      if (nbs.isAgentBusy(target)) {
        if (logger.isInfoEnabled()) {
          logger.info("adjustLaggards: " + target + " is busy");
        }
        i.remove();
        newLaggards
          .add(new Laggard(target,
                           laggard.getBlackboardCompletion(),
                           1.0, true));
      }
    }
    if (targets.size() > 0) {
      // Some targets were apparently missing assume they are busy
      for (Iterator i = targets.iterator(); i.hasNext(); ) {
        MessageAddress target = (MessageAddress) i.next();
        if (nbs.isAgentBusy(target)) {
          newLaggards.add(new Laggard(target, 0.0, 1.0, true));
        }
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug("adjustLaggards laggards: " + laggards);
      logger.debug("            newLaggards: " + newLaggards);
    }
    if (newLaggards.size() > 0) {
      laggards.addAll(newLaggards);
      return true;
    }
    return false;
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
