/**
 * 
 */
package de.zbit.kegg.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.prefs.BackingStoreException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.tolatex.SBML2LaTeX;
import org.sbml.tolatex.gui.LaTeXExportDialog;
import org.sbml.tolatex.io.LaTeXOptionsIO;

import y.view.Graph2D;
import y.view.Graph2DView;
import de.zbit.gui.ActionCommand;
import de.zbit.gui.GUIOptions;
import de.zbit.gui.GUITools;
import de.zbit.gui.ImageTools;
import de.zbit.gui.JBrowserPane;
import de.zbit.gui.JColumnChooser;
import de.zbit.gui.JHelpBrowser;
import de.zbit.gui.SystemBrowser;
import de.zbit.gui.VerticalLayout;
import de.zbit.gui.prefs.FileSelector;
import de.zbit.gui.prefs.PreferencesDialog;
import de.zbit.gui.prefs.PreferencesPanel;
import de.zbit.io.SBFileFilter;
import de.zbit.kegg.Translator;
import de.zbit.kegg.TranslatorOptions;
import de.zbit.kegg.io.AbstractKEGGtranslator;
import de.zbit.kegg.io.BatchKEGGtranslator;
import de.zbit.kegg.io.KEGGtranslator;
import de.zbit.util.StringUtil;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.util.prefs.SBProperties;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-11-12
 */
public class TranslatorUI extends JFrame implements ActionListener,
		WindowListener {
	
	/**
	 * 
	 * @author Andreas Dr&auml;ger
	 * @date 2010-11-12
	 */
	public static enum Action implements ActionCommand {
		/**
		 * {@link Action} that closes the program.
		 */
		EXIT,
		/**
		 * {@link Action} that show the online help.
		 */
		HELP,
		/**
		 * This {@link Action} shows the people in charge for this program.
		 */
		HELP_ABOUT,
		/**
		 * {@link Action} that displays the license of this program.
		 */
		HELP_LICENSE,
		/**
		 * {@link Action} to open a file.
		 */
		OPEN_FILE,
		/**
		 * {@link Action} to close a model that has been added to the
		 * {@link JTabbedPane}.
		 */
		CLOSE_MODEL,
		/**
		 * {@link Action} to configure the user's preferences.
		 */
		PREFERENCES,
		/**
		 * {@link Action} to save the conversion result to a file.
		 */
		SAVE_FILE,
		/**
		 * {@link Action} for LaTeX export.
		 */
		TO_LATEX;
		
		/*
		 * (non-Javadoc)
		 * 
		 * @see de.zbit.gui.ActionCommand#getName()
		 */
		public String getName() {
			switch (this) {
				case OPEN_FILE:
					return "Open";
				case CLOSE_MODEL:
					return "Close";
				case SAVE_FILE:
					return "Save";
				case TO_LATEX:
					return "Export to LaTeX";
				case PREFERENCES:
					return "Preferences";
				case EXIT:
					return "Exit";
				case HELP:
					return "Online Help";
				case HELP_ABOUT:
					return "About";
				case HELP_LICENSE:
					return "License";
				default:
					return "Unknown";
			}
		}
		
		/*
		 * (non-Javadoc)
		 * 
		 * @see de.zbit.gui.ActionCommand#getToolTip()
		 */
		public String getToolTip() {
			switch (this) {
				case OPEN_FILE:
					return "Opens a new KEGG file.";
				case CLOSE_MODEL:
					return "Closes the currently opened model.";
				case SAVE_FILE:
					return "Saves the currently opened model in one of the available formats.";
				case TO_LATEX:
					return "Converts the currently opened model to a LaTeX report file.";
				case PREFERENCES:
					return "Opens a dialog to configure all options for this program.";
				case EXIT:
					return "Closes this program.";
				case HELP:
					return "Displays the online help";
				case HELP_ABOUT:
					return "This shows who to contact if you encounter any problems with this program.";
				case HELP_LICENSE:
					return "Here you can see the license terms unter which this program is distributed.";
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
		ImageTools.initImages(LaTeXExportDialog.class.getResource("img"));
		ImageTools.initImages(TranslatorUI.class.getResource("img"));
		GUITools.initLaF("KEGGtranslator");
	}
	
	/**
	 * Default directory path's for saving and opening files.
	 */
	private String openDir, saveDir;
	/**
	 * This is where we place all the converted models.
	 */
	private JTabbedPane tabbedPane;
	
	/**
	 * 
	 */
	public TranslatorUI() {
		super("KEGGtranslator");
		
		// init preferences
		SBPreferences prefs = SBPreferences
				.getPreferencesFor(TranslatorOptions.class);
		File file = new File(prefs.get(TranslatorOptions.INPUT));
		this.openDir = file.isDirectory() ? file.getAbsolutePath() : file
				.getParent();
		file = new File(prefs.get(TranslatorOptions.OUTPUT));
		this.saveDir = file.isDirectory() ? file.getAbsolutePath() : file
				.getParent();
		
		// init GUI
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setJMenuBar(generateJMenuBar());
		
		Container container = getContentPane();
		container.setLayout(new BorderLayout());
		tabbedPane = new JTabbedPane();
		tabbedPane.add(getDefaultPanel());
		container.add(tabbedPane, BorderLayout.CENTER);
		
		pack();
		setMinimumSize(new Dimension(640, 480));
		setLocationRelativeTo(null);
	}
	
	/**
   * @return a simple panel that let's the user choose an input
   * file and ouput format.
   */
  private Component getDefaultPanel() {
    final JPanel r = new JPanel(new VerticalLayout());
    
    r.add(PreferencesPanel.getJComponentForOption(TranslatorOptions.INPUT));
    r.add(PreferencesPanel.getJComponentForOption(TranslatorOptions.FORMAT));
    
    JButton ok = new JButton("Translate");
    ok.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        File inFile = null; String format = null;
        
        // Get selected file and format
        for (Component c: r.getComponents()) {
          if (c.getName()==null) {
            continue;
          } else if (c.getName().equals(TranslatorOptions.INPUT.getOptionName()) &&
              (FileSelector.class.isAssignableFrom(c.getClass()))) {
            
            try {
              inFile = ((FileSelector)c).getSelectedFile();
            } catch (IOException e1) {
              GUITools.showErrorMessage(r, e1);
            }
          } else if (c.getName().equals(TranslatorOptions.FORMAT.getOptionName()) &&
              (JColumnChooser.class.isAssignableFrom(c.getClass()))) {
            format = ((JColumnChooser)c).getSelectedItem().toString();
          }
          if (inFile!=null && format!=null) break;
        }
        
        // Check input
        if (!TranslatorOptions.INPUT.getRange().isInRange(inFile)) {
          JOptionPane.showMessageDialog(r, "Please select a valid input file.", KEGGtranslator.appName, JOptionPane.WARNING_MESSAGE);
        } else if (!TranslatorOptions.FORMAT.getRange().isInRange(format)) {
          JOptionPane.showMessageDialog(r, "Please select a valid output format.", KEGGtranslator.appName, JOptionPane.WARNING_MESSAGE);
        } else {
          // Tanslate and add tab.
          try {
            // TODO: Move to new thread and add progress bar.
            translateAndAddTab(inFile, format);
          } catch (Exception e1) {
            GUITools.showErrorMessage(r, e1);
          }
        }
      }
    });
    r.add(ok);
    
    GUITools.setOpaqueForAllElements(r, false);
    return r;
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
			case EXIT:
				System.exit(0);
			case OPEN_FILE:
				openFile();
				break;
			case CLOSE_MODEL:
				closeModel();
				break;
			case PREFERENCES:
				PreferencesDialog.showPreferencesDialog();
				break;
			case SAVE_FILE:
				saveFile();
				break;
			case TO_LATEX:
				writeLaTeXReport(null);
				break;
			case HELP:
				GUITools.setEnabled(false, getJMenuBar(), Action.HELP);
				JHelpBrowser.showOnlineHelp(this, this,
						"KEGGtranslator - Online Help", getClass().getResource(
								"../html/help.html"));
				break;
			case HELP_ABOUT:
				JOptionPane.showMessageDialog(this, createJBrowser(
						"../html/about.html", 380, 220, false), "About",
						JOptionPane.INFORMATION_MESSAGE);
				break;
			case HELP_LICENSE:
				JOptionPane.showMessageDialog(this, createJBrowser(
						"../html/license.html", 640, 480, true), "License",
						JOptionPane.INFORMATION_MESSAGE,
						UIManager.getIcon("ICON_LICENSE_64"));
				break;
			default:
				System.out.println(action);
				break;
			}
		} catch (Throwable exc) {
			GUITools.showErrorMessage(this, exc);
		}
	}
	
	/**
	 * 
	 * @param url
	 * @param preferedWidth
	 * @param preferedHeight
	 * @param scorll
	 * @return
	 */
	private JComponent createJBrowser(String url, int preferedWidth,
			int preferedHeight, boolean scroll) {
		JBrowserPane browser = new JBrowserPane(getClass().getResource(url));
		browser.removeHyperlinkListener(browser);
		browser.addHyperlinkListener(new SystemBrowser());
		browser.setPreferredSize(new Dimension(preferedWidth, preferedHeight));
		if (scroll) {
			return new JScrollPane(browser,
					JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		}
		browser.setBorder(BorderFactory.createLoweredBevelBorder());
		return browser;
	}

	/**
	 * Closes the currently selected model without saving if the user approves.
	 */
	private void closeModel() {
		SBMLDocument doc = getSelectedDocument();
		String title = null;
		if (doc.isSetModel()) {
			Model model = doc.getModel();
			if (model.isSetName() || model.isSetId()) {
				title = "model ";
			}
			if (model.isSetName()) {
				title += model.getName();
			} else if (model.isSetId()) {
				title += model.getId();
			}
		}
		if (title == null) {
			title = "the currently selected SBML document";
		}
		if ((doc != null)
				&& (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this,
					StringUtil.toHTML(String.format(
						"Do you really want to close %s without saving?", title), 60),
					"Close selected document", JOptionPane.YES_NO_OPTION))) {
			tabbedPane.remove(tabbedPane.getSelectedIndex());
			if (tabbedPane.getTabCount() == 0) {
				GUITools.setEnabled(false, getJMenuBar(), Action.SAVE_FILE,
					Action.TO_LATEX, Action.CLOSE_MODEL);
			}
		}
	}
	
	/**
	 * 
	 * @return
	 */
	private JMenuBar generateJMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		
		/*
		 * File menu
		 */
		JMenuItem openFile = GUITools.createJMenuItem(this, Action.OPEN_FILE,
			UIManager.getIcon("ICON_OPEN_16"), KeyStroke.getKeyStroke('O',
				InputEvent.CTRL_DOWN_MASK), 'O', true);
		JMenuItem saveFile = GUITools.createJMenuItem(this, Action.SAVE_FILE,
			UIManager.getIcon("ICON_SAVE_16"), KeyStroke.getKeyStroke('S',
				InputEvent.CTRL_DOWN_MASK), 'S', false);
		JMenuItem toLaTeX = GUITools.createJMenuItem(this, Action.TO_LATEX,
			UIManager.getIcon("ICON_LATEX_16"), KeyStroke.getKeyStroke('E',
				InputEvent.CTRL_DOWN_MASK), 'E', false);
		JMenuItem close = GUITools.createJMenuItem(this, Action.CLOSE_MODEL,
				UIManager.getIcon("ICON_TRASH_16"), KeyStroke.getKeyStroke(
						'W', InputEvent.CTRL_DOWN_MASK), 'W', false);
		JMenuItem exit = GUITools.createJMenuItem(this, Action.EXIT, UIManager
				.getIcon("ICON_EXIT_16"), KeyStroke.getKeyStroke(
				KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK));
		menuBar.add(GUITools.createJMenu("File", openFile, saveFile, toLaTeX,
			close, new JSeparator(), exit));
		
		/*
		 * Edit menu
		 */
		JMenuItem preferences = GUITools.createJMenuItem(this,
				Action.PREFERENCES, UIManager.getIcon("ICON_PREFS_16"),
				KeyStroke.getKeyStroke('E', InputEvent.ALT_GRAPH_DOWN_MASK),
				'P', true);
		menuBar.add(GUITools.createJMenu("Edit", preferences));
		
		/*
		 * Help menu
		 */
		JMenuItem help = GUITools.createJMenuItem(this, Action.HELP, UIManager
				.getIcon("ICON_HELP_16"), KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), 'H', true);
		JMenuItem about = GUITools.createJMenuItem(this, Action.HELP_ABOUT,
			UIManager.getIcon("ICON_INFO_16"), KeyStroke.getKeyStroke(
				KeyEvent.VK_F2, 0), 'I', true);
		JMenuItem license = GUITools.createJMenuItem(this, Action.HELP_LICENSE,
			UIManager.getIcon("ICON_LICENSE_16"), KeyStroke.getKeyStroke(
					KeyEvent.VK_F3, 0), 'L', true);
		JMenu helpMenu = GUITools.createJMenu("Help", help, about, license);
		try {
			menuBar.setHelpMenu(helpMenu);
		} catch (Error exc) {
			menuBar.add(helpMenu);
		}
		
		return menuBar;
	}
	
	/**
	 * 
	 * @throws SBMLException
	 * @throws IOException
	 */
	private void openFile() throws SBMLException, IOException {
		File file = GUITools.openFileDialog(this, openDir, false, false,
			JFileChooser.FILES_ONLY, new FileFilterKGML());
		
		// TODO: Add file format select JOPtionPane.
		
		if (file != null) {
			openDir = file.getParent();
			translateAndAddTab(file, "sbml");
		}
	}

  private void translateAndAddTab(File file, String format) throws IOException, SBMLException {
    Object ret = translateFile(file, format);
    if (ret instanceof SBMLDocument) {
      SBMLDocument doc = (SBMLDocument) ret;
      String title = doc.isSetModel() && doc.getModel().isSetId() ? doc
          .getModel().getId() : doc.toString();
      tabbedPane.add(title, new SBMLModelSplitPane(doc));
      GUITools.setEnabled(true, getJMenuBar(), Action.SAVE_FILE,
            Action.TO_LATEX, Action.CLOSE_MODEL);
    } else if (ret instanceof Graph2D) {
      Graph2D doc = (Graph2D) ret;
      
      //String title = TODO Get title from graph
      String title = file.getName();
      tabbedPane.setSelectedComponent(
        tabbedPane.add(title, new Graph2DView(doc))
      );
      GUITools.setEnabled(true, getJMenuBar(), Action.SAVE_FILE, Action.CLOSE_MODEL);
    }
  }
	
	/**
	 * 
	 */
	private void saveFile() {
		File file = GUITools.saveFileDialog(this, saveDir, false, false, true,
			JFileChooser.FILES_ONLY, SBFileFilter.SBML_FILE_FILTER,
			SBFileFilter.IMAGE_FILE_FILTER, SBFileFilter.TeX_FILE_FILTER,
			SBFileFilter.PDF_FILE_FILTER, SBFileFilter.TEXT_FILE_FILTER);
		if (file != null) {
			saveDir = file.getParent();
			if (SBFileFilter.isTeXFile(file) || SBFileFilter.isPDFFile(file)) {
				writeLaTeXReport(file);
			} else {
				
			}
		}
	}
	
	/**
	 * 
	 * @param file
	 * @param format - desired output format
	 * @return
	 * @throws IOException 
	 */
	@SuppressWarnings("unchecked")
  private Object translateFile(File file, String format) throws IOException {
		// TODO Auto-generated method stub
		//SBMLDocument doc = new SBMLDocument(3, 1);
		//doc.createModel("model_" + (tabbedPane.getTabCount() + 1));
    //return doc;
	  
	  AbstractKEGGtranslator<?> translator = (AbstractKEGGtranslator<SBMLDocument>)
	    BatchKEGGtranslator.getTranslator(format, Translator.getManager());
	  return translator.translate(file);
	}
	
	/**
	 * 
	 * @param targetFile
	 *        can be null
	 */
	private void writeLaTeXReport(File targetFile) {
		SBMLDocument doc = getSelectedDocument();
		if ((doc != null) && LaTeXExportDialog.showDialog(this, doc, targetFile)) {
			if (targetFile == null) {
				SBPreferences prefsIO = SBPreferences
						.getPreferencesFor(LaTeXOptionsIO.class);
				targetFile = new File(prefsIO.get(LaTeXOptionsIO.REPORT_OUTPUT_FILE));
			}
			try {
				SBML2LaTeX.convert(doc, targetFile, true);
			} catch (Exception exc) {
				GUITools.showErrorMessage(this, exc);
			}
		}
	}
	
	/**
	 * 
	 * @return null if this GUI does not contain any {@link SBMLDocument}
	 *         instances.
	 */
	private SBMLDocument getSelectedDocument() {
		if (tabbedPane.getTabCount() > 0) { return ((SBMLModelSplitPane) tabbedPane
				.getSelectedComponent()).getSBMLDocument(); }
		return null;
	}

	/* (non-Javadoc)
	 * @see java.awt.event.WindowListener#windowActivated(java.awt.event.WindowEvent)
	 */
	public void windowActivated(WindowEvent we) {
	}

	/* (non-Javadoc)
	 * @see java.awt.event.WindowListener#windowClosed(java.awt.event.WindowEvent)
	 */
	public void windowClosed(WindowEvent we) {
		if (we.getSource() instanceof JHelpBrowser) {
			GUITools.setEnabled(true, getJMenuBar(), Action.HELP, Action.HELP_LICENSE);
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.WindowListener#windowClosing(java.awt.event.WindowEvent)
	 */
	public void windowClosing(WindowEvent we) {
		if (we.getSource() instanceof TranslatorUI) {
			try {
				SBProperties props = new SBProperties();
				props.put(GUIOptions.OPEN_DIR, openDir);
				props.put(GUIOptions.SAVE_DIR, saveDir);
				SBPreferences.saveProperties(GUIOptions.class, props);
			} catch (BackingStoreException exc) {
				GUITools.showErrorMessage(this, exc);
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.WindowListener#windowDeactivated(java.awt.event.WindowEvent)
	 */
	public void windowDeactivated(WindowEvent we) {
	}

	/* (non-Javadoc)
	 * @see java.awt.event.WindowListener#windowDeiconified(java.awt.event.WindowEvent)
	 */
	public void windowDeiconified(WindowEvent we) {
	}

	/* (non-Javadoc)
	 * @see java.awt.event.WindowListener#windowIconified(java.awt.event.WindowEvent)
	 */
	public void windowIconified(WindowEvent we) {
	}

	/* (non-Javadoc)
	 * @see java.awt.event.WindowListener#windowOpened(java.awt.event.WindowEvent)
	 */
	public void windowOpened(WindowEvent we) {
	}

}
