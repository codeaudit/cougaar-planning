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

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.DBInitializerService;
import org.cougaar.core.node.Node;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.service.AssetInitializerService;
import org.cougaar.util.GenericStateModelAdapter;

/** 
 * A component which creates and advertises the appropriate 
 * AssetInitializerService ServiceProvider.
 * <p>
 * The rule is that we use the CSMART DB if components were intialized from there.
 * Otherwise, if the components coming from XML,
 * we use the non-CSMART DB. Otherwise we try to initialize from INI-style files.
 * <p>
 * @see FileAssetInitializerServiceProvider
 * @see DBAssetInitializerServiceProvider
 * @see NonCSMARTDBInitializerServiceImpl
 **/
public final class AssetInitializerServiceComponent
extends GenericStateModelAdapter
implements Component 
{
  private ServiceBroker sb;

  private DBInitializerService dbInit;
  private ServiceProvider theSP;
  private LoggingService log;

  public void setBindingSite(BindingSite bs) {
    //this.sb = bs.getServiceBroker();
  }

  public void setNodeControlService(NodeControlService ncs) {
    if (ncs == null) {
      // Revocation
    } else {
      this.sb = ncs.getRootServiceBroker();
    }
  }

  /*
    // not available in nodeagent early on
  public void setDBInitializerService(DBInitializerService dbInit) {
    this.dbInit = dbInit;
  }
  */

  public void load() {
    super.load();

    log = (LoggingService)
      sb.getService(this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }

    dbInit = (DBInitializerService) sb.getService(this, DBInitializerService.class, null);

    // Do not provide this service if there is already one there.
    // This allows someone to provide their own component to provide
    // the asset initializer service in their configuration
    if (sb.hasService(AssetInitializerService.class)) {
      // already have AssetInitializer service!
      //
      // leave the existing service in place
      if (log.isInfoEnabled()) {
        log.info(
            "Not loading the default asset initializer service");
      }
      if (log != LoggingService.NULL) {
        sb.releaseService(this, LoggingService.class, log);
        log = null;
      }
      return;
    }

    theSP = chooseSP();
    if (theSP != null)
      sb.addService(AssetInitializerService.class, theSP);
    if (log != LoggingService.NULL) {
      sb.releaseService(this, LoggingService.class, log);
      log = null;
    }
  }

  public void unload() {
    if (theSP != null) {
      sb.revokeService(AssetInitializerService.class, theSP);
      theSP = null;
    }

    super.unload();
  }

  private ServiceProvider chooseSP() {
    try {
      ServiceProvider sp;
      String prop = System.getProperty(Node.INITIALIZER_PROP);
      // If user specified to load from the database
      if (prop != null && prop.indexOf("DB") != -1 && dbInit != null) {
	// Init from CSMART DB
        sp = new DBAssetInitializerServiceProvider(dbInit);
	if (log.isInfoEnabled())
	  log.info("Will init OrgAssets from CSMART DB");
	// Else if user specified to load from XML
      } else if (prop != null && prop.indexOf("XML") != -1) {
	// Initing config from XML. Assets will come from non-CSMART DB
	// Create a new DBInitializerService
	DBInitializerService myDbInit = new NonCSMARTDBInitializerServiceImpl();
	sp = new DBAssetInitializerServiceProvider(myDbInit);
	if (log.isInfoEnabled())
	  log.info("Will init OrgAssets from NON CSMART DB!");
      } else {
	// default to going from INI files
	sp = new FileAssetInitializerServiceProvider();
	if (log.isInfoEnabled())
	  log.info("Will init OrgAssets from INI Files");
      }
      return sp;
    } catch (Exception e) {
      log.error("Exception while creating AssetInitializerService", e);
      return null;
    }
  }
}
