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
 * Copyright (C) 2010-2012 by the University of Tuebingen, Germany.
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
import java.awt.Rectangle;
import java.awt.Shape;

import y.view.NodeRealizer;

/**
 * This interface must be implemented by all nodes, that can be cloned/ copied/
 * splitted to enhance the overall visualizability of the graph. E.g., when an
 * enzyme occurs in 3 reactions, it is mostly a much nicer graph if you create 3
 * nodes for the same enzyme.
 * 
 * <p>
 * According to SBGN-PD standards, the bottom 1/3 of those nodes must be colored
 * black:
 * <p>
 * <i>If an EPN is duplicated on a map, it is necessary to indicate this fact by
 * using the clone marker auxiliary unit. The purpose of this marker is to
 * provide the reader with a visual indication that this node has been cloned,
 * and that at least one other occurrence of the EPN can be found in the map (or
 * in a submap; see Section 2.7.1). The clone marker takes two forms, simple and
 * labeled, depending on whether the node being cloned can carry state variables
 * (i.e., whether it is a stateful EPN). Note that an EPN belongs to a single
 * compartment. If two glyphs labelled "X" are located in two different
 * compartments, such as ATP in cytosol and ATP in mitochondrial lumen, they
 * represent different EPNs, and therefore do not need to be marked as
 * cloned.</i>
 * </p>
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public abstract interface CloneMarker {
  
  /**
   * This defines the exact amount of the lower part of a node,
   * that should be painted black.
   */
  public final static double partToPaintBlack=1d/5d;
  
  /**
   * Add or remove a clone marker to this node.
   * @param b
   */
  public void setNodeIsCloned(boolean b);
  
  /**
   * If this returns true, the node has another instance
   * in this graph.
   * @return
   */
  public boolean isNodeCloned();
  


  /**
   * Provide some tools to help realize the {@link CloneMarker}.
   * 
   * @author Clemens Wrzodek
   * @version $Rev$
   */
  public static class Tools {
    
  /**
   * Use this method, e.g., in your "y.view.ShapeNodeRealizer#paintFilledShape(java.awt.Graphics2D)"
   * method to make the lower part black.
   * @param gfx
   * @param nr
   * @param shape
   */
  public static <T extends NodeRealizer & CloneMarker> void paintLowerBlackIfCloned(Graphics2D gfx, T nr, Shape shape) {
    // Eventually paint the lower part black
    if (nr.isNodeCloned()) {
      // Create clip and draw black
      gfx.setClip(shape);
      gfx.clip(new Rectangle((int)nr.getX(), (int)(nr.getY()+nr.getHeight()*(1d-partToPaintBlack)), (int)nr.getWidth(), (int)Math.ceil((nr.getHeight()*partToPaintBlack))));
      gfx.setColor(Color.BLACK);
      gfx.fill(gfx.getClip());
      // Reset
      gfx.setClip(null);
      gfx.setColor(nr.getFillColor());
    }
  }
  }
  
}
