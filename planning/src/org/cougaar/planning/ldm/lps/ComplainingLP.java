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

package org.cougaar.planning.ldm.lps;

import java.util.Collection;

import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.blackboard.PublishHistory;
import org.cougaar.core.domain.EnvelopeLogicProvider;
import org.cougaar.core.domain.LogicProvider;
import org.cougaar.core.domain.RootPlan;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/** Watch the LogPlan and complain when obvious errors and 
 * other suspicious patterns are detected.  
 * <p>
 * This particular
 * implementation is only concerned with vetting Envelopes.
 * It does not attempt to detect any in-state Blackboard
 * or Message problems.
 * <p>
 * The specific tests that are performed are described by the
 * execute method.
 * <p>
 * Properties:
 * org.cougaar.planning.ldm.lps.ComplainingLP.level may
 * be set to an integer. Valid values are 0 (silent), 1 (report 
 * only definite errors), and 2 (report anything suspicious).
 * The default value is 2.
 **/
public class ComplainingLP
implements LogicProvider, EnvelopeLogicProvider
{
  private static final Logger logger = Logging.getLogger(ComplainingLP.class);

  private final static String levelPROP = "org.cougaar.planning.ldm.lps.ComplainingLP.level";

  private static int level = 1;

  private final static int levelQUIET = 0;
  private final static int levelERROR = 1;
  private final static int levelWARN = 2;
  
  static {
    level = (Integer.valueOf(System.getProperty(levelPROP, String.valueOf(level)))).intValue();
    if (level < levelQUIET) level = levelQUIET;
    if (level > levelWARN) level = levelWARN;
  }

  private final RootPlan rootplan;
  private final MessageAddress self;

  public ComplainingLP(
      RootPlan rootplan,
      MessageAddress self) {
    this.rootplan = rootplan;
    this.self = self;
  }

  public void init() {
  }

  /**
   * Complain in any of the following cases:
   *  Unique Object Changed but not in blackboard (error).
   *  Unique Object add/removed/changed which has the same UID as an existing object
   * but that is not identical (warning).
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    if (level <= 0) return;     // quiet.  better would be to refuse to plug-in.

    Object obj = o.getObject();
    if (obj instanceof UniqueObject) {
      UID objuid = ((UniqueObject)obj).getUID();
      Object found = rootplan.findUniqueObject(objuid);
      boolean thereP = (found != null);
      if ((! thereP) && o.isChange() && level >= levelERROR)
        complain("change of non-existent object "+obj, obj);

      /*
        // cannot do these because LPs are applied after subscription updates.
      if ((! thereP) && o.isRemove() && level >= levelWARN)
        complain("redundant remove of object "+obj);

      if ((thereP) && o.isAdd() && level >= levelWARN)
        complain("redundant add of object "+obj);
      */
      if (thereP && found != obj && level >= levelWARN)
        complain("action="+o.getAction()+" on "+obj+" which is not == "+found, obj);

    }
  }
  private void complain(String complaint, Object obj) {
    logger.warn("Warning: "+self+" ComplainingLP observed "+complaint);
    PublishHistory history = rootplan.getHistory();
    if (history != null) history.dumpStacks(obj);
  }
}
