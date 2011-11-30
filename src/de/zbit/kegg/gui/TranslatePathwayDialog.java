/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of KEGGtranslator, a program to convert KGML files
 * from the KEGG database into various other formats, e.g., SBML, GML,
 * GraphML, and many more. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator> to
 * obtain the latest version of KEGGtranslator.
 *
 * Copyright (C) 2011 by the University of Tuebingen, Germany.
 *
 * KEGGtranslator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.kegg.gui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import de.zbit.gui.GUITools;
import de.zbit.gui.JLabeledComponent;
import de.zbit.gui.LayoutHelper;
import de.zbit.gui.prefs.PreferencesPanel;
import de.zbit.kegg.Translator;
import de.zbit.kegg.io.KEGGtranslatorIOOptions;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.util.Reflect;
import de.zbit.util.prefs.SBProperties;

/**
 * A Panel, which can be shown as dialog, that let's
 * the user select an organism, a pathway and output
 * format to create a new {@link TranslatorPanel}.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class TranslatePathwayDialog extends JPanel {
  private static final long serialVersionUID = 6884078022042984336L;
  
  private static final Logger log = Logger.getLogger(GUITools.class.getName());
  
  /**
   * Pre-selected output file format (optional)
   */
  Format outputFormat=null;
  
  /**
   * If {@link #outputFormat} is <code>NULL</code>, this
   * is the output format selector. Else, this is
   * <code>NULL</code>.
   */
  JComponent oFormatSelector=null;
  
  /**
   * The pathway selector (includes an organism selector).
   */
  PathwaySelector selector=null;
  
  /**
   * Allows to initialize a {@link TranslatorPanel} of a certain type. 
   */
  Class<? extends TranslatorPanel<?>> transPanelType = null;
  
  private LayoutHelper lh;
  
  public TranslatePathwayDialog() {
    this(null);
  }
  
  public TranslatePathwayDialog(Format preSelectedOutputFormat) {
    super();
    this.outputFormat = preSelectedOutputFormat;
    LayoutHelper lh = new LayoutHelper(this);
    showDownloadPanel(lh);
  }
  
  /**
   * Set the {@link TranslatorPanel} class that should get initialized.
   * This class MUST HAVE A CONSTRUCTOR FOR
   * ({@link String}, {@link Format}, {@link ActionListener}).
   * @param transPanelType
   */
  public void setTranslatorPanelClassToInitialize(
    Class<? extends TranslatorPanel<?>> transPanelType) {
    this.transPanelType = transPanelType;
  }

  
  /**
   * Adds selectors to select organism, pathway and output format to this panel.
   * @param lh
   */
  public void showDownloadPanel(LayoutHelper lh) {
    try {
      selector = PathwaySelector.createPathwaySelectorPanel(Translator.getFunctionManager(), lh);
      JComponent oFormat=null;
      if ((outputFormat == null) || (outputFormat == null)) {
        oFormat = PreferencesPanel.getJComponentForOption(KEGGtranslatorIOOptions.FORMAT, (SBProperties)null, null);
        oFormat =((JLabeledComponent) oFormat).getColumnChooser(); // Trim
        lh.add("Please select the output format", oFormat, false);
      }
      oFormatSelector = oFormat;

    } catch (Throwable exc) {
      GUITools.showErrorMessage(lh.getContainer(), exc);
    }
  }
  
  /**
   * Evaluates the selections on this dialog. This will create a
   * {@link TranslatorPanel} or show an error message.
   * @param translationResult optional translation listener for the
   * initiated translation.
   * @return
   */
  public TranslatorPanel<?> evaluateDialog(ActionListener translationResult) {
    // Check user selection
    String id = selector.getSelectedPathwayID();
    if (id==null || id.length()<1) {
      GUITools.showErrorMessage(null, "No valid pathway selected.");
      return null;
    }
    
    // Inform listeners
    firePropertyChange("PATHWAY_NAME", null, selector.getSelectedPathway());
    firePropertyChange("ORGANISM_NAME", null, selector.getOrganismSelector().getSelectedOrganism());
    
    // Get the output format
    Format outFormat = outputFormat;
    if (oFormatSelector!=null) {
      outFormat = Format.valueOf(Reflect.invokeIfContains(oFormatSelector, "getSelectedItem").toString());
    }
    
    // Create the panel and initiate the translation
    TranslatorPanel<?> tp=null;
    if (transPanelType!=null) {
      try {
        tp = transPanelType.getConstructor(String.class, Format.class, ActionListener.class)
        .newInstance(id, outFormat, translationResult);
      } catch (Exception e ){
        log.log(Level.WARNING, "Could not instantiate given translator panel.", e);
      }
    }
    if (tp==null) {
      tp = TranslatorPanel.createPanel(id, outFormat, translationResult);
    }
     
    
    return tp;
  }
  
  /**
   * Adds an ok button to this panel, that does on-click evaluation of the
   * dialog and adds the corresponding {@link TranslatorPanel} to the
   * given tabbed pane.
   * @param translationResult
   */
  public void addOkButton(final JTabbedPane addTabsHere, final ActionListener translationResult) {
    JButton okButton = new JButton(GUITools.getOkButtonText());
    //okButton.addActionListener()
    JPanel p2 = new JPanel();
    p2.setLayout(new FlowLayout(FlowLayout.LEFT));
    p2.add(okButton);
    lh.add(p2);
    okButton.setEnabled(GUITools.isEnabled(lh.getContainer()));
    
    // Action
    okButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        createOkButtonAction(addTabsHere, translationResult).run();
      }
    });
    
    // Show the selector.
    GUITools.enableOkButtonIfAllComponentsReady(lh.getContainer(), okButton);
    if (lh.getContainer() instanceof JComponent) {
      GUITools.setOpaqueForAllElements((JComponent)lh.getContainer(), false);
    }
  }
  
  /**
   * Creates a runnable that creates and adds tabs to a {@link JTabbedPane}.
   * @param addTabsHere this is where the created {@link TranslatorPanel} will be added
   * @param translationResult an optional action listener
   * @return
   */
  private Runnable createOkButtonAction(final JTabbedPane addTabsHere, final ActionListener translationResult) {
    return new Runnable() {
      public void run() {
        TranslatorPanel<?> tp = evaluateDialog(translationResult);
        if (tp!=null) {
          addTabsHere.addTab(selector.getSelectedPathway(), tp);
          addTabsHere.setSelectedIndex(addTabsHere.getTabCount() - 1);
        } 
      }
    };
  }
  
  /**
   * Shows and evaluates this panel in a dialog.
   * @param addTabsHere this is where the created {@link TranslatorPanel} will be added
   * @param translationResult an optional action listener
   * @param optionalPreSelectedFormat an optional pre-selection of desired output format
   */
  public static void showAndEvaluateDialog(final JTabbedPane addTabsHere, final ActionListener translationResult, Format optionalPreSelectedFormat) {
    TranslatePathwayDialog d = new TranslatePathwayDialog(optionalPreSelectedFormat);
    
    showAndEvaluateDialog(addTabsHere, translationResult, d);
  }
  
  /**
   * Shows and evaluates this panel in a dialog.
   * @param addTabsHere this is where the created {@link TranslatorPanel} will be added
   * @param translationResult an optional action listener
   * @param optionalPreSelectedFormat an optional pre-selection of desired output format
   */
  public static void showAndEvaluateDialog(final JTabbedPane addTabsHere, final ActionListener translationResult, TranslatePathwayDialog d) {
    
    GUITools.showOkCancelDialogInNewThred(d, "Download pathway", d.createOkButtonAction(addTabsHere, translationResult), null);
    d.selector.autoActivateOkButton(d);
  }
  
}
