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
package de.zbit.graph;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.Set;

import y.base.Edge;
import y.base.EdgeCursor;
import y.base.Node;
import y.geom.YPoint;
import y.view.EdgeRealizer;
import y.view.Graph2D;
import y.view.NodeRealizer;
import y.view.ShapeNodeRealizer;
import de.zbit.util.Utils;

/**
 * Creates a small node that should be used as reaction node
 * to visualize e.g. SBML reactions.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class ReactionNodeRealizer extends ShapeNodeRealizer {
  
  public ReactionNodeRealizer() {
    super(ShapeNodeRealizer.RECT);
    setHeight(8);
    setWidth(16);
  }
  
  public ReactionNodeRealizer(NodeRealizer nr) {
    super(nr);
    // If the given node realizer is of this type, then apply copy semantics. 
    if (nr instanceof ReactionNodeRealizer) {
      ReactionNodeRealizer fnr = (ReactionNodeRealizer) nr;
      // TODO: Copy the values of custom attributes. 
    }
  }
  
  public NodeRealizer createCopy(NodeRealizer nr) {
    return new ReactionNodeRealizer(nr);
  }
  
  /* (non-Javadoc)
   * @see y.view.NodeRealizer#paint(java.awt.Graphics2D)
   */
  @Override
  public void paint(Graphics2D arg0) {
    paintNode(arg0);
  }
  
  /* (non-Javadoc)
   * @see y.view.NodeRealizer#paintSloppy(java.awt.Graphics2D)
   */
  @Override
  public void paintSloppy(Graphics2D arg0) {
    paintNode(arg0);
  }
  
  /**
   * Paints the reaction-node.
   */
  public void paintNode(Graphics2D gfx) {
    gfx.setColor(Color.BLACK);
    int x = (int) getX(); int y = (int) getY();
    double min = Math.min(getWidth(), getHeight());
    double offsetX = (getWidth()-min)/2.0;
    double offsetY = (getHeight()-min)/2.0;
    
    // Draw the reaction node rectangle
    gfx.drawRect((int)offsetX+x, (int)offsetY+y, (int)min, (int)min);
    
    boolean vertical = offsetY>offsetX;
    if (!vertical) {
      int halfHeight = (int)(getHeight()/2.0);
      
      // Draw the small reaction lines on both sides, where substrates
      // and products should dock.
      gfx.drawLine(0+x, halfHeight+y, (int)offsetX+x, halfHeight+y);
      gfx.drawLine((int)(offsetX+min)+x, halfHeight+y, (int)getWidth()+x, halfHeight+y);
      
    } else {
      // Rotate node by 90°
      int halfWidth = (int)(getWidth()/2.0);
      
      // Draw the small reaction lines on both sides, where substrates
      // and products should dock.
      gfx.drawLine(halfWidth+x, 0+y, halfWidth+x, (int)offsetY+y);
      gfx.drawLine(halfWidth+x, (int)(offsetY+min)+y, halfWidth+x, (int)getHeight()+y);
      
    }
    
  }
  
  /**
   * True if the node is painted in a horizontal layout.
   * Use {@link #rotateNode()} to change the rotation.
   */
  public boolean isHorizontal() {
    return getWidth()>=getHeight();
  }
  
  /**
   * Rotates the node by 90°.
   */
  public void rotateNode() {
    double width = getWidth();
    double height = getHeight();
    setWidth(height);
    setWidth(width);
  }
  
  /**
   * Configures the orientation of this node and the docking point
   * for all adjacent edges to match the given parameters.
   * <p>Still, you should make sure that all reatants are on one side
   * of the node and all products on the other one!
   */
  public void fixLayout(Set<Node> reactants, Set<Node> products, Set<Node> modifier) {
    Node no = getNode();
    Graph2D graph = (Graph2D) no.getGraph();
    
    // Determine location of adjacent nodes
    Integer[] cases = new Integer[4];
    Arrays.fill(cases, 0);
//    int reactantsAbove=0;
//    int reactantsLeft=1;
//    int productsAbove=2;
//    int productsLeft=3;
    
    for (EdgeCursor ec = no.edges(); ec.ok(); ec.next()) {
      Edge v = ec.edge();
      Node other = v.opposite(no);
      NodeRealizer nr = graph.getRealizer(other);
      
      if (products.contains(other)) {
        if (nr.getX()<getCenterX()) cases[0]++;
        if (nr.getY()<getCenterY()) cases[1]++;
      } else if (reactants.contains(other)) {
        if (nr.getX()<getCenterX()) cases[2]++;
        if (nr.getY()<getCenterY()) cases[3]++;      }
    }
    
    // Determine orientation of this node
    double max = Utils.max(Arrays.asList(cases));
    boolean horizontal = isHorizontal();
    if (cases[0]==max && horizontal) rotateNode();
    else if (cases[1]==max && !horizontal) rotateNode();
    else if (cases[2]==max && horizontal) rotateNode();
    else if (cases[3]==max && !horizontal) rotateNode();
    
    // Dock all edges to the correct side
    if (isHorizontal()) {
      if (cases[1]>cases[3]) {
        // Reactants are left of this node
        setEdgesToDockOnLeftSideOfNode(reactants);
        setEdgesToDockOnRightSideOfNode(products);
      } else {
        setEdgesToDockOnLeftSideOfNode(products);
        setEdgesToDockOnRightSideOfNode(reactants);
      }
    } else {
      if (cases[0]>cases[2]) {
        // Reactants are left of this node
        setEdgesToDockOnUpperSideOfNode(reactants);
        setEdgesToDockOnLowerSideOfNode(products);
      } else {
        setEdgesToDockOnUpperSideOfNode(products);
        setEdgesToDockOnLowerSideOfNode(reactants);
      }
    }
    // TODO: reaction modifiers dock on square
    
  }

  /**
   * @param adjacentNodes
   */
  private void setEdgesToDockOnLowerSideOfNode(Set<Node> adjacentNodes) {
    YPoint dockToPoint = new YPoint(0,getHeight()/2);
    letAllEdgesDockToThisPointOnThisNode(adjacentNodes, dockToPoint);
  }

  /**
   * @param adjacentNodes
   */
  private void setEdgesToDockOnUpperSideOfNode(Set<Node> adjacentNodes) {
    YPoint dockToPoint = new YPoint(0,getHeight()/2*-1);
    letAllEdgesDockToThisPointOnThisNode(adjacentNodes, dockToPoint);
  }

  /**
   * @param adjacentNodes
   */
  private void setEdgesToDockOnRightSideOfNode(Set<Node> adjacentNodes) {
    YPoint dockToPoint = new YPoint(getWidth()/2,0);
    letAllEdgesDockToThisPointOnThisNode(adjacentNodes, dockToPoint);
  }

  /**
   * @param adjacentNodes
   */
  private void setEdgesToDockOnLeftSideOfNode(Set<Node> adjacentNodes) {
    YPoint dockToPoint = new YPoint(getWidth()/2*-1,0);
    letAllEdgesDockToThisPointOnThisNode(adjacentNodes, dockToPoint);
  }

  /**
   * @param adjacentNodes
   * @param dockToPoint
   */
  protected void letAllEdgesDockToThisPointOnThisNode(Set<Node> adjacentNodes,
    YPoint dockToPoint) {
    Node no = getNode();
    Graph2D graph = (Graph2D) no.getGraph();
    
    for (EdgeCursor ec = no.edges(); ec.ok(); ec.next()) {
      Edge v = ec.edge();
      Node other = v.opposite(no);
      EdgeRealizer er = graph.getRealizer(v);
      boolean isSource = v.source().equals(no);
      
      if (adjacentNodes.contains(other)) {
        if (isSource) {
          er.setSourcePoint(dockToPoint);
        } else {
          er.setTargetPoint(new YPoint(getWidth()/2*-1,0));
        }
      }
    }
  }
  
  
  //  /* (non-Javadoc)
  //   * @see y.view.NodeRealizer#getBoundingBox()
  //   */
  //  @Override
  //  public Rectangle2D.Double getBoundingBox() {
  //    Rectangle b = shape.getBounds();
  //    return new Rectangle2D.Double(b.getX(), b.getY(), b.getWidth(), b.getHeight());
  //  }
  //  
  //       
  //  /* (non-Javadoc)
  //   * @see y.view.ShapeNodeRealizer#calcUnionRect(java.awt.geom.Rectangle2D)
  //   */
  //  @Override
  //  public void calcUnionRect(Rectangle2D arg0) {
  //    super.calcUnionRect(arg0);
  //    // should be same as "shape.getBounds2D().createUnion(rect);"
  //  }
  
}
