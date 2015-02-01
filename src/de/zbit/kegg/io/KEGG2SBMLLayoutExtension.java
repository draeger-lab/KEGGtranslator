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
package de.zbit.kegg.io;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.sbml.jsbml.AbstractNamedSBase;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.ext.layout.BoundingBox;
import org.sbml.jsbml.ext.layout.Layout;
import org.sbml.jsbml.ext.layout.LayoutConstants;
import org.sbml.jsbml.ext.layout.LayoutModelPlugin;
import org.sbml.jsbml.ext.layout.ReactionGlyph;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;
import org.sbml.jsbml.ext.layout.SpeciesReferenceGlyph;
import org.sbml.jsbml.ext.layout.SpeciesReferenceRole;
import org.sbml.jsbml.ext.layout.TextGlyph;

import de.zbit.graph.MinAndMaxTracker;
import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.Graphics;
import de.zbit.kegg.parser.pathway.GraphicsType;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.kegg.parser.pathway.ReactionComponent;
import de.zbit.util.ArrayUtils;
import de.zbit.util.Utils;

/**
 * Add support for the layout extension to SBML translations.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class KEGG2SBMLLayoutExtension {
  private static final transient Logger log = Logger.getLogger(KEGG2SBMLLayoutExtension.class.getName());
  
  /**
   * Layout extension namespace URL.
   */
  public static final String LAYOUT_NS = LayoutConstants.namespaceURI;
  
  /**
   * Unique identifier to identify this Namespace/Extension.
   */
  public static final String LAYOUT_NS_NAME = LayoutConstants.shortLabel;
  
  
  /**
   * Add (translate) layout extension to the given model. Translates
   * all {@link Graphics} objects from KEGG to the layout extension.
   * Works with all {@link AbstractNamedSBase}s, thus with Species
   * as well as QualitativeSpecies.
   * <p><i>This will erase all previous layouts!</i></p>
   * @param p
   * @param doc
   * @param model
   * @param metabolic if true, will set {@link ReactionGlyph}s instead
   * of {@link SpeciesGlyph} whereever possible.
   */
  public static void addLayoutExtension(Pathway p, SBMLDocument doc, Model model, boolean metabolic) {
    addLayoutExtension(p, doc, model, metabolic,true);
  }
  /**
   * Add (translate) layout extension to the given model. Translates
   * all {@link Graphics} objects from KEGG to the layout extension.
   * Works with all {@link AbstractNamedSBase}s, thus with Species
   * as well as QualitativeSpecies.
   * @param p
   * @param doc
   * @param model
   * @param metabolic if true, will set {@link ReactionGlyph}s instead
   * of {@link SpeciesGlyph} whereever possible.
   * @param removeAllPreviousLayouts if {@code true}, will call
   * unsetListOfLayouts() and remove all previous layouts before adding
   * the new one.
   */
  public static void addLayoutExtension(Pathway p, SBMLDocument doc, Model model, boolean metabolic, boolean removeAllPreviousLayouts) {
    
    // Make sure extension is available
    // NOTE: this should be called every time! No need to check if it is already contained.
    doc.addNamespace(LAYOUT_NS_NAME, "xmlns", LAYOUT_NS);
    doc.getSBMLDocumentAttributes().put(LAYOUT_NS_NAME + ":required", "false");
    
    // Create layout model
    LayoutModelPlugin layoutModel = (LayoutModelPlugin) model.getExtension(LAYOUT_NS);
    if (layoutModel==null) {
      layoutModel = new LayoutModelPlugin(model);
      model.addExtension(LAYOUT_NS, layoutModel);
    } else {
      // Remove all previous layouts.
      if (removeAllPreviousLayouts) {
        layoutModel.unsetListOfLayouts();
      }
    }
    
    // Map enzymes to reactions in metabolic models
    Map<String, Collection<Reaction>> enzyme2rct = null;
    if (metabolic) {
      enzyme2rct = getEnzyme2ReactionMap(doc);
    }
    
    
    // Create Species and Reaction Glyps.
    Layout layout = layoutModel.createLayout();
    layout.setName(String.format("Translated %s layout.", metabolic?"metabolic":"qualitative"));
    layout.setId(createUniqueLayoutId(layout, layoutModel));
    
    // It's stupid, but the whole "layout" requires a dimension.
    // => track min and max values.
    MinAndMaxTracker tracker = new MinAndMaxTracker();
    
    Map<String, Integer> idCounts = new HashMap<String, Integer>();
    Map<String, ReactionGlyph> keggReactionName2glyph = new HashMap<String, ReactionGlyph>();
    
    // First, create a glyph for each reaction
    if (metabolic) {
      Map<String, Reaction> sbmlReactionName2reaction = new HashMap<String, Reaction>();
      for (Reaction r: doc.getModel().getListOfReactions()) {
        // I know that name must not be unique, but in KEGGtranslator, name IS unique
        // and name IS the same as used in KGML.
        sbmlReactionName2reaction.put(r.getName(), r);
      }
      for (de.zbit.kegg.parser.pathway.Reaction r: p.getReactions()) {
        if (sbmlReactionName2reaction.containsKey(r.getName())) {
          Reaction sbmlR = sbmlReactionName2reaction.get(r.getName());
          // Reactions may also be duplicated in KGMLs => don't create duplicate reactionGlyphs
          // for the same, single reaction!
          if (!layout.containsGlyph(sbmlR)) {
            ReactionGlyph glyph = layout.createReactionGlyph(createGlyphID(idCounts, sbmlR.getId()), sbmlR.getId());
            keggReactionName2glyph.put(r.getName(), glyph);
          }
        }
      }
      sbmlReactionName2reaction=null;
    }
    
    // Add compartments
    for (Compartment c : model.getListOfCompartments()) {
      if (!c.getId().equals("default")) {
        String compId = c.getId();
        layout.createCompartmentGlyph(createGlyphID(idCounts, compId), compId);
      }
    }
    
    // Create a glyph for each entry (In KGML, only entries have graph objects)
    for (Entry e : p.getEntries()) {
      Object s = e.getCustom();
      if (s!=null && e.hasGraphics()) {
        Graphics g = e.getGraphics();
        boolean isLineGraphic = g.getType().equals(GraphicsType.line);
        if (isLineGraphic) {
          // Line-graphics are nonsense.
          //continue;
          // ... but sometimes better than nothing.
        }
        tracker.track(g.getX(), g.getY(), g.getWidth(), g.getHeight());
        // TODO: Are lines (also in mutliple graphics tags) possible?
        
        if (s instanceof AbstractNamedSBase) {
          String speciesId = ((AbstractNamedSBase) s).getId();
          
          // Multiple species glyphs are permitted for one species!
          SpeciesGlyph sGlyph;
          //        if (listOfSpecGlyphs.isEmpty()) {
          sGlyph = layout.createSpeciesGlyph(createGlyphID(idCounts, speciesId), speciesId);
          TextGlyph tGlyph = layout.createTextGlyph(createGlyphID(idCounts, speciesId));
          tGlyph.setGraphicalObject(sGlyph);
          tGlyph.setOriginOfText(speciesId);
          BoundingBox speciesBox = sGlyph.getBoundingBox();
          if (speciesBox == null) {
            speciesBox = sGlyph.createBoundingBox();
          }
          speciesBox.createDimensions(g.getWidth(), g.getHeight(), 0d);
          
          /*
           * Create reaction glyph with x/y and species glyph with width/height.
           */
          if (metabolic && enzyme2rct.containsKey(speciesId)) {
            
            // Try to match reactions (if we have multiple instances of the same enzyme)
            Reaction rct = null;
            Collection<Reaction> rcts = enzyme2rct.get(speciesId);
            String[] entryReactions = e.getReactions();
            if (entryReactions!=null && rcts.size()>0) {
              // Match reactions
              for (Reaction r: rcts) {
                int pos = ArrayUtils.indexOf(entryReactions, r.toString());
                if (pos >= 0) {
                  rct = r;
                  break;
                }
              }
            }
            if (rct == null) {
              // No match => take first without positions
              for (Reaction r : rcts) {
                ReactionGlyph rg = layout.getReactionGlyph(r.getId());
                if (rg==null || rg.getBoundingBox()==null || !rg.getBoundingBox().isSetPosition()) {
                  rct = r;
                  break;
                }
              }
              log.fine("Could not match unique ReactionGlyph to " + e);
            }
            // Sometimes, reactions occur twice in documents or have two enzymes.
            // then, rct is NULL here!
            
            // Removing the reation is a bad idea, because sometimes there
            // are multiple instances of the same entry pointin to the same
            // reaction. We then prefer the reactangle (NOT the line GraphicsType).
            // Thus, we should not remove set reactions.
            //Utils.removeFromMapOfSets(enzyme2rct, rct, speciesId);
            
            // Set X and Y on the reactionGlyph
            boolean positionAttributesUsed = false;
            if (!g.isDefaultPosition() && (rct != null) && !isLineGraphic) {
              // LINE coordinate are much worse than rectangles. So prefer rectangles!
              List<ReactionGlyph> listOfGlyphs = layout.findReactionGlyphs(rct.getId());
              ReactionGlyph glyph;
              if ((listOfGlyphs == null) || listOfGlyphs.isEmpty()) {
                glyph = layout.createReactionGlyph(createGlyphID(idCounts, rct.getId()), rct.getId());
                keggReactionName2glyph.put(rct.getName(), glyph); // NOTE: The SBML reaction name must therefore be equal to the KGMLs reaction name.
              } else {
                glyph = listOfGlyphs.get(0);
              }
              glyph.unsetBoundingBox();
              BoundingBox rbox = glyph.createBoundingBox();
              rbox.createPosition(g.getX(), g.getY(), 0d);
              positionAttributesUsed = true;
            }
            // Set width and height on the species glyph
            // Multiple species glyphs are permitted for one species!
            //            List<SpeciesGlyph> listOfSpecGlyphs = layout.findSpeciesGlyphs(speciesId);
            //            if (listOfSpecGlyphs.isEmpty()) {
            
            //              } else {
            //                sGlyph = listOfSpecGlyphs.get(0);
            //              }
            
            if (!positionAttributesUsed && !g.isDefaultPosition()) {
              // X and Y are unused
              if (!speciesBox.isSetPosition() || !isLineGraphic) {
                // Line values are only better than nothing.
                speciesBox.createPosition(g.getX(), g.getY(), 0);
              }
            }
            
            //            }
            
          } else {
            
            /*
             * Signaling map (or metabolic and species is no enzyme)
             * => Just create SpeciesGlyph.
             */
            
            // Create a Glyph with x/y/width/height for the species
            //            List<SpeciesGlyph> listOfSpeciesGlyphs = layout.findSpeciesGlyphs(speciesId);
            
            if (!g.isDefaultPosition()) {
              if (!speciesBox.isSetPosition() || !isLineGraphic) {
                // Line values are only better than nothing.
                speciesBox.createPosition(g.getX(), g.getY(), 0);
              }
            }
          }
          
          
          /*
           * At this position, the speciesGlyph is created and the coordinates may be
           * transfered to a novel reactionGlyph. But since duplicate glyphs for a
           * species are allowed, create rectionGlyphs and add species Glyph as reference.
           */
          if (metabolic) {
            // Add to catalyzing reactions
            if (e.isSetReaction()) {
              for (String reaction : e.getReactions()) {
                ReactionGlyph rg = keggReactionName2glyph.get(reaction);
                if (rg!=null) {
                  SpeciesReferenceGlyph srg = rg.createSpeciesReferenceGlyph(createGlyphID(idCounts, speciesId), sGlyph.getId());
                  srg.setRole(SpeciesReferenceRole.MODIFIER);
                }
              }
            }
            
            // If unique assignment (by id) available, add as substrate/product
            if (e.isSetID()) {
              for (de.zbit.kegg.parser.pathway.Reaction r : p.getReactionsForEntry(e)) {
                ReactionGlyph rg = keggReactionName2glyph.get(r.getName());
                if (rg!=null) {
                  // do NOT assign by name. this does not solve the problem with clones.
                  for (ReactionComponent rc : r.getSubstrates()) {
                    if (rc.isSetID() && rc.getId().intValue() == e.getId()) {
                      SpeciesReferenceGlyph srg = rg.createSpeciesReferenceGlyph(createGlyphID(idCounts, speciesId), sGlyph.getId());
                      srg.setRole(SpeciesReferenceRole.SUBSTRATE);
                    }
                  }
                  for (ReactionComponent rc : r.getProducts()) {
                    if (rc.isSetID() && rc.getId().intValue() == e.getId()) {
                      SpeciesReferenceGlyph srg = rg.createSpeciesReferenceGlyph(createGlyphID(idCounts, speciesId), sGlyph.getId());
                      srg.setRole(SpeciesReferenceRole.PRODUCT);
                    }
                  }
                }
              }
            }
          }
          // --- End of adding SpeciesReferenceGlyphs
          
          
        }
      }
    }
    // TODO: other things to add?
    
    
    // Add the total dimension
    layout.createDimensions(tracker.getWidth(), tracker.getHeight(), 1);
  }
  
  
  /**
   * Returnes the next available (unsed) id of a layout, beginning with
   * "layout", "layout2", "layout3",...
   * @param layout
   * @param layoutModel
   * @return unused layout identifier
   */
  private static String createUniqueLayoutId(Layout layout, LayoutModelPlugin layoutModel) {
    String idPrefix = "layout";
    String id = idPrefix;
    
    ListOf<Layout> lol = layoutModel.getListOfLayouts();
    if (lol == null) {
      return id;
    }
    
    int s = 2;
    for (int i=0; i<lol.size(); i++) {
      if (lol.get(i).getId().equalsIgnoreCase(id)) {
        id = idPrefix + s;
        s++;
        i=-1;
      }
    }
    
    return id;
  }
  /**
   * 
   * @param idCounts
   * @param id
   * @return
   */
  private static String createGlyphID(Map<String, Integer> idCounts,
    String id) {
    String gID = "glyph_" + id;
    if (idCounts.containsKey(id)) {
      idCounts.put(id, Integer.valueOf(idCounts.get(id).intValue() + 1));
    } else {
      idCounts.put(id, Integer.valueOf(1));
    }
    gID = gID + '_' + idCounts.get(id);
    return gID;
  }
  
  /**
   * Create a map from species with enzymatic activity to reactions,
   * in which they occur as enzymes.
   * @param document
   * @return a map that links from every enzyme (species intance) to a list of reactions,
   * in which this enzyme occurs as modifier.
   */
  public static Map<String, Collection<Reaction>> getEnzyme2ReactionMap(SBMLDocument document) {
    Map<String, Collection<Reaction>> enzymeSpeciesIDs = new HashMap<String, Collection<Reaction>>();
    for (Reaction r : document.getModel().getListOfReactions()) {
      if (r.isSetListOfModifiers()) {
        for (ModifierSpeciesReference msr : r.getListOfModifiers()) {
          if (msr.isSetSpecies() && msr.getSpecies().length()>0) {
            Utils.addToMapOfSets(enzymeSpeciesIDs, msr.getSpecies(), r);
          }
        }
      }
    }
    return enzymeSpeciesIDs;
  }
  
  
}
