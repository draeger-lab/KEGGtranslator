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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import y.base.Node;
import y.base.NodeCursor;
import y.view.Graph2D;
import y.view.NodeRealizer;
import y.view.hierarchy.GroupNodeRealizer;
import de.zbit.util.Utils;

/**
 * A Stacking layout (example for two columns:<br/>
 * X X<br/>
 * X X<br/>
 * X X<br/>
 * X X<br/>
 * [...])<br/>
 * Use this for group nodes only.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class StackingNodeLayout {
  private final static int cols = 2;
  private final static double inset=0.0;
  /**
   * Group node headers are extended to the TOP, not
   * to the bottom! by this constant.
   */
  private final double groupNodeHeaderHeight = 23.2509766;
  
  /**
   * parent graph
   */
  private Graph2D graph;
  
  /**
   * If true, first layout other group nodes in this node,
   * then this node. This makes sense, because yFiles gives
   * wrong widths and heights for group nodes inside
   * group nodes.
   */
  boolean recursive=false;
  
  /*
   * Various variables required for the layout
   */
  private double averageNodeWidth=0;
  private double averageNodeHeight=0;
  private double firstX=0;
  private double firstY=0;
  private double maxNodeWidth=0;
  
  private double cur_x = firstX;
  private double cur_y = firstY;
  
  /**
   * Childs of the group node to layout.
   */
  List<Node> childs=null;
  
  /**
   * If we perform a {@link #recursive} layout, yFiles fails to
   * give correct dimensions for other group nodes.
   * => We need to store them.
   */
  private Map<Node, double[]> storedDimensions = new HashMap<Node, double[]>();
  
  /**
   * @param graph parent graph
   * @param node group node to layout
   * @param recursive if true, first layout other group nodes in this node, then this node.
   */
  private StackingNodeLayout(Graph2D graph, Node node, boolean recursive) {
    this.graph = graph;
    this.recursive = recursive;
    layoutGroupNode(node);
  }
  
  /**
   * Perform a simply stacking layout with {@link #cols} columns.
   * @param graph
   * @param groupNode
   * @return width and height of the just layouted node. 
   */
  public static double[] doLayout(Graph2D graph, Node groupNode) {
    StackingNodeLayout stack = new StackingNodeLayout(graph, groupNode, false);
    return new double[]{ stack.getCurrentNodeWidth(), stack.getCurrentNodeHeight()};
  }
  
  /**
   * Perform a simply stacking layout with {@link #cols} columns.
   * Perform also a stacking layout for all group nodes in this
   * group node.
   * @param graph
   * @param groupNode
   * @return width and height of the just layouted node. 
   */
  public static double[] doRecursiveLayout(Graph2D graph, Node groupNode) {
    StackingNodeLayout stack = new StackingNodeLayout(graph, groupNode, true);
    return new double[]{ stack.getCurrentNodeWidth(), stack.getCurrentNodeHeight()};
  }
  
  /**
   * @return raw node height {@link #groupNodeHeaderHeight} should be
   * added to this value to also consider the label overhead.
   */
  private double getCurrentNodeHeight() {
    return (cur_y-firstY);
  }

  /**
   * @return
   */
  private double getCurrentNodeWidth() {
    return (maxNodeWidth)+4;
  }
  
  
  private void layoutGroupNode(Node group) {
    prepareVariables(group);
    cur_x = firstX;
    cur_y = firstY;
    
    
    /**
     * A simple comparator to sort nodes by their width, descending
     */
    Collections.sort(childs, new Comparator<Node>() {
      public int compare(Node o1, Node o2) {
        return (int) (getNodeWidth(graph.getRealizer(o2)) - getNodeWidth(graph.getRealizer(o1)));
      }
    });
    
    System.out.println("PROCESSING " + graph.getLabelText(group));
    double maxHeightInRow=0;
    for (Node child: childs) {
      boolean isGroupNode = graph.getHierarchyManager().isGroupNode(child);
      // Layout child
      NodeRealizer cr = graph.getRealizer(child);
      System.out.println("  PLACING " + cr.getLabelText() + " at " + (cur_x-firstX) + ", " + (cur_y-firstY) );
      cr.setX(cur_x);
      cr.setY(cur_y);
      
      // Calculate how many cols the node took
      double w = getNodeWidth(cr);
      int slotsTaken = (int) Math.ceil(w/(averageNodeWidth+inset));
      
      // Increment X
      cur_x+=(slotsTaken*(averageNodeWidth+inset));
      maxNodeWidth = Math.max(maxNodeWidth, (cur_x-firstX));
      
      // Test if we need to jump to next line
      maxHeightInRow = Math.max(maxHeightInRow, getNodeHeight(cr)+inset);
      int usedSlotsInCurrentRow = (int) Math.ceil( ((double)(cur_x-firstX)) / (averageNodeWidth+inset));
      if (usedSlotsInCurrentRow>=cols) {
        cur_x=firstX;
        cur_y+=maxHeightInRow;
        maxHeightInRow = 0;//averageNodeHeight;
      }
    }
    
    // Update for getCurrentHeight().
    cur_y+=maxHeightInRow;
    
    // Re-calculate bounds
    NodeRealizer nr = graph.getRealizer(group);
    if (nr instanceof GroupNodeRealizer && ((GroupNodeRealizer)nr).isAutoBoundsEnabled()) {
      ((GroupNodeRealizer)nr).updateAutoSizeBounds();
    }
  }
  
  /**
   * @param group
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
  
  /**
   * Examines widths and heights of all childs, also
   * recurses and layout other group nodes if
   * {@link #recursive} is true.
   * @param group
   */
  private void prepareVariables(Node group) {
    if (group==null) return;
    
    // Get size of all children
    childs = new ArrayList<Node>(getChildren(group));
    List<Double> widths = new ArrayList<Double>();
    List<Double> heights = new ArrayList<Double>();
    double minX = Double.MAX_VALUE;
    double minY = Double.MAX_VALUE;
    
    //System.out.println("Group " + graph.getLabelText(group) + " has children:");
    for (Node n : childs) {
      //System.out.print("  " + graph.getLabelText(n));
      boolean isGroupNode = graph.getHierarchyManager().isGroupNode(n);
      if (isGroupNode) continue; // TODO: deep go into
      
      // Perform a recursive layout and get bounds of other
      // group nodes.
//      if (recursive && isGroupNode) {
//        doRecursiveLayout(graph, n);
//      }
      
      // Update node size
      NodeRealizer nr = graph.getRealizer(n);
//      if (nr instanceof GroupNodeRealizer && ((GroupNodeRealizer)nr).isAutoBoundsEnabled()) {
//        ((GroupNodeRealizer)nr).updateAutoSizeBounds();
//      }
      
      // Get width, height, x and y.
      double w = getNodeWidth(nr);
      double h = getNodeHeight(nr);
      
      widths.add(w);
      heights.add(h);
      maxNodeWidth = Math.max(maxNodeWidth, w);
      minX = Math.min(nr.getX(), minX);
      minY = Math.min(nr.getY(), minY);
      //System.out.println("  " + w + " :: " +  h );
    }
    
    // Set global
    averageNodeHeight = Utils.median(heights);
    averageNodeWidth = Utils.median(widths);
    averageNodeHeight = Math.min(averageNodeHeight, 30);
    averageNodeWidth = Math.min(averageNodeWidth, 60);
    
    firstX = minX;
    firstY = minY;
  }

  
  private double getNodeWidth(NodeRealizer nr) {
    double w = Math.max(nr.getWidth(), nr.getLabel().getWidth()-inset);
    return w;
  }
  
  private double getNodeHeight(NodeRealizer nr) {
    double h = Math.max(nr.getHeight(), nr.getLabel().getHeight()-inset);
    return h;
  }
  
  
  
  private void recursiveLayoutGroupNode(Node group) {
    // TODO: Prepare befor this
    prepareVariables(group);
    cur_x = firstX;
    cur_y = firstY;
    
    
    /**
     * A simple comparator to sort nodes by their width, descending
     */
    Collections.sort(childs, new Comparator<Node>() {
      public int compare(Node o1, Node o2) {
        return (int) (getNodeWidth(graph.getRealizer(o2)) - getNodeWidth(graph.getRealizer(o1)));
      }
    });
    
    double maxHeightInRow=0;
    for (Node child: childs) {
      boolean isGroupNode = graph.getHierarchyManager().isGroupNode(child);
      
      // Layout child
      NodeRealizer cr = graph.getRealizer(child);
      System.out.println("  PLACING " + cr.getLabelText() + " at " + (cur_x-firstX) + ", " + (cur_y-firstY) );
      cr.setX(cur_x);
      cr.setY(cur_y);
      
      // Calculate how many cols the node took
      double w = getNodeWidth(cr);
      int slotsTaken = (int) Math.ceil(w/(averageNodeWidth+inset));
      
      // Increment X
      cur_x+=(slotsTaken*(averageNodeWidth+inset));
      maxNodeWidth = Math.max(maxNodeWidth, (cur_x-firstX));
      
      // Test if we need to jump to next line
      maxHeightInRow = Math.max(maxHeightInRow, getNodeHeight(cr)+inset);
      int usedSlotsInCurrentRow = (int) Math.ceil( ((double)(cur_x-firstX)) / (averageNodeWidth+inset));
      if (usedSlotsInCurrentRow>=cols) {
        cur_x=firstX;
        cur_y+=maxHeightInRow;
        maxHeightInRow = 0;//averageNodeHeight;
      }
    }
    
    // Update for getCurrentHeight().
    cur_y+=maxHeightInRow;
    
    // Re-calculate bounds
    NodeRealizer nr = graph.getRealizer(group);
    if (nr instanceof GroupNodeRealizer && ((GroupNodeRealizer)nr).isAutoBoundsEnabled()) {
      ((GroupNodeRealizer)nr).updateAutoSizeBounds();
    }
  }
  
  
}
