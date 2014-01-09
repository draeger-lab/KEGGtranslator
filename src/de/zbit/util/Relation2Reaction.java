///*
// * $Id$
// * $URL$
// * ---------------------------------------------------------------------
// * This file is part of KEGGtranslator, a program to convert KGML files
// * from the KEGG database into various other formats, e.g., SBML, GML,
// * GraphML, and many more. Please visit the project homepage at
// * <http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator> to
// * obtain the latest version of KEGGtranslator.
// *
// * Copyright (C) 2011-2014 by the University of Tuebingen, Germany.
// *
// * KEGGtranslator is free software; you can redistribute it and/or
// * modify it under the terms of the GNU Lesser General Public License
// * as published by the Free Software Foundation. A copy of the license
// * agreement is provided in the file named "LICENSE.txt" included with
// * this software distribution and also available online as
// * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
// * ---------------------------------------------------------------------
// */
//package de.zbit.util;
//
//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.logging.Logger;
//
//import de.zbit.kegg.KeggInfoManagement;
//import de.zbit.kegg.KeggInfos;
//import de.zbit.kegg.KeggTools;
//import de.zbit.kegg.parser.KeggParser;
//import de.zbit.kegg.parser.pathway.Entry;
//import de.zbit.kegg.parser.pathway.EntryType;
//import de.zbit.kegg.parser.pathway.Pathway;
//import de.zbit.kegg.parser.pathway.Reaction;
//
///**
// * <b>TODO: WORK IN PROGRESS, does not work yet.</b><p>
// * The methods in this class can be employed to convert relations to
// * reactions.
// *
// * <p>Be careful, it is currently unused since the risk of
// * creating invalid pathway models with automatic relation to reaction
// * conversion is too high!
// *
// * @author Clemens Wrzodek
// */
//@Deprecated
//public class Relation2Reaction {
//  public static final transient Logger log = Logger.getLogger(Relation2Reaction.class.getName());
//
//  /**
//   * @param args
//   * @throws Exception
//   */
//  public static void main(String[] args) throws Exception {
//    Pathway p = KeggParser.parse("files/KGMLsamplefiles/hsa00010.xml").get(0);
//
//    Entry x = p.getEntryForId(34);
//
//    /*
//     * - Compunds identifizieren
//     * - Infos holen, reactions parsen
//     * - reactions suibstrates, products& enzymes, schauen was wir haben
//     * - wenn wir mind. 1 haben, dann reaction hinzuf��gen.
//     */
//    System.out.println(x);
//  }
//
//
//  @SuppressWarnings("unused")
//  public static void autocompleteReactions(Pathway p, KeggInfoManagement manager) {
//    int newEntrysId = p.getMaxEntryId();
//    /* PLEASE SEE KeggTools#autocompleteReactions(Pathway, KeggInfoManagement)
//     * for an updated version. This method is OUTDATED!
//     *
//     */
//    for (Reaction r : p.getReactions()) {
//      for (String ko_id : r.getName().split(" ")) {
//
//        // Get the complete reaction from Kegg
//        KeggInfos infos = new KeggInfos(ko_id, manager);
//        if (infos.queryWasSuccessfull()) {
//
//          ArrayList<String> reacts = getReactants(infos);
//
//          /*
//          Entry found = p.getEntryForName(reactant);
//          if (found == null) {
//            // Create a new entry
//            newEntrysId++;
//            Entry entry = new Entry(p, newEntrysId,reactant);
//
//            autocompleteLinkAndAddToPathway(p, entry);
//
//            if (isSubstrate) r.addSubstrate(new ReactionComponent(entry.getName()));
//            else r.addProduct(new ReactionComponent(entry.getName()));
//          }*/
//
//
//          // Add missing enzymes
//          if (infos.getEnzymes()!=null) {
//            // Get all Enzymes, that are already contained in the pathway.
//            List<Entry> modifier = p.getReactionModifiers(r.getName());
//            List<String> contained_enzymes = new LinkedList<String>();
//            for (Entry mod : modifier) {
//              contained_enzymes.addAll(KeggTools.getKeggEnzymeNames(mod, manager));
//            }
//            // remark: contained_enzymes contains doubles. But this doesn't matter
//
//            // Iterate through all enzymes in the reaction
//            String[] enzymes = infos.getEnzymes().trim().replaceAll("\\s+", " ").split(" ");
//            for (String enzyme: enzymes) {
//              boolean isContained=contained_enzymes.contains(enzyme);
//              if (!isContained) { // Add the enzyme
//                // Create a new entry
//                newEntrysId++;
//                Entry entry = new Entry(p, newEntrysId,"EC:"+enzyme,EntryType.enzyme);
//                entry.setReaction(r.getName());
//
//                KeggTools.autocompleteLinkAndAddToPathway(p, entry);
//              }
//
//            }
//          }
//
//
//        }
//      }
//
//    }
//
//  }
//
//
//  private static ArrayList<String> getReactants(KeggInfos infos) {
//    ArrayList<String> ret = new ArrayList<String>();
//
//    // Add missing reactants
//    if (infos.getEquation()!=null) {
//      String eq = infos.getEquation();
//      int dividerPos = eq.indexOf("<=>");
//      eq = eq.replace("<=>", " + ");
//
//      int curPos = eq.indexOf(" + ");
//      int lastPos = 0;
//      while (lastPos>=0) {
//        String reactant = eq.substring(lastPos, curPos>=0?curPos:eq.length()).trim();
//        boolean isSubstrate = (lastPos < dividerPos);
//        if (reactant.contains(" ")) { // e.g. "2 C00103"
//          reactant = reactant.substring(reactant.indexOf(" ")+1);
//        }
//        /*
//         * TODO: Note: This is outdated method and a novel version
//         * that those the same can be found in KeggTools.java !
//         */
//        char firstChar = reactant.charAt(0);
//        if (firstChar!='C' && firstChar!='G') {
//          log.warning(String.format("Warning: non-compound and non-glycan reactat: %s", reactant));
//        } else if (!reactant.contains(":")) {
//          if (firstChar=='C') {
//            reactant = "cpd:" + reactant;
//          } else if (firstChar=='G') {
//            reactant = "gl:" + reactant;
//          }
//        }
//
//        ret.add(reactant);
//        /*
//        Entry found = p.getEntryForName(reactant);
//        if (found == null) {
//          // Create a new entry
//          newEntrysId++;
//          Entry entry = new Entry(p, newEntrysId,reactant);
//
//          autocompleteLinkAndAddToPathway(p, entry);
//
//          if (isSubstrate) r.addSubstrate(new ReactionComponent(entry.getName()));
//          else r.addProduct(new ReactionComponent(entry.getName()));
//        }*/
//
//        lastPos = curPos<0?curPos:curPos+1;
//        curPos = eq.indexOf(" + ", curPos+1);
//      }
//    }
//    return ret;
//  }
//
//
//}
