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
package de.zbit.kegg.io;

import java.io.File;
import java.util.List;

import de.zbit.io.SBFileFilter;
import de.zbit.kegg.KeggInfoManagement;
import de.zbit.kegg.Translator;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.util.DirectoryParser;


/**
 * Translate multiple KGML files to the desired
 * {@link KEGGtranslatorIOOptions#FORMAT}.
 * 
 * @author Clemens Wrzodek
 * @since 1.0
 * @version $Rev$
 */
public class BatchKEGGtranslator {
  
  /**
   * 
   */
  private String changeOutdirTo = "";
  /**
   * 
   */
  private String orgOutdir = "";
  /**
   * Possible: SBML & GraphML. Default to GraphML
   */
  private Format outFormat = Format.GraphML;
  
  /**
   * The actual translator that is used for the translation.
   * Will be initialized with {@link #outFormat} and
   * {@link KeggInfoManagement}.
   */
  private KEGGtranslator translator;
  
  /**
   * 
   * @param dir
   * @return
   */
  private String getAndCreateOutDir(String dir) {
    String myDir = dir;
    if ((changeOutdirTo != null) && (changeOutdirTo.length() > 0)) {
      myDir = changeOutdirTo + myDir.substring(orgOutdir.length());
      try {
        new File(myDir).mkdirs();
      } catch (Exception e) {} // already existing...
    }
    return myDir;
  }
  
  /**
   * 
   * @param args
   */
  public static void main(String args[]) {
    BatchKEGGtranslator batch = new BatchKEGGtranslator();
    if (args != null && args.length > 0) {
      batch.setOrgOutdir(args[0]);
      if (args.length > 1)
        batch.setChangeOutdirTo(args[1]);
      batch.parseDirAndSubDir();
      return;
    }
    System.out.println("Demo Mode:");
    batch.setOrgOutdir(System.getProperty("user.home"));
    batch.setChangeOutdirTo(System.getProperty("user.home"));
    batch.parseDirAndSubDir();
  }
  
  /**
   * 
   * @return
   */
  public String getChangeOutdirTo() {
    return changeOutdirTo;
  }
  
  /**
   * 
   * @return
   */
  public KEGGtranslator getConverter() {
    return translator;
  }
  
  /**
   * 
   * @return
   */
  public String getOrgOutdir() {
    return orgOutdir;
  }
  
  /**
   * 
   * @return
   */
  public Format getOutFormat() {
    return outFormat;
  }
  
  /**
   * If called without a specific directory, uses
   * the {@link #orgOutdir} as input directory.
   */
  public void parseDirAndSubDir() {
    parseDirAndSubDir(orgOutdir);
  }
  
  /**
   * Automatic batch translation of all KGML files in dir
   * and all subdirectories.
   * @param dir
   */
  private void parseDirAndSubDir(String dir) {
    KeggInfoManagement manager = Translator.getManager();
    
    if (!dir.endsWith("/") && !dir.endsWith("\\"))
      if (dir.contains("\\")) dir+="\\"; else dir +="/";
    System.out.println("Parsing directory " + dir);
    
    
    if (translator==null) {
    	translator = getTranslator(outFormat, manager);
    }
    String fileExtension = getFileExtension(translator);
		  
    
    DirectoryParser dp = new DirectoryParser(dir);
    while (dp.hasNext()) {
      String fn = dp.next();
      File inFile = new File(dir+fn);
      
      //if (fn.equals("gml")|| fn.equals("metabolic")) continue;
      
      if (inFile.isDirectory()) {
        parseDirAndSubDir(dir + fn);
        
      } else if (SBFileFilter.isKGML(inFile)) {
        // Test if outFile already exists. Assumes: 1 Pathway per file. (should be true for all files... not crucial if assumption is wrong)
        String myDir = getAndCreateOutDir(dir);
        String outFileTemp = myDir + fn.trim().substring(0, fn.trim().length()-4) + fileExtension;
        if (new File(outFileTemp).exists()) {
          System.out.println("Skipping '"+inFile+"' file already exists.");
          continue; // Skip already converted files.
        } else {
          System.out.println("Converting '"+inFile+"' ..."); 
        }
        
        // Parse and convert all Pathways in XML file.
        List<Pathway> pw=null;
        try {
          pw = de.zbit.kegg.parser.KeggParser.parse(dir+fn);
        } catch (Throwable t) {t.printStackTrace();} // Show must go on...
        if (pw==null || pw.size()<1) continue;
        
        boolean appendNumber=(pw.size()>1);
        for (int i=0; i<pw.size(); i++) {
          String outFile = myDir + fn.trim().substring(0, fn.trim().length()-4) + (appendNumber?"-"+(i+1):"") + fileExtension;
          if (new File(outFile).exists()) continue; // Skip already converted files.
          
          // XXX: Main Part
          try {
            translator.translate(pw.get(i), outFile);
          } catch (Exception e) {
            e.printStackTrace();
          }
          
          if (translator.isLastFileWasOverwritten()) { // Datei war oben noch nicht da, spaeter aber schon => ein anderer prozess macht das selbe bereits.
            System.out.println("It looks like another instance is processing the same files. Going to next subfolder.");
            return; // Function is recursive.
          }
        }
        
        
      }
    }
    
    // Remember already queried objects (save cache)
    Translator.saveCache();
  }

  /**
   * Returns a KeggTranslater for the given outFormat.
   * @param outFormat
   * @param manager
   * @return
   */
	public static KEGGtranslator getTranslator(Format outFormat,
			KeggInfoManagement manager) {
		KEGGtranslator translator;
		switch (outFormat) {
		case SBML:
			translator = new KEGG2jSBML(manager);
			break;
    case SBML_QUAL:
      translator = new KEGG2SBMLqual(manager);
      break;
		case LaTeX:
			translator = new KEGG2jSBML(manager);
			break;
		case GraphML:
			translator = KEGG2yGraph.createKEGG2GraphML(manager);
			break;
		case GML:
			translator = KEGG2yGraph.createKEGG2GML(manager);
			break;
		case JPG:
			translator = KEGG2yGraph.createKEGG2JPG(manager);
			break;
		case GIF:
			translator = KEGG2yGraph.createKEGG2GIF(manager);
			break;
//    case SVG:
//      translator = KEGG2yGraph.createKEGG2SVG(manager);
//      break;
		case YGF:
			translator = KEGG2yGraph.createKEGG2YGF(manager);
			break;
		case TGF:
			translator = KEGG2yGraph.createKEGG2TGF(manager);
			break;
		case SBGN:
			translator  = new KEGG2SBGN(manager);
			break;
		default:
			System.err.println("Unknwon output Format: '" + outFormat + "'.");
			translator = null;
			break;
		}
		return translator;
	}
	
	/**
	 * Returns the file extesion (with preceding dot) for the
	 * given KEGGtranslator.
	 * @param translator
	 * @return
	 */
	public static String getFileExtension(KEGGtranslator translator) {
		String fileExtension = ".translated";
    if (translator instanceof KEGG2yGraph) {
      fileExtension = ((KEGG2yGraph)translator).getOutputHandler().getFileNameExtension();
    } else if (translator instanceof KEGG2jSBML) {
		  fileExtension = ".sbml.xml";
    }
		if (!fileExtension.startsWith(".")) fileExtension = "." + fileExtension;
		
		return fileExtension;
	}
  
  /**
   * 
   * @param changeOutdirTo
   */
  public void setChangeOutdirTo(String changeOutdirTo) {
    this.changeOutdirTo = changeOutdirTo;
  }
  
  /**
   * Set the translator you wish to use. This will determine the
   * output format of this class.
   * @param translator
   */
  public void setTranslator(KEGGtranslator translator) {
    this.translator = translator;
  }
  
  /**
   * 
   * @param orgOutdir
   */
  public void setOrgOutdir(String orgOutdir) {
    this.orgOutdir = orgOutdir;
  }
  
  /**
   * @param outFormat - "graphml" or "sbml".
   */
  public void setOutFormat(Format outFormat) {
    this.outFormat = outFormat;
  }
  
}
