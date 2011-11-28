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

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.zbit.graph.ComplexNode;

import y.view.NodeRealizer;
import y.view.ShapeNodeRealizer;
import de.zbit.graph.NucleicAcidFeatureNode;

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
  private static final int Macromolecule_Enzyme1 = 245;
  private static final int Macromolecule_Enzyme2 = 252;
  private static final int simpleChemical = 247;
  private static final int gene = 354;
  private static final int materialEntityOfUnspecifiedNature = 285;
  private static final int map = 552;
  private static final int nonCovalentComplex = 253;
  
  
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
    
    sbo2shape.put(gene, new NucleicAcidFeatureNode()); // nucleic acid feature - gene
    
    sbo2shape.put(materialEntityOfUnspecifiedNature, new ShapeNodeRealizer(ShapeNodeRealizer.ELLIPSE)); // unspecified - material entity of unspecified nature
    sbo2shape.put(map, new ShapeNodeRealizer(ShapeNodeRealizer.ELLIPSE)); // unspecified - empty set
    
    sbo2shape.put(nonCovalentComplex, new ComplexNode()); // complex - non-covalent complex
    
    
    SBMLVisualizationProperties.sbo2shape = Collections.unmodifiableMap(sbo2shape);
  }
  
  /**
   * @return the color of the appropriate shabe
   */
  private static Color getColor(int sboTerm) {
    switch (sboTerm) {
      case nonCovalentComplex:
        return new Color(24,116,205);    // DodgerBlue3
      case gene:
        return new Color( 255,255,0);    // Yellow
      case Macromolecule_Enzyme1:
        return new Color(0,205,0);       // Green 3
      case Macromolecule_Enzyme2:
        return new Color(0,205,0);       // Green 3
      case simpleChemical:
        return new Color(176,226,255);   // LightSkyBlue1
      case map:
        return new Color(224,238,238);   // azure2
      default:
        return new Color(144,238,144);   // LightGreen
    }
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
    
    ret.setFillColor(getColor(sboTerm));
    
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
