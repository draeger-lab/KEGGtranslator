package de.zbit.kegg;

import java.io.IOException;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.prefs.BackingStoreException;

import org.sbml.tolatex.LaTeXOptions;

import de.zbit.gui.GUIOptions;
import de.zbit.gui.cfg.SettingsDialog;
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
	 * @throws InvalidPropertiesFormatException
	 */
	public static void main(String[] args)
			throws InvalidPropertiesFormatException, IOException,
			BackingStoreException {
		String usage = "java Translator [options]";
		HashMap<String, Class<?>> defFileAndKeys = new HashMap<String, Class<?>>();
		defFileAndKeys.put(TranslatorOptions.CONFIG_FILE_LOCATION,
				TranslatorOptions.class);
		defFileAndKeys.put(GUIOptions.CONFIG_FILE_LOCATION, GUIOptions.class);
		defFileAndKeys.put(LaTeXOptions.CONFIG_FILE_LOCATION,
				LaTeXOptions.class);
		SBPreferences.analyzeCommandLineArguments(defFileAndKeys, usage, args);
		SettingsDialog d = new SettingsDialog("Settings");
		d.showSettingsDialog();
	}

}
