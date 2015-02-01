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
 * Copyright (C) 2010-2015 by the University of Tuebingen, Germany.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.BiochemicalReaction;
import org.biopax.paxtools.model.level3.Catalysis;
import org.biopax.paxtools.model.level3.Complex;
import org.biopax.paxtools.model.level3.ComplexAssembly;
import org.biopax.paxtools.model.level3.Control;
import org.biopax.paxtools.model.level3.ControlType;
import org.biopax.paxtools.model.level3.Controller;
import org.biopax.paxtools.model.level3.Conversion;
import org.biopax.paxtools.model.level3.ConversionDirectionType;
import org.biopax.paxtools.model.level3.Dna;
import org.biopax.paxtools.model.level3.DnaReference;
import org.biopax.paxtools.model.level3.DnaRegion;
import org.biopax.paxtools.model.level3.DnaRegionReference;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.EntityFeature;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.Gene;
import org.biopax.paxtools.model.level3.Interaction;
import org.biopax.paxtools.model.level3.InteractionVocabulary;
import org.biopax.paxtools.model.level3.Level3Element;
import org.biopax.paxtools.model.level3.ModificationFeature;
import org.biopax.paxtools.model.level3.MolecularInteraction;
import org.biopax.paxtools.model.level3.Named;
import org.biopax.paxtools.model.level3.NucleicAcid;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.Provenance;
import org.biopax.paxtools.model.level3.Rna;
import org.biopax.paxtools.model.level3.RnaReference;
import org.biopax.paxtools.model.level3.RnaRegion;
import org.biopax.paxtools.model.level3.RnaRegionReference;
import org.biopax.paxtools.model.level3.SequenceEntityReference;
import org.biopax.paxtools.model.level3.SequenceModificationVocabulary;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;
import org.biopax.paxtools.model.level3.SmallMolecule;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;
import org.biopax.paxtools.model.level3.Stoichiometry;
import org.biopax.paxtools.model.level3.TemplateDirectionType;
import org.biopax.paxtools.model.level3.TemplateReaction;
import org.biopax.paxtools.model.level3.TemplateReactionRegulation;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.XReferrable;
import org.biopax.paxtools.model.level3.Xref;

import de.zbit.kegg.api.KeggInfos;
import de.zbit.kegg.api.cache.KeggInfoManagement;
import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.EntryType;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.kegg.parser.pathway.Reaction;
import de.zbit.kegg.parser.pathway.ReactionComponent;
import de.zbit.kegg.parser.pathway.ReactionType;
import de.zbit.kegg.parser.pathway.Relation;
import de.zbit.kegg.parser.pathway.RelationType;
import de.zbit.kegg.parser.pathway.SubType;
import de.zbit.kegg.parser.pathway.ext.EntryExtended;
import de.zbit.kegg.parser.pathway.ext.EntryTypeExtended;
import de.zbit.util.ArrayUtils;
import de.zbit.util.DatabaseIdentifiers;
import de.zbit.util.DatabaseIdentifiers.IdentifierDatabases;
import de.zbit.util.StringUtil;
import de.zbit.util.objectwrapper.ValuePair;

/**
 * KEGG2BioPAX level 3 converter (also called KGML2BioPAX).
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class KEGG2BioPAX_level3 extends KEGG2BioPAX {
  
  
  /**
   * The root {@link Pathway} of our BioPAX conversion.
   */
  private org.biopax.paxtools.model.level3.Pathway pathway;
  
  /**
   * {@link BioSource} for the organism
   */
  private BioSource organism = null;
  
  /**
   * A common suffix for all {@link EntityReference}s.
   */
  public final static String EntityReferenceSuffix = ".eref";
  
  /**
   * A common suffix for all entities that are being duplicated as
   * result of a modification (may end with "_mod", BUT ALSO "_mod2",...).
   */
  public final static String ENTITY_MODIFICATION_SUFFIX = "_mod";
  /**
   * Initialize a new {@link KEGG2BioPAX} object, using a new Cache and a new KeggAdaptor.
   */
  public KEGG2BioPAX_level3() {
    this(new KeggInfoManagement());
  }
  
  /**
   * @param manager
   */
  public KEGG2BioPAX_level3(KeggInfoManagement manager) {
    super(BioPAXLevel.L3, manager);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.KEGG2BioPAX#addEntry(de.zbit.kegg.parser.pathway.Entry, de.zbit.kegg.parser.pathway.Pathway)
   */
  @Override
  public BioPAXElement addEntry(Entry entry, Pathway p) {
    
    /*
     * Get the actial object to create
     */
    Class<? extends BioPAXElement> instantiate = PhysicalEntity.class;
    if (entry.isSetType()) {
      if (entry.getType() == EntryType.compound) {
        instantiate = SmallMolecule.class;
      } else if (entry.getType() == EntryType.enzyme) {
        instantiate = Protein.class;
      } else if (entry.getType() == EntryType.gene) {
        instantiate = Protein.class;
      } else if (entry.getType() == EntryType.genes) {
        instantiate = Complex.class;
      } else if (entry.getType() == EntryType.group) {
        instantiate = Complex.class;
      } else if (entry.getType() == EntryType.map) {
        instantiate = org.biopax.paxtools.model.level3.Pathway.class;
      } else if (entry.getType() == EntryType.ortholog) {
        // TODO: We are loosing information here because orthologs need to be split into several entities. At least we have to annotate the element!!
        instantiate = Protein.class;
      } else if (entry.getType() == EntryType.reaction) {
        //instantiate = Interaction.class;
        // Reaction-nodes usually also occur as real reactions.
        return null;
      }
    }
    // Extended object is source was a non-KGMl document
    if (entry instanceof EntryExtended) {
      if (((EntryExtended) entry).isSetGeneType()) {
        if (((EntryExtended) entry).getGeneType() == EntryTypeExtended.dna) {
          instantiate = Dna.class;
        } else if (((EntryExtended) entry).getGeneType() == EntryTypeExtended.dna_region) {
          instantiate = DnaRegion.class;
        } else if (((EntryExtended) entry).getGeneType() == EntryTypeExtended.gene) {
          instantiate = Gene.class;
        } else if (((EntryExtended) entry).getGeneType() == EntryTypeExtended.protein) {
          instantiate = Protein.class;
        } else if (((EntryExtended) entry).getGeneType() == EntryTypeExtended.rna) {
          instantiate = Rna.class;
        } else if (((EntryExtended) entry).getGeneType() == EntryTypeExtended.rna_region) {
          instantiate = RnaRegion.class;
        }
      }
    }
    
    // Pathway references are also stored separately.
    boolean isPathwayReference = false;
    String name = entry.getName().trim();
    if ((name != null) && (name.toLowerCase().startsWith("path:") || entry.getType().equals(EntryType.map))) {
      isPathwayReference = true;
      instantiate = org.biopax.paxtools.model.level3.Pathway.class;
    }
    // Eventually skip this node. It's just a label for the current pathway.
    if (isPathwayReference && (entry.hasGraphics() && entry.getGraphics().getName().toLowerCase().startsWith("title:"))) {
      return null;//Do not add a pathway for the current pathway!
    }
    
    // Create the actual element
    String eId = '#'+NameToSId(entry.getName().length()>45?entry.getName().substring(0, 45):entry.getName());
    BioPAXElement element = model.addNew(instantiate, eId);
    pathwayComponentCreated(element);
    
    // NOTE: we can cast to Entity, as all used classes are derived from Entity
    // Get a good name for the node
    String fullName = null;
    if (entry.hasGraphics() && entry.getGraphics().getName().length() > 0) {
      fullName = entry.getGraphics().getName(); // + " (" + name + ")"; // Append ko Id(s) possible!
      name = fullName;
    }
    // Set name to real and human-readable name (from Inet data - Kegg API).
    name = getNameForEntry(entry);
    if (fullName!=null) {
      ((Entity)element).setStandardName(fullName); // Graphics name
    }
    String displayName = createDisplayName(name);
    ((Entity)element).setDisplayName(displayName); // Intelligent name
    // ---
    addDataSources(element);
    
    
    // For complex:
    if (entry.hasComponents() && (element instanceof Complex)) {
      // TODO: Create complexAssembly, add it to pathway?!?!? AND add components to left and complex to right.
      for (int c:entry.getComponents()) {
        Entry ce = p.getEntryForId(c);
        if (ce!=null && ce!=entry) {
          // Get current component (or create if not yet there)
          BioPAXElement ceb = (BioPAXElement) ce.getCustom();
          if (ceb==null) {
            ceb = addEntry(ce, p);
            // TODO: post-process the entry if it is an ortholog!
          }
          if ((ceb == null) || !(ceb instanceof PhysicalEntity)) {
            continue;
          }
          
          ((Complex) element).addComponent((PhysicalEntity) ceb);
        }
      }
    }
    
    // TODO: CellularLocation from EntryExtended in L3
    
    // Add various annotations and xrefs
    addAnnotations(entry, element);
    
    
    // Even though it's just "recommended", the BioPAX validator gives an
    // error if no entityReferences are set.
    if ((element instanceof SimplePhysicalEntity) &&
        !(element instanceof Complex)) {
      setupEntityReference(element); // TODO this is probably the root of all evil...
      // TODO: If the element has multiple Uniprot ids in its xref map then it is most likely a generic and needs to be split into individual entityReference objects!!!!
      // This happens when the entry is an ortholog.
    }
    
    
    entry.setCustom(element);
    return element;
  }
  
  /**
   * @param element
   */
  private void setupEntityReference(BioPAXElement element) {
    EntityReference er = getEntityReference(element);
    if (er==null) {
      er = createEntityReference(element);
    }
    
    if (er!=null) {
      ((SimplePhysicalEntity) element).setEntityReference(er);
      
      // Actually we could also MOVE all XRefs to the reference!
      if (((XReferrable)element).getXref()!=null) {
        List<Xref> unifications = new LinkedList<Xref>();
        for (Xref xr : ((XReferrable)element).getXref()) {
          er.addXref(xr);
          if (xr.getModelInterface().equals(UnificationXref.class)) {
            unifications.add(xr);
          }
        }
        // Unifications should relly only be used once (and this should be on the reference)
        for (Xref xr : unifications) {
          ((XReferrable)element).removeXref(xr);
        }
      }
      //---
      
      // Further set Organism and names (do not set organism on h2o and similar).
      if (er instanceof SequenceEntityReference &&
          !er.getModelInterface().equals(SmallMoleculeReference.class)) {
        ((SequenceEntityReference) er).setOrganism(organism);
      }
      if (er instanceof Named && element instanceof Named) {
        ((Named) er).setStandardName(((Named) element).getStandardName());
        ((Named) er).setDisplayName(((Named) element).getDisplayName());
        ((Named) er).setName(((Named) element).getName());
      }
      //---
    }
  }
  
  /**
   * Create an {@link EntityReference} for any {@link BioPAXElement}.
   * 
   * <p>Please setup organism, names and XRefs on this element.
   * 
   * @param element
   * @return corresponding {@link EntityReference} or {@code null}.
   */
  private EntityReference createEntityReference(BioPAXElement element) {
    String id = element.getRDFId() + EntityReferenceSuffix;
    id = ensureUniqueRDFId(id); // we cannot use nameToSID because it would remove, e.g., the starting dash #.
    EntityReference bpEr = null;
    
    if (element instanceof SmallMolecule){
      bpEr = model.addNew(SmallMoleculeReference.class, id);
      // must set to unknown, default is 0.0 which makes no sense...
      ((SmallMoleculeReference)bpEr).setMolecularWeight(BioPAXElement.UNKNOWN_FLOAT);
      
    } else if (element instanceof Protein) {
      bpEr = model.addNew(ProteinReference.class, id);
      
    } else if (element instanceof Rna) {
      bpEr = model.addNew(RnaReference.class, id);
      
    } else if (element instanceof Dna) {
      bpEr = model.addNew(DnaReference.class, id);
      
    } else if (element instanceof RnaRegion) {
      bpEr = model.addNew(RnaRegionReference.class, id);
      
    } else if (element instanceof DnaRegion) {
      bpEr = model.addNew(DnaRegionReference.class, id);
      
    } else {
      // We can't create entity references for complexes
      // or unknown or unspecified elements
    }
    
    
    // Adjust organism, names and xrefs (ATP has no organism...)
    if (bpEr instanceof SequenceEntityReference &&
        !bpEr.getModelInterface().equals(SmallMoleculeReference.class)) {
      ((SequenceEntityReference)bpEr).setOrganism(organism);
    }
    
    
    if (bpEr!=null) {
      pathwayComponentCreated(bpEr);
    }
    
    return bpEr;
  }
  
  /**
   * @param element
   */
  private void addDataSources(BioPAXElement element) {
    if (element instanceof Entity) {
      for (Provenance ds : pathway.getDataSource()) {
        ((Entity)element).addDataSource(ds);
      }
    }
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.KEGG2BioPAX#createPathwayInstance(de.zbit.kegg.parser.pathway.Pathway)
   */
  @Override
  protected BioPAXElement createPathwayInstance(Pathway p) {
    pathway = model.addNew(org.biopax.paxtools.model.level3.Pathway.class, p.getName());
    pathway.addAvailability(String.format("This file has been generated by %s version %s", System.getProperty("app.name"), System.getProperty("app.version")));
    String htmlName = (p.getTitle()); // Escaping is done automatically in Paxtools!
    pathway.addName(htmlName);
    String displayName = createDisplayName(htmlName);
    pathway.setDisplayName(displayName);
    pathway.setStandardName(htmlName);
    
    // Parse Kegg Pathway information
    boolean isKEGGPathway = DatabaseIdentifiers.checkID(DatabaseIdentifiers.IdentifierDatabases.KEGG_Pathway, p.getNameForMIRIAM());
    if (isKEGGPathway) {
      Xref xr = (Xref)createXRef(IdentifierDatabases.KEGG_Pathway, p.getNameForMIRIAM(), 1);
      if (xr!=null) {
        pathway.addXref(xr);
      }
    }
    
    // Retrieve further information via Kegg Adaptor
    organism = (BioSource) createBioSource(p);
    pathway.setOrganism(organism);
    
    // Get PW infos from KEGG Api for Description and GO ids.
    KeggInfos pwInfos = KeggInfos.get(p.getName(), manager); // NAME, DESCRIPTION, DBLINKS verwertbar
    if (pwInfos.queryWasSuccessfull()) {
      pathway.addComment((pwInfos.getDescription()));
      
      // GO IDs
      if (pwInfos.getGo_id() != null) {
        for (String goID : pwInfos.getGo_id().split("\\s")) {
          Xref xr = (Xref)createXRef(IdentifierDatabases.GeneOntology, goID, 2);
          if (xr!=null) {
            pathway.addXref(xr);
          }
        }
      }
    }
    
    // Add data sources
    Collection<BioPAXElement> sources = createDataSources(p);
    for (BioPAXElement source: sources) {
      pathway.addDataSource((Provenance) source);
    }
    
    return pathway;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.KEGG2BioPAX#addKGMLReaction(de.zbit.kegg.parser.pathway.Reaction, de.zbit.kegg.parser.pathway.Pathway)
   */
  @Override
  public BioPAXElement addKGMLReaction(Reaction r, Pathway p) {
    
    // Check if we have a reaction, that is catalyzed by enzymes
    Collection<Entry> enzymes = p.getReactionModifiers(r.getName());
    boolean hasEnzymes = enzymes!=null&&enzymes.size()>0;
    
    Class<? extends BioPAXElement> instantiate = BiochemicalReaction.class;
    if (hasEnzymes) {
      // TODO: Here the assumption seems to be an AND logic, but meant is an OR logic! We need to create a separate catalysis for each controller!
      instantiate = Catalysis.class;
    }
    
    // Create the actual reaction or Catalysis
    BioPAXElement element = model.addNew(instantiate, '#'+NameToSId(r.getName()));
    pathwayComponentCreated(element);
    BiochemicalReaction reaction;
    if ((element instanceof Catalysis)) {
      // setup enzymes
      if (hasEnzymes) {
        Set<BioPAXElement> addedEnzymes = new HashSet<BioPAXElement>();
        for (Entry ce:enzymes) {
          if (ce!=null) {
            // Get current component
            BioPAXElement ceb = (BioPAXElement) ce.getCustom();
            if ((ceb == null) || !(ceb instanceof Controller) || !addedEnzymes.add(ceb)) {
              continue;
            }
            
            ((Catalysis) element).addController((Controller) ceb);
          }
        }
      }
      
      addDataSources(element);
      ((Catalysis) element).addName(r.getName()+"_Catalysis");
      
      // Create actual reaction
      reaction = model.addNew(BiochemicalReaction.class, '#'+NameToSId(r.getName()));
      pathwayComponentCreated(reaction);
      ((Catalysis) element).addControlled(reaction);
    } else {
      reaction = (BiochemicalReaction) element;
    }
    
    // reversible/irreversible
    if (r.isSetType()) {
      reaction.setConversionDirection(r.getType()==ReactionType.reversible?
          ConversionDirectionType.REVERSIBLE:ConversionDirectionType.LEFT_TO_RIGHT);
    }
    
    
    reaction.addName(r.getName());
    String displayName = createDisplayName(r.getName());
    reaction.setDisplayName(displayName);
    addDataSources(reaction);
    
    // Add all reaction components
    for (ReactionComponent rc : r.getSubstrates()) {
      configureReactionComponent(p, reaction, rc, true);
    }
    for (ReactionComponent rc : r.getProducts()) {
      configureReactionComponent(p, reaction, rc, false);
    }
    
    // Add various annotations
    addAnnotations(r, reaction);
    
    return reaction;
  }
  
  
  /**
   * Retrieves the {@link PhysicalEntity} for a {@link ReactionComponent}.
   * @param p
   * @param reaction
   * @param rc
   * @return {@code true} if the component has been added successfully.
   */
  private boolean configureReactionComponent(Pathway p, BiochemicalReaction reaction, ReactionComponent rc, boolean substrate) {
    if (!rc.isSetID() && !rc.isSetName()) {
      rc = rc.getAlt();
      if (rc==null || ((!rc.isSetID() && !rc.isSetName()))) {
        return false;
      }
    }
    
    // Get BioPAX element for component
    Entry ce = p.getEntryForReactionComponent(rc);
    if (ce==null || ce.getCustom()==null) {
      return false;
    }
    BioPAXElement ceb = (BioPAXElement) ce.getCustom();
    if (ceb==null || !(ceb instanceof PhysicalEntity)) {
      return false;
    }
    
    // Set the stoichiometry
    Integer stoich = rc.getStoichiometry();
    Stoichiometry s = model.addNew(Stoichiometry.class, '#'+NameToSId(ce.getName()+"_"+getNameForElement(reaction)+"_stoich"));
    pathwayComponentCreated(s);
    s.setPhysicalEntity((PhysicalEntity) ceb);
    s.setStoichiometricCoefficient(stoich==null?1f:(float)stoich);
    
    // Add all elements to the reaction
    if (substrate) {
      reaction.addLeft((PhysicalEntity) ceb);
    } else {
      reaction.addRight((PhysicalEntity) ceb);
    }
    reaction.addParticipantStoichiometry(s);
    
    return true;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.KEGG2BioPAX#addKGMLRelation(de.zbit.kegg.parser.pathway.Relation, de.zbit.kegg.parser.pathway.Pathway)
   */
  @Override
  public BioPAXElement addKGMLRelation(Relation r, Pathway p) {
    /*
     * Relations:
     * - Conversion is generic with left and right,
     * - PhysicalInteraction is generic with just a pool of entities.
     */
    Collection<String> subtype = r.getSubtypesNames();
    
    // Get Participants
    Entry eOne = p.getEntryForId(r.getEntry1());
    Entry eTwo = p.getEntryForId(r.getEntry2());
    BioPAXElement qOne = eOne==null?null:(BioPAXElement) eOne.getCustom();
    BioPAXElement qTwo = eTwo==null?null:(BioPAXElement) eTwo.getCustom();
    if (qOne==null || qTwo==null) {
      // Happens, e.g. when remove_pw_references is true and there is a
      // relation to this (now removed) node.
      log.finer("Relation with unknown or removed entry: " + r);
      return null;
    }
    
    
    // Most relations have a left and right side => conversion as default
    Class<? extends BioPAXElement> instantiate = Conversion.class;
    boolean createConversionAndControl = false;
    
    
    // Compound (only PPREL) to Conversion, SKIP ALL OTHERS [IF CONSIDERREACTIONS()]
    if (considerReactions()) {
      if (subtype.contains(SubType.COMPOUND) || subtype.contains(SubType.HIDDEN_COMPOUND)) {
        if (r.isSetType() && (r.getType()==RelationType.PPrel)) {
          instantiate = Conversion.class;
        } else {
          // Other compound relations are copies of reactions, so no need to translate them.
          // KGML spec says:  "shared with two successive reactions"
          return null;
        }
      }
    }
    
    // Simple A -> B
    if (subtype.contains(SubType.STATE_CHANGE) || subtype.contains(SubType.INDIRECT_EFFECT)) {
      createConversionAndControl = false;
      instantiate = Conversion.class;
    }
    
    // Create a controlled "B -> B' (activated)" conversion
    if (subtype.contains(SubType.ACTIVATION) || subtype.contains(SubType.INHIBITION) ||
        subtype.contains(SubType.EXPRESSION) || subtype.contains(SubType.REPRESSION)) {
      createConversionAndControl = true;
      instantiate = Conversion.class;
      if (subtype.contains(SubType.EXPRESSION) || subtype.contains(SubType.REPRESSION)) {
        if (qTwo instanceof PhysicalEntity) {
          instantiate = TemplateReaction.class; // Create a Regulated template reaction
        }
      }
    }
    
    // "binding/assoc.", "dissociation", "missing interaction" and in doubt to PhysicalInteraction
    if ((subtype.contains(SubType.ASSOCIATION) || subtype.contains(SubType.BINDING) || subtype.contains(SubType.BINDING_ASSOCIATION)) ||
        (subtype.contains(SubType.DISSOCIATION)) || subtype.contains(SubType.MISSING_INTERACTION) || subtype.size()<1) {
      // This property may get overwritten later on!
      instantiate = MolecularInteraction.class; // Interaction is same as physicalInteraction.class in L2
    }
    // Check if "binding/assoc." describes the formation of a complex.
    if ((eTwo.getType().equals(EntryType.group) || eTwo.getType().equals(EntryType.genes)) &&
        (subtype.contains(SubType.ASSOCIATION) || subtype.contains(SubType.BINDING) || subtype.contains(SubType.BINDING_ASSOCIATION))) {
      instantiate = ComplexAssembly.class;
    }
    // Check if "DISSOCIATION" describes the DISASSEMBLY of a complex.
    if ((eOne.getType().equals(EntryType.group) || eOne.getType().equals(EntryType.genes)) &&
        (subtype.contains(SubType.DISSOCIATION))) {
      instantiate = ComplexAssembly.class; // this is also used for DISASSEMBLY.
    }
    
    // These types are controlleds relations in which A Phosphorylates B.
    if (subtype.contains(SubType.PHOSPHORYLATION) || subtype.contains(SubType.DEPHOSPHORYLATION) ||
        subtype.contains(SubType.GLYCOSYLATION) || subtype.contains(SubType.UBIQUITINATION) ||
        subtype.contains(SubType.METHYLATION)) {
      createConversionAndControl = true;
      instantiate = BiochemicalReaction.class;
    }
    
    // Make a final check, if we are able to create a the desired class (e.g., a conversion)
    if ((!createConversionAndControl && !(qOne instanceof PhysicalEntity)) || !(qTwo instanceof PhysicalEntity)) {
      // Explanation: if createConversionAndControl then qOne is the controller and not involved in the conversion.
      // else, it is translated to qOne->qTwo and it is involved in the conversion.
      if (Conversion.class.isAssignableFrom(instantiate)) {
        log.fine("Changing from Conversion to MolecularInteraction, because Conversion requires physical entities as participants " + r);
        instantiate = MolecularInteraction.class;
      }
      
      if ((MolecularInteraction.class.isAssignableFrom(instantiate)) &&
          ((!createConversionAndControl && !(qOne instanceof PhysicalEntity)) && !(qTwo instanceof PhysicalEntity))) {
        // MolecularInteraction requires at least one PhysicalEntity (only by definition).
        log.fine("Changing from MolecularInteraction to Interaction, because MolecularInteraction requires at least one physical entity as participant " + r);
        instantiate = Interaction.class;
      }
    }
    
    // If we do NOT create a controller/Control thing and just a simple A -> B
    // then try to "keep reaction chains", e.g., "A -> A' -> B".
    // Thus, look for a modified qOne (=A) here.
    if (!createConversionAndControl) {
      BioPAXElement qOneMod = getModifiedEntity(qOne, null);
      if (qOneMod!=null) {
        qOne = qOneMod;
      }
    }
    
    // Create the relation
    Interaction bpe = (Interaction) model.addNew(instantiate, '#'+NameToSId("KEGGrelation"));
    pathwayComponentCreated(bpe);
    bpe.setDisplayName(createDisplayName(ArrayUtils.implode(subtype, ", ") + " of " + getNameForElement(qTwo)));
    
    // Add Annotations
    addDataSources(bpe);
    if (subtype.size()>0) {
      if (!subtype.contains(SubType.COMPOUND)) {
        bpe.addComment("LINE-TYPE: " + r.getSubtypes().iterator().next().getValue());
      }
      bpe.addName(ArrayUtils.implode(subtype, ", "));
      
      for (SubType st: r.getSubtypes()) {
        bpe.addInteractionType((InteractionVocabulary) getInteractionVocuabulary(st));
      }
    }
    
    // Add participants
    if (bpe instanceof Conversion) {
      // if qTwo is no SimplePhysicalEntity, we cannot add any mofification feature. Hence,
      // it does not make sense to crate a controller/controlled thing.
      if (createConversionAndControl && (qTwo instanceof SimplePhysicalEntity)) {
        setupControllerControlled(r, bpe, qOne, qTwo);
        
      } else {
        
        // A "default arrow" from ony -> two.
        ((Conversion) bpe).addLeft((PhysicalEntity) qOne);
        ((Conversion) bpe).addRight((PhysicalEntity) qTwo);
      }
      
    } else {
      bpe.addParticipant((Entity) qOne);
      bpe.addParticipant((Entity) qTwo);
    }
    
    return bpe;
  }
  
  /**
   * Get the best possible name for a {@link BioPAXElement}.
   * @param qTwo
   * @return
   */
  private String getNameForElement(BioPAXElement qTwo) {
    String name = null;
    if (qTwo instanceof Named) {
      name = ((Named) qTwo).getDisplayName();
      if (name==null || name.length()<1) {
        name = ((Named) qTwo).getStandardName();
      }
      if ((name==null || name.length()<1) && ((Named) qTwo).getName()!=null) {
        name = ArrayUtils.implode(((Named) qTwo).getName(), ", ");
      }
    }
    
    if ((name == null) || (name.length() < 1)) {
      name = qTwo.getRDFId();
    }
    
    return name;
  }
  
  /**
   * @param r
   * @param bpe
   * @param qOne
   * @param qTwo
   */
  private void setupControllerControlled(Relation r, Interaction bpe, BioPAXElement qOne, BioPAXElement qTwo) {
    Collection<String> subtype = r.getSubtypesNames();
    
    // Determine the type of controller that should be created
    Class<? extends Control> instantiate = Control.class;
    
    if (bpe instanceof TemplateReaction) {
      instantiate = TemplateReactionRegulation.class;
      //    } else if (bpe instanceof BiochemicalReaction) {
      //      instantiate = Catalysis.class;
    }
    
    // Create the controller
    Control controller = model.addNew(instantiate, '#'+NameToSId("KEGGrelationController"));
    pathwayComponentCreated(controller);
    addDataSources(controller);
    String name = getNameForElement(qOne);
    name = ArrayUtils.implode(subtype, ", ") + " by " + name;
    controller.addName(name);
    controller.setDisplayName(createDisplayName(name));
    controller.addControlled(bpe);
    try {
      controller.addController((Controller) qOne);
      controller.addParticipant((Entity) qOne);
    } catch (Exception e) {
      //should actually never happen
      log.log(Level.WARNING, "Catched an unexpected exception.", e);
    }
    for (SubType st: r.getSubtypes()) {
      // Same InteractionTypes as bpe has.
      controller.addInteractionType((InteractionVocabulary) getInteractionVocuabulary(st));
    }
    
    // Setup the controlType
    if (subtype.contains(SubType.ACTIVATION) || subtype.contains(SubType.EXPRESSION)) {
      controller.setControlType(ControlType.ACTIVATION);
    } else if (subtype.contains(SubType.INHIBITION) || subtype.contains(SubType.REPRESSION)) {
      controller.setControlType(ControlType.INHIBITION);
    }
    
    // Maybe we need to setup a reverse reaction (B' -> B) instead of normally B -> B' (B' is e.g. a phosphorylated entitity).
    boolean modelReversely = (subtype.contains(SubType.DEPHOSPHORYLATION));
    
    
    // Get or create the modified qTwo protein
    BioPAXElement qThree = null;
    if (qTwo instanceof SimplePhysicalEntity && modelReversely) {
      // If a dephosphorylation occurs, we maybe already have a phosphorylation feature!
      // Search for an already phosphorylated entity
      BioPAXElement phosphoQTwo = getModifiedEntity(qTwo, SubType.PHOSPHORYLATION);
      if (phosphoQTwo!=null) {
        // Use the phosphorylated thing as source for the dephosphorylation.
        qThree = qTwo;
        qTwo = phosphoQTwo;
      } else {
        modelReversely = false;
      }
    } else {
      modelReversely = false;
    }
    
    // Create a third protein
    if (qThree==null) {
      if (!(bpe instanceof TemplateReaction) || !(qTwo instanceof PhysicalEntity)) {
        // the normal case
        qThree = createCopy(qTwo);
      } else {
        // we need to create some nucleicAcid
        BioPAXElement nAcid = createCopy(qTwo, NucleicAcid.class);
        qThree = qTwo;
        qTwo = nAcid;
      }
    }
    
    // Setup the Features
    for (SubType st: r.getSubtypes()) {
      boolean isDePhospho = (st.getName().equals(SubType.DEPHOSPHORYLATION));
      
      String modifiedName = st.getName();
      if (isDePhospho && modelReversely) {
        st = new SubType(SubType.PHOSPHORYLATION);
      }
      if (modifiedName.endsWith("ion")) {
        modifiedName = modifiedName.substring(0, modifiedName.length()-3)+"ed";
      }
      
      // Modification types are UNIQUE for a certain [combination of] subtypes.
      String modID = '#'+modifiedName.trim().replace(' ', '_').replace("/", "_or_");
      ModificationFeature mod = (ModificationFeature) model.getByID(modID);
      boolean modificationDidAlreadyExist = mod!=null;
      if (mod==null) {
        mod = model.addNew(ModificationFeature.class, modID);
        pathwayComponentCreated(mod);
        addDataSources(mod);
      }
      
      
      // Add the modification to both proteins and the reference
      if (qThree instanceof PhysicalEntity) {
        if (modelReversely && isDePhospho) {
          // This is modeled reversely by +p -> -p
          ((PhysicalEntity) qThree).addNotFeature(mod);
          ((PhysicalEntity) qTwo).addFeature(mod);
        } else {
          ((PhysicalEntity) qThree).addFeature(mod);
          ((PhysicalEntity) qTwo).addNotFeature(mod);
        }
        if (qTwo instanceof SimplePhysicalEntity) {
          EntityReference eRef = ((SimplePhysicalEntity) qTwo).getEntityReference();
          if (eRef!=null) {
            eRef.addEntityFeature(mod);
          }
        }
        removeContradictingFeatures((PhysicalEntity) qTwo);
        removeContradictingFeatures((PhysicalEntity) qThree);
      }
      
      // Annotate the kind of modification
      SequenceModificationVocabulary mVoc;
      boolean addCommentToSubstrate = false;
      mVoc = getSequenceModificationVocabulary(st);
      if (isDePhospho) {
        addCommentToSubstrate = true; // must not be equal to isReversePhospho !
        controller.addComment("Dephosphorylation");
      } else {
        String comment = ArrayUtils.implode(mVoc.getComment(), ", ").replace("ed_", "ion_");
        if (comment.endsWith("ed")) {
          comment = comment.substring(0, comment.length()-2)+"ion";
        }
        controller.addComment(comment); // E.g. "methylation_at_unknown_residue"
      }
      if (!modificationDidAlreadyExist) {
        mod.setModificationType(mVoc);
      }
      
      // FACT: if (isReversePhospho) then qThree gets NOT feature.
      // FACT: if (isReversePhospho && DEPHOSPHORYLATION) than type is now PHOSPHORYLATION.
      //String comment = (isReversePhospho?"NOT [":"")+ArrayUtils.implode(mVoc.getComment(), ", ")+(isReversePhospho?"]":""); // E.g. "methylated_at_unknown_residue"
      
      if (addCommentToSubstrate) {
        ((Level3Element) qTwo).addComment(ArrayUtils.implode(mVoc.getComment(), ", ")); // E.g. "methylated_at_unknown_residue"
      } else { // usual case, except for dephosphorylation, what is changed to phosphorylation of the substrate.
        ((Level3Element) qThree).addComment(ArrayUtils.implode(mVoc.getComment(), ", ")); // E.g. "methylated_at_unknown_residue"
      }
    }
    
    // Avoid duplicate entries, search if exactly this one has already been creted once
    List<BioPAXElement> objects = new ArrayList<BioPAXElement>(model.getObjects());
    boolean checkTwo = true, checkThree = true;
    for (BioPAXElement e : objects) {
      if (e == qTwo || e == qThree) {
        // The same pointer, not only equal!
        continue;
      } else if (checkTwo && e.isEquivalent(qTwo)) {
        model.remove(qTwo);
        qTwo = e;
        checkTwo = false;
      } else if (checkThree && e.isEquivalent(qThree)) {
        model.remove(qThree);
        qThree = e;
        checkThree = false;
      }
      if (!checkTwo && !checkThree) {
        break;
      }
    }
    
    
    // Configure the actual conversion
    ((Conversion) bpe).addLeft((PhysicalEntity) qTwo);
    ((Conversion) bpe).addRight((PhysicalEntity) qThree);
    controller.addParticipant((Entity) qTwo);
    controller.addParticipant((Entity) qThree);
    ((Conversion) bpe).setConversionDirection(ConversionDirectionType.LEFT_TO_RIGHT);
    
    if (bpe instanceof TemplateReaction && qTwo instanceof NucleicAcid) {
      ((TemplateReaction) bpe).setTemplate((NucleicAcid) qTwo);
      ((TemplateReaction) bpe).addProduct((PhysicalEntity) qThree);
      ((TemplateReaction) bpe).setTemplateDirection(TemplateDirectionType.FORWARD);
    }
  }
  
  /**
   * Removes features that occur as notFeatures and features.
   * @param qTwo
   */
  private void removeContradictingFeatures(PhysicalEntity qTwo) {
    List<EntityFeature> features = new ArrayList<EntityFeature>(qTwo.getFeature());
    features.retainAll(qTwo.getNotFeature());
    for (EntityFeature ft : features) {
      qTwo.removeFeature(ft);
      qTwo.removeNotFeature(ft);
    }
  }
  
  /**
   * Search an instance of {@code entity} that has a feature that has been
   * created, based on a modification from a {@code subtype}.
   * @param entity the BASIC, unmodified entity (e.g., does NOT end with {@link #ENTITY_MODIFICATION_SUFFIX}).
   * @param subtype (name of modification process). If {@code null}, any modified {@code entity} will be returned.
   * @return the already existing {@link BioPAXElement} which corresponds to {@code entity} with the given modification {@code subtype}.
   * Or {@code null} if such an element is not yet available.
   */
  private BioPAXElement getModifiedEntity(BioPAXElement entity, String subtype) {
    if (subtype!=null) {
      subtype = subtype.trim().replace(' ', '_').replace("/", "_or_");
    }
    // They end with "_mod", "_mod2",... look if they share the same
    // ent.Reference and maybe contain a phosphorylation feature.
    
    if (!(entity instanceof SimplePhysicalEntity)) {
      return null;
    }
    
    EntityReference eRef = ((SimplePhysicalEntity) entity).getEntityReference();
    BioPAXElement modEntity = model.getByID(entity.getRDFId() + ENTITY_MODIFICATION_SUFFIX);
    int i = 1;
    while (modEntity!=null) {
      // Are both derived from the same thing?
      if (modEntity instanceof SimplePhysicalEntity &&
          ((SimplePhysicalEntity)modEntity).getEntityReference().equals(eRef)) {
        // Does it contain the specified subtype?
        Set<EntityFeature> features = ((PhysicalEntity) modEntity).getFeature();
        if (features!=null) {
          if (subtype==null) {
            return modEntity;
          }
          for (EntityFeature f : features) {
            if (StringUtil.containsWord(f.getRDFId(), subtype)) {
              return modEntity;
            }
          }
        }
        
      }
      i++;
      modEntity = model.getByID(entity.getRDFId() + ENTITY_MODIFICATION_SUFFIX + i);
    }
    
    return null;
  }
  
  
  /**
   * <b>ONLY FOR LEVEL 3</b><br/>
   * Gets or creates a {@link SequenceModificationVocabulary} corresponding to the given {@link SubType}.
   * @return  {@link SequenceModificationVocabulary} for level 3.
   */
  protected SequenceModificationVocabulary getSequenceModificationVocabulary(SubType st) {
    String formattedName = st.getName().trim().replace(' ', '_').replace("/", "_or_");
    //String rfid = "#modification_type_" + formattedName;
    String rfid = getVocabularyID(st, true);
    SequenceModificationVocabulary voc = (SequenceModificationVocabulary) model.getByID(rfid);
    
    // Term is not yet available => create it.
    if (voc==null) {
      // Create the object
      voc = model.addNew(SequenceModificationVocabulary.class, rfid);
      pathwayComponentCreated(voc);
      
      // For methylation, phosphorylation, etc. we have MOD terms
      ValuePair<String, Integer> MODterm = SBOMapping.getMODTerm(st.getName());
      
      String termName;
      if (MODterm!=null && MODterm.getA()!=null && MODterm.getA().length()>0) {
        termName = MODterm.getA();
        
        // The term MUST be a string from MOD-ontology! Else, it is a BioPAX ERROR!
        voc.addTerm(termName);
      } else {
        termName = formattedName;
        if (termName.endsWith("ion")) {
          termName = termName.replace("ion", "ed"); // methylation -> methylated
        }
      }
      
      voc.addComment(termName); // + "_at_unknown_residue"
      
      
      boolean addedAUnification = false;
      // Add additional XRefs to MI, SBO and GO
      if (MODterm!=null && MODterm.getB()>0) {
        // It MUST BE any children of MOD:01157 or MOD:01156.
        BioPAXElement xr = createXRef(IdentifierDatabases.MOD, Integer.toString(MODterm.getB()), 1);
        addOntologyXRef(voc, xr, MODterm.getA());
        addedAUnification = true;
      }
      
      /*
       * It would be nice to include SBO and GO here with RELATIONSHIP xrefs (type=2).
       * However, BioPAX only allows unification xrefs, what is critically here, because
       * a modification is no interaction...
       * Therefore, I changed it now to create unifications (type=1) and exactly one xref!
       */
      
      // I know, SBO and GO are actually for interactions and not for states. But there is no other possibility to non-textually encode
      // e.g., "protein that is methylated at any residue".
      if (!addedAUnification) {
        int sbo = SBOMapping.getSBOTerm(st.getName());
        if (sbo>0) {
          BioPAXElement xr = createXRef(IdentifierDatabases.SBO, Integer.toString(sbo), 1);
          addOntologyXRef(voc, xr, formattedName);
          addedAUnification = true;
        }
      }
      
      if (!addedAUnification) {
        int go = SBOMapping.getGOTerm(st.getName());
        if (go>0) {
          BioPAXElement xr = createXRef(IdentifierDatabases.GeneOntology, Integer.toString(go), 1);
          addOntologyXRef(voc, xr, formattedName);
          addedAUnification = true;
        }
      }
    }
    
    return voc;
  }
  
  /**
   * Creates a copy of an {@link BioPAXElement} that has been created with
   * {@link #addEntry(Entry, Pathway)}. This is very useful, e.g., to
   * create a second instance of the same protein with a different features
   * (e.g., a phosphorylation).
   * 
   * @param element
   * @return copy of {@code element} with the same properties and only different RFId.
   */
  public BioPAXElement createCopy(BioPAXElement element) {
    return createCopy(element, element.getModelInterface());
  }
  public BioPAXElement createCopy(BioPAXElement element, Class<? extends BioPAXElement> typeOfCopy) {
    String eId = ensureUniqueRDFId(element.getRDFId() + ENTITY_MODIFICATION_SUFFIX); // Make unique
    BioPAXElement newElement = model.addNew(typeOfCopy, eId);
    pathwayComponentCreated(newElement);
    
    // Names
    if (element instanceof Named && newElement instanceof Named ) {
      ((Named) newElement).setStandardName(((Named) element).getStandardName());
      ((Named) newElement).setDisplayName(((Named) element).getDisplayName());
      ((Named) newElement).setName(((Named) element).getName());
    }
    // ---
    addDataSources(newElement);
    
    // Complex components
    if (element instanceof Complex && newElement instanceof Complex) {
      for (PhysicalEntity pe : ((Complex) newElement).getComponent()) {
        ((Complex) newElement).addComponent(pe);
      }
    }
    
    
    // Add all potential annotations that come from addAnnotations();
    if (element instanceof XReferrable && newElement instanceof XReferrable) {
      for (Xref xr : ((XReferrable) element).getXref()) {
        ((XReferrable) newElement).addXref(xr);
      }
    }
    if ((element instanceof BiochemicalReaction) && (newElement instanceof BiochemicalReaction)) {
      for (String ec : ((BiochemicalReaction) element).getECNumber()) {
        ((BiochemicalReaction) newElement).addECNumber(ec);
      }
    }
    if (element instanceof Level3Element && newElement instanceof Level3Element) {
      for (String c : ((Level3Element) element).getComment()) {
        ((Level3Element) newElement).addComment(c);
      }
    }
    
    // TODO: So far not copied (but not important in KEGGtranslator): CellularLocations, participantOf, Features, NotFeatures
    
    
    // Now comes the important part, we need to set a reference to the same entity as
    // the oritinal biopax element
    if (element instanceof SimplePhysicalEntity && newElement instanceof SimplePhysicalEntity) {
      ((SimplePhysicalEntity) newElement).setEntityReference(((SimplePhysicalEntity) element).getEntityReference());
    }
    
    return newElement;
  }
  
}
