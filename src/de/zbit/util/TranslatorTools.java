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
package de.zbit.util;

import java.awt.Color;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import y.base.DataMap;
import y.base.Edge;
import y.base.EdgeMap;
import y.base.Node;
import y.base.NodeCursor;
import y.base.NodeMap;
import y.layout.labeling.SALabeling;
import y.view.Graph2D;
import y.view.LineType;
import y.view.NodeLabel;
import y.view.NodeRealizer;
import y.view.hierarchy.HierarchyManager;
import de.zbit.graph.GraphTools;
import de.zbit.graph.StackingNodeLayout;
import de.zbit.graph.gui.TranslatorPanel;
import de.zbit.graph.io.Graph2Dwriter;
import de.zbit.graph.io.def.GenericDataMap;
import de.zbit.graph.io.def.GraphMLmaps;
import de.zbit.kegg.io.KEGG2yGraph;
import de.zbit.kegg.parser.pathway.EntryType;

/**
 * This class is intended to provide various translator tools.
 * 
 * <p><i>Note:<br/>
 * Due to yFiles license requirements, we have to obfuscate this class
 * in the JAR release of this application. Thus, this class
 * can not be found by using the class name.<br/> If you can provide us
 * with a proof of possessing a yFiles license yourself, we can send you
 * an unobfuscated release of KEGGtranslator.</i></p>
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class TranslatorTools extends GraphTools {
  public static final transient Logger log = Logger.getLogger(TranslatorTools.class.getName());
  
  /**
   * Static String for the <code>type</code> {@link NodeMap} of the {@link #graph}
   * to be used for, e.g. microRNAs.
   */
  public final static String RNA_TYPE = "RNA";
  
  
  public TranslatorTools(TranslatorPanel<Graph2D> tp){
    this(tp.getDocument());
  }
  
  public TranslatorTools(Graph2D graph){
    super(graph);
  }
  
  
  /**
   * Highlight all given GeneIDs in YELLOW color. And selects these nodes.
   * @param graph translated pathway with annotated geneIDs
   * @param ncbiGeneIDs geneIDs to color in YELLOW.
   */
  public void highlightGenes(Iterable<Integer> ncbiGeneIDs) {
    highlightGenes(ncbiGeneIDs, Color.YELLOW, Color.LIGHT_GRAY, true);
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
      String id = getKeggIDs(n);
      id = id==null?null:id.toLowerCase().trim();
      if (id!=null && id.startsWith("path:")) continue;
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
    // TODO: instead of globally resetting Border color and thickness,
    // remember highlighted nodes in graph and remove highlighting
    // prior to a new search.
    // Also try to somehow remember old color and thickness and restore
    // bzw. set thickness to math.max([2], currentThickness).
    containedString = containedString.toLowerCase();
    graph.unselectAll();
    for (Node n: graph.getNodeArray()) {
      Color color = Color.BLACK;
      LineType lt = LineType.LINE_1;
      // Check actual label and "all names" (label map).
      String infos = getNodeInfoIDs(n, GraphMLmaps.NODE_LABEL); // may be null
      if (graph.getLabelText(n).toLowerCase().contains(containedString) ||
          (infos!=null && infos.toLowerCase().contains(containedString))) {
        color = Color.RED;
        lt = LineType.LINE_2;
        graph.setSelected(n, true);
      }
      graph.getRealizer(n).setLineColor(color);
      graph.getRealizer(n).setLineType(lt);
      graph.getRealizer(n).getLabel().setTextColor(color);
    }
  }
  
  
  /**
   * Get all descriptors of all maps registered
   * in the graph.
   * @return
   */
  public Set<String> getMapDescriptors() {
    return descriptor2Map.keySet();
  }
  
  /**
   * Get a reverse map for the <code>descriptor</code>. This means,
   * descriptor object to List of nodes with this descriptor.
   * <p>Key is an Object, Value is a <pre>List<Node></pre>
   * @param descriptor
   * @return
   */
  public Map<Object, List<Node>> getReverseMap(String descriptor) {
    DataMap dm = getMap(descriptor);
    if (dm==null) return null;
    
    Map<Object, List<Node>> revMap = new HashMap<Object, List<Node>>();
    Node[] arr = graph.getNodeArray();
    for (Node n: arr) {
      Object o = dm.get(n);
      if (o==null) continue;
      List<Node> list = revMap.get(o);
      if (list==null) {
        list = new LinkedList<Node>();
        revMap.put(o, list);
      }
      list.add(n);
    }
    
    return revMap;
  }
  
  /**
   * @return a map from nodeLabel (microRNA_name Uppercased and trimmed) to the actual node.
   */
  @Deprecated
  public Map<String, List<Node>> getRNA2NodeMap() {

    // Build a map from GeneID 2 Node
    Map<String, List<Node>> mi2node = new HashMap<String, List<Node>>();

    NodeMap typeMap = (NodeMap) getMap(GraphMLmaps.NODE_TYPE);
    NodeMap labelMap = (NodeMap) getMap(GraphMLmaps.NODE_LABEL);
    if (typeMap==null || labelMap==null) {
      log.severe(String.format("Could not find %s %s mapping.", (typeMap==null?"type":""), (labelMap==null?"label":"")));
      return mi2node; // return an empty map.
    }
    
    // build the resulting map
    for (Node n : graph.getNodeArray()) {
      Object type = typeMap.get(n);
      if (type!=null && type.equals(RNA_TYPE)) {
        Object label = labelMap.get(n);
        if (label==null) continue;
        String key = label.toString().toUpperCase().trim();
        
        // Get list, associated with node label
        List<Node> list = mi2node.get(key);
        if (list==null) {
          list = new LinkedList<Node>();
          mi2node.put(key, list);
        }
        
        // Add node to list.
        list.add(n);
        
      }
    }
    
    return mi2node;
  }
  
  /**
   * @return true if and only if the underlying graph contains any nodes
   * of type {@link #RNA_TYPE}.
   */
  public boolean containsRNAnodes() {

    // Get Type map
    NodeMap typeMap = (NodeMap) getMap(GraphMLmaps.NODE_TYPE);
    if (typeMap==null) {
      log.severe(String.format("Could not find %s mapping.", (typeMap==null?"type":"")));
      return false;
    }
    
    // Check all nodes
    for (Node n : graph.getNodeArray()) {
      Object type = typeMap.get(n);
      if (type!=null && type.equals(RNA_TYPE)) {
        return true;
      }
    }
    
    return false;
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
//    GenericDataMap<DataMap, String> mapDescriptionMap = (GenericDataMap<DataMap, String>) graph.getDataProvider(Graph2Dwriter.mapDescription);
    NodeMap entrez = (NodeMap) descriptor2Map.get(GraphMLmaps.NODE_GENE_ID); //null;
//    if (mapDescriptionMap==null) return null;
//    for (int i=0; i<graph.getRegisteredNodeMaps().length; i++) {
//      NodeMap nm = graph.getRegisteredNodeMaps()[i];
//      if (mapDescriptionMap.getV(nm).equals(GraphMLmaps.NODE_GENE_ID)) {
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
   * Ensures that a specific map exists in this graph. Simply returns
   * the map, if it already existed, else, the map will be created as
   * an empty map.
   * @param descriptor
   * @param isNodeMap false, if and only if this should be an
   * {@link EdgeMap}. If true, a {@link NodeMap} will be created.
   * @return the map.
   */
  public DataMap ensureMapExists(String descriptor, boolean isNodeMap) {
    // Get the NodeMap for the descriptor.
    DataMap nodeMap = getMap(descriptor);
    
    if (nodeMap==null) {
      // Create non-existing map automatically
      nodeMap = createMap(descriptor, isNodeMap );
      log.finer(String.format("Created not existing Node to %s mapping.", (descriptor==null?"null":descriptor)));
    }
    return nodeMap;
  }
  

  /**
   * Returns the organism kegg abbreviation from a graph.
   * @param graph
   * @return e.g. "ko" or "hsa",...
   */
  public static String getOrganismKeggAbbrFromGraph(Graph2D graph) {
    if (graph==null) return null;
    
    for (Node n: graph.getNodeArray()) {
      String id = getKeggIDs(n);
      if (id==null) continue;
      id = id.toLowerCase().trim();
      if (id.contains(":")) {
        String kga = id.substring(0, id.indexOf(':'));
        if (!(kga.equals("cpd") || kga.equals("map") || kga.equals("path"))) {
          return kga;
        }
      }
    }
    
    return null;
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
      String id = getKeggIDs(n);
      if (id==null) continue;
      id = id.toLowerCase().trim();
      if (id.startsWith("path:") && id.contains(pathwayID)) {
        return n;
      }
    }
    
    return null;
  }

  /**
   * 
   * @param n any {@link Node}
   * @return {@code true} if it is the title, or any
   * pathway reference node. If not (or in doubt),
   * {@code false} is returned.
   */
  public static boolean isPathwayReference(Node n) {
    String id = getKeggIDs(n);
    if (id==null) return false; // in doubt...
    id = id.toLowerCase().trim();
    return (id.startsWith("path:"));
  }

  /**
   * @return the currently underlying {@link Graph2D} of this tools instance.
   */
  public Graph2D getGraph() {
    return graph;
  }
  
  
  public void layoutGroupNode(Node group) {
    if (group==null) return;
    StackingNodeLayout.doRecursiveLayout(graph, group);
  }
  
  /**
   * @param group must be a group node
   * @return all children of the given group node.
   */
  public Set<Node> getChildren(Node group) {
    Set<Node> childs = new HashSet<Node>();
    NodeCursor nc = graph.getHierarchyManager().getChildren(group);
    for (int i=1; i<=nc.size(); i++) {
      childs.add(nc.node());
      nc.next();
    }
    return childs;
  }
  
  public void layoutChildsAndGroupNodes(Collection<Node> nodes) {
    if (nodes==null) return;

    // Layout all group nodes.
    HierarchyManager hm = graph.getHierarchyManager();
    Set<Node> alreadyLayouted = new HashSet<Node>();
    for (Node n: nodes) {
      Node p = hm.getParentNode(n);
      if (!hm.isGroupNode(n) && p==null) {
        alreadyLayouted.add(n); // Layout them, too
        continue; 
      }
      
      // Get root node
      while ((p = hm.getParentNode(n)) !=null) {
        n=p;
      }
      
      // Layout recursive and add to set
      if (!alreadyLayouted.contains(n)) {
        StackingNodeLayout.doRecursiveLayout(graph, n);
        alreadyLayouted.add(n);
      }
    }
    
    // Dimension of groups may have changed. layout them.
    layoutNodeSubset(alreadyLayouted, false);
  }
  

  /**
   * Reset the color and label of the node to the parameters
   * stored in the map during translation of the pathway.
   * @param n
   */
  public void resetColorAndLabel(Node n) {
    
    // Reset Color
    Object HTMLcolor = getInfo(n, GraphMLmaps.NODE_COLOR);
    if (HTMLcolor!=null) {
      try {
        Color c = KEGG2yGraph.ColorFromHTML(HTMLcolor.toString());
        graph.getRealizer(n).setFillColor(c);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    
    // Reset name
    Object name = getInfo(n, GraphMLmaps.NODE_NAME);
    if (name!=null) {
      graph.getRealizer(n).setLabelText(name.toString());
    }
    
  }

  /**
   * Resets the node width and height to the values given
   * in the original KGML.
   * @param node
   */
  public void resetWidthAndHeight(Node node) {
    // TODO: Also reset shape
    DataMap nodeMap = descriptor2Map.get(GraphMLmaps.NODE_SIZE);
    if (nodeMap==null) {
      log.severe("Could not find original node sizes.");
      return;
    }
    
    String splitBy = Pattern.quote("|");
    //for (Node node: graph.getNodeArray()) {
    Object pos = nodeMap.get(node);
    if (pos==null) return;//continue;
    // pos is always Width|Height
    String[] WH = pos.toString().split(splitBy);
    graph.getRealizer(node).setWidth(Integer.parseInt(WH[0]));
    graph.getRealizer(node).setHeight(Integer.parseInt(WH[1]));
    //}
  }
  
  /**
   * Removes a data provider from the graph.
   * @param map
   */
  @SuppressWarnings("unchecked")
  public void removeMap(DataMap map) {
    
    // Also remove from my internal map.
    Iterator<Entry<String, DataMap>> it = descriptor2Map.entrySet().iterator();
    while (it.hasNext()) {
      if (it.next().getValue().equals(map)) {
        it.remove();
        break;
      }
    }
    
    // Remove from description map
    GenericDataMap<DataMap, String> mapDescriptionMap = (GenericDataMap<DataMap, String>) graph.getDataProvider(Graph2Dwriter.mapDescription);
    mapDescriptionMap.removeMapByKey(map, graph);
    
    // Remove from graph
    try {
      if (map instanceof NodeMap) {
        graph.disposeNodeMap((NodeMap) map);
      } else if (map instanceof EdgeMap) {
        graph.disposeEdgeMap((EdgeMap) map);
      }
    } catch (IllegalStateException e) {
      //  Map has been already disposed !
      log.log(Level.FINE, "Could not dispose map.", e);
    }
    
  }
  
  /**
   * Removes a data provider from the graph.
   * @param identifier
   */
  @SuppressWarnings("unchecked")
  public void removeMap(String identifier) {
    
    // Also remove from my internal map.
    DataMap item = descriptor2Map.remove(identifier);
    
    // Remove from description map
    GenericDataMap<DataMap, String> mapDescriptionMap = (GenericDataMap<DataMap, String>) graph.getDataProvider(Graph2Dwriter.mapDescription);
    mapDescriptionMap.removeMap(identifier, graph);
    
    // Remove from graph
    if (item!=null) {
      try {
        if (item instanceof NodeMap) {
          graph.disposeNodeMap((NodeMap) item);
        } else if (item instanceof EdgeMap) {
          graph.disposeEdgeMap((EdgeMap) item);
        }
      } catch (IllegalStateException e) {
        //  Map has been already disposed !
        Level l = Level.FINE;
        if (e.getMessage()!=null && e.getMessage().contains("Map has been already disposed")) {
          l = Level.FINEST;
        }
        log.log(l, String.format("Could not dispose map '%s'.",identifier), e);
      }
    }
    
  }

  /**
   * Gets information from a map and returns true if it is not null
   * (i.e. it is set) and if the content equals {@link Boolean#TRUE}.
   * @param node_or_edge and {@link Node} or {@link Edge}
   * @param descriptor a descriptor for a {@link NodeMap} or {@link EdgeMap} in the current
   * {@link #graph}.
   * @return true if {@link #getInfo(Object, String)} !=null and
   * equals {@code true}.
   */
  public boolean getBoolInfo(Object node_or_edge, String descriptor) {
    Object o = getInfo(node_or_edge, descriptor);
    
    if (o!=null) {
      if (o instanceof Boolean) return (Boolean)o;
      try {
         return Boolean.valueOf(o.toString());
      } catch (Throwable t) {}
    }
    return false;
  }

  /**
   * Calculates a simple stacked layout with 2 columns.
   * @param cr realizer of the node
   * @param gr realizer of the parent group node
   * @param nodesInGroup index of node in stacking group, starting with 1
   */
  public static void setStackingChildNodePosition(NodeRealizer cr, NodeRealizer gr, int nodesInGroup) {
    int cols = 2;
    double inset=5.0;
    
    // consider node with and label width to determine x layout.
    double w = Math.max(cr.getWidth(), cr.getLabel().getWidth()-inset);
    
    double x = ((nodesInGroup-1) %cols); // column
    x = (x*(w+inset)) + gr.getX(); // + cr.getX()
    double y = ((nodesInGroup-1) /cols); // row
    y = (y*(cr.getHeight()+inset)) + gr.getY();  // + cr.getY()
    
    log.fine("Set stacking coords of " + cr.getNode() + " to " + x + "|" + y);
    cr.setX(x);
    cr.setY(y);
  }
  
  /**
   * This method automatically places all node labels.
   */
  public void layoutLabels() {
    // Code for automatic node-label placement!
    SALabeling labeling = new SALabeling();
    labeling.setPlaceNodeLabels(true);
    labeling.setPlaceEdgeLabels(false);
    labeling.label( graph );
    //g.updateViews();
  }

  /**
   * Calculate the number of labels for a node.
   * @param n {@link Node}
   * @param model any value smaller than 0, to count all.
   * Else: allows to filter the labels to count for a
   * certain model property (e.g., {@link NodeLabel#SIDES}).
   * @return number of labels.
   */
  public int getNumberOfLabels(Node n, byte model) {
    NodeRealizer nr = graph.getRealizer(n);
    if (model<0) return nr.labelCount();
    else {
      int count = 0;
      for (int i=0; i<nr.labelCount(); i++) {
        if (nr.getLabel(i).getModel()==model) {
          count++;
        }
      }
      return count;
    }
  }

  /**
   * Create a map that maps from pathway ids (e.g. "path:mmu00910") to
   * nodes referencing to this pathway (grey, pathway-reference nodes,
   * used for cross-linking pathways).
   * @return Map from pathway id (LOWERCASED! e.g. path:mmu00910) to node.
   */
  public Map<String, List<Node>> getPathwayReferenceNodeMap() {

    // Build a map from PWID 2 Node
    Map<String, List<Node>> pw2node = new HashMap<String, List<Node>>();

    NodeMap typeMap = (NodeMap) getMap(GraphMLmaps.NODE_TYPE);
    NodeMap labelMap = (NodeMap) getMap(GraphMLmaps.NODE_KEGG_ID);
    if (typeMap==null || labelMap==null) {
      log.severe(String.format("Could not find %s %s mapping.", (typeMap==null?"type":""), (labelMap==null?"label":"")));
      return pw2node; // return an empty map.
    }
    
    // build the resulting map
    for (Node n : graph.getNodeArray()) {
      Object type = typeMap.get(n);
      if (type!=null && (type.toString().equals(EntryType.map.toString()) ||
          type.toString().equalsIgnoreCase("pathway"))) {
        Object label = labelMap.get(n);
        if (label==null) continue;
        String key = label.toString().toLowerCase().trim();
        
        // Get list, associated with node label
        List<Node> list = pw2node.get(key);
        if (list==null) {
          list = new LinkedList<Node>();
          pw2node.put(key, list);
        }
        
        // Add node to list.
        list.add(n);
        
      }
    }
    
    return pw2node;
  }
  
  
}
