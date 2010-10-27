package de.zbit.kegg;

import java.io.IOException;
import java.util.InvalidPropertiesFormatException;
import java.util.prefs.BackingStoreException;

import de.zbit.gui.cfg.SettingsDialog;
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
   * @throws InvalidPropertiesFormatException
   */
  public static void main(String[] args) throws InvalidPropertiesFormatException, IOException, BackingStoreException {
    SBProperties props = SBPreferences.analyzeCommandLineArguments(TranslatorOptions.class,
        TranslatorOptions.CONFIG_FILE_LOCATION, true,
        "java Translator [options]", args);
    System.out.println(props);
    SettingsDialog d = new SettingsDialog("Settings");
    d.showSettingsDialog();
  }
  
}
