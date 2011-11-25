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

import java.awt.Graphics2D;
import java.awt.Polygon;

import y.view.NodeRealizer;
import y.view.ShapeNodeRealizer;

/**
 * @author Finja B&uuml;chel
 * @version $Rev$
 */
public class ComplexNode extends ShapeNodeRealizer {
  

  public ComplexNode() {
    super(ShapeNodeRealizer.ROUND_RECT);
  }
  
  public ComplexNode(NodeRealizer nr) {
    super(nr);
    // If the given node realizer is of this type, then apply copy semantics. 
    if (nr instanceof ComplexNode) {
      ComplexNode fnr = (ComplexNode) nr;
      // TODO: Copy the values of custom attributes. 
    }
  }
  
  public NodeRealizer createCopy(NodeRealizer nr) {
    return new ComplexNode(nr);
  }
  
  /* (non-Javadoc)
   * @see y.view.ShapeNodeRealizer#paintShapeBorder(java.awt.Graphics2D)
   */
  @Override
  protected void paintShapeBorder(Graphics2D gfx) {
    gfx.setColor(getLineColor());
    gfx.draw(getPolygon());
  }
  
  /* (non-Javadoc)
   * @see y.view.ShapeNodeRealizer#paintFilledShape(java.awt.Graphics2D)
   */
  @Override
  protected void paintFilledShape(Graphics2D gfx) {
   if (!isTransparent() && getFillColor()!=null) {
      gfx.setColor(getFillColor());
      gfx.fill(getPolygon());
    }
  }
  
  /**
   * Paints the complex-node.
   *       1 . . . . . 2
   *     .               .
   *   8                   3
   *   .                   .
   *   .                   .
   *   7                   4
   *    .                 .
   *      6 . . . . . . 5
   * 
   */
  public Polygon getPolygon() {
    int arc = (int) (Math.min(getWidth(), getHeight())/5);
    Polygon nodeshape = new Polygon(); 
    nodeshape.addPoint((int)  getX()+arc,             (int)getY());                   // 1
    nodeshape.addPoint((int) (getX()+getWidth())-arc, (int)getY());                   // 2
    nodeshape.addPoint((int) (getX()+getWidth()),     (int)getY()+arc);               // 3
    nodeshape.addPoint((int) (getX()+getWidth()),     (int)(getY()+getHeight()-arc)); // 4
    nodeshape.addPoint((int) (getX()+getWidth())-arc, (int)(getY()+getHeight()));     // 5
    nodeshape.addPoint((int) (getX()+arc),            (int)(getY()+getHeight()));     // 6
    nodeshape.addPoint((int)  getX(),                 (int)(getY()+getHeight()-arc)); // 7
    nodeshape.addPoint((int)  getX(),                 (int)(getY()+arc));             // 8
    nodeshape.addPoint((int)  getX()+arc,             (int)(getY()));                 // 1
    
    return nodeshape;    
  }
}
