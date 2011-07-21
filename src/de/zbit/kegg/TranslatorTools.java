/*
 * $Id:  TranslatorTools.java 19:03:26 wrzodek $
 * $URL: TranslatorTools.java $
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

import java.awt.Color;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import y.base.DataMap;
import y.base.Edge;
import y.base.EdgeMap;
import y.base.Graph;
import y.base.Node;
import y.base.NodeMap;
import y.view.Graph2D;
import y.view.LineType;
import de.zbit.kegg.ext.GenericDataMap;
import de.zbit.kegg.gui.TranslatorPanel;
import de.zbit.kegg.io.KEGG2yGraph;

/**
 * This class is intended to provide various translator tools.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class TranslatorTools {
  public static final transient Logger log = Logger.getLogger(TranslatorTools.class.getName());
  
  /**
   * A graph on which operations are performed.
   */
  private Graph2D graph;
  
  /**
   * A reverse map of {@link Graph#getDataProvider(Object)} where Object is
   * {@link KEGG2yGraph#mapDescription}.
   * <p>In other words, returns directly the map for, e.g. "entrezIds"
   */
  private Map<String, DataMap> descriptor2Map=null;
  
  public TranslatorTools(TranslatorPanel tp){
    this(tp.isGraphML()?(Graph2D) tp.getDocument():null);
  }
  
  public TranslatorTools(Graph2D graph){
    super();
    this.graph=graph;
    if (this.graph==null) log.warning("Graph is null!");
    init();
  }
  
  @SuppressWarnings("unchecked")
  private void init() {
    GenericDataMap<DataMap, String> mapDescriptionMap = (GenericDataMap<DataMap, String>) graph.getDataProvider(KEGG2yGraph.mapDescription);
    descriptor2Map = mapDescriptionMap.createReverseMap();
  }
  
  
  /**
   * Highlight all given GeneIDs in RED color. And selects these nodes.
   * @param graph translated pathway with annotated geneIDs
   * @param ncbiGeneIDs geneIDs to color in Red.
   */
  public void highlightGenes(Iterable<Integer> ncbiGeneIDs) {
    highlightGenes(ncbiGeneIDs, Color.RED, Color.LIGHT_GRAY, true);
  }
  
  public void highlightGenes(Iterable<Integer> ncbiGeneIDs, Color highlightColor, Color forAllOthers, boolean changeSelection) {
    if (forAllOthers!=null) {
      setColorOfAllNodesExceptPathwayReferences(forAllOthers);
    }
    if (changeSelection) graph.unselectAll();
    Map<Integer, List<Node>> id2node = getGeneID2NodeMap();
    for (Integer integer : ncbiGeneIDs) {
      List<Node> nList = id2node.get(integer);
      if (nList!=null) {
        for (Node node : nList) {
          graph.getRealizer(node).setFillColor(highlightColor);
          if (changeSelection) {
            graph.getRealizer(node).setSelected(true);
          }
        }
      } else {
        log.info("Could not get a Node for " + integer);
      }
    }
  }
  
  /**
   * Set a unique {@link Color} to all nodes, that are no pathway references.
   * @param colorForUnaffectedNodes
   */
  public void setColorOfAllNodesExceptPathwayReferences(Color colorForUnaffectedNodes) {
    // Set unaffected color for all other nodes but reference nodes.
    for (Node n: graph.getNodeArray()) {
      if (TranslatorTools.getKeggIDs(n).toLowerCase().trim().startsWith("path:")) continue;
      graph.getRealizer(n).setFillColor(colorForUnaffectedNodes);
    }
  }
  
  /**
   * Draws the frame and caption of all nodes, containing the given
   * string in the nodeLabel tag red. Blackens all other frames and
   * labels.
   * @param containedString
   */
  public void searchGenes(String containedString) {
    containedString = containedString.toLowerCase();
    for (Node n: graph.getNodeArray()) {
      Color color = Color.BLACK;
      LineType lt = LineType.LINE_1;
      if (getNodeInfoIDs(n, "nodeLabel").toLowerCase().contains(containedString)) {
        color = Color.RED;
        lt = LineType.LINE_2;
      }
      graph.getRealizer(n).setLineColor(color);
      graph.getRealizer(n).setLineType(lt);
      graph.getRealizer(n).getLabel().setTextColor(color);
    }
  }
  
  /**
   * @param descriptor e.g., "keggIds" or "entrezIds". See {@link KEGG2yGraph} for a complete list.
   * @return map for the descriptor.
   */
  public DataMap getMap(String descriptor) {
    return descriptor2Map.get(descriptor);
  }

  /**
   * Return a map from Entrez GeneID to corresponding {@link Node} for the given
   * translated pathway.
   * @param graph
   * @return map from geneID to List of nodes.
   */
  public Map<Integer, List<Node>> getGeneID2NodeMap() {
    // Build a map from GeneID 2 Node
    Map<Integer, List<Node>> id2node = new HashMap<Integer, List<Node>>();
    
    // Get the NodeMap from entrez 2 node.
//    GenericDataMap<DataMap, String> mapDescriptionMap = (GenericDataMap<DataMap, String>) graph.getDataProvider(KEGG2yGraph.mapDescription);
    NodeMap entrez = (NodeMap) descriptor2Map.get("entrezIds"); //null;
//    if (mapDescriptionMap==null) return null;
//    for (int i=0; i<graph.getRegisteredNodeMaps().length; i++) {
//      NodeMap nm = graph.getRegisteredNodeMaps()[i];
//      if (mapDescriptionMap.getV(nm).equals("entrezIds")) {
//        entrez = nm;
//        break;
//      }
//    }
    if (entrez==null) {
      log.severe("Could not find Node2EntrezID mapping.");
      return null;
    }
    
    // build the resulting map
    for (Node n : graph.getNodeArray()) {
      Object entrezIds = entrez.get(n);
      if (entrezIds!=null && entrezIds.toString().length()>0) {
        String[] ids = entrezIds.toString().split(",|\\s"); // comma or space separated.
        for (String id: ids) {
          if (id==null || id.trim().length()<1) continue;
          try {
            // Get Node collection for gene ID
            Integer intId = Integer.parseInt(id);
            List<Node> list = id2node.get(intId);
            if (list==null) {
              list = new LinkedList<Node>();
              id2node.put(intId, list);
            }
            // Add node to list.
            list.add(n);
          } catch (NumberFormatException e) {
            log.log(Level.WARNING, "Could not get geneID for node.", e);
          }
        }
      }
    }
    
    return id2node;
  }
  
  /**
   * @param n a node
   * @param descriptor e.g., "keggIds" or "entrezIds". See {@link KEGG2yGraph} for a complete list.
   * @return the string associated with this node.
   */
  @SuppressWarnings("unchecked")
  public static String getNodeInfoIDs(Node n, String descriptor) {
    Graph graph = n.getGraph();
    
    // Get the NodeMap from kegg 2 node.
    GenericDataMap<DataMap, String> mapDescriptionMap = (GenericDataMap<DataMap, String>) graph.getDataProvider(KEGG2yGraph.mapDescription);
    NodeMap nodeMap = null;
    if (mapDescriptionMap==null) return null;
    for (int i=0; i<graph.getRegisteredNodeMaps().length; i++) {
      NodeMap nm = graph.getRegisteredNodeMaps()[i];
      if (mapDescriptionMap.getV(nm).equals(descriptor)) {
        nodeMap = nm;
        break;
      }
    }
    if (nodeMap==null) {
      log.severe("Could not find Node2" + descriptor==null?"null":descriptor + " mapping.");
      return null;
    }
    
    // return kegg id(s)
    Object id = nodeMap.get(n);
    return id!=null?id.toString():null;
  }
  
  /**
   * Actually, a faster and more generic version of {@link #getNodeInfoIDs(Node, String)}.
   * @param node_or_edge and {@link Node} or {@link Edge}
   * @param descriptor a descriptor for a {@link NodeMap} or {@link EdgeMap} in the current
   * {@link #graph}.
   * @return the requested information or null, if not available.
   */
  public String getInfo(Object node_or_edge, String descriptor) {
    // Get the NodeMap from kegg 2 node.
    DataMap nodeMap = descriptor2Map.get(descriptor);
    if (nodeMap==null) {
      log.severe("Could not find Node2" + descriptor==null?"null":descriptor + " mapping.");
      return null;
    }
    
    // return kegg id(s)
    Object id = nodeMap.get(node_or_edge);
    return id!=null?id.toString():null;
  }
  
  /**
   * Set information to a map, contained in the current {@link #graph}.
   * @param node_or_edge any node or edge
   * @param descriptor descriptor of the map (e.g. "entrezIds")
   * @param value value to set.
   */
  public void setInfo(Object node_or_edge, String descriptor, Object value) {
    // Get the NodeMap from kegg 2 node.
    DataMap nodeMap = descriptor2Map.get(descriptor);
    if (nodeMap==null) {
      log.severe("Could not find Node2" + (descriptor==null?"null":descriptor) + " mapping.");
      return;
    }
    
    // return kegg id(s)
    nodeMap.set(node_or_edge, value);
  }
  
  
  /**
   * Returns the organism kegg abbreviation from a graph.
   * @param graph
   * @return e.g. "ko" or "hsa",...
   */
  public static String getOrganismKeggAbbrFromGraph(Graph2D graph) {
    
    for (Node n: graph.getNodeArray()) {
      String id = getKeggIDs(n).toLowerCase().trim();
      if (id.contains(":")) {
        return id.substring(0, id.indexOf(':'));
      }
    }
    
    return null;
  }

  /**
   * @param n
   * @return kegg ids, separated by a "," for the given node.
   */
  public static String getKeggIDs(Node n) {
    return getNodeInfoIDs(n, "keggIds");
  }

  /**
   * Returns the node, referring to a certain pathway in the graph.
   * Can be used to get the title node, by simply submitted the 
   * pathwayID of the current graph as argument.
   * @param graph
   * @param pathwayID
   * @return
   */
  public static Node getTitleNode(Graph2D graph, String pathwayID) {
    pathwayID = pathwayID.toLowerCase().trim();
    for (Node n: graph.getNodeArray()) {
      String id = getKeggIDs(n).toLowerCase().trim();
      if (id.startsWith("path:") && id.contains(pathwayID)) {
        return n;
      }
    }
    
    return null;
  }
  
}
