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


package org.cougaar.planning.ldm.lps;

import java.util.Collection;
import org.cougaar.planning.ldm.*;
import org.cougaar.core.blackboard.Directive;
import org.cougaar.core.domain.*;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.plan.AllocationforCollections;
import org.cougaar.planning.ldm.plan.Deletion;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.TaskRescind;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * Sample LogicProvider for use by ClusterDispatcher to
 * take an incoming Deletion Directive and
 * perform Modification to the LOGPLAN
 **/

public class ReceiveDeletionLP
implements LogicProvider, MessageLogicProvider
{
  private final RootPlan rootplan;
  private final LogPlan logplan;
  private final PlanningFactory ldmf;
  private final MessageAddress self;

  private static final Logger logger = Logging.getLogger(ReceiveDeletionLP.class);

  public ReceiveDeletionLP(RootPlan rootplan, LogPlan logplan,
                           PlanningFactory ldmf, MessageAddress self)
  {
    this.rootplan = rootplan;
    this.logplan = logplan;
    this.ldmf = ldmf;
    this.self = self;
  }

  public void init() {
  }

  /**
   *  perform updates -- per Deletion ALGORITHM --
   *
   **/
  public void execute(Directive dir, Collection changes) {
    if (dir instanceof Deletion) {
      processDeletion((Deletion) dir);
    }
  }

  /**
   * The deletion prototol between agents.
   * DeletionPlugin in agent B decides it can delete a task received
   *    from agent A. The task is marked as deleted and then removed.
   *    DeletionLP in agent B reacts to the remove, notes the removed
   *    task is deleted, and sends a Deletion directive to agent A.
   * ReceiveDeletionLP (here) in agent A receives the Deletion
   *    directive, marks its copy of the remoteTask as deleted.
   * Eventually our (agent A) DeletionPlugin will delete the parent
   *    task of the deleted remote task. When that happens the task
   *    will be "rescinded", but the rescind will be marked as the
   *    rescind of a deleted task.
   * Agent B handles the rescind and the task is gone.
   *
   * Anomalies:
   * 1) Agent A resends the task before receiving the Deletion. Agent
   *    B finds a deleted matching task and resends the Deletion
   *    directive (just in case, see below). 
   * 2a) Agent A restarts after receiving the Deletion, but before
   *    persisting or rescinding the deleted task. The task is
   *    rehydrated as not deleted. Normal resynchronization causes the
   *    task to be resent and a new Deletion is returned as in case 1
   *    above.
   * 2b) Agent A restarts after receiving the Deletion, but before
   *    rescinding the deleted task. Normal resynchronization does not
   *    cause the task to be resent because the task is marked
   *    deleted.
   * 3) Agent A restarts after sending the rescind and the task
   *    returns. This case is problematic because there is no record
   *    anywhere of its having been deleted. We resolve this by not
   *    sending tasks that end in the past. This means that tasks that
   *    are initially published having end times in the past will not
   *    be propagated. Such tasks are anomalous to begin with.
   * 4) Agent B restarts after sending the Deletion, but before the
   *    task is rescinded and the deleted task is restored to its
   *    undeleted state. The notification from B to A signals that the
   *    task exists and is not deleted. This is at odds with A's
   *    state. Since the task has not yet been rescinded, the deleted
   *    status is removed.
   * 5) Same as 4 except A _has_ rescinded the task. It's too late to
   *    stop the outgoing TaskRescind. B ignores it because it is
   *    marked as the rescind of a deleted task and the task is not
   *    deleted. B's notification to A causes the usual TaskRescind
   *    that is also ignored by B for the same reason.
   **/
  private void processDeletion(Deletion del) {
    UID tuid = del.getTaskUID();
    PlanElement pe = logplan.findPlanElement(tuid);
    if (pe == null) {
      // Must have been rescinded, fabricate a TaskRescind to ack the
      // deletion
      TaskRescind ntr = ldmf.newTaskRescind(del.getChildTaskUID(), del.getSource(), true);
      rootplan.sendDirective(ntr);
      if (logger.isDebugEnabled()) logger.debug(self + ": ignoring Deletion for deleted task");
    } else {
      NewTask remoteTask = (NewTask) ((AllocationforCollections) pe).getAllocationTask();
      remoteTask.setDeleted(true);
      if (logger.isDebugEnabled()) logger.debug(self + ": acking Deletion for " + tuid);
      // Now the deleted remoteTask will simply hang around until the
      // deletion plugin gets around to deleting its parent structure.
    }
  }
}
