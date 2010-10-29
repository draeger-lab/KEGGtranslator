package de.zbit.kegg;

import java.awt.Dialog;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.TreeMap;
import java.util.prefs.BackingStoreException;

import de.zbit.gui.GUIOptions;
import de.zbit.gui.cfg.SettingsDialog;
import de.zbit.kegg.gui.TranslatorUI;
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
    TreeMap<String, Class<?>> defFileAndKeys = new TreeMap<String, Class<?>>();
    
    defFileAndKeys.put(TranslatorOptions.CONFIG_FILE_LOCATION,TranslatorOptions.class);
    
    //defFileAndKeys.put(GUIOptions.CONFIG_FILE_LOCATION, GUIOptions.class);
    //defFileAndKeys.put(LaTeXOptions.CONFIG_FILE_LOCATION,LaTeXOptions.class);
    
    SBProperties props = SBPreferences.analyzeCommandLineArguments(defFileAndKeys, args);
    
    // Demo
    SettingsDialog d = new SettingsDialog((Dialog)null);
    d.showSettingsDialog();
    
    // Should we start the GUI?
    if (Boolean.parseBoolean(props.getProperty(GUIOptions.GUI.toString()).toString())) {
      new TranslatorUI();
    }
    
  }
  
}
