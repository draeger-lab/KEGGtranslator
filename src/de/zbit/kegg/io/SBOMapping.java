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

import java.util.HashMap;
import java.util.Map;

import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.EntryType;
import de.zbit.kegg.parser.pathway.Relation;
import de.zbit.kegg.parser.pathway.SubType;
import de.zbit.kegg.parser.pathway.ext.EntryExtended;
import de.zbit.kegg.parser.pathway.ext.EntryTypeExtended;
import de.zbit.util.StringUtil;
import de.zbit.util.objectwrapper.ValuePair;

/**
 * This static class defines how to map from certain
 * {@link EntryType}s or GeneTypes to SBO
 * terms.
 * 
 * @author Clemens Wrzodek
 * @author Finja B&uuml;chel
 * @version $Rev$
 */
public class SBOMapping {
  
  /* **********************************
   * ENTRY TYPE MAPPINGS
   ***********************************/
  
  /**
   * SBO Term for EntryType "general modifier" (compound, map, other).
   */
  public static int ET_GeneralModifier2SBO = 13; // 13=catalyst
  
  /**
   * SBO Term for EntryType enzyme, gene, group, ortholog, genes.
   */
  public static int ET_EnzymaticModifier2SBO = 460;// 460="enzymatic catalyst"
  
  /**
   * SBO Term for EntryType Gene.
   */
  public static int ET_Gene2SBO = 252; // 252="polypeptide chain" [old: 354="informational molecule segment"]
  
  /**
   * SBO Term for EntryType Enzyme.
   */
  public static int ET_Enzyme2SBO = ET_Gene2SBO; // 245="macromolecule",	// 252="polypeptide chain"
  
  /**
   * SBO Term for EntryType Ortholog.
   */
  public static int ET_Ortholog2SBO = ET_Gene2SBO; // 354="informational molecule segment"
  /**
   * SBO Term for EntryType Group.
   */
  public static int ET_Group2SBO = 253; // 253="non-covalent complex"
  /**
   * SBO Term for EntryType Compound.
   */
  public static int ET_Compound2SBO = 247; // 247="Simple Chemical"
  /**
   * SBO Term for EntryType Map.
   */
  public static int ET_Map2SBO = 552; // 552="reference annotation"
  /**
   * SBO Term for EntryType Other.
   */
  public static int ET_Other2SBO = 285; // 285="material entity of unspecified nature"
  /**
   * SBO Term for EntryType emptySet.
   */
  public static int ET_EmptySet2SBO = 291; // 291="empty set"
  
  
  
  /* **********************************
   * GENE TYPE MAPPINGS
   * NOTE: The missing ones are already covered by the
   * EntryType!
   ***********************************/
  
  /**
   * SBO Term for GeneType Protein.
   */
  public static int GT_Protein2SBO = ET_Gene2SBO; // 252="polypeptide chain"
  
  /**
   * SBO Term for GeneType DNA.
   */
  public static int GT_DNA2SBO = 251; // 251="deoxyribonucleic acid"
  
  /**
   * SBO Term for GeneType DNARegion.
   */
  public static int GT_DNARegion2SBO = GT_DNA2SBO; // 251="deoxyribonucleic acid"
  
  /**
   * SBO Term for GeneType RNA.
   */
  public static int GT_RNA2SBO = 250; // 252="ribonucleic acid"
  
  /**
   * SBO Term for GeneType RNARegion.
   */
  public static int GT_RNARegion2SBO = GT_RNA2SBO; // 252="ribonucleic acid"
  
  /**
   * SBO Term for GeneType Gene (a real gene).
   */
  public static int GT_Gene2SBO = 354; // 354="informational molecule segment"
  
  
  /**
   * A map to translate {@link SubType}s of {@link Relation}s to SBO terms
   */
  private static Map<String, Integer> subtype2SBO = new HashMap<String, Integer>();
  
  /**
   * A try to translate the {@link SubType}s to GO terms.
   */
  private static Map<String, Integer> subtype2GO = new HashMap<String, Integer>();
  
  static {
    // Init subtype map
    subtype2SBO.put(SubType.ACTIVATION, 170); // = stimulation
    subtype2SBO.put(SubType.ASSOCIATION, 177); // = non-covalent binding
    subtype2SBO.put(SubType.BINDING, 177);
    subtype2SBO.put(SubType.BINDING_ASSOCIATION, 177);
    subtype2SBO.put(SubType.DISSOCIATION, 180);
    subtype2SBO.put(SubType.EXPRESSION, 170);
    subtype2SBO.put(SubType.GLYCOSYLATION, 217); // glycosylation
    subtype2SBO.put(SubType.INDIRECT_EFFECT, 344); // molecular interaction
    subtype2SBO.put(SubType.INHIBITION, 169);
    subtype2SBO.put(SubType.METHYLATION, 214); // methylation
    subtype2SBO.put(SubType.MISSING_INTERACTION, 396);  // uncertain process
    subtype2SBO.put(SubType.PHOSPHORYLATION, 216); // phosphorylation
    subtype2SBO.put(SubType.DEPHOSPHORYLATION, 330); // dephosphorylation
    subtype2SBO.put(SubType.REPRESSION, 169);
    subtype2SBO.put(SubType.STATE_CHANGE, 168); // control
    subtype2SBO.put(SubType.UBIQUITINATION, 224); // ubiquitination
    
    //subtype2GO.put(SubType.ACTIVATION, UNKNOWN); // = stimulation
    subtype2GO.put(SubType.ASSOCIATION, 5488); // = non-covalent binding
    subtype2GO.put(SubType.BINDING, 5488);
    subtype2GO.put(SubType.BINDING_ASSOCIATION, 5488);
    subtype2GO.put(SubType.DEPHOSPHORYLATION, 16311); // dephosphorylation
    //subtype2GO.put(SubType.DISSOCIATION, 180);
    subtype2GO.put(SubType.EXPRESSION, 10467); // = gene expression
    subtype2GO.put(SubType.GLYCOSYLATION, 70085); // glycosylation
    //subtype2GO.put(SubType.INDIRECT_EFFECT, 344);
    //subtype2GO.put(SubType.INHIBITION, 169);
    subtype2GO.put(SubType.METHYLATION, 32259); // methylation
    //subtype2GO.put(SubType.MISSING_INTERACTION, 396);
    subtype2GO.put(SubType.PHOSPHORYLATION, 16310); // phosphorylation
    //subtype2GO.put(SubType.REPRESSION, 169);
    //subtype2GO.put(SubType.STATE_CHANGE, 168);
    subtype2GO.put(SubType.UBIQUITINATION, 16567); // protein ubiquitination
  }
  
  /**
   * Get the most appropriate SBO term for this
   * <code>entry</code>.
   * @param entry
   * @return
   */
  public static int getSBOTerm(Entry entry) {
    if (entry instanceof EntryExtended &&
        ((EntryExtended) entry).isSetGeneType()) {
      EntryTypeExtended type = ((EntryExtended) entry).getGeneType();
      
      if (type.equals(EntryTypeExtended.protein)) {
        return GT_Protein2SBO;
      } else if (type.equals(EntryTypeExtended.gene)) {
        return GT_Gene2SBO;
      } else if (type.equals(EntryTypeExtended.dna)) {
        return GT_DNA2SBO;
      } else if (type.equals(EntryTypeExtended.dna_region)) {
        return GT_DNARegion2SBO;
      } else if (type.equals(EntryTypeExtended.rna)) {
        return GT_RNA2SBO;
      } else if (type.equals(EntryTypeExtended.rna_region)) {
        return GT_RNARegion2SBO;
      } else if (type.equals(EntryTypeExtended.emptySet)) {
        return ET_EmptySet2SBO;
      } else {
        // GeneType is NOT mandatory and just additionally
        // to the entryType!
        return getSBOTerm(entry.getType());
      }
    } else {
      return getSBOTerm(entry.getType());
    }
  }
  
  
  /**
   * Returns the SBO Term for an EntryType.
   * @param type the KEGG EntryType, you want an SBMO Term for.
   * @return SBO Term (integer).
   */
  private static int getSBOTerm(EntryType type) {
    if (type.equals(EntryType.compound)) {
      return ET_Compound2SBO;
    }
    if (type.equals(EntryType.enzyme)) {
      return ET_Enzyme2SBO;
    }
    if (type.equals(EntryType.gene)) {
      return GT_Protein2SBO;
    }
    if (type.equals(EntryType.group)) {
      return ET_Group2SBO;
    }
    if (type.equals(EntryType.genes)) {
      return ET_Group2SBO;
    }
    if (type.equals(EntryType.map)) {
      return ET_Map2SBO;
    }
    if (type.equals(EntryType.ortholog)) {
      return ET_Ortholog2SBO;
    }
    
    if (type.equals(EntryType.other)) {
      return ET_Other2SBO;
    }
    
    return ET_Other2SBO;
  }
  
  
  /**
   * Get an SBO term for a {@link SubType}.
   * Typically, {@link SubType}s are used in {@link Relation}s.
   * @param subtype one of the constants, defined in the {@link SubType} class.
   * @return appropriate SBO term.
   */
  public static int getSBOTerm(String subtype) {
    // NOTE: It is intended to return -1 for "compound"!
    Integer ret = subtype2SBO.get(subtype);
    if (ret==null) {
      ret = -1;
    }
    return ret;
  }
  
  /**
   * Get a GO term for a {@link SubType}.
   * @param subtype
   * @return
   */
  public static int getGOTerm(String subtype) {
    // NOTE: There are some subtypes without GO terms!
    Integer ret = subtype2GO.get(subtype);
    if (ret==null) {
      ret = -1;
    }
    return ret;
  }
  
  /**
   * Convert <code>subtype</code> to a MI-term that is a child of 'MI:0190' (Molecular Interaction (PSI-MI)).
   * The terms are for relations/interactions.
   * @param subtype
   * @return {@link ValuePair} with the term name and integer id. Or <code>NULL</code> if
   * no MI term is available that matches the given input {@link SubType}.
   */
  public static ValuePair<String, Integer> getMITerm(String subtype) {
    if (subtype.equals(SubType.ASSOCIATION)) {
      return new ValuePair<String, Integer>("association", 914); //MI:0914
    } else if (subtype.equals(SubType.BINDING)) {
      return new ValuePair<String, Integer>("covalent binding", 195);
    } else if (subtype.equals(SubType.BINDING_ASSOCIATION)) {
      return new ValuePair<String, Integer>("association", 914);
      
    } else if (subtype.equals(SubType.PHOSPHORYLATION)) {
      return new ValuePair<String, Integer>("phosphorylation reaction", 217);
    } else if (subtype.equals(SubType.DEPHOSPHORYLATION)) {
      return new ValuePair<String, Integer>("dephosphorylation reaction", 203);
    } else if (subtype.equals(SubType.UBIQUITINATION) ||
        subtype.equalsIgnoreCase("ubiquination")) {
      return new ValuePair<String, Integer>("ubiquitination reaction", 220);
    } else if (subtype.equals(SubType.GLYCOSYLATION)) {
      return new ValuePair<String, Integer>("glycosylation reaction", 559);
    } else if (subtype.equals(SubType.METHYLATION)) {
      return new ValuePair<String, Integer>("methylation reaction", 213);
      
    } else if (subtype.equals(SubType.COMPOUND) ||
        subtype.equals(SubType.HIDDEN_COMPOUND)) {
      return new ValuePair<String, Integer>("direct interaction", 407);
      
      //    } else if (subtype.equals(SubType.REPRESSION)) {
      //      return new ValuePair<String, Integer>("suppression", 796);
      //    } else if (subtype.equals(SubType.DEPHOSPHORYLATION)) {
      //      return new ValuePair<String, Integer>("dephosphorylation reaction", 203);
      //    } else if (subtype.equals(SubType.DEPHOSPHORYLATION)) {
      //      return new ValuePair<String, Integer>("dephosphorylation reaction", 203);
      //    } else if (subtype.equals(SubType.DEPHOSPHORYLATION)) {
      //      return new ValuePair<String, Integer>("dephosphorylation reaction", 203);
      //    } else if (subtype.equals(SubType.DEPHOSPHORYLATION)) {
      //      return new ValuePair<String, Integer>("dephosphorylation reaction", 203);
    }
    /* MISSING:
     * EXPRESSION => positive genetic interaction / 935
     * ACTIVATION => positive genetic interaction / 935
     * REPRESSION => negative genetic interaction / 933
     * INHIBITION => negative genetic interaction / 933
     * 
     * DISSOCIATION
     * INDIRECT_EFFECT
     * MISSING_INTERACTION
     * STATE_CHANGE
     * 
     */
    
    return null;
  }
  
  /**
   * Convert <code>subtype</code> to a MOD-term (Protein Modification Ontology (PSI-MOD))
   * that is either a child of 'MOD:01157' or 'MOD:01156'.
   * <p>The terms are for entries that are modified as result of an relation/interaction.
   * @param subtype
   * @return {@link ValuePair} with the term name and integer id. Or <code>NULL</code> if
   * no MI term is available that matches the given input {@link SubType}.
   */
  public static ValuePair<String, Integer> getMODTerm(String subtype) {
    
    if (subtype.equals(SubType.PHOSPHORYLATION)) {
      return new ValuePair<String, Integer>("phosphorylated residue", 696);
      //    } else if (subtype.equals(SubType.DEPHOSPHORYLATION)) {
      //      return new ValuePair<String, Integer>("5'-dephospho", 948);
      // This term is not valid (no child of 'MOD:01157' or 'MOD:01156').
    } else if (subtype.equals(SubType.UBIQUITINATION) ||
        subtype.equalsIgnoreCase("ubiquination")) {
      return new ValuePair<String, Integer>("ubiquitinylation residue", 492);
    } else if (subtype.equals(SubType.GLYCOSYLATION)) {
      return new ValuePair<String, Integer>("glycosylated residue", 693);
    } else if (subtype.equals(SubType.METHYLATION)) {
      return new ValuePair<String, Integer>("methylated residue", 427);
    }
    
    return null;
  }
  
  /**
   * Formats an SBO term. E.g. "177" to "SBO:0000177".
   * @param sbo
   * @return
   */
  public static String formatSBO(int sbo) {
    return formatSBO(sbo, "SBO:");
  }
  
  /**
   * Formats an SBO term. E.g. "177" to "SBO%3A0000177".
   * Uses HTML encoding %3A to encode the double point.
   * @param sbo
   * @return
   */
  public static String formatSBOforMIRIAM(int sbo) {
    return formatSBO(sbo, "SBO%3A");
  }
  
  /**
   * Formats an SBO term to contain 7 digits after an
   * arbitrary prefix. E.g., "177" and prefix "SBO%3A"
   * to "SBO%3A0000177".
   * @param sbo
   * @param prefix
   * @return
   */
  private static String formatSBO(int sbo, String prefix) {
    StringBuilder b = new StringBuilder(prefix);
    String iString = Integer.toString(sbo);
    b.append(StringUtil.replicateCharacter('0', 7-iString.length()));
    b.append(iString);
    return b.toString();
  }
  
}
