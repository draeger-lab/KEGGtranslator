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
import java.awt.geom.GeneralPath;

import y.view.NodeRealizer;
import y.view.ShapeNodeRealizer;

/**
 * Node for a nucleic acid feature. This is basically
 * a rectangle with normal upper corners and rounded
 * lower corners.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class NucleicAcidFeatureNode extends ShapeNodeRealizer {
  
  public NucleicAcidFeatureNode() {
    super(ShapeNodeRealizer.RECT);
  }
  
  public NucleicAcidFeatureNode(NodeRealizer nr) {
    super(nr);
    // If the given node realizer is of this type, then apply copy semantics. 
    if (nr instanceof NucleicAcidFeatureNode) {
      NucleicAcidFeatureNode fnr = (NucleicAcidFeatureNode) nr;
      // Copy the values of custom attributes (there are none).
    }
  }
  
  public NodeRealizer createCopy(NodeRealizer nr) {
    return new NucleicAcidFeatureNode(nr);
  }
  
  /* (non-Javadoc)
   * @see y.view.ShapeNodeRealizer#paintShapeBorder(java.awt.Graphics2D)
   */
  @Override
  protected void paintShapeBorder(Graphics2D gfx) {
    gfx.setColor(getLineColor());
    gfx.draw(getPath());
  }
  
  /* (non-Javadoc)
   * @see y.view.ShapeNodeRealizer#paintFilledShape(java.awt.Graphics2D)
   */
  @Override
  protected void paintFilledShape(Graphics2D gfx) {
   if (!isTransparent() && getFillColor()!=null) {
      gfx.setColor(getFillColor());
      gfx.fill(getPath());
    }
  }

  /**
   * @return
   */
  protected GeneralPath getPath() {
    int arc = (int) (getWidth()/10);
    
    GeneralPath path = new GeneralPath();
    path.moveTo(getX(), getY());
    path.lineTo(getX()+getWidth(), getY());
    path.lineTo(getX()+getWidth(), getY()+getHeight()-arc);
    path.quadTo(getX()+getWidth(), getY()+getHeight(), getX()+getWidth()-arc, getY()+getHeight());
    path.lineTo(getX()+arc, getY()+getHeight());
    path.quadTo(getX(), getY()+getHeight(), getX(), getY()+getHeight()-arc);
    path.closePath();
    return path;
  }
}
