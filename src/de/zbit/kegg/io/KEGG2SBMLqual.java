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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.AbstractNamedSBase;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.qual.Input;
import org.sbml.jsbml.ext.qual.InputTransitionEffect;
import org.sbml.jsbml.ext.qual.OutputTransitionEffect;
import org.sbml.jsbml.ext.qual.QualitativeModel;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Sign;
import org.sbml.jsbml.ext.qual.Transition;
import org.sbml.jsbml.util.ValuePair;
import org.sbml.jsbml.xml.stax.SBMLWriter;

import de.zbit.kegg.KeggInfoManagement;
import de.zbit.kegg.KeggInfos;
import de.zbit.kegg.Translator;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.kegg.parser.KeggParser;
import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.Graphics;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.kegg.parser.pathway.Relation;
import de.zbit.kegg.parser.pathway.SubType;
import de.zbit.util.ArrayUtils;
import de.zbit.util.Utils;

/**
 * KEGG2SBML with a qualitative model (SBML L3 V1, using the SBML Qual extension,
 * also KGML2JSBMLqual, KEGG2QUAL, KGML2QUAL).
 * 
 * @author Finja B&uuml;chel
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class KEGG2SBMLqual extends KEGG2jSBML {
  /**
   * Qual Namespace definition URL.
   */
  private static final String QUAL_NS = "http://www.sbml.org/sbml/level3/version1/qual/version1";
  
  /**
   * Unique identifier to identify this Namespace/Extension.
   */
  private static final String QUAL_NS_PREFIX = "qual";

  
  
  /*===========================
   * CONSTRUCTORS
   * ===========================*/
  
  /**
   * Initialize a new KEGG2SBMLqual object, using a new Cache and a new KeggAdaptor.
   */
  public KEGG2SBMLqual() {
    this(new KeggInfoManagement());
  }
  
  /**
   * Initialize a new Kegg2jSBML object, using the given cache.
   * @param manager
   */
  public KEGG2SBMLqual(KeggInfoManagement manager) {
    super(manager);
    // Important to manifest that we NEED the relations
    // see considerRealtions()
    
    loadPreferences();
  }
  
  
  /*===========================
   * FUNCTIONS
   * ===========================*/
  
  /** Load the default preferences from the SBPreferences object. */
  private void loadPreferences() {}
  
  /**
   * 
   * @return the level and version of the SBML core (2,4)
   */
  protected ValuePair<Integer, Integer> getLevelAndVersion() {
    return new ValuePair<Integer, Integer>(Integer.valueOf(3), Integer.valueOf(1));
  }
  
  @Override
  protected SBMLDocument translateWithoutPreprocessing(Pathway p) {
    // Translate to normal SBML
    SBMLDocument doc = super.translateWithoutPreprocessing(p);
    
    // Create qualitative model
    Model model = doc.getModel();
    QualitativeModel qualModel = new QualitativeModel(model);
   
    // Add extension and namespace to model
    doc.addNamespace(KEGG2SBMLqual.QUAL_NS_PREFIX, "xmlns", KEGG2SBMLqual.QUAL_NS);
    doc.getSBMLDocumentAttributes().put(QUAL_NS_PREFIX + ":required", "true");
    model.addExtension(KEGG2SBMLqual.QUAL_NS, qualModel);
    
    //Should be set to false or removed after everything is fine with layout extension
    if(true) {
      System.out.println(p.getName());
      String file = "matchingSpeciesLayout_" + p.getName().replace(":", "") + ".txt";
      System.out.println(file);
      try {        
        writeMatchingFile(file, p);
      } catch (IOException e) {
        log.log(Level.WARNING, "Could not write matching file: " + file, e);
      }
    }
    
    // Create qual species for every species
    createQualSpecies(p, qualModel);
    
    // Give a warning if we have no relations.
    if (p.getRelations().size()<1) {
      log.fine("File does not contain any relations. Graph will look quite boring...");
    } else {
      for (Relation r : p.getRelations()) {
        addKGMLRelation(r, p, qualModel);
      }  
    }
    
    if(!considerReactions()) {
      model.unsetListOfSpecies();
    }
    
    // Update (unset old and create new, qual-species related) layout extension
    if (addLayoutExtension) {
      KEGG2SBMLLayoutExtension.addLayoutExtension(p, doc, model);
    }
    
    
    
    return doc;
  }
  
  /**
   * 
   * for writing a matching file in the form 
   * species_id x y width height
   * 
   * the default is false;
   * @param entry
   * @param s
   * @throws IOException 
   */
  public static void writeMatchingFile(String fileName, Pathway p)
      throws IOException {
    BufferedWriter matchWriter = new BufferedWriter(new FileWriter(fileName));
    List<String> output = new LinkedList<String>();

    for (Entry entry : p.getEntries()) {
      Object s = entry.getCustom();
      if (s != null && s instanceof Species) {
        if (entry.hasGraphics()) {
          Graphics g = entry.getGraphics();
          output = new LinkedList<String>();
          output.add(((Species) s).getId());
          if (!g.isSetCoords()) {
            output.add(g.getX() + "");
            output.add(g.getY() + "");
            output.add(g.getWidth() + "");
            output.add(g.getHeight() + "");
          } else {
            output.add("coords");
            for (int i = 0; i < g.getCoords().length; i++) {
              output.add(g.getCoords()[i] + "");
            }
          }

          matchWriter.append(ArrayUtils.implode(output, "\t"));
          matchWriter.append('\n');
        }
      }
    }

    matchWriter.close();

  }
  
  /**
   * Creates a qual species for every entry in the pathway
   * (as a side effect, also for every species in the model).
   * @param model
   * @param qualModel
   */
  private void createQualSpecies(Pathway p, QualitativeModel qualModel) {
    for (Entry e: p.getEntries()) {
      Object s = e.getCustom();
      if (s!=null && s instanceof Species) {
        QualitativeSpecies qs = createQualitativeSpeciesFromSpecies((Species) s, qualModel);
        e.setCustom(qs);
      }
    }
  }

  public Transition addKGMLRelation(Relation r, Pathway p, QualitativeModel qualModel) {
    // create transition and add it to the model
    Transition t = qualModel.createTransition(NameToSId("tr"));

    QualitativeSpecies qOne = (QualitativeSpecies) p.getEntryForId(r.getEntry1()).getCustom();
    QualitativeSpecies qTwo = (QualitativeSpecies) p.getEntryForId(r.getEntry2()).getCustom();

    if (qOne==null || qTwo==null) {
      System.out.println("Relation with unknown entry!");
      return null;
    }
   
    // Input
    Input in = t.createInput(NameToSId("in"), qOne, InputTransitionEffect.none); //TODO: is this correct?
    in.setMetaId("meta_" + in.getId());

    // Output
    t.createOutput(NameToSId("out"), qTwo, OutputTransitionEffect.assignmentLevel); //TODO: is this correct?    
    
    //TODO: function term

    Sign sign = null;
    // Determine the sign variable
    List<SubType> subTypes = r.getSubtypes();
    if (subTypes != null && subTypes.size() > 0) {
      Collection<String> subTypeNames = r.getSubtypesNames();

      if (subTypeNames.contains(SubType.INHIBITION)) {
        in.setSBOTerm(169);
        sign = Sign.dual;
      } else if (subTypeNames.contains(SubType.ACTIVATION)) { //stimulation SBO:0000170
        sign = Sign.positive;
        in.setSBOTerm(170);
      } else if(subTypeNames.contains(SubType.REPRESSION)) { //inhibition, repression SBO:0000169
        sign = Sign.negative;
        in.setSBOTerm(169);
      } else if(subTypeNames.contains(SubType.INDIRECT_EFFECT)) { // indirect effect 
        sign = Sign.unknown;        
      } else if(subTypeNames.contains(SubType.STATE_CHANGE)) { //state change SBO:0000168        
        sign = Sign.unknown; //TODO: is unknown correct?
        in.setSBOTerm(168);
      } else {
        // just for debugging
        sign = Sign.unknown;
      }


      CVTerm cv= new CVTerm(CVTerm.Qualifier.BQB_IS);
      if (sign!=null){          
        if(subTypeNames.contains("dissociation")) { // dissociation SBO:0000177
          cv.addResource(KeggInfos.miriam_urn_sbo + formatSBO(177));
        } 
        if(subTypeNames.contains("binding/association")) { // binding/association SBO:0000177
          cv.addResource(KeggInfos.miriam_urn_sbo + formatSBO(177));   
        } 
        if (subTypeNames.contains("phosphorylation")) { // phosphorylation SBO:0000216
          cv.addResource(KeggInfos.miriam_urn_sbo + formatSBO(216));
        } 
        if (subTypeNames.contains("dephosphorylation")) { // dephosphorylation SBO:0000330
          cv.addResource(KeggInfos.miriam_urn_sbo + formatSBO(330));
        } 
        if (subTypeNames.contains("glycosylation")) { // glycosylation SBO:0000217
          cv.addResource(KeggInfos.miriam_urn_sbo + formatSBO(217));
        } 
        if (subTypeNames.contains("ubiquitination")) { // ubiquitination SBO:0000224
          cv.addResource(KeggInfos.miriam_urn_sbo + formatSBO(224));
        } 
        if (subTypeNames.contains("methylation")) { // methylation SBO:0000214
          cv.addResource(KeggInfos.miriam_urn_sbo + formatSBO(214));
        }
        
        if (cv.getNumResources()>0) {          
          setBiologicalQualifierISorHAS_VERSION(cv);
          in.addCVTerm(cv);
        }
        
        in.setSign(sign);
      }
    }

    return t;

  }
  
  /**
   * Formats an SBO term. E.g. "177" to "SBO%3A0000177".
   * @param i
   * @return
   */
  private String formatSBO(int i) {
    StringBuilder b = new StringBuilder("SBO%3A");
    String iString = Integer.toString(i);
    b.append(Utils.replicateCharacter('0', 7-iString.length()));
    b.append(iString);
    return b.toString();
  }

  /**
   * Checks if there is already a qual species, matching the given species
   * and returns it. If not, creates a new qual species for the given
   * species.
   * @param species
   * @param qualModel
   * @return
   */
  private QualitativeSpecies createQualitativeSpeciesFromSpecies(Species species, QualitativeModel qualModel) {
    String id = "qual_" + species.getId();
    QualitativeSpecies qs = qualModel.getQualitativeSpecies(id);
    if(qs == null){
      qs = qualModel.createQualitativeSpecies(id, "meta_" + id, species);
    }
    return qs;  
  }
  
  /**
   * Provides some direct access to KEGG2JSBML functionalities.
   * @param args
   * @throws Exception 
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws XMLStreamException
   * @throws ClassNotFoundException
   */
  public static void main(String[] args) throws Exception {
    // Speedup Kegg2SBML by loading alredy queried objects. Reduces network
    // load and heavily reduces computation time.
    Format format = Format.SBML_QUAL;
    AbstractKEGGtranslator<SBMLDocument> k2s;
    KeggInfoManagement manager = null;
    if (new File(Translator.cacheFileName).exists()
        && new File(Translator.cacheFileName).length() > 1) {
      manager = (KeggInfoManagement) KeggInfoManagement.loadFromFilesystem(Translator.cacheFileName);
    }
    k2s = (AbstractKEGGtranslator<SBMLDocument>) BatchKEGGtranslator.getTranslator(format, manager);
    // ---
    
    if (args != null && args.length > 0) {
      File f = new File(args[0]);
      if (f.isDirectory()) {
        // Directory mode. Convert all files in directory.
        BatchKEGGtranslator batch = new BatchKEGGtranslator();
        batch.setOrgOutdir(args[0]);
        if (args.length > 1)
          batch.setChangeOutdirTo(args[1]);
        batch.setTranslator(k2s);
        batch.setOutFormat(format);
        batch.parseDirAndSubDir();
        
      } else {
        // Single file mode.
        String outfile = args[0].substring(0,
          args[0].contains(".") ? args[0].lastIndexOf(".") : args[0].length())
          + ".sbml.xml";
        if (args.length > 1) outfile = args[1];
        
        Pathway p = KeggParser.parse(args[0]).get(0);
        k2s.translate(p, outfile);
      }
      
      // Remember already queried objects (save cache)
      if (k2s.getKeggInfoManager().hasChanged()) {
        KeggInfoManagement.saveToFilesystem(Translator.cacheFileName, k2s.getKeggInfoManager());
      }
      
      return;
    }
    
    
    // Just a few test cases here.
    System.out.println("Demo mode.");
    
    long start = System.currentTimeMillis();
    try {
      //k2s.translate("files/KGMLsamplefiles/hsa04010.xml", "files/KGMLsamplefiles/hsa04010.sbml.xml");
//      k2s.translate("files/KGMLsamplefiles/hsa00010.xml", "files/KGMLsamplefiles/hsa00010.sbml.xml");
      
      SBMLDocument doc = k2s.translate(new File("files/KGMLsamplefiles/hsa04210.xml"));
      new SBMLWriter().write(doc, "files/KGMLsamplefiles/hsa04210.sbml.xml"); 
      
      // Remember already queried objects
      if (k2s.getKeggInfoManager().hasChanged()) {
        KeggInfoManagement.saveToFilesystem(Translator.cacheFileName, k2s.getKeggInfoManager());
      }
      
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    
    System.out.println("Conversion took "+Utils.getTimeString((System.currentTimeMillis() - start)));
  }
  
  
  @Override
  protected boolean considerRelations() {
    return true;
  }

  @Override
  protected boolean considerReactions() {    
    return false;
  }
}
