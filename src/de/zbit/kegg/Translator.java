package de.zbit.kegg;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.BackingStoreException;

import de.zbit.gui.GUIOptions;
import de.zbit.kegg.gui.TranslatorUI;
import de.zbit.kegg.io.BatchKEGGtranslator;
import de.zbit.kegg.io.KEGGtranslator;
import de.zbit.util.SBPreferences;
import de.zbit.util.SBProperties;

/**
 * This class is the main class for the KEGGTranslator project.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2010-10-25
 */
public class Translator {
  /**
   * @param args
   * @throws BackingStoreException
   * @throws IOException
   * @throws URISyntaxException 
   */
	public static void main(String[] args) throws IOException, BackingStoreException, URISyntaxException {
		//TreeMap<String, Class<?>> defFileAndKeys = new TreeMap<String, Class<?>>();
		//defFileAndKeys.put(TranslatorOptions.CONFIG_FILE_LOCATION,TranslatorOptions.class);
		//defFileAndKeys.put(GUIOptions.CONFIG_FILE_LOCATION, GUIOptions.class);
		//defFileAndKeys.put(LaTeXOptions.CONFIG_FILE_LOCATION,LaTeXOptions.class);
		//SBProperties props = SBPreferences.analyzeCommandLineArguments(defFileAndKeys, args);
		
		List<Class<?>> configList = new LinkedList<Class<?>>();
		configList.add(TranslatorOptions.class);
		configList.add(GUIOptions.class);
		
		SBProperties props = SBPreferences.analyzeCommandLineArguments(TranslatorOptions.class, args);
		
		// Demo
		//PreferencesDialog d = new PreferencesDialog((Dialog)null);
		//PreferencesDialog.showPreferencesDialog();
		
		// Should we start the GUI?
		//  Boolean.parseBoolean(props.getProperty(GUIOptions.GUI.toString()).toString())
		if (args.length<1 || (props.containsKey(GUIOptions.GUI) && GUIOptions.GUI.getValue(props)) ) {
			new TranslatorUI();
		} else {
			translate(TranslatorOptions.FORMAT.getValue(props),
				props.get(TranslatorOptions.INPUT),
				props.get(TranslatorOptions.OUTPUT) );
		}
		
	}
	
	/**
	 * 
	 * @param format - currently one of {SBML,LaTeX,GraphML,GML,JPG,GIF,TGF,YGF}.
	 * @param input - input file
	 * @param output - output file
	 * @return
	 */
	public static boolean translate(String format, String input, String output) {
		
		// Check and build input
		File in = new File(input);
		if (!in.isFile() || !in.canRead()) {
			System.err.println("Invalid or not-readable input file.");
			return false;
		}
		
		// Check and build output
		File out = output==null? null: new File(output);
		if (out == null  || output.length()<1 || out.isDirectory()) {
			out = new File(input + '.' + format);
			System.out.println("Writing to " + out);
		}
		if (!out.canWrite()) {
			System.err.println("Cannot write to file " + out);
			return false;
		}
		if (out.exists()) {
			System.out.println("Overwriting exising file " + out);
		}
		
		// Initiate the manager
		KeggInfoManagement manager = TranslatorUI.getManager();
		
		// Check and build format
		KEGGtranslator translator = BatchKEGGtranslator.getTranslator(format, manager);
		if (translator==null) return false; // Error message already issued.
		
		// Translate.
		if (in.isDirectory()) {
		  BatchKEGGtranslator batch = new BatchKEGGtranslator();
      batch.setOrgOutdir(in.getPath());
      batch.setTranslator(translator);
      if (output!=null && output.length()>0 ) {
        batch.setChangeOutdirTo(output);
      }
      batch.parseDirAndSubDir();
    } else {
    	try {
				translator.translate(in.getPath(), out.getPath());
			} catch (Exception e) {
				e.printStackTrace();
			}
    }
			
	  return true;
	}
  
}
