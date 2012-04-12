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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

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

import de.zbit.gui.BaseFrameTab;
import de.zbit.gui.GUITools;
import de.zbit.gui.BaseFrame.BaseAction;
import de.zbit.gui.layout.VerticalLayout;
import de.zbit.io.FileDownload;
import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.kegg.Translator;
import de.zbit.kegg.io.AbstractKEGGtranslator;
import de.zbit.kegg.io.BatchKEGGtranslator;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.util.objectwrapper.ValuePairUncomparable;
import de.zbit.util.progressbar.AbstractProgressBar;
import de.zbit.util.progressbar.gui.ProgressBarSwing;

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
public abstract class TranslatorPanel <DocumentType> extends JPanel implements BaseFrameTab {
  private static final long serialVersionUID = 6030311193210321410L;
  public static final transient Logger log = Logger.getLogger(TranslatorPanel.class.getName());
  
  /**
   * This is the path where the background-logo will be loaded from. This must be
   * relative to the current path (of this class)!
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
  DocumentType document = null;
  /**
   * An action is fired to this listener, when the translation is done
   * or failed with an error.
   */
  ActionListener translationListener = null;
  /**
   * We need to remember the translator for saving the file later on.
   */
  private AbstractKEGGtranslator<?> translator = null;
  
  /**
   * Allows the programmer to store any additional data along with this panel.
   */
  Map<String, Object> additionalData=null;
  
  /**
   * 
   * @param inputFile
   * @param outputFormat
   * @param translationResult
   */
  public static TranslatorPanel<?> createPanel(final File inputFile, final Format outputFormat, ActionListener translationResult) {
    switch (outputFormat) {
      case SBML: case LaTeX: case SBML_QUAL:
        return new TranslatorSBMLPanel(inputFile, outputFormat, translationResult);
      case GraphML: case GML: case JPG: case GIF: case YGF: case TGF: /* case SVG:*/
        return new TranslatorGraphPanel(inputFile, outputFormat, translationResult);
        
      // Please only insert working items here.
      case SBGN:
        return new TranslatorSBGNPanel(inputFile, translationResult);
        
      case BioPAX_level2: case BioPAX_level3:
        return new TranslatorBioPAXPanel(inputFile, outputFormat, translationResult);
        
      default:
        GUITools.showErrorMessage(null, "Unknwon output Format: '" + outputFormat + "'.");
        return null;
      } 
  }
  
  public static TranslatorPanel<?> createPanel(final String pathwayID, final Format outputFormat, ActionListener translationResult) {
    switch (outputFormat) {
      case SBML: case LaTeX: case SBML_QUAL:
        return new TranslatorSBMLPanel(pathwayID, outputFormat, translationResult);
      case GraphML: case GML: case JPG: case GIF: case YGF: case TGF: /* case SVG:*/
        return new TranslatorGraphPanel(pathwayID, outputFormat, translationResult);
        
      case SBGN:
        return new TranslatorSBGNPanel(pathwayID, translationResult);
        
      case BioPAX_level2: case BioPAX_level3:
        return new TranslatorBioPAXPanel(pathwayID, outputFormat, translationResult);
        
      default:
        GUITools.showErrorMessage(null, "Unknwon output Format: '" + outputFormat + "'.");
        return null;
      } 
  }
  
  
  /**
   * Create a new translator-panel and initiates the translation.
   * @param inputFile
   * @param outputFormat
   * @param translationResult
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
    this.outputFormat = outputFormat;
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
   * Use this constructor if the document has already been translated.
   * This constructor does not call {@link #createTabContent()}.
   * @param inputFile
   * @param outputFormat
   * @param translationResult
   * @param translatedDocument
   * @throws Exception 
   */
  protected TranslatorPanel(final File inputFile, final Format outputFormat, ActionListener translationResult, DocumentType translatedDocument) {
    super();
    setLayout(new BorderLayout());
    setOpaque(false);
    this.inputFile = inputFile;
    this.outputFormat = outputFormat;
    this.translationListener = translationResult;
    this.document = translatedDocument;
    // It is intended that crateTabContent() is NOT CALLED!
  }
  
  
  private void showTemporaryLoadingPanel(String pwName, String organism) {
    // Show progress-bar
    removeAll();
    setLayout(new BorderLayout()); // LayoutHelper creates a GridBaglayout, reset it to default.
    final AbstractProgressBar pb = generateLoadingPanel(this, "Downloading '" + pwName + "'" +
      (organism!=null&&organism.length()>0? " for '"+organism+"'...":"..."));
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
      @SuppressWarnings("unchecked")
      protected void done() {
        removeAll();
        log.info("Pathway translation complete.");
        try {
          // Get the resulting document and check and handle eventual errors.
          document = (DocumentType) get();
          
          // Change the tab to the corresponding content.
          createTabContent();
        } catch (Throwable e) {
          if (!Thread.currentThread().isInterrupted()) {
            // Don't show errors for interrupted threads
            GUITools.showErrorMessage(null, e);
          }
          fireActionEvent(new ActionEvent(thiss,JOptionPane.ERROR,TranslatorUI.Action.TRANSLATION_DONE.toString()));
          return;
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
   * Create and place all {@link Component}s on this panel,
   * based on the current {@link #document}. 
   */
  protected abstract void createTabContent() throws Exception;
  
  
  /**
   * @param actionEvent
   */
  protected void fireActionEvent(ActionEvent actionEvent) {
    if (translationListener!=null) {
      translationListener.actionPerformed(actionEvent);
    }
  }

  /**
   * Returns a string representation of the contained pathway.
   * @return
   */
  public String getTitle() {
    return inputFile.getName();
  }
  
  /**
   * @return the {@link Translator} that has been used to
   * Translate this document.
   */
  public AbstractKEGGtranslator<?> getTranslator() {
    return translator;
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
      f.setTitle(System.getProperty("app.name"));
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
    if (parent instanceof TranslatorPanel<?>) {
      ((TranslatorPanel<?>)parent).fireActionEvent(newBar);
    } else if (parent instanceof TranslatorUI) {
      ((TranslatorUI)parent).actionPerformed(newBar);
    }
    
    return  pb;
  }
  
  /**
   * Create all file filters that are available to save this
   * tabs content. The first in the list is assumed to be
   * the default file filter.
   * @return
   */
  protected abstract List<FileFilter> getOutputFileFilter();
  
  /**
   * 
   * @return
   */
  public File saveToFile() {
    if (!isReady()) return null;
    
    // Create list of available output file filters
    List<FileFilter> ff = getOutputFileFilter();
    if (!(ff instanceof LinkedList<?>)) {
      ff = new LinkedList<FileFilter>(ff);
    }
    
    // Move the selected output format to the top of the list
    for (int i=0; i<ff.size(); i++) {
      Set<String> extensions = ((SBFileFilter)ff.get(i)).getExtensions();
      if (extensions!=null) {
        for (String ext: extensions) {
          if (ext.equalsIgnoreCase((this.outputFormat.toString()))) {
            ((LinkedList<FileFilter>) ff).addFirst(ff.remove(i));
            break;
          }
        }
      }
    }
    
    // Create an output file suggestion
    String outFileSuggestion = inputFile.getPath();
    if (inputFile.getName().contains(".")) outFileSuggestion = inputFile.getPath().substring(0, inputFile.getPath().lastIndexOf('.'));
    // Do not add an extension here! It is added automatically later on.  
        
    // We also need to know the selected file filter!
    //File file = GUITools.saveFileDialog(this, TranslatorUI.saveDir, false, false, true,
      //JFileChooser.FILES_ONLY, ff.toArray(new FileFilter[0]));
    JFileChooser fc = GUITools.createJFileChooser(TranslatorUI.saveDir, false,
      false, JFileChooser.FILES_ONLY, ff.toArray(new FileFilter[0]));
    fc.setSelectedFile(new File(outFileSuggestion));
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
      try {
        success = writeToFileUnchecked(file, format);
      } catch (Exception e) {
        GUITools.showErrorMessage(this, e);
        success = false;
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
   * Invoke the file write. All checks have already been made and also
   * a message of success/ failure is sent by other methods. Simply
   * write to the file in this method here.
   * @param file
   * @param format
   * @return true if everything went ok, false else.
   * @throws Exception
   */
  protected abstract boolean writeToFileUnchecked(File file, String format) throws Exception;
  
  /**
   * @return true, if the document has been saved.
   */
  public boolean isSaved() {
    return (!isReady() || documentHasBeenSaved);
  }


  /**
   * Enabled and disables item in the menu bar, based on the content of this panel.
   * @param menuBar
   */
  public void updateButtons(JMenuBar menuBar) {
    if (isReady()) {
      GUITools.setEnabled(true, menuBar, BaseAction.FILE_SAVE_AS, BaseAction.FILE_CLOSE);
    } else {
      // E.g. when translation still in progress, or on download frame
      GUITools.setEnabled(false, menuBar, BaseAction.FILE_SAVE_AS, /*Action.TO_LATEX,*/ BaseAction.FILE_CLOSE);
      
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
   * @return the translated document, which is either a Graph2D object
   * or an SBMLDocument.
   */
  public DocumentType getDocument() {
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

  /**
   * @return true if this is a GraphML panel (and the {@link #document}
   * is a Graph2D object).
   */
  public boolean isGraphML() {
    return (this instanceof TranslatorGraphPanel);
  }
  
  /**
   * @return true if this is a SBML panel (and the {@link #document}
   * is a SBMLDocument object).
   */
  public boolean isSBML() {
    return (this instanceof TranslatorSBMLPanel);
  }
  
}
