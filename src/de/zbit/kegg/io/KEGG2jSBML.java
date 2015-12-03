/*
 * $Id: KEGG2jSBML.java 412 2015-09-21 20:39:51Z draeger $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn/KEGGconverter/trunk/src/de/zbit/kegg/io/KEGG2jSBML.java $
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.CVTerm.Qualifier;
import org.sbml.jsbml.CVTerm.Type;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Creator;
import org.sbml.jsbml.History;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.TidySBMLWriter;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;
import org.sbml.jsbml.util.ValuePair;

import de.zbit.cache.InfoManagement;
import de.zbit.kegg.AtomBalanceCheck;
import de.zbit.kegg.AtomBalanceCheck.AtomCheckResult;
import de.zbit.kegg.KEGGtranslatorOptions;
import de.zbit.kegg.Translator;
import de.zbit.kegg.api.KeggInfos;
import de.zbit.kegg.api.cache.KeggInfoManagement;
import de.zbit.kegg.parser.KeggParser;
import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.EntryType;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.kegg.parser.pathway.Reaction;
import de.zbit.kegg.parser.pathway.ReactionComponent;
import de.zbit.kegg.parser.pathway.ReactionType;
import de.zbit.kegg.parser.pathway.ext.EntryExtended;
import de.zbit.sbml.util.AnnotationUtils;
import de.zbit.util.ArrayUtils;
import de.zbit.util.DatabaseIdentifierTools;
import de.zbit.util.DatabaseIdentifiers;
import de.zbit.util.DatabaseIdentifiers.IdentifierDatabases;
import de.zbit.util.EscapeChars;
import de.zbit.util.SortedArrayList;
import de.zbit.util.Utils;
import de.zbit.util.objectwrapper.Info;

/**
 * KEGG2JSBML converter (also KGML2JSBML, KEGG2SBML, KGML2SBML).
 * 
 * @author Clemens Wrzodek
 * @author Andreas Dr&auml;ger
 * @since 1.0
 * @version $Rev: 412 $
 */
public class KEGG2jSBML extends AbstractKEGGtranslator<SBMLDocument>  {
  
  /*
   * General Notes:
   * XXX: Important to know: subtype.setValue contains replacement of &gt; to > !!!
   * TODO: Edges (sub types of relations) now may have colors.
   */
  
  /**
   * Generate pure SBML or do you want to add CellDesigner annotations?
   */
  private boolean addCellDesignerAnnots = false;
  
  /**
   * Add the SBML-layout extension?
   */
  protected boolean addLayoutExtension = true;
  
  /**
   * Add the SBML-groups extension?
   */
  protected boolean useGroupsExtension = true;
  
  /**
   * Default compartment size.
   */
  private double defaultCompartmentSize = 1d;
  
  /**
   * Default initial amount of a species.
   */
  private double speciesDefaultInitialAmount = 1d;
  
  /**
   * The SBML level to be initialized. {@code NULL} results in
   * initializing it automatically (adjusts the level as needed (e.g.
   *  extensions require L3)).
   */
  protected Integer level = null;
  
  /**
   * The SBML version to be initialized. {@code NULL} results in
   * initializing it automatically (adjusts it as needed).
   */
  protected Integer version = null;
  
  
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
   * @param manager
   * @param level
   * @param version
   */
  public KEGG2jSBML(KeggInfoManagement manager, int level, int version) {
    this(manager);
    setLevelAndVersion(new ValuePair<Integer, Integer>(level, version));
  }
  
  /**
   * Set the SBML level and version that should be created.
   * @param level
   */
  private void setLevelAndVersion(ValuePair<Integer, Integer> level) {
    this.level = level.getL();
    version = level.getV();
  }
  
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
    addCellDesignerAnnots = addCellDesignerAnnots;
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
  
  public void setAddLayoutExtension(boolean b) {
    addLayoutExtension = b;
  }
  
  public void setUseGroupsExtension(boolean b) {
    useGroupsExtension = b;
  }
  
  
  
  /*===========================
   * FUNCTIONS
   * ===========================*/
  
  /** Load the default preferences from the SBPreferences object. */
  private void loadPreferences() {
    // CellDeisnger annotations are not supported anymore!
    //addCellDesignerAnnots = KEGGtranslatorOptions.CELLDESIGNER_ANNOTATIONS.getValue(prefs);
    addLayoutExtension = KEGGtranslatorOptions.ADD_LAYOUT_EXTENSION.getValue(prefs);
    useGroupsExtension = KEGGtranslatorOptions.USE_GROUPS_EXTENSION.getValue(prefs);
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
    if (!rc.isSetID() && !rc.isSetName()) {
      rc = rc.getAlt();
      if ((rc == null) || ((!rc.isSetID() && !rc.isSetName()))) {
        return;
      }
    }
    sr.setName(rc.getName());
    sr.setId(NameToSId(sr.getName()));
    sr.setMetaId("meta_" + sr.getId());
    sr.setSBOTerm(SBO);
    if (sr.getModel().getLevel() > 2) {
      /*
       * In order to obtain valid Level 3 models with identical properties than
       * in Level 2, we use the default value of earlier SBML releases:
       */
      if (rc.isSetStoichiometry()) {
        sr.setConstant(true);
      } else {
        sr.setConstant(false);
      }
    }
    
    // Set the stoichiometry
    Integer stoich = rc.getStoichiometry();
    sr.setStoichiometry(stoich == null ? 1d : stoich);
    
    // Get Species for ReactionComponent and assign to SpeciesReference.
    Entry rcEntry = p.getEntryForReactionComponent(rc);
    if ((rcEntry != null) && (rcEntry.getCustom() != null)) {
      if (rcEntry.getCustom() instanceof Species) {
        sr.setSpecies((Species) rcEntry.getCustom());
      } else {
        sr.setSpecies(((NamedSBase) rcEntry.getCustom()).getId());
      }
    }
    
    // Do not change SBO of parent species!
    //    if (sr.getSpeciesInstance() != null
    //        && sr.getSpeciesInstance().getSBOTerm() <= 0) {
    //      sr.getSpeciesInstance().setSBOTerm(SBO); // should be Product/Substrate
    //    }
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.AbstractKEGGtranslator#writeToFile(java.lang.Object, java.lang.String)
   */
  @Override
  public boolean writeToFile(SBMLDocument doc, String outFile) {
    if (new File(outFile).exists()) {
      lastFileWasOverwritten=true;
    }
    try {
      TidySBMLWriter.write(doc, outFile, System.getProperty("app.name"), System
        .getProperty("app.version"));
    } catch (Exception e) {
      log.log(Level.SEVERE, "Could not write SBML document.", e);
      return false;
    }
    return true;
  }
  
  protected final static String notesStartString = "<notes><body xmlns=\"http://www.w3.org/1999/xhtml\">";
  protected final static String notesEndString = "</body></notes>";
  
  /**
   * Converts the given pathway to a jSBML document.
   * @param p - the Kegg Pathway.
   * @return SBMLDocument
   * @throws XMLStreamException
   */
  @Override
  protected SBMLDocument translateWithoutPreprocessing(Pathway p) throws XMLStreamException {
    ValuePair<Integer,Integer> lv = getLevelAndVersion();
    SBMLDocument doc = new SBMLDocument(lv.getL().intValue(), lv.getV().intValue());
    //doc.addChangeListener(this);
    
    // Reset lists and buffers.
    CellDesignerUtils cdu = null;
    if (addCellDesignerAnnots) {
      cdu = new CellDesignerUtils();
    }
    
    // Initialize a progress bar.
    initProgressBar(p, false, false);
    
    // new Model with Kegg id as id.
    Model model = doc.createModel(NameToSId(p.getName().replace(":", "_")));
    model.setMetaId("meta_" + model.getId());
    model.setName(p.getTitle()); // NOTE: Name is sometimes changed later (see below)
    if (lv.getL().intValue()>2) {
      // Make consistent units for level 3 (same as in l2v4).
      UnitDefinition ud = UnitDefinition.getPredefinedUnit(UnitDefinition.TIME, 2, 4);
      org.sbml.jsbml.util.SBMLtools.setLevelAndVersion(ud, lv.getL().intValue(), lv.getV().intValue());
      model.setTimeUnits(ud);
      
      ud = UnitDefinition.getPredefinedUnit(UnitDefinition.VOLUME, 2, 4);
      org.sbml.jsbml.util.SBMLtools.setLevelAndVersion(ud, lv.getL().intValue(), lv.getV().intValue());
      model.setVolumeUnits(ud);
      
      ud = UnitDefinition.getPredefinedUnit(UnitDefinition.SUBSTANCE, 2, 4);
      org.sbml.jsbml.util.SBMLtools.setLevelAndVersion(ud, lv.getL().intValue(), lv.getV().intValue());
      model.setSubstanceUnits(ud);
    }
    
    Compartment compartment = createCompartment(model);
    
    // Create Model History
    History hist = new History();
    Creator creator = new Creator();
    creator.setOrganisation("ZBIT, University of T\u00fcbingen, WSI-CogSys");
    hist.addCreator(creator);
    // JSBML sets a date automatically
    //    hist.addModifiedDate(Calendar.getInstance().getTime());
    Annotation annot = new Annotation();
    //annot.setAbout("#" + model.getMetaId());
    model.setAnnotation(annot);
    model.setHistory(hist);
    StringBuffer notes = new StringBuffer(notesStartString);
    
    // CellDesigner Annotations
    if (addCellDesignerAnnots) {
      cdu.initCellDesignerAnnotations(model, doc);
    }
    
    // Parse Kegg Pathway information
    boolean isKEGGPathway = DatabaseIdentifiers.checkID(DatabaseIdentifiers.IdentifierDatabases.KEGG_Pathway, p.getNameForMIRIAM());
    if (isKEGGPathway) {
      CVTerm mtPwID = new CVTerm();
      mtPwID.setQualifierType(Type.MODEL_QUALIFIER);
      mtPwID.setModelQualifierType(Qualifier.BQM_IS_DERIVED_FROM);
      
      /* TODO: if it's an original KEGG pathway, the name ALWAYS
       * starts with "path:". Use different prefixes for pathways
       * from other sources and catch those cases here (also in
       * KEGG2GraphML and oterhs).
       */
      
      // Next line is same as "urn:miriam:kegg.pathway" + p.getName().substring(p.getName().indexOf(":"))
      String kgMiriamEntry = KeggInfos.getMiriamURNforKeggID(p.getName());
      if (kgMiriamEntry != null) {
        mtPwID.addResource(AnnotationUtils.convertURN2URI(kgMiriamEntry));
      }
      model.addCVTerm(mtPwID);
      
      // Add KEGG reference
      CVTerm term = DatabaseIdentifierTools.getCVTerm(IdentifierDatabases.PubMed, null, "9847135");
      term.setQualifierType(CVTerm.Type.MODEL_QUALIFIER);
      term.setModelQualifierType(Qualifier.BQM_IS_DESCRIBED_BY);
      model.addCVTerm(term);
      
    } else {
      // other database references on the pathway
      if (p.isSetDatabaseIdentifiers()) {
        List<CVTerm> cvTerms = DatabaseIdentifierTools.getCVTerms(p.getDatabaseIdentifiers(), null);
        if (cvTerms!=null && cvTerms.size()>0) {
          for (CVTerm cvTerm : cvTerms) {
            cvTerm.setQualifierType(Type.MODEL_QUALIFIER);
            cvTerm.setModelQualifierType(Qualifier.BQM_IS_DESCRIBED_BY);
            model.addCVTerm(cvTerm);
          }
        }
      }
    }
    
    // Add KEGGtranslator reference
    CVTerm term = DatabaseIdentifierTools.getCVTerm(IdentifierDatabases.PubMed, null, "21700675");
    term.setQualifierType(CVTerm.Type.MODEL_QUALIFIER);
    term.setModelQualifierType(Qualifier.BQM_IS_DESCRIBED_BY);
    model.addCVTerm(term);
    
    // Retrieve further information via Kegg Adaptor
    boolean titleAdded = false;
    if (p.isSetOrg()) {
      KeggInfos orgInfos = KeggInfos.get("gn:" + p.getOrg(), manager); // Retrieve all organism information via KeggAdaptor
      if (orgInfos.queryWasSuccessfull()) {
        CVTerm mtOrgID = DatabaseIdentifierTools.getCVTerm(IdentifierDatabases.NCBI_Taxonomy, null, orgInfos.getTaxonomy().split("\\s"));
        if (mtOrgID.getResourceCount() > 0) {
          model.addCVTerm(mtOrgID);
        }
        notes.append(String.format("<h1>Model of %s%s%s in %s%s%s</h1>\n", quotStart, formatTextForHTMLnotes(p.getTitle()), quotEnd, quotStart, orgInfos.getDefinition(), quotEnd));
        model.setName(String.format("%s (%s)", p.getTitle(), orgInfos.getDefinition()));
        titleAdded = true;
      }
    }
    if (!titleAdded) {
      notes.append(String.format("<h1>Model of %s%s%s</h1>\n",quotStart,formatTextForHTMLnotes(p.getTitle()),quotEnd ));
    }
    
    // Get PW infos from KEGG Api for Description and GO ids.
    if (isKEGGPathway) {
      KeggInfos pwInfos = KeggInfos.get(p.getName(), manager); // NAME, DESCRIPTION, DBLINKS verwertbar
      if (pwInfos.queryWasSuccessfull()) {
        if (pwInfos.getDescription()!=null) {
          notes.append(String.format("%s<br/>\n", formatTextForHTMLnotes(pwInfos.getDescription())));
        }
        
        // GO IDs
        if (pwInfos.getGo_id() != null) {
          CVTerm mtGoID = DatabaseIdentifierTools.getCVTerm(IdentifierDatabases.GeneOntology, null, pwInfos.getGo_id().split("\\s"));
          if (mtGoID.getResourceCount() > 0) {
            model.addCVTerm(mtGoID);
          }
        }
      }
    }
    
    String imageURL = null;
    if (p.isSetImage()) {
      imageURL = p.getImage();
    } else if (isKEGGPathway) {
      imageURL = "http://www.genome.jp/kegg-bin/show_pathway?"+KeggInfos.suffix(p.getName());
    }
    // TODO: Insert here special cases to catch, e.g. biocarta IDs, etc.
    
    if (imageURL!=null) {
      if (isKEGGPathway) {
        notes.append(String.format("<a href=\"%s\" title=\"Graphical representation hosted by KEGG\"><img src=\"%s\" alt=\"%s\"/></a><br/>(PNG image hosted by the Kyoto Encyclopedia of Genes and Genomes, KEGG).<br/>\n", imageURL, imageURL, p.getTitle()));
      } else {
        notes.append(String.format("<a href=\"%s\" title=\"Graphical representation\"><img src=\"%s\" alt=\"%s\"/></a><br/>\n", imageURL, imageURL, p.getTitle()));
      }
    }
    
    // Write model version and creation date, if available, into model notes.
    if (p.getVersion() > 0 || (p.getComment() != null && p.getComment().length() > 0)) {
      notes.append("<p>");
      if (p.getComment() != null && p.getComment().length() > 0) {
        notes.append(String.format("%s comment: %s%s%s<br/>\n", p.getOriginFormatName(), quotStart, formatTextForHTMLnotes(p.getComment()), quotEnd));
      }
      if (p.getVersion() > 0) {
        notes.append(String.format("%s version was: %s<br/>\n", p.getOriginFormatName(), Double.toString(p.getVersion())));
      }
      if (p.isSetAdditionalText()) {
        notes.append(p.getAdditionalText());
      }
      notes.append("</p>\n");
    }
    
    // Add KEGGtranslator reference to the notes
    String appName = System.getProperty("app.name")!=null?System.getProperty("app.name"):"KEGGtranslator";
    if (appName.equalsIgnoreCase("KEGGtranslator")) {
      appName = "<a href=\"http://identifiers.org/pubmed/21700675\" title=\"Access publication about KEGGtranslator\">KEGGtranslator</a>";
    }
    notes.append(String.format("<p>This model has been automatically generated by %s", appName));
    String version = System.getProperty("app.version");
    if (version!=null && version.length()>0) {
      notes.append(" V" + version);
    }
    String citation = System.getProperty("app.citation.html");
    if (citation!=null && citation.length()>0) {
      notes.append(String.format(" (%s)", citation));
    }
    
    if (isKEGGPathway) {
      notes.append(String.format(" using information coming from the KEGG PATHWAY Database (<a href=\"%s\" title=\"Original pathway\">original pathway</a>).\n", p.getLink()));
    }
    else if (p.isSetLink()) {
      notes.append(String.format("<a href=\"%s\">Original entry</a><br/>\n", p.getLink()));
    }
    
    notes.append("</p>");
    
    // Removed this because we have already two other positions in which we write who created this file (the history and also the comment within the file).
    //    notes.append(String.format("<div align=\"right\"><i><small>This file has been generated by %s version %s</small></i></div><br/>\n",
    //    	System.getProperty("app.name"), System.getProperty("app.version")));
    
    // Save all reaction modifiers in a list. String = reaction id.
    List<Info<String, ModifierSpeciesReference>> reactionModifiers = new SortedArrayList<Info<String, ModifierSpeciesReference>>();
    
    
    
    // Create species
    List<Entry> entries = getEntriesWithGroupsAsLast(p);
    Set<String> addedEntries = new HashSet<String>(); // contains just entrys with KEGG ids (no "undefined" entries)
    for (Entry entry : entries) {
      progress.DisplayBar();
      SBase spec = null;
      
      
      /*
       *  KEGG has pathways with duplicate entries (mostly signalling).
       *  Take a look, e.g. at the "MAPK signalling pathway" and "DUSP14"
       *  --
       *  BUT, if entry is no concrete KEGG entry (i.e. contains no ":"),
       *  then we should not group this to one. See e.g. the "ABC transporter
       *  pathway" with several groups called "undefined", but different content.
       *  Do NOT create just one species of all nodes called undefined.
       */
      if (entry.getName().contains(":") && !addedEntries.add(entry.getName())) {
        // Look for already added species from other entry
        // and link to this entry by adding the same species as "custom".
        Collection<Entry> col = p.getEntriesForName(entry.getName()); // should return at least 2 entries
        if ((col != null) && (col.size() > 0)) {
          Iterator<Entry> it = col.iterator();
          while (it.hasNext() && (spec = (SBase)it.next().getCustom())==null) {
            ;
          }
          entry.setCustom(spec);
        }
      }
      
      if (spec == null) {
        // Usual case if this entry is no duplicate.
        spec = addKGMLEntry(entry, p, model, compartment);
      }
      
      // Track reaction modifiers
      addToReactionModifierList(entry, spec, reactionModifiers);
    }
    
    // Add CellDesigner information to species / entries.
    if (addCellDesignerAnnots) {
      cdu.addCellDesignerAnnotationToAllSpecies(p);
    }
    
    // ------------------------------------------------------------------
    if(considerReactions()){
      // I noticed, that some reations occur multiple times in one KGML document,
      // (maybe its intended? e.g. R00014 in hsa00010.xml)
      Set<String> processedReactions = new HashSet<String>();
      
      
      // All species added. Parse reactions and relations.
      for (Reaction r : p.getReactions()) {
        if (processedReactions.add(r.getName())) {
          org.sbml.jsbml.Reaction sbReaction = addKGMLReaction(r,p,model,compartment,reactionModifiers);
          
          if (addCellDesignerAnnots && sbReaction!=null) {
            cdu.addCellDesignerAnnotationToReaction(sbReaction, r);
          }
        }
      }
      
      // Give a warning if we have no reactions.
      if (p.getReactions().size()<1 && !considerRelations()) {
        log.info(String.format("Pathway '%s' does not contain any reactions.", p.getName()!=null?p.getName():"Unknown"));
      }
    }
    // ------------------------------------------------------------------
    
    
    
    // Removing nodes here (removeOrphans) does not work, because
    // all CellDesigner annotations are static and don't get removed!
    
    
    // Finalize notes and annotations.
    notes.append(notesEndString);
    try {
      model.setNotes(notes.toString());
    } catch (Throwable t) {
      logger.warning(MessageFormat.format(
        "Cannot write notes for model ''{0}'' because of {1}: {2}.",
        model.getId(), t.getClass().getName(), Utils.getMessage(t)));
    }
    if (addCellDesignerAnnots) {
      cdu.addCellDesignerAnnotationToModel(p, model, compartment);
    }
    
    // Eventually add layout extension
    if (addLayoutExtension) {
      KEGG2SBMLLayoutExtension.addLayoutExtension(p, doc, model, true);
    }
    
    // Important: we don's satisfy all requirements for COBRA models, so we want to make sure the file is valid.
    if ((model.getSpeciesCount() > 0) && (model.getLevel() > 2)) {
      ((FBCModelPlugin) model.getPlugin(FBCConstants.getNamespaceURI(model.getLevel(), model.getVersion(), 2))).setStrict(false);
    }
    
    return doc;
  }
  
  /**
   * Create a 'default' compartment. Be careful, if called twice, will
   * create another 'default2' compartment, etc.
   * @see #createCompartment(Model, String)
   * @param model
   * @return
   */
  private Compartment createCompartment(Model model) {
    Compartment c = createCompartment(model, "default");
    /*
     * The default compartment is for species without an explicit compartment.
     * Hence, there is an SBO term for that (SBO:0000410):
     * 
     * "A compartment whose existence is inferred due to the presence of known
     * material entities which must be bounded, allowing the creation of
     * material entity pools."
     */
    c.setSBOTerm(410);
    return c;
  }
  
  /**
   * Create a compartment with the given {@code name}.
   * @param model
   * @param name
   * @return
   */
  private Compartment createCompartment(Model model, String name) {
    Compartment compartment = model.createCompartment(NameToSId(name));
    compartment.setName(name);
    // Create neccessary default compartment
    compartment.setSize(defaultCompartmentSize);
    //    compartment.setUnits(model.getUnitDefinition("volume"));
    compartment.setSpatialDimensions(3d); // a cell has 3 dimensions
    if (model.getLevel()>2) {
      // Set default value from l2v4
      compartment.setConstant(true);
    }
    //    compartment.setConstant(true);
    // Be careful: compartment ID and other compartment stuff are HARDCODED
    // in cellDesigner extension code generation!
    return compartment;
  }
  
  /**
   * Returns all entries of the pathway, with all group-nodes to the end of the list.
   * @param p
   * @return
   */
  private List<Entry> getEntriesWithGroupsAsLast(Pathway p) {
    // Move all group-nodes to the end of the list.
    // Because, if using the groups-extension, all members should already be created!
    Iterable<Entry> entriesArrayList = p.getEntries(); // original list
    LinkedList<Entry> entries = new LinkedList<Entry>(); // new list
    LinkedList<Entry> groupEntries = new LinkedList<Entry>(); // all groups
    if (entriesArrayList==null) {
      return entries;
    }
    
    // Add all non-groups and remember groups
    Iterator<Entry> it = entriesArrayList.iterator();
    while (it.hasNext()) {
      Entry e = it.next();
      if (e.getType().equals(EntryType.group) || e.hasComponents()) {
        groupEntries.add(e);
      } else {
        entries.add(e);
      }
    }
    
    // Add all groups
    for (Entry e: groupEntries) {
      entries.addLast(e);
    }
    
    return entries;
  }
  
  /**
   * Automatically returns an appropriate level and version. If previously
   * a level has been set manually with {@link #setLevelAndVersion(ValuePair)},
   * this is returned and the configuration of this class might be change
   * to be conform with the preset level.
   * 
   * @return the level and version of the SBML core (2,4) if no extension
   * should be used. Else: 3,1.
   */
  protected ValuePair<Integer, Integer> getLevelAndVersion() {
    
    // If a level is manually set, check if it supports extensions
    if (level!=null) {
      if (level<3) {
        if (addLayoutExtension) {
          log.warning("SBML supports extensions since Level 3. You've chosen to translate a document to Level 2 including the layout extension, what is not possible.\nDeactivating the layout extension for this translation.");
          setAddLayoutExtension(false);
        }
        if (useGroupsExtension) {
          log.warning("SBML supports extensions since Level 3. You've chosen to translate a document to Level 2 including the groups extension, what is not possible.\nDeactivating the groups extension for this translation.");
          setUseGroupsExtension(false);
        }
      }
      if (version == null) {
        // Actually, they should only be set in combination...
        if (level == 2) {
          version = 4;
        } else {
          version = 1;
        }
      }
      return new ValuePair<Integer, Integer>(level, version);
    }
    
    // Auto-adjust level as needed
    
    // Layout extension requires Level 3
    if (!addLayoutExtension && !useGroupsExtension) {
      return new ValuePair<Integer, Integer>(Integer.valueOf(2), Integer.valueOf(4));
    } else {
      return new ValuePair<Integer, Integer>(Integer.valueOf(3), Integer.valueOf(1));
    }
  }
  
  /**
   * Translate a KGML reaction to an SBML reaction.
   * @param r - the reaction to translate.
   * @param p - the pathway, in which the reaction is contained.
   * @param model - the current SBML model.
   * @param compartment - the current SBML compartment.
   * @param reactionModifiers - list of all reactionModifiers.
   * @throws XMLStreamException
   */
  private org.sbml.jsbml.Reaction addKGMLReaction(Reaction r, Pathway p, Model model, Compartment compartment,
    List<Info<String, ModifierSpeciesReference>> reactionModifiers) throws XMLStreamException {
    if (!reactionHasAtLeastOneSubstrateAndProduct(r, p)) {
      return null;
    }
    
    org.sbml.jsbml.Reaction sbReaction = model.createReaction();
    sbReaction.initDefaults();
    if (model.getLevel() > 2) {
      sbReaction.setFast(false);
      sbReaction.setReversible(true);
      sbReaction.setCompartment(compartment);
    }
    
    //Annotation rAnnot = new Annotation("");
    //rAnnot.setAbout(""); // IMPORTANT: Emtpy is wrong. it is being corrected in further on.
    //sbReaction.setAnnotation(rAnnot); // manchmal ist jSBML schon bescheuert... (Annotation darf nicht null sein, ist aber default null).
    
    StringBuffer notes = new StringBuffer(notesStartString);
    
    // Add substrates/ products
    sbReaction.setReversible(r.getType().equals(ReactionType.reversible));
    for (ReactionComponent rc : r.getSubstrates()) {
      int sbo = (r.getType().equals(ReactionType.irreversible))? 15 : 10;
      SpeciesReference sr = sbReaction.createReactant();
      configureReactionComponent(p, rc, sr, sbo); // 15=Substrate, 10=Reactant
    }
    for (ReactionComponent rc : r.getProducts()) {
      int sbo = (r.getType().equals(ReactionType.irreversible))? 11 : 10;
      SpeciesReference sr = sbReaction.createProduct();
      configureReactionComponent(p, rc, sr, sbo); // 11=Product
    }
    
    // Eventually add modifier
    List<ModifierSpeciesReference> modifier = getAllModifier(reactionModifiers,r);
    for (ModifierSpeciesReference mod : modifier) {
      sbReaction.addModifier(mod);
    }
    
    // Maybe add additional Miriam identifier (from other sources than kegg)
    if (r.isSetDatabaseIdentifiers()) {
      List<CVTerm> cvTerms = DatabaseIdentifierTools.getCVTerms(r.getDatabaseIdentifiers(), null);
      if ((cvTerms != null) && (cvTerms.size() > 0)) {
        for (CVTerm cvTerm : cvTerms) {
          sbReaction.addCVTerm(cvTerm);
        }
      }
    }
    
    
    // Add KEGG miriam identifier
    CVTerm reID = new CVTerm();
    CVTerm rePWs = new CVTerm();
    reID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
    reID.setBiologicalQualifierType(Qualifier.BQB_IS);
    rePWs.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
    rePWs.setBiologicalQualifierType(Qualifier.BQB_OCCURS_IN);
    
    for (String ko_id : r.getName().split(" ")) {
      String kgMiriamEntry = KeggInfos.getMiriamURNforKeggID(ko_id);
      if (kgMiriamEntry != null) {
        reID.addResource(AnnotationUtils.convertURN2URI(kgMiriamEntry));
      }
      
      // Retrieve further information via Kegg API
      KeggInfos infos = KeggInfos.get(ko_id, manager);
      if (infos.queryWasSuccessfull()) {
        notes.append("<p>");
        if (infos.getDefinition() != null) {
          notes.append(String.format("<b>Definition of %s%s%s:</b> %s<br/>\n",
            quotStart, ko_id.toUpperCase(), quotEnd, formatTextForHTMLnotes(infos.getDefinition()) ));
          logger.fine(sbReaction.getNotesString());
          // notes="<body xmlns=\"http://www.w3.org/1999/xhtml\"><p><b>&#8220;TEST&#8221;</b> A &lt;&#061;&gt;&#62;&#x3e;\u003E B<br/></p></body>";
        } else {
          notes.append(String.format("<b>%s</b><br/>\n", ko_id.toUpperCase()));
        }
        if (infos.getEquation() != null) {
          notes.append(String.format("<b>Equation for %s%s%s:</b> %s<br/>\n",
            quotStart, ko_id.toUpperCase(), quotEnd, EscapeChars.forHTML(infos.getEquation())));
        }
        String reactionImageURL = Pathway.getReactionPreviewPicture(ko_id, false);
        notes.append(String.format("<a href=\"%s\">", reactionImageURL));
        notes.append(String.format("<img src=\"%s\"/></a>\n", reactionImageURL));
        if (infos.getPathwayDescriptions() != null) {
          notes.append("<br/>\n<b>Occurs in:</b><br/>\n");
          notes.append("<ul>\n");
          for (String desc : infos.getPathwayDescriptions().split(",")) { // e.g. ",Glycolysis / Gluconeogenesis,Metabolic pathways"
            notes.append("<li>"+ formatTextForHTMLnotes(desc) + "</li>\n");
          }
          notes.append("</ul><br/>\n");
        }
        notes.append("</p>");
        
        if ((rePWs != null) && (infos.getPathways() != null)) {
          for (String pwId : infos.getPathways().split(",")) {
            String urn = AnnotationUtils.convertURN2URI(KeggInfos.miriam_urn_kgPathway + KeggInfos.suffix(pwId));
            if ((urn != null) && !rePWs.getResources().contains(urn)){
              rePWs.addResource(urn);
            }
          }
        }
      }
    }
    if (reID.getResourceCount() > 0) {
      sbReaction.addCVTerm(reID);
    }
    if (rePWs.getResourceCount() > 0) {
      sbReaction.addCVTerm(rePWs);
    }
    
    // Check the atom balance (only makes sense if reactions are corrected,
    // else, they are clearly wrong).
    if (autocompleteReactions && checkAtomBalance) {
      AtomCheckResult<Reaction> defects = AtomBalanceCheck.checkAtomBalance(manager, r, 1);
      if ((defects != null) && defects.hasDefects()) {
        notes.append("<p>");
        notes.append("<b><font color=\"#FF0000\">There are missing atoms in this reaction.</font></b><br/>" +
            "<small><i>Values lower than zero indicate missing atoms on the " +
            "substrate side, whereas positive values indicate missing atoms " +
            "on the product side.</i></small><br/>\n");
        notes.append(defects.getResultsAsHTMLtable());
        notes.append("</p>");
      } else if (defects==null) {
        notes.append("<p>");
        notes.append("<b><font color=\"#FF0000\">Could not check the atom balance of this reaction.</font></b>\n");
        notes.append("</p>");
      } else {
        notes.append("<p>");
        notes.append("<b><font color=\"#00FF00\">There are no missing atoms in this reaction.</font></b>\n");
        notes.append("</p>");
      }
    }
    
    
    // Finally, add the fully configured reaction.
    sbReaction.setName(r.getName());
    sbReaction.setId(NameToSId(r.getName()));
    notes.append(notesEndString);
    try {
      sbReaction.setNotes(notes.toString());
    } catch (Throwable t) {
      logger.warning(MessageFormat.format(
        "Cannot write notes for reaction ''{0}'' because of {1}: {2}.",
        sbReaction.getId(), t.getClass().getName(), Utils.getMessage(t)));
    }
    sbReaction.setMetaId("meta_" + sbReaction.getId());
    sbReaction.setSBOTerm(176); // biochemical reaction. Most generic SBO Term possible, for a reaction.
    //rAnnot.setAbout("#" + sbReaction.getMetaId());
    
    sbReaction.addCVTerm(new CVTerm(Qualifier.BQB_IS_DESCRIBED_BY, AnnotationUtils.convertURN2URI(KeggInfos.miriam_urn_eco + "ECO%3A0000313")));
    
    return sbReaction;
  }
  
  /**
   * Returns all reactionModifiers for the given reaction.
   * @param reactionModifiers - List of all modifiers (must be sorted)
   * @param r - The reaction, for which you want to have all modifiers.
   * @return list of modifiers, or empty list.
   */
  private List<ModifierSpeciesReference> getAllModifier (
    List<Info<String, ModifierSpeciesReference>> reactionModifiers, Reaction r) {
    return getAllModifier(reactionModifiers, r.getName());
  }
  
  /**
   * 
   * @param reactionModifiers
   * @param reaction
   * @return
   */
  private List<ModifierSpeciesReference> getAllModifier (
    List<Info<String, ModifierSpeciesReference>> reactionModifiers, String reaction) {
    List<ModifierSpeciesReference> modifier = new ArrayList<ModifierSpeciesReference>();
    String lName = reaction.toLowerCase().trim();
    int modifierPos = reactionModifiers.indexOf(lName);
    if (modifierPos < 0) {
      return modifier;
    }
    
    // Add the current item
    modifier.add(reactionModifiers.get(modifierPos).getInformation());
    //reactionModifiers.remove(modifierPos);
    
    // Multiple modifiers possible
    
    // Add all previous items
    int modifierPos2=modifierPos;
    while ((--modifierPos2) >= 0 && reactionModifiers.get(modifierPos2).getIdentifier().equalsIgnoreCase(lName)) {
      if (!modifier.contains(reactionModifiers.get(modifierPos2).getInformation())) {
        ;
      }
      modifier.add(reactionModifiers.get(modifierPos2).getInformation());
      //reactionModifiers.remove(modifierPos2);
    }
    
    // Add all following items
    modifierPos2=modifierPos;
    while ((++modifierPos2) < reactionModifiers.size() && reactionModifiers.get(modifierPos2).getIdentifier().equalsIgnoreCase(lName)) {
      if (!modifier.contains(reactionModifiers.get(modifierPos2).getInformation())) {
        ;
      }
      modifier.add(reactionModifiers.get(modifierPos2).getInformation());
      //reactionModifiers.remove(modifierPos2);
    }
    
    return modifier;
  }
  
  /**
   * Adds all available MIRIAM URNs and ids to the given species.
   * AND adds a description and more information from the KEGG api to the
   * species's description.
   * @param entry
   * @param spec
   * @throws XMLStreamException
   */
  public static void addMiriamURNs(Entry entry, SBase spec) throws XMLStreamException {
    // Get a map of existing identifiers or create a new one
    Map<DatabaseIdentifiers.IdentifierDatabases, Collection<String>> ids = new HashMap<DatabaseIdentifiers.IdentifierDatabases, Collection<String>>();
    if (entry instanceof EntryExtended) {
      ids = ((EntryExtended)entry).getDatabaseIdentifiers();
    }
    
    // Parse every gene/object in this node.
    for (String ko_id : entry.getName().split(" ")) {
      if (ko_id.trim().equalsIgnoreCase("undefined") || entry.hasComponents())
      {
        continue; // "undefined" = group node, which contains "Components"
      }
      
      
      // Retrieve further information via Kegg API -- Be careful: very slow! Precache all queries at top of this function!
      KeggInfos infos = KeggInfos.get(ko_id, manager);
      // Some infos can also be extracted if query was NOT succesfull
      
      
      // Add reactions as miriam annotation
      String reactionID = concatReactionIDs(entry.getParentPathway().getReactionsForEntry(entry), ArrayUtils.merge(entry.getReactions(), infos.getReaction_id()));
      if (reactionID != null && reactionID.length()>0) {
        Utils.addToMapOfSets(ids, IdentifierDatabases.KEGG_Reaction, reactionID.split("\\s"));
      }
      // Add all available identifiers (enzrez gene, ensembl, etc)
      infos.addAllIdentifiers(ids);
      
      if (infos.queryWasSuccessfull()) {
        
        // HTML Information
        StringBuffer notes = new StringBuffer(notesStartString);
        if ((infos.getDefinition() != null) && (infos.getName() != null)) {
          notes.append(String.format("<p><b>Description for %s%s%s:</b> %s</p>\n",
            quotStart, EscapeChars.forHTML(infos.getName()), quotEnd, formatTextForHTMLnotes(infos.getDefinition()) ));
        } else if (infos.getName() != null) {
          notes.append(String.format("<p><b>%s</b></p>\n", EscapeChars.forHTML(infos.getName())));
        }
        if (infos.containsMultipleNames()) {
          notes.append(String.format("<p><b>All given names:</b><br/>%s</p>\n",EscapeChars.forHTML(infos.getNames().replace(";", "")) ));
        }
        if (infos.getCas() != null) {
          notes.append(String.format("<p><b>CAS number:</b> %s</p>\n", infos.getCas()));
        }
        if (infos.getFormulaDirectOrFromSynonym(manager) != null) {
          notes.append(String.format("<p><b>Formula:</b> %s</p>\n", EscapeChars.forHTML(infos.getFormulaDirectOrFromSynonym(manager))));
          String ko_id_uc_t = ko_id.toUpperCase().trim();
          if (ko_id_uc_t.startsWith("CPD:")) {
            // KEGG and ChEBI provide picture for compounds (e.g., "C00118").
            notes.append(Pathway.getCompoundPreviewPicture(ko_id_uc_t, infos, Translator.path2models));
          }
        }
        if (entry.getType().equals(EntryType.map)) {
          // KEGG provides picture for referenced pathways (e.g., "path:hsa00620" => "map00620.gif").
          notes.append(Pathway.getPathwayPreviewPicture(ko_id));
        }
        if (infos.getMass() != null) {
          notes.append(String.format("<p><b>Mass:</b> %s</p>\n", infos.getMass()));
        }
        if (infos.getMolecularWeight() != null) {
          notes.append(String.format("<p><b>Molecular weight:</b> %s</p>\n", infos.getMolecularWeight()));
        }
        notes.append(notesEndString);
        spec.appendNotes(notes.toString());
      }
    }
    
    // Add all non-empty ressources.
    String pointOfView = entry.getRealType();
    if (pointOfView == null) {
      pointOfView = "protein";
    }
    if (pointOfView.equals("complex"))
    {
      pointOfView = "protein"; // complex are multple proteins.
    }
    List<CVTerm> cvTerms = DatabaseIdentifierTools.getCVTerms(ids, pointOfView);
    if ((cvTerms != null) && (cvTerms.size() > 0)) {
      for (CVTerm cvTerm : cvTerms) {
        spec.addCVTerm(cvTerm);
      }
    }
    
    // Set a static ECO Code
    // ECO_CODE "ECO:0000313"="imported information used in automatic assertion"
    spec.addCVTerm(new CVTerm(Qualifier.BQB_IS_DESCRIBED_BY, AnnotationUtils.convertURN2URI(KeggInfos.miriam_urn_eco + "ECO%3A0000313")));
  }
  
  
  /**
   * Creates a list of reactions identifiers. Trims the "rn:" prefix,
   * only contains unique and non null/empty strings.
   * @param reactions
   * @param reactionIDs
   * @return {@code null} if input did not contain valid reactionIDs.
   * else, a space-separated {@link String} with unique reaction ids.
   */
  public static String concatReactionIDs(Collection<Reaction> reactions, String... reactionIDs) {
    Set<String> ret = new HashSet<String>();
    
    // Add all reaction names from the list of reactions
    if (reactions != null) {
      for (Reaction r: reactions) {
        if ((r == null) || (r.getName() == null)) {
          continue;
        }
        String toAdd = KeggInfos.suffix(r.getName());
        if (toAdd.length() > 0) {
          ret.add(toAdd.toUpperCase());
        }
      }
    }
    
    // Add all string ids
    if (reactionIDs != null) {
      for (String r: reactionIDs) {
        if (r == null) {
          continue;
        }
        String toAdd = KeggInfos.suffix(r);
        if (toAdd.length() > 0) {
          ret.add(toAdd.toUpperCase());
        }
      }
    }
    
    // Create return value
    if (ret.size() < 1) {
      return null;
    } else if (ret.size() == 1) {
      return ret.iterator().next();
    } else {
      StringBuilder b = new StringBuilder();
      for (String item: ret) {
        if (b.length() > 0) {
          b.append(' ');
        }
        b.append(item);
      }
      return b.toString();
    }
  }
  
  /**
   * Assigns a {@link Qualifier} to {@code CVterm}, dependent
   * on the number of contained resources. If this CVTerm contains
   * one resource, {@code BQB_IS} is assigned, else
   * {@code BQB_HAS_VERSION}
   * is assignes.
   * @param CVterm any CVTerm
   */
  protected void setBiologicalQualifierISorHAS_VERSION(CVTerm CVterm) {
    if (CVterm.getResourceCount() > 1) {
      // Multiple proteins in one node
      CVterm.setBiologicalQualifierType(Qualifier.BQB_HAS_VERSION);
    } else {
      CVterm.setBiologicalQualifierType(Qualifier.BQB_IS);
    }
  }
  
  /**
   * Translates the given entry to jSBML.
   * 
   * @param entry KGML entry to add.
   * @param p pathway of the specified entry.
   * @param model current model.
   * @param compartment current compartment.
   * if it is an enzyme or similar.
   * @throws XMLStreamException
   */
  private SBase addKGMLEntry(Entry entry, Pathway p, Model model, Compartment compartment) throws XMLStreamException {
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
    if (entry.getType().equals(EntryType.reaction)) {
      // Reaction-nodes usually also occur as real reactions. They are only
      // required to be translated to nodes, if they are used in relations.
      if (!considerRelations()) {
        return null;
      }
    }
    
    /*
     * XXX: Gruppenknoten erstellen evtl. in einer SBML version >2 moeglich?
     * Gibt es sowas in SBML?
     * InCD -> ja, aber umsetzung ist ungenuegend (nur
     * zur visualisierung, keine SBML Species fuer alle species).
     * 
     * Geloest: ueber complexSpeciesAlias in CD. & MIRIAM in normal SBML
     * In simplen SBML ignoriert, da funktion nicht vorhanden.
     * Reaktionen werden stets so uebersetzt, wie sie im Dokument stehen
     * (meist nur vom Gruppenknoten aus, nicht von den Komponenten).
     * Das macht Sinn, da Gruppenknoten selbst nicht nur eine "Gruppe" ist,
     * sondern per se auch schon ein Protein o.Ae. definiert.
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
    //    boolean hasMultipleIDs = false;
    //    if (entry.getName().trim().contains(" ")) hasMultipleIDs = true;
    if (entry.hasGraphics() && entry.getGraphics().getName().length() > 0) {
      name = entry.getGraphics().getName(); // + " (" + name + ")"; // Append ko Id(s) possible!
    }
    // Set name to real and human-readable name (from Inet data - Kegg API).
    name = getNameForEntry(entry);
    // ---
    
    // Initialize species object
    NamedSBase spec;
    String id = NameToSId(name.replace(' ', '_')); // defined in org.sbml.jsbml.NamedSBase
    if (useGroupsExtension && (entry.hasComponents() || entry.getType().equals(EntryType.group))) {
      spec = KEGG2SBMLGroupExtension.createGroup(p, model, entry, "group_" + id);
    } else {
      
      // Eventually assign a different compartment
      Compartment c = compartment;
      if (entry instanceof EntryExtended) {
        if (((EntryExtended) entry).isSetCompartment()) {
          for (Compartment c1: model.getListOfCompartments()) {
            if (c1.getName().equals(((EntryExtended) entry).getCompartment())) {
              c = c1;
              break;
            }
          }
          // If it has some compartment assigned, which is not yet initialized, create a new instance.
          if (c.equals(compartment)) {
            c = createCompartment(model, ((EntryExtended) entry).getCompartment());
          }
        }
      }
      spec = model.createSpecies(id, c);
    }
    
    if (spec instanceof Species) {
      if (model.getLevel() > 2) {
        /*
         * In order to obtain valid Level 3 models with identical properties than
         * in Level 2, we use the default value of earlier SBML releases:
         */
        ((Species) spec).setHasOnlySubstanceUnits(false);
        ((Species) spec).setBoundaryCondition(false);
        ((Species) spec).setConstant(false); // defined in org.sbml.jsbml.Variable
        
        
        KeggInfos infos = KeggInfos.get(entry.getName(), manager);
        
        if ((infos != null) && infos.queryWasSuccessfull()) {
          
          // Component.getName() might be a glycan and the chemical formula is only given for compounds
          // => Look if we have synonym identifers for KEGG compound and refetch
          String formula = infos.getFormulaDirectOrFromSynonym(manager);
          if (formula != null) {
            FBCSpeciesPlugin specPlug = (FBCSpeciesPlugin) spec.getPlugin(FBCConstants.getNamespaceURI(spec.getLevel(), spec.getVersion(), 2));
            try {
              specPlug.setChemicalFormula(formula);
            } catch (IllegalArgumentException exc) {
              logger.warning(exc.getClass().getSimpleName() + ": " + Utils.getMessage(exc));
            }
          }
        }
        
      }
      ((Species) spec).setInitialAmount(speciesDefaultInitialAmount);
      //((Species) spec).setUnits(model.getUnitDefinition("substance"));
    }
    
    // ID has to be at this place, because other refer to it by id and if id is not set. refenreces go to null.
    // spec.setId(NameToSId(entry.getName().replace(' ', '_')));
    if (spec instanceof NamedSBase) {
      spec.setMetaId("meta_" + spec.getId()); // defined in org.sbml.jsbml.SBase
    }
    
    //Annotation specAnnot = new Annotation("");
    //specAnnot.setAbout("");
    //spec.setAnnotation(specAnnot); // manchmal ist jSBML schon bescheurt...
    StringBuffer notes = new StringBuffer(notesStartString);
    if (entry.isSetLink()) {
      notes.append(String.format("<a href=\"%s\">Original KEGG Entry</a><br/>\n", entry.getLink().replace("&", "&amp;")));
    }
    
    
    // Process Component information
    if (entry.hasComponents() || entry.getType().equals(EntryType.group)) {
      /////////////////////////////////////////////////////
      // TODO: Globally try to replace species by AbstractSBase
      /////////////////////////////////////////////////
      
      StringBuilder notesAppend = new StringBuilder(
        String.format("<p>This species is a group, consisting of %s components:<br/><ul>", entry.getComponents().size()));
      CVTerm cvt = new CVTerm(Type.BIOLOGICAL_QUALIFIER,Qualifier.BQB_IS_ENCODED_BY);
      for (int c:entry.getComponents()) {
        Entry ce = p.getEntryForId(c);
        if (ce == null) {
          notesAppend.append("<li>Unknown</li>");
        } else {
          String ce_name = getNameForEntry(ce);
          notesAppend.append(String.format("<li>%s</li>", ce_name));
          
          // Append all kegg ids as "has_part" NLN: Should be "IS_ENCONDED_BY"
          for (String kg_id: ce.getName().split(" ")) {
            String kgMiriamEntry = AnnotationUtils.convertURN2URI(KeggInfos.getMiriamURNforKeggID(kg_id, ce.getType()));
            if (kgMiriamEntry != null) {
              cvt.addResource(kgMiriamEntry);
            }
          }
          
        }
        
      }
      if (cvt.getResourceCount() > 0) {
        spec.addCVTerm(cvt);
      }
      notesAppend.append("</ul></p>");
      notes.append(notesAppend.toString());
    }
    
    // Set notes here, so other methods (miriam) can append the notes.
    notes.append(notesEndString);
    try {
      spec.setNotes(notes.toString());
    } catch (Throwable t) {
      logger.warning(MessageFormat.format(
        "Cannot write notes for species ''{0}'' because of {1}: {2}.",
        spec.getId(), t.getClass().getName(), Utils.getMessage(t)));
    }
    
    // Set SBO Term
    spec.setSBOTerm(SBOMapping.getSBOTerm(entry));
    
    // Add Miriam URNs and Description
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
   * Creates a {@link ModifierSpeciesReference} for entry and spec and adds this reference
   * to the {@code reactionModifiers} list.
   * 
   * @param entry
   * @param spec
   * @param reactionModifiers
   */
  private void addToReactionModifierList(Entry entry, SBase spec, List<Info<String, ModifierSpeciesReference>> reactionModifiers) {
    if (!entry.hasReaction() || spec == null) {
      return;
    }
    
    for (String reaction: entry.getReactions()) {
      
      // Look if the element is already in out list.
      /* If we have duplicate entries (for visualization reasons) but only create one species,
       * then we would add 2 equal modifiers here. Thus, we need the "contains" check.
       */
      if (spec instanceof NamedSBase) {
        List<ModifierSpeciesReference> existingMods = getAllModifier(reactionModifiers, reaction);
        boolean alreadyInList = false;
        for (ModifierSpeciesReference m:existingMods) {
          if (m.getSpecies().equals(((NamedSBase)spec).getId())) {
            alreadyInList = true;
            break;
          }
        }
        if (alreadyInList) {
          continue;
        }
      }
      
      // Q: Ist es richtig, sowohl dem Modifier als auch der species eine neue id zu geben? A: Nein, ist nicht richtig.
      // spec.setSBOTerm(ET_SpecialReactionCase2SBO);
      ModifierSpeciesReference modifier = null;
      if (spec instanceof Species) {
        modifier = new ModifierSpeciesReference((Species)spec);
      } else if (spec instanceof NamedSBase) {
        modifier = new ModifierSpeciesReference(((NamedSBase)spec).getId());
      }
      if (modifier==null) {
        log.warning("Can not add rection modifier: " + spec);
        return;
      }
      modifier.setLevel(spec.getLevel());
      modifier.setVersion(spec.getVersion());
      
      // Annotation is empty in ModifierSpeciesReference
      //Annotation tempAnnot = new Annotation("");
      //tempAnnot.setAbout("");
      //modifier.setAnnotation(tempAnnot);
      if (addCellDesignerAnnots) {
        if (!CellDesignerUtils.containsCellDesignerNS(modifier)) {
          modifier.getAnnotation().addDeclaredNamespace("xmlns:celldesigner", "http://www.sbml.org/2001/ns/celldesigner");
          modifier.setNamespace("xmlns:celldesigner=http://www.sbml.org/2001/ns/celldesigner");
        }
      }
      modifier.setId(NameToSId("mod_" + reaction)); // FIXME: This avoids duplicates!!
      modifier.setMetaId("meta_" + modifier.getId());
      modifier.setName(modifier.getId());
      if (entry.getType().equals(EntryType.enzyme)   || entry.getType().equals(EntryType.gene)
          || entry.getType().equals(EntryType.group) || entry.getType().equals(EntryType.ortholog)
          || entry.getType().equals(EntryType.genes)) {
        // 1 & 2: klar. 3 (group): Doku sagt "MOSTLY a protein complex". 4 (ortholog): Kommen in
        // nicht-spezies spezifischen PWs vor und sind quasi otholog geclusterte gene.
        // 5. (genes) ist group in kgml versionen <0.7.
        modifier.setSBOTerm(SBOMapping.ET_EnzymaticModifier2SBO); // 460 = Enzymatic catalyst
      } else { // "Metall oder etwas anderes, was definitiv nicht enzymatisch wirkt"
        modifier.setSBOTerm(SBOMapping.ET_GeneralModifier2SBO); // 13 = Catalyst
      }
      
      // Remember modifier for later association with reaction.
      reactionModifiers.add(new Info<String, ModifierSpeciesReference>(reaction.toLowerCase().trim(), modifier));
    }
  }
  
  
  
  /**
   * Extends a list of {@link ReactionComponent}s by adding all children
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
        if (rc==null || rc.getName() == null || rc.getName().trim().length()<1) {
          continue;
        }
      }
      
      // Add the node anyways
      ret.add(rc);
      
      // If there are groupNodes, add also the children to the list.
      Entry s = p.getEntryForReactionComponent(rc);
      if (isGroupNode(s) && s.hasComponents()) {
        for (Integer c: s.getComponents()) {
          Entry child = p.getEntryForId(c);
          if (child!=null) {
            ret.add(new ReactionComponent(child));
          }
        }
      }
    }
    
    return ret;
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
    AbstractKEGGtranslator<SBMLDocument> k2s;
    if (new File(Translator.cacheFileName).exists()
        && new File(Translator.cacheFileName).length() > 1) {
      KeggInfoManagement manager = (KeggInfoManagement) InfoManagement.loadFromFilesystem(Translator.cacheFileName);
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
        if (args.length > 1) {
          batch.setChangeOutdirTo(args[1]);
        }
        batch.setTranslator(k2s);
        batch.setOutFormat(KEGGtranslatorIOOptions.Format.SBML);
        batch.parseDirAndSubDir();
        
      } else {
        // Single file mode.
        String outfile = args[0].substring(0,
          args[0].contains(".") ? args[0].lastIndexOf(".") : args[0].length())
          + ".sbml.xml";
        if (args.length > 1) {
          outfile = args[1];
        }
        
        Pathway p = KeggParser.parse(args[0]).get(0);
        try {
          k2s.translate(p, outfile);
        } catch (Throwable e) {
          e.printStackTrace();
        }
      }
      
      // Remember already queried objects (save cache)
      if (AbstractKEGGtranslator.getKeggInfoManager().hasChanged()) {
        KeggInfoManagement.saveToFilesystem(Translator.cacheFileName, AbstractKEGGtranslator.getKeggInfoManager());
      }
      
      return;
    }
    
    
    
    // Just a few test cases here.
    logger.info("Demo mode.");
    
    long start = System.currentTimeMillis();
    try {
      //k2s.translate("files/KGMLsamplefiles/hsa04010.xml", "files/KGMLsamplefiles/hsa04010.sbml.xml");
      k2s.translate("files/KGMLsamplefiles/hsa00010.xml", "files/KGMLsamplefiles/hsa00010.sbml.xml");
      
      // Remember already queried objects
      if (AbstractKEGGtranslator.getKeggInfoManager().hasChanged()) {
        KeggInfoManagement.saveToFilesystem(Translator.cacheFileName, AbstractKEGGtranslator.getKeggInfoManager());
      }
      
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    
    logger.info("Conversion took " + Utils.getTimeString((System.currentTimeMillis() - start)));
  }
  
  /**
   * A {@link Logger} for this class.
   */
  private static transient final Logger logger = Logger.getLogger(KEGG2jSBML.class.getName());
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.AbstractKEGGtranslator#considerRelations()
   */
  @Override
  protected boolean considerRelations() {
    return false;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.AbstractKEGGtranslator#considerReactions()
   */
  @Override
  protected boolean considerReactions() {
    return true;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.KEGGtranslator#isGraphicalOutput()
   */
  @Override
  public boolean isGraphicalOutput() {
    // Convert reaction-nodes to real reactions.
    return false;
  }
  
}
