/**
 * 
 */
package de.zbit.kegg.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.prefs.BackingStoreException;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.xml.stax.SBMLWriter;
import org.sbml.tolatex.SBML2LaTeX;
import org.sbml.tolatex.gui.LaTeXExportDialog;
import org.sbml.tolatex.io.LaTeXOptionsIO;

import de.zbit.gui.ActionCommand;
import de.zbit.gui.GUIOptions;
import de.zbit.gui.GUITools;
import de.zbit.gui.ImageTools;
import de.zbit.gui.ProgressBarSwing;
import de.zbit.gui.VerticalLayout;
import de.zbit.gui.prefs.PreferencesDialog;
import de.zbit.io.SBFileFilter;
import de.zbit.kegg.KeggInfoManagement;
import de.zbit.kegg.io.KEGG2jSBML;
import de.zbit.kegg.io.KEGGtranslator;
import de.zbit.util.prefs.SBPreferences;

/**
 * @author Andreas Dr&auml;ger
 */
public class TranslatorUI_Buggy extends JFrame implements ActionListener,
		WindowListener {
	
	/**
	 * This is a enumeration of all possible commands this {@link ActionListener}
	 * can process.
	 * 
	 * @author Andreas Dr&auml;ger
	 */
	public static enum Command implements ActionCommand {
		/**
		 * {@link Command} that closes the program.
		 */
		EXIT,
		/**
		 * {@link Command} to open a file.
		 */
		OPEN_FILE,
		/**
		 * {@link Command} to configure the user's preferences.
		 */
		PREFERENCES,
		/**
		 * {@link Command} to save the conversion result to a file.
		 */
		SAVE_FILE,
		/**
		 * {@link Command} for LaTeX export.
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
				case SAVE_FILE:
					return "Save";
				case TO_LATEX:
					return "Export to LaTeX";
				case PREFERENCES:
					return "Preferences";
				case EXIT:
					return "Exit";
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
				case SAVE_FILE:
					return "Saves the currently opened model in one of the available formats.";
				case TO_LATEX:
					return "Converts the currently opened model to a LaTeX report file.";
				case PREFERENCES:
					return "Opens a dialog to configure all options for this program.";
				case EXIT:
					return "Closes this program.";
				default:
					return "Unknown";
			}
		}
	}
	
	/**
	 * Speedup Kegg2SBML by loading already queried objects. Reduces network load
	 * and heavily reduces computation time.
	 */
	private static KEGG2jSBML k2s;
	
	/**
	 * Generated serial version id
	 */
	private static final long serialVersionUID = -3833481758555783529L;
	
	static {
		ImageTools.initImages(TranslatorUI_Buggy.class.getResource("img"));
		GUITools.initLaF(KEGGtranslator.APPLICATION_NAME);
		
		KeggInfoManagement manager = getManager();
		k2s = new KEGG2jSBML(manager);
	}
	
	public static KeggInfoManagement getManager() {
		KeggInfoManagement manager;
		if (new File(KEGGtranslator.cacheFileName).exists()
				&& new File(KEGGtranslator.cacheFileName).length() > 0) {
			try {
				manager = (KeggInfoManagement) KeggInfoManagement
						.loadFromFilesystem(KEGGtranslator.cacheFileName);
			} catch (IOException e) {
				e.printStackTrace();
				manager = new KeggInfoManagement();
			}
		} else {
			manager = new KeggInfoManagement();
		}
		return manager;
	}
	
	/**
	 * Basis directory when opening files.
	 */
	private String baseOpenDir;
	/**
	 * Basis directory when saving files.
	 */
	private String baseSaveDir;
	/**
	 * The SBML document as a result of a successful conversion.
	 */
	private SBMLDocument doc;
	/**
	 * The user's configuration.
	 */
	private SBPreferences prefs;
	
	/**
	 * Shows a small GUI.
	 */
	public TranslatorUI_Buggy() {
		super();
		addWindowListener(this);
		try {
			prefs = SBPreferences.getPreferencesFor(GUIOptions.class);
			this.baseOpenDir = prefs.getString(GUIOptions.OPEN_DIR);
			this.baseSaveDir = prefs.getString(GUIOptions.SAVE_DIR);
			showGUI();
		} catch (IOException exc) {
      // Impossible.
		} catch (SBMLException exc) {
			GUITools.showErrorMessage(this, exc);
			dispose();
			System.exit(1);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		switch (Command.valueOf(e.getActionCommand())) {
			case OPEN_FILE:
				// Translate KEGG file to SBML document.
				if (isVisible()) {
					setVisible(false);
					removeAll();
				}
				try {
					doc = new SBMLDocument(2, 4); 
					doc.createModel("model");
						//translate(openFile());
					showGUI();
				} catch (Throwable exc) {
				  exc.printStackTrace();
					GUITools.showErrorMessage(this, exc);
				}
				break;
			case SAVE_FILE:
				saveFile();
				break;
			case TO_LATEX:
				if (LaTeXExportDialog.showDialog(this, doc)) {
					SBPreferences prefsIO = SBPreferences
							.getPreferencesFor(LaTeXOptionsIO.class);
					try {
						SBML2LaTeX.convert(doc, prefsIO
								.get(LaTeXOptionsIO.REPORT_OUTPUT_FILE), true);
					} catch (Exception exc) {
						GUITools.showErrorMessage(this, exc);
					}
				}
				break;
			case PREFERENCES:
				PreferencesDialog.showPreferencesDialog();
				break;
			case EXIT:
				dispose();
				try {
					prefs.flush();
				} catch (BackingStoreException exc) {
					GUITools.showErrorMessage(this, exc);
				}
				System.exit(0);
			default:
				System.err.printf("unsuported action: %s\n", e.getActionCommand());
				break;
		}
	}
	
	/**
	 * Creates a JMenuBar for this component that provides access to all Actions
	 * definied in the enum Command.
	 * 
	 * @return
	 */
	private JMenuBar createJMenuBar() {
		JMenuBar bar = new JMenuBar();
		JMenu menu = new JMenu("File");
		menu.add(GUITools.createJMenuItem(this, Command.OPEN_FILE));
		menu.add(GUITools.createJMenuItem(this, Command.SAVE_FILE));
		menu.add(GUITools.createJMenuItem(this, Command.TO_LATEX));
		menu.addSeparator();
		menu.add(GUITools.createJMenuItem(this, Command.EXIT));
		bar.add(menu);
		bar.add(GUITools.createJMenu("Edit", GUITools.createJMenuItem(this,
			Command.PREFERENCES)));
		return bar;
	}
	
	/**
	 * Create and display a temporary loading panel with the given message and a
	 * progress bar.
	 * 
	 * @param loadingMessage
	 *        - the Message to display.
	 * @return JDialog
	 */
	private JDialog createLoadingJPanel(String loadingMessage) {
		// Create the panel
		Dimension panelSize = new java.awt.Dimension(400, 75);
		JPanel p = new JPanel(new VerticalLayout());
		p.setPreferredSize(panelSize);
		
		// Create the label and progressBar
		JLabel jl = new JLabel(loadingMessage);
		Font font = new java.awt.Font("Tahoma", Font.PLAIN, 12);
		jl.setFont(font);
		
		JProgressBar prog = new JProgressBar();
		prog.setPreferredSize(new Dimension(panelSize.width - 20,
			panelSize.height / 4));
		p.add(jl, BorderLayout.NORTH);
		p.add(prog, BorderLayout.CENTER);
		
		// Link the progressBar to the keggConverter
		k2s.setProgressBar(new ProgressBarSwing(prog));
		
		// Display the panel in an jFrame
		JDialog f = new JDialog();
		f.setTitle(KEGGtranslator.APPLICATION_NAME);
		f.setSize(p.getPreferredSize());
		f.setContentPane(p);
		f.setPreferredSize(p.getPreferredSize());
		f.setLocationRelativeTo(null);
		f.setVisible(true);
		f.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		
		return f;
	}
	
	/**
	 * Open and display a KGML file.
	 */
	private File openFile() {
		File file = GUITools.openFileDialog(this, baseOpenDir, false, 
			JFileChooser.FILES_ONLY, new FileFilterKGML());
		if (file != null) {
			baseOpenDir = file.getParent();
			prefs.put(GUIOptions.OPEN_DIR, baseOpenDir);
			return file;
		} else {
			dispose();
			System.exit(0);
		}
		return new File(baseOpenDir);
	}
	
	/**
	 * Save a conversion result to a file
	 */
	private void saveFile() {
		if (isVisible() && (doc != null)) {
			File file = GUITools.saveFileDialog(this, baseSaveDir, false, false,
				JFileChooser.FILES_ONLY, SBFileFilter.createSBMLFileFilter());
			if (file != null) {
				try {
					baseSaveDir = file.getParent();
					prefs.put(GUIOptions.SAVE_DIR, baseSaveDir);
					SBMLWriter.write(doc, file, "SBML from KEGG",
						KEGG2jSBML.VERSION_NUMBER);
				} catch (Throwable exc) {
					GUITools.showErrorMessage(this, exc);
				}
			}
		}
	}
	
	/**
	 * Displays an overview of the result of a conversion.
	 * 
	 * @throws SBMLException
	 * @throws IOException
	 */
	private void showGUI() throws SBMLException, IOException {
		getContentPane().removeAll();
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setJMenuBar(createJMenuBar());
		if (doc == null) {
			// this.doc = translate(openFile());
		} else {
			getContentPane().add(new SBMLModelSplitPane(doc));
			setTitle("SBML from KEGG " + doc.getModel().getId());
		}
		pack();
		setMinimumSize(new Dimension(640, 480));
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	/**
	 * This method translates a given KGML file into an SBMLDocument by calling
	 * the dedicated method in {@link KEGG2jSBML}.
	 * 
	 * @param f
	 * @return
	 */
	private SBMLDocument translate(File f) {
		JDialog load = null;
		try {
			load = createLoadingJPanel("Translating kegg pathway " + f.getName()
					+ "...");
			return k2s.translate(f);
		} catch (Throwable exc) {
			GUITools.showErrorMessage(this, exc, String.format(
				"Could not read input file %s.", f.getAbsolutePath()));
		} finally {
			if (load != null) {
				load.dispose();
			}
		}
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.WindowListener#windowActivated(java.awt.event.WindowEvent)
	 */
	public void windowActivated(WindowEvent e) {
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.WindowListener#windowClosed(java.awt.event.WindowEvent)
	 */
	public void windowClosed(WindowEvent e) {
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.WindowListener#windowClosing(java.awt.event.WindowEvent)
	 */
	public void windowClosing(WindowEvent e) {
		try {
			prefs.flush();
		} catch (BackingStoreException exc) {
			GUITools.showErrorMessage(this, exc);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.WindowListener#windowDeactivated(java.awt.event.WindowEvent
	 * )
	 */
	public void windowDeactivated(WindowEvent e) {
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.WindowListener#windowDeiconified(java.awt.event.WindowEvent
	 * )
	 */
	public void windowDeiconified(WindowEvent e) {
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.WindowListener#windowIconified(java.awt.event.WindowEvent)
	 */
	public void windowIconified(WindowEvent e) {
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.WindowListener#windowOpened(java.awt.event.WindowEvent)
	 */
	public void windowOpened(WindowEvent e) {
	}
	
}
