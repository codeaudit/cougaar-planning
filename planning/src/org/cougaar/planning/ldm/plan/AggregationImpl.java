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

package org.cougaar.planning.ldm.plan;

import org.cougaar.planning.ldm.plan.Aggregation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.Composition;
import org.cougaar.planning.ldm.plan.Plan;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.MPTask;
import org.cougaar.core.blackboard.Subscriber;

import java.util.*;
import java.beans.*;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;


/** AggregationImpl.java
 * Implementation for aggregation
 */
 
public class AggregationImpl extends PlanElementImpl 
  implements Aggregation
{
 
  private transient Composition comp;  // changed to transient : Persistence
  
  public AggregationImpl() {}
 	
  /* Constructor that takes the composition and
   * assumes that there was no estimate.
   * @param p Plan
   * @param t Task
   * @param composition
   * @return Aggregation
   */
  public AggregationImpl(Plan p, Task t, Composition composition)  {
    super(p, t);
    this.comp = composition;
  }
  
  /* Constructor that takes the composition and an estimated result
   * @param t
   * @param p
   * @param composition
   * @param estimatedresult
   * @return Aggregation
   */
  public AggregationImpl(Plan p, Task t, Composition composition, AllocationResult estimatedresult)  {
    super(p, t);
    this.comp = composition;
    this.estAR = estimatedresult;
  }  

  /** Returns the Composition created by the aggregations of the task.
    * @see org.cougaar.planning.ldm.plan.Composition
    * @return Composition
    **/
   public Composition getComposition() {
     return comp;
   }  


  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    stream.writeObject(comp);
 }

  private void readObject(ObjectInputStream stream)
                throws ClassNotFoundException, IOException
  {
    /** ----------
      *    READ handlers common to Persistence and
      *    Network serialization.  NOte that these
      *    cannot be references to Persistable objects.
      *    defaultReadObject() is likely to belong here...
      * ---------- **/
    stream.defaultReadObject();

    comp = (Composition)stream.readObject();
  }

  public String toString() {
    return "[Aggregation of " + getTask().getUID() + " to " + comp + "]";
  }
  
  // ActiveSubscription code
  // override PlanElementImpls remove stuff
  public void removingFromBlackboard(Subscriber s) {
    Task t = getTask();
    ((TaskImpl)t).privately_resetPlanElement();
    Composition c = getComposition();

    if (c == null) return; // if already disconnected...

    if (c.isPropagating() ) { // if we're auto-propagating
      CompositionImpl ci = (CompositionImpl) c;

      // check to make sure we haven't already done the mass cleanup
      if (ci.doingCleanUp()) { // atomic check and clear
        // since we will get "notified" for every aggregation we rescinded
        // during the mass cleanup
        // rescind every aggregation planelement associated with the composition
        
        List aggpes = ci.clearAggregations();    // atomic get and clear the list
        ListIterator it = aggpes.listIterator();
        while (it.hasNext()) {
          Aggregation anagg = (Aggregation) it.next();
          if (anagg != this) {     // dont recurse on ourselves...
            Task atask = anagg.getTask();
            s.publishRemove(anagg);
            s.publishChange(atask);
          }
        }

        // rescind the combined task
        s.publishRemove(c.getCombinedTask());
      }  
    } else {      // we're not auto-propagating
      // clean up the references to this pe and its task in 
      // the composition and the combined MPTask
      ((CompositionImpl)c).removeAggregation(this);
      MPTask combtask = (MPTask) c.getCombinedTask();
      ((MPTaskImpl)combtask).removeParentTask(t);
      // if this happens to be the last parent of this combined task
      // rescind the combined task since its no longer valid.
      Enumeration parentsleft = combtask.getParentTasks();
      if (! parentsleft.hasMoreElements()) {
        s.publishRemove(combtask);
      }
    }
  }

  // beaninfo
  protected void addPropertyDescriptors(Collection c) throws IntrospectionException {
    super.addPropertyDescriptors(c);
    c.add(new PropertyDescriptor("composition", AggregationImpl.class, "getComposition", null));
  }
}
