/*
 * <copyright>
 *  
 *  Copyright 2000-2004 BBNT Solutions, LLC
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

package org.cougaar.planning.examples;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.domain.Factory;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.servlet.ComponentServlet;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.Verb;

/**
 * An example servlet that adds a task to the blackboard.
 * <p>
 * Load with:<pre>
 *   &lt;component 
 *     class='org.cougaar.planning.examples.AddTaskServlet'&gt;
 *     &lt;argument&gt;/addtask&lt;/argument&gt;
 *   &lt;/component&gt;
 * </pre>
 * <p>
 * Note that the compiled examples jars are not included in the
 * release; you must compile them from the source code using the
 * ant script, e.g.:<pre>
 *   cd $CIP/planning
 *   ant compile-examples
 *   cd tmp/examples 
 *   jar cvf $CIP/lib/planning_examples.jar * 
 * </pre> 
 */
public class AddTaskServlet
extends ComponentServlet
implements BlackboardClient
{

  private BlackboardService blackboard;
  private DomainService ds;
  private PlanningFactory ldmf;

  //
  // Get the blackboard and planning factory.
  //
  // Instead of doing this in "load()", using the usual
  // "getServiceBroker().getService(..)", let's use the equivalent
  // component model's reflection support for
  // "setXService(XService x) {..}".
  //

  public void setBlackboardService(BlackboardService blackboard) {
    this.blackboard = blackboard;
  }

  public void setDomainService(DomainService ds) {
    this.ds = ds;
    if (ds != null) {
      this.ldmf = (PlanningFactory) ds.getFactory("planning");
    }
  }

  public void doGet(
      HttpServletRequest req,
      HttpServletResponse res) throws IOException {
    PrintWriter out = res.getWriter();
    out.println("<html><body bgcolor=\"white\">");

    // add a new task to the blackboard:
    NewTask nt = ldmf.newTask();
    nt.setSource(agentId);
    nt.setVerb(Verb.get("fromAddTaskServlet"));
    try {
      blackboard.openTransaction();
      blackboard.publishAdd(nt);
    } finally {
      blackboard.closeTransactionDontReset();
    }

    out.println(
        "AddTaskServlet "+
        "publish-Added task (<tt>"+
        nt.getUID()+
        "</tt>) with verb \"<tt>"+
        nt.getVerb()+
        "</tt>\" to agent <b>"+
        agentId+
        "</b>'s Blackboard.");
    out.println("</body></html>");
  }

  //
  // These are oddities of implementing BlackboardClient:
  //
  // Note: A Component must implement BlackboardClient in order 
  // to obtain BlackboardService.
  //

  // odd BlackboardClient method:
  public String getBlackboardClientName() {
    return toString();
  }

  // odd BlackboardClient method, bug 2515:
  public long currentTimeMillis() {
    throw new UnsupportedOperationException(
        this+" asked for the current time???");
  }
}
