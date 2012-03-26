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

import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.EntryType;
import de.zbit.kegg.parser.pathway.ext.EntryExtended;
import de.zbit.kegg.parser.pathway.ext.GeneType;

/**
 * This static class defines how to map from certain
 * {@link EntryType}s or {@link GeneType}s to SBO
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
  public static int ET_GeneralModifier2SBO = 13; // 13=catalyst // 460="enzymatic catalyst"
  /**
   * SBO Term for EntryType enzyme, gene, group, ortholog, genes.
   */
  public static int ET_EnzymaticModifier2SBO = 460;
  /**
   * SBO Term for EntryType Ortholog.
   */
  public static int ET_Ortholog2SBO = 354; // 354="informational molecule segment"
  /**
   * SBO Term for EntryType Enzyme.
   */
  public static int ET_Enzyme2SBO = 245; // 245="macromolecule",	// 252="polypeptide chain"
  /**
   * SBO Term for EntryType Gene.
   */
  public static int ET_Gene2SBO = 354; // 354="informational molecule segment"
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
  
  
  
  
  /* **********************************
   * GENE TYPE MAPPINGS
   * NOTE: The missing ones are already covered by the
   * EntryType!
   ***********************************/
  
  /**
   * SBO Term for GeneType Protein.
   */
  public static int GT_Protein2SBO = 252; // 252="polypeptide chain"
  
  /**
   * SBO Term for GeneType DNA.
   */
  public static int GT_DNA2SBO = 251; // 251="deoxyribonucleic acid"
  
  /**
   * SBO Term for GeneType DNARegion.
   */
  public static int GT_DNARegion2SBO = 251; // 251="deoxyribonucleic acid"
  
  /**
   * SBO Term for GeneType RNA.
   */
  public static int GT_RNA2SBO = 250; // 252="ribonucleic acid"
  
  /**
   * SBO Term for GeneType RNARegion.
   */
  public static int GT_RNARegion2SBO = 250; // 252="ribonucleic acid"
  
  
  /**
   * Get the most appropriate SBO term for this
   * <code>entry</code>.
   * @param entry
   * @return
   */
  public static int getSBOTerm(Entry entry) {
    if (entry instanceof EntryExtended && 
        ((EntryExtended) entry).isSetGeneType()) {
      GeneType type = ((EntryExtended) entry).getGeneType();
      
      if (type.equals(GeneType.protein)) {
        return GT_Protein2SBO;
      } else if (type.equals(GeneType.dna)) {
        return GT_DNA2SBO;
      } else if (type.equals(GeneType.dna_region)) {
        return GT_DNARegion2SBO;
      } else if (type.equals(GeneType.rna)) {
        return GT_RNA2SBO;
      } else if (type.equals(GeneType.rna_region)) {
        return GT_RNARegion2SBO;
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
      return ET_Other2SBO;
    
    return ET_Other2SBO;
  }
  
  
  
}
