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

package org.cougaar.planning.plugin.asset;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import org.cougaar.core.blackboard.AnonymousChangeReport;
import org.cougaar.core.blackboard.ChangeReport;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;

import org.cougaar.planning.Constants;
import org.cougaar.planning.ldm.PlanningFactory;

import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AssetTransfer;
import org.cougaar.planning.ldm.plan.NewPrepositionalPhrase;
import org.cougaar.planning.ldm.plan.NewSchedule;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Verb;

import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.planning.plugin.util.PluginHelper;

import org.cougaar.util.UnaryPredicate;

/**
 * AssetReportPlugin manages REPORTFORDUTY and REPORTFORSERVICE relationships
 * Handles both expansion and allocation of these tasks.
 * @see org.cougaar.core.plugin.legacy.SimplifiedPlugin
 * @see org.cougaar.core.plugin.legacy.SimplifiedPluginTest
 */

public class AssetReportPlugin extends SimplePlugin
{
  protected PlanningFactory myPlanningFactory;
  private IncrementalSubscription myTasks;
  private IncrementalSubscription myAssetTransfers;

  private IncrementalSubscription myLocalAssets;

  protected LoggingService myLogger;

  //Override the setupSubscriptions() in the SimplifiedPlugin.
  protected void setupSubscriptions() {
    myPlanningFactory = (PlanningFactory) getFactory("planning");
    if (myPlanningFactory == null) {
      throw new RuntimeException("Missing \"planning\" factory");
    }

    myLogger = (LoggingService)
      getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
    if (myLogger == null) {
      myLogger = LoggingService.NULL;
    }

    // subscribe for incoming Report Tasks 
    myTasks = 
      (IncrementalSubscription) subscribe(getTaskPredicate());

    // subscribe to my allocations in order to propagate allocationresults
    myAssetTransfers = (IncrementalSubscription)subscribe(getAssetTransferPred());

    // subscribe to my local assets so I can propagate modifications
    myLocalAssets = (IncrementalSubscription)subscribe(getLocalAssetPred());
  }
  
  public void execute() {
    // Handle REPORT tasks, expanding and allocating all at once
    if (myTasks.hasChanged()) {
      if (myLogger.isInfoEnabled())
	myLogger.info(getAgentIdentifier() + " had RFD or RFS task subscription fire");
      Enumeration newtasks = myTasks.getAddedList();
      while (newtasks.hasMoreElements()) {
        Task currentTask = (Task)newtasks.nextElement();
	if (myLogger.isInfoEnabled())
	  myLogger.info("       with added Task to now allocate: " + currentTask);
        allocate(currentTask);
      }
    }  
  
    // If get back a reported result, automatically send it up.
    if (myAssetTransfers.hasChanged()) {
      if (myLogger.isInfoEnabled())
	myLogger.info(getAgentIdentifier() + " had AssetTransfer with RFD or RFS task subscription fire");
      Enumeration changedallocs = myAssetTransfers.getChangedList();
      boolean didLog = false;
      int notSent = 0;
      PlanElement cpe = null;
      while (changedallocs.hasMoreElements()) {
        cpe = (PlanElement)changedallocs.nextElement();
        if (PluginHelper.updatePlanElement(cpe)) {
	  if (myLogger.isInfoEnabled()) {
	    myLogger.info("        with a changed PE to propagate up: " + cpe);
	    didLog = true;
	  }
          publishChange(cpe);
        } else if (myLogger.isInfoEnabled()) {
	  notSent++;
	}
      }
      if (!didLog && myLogger.isInfoEnabled())
	myLogger.info("      with " + notSent + " changed ATs but no PE changes to propagate. Last PE was: " + cpe);
    }
  
    if (myLocalAssets.hasChanged()) {
      if (myLogger.isInfoEnabled())
	myLogger.info(getAgentIdentifier() + " had Local HasRelationship asset subscription fire -- will resend AssetTransfers if the ChangeReport is not a RelationshipSchedule change");
      resendAssetTransfers();
    }
  }

  /**
   * getTaskPredicate - returns task predicate for task subscription
   * Default implementation subscribes to all non-internal tasks. Derived classes
   * should probably implement a more specific version.
   * 
   * @return UnaryPredicate - task predicate to be used.
   */
  protected UnaryPredicate getTaskPredicate() {
    return allReportTaskPred();
  }

  protected UnaryPredicate getAssetTransferPred() {
    return allReportAssetTransferPred();
  }

  protected UnaryPredicate getLocalAssetPred() {
    return new allLocalAssetPred(getMessageAddress());
  }

  private void allocate(Task task) {
    if (task.getPlanElement() != null) {
      myLogger.error(getMessageAddress().toString()+
		     "/AssetReportPlugin: unable to process " + task.getUID() + 
		     " - task already has a PlanElement - " + 
		     task.getPlanElement() + ".\n");
      return;
    }

    Asset reportingAsset = task.getDirectObject();

    if (!reportingAsset.getClusterPG().getMessageAddress().equals(getMessageAddress())) {
      allocateRemote(task);
    } else {
      allocateLocal(task);
    }
  }

  private void allocateLocal(Task task) {
    Asset reportingAsset = task.getDirectObject();
    Asset reportee = (Asset) findIndirectObject(task, Constants.Preposition.FOR);

    Asset localReportingAsset = findLocalAsset(reportingAsset);
    if ((localReportingAsset == null) ||
        (!((HasRelationships )localReportingAsset).isLocal())) {
        //(!localReportingAsset.getClusterPG().getMessageAddress().equals(getMessageAddress()))) {
      myLogger.error(getMessageAddress().toString()+
		     "/AssetReportPlugin: unable to process " + 
		     task.getVerb() + " task - " + 
		     reportingAsset + " reporting to " + reportee + ".\n" +
		     reportingAsset + " not local to this cluster."
		     );
      return;
    }
    
    long startTime = (long) task.getPreferredValue(AspectType.START_TIME);
    long endTime = (long) task.getPreferredValue(AspectType.END_TIME);
    

    // Make RelationshipSchedule for the reporting asset
    Collection roles = 
      (Collection) findIndirectObject(task, Constants.Preposition.AS);
    RelationshipSchedule schedule = 
      myPlanningFactory.newRelationshipSchedule((HasRelationships) reportingAsset);
    for (Iterator iterator = roles.iterator(); iterator.hasNext();) {
      Relationship relationship = 
        myPlanningFactory.newRelationship((Role) iterator.next(),
					  (HasRelationships) reportingAsset,
					  (HasRelationships) reportee,
					  startTime,
					  endTime);
      schedule.add(relationship);
    }
    ((HasRelationships) reportingAsset).setRelationshipSchedule(schedule);
    
    // create the transfer
    NewSchedule availSchedule = 
      myPlanningFactory.newSimpleSchedule(startTime,
					  endTime);

    AllocationResult newEstimatedResult = 
      PluginHelper.createEstimatedAllocationResult(task,
                                                   theLDMF,
                                                   1.0,
                                                   true);

    AssetTransfer assetTransfer = 
      myPlanningFactory.createAssetTransfer(task.getPlan(), task, 
					    reportingAsset,
					    availSchedule, 
					    reportee,
					    newEstimatedResult, 
					    Role.ASSIGNED);
    publishAdd(assetTransfer);
  }

  private void allocateRemote(Task task) {
    Asset reportingAsset = task.getDirectObject();
      
    AllocationResult newEstimatedResult = 
      PluginHelper.createEstimatedAllocationResult(task,
                                                   theLDMF,
                                                   1.0,
                                                   true);
    
    Allocation allocation = 
      myPlanningFactory.createAllocation(task.getPlan(), task, 
					 reportingAsset,
					 newEstimatedResult, 
					 Role.ASSIGNED);
    publishAdd(allocation);
    return;
  }

  protected Asset findLocalAsset(Asset asset) {
    final Object key = asset.getKey();
    Asset localAsset = null;
    
    // Query subscription to see if clientAsset already exists
    Collection collection = query(new UnaryPredicate() {
      
      public boolean execute(Object o) {
        if ((o instanceof Asset) &&
            (((Asset) o).getKey().equals(key)))
          return true;
        else {
          return false;
        }
      }
    });

    if (collection.size() > 0) {
      Iterator iterator = collection.iterator();
      localAsset = (Asset)iterator.next();

      if (iterator.hasNext()) {
        throw new RuntimeException("AssetReportPlugin - multiple assets with UIC = " + 
                                   key);
      }
    } 

    return localAsset;
  }

  // ###############################################################
  // END Allocation
  // ###############################################################

  protected Object findIndirectObject(Task _task, String _prep) {
    PrepositionalPhrase pp = _task.getPrepositionalPhrase(_prep);
    if (pp == null)
      throw new RuntimeException("Didn't find a single \"" + _prep + 
                                 "\" Prepositional Phrase in " + _task);

    return pp.getIndirectObject();
  }

  private void resendAssetTransfers() {
    // BOZO - No support for removal of a local Asset
    Collection changes = myLocalAssets.getChangedCollection();
    if ((changes == null) || 
        (changes.isEmpty())) {
      return;
    }

    for (Iterator iterator = changes.iterator();
         iterator.hasNext();) {
      Asset localAsset = (Asset) iterator.next();
      // Determine whether or not asset transfers for the local asset should be 
      // resent.  At this point, do not resend just because the relationship 
      // schedule changed. Warning - legtimate change will get lost if batched
      // with relationship schedule changes unless a separate change report is 
      // generated.
      Collection changeReports = myLocalAssets.getChangeReports(localAsset);
      boolean resendRequired = false;

      if ((changeReports != AnonymousChangeReport.SET) &&
          (changeReports != null)) {
        for (Iterator reportIterator = changeReports.iterator();
             reportIterator.hasNext();) {
          ChangeReport report = (ChangeReport) reportIterator.next();
          if (!(report instanceof RelationshipSchedule.RelationshipScheduleChangeReport)) {
            resendRequired = true;
            break;
          }
        }
      } else {
        resendRequired = true;
      }
      if (resendRequired) {
        resendAssetTransfers(localAsset, myAssetTransfers.getCollection(), changeReports);
      }
    }
  }

  /**
     Resend a collection of AssetTransfers. Asset transfers are "sent"
     by doing a publishChange. The change reports are supplied by the
     caller, but are just the change reports of the change that
     initiated this resend. Only transfers of our Asset to
     other Assets are sent, the rest are ignored.
   **/
  private void resendAssetTransfers(Asset localAsset,
                                    Collection transfers,
                                    Collection changeReports)
  {
    for (Iterator i = transfers.iterator(); i.hasNext();) {
      AssetTransfer at = (AssetTransfer) i.next();
      if (at.getAsset().equals(localAsset)) {
        if (at.getAssignee().equals(localAsset)) {
	  if (myLogger.isDebugEnabled()) {
	    myLogger.debug(getAgentIdentifier() + 
			   " resendAssetTransfers: not resending " + at);
	  }
        } else {
	  if (myLogger.isInfoEnabled())
	    myLogger.info(getAgentIdentifier() + " IS resending AssetTransfer of self to " + at.getAssignee());
          at.indicateAssetChange();
          publishChange(at, changeReports);
        }
      }
    }
  }

  // #######################################################################
  // BEGIN predicates
  // #######################################################################
  
  // predicate for getting allocatable tasks of report for duty
  private static UnaryPredicate allReportTaskPred() {
    return new UnaryPredicate() {
      public boolean execute(Object o) {
	if (o instanceof Task) {
          Task task = (Task) o;
	  if (task.getVerb().equals(Constants.Verb.REPORT)) {
	    return true;
          }
	}
	return false;
      }
    };
  }

  private static UnaryPredicate allReportAssetTransferPred() {
    return new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof AssetTransfer) {
          Task t = ((AssetTransfer)o).getTask();
          if (t.getVerb().equals(Constants.Verb.REPORT)) {
            // if the PlanElement is for the correct kind of task then
            // make sure it's an assettransfer
            return true;
          }
        }
        return false;
      }
    };
  }

  private static class allLocalAssetPred implements UnaryPredicate {
    private final MessageAddress myCID;
    public allLocalAssetPred(MessageAddress cid) {
      super();
      myCID = cid;
    }
    public boolean execute(Object o) {
      if ((o instanceof Asset) &&
          (o instanceof HasRelationships) &&
          (((Asset) o).hasClusterPG())) {
        return ((Asset) o).getClusterPG().getMessageAddress().equals(myCID);
      } else {
        return false;
      }
    }
  }
}

