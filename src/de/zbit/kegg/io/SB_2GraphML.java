///*
// * $Id$
// * $URL$
// * ---------------------------------------------------------------------
// * This file is part of KEGGtranslator, a program to convert KGML files
// * from the KEGG database into various other formats, e.g., SBML, GML,
// * GraphML, and many more. Please visit the project homepage at
// * <http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator> to
// * obtain the latest version of KEGGtranslator.
// *
// * Copyright (C) 2010-2012 by the University of Tuebingen, Germany.
// *
// * KEGGtranslator is free software; you can redistribute it and/or 
// * modify it under the terms of the GNU Lesser General Public License
// * as published by the Free Software Foundation. A copy of the license
// * agreement is provided in the file named "LICENSE.txt" included with
// * this software distribution and also available online as
// * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
// * ---------------------------------------------------------------------
// */
//package de.zbit.kegg.io;
//
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.Set;
//
//import y.base.DataMap;
//import y.base.Edge;
//import y.base.Node;
//import y.base.NodeMap;
//import y.layout.organic.SmartOrganicLayouter;
//import y.view.Graph2D;
//import y.view.NodeRealizer;
//import de.zbit.graph.ReactionNodeRealizer;
//import de.zbit.kegg.ext.GenericDataMap;
//import de.zbit.kegg.ext.GraphMLmaps;
//import de.zbit.kegg.ext.SBGNVisualizationProperties;
//import de.zbit.kegg.io.KEGG2yGraph;
//import de.zbit.util.TranslatorTools;
//
///**
// * This is an abstract superclass for various systems biology formats to create
// * yFiles graph structure. All methods that create yFiles graph structures from
// * Systems Bilogy formats (such as SBML, SBGN, etc.) should extend this class!
// * 
// * <p>This generic superclass should NOT use ANY SBML or SBGN, etc. classes.
// * Only generic java classes and yFiles should be imported.
// * @author Clemens Wrzodek
// * @version $Rev$
// */
//public abstract class SB_2GraphML <T> {
//  
//  /**
//   * Use this hashmap to map every graph-object
//   * to an SBML-identifier.
//   */
//  protected Map<Object, String> GraphElement2SBid = new HashMap<Object, String>();
//  
//  /**
//   * This maps every identifier of any SB-object (e.g. a species) to the corresponding
//   * graph node.
//   * The reverse of {@link #GraphElement2SBid}.
//   */
//  protected Map<String, Node> id2node = new HashMap<String, Node>();
//  
//  /**
//   * A map, containing "x|y" coordinates of all nodes.
//   */
//  protected NodeMap nodePosition;
//  
//  /**
//   * The translated graph
//   */
//  protected Graph2D simpleGraph;
//  
//  /**
//   * This set should contain all nodes that need to be layouted
//   * (i.e. the source format had not specific coordinates).
//   */
//  protected Set<Node> unlayoutedNodes;
//  
//  
//  /**
//   * By default, the number of columns to create when no layout information
//   * is available.
//   */
//  protected static int COLUMNS = 5;
//  
//  /**
//   * Clone enzymes in a way that exclusively one enzyme copy is
//   * available for each reaction.
//   * <p>Later, we may create an option for that...
//   */
//  protected boolean splitEnzymesToOnlyOccurOnceInAnyReaction = true;
//  
//  
//  /**
//   * @return
//   */
//  public boolean isSplitEnzymesToOnlyOccurOnceInAnyReaction() {
//    return splitEnzymesToOnlyOccurOnceInAnyReaction;
//  }
//
//
//  /**
//   * @param <code>TRUE</code> if every enzyme should be splitted for
//   * every reaction.
//   */
//  public void setSplitEnzymesToOnlyOccurOnceInAnyReaction(
//    boolean splitEnzymesToOnlyOccurOnceInAnyReaction) {
//    this.splitEnzymesToOnlyOccurOnceInAnyReaction = splitEnzymesToOnlyOccurOnceInAnyReaction;
//  }
//
//
//  /**
//   * Returns a map from every graph element ({@link Node} or
//   * {@link Edge}) to the corresponding ID of the SB-document.
//   * @return a map from graph object to sb id
//   */
//  public Map<Object, String> getGraphElement2SBid() {
//    return GraphElement2SBid;
//  }
//
//
//  /**
//   * Returns a map from every ID of the SB-document to the corresponding graph
//   * element ({@link Node}.
//   * 
//   * @return a map from SB-ID to {@link Node}
//   */
//  public Map<String, Node> getId2node() {
//    return id2node;
//  }
//
//
//  /**
//   * Returns the last result of the last call to {@link #createGraph(Object)}.
//   * @return Graph
//   */
//  public Graph2D getSimpleGraph() {
//    return simpleGraph;
//  }
//
//  /**
//   * Creates a new {@link Graph2D} instance of {@link #simpleGraph}.
//   * This should be called by all extending classes to initialize
//   * the graph.
//   */
//  protected void crateNewGraph() {
//    simpleGraph = new Graph2D();
//    
//    // Add some standardized maps, required by some utility methods
//    nodePosition = simpleGraph.createNodeMap();
//    GenericDataMap<DataMap, String> mapDescriptionMap = KEGG2yGraph.addMapDescriptionMapToGraph(simpleGraph);
//    mapDescriptionMap.set(nodePosition, GraphMLmaps.NODE_POSITION);
//    
//    // Convert each species to a graph node
//    unlayoutedNodes = new HashSet<Node>();
//  }
//
//
//  public Graph2D createGraph(T document) {
//    // Reset all variables and create the graph instance
//    crateNewGraph();
//    
//    // Create the real graph objects
//    createNodesAndEdges(document);
//    
//
//    // Apply a layouting algorithm to unlayouted nodes
//    if (unlayoutedNodes.size()>0) {
//      TranslatorTools tools = new TranslatorTools(simpleGraph);
//      if (isAnyLayoutInformationAvailable()) {
//        // Only layout nodes, that had no coords in the layout extension
//        // XXX: Would be nicer if we could somehow layout the subset with OrthogonalLayouter
//        tools.layoutNodeSubset(unlayoutedNodes, true);
//      } else {
//        // Layout the whole graph if no layoutExtension is available at all.
//        tools.layout(SmartOrganicLayouter.class);
//      }
//      simpleGraph.unselectAll();
//    }
//        
//    // Fix ReactionNode nodes (determines 90° rotatable node orientation)
//    /* TODO: These reaction nodes are not nice and
//     * require still massive improvements!
//     */
//    improveReactionNodeLayout();
//    
//    return simpleGraph;
//  }
//  
//  /**
//   * Please implement this method that should perform the main part of the
//   * conversion. The graph (and all other variable) are already setup. Use
//   * the {@link #createNode(String, String, int)} methods to create nodes
//   * and refer to objects (in either direction) using the maps in this class.
//   */
//  protected abstract void createNodesAndEdges(T document);
//  
//  /**
//   * You may implement this method to perform a post-processing
//   * on the complete graph, i.e., to enhance the reaction node layout.
//   */
//  protected void improveReactionNodeLayout() {
//    // OPTIONALLY
//  }
//
//  /**
//   * Shoudl return <code>TRUE</code> if and only if any layout information
//   * of at least one node was available, during the translation (should one
//   * be called AFTER a document is translated).
//   * @return
//   */
//  protected abstract boolean isAnyLayoutInformationAvailable();
//
//  /**
//   * 
//   * @param id
//   * @param label
//   * @param sboTerm
//   * @return
//   */
//  protected Node createNode(String id, String label, int sboTerm) {
//    return createNode(id, label, sboTerm, Double.NaN, Double.NaN);
//  }
//  protected Node createNode(String id, String label, int sboTerm, double x, double y) {
//    return createNode(id, label, sboTerm, x, y, 46d, 17d);
//  }
//  protected Node createNode(String id, String label, int sboTerm, double x, double y, double width, double height) {
//    boolean nodeHadLayoutInformation = false;
//    boolean nodeShouldBeACircle = false;
//    
//    // Create node and put it in local maps
//    Node n = simpleGraph.createNode();
//    id2node.put(id, n);
//    GraphElement2SBid.put(n, id);
//    
//    // Set Node shape (and color) based on SBO-terms
//    NodeRealizer nr;
//    if (sboTerm<=0) {
//      nr = simpleGraph.getRealizer(n);
//    } else {
//      
//      nr = SBGNVisualizationProperties.getNodeRealizer(sboTerm);
//      nr = nr.createCopy(); // TODO: does this also copy pre-defined labels? (it should!)
//      simpleGraph.setRealizer(n, nr);
//      nodeShouldBeACircle = SBGNVisualizationProperties.isCircleShape(sboTerm);
//    }
//    
//    // Setup node properties
//    if (label!=null && !(nr instanceof ReactionNodeRealizer)) {
//      nr.setLabelText(label);
//    }
//    
//    // Auto-set missing coordinates
//    if (Double.isNaN(x) || Double.isNaN(y)) {
//      int nodesWithoutCoordinates = unlayoutedNodes.size();
//      // Make a simple grid-layout to set some initial coords
//      x = (nodesWithoutCoordinates%COLUMNS)*(width+width/2);
//      y = (nodesWithoutCoordinates/COLUMNS)*(height+height);
//      
//      nodesWithoutCoordinates++;
//      unlayoutedNodes.add(n);
//      nodeHadLayoutInformation = false;
//    } else {
//      nodeHadLayoutInformation = true;
//    }
//    
//    // Set coordinates
//    nr.setCenterX(x);
//    nr.setCenterY(y);
//    nr.setWidth(width);
//    nr.setHeight(height);
//    
//    if (nodeShouldBeACircle) {
//      // Make a square (w=h)
//      double min;
//      if (unlayoutedNodes.contains(n)) {
//        min = 8; // KEGG compounds always have w and h of 8 by default.  
//      } else {
//        min = Math.min(width, height);
//      }
//      
//      nr.setWidth(min);
//      nr.setHeight(min);
//    }
//    
//    // Eventually Remember in defined hashmap
//    if (nodeHadLayoutInformation) {
//      nodePosition.set(n, (int) nr.getX() + "|" + (int) nr.getY());
//    }
//    
//    return n;
//  }
//}
