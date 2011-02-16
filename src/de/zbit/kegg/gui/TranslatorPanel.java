/*
 * Copyright (c) 2011 Center for Bioinformatics of the University of Tuebingen.
 * 
 * This file is part of KEGGtranslator, a program to convert KGML files from the
 * KEGG database into various other formats, e.g., SBML, GraphML, and many more.
 * Please visit <http://www.ra.cs.uni-tuebingen.de/software/KEGGtranslator> to
 * obtain the latest version of KEGGtranslator.
 * 
 * KEGGtranslator is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * KEGGtranslator is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with KEGGtranslator. If not, see
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package de.zbit.kegg.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.tolatex.SBML2LaTeX;
import org.sbml.tolatex.gui.LaTeXExportDialog;
import org.sbml.tolatex.io.LaTeXOptionsIO;

import y.view.EditMode;
import y.view.Graph2D;
import y.view.Graph2DView;
import y.view.Graph2DViewMouseWheelZoomListener;
import de.zbit.gui.GUITools;
import de.zbit.gui.JColumnChooser;
import de.zbit.gui.LayoutHelper;
import de.zbit.gui.ProgressBarSwing;
import de.zbit.gui.VerticalLayout;
import de.zbit.gui.BaseFrame.BaseAction;
import de.zbit.gui.prefs.PreferencesPanel;
import de.zbit.io.SBFileFilter;
import de.zbit.kegg.Translator;
import de.zbit.kegg.ext.RestrictedEditMode;
import de.zbit.kegg.ext.TranslatorPanelOptions;
import de.zbit.kegg.gui.TranslatorUI.Action;
import de.zbit.kegg.io.AbstractKEGGtranslator;
import de.zbit.kegg.io.BatchKEGGtranslator;
import de.zbit.kegg.io.KEGG2jSBML;
import de.zbit.kegg.io.KEGG2yGraph;
import de.zbit.kegg.io.KEGGtranslator;
import de.zbit.kegg.io.KEGGtranslatorIOOptions;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.util.AbstractProgressBar;
import de.zbit.util.FileDownload;
import de.zbit.util.Reflect;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.util.prefs.SBProperties;

/**
 * This should be used as a panel on a JTabbedPane.
 * It handles all the translating and visualizing, etc. of a KEGG pathway.
 * @author wrzodek
 */
public class TranslatorPanel extends JPanel {
  private static final long serialVersionUID = 6030311193210321410L;
  File inputFile;
  Format outputFormat;
  boolean documentHasBeenSaved=false;
  
  /**
   * Result of translating {@link #inputFile} to {@link #outputFormat}.
   */
  Object document = null;
  /**
   * An action is fired to this listener, when the translation is done
   * or failed with an error.
   */
  ActionListener translationListener = null;
  /**
   * We need to remember the translator for saving the file later on.
   */
  AbstractKEGGtranslator<?> translator = null;
  
  /**
   * Create a new translator-panel and initiates the translation.
   * @param inputFile
   * @param outputFormat
   */
  public TranslatorPanel(final File inputFile, final Format outputFormat, ActionListener translationResult) {
    super();
    setLayout(new BorderLayout());
    setOpaque(false);
    this.inputFile = inputFile;
    this.outputFormat = outputFormat;
    this.translationListener = translationResult;
    
    translate();
  }
  
  /**
   * Puts a download-KGML-panel on this Translator panel and replaces it
   * with the translation of the downloaded kgml, as soon as the user
   * has choosen a pathway.
   * @param translationResult
   */
  public TranslatorPanel(ActionListener translationResult) {
    super();
    setLayout(new BorderLayout());
    setOpaque(false);
    this.inputFile = null;
    this.outputFormat = null;
    this.translationListener = translationResult;
    
    showDownloadPanel(new LayoutHelper(this));
  }
  
  public void showDownloadPanel(LayoutHelper lh) {
    try {
      final PathwaySelector selector = PathwaySelector.createPathwaySelectorPanel(Translator.getFunctionManager(), lh);
      JComponent oFormat=null;
      if ((outputFormat == null) || (outputFormat == null)) {
        oFormat = (JColumnChooser) PreferencesPanel.getJComponentForOption(KEGGtranslatorIOOptions.FORMAT, (SBProperties)null, null);
        oFormat =((JColumnChooser) oFormat).getColumnChooser();
        lh.add("Please select the output format", oFormat, false);
      }
      final JComponent oFormatFinal = oFormat;

      JButton okButton = new JButton(GUITools.getOkButtonText());
      //okButton.addActionListener()
      JPanel p2 = new JPanel();
      p2.setLayout(new FlowLayout(FlowLayout.LEFT));
      p2.add(okButton);
      lh.add(p2);
      okButton.setEnabled(GUITools.isEnabled(lh.getContainer()));
      
      // Action
      final Container thiss = lh.getContainer();
      okButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // Show progress-bar
          removeAll();
          setLayout(new BorderLayout()); // LayoutHelper creates a GridBaglayout, reset it to default.
          final AbstractProgressBar pb = generateLoadingPanel(thiss, "Downloading '" + selector.getSelectedPathway() + "' " +
          		"for '"+selector.getOrganismSelector().getSelectedOrganism()+"'...");
          FileDownload.ProgressBar = pb;
          repaint();
          
          // Execute download and translation in new thread
          final SwingWorker<String, Void> downloadWorker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
              return KGMLSelectAndDownload.evaluateOKButton(selector);
            }
            protected void done() {
              String localFile=null;
              try {
                localFile = get();
              } catch (Exception e) {
                e.printStackTrace();
                GUITools.showErrorMessage(thiss, e);
              }
              if (localFile!=null) {            
                // Perform translation
                inputFile = new File(localFile);
                outputFormat = Format.valueOf(Reflect.invokeIfContains(oFormatFinal, "getSelectedItem").toString());
                removeAll();
                repaint();
                
                translate();
                GUITools.packParentWindow(thiss);
              } else {
                // Remove the tab
                thiss.getParent().remove(thiss);
              }
            }
          };
          downloadWorker.execute();
          
        }
      });
      
      // Show the selector.
      GUITools.enableOkButtonIfAllComponentsReady(lh.getContainer(), okButton);
      if (lh.getContainer() instanceof JComponent) {
        GUITools.setOpaqueForAllElements((JComponent)lh.getContainer(), false);
      }
    } catch (Throwable exc) {
      GUITools.showErrorMessage(lh.getContainer(), exc);
    }
    
  }
  
  /**
   * Translates the {@link #inputFile} to {@link #outputFormat}.
   */
  public void translate() {
    final AbstractProgressBar pb = generateLoadingPanel(this, "Translating pathway...");
    final JComponent thiss = this;
    
    final SwingWorker<Object, Void> translateWorker = new SwingWorker<Object, Void>() {
      @Override
      protected Object doInBackground() throws Exception {
        translator = (AbstractKEGGtranslator<?>) BatchKEGGtranslator.getTranslator(outputFormat, Translator.getManager());
        translator.setProgressBar(pb);
        return translator.translate(inputFile);
      }
      protected void done() {
        removeAll();
        // Get the resulting document and check and handle eventual errors.
        try {
          document = get();
        } catch (Throwable e) {
          GUITools.showErrorMessage(null, e);
          fireActionEvent(new ActionEvent(thiss,JOptionPane.ERROR,TranslatorUI.Action.TRANSLATION_DONE.toString()));
          return;
        }
        
        // Change the tab to the corresponding content.
        if (isSBML()) {
          SBMLDocument doc = (SBMLDocument) document;
          
          // Create a new visualization of the model.
          try {
            add(new SBMLModelSplitPane(doc));
          } catch (Exception e) {
            e.printStackTrace();
            GUITools.showErrorMessage(null, e);
            fireActionEvent(new ActionEvent(thiss,JOptionPane.ERROR,TranslatorUI.Action.TRANSLATION_DONE.toString()));
            return;
          }
          
        } else if (isGraphML()) {
          // Create a new visualization of the model.
          Graph2DView pane = new Graph2DView((Graph2D) document);
          add(pane);
          
          /*
           * Get settings to control visualization behaviour
           */
          SBPreferences prefs = SBPreferences.getPreferencesFor(TranslatorPanelOptions.class);
          
          // Set KEGGtranslator logo as background
          if (TranslatorPanelOptions.SHOW_LOGO_IN_GRAPH_BACKGROUND.getValue(prefs)) {
            RestrictedEditMode.addBackgroundImage(getClass().getResource("img/Logo2.png"), pane);
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
          EditMode editMode = new RestrictedEditMode();
          editMode.showNodeTips(true);
          pane.addViewMode(editMode);
          
          if (TranslatorPanelOptions.SHOW_PROPERTIES_TABLE.getValue(prefs)) {
            ((RestrictedEditMode)editMode).addPropertiesTable(pane);
          }
          
          pane.getCanvasComponent().addMouseWheelListener(new Graph2DViewMouseWheelZoomListener());
          pane.fitContent(true);
          pane.setFitContentOnResize(true);
        } 
        
        // Fire the listener
        validate();
        repaint();
        fireActionEvent(new ActionEvent(thiss,JOptionPane.OK_OPTION,TranslatorUI.Action.TRANSLATION_DONE.toString()));
        return;
      }

    };
    
    // Run the worker
    translateWorker.execute();
  }
  
  /**
   * @param actionEvent
   */
  private void fireActionEvent(ActionEvent actionEvent) {
    if (translationListener!=null) {
      translationListener.actionPerformed(actionEvent);
    }
  }

  /**
   * Returns a string representation of the contained pathway.
   * @return
   */
  public String getTitle() {
    if (isSBML()) {
      SBMLDocument doc = (SBMLDocument) document;
      // Set nice title
      String title = doc.isSetModel() && doc.getModel().isSetId() ? doc.getModel().getId() : doc.toString();
      return title;
    } else {
      return inputFile.getName();
    }
  }
  
  
  /**
   * Create and display a temporary loading panel with the given message and a
   * progress bar.
   * @param parent - may be null. Else: all elements will be placed on this container
   * @return - the ProgressBar of the container.
   */
  private static AbstractProgressBar generateLoadingPanel(Container parent, String loadingText) {
    Dimension panelSize = new Dimension(400, 75);
    
    // Create the panel
    Container panel = new JPanel(new VerticalLayout());
    panel.setPreferredSize(panelSize);
    
    // Create the label and progressBar
    JLabel jl = new JLabel((loadingText!=null && loadingText.length()>0)?loadingText:"Please wait...");
    //Font font = new java.awt.Font("Tahoma", Font.PLAIN, 12);
    //jl.setFont(font);
    
    JProgressBar prog = new JProgressBar();
    prog.setPreferredSize(new Dimension(panelSize.width - 20,
      panelSize.height / 4));
    panel.add(jl, BorderLayout.NORTH);
    panel.add(prog, BorderLayout.CENTER);
    
    if (panel instanceof JComponent) {
      GUITools.setOpaqueForAllElements((JComponent) panel, false);
    }
    
    if (parent!=null) {
      parent.add(panel);
    } else {
      // Display the panel in an jFrame
      JDialog f = new JDialog();
      f.setTitle(KEGGtranslator.APPLICATION_NAME);
      f.setSize(panel.getPreferredSize());
      f.setContentPane(panel);
      f.setPreferredSize(panel.getPreferredSize());
      f.setLocationRelativeTo(null);
      f.setVisible(true);
      f.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    }
    
    return new ProgressBarSwing(prog);
  }
  
  
  /**
   * @return true if and only if this panel contains an sbml document.
   */
  public boolean isSBML() {
    return (document!=null && document instanceof SBMLDocument);
  }
  
  /**
   * @return true if and only if this panel contains an Graph2D document.
   */
  public boolean isGraphML() {
    return (document!=null && document instanceof Graph2D);
  }
  
  public File saveToFile() {
    LinkedList<FileFilter> ff = new LinkedList<FileFilter>();
    
    // Create list of available ouput file filters
    SBFileFilter defaultFF;
    if (isSBML()) {
      defaultFF = SBFileFilter.createSBMLFileFilter();
      ff.add(defaultFF);
      ff.add(SBFileFilter.createTeXFileFilter());
      ff.add(SBFileFilter.createPDFFileFilter());
    } else if (isGraphML()){
      ff.add(SBFileFilter.createGraphMLFileFilter());
      ff.add(SBFileFilter.createGMLFileFilter());
      ff.add(SBFileFilter.createJPEGFileFilter());        
      ff.add(SBFileFilter.createGIFFileFilter());
      ff.add(SBFileFilter.createYGFFileFilter());
      ff.add(SBFileFilter.createTGFFileFilter());
      for (int i=0; i<ff.size(); i++) {
        if (((SBFileFilter)ff.get(i)).getExtension().toLowerCase().startsWith(this.outputFormat.toString().toLowerCase())) {
          ff.addFirst(ff.remove(i));
          break;
        }
      }
      defaultFF = (SBFileFilter) ff.getFirst();
    } else {
      return null;
    }
    
    // We also need to know the selected file filter!
    //File file = GUITools.saveFileDialog(this, TranslatorUI.saveDir, false, false, true,
      //JFileChooser.FILES_ONLY, ff.toArray(new FileFilter[0]));
    JFileChooser fc = GUITools.createJFileChooser(TranslatorUI.saveDir, false,
      false, JFileChooser.FILES_ONLY, ff.toArray(new FileFilter[0]));
    fc.setSelectedFile(inputFile.getPath().contains(".")?new File(inputFile.getPath().substring(0, inputFile.getPath().lastIndexOf('.'))):new File(inputFile.getPath()+'.'+defaultFF.getExtension()) );
    if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return null;
    
    // Check file
    File f = fc.getSelectedFile();
    String extension = ((SBFileFilter)fc.getFileFilter()).getExtension();
    
    // Eventually append extension to output file
    if (!f.getName().contains(".")) {
      f = new File(f.getPath() + '.' + extension);
    }
    
    // Check if file exists and is writable
    boolean showOverride = f.exists();
    if (!f.exists()) try {
      f.createNewFile();
    } catch (IOException e) {
      GUITools.showErrorMessage(this, e);
      e.printStackTrace();
      return null;
    }
		if (!f.canWrite() || f.isDirectory()) {
			GUITools.showNowWritingAccessWarning(this, f);
    } else if (!showOverride || (showOverride && GUITools.overwriteExistingFile(this, f))) {
      // This is the usual case
    	return saveToFile(f, extension);
		}
		return null;
  }
  
  /**
   * This does the real saving work, without checking write acces,
   * file exists, etc. and without asking the user anything.
   * 
   * @param file
   * @param format
   * @return the saved file.
   */
  public File saveToFile(File file, String format) {
    /*format = format.toLowerCase().trim();
    if (!file.getName().toLowerCase().endsWith(format)) {
      file = new File(file.getPath() + '.' + format);
    }*/
      
    if (file != null) {
      TranslatorUI.saveDir = file.getParent();
      if (SBFileFilter.isTeXFile(file) || SBFileFilter.isPDFFile(file) || format.equals("tex") || format.equals("pdf")) {
        writeLaTeXReport(file);
      } else if (translator instanceof KEGG2yGraph){
        try {
          ((KEGG2yGraph)translator).writeToFile((Graph2D) document, file.getPath(), format);
        } catch (Exception e) {
          e.printStackTrace();
          GUITools.showErrorMessage(this, e);
        }
      } else if (translator instanceof KEGG2jSBML){
        ((KEGG2jSBML)translator).writeToFile((SBMLDocument) document, file.getPath());
      }
    }
    
    documentHasBeenSaved = true;
    return file;
  }
  
  /**
   * @return true, if the document has been saved.
   */
  public boolean isSaved() {
    return (document==null || documentHasBeenSaved);
  }


  /**
   * Enabled and disables item in the menu bar, based on the content of this panel.
   * @param menuBar
   */
  public void updateButtons(JMenuBar menuBar) {
    if (isSBML()) {
      GUITools.setEnabled(true, menuBar, BaseAction.FILE_SAVE, Action.TO_LATEX, BaseAction.FILE_CLOSE);
    } else if (isGraphML()) {
      GUITools.setEnabled(true, menuBar, BaseAction.FILE_SAVE, BaseAction.FILE_CLOSE);
      GUITools.setEnabled(false, menuBar,Action.TO_LATEX);
    } else {
      // E.g. when translation still in progress, or on download frame
      GUITools.setEnabled(false, menuBar, BaseAction.FILE_SAVE, Action.TO_LATEX, BaseAction.FILE_CLOSE);
      
      if (this.inputFile==null) {
        // Download frame or invalid menu item
        GUITools.setEnabled(true, menuBar, BaseAction.FILE_CLOSE);
      }
    }
  }
  
  /**
   * @param targetFile - can be null.
   */
  public void writeLaTeXReport(File targetFile) {
    if (document==null) return;
    if (!isSBML()) {
      GUITools.showMessage("This option is only available for SBML documents.", KEGGtranslator.APPLICATION_NAME);
      return;
    }
    
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
