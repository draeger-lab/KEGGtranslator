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
package de.zbit.kegg;

import de.zbit.gui.actioncommand.ActionCommand;
import de.zbit.kegg.ext.KEGGTranslatorPanelOptions;
import de.zbit.util.StringUtil;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;
import de.zbit.util.prefs.Range;

/**
 * This class contains user-specific general settings for translating
 * KEGG files to other output formats.
 * 
 * @author Clemens Wrzodek
 * @author Andreas Dr&auml;ger
 * @since 1.0
 * @version $Rev$
 */
public abstract interface KEGGtranslatorOptions extends KeyProvider {
  
  /**
   * Enum to select how to name the nodes.
   * @author Clemens Wrzodek
   */
  public static enum NODE_NAMING implements ActionCommand {
    /**
     * Simply first name with fewest chars
     */
    SHORTEST_NAME,
    /**
     * Name, mentioned in the KGML
     */
    FIRST_NAME_FROM_KGML,
    /**
     * Simply first Name the api returns
     */
    FIRST_NAME,
    /**
     * Complete (unrecommended) very long String of all names.
     */
    ALL_FIRST_NAMES,
    /**
     * Same as {@link #INTELLIGENT}, but preferes EC numbers, when
     * available.
     */    
    INTELLIGENT_WITH_EC_NUMBERS,
    /**
     * 
     */
    INTELLIGENT;

    /* (non-Javadoc)
     * @see de.zbit.gui.ActionCommand#getName()
     */
    public String getName() {
      switch (this) {
      case FIRST_NAME_FROM_KGML:
        return "First name from KGML";
      case FIRST_NAME:
        return "First given name (usually the HGNC symbol)";
        
      default:
        return StringUtil.firstLetterUpperCase(toString().toLowerCase()
            .replace('_', ' '));
      }
    }

    /* (non-Javadoc)
     * @see de.zbit.gui.ActionCommand#getToolTip()
     */
    public String getToolTip() {
      switch (this) {
        case SHORTEST_NAME:
        return "Name genes according to the name with fewest characters (creates nice graphs).";
        case FIRST_NAME_FROM_KGML:
          return "Name genes according to name in KGML.";
        case FIRST_NAME:
          return "Name genes according to the first name, given by the KEGG API. This is usually the HGNC symbol.";
        case INTELLIGENT_WITH_EC_NUMBERS:
          return "Assigns EC numbers to enzymes and official symbols/ family prefixes or short names to others";
        case INTELLIGENT:
          return "Assigns HGNC symbols to genes, prefixes to families and short names to compounds.";
        case ALL_FIRST_NAMES:
          return "Name genes according to the first names, given by the KEGG API. " +
          		"This is will create multiple names if one KEGG entry consists of multiple genes and is thus NOT RECOMMENDED.";

      default:
        return "";
      }
    };
  }
  
  /*
   * Generic translation options
   */
  /**  
   * If true, remove all nodes that have no edges, before translating the pathway.
   */
  public static final Option<Boolean> REMOVE_ORPHANS = new Option<Boolean>("REMOVE_ORPHANS",Boolean.class,
      "If true, remove all nodes that have no edges before translating the pathway.", (short) 2, "-ro", false);

  /**
   * If true, shows only short names of all KEGG entries.
   */
//  public static final Option<Boolean> SHORT_NAMES = new Option<Boolean>("SHORT_NAMES",Boolean.class,
//      "If true, shows only short names of all KEGG entries.", true);
  
  /**
   * How to label translated KEGG entries in the target node/species/etc.
   */
  public static final Option<NODE_NAMING> GENE_NAMES = new Option<NODE_NAMING>("GENE_NAMES",NODE_NAMING.class,
      "For one KEGG object, multiple names are available. Choose how to assign one name to this object.",
      new Range<NODE_NAMING>(NODE_NAMING.class, Range.toRangeString(NODE_NAMING.class)),
      NODE_NAMING.INTELLIGENT, "Label genes by");

  /*
   * TODO add an option as extension to GENE_NAMES:
   * - "Always use EC numbers as label for enzymes"
   *  
   */
  
  /**
   * If true, shows the chemical formula for all compounds, instead of the name.
   */
  public static final Option<Boolean> SHOW_FORMULA_FOR_COMPOUNDS = new Option<Boolean>("SHOW_FORMULA_FOR_COMPOUNDS",Boolean.class,
      "If true, shows the chemical formula for all compounds, instead of the name.", (short) 2, "-formula", false);
  
  /**
   * If true, removes all gene-nodes in the KEGG document, which are white.
   */
  public static final Option<Boolean> REMOVE_WHITE_GENE_NODES = new Option<Boolean>("REMOVE_WHITE_GENE_NODES",Boolean.class,
      "If true, removes all white gene-nodes in the KEGG document (usually enzymes that have no real instance on the current organism).", (short) 2, "-nowhite", false);

  /**
   * If true, automatically looks for missing reactants and enzymes of reactions and adds them to the document. 
   */
  public static final Option<Boolean> AUTOCOMPLETE_REACTIONS = new Option<Boolean>("AUTOCOMPLETE_REACTIONS",Boolean.class,
      "If true, automatically looks for missing reactants and enzymes of reactions and adds them to the document.", (short) 2, "-ar", true);
  
  /**
   * Check atom balance
   */
  public static final Option<Boolean> CHECK_ATOM_BALANCE = new Option<Boolean>("CHECK_ATOM_BALANCE", Boolean.class,
      "Check the atom balance of metabolic reactions and write a summary to the reaction notes. Depends on autocomplete reactions.", (short) 2, "-cbal", true, 
      AUTOCOMPLETE_REACTIONS, KEGGTranslatorPanelOptions.TRUE_RANGE);

  /**
   * If true, removes all entries (nodes, species, etc.) referring to other pathways.
   */
  public static final Option<Boolean> REMOVE_PATHWAY_REFERENCES = new Option<Boolean>("REMOVE_PATHWAY_REFERENCES",Boolean.class,
      "If true, removes all entries (nodes, species, etc.) referring to other pathways.", (short) 2, "-nopr", false);
  
  /**
   * If true, no additional information will be retrieved from the KEGG-Server.
   * Unfortunately, is not considered by every method and translator => feature
   * has been deactivated until it is required by somebody.
   */
  // =! retrieveKeggAnnots
  public static final Option<Boolean> OFFLINE_MODE = new Option<Boolean>("OFFLINE_MODE",Boolean.class,
      "If true, no additional information will be retrieved from the KEGG-Server.", false, false);

  /**
   * Define various options that are used in all translations.
   */
  @SuppressWarnings("unchecked")
  public static final OptionGroup<?> GENERIC_OPTIONS = new OptionGroup<Object>(
      "Generic translation options",
      "Define various options that are used in all translations.", // SHORT_NAMES
      REMOVE_ORPHANS, GENE_NAMES, SHOW_FORMULA_FOR_COMPOUNDS, REMOVE_WHITE_GENE_NODES,
      AUTOCOMPLETE_REACTIONS, REMOVE_PATHWAY_REFERENCES, OFFLINE_MODE);

  /*
   * Graphical, yFiles based translations
   */
  
  /**
   * If true, merges all nodes that have exactly the same relations (sources, targets and types). 
   */
  public static final Option<Boolean> MERGE_NODES_WITH_SAME_EDGES = new Option<Boolean>("MERGE_NODES_WITH_SAME_EDGES",Boolean.class,
      "If true, merges all nodes that have exactly the same relations (sources, targets and types).", (short) 2, "-merge", false);
  public static final Option<Boolean> CREATE_EDGE_LABELS = new Option<Boolean>("CREATE_EDGE_LABELS",Boolean.class,
      "If true, creates describing labels for each edge in the graph.", (short) 2, "-cel", false);
  public static final Option<Boolean> HIDE_LABELS_FOR_COMPOUNDS = new Option<Boolean>("HIDE_LABELS_FOR_COMPOUNDS",Boolean.class,
      "If true, hides labels for all compounds (=small molecules).", (short) 2, "-hc", false);
  public static final Option<Boolean> DRAW_GREY_ARROWS_FOR_REACTIONS = new Option<Boolean>("DRAW_GREY_ARROWS_FOR_REACTIONS",Boolean.class,
      "If true, creates grey arrows for reactions and arrows with transparent circles as heads for reaction modifiers. This does only " +
      "affect reactions defined by KEGG, not the relations.", (short) 2, "-dar", false);
  
  /**
   * Define various options that are used in yFiles based translations.
   */
  @SuppressWarnings("unchecked")
  public static final OptionGroup<Boolean> GRAPH_OPTIONS = new OptionGroup<Boolean>(
      "Translation options for graphical outputs",
      "Define various options that are used in yFiles based translations.",
      MERGE_NODES_WITH_SAME_EDGES, CREATE_EDGE_LABELS, HIDE_LABELS_FOR_COMPOUNDS, DRAW_GREY_ARROWS_FOR_REACTIONS);
  
  /*
   * Funcional, SBML based translations
   */
  
  /**
   * If true, adds celldesigner annotations to the SBML-XML document.
   */
  public static final Option<Boolean> CELLDESIGNER_ANNOTATIONS = new Option<Boolean>("CELLDESIGNER_ANNOTATIONS",Boolean.class,
      "If true, adds celldesigner annotations to the SBML-XML document.", (short) 2, "-cd", false, false);

  /**
   * If true, adds layout information to the SBML-document.
   */
  public static final Option<Boolean> ADD_LAYOUT_EXTENSION = new Option<Boolean>("ADD_LAYOUT_EXTENSION",Boolean.class,
      "If true, adds layout information, using the SBML layout extension to the SBML document." +
      "As a side-effect, this will create an SBML Level 3 model.", (short) 2, "-layout", false);

  /**
   * If true, uses the groups extension to encode groups in the SBML document.
   */
  public static final Option<Boolean> USE_GROUPS_EXTENSION = new Option<Boolean>("USE_GROUPS_EXTENSION",Boolean.class,
      "If true, uses the SBML level 3 groups extension to encode groups in the SBML document." +
      "As a side-effect, this will create an SBML Level 3 model.", (short) 2, "-groups", true);
  
  /**
   * Define various options that are used in SBML based translations.
   */
  @SuppressWarnings("unchecked")
  public static final OptionGroup<Boolean> SBML_OPTIONS = new OptionGroup<Boolean>(
      "Translation options for SBML outputs",
      "Define various options that are used in SBML based translations.",
      CELLDESIGNER_ANNOTATIONS, ADD_LAYOUT_EXTENSION, USE_GROUPS_EXTENSION, CHECK_ATOM_BALANCE);
  
}
