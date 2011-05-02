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

import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
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
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.sbml.tolatex.gui.LaTeXExportDialog;

import de.zbit.gui.ActionCommand;
import de.zbit.gui.BaseFrame;
import de.zbit.gui.GUIOptions;
import de.zbit.gui.GUITools;
import de.zbit.gui.JLabeledComponent;
import de.zbit.gui.JTabbedLogoPane;
import de.zbit.gui.prefs.FileHistory;
import de.zbit.gui.prefs.FileSelector;
import de.zbit.gui.prefs.PreferencesPanel;
import de.zbit.kegg.Translator;
import de.zbit.kegg.io.KEGGtranslator;
import de.zbit.kegg.io.KEGGtranslatorIOOptions;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.util.StringUtil;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.util.prefs.SBProperties;

/**
 * @author Andreas Dr&auml;ger
 * @author Clemens Wrzodek
 * @date 2010-11-12
 * @since 1.0
 * @version $Rev$
 */
public class TranslatorUI extends BaseFrame implements ActionListener,
		KeyListener, ItemListener {

	/**
	 * 
	 * @author Andreas Dr&auml;ger
	 * @date 2010-11-12
	 */
	public static enum Action implements ActionCommand {
//		/**
//		 * {@link Action} for LaTeX export.
//		 */
//		TO_LATEX,
    /**
     * {@link Action} for downloading KGMLs.
     */
		DOWNLOAD_KGML,
		/**
		 * Invisible {@link Action} that should be performed, whenever an
		 * translation is done.
		 */
		TRANSLATION_DONE;
		/**
		 * Invisible {@link Action} that should be performed, whenever a file
		 * has been droppen on this panel.
		 */
		//FILE_DROPPED

		/*
		 * (non-Javadoc)
		 * 
		 * @see de.zbit.gui.ActionCommand#getName()
		 */
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

		/*
		 * (non-Javadoc)
		 * 
		 * @see de.zbit.gui.ActionCommand#getToolTip()
		 */
		public String getToolTip() {
			switch (this) {
//			case TO_LATEX:
//				return "Converts the currently opened model to a LaTeX report file.";
			case DOWNLOAD_KGML:
        return "Downloads KGML-formatted XML pathways from KEGG server.";
			default:
				return "Unknown";
			}
		}
	}

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 6631262606716052915L;

	static {    
		String iconPaths[] = {"KEGGtranslatorIcon_16.png","KEGGtranslatorIcon_32.png","KEGGtranslatorIcon_48.png","KEGGtranslatorIcon_128.png","KEGGtranslatorIcon_256.png"};
		for (String path : iconPaths) {
		  URL url = TranslatorUI.class.getResource("img/" + path);
		  if (url!=null) {
			  UIManager.put(path.substring(0, path.lastIndexOf('.')), new ImageIcon(url));
		  }
		}
		LaTeXExportDialog.initImages();
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
		super();
		// init preferences
		initPreferences();
		File file = new File(prefsIO.get(KEGGtranslatorIOOptions.INPUT));
		openDir = file.isDirectory() ? file.getAbsolutePath() : file
				.getParent();
		file = new File(prefsIO.get(KEGGtranslatorIOOptions.OUTPUT));
		saveDir = file.isDirectory() ? file.getAbsolutePath() : file
				.getParent();
		// Depending on the current OS, we should add the following image
		// icons: 16x16, 32x32, 48x48, 128x128 (MAC), 256x256 (Vista).
		int[] resolutions=new int[]{16,32,48,128,256};
		List<Image> icons = new LinkedList<Image>();
		for (int res: resolutions) {
		  Object icon = UIManager.get("KEGGtranslatorIcon_"+res);
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
			prefsIO = SBPreferences
					.getPreferencesFor(KEGGtranslatorIOOptions.class);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#createJToolBar()
	 */
	protected JToolBar createJToolBar() {
		initPreferences();
		// final JPanel r = new JPanel(new VerticalLayout());
		final JToolBar r = new JToolBar("Translate new file",
				JToolBar.HORIZONTAL);

		JComponent jc = PreferencesPanel.getJComponentForOption(KEGGtranslatorIOOptions.INPUT, prefsIO, this);
		// Allow a change of Focus (important!)
		if (jc instanceof FileSelector) ((FileSelector)jc).removeInputVerifier();
		r.add(jc);
		
		// r.add(new JSeparator(JSeparator.VERTICAL));
		r.add(PreferencesPanel.getJComponentForOption(KEGGtranslatorIOOptions.FORMAT,
				prefsIO, this));

		// Button and action
		JButton ok = new JButton("Translate now!", UIManager
				.getIcon("ICON_GEAR_16"));
		ok.setToolTipText(StringUtil.toHTML(
								"Starts the conversion of the input file to the selected output format and displays the result on this workbench.",
								GUITools.TOOLTIP_LINE_LENGTH));
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Get selected file and format
				File inFile = getInputFile(r);
				String format = getOutputFileFormat(r);

				// Translate
				createNewTab(inFile, format);
			}
		});
		r.add(ok);

		GUITools.setOpaqueForAllElements(r, false);
		return r;
	}

	/**
	 * Searches for any JComponent with
	 * "TranslatorOptions.FORMAT.getOptionName()" on it and returns the selected
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
			JOptionPane.showMessageDialog(this, message, KEGGtranslator.APPLICATION_NAME, JOptionPane.WARNING_MESSAGE);
		} else {
			Format f = null;
			try {
				f = Format.valueOf(format);
			} catch (Throwable exc) {
			  exc.printStackTrace();
				JOptionPane.showMessageDialog(this, '\'' + format + "' is no valid output format.",
						KEGGtranslator.APPLICATION_NAME, JOptionPane.WARNING_MESSAGE);
			}
			if (f != null) {
				// Tanslate and add tab.
				try {
					openDir = inFile.getParent();
					tabbedPane.addTab(inFile.getName(), new TranslatorPanel(
							inFile, f, this));
					tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
				} catch (Exception e1) {
					GUITools.showErrorMessage(this, e1);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		try {
			Action action = Action.valueOf(e.getActionCommand());
			switch (action) {
			case TRANSLATION_DONE:
				TranslatorPanel source = (TranslatorPanel) e.getSource();
				if (e.getID() != JOptionPane.OK_OPTION) {
					// If translation failed, remove the tab. The error
					// message has already been issued by the translator.
					tabbedPane.removeTabAt(tabbedPane.indexOfComponent(source));
				} else {
					tabbedPane.setTitleAt(tabbedPane.indexOfComponent(source),
							source.getTitle());
				}
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
        try {
          tabbedPane.addTab(action.getName(), new TranslatorPanel(this));
          tabbedPane.setSelectedIndex(tabbedPane.getTabCount()-1);
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
	 * @return true, if the tab has been closed.
	 */
	private boolean closeTab(int index) {
		if (index<0 || index >= tabbedPane.getTabCount())
			return false;
		Component comp = tabbedPane.getComponentAt(index);
		String title = tabbedPane.getTitleAt(index);
		if (title == null || title.length() < 1) {
			title = "the currently selected document";
		}

		// Check if document already has been saved
		if ((comp instanceof TranslatorPanel)
				&& !((TranslatorPanel) comp).isSaved()) {
			if ((JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(this,
					StringUtil.toHTML(String.format(
							"Do you really want to close %s without saving?",
							title), GUITools.TOOLTIP_LINE_LENGTH), "Close selected document",
					JOptionPane.YES_NO_OPTION))) {
				return false;
			}
		}

		// Close the document.
		tabbedPane.removeTabAt(index);
		updateButtons();
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see de.zbit.gui.BaseFrame#openFile(java.io.File[])
	 */
	public File[] openFile(File... files) {
	  boolean askOutputFormat = false;
	  
		// Ask input file
		if ((files == null) || (files.length < 1)) {
			files = GUITools.openFileDialog(this, openDir, false, true,
				JFileChooser.FILES_ONLY, new FileFilterKGML());
			
			askOutputFormat=true;
		}
		if ((files == null) || (files.length < 1)) {
			return files;
		}
		
    // Ask output format
    String format = getOutputFileFormat(toolBar);
    if ( askOutputFormat || (format == null) || (format.length() < 1)) {
      JLabeledComponent outputFormat = (JLabeledComponent) PreferencesPanel.getJComponentForOption(KEGGtranslatorIOOptions.FORMAT, prefsIO, null);
      outputFormat.setTitle("Please select the output format");
      JOptionPane.showMessageDialog(this, outputFormat, KEGGtranslator.APPLICATION_NAME, JOptionPane.QUESTION_MESSAGE);
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
		GUITools.setEnabled(false, getJMenuBar(), BaseAction.FILE_SAVE,
				//Action.TO_LATEX,
				BaseAction.FILE_CLOSE);
		TranslatorPanel o = getCurrentlySelectedPanel();
		if (o != null) {
			o.updateButtons(getJMenuBar());
		}
	}

	/**
	 * @return the currently selected TranslatorPanel from the
	 *         {@link #tabbedPane}, or null if either no or no valid selection
	 *         exists.
	 */
	private TranslatorPanel getCurrentlySelectedPanel() {
		if ((tabbedPane == null) || (tabbedPane.getSelectedIndex() < 0)) {
			return null;
		}
		Object o = ((JTabbedPane) tabbedPane).getSelectedComponent();
		if ((o == null) || !(o instanceof TranslatorPanel)) {
			return null;
		}
		return ((TranslatorPanel) o);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#saveFile()
	 */
	public void saveFile() {
		TranslatorPanel o = getCurrentlySelectedPanel();
		if (o != null) {
			o.saveToFile();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	public void keyPressed(KeyEvent e) {
		// Preferences for the "input file"
		PreferencesPanel.setProperty(prefsIO, e.getSource(), true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
	 */
	public void keyReleased(KeyEvent e) {
		// Preferences for the "input file"
		PreferencesPanel.setProperty(prefsIO, e.getSource(), true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	public void keyTyped(KeyEvent e) {
		// Preferences for the "input file"
		PreferencesPanel.setProperty(prefsIO, e.getSource(), true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent e) {
		// Preferences for the "output format"
		PreferencesPanel.setProperty(prefsIO, e.getSource(), true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#additionalFileMenuItems()
	 */
	@Override
	protected JMenuItem[] additionalFileMenuItems() {
		return new JMenuItem[] {
		    /*GUITools.createJMenuItem(this,
				Action.TO_LATEX, UIManager.getIcon("ICON_LATEX_16"), KeyStroke
						.getKeyStroke('E', InputEvent.CTRL_DOWN_MASK), 'E',
				false),*/
				GUITools.createJMenuItem(this,
	        Action.DOWNLOAD_KGML, UIManager.getIcon("ICON_GEAR_16"), KeyStroke
	            .getKeyStroke('D', InputEvent.CTRL_DOWN_MASK), 'D',
	        true)
	        };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#closeFile()
	 */
	public boolean closeFile() {
		if (tabbedPane.getSelectedIndex() < 0) {
			return false;
		}
		return closeTab(tabbedPane.getSelectedIndex());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#createMainComponent()
	 */
	protected Component createMainComponent() {
	  // If you encounter an exception here PUT THE RESOURCES FOLDER ON YOUR CLASS PATH!
	  ImageIcon logo = new ImageIcon(TranslatorUI.class.getResource("img/Logo2.png"));
	  
	  // Crop animated loading bar from image.
	  //logo.setImage(ImageTools.cropImage(logo.getImage(), 0, 0, logo.getIconWidth(), logo.getIconHeight()-30));
	  
	  // Create the tabbed pane, with the KeggTranslator logo.
		tabbedPane = new JTabbedLogoPane(logo);
		
		// Change active buttons, based on selection.
		tabbedPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				updateButtons();
			}
		});
		return tabbedPane;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#exit()
	 */
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
		  File f = getInputFile(toolBar);
		  if (f != null && KEGGtranslatorIOOptions.INPUT.getRange().isInRange(f)) {
		    props.put(KEGGtranslatorIOOptions.INPUT, f);
		  }
		  props.put(KEGGtranslatorIOOptions.FORMAT, getOutputFileFormat(toolBar));
		  SBPreferences.saveProperties(KEGGtranslatorIOOptions.class, props);
		  
			props.clear();
			props.put(GUIOptions.OPEN_DIR, openDir);
			if (saveDir != null && saveDir.length() > 1) {
				props.put(GUIOptions.SAVE_DIR, saveDir);
			}
		  SBPreferences.saveProperties(GUIOptions.class, props);
		  
		} catch (BackingStoreException exc) {
		  exc.printStackTrace();
		  // Unimportant error... don't bother the user here.
			// GUITools.showErrorMessage(this, exc);
		}
		dispose();
		System.exit(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#getCommandLineOptions()
	 */
	@SuppressWarnings("unchecked")
	public Class<? extends KeyProvider>[] getCommandLineOptions() {
		return Translator.getCommandLineOptions().toArray(new Class[0]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#getURLAboutMessage()
	 */
	public URL getURLAboutMessage() {
		//return getClass().getResource("../html/about.html");
	  // "../" does not work inside a jar.
		return Translator.class.getResource("html/about.html");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#getURLLicense()
	 */
	public URL getURLLicense() {
		//return getClass().getResource("../html/license.html");
   	// "../" does not work inside a jar.
		return Translator.class.getResource("html/license.html");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#getURLOnlineHelp()
	 */
	public URL getURLOnlineHelp() {
		//return getClass().getResource("../html/help.html");
  	// "../" does not work inside a jar.
	  return Translator.class.getResource("html/help.html");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#getApplicationName()
	 */
	public String getApplicationName() {
		return KEGGtranslator.APPLICATION_NAME;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#getDottedVersionNumber()
	 */
	public String getDottedVersionNumber() {
		return KEGGtranslator.VERSION_NUMBER;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#getURLOnlineUpdate()
	 */
	public URL getURLOnlineUpdate() {
		try {
			return Translator.getURLOnlineUpdate();
		} catch (MalformedURLException exc) {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see de.zbit.gui.BaseFrame#getFileHistoryKeyProvider()
	 */
	@Override
	public Class<? extends FileHistory> getFileHistoryKeyProvider() {
		return KEGGtranslatorHistory.class;
	}

	/* (non-Javadoc)
	 * @see de.zbit.gui.BaseFrame#getMaximalFileHistorySize()
	 */
	public short getMaximalFileHistorySize() {
		return 10;
	}
}
