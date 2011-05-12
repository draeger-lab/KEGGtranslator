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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Creator;
import org.sbml.jsbml.History;
import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.SBaseChangedEvent;
import org.sbml.jsbml.SBaseChangedListener;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.CVTerm.Qualifier;
import org.sbml.jsbml.CVTerm.Type;
import org.sbml.jsbml.xml.stax.SBMLWriter;

import de.zbit.kegg.KEGGtranslatorOptions;
import de.zbit.kegg.KeggInfoManagement;
import de.zbit.kegg.KeggInfos;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.kegg.parser.KeggParser;
import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.EntryType;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.kegg.parser.pathway.Reaction;
import de.zbit.kegg.parser.pathway.ReactionComponent;
import de.zbit.kegg.parser.pathway.ReactionType;
import de.zbit.util.EscapeChars;
import de.zbit.util.Info;
import de.zbit.util.SortedArrayList;
import de.zbit.util.Utils;

/**
 * KEGG2JSBML converter (also KGML2JSBML).
 * 
 * @author Clemens Wrzodek
 * @author Andreas Dr&auml;ger
 * @since 1.0
 * @version $Rev$
 */
public class KEGG2jSBML extends AbstractKEGGtranslator<SBMLDocument> implements SBaseChangedListener {
  /*
   * General Notes:
   * XXX: Important to know: subtype.setValue contains replacement of &gt; to > !!!
   * TODO: Edges (sub types of relations) now may have colors.
   */
  
  /**
   * Generate pure SBML or do you want to add CellDesigner annotations?
   */
  private boolean addCellDesignerAnnots = true;
  
  /**
   * If false, simply sets an SBO term to the species, based on entryType.
   * If true, creates a ModifierSpeciesReference that gets the SBO term associated
   * and will be added to the reaction.
   * Probably setting it to true, will return the better model.
   */
  private boolean treatEntrysWithReactionDifferent = true;
  
  /**
   * Contains all ids already assigned to an element in the sbml document.
   * Used for avoiding giving the same id to two or more different elements.
   */
  private ArrayList<String> SIds = new ArrayList<String>();
  
  /**
   * Default compartment size.
   */
  private double defaultCompartmentSize=1d;
  
  /**
   * Default initial amount of a species.
   */
  private double speciesDefaultInitialAmount=1d;
  
  
  /*===========================
   * PUBLIC, STATIC VARIABLES
   * ===========================*/
  
  /**
   * String to use for quotation start marks in SBML descriptions/ titles.
   */
  public static String quotStart = "&#8220;"; // "\u201C";//"&#8220;"; // &ldquo;
  
  /**
   * String to use for quotation end marks in SBML descriptions/ titles.
   */
  public static String quotEnd = "&#8221;"; // "\u201D";//"&#8221;"; // &rdquo;
  
  /**
   * SBO Term for EntryType "general modifier" (compound, map, other).
   * Only if @link {@link #treatEntrysWithReactionDifferent} is true.
   */
  public static int ET_GeneralModifier2SBO = 13; // 13=catalyst // 460="enzymatic catalyst"
  
  /**
   * SBO Term for EntryType enzyme, gene, group, ortholog, genes.
   * Only if @link {@link #treatEntrysWithReactionDifferent} is true.
   */
  public static int ET_EnzymaticModifier2SBO = 460;
  
  /**
   * SBO Term for EntryType Ortholog.
   * Only if @link {@link #treatEntrysWithReactionDifferent} is false.
   */
  public static int ET_Ortholog2SBO = 243; // 243="gene" // 404="unit of genetic information"
  
  /**
   * SBO Term for EntryType Enzyme.
   * Only if @link {@link #treatEntrysWithReactionDifferent} is false.
   */
  public static int ET_Enzyme2SBO = 14; // 14="Enzyme",	// 252="polypeptide chain"
  
  /**
   * SBO Term for EntryType Gene.
   * Only if @link {@link #treatEntrysWithReactionDifferent} is false.
   */
  public static int ET_Gene2SBO = 243; // 243="gene"
  
  /**
   * SBO Term for EntryType Group.
   * Only if @link {@link #treatEntrysWithReactionDifferent} is false.
   */
  public static int ET_Group2SBO = 253; // 253="non-covalent complex"
  
  /**
   * SBO Term for EntryType Compound.
   * Only if @link {@link #treatEntrysWithReactionDifferent} is false.
   */
  public static int ET_Compound2SBO = 247; // 247="Simple Molecule"
  
  /**
   * SBO Term for EntryType Map.
   * Only if @link {@link #treatEntrysWithReactionDifferent} is false.
   */
  public static int ET_Map2SBO = 291; // 291="Empty set"
  /**
   * SBO Term for EntryType Other.
   * Only if @link {@link #treatEntrysWithReactionDifferent} is false.
   */
  public static int ET_Other2SBO = 285; // 285="material entity of unspecified nature"
  
  /*===========================
   * CONSTRUCTORS
   * ===========================*/
  
  /**
   * Initialize a new Kegg2jSBML object, using a new Cache and a new KeggAdaptor.
   */
  public KEGG2jSBML() {
    this(new KeggInfoManagement());
  }
  
  /**
   * Initialize a new Kegg2jSBML object, using the given cache.
   * @param manager
   */
  public KEGG2jSBML(KeggInfoManagement manager) {
    super(manager);
    
    loadPreferences();
  }
  
  /*===========================
   * Getters and Setters
   * ===========================*/
  
  /**
   * See {@link #addCellDesignerAnnots}
   * @return
   */
  public boolean isAddCellDesignerAnnots() {
    return addCellDesignerAnnots;
  }
  /**
   * @param addCellDesignerAnnots - see {@link #addCellDesignerAnnots}.
   */
  public void setAddCellDesignerAnnots(boolean addCellDesignerAnnots) {
    this.addCellDesignerAnnots = addCellDesignerAnnots;
  }
  /**
   * See {@link #treatEntrysWithReactionDifferent}
   * @return
   */
  public boolean isTreatEntrysWithReactionDifferent() {
    return treatEntrysWithReactionDifferent;
  }
  /**
   * @param b - see {@link #treatEntrysWithReactionDifferent}.
   */
  public void setTreatEntrysWithReactionDifferent(boolean b) {
    this.treatEntrysWithReactionDifferent = b;
  }
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.AbstractKEGGtranslator#isOutputFunctional()
   */
  @Override
  public boolean isOutputFunctional() {
    return true;
  }
  
  /**
   * Returns the default compartment size.
   * @return
   */
  public double getDefaultCompartmentSize() {
    return defaultCompartmentSize;
  }
  
  /**
   * Set the default size of a compartment.
   * Default: 1.
   * @param d
   */
  public void setDefaultCompartmentSize(double d) {
    defaultCompartmentSize=d;
  }
  
  /**
   * Returns the default initial amount of a species.
   * @return
   */
  public double getDefaultSpeciesInitialAmount() {
    return speciesDefaultInitialAmount;
  }
  
  /**
   * Set the default initial amount of a species.
   * Default: 1.
   * @param d
   */
  public void setDefaultSpeciesInitialAmount(double d) {
    speciesDefaultInitialAmount=d;
  }
  
  
  
  /*===========================
   * FUNCTIONS
   * ===========================*/
  
  /** Load the default preferences from the SBPreferences object. */
  private void loadPreferences() {
    addCellDesignerAnnots = KEGGtranslatorOptions.CELLDESIGNER_ANNOTATIONS.getValue(prefs);
  }
  
  /**
   * Configures the SpeciesReference: Sets the name,
   * id, metaId, species and SBO term.
   * 
   * @param p
   * @param rc
   * @param sr
   * @param SBO
   */
  private void configureReactionComponent(Pathway p, ReactionComponent rc, SpeciesReference sr, int SBO) {
    if (rc.getName() == null || rc.getName().trim().length()<1) {
      rc = rc.getAlt();
      if (rc==null || rc.getName() == null || rc.getName().trim().length()<1) return;
    }
    sr.setName(rc.getName());
    sr.setId(NameToSId(sr.getName()));
    sr.setMetaId("meta_" + sr.getId());
    
    Entry spec = p.getEntryForName(rc.getName());
    if ((spec != null) && (spec.getCustom() != null)) {
      sr.setSpecies((Species) spec.getCustom());
    }
    
    if (sr.getSpeciesInstance() != null
        && sr.getSpeciesInstance().getSBOTerm() <= 0) {
      sr.getSpeciesInstance().setSBOTerm(SBO); // should be Product/Substrate
      sr.setSBOTerm(SBO);
    }
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.AbstractKEGGtranslator#writeToFile(java.lang.Object, java.lang.String)
   */
  @Override
  public boolean writeToFile(SBMLDocument doc, String outFile) {
    if (new File(outFile).exists()) lastFileWasOverwritten=true;
    try {
      SBMLWriter writer = new SBMLWriter();
      writer.write(doc, outFile, KEGGtranslator.APPLICATION_NAME, KEGGtranslator.VERSION_NUMBER);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }
  
  private final static String notesStartString = "<notes><body xmlns=\"http://www.w3.org/1999/xhtml\">";
  private final static String notesEndString = "</body></notes>";
  
  /**
   * Converts the given pathway to a jSBML document.
   * @param p - the Kegg Pathway.
   * @return SBMLDocument
   */
  @Override
  protected SBMLDocument translateWithoutPreprocessing(Pathway p) {
    int level = 2;
    int version = 4;
    SBMLDocument doc = new SBMLDocument(level, version);
    doc.addChangeListener(this);
    
    // Reset lists and buffers.
    SIds = new ArrayList<String>(); // Reset list of given SIDs. These are being remembered to avoid double ids.
    CellDesignerUtils cdu = null;
    if (addCellDesignerAnnots) cdu = new CellDesignerUtils(); 
    
    // Initialize a progress bar.
    initProgressBar(p,false,false);
    
    // new Model with Kegg id as id.
    Model model = doc.createModel(NameToSId(p.getName().replace(":", "_")));
    model.setMetaId("meta_" + model.getId());
    model.setName(p.getTitle());
    Compartment compartment = model.createCompartment("default");
    // Create neccessary default compartment
    compartment.setSize(defaultCompartmentSize);
    compartment.setUnits(model.getUnitDefinition("volume"));
    // Be careful: compartment ID ant other compartment stuff are HARDCODED
    // in cellDesigner extension code generation!
    
    // Create Model History
    History hist = new History();
    Creator creator = new Creator();
    creator.setOrganisation("ZBIT, University of T\u00fcbingen, WSI-RA");
    hist.addCreator(creator);
    hist.addModifiedDate(Calendar.getInstance().getTime());
    Annotation annot = new Annotation();
    //annot.setAbout("#" + model.getMetaId());
    model.setAnnotation(annot);
    model.setHistory(hist);
    StringBuffer notes = new StringBuffer(notesStartString);
    
    // CellDesigner Annotations
    if (addCellDesignerAnnots) {
      cdu.initCellDesignerAnnotations(model, doc);
    }
    
    model.getAnnotation().addRDFAnnotationNamespace("bqbiol", "", "http://biomodels.net/biology-qualifiers/");
    model.getAnnotation().addRDFAnnotationNamespace("bqmodel", "", "http://biomodels.net/model-qualifiers/");
    
    // Parse Kegg Pathway information
    CVTerm mtPwID = new CVTerm();
    mtPwID.setQualifierType(Type.MODEL_QUALIFIER);
    mtPwID.setModelQualifierType(Qualifier.BQM_IS);
    mtPwID.addResource(KeggInfos.getMiriamURIforKeggID(p.getName())); // same as "urn:miriam:kegg.pathway" + p.getName().substring(p.getName().indexOf(":"))
    model.addCVTerm(mtPwID);
    
    // Retrieve further information via Kegg Adaptor
    KeggInfos orgInfos = new KeggInfos("GN:" + p.getOrg(), manager); // Retrieve all organism information via KeggAdaptor
    if (orgInfos.queryWasSuccessfull()) {
      CVTerm mtOrgID = new CVTerm();
      mtOrgID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
      mtOrgID.setBiologicalQualifierType(Qualifier.BQB_OCCURS_IN);
      appendAllIds(orgInfos.getTaxonomy(), mtOrgID, KeggInfos.miriam_urn_taxonomy);
      model.addCVTerm(mtOrgID);
      
      notes.append(String.format("<h1>Model of %s%s%s in %s%s%s</h1>\n", quotStart, p.getTitle(), quotEnd, quotStart, orgInfos.getDefinition(), quotEnd));
    } else {
      notes.append(String.format("<h1>Model of " + quotStart + "%s"+ quotEnd + "</h1>\n", p.getTitle()));
    }
    
    // Get PW infos from KEGG Api for Description and GO ids.
    KeggInfos pwInfos = new KeggInfos(p.getName(), manager); // NAME, DESCRIPTION, DBLINKS verwertbar
    if (pwInfos.queryWasSuccessfull()) {
      if (pwInfos.getDescription()!=null) {
        notes.append(String.format("%s<br/>\n", pwInfos.getDescription()));
      }
      
      // GO IDs
      if (pwInfos.getGo_id() != null) {
        CVTerm mtGoID = new CVTerm();
        mtGoID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
        mtGoID.setBiologicalQualifierType(Qualifier.BQB_IS_VERSION_OF);
        appendAllGOids(pwInfos.getGo_id(), mtGoID);
        if (mtGoID.getNumResources() > 0)
          model.addCVTerm(mtGoID);
      }
    }
    
    String alternativeImageURL = "http://www.genome.jp/kegg-bin/show_pathway?"+
    (p.getName().contains(":")?p.getName().substring(p.getName().indexOf(":")+1): p.getName());
    
    notes.append(String.format("<a href=\"%s\"><img src=\"%s\" alt=\"%s\"/></a><br/>\n", p.getImage(), p.getImage(), alternativeImageURL));
    notes.append(String.format("<a href=\"%s\">Original Entry</a><br/>\n", p.getLink()));
    
    // Write model version and creation date, if available, into model notes.
    if (p.getVersion() > 0 || (p.getComment() != null && p.getComment().length() > 0)) {
      notes.append("<p>");
      if (p.getComment() != null && p.getComment().length() > 0) {
        notes.append(String.format("KGML comment: %s%s%s<br/>\n", quotStart, p.getComment(), quotEnd));
      }
      if (p.getVersion() > 0) {
        notes.append(String.format("KGML version was: %s<br/>\n", Double.toString(p.getVersion())));
      }
      notes.append("</p>");
    }
    notes.append(String.format("<div align=\"right\"><i><small>This file has been generated by %s version %s</small></i></div><br/>\n", 
      KEGGtranslator.APPLICATION_NAME, VERSION_NUMBER));
    
    // Save all reaction modifiers in a list. String = reaction id.
    SortedArrayList<Info<String, ModifierSpeciesReference>> reactionModifiers = new SortedArrayList<Info<String, ModifierSpeciesReference>>();
    
    // Create species
    ArrayList<Entry> entries = p.getEntries();
    for (Entry entry : entries) {
      progress.DisplayBar();
      
      addKGMLEntry(entry, p, model, compartment, reactionModifiers);
    }
    
    // Add CellDesigner information to species / entries.
    if (addCellDesignerAnnots) cdu.addCellDesignerAnnotationToAllSpecies(p);
    
    // ------------------------------------------------------------------
    
    // I noticed, that some reations occur multiple times in one KGML document,
    // (maybe its intended? e.g. R00014 in hsa00010.xml)
    List<String> processedReactions = new SortedArrayList<String>();
    
    
    // All species added. Parse reactions and relations.
    for (Reaction r : p.getReactions()) {
      if (!processedReactions.contains(r.getName())) {
        org.sbml.jsbml.Reaction sbReaction = addKGMLReaction(r,p,model,compartment,reactionModifiers);
        
        if (addCellDesignerAnnots && sbReaction!=null)
          cdu.addCellDesignerAnnotationToReaction(sbReaction, r);
        processedReactions.add(r.getName());
      }
    }
    
    // Give a warning if we have no reactions.
    if (p.getReactions().size()<1 && !considerRelations) {
      System.err.println("Warning: File does not contain any reactions.");
      //  Consider setting 'considerRelations' to true.
    }
    
    // ------------------------------------------------------------------
    
    
    
    // Removing nodes here (removeOrphans) does not work, because
    // all CellDesigner annotations are static and don't get removed!
    
    
    // Finalize notes and annotations.
    notes.append(notesEndString);
    model.setNotes(notes.toString());
    if (addCellDesignerAnnots) {
      cdu.addCellDesignerAnnotationToModel(p, model, compartment);
    }
    
    return doc;
  }
  
  /**
   * Translate a KGML reaction to an SBML reaction.
   * @param r - the reaction to translate.
   * @param p - the pathway, in which the reaction is contained.
   * @param model - the current SBML model.
   * @param compartment - the current SBML compartment.
   * @param reactionModifiers - list of all reactionModifiers.
   */
  private org.sbml.jsbml.Reaction addKGMLReaction(Reaction r, Pathway p, Model model, Compartment compartment,
    SortedArrayList<Info<String, ModifierSpeciesReference>> reactionModifiers) {
    // Skip reaction if it has either no reactants or no products.
    boolean hasAtLeastOneReactantAndProduct = false;
    for (ReactionComponent rc : r.getSubstrates()) {
      Entry spec = p.getEntryForName(rc.getName());
      if (spec == null || spec.getCustom() == null) continue;
      hasAtLeastOneReactantAndProduct = true;
      break;
    }
    if (!hasAtLeastOneReactantAndProduct) return null;//continue;
    hasAtLeastOneReactantAndProduct = false;
    for (ReactionComponent rc : r.getProducts()) {
      Entry spec = p.getEntryForName(rc.getName());
      if (spec == null || spec.getCustom() == null) continue;
      hasAtLeastOneReactantAndProduct = true;
      break;
    }
    
    if (!hasAtLeastOneReactantAndProduct) return null;//continue;
    
    org.sbml.jsbml.Reaction sbReaction = model.createReaction();
    sbReaction.initDefaults();
    if (sbReaction.getLevel()>=3) {
      sbReaction.setCompartment(compartment);
    }
    
    //Annotation rAnnot = new Annotation("");
    //rAnnot.setAbout(""); // IMPORTANT: Emtpy is wrong. it is being corrected in further on.
    //sbReaction.setAnnotation(rAnnot); // manchmal ist jSBML schon bescheuert... (Annotation darf nicht null sein, ist aber default null).
    
    StringBuffer notes = new StringBuffer(notesStartString);
    
    // Add substrates/ products
    sbReaction.setReversible(r.getType().equals(ReactionType.reversible));
    for (ReactionComponent rc : r.getSubstrates()) {
      SpeciesReference sr = sbReaction.createReactant();
      configureReactionComponent(p, rc, sr, 15); // 15 =Substrate
    }
    for (ReactionComponent rc : r.getProducts()) {
      SpeciesReference sr = sbReaction.createProduct();
      configureReactionComponent(p, rc, sr, 11); // 11 =Product
    }
    
    // Eventually add modifier
    List<ModifierSpeciesReference> modifier = getAllModifier(reactionModifiers,r);
    for (ModifierSpeciesReference mod : modifier) {
      sbReaction.addModifier(mod);
    }
    
    // Miriam identifier
    CVTerm reID = new CVTerm(); reID.setQualifierType(Type.BIOLOGICAL_QUALIFIER); reID.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm rePWs = new CVTerm(); rePWs.setQualifierType(Type.BIOLOGICAL_QUALIFIER); rePWs.setBiologicalQualifierType(Qualifier.BQB_OCCURS_IN);
    
    for (String ko_id : r.getName().split(" ")) {
      reID.addResource(KeggInfos.getMiriamURIforKeggID(ko_id));
      
      // Retrieve further information via Kegg API
      KeggInfos infos = new KeggInfos(ko_id, manager);
      if (infos.queryWasSuccessfull()) {
        notes.append("<p>");
        if (infos.getDefinition() != null) {
          notes.append(String.format("<b>Definition of %s%s%s:</b> %s<br/>\n",
            quotStart, ko_id.toUpperCase(), quotEnd, EscapeChars.forHTML(infos.getDefinition().replace("\n", " "))));
          // System.out.println(sbReaction.getNotesString());
          // notes="<body xmlns=\"http://www.w3.org/1999/xhtml\"><p><b>&#8220;TEST&#8221;</b> A &lt;&#061;&gt;&#62;&#x3e;\u003E B<br/></p></body>";
        } else
          notes.append(String.format("<b>%s</b><br/>\n", ko_id.toUpperCase()));
        if (infos.getEquation() != null) {
          notes.append(String.format("<b>Equation for %s%s%s:</b> %s<br/>\n",
            quotStart, ko_id.toUpperCase(), quotEnd, EscapeChars.forHTML(infos.getEquation())));
        }
        String prefix = "http://www.genome.jp/Fig/reaction/";
        String suffix = KeggInfos.suffix(ko_id.toUpperCase()) + ".gif";
        notes.append(String.format("<a href=\"%s%s\">", prefix, suffix));
        notes.append(String.format("<img src=\"%s%s\"/></a>\n", prefix, suffix));
        if (infos.getPathwayDescriptions() != null) {
          notes.append("<br/>\n<b>Occurs in:</b><br/>\n");
          notes.append("<ul>\n");
          for (String desc : infos.getPathwayDescriptions().split(",")) { // e.g. ",Glycolysis / Gluconeogenesis,Metabolic pathways"
            notes.append("<li>"+ desc + "</li>\n");
          }
          notes.append("</ul><br/>\n");
        }
        notes.append("</p>");
        
        if (rePWs != null && infos.getPathways() != null) {
          for (String pwId : infos.getPathways().split(",")) {
            String urn = KeggInfos.miriam_urn_kgPathway + KeggInfos.suffix(pwId);
            if (!rePWs.getResources().contains(urn)){
              rePWs.addResource(urn);
            }
          }
        }
      }
    }
    if ((reID.getNumResources() > 0) || (rePWs.getNumResources() > 0)) {
      sbReaction.getAnnotation().addRDFAnnotationNamespace("bqbiol", "", "http://biomodels.net/biology-qualifiers/");
    }
    if (reID.getNumResources() > 0) sbReaction.addCVTerm(reID);
    if (rePWs.getNumResources() > 0) sbReaction.addCVTerm(rePWs);
    
    
    // Finally, add the fully configured reaction.
    sbReaction.setName(r.getName());
    sbReaction.setId(NameToSId(r.getName()));
    notes.append(notesEndString);
    sbReaction.setNotes(notes.toString());
    sbReaction.setMetaId("meta_" + sbReaction.getId());
    sbReaction.setSBOTerm(231); // interaction. Most generic SBO Term possible, for a reaction.
    //rAnnot.setAbout("#" + sbReaction.getMetaId());
    
    return sbReaction;
  }
  
  /**
   * Returns all reactionModifiers for the given reaction.
   * @param reactionModifiers - List of all modifiers (must be sorted)
   * @param r - The reaction, for which you want to have all modifiers.
   * @return list of modifiers, or empty list.
   */
  private List<ModifierSpeciesReference> getAllModifier (
    SortedArrayList<Info<String, ModifierSpeciesReference>> reactionModifiers, Reaction r) {
    List<ModifierSpeciesReference> modifier = new ArrayList<ModifierSpeciesReference>();
    String lName = r.getName().toLowerCase().trim();
    int modifierPos = reactionModifiers.indexOf(lName);
    if (modifierPos<0) return modifier;
    
    // Add the current item
    modifier.add(reactionModifiers.get(modifierPos).getInformation());
    //reactionModifiers.remove(modifierPos);
    
    // Multiple modifiers possible
    
    // Add all previous items
    int modifierPos2=modifierPos;
    while ((--modifierPos2) >= 0 && reactionModifiers.get(modifierPos2).getIdentifier().equalsIgnoreCase(lName)) {
      if (!modifier.contains(reactionModifiers.get(modifierPos2).getInformation()));
      modifier.add(reactionModifiers.get(modifierPos2).getInformation());
      //reactionModifiers.remove(modifierPos2);
    }
    
    // Add all following items
    modifierPos2=modifierPos;
    while ((++modifierPos2) < reactionModifiers.size() && reactionModifiers.get(modifierPos2).getIdentifier().equalsIgnoreCase(lName)) {
      if (!modifier.contains(reactionModifiers.get(modifierPos2).getInformation()));
      modifier.add(reactionModifiers.get(modifierPos2).getInformation());
      //reactionModifiers.remove(modifierPos2);
    }    
    
    return modifier;
  }
  
  /**
   * Adds all available MIRIAM URNs and ids to the given species/entry.
   */
  private void addMiriamURNs(Entry entry, Species spec) {
    
    CVTerm cvtKGID = new CVTerm(); cvtKGID.setQualifierType(Type.BIOLOGICAL_QUALIFIER); cvtKGID.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm cvtEntrezID = new CVTerm(); cvtEntrezID.setQualifierType(Type.BIOLOGICAL_QUALIFIER); cvtEntrezID.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm cvtOmimID = new CVTerm(); cvtOmimID.setQualifierType(Type.BIOLOGICAL_QUALIFIER); cvtOmimID.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm cvtEnsemblID = new CVTerm(); cvtEnsemblID.setQualifierType(Type.BIOLOGICAL_QUALIFIER); cvtEnsemblID.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm cvtUniprotID = new CVTerm(); cvtUniprotID.setQualifierType(Type.BIOLOGICAL_QUALIFIER); cvtUniprotID.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm cvtChebiID = new CVTerm(); cvtChebiID.setQualifierType(Type.BIOLOGICAL_QUALIFIER); cvtChebiID.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm cvtDrugbankID = new CVTerm(); cvtDrugbankID.setQualifierType(Type.BIOLOGICAL_QUALIFIER); cvtDrugbankID.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm cvtGoID = new CVTerm(); cvtGoID.setQualifierType(Type.BIOLOGICAL_QUALIFIER); cvtGoID.setBiologicalQualifierType(Qualifier.BQB_IS_VERSION_OF);
    CVTerm cvtHGNCID = new CVTerm(); cvtHGNCID.setQualifierType(Type.BIOLOGICAL_QUALIFIER); cvtHGNCID.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm cvtPubchemID = new CVTerm(); cvtPubchemID.setQualifierType(Type.BIOLOGICAL_QUALIFIER); cvtPubchemID.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm cvt3dmetID = new CVTerm(); cvt3dmetID.setQualifierType(Type.BIOLOGICAL_QUALIFIER); cvt3dmetID.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm cvtReactionID = new CVTerm(); cvtReactionID.setQualifierType(Type.BIOLOGICAL_QUALIFIER); cvtReactionID.setBiologicalQualifierType(Qualifier.BQB_IS_DESCRIBED_BY);
    CVTerm cvtTaxonomyID = new CVTerm(); cvtTaxonomyID.setQualifierType(Type.BIOLOGICAL_QUALIFIER); cvtTaxonomyID.setBiologicalQualifierType(Qualifier.BQB_OCCURS_IN);
    // New as of oktober 2010:
    CVTerm PDBeChem = new CVTerm(); PDBeChem.setQualifierType(Type.BIOLOGICAL_QUALIFIER); PDBeChem.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm GlycomeDB = new CVTerm(); GlycomeDB.setQualifierType(Type.BIOLOGICAL_QUALIFIER); GlycomeDB.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm LipidBank = new CVTerm(); LipidBank.setQualifierType(Type.BIOLOGICAL_QUALIFIER); LipidBank.setBiologicalQualifierType(Qualifier.BQB_IS);
    
    
    // Parse every gene/object in this node.
    for (String ko_id : entry.getName().split(" ")) {
      if (ko_id.trim().equalsIgnoreCase("undefined") || entry.hasComponents()) continue; // "undefined" = group node, which contains "Components"
      
      // Add Kegg-id Miriam identifier
      String kgMiriamEntry = KeggInfos.getMiriamURIforKeggID(ko_id, entry.getType());
      if (kgMiriamEntry != null) cvtKGID.addResource(kgMiriamEntry);
      
      // Retrieve further information via Kegg API -- Be careful: very slow! Precache all queries at top of this function!
      KeggInfos infos = new KeggInfos(ko_id, manager);
      if (infos.queryWasSuccessfull()) {
        
        // HTML Information
        StringBuffer notes = new StringBuffer(notesStartString);
        if ((infos.getDefinition() != null) && (infos.getName() != null)) {
          notes.append(String.format("<p><b>Description for %s%s%s:</b> %s</p>\n",
            quotStart, EscapeChars.forHTML(infos.getName()), quotEnd, EscapeChars.forHTML(infos.getDefinition().replace("\n", " "))));
        } else if (infos.getName() != null) {
          notes.append(String.format("<p><b>%s</b></p>\n", EscapeChars.forHTML(infos.getName())));
        }
        if (infos.containsMultipleNames())
          notes.append(String.format("<p><b>All given names:</b><br/>%s</p>\n",EscapeChars.forHTML(infos.getNames().replace(";", ""))));
        if (infos.getCas() != null)
          notes.append(String.format("<p><b>CAS number:</b> %s</p>\n", infos.getCas()));
        if (infos.getFormula() != null)
          notes.append(String.format("<p><b>Formula:</b> %s</p>\n", EscapeChars.forHTML(infos.getFormula())));
        if (infos.getMass() != null)
          notes.append(String.format("<p><b>Mass:</b> %s</p>\n", infos.getMass()));
        notes.append(notesEndString);
        spec.appendNotes(notes.toString());
        
        // Parse "NCBI-GeneID:","UniProt:", "Ensembl:", ...
        if (infos.getEnsembl_id() != null)
          appendAllIds(infos.getEnsembl_id(), cvtEnsemblID, KeggInfos.miriam_urn_ensembl);
        if (infos.getChebi() != null)
          appendAllIds(infos.getChebi(), cvtChebiID, KeggInfos.miriam_urn_chebi, "CHEBI:");
        if (infos.getDrugbank() != null)
          appendAllIds(infos.getDrugbank(), cvtDrugbankID, KeggInfos.miriam_urn_drugbank);
        if (infos.getEntrez_id() != null)
          appendAllIds(infos.getEntrez_id(), cvtEntrezID, KeggInfos.miriam_urn_entrezGene);
        if (infos.getGo_id() != null)
          appendAllGOids(infos.getGo_id(), cvtGoID);
        if (infos.getHgnc_id() != null)
          appendAllIds(infos.getHgnc_id(), cvtHGNCID, KeggInfos.miriam_urn_hgnc, "HGNC:");
        
        if (infos.getOmim_id() != null)
          appendAllIds(infos.getOmim_id(), cvtOmimID, KeggInfos.miriam_urn_omim);
        if (infos.getPubchem() != null)
          appendAllIds(infos.getPubchem(), cvtPubchemID, KeggInfos.miriam_urn_PubChem_Substance);
        
        if (infos.getThree_dmet() != null)
          appendAllIds(infos.getThree_dmet(), cvt3dmetID, KeggInfos.miriam_urn_3dmet);
        if (infos.getUniprot_id() != null)
          appendAllIds(infos.getUniprot_id(), cvtUniprotID, KeggInfos.miriam_urn_uniprot);
        
        if (infos.getReaction_id() != null)
          appendAllIds(infos.getReaction_id(), cvtReactionID, KeggInfos.miriam_urn_kgReaction);
        if (infos.getTaxonomy() != null)
          appendAllIds(infos.getTaxonomy(), cvtTaxonomyID, KeggInfos.miriam_urn_taxonomy);
        
        if (infos.getPDBeChem()!= null)
          appendAllIds(infos.getPDBeChem(), PDBeChem, KeggInfos.miriam_urn_PDBeChem);
        if (infos.getGlycomeDB()!= null)
          appendAllIds(infos.getGlycomeDB(), GlycomeDB, KeggInfos.miriam_urn_GlycomeDB);
        if (infos.getLipidBank()!= null)
          appendAllIds(infos.getLipidBank(), LipidBank, KeggInfos.miriam_urn_LipidBank);
      }
      
    }
    // Add all non-empty ressources.
    if (cvtKGID.getNumResources() > 0) spec.addCVTerm(cvtKGID);
    if (cvtEntrezID.getNumResources() > 0) spec.addCVTerm(cvtEntrezID);
    if (cvtOmimID.getNumResources() > 0) spec.addCVTerm(cvtOmimID);
    if (cvtEnsemblID.getNumResources() > 0) spec.addCVTerm(cvtEnsemblID);
    if (cvtUniprotID.getNumResources() > 0) spec.addCVTerm(cvtUniprotID);
    if (cvtChebiID.getNumResources() > 0) spec.addCVTerm(cvtChebiID);
    if (cvtDrugbankID.getNumResources() > 0) spec.addCVTerm(cvtDrugbankID);
    if (cvtGoID.getNumResources() > 0) spec.addCVTerm(cvtGoID);
    if (cvtHGNCID.getNumResources() > 0) spec.addCVTerm(cvtHGNCID);
    if (cvtPubchemID.getNumResources() > 0) spec.addCVTerm(cvtPubchemID);
    if (cvt3dmetID.getNumResources() > 0) spec.addCVTerm(cvt3dmetID);
    if (cvtReactionID.getNumResources() > 0) spec.addCVTerm(cvtReactionID);
    if (cvtTaxonomyID.getNumResources() > 0) spec.addCVTerm(cvtTaxonomyID);
    if (PDBeChem.getNumResources() > 0) spec.addCVTerm(PDBeChem);
    if (GlycomeDB.getNumResources() > 0) spec.addCVTerm(GlycomeDB);
    if (LipidBank.getNumResources() > 0) spec.addCVTerm(LipidBank);
  }
  
  
  /**
   * Translates the given entry to jSBML.
   * 
   * @param entry - KGML entry to add.
   * @param p - pathway of the specified entry.
   * @param model - current model.
   * @param compartment - current compartment.
   * @param reactionModifiers - list of modifiers to add this species,
   * if it is an enzyme or similar.
   */
  private Species addKGMLEntry(Entry entry, Pathway p, Model model, Compartment compartment,
    List<Info<String, ModifierSpeciesReference>> reactionModifiers) {
    /*
     * <entry id="1" name="ko:K00128" type="ortholog" reaction="rn:R00710" link="http://www.genome.jp/dbget-bin/www_bget?ko+K00128">
     * <graphics name="K00128" fgcolor="#000000" bgcolor="#BFBFFF" type="rectangle" x="170" y="1018" width="45" height="17"/>
     * </entry>
     */
    
    boolean isPathwayReference = false;
    String name = entry.getName().trim();
    if ((name != null) && (name.toLowerCase().startsWith("path:") || entry.getType().equals(EntryType.map))) {
      isPathwayReference = true;
    }
    // Eventually skip this node. It's just a label for the current pathway.
    if (isPathwayReference && (entry.hasGraphics() && entry.getGraphics().getName().toLowerCase().startsWith("title:"))) {
      compartment.setName(entry.getGraphics().getName().substring(6).trim());
      return null;//continue;
    }
    
    /*
     * XXX: Gruppenknoten erstellen.
     * Gibt es sowas in SBML?
     * InCD -> ja, aber umsetzung ist ungenügend (nur
     * zur visualisierung, keine SBML Species für alle species).
     * 
     * Gelöst: Über complexSpeciesAlias in CD.
     * In simplen SBML ignoriert, da funktion nicht vorhanden.
     * Reaktionen werden stets so übersetzt, wie sie im Dokument stehen
     * (meist nur vom Gruppenknoten aus, nicht von den Komponenten).
     * Das macht Sinn, da Gruppenknoten selbst nicht nur eine "Gruppe" ist,
     * sondern per se auch schon ein Protein o.ä. definiert.
     *
     *  Beispiel (aus map04010hsa.xml):
     *      <entry id="141" name="group:" type="genes">
     *         <graphics fgcolor="#000000" bgcolor="#FFFFFF" type="rectangle" x="945" y="310" width="129" height="59"/>
     *         <component id="131"/>
     *         <component id="133"/>
     *         <component id="134"/>
     *     </entry>
     *  Beispiel aktuell (kg0.7 map04010.xml):
     *    <entry id="138" name="undefined" type="group">
     *         <graphics fgcolor="#000000" bgcolor="#FFFFFF"
     *              type="rectangle" x="945" y="310" width="129" height="59"/>
     *         <component id="131"/>
     *         <component id="133"/>
     *         <component id="134"/>
     *     </entry> 
     */
    
    // Get a good name for the node
    boolean hasMultipleIDs = false;
    if (entry.getName().trim().contains(" ")) hasMultipleIDs = true;
    if (entry.hasGraphics()&& entry.getGraphics().getName().length() > 0) {
      name = entry.getGraphics().getName(); // + " (" + name + ")"; // Append ko Id(s) possible!
    }
    // Set name to real and human-readable name (from Inet data - Kegg API).
    if (!hasMultipleIDs) {
      // Be careful: very slow, uses Cache - so doesn't matter to query the same id one or more times.
      KeggInfos infos = new KeggInfos(entry.getName().trim(), manager);
      if (infos.queryWasSuccessfull() && showShortNames && (infos.getNames() != null)) {
        name = infos.getNames();
        // Choose the shortest name.
        String[] multiNames = name.split(";");
        for(String cname:multiNames) {
          cname = cname.replace("\n", "").trim();
          if (cname.length()>0 && cname.length()<name.length()) {
            name = cname;
          }
        }
        
        // ! showShortNames
      } else if (infos.queryWasSuccessfull() && (infos.getName() != null)) {
        name = infos.getName();
      }
    }
    
    // Eventually cut at comma ("PCK1, MGC22652" => "PCK1").
    if (showShortNames) {
      name = shortenName(name);
    }
    // ---
    
    // Initialize species object
    Species spec = model.createSpecies();
    spec.setCompartment(compartment); // spec.setId("s_" +
    // entry.getId());
    spec.setInitialAmount(speciesDefaultInitialAmount);
    spec.setUnits(model.getUnitDefinition("substance"));
    
    // ID has to be at this place, because other refer to it by id and if id is not set. refenreces go to null.
    // spec.setId(NameToSId(entry.getName().replace(' ', '_')));
    spec.setId(NameToSId(name.replace(' ', '_')));
    spec.setMetaId("meta_" + spec.getId());
    
    //Annotation specAnnot = new Annotation("");
    //specAnnot.setAbout("");
    //spec.setAnnotation(specAnnot); // manchmal ist jSBML schon bescheurt...
    StringBuffer notes = new StringBuffer(notesStartString);
    notes.append(String.format("<a href=\"%s\">Original Kegg Entry</a><br/>\n", entry.getLink()));
    notes.append(notesEndString);
    // Set notes here, so other methods (miriam) can append the notes.
    spec.setNotes(notes.toString());
    
    // Set SBO Term
    if (treatEntrysWithReactionDifferent && entry.getReaction() != null && entry.getReaction().trim().length() != 0) {
      // Q: Ist es richtig, sowohl dem Modifier als auch der species eine neue id zu geben? A: Nein, ist nicht richtig.
      // spec.setSBOTerm(ET_SpecialReactionCase2SBO);
      ModifierSpeciesReference modifier = new ModifierSpeciesReference(spec);
      
      // Annotation is empty in ModifierSpeciesReference
      //Annotation tempAnnot = new Annotation("");
      //tempAnnot.setAbout("");
      //modifier.setAnnotation(tempAnnot);
      
      if (addCellDesignerAnnots) {
        modifier.getAnnotation().addAnnotationNamespace("xmlns:celldesigner", "","http://www.sbml.org/2001/ns/celldesigner");
        modifier.addNamespace("xmlns:celldesigner=http://www.sbml.org/2001/ns/celldesigner");
      }
      modifier.setId(this.NameToSId("mod_" + entry.getReaction()));
      modifier.setMetaId("meta_" + modifier.getId());
      modifier.setName(modifier.getId());
      if (entry.getType().equals(EntryType.enzyme)   || entry.getType().equals(EntryType.gene)
          || entry.getType().equals(EntryType.group) || entry.getType().equals(EntryType.ortholog)
          || entry.getType().equals(EntryType.genes)) {
        // 1 & 2: klar. 3 (group): Doku sagt "MOSTLY a protein complex". 4 (ortholog): Kommen in
        // nicht-spezies spezifischen PWs vor und sind quasi otholog geclusterte gene.
        // 5. (genes) ist group in kgml versionen <0.7.
        modifier.setSBOTerm(ET_EnzymaticModifier2SBO); // 460 = Enzymatic catalyst
      } else { // "Metall oder etwas anderes, was definitiv nicht enzymatisch wirkt"
        modifier.setSBOTerm(ET_GeneralModifier2SBO); // 13 = Catalyst
      }
      
      // Remember modifier for later association with reaction.
      reactionModifiers.add(new Info<String, ModifierSpeciesReference>(entry.getReaction().toLowerCase().trim(), modifier));
      
    } else {
      spec.setSBOTerm(getSBOTerm(entry.getType()));
    }
    
    // Process graphics information
    if (entry.hasGraphics()) {
      // is handled by cellDesigner functions.
    }
    
    // Process Component information
    /*
     * // No need to do that! See Group Node information above.
     * if (entry.getComponents()!=null && entry.getComponents().size()>0) {
     *   for (int c:entry.getComponents()) {
     * 
     * } }
     */
    spec.getAnnotation().addRDFAnnotationNamespace("bqbiol", "", "http://biomodels.net/biology-qualifiers/");
    addMiriamURNs(entry, spec);
    
    // Finally, add the fully configured species.
    spec.setName(name);
    //specAnnot.setAbout("#" + spec.getMetaId());
    entry.setCustom(spec); // Remember node in KEGG Structure for further references.
    // NOT here, because it may depend on other entries, that are not yet processed.
    //if (addCellDesignerAnnots) addCellDesignerAnnotationToSpecies(spec, entry);
    // Not neccessary to add species to model, due to call in "model.createSpecies()".
    
    return spec;
  }
  
  /**
   * Extends a list of ReactionComponent by adding all children
   * of all group nodes, in addition to the group nodes itself.
   * @param src
   * @return
   */
  @SuppressWarnings("unused")
  private ArrayList<ReactionComponent> splitGroupNodes (Pathway p, ArrayList<ReactionComponent> src) {
    // We can not use group nodes in SBML, so we need to split them to the
    // single components and treat every component individually.
    ArrayList<ReactionComponent> ret = new ArrayList<ReactionComponent>();
    for (ReactionComponent rc : src) {
      // Check, if the given ReactionComponent is valid.
      if (rc.getName() == null || rc.getName().trim().length()<1) {
        rc = rc.getAlt();
        if (rc==null || rc.getName() == null || rc.getName().trim().length()<1)
          continue;
      }
      
      // Add the node anyways
      ret.add(rc);
      
      // If there are groupNodes, add also the children to the list.
      Entry s = p.getEntryForName(rc.getName());
      if (isGroupNode(s) && s.hasComponents()) {
        for (Integer c: s.getComponents()) {
          Entry child = p.getEntryForId(c);
          if (child!=null) ret.add(new ReactionComponent(child.getName()));
        }
      }
    }
    
    return ret;
  }
  
  /**
   * Generates a valid SId from a given name. If the name already is a valid
   * SId, the name is returned. If the SId already exists in this document,
   * "_<number>" will be appended and the next free number is being assigned.
   * => See SBML L2V4 document for the Definition of SId. (Page 12/13)
   * 
   * @param name
   * @return SId
   */
  private String NameToSId(String name) {
    /*
     * letter ::= �a�..�z�,�A�..�Z� digit ::= �0�..�9� idChar ::= letter |
     * digit | �_� SId ::= ( letter | �_� ) idChar*
     */
    String ret = "";
    if (name == null || name.trim().length() == 0) {
      ret = incrementSIdSuffix("SId");
      SIds.add(ret);
    } else {
      name = name.trim();
      char c = name.charAt(0);
      if (!(Character.isLetter(c) || c == '_')) ret = "SId_"; else ret = Character.toString(c);
      for (int i = 1; i < name.length(); i++) {
        c = name.charAt(i);
        if (Character.isLetter(c) || Character.isDigit(c) || c == '_') ret += Character.toString(c);
      }
      if (SIds.contains(ret)) ret = incrementSIdSuffix(ret);
      SIds.add(ret);
    }
    
    return ret;
  }
  
  
  /**
   * Appends "_<Number>" to a given String. <Number> is being set to the next
   * free number, so that this sID is unique in this sbml document. Should
   * only be called from "NameToSId".
   * 
   * @return
   */
  private String incrementSIdSuffix(String prefix) {
    int i = 1;
    String aktString = prefix + "_" + i;
    while (SIds.contains(aktString)) {
      aktString = prefix + "_" + (++i);
    }
    return aktString;
  }
  
  /**
   * Adds one or multiple goIDs to a CVTerm. Will automatically
   * split the IDs and append the MIRIAM URN.
   * @param goIDs - divided by space. Every id is exactly 7 digits long.
   * @param mtGoID - CVTerm to add MIRIAM URNs with GO ids to.
   */
  private static void appendAllGOids(String goIDs, CVTerm mtGoID) {
    for (String go_id : goIDs.split(" ")) {
      if (go_id.length() != 7 || !containsOnlyDigits(go_id))
        continue; // Invalid GO id.
      String urn = KeggInfos.getGo_id_with_MiriamURN(go_id);
      if (!mtGoID.getResources().contains(urn)) {
        mtGoID.addResource(urn);
      }
    }
  }
  
  /**
   * Append all IDs with Miriam URNs to a CV term. Multiple IDs are separated
   * by a space. Only the part behind the ":" will be added (if an ID contains
   * a ":").
   * 
   * @param IDs
   * @param myCVterm
   * @param miriam_URNPrefix
   */
  private static void appendAllIds(String IDs, CVTerm myCVterm, String miriam_URNPrefix) {
    if (IDs==null || IDs.length()<1) return;
    for (String id : IDs.split(" ")) {
      String urn = miriam_URNPrefix + KeggInfos.suffix(id);
      if (!myCVterm.getResources().contains(urn)) {
        myCVterm.addResource(urn);
      }
    }
  }
  
  /**
   * Append all IDs with Miriam URNs to a CV term. Multiple IDs are separated
   * by a space. All ids are required to contain a ":". If not,
   * mayContainDoublePointButAppendThisStringIfNot will be used. E.g.
   * "[mayContainDoublePointButAppendThisStringIfNot]:[ID]" or [ID] if it
   * contains ":".
   * 
   * @param IDs
   * @param myCVterm
   * @param miriam_URNPrefix
   * @param mayContainDoublePointButAppendThisStringIfNot
   */
  private static void appendAllIds(String IDs, CVTerm myCVterm, String miriam_URNPrefix, String mayContainDoublePointButAppendThisStringIfNot) {
    // Trim double point from
    // 'mayContainDoublePointButAppendThisStringIfNot' eventually.
    String s = mayContainDoublePointButAppendThisStringIfNot;
    if (s.endsWith(":")) {
      s = s.substring(0, s.length() - 1);
    }
    
    // Add every id to CVTerm.
    for (String id : IDs.split(" ")) {
      // Add prefix + id (with or without ":").
      String urn = miriam_URNPrefix + (id.contains(":") ? id.trim() : s + ":" + id.trim());
      if (!myCVterm.getResources().contains(urn)) {
        myCVterm.addResource(urn);
      }
    }
  }
  
  /**
   * Checks wether a Strings consists just of digits or not.
   * @param myString - String to check.
   * @return true if and only if every Character in the given
   * String is a digit.
   */
  private static boolean containsOnlyDigits(String myString) {
    char[] ch = myString.toCharArray();
    for (char c : ch)
      if (!Character.isDigit(c))
        return false;
    return true;
  }
  
  
  /**
   * Returns the SBO Term for an EntryType.
   * @param type - the KEGG EntryType, you want an SBMO Term for.
   * @return SBO Term (integer).
   */
  private static int getSBOTerm(EntryType type) {
    if (type.equals(EntryType.compound))
      return ET_Compound2SBO;
    if (type.equals(EntryType.enzyme))
      return ET_Enzyme2SBO;
    if (type.equals(EntryType.gene))
      return ET_Gene2SBO;
    if (type.equals(EntryType.group))
      return ET_Group2SBO;
    if (type.equals(EntryType.genes))
      return ET_Group2SBO;
    if (type.equals(EntryType.map))
      return ET_Map2SBO;
    if (type.equals(EntryType.ortholog))
      return ET_Ortholog2SBO;
    
    if (type.equals(EntryType.other))
      return ET_Compound2SBO;
    
    return ET_Compound2SBO;
  }
  
  
  /**
   * Provides some direct access to KEGG2JSBML functionalities.
   * @param args
   * @throws IOException
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws XMLStreamException
   * @throws ClassNotFoundException
   */
  public static void main(String[] args) throws IOException {
    // Speedup Kegg2SBML by loading alredy queried objects. Reduces network
    // load and heavily reduces computation time.
    AbstractKEGGtranslator<SBMLDocument> k2s;
    if (new File(KEGGtranslator.cacheFileName).exists()
        && new File(KEGGtranslator.cacheFileName).length() > 0) {
      KeggInfoManagement manager = (KeggInfoManagement) KeggInfoManagement.loadFromFilesystem(KEGGtranslator.cacheFileName);
      k2s = new KEGG2jSBML(manager);
    } else {
      k2s = new KEGG2jSBML();
    }
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
        batch.setOutFormat(Format.SBML);
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
        KeggInfoManagement.saveToFilesystem(KEGGtranslator.cacheFileName, k2s.getKeggInfoManager());
      }
      
      return;
    }
    
    
    // Just a few test cases here.
    System.out.println("Demo mode.");
    
    long start = System.currentTimeMillis();
    try {
      //k2s.translate("files/KGMLsamplefiles/hsa04010.xml", "files/KGMLsamplefiles/hsa04010.sbml.xml");
      k2s.translate("files/KGMLsamplefiles/hsa00010.xml", "files/KGMLsamplefiles/hsa00010.sbml.xml");
      
      // Remember already queried objects
      if (k2s.getKeggInfoManager().hasChanged()) {
        KeggInfoManagement.saveToFilesystem(KEGGtranslator.cacheFileName, k2s.getKeggInfoManager());
      }
      
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    
    System.out.println("Conversion took "+Utils.getTimeString((System.currentTimeMillis() - start)));
  }
  
  /* (non-Javadoc)
   * @see org.sbml.jsbml.SBaseChangedListener#sbaseAdded(org.sbml.jsbml.SBase)
   */
  public void sbaseAdded(SBase sb) {
    //System.out.println("[ADD] " + sb.toString());
  }
  
  /* (non-Javadoc)
   * @see org.sbml.jsbml.SBaseChangedListener#sbaseRemoved(org.sbml.jsbml.SBase)
   */
  public void sbaseRemoved(SBase sb) {
    //System.out.println("[RMV] " + sb.toString());
  }
  
  /* (non-Javadoc)
   * @see org.sbml.jsbml.SBaseChangedListener#stateChanged(org.sbml.jsbml.SBaseChangedEvent)
   */
  public void stateChanged(SBaseChangedEvent ev) {
    //System.out.println("[CHG] " + ev.toString());
  }
  
}
