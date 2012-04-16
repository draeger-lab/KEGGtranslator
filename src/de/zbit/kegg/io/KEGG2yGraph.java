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
 * 
 * There are special restrictions for this file. Each procedure that
 * is using the yFiles API must stick to their license restrictions.
 * Please see the following link for more information
 * <http://www.yworks.com/en/products_yfiles_sla.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.kegg.io;

import java.awt.Color;
import java.awt.Font;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import y.base.DataMap;
import y.base.Edge;
import y.base.EdgeCursor;
import y.base.EdgeMap;
import y.base.Node;
import y.base.NodeList;
import y.base.NodeMap;
import y.geom.YInsets;
import y.io.GIFIOHandler;
import y.io.GMLIOHandler;
import y.io.GraphMLIOHandler;
import y.io.IOHandler;
import y.io.JPGIOHandler;
import y.io.TGFIOHandler;
import y.io.YGFIOHandler;
import y.view.Arrow;
import y.view.EdgeLabel;
import y.view.EdgeRealizer;
import y.view.GenericEdgeRealizer;
import y.view.GenericNodeRealizer;
import y.view.Graph2D;
import y.view.LineType;
import y.view.NodeLabel;
import y.view.NodeRealizer;
import y.view.ShapeNodeRealizer;
import y.view.hierarchy.GroupNodeRealizer;
import y.view.hierarchy.HierarchyManager;
import de.zbit.graph.LineNodeRealizer;
import de.zbit.graph.io.Graph2Dwriter;
import de.zbit.graph.io.def.GenericDataMap;
import de.zbit.graph.io.def.GraphMLmaps;
import de.zbit.kegg.KEGGtranslatorOptions;
import de.zbit.kegg.api.KeggInfos;
import de.zbit.kegg.api.cache.KeggInfoManagement;
import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.EntryType;
import de.zbit.kegg.parser.pathway.Graphics;
import de.zbit.kegg.parser.pathway.GraphicsType;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.kegg.parser.pathway.Reaction;
import de.zbit.kegg.parser.pathway.ReactionComponent;
import de.zbit.kegg.parser.pathway.ReactionType;
import de.zbit.kegg.parser.pathway.Relation;
import de.zbit.kegg.parser.pathway.SubType;
import de.zbit.kegg.parser.pathway.ext.EntryExtended;
import de.zbit.util.ArrayUtils;
import de.zbit.util.DatabaseIdentifiers;
import de.zbit.util.DatabaseIdentifiers.IdentifierDatabases;
import de.zbit.util.SortedArrayList;
import de.zbit.util.Utils;

/**
 * KEGG2yGraph converter. Can generate, e.g., GML or GraphML
 * and many more graph-based output formats.
 * 
 * <p>Keywords: KEGG2GraphML, KEGG2GML, KEGG2JPG,
 * KGML2GraphML, KGML2GML, KGML2JPG.
 * 
 * <p><i>Note:<br/>
 * Due to yFiles license requirements, we have to obfuscate this class
 * in the JAR release of this application. Thus, this class
 * can not be found by using the class name.<br/> If you can provide us
 * with a proof of possessing a yFiles license yourself, we can send you
 * an unobfuscated release of KEGGtranslator.</i></p>
 * 
 * @author Clemens Wrzodek
 * @since 1.0
 * @version $Rev$
 */
public class KEGG2yGraph extends AbstractKEGGtranslator<Graph2D> {
  
  /**
   * This translation is mainly graph based, so simply ignoring entries
   * without an graph attribute makes sense.
   */
  private boolean showEntriesWithoutGraphAttribute=true;
  /**
   * Compounds are small molecules, biopolymers, and other chemical
   * substances that are relevant to biological systems. 
   */
  private boolean skipCompounds=false;
  /**
   * If multiple nodes having exactly the same edges, they are being grouped
   * to one node if this is set to true.
   */
  private boolean groupNodesWithSameEdges=false;
  /**
   * Create labels for edges (activation, compound, phosphorylation, etc.)
   * or not.
   */
  private boolean createEdgeLabels=false;
  /**
   * If true, hides labels for all compounds (=small molecules).
   */
  private boolean hideLabelsForCompounds=false;
  /**
   * Draws grey arrows for reactions. This is not recommended since reactions
   * are mostly formally duplicates of the processed relations.
   */
  private boolean drawArrowsForReactions=false;
  /**
   * Important: This determines the output format. E.g. a GraphMLIOHandler
   * will write a graphML file, a GMLIOHandler will write a GML file.
   * 
   */
  private Graph2Dwriter outputHandler = null;
  

  
  /*===========================
   * CONSTRUCTORS
   * ===========================*/
  
  /**
   * 
   */
  public KEGG2yGraph(IOHandler outputHandler) {
    this(outputHandler, new KeggInfoManagement());
  }
  /**
   * @param manager
   */
  public KEGG2yGraph(IOHandler outputHandler, KeggInfoManagement manager) {
    this (new Graph2Dwriter(outputHandler), manager);
    
    loadPreferences();
  }
  
  /**
   * @param manager
   */
  public KEGG2yGraph(Graph2Dwriter outputHandler, KeggInfoManagement manager) {
    super(manager);
    this.outputHandler = outputHandler;
    this.outputHandler.setTranslator(this);
    
    loadPreferences();
  }
  
  /*===========================
   * Getters and Setters
   * ===========================*/
  
  /**
   * See {@link #setShowEntriesWithoutGraphAttribute(boolean)}
   */
  public boolean isShowEntriesWithoutGraphAttribute() {
    return showEntriesWithoutGraphAttribute;
  }
  /**
   * This translation is mainly graph based, so simply ignoring entries
   * without an graph attribute makes sense.
   * @param showEntriesWithoutGraphAttribute
   */
  public void setShowEntriesWithoutGraphAttribute(boolean showEntriesWithoutGraphAttribute) {
    this.showEntriesWithoutGraphAttribute = showEntriesWithoutGraphAttribute;
  }
  /**
   * If true, all compounds will get skipped in the translation.
   * See also: {@link #skipCompounds}
   * @return
   */
  public boolean isSkipCompounds() {
    return skipCompounds;
  }
  /**
   * See {@link #isSkipCompounds()}
   * @param skipCompounds
   */
  public void setSkipCompounds(boolean skipCompounds) {
    this.skipCompounds = skipCompounds;
  }
  /**
   * If multiple nodes having exactly the same edges, they are being grouped
   * to one node if this is set to true.
   * @return
   */
  public boolean isGroupNodesWithSameEdges() {
    return groupNodesWithSameEdges;
  }
  /**
   * See {@link #isGroupNodesWithSameEdges()}
   * @param groupNodesWithSameEdges
   */
  public void setGroupNodesWithSameEdges(boolean groupNodesWithSameEdges) {
    this.groupNodesWithSameEdges = groupNodesWithSameEdges;
  }
  
  /**
   * 
   * @return
   */
  public Graph2Dwriter getWriter() {
    return outputHandler;
  }
  
  /**
   * If labels for edges are being generated or not.
   * Examples: (activation, compound, phosphorylation, etc.)
   * @return createEdgeLabel
   */
  public boolean isCreateEdgeLabels() {
    return createEdgeLabels;
  }
  /**
   * @see #isCreateEdgeLabels()
   * @param createEdgeLabel the createEdgeLabel to set
   */
  public void setCreateEdgeLabels(boolean createEdgeLabels) {
    this.createEdgeLabels = createEdgeLabels;
  }
  
  /**
   * @return the drawArrowsForReactions
   */
  public boolean isDrawArrowsForReactions() {
    return drawArrowsForReactions;
  }
  /**
   * @param drawArrowsForReactions the drawArrowsForReactions to set
   */
  public void setDrawArrowsForReactions(boolean drawArrowsForReactions) {
    this.drawArrowsForReactions = drawArrowsForReactions;
  }
  
  /*===========================
   * FUNCTIONS
   * ===========================*/
  
  /** Load the default preferences from the SBPreferences object. */
  private void loadPreferences() {
  	groupNodesWithSameEdges = KEGGtranslatorOptions.MERGE_NODES_WITH_SAME_EDGES.getValue(prefs);
  	createEdgeLabels = KEGGtranslatorOptions.CREATE_EDGE_LABELS.getValue(prefs);
  	drawArrowsForReactions = KEGGtranslatorOptions.DRAW_GREY_ARROWS_FOR_REACTIONS.getValue(prefs);
  	hideLabelsForCompounds = KEGGtranslatorOptions.HIDE_LABELS_FOR_COMPOUNDS.getValue(prefs);
  	
  	// Wee need to set autocompleteReactions to false, because it does not make
  	// sense in out context and considerReactions() is sometimes true.
  	autocompleteReactions = false;
  }

  
  /**
   * Converts an HTML color to an awt color.
   * @param theColor
   * @return java.awt.Color
   */
  public static Color ColorFromHTML(String theColor) {
    if (theColor.startsWith("#")) theColor = theColor.substring(1);
    if (theColor.trim().equalsIgnoreCase("none")) theColor="000000";
    
    if (theColor.length() != 6)
      throw new IllegalArgumentException("Not a valid HTML color: " + theColor);
    return new Color(
        Integer.valueOf(theColor.substring(0, 2), 16).intValue(),
        Integer.valueOf(theColor.substring(2, 4), 16).intValue(),
        Integer.valueOf(theColor.substring(4, 6), 16).intValue());
  }
  
  /**
   * Converts a java.awt.Color to the corresponding HTML-color code.
   * @param color
   * @return e.g. "#FEFEFE"
   */
  public static String ColorToHTML(Color color) {
    return "#" + Integer.toHexString(color.getRGB()).substring(2).toUpperCase();
  }
  
  
  /**
   * Compares two edges.
   * @param e - edge1
   * @param e2 - edge2
   * @param graph
   * @return true if and only if the two edges are completly equal, without
   * considering the target or source node.
   * More specific: Arrow shapes, lineType and labelTexts are equal.
   */
  public static boolean edgesEqualExceptTargets(Edge e, Edge e2, Graph2D graph) {
    EdgeRealizer er = graph.getRealizer(e);
    EdgeRealizer er2 = graph.getRealizer(e2);
    if (er.equals(er2) || er.getTargetArrow().equals(er2.getTargetArrow()) && er.getSourceArrow().equals(er2.getSourceArrow()) && er.getLineType().equals(er2.getLineType()) && er.getLabelText().equals(er2.getLabelText())) {
      return true;
    }
    return false;
  }
  
  
  /**
   * Tests, if two nodes do have exactly the same source and target
   * nodes on their edges and if the edges do have the same shape
   * (Arrow, LineType and description).
   * @param n1 - Node 1
   * @param n2 - Node 2
   * @param graph
   * @return true if and only if the nodes have the same edges, except
   * the source of the edges.
   */
  public static boolean nodesHaveSameEdges(Node n1, Node n2, Graph2D graph) {
    if (n1.inDegree()!=n2.inDegree() || n1.outDegree()!=n2.outDegree()) return false;
    
    Edge e = n1.firstInEdge();
    while (e!=null) {
      Edge e2 = n2.firstInEdge();
      boolean found = false;
      while (e2!=null) {
        if (e.source().equals(e2.source())) {
          if (edgesEqualExceptTargets(e, e2, graph)) {
            found = true;
            break;
          }
        }
        e2 = e2.nextInEdge();
      }
      
      if (!found) return false; // Edge from n1 not in n2
      e = e.nextInEdge();
    }
    
    // Same for outEdges
    e = n1.firstOutEdge();
    while (e!=null) {
      Edge e2 = n2.firstOutEdge();
      boolean found = false;
      while (e2!=null) {
        if (e.target().equals(e2.target())) {
          if (edgesEqualExceptTargets(e, e2, graph)) {
            found = true;
            break;
          }
        }
        e2 = e2.nextOutEdge();
      }
      
      if (!found) return false; // Edge from n1 not in n2
      e = e.nextOutEdge();
    }
    
    return true;
  }
  
  /**
   * Calculates the distance between all nodes in the given list.
   * Nodes that have a minimum distance above a given threshold
   * are considered being outliers and get removed.
   * @param nl - List of nodes
   * @param graph
   * @param threshold - threshold for outlier detection (e.g. 50)
   * @return filtered nodelist
   */
  private static NodeList removeOutlier(NodeList nl, Graph2D graph, int threshold) {
    // Calculate minimal distances
    double[] minDists = new double[nl.size()];
    for (int j=0; j<nl.size(); j++) {
      NodeRealizer n1 = graph.getRealizer((Node) nl.get(j));
      double minDist=Double.MAX_VALUE;
      for (int k=0; k<nl.size(); k++) {
        if (j==k) continue;
        NodeRealizer n2 = graph.getRealizer((Node) nl.get(k));
        double dist = Math.max(Math.abs(n1.getCenterX()-n2.getCenterX()), Math.abs(n1.getCenterY()-n2.getCenterY()));
        minDist = Math.min(minDist, dist);
      }
      //System.out.println(minDist + " \t" + n1.getLabelText() );
      minDists[j] = minDist;
    }
    
    ArrayList<Integer> toRemove = new ArrayList<Integer>();
    if (nl.size()<2) return nl; // one node
    
    for (int j=0; j<minDists.length; j++)
      if (minDists[j]>threshold) toRemove.add(j);
    
    // Nothing to do?
    if (toRemove.size()<1) return nl;
    
    NodeList nl2 = new NodeList(); 
    for (int j=0; j<nl.size(); j++) {
      if (toRemove.contains(j)) continue;
      nl2.add(nl.get(j));
    }
    nl = nl2;
    
    return nl;
  }
  
  /**
   * Standard Setup for group nodes.
   * @param nl
   * @param changeCaption - null if you don't want to change the caption.
   */
  public static NodeRealizer setupGroupNode(NodeLabel nl, String changeCaption) {
    GroupNodeRealizer nr = new GroupNodeRealizer();
    ((GroupNodeRealizer)nr).setGroupClosed(false);
    nr.setTransparent(true);
    
    if (changeCaption!=null) {
      nl.setText(changeCaption);
    }
    
    nr.setMinimalInsets(new YInsets(5, 2, 2, 2)); // top, left, bottom, right
    nr.setAutoBoundsEnabled(true);
    nl.setPosition(NodeLabel.TOP);
    nl.setBackgroundColor(new Color((float)0.8,(float)0.8,(float)0.8,(float)0.5));
    nl.setFontSize(10);
    nl.setAutoSizePolicy(NodeLabel.AUTOSIZE_NODE_WIDTH);
    
    nr.setLabel(nl);
    
    return nr;
  }
  
  /**
   * Convert a Kegg Pathway to a yFiles Graph2D object.
   * You should write this pathway to disk with the internal writeGraphToFile method.
   * If you don't do so, all meta-information (entrez ids for labels, etc.) will be lost.
   */
  protected Graph2D translateWithoutPreprocessing(Pathway p) {
    Graph2D graph = new Graph2D();
    ArrayList<String> PWReferenceNodeTexts = new ArrayList<String>();
    boolean showProgressForRelations = KeggInfoManagement.offlineMode;
    
    //Create graph annotation maps
    NodeMap nodeDescription = graph.createNodeMap();
    NodeMap entityType = graph.createNodeMap();
    NodeMap nodeLabel = graph.createNodeMap();
    NodeMap entrezIds = graph.createNodeMap();
    NodeMap keggOntIds = graph.createNodeMap();
    NodeMap uniprotIds = graph.createNodeMap();
    NodeMap ensemblIds = graph.createNodeMap();
    NodeMap nodeURLs = graph.createNodeMap();
    
    NodeMap nodeName = graph.createNodeMap();
    NodeMap nodeColor = graph.createNodeMap();
    NodeMap nodePosition = graph.createNodeMap();
    NodeMap nodeSize = graph.createNodeMap();
    //NodeMap bindsToChemicals = graph.createNodeMap();
    
    EdgeMap edgeDescription = graph.createEdgeMap(); // = Relation.type
    EdgeMap interactionDescription = graph.createEdgeMap(); // = Subtype.name "inhibition", "activation", "interaction", "physical interaction", "catalysis", "transcriptional control", "control"
    //EdgeMap interactionType = graph.createEdgeMap(); // "control", "catalysis", "physical interaction", "transport", "complex assembly", "biochemical reaction"
    //EdgeMap edgeURLs = graph.createEdgeMap();
    
    
    // Set the link
    if (p.isSetLink()) {
      try {
        graph.setURL(new URL(p.getLink()));
      } catch (MalformedURLException e1) {
        e1.printStackTrace();
      }
    }
    ArrayList<Node> parentGroupNodes = new ArrayList<Node>();
    ArrayList<List<Integer>> groupNodeChildren = new ArrayList<List<Integer>>();
    HierarchyManager hm = graph.getHierarchyManager();
    if (hm==null) hm = new HierarchyManager(graph);
    
    // Initialize a progress bar.
    initProgressBar(p,showProgressForRelations,false);
    
    // Save all reaction modifiers in a map. String = reaction id.
    Map<String, Node> reactionModifiers = new HashMap<String, Node>();
    
    // Add nodes for all Entries
    for (int i=0; i<p.getEntries().size(); i++) {
      progress.DisplayBar("Node " + (i+1) + "/" + p.getEntries().size());
      Entry e = p.getEntries().get(i);
      if (skipCompounds && e.getType().equals(EntryType.compound)) continue;
      
      Node n = null;
      Object nodeLink = null;
      boolean isPathwayReference=false;
      String name = e.getName().trim();
      if (name.toLowerCase().startsWith("path:") || e.getType().equals(EntryType.map)) isPathwayReference=true;
      
      // Get the graphics object, create a default one or skip this entry.
      Graphics g = null;
      if (e.hasGraphics()) {
        g = e.getGraphics();
      } else if (showEntriesWithoutGraphAttribute || autocompleteReactions) {
        // Create any graphics object with default attributes
        g = new Graphics(e);
      }
      
      // Handle the graphics first and then create this node.
      LineNodeRealizer line = null;
      if (g!=null) {
        /* Example:
         *     <entry id="16" name="ko:K04467 ko:K07209 ko:K07210" type="ortholog">
                 <graphics name="IKBKA..." fgcolor="#000000" bgcolor="#FFFFFF"
                   type="rectangle" x="785" y="141" width="45" height="17"/>
                   
         *   ... is actually a compund!?!?
         */
        
        // Get name, description and other annotations via api (organism specific) possible!!
        //Graphics g = e.getGraphics();
        if (g.getName().length()!=0) {
          name = g.getName();
        }
        /*if (g.getWidth()>0 && g.getHeight()>0) {
          n = graph.createNode(g.getX(), g.getY(), g.getWidth(), g.getHeight(), name);
        } else {
          n = graph.createNode(g.getX(), g.getY(), name);
        }*/
        
        NodeRealizer nr;
        NodeLabel nl = new NodeLabel(name);
        
        // one of "rectangle, circle, roundrectangle, line, other"
        boolean addThisNodeToGroupNodeList=false;
        if (isGroupNode(e)) {
          groupNodeChildren.add(e.getComponents());
          addThisNodeToGroupNodeList = true;
          
          
          // New Text
          String newText = null;
          if (nl.getText().length()==0 || nl.getText().startsWith("undefined")) {
            newText = "Group";
          }
          nr = setupGroupNode(nl, newText);
        } else {
          // set depdent on graphics object in setupGraphics()
          nr = null;
        }
        
        // Parse and set information from the graphics object
        nr = setupGraphics(nr, nl, g);
        if (nr instanceof LineNodeRealizer) {
          line = (LineNodeRealizer) nr;
        } else if (e.getType().equals(EntryType.compound)) {
          // Show label below node
          nl.setModel(NodeLabel.SIDES);
          nl.setPosition(NodeLabel.S);
          nl.setDistance(0);
        }
        
        nr.setLabel(nl);
        if (isPathwayReference && name.toUpperCase().startsWith("TITLE:")) {
          // Name of this pathway
          nl.setFontStyle(Font.BOLD);
          nl.setText(name); // name.substring("TITLE:".length()).trim()
          nr.setFillColor(Color.GREEN);
        } else if (isPathwayReference) {
          // Reference to another pathway
          nr.setFillColor(Color.LIGHT_GRAY);
        }
        
        
        // Store hyperlinks in the node-label (and later on in the node itself).
        String link = e.getLink();
        if (link!=null && link.length()!=0) {
          try {
            // Convert to URL, because type is infered of the submitted object
            // and URL is better than STRING.
            nodeLink = new URL(link);
          } catch (MalformedURLException e1) {
            nodeLink = link;
          }
          nl.setUserData(nodeLink);
        }
        
        n = graph.createNode(nr);
        if (addThisNodeToGroupNodeList) {
          hm.convertToGroupNode(n);
          parentGroupNodes.add(n);
        }
      }
      
      // Multiple graphics objects are actually only observed when
      // KEGG tries to create shapes with (multiple) lines.
      if (e.hasMultipleGraphics()) {
        for (Graphics g2: e.getMoreGraphics()) {
          // Mostly, lines are multiple graphics => try to pack in same node.
          if (g2.getType().equals(GraphicsType.line) && line!=null && g2.isSetCoords()) {
            // Just add another line to the existing line node
            line.startNewLine();
            Integer[] coords = g2.getCoords();
            for (int j=0; j<(coords.length-1); j+=2) {
              line.addSplineCoords(coords[j], coords[j+1]);
            }
            continue;
          }
          NodeRealizer nr = null;
          NodeLabel nl = new NodeLabel(g2.getName());

          // Parse and set information from the graphics object
          nr = setupGraphics(nr, nl, g2);
          nr.setLabel(nl);
          if (nr instanceof LineNodeRealizer) {
            line = (LineNodeRealizer) nr;
          }
          
          // Store hyperlinks in the node-label (and later on in the node itself).
          if (nodeLink!=null) {
            nl.setUserData(nodeLink);
          }
          
          // Create a node, but don't set the reference
          graph.createNode(nr);
        }
      }
      
      
      
      // Node was created. Postprocessing options.
      if (n!=null) {
        // Remember node in KEGG Structure for further references.
        e.setCustom(n);
        
        // Init variables
        List<KeggInfos> keggInfos = new LinkedList<KeggInfos>();
        String name2="",definition="",entrezIds2="",uniprotIds2="",eType="",ensemblIds2="";
        
        // Get a map of existing identifiers or create a new one
        Map<DatabaseIdentifiers.IdentifierDatabases, Collection<String>> ids = new HashMap<DatabaseIdentifiers.IdentifierDatabases, Collection<String>>();
        if (e instanceof EntryExtended) {
          ids = ((EntryExtended)e).getDatabaseIdentifiers();
          
          String temp = extractExistingIdentifiers(ids, IdentifierDatabases.EntrezGene);
          if (temp!=null) entrezIds2 = temp;
          temp = extractExistingIdentifiers(ids, IdentifierDatabases.UniProt_AC);
          if (temp!=null) uniprotIds2 = temp;
          temp = extractExistingIdentifiers(ids, IdentifierDatabases.Ensembl);
          if (temp!=null) ensemblIds2 = temp;
        }
        
        // Get all available annotations
        for (String ko_id:e.getName().split(" ")) {
          //Definition[] results = adap.getGenesForKO(e.getName(), retrieveKeggAnnotsForOrganism); // => GET only (und alles aus GET rausparsen). Zusaetzlich: in sortedArrayList merken.
          if (ko_id.trim().equalsIgnoreCase("undefined") || e.hasComponents()) continue;
          
          KeggInfos infos = KeggInfos.get(ko_id, manager);
          keggInfos.add(infos);
          
          // Add all available identifiers (enzrez gene, ensembl, etc)
          infos.addAllIdentifiers(ids);
          
          // "NCBI-GeneID:","UniProt:", "Ensembl:", ... aus dem GET rausparsen
          if (infos.queryWasSuccessfull()) {
            
            String text = infos.getNames();
            if (text!=null && text.length()!=0) {
              // Problem here is that space is used to separate gene synonyms, but
              // compounds may contain spaces in names. Thus, they need special treatment
              if (ko_id.startsWith("cpd:")) text = text.replace(" ", "-");
              
              name2+=(name2.length()>0?", ":"")+text.replace(", ", " ").replace(';', ' ').replace("\n", "");
            }
            // Append formula for compounds. ONLY WITH SPACE, because compounds synonyms are space divided
            text = infos.getFormula();
            if (text!=null && text.length()!=0) name2+=(name2.length()>0?" ":"")+text;
            
            if (e.getType().equals(EntryType.map)) { // => Link zu anderem Pathway oder Title-Node des aktuellem PW.
              text = infos.getDescription();
              if (text!=null && text.length()!=0) definition+=(definition.length()!=0?",":"")+text.replace(",", "").replace("\n", " ");
            } else {
              text = infos.getDefinition();
              if (text!=null && text.length()!=0) definition+=(definition.length()!=0?",":"")+text.replace(",", "").replace("\n", " ");
            }
            
            // Set entrez, uniprot and ensembl identifiers
            text = infos.getEntrez_id(); //KeggAdaptor.extractInfo(infos, "NCBI-GeneID:", "\n"); //adap.getEntrezIDs(ko_id);
            if (text!=null && text.length()!=0) entrezIds2+=(entrezIds2.length()!=0?",":"")+text; //.replace(",", "");
            text = infos.getUniprot_id(); //KeggAdaptor.extractInfo(infos, "UniProt:", "\n"); //adap.getUniprotIDs(ko_id);
            if (text!=null && text.length()!=0) uniprotIds2+=(uniprotIds2.length()!=0?",":"")+text; //.replace(",", "");
            text = infos.getEnsembl_id(); //KeggAdaptor.extractInfo(infos, "Ensembl:", "\n"); //adap.getEnsemblIDs(ko_id);
            if (text!=null && text.length()!=0) ensemblIds2+=(ensemblIds2.length()!=0?",":"")+text; //.replace(",", "");
            
            
            if (eType.length()==0) { // Not yet set
              if (e.getType().equals(EntryType.compound))
                eType = "small molecule";
              else if (e.getType().equals(EntryType.gene))
                eType = "protein";
              else
                eType = e.getType().toString();
            }
            // XXX Fuer Jochens Annotationen (entityType):
            // Jochen:  "protein", "protein in complex", "complex", "RNA", "DNA", "small molecule", "RNA in complex", "DNA in complex", "small molecule in complex", "pathway", "biological process"
            // Mapping: gene/ortholog => protein. Group=>complex, compound=>complex, map=>pathway(/biolog. process)
            //          enzyme & other fehlen.
            // Außerdem: "bindsToChemicals" nicht gesetzt.
          }
        }
        
        // Assign new name based on API and user selection
        name = getNameForEntry(e, keggInfos.toArray(new KeggInfos[0]));
        
        if (name!=null && name.startsWith("undefined") && 
            !graph.getRealizer(n).getLabelText().startsWith("undefined")) {
          // The new name is less meaningfull than the old one. Keep old one.
          // Actualy, this is onle the case for group nodes!
        } else {
          graph.getRealizer(n).setLabelText(name);
        }
        if (hideLabelsForCompounds && e.getType().equals(EntryType.compound)) {
          graph.getRealizer(n).removeLabel(graph.getRealizer(n).getLabel());
        }

        if (isPathwayReference) {
          eType = "pathway";
        }
        
        nodeLabel.set(n, name2);
        nodeDescription.set(n, definition);
        entrezIds.set(n, entrezIds2);
        uniprotIds.set(n, uniprotIds2);
        ensemblIds.set(n, ensemblIds2);
        entityType.set(n, eType);
        
        NodeRealizer nr = graph.getRealizer(n);
        nodeColor.set(n, ColorToHTML(nr.getFillColor()) );
        nodeName.set(n, nr.getLabelText());
        nodePosition.set(n, (int) nr.getX() + "|" + (int) nr.getY());
        nodeSize.set(n, (int) nr.getWidth() + "|" + (int) nr.getHeight());
      }
      keggOntIds.set(n, e.getName().replace(" ", ","));
      if (e.getLink()!=null && e.getLink().length()!=0) nodeURLs.set(n, e.getLink());
      
      if (isPathwayReference) PWReferenceNodeTexts.add(graph.getRealizer(n).getLabelText());
      if (e.hasReaction()) {
        for (String reaction : e.getReactions()) {
          reactionModifiers.put(reaction.toLowerCase().trim(), n);
        }
      }
    }
    
    
    // Make group node hirarchies
    for (int i=0; i<parentGroupNodes.size(); i++) {
      NodeList nl = new NodeList();
      double x=Double.MAX_VALUE,y=Double.MAX_VALUE,width=0,height=0;
      for (int n2: groupNodeChildren.get(i)) {
        Entry two = p.getEntryForId(n2);
        if (two==null) {
          System.out.println("WARNING: Missing node for id " + n2);
          continue;
        }
        Node twoNode = (Node) two.getCustom();
        NodeRealizer nr = graph.getRealizer(twoNode);
        x = Math.min(x, nr.getX());
        y = Math.min(y, nr.getY());
        width=Math.max(width, (nr.getWidth()+nr.getX()));
        height=Math.max(height, (nr.getHeight()+nr.getY()));
        
        nl.add(twoNode);
      }
      
      // Reposition group node to fit content
      if (nl.size()>0) {
        int offset = 5;
        graph.setLocation(parentGroupNodes.get(i), x-offset, y-offset-14);
        graph.setSize(parentGroupNodes.get(i), width-x+2*offset, height-y+2*offset+11);
        
        // Set hierarchy
        hm.setParentNode(nl, parentGroupNodes.get(i));
        
        // Reposition group node to fit content (2nd time is necessary. Maybe yFiles bug...)
        graph.setLocation(parentGroupNodes.get(i), x-offset, y-offset-14);
        graph.setSize(parentGroupNodes.get(i), width-x+2*offset, height-y+2*offset+11);
      }
    }
    
    
    
    // Add Edges for all Relations
    for (int i=0; i<p.getRelations().size(); i++) {
      if (showProgressForRelations) progress.DisplayBar("Relation " + (i+1) + "/" + p.getRelations().size());
      Relation r = p.getRelations().get(i);
      Entry one = p.getEntryForId(r.getEntry1());
      Entry two = p.getEntryForId(r.getEntry2());
      
      if (one==null || two==null) {
        // This happens, e.g. when removing pathways nodes
        // or in general when removing nodes... => below
        // info, because mostly this is wanted by user.
        log.fine("Relation with unknown entry!");
        continue;
      }
      
      Node nOne = (Node) one.getCustom();
      Node nTwo = (Node) two.getCustom();
      
      Edge myEdge;
      // XXX: Hier noch moeglich die Type der reaktion (PPI, etc.) hinzuzufuegen.
      if (r.isSetSubTypes()) {
        for (int stI=0; stI<r.getSubtypes().size(); stI++) {
          SubType st = r.getSubtypes().get(stI);
          EdgeRealizer er = new GenericEdgeRealizer();
          EdgeLabel el=null;
          if (createEdgeLabels) {
            el = new EdgeLabel(st.getName());
            el.setFontSize(8);
            er.addLabel(el);
          }
          
          if (st.getName().trim().equalsIgnoreCase("compound") && Utils.isNumber(st.getValue(),true)) {
            Entry compNode = p.getEntryForId(Integer.parseInt(st.getValue()));
            if (compNode==null || compNode.getCustom()==null) {System.err.println("Could not find Compound Node."); graph.createEdge(nOne, nTwo, er); continue;}
            
            Node compoundNode = (Node) compNode.getCustom();
            
            if (nTwo.getEdgeTo(compoundNode)==null)
              graph.createEdge(nTwo, compoundNode, er);
            if (compoundNode.getEdgeTo(nOne)==null)
              graph.createEdge(compoundNode, nOne, er);
            
            
          } else {
            
            String value = st.getValue();
            if (value!=null) {
              if (value.equals("-->")) {
                er.setTargetArrow((Arrow.STANDARD));
              } else if(value.equals("--|")) {
                er.setTargetArrow((Arrow.T_SHAPE)); // T_SHAPE erst ab yFiles 2.7
              } else if(value.equals("..>")) {
                er.setLineType(LineType.DASHED_1);
                er.setTargetArrow((Arrow.STANDARD));
              } else if(value.equals("...")) {
                er.setLineType(LineType.DASHED_1);
                er.setArrow(Arrow.NONE);
              } else if(value.equals("-+-")) { // Ab 2.7 YFiles Version only...
                er.setLineType(LineType.DASHED_DOTTED_1);
                er.setArrow(Arrow.NONE);
              } else if(value.equals("---")) {
                er.setArrow(Arrow.NONE);
              } else if (value.length()==2 && el!=null) {
                // +p +m und sowas...
                el.setText(st.getValue());
                el.setFontSize(10);
              }
            }
            
            if (st.getEdgeColor()!=null && st.getEdgeColor().length()>0) {
              if (st.getEdgeColor().startsWith("#"))
                er.setLineColor(ColorFromHTML(st.getEdgeColor()));
              else
                System.err.println("Invalid edge color: " + st.getEdgeColor());
            }
            
            if (nOne.getEdgeTo(nTwo)==null) {
              myEdge = graph.createEdge(nOne, nTwo, er);
              
              edgeDescription.set(myEdge, r.getType().toString());
              if (st!=null) interactionDescription.set(myEdge, st.getName()); // => Subtype Name // compound, hidden compound, activation, inhibition, expression, repression, indirect effect, state change, binding/association, dissociation, missing interaction, phosphorylation, dephosphorylation, glycosylation, ubiquitination, methylation 
              // Jochen: "inhibition", "activation", "interaction", "physical interaction", "catalysis", "transcriptional control", "control"
            }
          }
        }
      } else {
        if (nOne.getEdgeTo(nTwo)==null) {
          myEdge = graph.createEdge(nOne, nTwo);
          
          edgeDescription.set(myEdge, r.getType().toString());
        }
      }
      
    }

    // Give a warning if we have no relations.
    if (p.getRelations().size()<1) {
      log.fine("File does not contain any relations. Graph will look quite boring...");
    }
    
    
    // Eventually draw arrows for reactions. These are mostly functional duplicates of the
    // already drawn relations, thus it is not recommended.
    if (drawArrowsForReactions) {
      // I noticed, that some reations occur multiple times in one KGML document,
      // (maybe its intended? e.g. R00014 in hsa00010.xml)
      List<String> processedReactions = new SortedArrayList<String>();
      for (Reaction r : p.getReactions()) {
        if (!processedReactions.contains(r.getName())) {
          addKGMLReaction(r,p,graph,reactionModifiers);
          processedReactions.add(r.getName());
        }
      }
    }
    
    
    // Kanten von Knoten, welche exakt selben In- und Output haben zusammenfassen. (=> Groupnode)
    if (groupNodesWithSameEdges) {
      Node[] myNodes = graph.getNodeArray();
      for (int i=0; i<myNodes.length-1; i++) {
        NodeList nl = new NodeList();
        nl.add(myNodes[i]);
        
        // Nur "Sinnvolle" zusammenfassungen vornehmen.       
        if (hm.isGroupNode(myNodes[i]) || hm.getParentNode(myNodes[i])!=null || !hm.isNormalNode(myNodes[i]) || myNodes[i].edges().size()<1) continue;
        
        for (int j=i+1; j<myNodes.length; j++) {
          if (hm.isGroupNode(myNodes[j]) || hm.getParentNode(myNodes[j])!=null || !hm.isNormalNode(myNodes[j])) continue;
          
          // Wenn in selber (optischer) "Reihe" und selbe kanten, dann groupen.
          if (graph.getRealizer(myNodes[i]).getX()==graph.getRealizer(myNodes[j]).getX() || graph.getRealizer(myNodes[i]).getY()==graph.getRealizer(myNodes[j]).getY())
            if (nodesHaveSameEdges(myNodes[i], myNodes[j], graph))
              nl.add(myNodes[j]);
        }
        
        // Remove Outlier (More than 50px away from closest node)
        nl = removeOutlier(nl,graph, 50);
        
        if (nl.size()>1) {
          // Create new Group node and setup hirarchies
          
          GroupNodeRealizer gnr = (GroupNodeRealizer) setupGroupNode(new NodeLabel(), "");
          
          //gnr.setAutoBoundsInsets(new YInsets(1, 1, 1, 1));
          
          //gnr.setBorderInsets(new YInsets(1, 1, 1, 1));
          
          Node n = graph.createNode(gnr);
          hm.convertToGroupNode(n);
          hm.setParentNode(nl, n);
          
          //gnr.setAutoBoundsInsets(new YInsets(1, 1, 1, 1));
          //gnr.setMinimalInsets(new YInsets(1, 1, 1, 1));
          //gnr.setBorderInsets(new YInsets(1, 1, 1, 1));
          
          // Copy edges to group node
          Node source = ((Node)nl.get(0));
          Edge e = source.firstInEdge();
          while (e!=null) {
            graph.createEdge(e.source(), n, graph.getRealizer(e));
            e=e.nextInEdge();
          }
          e = source.firstOutEdge();
          while (e!=null) {
            graph.createEdge(n, e.target(), graph.getRealizer(e));
            e=e.nextOutEdge();
          }
          // Remove edges from all internal nodes
          for (int j=0; j<nl.size(); j++) {
            source = ((Node)nl.get(j));
            EdgeCursor ec = source.edges();
            while (ec.ok()) {
              graph.removeEdge(ec.edge());
              ec.next();
            }
          }
          gnr.setEdgesDirty();
          
        }
      }
    }
    
    /*
    // Bezieht sich nur auf "Non-map nodes" (Also nicht auf Pathway referenzen).
    public static boolean removeDegreeZeroNodes=true;
    if (removeDegreeZeroNodes) { // Verwaiste knoten loeschen
      Node[] nodes = graph.getNodeArray();
      for (Node n: nodes) {
        if (PWReferenceNodeTexts.contains(graph.getRealizer(n).getLabelText())) continue; // Keine Links auf andere Pathways removen.
        
        if (n.degree()<1) graph.removeNode(n);
      }
    }*/
    
    
    
    // Resize Group Nodes to fit content
    /*
    for (int i=0; i<parentGroupNodes.size(); i++) {
      NodeList nl = new NodeList();
      double x=Double.MAX_VALUE,y=Double.MAX_VALUE,width=0,height=0;
      for (int n2: groupNodeChildren.get(i)) {
        Entry two = p.getEntryForId(n2);
        if (two==null) {
          System.out.println("WARNING: Missing node for id " + n2);
          continue;
        }
        Node twoNode = (Node) two.getCustom();
        NodeRealizer nr = graph.getRealizer(twoNode);
        x = Math.min(x, nr.getX());
        y = Math.min(y, nr.getY());
        width=Math.max(width, (nr.getWidth()+nr.getX()));
        height=Math.max(height, (nr.getHeight()+nr.getY()));
        
        nl.add(twoNode);
      }
      
      // Reposition group node to fit content
      int offset = 5;
      graph.setLocation(parentGroupNodes.get(i), x-offset, y-offset-14);
      graph.setSize(parentGroupNodes.get(i), width-x+2*offset, height-y+2*offset+11);
      
      // Set hirarchie
      hm.setParentNode(nl, parentGroupNodes.get(i));

      // Reposition group node to fit content (2nd time is neccessary. Maybe yFiles bug...)
      graph.setLocation(parentGroupNodes.get(i), x-offset, y-offset-14);
      graph.setSize(parentGroupNodes.get(i), width-x+2*offset, height-y+2*offset+11);
      
    }*/
    
    
    // Finalization: Zoom to fit content, etc.
    // Disables: leads to very low resultions on JPG files.
    //configureView(new Graph2DView(graph));
    
    /*
     * Create a data provider that stores the names of all
     * data providers (Maps).
     */    
    GenericDataMap<DataMap, String> mapDescriptionMap = Graph2Dwriter.addMapDescriptionMapToGraph(graph);
    
    mapDescriptionMap.set(nodeLabel, GraphMLmaps.NODE_LABEL);
    mapDescriptionMap.set(entrezIds, GraphMLmaps.NODE_GENE_ID);
    mapDescriptionMap.set(entityType, GraphMLmaps.NODE_TYPE);
    mapDescriptionMap.set(nodeDescription, GraphMLmaps.NODE_DESCRIPTION);
    mapDescriptionMap.set(keggOntIds, GraphMLmaps.NODE_KEGG_ID);
    mapDescriptionMap.set(uniprotIds, GraphMLmaps.NODE_UNIPROT_ID);
    mapDescriptionMap.set(ensemblIds, GraphMLmaps.NODE_ENSEMBL_ID);
    mapDescriptionMap.set(nodeURLs, GraphMLmaps.NODE_URL);
    mapDescriptionMap.set(nodeColor, GraphMLmaps.NODE_COLOR);
    mapDescriptionMap.set(nodeName, GraphMLmaps.NODE_NAME);
    mapDescriptionMap.set(nodePosition, GraphMLmaps.NODE_POSITION);
    mapDescriptionMap.set(nodeSize, GraphMLmaps.NODE_SIZE);
    
    mapDescriptionMap.set(edgeDescription, GraphMLmaps.EDGE_DESCRIPTION);
    mapDescriptionMap.set(interactionDescription, GraphMLmaps.EDGE_TYPE);
    
    return graph;
  }
  
  /**
   * @param ids
   * @return comma separated list of existing identifiers for the given db,
   * or null of non exists. 
   */
  public String extractExistingIdentifiers(Map<DatabaseIdentifiers.IdentifierDatabases, Collection<String>> ids,
    DatabaseIdentifiers.IdentifierDatabases db) {
    Collection<String> temp = ids.get(db);
    if (temp!=null && temp.size()>0) {
      String union = ArrayUtils.implode(temp, ",");
      while ((union = union.replace(",,", "")).contains(",,"));
      if (union.trim().length()>0) {
        return union;
      }
    }
    return null;
  }

  
  
  /**
   * Set parameters from the graphics object on the
   * {@link NodeRealizer} / {@link NodeLabel}.
   * @param nr
   * @param nl
   * @param g
   */
  private NodeRealizer setupGraphics(NodeRealizer nr, NodeLabel nl, Graphics g) {
    if (nr==null) {
      if (g.getType().equals(GraphicsType.rectangle)) {
        nr = new ShapeNodeRealizer(ShapeNodeRealizer.RECT);
      } else if (g.getType().equals(GraphicsType.circle)) {
        nr = new ShapeNodeRealizer(ShapeNodeRealizer.ELLIPSE);
        nl.setFontSize(10); // looks better on small ellipses
      } else if (g.getType().equals(GraphicsType.roundrectangle)) {
        nr = new ShapeNodeRealizer(ShapeNodeRealizer.ROUND_RECT);
      } else if (g.getType().equals(GraphicsType.line) && g.isSetCoords()) {
        nr = new LineNodeRealizer();
      } else {
        nr = new GenericNodeRealizer();
      }
    }
    
    // Colors
    try {
      if (g.isSetBGcolor())
        nr.setFillColor(ColorFromHTML(g.getBgcolor()));
    } catch (Throwable t) {t.printStackTrace();}
    
    try {
      if (g.isSetFGcolor()) {
        Color color = ColorFromHTML(g.getFgcolor());
        nl.setTextColor(color);
        if (nr instanceof LineNodeRealizer) {
          nr.setFillColor(color);
        }
      } 
    } catch (Throwable t) {t.printStackTrace();}
    
    // Coordinates
    if (g.isSetCoords() && (nr instanceof LineNodeRealizer)) {
      // Is it a line/spline
      Integer[] coords = g.getCoords();
      for (int j=0; j<(coords.length-1); j+=2) {
        ((LineNodeRealizer)nr).addSplineCoords(coords[j], coords[j+1]);
      }
      
    } else {
      // ... or a (simple) node
      if (g.getWidth()>0 || g.getHeight()>0) {
        nr.setWidth(g.getWidth());
        nr.setHeight(g.getHeight());
      }
      nr.setCenter(g.getX(), g.getY());
    }
    
    return nr;
  }
  
  
  /**
   * @param r
   * @param p
   * @param graph
   * @param reactionModifiers
   */
  private void addKGMLReaction(Reaction r, Pathway p, Graph2D graph, Map<String, Node> reactionModifiers) {
    if (!reactionHasAtLeastOneSubstrateAndProduct(r, p)) return;
    
    EdgeRealizer er = new GenericEdgeRealizer();
    er.setTargetArrow(Arrow.STANDARD);
    er.setLineColor(Color.LIGHT_GRAY);
    if (r.getType().equals(ReactionType.reversible)) {
      er.setSourceArrow(Arrow.STANDARD);
    }
    
    
    // yFiles only provides 1:1 relations => Create edge for every combination.
    for (ReactionComponent sub:r.getSubstrates()) {
      Entry one = p.getEntryForReactionComponent(sub);
      for (ReactionComponent prod:r.getProducts()) {
        Entry two = p.getEntryForReactionComponent(prod);
        if (one==null || two==null) continue;        
        
        // Get nodes, corresponding to entries
        Node nOne = (Node) one.getCustom();
        Node nTwo = (Node) two.getCustom();
        
        // Create edge, if not existent.
        if (nOne.getEdgeTo(nTwo)==null) {
          graph.createEdge(nOne, nTwo, er);
        }
        
        // Consider reaction modifiers
        Node modifier = reactionModifiers.get(r.getName().toLowerCase().trim());
        if (modifier!=null && modifier.getEdgeTo(nTwo)==null) {
          er.setSourceArrow(Arrow.NONE);
          er.setTargetArrow(Arrow.TRANSPARENT_CIRCLE);
          graph.createEdge(modifier, nTwo, er);
        }
      }
    }
    
  }
  
  
  /**
   * Convenient method to create a new KEGG2GraphML translator.
   * @param manager - KeggInfoManagement to use for retrieving
   * annotations.
   * @return KEGGtranslator (KEGG2yGraph, yFiles implementation)
   */
  public static KEGG2yGraph createKEGG2GraphML(KeggInfoManagement manager) {
    return new KEGG2yGraph(new GraphMLIOHandler(), manager);
  }
  
  /**
   * Convenient method to create a new KEGG2GML translator.
   * @param manager - KeggInfoManagement to use for retrieving
   * annotations.
   * @return KEGGtranslator (KEGG2yGraph, yFiles implementation)
   */
  public static KEGG2yGraph createKEGG2GML(KeggInfoManagement manager) {
    return new KEGG2yGraph(new GMLIOHandler(), manager);
  }
  
  /**
   * Convenient method to create a new KEGG2SVG translator.
   * <p>Requires the yFiles SVG extension libraries on the projects
   * build path!
   * @param manager - KeggInfoManagement to use for retrieving
   * annotations.
   * @return KEGGtranslator (KEGG2yGraph, yFiles implementation)
   */
  public static KEGG2yGraph createKEGG2SVG(KeggInfoManagement manager) {
    IOHandler ioh = Graph2Dwriter.createSVGIOHandler();
    if (ioh!=null) {      
      return new KEGG2yGraph(ioh, manager);
    } else {
      return null;
    }
  }
  
  /**
   * Convenient method to create a new KEGG2JPG translator.
   * @param manager - KeggInfoManagement to use for retrieving
   * annotations.
   * @return KEGGtranslator (KEGG2yGraph, yFiles implementation)
   */
  public static KEGG2yGraph createKEGG2JPG(KeggInfoManagement manager) {
    return new KEGG2yGraph(new JPGIOHandler(), manager);
  }
  
  /**
   * Convenient method to create a new KEGG2GIF translator.
   * @param manager - KeggInfoManagement to use for retrieving
   * annotations.
   * @return KEGGtranslator (KEGG2yGraph, yFiles implementation)
   */
  public static KEGG2yGraph createKEGG2GIF(KeggInfoManagement manager) {
    return new KEGG2yGraph(new GIFIOHandler(), manager);
  }
  
  /**
   * Convenient method to create a new KEGG2TGF translator.
   * @param manager - KeggInfoManagement to use for retrieving
   * annotations.
   * @return KEGGtranslator (KEGG2yGraph, yFiles implementation)
   */
  public static KEGG2yGraph createKEGG2TGF(KeggInfoManagement manager) {
    return new KEGG2yGraph(new TGFIOHandler(), manager);
  }
  
  /**
   * Convenient method to create a new KEGG2YGF translator.
   * @param manager - KeggInfoManagement to use for retrieving
   * annotations.
   * @return KEGGtranslator (KEGG2yGraph, yFiles implementation)
   */
  public static KEGG2yGraph createKEGG2YGF(KeggInfoManagement manager) {
    return new KEGG2yGraph(new YGFIOHandler(), manager);
  }
  
  
  /**
   * @param document
   * @param path
   * @param format output file extension, e.g., "gif", "graphml", "gml", "jpg",...
   * @throws Exception 
   * @return true if everything went fine.
   */
  public boolean writeToFile(Graph2D graph, String outFile, String format) throws Exception {
    return outputHandler.writeToFile(graph, outFile, format);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.AbstractKEGGtranslator#writeToFile(java.lang.Object, java.lang.String)
   */
  @Override
  public boolean writeToFile(Graph2D doc, String outFile) {
    return outputHandler.writeToFile(doc, outFile);
  }
  
  @Override
  protected boolean considerRelations() {
    return true;
  }
  
  @Override
  protected boolean considerReactions() {
    // Actually false, but the user might want to draw some arrows here.
    // Return value is important, e.g. for the "remove orphans" feature.
    return drawArrowsForReactions;
  }
  
  
}
