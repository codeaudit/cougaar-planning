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
import java.util.Enumeration;

import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.domain.EnvelopeLogicProvider;
import org.cougaar.core.domain.LogicProvider;
import org.cougaar.core.domain.RootPlan;
import org.cougaar.planning.ldm.LogPlan;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Workflow;

public class WorkflowAllocationLP
implements LogicProvider, EnvelopeLogicProvider
{
  private final RootPlan rootplan;
  private final LogPlan logplan;

  public WorkflowAllocationLP(
      RootPlan rootplan,
      LogPlan logplan) {
    this.rootplan = rootplan;
    this.logplan = logplan;
  }

  public void init() {
  }

  /** @param Object  Object Envelope.tuple
   *        where Envelope.Tuple.object
   *            == PlanElement object ADDED TO LOGPLAN containing Expansion
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    // drop changes
    if (o.isAdd()) {
      Object obj = o.getObject();
      if ((obj instanceof Expansion)) {
        processExpansion((Expansion) obj);
      }
    }
  }

  private void processExpansion(Expansion exp) {
    Workflow work = exp.getWorkflow();
    rootplan.add(work);

      // Add each subtask of the workflow to the blackboard so
      // that allocators can allocate against single tasks
      // (and we don't need pass-thru expanders
    Enumeration tasks = work.getTasks();
    while (tasks.hasMoreElements()) {
      Task t = (Task) tasks.nextElement();
      if (t != null) {
        if (logplan.findTask(t) == null)
          rootplan.add(t);
      }
    }
  }
}
