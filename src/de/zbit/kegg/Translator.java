package de.zbit.kegg;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.prefs.BackingStoreException;

import de.zbit.kegg.gui.TranslatorUI;
import de.zbit.util.SBPreferences;

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
		
		SBPreferences.analyzeCommandLineArguments(TranslatorOptions.class, args);
		
		// Demo
		//PreferencesDialog d = new PreferencesDialog((Dialog)null);
		//PreferencesDialog.showPreferencesDialog();
		
		// Should we start the GUI?
		//  Boolean.parseBoolean(props.getProperty(GUIOptions.GUI.toString()).toString())
		if (args.length<1) {
			new TranslatorUI();
		} else {
			// TODO: Implement no-gui functionality.
		}
		
	}
  
}
