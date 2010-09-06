/**
 * 
 */
package de.zbit.kegg.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.xml.stax.SBMLWriter;

import de.zbit.gui.GUITools;
import de.zbit.io.SBFileFilter;
import de.zbit.kegg.KeggInfoManagement;
import de.zbit.kegg.io.FileFilterKGML;
import de.zbit.kegg.io.KEGG2jSBML;

/**
 * @author draeger
 * 
 */
public class ConverterUI extends JDialog implements ActionListener {

	/**
	 * This is a enumeration of all possible commands this
	 * {@link ActionListener} can process.
	 * 
	 * @author draeger
	 * 
	 */
	public static enum Command {
		/**
		 * Command to open a file.
		 */
		OPEN_FILE,
		/**
		 * Command to save the conversion result to a file.
		 */
		SAVE_FILE;

		/**
		 * Returns a human-readable name for each command.
		 * 
		 * @return
		 */
		public String getName() {
			switch (this) {
			case OPEN_FILE:
				return "Open";
			case SAVE_FILE:
				return "Save";
			default:
				return "Unknown";
			}
		}
	}

	static {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		if (new File("keggdb.dat").exists()
				&& new File("keggdb.dat").length() > 0) {
			KeggInfoManagement manager = (KeggInfoManagement) KeggInfoManagement
					.loadFromFilesystem("keggdb.dat");
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
	 * @param args
	 *            accepted arguments are --input=<base directory to open files>
	 *            and --output=<base directory to save files>
	 * @throws SBMLException
	 */
	public static void main(String[] args) throws SBMLException {
		if (args.length > 0) {
			String infile = null, outfile = null;
			for (String arg : args) {
				if (arg.startsWith("--input=")) {
					infile = arg.split("=")[1];
				} else if (arg.startsWith("--output=")) {
					outfile = arg.split("=")[1];
				}
			}
			if (infile != null) {
				if (outfile != null) {
					new ConverterUI(infile, outfile);
				} else {
					new ConverterUI(infile, System.getProperty("user.dir"));
				}
			}
		} else {
			new ConverterUI();
		}
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
	 * Shows a small GUI.
	 * 
	 * @throws SBMLException
	 */
	public ConverterUI() throws SBMLException {
		this(System.getProperty("user.dir"), System.getProperty("user.dir"));
	}

	/**
	 * 
	 * @param baseDir
	 * @param saveDir
	 * @throws SBMLException
	 */
	public ConverterUI(String baseDir, String saveDir) throws SBMLException {
		super();
		this.baseOpenDir = baseDir;
		this.baseSaveDir = saveDir;
		this.doc = convert(openFile());
		showGUI(doc);
	}

	public void actionPerformed(ActionEvent e) {
		switch (Command.valueOf(e.getActionCommand())) {
		case OPEN_FILE:
			// Convert Kegg File to SBML document.
			if (isVisible()) {
				setVisible(false);
				removeAll();
			}
			try {
				showGUI(convert(openFile()));
			} catch (SBMLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			break;
		case SAVE_FILE:
			saveFile();
			break;
		default:
			System.err.println("unsuported action");
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
		for (Command com : Command.values()) {
			JMenuItem item = new JMenuItem(com.getName());
			item.setActionCommand(com.toString());
			item.addActionListener(this);
			menu.add(item);
		}
		bar.add(menu);
		return bar;
	}

	/**
	 * Open and display a KGML file.
	 */
	private File openFile() {
		File f = new File(baseOpenDir);
		if (f.isDirectory()) {
			JFileChooser chooser = GUITools
					.createJFileChooser(baseOpenDir, false, false,
							JFileChooser.FILES_ONLY, new FileFilterKGML());
			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				f = chooser.getSelectedFile();
				baseOpenDir = f.getParent();
			} else {
				dispose();
				System.exit(0);
			}
		}
		return f;
	}

	/**
	 * Save a conversion result to a file
	 */
	private void saveFile() {
		if (isVisible() && (doc != null)) {
			JFileChooser chooser = GUITools.createJFileChooser(baseSaveDir,
					false, false, JFileChooser.FILES_ONLY,
					SBFileFilter.SBML_FILE_FILTER);
			if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				File f = chooser.getSelectedFile();
				if (!f.exists() || GUITools.overwriteExistingFile(this, f)) {
					try {
						SBMLWriter.write(doc, chooser.getSelectedFile()
								.getAbsolutePath());
					} catch (Exception exc) {
						exc.printStackTrace();
						JOptionPane.showMessageDialog(this, exc.getMessage(),
								exc.getClass().getSimpleName(),
								JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		}
	}

	/**
	 * Displays an overview of the result of a conversion.
	 * 
	 * @throws SBMLException
	 */
	private void showGUI(SBMLDocument doc) throws SBMLException {
		getContentPane().removeAll();
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setJMenuBar(createJMenuBar());
		getContentPane().add(new SBMLModelSplitPane(doc.getModel()));
		setTitle("KGML2SBMLconverter " + doc.getModel().getId());
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	/**
	 * This method convertes a given KGML file into an SBMLDocument by calling
	 * the dedicated method in Kegg2jSBML.
	 * 
	 * @param f
	 * @return
	 */
	private SBMLDocument convert(File f) {
		try {
			this.doc = k2s.convert(f);
			return doc;
		} catch (IOException exc) {
			JOptionPane.showMessageDialog(this, exc.getMessage(), exc
					.getClass().getSimpleName(), JOptionPane.WARNING_MESSAGE);
		}
		return null;
	}

}
