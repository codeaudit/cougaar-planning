/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.measure.CountRate;
import org.cougaar.planning.ldm.measure.FlowRate;
import org.cougaar.planning.ldm.measure.Rate;
import org.cougaar.planning.ldm.plan.AspectRate;
import org.cougaar.planning.ldm.plan.AspectType; // inlined
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.Context;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.Plan;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.planning.ldm.predicate.TaskPredicate;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.util.UnaryPredicate;

/**
 * A plugin for testing completion detection. Injects a slug of new
 * Supply tasks every day (of execution) based on the ProjectSupply
 * tasks in the logplan. Each ProjectSupply leads to one Supply task
 * for which the quantity is one day's projected demand. The intention
 * is to stimulate the inventory manager to run and set off a wave of
 * activity through the society that the completion plugins detect to
 * inhibit the speeded up advancement of time until the cessation of
 * activity is detected.
 **/
public class TestCompletionPlugin extends SimplePlugin {
  private static final long ONE_DAY = 86400000L;
  private static final long TIME_OFFSET = ONE_DAY / 2;
  private static final Verb SUPPLY = Verb.getVerb("Supply");
  private static final Verb PROJECTSUPPLY = Verb.getVerb("ProjectSupply");
  private static final String MAINTAINING = "Maintaining";
  private static final String FOR = "For";
  private static final int DEMANDRATE = AspectType.N_CORE_ASPECTS + 0;

  private long demoNow;
  private long nextWakeTime;
  private Alarm timer;
  private LoggingService logger;
  private PlanningFactory ldmf;

  public void setLoggingService(LoggingService ls) {
    logger = ls;
  }

  public void setupSubscriptions() {
    ldmf = (PlanningFactory) getFactory("planning");
    demoNow = currentTimeMillis();
    nextWakeTime = TIME_OFFSET + (((demoNow) / ONE_DAY) + 1) * ONE_DAY;
    timer = wakeAt(nextWakeTime);
  }

  public void execute() {
    if (timer.hasExpired()) {
      demoNow = currentTimeMillis();
      do {
        generateSupplyTasks();
        nextWakeTime += ONE_DAY;
      } while (nextWakeTime < demoNow);
      timer = wakeAt(nextWakeTime);
    }
  }

  private String parseMaintainedItemType(String mi) {
    int pos1 = mi.indexOf(": <") + 3;
    int pos2 = mi.indexOf(">", pos1);
    return mi.substring(pos1, pos2);
  }

  private void generateSupplyTasks() {
    final String selfIdentification = getMessageAddress().toString();
    TaskPredicate projectionTaskPredicate = new TaskPredicate() {
        public boolean execute(Task task) {
          if (task.getVerb().equals(PROJECTSUPPLY)) {
            PrepositionalPhrase maintaining =
              task.getPrepositionalPhrase(MAINTAINING);
            if (maintaining != null) {
              String maintainedItem =
                maintaining.getIndirectObject().toString();
              String maintainedItemType =
                parseMaintainedItemType(maintainedItem);
              if (maintainedItemType.equals("Inventory")) return false;
              PrepositionalPhrase pp =
                task.getPrepositionalPhrase(FOR);
              String orgName = (String) pp.getIndirectObject();
              if (selfIdentification.equals(orgName)) {
                return true;
              }
            }
          }
          return false;
        }
      };
    Collection projections = query(projectionTaskPredicate);
    if (projections.size() == 0) return;
    if (logger.isDebugEnabled()){
      logger.debug(selfIdentification
                   + ": "
                   + projections.size()
                   + " projections");
    }
    for (Iterator i = projections.iterator(); i.hasNext(); ) {
      Task projectionTask = (Task) i.next();
      NewTask supplyTask = ldmf.newTask();
      supplyTask.setVerb(SUPPLY);
      supplyTask.setDirectObject(projectionTask.getDirectObject());
      // Copy all but the demand rate pp
      Enumeration fe = projectionTask.getPrepositionalPhrases();
//          new FilteredEnumeration(, demandRateFilter);
      supplyTask.setPrepositionalPhrases(fe);
      supplyTask.setPlan(projectionTask.getPlan());
      supplyTask.setContext(projectionTask.getContext());
      Vector prefs = new Vector(3);
      Preference pref;
      ScoringFunction sf;
      AspectValue av;
      av = AspectValue.newAspectValue(AspectType.START_TIME, demoNow);
      sf = new ScoringFunction.StepScoringFunction(av, 0.00, 0.99);
      pref = ldmf.newPreference(AspectType.START_TIME, sf, 1.0);
      prefs.addElement(pref);
      av = AspectValue.newAspectValue(AspectType.END_TIME, demoNow);
      sf = new ScoringFunction.StepScoringFunction(av, 0.00, 0.99);
      pref = ldmf.newPreference(AspectType.END_TIME, sf, 1.0);
      prefs.addElement(pref);
      double rateValue = 0.0;
      Rate rate = ((AspectRate) PluginHelper
                   .getPreferenceBest(projectionTask, DEMANDRATE))
        .getRateValue();
      if (rate instanceof CountRate) {
        rateValue = ((CountRate) rate).getValue(CountRate.EACHES_PER_DAY);
      } else if (rate instanceof FlowRate) {
        rateValue = ((FlowRate) rate).getValue(FlowRate.GALLONS_PER_DAY);
      } else {
        continue;
      }
      if (rateValue <= 0.0) continue;
      double theQuantity = rateValue * ONE_DAY;
      av = AspectValue.newAspectValue(AspectType.QUANTITY, theQuantity);
      sf = new ScoringFunction.StrictValueScoringFunction(av);
      pref = ldmf.newPreference(AspectType.QUANTITY, sf, 1.0);
      prefs.addElement(pref);
      supplyTask.setPreferences(prefs.elements());
      publishAdd(supplyTask);
    }
  }
}
