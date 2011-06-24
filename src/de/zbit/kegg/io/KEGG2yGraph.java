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
import java.awt.Rectangle;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

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
import y.io.graphml.GraphMLHandler;
import y.io.graphml.KeyScope;
import y.io.graphml.KeyType;
import y.io.graphml.graph2d.Graph2DGraphMLHandler;
import y.view.Arrow;
import y.view.EdgeLabel;
import y.view.EdgeRealizer;
import y.view.GenericEdgeRealizer;
import y.view.GenericNodeRealizer;
import y.view.Graph2D;
import y.view.Graph2DView;
import y.view.LineType;
import y.view.NodeLabel;
import y.view.NodeRealizer;
import y.view.ShapeNodeRealizer;
import y.view.hierarchy.GroupNodeRealizer;
import y.view.hierarchy.HierarchyManager;
import de.zbit.kegg.KeggInfoManagement;
import de.zbit.kegg.KeggInfos;
import de.zbit.kegg.KEGGtranslatorOptions;
import de.zbit.kegg.ext.GenericDataMap;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.kegg.parser.KeggParser;
import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.EntryType;
import de.zbit.kegg.parser.pathway.Graphics;
import de.zbit.kegg.parser.pathway.GraphicsType;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.kegg.parser.pathway.Relation;
import de.zbit.kegg.parser.pathway.SubType;
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
   * Important: This determines the output format. E.g. a GraphMLIOHandler
   * will write a graphML file, a GMLIOHandler will write a GML file.
   * 
   */
  private IOHandler outputHandler = null;
  
  /**
   * This is used internally to identify a certain dataHandler in the Graph document.
   * The content is not important, it should just be any defined static final string.
   */
  public final static String mapDescription="-MAP_DESCRIPTION-";
  
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
    super(manager);
    this.outputHandler = outputHandler;
    
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
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.AbstractKEGGtranslator#isOutputFunctional()
   */
  @Override
  public boolean isOutputFunctional() {
    return false;
  }
  /**
   * The IOHandler determines the output format.
   * May be GraphMLIOHandler or GMLIOHandler,...
   * @return
   */
  public IOHandler getOutputHandler() {
    return outputHandler;
  }
  /**
   * Set the outputHander to use when writing the file.
   * May be GraphMLIOHandler or GMLIOHandler,...
   * See also: {@link #outputHandler}
   * @param outputHandler
   */
  public void setOutputHandler(IOHandler outputHandler) {
    this.outputHandler = outputHandler;
  }
  
  /*===========================
   * FUNCTIONS
   * ===========================*/
  
  /** Load the default preferences from the SBPreferences object. */
  private void loadPreferences() {
  	groupNodesWithSameEdges = KEGGtranslatorOptions.MERGE_NODES_WITH_SAME_EDGES.getValue(prefs);
  	createEdgeLabels = KEGGtranslatorOptions.CREATE_EDGE_LABELS.getValue(prefs);
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
   * Configures the view that is used as rendering environment for some output
   * formats.
   * @param view - Graph2DView
   */
  private static void configureView(Graph2DView view) {
    Graph2D graph = view.getGraph2D();
    Rectangle box = graph.getBoundingBox();
    // Dimension dim = //inBox.getSize();
    // view.setPreferredSize(dim);
    // view.setSize(dim);
    view.zoomToArea(box.getX() - 10, box.getY() - 10, box.getWidth() + 20, box.getHeight() + 20);
    view.fitContent();
    view.setPaintDetailThreshold(0.0); // never switch to less detail mode
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
  private static NodeRealizer setupGroupNode(NodeLabel nl, String changeCaption) {
    GroupNodeRealizer nr = new GroupNodeRealizer();
    ((GroupNodeRealizer)nr).setGroupClosed(false);
    nr.setTransparent(true);
    
    if (changeCaption!=null) {
      String newText = changeCaption; //"Group";
      nl.setText(newText);
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
    //NodeMap bindsToChemicals = graph.createNodeMap();
    
    EdgeMap edgeDescription = graph.createEdgeMap(); // = Relation.type
    EdgeMap interactionDescription = graph.createEdgeMap(); // = Subtype.name "inhibition", "activation", "interaction", "physical interaction", "catalysis", "transcriptional control", "control"
    //EdgeMap interactionType = graph.createEdgeMap(); // "control", "catalysis", "physical interaction", "transport", "complex assembly", "biochemical reaction"
    //EdgeMap edgeURLs = graph.createEdgeMap();
    
    
    // Set the link
    if (p.getLink()!=null && p.getLink().length()!=0) {
      try {
        graph.setURL(new URL(p.getLink()));
      } catch (MalformedURLException e1) {
        e1.printStackTrace();
      }
    }
    ArrayList<Node> parentGroupNodes = new ArrayList<Node>();
    ArrayList<ArrayList<Integer>> groupNodeChildren = new ArrayList<ArrayList<Integer>>();
    HierarchyManager hm = graph.getHierarchyManager();
    if (hm==null) hm = new HierarchyManager(graph);
    
    // Initialize a progress bar.
    initProgressBar(p,showProgressForRelations,false);
    
    // Add nodes for all Entries
    for (int i=0; i<p.getEntries().size(); i++) {
      progress.DisplayBar("Node " + (i+1) + "/" + p.getEntries().size());
      Entry e = p.getEntries().get(i);
      if (skipCompounds && e.getType().equals(EntryType.compound)) continue;
      
      Node n = null;
      boolean isPathwayReference=false;
      String name = e.getName().trim();
      if (name.toLowerCase().startsWith("path:") || e.getType().equals(EntryType.map)) isPathwayReference=true;
      
      
      // Get the graphics object, create a default one or skip this entry.
      Graphics g = null;
      if (e.hasGraphics()) {
        g = e.getGraphics();
      } else if (showEntriesWithoutGraphAttribute || autocompleteReactions) {
        g = new Graphics(e);
      }
      
      // If it should get drawed, then create this node.
      if (g!=null) {
        /* Example:
         *     <entry id="16" name="ko:K04467 ko:K07209 ko:K07210" type="ortholog">
                 <graphics name="IKBKA..." fgcolor="#000000" bgcolor="#FFFFFF"
                   type="rectangle" x="785" y="141" width="45" height="17"/>
                   
         *   ... is actually a compund!?!?
         */
        
        // Get name, description and other annotations via api (organism specific) possible!!
        //Graphics g = e.getGraphics();
        if (g.getName().length()!=0)
          name = g.getName(); // + " (" + name + ")"; // Append ko Id(s) possible!
        /*if (g.getWidth()>0 && g.getHeight()>0) {
          n = graph.createNode(g.getX(), g.getY(), g.getWidth(), g.getHeight(), name);
        } else {
          n = graph.createNode(g.getX(), g.getY(), name);
        }*/
        
        
        
        
        NodeRealizer nr;
        NodeLabel nl = new NodeLabel(name);
        
        // one of "rectangle, circle, roundrectangle, line, other"
        boolean addThisNodeToGorupNodeList=false;
        if (isGroupNode(e)) {
          groupNodeChildren.add(e.getComponents());
          addThisNodeToGorupNodeList = true;
          
          
          // New Text
          String newText = null;
          if (nl.getText().length()==0 || nl.getText().equals("undefined")) {
            /*String newText = "Group of: " + "\n";
            char sep = ',';
            for (Integer i2: e.getComponents()) {
              Entry e2 = p.getEntryForId(i2);
              if (e2==null) {
                newText+=i2+sep;
              } else if (e2.hasGraphics() && !e2.getGraphics().getName().isEmpty()) {
                newText+=e2.getGraphics().getName()+sep;
              } else {
                newText+=e2.getName()+sep;
              }
            }*/
            newText = "Group";
          }
          nr = setupGroupNode(nl, newText);
        } else if (g.getType().equals(GraphicsType.rectangle)) {
          nr = new ShapeNodeRealizer(ShapeNodeRealizer.RECT);
        } else if (g.getType().equals(GraphicsType.circle)) {
          nr = new ShapeNodeRealizer(ShapeNodeRealizer.ELLIPSE);
          nl.setFontSize(10); // looks better on small ellipses
        } else if (g.getType().equals(GraphicsType.roundrectangle)) {
          nr = new ShapeNodeRealizer(ShapeNodeRealizer.ROUND_RECT);
          //} else if (g.getType().toString().equals("line")) { // Vielleicht in neuerer yFiles version line verfuegbar?
          //nr = new ShapeNodeRealizer(ShapeNodeRealizer.TRAPEZOID);
        } else
          nr = new GenericNodeRealizer();
        
        try {
          if (g.isBGcolorSet())
            nr.setFillColor(ColorFromHTML(g.getBgcolor()));
        } catch (Throwable t) {t.printStackTrace();}
        
        try {
          if (g.isBGcolorSet())
            nl.setTextColor(ColorFromHTML(g.getFgcolor()));
        } catch (Throwable t) {t.printStackTrace();}
        
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
            nl.setUserData(new URL(link));
          } catch (MalformedURLException e1) {
            nl.setUserData(link);
          }
        }
        
        nr.setX(g.getX());
        nr.setY(g.getY());
        if (g.getWidth()>0 || g.getHeight()>0) {
          nr.setWidth(g.getWidth());
          nr.setHeight(g.getHeight());
        }
        
        
        n = graph.createNode(nr);
        if (addThisNodeToGorupNodeList) {
          hm.convertToGroupNode(n);
          parentGroupNodes.add(n);
        }
        
      }
      
      
      // Node was created. Postprocessing options.
      if (n!=null) {
        // Remember node in KEGG Structure for further references.
        e.setCustom(n);
        
        
        // KeggAdaptor.
        boolean hasMultipleIDs = false;
        if (e.getName().trim().contains(" ")) hasMultipleIDs = true;
        
        String name2="",definition="",entrezIds2="",uniprotIds2="",eType="",ensemblIds2="";
        for (String ko_id:e.getName().split(" ")) {
          //Definition[] results = adap.getGenesForKO(e.getName(), retrieveKeggAnnotsForOrganism); // => GET only (und alles aus GET rausparsen). Zusaetzlich: in sortedArrayList merken.
          if (ko_id.trim().equalsIgnoreCase("undefined") || e.hasComponents()) continue;
          
          KeggInfos infos = new KeggInfos(ko_id, manager);
          
          // "NCBI-GeneID:","UniProt:", "Ensembl:", ... aus dem GET rausparsen
          if (infos.queryWasSuccessfull()) {
            String oldText=graph.getRealizer(n).getLabelText();
            
            // KEGG always provides multiple names for an entry.
            String exName = infos.getNames();            
            if (exName!=null && exName.length()!=0) {
              
              if (!showShortNames) {
                // Take last name (mostly very descriptive)
                int pos = exName.lastIndexOf(";");
                if (pos>0 && pos<(exName.length()-1)) exName = exName.substring(pos+1, exName.length()).replace("\n", "").trim();
              } else {
                // Choose the shortest name.
                String[] multiNames = exName.split(";");
                for(String cname:multiNames) {
                  cname = cname.replace("\n", "").trim();
                  if (cname.length()>0 && cname.length()<exName.length()) {
                    exName = cname;
                  }
                }
              }
              
              // Eventually assign new name.
              if (!hasMultipleIDs) // Knotennamen nur anpassen, falls nicht mehrere IDs.
                graph.getRealizer(n).setLabelText(exName);
              else if (oldText.length()==0) // ... oder wenn er bisher leer ist.
                graph.getRealizer(n).setLabelText(exName);
            }
            
            
            
            String text = infos.getNames();
            if (text!=null && text.length()!=0) name2+=(name2.length()!=0?",":"")+text.replace(",", "");
            
            if (e.getType().equals(EntryType.map)) { // => Link zu anderem Pathway oder Title-Node des aktuellem PW.
              text = infos.getDescription();
              if (text!=null && text.length()!=0) definition+=(definition.length()!=0?",":"")+text.replace(",", "").replace("\n", " ");
            } else {
              text = infos.getDefinition();
              if (text!=null && text.length()!=0) definition+=(definition.length()!=0?",":"")+text.replace(",", "").replace("\n", " ");
            }
            
            text = infos.getEntrez_id(); //KeggAdaptor.extractInfo(infos, "NCBI-GeneID:", "\n"); //adap.getEntrezIDs(ko_id);
            if (text!=null && text.length()!=0) entrezIds2+=(entrezIds2.length()!=0?",":"")+text; //.replace(",", "");
            text = infos.getUniprot_id(); //KeggAdaptor.extractInfo(infos, "UniProt:", "\n"); //adap.getUniprotIDs(ko_id);
            if (text!=null && text.length()!=0) uniprotIds2+=(uniprotIds2.length()!=0?",":"")+text; //.replace(",", "");
            text = infos.getEnsembl_id(); //KeggAdaptor.extractInfo(infos, "Ensembl:", "\n"); //adap.getEnsemblIDs(ko_id);
            if (text!=null && text.length()!=0) ensemblIds2+=(ensemblIds2.length()!=0?",":"")+text; //.replace(",", "");
            
            ////eType+=(!eType.length()==0?",":"")+e.getType().toString();
            //eType+=(!eType.length()==0?",":"");
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
        
      }
      keggOntIds.set(n, e.getName().replace(" ", ","));
      if (e.getLink()!=null && e.getLink().length()!=0) nodeURLs.set(n, e.getLink());
      
      if (isPathwayReference) PWReferenceNodeTexts.add(graph.getRealizer(n).getLabelText());
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
        
        // Set hirarchie
        hm.setParentNode(nl, parentGroupNodes.get(i));
        
        // Reposition group node to fit content (2nd time is neccessary. Maybe yFiles bug...)
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
        System.out.println("Relation with unknown entry!");
        continue;
      }
      
      Node nOne = (Node) one.getCustom();
      Node nTwo = (Node) two.getCustom();
      
      Edge myEdge;
      // XXX: Hier noch moeglich die Type der reaktion (PPI, etc.) hinzuzufuegen.
      if (r.getSubtypes()!=null && r.getSubtypes().size()>0) {
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
      System.err.println("Warning: File does not contain any relations. Graph will look quite boring...");
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
    
    // Shorten name
    if (showShortNames) {
      graph = modifyNodeLabels(graph,nodeName,false,true);
    }
    
    
    /*
     * Create a data provider that stores the names of all
     * data providers (Maps).
     */
    GenericDataMap<DataMap, String> mapDescriptionMap = new GenericDataMap<DataMap, String>(mapDescription);
    graph.addDataProvider(mapDescription, mapDescriptionMap);
    
    mapDescriptionMap.set(nodeLabel, "nodeLabel");
    mapDescriptionMap.set(entrezIds, "entrezIds"); // String "entrezIds" is also hardcoded in TranslatorTools.java!
    mapDescriptionMap.set(entityType, "type");
    mapDescriptionMap.set(nodeDescription, "description");
    mapDescriptionMap.set(keggOntIds, "keggIds");
    mapDescriptionMap.set(uniprotIds, "uniprotIds");
    mapDescriptionMap.set(ensemblIds, "ensemblIds");
    mapDescriptionMap.set(nodeURLs, "url");
    mapDescriptionMap.set(nodeColor, "nodeColor");
    mapDescriptionMap.set(nodeName, "nodeName");
    mapDescriptionMap.set(nodePosition, "nodePosition");
    mapDescriptionMap.set(edgeDescription, "description");
    mapDescriptionMap.set(interactionDescription, "interactionType");
    
    return graph;
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.AbstractKEGGtranslator#writeToFile(java.lang.Object, java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean writeToFile(Graph2D graph, String outFile) {
    
    // initialize and check the IOHandler
    if (outputHandler==null) {
      outputHandler = new GraphMLIOHandler(); // new GMLIOHandler();
    }
    if (!outputHandler.canWrite()) {
      System.err.println("Y OutputHandler can't write.");
      return false;
    }
    
    // Try to set metadata annotations
    if (outputHandler instanceof GraphMLIOHandler) {
      Graph2DGraphMLHandler ioh = ((GraphMLIOHandler) outputHandler).getGraphMLHandler() ;
      
      try {
        GenericDataMap<DataMap, String> mapDescriptionMap = (GenericDataMap<DataMap, String>) graph.getDataProvider(mapDescription);
        
        EdgeMap[] eg = graph.getRegisteredEdgeMaps();
        if (eg!=null)
          for (int i=0; i<eg.length;i++)
            addDataMap(eg[i], ioh, mapDescriptionMap.getV(eg[i]));
        NodeMap[] nm = graph.getRegisteredNodeMaps();
        if (nm!=null)
          for (int i=0; i<nm.length;i++)
            addDataMap(nm[i], ioh, mapDescriptionMap.getV(nm[i]));
        
      } catch(Throwable e) {
        System.err.println("Cannot write annotations to graph file.");
        e.printStackTrace();
      }
      
      
      
    }
    // ----------------
    
    // Zoom by default to fit content in graphML
    if (outputHandler instanceof GraphMLIOHandler ||
        outputHandler instanceof GMLIOHandler ||
        outputHandler instanceof YGFIOHandler) {
      configureView(new Graph2DView(graph));
    }
    
    // => Moved to a global setting.
    //if (outputHandler instanceof JPGIOHandler) {
      //graph = modifyNodeLabels(graph,true,true);
    //}
    
    // Try to write the file.
    int retried=0;
    if (new File(outFile).exists()) lastFileWasOverwritten=true;
    while (retried<3) {
      try {
      	
        // Create a specific ouputStream, that removes all
      	// y-Files-is-the-man--poser-strings.
        OutputStream out = null;
        if (outputHandler instanceof GraphMLIOHandler ||
            outputHandler instanceof GMLIOHandler) {
        	out = new YFilesWriter(new BufferedOutputStream(new FileOutputStream(outFile)));
        }
        
        if (out==null)
          outputHandler.write(graph, outFile);
        else {
      	  outputHandler.write(graph, out);
      	  out.close();
        }
      	
        return true; // Success => No more need to retry
      } catch (IOException iex) {
        retried++;
        if (retried>2) {
          System.err.println("Error while encoding file " + outFile + "\n" + iex);
          iex.printStackTrace();
          break;
        }
      }
    }
    return false;
  }
  
  /**
   * Modifies all labels of all nodes in the given graph.
   * @param g - the graph to modiy
   * @param nodeName - a list to store the node names separately
   * @param removeSpeciesTitles -
   * convert e.g. "Citrate cycle (TCA cycle) - Homo sapiens (human)" => "Citrate cycle (TCA cycle)"
   * BE CAREFUL WITH THAT: "HNF-4" is also converted to "HNF"!
   * @param removeMultipleNodeNames -
   * convert e.g. "PCK1, MGC22652, PEPCK-C, PEPCK1, PEPCKC..." => "PCK1"
   * @return Graph2D g
   */
  public static Graph2D modifyNodeLabels(Graph2D g, NodeMap nodeName, boolean removeSpeciesTitles, boolean removeMultipleNodeNames) {
    for (y.base.Node n:g.getNodeArray()) {
      String t = g.getLabelText(n);
      // Convert "Citrate cycle (TCA cycle) - Homo sapiens (human)" => "Citrate cycle (TCA cycle)"
      if (removeSpeciesTitles && t.contains("-")) {
        t = t.substring(0, t.indexOf("-")-1);
        g.setLabelText(n, t);
        if (nodeName!=null) nodeName.set(n, t);
      }
      
      // Convert "PCK1, MGC22652, PEPCK-C, PEPCK1, PEPCKC..." => "PCK1"
      if (removeMultipleNodeNames) {
        t = shortenName(t);
        g.setLabelText(n, t);
        if (nodeName!=null) nodeName.set(n, t);
      }
    }
    return g;
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
   * @param args
   * @throws IOException 
   */
  public static void main(String[] args) throws IOException {
    KeggInfoManagement manager;
    if (new File(KEGGtranslator.cacheFileName).exists()
        && new File(KEGGtranslator.cacheFileName).length() > 0) {
      manager = (KeggInfoManagement) KeggInfoManagement.loadFromFilesystem(KEGGtranslator.cacheFileName);
    } else {
      manager = new KeggInfoManagement();
    }
    KEGG2yGraph k2g = createKEGG2GraphML(manager);
    // ---
    
    if (args != null && args.length > 0) {
      File f = new File(args[0]);
      if (f.isDirectory()) {
        // Directory mode. Convert all files in directory.
        BatchKEGGtranslator batch = new BatchKEGGtranslator();
        batch.setOrgOutdir(args[0]);
        if (args.length > 1)
          batch.setChangeOutdirTo(args[1]);
        batch.setTranslator(k2g);
        batch.setOutFormat(Format.GraphML);
        batch.parseDirAndSubDir();
        
      } else {
        // Single file mode.
        String outfile = args[0].substring(0,
            args[0].contains(".") ? args[0].lastIndexOf(".") : args[0].length())
            + "." + k2g.getOutputHandler().getFileNameExtension();
        if (args.length > 1) outfile = args[1];
        
        Pathway p = KeggParser.parse(args[0]).get(0);
        k2g.translate(p, outfile);
      }
      
      // Remember already queried objects (save cache)
      if (k2g.getKeggInfoManager().hasChanged()) {
        KeggInfoManagement.saveToFilesystem(KEGGtranslator.cacheFileName, k2g.getKeggInfoManager());
      }
      
      return;
    }

    // Just a few test cases here.
    System.out.println("Demo mode.");
    
    long start = System.currentTimeMillis();
    try {
      // hsa04310 04115 hsa04010
      k2g.translate("files/KGMLsamplefiles/hsa04010.xml", "files/KGMLsamplefiles/hsa04010." + k2g.getOutputHandler().getFileNameExtension());
      //k2g.translate("files/KGMLsamplefiles/hsa00010.xml", "files/KGMLsamplefiles/hsa00010." + k2g.getOutputHandler().getFileNameExtension());
      
      // Remember already queried objects
      if (k2g.getKeggInfoManager().hasChanged()) {
        KeggInfoManagement.saveToFilesystem(KEGGtranslator.cacheFileName, k2g.getKeggInfoManager());
      }
      
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    
    System.out.println("Conversion took "+Utils.getTimeString((System.currentTimeMillis() - start)));
  }
  
  /**
   * Add the given DataMap (e.g. NodeMap) to the given GraphHandler, 
   * using the given description.
   * Adds the map as InputDataAcceptor and OutputDataProvider.
   * Keytype will be set to KeyType.STRING by this function.
   * @param nm - the DataMap (NodeMap / EdgeMap)
   * @param ioh - the GraphHandler (e.g. Graph2DGraphMLHandler)
   * @param desc - the Description of the map
   * @param scope - KeyScope (e.g. KeyScope.NODE)
   */
  private static void addDataMap(DataMap nm, GraphMLHandler ioh, String desc) {
    KeyScope scope;
    if (nm instanceof NodeMap)scope = KeyScope.NODE;
    else if (nm instanceof EdgeMap)scope = KeyScope.EDGE;
    else scope = KeyScope.ALL;
    
    addDataMap(nm, ioh, desc, KeyType.STRING, scope);//AttributeConstants.TYPE_STRING
  }
  
  /**
   * Add the given DataAcceptor (e.g. NodeMap) to the given GraphHandler, 
   * using the given description.
   * Adds the map as InputDataAcceptor and OutputDataProvider.
   * @param nm - the DataAcceptor (NodeMap / EdgeMap)
   * @param ioh - the GraphHandler (e.g. Graph2DGraphMLHandler)
   * @param desc - Description
   * @param keytype - KeyType (e.g. KeyType.STRING)
   * @param scope - KeyScope (e.g. KeyScope.NODE)
   */
  private static void addDataMap(DataMap nm, GraphMLHandler ioh, String desc, KeyType keytype, KeyScope scope) {
    ioh.addInputDataAcceptor (desc, nm, scope, keytype);
    ioh.addOutputDataProvider(desc, nm, scope, keytype);
    //ioh.addAttribute(nm, desc, keytype);    // <= yf 2.6
  }
  
  /**
   * @param document
   * @param path
   * @param format
   * @throws Exception 
   */
  public void writeToFile(Graph2D graph, String outFile, String format) throws Exception {
    IOHandler io;
    if (format.equalsIgnoreCase("gif")) {
      io = new GIFIOHandler();
    } else if (format.equalsIgnoreCase("graphml")) {
      io = new GraphMLIOHandler();
    } else if (format.equalsIgnoreCase("gml")) {
      io = new GMLIOHandler();
    } else if (format.equalsIgnoreCase("ygf")) {
      io = new YGFIOHandler();
    } else if (format.equalsIgnoreCase("tgf")) {
      io = new TGFIOHandler();
    } else if (format.equalsIgnoreCase("jpg") || format.equalsIgnoreCase("jpeg")) {
      io = new JPGIOHandler();
    } else {
      throw new Exception("Unknown output format.");
    }
    setOutputHandler(io);
    writeToFile(graph, outFile);
  }
  
}
