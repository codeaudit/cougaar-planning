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

import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.plan.*;

/**
 * Special implementation of Workflow Interface that is only useful to
 * mark tasks being send between agents so that aliasing of the tasks
 * can be avoided.
 **/
  
public class SpecialWorkflow implements Workflow {
  public Task getParentTask() { throw new UnsupportedOperationException(); }
  public Enumeration getTasks() { throw new UnsupportedOperationException(); }
  public Enumeration getConstraints() { throw new UnsupportedOperationException(); }
  public Enumeration getTaskConstraints(Task task) { throw new UnsupportedOperationException(); }
  public Enumeration getPairConstraints(Task constrainedTask, Task constrainingTask) { throw new UnsupportedOperationException(); }
  public AllocationResult aggregateAllocationResults() { throw new UnsupportedOperationException(); }
  public SubtaskResults getSubtaskResults() { throw new UnsupportedOperationException(); }
  public boolean constraintViolation() { throw new UnsupportedOperationException(); }
  public Enumeration getViolatedConstraints() { throw new UnsupportedOperationException(); }
  public boolean isPropagatingToSubtasks() { throw new UnsupportedOperationException(); }
  public Constraint getNextPendingConstraint() { throw new UnsupportedOperationException(); }
  public Annotation getAnnotation() { throw new UnsupportedOperationException(); }
  public void setAnnotation(Annotation pluginAnnotation) { throw new UnsupportedOperationException(); }
  public UID getUID() { throw new UnsupportedOperationException(); }
  public void setUID(UID uid) { throw new UnsupportedOperationException(); }
}

