package de.zbit.kegg;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.BackingStoreException;

import javax.swing.SwingUtilities;

import de.zbit.gui.GUIOptions;
import de.zbit.kegg.gui.TranslatorUI;
import de.zbit.kegg.io.BatchKEGGtranslator;
import de.zbit.kegg.io.KEGGtranslator;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.util.prefs.SBProperties;

/**
 * This class is the main class for the KEGGTranslator project.
 * 
 * @author Andreas Dr&auml;ger
 * @author Clemens Wrzodek
 * @date 2010-10-25
 */
public class Translator {
  /**
   * The cache to be used by all KEGG interacting classes.
   * Access via {@link #getManager()}.
   */
  private static KeggInfoManagement manager=null;
	/**
	 * @param args
	 * @throws BackingStoreException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static void main(String[] args) throws IOException,
		BackingStoreException, URISyntaxException {
		// --input files/KGMLsamplefiles/hsa00010.xml --format GraphML --output test.txt
		List<Class<?>> configList = new LinkedList<Class<?>>();
		configList.add(TranslatorOptions.class);
		configList.add(GUIOptions.class);
				
		SBProperties props = SBPreferences.analyzeCommandLineArguments(
			TranslatorOptions.class, args);
		
		//		KeggInfoManagement manager = getManager();
		//		k2s = new KEGG2jSBML(manager);
		
		// Should we start the GUI?
		if ((args.length < 1)
				|| (props.containsKey(GUIOptions.GUI) && GUIOptions.GUI.getValue(props))) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					TranslatorUI ui = new TranslatorUI();
					ui.setVisible(true);
				}
			});
		} else {
			translate(TranslatorOptions.FORMAT.getValue(props), props
					.get(TranslatorOptions.INPUT), props.get(TranslatorOptions.OUTPUT));
		}
		
	}
	
	/**
	 * 
	 * @param format - currently one of {SBML,LaTeX,GraphML,GML,JPG,GIF,TGF,YGF}.
	 * @param input - input file
	 * @param output - output file
	 * @return
	 * @throws IOException
	 */
	public static boolean translate(String format, String input, String output)
		throws IOException {
		
		// Check and build input
		File in = new File(input);
		if (!in.isFile() || !in.canRead()) {
			System.err.println("Invalid or not-readable input file.");
			return false;
		}
		
		// Initiate the manager
		KeggInfoManagement manager = getManager();
		
		// Check and build format
		KEGGtranslator translator = BatchKEGGtranslator.getTranslator(format, manager);
		if (translator == null) return false; // Error message already issued.
			
		// Check and build output
		File out = output == null ? null : new File(output);
		if (out == null || output.length() < 1 || out.isDirectory()) {
			String fileExtension = BatchKEGGtranslator.getFileExtension(translator);
			out = new File(removeFileExtension(input) + fileExtension);
			
			System.out.println("Writing to " + out);
		}
		if (out.exists()) {
			System.out.println("Overwriting exising file " + out);
		}
		out.createNewFile();
		if (!out.canWrite()) {
			System.err.println("Cannot write to file " + out);
			return false;
		}
		
		// Translate.
		if (in.isDirectory()) {
			BatchKEGGtranslator batch = new BatchKEGGtranslator();
			batch.setOrgOutdir(in.getPath());
			batch.setTranslator(translator);
			if (output != null && output.length() > 0) {
				batch.setChangeOutdirTo(output);
			}
			batch.parseDirAndSubDir();
			// parseDir... is saving the cache.
		} else {
			try {
				translator.translate(in.getPath(), out.getPath());
				saveCache();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return true;
	}
	
	/**
	 * 
	 * @return
	 */
	public synchronized static KeggInfoManagement getManager() {
	  
	  // Try to load from cache file
		if (manager==null && new File(KEGGtranslator.cacheFileName).exists() && new File(KEGGtranslator.cacheFileName).length() > 0) {
			try {
				manager = (KeggInfoManagement) KeggInfoManagement.loadFromFilesystem(KEGGtranslator.cacheFileName);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// Create new, if loading failed
		if (manager==null) {
			manager = new KeggInfoManagement();
		}
		
		return manager;
	}
	
	/**
	 * Remember already queried KEGG objects (save cache)
	 */
	public synchronized static void saveCache() {
    if (manager!=null && manager.hasChanged()) {
      KeggInfoManagement.saveToFilesystem(KEGGtranslator.cacheFileName, manager);
    }
	}
	
	/**
	 * If the input has a file extension, it is removed. else, the input is
	 * returned.
	 * 
	 * @param input
	 * @return
	 */
	private static String removeFileExtension(String input) {
		int pos = input.lastIndexOf('.');
		if (pos > 0) { return input.substring(0, pos); }
		return input;
	}
	
}
