/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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

package org.cougaar.planning.plugin.completion;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.DemoControlService;
import org.cougaar.core.servlet.BaseServletComponent;

/**
 * This plugin gathers and integrates completion information from
 * nodes in a society to determine the "completion" of the current
 * tasks. This plugin should be included only in one agent at the root
 * of the society such as NCA. When the root determines that
 * completion has been acheived (or is never going to be achieved), it
 * advances the clock with the expectation that the advancement will
 * engender additional activity and waits for the completion of that
 * work.
 **/

public class SimpleTimeAdvanceComponent extends BaseServletComponent {
  private static final Class[] stringArgType = {String.class};

  private int DAYS = 1;

  private int HOURS = 0;

  private AlarmService alarmService;

  private DemoControlService demoControlService;

  public void setAlarmService(AlarmService as) {
    alarmService = as;
  }

  public void setDemoControlService(DemoControlService dcs) {
    demoControlService = dcs;
  }

  public SimpleTimeAdvanceComponent() {
    super();
  }

  protected String getPath() {
    return "/" + getRelativePath();
  }

  protected String getRelativePath() {
    return "timeAdvance";
  }

  protected Servlet createServlet() {
    try {
      return new SimpleTimeAdvanceServlet();
    } catch (RuntimeException e) {
      e.printStackTrace();
      throw e;
    }
  }

  private class SimpleTimeAdvanceServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      doPostOrGet(request, response);
    }
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
      doPostOrGet(request, response);
    }
    protected void doPostOrGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException
    {
      boolean doUpdate = ("Advance".equals(request.getParameter("submit")));
      PrintWriter out = response.getWriter();
      response.setContentType("text/html");
      out.println("<html>\n  <head>\n    <title>Time Advance Control</title>\n  </head>");
      out.println("  <body>\n    <h1>Time Advance Control</h1>"
                  + "    <form action=\"" + getRelativePath() + "\" method=\"get\">");
      out.println("      <table>");
      out.println("        <tr><td>Scenario Time</td><td>"
                  + formatDate(alarmService.currentTimeMillis())
                  + "</td></tr>");
      DAYS =
        handleField("days",
                    "Advance Days",
                    "The days for each advancement",
                    new Integer(DAYS),
                    request, out).intValue();
      HOURS =
        handleField("hours",
                    "Advance Hours",
                    "The hours for each advancement",
                    new Integer(HOURS),
                    request, out).intValue();
      out.println("      </table>");
      out.println("<input type=\"submit\" name=\"submit\" value=\"Advance\">");
      out.println("<input type=\"submit\" name=\"submit\" value=\"Refresh\">");
      if (doUpdate) {
        long TIME_STEP = DAYS * 86400000L + HOURS * 3600000L;
        long newTime = alarmService.currentTimeMillis() + TIME_STEP;
        long quantization = 1L;
        if (TIME_STEP >= 86400000L) {
          // Quantize to nearest day if step is a multiple of one day
          if ((TIME_STEP % 86400000L) == 0L) {
            quantization = 86400000L;
          }
        } else if ((TIME_STEP % 3600000L) == 0L) {
          // Quantize to nearest hour if step is a multiple of one hour
          quantization = 3600000;
        }
        newTime = (newTime / quantization) * quantization;
        demoControlService.setSocietyTime(newTime, true);
        out.println("Advancing to " + formatDate(newTime));
      }
      out.println("    </form>\n  </body>\n</html>");
    }

    private Number handleField(String name, String label, String description, Number currentValue,
                               HttpServletRequest request, PrintWriter out)
    {
      Number newValue = createValue(currentValue, request.getParameter(name));
      if (newValue != null && !newValue.equals(currentValue)) {
        currentValue = newValue;
      }
      out.println("        <tr><td>" + label + "</td><td>"
                  + "<input name=\"" + name + "\" type=\"text\" value=\""
                  + currentValue.toString() + "\"></td></tr>");
      return currentValue;
    }

    private Number createValue(Number currentValue, String v) {
      try {
        Constructor constructor = currentValue.getClass().getConstructor(stringArgType);
        Object[] args = {v};
        return (Number) constructor.newInstance(args);
      } catch (Exception e) {
        return currentValue;
      }
    }
  }

  private static final SimpleDateFormat dateFormat =
    new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
  private static Date fdate = new Date();
  static {
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
  }
  public static String formatDate(long time) {
    synchronized (fdate) {
      fdate.setTime(time);
      return dateFormat.format(fdate);
    }
  }
}
