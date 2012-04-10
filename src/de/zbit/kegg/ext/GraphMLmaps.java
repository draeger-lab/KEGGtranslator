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
package de.zbit.kegg.ext;

import de.zbit.kegg.parser.pathway.EntryType;

/**
 * This is a container for GraphML map-references the provide
 * additional information for nodes.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public interface GraphMLmaps {
  
  /**
   * The label of the node, as it is drawn in the graph.
   */
  public final static String NODE_LABEL = "nodeLabel";
  
  /**
   * Entrez gene ids (NCBI-GENE-ID). Usually splitted for same
   * genes by a space and multiple orthologs, are divided by a comma.
   */
  public final static String NODE_GENE_ID = "entrezIds";
  
  /**
   * Node type ("small molecule", "protein" or any {@link EntryType}.
   */
  public final static String NODE_TYPE = "type";
  
  /**
   * Human-readable description for the node.
   */
  public final static String NODE_DESCRIPTION = "description";
  
  /**
   * KEGG ids. Usually splitted for same genes by a space
   * and multiple orthologs, are divided by a comma.
   */
  public final static String NODE_KEGG_ID = "keggIds";
  
  /**
   * UNIPROT ids. Usually splitted for same genes by a space
   * and multiple orthologs, are divided by a comma.
   */
  public final static String NODE_UNIPROT_ID = "uniprotIds";
  
  /**
   * ENSEMBL ids. Usually splitted for same genes by a space
   * and multiple orthologs, are divided by a comma.
   */
  public final static String NODE_ENSEMBL_ID = "ensemblIds";
  
  /**
   * URL providing further information for the node.
   */
  public final static String NODE_URL = "url";
  
  /**
   * Original node color as given by the KGML.
   */
  public final static String NODE_COLOR = "nodeColor";
  
  /**
   * <b>All synonyms</b> and symbols for the node. Usually
   * splitted for same genes by a space and multiple
   * orthologs, are divided by a comma.
   */
  public final static String NODE_NAME = "nodeName";
  
  /**
   * Original node position as given by the KGML. ("X|Y")
   */
  public final static String NODE_POSITION = "nodePosition";

  /**
   * Original node size as given by the KGML. ("Width|Height")
   */
  public final static String NODE_SIZE = "nodeSize";
  
  
  
  /**
   * Human-readable description for the edge.
   */
  public final static String EDGE_DESCRIPTION = "description";
  
  /**
   * Interaction type of the edge. E.g., "+p" for phosphorylation or
   * "+m" and so on.
   */
  public final static String EDGE_TYPE = "interactionType";
  
  
  
  
}
