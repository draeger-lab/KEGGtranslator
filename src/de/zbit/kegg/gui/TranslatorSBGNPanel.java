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
import java.util.LinkedList;
import java.util.List;

import javax.swing.filechooser.FileFilter;

import org.sbgn.bindings.Sbgn;

import y.view.Graph2D;
import de.zbit.graph.gui.TranslatorGraphLayerPanel;
import de.zbit.graph.io.SBGN2GraphML;
import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.kegg.io.KEGG2SBGN;
import de.zbit.kegg.io.KEGGImporter;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;

/**
 * A basic panel which uses a GraphLayer to visualize SBGN documents.
 * @author Manuel Ruff
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class TranslatorSBGNPanel extends TranslatorGraphLayerPanel<Sbgn>{
  private static final long serialVersionUID = -6585611929238639630L;
  
  
  /**
   * Create a new translator-panel and initiates the translation.
   * 
   * @param inputFile
   * @param translationResult
   */
  public TranslatorSBGNPanel(File inputFile, ActionListener translationResult) {
    this(inputFile, Format.SBGN, translationResult);
  }
  
  /**
   * Create a new translator-panel and initiates the translation.
   * @param inputFile
   * @param outputFormat
   * @param translationResult
   */
  public TranslatorSBGNPanel(File inputFile, Format outputFormat,
    ActionListener translationResult) {
    super(new KEGGImporter(inputFile, outputFormat), inputFile, outputFormat.toString(), translationResult);
  }
  
  /**
   * Initiates a download and translation of the given pathway.
   * @param pathwayID pathway identifier (e.g., "mmu00010")
   * @param outputFormat
   * @param translationResult
   */
  public TranslatorSBGNPanel(String pathwayID, Format outputFormat,
    ActionListener translationResult) {
    super(new KEGGImporter(pathwayID, outputFormat), outputFormat.toString(), translationResult);
  }
  
  /**
   * Initiates a download and translation of the given pathway.
   * @param pathwayID pathway identifier (e.g., "mmu00010")
   * @param translationResult
   */
  public TranslatorSBGNPanel(String pathwayID, ActionListener translationResult) {
    this(pathwayID, Format.SBGN, translationResult);
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorGraphLayerPanel#createGraphFromDocument(java.lang.Object)
   */
  @Override
  protected Graph2D createGraphFromDocument(Sbgn document) {
    SBGN2GraphML converter = new SBGN2GraphML();
    return converter.createGraph(document);
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorGraphLayerPanel#getOutputFileFilterForRealDocument()
   */
  @Override
  protected List<FileFilter> getOutputFileFilterForRealDocument() {
    List<FileFilter> ff = new LinkedList<FileFilter>();
    ff.add(SBFileFilter.createSBGNFileFilter());
    return ff;
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorGraphLayerPanel#writeRealDocumentToFileUnchecked(java.io.File, java.lang.String)
   */
  @Override
  protected boolean writeRealDocumentToFileUnchecked(File file, String format)
      throws Exception {
    return ((KEGG2SBGN)getTranslator()).writeToFile(document, file.getPath());
  }
  
  /* (non-Javadoc)
   * @see de.zbit.graph.gui.TranslatorGraphLayerPanel#isDetailPanelAvailable()
   */
  @Override
  public boolean isDetailPanelAvailable() {
    return false;
  }
  
}
