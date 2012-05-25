/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of KEGGtranslator, a program to convert KGML files
 * from the KEGG database into various other formats, e.g., SBML, GML,
 * GraphML, and many more. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator> to
 * obtain the latest version of KEGGtranslator.
 *
 * Copyright (C) 2011 by the University of Tuebingen, Germany.
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

import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;

import de.zbit.AppConf;
import de.zbit.Launcher;
import de.zbit.gui.GUIOptions;
import de.zbit.io.FileTools;
import de.zbit.kegg.api.cache.KeggFunctionManagement;
import de.zbit.kegg.api.cache.KeggInfoManagement;
import de.zbit.kegg.ext.KEGGTranslatorPanelOptions;
import de.zbit.kegg.gui.TranslatorUI;
import de.zbit.kegg.io.AbstractKEGGtranslator;
import de.zbit.kegg.io.BatchKEGGtranslator;
import de.zbit.kegg.io.KEGGtranslator;
import de.zbit.kegg.io.KEGGtranslatorIOOptions;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
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
 * @version $Rev$
 */
public class Translator extends Launcher {
	
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
	private static final transient Logger log = Logger.getLogger(Translator.class.getName());

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
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = -428595670090648615L;
	
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
				managerFunction = (KeggFunctionManagement) KeggFunctionManagement
						.loadFromFilesystem(Translator.cacheFunctionFileName);
			} catch (Throwable e) { // IOException or class cast, if class is moved.
				e.printStackTrace();
				managerFunction = null;
				
				// Delete invalid cache file
				try {
					File f = new File(Translator.cacheFunctionFileName);
					if (f.exists() && f.canRead()) {
						log.info(String.format("Deleting invalid cache file %s.", f
								.getName()));
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
	  // Try to load from cache file
		if ((manager == null) && new File(Translator.cacheFileName).exists() && new File(Translator.cacheFileName).length() > 1) {
			try {
				manager = (KeggInfoManagement) KeggInfoManagement.loadFromFilesystem(Translator.cacheFileName);
			} catch (Throwable e) { // IOException or class cast, if class is moved.
			  e.printStackTrace();
			  manager = null;
			  // Delete invalid cache file
			  try {
			    File f = new File(Translator.cacheFileName);
			    if (f.exists() && f.canRead()) {
						log.info(String.format("Deleting invalid cache file %s.", f
								.getName()));
			      f.delete();
			    }
			  } catch (Throwable t) {}
			  
			}
		}
		
		// Create new, if loading failed
		if (manager == null) {
			manager = new KeggInfoManagement(10000);
		}
		
		// Set cache size and eventually remove some items from the cache
    int initialSize=-1;
    try {
      SBPreferences prefs = SBPreferences.getPreferencesFor(KEGGtranslatorCommandLineOnlyOptions.class);
      initialSize = KEGGtranslatorCommandLineOnlyOptions.CACHE_SIZE.getValue(prefs);
      
      if (KEGGtranslatorCommandLineOnlyOptions.CLEAR_FAIL_CACHE.getValue(prefs)) {
        log.info("Clearing cache of failed-to-retrieve objects.");
        manager.clearFailCache();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (initialSize<=0) {
      initialSize = 10000;
    }
    manager.setCacheSize(initialSize);


		
		return manager;
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
			log.severe("Invalid or not-readable input file.");
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
     KeggFunctionManagement.saveToFilesystem(Translator.cacheFunctionFileName, managerFunction);
   }
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
			log.severe("Invalid or not-readable input file.");
			return false;
		}
		
		// Initiate the manager
		KeggInfoManagement manager = getManager();
		
		// Check and build format
		KEGGtranslator translator = BatchKEGGtranslator.getTranslator(format, manager);
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
				log.info(String.format("Writing to %s.", out));
		  }
		  
		  // Further check out
		  if (out.exists()) {
		    log.info(String.format("Overwriting exising file %s.", out));
		  }
		  out.createNewFile();
		  if (!out.canWrite()) {
		    log.severe(String.format("Cannot write to file %s.", out));
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

	/*
	 * (non-Javadoc)
	 * @see de.zbit.Launcher#commandLineMode(de.zbit.AppConf)
	 */
	public void commandLineMode(AppConf appConf) {
		SBProperties props = appConf.getCmdArgs();
		
		// Make command-line options persistent
		SBPreferences prefs = SBPreferences.getPreferencesFor(KEGGtranslatorCommandLineOnlyOptions.class);
		prefs.restoreDefaults();
		try {
		  prefs.saveContainedOptions(props);
		} catch (BackingStoreException e) {
		  log.log(Level.WARNING, "Could not process command-line-only options.", e);
		}
		
		// Initiate translation
		try {
			translate(KEGGtranslatorIOOptions.FORMAT.getValue(props),
				props.get(KEGGtranslatorIOOptions.INPUT),
				props.get(KEGGtranslatorIOOptions.OUTPUT));
		} catch (IOException exc) {
			log.warning(exc.getLocalizedMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see de.zbit.Launcher#getAppName()
	 */
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

	/*
	 * (non-Javadoc)
	 * @see de.zbit.Launcher#getCmdLineOptions()
	 */
	public List<Class<? extends KeyProvider>> getCmdLineOptions() {
		List<Class<? extends KeyProvider>> configList = new ArrayList<Class<? extends KeyProvider>>(3);
		configList.add(KEGGtranslatorIOOptions.class);
		configList.add(KEGGtranslatorCommandLineOnlyOptions.class);
		configList.add(KEGGtranslatorOptions.class);
		configList.add(GUIOptions.class);
		return configList;
	}

	/*
	 * (non-Javadoc)
	 * @see de.zbit.Launcher#getInteractiveOptions()
	 */
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

	/*
	 * (non-Javadoc)
	 * @see de.zbit.Launcher#getLogPackages()
	 */
	public String[] getLogPackages() {
		return new String[] {"de.zbit", "org.sbml"};
	}
	
	/* (non-Javadoc)
	 * @see de.zbit.Launcher#getLogLevel()
	 */
	@Override
	public Level getLogLevel() {
	  return Level.INFO;
	}

	/*
	 * (non-Javadoc)
	 * @see de.zbit.Launcher#getURLlicenseFile()
	 */
	public URL getURLlicenseFile() {
		URL url = null;
    try {
      url = new URL("http://www.gnu.org/licenses/lgpl-3.0-standalone.html");
    } catch (MalformedURLException exc) {
      log.log(Level.FINE, exc.getLocalizedMessage(), exc);
    }
    return url;
	}
	
	/*
	 * (non-Javadoc)
	 * @see de.zbit.Launcher#getURLOnlineUpdate()
	 */
	public URL getURLOnlineUpdate() {
		URL url = null;
    try {
      url = new URL("http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator/downloads/");
    } catch (MalformedURLException e) {
      log.log(Level.FINE, e.getLocalizedMessage(), e);
    }
    return url;
	}

	/*
	 * (non-Javadoc)
	 * @see de.zbit.Launcher#getVersionNumber()
	 */
	public String getVersionNumber() {
		return "2.0.0";
	}

	/*
	 * (non-Javadoc)
	 * @see de.zbit.Launcher#getYearOfProgramRelease()
	 */
	public short getYearOfProgramRelease() {
		return (short) 2011;
	}

	/*
	 * (non-Javadoc)
	 * @see de.zbit.Launcher#getYearWhenProjectWasStarted()
	 */
	public short getYearWhenProjectWasStarted() {
		return (short) 2010;
	}
	
	/* (non-Javadoc)
	 * @see de.zbit.Launcher#addCopyrightToSplashScreen()
	 */
	@Override
	protected boolean addCopyrightToSplashScreen() {
	  return false;
	}

	/*
	 * (non-Javadoc)
	 * @see de.zbit.Launcher#initGUI(de.zbit.AppConf)
	 */
	public Window initGUI(AppConf appConf) {
		return new TranslatorUI(appConf);
	}
  	
}
