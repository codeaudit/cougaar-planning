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
 
package org.cougaar.planning.examples;

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;
import javax.swing.*;

import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.plugin.legacy.SimplePlugin;

public class AlertCreatorPlugin extends SimplePlugin 
{

  // This plugin doesn't subscribe, but needs to set up a UI
  // for the creation of alerts
  public void setupSubscriptions()
  {
    //    System.out.println("AlertCreatorPlugin.setupSubscriptions...");

    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	generateAlert();
      }
    };
    createGUI("Create Alert", "Alert", listener);
  }

  private static int counter = 1;

  private static String []paramAction = 
  {"infomessage.html", "cat", "dog", "parrot"};

  private static String []paramDescription =
  {"View Info Messages", "A Cat", "A Dog", "A Parrot"}; 
  
  private void generateAlert()
  {
    openTransaction();
    NewAlert alert = theLDMF.newAlert();
    alert.setAlertText("AlertText-" + counter);

    int numParams = (counter % 4);

    AlertParameter []params = new AlertParameter[numParams];
    for (int i = 0; i < numParams; i++) {
      NewAlertParameter param = theLDMF.newAlertParameter();
      param.setParameter(paramAction[i]);  
      param.setDescription(paramDescription[i]);
      params[i] = param;
    }
    alert.setAlertParameters(params);

    alert.setAcknowledged(false);

    alert.setSeverity(counter);

    alert.setOperatorResponseRequired((numParams > 0));

    System.out.println("Publishing new alert " + alert.getAlertText());

    counter++;
    
    publishAdd(alert);
    closeTransactionDontReset();
  }

  public void execute()
  {
    //    System.out.println("AlertCreatorPlugin.execute...");
  }

  /**
   * Create a simple free-floating GUI button with a label
   */
  private static void createGUI(String button_label, String frame_label, 
				ActionListener listener) 
  {
    JFrame frame = new JFrame(frame_label);
    frame.getContentPane().setLayout(new FlowLayout());
    JPanel panel = new JPanel();
    // Create the button
    JButton button = new JButton(button_label);

    // Register a listener for the button
    button.addActionListener(listener);
    panel.add(button);
    frame.getContentPane().add("Center", panel);
    frame.pack();
    frame.setVisible(true);
  }



}
