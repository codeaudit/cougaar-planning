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

package org.cougaar.planning.plugin.legacy;


/** 
 * This is the basic class required for
 * implementing a "stateless" plugin.  Extending
 * classes should avoid any data members - any
 * plugin-instance specific state should be
 * stored in an implementation of Plugin.State.
 *
 * Only a single instance of each class of Plugin
 * will be constructed <em>per VM</em>.
 **/
public abstract class Plugin
{
  /** Infrastructure calls to allow the plugin
   * to contact the cluster, setup subscriptions,
   * recover from rehydration, etc.
   *
   * A critical step is for the plugin to call
   * support.setPluginState(PluginState) so
   * that it can retrieve any desired state (e.g.
   * subscriptions, etc) in later invocations.
   * 
   * If the plugin does not supply this method,
   * the default will be that the plugin not
   * get a Plugin.State.
   *
   * initialize will always be called inside 
   * a transaction.
   **/
  protected void initialize(PluginContext support) {
  }

  /**
   * execute is called by the infrastructure
   * any time the plugin should run.  It will
   * never be called in a context when initialize
   * has not run.
   *
   * execute will always be called inside 
   * a transaction.
   */
  protected void execute(PluginContext support) {
  }

  /** a Plugin may add a single State instance
   * to a PluginContext via the PluginContext.setPluginState
   * method during a call to initialize.
   * Thereafter, the plugin may retrieve the state
   * for use in subsequent execute cycles.  
   *
   * It is completely up to each plugin what appears in
   * its State instance.
   *
   * State implementations need not be serializable for
   * persistence or any other reason.
   **/
  public interface State {
  }
}
    
  
  
