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
package de.zbit.kegg.io;

import org.sbml.jsbml.AbstractNamedSBase;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.ext.layout.ExtendedLayoutModel;
import org.sbml.jsbml.ext.layout.Layout;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;

import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.Graphics;
import de.zbit.kegg.parser.pathway.Pathway;

/**
 * Add support for the layout extension to SBML translations.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class KEGG2SBMLLayoutExtension {
  
  /**
   * Layout extension namespace URL.
   */
  public static final String LAYOUT_NS = "http://www.sbml.org/sbml/level3/version1/layout/version1";
  
  /**
   * Unique identifier to identify this Namespace/Extension.
   */
  public static final String LAYOUT_NS_NAME = "layout";

  
  /**
   * Add (translate) layout extension to the given model. Translates
   * all {@link Graphics} objects from KEGG to the layout extension.
   * Works with all {@link AbstractNamedSBase}s, thus with Species
   * as well as QualitativeSpecies. 
   * @param p
   * @param doc
   * @param model
   */
  public static void addLayoutExtension(Pathway p, SBMLDocument doc, Model model) {
   
    doc.addNamespace(LAYOUT_NS_NAME, "xmlns", LAYOUT_NS);
    doc.getSBMLDocumentAttributes().put(LAYOUT_NS_NAME + ":required", "false");
    
    ExtendedLayoutModel layoutModel = (ExtendedLayoutModel) model.getExtension(LAYOUT_NS);
    if (layoutModel==null) {
      layoutModel = new ExtendedLayoutModel(model);
      model.addExtension(LAYOUT_NS, layoutModel);
    } else {
      // Remove all previous layouts.
      layoutModel.unsetListOfLayouts();
    }
    
    
    // Give a warning if we have no relations.
    Layout layout = layoutModel.createLayout();
    for (Entry e : p.getEntries()) {
      Object s = e.getCustom();
      if (s!=null && e.hasGraphics()) {
        Graphics g = e.getGraphics();
        // TODO: Multiple graphics (i.e. lines)?
        
        if (s instanceof AbstractNamedSBase) {
          SpeciesGlyph glyph = layout.createSpeciesGlyph(((AbstractNamedSBase) s).getId());
          glyph.createBoundingBox(g.getWidth(), g.getHeight(), 1, g.getX(), g.getY(), 0);
        }
      }
    }
    // TODO: other things to add?
    
    //return;
  }
  
  
}
