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
 * Copyright (C) 2011-2014 by the University of Tuebingen, Germany.
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
import java.util.List;

import javax.swing.filechooser.FileFilter;

import y.view.Graph2D;
import de.zbit.graph.gui.TranslatorGraphLayerPanel;
import de.zbit.graph.gui.TranslatorPanel;
import de.zbit.kegg.io.KEGG2yGraph;
import de.zbit.kegg.io.KEGGImporter;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.util.NotifyingWorker;

/**
 * Extension of {@link TranslatorPanel} to visualize yFiles
 * graphs.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class TranslatorGraphPanel extends TranslatorGraphLayerPanel<Graph2D> {
  private static final long serialVersionUID = -1083637150034491451L;
  
  /**
   * Create a new translator-panel and initiates the translation.
   * 
   * @param inputFile
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
    super(new KEGGImporter(inputFile, outputFormat), inputFile, outputFormat.toString(), translationResult);
  }
  
  /**
   * Initiates a download and translation of the given pathway.
   * @param pathwayID pathway identifier (e.g., "mmu00010")
   * @param outputFormat any GraphML compatible formats (unchecked).
   * @param translationResult
   */
  public TranslatorGraphPanel(String pathwayID, Format outputFormat,
    ActionListener translationResult) {
    super(new KEGGImporter(pathwayID, outputFormat), outputFormat.toString(), translationResult);
  }
  
  /**
   * Initiates a translation of the given pathway.
   * @param pathway a given KEGG pathway
   * @param outputFormat any GraphML compatible formats (unchecked).
   * @param translationResult
   */
  public TranslatorGraphPanel(Pathway pathway, Format outputFormat,
    ActionListener translationResult) {
    super(new KEGGImporter(pathway, outputFormat), outputFormat.toString(), translationResult);
  }
  
  /**
   * Initiates a translation of the given pathway.
   * @param pathwayImporter a given KEGG pathway
   * @param outputFormat any GraphML compatible formats (unchecked).
   * @param translationResult
   */
  public TranslatorGraphPanel(NotifyingWorker<?> pathwayImporter, Format outputFormat,
    ActionListener translationResult) {
    super(pathwayImporter, outputFormat.toString(), translationResult);
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
   * @see de.zbit.kegg.gui.TranslatorGraphLayerPanel#createGraphFromDocument(java.lang.Object)
   */
  @Override
  protected Graph2D createGraphFromDocument(Graph2D document) {
    return document;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorGraphLayerPanel#getOutputFileFilterForRealDocument()
   */
  @Override
  protected List<FileFilter> getOutputFileFilterForRealDocument() {
    return null; // Graph formats are included by default!
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorGraphLayerPanel#writeRealDocumentToFileUnchecked(java.io.File, java.lang.String)
   */
  @Override
  protected boolean writeRealDocumentToFileUnchecked(File file, String format)
      throws Exception {
    return ((KEGG2yGraph)getTranslator()).writeToFile(document, file.getPath(), format);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.graph.gui.TranslatorGraphLayerPanel#isDetailPanelAvailable()
   */
  @Override
  public boolean isDetailPanelAvailable() {
    return false;
  }
  
}
