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

package org.cougaar.planning.ldm;

import org.cougaar.core.domain.Factory;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.UIDServer;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.service.PrototypeRegistryService;

/**
 * Plugins primary interface to the LDM.
 *
 * @see org.cougaar.planning.ldm.LDMPluginServesLDM
 **/
public interface LDMServesPlugin extends PrototypeRegistryService {

  /**
   * Equivalent to <code>((PlanningFactory) getFactory("planning"))</code>.
   */
  PlanningFactory getFactory();

  /** @return the Requested Domain's LDM Factory.
   **/
  Factory getFactory(String domainName);

  /** @return the Requested Domain's LDM Factory.
   **/
  Factory getFactory(Class domainClass);

  /** @return the classloader to be used for loading classes for the LDM.
   * Domain Plugins should not use this, as they will have been themselves
   * loaded by this ClassLoader.  Some infrastructure components which are
   * not loaded in the same way will require this for correct operation.
   **/
  ClassLoader getLDMClassLoader();

  /** The current cluster's CID */
  MessageAddress getMessageAddress();
  
  UIDServer getUIDServer();

  /**
   * If the Delegator is used, this gets the real thing
   **/
  LDMServesPlugin getLDM();

  class Delegator implements LDMServesPlugin {
    private LDMServesPlugin ldm;
    Delegator() {
    }

    void setLDM(LDMServesPlugin ldm) {
      this.ldm = ldm;
    }

    public LDMServesPlugin getLDM() {
      return ldm != null ? ldm : this;
    }
    public void addPrototypeProvider(PrototypeProvider prov) {
      ldm.addPrototypeProvider(prov);
    }
    public void addPropertyProvider(PropertyProvider prov) {
      ldm.addPropertyProvider(prov);
    }
    public void addLatePropertyProvider(LatePropertyProvider lpp) {
      ldm.addLatePropertyProvider(lpp);
    }
    public void cachePrototype(String aTypeName, Asset aPrototype) {
      ldm.cachePrototype(aTypeName, aPrototype);
    }
    public boolean isPrototypeCached(String aTypeName) {
      return ldm.isPrototypeCached(aTypeName);
    }
    public Asset getPrototype(String aTypeName, Class anAssetClass) {
      return ldm.getPrototype(aTypeName, anAssetClass);
    }
    public Asset getPrototype(String aTypeName) {
      return ldm.getPrototype(aTypeName);
    }
    public void fillProperties(Asset anAsset) {
      ldm.fillProperties(anAsset);
    }
    public PropertyGroup lateFillPropertyGroup(Asset anAsset, Class pg, long time) {
      return ldm.lateFillPropertyGroup(anAsset, pg, time);
    }
    public int getPrototypeProviderCount() {
      return ldm.getPrototypeProviderCount();
    }
    public int getPropertyProviderCount() {
      return ldm.getPropertyProviderCount();
    }
    public int getCachedPrototypeCount() {
      return ldm.getCachedPrototypeCount();
    }

    public PlanningFactory getFactory() {
      return ldm.getFactory();
    }
    public Factory getFactory(String domainName) {
      return ldm.getFactory(domainName);
    }
    public Factory getFactory(Class domainClass) {
      return ldm.getFactory(domainClass);
    }
    public ClassLoader getLDMClassLoader() {
      return ldm.getLDMClassLoader();
    }
    public MessageAddress getMessageAddress() {
      return ldm.getMessageAddress();
    }
    public UIDServer getUIDServer() {
      return ldm.getUIDServer();
    }
  }
}
