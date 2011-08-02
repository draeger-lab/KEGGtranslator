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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;

import javax.swing.SwingUtilities;

import de.zbit.gui.GUIOptions;
import de.zbit.gui.GUITools;
import de.zbit.kegg.ext.TranslatorPanelOptions;
import de.zbit.kegg.gui.TranslatorUI;
import de.zbit.kegg.io.AbstractKEGGtranslator;
import de.zbit.kegg.io.BatchKEGGtranslator;
import de.zbit.kegg.io.KEGGtranslator;
import de.zbit.kegg.io.KEGGtranslatorIOOptions;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.util.logging.LogUtil;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.util.prefs.SBProperties;

/**
 * This class is the main class for the KEGGtranslator project.
 * 
 * <p>Recommended VM-Arguments:
 * "-splash:bin/de/zbit/kegg/gui/img/Logo.gif -Duser.language=en -Duser.country=US"
 * 
 * @author Andreas Dr&auml;ger
 * @author Clemens Wrzodek
 * @date 2010-10-25
 * @since 1.0
 * @version $Rev$
 */
public class Translator {
  
  /**
   * Filename of the KEGG cache file (implemented just
   * like the browser cache). Must be loaded upon start
   * and saved upon exit.
   */
  public final static String cacheFileName = "keggdb.dat";
  
  /**
   * Filename of the KEGG function cache file (implemented just
   * like the browser cache). Must be loaded upon start
   * and saved upon exit.
   */  
  public final static String cacheFunctionFileName = "keggfc.dat";
  
  /**
   * The name of the application.
   * Removed the final attribute, such that referencing applications can still change this.
   */
  public static String APPLICATION_NAME = "KEGGtranslator";
  
  /**
   * Version number of this translator
   */
  public static final String VERSION_NUMBER = "1.2.0";
  
  
  /**
   * The cache to be used by all KEGG interacting classes.
   * Access via {@link #getManager()}.
   */
  private static KeggInfoManagement manager=null;
  
  /**
   * The cache to be used by all KEGG-Functions interacting classes.
   * Access via {@link #getFunctionManager()}.
   */
  private static KeggFunctionManagement managerFunction=null;
  
	/**
	 * @param args
	 * @throws BackingStoreException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static void main(String[] args) throws IOException,
		BackingStoreException, URISyntaxException {
	  LogUtil.initializeLogging(Level.FINE);
		// --input files/KGMLsamplefiles/hsa00010.xml --format GraphML --output test.txt
	  //Locale.setDefault(Locale.US);
	  GUIOptions.GUI.setDefaultValue(Boolean.FALSE);
	  
	  // Just for dependency demonstration.
	  TranslatorPanelOptions.SHOW_PROPERTIES_TABLE.addDependency(TranslatorPanelOptions.SHOW_NAVIGATION_AND_OVERVIEW_PANELS, Boolean.TRUE);
	  
		SBProperties props = SBPreferences.analyzeCommandLineArguments(
				getCommandLineOptions(), args);
		
		/*if (props.containsKey(GUIOptions.LANGUAGE)) {
			String userLanguage = props.get(GUIOptions.LANGUAGE);
			if (!userLanguage.equals(System.getProperty("user.language"))) {
				Locale.setDefault(new Locale(userLanguage));
			}
		}*/
		
		// Should we start the GUI?
		if ((args.length < 1) || props.getBooleanProperty(GUIOptions.GUI)) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					TranslatorUI ui = new TranslatorUI();
					ui.setVisible(true);
					GUITools.hideSplashScreen();
					ui.toFront();
				}
			});
		} else {
			translate(KEGGtranslatorIOOptions.FORMAT.getValue(props),
					props.get(KEGGtranslatorIOOptions.INPUT),
					props.get(KEGGtranslatorIOOptions.OUTPUT));
			GUITools.hideSplashScreen();
		}
	}

  /**
	 * @return
	 */
	public static List<Class<? extends KeyProvider>> getCommandLineOptions() {
		List<Class<? extends KeyProvider>> configList = new LinkedList<Class<? extends KeyProvider>>();
		configList.add(KEGGtranslatorIOOptions.class);
		configList.add(KEGGtranslatorOptions.class);
		configList.add(GUIOptions.class);
		return configList;
	}

	/**
	 * @throws MalformedURLException 
	 * 
	 */
	public static URL getURLOnlineUpdate() throws MalformedURLException {
		return new URL("http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator/downloads/");
	}
	
	/**
	 * 
	 * @param format - currently one of {SBML,LaTeX,GraphML,GML,JPG,GIF,TGF,YGF}.
	 * @param input - input file
	 * @param output - output file
	 * @return
	 * @throws IOException
	 */
	public static boolean translate(Format format, String input, String output)
		throws IOException {
		
		// Check and build input
		File in = input==null?null:new File(input);
		if (in==null || !in.canRead()) { // in might also be a directory
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
		if (!in.isDirectory()) { // else: batch-mode
		  if ((out == null) || (output.length() < 1) || out.isDirectory()) {
		    String fileExtension = BatchKEGGtranslator.getFileExtension(translator);
		    out = new File(removeFileExtension(input) + fileExtension);
		    
		    System.out.println("Writing to " + out);
		  }
		  
		  // Further check out
		  if (out.exists()) {
		    System.out.println("Overwriting exising file " + out);
		  }
		  out.createNewFile();
		  if (!out.canWrite()) {
		    System.err.println("Cannot write to file " + out);
		    return false;
		  }
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
  if (in==null || !in.canRead() || in.isDirectory()) {
    System.err.println("Invalid or not-readable input file.");
    return null;
  }
  
  // Initiate the manager
  KeggInfoManagement manager = getManager();
  
  // Check and build format
  AbstractKEGGtranslator<?> translator = (AbstractKEGGtranslator<?>) BatchKEGGtranslator.getTranslator(format, manager);
  if (translator == null) return false; // Error message already issued.
  
  // Translate.
  return translator.translate(in);
}
	
	/**
	 * 
	 * @return
	 */
	public synchronized static KeggInfoManagement getManager() {
	  
	  // Try to load from cache file
		if (manager==null && new File(Translator.cacheFileName).exists() && new File(Translator.cacheFileName).length() > 1) {
			try {
				manager = (KeggInfoManagement) KeggInfoManagement.loadFromFilesystem(Translator.cacheFileName);
			} catch (Throwable e) { // IOException or class cast, if class is moved.
			  e.printStackTrace();
			  // Delete invalid cache file
			  try {
			    File f = new File(Translator.cacheFileName);
			    if (f.exists() && f.canRead()) {
			      System.out.println("Deleting invalid cache file " + f.getName());
			      f.delete();
			    }
			  } catch (Throwable t) {}
			  
			}
		}
		
		// Create new, if loading failed
		if (manager==null) {
			manager = new KeggInfoManagement(5000);
		}
		
		return manager;
	}
	
	 public synchronized static KeggFunctionManagement getFunctionManager() {
	    
	    // Try to load from cache file
	    if (managerFunction==null && new File(Translator.cacheFunctionFileName).exists() && new File(Translator.cacheFunctionFileName).length() > 1) {
	      try {
	        managerFunction = (KeggFunctionManagement) KeggFunctionManagement.loadFromFilesystem(Translator.cacheFunctionFileName);
	      } catch (Throwable e) { // IOException or class cast, if class is moved.
	        e.printStackTrace();
	        
	        // Delete invalid cache file
	        try {
	          File f = new File(Translator.cacheFunctionFileName);
	          if (f.exists() && f.canRead()) {
	            System.out.println("Deleting invalid cache file " + f.getName());
	            f.delete();
	          }
	        } catch (Throwable t) {}
	      }
	    }
	    
	    // Create new, if loading failed
	    if (managerFunction==null) {
	      managerFunction = new KeggFunctionManagement(5000);
	    }
	    
	    return managerFunction;
	  }
	
	/**
	 * Remember already queried KEGG objects (save cache)
	 */
	public synchronized static void saveCache() {
    if (manager!=null && manager.hasChanged()) {
      KeggInfoManagement.saveToFilesystem(Translator.cacheFileName, manager);
    }
    if (managerFunction!=null && managerFunction.isCacheChangedSinceLastLoading()) {
      KeggFunctionManagement.saveToFilesystem(Translator.cacheFunctionFileName, managerFunction);
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
