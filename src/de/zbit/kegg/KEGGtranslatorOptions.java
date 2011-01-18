/*
 * Copyright (c) 2011 Center for Bioinformatics of the University of Tuebingen.
 * 
 * This file is part of KEGGtranslator, a program to convert KGML files from the
 * KEGG database into various other formats, e.g., SBML, GraphML, and many more.
 * Please visit <http://www.ra.cs.uni-tuebingen.de/software/KEGGtranslator> to
 * obtain the latest version of KEGGtranslator.
 * 
 * KEGGtranslator is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * KEGGtranslator is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with KEGGtranslator. If not, see
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package de.zbit.kegg;

import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;

/**
 * @author wrzodek
 * @author Andreas Dr&auml;ger
 */
public abstract interface KEGGtranslatorOptions extends KeyProvider {
  
  /*
   * Generic translation options
   */
  /**  
   * If true, remove all nodes that have no edges, before translating the pathway.
   */
  public static final Option<Boolean> REMOVE_ORPHANS = new Option<Boolean>("REMOVE_ORPHANS",Boolean.class,
      "If true, remove all nodes that have no edges, before translating the pathway.", (short) 2, "-ro", true);

  /**
   * If true, shows only short names of all KEGG entries.
   */
  public static final Option<Boolean> SHORT_NAMES = new Option<Boolean>("SHORT_NAMES",Boolean.class,
      "If true, shows only short names of all KEGG entries.", true);
  
  /**
   * If true, removes all gene-nodes in the KEGG document, which are white.
   */
  public static final Option<Boolean> REMOVE_WHITE_GENE_NODES = new Option<Boolean>("REMOVE_WHITE_GENE_NODES",Boolean.class,
      "If true, removes all gene-nodes in the KEGG document, which are white.", true);

  /**
   * If true, automatically looks for missing reactants and enzymes of reactions and adds them to the document. 
   */
  public static final Option<Boolean> AUTOCOMPLETE_REACTIONS = new Option<Boolean>("AUTOCOMPLETE_REACTIONS",Boolean.class,
      "If true, automatically looks for missing reactants and enzymes of reactions and adds them to the document.", (short) 2, "-ar", true);
  
  /**
   * If true, no additional information will be retrieved from the KEGG-Server.
   */
  // =! retrieveKeggAnnots
  public static final Option<Boolean> OFFLINE_MODE = new Option<Boolean>("OFFLINE_MODE",Boolean.class,
      "If true, no additional information will be retrieved from the KEGG-Server.", false);

  /**
   * Define various options that are used in all translations.
   */
  @SuppressWarnings("unchecked")
  public static final OptionGroup<Boolean> GENERIC_OPTIONS = new OptionGroup<Boolean>(
      "Generic translation options",
      "Define various options that are used in all translations.",
      REMOVE_ORPHANS, SHORT_NAMES, REMOVE_WHITE_GENE_NODES, AUTOCOMPLETE_REACTIONS, OFFLINE_MODE);

  /*
   * Graphical, yFiles based translations
   */
  
  /**
   * If true, merges all nodes that have exactly the same relations (sources, targets and types). 
   */
  public static final Option<Boolean> MERGE_NODES_WITH_SAME_EDGES = new Option<Boolean>("MERGE_NODES_WITH_SAME_EDGES",Boolean.class,
      "If true, merges all nodes that have exactly the same relations (sources, targets and types).", (short) 2, "--merge", false);
  public static final Option<Boolean> CREATE_EDGE_LABELS = new Option<Boolean>("CREATE_EDGE_LABELS",Boolean.class,
      "If true, creates describing labels for each edge in the graph.", (short) 2, "--cel", false);
  
  /**
   * Define various options that are used in yFiles based translations.
   */
  @SuppressWarnings("unchecked")
  public static final OptionGroup<Boolean> GRAPH_OPTIONS = new OptionGroup<Boolean>(
      "Translation options for graphical outputs",
      "Define various options that are used in yFiles based translations.",
      MERGE_NODES_WITH_SAME_EDGES, CREATE_EDGE_LABELS);
  
  /*
   * Funcional, SBML based translations
   */
  
  /**
   * If true, adds celldesigner annotations to the SBML-XML document.
   */
  public static final Option<Boolean> CELLDESIGNER_ANNOTATIONS = new Option<Boolean>("CELLDESIGNER_ANNOTATIONS",Boolean.class,
      "If true, adds celldesigner annotations to the SBML-XML document.", (short) 2, "-cd", false);
  
  /**
   * Define various options that are used in SBML based translations.
   */
  @SuppressWarnings("unchecked")
  public static final OptionGroup<Boolean> SBML_OPTIONS = new OptionGroup<Boolean>(
      "Translation options for SBML outputs",
      "Define various options that are used in SBML based translations.",
      CELLDESIGNER_ANNOTATIONS);
  
}
