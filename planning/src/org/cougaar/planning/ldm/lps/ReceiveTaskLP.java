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

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.cougaar.core.blackboard.Directive;
import org.cougaar.core.blackboard.SubscriberException;
import org.cougaar.core.domain.LogicProvider;
import org.cougaar.core.domain.MessageLogicProvider;
import org.cougaar.core.domain.RootPlan;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.planning.ldm.LogPlan;
import org.cougaar.planning.ldm.plan.Context;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.TaskImpl;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * Sample LogicProvider for use by ClusterDispatcher to
 * take an incoming Task (excepting Rescind task) and
 * add to the LogPlan w/side-effect of also disseminating to
 * other subscribers.
 * Only adds tasks that haven't been seen before, allowing stability
 * in the face of wire retransmits.
 **/

public class ReceiveTaskLP
implements LogicProvider, MessageLogicProvider
{
  private Logger logger;

  private final RootPlan rootplan;
  private final LogPlan logplan;
  private Set existingPhrases = new HashSet();
  private Set newPhrases = new HashSet();

  public ReceiveTaskLP(
      RootPlan rootplan,
      LogPlan logplan,
      MessageAddress self)
  {
    this.rootplan = rootplan;
    this.logplan = logplan;
    logger = new LoggingServiceWithPrefix(Logging.getLogger(ReceiveTaskLP.class),
                                          self.toString() + ": ");
  }

  public void init() {
  }

  /**
   * Adds Task to LogPlan... Side-effect = other subscribers also
   * updated. If the task is already in the logplan, then there is
   * probably a change in task preferences. If there is no change in
   * task preferences, then it might be the case that the sending
   * agent has undergone a restart and is trying to resynchronize
   * its tasks. We need to activate the NotificationLP to send the
   * estimated allocation result for the plan element of the task. We
   * do this by publishing a change of the plan element (if it
   * exists).
   * Also, the received task may have been deleted. If so, complete
   * the deletion process by actually removing the task from the
   * blackboard.
   **/
  public void execute(Directive dir, Collection changes) {
    if (dir instanceof Task) {
      Task tsk = (Task) dir;
      try {
        Task existingTask = logplan.findTask(tsk);
        // We don't know about this task
        if (existingTask == null) {
          if (tsk.isDeleted()) {
            // Ignore received deleted tasks
            if (logger.isDebugEnabled()) {
              logger.debug("Ignoring new, but deleted task from another node " + tsk.getUID());
            }
          } else {
            // Add it to our blackboard
            if (logger.isDebugEnabled()) {
              logger.debug("Received new task from another node " + tsk.getUID());
            }
            rootplan.add(tsk);
          }
        } else if (tsk.isDeleted()) {
          if (existingTask.isDeleted()) {
            if (logger.isDebugEnabled()) {
              logger.debug("Removing deleted task " + tsk.getUID());
            }
            rootplan.remove(existingTask); // Complete the removal
          } else {
            // Whoops! We must have restarted and reverted to a
            // pre-deletion state.
            if (logger.isDebugEnabled()) {
              logger.debug("Received deleted task, but blackboard task is undeleted "
                           + tsk.getUID());
            }
          }
        } else if (tsk == existingTask) {
          // This never happens any more because a new task is created
          // for each transmission
          if (logger.isWarnEnabled()) {
            logger.warn("Received task instance already on blackboard " + tsk.getUID());
          }
          rootplan.change(existingTask, changes);
        } else {
          // Update task from received task
          boolean changedTask = false;
          Preference[] newPreferences = ((TaskImpl) tsk).getPreferencesAsArray();
          Preference[] existingPreferences = ((TaskImpl) existingTask).getPreferencesAsArray();
          if (logger.isDebugEnabled()) {
            logger.debug("Comparing " + tsk);
          }
          if (!java.util.Arrays.equals(newPreferences, existingPreferences)) {
            if (logger.isDebugEnabled()) {
              logger.debug("Preferences differ "
                           + newPreferences +
                           "!="
                           + existingPreferences);
            }
            ((NewTask) existingTask).setPreferences(tsk.getPreferences());
            changedTask = true;
          } else {
            if (logger.isDebugEnabled()) {
              logger.debug("Preferences compare equal "
                           + newPreferences
                           + "=="
                           + existingPreferences);
            }
          }
          for (Enumeration e = existingTask.getPrepositionalPhrases(); e.hasMoreElements(); ) {
            existingPhrases.add(e.nextElement());
          }
          for (Enumeration e = tsk.getPrepositionalPhrases(); e.hasMoreElements(); ) {
            newPhrases.add(e.nextElement());
          }


	  boolean match = false;

	  if (newPhrases.size() == existingPhrases.size()) {
	    for (Iterator existingIterator = existingPhrases.iterator();
		 existingIterator.hasNext();) {
	      Object existingPhrase = existingIterator.next();
	      match = false;

	      for (Iterator newIterator = newPhrases.iterator();
		   newIterator.hasNext();) {
		if (newIterator.next().equals(existingPhrase)) {
		  match = true;
		  break;
		}
	      }
	      
	      if (match == false) {
		break;
	      }
	    }
	  }
		 

          if (!match) {
            ((NewTask) existingTask).setPrepositionalPhrases(tsk.getPrepositionalPhrases());
            changedTask = true;
            if (logger.isDebugEnabled()) {
              logger.debug("Phrases differ " + newPhrases + "!=" + existingPhrases);
            }
          } else {
            if (logger.isDebugEnabled()) {
              logger.debug("Phrases compare equal " + newPhrases + "==" + existingPhrases);
            }
          }
            
          existingPhrases.clear();
          newPhrases.clear();
          Context existingContext = existingTask.getContext();
          Context tskContext = tsk.getContext();
          if (logger.isWarnEnabled()) {
            if (existingContext == null) {
              logger.warn("existingTask has null context: " + existingTask);
            }
            if (tskContext == null) {
              logger.warn("received Task has null context: " + tsk);
            }
          }
          if (((existingContext != null) && !existingContext.equals(tskContext)) ||
	      ((existingContext == null) && (tskContext != null))) {
            ((NewTask) existingTask).setContext(tskContext);
            changedTask = true;
            if (logger.isDebugEnabled()) {
              logger.debug("Contexts differ: "
                           + tskContext
                           + "!="
                           + existingContext);
            }
          }
          if (!existingTask.getVerb().equals(tsk.getVerb())) {
            ((NewTask) existingTask).setVerb(tsk.getVerb());
            changedTask = true;
            if (logger.isDebugEnabled()) {
              logger.debug("Verbs differ "
                           + tsk.getVerb()
                           + "!="
                           + existingTask.getVerb());
            }
          } 
          if (changedTask) {
            rootplan.change(existingTask, changes);
          } else {
            PlanElement pe = existingTask.getPlanElement();
            if (pe != null) {
              rootplan.change(pe, changes);	// Cause estimated result to be resent
            }
          }
        }
      } catch (SubscriberException se) {
        logger.error("Could not add Task to LogPlan: " + tsk, se);
      }
    }
  }
}
