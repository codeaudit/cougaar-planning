/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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
 
package org.cougaar.planning.servlet;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.servlet.SimpleServletSupport;

/**
 * <pre>
 * Servlet worker that gathers Organizational hierarchy information 
 * from a Cougaar society.
 *
 * Takes three parameters : 
 * - recurse, which controls whether to recurse down the hierarchy
 * - format, which specifies whether the response is a data stream, xml, or html.
 * - allRelationships, which returns all the relationships for an agent
 *   html is in the familiar CLUSTERS_R format.
 *
 * An example URL is :
 * 
 * http://localhost:8800/$TRANSCOM/hierarchy?recurse=true&format=xml
 *
 * This says to recurse from the TRANSCOM agent and return an XML document.                       
 * 
 * Example output from this query :
 *
 * <?xml version="1.0" ?> 
 * <Hierarchy RootID="TRANSCOM">
 *   <Org>
 *     <OrgID>TRANSCOM</OrgID>
 *     <Name>TRANSCOM</Name>
 *     <Rel OrgID="GlobalAir" Rel="0"/>
 *     <Rel OrgID="GlobalSea" Rel="0"/>
 *   </Org>
 *   <Org>
 *     <OrgID>GlobalAir</OrgID>
 *     <Name>GlobalAir</Name>
 *     <Rel OrgID="PlanePacker" Rel="0"/>
 *   </Org>
 *   ....
 * </Hierarchy>
 * </pre>
 */
public class HierarchyServlet
  extends ServletBase {
  public static boolean VERBOSE = false;
  static {
      VERBOSE = Boolean.getBoolean("org.cougaar.mlm.ui.psp.transit.HierarchyServlet.verbose");
  }

  /**
   * This is the path for my Servlet, relative to the
   * Agent's URLEncoded name.
   * <p>
   * For example, on Agent "X" the URI request path
   * will be "/$X/hello".
   */
  private final String myPath = "/Hierarchy";

  /**
   * Pretty to-String for debugging.
   */
  public String toString() {
    return getClass().getName()+"("+myPath+")";
  }

  protected ServletWorker createWorker () { return new HierarchyWorker (); }
 
  /** <pre>
   *
   * USAGE 
   *
   * Allows a number of choices:
   * 1) Recursive or not
   * 2) All relationships or just superior-subordinate
   * 3) Format : html, xml, or serialized java objects
   *
   * Only called if no arguments are given.
   * </pre>
   */
  public void getUsage(PrintWriter out, SimpleServletSupport support) {
    out.print("<HTML><HEAD><TITLE>Hierarchy Usage</TITLE></HEAD><BODY>\n"+
	      "<H2><CENTER>Hierarchy Usage</CENTER></H2><P>\n"+
	      "<FORM METHOD=\"GET\" ACTION=\"/$");
    out.print(support.getEncodedAgentName());
    out.print(support.getPath());
    // choose between shallow and recursive displays
    out.print("\">\n"+
	      "Show organization hierarchy for:<p>\n"+
	      "&nbsp;&nbsp;<INPUT TYPE=\"radio\" NAME=\"recurse\" "+
	      "VALUE=\"true\" CHECKED>"+
	      "&nbsp;All related clusters<p>\n"+
	      "&nbsp;&nbsp;<INPUT TYPE=\"radio\" NAME=\"recurse\" "+
	      "VALUE=\"false\">"+
	      "&nbsp;Just ");
    out.print(support.getAgentIdentifier());
    // checkbox for showing all relationships
    out.print("<P>\n"+
	      "&nbsp;&nbsp;<INPUT TYPE=\"checkbox\" NAME=\"allRelationships\" "+
	      "VALUE=\"true\">"+
	      "&nbsp;Show complete relations (not just superior-subordinate)<p>\n");
    // choose data format - html, xml, or java objects 
    out.print("<P>\nShow results as "+
	      "&nbsp;&nbsp;<INPUT TYPE=\"radio\" NAME=\"format\" "+
	      "VALUE=\"html\" CHECKED>"+
	      "&nbsp;html ");
    out.print("<INPUT TYPE=\"radio\" NAME=\"format\" "+
	      "VALUE=\"xml\">"+
	      "&nbsp;xml ");
    out.print("<INPUT TYPE=\"radio\" NAME=\"format\" "+
	      "VALUE=\"data\">"+
	      "&nbsp;serialized Java objects ");
    out.print("<P>\n"+
	      "<INPUT TYPE=\"submit\" NAME=\"Display\">\n"+
	      "</FORM></BODY></HTML>");
  }
}

