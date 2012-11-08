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
 * Copyright (C) 2010-2012 by the University of Tuebingen, Germany.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.io.sif.InteractionRule;
import org.biopax.paxtools.io.sif.SimpleInteractionConverter;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level2.Level2Element;
import org.biopax.paxtools.model.level2.XReferrable;
import org.biopax.paxtools.model.level2.bioSource;
import org.biopax.paxtools.model.level2.biochemicalReaction;
import org.biopax.paxtools.model.level2.dataSource;
import org.biopax.paxtools.model.level2.entity;
import org.biopax.paxtools.model.level2.openControlledVocabulary;
import org.biopax.paxtools.model.level2.pathway;
import org.biopax.paxtools.model.level2.pathwayComponent;
import org.biopax.paxtools.model.level2.publicationXref;
import org.biopax.paxtools.model.level2.relationshipXref;
import org.biopax.paxtools.model.level2.smallMolecule;
import org.biopax.paxtools.model.level2.unificationXref;
import org.biopax.paxtools.model.level2.xref;
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.BiochemicalReaction;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.InteractionVocabulary;
import org.biopax.paxtools.model.level3.Level3Element;
import org.biopax.paxtools.model.level3.Named;
import org.biopax.paxtools.model.level3.Provenance;
import org.biopax.paxtools.model.level3.PublicationXref;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.SequenceModificationVocabulary;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;
import org.biopax.paxtools.model.level3.SmallMolecule;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.Xref;
import org.sbml.jsbml.CVTerm.Qualifier;

import de.zbit.kegg.AtomBalanceCheck;
import de.zbit.kegg.AtomBalanceCheck.AtomCheckResult;
import de.zbit.kegg.api.KeggInfos;
import de.zbit.kegg.api.cache.KeggInfoManagement;
import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.EntryType;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.kegg.parser.pathway.Reaction;
import de.zbit.kegg.parser.pathway.Relation;
import de.zbit.kegg.parser.pathway.SubType;
import de.zbit.kegg.parser.pathway.ext.EntryExtended;
import de.zbit.util.ArrayUtils;
import de.zbit.util.DatabaseIdentifierTools;
import de.zbit.util.DatabaseIdentifiers;
import de.zbit.util.DatabaseIdentifiers.DatabaseContent;
import de.zbit.util.DatabaseIdentifiers.IdentifierDatabases;
import de.zbit.util.EscapeChars;
import de.zbit.util.Species;
import de.zbit.util.StringUtil;
import de.zbit.util.Utils;
import de.zbit.util.objectwrapper.ValuePair;

/**
 * Abstract KEGG2BioPAX converter (also called KGML2BioPAX). This converter is
 * extended by others that are specialized in generating BioPAX level 2 or
 * BioPAX level 3 code.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public abstract class KEGG2BioPAX extends AbstractKEGGtranslator<Model> {
  public static final transient Logger log = Logger.getLogger(KEGG2BioPAX.class.getName());
  
  /**
   * The current {@link BioPAXFactory}.
   */
  protected BioPAXFactory factory = null;
  
  /**
   * The translated pathway.
   */
  protected Model model = null;
  
  /**
   * The actual translated pathway (real object diverges between L3 and L2)
   */
  protected BioPAXElement pathway = null;
  
  /**
   * The {@link BioPAXLevel}.
   */
  protected BioPAXLevel level = BioPAXLevel.L3;
  
  /**
   * This is for speed-improvement and saves the information if
   * {@link Entry}s with {@link EntryType#reaction} are in the
   * last translated pathway.
   */
  private boolean entriesWithTypeReactionAvailable=false;
  
  /**
   * @param manager
   */
  public KEGG2BioPAX(BioPAXLevel level, KeggInfoManagement manager) {
    super(manager);
    this.level = level;
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.AbstractKEGGtranslator#translateWithoutPreprocessing(de.zbit.kegg.parser.pathway.Pathway)
   */
  @Override
  protected Model translateWithoutPreprocessing(Pathway p) {
    
    // Init the factory and model
    factory = level.getDefaultFactory();
    model = factory.createModel();
    
    // Initialize a progress bar.
    initProgressBar(p,false,false);
    
    // The order of the following processes is important!
    log.fine("Creating the BioPAX pathway instance.");
    pathway = createPathwayInstance(p);
    log.fine("Creating the BioPAX entities.");
    createPhysicalEntities(p);
    if(considerReactions()){
      log.fine("Creating the BioPAX biochemical reactions.");
      createReactions(p);
    }
    if (considerRelations()) {
      log.fine("Creating the BioPAX relations/interactions.");
      createRelations(p);
    }
    // TODO: (eventuell) ???
    // pathway.addPATHWAY_COMPONENTS(pathwayComponent.class);
    
    return model;
  }
  
  /**
   * Please implement this class and initialize the BioPAX {@link pathway}
   * object (for your level) and add all possible annotations source, etc.
   * to the pathway.
   * @param p
   * @return the created pathway
   */
  protected abstract BioPAXElement createPathwayInstance(Pathway p);
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.AbstractKEGGtranslator#considerRelations()
   */
  @Override
  protected boolean considerRelations() {
    return true;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.AbstractKEGGtranslator#considerReactions()
   */
  @Override
  protected boolean considerReactions() {
    return true;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.AbstractKEGGtranslator#writeToFile(java.lang.Object, java.lang.String)
   */
  @Override
  public boolean writeToFile(Model model, String outFile) {
    if (new File(outFile).exists()) lastFileWasOverwritten=true;
    try {
      //      JenaIOHandler io = new JenaIOHandler(model.getLevel());
      BioPAXIOHandler io = new SimpleIOHandler(model.getLevel());
      model.setXmlBase("http://www.ra.cs.uni-tuebingen.de/software/KEGGtranslator/");
      io.convertToOWL(model, new FileOutputStream(outFile));
      
    } catch (Exception e) {
      log.log(Level.SEVERE, "Could not write BioPAX document.", e);
      return false;
    }
    return true;
  }
  
  
  public boolean writeToSIFFile(Model model, String outFile) {
    if (new File(outFile).exists()) lastFileWasOverwritten=true;
    try {
      SimpleInteractionConverter sic =
        new SimpleInteractionConverter(SimpleInteractionConverter
          .getRules(model.getLevel()).toArray(new InteractionRule[]{}));
      
      sic.writeInteractionsInSIF(model, new FileOutputStream(outFile));
      
    } catch (Exception e) {
      log.log(Level.SEVERE, "Could not write BioPAX document.", e);
      return false;
    }
    return true;
  }
  
  /**
   * Create a BioPAX cross-reference (xref).
   * @param db the {@link IdentifierDatabases}
   * @param id the actual identifier
   * @return {@link xref} for level 2 {@link BioPAXFactory}s abd
   * {@link Xref} for level 3 factories.
   */
  public BioPAXElement createXRef(IdentifierDatabases db, String id) {
    return createXRef(db, id, 0);
  }
  
  /**
   * Create a BioPAX cross-reference (xref).
   * @param db the {@link IdentifierDatabases}
   * @param id the actual identifier
   * @param type <ul><li>1 for an {@link UnificationXref}(=IS), </li><li>2 for
   * an {@link RelationshipXref} (=HAS_SOMETHING_TO_DO_WITH), </li><li>3
   * for a {@link PublicationXref}, </li><li>all 
   * other values for generic {@link Xref}s. </ul>
   * @return {@link xref} for level 2 {@link BioPAXFactory}s abd
   * {@link Xref} for level 3 factories.
   */
  public BioPAXElement createXRef(IdentifierDatabases db, String id, int type) {
    if (id==null) return null;
    if (type<1 || type>3) {
      type = 2; // Default to relationship.
    }
    String formattedID = DatabaseIdentifiers.getFormattedID(db, id);
    if (!DatabaseIdentifiers.checkID(db, formattedID)) {
      log.warning("Skipping invalid database entry " + id);
      return null;
    }
    
    if (formattedID==null || formattedID.length()<1) formattedID = id;
    // Igor R. told me not to use the identifiers.org URL as URI
    // String uri = DatabaseIdentifiers.getMiriamURI(db, formattedID);
    
    // We cannot use nameToSId here, because it makes the ID unique, what is undesired. 
    String uri = formattedID.startsWith(db.toString().toUpperCase()) ? 
        formattedID : StringUtil.toWord(db.toString() + '_' + formattedID);
    uri += "_" + type; // We need to create a different XRef for unifications or relationships!
    
    // Avoid creating duplicates.
    if (model.getByID(uri)!=null) return model.getByID(uri);
    
    BioPAXElement xr = null;
    if (model.getLevel()==BioPAXLevel.L2) {
      
      Class<? extends BioPAXElement> instantiate = xref.class;
      if (type==1) {
        instantiate = unificationXref.class;
      } else if (type==2) {
        instantiate = relationshipXref.class;
      } else if (type==3) {
        instantiate = publicationXref.class;
      } else {
        // we can NOT instantiate xref.class
        instantiate = relationshipXref.class;
      }
      
      xr = model.addNew(instantiate, uri);
      pathwayComponentCreated(xr);
      ((xref)xr).setDB(db.getOfficialName());
      ((xref)xr).setID(formattedID);
      
    } else if (model.getLevel()==BioPAXLevel.L3) {
      
      Class<? extends BioPAXElement> instantiate = Xref.class;
      if (type==1) {
        instantiate = UnificationXref.class;
      } else if (type==2) {
        instantiate = RelationshipXref.class;
      } else if (type==3) {
        instantiate = PublicationXref.class;
      } else {
        // we can NOT instantiate Xref.class
        instantiate = RelationshipXref.class;
      }
      
      xr = model.addNew(instantiate, uri);
      pathwayComponentCreated(xr);
      ((Xref)xr).setDb(db.getOfficialName());
      ((Xref)xr).setId(formattedID);
    } else {
      log.severe(String.format("Level %s not supported.", factory.getLevel()));
    }
    
    return xr;
  }


  /**
   * Creates a biosource, corrsponding to the organism/species of
   * the input pathway <code>p</code>.
   * <p> Please call this method only once per model and save the
   * result somewhere, in case you need it multiple times.</p>
   * @param factory
   * @param p
   * @return either a {@link bioSource} for level 2, or a {@link BioSource} for level 3.
   */
  public BioPAXElement createBioSource(Pathway p) {
    String speciesString = "Unknown";
    String taxonID="";
    
    // Get from KEGG API
    KeggInfos orgInfos = KeggInfos.get("GN:" + p.getOrg(), manager); // Retrieve all organism information via KeggAdaptor
    if (orgInfos.queryWasSuccessfull()) {
      speciesString = orgInfos.getDefinition();
      taxonID = orgInfos.getTaxonomy().trim().replaceAll("\\s.*", "");
    } else {
      // Get Organism from internal list
      if (p.isSetOrg()) {
        speciesString = p.getOrg();
        Species species=null;
        try {
          species = Species.get(p.getOrg(), Species.KEGG_ABBR);
        } catch (IOException e) {
          log.log(Level.WARNING, "Could not get internal species list.", e);
        }
        if (species!=null) {
          if (species.isSetScientificName()) {
            speciesString = species.getScientificName();
          }
          if (species.isSetTaxonomyId()) {
            taxonID = species.getNCBITaxonID().toString();
          }
        }
      }
    }
    
    BioPAXElement biosource = null;
    if (model.getLevel()==BioPAXLevel.L2) {
      bioSource bioSource = model.addNew(bioSource.class, '#'+NameToSId(speciesString));
      bioSource.setNAME(speciesString);
      if (taxonID!=null && taxonID.length()>0) {
        unificationXref uxr = (unificationXref) createXRef(IdentifierDatabases.NCBI_Taxonomy, taxonID, 1);
        if (uxr!=null) {
          bioSource.setTAXON_XREF(uxr);
        }
      }
      biosource = bioSource;
      
    } else if (model.getLevel()==BioPAXLevel.L3) {
      BioSource bioSource = model.addNew(BioSource.class, NameToSId(speciesString));
      //bioSource.setName(Collections.singleton(speciesString));
      bioSource.setDisplayName(createDisplayName(speciesString));
      if (taxonID!=null && taxonID.length()>0) {
        UnificationXref uxr = (UnificationXref) createXRef(IdentifierDatabases.NCBI_Taxonomy, taxonID, 1);
        if (uxr!=null) {
          bioSource.addXref(uxr);
        }
      }
      biosource = bioSource;
    } else {
      log.severe(String.format("Level %s not supported.", factory.getLevel()));
    }
    pathwayComponentCreated(biosource);
    
    return biosource;
  }
  
  /**
   * Creates a cross-reference to the KEGGtranslator publication.
   * @return PublicationXref to the KEGGtranslator publication.
   */
  protected BioPAXElement getPublicationXref() {
    BioPAXElement xr = createXRef(IdentifierDatabases.PubMed, "21700675", 3);
    if (xr instanceof PublicationXref) {
      ((PublicationXref) xr).setTitle("KEGGtranslator: visualizing and converting the KEGG PATHWAY database to various formats");
      ((PublicationXref) xr).setYear(2011);
      ((PublicationXref) xr).addUrl("http://www.ncbi.nlm.nih.gov/pubmed/21700675");
      ((PublicationXref) xr).addAuthor("Andreas Zell");
      ((PublicationXref) xr).addAuthor("Andreas Dräger");
      ((PublicationXref) xr).addAuthor("Clemens Wrzodek");
      ((PublicationXref) xr).addSource("Bioinformatics 2011, 27(16), 2314-2315");
      
    } else if (xr instanceof publicationXref) {
      ((publicationXref) xr).setTITLE("KEGGtranslator: visualizing and converting the KEGG PATHWAY database to various formats");
      ((publicationXref) xr).setYEAR(2011);
      ((publicationXref) xr).addURL("http://www.ncbi.nlm.nih.gov/pubmed/21700675");
      ((publicationXref) xr).addAUTHORS("Wrzodek C., Dräger A., Zell A.");
      ((publicationXref) xr).addSOURCE("Bioinformatics 2011, 27(16), 2314-2315");
    }
    
    return xr;
  }
  
  /**
   * Creates source references to KEGGtranslator, KEGG database itself and
   * (eventually) to the original format of the pathway source.
   * <p> Please call this method only once per model and save the
   * result somewhere, in case you need it multiple times.</p>
   * @param p
   * @return
   */
  public Collection<BioPAXElement> createDataSources(Pathway p) {
    Collection<BioPAXElement> ret = new LinkedList<BioPAXElement>();
    
    if (model.getLevel()==BioPAXLevel.L2) {
      dataSource ds = model.addNew(dataSource.class, NameToSId(System.getProperty("app.name"))+"_DataSource");
      pathwayComponentCreated(ds);
      ds.setNAME(Collections.singleton(System.getProperty("app.name")));
      ds.setCOMMENT(Collections.singleton("http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator/"));
      ds.addXREF((xref) getPublicationXref());
      ret.add(ds);
      
      ds = model.addNew(dataSource.class, "KEGG_DataSource");
      pathwayComponentCreated(ds);
      ds.setNAME(Collections.singleton("KEGG Data"));
      ds.setCOMMENT(Collections.singleton("http://www.genome.jp/kegg/"));
      ret.add(ds);
      
      if (p!=null && p.getOriginFormatName()!=null && !p.getOriginFormatName().equalsIgnoreCase("kgml") 
          && p.getOriginFormatName().length()>0) {
        ds = model.addNew(dataSource.class, NameToSId(p.getOriginFormatName())+"_DataSource");
        pathwayComponentCreated(ds);
        ds.setNAME(Collections.singleton(p.getOriginFormatName()+" Data"));
        ret.add(ds);
      }
    } else if (model.getLevel()==BioPAXLevel.L3) {
      Provenance ds = model.addNew(Provenance.class, NameToSId(System.getProperty("app.name"))+"_DataSource");
      pathwayComponentCreated(ds);
//      ds.setName(Collections.singleton(System.getProperty("app.name")));
      ds.setDisplayName(System.getProperty("app.name"));
      ds.addComment("http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator/");
      ds.addXref((Xref) getPublicationXref());
      ret.add(ds);
      
      ds = model.addNew(Provenance.class, "KEGG_DataSource");
      pathwayComponentCreated(ds);
//      ds.setName(Collections.singleton("KEGG Data"));
      //ds.setStandardName("KEGG database"); // No need to duplicate information
      ds.setDisplayName("KEGG database");
      ds.addComment("http://www.genome.jp/kegg/");
      ret.add(ds);
      
      if (p!=null && p.getOriginFormatName()!=null && !p.getOriginFormatName().equalsIgnoreCase("kgml") 
          && p.getOriginFormatName().length()>0) {
        ds = model.addNew(Provenance.class, NameToSId(p.getOriginFormatName())+"_DataSource");
        pathwayComponentCreated(ds);
        ds.setName(Collections.singleton(p.getOriginFormatName()+" Data"));
        ret.add(ds);
      }
    }
    
    return ret;
  }
  
  /**
   * Queries the KEGG API and adds various identifiers as {@link Xref}s,
   * further adds EC-Numbers, description, equation, etc.
   * @param r
   * @param element
   */
  public void addAnnotations(Reaction r, BioPAXElement reaction) {
    // Various Annotations
    for (String ko_id : r.getName().split(" ")) {
      BioPAXElement xr = createXRef(IdentifierDatabases.KEGG_Reaction, ko_id, 1);
      if (xr!=null) {
        if (reaction instanceof XReferrable) {
          ((XReferrable) reaction).addXREF((xref) xr);
        } else if (reaction instanceof org.biopax.paxtools.model.level3.XReferrable) {
          ((org.biopax.paxtools.model.level3.XReferrable) reaction).addXref((Xref) xr);
        }
      }
      
      
      // Retrieve further information via Kegg API
      KeggInfos infos = KeggInfos.get(ko_id, manager);
      if (infos.queryWasSuccessfull()) {
        
        // Add all EC Numbers
        if (infos.getEnzymes()!=null) {
          Set<String> ec = new HashSet<String>();
          ec.addAll(Arrays.asList(infos.getEnzymes().split("\\s")));
          ec.remove(""); ec.remove(null);
          if (reaction instanceof biochemicalReaction) {
            ((biochemicalReaction) reaction).setEC_NUMBER(ec);
          } else if (reaction instanceof BiochemicalReaction) {
            for (String ec_number: ec) {
              ((BiochemicalReaction) reaction).addECNumber(ec_number);
            }
          }
        }
        
        if (infos.getDefinition() != null) {
          String def = String.format("Definition of %s: %s", ko_id.toUpperCase(), (infos.getDefinition())); // Paxtools escapes HTML-chars automatically
          if (reaction instanceof Level2Element) {
            ((Level2Element) reaction).addCOMMENT(def);
          } else if (reaction instanceof Level3Element) {
            ((Level3Element) reaction).addComment(def);
          }
        }
        if (infos.getEquation() != null) {
          String equation = String.format("Equation: %s", EscapeChars.forHTML(infos.getEquation()));
          if (reaction instanceof Level2Element) {
            ((Level2Element) reaction).addCOMMENT(equation);
          } else if (reaction instanceof Level3Element) {
            ((Level3Element) reaction).addComment(equation);
          }
        }
        if (infos.getPathwayDescriptions() != null) {
          StringBuilder notes = new StringBuilder("Occurs in: ");
          notes.append(infos.getPathwayDescriptions());
          if (reaction instanceof Level2Element) {
            ((Level2Element) reaction).addCOMMENT(notes.toString());
          } else if (reaction instanceof Level3Element) {
            ((Level3Element) reaction).addComment(notes.toString());
          }
        }
        
        
        if (infos.getPathways() != null) {
          for (String pwId : infos.getPathways().split(",")) {
            xr = createXRef(IdentifierDatabases.KEGG_Pathway, pwId, 2);
            if (xr!=null) {
              if (reaction instanceof XReferrable) {
                ((XReferrable) reaction).addXREF((xref) xr);
              } else if (reaction instanceof org.biopax.paxtools.model.level3.XReferrable) {
                ((org.biopax.paxtools.model.level3.XReferrable) reaction).addXref((Xref) xr);
              }
            }
            
          }
        }
      }
    }
  }
  
  /**
   * Queries the KEGG API and adds various identifiers as {@link Xref}s,
   * further adds description as comment, synonyms, mol. weight and
   * chemical formula for compounds, etc.
   * @param entry
   * @param element
   */
  public void addAnnotations(Entry entry, BioPAXElement element) {
    
    // Get a map of existing identifiers or create a new one
    Map<DatabaseIdentifiers.IdentifierDatabases, Collection<String>> ids = new HashMap<DatabaseIdentifiers.IdentifierDatabases, Collection<String>>();
    if (entry instanceof EntryExtended) {
      ids = ((EntryExtended)entry).getDatabaseIdentifiers();
    }
    
    // Parse every gene/object in this node.
    for (String ko_id : entry.getName().split(" ")) {
      if (ko_id.trim().equalsIgnoreCase("undefined")) continue;
      
      // Retrieve further information via Kegg API -- Be careful: very slow! Precache all queries at top of this function!
      KeggInfos infos = KeggInfos.get(ko_id, manager);
      // Some infos can also be extracted if query was NOT succesfull
      
      // Add reactions as miriam annotation
      String reactionID = KEGG2jSBML.concatReactionIDs(entry.getParentPathway().getReactionsForEntry(entry), ArrayUtils.merge(entry.getReactions(), infos.getReaction_id()));
      if (reactionID != null && reactionID.length()>0) {
        Utils.addToMapOfSets(ids, IdentifierDatabases.KEGG_Reaction, reactionID.split("\\s"));
      }
      // Add all available identifiers (entrez gene, ensembl, etc)
      infos.addAllIdentifiers(ids);
      
      if (infos.queryWasSuccessfull()) {
        
        // HTML Information
        if (infos.getNames()!=null) {
          for (String synonym: infos.getNames().split("((;)|(,\\s))")) {
            if (synonym.trim().length()>0) {
              if (element instanceof entity) {
                ((entity) element).addSYNONYMS(synonym);
              } else if (element instanceof Named) {
                ((Named) element).addName(synonym);
              }
            }
          }
        }
        if (infos.getDefinition()!=null) {
          if (element instanceof Level2Element) {
            ((Level2Element) element).addCOMMENT((infos.getDefinition())); // Paxtools escapes the chars.
          } else if (element instanceof Level3Element) {
            ((Level3Element) element).addComment((infos.getDefinition()));
          }
        }
        
        // Mass and Formula for small molecules
        if (element instanceof smallMolecule) {
          if (infos.getFormulaDirectOrFromSynonym(manager) != null) {
            ((smallMolecule) element).setCHEMICAL_FORMULA(infos.getFormulaDirectOrFromSynonym(manager));
          }
          if (infos.getMolecularWeight() != null) {
            ((smallMolecule) element).setMOLECULAR_WEIGHT(getNumber(infos.getMolecularWeight()));
          } else if (infos.getMass()!=null) {
            ((smallMolecule) element).setMOLECULAR_WEIGHT(getNumber(infos.getMass()));
          }
        } else if (element instanceof SmallMolecule) {
          if (infos.getFormulaDirectOrFromSynonym(manager) != null || infos.getMass() != null) {
            BioPAXElement refNative = getEntityReference(element);
            if (refNative==null || refNative instanceof SmallMoleculeReference) {
              // should always be true
              SmallMoleculeReference ref = (SmallMoleculeReference) refNative; 
              if (ref==null) {
                ref = model.addNew(SmallMoleculeReference.class, 
                  ensureUniqueRDFId(element.getRDFId() + KEGG2BioPAX_level3.EntityReferenceSuffix));
                pathwayComponentCreated(ref);
                ((SmallMolecule) element).setEntityReference(ref);
              }
              
              // Add some Xrefs to the SmallMoleculeReference
              // This is now done later in the specific level 3 class!
              //addSmallMoleculeXRefs(ref, ids);
              
              if (infos.getFormulaDirectOrFromSynonym(manager) != null) {
                ref.setChemicalFormula(infos.getFormulaDirectOrFromSynonym(manager));
              }
              if (infos.getMolecularWeight() != null) {
                ref.setMolecularWeight((float) getNumber(infos.getMolecularWeight()));
              } else if (infos.getMass() != null) {
                ref.setMolecularWeight((float) getNumber(infos.getMass()));
              }
            }
          }
        }
      }
    }
    
    // Add X-REFs
    String pointOfView = entry.getRealType();
    if (pointOfView == null) pointOfView = "protein";
    if (pointOfView.equals("complex")) pointOfView = "protein"; // complex are multple proteins.
    boolean hadAlreadyOneUnification = false;
    for (IdentifierDatabases db: ids.keySet()) {
      Collection<?> id = ids.get(db);
      if (id==null) continue;
      for (Object i: id) {
        // We should only add EXACTLY ONE unification XRef
        int type = infereType(db, pointOfView, i.toString());
        if (type==1 && hadAlreadyOneUnification) {
          type = 2; // Switch to relationship
        }
        // Unfortunately, biopax does not allow grouping multiple ids in one xref bag for one db...
        BioPAXElement xref = createXRef(db, i.toString(), type);
        if (xref!=null) {
          if (UnificationXref.class.isAssignableFrom(xref.getModelInterface()) ||
              unificationXref.class.isAssignableFrom(xref.getModelInterface())) {
            hadAlreadyOneUnification = true;
          }
          if (element instanceof XReferrable) {
            ((XReferrable) element).addXREF((org.biopax.paxtools.model.level2.xref) xref);
          } else if (element instanceof org.biopax.paxtools.model.level3.XReferrable) {
            ((org.biopax.paxtools.model.level3.XReferrable) element).addXref((Xref) xref);
          }
        }
        
      }
    }
  }


  /**
   * Adds all available small molecular xrefs.
   * to a {@link BioPAXElement}.
   * @param element
   * @param ids
   */
  private void addSmallMoleculeXRefs(BioPAXElement element, Map<DatabaseIdentifiers.IdentifierDatabases, Collection<String>> ids) {
    if (ids==null) return;
    
    for (IdentifierDatabases db : IdentifierDatabases.values()) {
      DatabaseContent t = DatabaseIdentifiers.getDatabaseType(db);
      if (t!=null && t.equals(DatabaseContent.small_molecule)) {
        
        // Add a Xref
        if (ids.containsKey(db)) {
          Collection<String> ids2 = ids.get(db);
          for (String id: ids2) {
            // Unfortunately, biopax does not allow grouping multiple ids in one xref bag for one db...
            int xrefType = infereType(db, "small_molecule", id);
            BioPAXElement xref = createXRef(db, id, xrefType);
            
            if (xref!=null) {
              if (element instanceof XReferrable) {
                ((XReferrable) element).addXREF((org.biopax.paxtools.model.level2.xref) xref);
              } else if (element instanceof org.biopax.paxtools.model.level3.XReferrable) {
                ((org.biopax.paxtools.model.level3.XReferrable) element).addXref((Xref) xref);
              }
            }
            
          }
        }
      }
    }
  }
  
  
  /**
   * 
   * @param stringStartWithNum e.g. "123.456 mol"
   * @return number from string e.g. 123.456
   */
  private double getNumber(String stringStartWithNum) {
    stringStartWithNum = stringStartWithNum.trim();
    boolean pointEncountered=false;
    int i=0;
    for (; i<stringStartWithNum.length(); i++) {
      char c = stringStartWithNum.charAt(i);
      if (Character.isDigit(c)) continue;
      else if (c=='.') {
        if (pointEncountered) break;
        pointEncountered = true;
      } else {
        break;
      }
    }
    
    if (i==0) return 0d;
    if (i>=stringStartWithNum.length()) {
      return Double.parseDouble(stringStartWithNum);
    } else {
      if (stringStartWithNum.charAt(i)-1=='.') i--;
      if (i==0) return 0d;
      else return Double.parseDouble(stringStartWithNum.substring(0, i));
    }
    
    
  }
  
  
  /**
   * @param db
   * @param pointOfView
   * @param id
   * @return type <ul><li>1 for an {@link UnificationXref}(=IS), </li><li>2 for
   * an {@link RelationshipXref} (=HAS_SOMETHING_TO_DO_WITH), </li><li>3
   * for a {@link PublicationXref}, </li><li>all 
   * other values for generic {@link Xref}s. </ul>
   */
  private int infereType(IdentifierDatabases db, String pointOfView, String id) {
    // Look for publication
    if (DatabaseIdentifiers.getDatabaseType(db)==DatabaseIdentifiers.DatabaseContent.publication) {
      return 3;
    }
    
    // Let the qualifier handle these things
    Qualifier bqb = DatabaseIdentifierTools.getBQBQualifier(db, pointOfView, id);
    if (bqb==Qualifier.BQB_IS || bqb == Qualifier.BQB_HAS_VERSION) {
      return 1;
    } else {
      return 2;
    }
  }
  
  
  /**
   * Adds all KEGG {@link Reaction}s to the BioPAX {@link #model}.
   * @param p
   */
  public void createReactions(Pathway p) {
    // I noticed, that some reations occur multiple times in one KGML document,
    // (maybe its intended? e.g. R00014 in hsa00010.xml)
    Set<String> processedReactions = new HashSet<String>();
    
    // All species added. Parse reactions and relations.
    for (Reaction r : p.getReactions()) {
      if (!reactionHasAtLeastOneSubstrateAndProduct(r, p)) continue;
      
      if (processedReactions.add(r.getName())) {
        BioPAXElement reaction = addKGMLReaction(r,p);
        
        // Check the atom balance (only makes sense if reactions are corrected,
        // else, they are clearly wrong).
        if (autocompleteReactions && checkAtomBalance) {
          AtomCheckResult defects = AtomBalanceCheck.checkAtomBalance(manager, r, 1);
          StringBuilder notes = new StringBuilder();
          if (defects!=null && defects.hasDefects()) {
            notes.append("There are missing atoms in this reaction. " +
              "Values lower than zero indicate missing atoms on the " +
              "substrate side, whereas positive values indicate missing atoms " +
            "on the product side: ");
            notes.append(defects.getDefects().toString());
          } else if (defects==null) {
            notes.append("Could not check the atom balance of this reaction.");
          } else {
            notes.append("There are no missing atoms in this reaction.");
          }
          
          // Add comment to element
          if (reaction instanceof Level2Element) {
            ((Level2Element)reaction).addCOMMENT(notes.toString());
          } else if (reaction instanceof Level3Element) {
            ((Level3Element)reaction).addComment(notes.toString());
          }
          
        }
        
        setReactionToReactionEntry(p, r, reaction);
      }
    }
    
    // Give a warning if we have no reactions.
    if (p.getReactions().size()<1 && !considerRelations()) {
      log.info(String.format("Pathway '%s' does not contain any reactions.", p.getName()!=null?p.getName():"Unknown"));
    }
  }
  
  /**
   * KGML can provide {@link Entry}s with {@link EntryType#reaction}. These
   * are translated to real reactions. Thus, they have no custom {@link BioPAXElement}
   * set.
   * <br/>But these entries might be reused in relations (i.e., relations involving
   * reactions) and thus, this method will set the custom attribute of an {@link Entry}
   * to the BioPAX reaction given as <code>reaction</code> .
   * @param p
   * @param r KEGG reaction
   * @param reaction BioPAX reaction
   */
  private void setReactionToReactionEntry(Pathway p, Reaction r, BioPAXElement reaction) {
    if (!entriesWithTypeReactionAvailable) {
      return;
    }
    for (Entry e: p.getEntries()) {
      if (e.getType().equals(EntryType.reaction)) {
        if (e.getCustom()==null && e.getName().contains(r.getName())) {
          e.setCustom(reaction);
        }
      }
    }
  }
  
  
  /**
   * Adds all KEGG {@link Relation}s to the BioPAX {@link #model}.
   * @param p
   */
  public void createRelations(Pathway p) {
    Set<String> processedRelations = new HashSet<String>();
    
    
    
    // Resort the list:
    // Try to keep the current order, but move the "block" of all phosphorylations (and similar)
    // to top of list, append the reverse reactions (DEPHOSPHORYLATION) and then append the rest.
    List<Relation> sorted = new ArrayList<Relation>(p.getRelations().size());
    int reverseReactionsPosition = 0;
    int addedReverseReactions = 0;
    Set<String> avoidDuplicates = new HashSet<String>();
    for (Relation r : p.getRelations()) {
      Collection<String> subtypes = r.getSubtypesNames();
      
      // Avoid duplicates
      Entry eOne = p.getEntryForId(r.getEntry1());
      Entry eTwo = p.getEntryForId(r.getEntry2());
      String uniqueString = eOne.getName() + "|" + eTwo.getName() + "|" + ArrayUtils.implode(subtypes, "|", true);
      if (!avoidDuplicates.add(uniqueString)) {
        continue; // Duplicate realtion
      }
      
      // Insert into sorted list
      if (subtypes.contains(SubType.PHOSPHORYLATION) || subtypes.contains(SubType.METHYLATION) ||
        subtypes.contains(SubType.UBIQUITINATION) || subtypes.contains(SubType.GLYCOSYLATION)) {
        sorted.add(reverseReactionsPosition, r); // append to top of list
        reverseReactionsPosition++;
      } else if (subtypes.contains(SubType.DEPHOSPHORYLATION)) {
        sorted.add(reverseReactionsPosition+addedReverseReactions, r); // append below phosphorylations
        addedReverseReactions++;
      } else {
        sorted.add(r); // add to end of list
      }
    }
    
    
    // Appropriately add all relations to the model
    for (Relation r : sorted) {
      if (processedRelations.add(r.toString())) {
        log.finer("Processing " + r.toString());
        addKGMLRelation(r,p);
      }
    }
    
    // Give a warning if we have no reactions.
    if (p.getRelations().size()<1 && !considerReactions()) {
      log.info(String.format("Pathway '%s' does not contain any relations.", p.getName()!=null?p.getName():"Unknown"));
    }
  }
  
  /**
   * Adds all entries of the given pathway to the BioPAX model.
   * @param p
   */
  public void createPhysicalEntities(Pathway p) {
    // Create species
    ArrayList<Entry> entries = p.getEntries();
    Set<String> addedEntries = new HashSet<String>(); // contains just entrys with KEGG ids (no "undefined" entries) 
    for (Entry entry : entries) {
      progress.DisplayBar();
      BioPAXElement spec = null;
      if (entry.getType().equals(EntryType.reaction)) {
        entriesWithTypeReactionAvailable=true;
      }
      
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
          while (it.hasNext() && (spec = (BioPAXElement)it.next().getCustom())==null);
          entry.setCustom(spec);
        }
      }
      
      if (spec==null) {
        // Usual case if this entry is no duplicate.
        spec = addEntry(entry, p);
      }
    }
  }
  
  /**
   * Gets or creates a {@link InteractionVocabulary} corresponding to the
   * given {@link SubType}.
   * @return  {@link InteractionVocabulary} for level 3 and
   * {@link openControlledVocabulary} for level 2.
   */
  protected BioPAXElement getInteractionVocuabulary(SubType st) {
    String formattedName = st.getName().trim().replace(' ', '_').replace("/", "_or_");
    
    //String rfid = "#relation_subtype_" + formattedName;
    String rfid = getVocabularyID(st, false);
    BioPAXElement voc=null;
    if (level == BioPAXLevel.L3) {
      voc = (InteractionVocabulary) model.getByID(rfid);
    } else if (level == BioPAXLevel.L2) {
      voc = (openControlledVocabulary) model.getByID(rfid);
    }
    
    // Term is not yet available => create it.
    if (voc==null) {
      // Convert to a term that is a child of 'MI:0190' (Molecular Interaction Ontology)
      ValuePair<String, Integer> miTerm = SBOMapping.getMITerm(st.getName());
      String termName = miTerm!=null?miTerm.getA():null;//formattedName;
      
      if (level == BioPAXLevel.L3) {
        voc = model.addNew(InteractionVocabulary.class, rfid);
        pathwayComponentCreated(voc);
        if (termName!=null) {
          ((InteractionVocabulary)voc).addTerm(termName);
        }
        ((InteractionVocabulary)voc).addComment(formattedName);// In all cases, add the original KEGG name
      } else if (level == BioPAXLevel.L2) {
        voc = model.addNew(openControlledVocabulary.class, rfid);
        pathwayComponentCreated(voc);
        if (termName!=null) {
          ((openControlledVocabulary)voc).addTERM(termName);
        }
        ((openControlledVocabulary)voc).addCOMMENT(formattedName);// In all cases, add the original KEGG name
      }
      

      // Add additional XRefs to MI, SBO and GO
      boolean addedOneUnificationXRef = false;
      if (miTerm!=null && miTerm.getB()!=null && miTerm.getB()>0) {
        BioPAXElement xr = createXRef(IdentifierDatabases.MI, Integer.toString(miTerm.getB()), 1);
        addOntologyXRef(voc, xr, miTerm.getA());
        addedOneUnificationXRef = true;
      }
      
      if (!addedOneUnificationXRef) { // Unfortunately, the spec does not allow relationship xrefs...
        int sbo = SBOMapping.getSBOTerm(st.getName());
        if (sbo>0) {
          BioPAXElement xr = createXRef(IdentifierDatabases.SBO, Integer.toString(sbo), addedOneUnificationXRef?2:1);
          addOntologyXRef(voc, xr, formattedName);
          addedOneUnificationXRef = true;
        }
      }
      
      if (!addedOneUnificationXRef) { // Unfortunately, the spec does not allow relationship xrefs...
        int go = SBOMapping.getGOTerm(st.getName());
        if (go>0) {
          BioPAXElement xr = createXRef(IdentifierDatabases.GeneOntology, Integer.toString(go), addedOneUnificationXRef?2:1);
          addOntologyXRef(voc, xr, formattedName);
          addedOneUnificationXRef = true;
        }
      }
    }
    
    return voc;
  }

  /**
   * Get the RDF-ID (URI) that is used for a controlled vocabulary.
   * This is to date either a {@link SequenceModificationVocabulary} or
   * a {@link InteractionVocabulary}.
   * <p>
   * This method tries to denote the vocabulary with an identifiers.org URI.
   * This has been suggested by Igor R.
   * @param st the corresponding subtype
   * @param proteinModification {@code TRUE} ONLY for modification
   * vocabularies, such as {@link SequenceModificationVocabulary}.
   * @return an RDF-ID of the vocabulary element.
   */
  protected String getVocabularyID(SubType st, boolean proteinModification) {
    
    // Use UNIQUE database for distinction between interactions and modifications
    if (proteinModification) {
      ValuePair<String, Integer> MODterm = SBOMapping.getMODTerm(st.getName());
      if (MODterm!=null && MODterm.getB()!=null && MODterm.getB()>0) {
        return DatabaseIdentifiers.getMiriamURI(IdentifierDatabases.MOD, Integer.toString(MODterm.getB()));
      }
    } else {
      ValuePair<String, Integer> miTerm = SBOMapping.getMITerm(st.getName());
      if (miTerm!=null && miTerm.getB()!=null && miTerm.getB()>0) {
        return DatabaseIdentifiers.getMiriamURI(IdentifierDatabases.MI, Integer.toString(miTerm.getB()));
      }
    }
    
    String uri = null;
    int sbo = SBOMapping.getSBOTerm(st.getName());
    if (sbo>0) {
      uri = DatabaseIdentifiers.getMiriamURI(IdentifierDatabases.SBO, Integer.toString(sbo));
    }
    
    if (uri==null) {
      int go = SBOMapping.getGOTerm(st.getName());
      if (go>0) {
        uri = DatabaseIdentifiers.getMiriamURI(IdentifierDatabases.GeneOntology, Integer.toString(go));
      }
    }
    
    // Should actually never occur...
    if (uri==null) {
      String formattedName = st.getName().trim().replace(' ', '_').replace("/", "_or_");
      uri = "#voc_subtype_" + formattedName;
    }
    
    // We NEED a distinction between URIs for modifications (SequenceModificationVocabulary classes)
    // and interactions (InteractionVocabulary classes)
    if (proteinModification) {
      uri += "_mod";
    }
    
    return uri;
  }


  /**
   * @param xReferrableBPelement
   * @param xRef
   * @param formattedName
   */
  protected void addOntologyXRef(BioPAXElement xReferrableBPelement,
    BioPAXElement xRef, String formattedName) {
    if (xRef!=null) {
      if (level == BioPAXLevel.L3) {
        ((Xref) xRef).addComment(formattedName);
        ((org.biopax.paxtools.model.level3.XReferrable) xReferrableBPelement).addXref((Xref) xRef);
      } else if (level == BioPAXLevel.L2) {
        ((xref) xRef).addCOMMENT(formattedName);
        ((org.biopax.paxtools.model.level2.XReferrable) xReferrableBPelement).addXREF((xref) xRef);
      }
    }
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.KEGGtranslator#isGraphicalOutput()
   */
  public boolean isGraphicalOutput() {
    // Convert reaction-nodes to real reactions.
    return false;
  }
  
  /**
   * Please implement this method to add the given {@link Entry}
   * <code>e</code> as appropriate BioPAX entity to the {@link #model}.
   * @param e
   * @param p
   * @return created {@link BioPAXElement}
   */
  public abstract BioPAXElement addEntry(Entry e, Pathway p);
  
  /**
   * Please implement this method to add the given {@link Reaction}
   * <code>r</code> as appropriate BioPAX entity to the {@link #model}.
   * @param r
   * @param p
   * @return created {@link BioPAXElement}
   */
  public abstract BioPAXElement addKGMLReaction(Reaction r, Pathway p);
  
  /**
   * Please implement this method to add the given {@link Relation}
   * <code>r</code> as appropriate BioPAX entity to the {@link #model}.
   * @param r
   * @param p
   * @return created {@link BioPAXElement}
   */
  public abstract BioPAXElement addKGMLRelation(Relation r, Pathway p);
  
  /**
   * This method should be called whenever any pathway component is created.
   * @param element
   */
  protected void pathwayComponentCreated(BioPAXElement element) {
    if (element!=null) {
      if (element instanceof pathwayComponent) {
        ((org.biopax.paxtools.model.level2.pathway) pathway).addPATHWAY_COMPONENTS((pathwayComponent) element);
      } else if (element instanceof org.biopax.paxtools.model.level3.Process) {
        ((org.biopax.paxtools.model.level3.Pathway) pathway).addPathwayComponent((org.biopax.paxtools.model.level3.Process) element);
      }
    }
  }
  
  /**
   * Ensures a displayName that has at most 24 characters.
   * @param displayName any (potentially long) name
   * @return a string that is no longer than 24 characters.
   */
  public static String createDisplayName(String displayName) {
    if (displayName.length()>24) {
      // Try to cut the string at a good position
      int max = displayName.lastIndexOf(' ', 21);
      max = Math.max(max, displayName.lastIndexOf('\t', 21));
      max = Math.max(max, displayName.lastIndexOf('\n', 21));
      max = Math.max(max, displayName.lastIndexOf(',', 21));
      max = Math.max(max, displayName.lastIndexOf(';', 21));
      
      if (max>=10) {
        displayName = displayName.substring(0, max)+"...";
      } else {
        displayName = displayName.substring(0, 20)+"...";
      }
    }
    
    return displayName;
  }
  
  /**
   * Ensures the given {@code id} is unique in the current
   * {@link #model}.
   * <p><b>If possible, use {@link #NameToSId(String)} instead of this method!!!</b></p>
   * @param id
   * @return
   */
  protected String ensureUniqueRDFId(String id) {
    String originalID = id;
    if (model.containsID(id)) {
      int i=2;
      id = originalID.concat(Integer.toString(i));
      while (model.containsID(id)) {
        i++;
        id = originalID.concat(Integer.toString(i));
      }
    }
    return id;
  }
  
  /**
   * Only for Level3.
   * Gets the common reference to the given element.
   * Onls for {@link SimplePhysicalEntity}s.
   * <p>Does NOT create one if there is none (returns {@code null}).
   * @param element
   * @return 
   * @return
   */
  protected EntityReference getEntityReference(BioPAXElement element) {
    if (element instanceof SimplePhysicalEntity) {
      return ((SimplePhysicalEntity) element).getEntityReference();
    }
    return (EntityReference) model.getByID(element.getRDFId() + KEGG2BioPAX_level3.EntityReferenceSuffix);
  }
  
}
