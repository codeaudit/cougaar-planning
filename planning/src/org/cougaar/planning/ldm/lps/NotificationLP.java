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

package org.cougaar.planning.ldm.lps;

import org.cougaar.planning.ldm.*;
import org.cougaar.core.blackboard.*;

import org.cougaar.core.agent.*;

import org.cougaar.core.domain.*;

import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.planning.ldm.plan.Aggregation;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AssetTransfer;
import org.cougaar.planning.ldm.plan.ClusterObjectFactory;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.MPTask;
import org.cougaar.planning.ldm.plan.NewNotification;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.PEforCollections;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.ScheduleElement;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.TaskScoreTable;

import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.util.UID;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;


import java.util.Enumeration;
import java.util.Collection;

/** RescindLogicProvider class provides the logic to capture 
 * rescinded PlanElements (removed from collection)
  *
  * @author  ALPINE <alpine-software@bbn.com>
  *
  **/

public class NotificationLP
implements LogicProvider, EnvelopeLogicProvider, RestartLogicProvider
{
  private static Logger logger = Logging.getLogger(NotificationLP.class);

  private final RootPlan rootplan;
  private final LogPlan logplan;
  private final PlanningFactory ldmf;
  private final MessageAddress self;

  public NotificationLP(
      RootPlan rootplan,
      LogPlan logplan,
      PlanningFactory ldmf,
      MessageAddress self) {
    this.rootplan = rootplan;
    this.logplan = logplan;
    this.ldmf = ldmf;
    this.self = self;
  }

  public void init() {
  }

  /**
   *  @param Object an Envelope.Tuple.object is an ADDED 
   * PlanElement which contains an Allocation to an Organization.
   * Do something if the test returned true i.e. it was an Allocation
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    Object obj = o.getObject();
    if ( ( o.isAdd() && (obj instanceof PlanElement)) ||
         ( o.isChange() &&
           ( obj instanceof PlanElement ) &&
           ((PEforCollections) obj).shouldDoNotification())
         ) {
      PlanElement pe = (PlanElement) obj;
      checkValues(pe, changes);
    } 
  }

  public void restart(final MessageAddress cid) {
    UnaryPredicate pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof PlanElement) {
          PlanElement pe = (PlanElement) o;
          MessageAddress source = pe.getTask().getSource();
          return RestartLogicProviderHelper.matchesRestart(self, cid, source);
        }
        return false;
      }
    };
    Enumeration enum = rootplan.searchBlackboard(pred);
    while (enum.hasMoreElements()) {
      PlanElement pe = (PlanElement) enum.nextElement();
      checkValues(pe, null);
    }
  }

  private void checkValues(PlanElement pe, Collection changes) {
    Task task = pe.getTask();

    if (logger.isDebugEnabled()) {
      logger.debug("\n" + self + ": task = " + task );
    }

    ((PEforCollections)pe).setNotification(false);
    if (task instanceof MPTask) {
      TaskScoreTable resultsbytask = ((MPTask)task).getComposition().calculateDistribution();
      if (resultsbytask != null) {
	Enumeration etasks = ((MPTask)task).getParentTasks();
	while (etasks.hasMoreElements()) {
	  Task pt = (Task) etasks.nextElement();
	  if (pt != null) {
	    AllocationResult result = resultsbytask.getAllocationResult(pt);
	    createNotification(pt.getUID(), task, result, changes);
	  } // else no notification need be generated
	}
      }
    } else {
      UID ptuid = task.getParentTaskUID();
      if (ptuid != null) {
	AllocationResult ar = pe.getEstimatedResult();
	createNotification(ptuid, task, ar, changes);
      } // else no notification need be generated
    }
  }
  
  private void createNotification(UID ptuid, Task t, AllocationResult ar, Collection changes) {
    MessageAddress dest = t.getSource();
    if (self == dest || self.equals(dest)) {
      // deliver intra-agent notifications directly
      ReceiveNotificationLP.propagateNotification(
          rootplan,logplan,ptuid,ar,t.getUID(), changes);
    } else {
      // need to send an actual notification
      NewNotification nn = ldmf.newNotification();
      nn.setTaskUID(ptuid);
      nn.setPlan(t.getPlan());
      nn.setAllocationResult(ar);
      // set the UID of the child task for Expansion aggregation change purposes
      nn.setChildTaskUID(t.getUID());
      if (ptuid == null) {
	logger.error("createNotification: parent task UID is null for task " +
		     t);
      }

      MessageAddress newDest = t.getSource();
      //MessageAddress newDest = pt.getDestination();
      MessageAddress newSource = self;
      
      nn.setSource(newSource);
      nn.setDestination(newDest);
      rootplan.sendDirective(nn, changes);
    }
  }
}
