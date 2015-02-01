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
 * Copyright (C) 2011-2015 by the University of Tuebingen, Germany.
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
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.HashSet;

import y.view.LineType;
import y.view.NodeRealizer;
import y.view.ShapeNodeRealizer;

/**
 * Creates a spline (node). Note that this node ignores
 * the X/Y/W/H properties and thus, can not be replaced
 * or moved! The only control is by adding points with
 * {@link #addSplineCoords(int, int)} as absolute
 * coordinates!
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class LineNodeRealizer extends ShapeNodeRealizer {
  
  /**
   * By default, create a line of width 3.
   */
  private final static LineType defaultStroke = LineType.LINE_3;
  
  /**
   * Allows to say that line i=Integer and i+1
   * should not get connected. Must be sorted incrementally!
   */
  Collection<Integer> doNotConnectIndex = null;
  
  public LineNodeRealizer() {
    super();
    shape = new Polygon();
    setLineType(defaultStroke);
  }
  
  public LineNodeRealizer(NodeRealizer nr) {
    super(nr);
    // If the given node realizer is of this type, then apply copy semantics.
    if (nr instanceof LineNodeRealizer) {
      LineNodeRealizer fnr = (LineNodeRealizer) nr;
      // Copy the values of custom attributes.
      setCoordLists((Polygon)fnr.shape);
    }
  }
  
  /* (non-Javadoc)
   * @see y.view.ShapeNodeRealizer#createCopy(y.view.NodeRealizer)
   */
  @Override
  public NodeRealizer createCopy(NodeRealizer nr) {
    return new LineNodeRealizer(nr);
  }
  
  /**
   * 
   * @param other already cloned other list.
   */
  private void setCoordLists(Polygon other) {
    shape = new Polygon(); // clear
    for (int i=0; i<other.npoints; i++) {
      ((Polygon)shape).addPoint(other.xpoints[i], other.ypoints[i]);
    }
    
  }
  
  /**
   * Add a pair of x/y coordinates for the current spline.
   * 
   * @param x
   * @param y
   */
  public void addSplineCoords(int x, int y) {
    ((Polygon)shape).addPoint(x, y);
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
  
  /* (non-Javadoc)
   * @see y.view.NodeRealizer#getX()
   */
  @Override
  public double getX() {
    //return shape.getBounds().getX();
    //return fake value to not hide the line
    return getCenterX();
  }
  
  /* (non-Javadoc)
   * @see y.view.NodeRealizer#getY()
   */
  @Override
  public double getY() {
    //return shape.getBounds().getY();
    //return fake value to not hide the line
    return getCenterY();
  }
  
  /* (non-Javadoc)
   * @see y.view.NodeRealizer#getCenterX()
   */
  @Override
  public double getCenterX() {
    return shape.getBounds().getCenterX();
  }
  
  /* (non-Javadoc)
   * @see y.view.NodeRealizer#getCenterY()
   */
  @Override
  public double getCenterY() {
    return shape.getBounds().getCenterY();
  }
  
  /* (non-Javadoc)
   * @see y.view.NodeRealizer#getWidth()
   */
  @Override
  public double getWidth() {
    return shape.getBounds().getWidth();
  }
  
  /* (non-Javadoc)
   * @see y.view.NodeRealizer#getHeight()
   */
  @Override
  public double getHeight() {
    return shape.getBounds().getHeight();
  }
  
  /**
   * Paints the spline-node.
   */
  @Override
  public void paintNode(Graphics2D gfx) {
    gfx.setStroke(getLineType());
    Color c = getFillColor();
    if (c!=null) {
      gfx.setColor(c);
      // since it is a line, don't fill it.
      //      gfx.fillPolygon(((Polygon)shape));
    }
    Polygon p = (Polygon)shape;
    for (int i=1; i<p.npoints; i++) {
      try {
        if (doNotConnectIndex==null || !doNotConnectIndex.contains(i)) {
          gfx.drawLine(p.xpoints[i-1], p.ypoints[i-1], p.xpoints[i], p.ypoints[i]);
        }
      } catch (Throwable e) {}
    }
    // do NOT connect line. That's why we don't paint the polygon directly!
    //gfx.drawLine(p.xpoints[p.npoints-1], p.ypoints[p.npoints-1], p.xpoints[0], p.ypoints[0]);
  }
  
  
  /* (non-Javadoc)
   * @see y.view.NodeRealizer#getBoundingBox()
   */
  @Override
  public Rectangle2D.Double getBoundingBox() {
    Rectangle b = shape.getBounds();
    return new Rectangle2D.Double(b.getX(), b.getY(), b.getWidth(), b.getHeight());
  }
  
  
  /* (non-Javadoc)
   * @see y.view.ShapeNodeRealizer#calcUnionRect(java.awt.geom.Rectangle2D)
   */
  @Override
  public void calcUnionRect(Rectangle2D arg0) {
    super.calcUnionRect(arg0);
    // should be same as "shape.getBounds2D().createUnion(rect);"
  }
  
  /**
   * Avoid connecting the last added coordinates and the next
   * added ones.
   */
  public void startNewLine() {
    Integer np = ((Polygon)shape).npoints;
    if (np!=null && np>0) {
      if (doNotConnectIndex==null) {
        doNotConnectIndex = new HashSet<Integer>();
      }
      doNotConnectIndex.add(np);
    }
  }
  
}
