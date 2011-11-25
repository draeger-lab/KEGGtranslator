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
import java.awt.Polygon;
import java.awt.geom.PathIterator;

import y.base.Graph;
import y.view.NodeRealizer;
import y.view.ShapeNodeRealizer;

/**
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class NucleicAcidFeatureNode extends ShapeNodeRealizer {
  
  // TODO: Fix this stub!
  private void asffasf () {
    int arc = (int) (getWidth()/2);
    Polygon nodeshape = new Polygon() {
      public java.awt.geom.PathIterator getPathIterator(java.awt.geom.AffineTransform at) {
        PathIterator pi = super.getPathIterator(at);
        
        while (!pi.isDone()) {
          pi.currentSegment(coords)
        }
        pi.
        int windingRule = pi.getWindingRule();
        while (!pi.isDone()) {
          switch (pi.currentSegment(coords)) {
            
          }
        }
      };
    }
    nodeshape.addPoint((int)getX(), (int)getY());
    nodeshape.addPoint((int) (getX()+getWidth()), (int)getY());
    nodeshape.addPoint((int) (getX()+getWidth()), (int)(getY()+getHeight()-arc));
    
  
  }

  public NucleicAcidFeatureNode() {
    super(ShapeNodeRealizer.RECT);
  }
  
  public NucleicAcidFeatureNode(NodeRealizer nr) {
    super(nr);
    // If the given node realizer is of this type, then apply copy semantics. 
    if (nr instanceof NucleicAcidFeatureNode) {
      NucleicAcidFeatureNode fnr = (NucleicAcidFeatureNode) nr;
      // TODO: Copy the values of custom attributes. 
    }
  }
  
  public NodeRealizer createCopy(NodeRealizer nr) {
    return new NucleicAcidFeatureNode(nr);
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
    int arc = (int) (getWidth()/5.0);
    gfx.drawRoundRect((int)getX(), (int)getY(), (int)getWidth(), (int)getHeight(), arc, arc);
    // TODO: Remove rounded upper edges
    // TODO: Make non-rounded upper edges.
    
    
  }
}
