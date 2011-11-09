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

import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.swing.filechooser.FileFilter;

import y.view.DefaultGraph2DRenderer;
import y.view.EditMode;
import y.view.Graph2D;
import y.view.Graph2DView;
import y.view.Graph2DViewMouseWheelZoomListener;
import de.zbit.graph.RestrictedEditMode;
import de.zbit.io.SBFileFilter;
import de.zbit.kegg.ext.TranslatorPanelOptions;
import de.zbit.kegg.io.KEGG2yGraph;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.util.TranslatorTools;
import de.zbit.util.prefs.SBPreferences;

/**
 * Extension of {@link TranslatorPanel} to visualize yFiles
 * graphs.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class TranslatorGraphPanel extends TranslatorPanel<Graph2D> {
  private static final long serialVersionUID = -1083637150034491451L;


  /**
   * @return all available output file formats for GraphML files.
   */
  public static List<SBFileFilter> getGraphMLfilefilter() {
    LinkedList<SBFileFilter> ff = new LinkedList<SBFileFilter>();
    ff.add(SBFileFilter.createGraphMLFileFilter());
    ff.add(SBFileFilter.createGMLFileFilter());
    ff.add(SBFileFilter.createJPEGFileFilter());        
    ff.add(SBFileFilter.createGIFFileFilter());
    ff.add(SBFileFilter.createYGFFileFilter());
    ff.add(SBFileFilter.createTGFFileFilter());
    if (KEGG2yGraph.isSVGextensionInstalled()) {
      ff.add(SBFileFilter.createSVGFileFilter());
    }
    return ff;
  }
  
  
  /**
   * Create a new translator-panel and initiates the translation.
   * @param inputFile
   * @param outputFormat
   * @param translationResult
   */
  public TranslatorGraphPanel(File inputFile, ActionListener translationResult) {
    this(inputFile, Format.GraphML, translationResult);
  }

  /**
   * Create a new translator-panel and initiates the translation.
   * @param inputFile
   * @param outputFormat any GraphML compatible formats (unchecked).
   * @param translationResult
   */
  public TranslatorGraphPanel(File inputFile, Format outputFormat,
    ActionListener translationResult) {
    super(inputFile, outputFormat, translationResult);
  }

  /**
   * Initiates a download and translation of the given pathway.
   * @param pathwayID pathway identifier (e.g., "mmu00010")
   * @param outputFormat any GraphML compatible formats (unchecked).
   * @param translationResult
   */
  public TranslatorGraphPanel(String pathwayID, Format outputFormat,
    ActionListener translationResult) {
    super(pathwayID, outputFormat, translationResult);
  }
  
  /**
   * Initiates a download and translation of the given pathway.
   * @param pathwayID pathway identifier (e.g., "mmu00010")
   * @param translationResult
   */
  public TranslatorGraphPanel(String pathwayID, ActionListener translationResult) {
    this(pathwayID, Format.GraphML, translationResult);
  }


  /* (non-Javadoc)
   * @see javax.swing.JComponent#setEnabled(boolean)
   */
  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    
    // Also enable the yFiles views
    try {
      TranslatorTools.enableViews(document,enabled);
    } catch (Throwable e) {}
  }
  
  /* (non-Javadoc)
   * @see java.awt.Component#repaint()
   */
  @Override
  public void repaint() {
    super.repaint();
    if (!isReady()) return;
    
    // Update graph
    // updateViews() does not update, but clear the visualization
    // strange thing...
    //((Graph2D)document).updateViews();
  }
  

  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorPanel#createTabContent()
   */
  @Override
  public void createTabContent() throws Exception {
 // Create a new visualization of the model.
    Graph2DView pane = new Graph2DView((Graph2D) document);
    add(pane);
    
    // Important to draw nodes last, edges should be BELOW nodes.
    if (pane.getGraph2DRenderer() instanceof DefaultGraph2DRenderer ){
      ((DefaultGraph2DRenderer) pane.getGraph2DRenderer()).setDrawEdgesFirst(true);
    }
    
    // Make group nodes collapsible.
    // Unfortunately work-in-progress.
    //pane.addViewMode(new CollapseGroupNodesViewMode((Graph2D) document));
    
    /*
     * Get settings to control visualization behavior
     */
    SBPreferences prefs = SBPreferences.getPreferencesFor(TranslatorPanelOptions.class);
    
    // Set KEGGtranslator logo as background
    if (TranslatorPanelOptions.SHOW_LOGO_IN_GRAPH_BACKGROUND.getValue(prefs)) {
      RestrictedEditMode.addBackgroundImage(getClass().getResource(logoResourcePath), pane);
    }
    //--
    // Show Navigation and Overview
    if (TranslatorPanelOptions.SHOW_NAVIGATION_AND_OVERVIEW_PANELS.getValue(prefs)) {
      RestrictedEditMode.addOverviewAndNavigation(pane);
    }
    //--
    
    pane.setSize(getSize());
    //ViewMode mode = new NavigationMode();
    //pane.addViewMode(mode);
    EditMode editMode = new RestrictedEditMode(translationListener, this);
    editMode.showNodeTips(true);
    pane.addViewMode(editMode);
    
    if (TranslatorPanelOptions.SHOW_PROPERTIES_TABLE.getValue(prefs)) {
      ((RestrictedEditMode)editMode).addPropertiesTable(pane);
    }
    
    pane.getCanvasComponent().addMouseWheelListener(new Graph2DViewMouseWheelZoomListener());
    pane.fitContent(true);
    pane.setFitContentOnResize(true);
  }

  
  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorPanel#getOutputFileFilter()
   */
  @Override
  protected LinkedList<FileFilter> getOutputFileFilter() {
    LinkedList<FileFilter> ff = new LinkedList<FileFilter>();
    ff.addAll(getGraphMLfilefilter());
    return ff;
  }


  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorPanel#writeToFileUnchecked(java.io.File, java.lang.String)
   */
  @Override
  protected boolean writeToFileUnchecked(File file, String format) throws Exception {
    return ((KEGG2yGraph)translator).writeToFile(document, file.getPath(), format);
  }
  
}
