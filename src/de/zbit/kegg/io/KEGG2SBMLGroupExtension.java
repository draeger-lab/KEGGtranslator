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
package de.zbit.kegg.io;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.ext.groups.Group;
import org.sbml.jsbml.ext.groups.GroupModel;
import org.sbml.jsbml.xml.parsers.GroupsParser;

import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.Pathway;

/**
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class KEGG2SBMLGroupExtension {
  private static final transient Logger log = Logger.getLogger(KEGG2SBMLGroupExtension.class.getName());
  
  /**
   * Layout extension namespace URL.
   */
  public static final String GROUP_NS = GroupsParser.namespaceURI;
  
  /**
   * Unique identifier to identify this Namespace/Extension.
   */
  public static final String GROUP_NS_NAME = GroupsParser.shortLabel;


  /**
   * Create a group from an entry. This is only useful if
   * {@link Entry#hasComponents()}.
   * 
   * @param p
   * @param model
   * @param entry
   * @return
   */
  public static Group createGroup(Pathway p, Model model, Entry entry) {
    return createGroup(p, model.getParent(), model, entry);
  }
  
  /**
   * Create a group from an entry. This is only useful if
   * {@link Entry#hasComponents()}.
   * 
   * @param p
   * @param doc
   * @param model
   * @param entry
   * @return
   */
  public static Group createGroup(Pathway p, SBMLDocument doc, Model model, Entry entry) {
    // Make sure extension is available
    // NOTE: this should be called every time! No need to check if it is already contained.
    doc.addNamespace(GROUP_NS_NAME, "xmlns", GROUP_NS);
    doc.getSBMLDocumentAttributes().put(GROUP_NS_NAME + ":required", "true");
    
    // Create group model
    GroupModel groupModel = (GroupModel) model.getExtension(GROUP_NS);
    if (groupModel==null) {
      groupModel = new GroupModel(model);
      model.addExtension(GROUP_NS, groupModel);
    }
    
    // Get all group-members
    List<String> componentSpeciesIDs = new ArrayList<String>();
    if (entry.hasComponents()) {
      for (int c:entry.getComponents()) {
        Entry ce = p.getEntryForId(c);
        if (ce!=null && ce.getCustom()!=null && ce.getCustom() instanceof NamedSBase) {
          String speciesID = ((NamedSBase)ce.getCustom()).getId();
          componentSpeciesIDs.add(speciesID);
        }
      }
    }
    
    // Create group and add all members
    Group g = groupModel.createGroup();
    for (String id: componentSpeciesIDs) {
      g.createMember(id);
    }
    
    return g;
  }
  
  
}