/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.planning.ldm.lps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import org.cougaar.core.blackboard.Directive;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.domain.EnvelopeLogicProvider;
import org.cougaar.core.domain.LogicProvider;
import org.cougaar.core.domain.RootPlan;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.LogPlan;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.ClusterPG;
import org.cougaar.planning.ldm.plan.Aggregation;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationforCollections;
import org.cougaar.planning.ldm.plan.AssetRescind;
import org.cougaar.planning.ldm.plan.AssetTransfer;
import org.cougaar.planning.ldm.plan.AssignedAvailabilityElement;
import org.cougaar.planning.ldm.plan.Composition;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.NewSchedule;
import org.cougaar.planning.ldm.plan.MPTask;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleElement;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.TaskRescind;
import org.cougaar.planning.ldm.plan.Workflow;
import org.cougaar.planning.ldm.plan.WorkflowImpl;
import org.cougaar.util.Enumerator;
import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.PropertyParser;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/** RescindLogicProvider class provides the logic to capture 
 * rescinded PlanElements (removed from collection)
 *
 * Attempts to do a complete LogPlan rescind walk, not depending on
 * being re-called to do the "next" level of rescind.
 **/
public class RescindLP
  implements LogicProvider, EnvelopeLogicProvider {

  private static final Logger logger = Logging.getLogger(RescindLP.class);

  // If true, check for link consistency on Task/PE add/remove
  private static final boolean CHECKCONSISTENCY = PropertyParser.getBoolean("org.cougaar.planning.ldm.lps.RescindLP.checkBadTask", true);

  // If true and find inconsistencies above, clean up
  // where possible
  private static final boolean DOREMOVES = PropertyParser.getBoolean("org.cougaar.planning.ldm.lps.RescindLP.removeBadTask", true);

  private final RootPlan rootplan;
  private final LogPlan logplan;
  private final PlanningFactory ldmf;
  private final MessageAddress self;

  //private List conflictlist = new ArrayList();

  public RescindLP(
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
   *  @param o  EnvelopeTuple
   *             where Envelope.Tuple.object is an ADDED PlanElement which contains
   *                             an Allocation to an agent asset.
   * Do something if the test returned true i.e. it was a PlanElement being removed
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    // drop changes
    Object obj = o.getObject();
    if (o.isRemove()) {
      if (obj instanceof Task) {  // task
        taskRemoved((Task) obj);
      }else  if (obj instanceof PlanElement) {                    // PE
        planElementRemoved((PlanElement) obj);
      }
    } else if (o.isAdd()) {
      if (obj instanceof DeferredRescind) {
        processDeferredRescind((DeferredRescind) obj);
      } else if (obj instanceof PlanElement) {
        planElementAdded((PlanElement) obj);
      } else if (obj instanceof Task) {
	taskAdded((Task) obj);
      }
    }
  }

  private void taskAdded(Task t) {
    // If the task has no parent, maybe it was just removed.
    // Do a DeferredRescind(checking) and see if that's still true.
    // If so, remove this task.
    
    // Also, if the task results from an Expansion or Aggregation, but that PE is not there, do similar.
    if (CHECKCONSISTENCY && !isTaskConsistent(t)) {
      // create & publish new DeferredRescind
      if (logger.isDebugEnabled())
	logger.debug(self + ": Adding deferred rescind of inconsistent task " + t);
      rootplan.add(new DeferredRescind(t));
    }
  }

  // New local tasks should have a parent on the BBoard
  // That parent when tracing down should claim this task 
  // If the parent was expanded, then it's Exp should be same
  // as the task's workflow's Exp, and similarly the Workflows
  // should be the same. And the Exp should be on the BBoard
  // 1: pT on bb (if local task claimin a pT)
  // 2: pT has PE
  // 3: pT's PE is on bb
  // 4: If pT is Exp, it's workflow == t.getWorkflow != null
  // 5: if had workflow, it claims this task
  // 6: else if pT's PE is All,  it's getAllocationTaskUID() == t.getUID()
  // 7: else if pT's PE is Agg???? This task should be the MPTask
  // Note: Could put public helper in TaskImpl that takes
  // result of logplan.findTask(pUID), logplan.findPlanElement(parent)
  // -- or a ref to the logplan I suppose --,
  // and the self MessageAddress
  // Maybe cleaner that way?
  private boolean isTaskConsistent(Task t) {
    // If using Debug logging, then do all checks
    // even after one fails
    boolean debug = false;
    if (logger.isDebugEnabled())
      debug = true;
    boolean result = true;


    // Infrastructure could have removed this task already,
    // leaving it in an inconsistent state not worth checking
    Task bT = logplan.findTask(t.getUID());
    if (bT == null) {
      if (logger.isDebugEnabled())
	logger.debug(self + ": Task added but removed by the time I looked: " + t);
      return true;
    } else if (bT != t) {
      if (logger.isInfoEnabled())
	logger.info(self + ": Added task doesn't match what logplan had for this UID. Added: " + t + ". LogPlan has: " + bT);
      if (! debug)
	return false;
      else
	result = false;
    }

    UID pUID = t.getParentTaskUID();
    MessageAddress dest = t.getSource();
    Workflow w = t.getWorkflow();
    // If pUID is non-null && local, 
    if (pUID != null && (self == dest || self.equals(dest))) {
      // 1: this UID should be on the LogPlan.
      Task parent = logplan.findTask(pUID);
      if (parent == null) {
	// PROBLEM: Local task claims parent not on LogPlan
	if (logger.isInfoEnabled())
	  logger.info(self + ": Added task (local)'s parent not found on LogPlan: " + t);
	// Should later remove this task
	if (! debug)
	  return false;
	else
	  result = false;

	// Local task with parent avail
	if (w == null) {
	  // Added task whose parent is missing with no workflow.
	  // Nothing more to check.
	  return result;
	} else {
	  // Look at the workflow: Does it contain me?
	  boolean hasSub = false;
	  synchronized (w) {
	    Enumeration enum = w.getTasks();
	    while (enum.hasMoreElements()) {
	      Task subT = (Task)enum.nextElement();
	      if (subT == t) {
		hasSub = true;
		break;
	      }
	    }
	  }
	  
	  if (! hasSub) {
	    if (logger.isInfoEnabled()) 
	      logger.info(self + ": Added task's workflow does not contain this task. Task: " + t + ", workflow: " + w);
	    if (! debug)
	      return false;
	    else
	      result = false;
	  }
	  
	  // Does it point to the same task as I point to?
	  Task wPTask = w.getParentTask();
	  if (wPTask != null) {
	    if (wPTask.getUID().equals(pUID)) {
	      parent = wPTask;
	      if (logger.isDebugEnabled())
		logger.debug(self + ": Added task whose parent was missing had a workflow that still pointed to the parent task. Task: " + t);
	    } else {
	      if (logger.isInfoEnabled())
		logger.info(self + ": Added task whose parent was missing had a workflow that pointed to a different parent task. Task: " + t + ", workflow: " + w);
	      // So the parent was not on the LogPlan, and the workflow
	      // pointed to a different parent. Bail out.
	      // FIXME: Could check to see if that wf's parent task refers back to this workflow and/or is on the bboard & of course it's planelement
	      return false;
	    }
	  } else {
	    // local task with no parent but workflow also missing parent
	    // is a rescind in progress? From above,
	    // this task will be marked inconsistent already
	    if (logger.isInfoEnabled())
	      logger.info(self + ": Added task with missing parent whose workflow's parent link is null. Task: " + t);
	    return false;
	  }
	} // missing parent but had a workflow
      } // end of block handling missing parent task

      // Get here means the local parent was found.
	
      // 2: It should also have a non-null PE
      PlanElement ppe = parent.getPlanElement();
      PlanElement bppe = logplan.findPlanElement(parent);
      if (ppe == null) {
	// problem
	if (logger.isInfoEnabled()) 
	  logger.info(self + ": Added task's parent has no PE. Task: " + t + ". Logplan lists pe: " + (bppe != null ? (bppe.getUID() + ":") : "") + bppe);
	// Should later remove both this task and the parent!!! FIXME!!!
	// Or maybe the parent is OK?
	if (! debug)
	  return false;
	else
	  result = false;

	// If the parent has no plan element, what else can I check?
	if (w == null) {
	  // Added task whose parent's planelement is missing with no workflow.
	  // Nothing more to check.
	  return result;
	} else {
	  // Look at the workflow: Does it contain me?
	  boolean hasSub = false;
	  synchronized (w) {
	    Enumeration enum = w.getTasks();
	    while (enum.hasMoreElements()) {
	      Task subT = (Task)enum.nextElement();
	      if (subT == t) {
		hasSub = true;
		break;
	      }
	    }
	  }
	  
	  if (! hasSub) {
	    if (logger.isInfoEnabled()) 
	      logger.info(self + ": Added task's workflow does not contain this task. Task: " + t + ", workflow: " + w);
	    if (! debug)
	      return false;
	    else
	      result = false;
	  }
	  
	  // Does it point to the same task as I point to?
	  Task wPTask = w.getParentTask();
	  if (wPTask != null) {
	    if (wPTask == parent) {
	      if (logger.isDebugEnabled())
		logger.debug(self + ": Added task whose parent's PE was missing had a workflow that still pointed to the parent task. Task: " + t);
	    } else {
	      if (logger.isInfoEnabled())
		logger.info(self + ": Added task whose parent's PE was missing had a workflow that pointed to a different parent task. Task: " + t + ", workflow: " + w);
	      // So the parent's PE was not on the LogPlan, and the workflow
	      // pointed to a different parent. Bail out.
	      // FIXME: Could check to see if that wf's parent task refers back to this workflow and/or is on the bboard & of course it's planelement
	      return false;
	    }
	  } else {
	    // This task's workflow didn't point back to the task's parent!
	    if (logger.isInfoEnabled())
	      logger.info(self + ": Added task had parent missing PE & a workflow without a parent task. Task: " + t + ", workflow: " + w);
	    return false;
	  }
	} // missing parent's PE but had a workflow
	return false;
      } // handle no PE from parent task
      // At this point we've handled missing parent or parent missing PE.
      // Below we'll handle non-local task or not pointing
      // to a parent
      
      // 3: That PE should be on the LogPlan
      if (bppe != ppe) {
	// problem
	if (logger.isInfoEnabled())
	  logger.info(self + ": Added task's parent's PE not on LogPlan consistently. Task: " + t + ", parent's PE: " + (ppe!= null ? (ppe.getUID()+":"+ppe) : ppe.toString()) + ", but LogPlan says: " + (bppe != null ? (bppe.getUID()+":"+bppe) : bppe.toString()));
	// Should later remove both this task and the parent!!! FIXME!!!
	// Should also probably remove both the planElement's referred
	// to here.... FIXME!!!
	// Or maybe the parent is OK, but the task and PEs are not?
	if (! debug)
	  return false;
	else
	  result = false;
      }

      if (ppe == null && bppe != null) {
	// If we get here the parent listed no PE,
	// but the blackboard PE is not. Use that to continue to debug
	ppe = bppe;
      }

      // That PE should be an Expansion or Aggregation (maybe Alloc too?)
      // 4: If the PE is an Expansion, then t.getWorkflow below 
      // should be non-null and == exp.getWorkflow()
      if (ppe instanceof Expansion) {
	Workflow pw = ((Expansion)ppe).getWorkflow();
	if (pw == null) {
	  if (logger.isInfoEnabled())
	    logger.info(self + ": Added task's parent's expansion had no workflow. Task: " + t + ", Expansion: " + ppe.getUID() + ":" + ppe);
	  // Should remove the task, parent, and Expansion? 
	  // Or maybe just the task? FIXME!!!
	  if (! debug)
	    return false;
	  else
	    result = false;
	}

	if (w == null) {
	  if (logger.isInfoEnabled())
	    logger.info(self + ": Added task's parent was Expanded, but this task has no workflow. Task: " + t);
	  // Task is clearly bad. But is parent OK? FIXME
	  if (! debug)
	    return false;
	  else
	    result = false;
	} 

	if (w != pw) {
	  if (logger.isInfoEnabled())
	    logger.info(self + ": Added task's parent's expansion's workflow not same as this task's workflow. Task: " + t + " claims workflow: " + w + ", but parent has workflow: " + pw);
	  // Added task is bad. parent may be OK though? FIXME!
	  // All sub's of the added task's workflow are also suspect
	  if (! debug)
	    return false;
	  else
	    result = false;
	}
	
	// 4.5: Extra check.
	if (w != null && w.getParentTask() == null) {
	  if (logger.isInfoEnabled()) 
	    logger.info(self + ": Added task's workflow's parent is null. Task: " + t + ", workflow: " + w);
	  // The task and all subs of the workflow are bad. FIXME
	  // But the parent task pointed to this workflow, so is the 
	  // parent task also bad?
	  if (! debug)
	    return false;
	  else
	    result = false;
	}

	if (w != null && w.getParentTask() != parent) {
	  if (logger.isInfoEnabled())
	    logger.info(self + ": Added task's parent not same as added task's workflow's parent. Task: " + t + ", workflow: " + w);
	  // The workflow is pointed 2 from 2 directions, but it's upwards
	  // pointer is bad. Huh?
	  if (! debug)
	    return false;
	  else
	    result = false;
	}
	
	if (w != null) {
	  // 5: Confirm that workflow has this subtask
	  boolean hasSub = false;
	  synchronized (w) {
	    Enumeration enum = w.getTasks();
	    while (enum.hasMoreElements()) {
	      Task subT = (Task)enum.nextElement();
	      if (subT == t) {
		hasSub = true;
		break;
	      }
	    }
	  }

	  if (! hasSub) {
	    if (logger.isInfoEnabled()) 
	      logger.info(self + ": Added task's workflow does not contain this task. Task: " + t + ", workflow: " + w);
	    if (! debug)
	      return false;
	    else
	      result = false;
	  }
	}
	
	// end of parent was expanded check
      } else if (ppe instanceof Allocation) {
	// 6: ppe Allocation must have this t's UID as allocTaskUID
	UID aUID = ((AllocationforCollections)ppe).getAllocationTaskUID();
	if (aUID == null) {
	  if (logger.isInfoEnabled())
	    logger.info(self + ": Added task's parent's allocation says AllocTask is null? Task: " + t + ", parent's alloc: " + ppe.getUID() + ":" + ppe);
	  // Task is bad. Allocation & parent may be OK FIXME
	  if (! debug)
	    return false;
	  else
	    result = false;
	} else if (aUID != t.getUID()) {
	  if (logger.isInfoEnabled())
	    logger.info(self + ": Added task's parent's allocation's allocTask not same as this task. Task: " + t + ", allocation: " + ppe.getUID() + ":" + ppe);
	  // Task is bad. Alloc & parent may be OK - FIXME
	  if (! debug)
	    return false;
	  else
	    result = false;
	}
      } else if (ppe instanceof Aggregation) {
	// 7: If ppe is Aggregation?
	// This task should be the MPTask
	Composition c = ((Aggregation)ppe).getComposition();
	if (c == null) {
	  if (logger.isInfoEnabled())
	    logger.info(self + ": Added task's parent's Aggregation PE had no composition. Task: " + t + " aggregation: " + ppe.getUID() + ":" + ppe);
	  if (! debug)
	    return false;
	  else
	    result = false;
	} else {
	  // Could check that composition has the parent task as a parent
	  MPTask mpt = c.getCombinedTask();
	  if (mpt == null) {
	    if (logger.isInfoEnabled())
	      logger.info(self + ": Added task's parent's aggregation's composition had no CombinedTask. Task: " + t + ", aggregation: " + ppe.getUID() + ":" + ppe);
	    if (! debug)
	      return false;
	    else
	      result = false;
	  } else if (mpt != t) {
	    if (logger.isInfoEnabled())
	      logger.info(self + ": Added task's parent's aggregation's MPTask not same as this task. Task: " + t + ", mptask: " + mpt + ", aggregation: " + ppe.getUID() + ":" + ppe);
	    if (! debug)
	      return false;
	    else
	      result = false;
	  }
	}
      } // switch on type of parent PE
    } else if (w != null) {
      // task with no parent or parent is remote
      // Had no parent but it says it has a workflow?
      if (logger.isInfoEnabled())
	logger.info(self + ": Added task had no or non-local parent. Task Source: " + dest + ". For comparison, dest: " + t.getDestination() + ". But it has a workflow! Task: " + t + ", workflow: " + w);
      // Keep going? Does the workflow have a parent? Does
      // that parent exist? If so, maybe remove that parent
      // so it propogates back down to the Expansion & clears out
      // the workflow and removes the task?
      // Does the workflow contain this task?
      if (! debug)
	return false;
      else
	result = false;

      // Look at the workflow: Does it contain me?
      boolean hasSub = false;
      synchronized (w) {
	Enumeration enum = w.getTasks();
	while (enum.hasMoreElements()) {
	  Task subT = (Task)enum.nextElement();
	  if (subT == t) {
	    hasSub = true;
	    break;
	  }
	}
      }
      
      if (! hasSub) {
	if (logger.isInfoEnabled()) 
	  logger.info(self + ": Added task's workflow does not contain this task. Task: " + t + ", workflow: " + w);
	if (! debug)
	  return false;
	else
	  result = false;
      }
      
      // Does it point to the same task as I point to?
      Task wPTask = w.getParentTask();
      if (wPTask != null) {
	if (wPTask.getUID().equals(pUID)) {
	  if (logger.isDebugEnabled())
	    logger.debug(self + ": Added task whose parent was missing or remote had a workflow that still pointed to the parent task. Task: " + t + ", workflow: " + w);
	} else {
	  if (logger.isInfoEnabled())
	    logger.info(self + ": Added task whose parent was missing or remote had a workflow that pointed to a different parent task. Task: " + t + ", workflow: " + w);
	  // So the parent was not on the LogPlan, and the workflow
	  // pointed to a different parent. Bail out.
	  // FIXME: Could check to see if that wf's parent task refers back to this workflow and/or is on the bboard & of course it's planelement
	}
      } else {
	if (logger.isInfoEnabled())
	  logger.info(self + ": Added Task with remote or missing parent had a workflow with no parent. Task: " + t + ", workflow " + w);
      }
    } // missing or remote parent but had a workflow

    // Task with no or non-local parent and no workflow. Just fine.
    return result;
  }

  private void planElementAdded(PlanElement pe) {
    Task task = pe.getTask();
    // Could check that the PE is still on the LogPlan at this point...
    if (logplan.findTask(task) == null) {
      if (logger.isDebugEnabled()) {
	logger.debug(self + ": Removing added planelement [task not found in the logplan] for " + task + " as " + pe.getUID() + ":" + pe);
      }
      removePlanElement(pe, true);
      // Unless we're doing debug logging, skip out here.
      if (! logger.isDebugEnabled())
	return;
    }

    if (! CHECKCONSISTENCY)
      return;

    // With the ASO fix, the below is hopefully extra:
    PlanElement bPE = logplan.findPlanElement(task);
    PlanElement tPE = task.getPlanElement();
    if (bPE == null) {
      // added PE not on LogPlan
      // This is OK if the PE were since removed. 
      if (tPE == null) {
	// And it's task doesn't point to it
	// In other words, it's been removed
	if (logger.isDebugEnabled())
	  logger.debug(self + ": Added PE not on Task or LogPlan. Removed? " + pe.getUID() + ":" + pe);
      } else if (tPE != pe) {
	// And it's task points to a different PE
	// w/o ASO change, another thread may be adding a new PE
	// after this PE was removed. With that change,
	// this is an error.
	if (logger.isInfoEnabled())
	  logger.info(self + ": Added PE not on LogPlan (nothing is), but task points to another PE. Is another add in progress after this was removed? PE: " + pe.getUID() + ":" + pe + ", task: " + task + ", Task's new PE: " + tPE.getUID() + ":" + tPE);
      }
    } else if (bPE != pe) {
      // added PE not on LogPlan, another is.
      // This is OK if another add has already completed
      // We must watch though, w/o ASO change, for inconsistent pointers
      if (bPE == tPE) {
	// And its task points to this other PE
	// Does the other PE point to this task? Presumably?
	// This could happen if this PE were removed
	// And another added before this LP ran.

	// This is a strange thing for a PE to do, but i guess so...
	// I see lots of these allocate to InventoryAssets, PackaedPOL,
	// MaintainedItem -- diff UID, alloc to same NSN
	if (logger.isDebugEnabled())
	  logger.debug(self + ": Added PE apparently since removed & replaced. Added: " + pe.getUID() +":" + pe + " replaced with " + bPE.getUID() + ":" + bPE + " for task " + task);
      } else if (tPE == null) {
	// Added PE's task points to no PE, but has a different
	// PE on the LogPlan. This shouldn't happen
	if (logger.isWarnEnabled())
	  logger.warn(self + ": Added PE not on task or LogPlan. Removed? Task points to no PE, but LogPlan points to a different PE!?! Task: " + task + ", LogPlan's PE: " + bPE.getUID() +":" + bPE);
      } else if (tPE == pe) {
	// Added PE's task points to it (ie no publishRemove called), 
	// but has a different PE on the LogPlan - ie
	// another publishAdd finished after this one. Maybe
	// Add 1 starts, then remove, then add #2 finishes, then add #1
	// finishes, then this LP runs?
	if (logger.isWarnEnabled())
	  logger.warn(self + ": Added PE is ref'ed by its task, but LogPlan has another. Added PE: " + pe.getUID() + ":" + pe + ", Task: " + task + ", LogPlan's PE: " + bPE.getUID() + ":" + bPE);
      } else {
	// Added PE's Task points to 1 PE, LogPlan to another, and neither
	// are the PE that was just added!
	if (logger.isWarnEnabled())
	  logger.warn(self + ": Added PE not ref'ed, and Task and LogPlan point to different PEs. Task " + task + ", Task's PE: " + tPE.getUID() + ":" + tPE + ", LogPlan's PE: " + bPE.getUID() + ":" + bPE);
      }
    } else {
      // LogPlan has this PE. Does the task?
      if (tPE == null) {
	// w/o ASO change it means a remove is in progress
	// With that change, this is an error.
	if (logger.isInfoEnabled())
	  logger.info(self + ": Added PE on LogPlan but Task has none. Is PE removal in progress? PE: " + pe.getUID() +":" + pe + ", Task: " + task);
      } else if (tPE != pe) {
	// Huh? W/o ASO change this may mean the first add was in
	// progress, a remove started but didn't finish,
	// then another add started (changing the task pointer),
	// and only now is the first add finishing.
	if (logger.isInfoEnabled())
	  logger.info(self + ": Added PE on LogPlan but Task already points to a different PE. Another remove and add both in progress? PE: " + pe.getUID() + ":" + pe + ", Task: " + task + ", Task's PE: " + tPE.getUID() + ":" + tPE);
      }
      // Task has what LogPlan has which is this PE. Normal case.
    }

    // Other to do: If it's an Expansion, does it have a workflow?
  }

  private void removeTaskFromWorkflow(Task t) {
    if (t == null)
      return;
    WorkflowImpl w = (WorkflowImpl)t.getWorkflow();
    if (w == null)
      return;

    boolean hasSub = false;
    synchronized (w) {
      Enumeration enum = w.getTasks();
      while (enum.hasMoreElements()) {
	Task subT = (Task)enum.nextElement();
	if (subT == t) {
	  hasSub = true;
	  break;
	}
      }
    }

    if (hasSub) {
      if (logger.isInfoEnabled())
	logger.info(self + " removing task from workflow. Task: " + t);
      w.removeTask(t);
    }
  }

  private void processDeferredRescind(DeferredRescind deferredRescind) {
    if (deferredRescind.tr != null) {
      UID rtuid = deferredRescind.tr.getTaskUID();
      Task t = logplan.findTask(rtuid);
      if (t != null) {
	if (logger.isDebugEnabled())
	  logger.debug(self + ": Found task for DeferredRescind. Removing " + t);
	removeTask(t);
	rootplan.remove(deferredRescind);
      } else {
	if (logger.isDebugEnabled())
	  logger.debug(self + ": Never found task for DeferredRescind. Giving up on " + rtuid);
	rootplan.remove(deferredRescind);
      }
    } else if (deferredRescind.t != null) {
      // Check consistency as above.
      // assert CHECKCONSISTENCY == true
      if (!isTaskConsistent(deferredRescind.t)) {
	if (DOREMOVES) {
	  if (logger.isWarnEnabled())
	    logger.warn(self + ": New task inconsistent after deferral, removing: " + deferredRescind.t);
	  // FIXME: remove parent task & PE too?
	  // If task is in a workflow, must first remove it from the workflow
	  removeTaskFromWorkflow(deferredRescind.t);
	  removeTask(deferredRescind.t);
	} else {
	  if (logger.isWarnEnabled())
	    logger.warn(self + ": New task inconsistent after deferral, NOT REMOVING: " + deferredRescind.t);
	  rootplan.remove(deferredRescind);
	}
      } else {
	if (logger.isInfoEnabled())
	  logger.info(self + ": New task was not, now is consistent after deferral. Leaving: " + deferredRescind.t);
	rootplan.remove(deferredRescind);
      }

    }
  }

  /** remove PE and any cascade objects */
  private void removePlanElement(PlanElement pe, boolean force) {
    if (pe != null) {
      if (force || logplan.findPlanElement(pe.getTask()) != null) {
	rootplan.remove(pe);
	//      planElementRemoved(pe);
      }
    }
  }

  /** rescind the cascade of any PE (does not remove the PE) */
  private void planElementRemoved(PlanElement pe) {
    // Is the PE on the LogPlan or the Task? - if it is, re remove it
    Task t = pe.getTask();
    if (CHECKCONSISTENCY && t != null) {
      PlanElement bPE = logplan.findPlanElement(t);
      PlanElement tPE = t.getPlanElement();
      if (bPE == pe) {
	// removed PE still listed as on the LogPlan under it's task
	if (tPE == pe) {
	  // removed PE still pointed to by it's task
	  // So the PE is on the LogPlan
	  // and the task points to it - it's as though
	  // it never happened at all. Leave it?
	  if (logger.isWarnEnabled())
	    logger.warn(self + ": Removed PE still on LogPlan and Task points to it. PE: " + pe.getUID() + ":" + pe + ", Task: " + t);
	  if (DOREMOVES) {
	    removePlanElement(pe, true);
	    // Do the follow-on the next time around...
	    return;
	  }
	} else if (tPE == null) {
	  // the ASO link fixing happened by the PE
	  // is still on the LogPlan.
	  // Re-remove the PE
	  if (logger.isWarnEnabled())
	    logger.warn(self + ": Removed PE still on LogPlan though Task points to no PE. Re removing it. PE: " + pe.getUID() + ":" + pe + ", Task: " + t);
	  if (DOREMOVES) {
	    removePlanElement(pe, true);
	    // Do the follow-on the next time around...
	    return;
	  }
	} else {
	  // the ASO link fixing happened by the PE and another
	  // PE is pointed to?
	  // is still on the LogPlan.
	  // Re-remove the PE
	  if (logger.isWarnEnabled())
	    logger.warn(self + ": Removed PE still on LogPlan though Task doesn't point to it. Re removing it. PE: " + pe.getUID() + ":" + pe + ", Task: " + t + ". Task points to PE: " + tPE.getUID() + ":" + tPE);
	  if (DOREMOVES) {
	    removePlanElement(pe, true);
	    // Do the follow-on the next time around...
	    return;
	  }
	}
      } else if (tPE != bPE) {
	if (tPE == pe) {
	  // The PE is gone from the LogPlan, but
	  // the task still points to it. As though
	  // the ASO stuff didn't happen but the LogPlan stuff did.
	  // Note that bboard may have null or different PE
	  if (logger.isWarnEnabled())
	    logger.warn(self + ": Removed PE not on LogPlan but Task still points to it. Re-removing. PE: " + pe.getUID() + ":" + pe + ", Task: " + t + ". Logplan has " + (bPE == null ? "null" : (bPE.getUID() + ":" + bPE)));
	  if (DOREMOVES) {
	    removePlanElement(pe, true);
	    return;
	  }
	} else {
	  // Removed PE not on LogPlan and not pointed to.
	  // But it's task also doesn't point to the PE 
	  // that is on the LogPlan under it's UID
	  // Task's PE may be null or different
	  if (logger.isInfoEnabled())
	    logger.info(self + " Removed PE was removed, but it's Task points to different PE than what LogPlan has under its UID. Task: " + t + ", LogPlan's PE: " + (bPE == null ? "null" : (bPE.getUID() + ":" + bPE)) + ", Task's PE: " + (tPE == null ? "null" : (tPE.getUID() + ":" + tPE)));
	}
      }
    } // end of error check PE removal

    //logger.printDot("p");
    if (pe instanceof Allocation) {
      // remove planelement from the asset's roleschedule
      //removePERS(pe);
      allocationRemoved((Allocation) pe);
    } else if (pe instanceof Expansion) {
      // Do nothing
    } else if (pe instanceof AssetTransfer) {
      // remove planelement from the asset's roleschedule
      //removePERS(pe);
      assetTransferRemoved((AssetTransfer) pe);
    } else if (pe instanceof Aggregation) {
      // Do nothing
    } else if (pe instanceof Disposition) {
      // do nothing since its the end of the line
    } else {
      logger.error(self + ": Unknown planelement "+pe, new Throwable());
    }
  }

  /** rescind the cascade of an allocation */
  private void allocationRemoved(Allocation all) {
    //logger.printDot("a");
    Asset a = all.getAsset();
    ClusterPG cpg = a.getClusterPG();
    if (cpg != null) {
      MessageAddress cid = cpg.getMessageAddress();
      if (cid != null) {
        UID remoteUID = ((AllocationforCollections) all).getAllocationTaskUID();
        if (remoteUID != null) {
	  if (logger.isDebugEnabled())
	    logger.debug(self + ": Removed Allocation, so will propagate and rescind alloc task. Alloc: " + all + ", alloc task: " + remoteUID);
          TaskRescind trm = ldmf.newTaskRescind(remoteUID, cid);
          ((AllocationforCollections) all).setAllocationTaskUID(null);
          rootplan.sendDirective((Directive) trm);
        }
      }
    }
  }

  /** remove a task and any PE addressing it */
  private void removeTask(Task task) {
    if (task != null) {
      rootplan.remove(task);
    }
  }

  /** remove the PE associated with a task (does not remove the task) */
  private void taskRemoved(Task task) {
    if (CHECKCONSISTENCY) {
      // Is the task on the LogPlan? If it is, re remove it    
      Task bT = logplan.findTask(task.getUID());
      if (bT == task) {
	if (DOREMOVES) {
	  if (logger.isWarnEnabled())
	    logger.warn(self + ": removed Task still on LogPlan? Re-removing " + task);
	  removeTask(task);
	  return; // Do the follow-on cleanup the next time around
	} else {
	  if (logger.isWarnEnabled())
	    logger.warn(self + ": removed Task still on LogPlan? " + task);
	}
      } else if (bT != null) {
	if(logger.isWarnEnabled())
	  logger.warn(self + ": removed Task UID listed as on LogPlan, but it's a different object. Removed " + task + " and LogPlan has " + bT);
	// FIXME: Remove task? bT?
	return;
      }
    } // end of consistency checks

    // get the planelement with this task
    PlanElement taskpe = task.getPlanElement();

    PlanElement bpe = null;
    if (CHECKCONSISTENCY) {
      bpe = logplan.findPlanElement(task);
      if (taskpe != bpe) {
	// Task and logplan reference different PEs for this Task
	if (taskpe == null) {
	  // Task has no PE link (as though the PE had already been removed)
	  // But the logplan has a PE still
	  // Presumably a publishRemove(PE) just happened in another transaction,
	  // but has not finished.
	  if (logger.isInfoEnabled())
	    logger.info(self + ": Removed Task has no PE, but logplan still lists a PE under this UID. Task: " + task + ", LogPlan's PE: " + bpe.getUID() + ":" + bpe);
	} else if (bpe == null) {
	  // Presumably a publishAdd(PE) just happened in another transaction,
	  // but has not finished
	  if (logger.isInfoEnabled())
	    logger.info(self + ": Removed task lists a PE, but LogPlan has no PE under this UID. Task: " + task + ", Task's PE: " + taskpe.getUID() + ":" + taskpe);
	} else {
	  if (logger.isWarnEnabled())
	    logger.warn(self + ": Removed task lists a different PE than LogPlan has. Task: " + task + ", Task's PE: " + taskpe.getUID() + ":" + taskpe + ", LogPlan's PE: " + bpe.getUID() + ":" + bpe);
	}
      } // end of consistency checks
      
      // rescind (or remove) this planelement from the logplan
      if (DOREMOVES && bpe != null) {
	removePlanElement(bpe, false);
      }
    }
    
    // If we didn't just remove the only PE above, remove
    // the pe referenced by the task
    if (taskpe != bpe && taskpe != null) {
      // A false below would mean that if bpe == null then taskpe won't
      // be removed. That's old behavior.
      // Use DOREMOVES to toggle this.
      removePlanElement(taskpe, DOREMOVES);
    }
  }

  /** remove the cascade associated with an AssetTransfer **/
  private void assetTransferRemoved(AssetTransfer at) {
    // create an AssetRescind message
    Schedule rescindSchedule;


    //Remove info from local assets
    Asset localAsset = logplan.findAsset(at.getAsset());
    if (localAsset == null) {
      logger.error(self + ": Rescinded transferred asset - " + 
		   at.getAsset() + " - not found in logplan.");
      return;
    }


    if ((at.getAsset() instanceof HasRelationships) &&
        (at.getAssignee() instanceof HasRelationships)) {
      rescindSchedule = ldmf.newAssignedRelationshipSchedule();
      RelationshipSchedule transferSchedule = 
        ((HasRelationships)at.getAsset()).getRelationshipSchedule();

      for (Iterator iterator = new ArrayList(transferSchedule).iterator();
           iterator.hasNext();) {
        Relationship relationship = (Relationship)iterator.next();
        ((NewSchedule)rescindSchedule).add(ldmf.newAssignedRelationshipElement(relationship));
      }
      
      HasRelationships localAssignee = (HasRelationships)logplan.findAsset(at.getAssignee());
      if (localAssignee == null) {
        logger.error(self + ": Rescinded assignee - " + 
		     at.getAssignee() + " - not found in logplan.");
        return;
      }

      // Update local relationship schedules
      RelationshipSchedule localSchedule = 
        ((HasRelationships) localAsset).getRelationshipSchedule();
      localSchedule.removeAll(transferSchedule);        
      
      localSchedule = localAssignee.getRelationshipSchedule();
      localSchedule.removeAll(transferSchedule);
      
      // Update asset avail
      // Remove all current entries denoting asset avail to assignee
      // Will add in new entry based on the current relationship schedule
      NewSchedule assetAvailSchedule = 
        (NewSchedule) localAsset.getRoleSchedule().getAvailableSchedule();
      final Asset assignee = at.getAssignee();
      synchronized (assetAvailSchedule) {
        Collection remove = assetAvailSchedule.filter(new UnaryPredicate() {
	    public boolean execute(Object o) {
	      return ((o instanceof AssignedAvailabilityElement) &&
		      (((AssignedAvailabilityElement)o).getAssignee().equals(assignee)));
	    }  
	  });
        assetAvailSchedule.removeAll(remove);
      } // end sync block

      // Get all relationships with asset
      RelationshipSchedule relationshipSchedule = 
        (localAssignee).getRelationshipSchedule();
      Collection collection = 
        relationshipSchedule.getMatchingRelationships((HasRelationships) localAsset,
                                                      new MutableTimeSpan());
      
      // If any relationships, add a single avail element with the 
      // min start and max end
      if (collection.size() > 0) {
        Schedule schedule = ldmf.newSchedule(new Enumerator(collection));
        
        // Add a new avail element
        synchronized (assetAvailSchedule) {
          assetAvailSchedule.add(ldmf.newAssignedAvailabilityElement((Asset)localAssignee,
                                                                     schedule.getStartTime(),
                                                                     schedule.getEndTime()));
        }
      }

      rootplan.change(localAsset, null);
      rootplan.change(localAssignee, null);
    } else {
      rescindSchedule = at.getSchedule();

      // Update asset avail - remove all current entries which match the rescind
      // schedule
      NewSchedule assetAvailSchedule = 
        (NewSchedule)((Asset)localAsset).getRoleSchedule().getAvailableSchedule();
      final Asset assignee = at.getAssignee();
      synchronized (assetAvailSchedule) {
        //final Asset asset = (Asset)localAsset;
        Collection assignedAvailSchedule = assetAvailSchedule.filter(new UnaryPredicate() {
	    public boolean execute(Object o) {
	      return ((o instanceof AssignedAvailabilityElement) &&
		      (((AssignedAvailabilityElement)o).getAssignee().equals(assignee)));
	    }  
	  });
        
        //iterate over rescind schedule and remove matching avail elements
        for (Iterator iterator = rescindSchedule.iterator();
             iterator.hasNext();) {
          ScheduleElement rescind = (ScheduleElement)iterator.next();
    
          Iterator localIterator = assignedAvailSchedule.iterator();
      
          //boolean found = false;
          while (localIterator.hasNext()) {
            ScheduleElement localAvailability = 
              (ScheduleElement)localIterator.next();

            if ((rescind.getStartTime() == localAvailability.getStartTime()) &&
                (rescind.getEndTime() == localAvailability.getEndTime())) {
              assignedAvailSchedule.remove(localAvailability);
              break;
            }
          }
        }
      }
      rootplan.change(localAsset, null);
    }
   
    AssetRescind arm = ldmf.newAssetRescind(at.getAsset(), 
                                            at.getAssignee(),
                                            rescindSchedule);
    rootplan.sendDirective((Directive)arm);
  }
  
  /** remove the plan element from the asset's roleschedule **/
  private void removePERS(PlanElement pe) {
    /*
      boolean conflict = false;
    */
    Asset rsasset = null;
    if (pe instanceof Allocation) {
      Allocation alloc = (Allocation) pe;
      rsasset = alloc.getAsset();
      /*
	if ( alloc.isPotentialConflict() ) {
        conflict = true;
	}
      */
    } else if (pe instanceof AssetTransfer) {
      AssetTransfer at = (AssetTransfer) pe;
      rsasset = at.getAsset();
      /*
	if ( at.isPotentialConflict() ) {
        conflict = true;
	}
      */
    }
    if (rsasset != null) {
      if (logger.isDebugEnabled()) {
	logger.debug(self + " RESCIND REMOVEPERS called for: " + rsasset);
      }
      /*
	RoleScheduleImpl rsi = (RoleScheduleImpl) rsasset.getRoleSchedule();
	// if the pe had a conflict re-check the roleschedule
	if (conflict) {
        checkConflictFlags(pe, rsi);
	}
      */
    } else {
      if (logger.isWarnEnabled())
	logger.warn(self + " Could not remove rescinded planelement");
    }
  }
  
  /*
    // if the rescinded pe had a potential conflict re-set the conflicting pe(s)
    private void checkConflictFlags(PlanElement pe, RoleSchedule rs) {
    // re-set any existing items in the conflict list.
    conflictlist.clear();
    AllocationResult estar = pe.getEstimatedResult();
    
    // make sure that the start time and end time aspects are defined.
    // if they aren't, don't check anything
    // (this could happen with a propagating failed allocation result).
    if ( (estar.isDefined(AspectType.START_TIME) ) && (estar.isDefined(AspectType.END_TIME) ) ) {
    Date sdate = new Date( ((long)estar.getValue(AspectType.START_TIME)) );
    Date edate = new Date( ((long)estar.getValue(AspectType.END_TIME)) );
    
    // check for encapsulating schedules of other plan elements.
    OrderedSet encap = rs.getEncapsulatedRoleSchedule(sdate, edate);
    Enumeration encapconflicts = encap.elements();
    while (encapconflicts.hasMoreElements()) {
    PlanElement conflictpe = (PlanElement) encapconflicts.nextElement();
    // make sure its not our pe.
    if ( !(conflictpe == pe) ) {
    conflictlist.add(conflictpe);
    }
    }
    
    // check for ovelapping schedules of other plan elements.
    OrderedSet overlap = rs.getOverlappingRoleSchedule(sdate, edate);
    Enumeration overlapconflicts = overlap.elements();
    while (overlapconflicts.hasMoreElements()) {
    PlanElement overconflictpe = (PlanElement) overlapconflicts.nextElement();
    // once again, make sure its not our pe.
    if ( !(overconflictpe == pe) ) {
    conflictlist.add(overconflictpe);
    }
    }
    }
    
    if ( ! conflictlist.isEmpty() ) {
    ListIterator lit = conflictlist.listIterator();
    while ( lit.hasNext() ) {
    RoleScheduleConflicts conpe = (RoleScheduleConflicts) lit.next();
    // re-set this pe's conflict flag to false
    conpe.setPotentialConflict(false);
    // set the check flag to true so that the RoleScheduleConflictLP will
    // run again on the publish change in case this pe had conflicts with
    // other pe's (besides the one that was just rescinded)
    conpe.setCheckConflicts(true);
    rootplan.change(conpe);
    }
    }
    }
  */

  public static class DeferredRescind implements java.io.Serializable {
    public TaskRescind tr = null;
    public Task t = null;
    public int tryCount = 0;
    public DeferredRescind(TaskRescind tr) {
      this.tr = tr;
    }

    public DeferredRescind(Task t) {
      this.t = t;
    }
  }

}
