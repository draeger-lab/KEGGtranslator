/*
 * $Id: Translator.java 410 2015-09-13 04:49:20Z draeger $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn/KEGGconverter/trunk/src/de/zbit/kegg/Translator.java $
 * ---------------------------------------------------------------------
 * This file is part of KEGGtranslator, a program to convert KGML files
 * from the KEGG database into various other formats, e.g., SBML, GML,
 * GraphML, and many more. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator> to
 * obtain the latest version of KEGGtranslator.
 *
 * Copyright (C) 2011-2015 by the University of Tuebingen, Germany.
 *
 * KEGGtranslator is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.kegg;

import static de.zbit.util.Utils.getMessage;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;

import de.zbit.AppConf;
import de.zbit.Launcher;
import de.zbit.cache.InfoManagement;
import de.zbit.garuda.GarudaOptions;
import de.zbit.gui.GUIOptions;
import de.zbit.io.FileTools;
import de.zbit.kegg.api.KeggInfos;
import de.zbit.kegg.api.cache.KeggFunctionManagement;
import de.zbit.kegg.api.cache.KeggInfoManagement;
import de.zbit.kegg.ext.KEGGTranslatorPanelOptions;
import de.zbit.kegg.gui.TranslatorUI;
import de.zbit.kegg.io.AbstractKEGGtranslator;
import de.zbit.kegg.io.BatchKEGGtranslator;
import de.zbit.kegg.io.KEGGtranslator;
import de.zbit.kegg.io.KEGGtranslatorIOOptions;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.util.ResourceManager;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.util.prefs.SBProperties;

/**
 * This class is the main class for the KEGGtranslator project.
 * 
 * <p>
 * Recommended VM-Arguments:
 * <pre>-Xms128m -Xmx512m -splash:bin/de/zbit/kegg/gui/img/Logo.gif -Duser.language=en -Duser.country=US</pre>
 * 
 * @author Andreas Dr&auml;ger
 * @author Clemens Wrzodek
 * @date 2010-10-25
 * @since 1.0
 * @version $Rev: 410 $
 */
public class Translator extends Launcher {
  
  /**
   * Localization support
   */
  @SuppressWarnings("unused")
  private static final transient ResourceBundle bundle = ResourceManager.getBundle(Translator.class.getName());
  
  /**
   * {@link File} name of the KEGG cache {@link File} (implemented just like the
   * browser cache). Must be loaded upon start and saved upon exit.
   */
  public final static String cacheFileName = "keggdb.dat";
  
  /**
   * {@link File} name of the KEGG function cache {@link File} (implemented just
   * like the browser cache). Must be loaded upon start and saved upon exit.
   */
  public final static String cacheFunctionFileName = "keggfc.dat";
  
  /**
   * The {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(Translator.class.getName());
  
  /**
   * The cache to be used by all KEGG interacting classes.
   * Access via {@link #getManager()}.
   */
  private static KeggInfoManagement manager = null;
  
  /**
   * The cache to be used by all KEGG functions interacting classes.
   * Access via {@link #getFunctionManager()}.
   */
  private static KeggFunctionManagement managerFunction = null;
  
  /**
   * Adjusts a few methods in KEGGtranslator to generate an ouput for
   * the path2models project if true.
   * <p><b>PLESE ALWAYS KEEP THE DEFAULT, INITIAL VALUE TO FALSE!</b>
   */
  public static boolean path2models = false;
  
  /**
   * Generated serial version identifier.
   */
  private static final long serialVersionUID = -428595670090648615L;
  
  /**
   * Adjusts the current KEGGtranslator instances to create an outout
   * for the path2models project.
   */
  public static void adjustForPath2Models() {
    path2models = true;
    KeggInfos.path2models = true;
    /*
     * 1) Set the required options
     * 2) Uncomment code in [KEGG] Pathway.java for 'getCompoundPreviewPicture()'
     * 3) Activate additional KEGG COMPOUND 2 ChEBI mapping in KeggInfos.java ('getChebi()'-method)
     */
    
    KEGGtranslatorOptions.AUTOCOMPLETE_REACTIONS.setDefaultValue(Boolean.TRUE);
    KEGGtranslatorOptions.USE_GROUPS_EXTENSION.setDefaultValue(Boolean.FALSE);
    KEGGtranslatorOptions.REMOVE_ORPHANS.setDefaultValue(Boolean.FALSE);
    KEGGtranslatorOptions.REMOVE_WHITE_GENE_NODES.setDefaultValue(Boolean.TRUE);
    KEGGtranslatorOptions.SHOW_FORMULA_FOR_COMPOUNDS.setDefaultValue(Boolean.FALSE);
    KEGGtranslatorOptions.REMOVE_PATHWAY_REFERENCES.setDefaultValue(Boolean.TRUE);
    KEGGtranslatorOptions.CELLDESIGNER_ANNOTATIONS.setDefaultValue(Boolean.FALSE);
    KEGGtranslatorOptions.ADD_LAYOUT_EXTENSION.setDefaultValue(Boolean.TRUE);
    KEGGtranslatorOptions.CHECK_ATOM_BALANCE.setDefaultValue(Boolean.FALSE);
    
    SBPreferences prefs = SBPreferences.getPreferencesFor(KEGGtranslatorOptions.class);
    prefs.put(KEGGtranslatorOptions.AUTOCOMPLETE_REACTIONS, Boolean.TRUE);
    prefs.put(KEGGtranslatorOptions.USE_GROUPS_EXTENSION, Boolean.FALSE);
    prefs.put(KEGGtranslatorOptions.REMOVE_ORPHANS, Boolean.FALSE);
    prefs.put(KEGGtranslatorOptions.REMOVE_WHITE_GENE_NODES, Boolean.TRUE);
    prefs.put(KEGGtranslatorOptions.SHOW_FORMULA_FOR_COMPOUNDS, Boolean.FALSE);
    prefs.put(KEGGtranslatorOptions.REMOVE_PATHWAY_REFERENCES, Boolean.TRUE);
    prefs.put(KEGGtranslatorOptions.CELLDESIGNER_ANNOTATIONS, Boolean.FALSE);
    prefs.put(KEGGtranslatorOptions.ADD_LAYOUT_EXTENSION, Boolean.TRUE);
    prefs.put(KEGGtranslatorOptions.CHECK_ATOM_BALANCE, Boolean.FALSE);
    
    try {
      prefs.flush();
    } catch (BackingStoreException e) {
      logger.log(Level.SEVERE, "Could not adjust KEGGtranslator options for path2models.", e);
    }
  }
  
  /**
   * 
   * @return
   */
  public synchronized static KeggFunctionManagement getFunctionManager() {
    
    // Try to load from cache file
    if (managerFunction == null
        && new File(Translator.cacheFunctionFileName).exists()
        && new File(Translator.cacheFunctionFileName).length() > 1) {
      try {
        managerFunction = (KeggFunctionManagement) InfoManagement
            .loadFromFilesystem(Translator.cacheFunctionFileName);
      } catch (Throwable e) { // IOException or class cast, if class is moved.
        e.printStackTrace();
        managerFunction = null;
        
        // Delete invalid cache file
        try {
          File f = new File(Translator.cacheFunctionFileName);
          if (f.exists() && f.canRead()) {
            logger.info(MessageFormat.format("Deleting invalid cache file {0}.", f.getName()));
            f.delete();
          }
        } catch (Throwable t) {
        }
      }
    }
    
    // Create new, if loading failed
    if (managerFunction == null) {
      managerFunction = new KeggFunctionManagement(5000);
    }
    
    return managerFunction;
  }
  
  /**
   * 
   * @return
   */
  public synchronized static KeggInfoManagement getManager() {
    boolean newManangerLoadedOrInitialized = (manager==null);
    // Try to load from cache file
    if ((manager == null) && new File(Translator.cacheFileName).exists() && new File(Translator.cacheFileName).length() > 1) {
      try {
        manager = (KeggInfoManagement) InfoManagement.loadFromFilesystem(Translator.cacheFileName);
      } catch (Throwable e) { // IOException or class cast, if class is moved.
        e.printStackTrace();
        manager = null;
        // Delete invalid cache file
        try {
          File f = new File(Translator.cacheFileName);
          if (f.exists() && f.canRead()) {
            logger.info(MessageFormat.format("Deleting invalid cache file {0}.", f.getName()));
            f.delete();
          }
        } catch (Throwable t) {
          logger.log(Level.FINEST, t.getMessage(), t);
        }
      }
    }
    
    // Create new, if loading failed
    if (manager == null) {
      manager = new KeggInfoManagement(10000);
    }
    
    // Set cache size and eventually remove some items from the cache
    if (newManangerLoadedOrInitialized) {
      int initialSize=-1;
      try {
        SBPreferences prefs = SBPreferences.getPreferencesFor(KEGGtranslatorCommandLineOnlyOptions.class);
        initialSize = KEGGtranslatorCommandLineOnlyOptions.CACHE_SIZE.getValue(prefs);
        
        if (KEGGtranslatorCommandLineOnlyOptions.CLEAR_FAIL_CACHE.getValue(prefs)) {
          logger.info("Clearing cache of failed-to-retrieve objects.");
          manager.clearFailCache();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      if (initialSize<=0) {
        initialSize = 10000;
      }
      manager.setCacheSize(initialSize);
    }
    
    
    
    return manager;
  }
  
  
  /**
   * 
   * @param args
   */
  public static void main(String args[]) {
    // --input files/KGMLsamplefiles/hsa00010.xml --format GraphML --output test.txt
    new Translator(args);
  }
  
  /**
   * Remember already queried KEGG objects (save cache)
   */
  public synchronized static void saveCache() {
    if ((manager != null) && manager.hasChanged()) {
      KeggInfoManagement.saveToFilesystem(Translator.cacheFileName, manager);
    }
    if ((managerFunction != null) && managerFunction.isCacheChangedSinceLastLoading()) {
      InfoManagement.saveToFilesystem(Translator.cacheFunctionFileName, managerFunction);
    }
  }
  
  /**
   * Wrapper methods for applications including KEGGtranslator as library:
   * this method translates a given KGML document and returns the resulting
   * data structure (currently SBMLDocument or Graph2D).
   * @param format
   * @param in
   * @return
   * @throws IOException
   */
  public static Object translate(Format format, File in) throws IOException {
    // Check and build input
    if ((in == null) || !in.canRead() || in.isDirectory()) {
      logger.severe("Invalid or not-readable input file.");
      return null;
    }
    
    // Initiate the manager
    KeggInfoManagement manager = getManager();
    
    // Check and build format
    AbstractKEGGtranslator<?> translator = (AbstractKEGGtranslator<?>) BatchKEGGtranslator
        .getTranslator(format, manager);
    if (translator == null) {
      return false; // Error message already issued.
    }
    
    // Translate.
    return translator.translate(in);
  }
  
  /**
   * 
   * @param format One of all valid output {@link Format}s.
   * @param input input file
   * @param output output file
   * @return
   * @throws IOException
   */
  public static boolean translate(Format format, String input, String output)
      throws IOException {
    
    // Check and build input
    File in = input == null ? null : new File(input);
    if ((in == null) || !in.canRead()) {
      // in might also be a directory
      logger.severe("Invalid or not-readable input file.");
      return false;
    }
    
    // Initiate the manager
    KeggInfoManagement manager = getManager();
    
    // Check and build format
    KEGGtranslator<?> translator = BatchKEGGtranslator.getTranslator(format, manager);
    if (translator == null) {
      return false; // Error message already issued.
    }
    
    // Check and build output
    File out = output == null ? null : new File(output);
    if (!in.isDirectory()) {
      // else: batch-mode
      if ((out == null) || (output.length() < 1) || out.isDirectory()) {
        String fileExtension = BatchKEGGtranslator.getFileExtension(translator);
        out = new File(FileTools.removeFileExtension(input) + fileExtension);
        logger.info(MessageFormat.format("Writing to {0}.", out));
      }
      
      // Further check out
      if (out.exists()) {
        logger.info(MessageFormat.format("Overwriting exising file {0}.", out));
      }
      out.createNewFile();
      if (!out.canWrite()) {
        logger.severe(MessageFormat.format("Cannot write to file {0}.", out));
        return false;
      }
    }
    
    // Translate.
    if (in.isDirectory()) {
      BatchKEGGtranslator batch = new BatchKEGGtranslator();
      batch.setOrgOutdir(in.getPath());
      batch.setTranslator(translator);
      batch.setOutFormat(format);
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
   * @param args
   */
  public Translator(String[] args) {
    super(args);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#addCopyrightToSplashScreen()
   */
  @Override
  protected boolean addCopyrightToSplashScreen() {
    return false;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#commandLineMode(de.zbit.AppConf)
   */
  @Override
  public void commandLineMode(AppConf appConf) {
    SBProperties props = appConf.getCmdArgs();
    
    // Maybe adjust for path2models
    SBPreferences prefs = SBPreferences.getPreferencesFor(KEGGtranslatorCommandLineOnlyOptions.class);
    if (KEGGtranslatorCommandLineOnlyOptions.PATH2MODELS.getValue(props)) {
      adjustForPath2Models();
    }
    
    // Make command-line options persistent
    prefs.restoreDefaults(); // This is just used as an empty prefs-template.
    try {
      prefs.saveContainedOptions(props);
    } catch (BackingStoreException e) {
      logger.log(Level.WARNING, "Could not process command-line-only options.", e);
    }
    
    // Initiate translation
    try {
      translate(KEGGtranslatorIOOptions.FORMAT.getValue(props),
        props.get(KEGGtranslatorIOOptions.INPUT),
        props.get(KEGGtranslatorIOOptions.OUTPUT));
    } catch (IOException exc) {
      logger.warning(getMessage(exc));
    }
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#getAppName()
   */
  @Override
  public String getAppName() {
    return "KEGGtranslator";
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#getCitation(boolean)
   */
  @Override
  public String getCitation(boolean HTMLstyle) {
    if (HTMLstyle) {
      return "KEGGtranslator: visualizing and converting the KEGG PATHWAY database to various formats. Wrzodek C, Dr&#228;ger A, Zell A.<i>Bioinformatics</i>. 2011, <b>27</b>:2314-2315";
    } else {
      return "KEGGtranslator: visualizing and converting the KEGG PATHWAY database to various formats. Wrzodek C, Dr&#228;ger A, Zell A. Bioinformatics. 2011, 27:2314-2315";
    }
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#getCmdLineOptions()
   */
  @Override
  public List<Class<? extends KeyProvider>> getCmdLineOptions() {
    List<Class<? extends KeyProvider>> configList = new ArrayList<Class<? extends KeyProvider>>(3);
    configList.add(KEGGtranslatorIOOptions.class);
    configList.add(KEGGtranslatorCommandLineOnlyOptions.class);
    configList.add(KEGGtranslatorOptions.class);
    configList.add(GUIOptions.class);
    if (isGarudaEnabled()) {
      configList.add(GarudaOptions.class);
    }
    return configList;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#getInteractiveOptions()
   */
  @Override
  public List<Class<? extends KeyProvider>> getInteractiveOptions() {
    // Return NULL here to only show options as dialog, that
    // are defined in de.zbit.gui.prefs.PreferencePanels
    
    // All options here are made persistent, in contrast to getCmdLineOptions()
    List<Class<? extends KeyProvider>> configList = new ArrayList<Class<? extends KeyProvider>>(3);
    configList.add(KEGGtranslatorIOOptions.class);
    configList.add(KEGGtranslatorOptions.class);
    configList.add(KEGGTranslatorPanelOptions.class);
    //configList.add(GUIOptions.class);
    return configList;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#getLogLevel()
   */
  @Override
  public Level getLogLevel() {
    return Level.INFO;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#getLogPackages()
   */
  @Override
  public String[] getLogPackages() {
    return new String[] {"de.zbit", "org.sbml"};
  }
  
  /*
   * (non-Javadoc)
   * @see de.zbit.Launcher#getURLlicenseFile()
   */
  @Override
  public URL getURLlicenseFile() {
    URL url = null;
    try {
      url = new URL("http://www.gnu.org/licenses/lgpl-3.0-standalone.html");
    } catch (MalformedURLException exc) {
      logger.log(Level.FINE, getMessage(exc), exc);
    }
    return url;
  }
  
  /*
   * (non-Javadoc)
   * @see de.zbit.Launcher#getURLOnlineUpdate()
   */
  @Override
  public URL getURLOnlineUpdate() {
    URL url = null;
    try {
      url = new URL("http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator/downloads/");
    } catch (MalformedURLException e) {
      logger.log(Level.FINE, e.getLocalizedMessage(), e);
    }
    return url;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#getVersionNumber()
   */
  @Override
  public String getVersionNumber() {
    return "2.5";
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#getYearOfProgramRelease()
   */
  @Override
  public short getYearOfProgramRelease() {
    return (short) 2015;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#getYearWhenProjectWasStarted()
   */
  @Override
  public short getYearWhenProjectWasStarted() {
    return (short) 2010;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#initGUI(de.zbit.AppConf)
   */
  @Override
  public java.awt.Window initGUI(AppConf appConf) {
    return new TranslatorUI(appConf);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#isGarudaEnabled()
   */
  @Override
  public boolean isGarudaEnabled() {
    return true;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#showsGUI()
   */
  @Override
  public boolean showsGUI() {
    SBProperties props = getCommandLineArgs();
    boolean showGUI = (props.size() < 1) || props.getBooleanProperty(GUIOptions.GUI);
    if (!showGUI) {
      // Check if an input file is given. This is required for and will trigger the command-line mode.
      String inputFile = props.getProperty(KEGGtranslatorIOOptions.INPUT);
      if ((inputFile == null) || (inputFile.length() < 1)) {
        showGUI = true;
      }
    }
    return showGUI;
  }
  
}
