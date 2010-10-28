/**
 * 
 */
package de.zbit.kegg.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

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
import org.sbml.tolatex.gui.LaTeXExportDialog;

import de.zbit.gui.ActionCommand;
import de.zbit.gui.GUIOptions;
import de.zbit.gui.GUITools;
import de.zbit.gui.ImageTools;
import de.zbit.gui.VerticalLayout;
import de.zbit.gui.cfg.SettingsDialog;
import de.zbit.io.SBFileFilter;
import de.zbit.kegg.KeggInfoManagement;
import de.zbit.kegg.io.KEGG2jSBML;
import de.zbit.kegg.io.KEGGtranslator;
import de.zbit.util.ProgressBarSwing;
import de.zbit.util.SBPreferences;

/**
 * @author Andreas Dr&auml;ger
 */
public class TranslatorUI extends JFrame implements ActionListener {

    /**
     * This is a enumeration of all possible commands this
     * {@link ActionListener} can process.
     * 
     * @author Andreas Dr&auml;ger
     */
    public static enum Command implements ActionCommand {
	/**
	 * {@link Command} to open a file.
	 */
	OPEN_FILE,
	/**
	 * {@link Command} to save the conversion result to a file.
	 */
	SAVE_FILE,
	/**
	 * {@link Command} for LaTeX export.
	 */
	TO_LATEX,
	/**
	 * {@link Command} to configure the user's preferences.
	 */
	PREFERENCES,
	/**
	 * {@link Command} that closes the program.
	 */
	EXIT;

	/*
	 * (non-Javadoc)
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

    static {
	ImageTools.initImages(TranslatorUI.class.getResource("img"));
	GUITools.initLaF(KEGGtranslator.appName);
	if (new File(KEGGtranslator.cacheFileName).exists()
		&& new File(KEGGtranslator.cacheFileName).length() > 0) {
	    KeggInfoManagement manager;
	    try {
		manager = (KeggInfoManagement) KeggInfoManagement
			.loadFromFilesystem(KEGGtranslator.cacheFileName);
	    } catch (IOException e) {
		e.printStackTrace();
		manager = new KeggInfoManagement();
	    }
	    k2s = new KEGG2jSBML(manager);
	} else {
	    k2s = new KEGG2jSBML();
	}
    }

    /**
     * Generated serial version id
     */
    private static final long serialVersionUID = -3833481758555783529L;

    /**
     * Speedup Kegg2SBML by loading already queried objects. Reduces network
     * load and heavily reduces computation time.
     */
    private static KEGG2jSBML k2s;

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
    public TranslatorUI() {
	super();
	try {
	    prefs = SBPreferences.getPreferencesFor(GUIOptions.class,
		GUIOptions.CONFIG_FILE_LOCATION);
	    this.baseOpenDir = prefs.get(GUIOptions.OPEN_DIR);
	    this.baseSaveDir = prefs.get(GUIOptions.SAVE_DIR);
	    this.doc = translate(openFile());
	    showGUI(doc);
	} catch (IOException exc) {
	    GUITools.showErrorMessage(this, exc, String.format(
		"Cannot read configuration file %s.",
		GUIOptions.CONFIG_FILE_LOCATION));
	    dispose();
	    System.exit(1);
	} catch (SBMLException exc) {
	    GUITools.showErrorMessage(this, exc);
	    dispose();
	    System.exit(1);
	}
    }

    /*
     * (non-Javadoc)
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
		showGUI(translate(openFile()));
	    } catch (Throwable exc) {
		GUITools.showErrorMessage(this, exc);
	    }
	    break;
	case SAVE_FILE:
	    saveFile();
	    break;
	case TO_LATEX:
	    new LaTeXExportDialog(this, doc);
	    break;
	case PREFERENCES:
	    SettingsDialog d = new SettingsDialog(this);
	    d.showSettingsDialog();
	    break;
	case EXIT:
	    dispose();
	    System.exit(0);
	default:
	    System.err.println("unsuported action: " + e.getActionCommand());
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
     * Open and display a KGML file.
     */
    private File openFile() {
	File file = GUITools.openFileDialog(this, baseOpenDir, false, false,
	    JFileChooser.FILES_ONLY, new FileFilterKGML());
	if (file != null) {
	    baseOpenDir = file.getParent();
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
	    File file = GUITools.saveFileDialog(this, baseSaveDir, false,
		false, JFileChooser.FILES_ONLY, SBFileFilter.SBML_FILE_FILTER);
	    if (file != null) {
		try {
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
    private void showGUI(SBMLDocument doc) throws SBMLException, IOException {
	getContentPane().removeAll();
	setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	setJMenuBar(createJMenuBar());
	getContentPane().add(new SBMLModelSplitPane(doc.getModel()));
	setTitle("SBML from KEGG " + doc.getModel().getId());
	pack();
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
	    load = createLoadingJPanel("Translating kegg pathway "
		    + f.getName() + "...");
	    this.doc = k2s.translate(f);
	    return doc;
	} catch (Throwable exc) {
	    GUITools.showErrorMessage(this, exc, String.format(
		"Could not read input file %s.", f.getAbsolutePath()));
	} finally {
	    if (load != null)
		load.dispose();
	}
	return null;
    }

    /**
     * Create and display a temporary loading panel with the given
     * message and a progress bar.
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
	f.setTitle(KEGGtranslator.appName);
	f.setSize(p.getPreferredSize());
	f.setContentPane(p);
	f.setPreferredSize(p.getPreferredSize());
	f.setLocationRelativeTo(null);
	f.setVisible(true);
	f.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

	return f;
    }

}
