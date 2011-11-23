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
import java.net.MalformedURLException;
import java.net.URL;
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
import de.zbit.kegg.io.AbstractKEGGtranslator;
import de.zbit.kegg.io.BatchKEGGtranslator;
import de.zbit.kegg.io.KEGG2yGraph;
import de.zbit.kegg.io.KEGGtranslator;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.util.ThreadManager;
import de.zbit.util.TranslatorTools;
import de.zbit.util.prefs.SBPreferences;

/**
 * This abstract class should be used for all output formats, that want to
 * get visualized as Graph. This builds a graph layer between the
 * actual output format and Graph, so your actual output format can be visualized
 * as Graph.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public abstract class TranslatorGraphLayerPanel <DocumentType> extends TranslatorPanel<DocumentType> {
  private static final long serialVersionUID = 3437289245211176473L;
  
  /**
   * The current graph layer
   */
  Graph2D graphLayer;


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
  public TranslatorGraphLayerPanel(File inputFile, ActionListener translationResult) {
    this(inputFile, Format.JPG, translationResult);
  }

  /**
   * Create a new translator-panel and initiates the translation.
   * @param inputFile
   * @param outputFormat
   * @param translationResult
   */
  public TranslatorGraphLayerPanel(File inputFile, Format outputFormat,
    ActionListener translationResult) {
    super(inputFile, outputFormat, translationResult);
  }

  /**
   * Initiates a download and translation of the given pathway.
   * @param pathwayID pathway identifier (e.g., "mmu00010")
   * @param outputFormat
   * @param translationResult
   */
  public TranslatorGraphLayerPanel(String pathwayID, Format outputFormat,
    ActionListener translationResult) {
    super(pathwayID, outputFormat, translationResult);
  }
  
  /**
   * Initiates a download and translation of the given pathway.
   * @param pathwayID
   * @param translationResult
   */
  public TranslatorGraphLayerPanel(String pathwayID, ActionListener translationResult) {
    this(pathwayID, Format.JPG, translationResult);
  }


  /* (non-Javadoc)
   * @see javax.swing.JComponent#setEnabled(boolean)
   */
  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    
    // Also enable the yFiles views
    try {
      if (graphLayer!=null) TranslatorTools.enableViews(graphLayer,enabled);
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
    graphLayer = createGraphFromDocument(document);
    
    // Create a new visualization of the model.
    Graph2DView pane = new Graph2DView(graphLayer);
    add(pane);
    
    // Important to draw nodes last, edges should be BELOW nodes.
    if (pane.getGraph2DRenderer() instanceof DefaultGraph2DRenderer ){
      ((DefaultGraph2DRenderer) pane.getGraph2DRenderer()).setDrawEdgesFirst(true);
    }
    
    // Make group nodes collapsible.
    // Unfortunately work-in-progress.
    //pane.addViewMode(new CollapseGroupNodesViewMode((Graph2D) graphLayer));
    
    /*
     * Get settings to control visualization behavior
     */
    SBPreferences prefs = SBPreferences.getPreferencesFor(TranslatorPanelOptions.class);
    
    // Set KEGGtranslator logo as background
    addBackgroundImage(pane, translator, prefs);
    
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


  /**
   * Convert the given document to a visualizable graph file.
   * @param document
   * @return
   */
  protected abstract Graph2D createGraphFromDocument(DocumentType document);


  /**
   * Setup the background image as set in the preferences
   * @param pane the pane to add the background image
   * @param translator the translator used for translation
   * @param prefs might be null, else, prefs object for {@link TranslatorPanelOptions}
   * @throws MalformedURLException
   */
  public static void addBackgroundImage(Graph2DView pane, AbstractKEGGtranslator<?> translator, SBPreferences prefs)
  throws MalformedURLException {
    addBackgroundImage(pane, translator, prefs, false);
  }
  public static void addBackgroundImage(Graph2DView pane, AbstractKEGGtranslator<?> translator, SBPreferences prefs, boolean waitUntilComplete)
    throws MalformedURLException {
    if (prefs ==null) {
      prefs = SBPreferences.getPreferencesFor(TranslatorPanelOptions.class);
    }
    if (TranslatorPanelOptions.SHOW_LOGO_IN_GRAPH_BACKGROUND.getValue(prefs)) {
      RestrictedEditMode.addBackgroundImage(TranslatorGraphPanel.class.getResource(logoResourcePath), pane);
    } else
    if (TranslatorPanelOptions.SHOW_KEGG_PICTURE_IN_GRAPH_BACKGROUND.getValue(prefs)) {
      Integer brighten = (TranslatorPanelOptions.BRIGHTEN_KEGG_BACKGROUND_IMAGE.getValue(prefs));
      if (brighten==null || brighten<0) brighten = 0;
      
      String image = translator.getLastTranslatedPathway().getImage();
      if (image!=null) {
        Thread t = RestrictedEditMode.addDynamicBackgroundImage(new URL(image), pane, brighten);
        if (waitUntilComplete) {
          ThreadManager.awaitTermination(t);
        }
      }
    }
  }

  
  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorPanel#getOutputFileFilter()
   */
  @Override
  protected LinkedList<FileFilter> getOutputFileFilter() {
    LinkedList<FileFilter> ff = getOutputFileFilterForRealDocument();
    if (ff==null) ff = new LinkedList<FileFilter>();
    if (isAllowedToSaveAsGraphFormats()) {
      ff.addAll(getGraphMLfilefilter());
    }
    return ff;
  }
  
  /**
   * @return true if the users should be able to save this as graph formats,
   * in addition to the {@link #getOutputFileFilterForRealDocument()}.
   */
  public boolean isAllowedToSaveAsGraphFormats() {
    return true;
  }
  
  /**
   * Create all file filters that are available to save this
   * tabs content. The first in the list is assumed to be
   * the default file filter.
   * @return
   */
  protected abstract LinkedList<FileFilter> getOutputFileFilterForRealDocument();

  
  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorPanel#writeToFileUnchecked(java.io.File, java.lang.String)
   */
  @Override
  protected boolean writeToFileUnchecked(File file, String format) throws Exception {
    // is it a Graph format?
    boolean isGraphFormat = false;
    if (isAllowedToSaveAsGraphFormats()) {
      try {
        isGraphFormat = KEGG2yGraph.writeableFileExtensions.valueOf(format.toLowerCase().trim())!=null;
      } catch (Exception e) {
        isGraphFormat = false;
      }
    }
    
    // Write graph formatted file
    if (isGraphFormat) {
      Format f = null;
      try {
        f = Format.valueOf(format);
      } catch (Exception e) {
        f=null;
      }
      
      KEGGtranslator trans2;
      if (translator instanceof KEGG2yGraph) {
        trans2 = translator;
      } else if (f!=null) {
        trans2 = BatchKEGGtranslator.getTranslator(Format.valueOf(format), translator.getKeggInfoManager());
      } else {
        trans2 = KEGG2yGraph.createKEGG2JPG(translator.getKeggInfoManager());
      }
      return ((KEGG2yGraph)trans2).writeToFile(graphLayer, file.getPath(), format);
      
    } else {
      
      return writeRealDocumentToFileUnchecked(file, format);
    }
  }
  
  /**
   * Invoke the file write. All checks have already been made and also
   * a message of success/ failure is sent by other methods. Simply
   * write to the file in this method here.
   * @param file
   * @param format
   * @return true if everything went ok, false else.
   * @throws Exception
   */
  protected abstract boolean writeRealDocumentToFileUnchecked(File file, String format) throws Exception;
  
}
