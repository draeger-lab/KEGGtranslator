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
import java.util.prefs.BackingStoreException;

import javax.swing.SwingUtilities;

import de.zbit.gui.GUIOptions;
import de.zbit.gui.GUITools;
import de.zbit.kegg.gui.TranslatorUI;
import de.zbit.kegg.io.BatchKEGGtranslator;
import de.zbit.kegg.io.KEGGtranslator;
import de.zbit.kegg.io.KEGGtranslatorIOOptions;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.util.prefs.KeyProvider;
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
		// --input files/KGMLsamplefiles/hsa00010.xml --format GraphML --output test.txt
	  //Locale.setDefault(Locale.US);
	  
		SBProperties props = SBPreferences.analyzeCommandLineArguments(
				getCommandLineOptions(), args);
		/*if (props.containsKey(GUIOptions.LANGUAGE)) {
			String userLanguage = props.get(GUIOptions.LANGUAGE);
			if (!userLanguage.equals(System.getProperty("user.language"))) {
				Locale.setDefault(new Locale(userLanguage));
			}
		}*/
		
		//		KeggInfoManagement manager = getManager();
		//		k2s = new KEGG2jSBML(manager);
		
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
	  // TODO: Nach cogSys ändern.
		return new URL("http://www.ra.cs.uni-tuebingen.de/software/KEGGtranslator/downloads/");
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
		if ((out == null) || (output.length() < 1) || out.isDirectory()) {
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
			} catch (Throwable e) { // IOException or class cast, if class is moved.
				e.printStackTrace();
			}
		}
		
		// Create new, if loading failed
		if (manager==null) {
			manager = new KeggInfoManagement();
		}
		
		return manager;
	}
	
	 public synchronized static KeggFunctionManagement getFunctionManager() {
	    
	    // Try to load from cache file
	    if (managerFunction==null && new File(KEGGtranslator.cacheFunctionFileName).exists() && new File(KEGGtranslator.cacheFunctionFileName).length() > 0) {
	      try {
	        managerFunction = (KeggFunctionManagement) KeggFunctionManagement.loadFromFilesystem(KEGGtranslator.cacheFunctionFileName);
	      } catch (Throwable e) { // IOException or class cast, if class is moved.
	        e.printStackTrace();
	      }
	    }
	    
	    // Create new, if loading failed
	    if (managerFunction==null) {
	      managerFunction = new KeggFunctionManagement();
	    }
	    
	    return managerFunction;
	  }
	
	/**
	 * Remember already queried KEGG objects (save cache)
	 */
	public synchronized static void saveCache() {
    if (manager!=null && manager.hasChanged()) {
      KeggInfoManagement.saveToFilesystem(KEGGtranslator.cacheFileName, manager);
    }
    if (managerFunction!=null && managerFunction.isCacheChangedSinceLastLoading()) {
      KeggFunctionManagement.saveToFilesystem(KEGGtranslator.cacheFunctionFileName, managerFunction);
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
