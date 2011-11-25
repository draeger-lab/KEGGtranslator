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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.sbml.jsbml.AbstractNamedSBase;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.ext.SBasePlugin;
import org.sbml.jsbml.ext.layout.BoundingBox;
import org.sbml.jsbml.ext.layout.ExtendedLayoutModel;
import org.sbml.jsbml.ext.layout.Layout;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;
import org.sbml.jsbml.ext.qual.Input;
import org.sbml.jsbml.ext.qual.Output;
import org.sbml.jsbml.ext.qual.QualitativeModel;
import org.sbml.jsbml.ext.qual.Transition;

import y.base.Node;
import y.view.Graph2D;
import y.view.NodeRealizer;
import y.view.ShapeNodeRealizer;
import de.zbit.gui.GUITools;
import de.zbit.io.SBFileFilter;
import de.zbit.kegg.io.KEGG2SBMLLayoutExtension;
import de.zbit.kegg.io.KEGG2SBMLqual;
import de.zbit.kegg.io.KEGG2jSBML;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;

/**
 * A basic panel which uses a GraphLayer to visualize SBML documents.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class TranslatorSBMLgraphPanel extends TranslatorGraphLayerPanel<SBMLDocument> {
  private static final long serialVersionUID = 2361032893527709646L;
  
  /**
   * If false, shows the normal, quantitative SBML model. If true,
   * shows the qual model.
   */
  private boolean showQualModel=false;


  /**
   * Create a new translator-panel and initiates the translation.
   * @param inputFile
   * @param outputFormat
   * @param translationResult
   */
  public TranslatorSBMLgraphPanel(File inputFile, ActionListener translationResult) {
    this(inputFile, Format.SBML, translationResult);
  }

  /**
   * Create a new translator-panel and initiates the translation.
   * @param inputFile
   * @param outputFormat
   * @param translationResult
   */
  public TranslatorSBMLgraphPanel(File inputFile, Format outputFormat, ActionListener translationResult) {
    super(inputFile, outputFormat, translationResult);
  }

  /**
   * Initiates a download and translation of the given pathway.
   * @param pathwayID pathway identifier (e.g., "mmu00010")
   * @param outputFormat
   * @param translationResult
   */
  public TranslatorSBMLgraphPanel(String pathwayID, Format outputFormat, ActionListener translationResult) {
    super(pathwayID, outputFormat, translationResult);
  }
  
  /**
   * Initiates a download and translation of the given pathway.
   * @param pathwayID pathway identifier (e.g., "mmu00010")
   * @param translationResult
   */
  public TranslatorSBMLgraphPanel(String pathwayID, ActionListener translationResult) {
    this(pathwayID, Format.SBML, translationResult);
  }
  
  public TranslatorSBMLgraphPanel(File inputFile, Format outputFormat, ActionListener translationResult, SBMLDocument document) {
    this(inputFile, outputFormat, translationResult, document, false);
  }
  
  public TranslatorSBMLgraphPanel(File inputFile, Format outputFormat, ActionListener translationResult, SBMLDocument document, boolean showQualModel) {
    super(inputFile, outputFormat, translationResult, document);
    this.showQualModel = showQualModel;
    
    try {
      createTabContent();
    } catch (Throwable e) {
      GUITools.showErrorMessage(null, e);
      fireActionEvent(new ActionEvent(this,JOptionPane.ERROR,TranslatorUI.Action.TRANSLATION_DONE.toString()));
      return;
    }
  }



  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorGraphLayerPanel#createGraphFromDocument(java.lang.Object)
   */
  @Override
  protected Graph2D createGraphFromDocument(SBMLDocument document) {
    Graph2D simpleGraph = new Graph2D();
    if (document==null || !document.isSetModel()) return simpleGraph;
    
    // Get list of species
    List<? extends AbstractNamedSBase> species;
    if (showQualModel) {
      SBasePlugin qm = document.getModel().getExtension(KEGG2SBMLqual.QUAL_NS);
      if (qm!=null && qm instanceof QualitativeModel) {
        QualitativeModel q = (QualitativeModel) qm;
        if (!q.isSetListOfQualitativeSpecies()) return simpleGraph;
        species = q.getListOfQualitativeSpecies();
      } else {
        log.warning("SBMLDocument contains no qual-model.");
        return simpleGraph;
      }
    } else {
      species = document.getModel().getListOfSpecies();
    }
    
    // Eventually get the layout extension
    SBasePlugin layoutExtension = document.getModel().getExtension(KEGG2SBMLLayoutExtension.LAYOUT_NS);
    boolean useLayoutExtension = layoutExtension!=null;
    Map<String, BoundingBox> layoutMap = null;
    if (useLayoutExtension) {
      if (((ExtendedLayoutModel)layoutExtension).isSetListOfLayouts()) {
        // TODO: For generic releases, it would be nice to have a JList
        // that let's the user choose the layout.
        Layout l = ((ExtendedLayoutModel)layoutExtension).getLayout(0);
        layoutMap = new HashMap<String, BoundingBox>();
        for (SpeciesGlyph sg: l.getListOfSpeciesGlyphs()) {
          if (sg.isSetBoundingBox()) {
            layoutMap.put(sg.getSpecies(), sg.getBoundingBox());
          }
        }
        useLayoutExtension = layoutMap.size()>0;
      } else {
        useLayoutExtension = false;
      }
    }
    
    // Convert each species to a graph node
    Map<String, Node> species2node = new HashMap<String, Node>();
    int nodesWithoutCoordinates=0;
    int COLUMNS = species.size()/5; // Show in 5 rows by default
    for (AbstractNamedSBase s : species) {
      Node n = simpleGraph.createNode();
      species2node.put(s.getId(), n);
      
      // TODO: Set Node shape (and color?) based on SBO-terms
      NodeRealizer nr;
      if (!s.isSetSBOTerm()) {
        nr = simpleGraph.getRealizer(n);
      } else {
        nr = new ShapeNodeRealizer(ShapeNodeRealizer.PARALLELOGRAM);
        simpleGraph.setRealizer(n, nr);        
      } 
      
      // Initialize default layout variables
      double x=Double.NaN;
      double y=Double.NaN;
      double w=46;
      double h=17;
      
      // Get information from the layout extension
      if (useLayoutExtension) {
        BoundingBox g = layoutMap.get(s.getId());
        if (g!=null) {
          if (g.isSetDimensions()) {
            w = g.getDimensions().getWidth();
            h = g.getDimensions().getHeight();
          }
          if (g.isSetPoint()) {
            x = g.getPoint().getX();
            y = g.getPoint().getY();
          }
        }
      }
      
      // Auto-set missing coordinates
      if (Double.isNaN(x) || Double.isNaN(y)) {
        // Make a simple grid-layout to set some initial coords
        x = (nodesWithoutCoordinates%COLUMNS)*(w+w/2);
        y = (nodesWithoutCoordinates/COLUMNS)*(h+h);
        
        nodesWithoutCoordinates++;
      }
      
      // Set coordinates
      nr.setCenterX(x);
      nr.setCenterY(y);
      nr.setWidth(w);
      nr.setHeight(h);
      
      nr.setLabelText(s.getName());
    }
    
    if (showQualModel) {
      QualitativeModel qm = ((QualitativeModel)document.getModel().getExtension(KEGG2SBMLqual.QUAL_NS));
      for (Transition t : qm.getListOfTransitions()) {
        // TODO: Actually, instead of making n*m edges
        // make n edges to a fake-node, that branches off to m-nodes
        // (correct SBML/SBGN-Style).
        for (Input i: t.getListOfInputs()) {
          for (Output o: t.getListOfOutputs()) {
            Node source = species2node.get(i.getQualitativeSpecies());
            Node target = species2node.get(o.getQualitativeSpecies());
            
            simpleGraph.createEdge(source, target);
            
            // TODO: Change edge shape based on properties
            //EdgeRealizer nr = simpleGraph.getRealizer(e);
          }
        }
      }
    } else {
      // TODO: Reactions.
    }
    
    // TODO: Apply Hirarchic layout if no layoutExtension is available. 
    
    return simpleGraph;
  }


  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorGraphLayerPanel#getOutputFileFilterForRealDocument()
   */
  @Override
  protected List<FileFilter> getOutputFileFilterForRealDocument() {
    List<FileFilter> ff = new LinkedList<FileFilter>();
    ff.add(SBFileFilter.createSBMLFileFilter());
    return ff;
  }


  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorGraphLayerPanel#writeRealDocumentToFileUnchecked(java.io.File, java.lang.String)
   */
  @Override
  protected boolean writeRealDocumentToFileUnchecked(File file, String format)
    throws Exception {
    if (SBFileFilter.isTeXFile(file) || SBFileFilter.isPDFFile(file) || format.equals("tex") || format.equals("pdf")) {
      TranslatorSBMLPanel.writeLaTeXReport(file, document);
      return true; // Void result... can't check.
    } else {
      return ((KEGG2jSBML)getTranslator()).writeToFile(document, file.getPath());
    }
  }
}
