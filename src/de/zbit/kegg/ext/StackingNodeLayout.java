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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import y.base.Node;
import y.view.Graph2D;
import y.view.NodeRealizer;
import de.zbit.kegg.TranslatorTools;
import de.zbit.util.Utils;

/**
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class StackingNodeLayout {
  private final static int cols = 2;
  private final static double inset=5.0;
  
  /**
   * parent graph
   */
  private Graph2D graph;
  
  /*
   * Various variables required for the layout
   */
  private double averageNodeWidth=0;
  private double averageNodeHeight=0;
  private double firstX=0;
  private double firstY=0;
  
  /**
   * Childs of the group node to layout.
   */
  List<Node> childs=null;
  
  private StackingNodeLayout(Graph2D graph, Node node) {
    this.graph = graph;
    prepareVariables(node);
  }
  
  
  /*
   * TODO:
   * - Make simpler method with currentX and Y.
   * - Sort nodes by Width
   * add one by another
   * 
   */
  
  
  public void layoutGroupNode() {
    double x = firstX;
    double y = firstY;
    double maxHeightInRow=averageNodeHeight;
    
    
    /**
     * A simple comparator to sort nodes by their width
     */
    Collections.sort(childs, new Comparator<Node>() {
      public int compare(Node o1, Node o2) {
        return (int) (getNodeWidth(graph.getRealizer(o1)) - getNodeWidth(graph.getRealizer(o2)));
      }
    });
    
    
    for (Node child: childs) {
      // Layout child
      NodeRealizer cr = graph.getRealizer(child);
      cr.setX(x);
      cr.setY(y);
      
      
      // Calculate how many cols the node took
      double w = getNodeWidth(cr);
      int slotsTaken = (int) Math.ceil(w/averageNodeWidth+inset);
      
      // Increment X
      x+=(slotsTaken*(averageNodeWidth+inset));
      
      // Test if we need to jump to next line
      maxHeightInRow = Math.max(maxHeightInRow, cr.getHeight());
      int slotsInCurrentRow = (int) Math.ceil( ((double)(x-firstX)) / (averageNodeWidth+inset));
      if (slotsInCurrentRow>cols) {
        x=firstX;
        y+=Math.max(averageNodeHeight, maxHeightInRow);
        maxHeightInRow = averageNodeHeight;
      }
    }
    
    
    
//    // for every col a pointer to initial x and y
//    double[] x = new double[cols];
//    for (int i=0;i<x.length; i++) {
//      x[i] = firstX + (i*(averageNodeWidth+inset));
//    }
//    
//    double[] y = new double[cols];
//    Arrays.fill(y, firstY);
//    
//    // Put childs in slots
//    for (Node child: childs) {
//      // Get slot and put child there
//      int slot = getNextFreeSlot(x, y);
//      NodeRealizer cr = graph.getRealizer(child);
//      cr.setX(x[slot]);
//      cr.setY(y[slot]);
//      
//      // Update slot coordinates
//      y[slot]+=Math.max(averageNodeHeight, cr.getHeight());
//      
//      double w = getNodeWidth(cr);
//      if (w>averageNodeWidth) {
//        
//      }
//      x[slot]+=Math.max(averageNodeWidth, w);
//      
//      
//      
//    }
  }
  
  /**
   * Get next free slot
   * @param x
   * @param y
   * @return slot between 0 and {@link #cols}
   */
  private int getNextFreeSlot(double[] x, double[] y) {
    int slot = 0;
    for (int i=1; i<y.length; i++) {
      if (y[i]<y[slot]) {
        slot = i;
      }
      if (y[i]==y[slot]) {
        if (x[i]<x[slot]) {;
          slot = i;
        }
      }
    }
    return slot;
  }
  
  public static void setStackingChildNodePosition(NodeRealizer cr, double nodeWidth, double nodeHeight, double firstX, double firstY, int nodeIndex) {
    int cols = 2;
    double inset=5.0;
    
    double x = ((nodeIndex-1) %cols); // column
    x = (x*(nodeWidth+inset)) + firstX;
    double y = ((nodeIndex-1) /cols); // row
    y = (y*(nodeHeight+inset)) + firstY;
    
    cr.setX(x);
    cr.setY(y);
  }
  
  
  public void prepareVariables(Node group) {
    if (group==null) return;
    
    // Get size of all children
    childs = new ArrayList<Node>(TranslatorTools.getChildren(group));
    Set<Double> widths = new HashSet<Double>();
    Set<Double> heights = new HashSet<Double>();
    double minX = Double.MAX_VALUE;
    double minY = Double.MAX_VALUE;
    for (Node n : childs) {
      NodeRealizer nr = graph.getRealizer(n);
      double w = getNodeWidth(nr);
      widths.add(w);
      heights.add(nr.getHeight());
      minX = Math.min(nr.getX(), minX);
      minY = Math.min(nr.getY(), minY);
    }
    
    // Set global
    averageNodeHeight = Utils.median(heights);
    averageNodeWidth = Utils.median(widths);
    firstX = minX;
    firstY = minY;
  }



  private static double getNodeWidth(NodeRealizer nr) {
    double w = Math.max(nr.getWidth(), nr.getLabel().getWidth()-inset);
    return w;
  }
  
  
}
