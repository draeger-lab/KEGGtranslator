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

import y.view.NodeRealizer;
import y.view.ShapeNodeRealizer;

/**
 * Creates a small node that should be used as reaction node
 * to visualize e.g. SBML reactions.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class ReactionNodeRealizer extends ShapeNodeRealizer {
  
  public ReactionNodeRealizer() {
    super();
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
    
    boolean vertical = offsetY>offsetX; // TODO: <===
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
