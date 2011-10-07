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
 * 
 * There are special restrictions for this file. Each procedure that
 * is using the yFiles API must stick to their license restrictions.
 * Please see the following link for more information
 * <http://www.yworks.com/en/products_yfiles_sla.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.kegg.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.tolatex.LaTeXOptions;
import org.sbml.tolatex.SBML2LaTeX;
import org.sbml.tolatex.gui.LaTeXExportDialog;
import org.sbml.tolatex.io.LaTeXOptionsIO;

import y.view.DefaultGraph2DRenderer;
import y.view.EditMode;
import y.view.Graph2D;
import y.view.Graph2DView;
import y.view.Graph2DViewMouseWheelZoomListener;
import de.zbit.graph.RestrictedEditMode;
import de.zbit.gui.BaseFrameTab;
import de.zbit.gui.GUITools;
import de.zbit.gui.JLabeledComponent;
import de.zbit.gui.LayoutHelper;
import de.zbit.gui.ProgressBarSwing;
import de.zbit.gui.VerticalLayout;
import de.zbit.gui.BaseFrame.BaseAction;
import de.zbit.gui.prefs.PreferencesPanel;
import de.zbit.io.SBFileFilter;
import de.zbit.kegg.Translator;
import de.zbit.kegg.ext.TranslatorPanelOptions;
import de.zbit.kegg.io.AbstractKEGGtranslator;
import de.zbit.kegg.io.BatchKEGGtranslator;
import de.zbit.kegg.io.KEGG2jSBML;
import de.zbit.kegg.io.KEGG2yGraph;
import de.zbit.kegg.io.KEGGtranslatorIOOptions;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.sbml.gui.SBMLModelSplitPane;
import de.zbit.util.AbstractProgressBar;
import de.zbit.util.FileDownload;
import de.zbit.util.Reflect;
import de.zbit.util.TranslatorTools;
import de.zbit.util.ValuePairUncomparable;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.util.prefs.SBProperties;

/**
 * This should be used as a panel on a JTabbedPane.
 * It handles all the translating and visualizing, etc. of a KEGG pathway.
 * 
 * <p><i>Note:<br/>
 * Due to yFiles license requirements, we have to obfuscate this class
 * in the JAR release of this application. Thus, this class
 * can not be found by using the class name.<br/> If you can provide us
 * with a proof of possessing a yFiles license yourself, we can send you
 * an unobfuscated release of KEGGtranslator.</i></p>
 * 
 * @author Clemens Wrzodek
 * @since 1.0
 * @version $Rev$
 */
public class TranslatorPanel extends JPanel implements BaseFrameTab {
  private static final long serialVersionUID = 6030311193210321410L;
  public static final transient Logger log = Logger.getLogger(TranslatorPanel.class.getName());
  
  /**
   * This is the path where the background-logo will be loaded from. This must be
   * relative to the current path!
   */
  public static String logoResourcePath = "img/Logo2.png";
  
  /**
   * KGML formatted input file
   */
  File inputFile;
  
  /**
   * Desired output file format
   */
  Format outputFormat;
  
  /**
   * Boolean flag to remember weather the contained {@link #document}
   * has been saved successfully at least once.
   */
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
   * Allows the programmer to store any additional data along with this panel.
   */
  Map<String, Object> additionalData=null;
  
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
   * Initiates a download and translation of the given pathway.
   * @param pathwayID pathway identifier (e.g., "mmu00010")
   * @param outputFormat
   * @param translationResult
   */
  public TranslatorPanel(final String pathwayID, 
    final Format outputFormat, ActionListener translationResult) {
    super();
    setLayout(new BorderLayout());
    setOpaque(false);
    this.inputFile = null;
    this.outputFormat = null;
    this.translationListener = translationResult;
    
    // Execute download and translation in new thread
    showTemporaryLoadingPanel(pathwayID, null);
    final SwingWorker<String, Void> downloadWorker = new SwingWorker<String, Void>() {
      @Override
      protected String doInBackground() throws Exception {
        try {
          return KGMLSelectAndDownload.downloadPathway(pathwayID, false);
        } catch (Exception e) {
          // Mostly 1) pathway does not exists for organism or 2) no connection to server
          GUITools.showErrorMessage(null, e);
          return null;
        }
      }
      protected void done() {
        String localFile=null;
        try {
          localFile = get();
        } catch (Exception e) {
          GUITools.showErrorMessage(null, e);
        }
        pathwayDownloadComplete(localFile, outputFormat);
      }
    };
    downloadWorker.execute();
  }
  
  /**
   * Puts a download-KGML-panel on this Translator panel and replaces it
   * with the translation of the downloaded kgml, as soon as the user
   * has choosen a pathway.
   * @param translationResult
   */
  public TranslatorPanel(ActionListener translationResult) {
    this(null, translationResult);
  }
  
  /**
   * Shows a download pathway dialog with a predefined output format.
   * @see TranslatorPanel#TranslatorPanel(ActionListener)
   * @param outputFormat predefined output format of downloaded pathway.
   * @param translationResult
   */
  public TranslatorPanel(final Format outputFormat, ActionListener translationResult) {
    super();
    setLayout(new BorderLayout());
    setOpaque(false);
    this.inputFile = null;
    this.outputFormat = outputFormat;
    this.translationListener = translationResult;
    
    showDownloadPanel(new LayoutHelper(this));
  }
  
  public void showDownloadPanel(LayoutHelper lh) {
    try {
      final PathwaySelector selector = PathwaySelector.createPathwaySelectorPanel(Translator.getFunctionManager(), lh);
      JComponent oFormat=null;
      if ((outputFormat == null) || (outputFormat == null)) {
        oFormat = PreferencesPanel.getJComponentForOption(KEGGtranslatorIOOptions.FORMAT, (SBProperties)null, null);
        oFormat =((JLabeledComponent) oFormat).getColumnChooser(); // Trim
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
//      final Container thiss = lh.getContainer();
      okButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          firePropertyChange("PATHWAY_NAME", null, selector.getSelectedPathway());
          firePropertyChange("ORGANISM_NAME", null, selector.getOrganismSelector().getSelectedOrganism());
          
          showTemporaryLoadingPanel(selector.getSelectedPathway(),
            selector.getOrganismSelector().getSelectedOrganism());
          
          // Execute download and translation in new thread
          final SwingWorker<String, Void> downloadWorker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
              try {
                return KGMLSelectAndDownload.evaluateOKButton(selector);
              } catch (Exception e) {
                // Mostly 1) pathway does not exists for organism or 2) no connection to server
                GUITools.showErrorMessage(null, e);
                return null;
              }
            }
            protected void done() {
              String localFile=null;
              try {
                localFile = get();
              } catch (Exception e) {
                e.printStackTrace();
                GUITools.showErrorMessage(null, e);
              }
              Format outFormat = outputFormat;
              if (oFormatFinal!=null) {
                outFormat = Format.valueOf(Reflect.invokeIfContains(oFormatFinal, "getSelectedItem").toString());
              }
              pathwayDownloadComplete(localFile, outFormat);
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
  
  private void showTemporaryLoadingPanel(String pwName, String organism) {
    // Show progress-bar
    removeAll();
    setLayout(new BorderLayout()); // LayoutHelper creates a GridBaglayout, reset it to default.
    final AbstractProgressBar pb = generateLoadingPanel(this, "Downloading '" + pwName + "' " +
      (organism!=null&&organism.length()>0? "for '"+organism+"'...":""));
    FileDownload.ProgressBar = pb;
    repaint();
  }
  
  /**
   * Create, display and return a temporary statusLabel ({@link JLabel})
   * and a {@link JProgressBar} at the bottom of this panel. Does not
   * touch existing content.
   * 
   * <p>Do not forget to call {@link #hideTemporaryLoadingBar()} afterwards.
   * 
   * @param initialStatusText
   * @return ValuePairUncomparable<JLabel, JProgressBar>()
   */
  public ValuePairUncomparable<JLabel, JProgressBar> showTemporaryLoadingBar(String initialStatusText) {
    setEnabled(false);
    try {
      TranslatorTools.enableViews(((Graph2D)document),false);
    } catch (Throwable e) {}
    JPanel statusBar = new JPanel();
    
    JLabel statusLabel = new JLabel(initialStatusText);
    final Dimension minimumSize = statusLabel.getMinimumSize();
    statusLabel.setMinimumSize(new Dimension(Math.max(200, minimumSize.width), minimumSize.height));
    statusBar.add(statusLabel, BorderLayout.LINE_START);
    
    JProgressBar jp = new JProgressBar();
    jp.setIndeterminate(true);
    statusBar.add(jp, BorderLayout.CENTER);
    
    add(statusBar, BorderLayout.SOUTH);
    //invalidate();
    //super.repaint(); // No need to repaint graph.
    return new ValuePairUncomparable<JLabel, JProgressBar>(statusLabel, jp);
  }
  
  /**
   * Hide the temporary loading status bar, created with
   * {@link #showTemporaryLoadingBar()}
   */
  public void hideTemporaryLoadingBar() {
    setEnabled(true);
    try {
      TranslatorTools.enableViews(((Graph2D)document),true);
    } catch (Throwable e) {}
    if (!(getLayout() instanceof BorderLayout)) return;
    Component c = ((BorderLayout)getLayout()).getLayoutComponent(BorderLayout.SOUTH);
    if (c==null) return;
    remove(c);
  }
  
  
  
  
  
  
  private void pathwayDownloadComplete(String localFile, Format outputFormat) {
    //String localFile=null;

    if (localFile!=null) {
      log.info("Pathway download successful.");
      // Perform translation
      this.inputFile = new File(localFile);
      this.outputFormat = outputFormat;
      removeAll();
      repaint();
      
      translate();
      GUITools.packParentWindow(this);
    } else {
      log.warning("Pathway download failed.");
      // Remove the tab
      this.getParent().remove(this);
      
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
        log.info("Pathway translation complete.");
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
						add(new SBMLModelSplitPane(doc, SBPreferences.getPreferencesFor(
							LaTeXOptions.class).getBoolean(
							LaTeXOptions.PRINT_NAMES_IF_AVAILABLE)));
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
          EditMode editMode = new RestrictedEditMode(translationListener, thiss);
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
   * @return the input file of this panel.
   */
  public File getInputFile() {
    return inputFile;
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
    JPanel panel = new JPanel(new VerticalLayout());
    panel.setPreferredSize(panelSize);
    panel.setOpaque(false);
    
    // Create the label and progressBar
    loadingText = (loadingText!=null && loadingText.length()>0)?loadingText:"Please wait...";
    JLabel jl = new JLabel(loadingText);
    log.info(loadingText);
    //Font font = new java.awt.Font("Tahoma", Font.PLAIN, 12);
    //jl.setFont(font);
    
    JProgressBar prog = new JProgressBar();
    prog.setPreferredSize(new Dimension(panelSize.width - 20,
      panelSize.height / 4));
    panel.add(jl);//, BorderLayout.NORTH);
    panel.add(prog);//, BorderLayout.CENTER);
    
    if (panel instanceof JComponent) {
      GUITools.setOpaqueForAllElements((JComponent) panel, false);
    }
    
    if (parent!=null) {
      parent.add(panel);
    } else {
      // Display the panel in an jFrame
      JDialog f = new JDialog();
      f.setTitle(Translator.APPLICATION_NAME);
      f.setSize(panel.getPreferredSize());
      f.setContentPane(panel);
      f.setPreferredSize(panel.getPreferredSize());
      f.setLocationRelativeTo(null);
      f.setVisible(true);
      f.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    }
    
    // Make progressBar
    ProgressBarSwing pb = new ProgressBarSwing(prog);
    
    // Inform others of this action
    ActionEvent newBar = new ActionEvent(pb, JOptionPane.DEFAULT_OPTION, "NEW_PROGRESSBAR");
    if (parent instanceof TranslatorPanel) {
      ((TranslatorPanel)parent).fireActionEvent(newBar);
    } else if (parent instanceof TranslatorUI) {
      ((TranslatorUI)parent).actionPerformed(newBar);
    }
    
    return  pb;
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
  
  /**
   * 
   * @return
   */
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
      ff.addAll(getGraphMLfilefilter());
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
    fc.setSelectedFile(inputFile.getPath().contains(".")?new File(inputFile.getPath().substring(0, inputFile.getPath().lastIndexOf('.'))):
      new File(inputFile.getPath()+'.'+defaultFF.getExtension()) );
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

    boolean success = false;
    if (file != null) {
      TranslatorUI.saveDir = file.getParent();
      if (SBFileFilter.isTeXFile(file) || SBFileFilter.isPDFFile(file) || format.equals("tex") || format.equals("pdf")) {
        writeLaTeXReport(file);
        success=true; // Void result... can't check.
      } else if (translator instanceof KEGG2yGraph){
        try {
          success = ((KEGG2yGraph)translator).writeToFile((Graph2D) document, file.getPath(), format);
        } catch (Exception e) {
          GUITools.showErrorMessage(this, e);
          success=false;
        }
      } else if (translator instanceof KEGG2jSBML){
        success = ((KEGG2jSBML)translator).writeToFile((SBMLDocument) document, file.getPath());
      }
    }
    success&=(file.exists()&& (file.length()>0));
    
    // Report success or failure.
    if (success) {
      documentHasBeenSaved = true;
      log.info("Pathway has been saved successfully to '" + file.getName() + "'.");
    } else {
      log.warning("Saving pathway to disk failed.");
    }
    
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
      GUITools.setEnabled(true, menuBar, BaseAction.FILE_SAVE, /*Action.TO_LATEX,*/ BaseAction.FILE_CLOSE);
    } else if (isGraphML()) {
      GUITools.setEnabled(true, menuBar, BaseAction.FILE_SAVE, BaseAction.FILE_CLOSE);
//      GUITools.setEnabled(false, menuBar,Action.TO_LATEX);
    } else {
      // E.g. when translation still in progress, or on download frame
      GUITools.setEnabled(false, menuBar, BaseAction.FILE_SAVE, /*Action.TO_LATEX,*/ BaseAction.FILE_CLOSE);
      
      if (this.inputFile==null) {
        // Download frame or invalid menu item
        GUITools.setEnabled(true, menuBar, BaseAction.FILE_CLOSE);
      }
    }
  }
  
  /**
   * @see #updateButtons(JMenuBar)
   */
  public void updateButtons(JMenuBar menuBar, JToolBar toolbar) {
    updateButtons(menuBar);
    // Toolbar must not be changed!
  }
  
  /**
   * @param targetFile - can be null.
   */
  public void writeLaTeXReport(File targetFile) {
    if (document==null) return;
    if (!isSBML()) {
      GUITools.showMessage("This option is only available for SBML documents.", Translator.APPLICATION_NAME);
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
  
  /**
   * @return the translated document, which is either a {@link Graph2D} object (if {@link #isGraphML()} is true)
   * or an {@link SBMLDocument} if {@link #isSBML()} is true.
   */
  public Object getDocument() {
    return document;
  }
  
  /**
   * @return true if and only if a translated pathway is stored in this class.
   */
  public boolean isReady() {
    return document!=null;
  }

  /**
   * @see #setData(Object)
   * @param key key under which the object has been stored
   * @return the object, stored with {@link #setData(String, Object)}, using
   * the given <code>key</code>.
   */
  public Object getData(String key) {
    if (additionalData==null) return null;
    return additionalData.get(key);
  }

  /**
   * Allows the programmer to store any additional data along with this panel.
   * @param key a key for the object to store
   * @param object the object to set
   */
  public void setData(String key, Object object) {
    if (additionalData==null) additionalData = new HashMap<String, Object>();
    additionalData.put(key, object);
  }
  
  /* (non-Javadoc)
   * @see java.awt.Component#repaint()
   */
  @Override
  public void repaint() {
    super.repaint();
    // Update graph
    if (isGraphML()) {
      ((Graph2D)document).updateViews();
    }
  }
  
  
}
