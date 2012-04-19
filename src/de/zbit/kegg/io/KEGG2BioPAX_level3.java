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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.BiochemicalReaction;
import org.biopax.paxtools.model.level3.Catalysis;
import org.biopax.paxtools.model.level3.Complex;
import org.biopax.paxtools.model.level3.Controller;
import org.biopax.paxtools.model.level3.Conversion;
import org.biopax.paxtools.model.level3.ConversionDirectionType;
import org.biopax.paxtools.model.level3.Dna;
import org.biopax.paxtools.model.level3.DnaRegion;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.Gene;
import org.biopax.paxtools.model.level3.Interaction;
import org.biopax.paxtools.model.level3.InteractionVocabulary;
import org.biopax.paxtools.model.level3.MolecularInteraction;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.model.level3.Provenance;
import org.biopax.paxtools.model.level3.Rna;
import org.biopax.paxtools.model.level3.RnaRegion;
import org.biopax.paxtools.model.level3.SmallMolecule;
import org.biopax.paxtools.model.level3.Stoichiometry;
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
    BioPAXElement element = model.addNew(instantiate, '#'+NameToSId(entry.getName()));
    
    // NOTE: we can cast to Entity, as all used classes are derived from Entity
    // Get a good name for the node
    String fullName = null;
    if (entry.hasGraphics() && entry.getGraphics().getName().length() > 0) {
      fullName = entry.getGraphics().getName(); // + " (" + name + ")"; // Append ko Id(s) possible!
      name = fullName;
    }
    // Set name to real and human-readable name (from Inet data - Kegg API).
    name = getNameForEntry(entry);
    ((Entity)element).addName(fullName!=null?fullName:name); // Graphics name (OR (if null) same as below)
    ((Entity)element).setDisplayName(name); // Intenligent name
    ((Entity)element).setStandardName(name);
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
          }
          if (ceb==null || !(ceb instanceof PhysicalEntity)) continue;
                  
          ((Complex)element).addComponent((PhysicalEntity) ceb);
        }
      }
    }
    
    // XXX: Possible to set ORGANISM on COMPLEX & sequenceEntity (& Gene in L3)
    // TODO: CellularLocation from EntryExtended in L3
    
    // Add various annotations and xrefs
    addAnnotations(entry, element);
    
    entry.setCustom(element);
    return element;
  }

  /**
   * @param element
   */
  private void addDataSources(BioPAXElement element) {
    for (Provenance ds : pathway.getDataSource()) {
      ((Entity)element).addDataSource(ds);
    }
  }

  /* (non-Javadoc)
   * @see de.zbit.kegg.io.KEGG2BioPAX#createPathwayInstance(de.zbit.kegg.parser.pathway.Pathway)
   */
  @Override
  protected BioPAXElement createPathwayInstance(Pathway p) {
    pathway = model.addNew(org.biopax.paxtools.model.level3.Pathway.class, p.getName());
    pathway.addAvailability(String.format("This file has been generated by %s version %s", System.getProperty("app.name"), System.getProperty("app.version")));
    pathway.addName(formatTextForHTMLnotes(p.getTitle()));
    pathway.setDisplayName(formatTextForHTMLnotes(p.getTitle()));
    pathway.setStandardName(formatTextForHTMLnotes(p.getTitle()));
    
    // Parse Kegg Pathway information
    boolean isKEGGPathway = DatabaseIdentifiers.checkID(DatabaseIdentifiers.IdentifierDatabases.KEGG_Pathway, p.getName());
    if (isKEGGPathway) {
      Xref xr = (Xref)createXRef(IdentifierDatabases.KEGG_Pathway, p.getName());
      pathway.addXref(xr);
    }

    // Retrieve further information via Kegg Adaptor
    pathway.setOrganism((BioSource) createBioSource(p));
    
    // Get PW infos from KEGG Api for Description and GO ids.
    KeggInfos pwInfos = KeggInfos.get(p.getName(), manager); // NAME, DESCRIPTION, DBLINKS verwertbar
    if (pwInfos.queryWasSuccessfull()) {
      pathway.addComment(formatTextForHTMLnotes(pwInfos.getDescription()));
      
      // GO IDs
      if (pwInfos.getGo_id() != null) {
        for (String goID : pwInfos.getGo_id().split("\\s")) {
          pathway.addXref((Xref)createXRef(IdentifierDatabases.GeneOntology, goID, 2));
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
      instantiate = Catalysis.class;
    }
    
    // Create the actual reaction or Catalysis
    BioPAXElement element = model.addNew(instantiate, '#'+NameToSId(r.getName()));
    BiochemicalReaction reaction;
    if ((element instanceof Catalysis)) {
      // setup enzymes
      if (hasEnzymes) {
        Set<BioPAXElement> addedEnzymes = new HashSet<BioPAXElement>();
        for (Entry ce:enzymes) {
          if (ce!=null) {
            // Get current component
            BioPAXElement ceb = (BioPAXElement) ce.getCustom();
            if (ceb==null || !(ceb instanceof Controller) || !addedEnzymes.add(ceb)) continue;
            
            ((Catalysis) element).addController((Controller) ceb);
          }
        }
      }
      
      addDataSources(element);
      ((Catalysis) element).addName(r.getName()+"_Catalysis");
      
      // Create actual reaction
      reaction = model.addNew(BiochemicalReaction.class, '#'+NameToSId(r.getName()));
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
    reaction.setDisplayName(r.getName());
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
   * @return <code>TRUE</code> if the component has been added successfully.
   */
  private boolean configureReactionComponent(Pathway p, BiochemicalReaction reaction, ReactionComponent rc, boolean substrate) {
    if (!rc.isSetID() && !rc.isSetName()) {
      rc = rc.getAlt();
      if (rc==null || ((!rc.isSetID() && !rc.isSetName()))) return false;
    }
    
    // Get BioPAX element for component
    Entry ce = p.getEntryForReactionComponent(rc);
    if (ce==null || ce.getCustom()==null) return false;
    BioPAXElement ceb = (BioPAXElement) ce.getCustom();
    if (ceb==null || !(ceb instanceof PhysicalEntity)) return false;
    
    // Set the stoichiometry
    Integer stoich = rc.getStoichiometry();
    Stoichiometry s = model.addNew(Stoichiometry.class, '#'+NameToSId(ce.getName()+"_"+reaction.getDisplayName()+"_stoich"));
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
    
    // "binding/assoc.", "dissociation", "missing interaction" and in doubt to PhysicalInteraction
    if ((subtype.contains(SubType.ASSOCIATION) || subtype.contains(SubType.BINDING) || subtype.contains(SubType.BINDING_ASSOCIATION)) ||
        (subtype.contains(SubType.DISSOCIATION)) || subtype.contains(SubType.MISSING_INTERACTION) || subtype.size()<1) {
      instantiate = MolecularInteraction.class; // Interaction is same as physicalInteraction.class in L2
    }
    
    // Make a final check, if we are able to create a the desired class (e.g., a conversion)
    if (!(qOne instanceof PhysicalEntity) || !(qTwo instanceof PhysicalEntity)) { 
      if (instantiate == Conversion.class) {
        log.fine("Changing from Conversion to MolecularInteraction, because Conversion requires physical entities as participants " + r);
        instantiate = MolecularInteraction.class;
      } 
      if ((instantiate == MolecularInteraction.class) && 
          (!(qOne instanceof PhysicalEntity) && !(qTwo instanceof PhysicalEntity))) {
        // MolecularInteraction requires at least one PhysicalEntity (only by definition).
        log.fine("Changing from MolecularInteraction to Interaction, because MolecularInteraction requires at least one physical entity as participant " + r);
        instantiate = Interaction.class;
      }
    }
    
    // Create the relation
    Interaction bpe = (Interaction) model.addNew(instantiate, '#'+NameToSId("KEGGrelation"));
    
    
    // Add Annotations
    addDataSources(bpe);
    if (subtype.size()>0) {
      bpe.addComment("LINE-TYPE: " + r.getSubtypes().iterator().next().getValue());
      bpe.addName(ArrayUtils.implode(subtype, ", "));
      
      for (SubType st: r.getSubtypes()) {
        bpe.addInteractionType((InteractionVocabulary) getInteractionVocuabulary(st));
      }
    }
    
    // Add participants
    if (bpe instanceof Conversion) {
      ((Conversion) bpe).addLeft((PhysicalEntity) qOne);
      ((Conversion) bpe).addRight((PhysicalEntity) qTwo);
    } else {
      bpe.addParticipant((Entity) qOne);
      bpe.addParticipant((Entity) qTwo);
    }
    
    return bpe;
  }

}
