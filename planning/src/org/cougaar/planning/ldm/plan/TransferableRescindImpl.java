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

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.plan.NewTransferableRescind;
import org.cougaar.planning.ldm.plan.Plan;
import org.cougaar.planning.ldm.plan.Transferable;
import org.cougaar.planning.ldm.plan.TransferableRescind;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/** TransferableRescind implementation
 * TransferableRescind allows a transferable to be rescinded from the Plan. 
 * @author  ALPINE <alpine-software@bbn.com>
 *
 **/

public class TransferableRescindImpl 
  extends PlanningDirectiveImpl
  implements TransferableRescind, NewTransferableRescind
{

  private UID transferableUID = null;
        
  /**
   * Returns the transferable to be rescinded
   * @return Transferable
   */

  public UID getTransferableUID() {
    return transferableUID;
  }
     
  public void setTransferableUID(UID tuid) {
    transferableUID = tuid;
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }

  private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
    stream.defaultReadObject();
  }

  public String toString() {
    return "<TransferableRescind for " + transferableUID + ">";
  }

}
