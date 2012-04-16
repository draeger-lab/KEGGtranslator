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

import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.tolatex.LaTeXOptions;
import org.sbml.tolatex.SBML2LaTeX;
import org.sbml.tolatex.gui.LaTeXExportDialog;
import org.sbml.tolatex.io.LaTeXOptionsIO;

import de.zbit.graph.gui.TranslatorGraphLayerPanel;
import de.zbit.graph.gui.TranslatorPanel;
import de.zbit.graph.gui.TranslatorSBMLgraphPanel;
import de.zbit.gui.GUITools;
import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.kegg.io.KEGG2SBMLqual;
import de.zbit.kegg.io.KEGG2jSBML;
import de.zbit.kegg.io.KEGGImporter;
import de.zbit.kegg.io.KEGGtranslator;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.sbml.gui.SBMLModelSplitPane;
import de.zbit.util.prefs.SBPreferences;

/**
 * Extension of {@link TranslatorPanel} to visualize SBML
 * Documents of translated pathways.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class TranslatorSBMLPanel extends TranslatorPanel<SBMLDocument> {
  private static final long serialVersionUID = -1110886974773446666L;

  /**
   * Create a new translator-panel and initiates the translation.
   * @param inputFile
   * @param outputFormat
   * @param translationResult
   */
  public TranslatorSBMLPanel(File inputFile, ActionListener translationResult) {
    this(inputFile, Format.SBML, translationResult);
  }

  /**
   * Create a new translator-panel and initiates the translation.
   * @param inputFile
   * @param outputFormat any SBML compatible formats (unchecked).
   * @param translationResult
   */
  public TranslatorSBMLPanel(File inputFile, Format outputFormat,
    ActionListener translationResult) {
    super(new KEGGImporter(inputFile, outputFormat), inputFile, outputFormat.toString(), translationResult);
  }

  /**
   * Initiates a download and translation of the given pathway.
   * @param pathwayID pathway identifier (e.g., "mmu00010")
   * @param outputFormat any SBML compatible formats (unchecked).
   * @param translationResult
   */
  public TranslatorSBMLPanel(String pathwayID, Format outputFormat,
    ActionListener translationResult) {
    super(new KEGGImporter(pathwayID, outputFormat), outputFormat.toString(), translationResult);
  }
  
  /**
   * Initiates a download and translation of the given pathway.
   * @param pathwayID pathway identifier (e.g., "mmu00010")
   * @param translationResult
   */
  public TranslatorSBMLPanel(String pathwayID, ActionListener translationResult) {
    this(pathwayID, Format.SBML, translationResult);
  }

  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorPanel#createTabContent()
   */
  @Override
  public void createTabContent() throws Exception {
    JTabbedPane tabs = new JTabbedPane();
    
    // Create a new visualization of the model.
    SBMLModelSplitPane treePane = new SBMLModelSplitPane(document, 
      SBPreferences.getPreferencesFor(LaTeXOptions.class).getBoolean(
        LaTeXOptions.PRINT_NAMES_IF_AVAILABLE));
    
    // TODO: Nice icons
    tabs.addTab("Detail view", null, treePane);
    
    
    TranslatorPanel<SBMLDocument> quantGraph=null;
    if (hasSpecies(document)) {
      quantGraph = createGraphPanel(false);
      tabs.addTab("Graph view", quantGraph);
    }
    
    if (KEGG2SBMLqual.hasQualSpecies(document)) {
      TranslatorPanel<SBMLDocument> qualGraph = createGraphPanel(true);
      tabs.addTab("Graph view", qualGraph);
      if (quantGraph!=null) {
        tabs.setTitleAt(tabs.indexOfComponent(quantGraph), "Graph of quantitative model");
        tabs.setTitleAt(tabs.indexOfComponent(qualGraph), "Graph of qualitative model");
      }
    }
    
    // Add tabs to content
    add (tabs.getTabCount()>1?tabs:treePane);
  }

  /**
   * @param forQual show the qual model or the quantitative model.
   * @return {@link TranslatorSBMLgraphPanel} wrapping some methods
   * of this panel.
   */
  protected TranslatorGraphLayerPanel<SBMLDocument> createGraphPanel(boolean forQual) {
    final TranslatorSBMLPanel thiss = this;
    //super(new KEGGImporter(inputFile, outputFormat), outputFormat.toString(), translationResult);

    TranslatorGraphLayerPanel<SBMLDocument> qualGraph = new TranslatorSBMLgraphPanel(getInputFile(), 
      (forQual?Format.SBML_QUAL: Format.SBML).toString(), getUIActionListener(), getDocument(), forQual) {
      private static final long serialVersionUID = -2987555994319694097L;

      /* (non-Javadoc)
       * @see de.zbit.kegg.gui.TranslatorPanel#getTranslator()
       */
      @Override
      public KEGGtranslator<?> getTranslator() {
        return thiss.getTranslator();
      }
      @Override
      public Object getData(String key) {
        return thiss.getData(key);
      }
      @Override
      public void setData(String key, Object object) {
        thiss.setData(key, object);
      }
      @Override
      public boolean isSaved() {
        return thiss.isSaved();
      }
    };
    
    // Setup the background image
    TranslatorPanelTools.setupBackgroundImage(qualGraph);
    
    return qualGraph;
  }

  /**
   * Check if any {@link SBMLDocument} has at least one species.
   * @param document
   * @return true if the document has at least one species
   */
  public static boolean hasSpecies(SBMLDocument document) {
    if (document==null) return false;
    if (!document.isSetModel()) return false;
    if (!document.getModel().isSetListOfSpecies()) return false;
    return (document.getModel().getListOfSpecies().size()>0);
  }

  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorPanel#getOutputFileFilter()
   */
  @Override
  protected List<FileFilter> getOutputFileFilter() {
    LinkedList<FileFilter> ff = new LinkedList<FileFilter>();
    ff.add(SBFileFilter.createSBMLFileFilter());
    ff.add(SBFileFilter.createTeXFileFilter());
    ff.add(SBFileFilter.createPDFFileFilter());
    // TODO: if graph is active also show graph file filters.
    return ff;
  }

  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorPanel#getTitle()
   */
  @Override
  public String getTitle() {
    if (!isReady()) return super.getTitle();
    // Set nice title
    String title = document.isSetModel() && document.getModel().isSetName() ? document.getModel().getName() : super.getTitle();
    return title;
  }

  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorPanel#writeToFileUnchecked(java.io.File, java.lang.String)
   */
  @Override
  protected boolean writeToFileUnchecked(File file, String format) throws Exception {
    // TODO Write graph formats (wrap to responsible panel).
    if (SBFileFilter.isTeXFile(file) || SBFileFilter.isPDFFile(file) || format.equals("tex") || format.equals("pdf")) {
      if (!isReady()) return false;
      writeLaTeXReport(file, document);
      return true; // Void result... can't check.
    } else {
      return ((KEGG2jSBML)getTranslator()).writeToFile(document, file.getPath());
    }
  }
  
  
  /**
   * @param targetFile - can be null.
   */
  public static void writeLaTeXReport(File targetFile, SBMLDocument document) {
    if (document==null) return;
    
    final SBMLDocument doc = (SBMLDocument) document;
    if ((doc != null) && LaTeXExportDialog.showDialog(null, doc, targetFile)) {
      if (targetFile == null) {
        SBPreferences prefsIO = SBPreferences.getPreferencesFor(LaTeXOptionsIO.class);
        targetFile = new File(prefsIO.get(LaTeXOptionsIO.REPORT_OUTPUT_FILE));
      }
      
      // Run in background
      final File finalTargetFile = targetFile;
      SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
        @Override
        protected Void doInBackground() throws Exception {
          try {
            SBML2LaTeX.convert(doc, finalTargetFile, true);
          } catch (Exception exc) {
            GUITools.showErrorMessage(null, exc);
          }
          return null;
        }
      };
      worker.execute();
      //---
      
    }
  }
  
}
