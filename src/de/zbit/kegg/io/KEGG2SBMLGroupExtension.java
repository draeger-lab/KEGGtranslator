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
package de.zbit.kegg.io;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.sbml.jsbml.AbstractSBase;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.ext.groups.Group;
import org.sbml.jsbml.ext.groups.GroupKind;
import org.sbml.jsbml.ext.groups.GroupsConstants;
import org.sbml.jsbml.ext.groups.GroupsModelPlugin;
import org.sbml.jsbml.ext.groups.Member;

import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.Pathway;

/**
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class KEGG2SBMLGroupExtension {
  
  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger log = Logger.getLogger(KEGG2SBMLGroupExtension.class.getName());
  
  /**
   * Layout extension namespace URL.
   */
  public static final String GROUP_NS = GroupsConstants.namespaceURI;
  
  /**
   * Unique identifier to identify this Namespace/Extension.
   */
  public static final String GROUP_NS_NAME = GroupsConstants.shortLabel;
  
  
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
    GroupsModelPlugin groupModel = getGroupsModelPlugin(model);
    
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
    
    // The KIND attribute is required. Possible values are listed in GroupKind
    g.setKind(GroupKind.collection);
    
    return g;
  }
  
  
  /**
   * Clones the given group <code>g</code>
   * @param id the id of the new group
   * @param g
   * @param prefixForMembers this will be prepended to all member symbols
   * in the new group.
   * @return the nre {@link Group}.
   */
  public static Group cloneGroup(String id, Group g, String prefixForMembers) {
    if (g==null) {
      return null;
    }
    
    GroupsModelPlugin groupModel = getGroupsModelPlugin(g);
    
    // Create group and add all members
    Group gNew = new Group(g);
    gNew.setId(id);
    gNew.setMetaId("meta_" + id);
    gNew.unsetListOfMembers();
    
    
    // Add all members with new prefix
    for (Member m: g.getListOfMembers()) {
      String symbol = m.getIdRef();
      if (prefixForMembers != null) {
        symbol = prefixForMembers + symbol;
      }
      gNew.createMember(symbol);
    }
    
    groupModel.addGroup(gNew);
    return gNew;
  }
  
  /**
   * Duplicates all members of the given group <code>g</code> and
   * adds a prefix to all duplicated members.
   * @param g
   * @param prefixForMembers
   */
  public static void cloneGroupComponents(Group g, String prefixForMembers) {
    if (g == null) {
      return;
    }
    
    // Create group and add all members
    List<String> symobls = new LinkedList<String>();
    for (Member m: g.getListOfMembers()) {
      symobls.add(m.getIdRef());
    }
    
    // Add all members with new prefix
    for (String symbol: symobls) {
      if (prefixForMembers!=null) {
        if (!symbol.startsWith(prefixForMembers)) {
          symbol = prefixForMembers + symbol;
          g.createMember(symbol);
        }
      }
    }
    
    return;
  }
  
  /**
   * Get or create the {@link GroupsModelPlugin}.
   * @param g any {@link AbstractSBase}.
   * @return
   */
  private static GroupsModelPlugin getGroupsModelPlugin(AbstractSBase g) {
    Model model = g.getModel();
    SBMLDocument doc = model.getSBMLDocument();
    
    // Make sure extension is available
    // NOTE: this should be called every time! No need to check if it is already contained.
    doc.addNamespace(GROUP_NS_NAME, "xmlns", GROUP_NS);
    doc.getSBMLDocumentAttributes().put(GROUP_NS_NAME + ":required", "true");
    
    // Create group model
    GroupsModelPlugin groupModel = (GroupsModelPlugin) model.getExtension(GROUP_NS);
    if (groupModel == null) {
      groupModel = new GroupsModelPlugin(model);
      model.addExtension(GROUP_NS, groupModel);
    }
    
    return groupModel;
  }
  
}