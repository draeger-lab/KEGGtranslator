/*
 * $Id: MinAndMaxTracker.java 402 2015-02-03 20:22:57Z draeger $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn/KEGGconverter/trunk/src/de/zbit/graph/MinAndMaxTracker.java $
 * ---------------------------------------------------------------------
 * This file is part of KEGGtranslator, a program to convert KGML files
 * from the KEGG database into various other formats, e.g., SBML, GML,
 * GraphML, and many more. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator> to
 * obtain the latest version of KEGGtranslator.
 *
 * Copyright (C) 2010-2015 by the University of Tuebingen, Germany.
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

/**
 * This is a helper class that tracks always the minimum and
 * maximum x/y coordinates, including the width and height
 * of nodes.
 * 
 * @author Clemens Wrzodek
 * @version $Rev: 402 $
 */
public class MinAndMaxTracker {
  
  /**
   * 
   */
  double minX = Double.MAX_VALUE;
  /**
   * 
   */
  double minY = Double.MAX_VALUE;
  
  /**
   * 
   */
  double maxX = Double.MIN_VALUE;
  /**
   * 
   */
  double maxY = Double.MIN_VALUE;
  
  /**
   * 
   */
  public MinAndMaxTracker() {
    super();
  }
  
  /**
   * 
   * @param x
   * @param y
   */
  public void track(int x, int y) {
    track((double)x,(double)y);
  }
  
  /**
   * 
   * @param x
   * @param y
   * @param width
   * @param height
   */
  public void track(int x, int y, int width, int height) {
    track((double)x,(double)y, (double)width, (double)height);
  }
  
  /**
   * 
   * @param x
   * @param y
   */
  public void track(double x, double y) {
    track(x, y, 0d, 0d);
  }
  
  /**
   * 
   * @param x
   * @param y
   * @param width
   * @param height
   */
  public void track(double x, double y, double width, double height) {
    minX = Math.min(minX, x);
    minY = Math.min(minY, y);
    
    maxX = Math.max(maxX, x+width);
    maxY = Math.max(maxY, y+height);
  }
  
  /**
   * @return the minX
   */
  public double getMinX() {
    return minX==Double.MAX_VALUE?0:minX;
  }
  
  /**
   * @return the minY
   */
  public double getMinY() {
    return minY==Double.MAX_VALUE?0:minY;
  }
  
  /**
   * @return the maxX
   */
  public double getMaxX() {
    return maxX==Double.MIN_VALUE?0:maxX;
  }
  
  /**
   * @return the maxY
   */
  public double getMaxY() {
    return maxY==Double.MIN_VALUE?0:maxY;
  }
  
  /**
   * Get the total width:
   * <pre>
   * Math.abs(getMinX()) + Math.abs(getMaxX());
   * </pre>
   * @return
   */
  public double getWidth() {
    return Math.abs(getMinX()) + Math.abs(getMaxX());
  }
  
  /**
   * Get the total height:
   * <pre>
   * Math.abs(getMinY()) + Math.abs(getMaxY());
   * </pre>
   * @return
   */
  public double getHeight() {
    return Math.abs(getMinY()) + Math.abs(getMaxY());
  }
  
}
