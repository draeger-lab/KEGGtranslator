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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import y.view.NodeRealizer;
import y.view.ShapeNodeRealizer;

/**
 * This class stores the sbo terms and the corresponding NodeRealizer classes to visualize SBGN
 * 
 * @author Finja B&uuml;chel
 * @version $Rev$
 */
public class SBMLVisualizationProperties {

  public static final Logger log = Logger.getLogger(SBMLVisualizationProperties.class.getName());
  
  /**
   * contains all available shapes
   */
  public static Map<Integer, NodeRealizer> sbo2shape;
  
  
  /**
   * default shape is an Ellipse
   */
  private final static ShapeNodeRealizer defaultShape = new ShapeNodeRealizer(ShapeNodeRealizer.ELLIPSE);
  
  /**
   * 
   */
  private static int Macromolecule_Enzyme1 = 245;
  private static int Macromolecule_Enzyme2 = 252;
  private static int simpleChemical = 247;
  private static int gene = 354;
  private static int materialEntityOfUnspecifiedNature = 285;
  private static int emptySet = 552;
  private static int nonCovalentComplex = 253;
  
  
  /**
   * Static constructor to fill our static maps.
   */
  {
    init();
  }

  private static void init() {
    sbo2shape = new HashMap<Integer, NodeRealizer>();
   
    sbo2shape.put(Macromolecule_Enzyme1, new ShapeNodeRealizer(ShapeNodeRealizer.ROUND_RECT)); // macromolecule - enzyme
    sbo2shape.put(Macromolecule_Enzyme2, new ShapeNodeRealizer(ShapeNodeRealizer.ROUND_RECT)); // macromolecule - enzyme
    
    sbo2shape.put(simpleChemical, new ShapeNodeRealizer(ShapeNodeRealizer.ELLIPSE)); // simple chemical - simple chemical
    
    sbo2shape.put(gene, new ShapeNodeRealizer(ShapeNodeRealizer.ROUND_RECT)); // nucleic acid feature - gene
    
    sbo2shape.put(materialEntityOfUnspecifiedNature, new ShapeNodeRealizer(ShapeNodeRealizer.ELLIPSE)); // unspecified - material entity of unspecified nature
    sbo2shape.put(emptySet, new ShapeNodeRealizer(ShapeNodeRealizer.ELLIPSE)); // unspecified - empty set
    
    sbo2shape.put(nonCovalentComplex, new ShapeNodeRealizer(ShapeNodeRealizer.ROUND_RECT)); // complex - non-covalent complex
    
    
    SBMLVisualizationProperties.sbo2shape = Collections.unmodifiableMap(sbo2shape);
  }

  /**
   * 
   * @param sboTerm
   * @return the adaquate {@link ShapeNodeRealizer}, if for this sboterm no {@link ShapeNodeRealizer}
   * is available it return the {@link #defaultShape}
   */
  public static NodeRealizer getNodeRealizer(int sboTerm) {
    if (sbo2shape == null) {
      init();
    }
    NodeRealizer ret = sbo2shape.get(sboTerm);
    if (ret == null) {
      ret = defaultShape;
      log.log(Level.FINE, "sboTerm: " + sboTerm + " couldn't be assigned to a shape, default shape is used");
    }
    return ret;
  }

  /**
   * @param sboTerm
   * @return
   */
  public static boolean isCircleShape(int sboTerm) {
    if (sboTerm == simpleChemical) {
      return true;
    } else {
      return false;
    }
  }
  
  

}
