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


import org.cougaar.core.agent.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.domain.*;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.*;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;


import java.util.*;

import org.cougaar.planning.ldm.plan.TaskImpl;

/**
  * LogicProvider for use by ClusterDispatcher to
  * take an incoming Rescind Directive and
  * perform Modification to the LOGPLAN
  * 
  *  1. Rescind Task - removes the task and any plan elements which
  *   address the that task.  Any cascade effect is then handled by
  *   RescindLP.
  **/

public class ReceiveRescindLP
implements LogicProvider, MessageLogicProvider
{
  private static final Logger logger = Logging.getLogger(ReceiveRescindLP.class);

  private final RootPlan rootplan;
  private final LogPlan logplan;

  public ReceiveRescindLP(
      RootPlan rootplan,
      LogPlan logplan) {
    this.rootplan = rootplan;
    this.logplan = logplan;
  }

  public void init() {
  }

  /**
   *  perform updates -- per Rescind ALGORITHM --
   *
   **/
  public void execute(Directive dir, Collection changes) {
    // drop changes
    if (dir instanceof TaskRescind) {
      receiveTaskRescind((TaskRescind) dir);
    }
  }

  private void receiveTaskRescind(TaskRescind tr) {
    UID tuid = tr.getTaskUID();
    System.err.print("R");

    // just rescind the task; let the RescindLP handle the rest
    //
    Task t = logplan.findTask(tuid);
    if (t != null) {
      rootplan.remove(t);
    } else {
      if (logger.isDebugEnabled()) {
	logger.debug("Couldn't find task to rescind: " + tuid);
      }
      rootplan.add(new RescindLP.DeferredRescind(tr));
    }
  }
}
 



