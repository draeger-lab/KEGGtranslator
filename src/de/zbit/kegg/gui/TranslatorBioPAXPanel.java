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

import javax.swing.JScrollPane;
import javax.swing.filechooser.FileFilter;

import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;

import y.view.Graph2D;
import y.view.HitInfo;
import de.zbit.graph.gui.TranslatorGraphLayerPanel;
import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.kegg.io.AbstractKEGGtranslator;
import de.zbit.kegg.io.KEGG2BioPAX;
import de.zbit.kegg.io.KEGG2yGraph;
import de.zbit.kegg.io.KEGGImporter;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.kegg.parser.pathway.Pathway;

/**
 * This panel uses the BioPAX2SBML Converter to visualize BioPAX content.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class TranslatorBioPAXPanel extends TranslatorGraphLayerPanel<Model>{

  private static final long serialVersionUID = -6585611929238639630L;


  /**
   * Create a new translator-panel and initiates the translation.
   * @param inputFile
   * @param outputFormat
   * @param translationResult
   */
  public TranslatorBioPAXPanel(File inputFile, Format outputFormat,
    ActionListener translationResult) {
    super(new KEGGImporter(inputFile, outputFormat), inputFile, outputFormat.toString(), translationResult);
  }

  /**
   * Initiates a download and translation of the given pathway.
   * @param pathwayID pathway identifier (e.g., "mmu00010")
   * @param outputFormat
   * @param translationResult
   */
  public TranslatorBioPAXPanel(String pathwayID, Format outputFormat,
    ActionListener translationResult) {
    super(new KEGGImporter(pathwayID, outputFormat), outputFormat.toString(), translationResult);
  }

  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorGraphLayerPanel#createGraphFromDocument(java.lang.Object)
   */
  @Override
  protected Graph2D createGraphFromDocument(Model document) {
    if (getTranslator()==null) return null;
    Pathway sourcePW = getTranslator().getLastTranslatedPathway();
    if (sourcePW==null) return null;
    KEGG2yGraph toGraph = KEGG2yGraph.createKEGG2GraphML(AbstractKEGGtranslator.getKeggInfoManager());
    toGraph.setDrawArrowsForReactions(true);
    return toGraph.translate(sourcePW);
  }


  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorGraphLayerPanel#getOutputFileFilterForRealDocument()
   */
  @Override
  protected List<FileFilter> getOutputFileFilterForRealDocument() {
    List<FileFilter> ff = new LinkedList<FileFilter>();
    if (document!=null && document.getLevel()==BioPAXLevel.L2) {
      ff.add(SBFileFilter.createBioPAXFileFilterL2());
    } else if (document!=null && document.getLevel()==BioPAXLevel.L3) {
      ff.add(SBFileFilter.createBioPAXFileFilterL3());
    } else {
      ff.add(SBFileFilter.createBioPAXFileFilter());
    }
    ff.add(SBFileFilter.createSIFFileFilter());
    
    return ff;
  }


  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorGraphLayerPanel#writeRealDocumentToFileUnchecked(java.io.File, java.lang.String)
   */
  @Override
  protected boolean writeRealDocumentToFileUnchecked(File file, String format)
    throws Exception {
    if (format.equalsIgnoreCase("sif")) {
      return ((KEGG2BioPAX)getTranslator()).writeToSIFFile(document, file.getPath());
    } else {
      return ((KEGG2BioPAX)getTranslator()).writeToFile(document, file.getPath());
    }
  }
  
  /* (non-Javadoc)
   * @see de.zbit.graph.gui.TranslatorGraphLayerPanel#isAllowedToSaveAsGraphFormats()
   */
  @Override
  public boolean isAllowedToSaveAsGraphFormats() {
    return false;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorGraphLayerPanel#updateDetailPanel(javax.swing.JScrollPane, y.view.HitInfo)
   */
  @Override
  protected void updateDetailPanel(JScrollPane detailPanel, HitInfo clickedObjects) {

// TODO: Make a table, showing BioPAX properties of selected objects.
    
//    BioPAXElement bpe=null;
//    // In order to use properties you first need to get an EditorMap
//    EditorMap editorMap = new SimpleEditorMap(BioPAXLevel.L3);
//    // And then get all the editors for our biopax element
//    Set<PropertyEditor> editors = editorMap.getEditorsOf(bpe);
//    // Let’s prepare a table to return values
//    String value[][] = new String[2][editors.size()];
//    int row =0;
//    
// // For each property
//    for (PropertyEditor editor : editors)
//    {
//    // First column is the name of the property, e.g. “Name”
//    value[0][row]= editor.getProperty();
//    // Second column is the value e.g. “p53”, note that the value is // sometimes a Set and we
//    are using the Set.toString() to display // the contents of multiple cardinality properties
//    value[1][row] = editor.getValueFromBean(bpe).toString();
//    // increase the row index
//    row++;
//    }

    
  }
}
