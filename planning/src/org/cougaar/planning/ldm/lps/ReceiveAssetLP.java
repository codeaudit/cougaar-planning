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

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.blackboard.*;
import org.cougaar.planning.ldm.*;
import org.cougaar.core.domain.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import org.cougaar.core.agent.*;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.LocalPG;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.asset.PropertyGroupSchedule;

import org.cougaar.planning.ldm.plan.AssetAssignment;
import org.cougaar.planning.ldm.plan.AssignedRelationshipElement;
import org.cougaar.planning.ldm.plan.AssignedAvailabilityElement;
import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.NewRelationshipSchedule;
import org.cougaar.planning.ldm.plan.NewRoleSchedule;
import org.cougaar.planning.ldm.plan.NewSchedule;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleElement;

import org.cougaar.util.Enumerator;
import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;


/**
  * Sample LogicProvider for use by ClusterDispatcher to
  * take an incoming AssetAssignment Directive and
  * add Asset to the LogPlan w/side-effect of also disseminating to
  * other subscribers.
  **/

public class ReceiveAssetLP
implements LogicProvider, MessageLogicProvider {
  private static Logger logger = Logging.getLogger(ReceiveAssetLP.class);

  private final RootPlan rootplan;
  private final LogPlan logplan;
  private final PlanningFactory ldmf;
  private final MessageAddress self;

  private static TimeSpan ETERNITY = new MutableTimeSpan();

  public ReceiveAssetLP(
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
   * Adds Assets  to LogPlan... Side-effect = other subscribers
   * also updated.
   **/
  public void execute(Directive dir, Collection changes)
  {
    if (dir instanceof AssetAssignment) {
      AssetAssignment aa  = (AssetAssignment)dir;
      if (logger.isDebugEnabled()) logger.debug("Received " + aa);
      receiveAssetAssignment(aa);
    }
  }

  private final static boolean related(Asset a) {
    return (a instanceof HasRelationships); 
  }

  private void receiveAssetAssignment(AssetAssignment aa) {
    // figure out the assignee
    Asset assigneeT = aa.getAssignee();// assignee from message
    Asset assigneeL = logplan.findAsset(assigneeT); // local assignee instance
    
    if (assigneeL == null) {
      logger.error("ReceiveAssetLP: Unable to find receiving asset " + 
                   assigneeT + " in "+self);
      return;
    }
    Asset assignee = assigneeL;

    // figure out the asset being transferred
    Asset assetT = aa.getAsset();   // asset from message
    Asset assetL = logplan.findAsset(assetT);// local instance of asset

    Asset asset = assetL;

    if (asset == null) {
      // Clone to ensure that we don't end up with cross agent asset 
      // references
      asset = ldmf.cloneInstance(assetT);
      if (related(asset)) {
        ((HasRelationships) asset).setLocal(false);
      }
    } 

    boolean updateRelationships =
      aa.getSchedule() != null && related(assetT) && related(assignee);

    //Only munge relationships pertinent to the transfer - requires that 
    //both receiving and transferring assets have relationship schedules
    if (updateRelationships) {
      HasRelationships dob = ((HasRelationships)asset);
      NewRelationshipSchedule rs_a;

      if (assetL == null) {
        // Serialized object does not include a relationship schedule
        rs_a = ldmf.newRelationshipSchedule(dob);
        dob.setRelationshipSchedule(rs_a);        
      } else {
        rs_a = (NewRelationshipSchedule)dob.getRelationshipSchedule();
      }

      HasRelationships iob = ((HasRelationships)assignee);
      NewRelationshipSchedule rs_assignee = 
        (NewRelationshipSchedule)iob.getRelationshipSchedule();

      if (aa.isUpdate() || aa.isRepeat()) {
        removeExistingRelationships(aa.getSchedule(), rs_a, rs_assignee);
      }

      addRelationships(aa, rs_a, rs_assignee);
      
      Collection changeReports = new ArrayList();
      changeReports.add(new RelationshipSchedule.RelationshipScheduleChangeReport());
      rootplan.change(assignee, changeReports);
    }


    if (assetL != null) {
      // If we already had a matching asset - update with property groups
      // from the asset transfer.
      Vector transferredPGs = assetT.fetchAllProperties();

      for (Iterator pgIterator = transferredPGs.iterator();
           pgIterator.hasNext();) {
        Object next = pgIterator.next();

        //Don't propagate LocalPGs
        if (!(next instanceof LocalPG)) {
          if (next instanceof PropertyGroup) {
            assetL.addOtherPropertyGroup((PropertyGroup) next);
          } else if (next instanceof PropertyGroupSchedule) {
            assetL.addOtherPropertyGroupSchedule((PropertyGroupSchedule) next);
          } else {
            logger.error("ReceiveAssetLP: unrecognized property type - " + 
                         next + " - on transferred asset " + assetL);
          }
        }
      }
    } else {
      if (aa.isUpdate() || aa.isRepeat()) {
        logger.error("Received Update Asset Transfer, but cannot find original "+ aa);
      } 
    }

    // fix up the available schedule for the transferring asset
    if (aa.getSchedule() != null) {
      fixAvailSchedule(aa, asset, assignee);
    }
      
    // publish the add or change
    if (assetL == null) {            // add it if it wasn't already there
      rootplan.add(asset);
    } else {
      if (updateRelationships) {
        Collection changeReports = new ArrayList();
        changeReports.add(new RelationshipSchedule.RelationshipScheduleChangeReport());
        rootplan.change(asset, changeReports);
      } else {
        rootplan.change(asset, null);
      }
    }


  }

  protected void addRelationships(AssetAssignment aa,
                                  RelationshipSchedule transferringSchedule,
                                  RelationshipSchedule receivingSchedule) {
    Asset transferring = (Asset)transferringSchedule.getHasRelationships();
    Asset receiving = (Asset)receivingSchedule.getHasRelationships();

    Collection localRelationships = convertToRelationships(aa,
                                                           transferring,
                                                           receiving);
    transferringSchedule.addAll(localRelationships);

    receivingSchedule.addAll(localRelationships);
  }

  private void removeExistingRelationships(Schedule aaSchedule,
                                           NewRelationshipSchedule transferringSchedule,
                                           NewRelationshipSchedule receivingSchedule) {
    HasRelationships transferringAsset = 
      transferringSchedule.getHasRelationships();
    String transferringID = 
      ((Asset) transferringAsset).getItemIdentificationPG().getItemIdentification();

    HasRelationships receivingAsset = 
      receivingSchedule.getHasRelationships();
    String receivingID = 
      ((Asset) receivingAsset).getItemIdentificationPG().getItemIdentification();

    //Safe because aaSchedule should be an AssignedRelationshipSchedule - 
    // supports iterator(). (Assumption is that AssignedRelationshipSchedule
    // is only used by LPs.)
    for (Iterator aaIterator = aaSchedule.iterator();
         aaIterator.hasNext();) {
      AssignedRelationshipElement aaRelationship = 
        (AssignedRelationshipElement) aaIterator.next();
      
      Role role = (aaRelationship.getItemIDA().equals(receivingID)) ?
        aaRelationship.getRoleA() : aaRelationship.getRoleB();
      
      Collection remove = 
        transferringSchedule.getMatchingRelationships(role,
                                                      receivingAsset,
                                                      ETERNITY);
      
      transferringSchedule.removeAll(remove);
      
      role = (aaRelationship.getItemIDA().equals(transferringID)) ?
        aaRelationship.getRoleA() : aaRelationship.getRoleB();
      remove = 
        receivingSchedule.getMatchingRelationships(role,
                                                   transferringAsset,
                                                   ETERNITY);
      
      receivingSchedule.removeAll(remove);
    }
  }

  // Update availability info for the transferred asset
  // AvailableSchedule reflects availability within the current agent
  private void fixAvailSchedule(AssetAssignment aa, Asset asset,
                                final Asset assignee) {
    NewSchedule availSchedule = 
      (NewSchedule)asset.getRoleSchedule().getAvailableSchedule();

    if (availSchedule == null) {
      availSchedule = ldmf.newAssignedAvailabilitySchedule();
      ((NewRoleSchedule)asset.getRoleSchedule()).setAvailableSchedule(availSchedule);
    }

    synchronized (availSchedule) {

      if ((aa.isUpdate() || aa.isRepeat()) || 
          (related(asset) && related(assignee))) {
        Collection remove = availSchedule.filter(new UnaryPredicate() {
          public boolean execute(Object o) {
            return ((o instanceof AssignedAvailabilityElement) &&
                     (((AssignedAvailabilityElement)o).getAssignee().equals(assignee)));
          }
        });
        availSchedule.removeAll(remove);
      }
      
      //Add entries to the available schedule using the assignee
      if (related(asset) && related(assignee)) {
        //Construct aggregate avail info from the relationship schedule
        RelationshipSchedule relationshipSchedule = 
          ((HasRelationships)asset).getRelationshipSchedule();
        Collection collection = 
          relationshipSchedule.getMatchingRelationships((HasRelationships)assignee,
                                                        ETERNITY);
        
        // If any relationships, add a single avail element with the 
        // min start and max end
        if (collection.size() > 0) {
          Schedule schedule = ldmf.newSchedule(new Enumerator(collection));
          availSchedule.add(ldmf.newAssignedAvailabilityElement(assignee,
                                                                schedule.getStartTime(),
                                                                schedule.getEndTime()));
        }
      } else {
        //Copy availability info directly from aa schedule

        //Don't iterate over schedule directly because Schedule doesn't support
        //iterator().
        Iterator iterator = new ArrayList(aa.getSchedule()).iterator();
        while (iterator.hasNext()) {
          ScheduleElement avail = (ScheduleElement)iterator.next();
          availSchedule.add(ldmf.newAssignedAvailabilityElement(assignee, 
                                                                avail.getStartTime(),
                                                                avail.getEndTime()));
        }
      }
    } // end sync block
  }

  protected Collection convertToRelationships(AssetAssignment aa,
                                              Asset transferring,
                                              Asset receiving) {
    ArrayList relationships = new ArrayList(aa.getSchedule().size());

    //Safe because aaSchedule should be an AssignedRelationshipSchedule - 
    // supports iterator(). (Assumption is that AssignedRelationshipSchedule
    // is only used by LPs.)
    for (Iterator iterator = aa.getSchedule().iterator();
         iterator.hasNext();) {
      AssignedRelationshipElement aaRelationship = 
        (AssignedRelationshipElement)iterator.next();
      
      Relationship localRelationship = ldmf.newRelationship(aaRelationship,
                                                            transferring,
                                                            receiving);
      relationships.add(localRelationship);
    }
    
    return relationships;
  }
}









