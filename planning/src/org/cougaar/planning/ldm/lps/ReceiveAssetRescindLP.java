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

import org.cougaar.core.blackboard.*;

import org.cougaar.core.domain.*;
import org.cougaar.planning.ldm.*;
import org.cougaar.core.domain.MessageLogicProvider;

import java.util.*;

import org.cougaar.core.agent.*;

import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.planning.ldm.plan.AssetRescind;
import org.cougaar.planning.ldm.plan.AssignedAvailabilityElement;
import org.cougaar.planning.ldm.plan.AssignedRelationshipElement;
import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.NewSchedule;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.Schedule;

import org.cougaar.core.util.UID;

import org.cougaar.util.Enumerator;
import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;



/**
 * Catch assets so that we can relink the relationships properly.
 **/

public class ReceiveAssetRescindLP
implements LogicProvider, MessageLogicProvider
{
  private static final Logger logger = Logging.getLogger(ReceiveAssetRescindLP.class);

  private final RootPlan rootplan;
  private final LogPlan logplan;
  private final PlanningFactory ldmf;

  public ReceiveAssetRescindLP(
      RootPlan rootplan,
      LogPlan logplan,
      PlanningFactory ldmf) {
    this.rootplan = rootplan;
    this.logplan = logplan;
    this.ldmf = ldmf;
  }
  
  public void init() {
  }

  /**
   *  perform updates -- per Rescind ALGORITHM --
   *
   **/
  public void execute(Directive dir, Collection changes) {
    // drop changes
    if (dir instanceof AssetRescind) {
      receiveAssetRescind((AssetRescind)dir);
    }
  }

  private final static boolean related(Asset a) {
    return (a instanceof HasRelationships); 
  }

  private void receiveAssetRescind(AssetRescind ar) {
    Asset localAsset = logplan.findAsset(ar.getAsset());
    if (localAsset == null) {
      logger.error("Rescinded asset - " + ar.getAsset() + 
		   " - not found in logplan.");
      return;
    }

    Asset localAssignee = logplan.findAsset(ar.getRescindee());
    if (localAssignee == null) {
      logger.error("Assignee asset - " + 
		   ar.getRescindee() + " - not found in logplan.");
      return;
    }


    if (related(ar.getAsset()) &&
        related(ar.getRescindee())) {
      updateRelationshipSchedules(ar, localAsset, localAssignee);
    }

    updateAvailSchedule(ar, localAsset, localAssignee);
  
    rootplan.change(localAsset, null);
    rootplan.change(localAssignee, null);
  }

  private void updateRelationshipSchedules(AssetRescind ar,
                                           Asset asset,
                                           Asset assignee) {

    RelationshipSchedule assetRelationshipSchedule = 
      ((HasRelationships) asset).getRelationshipSchedule();

    RelationshipSchedule assigneeRelationshipSchedule = 
      ((HasRelationships) assignee).getRelationshipSchedule();


    // Remove matching relationships
    Collection rescinds = convertToRelationships(ar, asset, assignee);

    assetRelationshipSchedule.removeAll(rescinds);

    assigneeRelationshipSchedule.removeAll(rescinds);
  }

  // Update availability info for the asset (aka transferring asset)
  // AvailableSchedule reflects availablity within the current agent
  private void updateAvailSchedule(AssetRescind ar,
                                   Asset asset,
                                   final Asset assignee) {

    NewSchedule assetAvailSchedule = 
      (NewSchedule)asset.getRoleSchedule().getAvailableSchedule();

    if (!related(asset)) {
    
      // Remove Matching Availabilities
      synchronized (assetAvailSchedule) {
        assetAvailSchedule.removeAll(ar.getSchedule());
      }

      // We're done
      return;
    }
    
    //For Assets with relationships, need to recompute the avail schedule
    //based on the relationship schedule

    // Remove all current entries denoting asset avail to assignee
    synchronized (assetAvailSchedule) {
      Collection remove = assetAvailSchedule.filter(new UnaryPredicate() {
        public boolean execute(Object o) {
          return ((o instanceof AssignedAvailabilityElement) &&
                  (((AssignedAvailabilityElement)o).getAssignee().equals(assignee)));
        }  
      });
      assetAvailSchedule.removeAll(remove);
      
      // Get all relationships between asset and assignee
      RelationshipSchedule relationshipSchedule = 
        ((HasRelationships) asset).getRelationshipSchedule();
      Collection collection = 
        relationshipSchedule.getMatchingRelationships((HasRelationships) assignee,
                                                      new MutableTimeSpan());
      
      // If any relationships, add a single avail element with the 
      // min start and max end
      if (collection.size() > 0) {
        Schedule schedule = ldmf.newSchedule(new Enumerator(collection));
        
        // Add a new avail element
        assetAvailSchedule.add(ldmf.newAssignedAvailabilityElement(assignee,
                                                                   schedule.getStartTime(),
                                                                   schedule.getEndTime()));
      }
    } // end sync block
  }

  protected Collection convertToRelationships(AssetRescind ar,
                                              Asset asset,
                                              Asset assignee) {
    ArrayList relationships = new ArrayList(ar.getSchedule().size());

    // Safe because ar.getSchedule is an AssignedRelationshipScheduleImpl.
    // AssignedRelationshipImpl supports iterator. (Assumption is that
    // AssignedRelaionshipImpl is only used/processed by LPs.)
    for (Iterator iterator = ar.getSchedule().iterator(); 
         iterator.hasNext();) {
      AssignedRelationshipElement rescindElement = 
        (AssignedRelationshipElement)iterator.next();
      
      Relationship relationship = ldmf.newRelationship(rescindElement,
                                                       asset,
                                                       assignee);
      
      relationships.add(relationship);
    }

    return relationships;
  }
}
 






