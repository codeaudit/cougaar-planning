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

package org.cougaar.planning.plugin.legacy;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.PluginBase;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.util.ConfigFinder;
import org.cougaar.util.GenericStateModelAdapter;

  /** 
   * First cut at a class that performs basic new-fangled Plugin functions
   **/

public abstract class PluginSupport 
  extends GenericStateModelAdapter
  implements PluginBase
{

  private PluginBindingSite pluginBindingSite = null;

  public void setBindingSite(final BindingSite bs) {
    pluginBindingSite = new PluginBindingSite() {
      public MessageAddress getAgentIdentifier() {
        return PluginSupport.this.getAgentIdentifier();
      }
      public ConfigFinder getConfigFinder() {
        return PluginSupport.this.getConfigFinder();
      }
      public ServiceBroker getServiceBroker() {
        return bs.getServiceBroker();
      }
      public void requestStop() {
        bs.requestStop();
      }
    };
  }

  private MessageAddress agentId = null;
  public final void setAgentIdentificationService(
      AgentIdentificationService ais) {
    this.agentId = ais.getMessageAddress();
  }
  public MessageAddress getAgentIdentifier() {
    return agentId;
  }
  public ConfigFinder getConfigFinder() {
    return ConfigFinder.getInstance();
  }

  protected final PluginBindingSite getBindingSite() {
    return pluginBindingSite;
  }

  /** storage for wasAwakened. 
   **/
  private boolean explicitlyAwakened = false;

  /** true IFF were we awakened explicitly (i.e. we were asked to run
   * even if no subscription activity has happened).
   * The value is valid only while running in the main plugin thread.
   */
  protected boolean wasAwakened() { return explicitlyAwakened; }

  /** For PluginBinder use only **/
  public final void setAwakened(boolean value) { explicitlyAwakened = value; }

}
