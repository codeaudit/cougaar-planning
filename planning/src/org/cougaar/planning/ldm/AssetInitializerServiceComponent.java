/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
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
  private static final String INITIALIZER_PROP = 
    "org.cougaar.core.node.InitializationComponent";

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
      String prop = System.getProperty(INITIALIZER_PROP);
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
