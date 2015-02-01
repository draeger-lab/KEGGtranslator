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
package de.zbit.kegg.io;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import de.zbit.graph.io.Graph2Dwriteable.WriteableFileExtensions;
import de.zbit.graph.io.Graph2Dwriter;
import de.zbit.graph.io.SBGN2GraphML;
import de.zbit.graph.io.SBML2GraphML;
import de.zbit.io.DirectoryParser;
import de.zbit.io.FileTools;
import de.zbit.io.SerializableTools;
import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.kegg.KEGGtranslatorCommandLineOnlyOptions;
import de.zbit.kegg.Translator;
import de.zbit.kegg.api.cache.KeggInfoManagement;
import de.zbit.kegg.gui.TranslatorPanelTools;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.util.prefs.SBPreferences;

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
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(BatchKEGGtranslator.class.getName());
  
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
  private KEGGtranslator<?> translator;
  
  /**
   * Load preferences only once when {@link BatchKEGGtranslator} is started.
   */
  SBPreferences prefs = SBPreferences.getPreferencesFor(KEGGtranslatorCommandLineOnlyOptions.class);
  
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
      if (args.length > 1) {
        batch.setChangeOutdirTo(args[1]);
      }
      batch.parseDirAndSubDir();
      return;
    }
    logger.info("Demo Mode:");
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
  public KEGGtranslator<?> getConverter() {
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
    
    if (!dir.endsWith("/") && !dir.endsWith("\\")) {
      if (dir.contains("\\")) {
        dir+="\\";
      } else {
        dir +="/";
      }
    }
    logger.info("Parsing directory " + dir);
    
    
    if (translator == null) {
      translator = getTranslator(outFormat, manager);
    }
    String fileExtension = getFileExtension(translator);
    
    
    DirectoryParser dp = new DirectoryParser(dir);
    while (dp.hasNext()) {
      String fn = dp.next();
      File inFile = new File(dir+fn);
      
      //if (fn.equals("gml")|| fn.equals("metabolic")) continue;
      
      if (inFile.isDirectory()) {
        inFile = null; // There are errors when parsing large dirs "too many open files".
        parseDirAndSubDir(dir + fn);
        
      } else {
        
        // Maybe we have a serialized pathway
        // (This is just used by us...)
        Object loaded = null;
        try {
          loaded = SerializableTools.loadObjectAutoDetectZIP(inFile);
          if (!(loaded instanceof Pathway)) {
            loaded = null;
          }
        } catch (Exception e1) {}
        
        if (loaded!=null || SBFileFilter.isKGML(inFile)) {
          // Test if outFile already exists. Assumes: 1 Pathway per file. (should be true for all files... not crucial if assumption is wrong)
          String myDir = getAndCreateOutDir(dir);
          String outFileTemp = myDir + FileTools.removeFileExtension(fn) + fileExtension;
          if (new File(outFileTemp).exists()) {
            logger.info("Skipping '"+inFile+"' file already exists.");
            continue; // Skip already converted files.
          } else {
            logger.info("Converting '"+inFile+"' ...");
          }
          
          // Parse and convert all Pathways in XML file.
          List<Pathway> pw=null;
          if (loaded!=null) {
            pw = new LinkedList<Pathway>();
            pw.add((Pathway) loaded);
          } else {
            try {
              pw = de.zbit.kegg.parser.KeggParser.parse(dir+fn);
            } catch (Throwable t) {t.printStackTrace();} // Show must go on...
          }
          if (pw == null || pw.size()<1) {
            continue;
          }
          
          boolean appendNumber=(pw.size()>1);
          for (int i = 0; i<pw.size(); i++) {
            String outFile = myDir + fn.trim().substring(0, fn.trim().length()-4) + (appendNumber?"-"+(i+1):"") + fileExtension;
            if (new File(outFile).exists())
            {
              continue; // Skip already converted files.
            }
            
            // XXX: Main Part
            try {
              if (KEGGtranslatorCommandLineOnlyOptions.CREATE_JPG.getValue(prefs)) {
                // Translate, but create image from translated document
                Object translateDoc = translator.translate(pw.get(i));
                writeAsJPG(translateDoc, pw.get(i), outFile, outFormat);
                
              } else {
                // Translate to output file
                translator.translate(pw.get(i), outFile);
              }
              
            } catch (Exception e) {
              e.printStackTrace();
            }
            
            if (translator.isLastFileWasOverwritten()) { // Datei war oben noch nicht da, spaeter aber schon => ein anderer prozess macht das selbe bereits.
              logger.warning("It looks like another instance is processing the same files. Going to next subfolder.");
              return; // Function is recursive.
            }
          }
          
          
        }
      }
    }
    
    // Remember already queried objects (save cache)
    Translator.saveCache();
  }
  
  /**
   * @param translatedDoc translated pathway
   * @param originalPW original and untranslated pathway
   * @param outFile file to write
   * @param outFormat user selected output format
   * @return {@code true} if a JPG has been successfully written.
   * @throws Exception if something went wrong or a required library is not available.
   */
  private boolean writeAsJPG(Object translatedDoc, Pathway originalPW, String outFile, Format outFormat) throws Exception {
    if (translatedDoc==null) {
      return false;
    }
    
    outFile = FileTools.removeFileExtension(outFile) + ".jpg";
    Graph2Dwriter writer = new Graph2Dwriter(WriteableFileExtensions.jpg);
    TranslatorPanelTools.setupBackgroundImage(writer);
    Object myGraph = null; // actually a Graph2D object
    
    // NOTE: we should at all costs avoid imports from yFiles, JSBML or other
    // libraries here!
    
    switch (outFormat) {
      // BioPAX should be redirected to default:
      //      case BioPAX_level2:
      //      case BioPAX_level3:
      
      case GIF:
      case GML:
      case GraphML:
      case JPG:
      case TGF:
      case YGF:
        myGraph = translatedDoc;
        break;
        
      case SBGN:
        myGraph = new SBGN2GraphML().createGraph((org.sbgn.bindings.Sbgn) translatedDoc);
        break;
        
      case SBML:
      case SBML_L2V4:
      case SBML_L3V1:
        myGraph = new SBML2GraphML().createGraph((org.sbml.jsbml.SBMLDocument) translatedDoc);
        break;
        
      case SBML_QUAL:
        myGraph = new SBML2GraphML(true).createGraph((org.sbml.jsbml.SBMLDocument) translatedDoc);
        break;
        
      case SBML_CORE_AND_QUAL:
        // Create 2 files
        myGraph = new SBML2GraphML().createGraph((org.sbml.jsbml.SBMLDocument) translatedDoc);
        
        // Write qual_graph immediately
        Object myGraph2 = new SBML2GraphML(true).createGraph((org.sbml.jsbml.SBMLDocument) translatedDoc);
        writer.writeToFile((y.view.Graph2D)myGraph2, FileTools.removeFileExtension(outFile) + "SBML_QUAL.jpg");
        break;
        
      default:
        // Simply translate PW to graph and ignore all formats
        myGraph = new KEGG2yGraph(writer.getOutputHandler()).translate(originalPW);
        break;
    }
    
    return writer.writeToFile((y.view.Graph2D)myGraph, outFile);
  }
  
  /**
   * Returns a KeggTranslater for the given outFormat.
   * @param outFormat
   * @param manager
   * @return
   */
  public static KEGGtranslator<?> getTranslator(Format outFormat, KeggInfoManagement manager) {
    KEGGtranslator<?> translator;
    switch (outFormat) {
      case SBML:
        translator = new KEGG2jSBML(manager);
        break;
      case SBML_L2V4:
        translator = new KEGG2jSBML(manager, 2, 4);
        break;
      case SBML_L3V1:
        translator = new KEGG2jSBML(manager, 3, 1);
        break;
      case SBML_QUAL:
        translator = new KEGG2SBMLqual(manager);
        break;
      case SBML_CORE_AND_QUAL:
        translator = new KEGG2SBMLqual(manager);
        ((KEGG2SBMLqual)translator).setConsiderReactions(true);
        break;
        
        /*case LaTeX:
			translator = new KEGG2jSBML(manager);
			break;*/
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
      case BioPAX_level2: case SIF:
        translator = new KEGG2BioPAX_level2(manager);
        break;
      case BioPAX_level3:
        translator = new KEGG2BioPAX_level3(manager);
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
  public static String getFileExtension(KEGGtranslator<?> translator) {
    String fileExtension = ".translated";
    if (translator instanceof KEGG2yGraph) {
      fileExtension = ((KEGG2yGraph)translator).getWriter().getOutputHandler().getFileNameExtension();
    } else if (translator instanceof KEGG2jSBML) {
      fileExtension = ".sbml.xml";
    }
    if (!fileExtension.startsWith(".")) {
      fileExtension = "." + fileExtension;
    }
    
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
  public void setTranslator(KEGGtranslator<?> translator) {
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
