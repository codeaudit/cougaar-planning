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

package org.cougaar.planning.ldm.plan;

import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.NewNotification;
import org.cougaar.planning.ldm.plan.Notification;
import org.cougaar.planning.ldm.plan.Plan;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.core.util.UID;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/** An implementation of org.cougaar.TaskResponse
 */
public class TaskResponseImpl extends PlanningDirectiveImpl
  implements TaskResponse, NewTaskResponse
{
                
  private UID taskUID; 
  private UID childUID;
                
  //no-arg constructor
  public TaskResponseImpl () {
    super();
  }

  //constructor that takes the Task and Plan
  public TaskResponseImpl(Task t, Plan plan) {
    taskUID = t.getUID();
    setPlan(plan);
  }
                
  public TaskResponseImpl(UID tuid, Plan plan) {
    taskUID = tuid;
    setPlan(plan);
  }

  /** implementation of the TaskResponse interface */
                
  /** 
   * Returns the task UID the notification is in reference to.
   * @return Task 
   **/
                
  public UID getTaskUID() {
    return taskUID;
  }
  
  /** Get the child task's UID that was disposed.  It's parent task is getTask();
    * Useful for keeping track of which subtask of an Expansion caused
    * the re-aggregation of the Expansion's reported allocationresult.
    * @return UID
    */
  public UID getChildTaskUID() {
    return childUID;
  }
  
  // implementation methods for the NewTaskResponse interface

  /** 
   * Sets the task the notification is in reference to.
   * @param t 
   **/
                
  public void setTask(Task t) {
    taskUID = t.getUID();
  }

  public void setTaskUID(UID tuid) {
    taskUID = tuid;
  }
  
  /** Sets the child task's UID that was disposed.  It's parent task is getTask();
    * Useful for keeping track of which subtask of an Expansion caused
    * the re-aggregation of the Expansion's reported allocationresult.
    * @param thechildUID
    */
  public void setChildTaskUID(UID thechildUID) {
    childUID = thechildUID;
  }
                    
  /** Always serialize Notifications with TaskProxy
   */
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }

  private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
    stream.defaultReadObject();
  }
}
