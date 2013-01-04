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
 * Copyright (C) 2011-2013 by the University of Tuebingen, Germany.
 *
 * KEGGtranslator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import y.base.Node;
import y.base.NodeCursor;
import y.base.NodeList;
import y.view.Graph2D;
import y.view.NodeRealizer;
import y.view.hierarchy.HierarchyManager;
import de.zbit.math.MathUtils;

/**
 * A Stacking layout (example for two columns:<br/>
 * X X<br/>
 * X X<br/>
 * X X<br/>
 * X X<br/>
 * [...])<br/>
 * Use this for group nodes only.
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
public class StackingNodeLayout {
  public static final transient Logger log = Logger.getLogger(StackingNodeLayout.class.getName());
  
  /**
   * Number of columns in this group Node that
   * are available to stack the children
   */
  private final static int cols = 2;
  
  /**
   * Define a distance to keep to sourounding nodes
   */
  private final static double inset=0.0;
  /**
   * Group node headers are extended to the TOP, not
   * to the bottom! by this constant.
   */
  private final double groupNodeHeaderHeight = 25;//23.2509766;
  
  /**
   * parent graph of given group node
   */
  private Graph2D graph;
  
  /**
   * If true, first layout other group nodes in this node,
   * then this node. This makes sense, because yFiles gives
   * wrong widths and heights for group nodes inside
   * group nodes.
   */
  boolean recursive=false;
  
  /**
   * The width of one column
   */
  private double averageNodeWidth=0;
  /**
   * The leftmost x-coordinate
   */
  private double firstX=0;
  
  /**
   * The topmost y-coordinate
   */
  private double firstY=0;
  
  /**
   * Childs of the group node to layout.
   */
  List<Node> childs=null;
  
  /**
   * @param graph parent graph
   * @param group group node to layout
   * @param recursive if true, first layout other group nodes in this node, then this node.
   */
  private StackingNodeLayout(Graph2D graph, Node group, boolean recursive) {
    this.graph = graph;
    this.recursive = recursive;
    prepareVariables(group);
    layoutGroupNode(group);
    log.fine("Performing stacking layout on " + group);
  }
  
  /**
   * Perform a simply stacking layout with {@link #cols} columns.
   * @param graph graph in which <code>groupNode</code> is contained
   * @param groupNode the group node whose childs should be stacked
   */
  public static void doLayout(Graph2D graph, Node groupNode) {
    new StackingNodeLayout(graph, groupNode, false);
  }
  
  /**
   * Perform a simply stacking layout with {@link #cols} columns.
   * Perform also a stacking layout for all group nodes in this
   * group node.
   * @param graph graph in which <code>groupNode</code> is contained
   * @param groupNode the group node whose childs should be stacked
   */
  public static void doRecursiveLayout(Graph2D graph, Node groupNode) {
    new StackingNodeLayout(graph, groupNode, true);
  }
  
  /**
   * Performs the stacking layout.<br/><i>
   * Requires: {@link #prepareVariables(Node)} called in advance.</i>
   * @param group node to layout
   */
  private void layoutGroupNode(Node group) {
    if (childs==null || group==null) return; // Trivial
    double cur_x = firstX;
    double cur_y = firstY;
    
    // Important: nodes in group nodes must come first and must be sorted
    // by hirarchy (group nodes they belong to)!
    double maxHeightInRow=0;
    List<Node> parents = new ArrayList<Node>();
    parents.add(group);
    for (Node child: childs) {
      // Evaluate parent. Do we have to shift to a new line?
      // Do we have to consider space for another group node header?
      Node parent = graph.getHierarchyManager().getParentNode(child);
      if (!parent.equals(parents.get(parents.size()-1))) {
        if (parents.size()>1) parents.remove(parents.size()-1);
        // NextRow, add Header.
        if (cur_x!=firstX) {
          cur_x=firstX;
          cur_y+=maxHeightInRow;
          maxHeightInRow = 0;//averageNodeHeight;
        }
        
        // Consider group node header and remember parent
        if (!parent.equals(parents.get(parents.size()-1))) {
          // we dived "deeper".
          cur_y+=groupNodeHeaderHeight;
          parents.add(parent);
        }
      }
      //----
      
      // Layout child
      NodeRealizer cr = graph.getRealizer(child);
      cr.setX(cur_x);
      cr.setY(cur_y);
      
      // Calculate how many cols the node took
      double w = getNodeWidth(cr);
      int slotsTaken = (int) Math.ceil(w/(averageNodeWidth+inset));
      
      // Increment X
      cur_x+=(slotsTaken*(averageNodeWidth+inset));
      
      // Test if we need to jump to next line
      maxHeightInRow = Math.max(maxHeightInRow, getNodeHeight(cr)+inset);
      int usedSlotsInCurrentRow = (int) Math.ceil( ((double)(cur_x-firstX)) / (averageNodeWidth+inset));
      if (usedSlotsInCurrentRow>=cols) {
        cur_x=firstX;
        cur_y+=maxHeightInRow;
        maxHeightInRow = 0;//averageNodeHeight;
      }
    }
  }
  
  /**
   * @param group
   * @return all children of the given <code>group</code> node.
   */
  @SuppressWarnings("unchecked")
  public List<Node> getChildren(Node group) {
    NodeCursor nc = graph.getHierarchyManager().getChildren(group);
    if (nc==null) {
      return null;
    } else {
      return new NodeList(nc);
    }
  }
  
  /**
   * Returns a list of all child nodes of this group node
   * and contained nodes.<br/>
   * The order of the returned list is fixed: First grouped nodes
   * than ungrouped nodes!
   * @param group
   * @return all children of the given <code>group</code> 
   * node and all group nodes in this group node.
   */
  public List<Node> getChildrenDeep(Node group) {
    // Put grouped nodes first in the set.
    List<Node> childsInGroups = new ArrayList<Node>();
    List<Node> childsSimple = new ArrayList<Node>();
    
    HierarchyManager hm = graph.getHierarchyManager();
    NodeCursor nc = hm.getChildren(group);
    NodeList nl = nc==null?null:new NodeList(nc);
    if (nl!=null) {
      for (Object c : nl) {
        Node current = (Node) c;
        if (hm.isGroupNode(current)) {
          childsInGroups.addAll(getChildrenDeep(current));
        } else {
          childsSimple.add(current);
        }
      }
    }
    
    List<Node> childs = new ArrayList<Node>();
    childs.addAll(childsInGroups);
    childs.addAll(childsSimple);
    
    return childs;
  }
  
  /**
   * Examines widths and heights of all childs, also
   * recurses and layout other group nodes if
   * {@link #recursive} is true.
   * @param group node to layout
   */
  private void prepareVariables(Node group) {
    if (group==null) return;
    
    // Get size of all children
    childs = recursive?getChildrenDeep(group):getChildren(group);
    List<Double> widths = new ArrayList<Double>();
    double minX = Double.MAX_VALUE;
    double minY = Double.MAX_VALUE;
    
    // Get width, height, x and y of every node.
    for (Node n : childs) {
      NodeRealizer nr = graph.getRealizer(n);
      
      widths.add(getNodeWidth(nr));
      minX = Math.min(nr.getX(), minX);
      minY = Math.min(nr.getY(), minY);
    }
    
    // averageNodeWidth is the width of one column.
    averageNodeWidth = MathUtils.median(widths);
    //averageNodeHeight = Math.min(averageNodeHeight, 30);
    //averageNodeWidth = Math.min(averageNodeWidth, 60);
    
    firstX = minX;
    firstY = minY;
  }

  /**
   * @param nr
   * @return node width
   */
  private double getNodeWidth(NodeRealizer nr) {
    double w = Math.max(nr.getWidth(), nr.getLabel().getWidth()-inset);
    return w;
  }
  
  /**
   * @param nr
   * @return node height
   */
  private double getNodeHeight(NodeRealizer nr) {
    double h = Math.max(nr.getHeight(), nr.getLabel().getHeight()-inset);
    return h;
  }
  
  
}
