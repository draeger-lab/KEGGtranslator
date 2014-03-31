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

import static de.zbit.util.Utils.getMessage;

import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.EventHandler;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import de.zbit.AppConf;
import de.zbit.garuda.GarudaActions;
import de.zbit.garuda.GarudaFileSender;
import de.zbit.garuda.GarudaGUIfactory;
import de.zbit.garuda.GarudaOptions;
import de.zbit.garuda.GarudaSoftwareBackend;
import de.zbit.graph.RestrictedEditMode;
import de.zbit.graph.gui.TranslatorGraphLayerPanel;
import de.zbit.graph.gui.TranslatorPanel;
import de.zbit.gui.BaseFrame;
import de.zbit.gui.GUIOptions;
import de.zbit.gui.GUITools;
import de.zbit.gui.JLabeledComponent;
import de.zbit.gui.JTabbedPaneDraggableAndCloseable;
import de.zbit.gui.actioncommand.ActionCommand;
import de.zbit.gui.prefs.FileSelector;
import de.zbit.gui.prefs.PreferencesPanel;
import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.kegg.KEGGtranslatorOptions;
import de.zbit.kegg.Translator;
import de.zbit.kegg.ext.KEGGTranslatorPanelOptions;
import de.zbit.kegg.io.KEGG2jSBML;
import de.zbit.kegg.io.KEGGtranslatorIOOptions;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.util.StringUtil;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.util.prefs.SBProperties;
import de.zbit.util.progressbar.AbstractProgressBar;

/**
 * The main UI of KEGGtranslator.
 * 
 * @author Andreas Dr&auml;ger
 * @author Clemens Wrzodek
 * @date 2010-11-12
 * @since 1.0
 * @version $Rev$
 */
public class TranslatorUI extends BaseFrame implements ActionListener,
KeyListener, ItemListener {
  
  /**
   * Generated serial version identifier.
   */
  private static final long serialVersionUID = 6631262606716052915L;
  
  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(TranslatorUI.class.getName());
  
  /**
   * This is the path (relative to THIS CLASS PATH) where the watermark
   * logo for this application resides.
   */
  public static String watermarkLogoResource = "img/Logo2.png";
  
  /**
   * A reference to the Garuda core.
   */
  private GarudaSoftwareBackend garudaBackend;
  
  /**
   * @author Clemens Wrzodek
   * @author Andreas Dr&auml;ger
   * @date 2010-11-12
   */
  public static enum Action implements ActionCommand {
    //		/**
    //		 * {@link Action} for LaTeX export.
    //		 */
    //		TO_LATEX,
    /**
     * {@link Action} for new instances of {@link AbstractProgressBar}s.
     * This will display the progress in the {@link #statusBar}.
     */
    NEW_PROGRESSBAR,
    /**
     * {@link Action} for downloading KGMLs.
     */
    DOWNLOAD_KGML,
    /**
     * This is coming from {@link RestrictedEditMode#OPEN_PATHWAY} and must be
     * renamed accordingly. The source is a kegg pathway id that should be opened
     * as new tab, when this action is fired. This is an invisible action.
     */
    OPEN_PATHWAY,
    /**
     * Invisible {@link Action} that should be performed, whenever an
     * translation is done.
     */
    TRANSLATION_DONE;
    /**
     * Invisible {@link Action} that should be performed, whenever a file
     * has been dropped on this panel.
     */
    //FILE_DROPPED
    
    /*
     * (non-Javadoc)
     * 
     * @see de.zbit.gui.ActionCommand#getName()
     */
    @Override
    public String getName() {
      switch (this) {
        //			case TO_LATEX:
        //				return "Export to LaTeX";
        case DOWNLOAD_KGML:
          return "Download KGML";
          
        default:
          return StringUtil.firstLetterUpperCase(toString().toLowerCase()
            .replace('_', ' '));
      }
    }
    
    /* (non-Javadoc)
     * @see de.zbit.gui.ActionCommand#getToolTip()
     */
    @Override
    public String getToolTip() {
      switch (this) {
        //			case TO_LATEX:
        //				return "Converts the currently opened model to a LaTeX report file.";
        case DOWNLOAD_KGML:
          return "Downloads KGML-formatted XML pathways from KEGG server.";
        default:
          return "";
      }
    }
  }
  
  static {
    String iconPaths[] = {
        "KEGGtranslatorIcon_16.png",
        "KEGGtranslatorIcon_32.png",
        "KEGGtranslatorIcon_48.png",
        "KEGGtranslatorIcon_64.png",
        "KEGGtranslatorIcon_128.png",
        "KEGGtranslatorIcon_256.png"
    };
    for (String path : iconPaths) {
      URL url = TranslatorUI.class.getResource("img/" + path);
      if (url!=null) {
        UIManager.put(path.substring(0, path.lastIndexOf('.')), new ImageIcon(url));
      }
    }
    try {
      org.sbml.tolatex.gui.LaTeXExportDialog.initImages();
    } catch (Throwable t) {
      // Also allow KEGGtranslator to compile without
      // SBML2LaTeX !
    }
  }
  
  /**
   * Default directory path's for saving and opening files. Only init them
   * once. Other classes should use these variables.
   */
  public static String openDir, saveDir;
  /**
   * This is where we place all the converted models.
   */
  private JTabbedPane tabbedPane;
  /**
   * preferences is holding all project specific preferences
   */
  private SBPreferences prefsIO;
  
  /**
   * 
   */
  public TranslatorUI() {
    this(null);
  }
  
  public TranslatorUI(AppConf appConf) {
    super(appConf);
    
    // init preferences
    initPreferences();
    File file = new File(prefsIO.get(KEGGtranslatorIOOptions.INPUT));
    openDir = file.isDirectory() ? file.getAbsolutePath() : file.getParent();
    file = new File(prefsIO.get(KEGGtranslatorIOOptions.OUTPUT));
    saveDir = file.isDirectory() ? file.getAbsolutePath() : file.getParent();
    
    // Depending on the current OS, we should add the following image
    // icons: 16x16, 32x32, 48x48, 128x128 (MAC), 256x256 (Vista).
    int[] resolutions=new int[]{16,32,48,128,256};
    List<Image> icons = new LinkedList<Image>();
    for (int res: resolutions) {
      Object icon = UIManager.get("KEGGtranslatorIcon_" + res);
      if ((icon != null) && (icon instanceof ImageIcon)) {
        icons.add(((ImageIcon) icon).getImage());
      }
    }
    setIconImages(icons);
    
  }
  
  /**
   * Init preferences, if not already done.
   */
  private void initPreferences() {
    if (prefsIO == null) {
      prefsIO = SBPreferences.getPreferencesFor(KEGGtranslatorIOOptions.class);
    }
    TranslatorGraphLayerPanel.optionClass = KEGGTranslatorPanelOptions.class;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#createJToolBar()
   */
  @Override
  protected JToolBar createJToolBar() {
    initPreferences();
    // final JPanel r = new JPanel(new VerticalLayout());
    final JToolBar r = new JToolBar("Translate new document", SwingConstants.HORIZONTAL);
    
    // Add "Download KGML" / "pathway from KEGG" button
    JButton download = GUITools.createJButton(this, Action.DOWNLOAD_KGML, UIManager.getIcon("ICON_GEAR_16"), 'D');
    r.add(download);
    r.addSeparator();
    
    // Add "Translate file" stuff
    JComponent jc = PreferencesPanel.createJComponentForOption(KEGGtranslatorIOOptions.INPUT, prefsIO, this);
    // Allow a change of Focus (important!)
    if (jc instanceof FileSelector) {
      ((FileSelector)jc).removeInputVerifier();
    }
    r.add(jc);
    r.add(PreferencesPanel.createJComponentForOption(KEGGtranslatorIOOptions.FORMAT, prefsIO, this));
    
    // Button and action
    JButton ok = new JButton("Translate now!", UIManager.getIcon("ICON_GEAR_16"));
    ok.setToolTipText(StringUtil
      .toHTMLToolTip("Starts the conversion of the input file to the selected output format and displays the result on this workbench."));
    ok.addActionListener(new ActionListener() {
      /* (non-Javadoc)
       * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
       */
      @Override
      public void actionPerformed(ActionEvent e) {
        // Get selected file and format
        File inFile = getInputFile(r);
        String format = getOutputFileFormat(r);
        
        // Check if it is conform with current settings
        if (!checkSettingsAndIssueWarning(format)) {
          return;
        }
        
        // Translate
        createNewTab(inFile, format);
        
        // Add to histoy
        addToFileHistory(Collections.singleton(inFile));
      }
    });
    r.add(ok);
    
    GUITools.setOpaqueForAllElements(r, false);
    return r;
  }
  
  /**
   * Checks if the current application preferences are conform
   * with the currently selected format and eventually
   * issues a warning.
   * @param format
   * @return <code>FALSE</code> if the translation should be
   * stopped.
   */
  protected boolean checkSettingsAndIssueWarning(String format) {
    
    Format f = Format.valueOf(format);
    if (f == null) {
      GUITools.showErrorMessage(this, "Unknown output format: " + format);
      return false;
    } else if (f == Format.SBML_L2V4) {
      // Check if Level 2 and extensions are selected.
      SBPreferences prefs = SBPreferences.getPreferencesFor(KEGGtranslatorOptions.class);
      if (KEGGtranslatorOptions.ADD_LAYOUT_EXTENSION.getValue(prefs) ||
          KEGGtranslatorOptions.USE_GROUPS_EXTENSION.getValue(prefs)) {
        String message = "SBML supports extensions since Level 3. You've chosen to translate a document to Level 2 including the layout or groups extension, what is not possible.\nDo you want to deactivate the extensions for this translation?";
        int ret = GUITools.showQuestionMessage(this, message, "Conflict between selected Level and extension support", JOptionPane.OK_CANCEL_OPTION);
        if (ret == JOptionPane.CANCEL_OPTION) {
          return false;
        }
      }
    }
    
    return true;
  }
  
  /**
   * Searches for any JComponent with
   * {@link KEGGtranslatorIOOptions#FORMAT}.getOptionName() on it and returns the selected
   * format. Use it e.g. with {@link #translateToolBar}.
   * 
   * @param r
   * @return String - format.
   */
  private String getOutputFileFormat(JComponent r) {
    String format = null;
    for (Component c : r.getComponents()) {
      if (c.getName() == null) {
        continue;
      } else if (c.getName().equals(
        KEGGtranslatorIOOptions.FORMAT.getOptionName())
        && (JLabeledComponent.class.isAssignableFrom(c.getClass()))) {
        format = ((JLabeledComponent) c).getSelectedItem().toString();
        break;
      }
    }
    return format;
  }
  
  /**
   * Searches for any JComponent with
   * "TranslatorOptions.INPUT.getOptionName()" on it and returns the selected
   * file. Use it e.g. with {@link #translateToolBar}.
   * 
   * @param r
   * @return File - input file.
   */
  private File getInputFile(JComponent r) {
    File inFile = null;
    for (Component c : r.getComponents()) {
      if (c.getName() == null) {
        continue;
      } else if (c.getName().equals(
        KEGGtranslatorIOOptions.INPUT.getOptionName())
        && (FileSelector.class.isAssignableFrom(c.getClass()))) {
        try {
          inFile = ((FileSelector) c).getSelectedFile();
        } catch (IOException e1) {
          GUITools.showErrorMessage(r, e1);
          e1.printStackTrace();
        }
      }
    }
    return inFile;
  }
  
  /**
   * A method to set the value of a currently displayed {@link FileSelector}
   * corresponding to the <code>KEGGtranslatorIOOptions.INPUT</code> option.
   * @param r the JComponent on which the component for the mentioned
   * optioned is placed.
   * @param file the file to set
   */
  private void setInputFile(JComponent r, File file) {
    for (Component c : r.getComponents()) {
      if (c.getName() == null) {
        continue;
      } else if (c.getName().equals(
        KEGGtranslatorIOOptions.INPUT.getOptionName())
        && (FileSelector.class.isAssignableFrom(c.getClass()))) {
        ((FileSelector) c).setSelectedFile(file);
      }
    }
  }
  
  /**
   * Translate and create a new tab.
   * 
   * @param inFile
   * @param format
   */
  private void createNewTab(File inFile, String format) {
    // Check input
    if (!KEGGtranslatorIOOptions.INPUT.getRange().isInRange(inFile)) {
      String message = "The given file is no valid input file.";
      if (inFile!=null) {
        message = '\'' + inFile.getName() + "' is no valid input file.";
      }
      JOptionPane.showMessageDialog(this, message, System.getProperty("app.name"), JOptionPane.WARNING_MESSAGE);
    } else {
      Format f = null;
      try {
        f = Format.valueOf(format);
      } catch (Throwable exc) {
        exc.printStackTrace();
        JOptionPane.showMessageDialog(this, '\'' + format + "' is no valid output format.",
          System.getProperty("app.name"), JOptionPane.WARNING_MESSAGE);
      }
      if (f != null) {
        // Tanslate and add tab.
        try {
          openDir = inFile.getParent();
          addTranslatorTab(TranslatorPanelTools.createPanel(inFile, f, this));
          
        } catch (Exception e1) {
          GUITools.showErrorMessage(this, e1);
        }
      }
    }
  }
  
  /**
   * Adds a new {@link TranslatorPanel} to this {@link #tabbedPane}
   * and changes the selection to this new panel.
   * @param tp
   */
  public void addTranslatorTab(TranslatorPanel<?> tp) {
    addTranslatorTab(null, tp);
  }
  
  /**
   * Adds a new {@link TranslatorPanel} to this {@link #tabbedPane}
   * and changes the selection to this new panel.
   * @param tabName name for the tab
   * @param tp
   */
  public void addTranslatorTab(String tabName, TranslatorPanel<?> tp) {
    try {
      if (tabName == null) {
        tabName = tp.getTitle();
      }
      tabbedPane.addTab(tabName, tp);
      tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    } catch (Exception e1) {
      GUITools.showErrorMessage(this, e1);
    }
  }
  
  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    try {
      Action action = Action.valueOf(e.getActionCommand());
      switch (action) {
        case TRANSLATION_DONE:
          TranslatorPanel<?> source = (TranslatorPanel<?>) e.getSource();
          int index = tabbedPane.indexOfComponent(source);
          if (index>=0) {// ELSE: User closed the tab before completion
            if (e.getID() != JOptionPane.OK_OPTION) {
              // If translation failed, remove the tab. The error
              // message has already been issued by the translator.
              tabbedPane.removeTabAt(index);
            } else {
              // Do not change title here. Initial title is mostly
              // better than this one ;-)
              //tabbedPane.setTitleAt(index, source.getTitle());
            }
          }
          getStatusBar().hideProgress();
          updateButtons();
          break;
          /* Moved to BaseFrame.
			case FILE_DROPPED:
				String format = getOutputFileFormat(toolBar);
				if ((format == null) || (format.length() < 1)) {
					break;
				}
				createNewTab(((File) e.getSource()), format);
				break;
           */
          //			case TO_LATEX:
          //				writeLaTeXReport();
          //				break;
        case DOWNLOAD_KGML:
          TranslatePathwayDialog.showAndEvaluateDialog(tabbedPane, this, (Format) null);
          //        try {
          //          tabbedPane.addTab(action.getName(), new TranslatorPanel(this));
          //          tabbedPane.setSelectedIndex(tabbedPane.getTabCount()-1);
          //        } catch (Exception e1) {
          //          GUITools.showErrorMessage(this, e1);
          //        }
          break;
        case NEW_PROGRESSBAR:
          getStatusBar().showProgress((AbstractProgressBar)e.getSource());
          break;
        case OPEN_PATHWAY:
          try {
            addTranslatorTab(e.getSource().toString(), TranslatorPanelTools.createPanel(e.getSource().toString(),Format.GraphML,this));
          } catch (Exception e1) {
            GUITools.showErrorMessage(this, e1);
          }
          break;
        default:
          System.out.println(action);
          break;
      }
    } catch (Throwable exc) {
      GUITools.showErrorMessage(this, exc);
    }
  }
  
  //	/**
  //	 * @param object
  //	 */
  //	private void writeLaTeXReport() {
  //		TranslatorPanel o = getCurrentlySelectedPanel();
  //		if (o != null) {
  //			o.writeLaTeXReport(null);
  //		}
  //	}
  
  /**
   * Closes the tab at the specified index.
   * 
   * @param index
   * @return {@code true} if the tab has been closed.
   */
  private boolean closeTab(int index) {
    if ((index < 0) || (index >= tabbedPane.getTabCount())) {
      return false;
    }
    Component comp = tabbedPane.getComponentAt(index);
    String title = tabbedPane.getTitleAt(index);
    if ((title == null) || (title.length() < 1)) {
      title = "the currently selected document";
    }
    
    // Check if document already has been saved
    if ((comp instanceof TranslatorPanel<?>)
        && !((TranslatorPanel<?>) comp).isSaved()) {
      if ((JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(this,
        StringUtil.toHTMLToolTip(
          "Do you really want to close %s%s%s without saving?",
          KEGG2jSBML.quotStart, title, KEGG2jSBML.quotEnd),
          "Close selected document",
          JOptionPane.YES_NO_OPTION))) {
        return false;
      }
    }
    
    // Close the document.
    tabbedPane.removeTabAt(index);
    updateButtons();
    if ((garudaBackend != null) && (tabbedPane.getTabCount() == 0)) {
      GUITools.setEnabled(false, getJMenuBar(), getJToolBar(), GarudaActions.SENT_TO_GARUDA);
    }
    return true;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#openFile(java.io.File[])
   */
  @Override
  public File[] openFile(File... files) {
    boolean askOutputFormat = false;
    
    // Ask input file
    if ((files == null) || (files.length < 1)) {
      files = GUITools.openFileDialog(this, openDir, false, true,
        JFileChooser.FILES_ONLY, SBFileFilter.createKGMLFileFilter());
      
      askOutputFormat=true;
    }
    if ((files == null) || (files.length < 1)) {
      return files;
    } else {
      // Set value to box if it does not yet contain a valid value
      if (getInputFile(toolBar)==null) {
        setInputFile(toolBar, files[0]);
      }
    }
    
    // Ask output format
    String format = getOutputFileFormat(toolBar);
    if ( askOutputFormat || (format == null) || (format.length() < 1)) {
      JLabeledComponent outputFormat = (JLabeledComponent) PreferencesPanel.createJComponentForOption(KEGGtranslatorIOOptions.FORMAT, prefsIO, null);
      outputFormat.setTitle("Please select the output format");
      JOptionPane.showMessageDialog(this, outputFormat, System
        .getProperty("app.name"), JOptionPane.QUESTION_MESSAGE);
      format =  outputFormat.getSelectedItem().toString();
    }
    
    
    // Translate
    for (File f : files) {
      createNewTab(f, format);
    }
    return files;
  }
  
  /**
   * Enables and disables buttons in the menu, depending on the current tabbed
   * pane content.
   */
  private void updateButtons() {
    GUITools.setEnabled(false, getJMenuBar(), BaseAction.FILE_SAVE_AS,
      //Action.TO_LATEX,
      BaseAction.FILE_CLOSE);
    TranslatorPanel<?> o = getCurrentlySelectedPanel();
    if (o != null) {
      o.updateButtons(getJMenuBar());
      if ((garudaBackend != null) && (tabbedPane.getTabCount() == 1)) {
        // TabCount check is needed to avoid that this is done again when more tabs are added.
        GUITools.setEnabled(true, getJMenuBar(), getJToolBar(), GarudaActions.SENT_TO_GARUDA);
      }
    }
  }
  
  /**
   * @return the currently selected TranslatorPanel from the
   *         {@link #tabbedPane}, or null if either no or no valid selection
   *         exists.
   */
  private TranslatorPanel<?> getCurrentlySelectedPanel() {
    if ((tabbedPane == null) || (tabbedPane.getSelectedIndex() < 0)) {
      return null;
    }
    Object o = tabbedPane.getSelectedComponent();
    if ((o == null) || !(o instanceof TranslatorPanel<?>)) {
      return null;
    }
    return ((TranslatorPanel<?>) o);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#saveFileAs()
   */
  @Override
  public File saveFileAs() {
    TranslatorPanel<?> o = getCurrentlySelectedPanel();
    if (o != null) {
      return o.saveToFile();
    }
    return null;
  }
  
  /* (non-Javadoc)
   * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
   */
  @Override
  public void keyPressed(KeyEvent e) {
    // Preferences for the "input file"
    PreferencesPanel.setProperty(prefsIO, e.getSource(), true);
  }
  
  /* (non-Javadoc)
   * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
   */
  @Override
  public void keyReleased(KeyEvent e) {
    // Preferences for the "input file"
    PreferencesPanel.setProperty(prefsIO, e.getSource(), true);
  }
  
  /* (non-Javadoc)
   * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
   */
  @Override
  public void keyTyped(KeyEvent e) {
    // Preferences for the "input file"
    PreferencesPanel.setProperty(prefsIO, e.getSource(), true);
  }
  
  /* (non-Javadoc)
   * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
   */
  @Override
  public void itemStateChanged(ItemEvent e) {
    // Preferences for the "output format"
    PreferencesPanel.setProperty(prefsIO, e.getSource(), true);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#additionalFileMenuItems()
   */
  @Override
  protected JMenuItem[] additionalFileMenuItems() {
    List<JMenuItem> additionalItems = new ArrayList<JMenuItem>(2);
    if (!appConf.getCmdArgs().containsKey(GarudaOptions.CONNECT_TO_GARUDA)
        || appConf.getCmdArgs().getBoolean(GarudaOptions.CONNECT_TO_GARUDA)) {
      additionalItems.add(GarudaGUIfactory.createGarudaMenu(
        EventHandler.create(ActionListener.class, this, "sendToGaruda")));
    }
    /*
     * SBML2LaTeX
    additionalItems.add(GUITools.createJMenuItem(this,
				Action.TO_LATEX, UIManager.getIcon("ICON_LATEX_16"), KeyStroke
						.getKeyStroke('E', InputEvent.CTRL_DOWN_MASK), 'E', false));
     */
    additionalItems.add(GUITools.createJMenuItem(this,
      Action.DOWNLOAD_KGML, UIManager.getIcon("ICON_GEAR_16"), KeyStroke
      .getKeyStroke('D', InputEvent.CTRL_DOWN_MASK), 'D', true));
    
    return additionalItems.toArray(new JMenuItem[0]);
  }
  
  /**
   * 
   */
  public void sendToGaruda() {
    final TranslatorPanel<?> o = getCurrentlySelectedPanel();
    if (o != null) {
      List<FileFilter> formatList = o.getOutputFileFilter();
      if ((formatList == null) || (formatList.size() < 1)) {
        return;
      }
      FileFilter selectedFilter = formatList.get(0);
      if (formatList.size() > 1) {
        selectedFilter = (FileFilter) JOptionPane.showInputDialog(this, "message", "title", JOptionPane.QUESTION_MESSAGE, null, formatList.toArray(), formatList.get(0));
      }
      if ((selectedFilter != null) && (selectedFilter instanceof SBFileFilter)) {
        final String outputFormat = ((SBFileFilter) selectedFilter).getExtension();
        final Component parent = this;
        logger.fine("Selected file format = " + outputFormat);
        new SwingWorker<Void, Void>() {
          /* (non-Javadoc)
           * @see javax.swing.SwingWorker#doInBackground()
           */
          @Override
          protected Void doInBackground() throws Exception {
            File file = File.createTempFile("kgtrans_temp_data", '.' + outputFormat);
            file.deleteOnExit();
            o.saveToFile(file, outputFormat);
            
            logger.fine("Launching Garuda sender");
            GarudaFileSender sender = new GarudaFileSender(parent, garudaBackend, file, outputFormat.toUpperCase());
            sender.execute();
            return null;
          }
        }.execute();
      }
    }
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#closeFile()
   */
  @Override
  public boolean closeFile() {
    if (tabbedPane.getSelectedIndex() < 0) {
      return false;
    }
    return closeTab(tabbedPane.getSelectedIndex());
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#createMainComponent()
   */
  @Override
  protected Component createMainComponent() {
    // If you encounter an exception here PUT THE RESOURCES FOLDER ON YOUR CLASS PATH!
    ImageIcon logo = new ImageIcon(getWatermarkLogoResource());
    
    // Crop animated loading bar from image.
    //logo.setImage(ImageTools.cropImage(logo.getImage(), 0, 0, logo.getIconWidth(), logo.getIconHeight()-30));
    
    // Create the tabbed pane, with the KeggTranslator logo.
    tabbedPane = new JTabbedPaneDraggableAndCloseable(logo);
    ((JTabbedPaneDraggableAndCloseable) tabbedPane).setShowCloseIcon(false);
    
    // Change active buttons, based on selection.
    tabbedPane.addChangeListener(new ChangeListener() {
      /* (non-Javadoc)
       * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
       */
      @Override
      public void stateChanged(ChangeEvent e) {
        updateButtons();
      }
    });
    return tabbedPane;
  }
  
  /**
   * @return
   */
  public static URL getWatermarkLogoResource() {
    return TranslatorUI.class.getResource(watermarkLogoResource);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#exit()
   */
  @Override
  public void exit() {
    // Close all tab. If user want's to save a tab first, cancel the closing
    // process.
    while (tabbedPane.getTabCount() > 0) {
      if (!closeTab(0)) {
        return;
      }
    }
    
    // Close the app and save caches.
    setVisible(false);
    try {
      Translator.saveCache();
      
      SBProperties props = new SBProperties();
      { // Save KEGGtranslatorIOOptions
        File f = getInputFile(toolBar);
        if (f != null && KEGGtranslatorIOOptions.INPUT.getRange().isInRange(f, props)) {
          props.put(KEGGtranslatorIOOptions.INPUT, f);
        }
        props.put(KEGGtranslatorIOOptions.FORMAT, getOutputFileFormat(toolBar));
        SBPreferences.saveProperties(KEGGtranslatorIOOptions.class, props);
      }
      props.clear();
      
      { // Save GUIOptions
        if (openDir != null && openDir.length() > 1) {
          props.put(GUIOptions.OPEN_DIR, openDir);
        }
        if (saveDir != null && saveDir.length() > 1) {
          props.put(GUIOptions.SAVE_DIR, saveDir);
        }
        if (props.size()>0) {
          SBPreferences.saveProperties(GUIOptions.class, props);
        }
      }
      
    } catch (BackingStoreException exc) {
      Logger.getLogger(getClass().getName()).log(Level.WARNING, getMessage(exc), exc);
      // Unimportant error... don't bother the user here.
      // GUITools.showErrorMessage(this, exc);
    }
    dispose();
    //System.exit(0);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#getURLAboutMessage()
   */
  @Override
  public URL getURLAboutMessage() {
    //return getClass().getResource("../html/about.html");
    // "../" does not work inside a jar.
    return Translator.class.getResource("html/about.html");
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#getURLLicense()
   */
  @Override
  public URL getURLLicense() {
    //return getClass().getResource("../html/license.html");
    // "../" does not work inside a jar.
    return Translator.class.getResource("html/license.html");
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#getURLOnlineHelp()
   */
  @Override
  public URL getURLOnlineHelp() {
    //return getClass().getResource("../html/help.html");
    // "../" does not work inside a jar.
    return Translator.class.getResource("html/help.html");
  }
  
  /* (non-Javadoc)
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    logger.fine(evt.toString());
    String propName = evt.getPropertyName();
    if (propName.equals(GarudaSoftwareBackend.GARUDA_ACTIVATED)) {
      garudaBackend = (GarudaSoftwareBackend) evt.getNewValue();
      if (tabbedPane.getTabCount() > 0) {
        GUITools.setEnabled(true, getJMenuBar(), getJToolBar(), GarudaActions.SENT_TO_GARUDA);
      }
    }
  }
  
}
