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
package de.zbit.kegg.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileFilter;

import org.sbml.jsbml.AbstractNamedSBase;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.SimpleSpeciesReference;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.ext.SBasePlugin;
import org.sbml.jsbml.ext.layout.BoundingBox;
import org.sbml.jsbml.ext.layout.ExtendedLayoutModel;
import org.sbml.jsbml.ext.layout.Layout;
import org.sbml.jsbml.ext.layout.ReactionGlyph;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;
import org.sbml.jsbml.ext.qual.Input;
import org.sbml.jsbml.ext.qual.Output;
import org.sbml.jsbml.ext.qual.QualitativeModel;
import org.sbml.jsbml.ext.qual.Sign;
import org.sbml.jsbml.ext.qual.Transition;

import y.base.DataMap;
import y.base.Edge;
import y.base.Node;
import y.base.NodeMap;
import y.layout.organic.SmartOrganicLayouter;
import y.view.Arrow;
import y.view.EdgeRealizer;
import y.view.Graph2D;
import y.view.HitInfo;
import y.view.LineType;
import y.view.NodeRealizer;
import de.zbit.graph.CloneMarker;
import de.zbit.graph.ReactionNodeRealizer;
import de.zbit.gui.GUITools;
import de.zbit.gui.layout.LayoutHelper;
import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.kegg.ext.GenericDataMap;
import de.zbit.kegg.ext.GraphMLmaps;
import de.zbit.kegg.ext.SBMLVisualizationProperties;
import de.zbit.kegg.io.KEGG2SBMLLayoutExtension;
import de.zbit.kegg.io.KEGG2SBMLqual;
import de.zbit.kegg.io.KEGG2jSBML;
import de.zbit.kegg.io.KEGG2yGraph;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.math.MathUtils;
import de.zbit.sbml.gui.SBasePanel;
import de.zbit.util.TranslatorTools;
import de.zbit.util.objectwrapper.ValuePair;

/**
 * A basic panel which uses a GraphLayer to visualize SBML documents.
 * SBML-visualization is performed according to the SBGN
 * process descriptions.<br/>
 * <img src="http://www.sbgn.org/images/4/4a/Refcard-PD.png"/>
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class TranslatorSBMLgraphPanel extends TranslatorGraphLayerPanel<SBMLDocument> {
  private static final long serialVersionUID = 2361032893527709646L;
  
  /**
   * If false, shows the normal, quantitative SBML model. If true,
   * shows the qual model.
   */
  private boolean showQualModel=false;
  
  /**
   * Use this hashmap to map every graph-object
   * to an SBML-identifier.
   */
  private Map<Object, String> GraphElement2SBMLid = new HashMap<Object, String>();
  
  /**
   * Clone enzymes in a way that exclusively one enzyme copy is
   * available for each reaction.
   * <p>Later, we may create an option for that...
   */
  private boolean splitEnzymesToOnlyOccurOnceInAnyReaction = true;


  /**
   * Create a new translator-panel and initiates the translation.
   * @param inputFile
   * @param outputFormat
   * @param translationResult
   */
  public TranslatorSBMLgraphPanel(File inputFile, ActionListener translationResult) {
    this(inputFile, Format.SBML, translationResult);
  }

  /**
   * Create a new translator-panel and initiates the translation.
   * @param inputFile
   * @param outputFormat
   * @param translationResult
   */
  public TranslatorSBMLgraphPanel(File inputFile, Format outputFormat, ActionListener translationResult) {
    super(inputFile, outputFormat, translationResult);
  }

  /**
   * Initiates a download and translation of the given pathway.
   * @param pathwayID pathway identifier (e.g., "mmu00010")
   * @param outputFormat
   * @param translationResult
   */
  public TranslatorSBMLgraphPanel(String pathwayID, Format outputFormat, ActionListener translationResult) {
    super(pathwayID, outputFormat, translationResult);
  }
  
  /**
   * Initiates a download and translation of the given pathway.
   * @param pathwayID pathway identifier (e.g., "mmu00010")
   * @param translationResult
   */
  public TranslatorSBMLgraphPanel(String pathwayID, ActionListener translationResult) {
    this(pathwayID, Format.SBML, translationResult);
  }
  
  public TranslatorSBMLgraphPanel(File inputFile, Format outputFormat, ActionListener translationResult, SBMLDocument document) {
    this(inputFile, outputFormat, translationResult, document, false);
  }
  
  public TranslatorSBMLgraphPanel(File inputFile, Format outputFormat, ActionListener translationResult, SBMLDocument document, boolean showQualModel) {
    super(inputFile, outputFormat, translationResult, document);
    this.showQualModel = showQualModel;
    
    try {
      createTabContent();
    } catch (Throwable e) {
      GUITools.showErrorMessage(null, e);
      fireActionEvent(new ActionEvent(this,JOptionPane.ERROR,TranslatorUI.Action.TRANSLATION_DONE.toString()));
      return;
    }
  }



  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorGraphLayerPanel#createGraphFromDocument(java.lang.Object)
   */
  @Override
  protected Graph2D createGraphFromDocument(SBMLDocument document) {
    Graph2D simpleGraph = new Graph2D();
    if (document==null || !document.isSetModel()) return simpleGraph;
    
    // Get list of species
    List<? extends AbstractNamedSBase> species;
    if (showQualModel) {
      SBasePlugin qm = document.getModel().getExtension(KEGG2SBMLqual.QUAL_NS);
      if (qm!=null && qm instanceof QualitativeModel) {
        QualitativeModel q = (QualitativeModel) qm;
        if (!q.isSetListOfQualitativeSpecies()) return simpleGraph;
        species = q.getListOfQualitativeSpecies();
      } else {
        log.warning("SBMLDocument contains no qual-model.");
        return simpleGraph;
      }
    } else {
      species = document.getModel().getListOfSpecies();
    }
    
    // Eventually get the layout extension
    SBasePlugin layoutExtension = document.getModel().getExtension(KEGG2SBMLLayoutExtension.LAYOUT_NS);
    boolean useLayoutExtension = layoutExtension!=null;
    Map<String, BoundingBox> speciesLayoutMap = null;
    Map<String, BoundingBox> reactionLayoutMap = null;
    if (useLayoutExtension) {
      if (((ExtendedLayoutModel)layoutExtension).isSetListOfLayouts()) {
        // TODO: For generic releases, it would be nice to have a JList
        // that let's the user choose the layout.
        Layout l = ((ExtendedLayoutModel)layoutExtension).getLayout(0);
        speciesLayoutMap = new HashMap<String, BoundingBox>();
        for (SpeciesGlyph sg: l.getListOfSpeciesGlyphs()) {
          if (sg.isSetBoundingBox()) {
            speciesLayoutMap.put(sg.getSpecies(), sg.getBoundingBox());
          }
        }
        reactionLayoutMap = new HashMap<String, BoundingBox>();
        for (ReactionGlyph sg: l.getListOfReactionGlyphs()) {
          if (sg.isSetBoundingBox()) {
            speciesLayoutMap.put(sg.getReaction(), sg.getBoundingBox());
          }
        }
        useLayoutExtension = speciesLayoutMap.size()>0 || reactionLayoutMap.size()>0;
      } else {
        useLayoutExtension = false;
      }
    }
    
    // Create alist of species with enzymatic activity.
    Set<String> enzymeSpeciesIDs = getListOfEnzymes(document);
    
    // Add some standardized maps, required by some utility methods
    NodeMap nodePosition = simpleGraph.createNodeMap();
    GenericDataMap<DataMap, String> mapDescriptionMap = KEGG2yGraph.addMapDescriptionMapToGraph(simpleGraph);
    mapDescriptionMap.set(nodePosition, GraphMLmaps.NODE_POSITION);
    
    // Convert each species to a graph node
    Map<String, Node> species2node = new HashMap<String, Node>();
    Map<Reaction, ReactionNodeRealizer> reaction2node = new HashMap<Reaction, ReactionNodeRealizer>();
    Set<Node> unlayoutedNodes = new HashSet<Node>();
    int nodesWithoutCoordinates=0;
    int COLUMNS = Math.max(1, species.size()/5); // Show in 5 rows by default
    for (AbstractNamedSBase s : species) {
      boolean nodeHadLayoutInformation = false;
      boolean nodeShouldBeACircle = false;
      Node n = simpleGraph.createNode();
      species2node.put(s.getId(), n);
      GraphElement2SBMLid.put(n, s.getId());
      
      // Set Node shape (and color) based on SBO-terms
      NodeRealizer nr;
      if (!s.isSetSBOTerm()) {
        nr = simpleGraph.getRealizer(n);
      } else {
        // ... except for enzymes
        /*
         * IMPORTANT ReactionModifiers (referencing to species)
         * should get the enzyme shape, even if the species is a gene!
         * The picture will otherwise show genes, catalyzing reactions
         * instead of enzymes!!!!!
         */
        if (enzymeSpeciesIDs.contains(s.getId())) {
          nr = SBMLVisualizationProperties.getEnzymeRelizer();
        } else {
          nr = SBMLVisualizationProperties.getNodeRealizer(s.getSBOTerm());
        }
        nr = nr.createCopy();
        simpleGraph.setRealizer(n, nr);
        nodeShouldBeACircle = SBMLVisualizationProperties.isCircleShape(s.getSBOTerm());
      } 
      
      // Initialize default layout variables
      double x=Double.NaN;
      double y=Double.NaN;
      double w=46;
      double h=17;
      
      // Get information from the layout extension
      if (useLayoutExtension) {
        BoundingBox g = speciesLayoutMap.get(s.getId());
        if (g!=null) {
          if (g.isSetDimensions()) {
            w = g.getDimensions().getWidth();
            h = g.getDimensions().getHeight();
          }
          if (g.isSetPosition()) {
            // Ignore 0|0 positions. They're due to default values
            if (g.getPosition().getX()!=0d || g.getPosition().getY()!=0d) {
              x = g.getPosition().getX();
              y = g.getPosition().getY();
              nodeHadLayoutInformation=true;
            }
          }
        }
      }
      
      // Auto-set missing coordinates
      if (Double.isNaN(x) || Double.isNaN(y)) {
        // Make a simple grid-layout to set some initial coords
        x = (nodesWithoutCoordinates%COLUMNS)*(w+w/2);
        y = (nodesWithoutCoordinates/COLUMNS)*(h+h);
        
        nodesWithoutCoordinates++;
        unlayoutedNodes.add(n);
      } 
      
      // Set coordinates
      nr.setCenterX(x);
      nr.setCenterY(y);
      nr.setWidth(w);
      nr.setHeight(h);
      
      if (nodeShouldBeACircle) {
        double min;
        if (unlayoutedNodes.contains(n)) {
          min = 8; // KEGG compounds always have w and h of 8 by default.  
        } else {
          min = Math.min(w, h);
        }
        
        nr.setWidth(min);
        nr.setHeight(min);
      }
      
      if (nodeHadLayoutInformation) {
        // Remember in defined hashmap
        nodePosition.set(n, (int) nr.getX() + "|" + (int) nr.getY());
      }
      
      
      nr.setLabelText(s.getName());
    }
    
    if (showQualModel) {
      QualitativeModel qm = ((QualitativeModel)document.getModel().getExtension(KEGG2SBMLqual.QUAL_NS));
      for (Transition t : qm.getListOfTransitions()) {
        // TODO: Actually, instead of making n*m edges
        // make n edges to a fake-node, that branches off to m-nodes
        // (correct SBML/SBGN-Style).
        if (!t.isSetListOfInputs() || !t.isSetListOfOutputs()) {
          continue;
        }
        
        for (Input i: t.getListOfInputs()) {
          for (Output o: t.getListOfOutputs()) {
            
            Node source = species2node.get(i.getQualitativeSpecies());
            Node target = species2node.get(o.getQualitativeSpecies());
            
            Edge e = simpleGraph.createEdge(source, target);
            GraphElement2SBMLid.put(e, t.getId());
            
            if (i.isSetSign()) {
              if (i.getSign().equals(Sign.positive)) {
                simpleGraph.getRealizer(e).setArrow(Arrow.STANDARD);
              } else if (i.getSign().equals(Sign.negative)) {
                simpleGraph.getRealizer(e).setArrow(Arrow.T_SHAPE);
              } else if (i.getSign().equals(Sign.dual)) {
                // Diamond is used in SBGN-PD as "modulation".
                simpleGraph.getRealizer(e).setArrow(Arrow.DIAMOND);
              }
            }
            
          }
        }
      }
      
    } else {
      
      // Add all reactions to the graph
      Set<Node> usedEnzymes = new HashSet<Node>();
      for (Reaction r : document.getModel().getListOfReactions()) {
        
        if (r.isSetListOfReactants() && r.isSetListOfProducts()) {
          
          // Create the reaction node
          NodeRealizer nr = new ReactionNodeRealizer();
          reaction2node.put(r, (ReactionNodeRealizer) nr);
          Node rNode = simpleGraph.createNode(nr);
          GraphElement2SBMLid.put(rNode, r.getId());
          
          // Get information from the layout extension
          double x=Double.NaN;
          double y=Double.NaN;
          if (useLayoutExtension) {
            BoundingBox g = reactionLayoutMap.get(r.getId());
            if (g!=null) {
              if (g.isSetDimensions()) {
                nr.setWidth(g.getDimensions().getWidth());
                nr.setHeight(g.getDimensions().getHeight());
              }
              if (g.isSetPosition()) {
                // Ignore 0|0 positions. They're due to default values
                if (g.getPosition().getX()!=0d || g.getPosition().getY()!=0d) {
                  x = g.getPosition().getX();
                  y = g.getPosition().getY();
                }
              }
            }
          }
          
          // Adding them to the to-be-layouted list will lead to position
          // them as freely as possible by yFiles, which is WRONG for these
          // nodes. Actually, they should always be between products and
          // substrate... needing special layouting!
          //unlayoutedNodes.add(rNode);
          if (Double.isNaN(x) || Double.isNaN(y)) {
            ValuePair<Double, Double> xy = calculateMeanCoords(r.getListOfReactants(), r.getListOfProducts(), species2node, simpleGraph);
            nr.setX(xy.getA());
            nr.setY(xy.getB());
          }
          
          // TODO: Add stoichiometry to edges (docked to corresponding node):
          // subtrate on substrate node
          // product on product node
          
          // Add edges to the reaction node
          for (SpeciesReference sr : r.getListOfReactants()) {
            Node source = species2node.get(sr.getSpecies());
            if (source!=null) {
              Edge e = simpleGraph.createEdge(source, rNode);
              GraphElement2SBMLid.put(e, r.getId());
              EdgeRealizer er = simpleGraph.getRealizer(e);
              if (r.isReversible()) {
                er.setSourceArrow(Arrow.STANDARD);
              } else {
                er.setSourceArrow(Arrow.NONE);
              }
              er.setArrow(Arrow.NONE);
            }
          }

          for (SpeciesReference sr : r.getListOfProducts()) {
            Node target = species2node.get(sr.getSpecies());
            if (target!=null) {
              Edge e = simpleGraph.createEdge(rNode, target);
              GraphElement2SBMLid.put(e, r.getId());
              EdgeRealizer er = simpleGraph.getRealizer(e);
              er.setArrow(Arrow.STANDARD);
              er.setSourceArrow(Arrow.NONE);
            }
          }
          
          for (ModifierSpeciesReference sr : r.getListOfModifiers()) {
            Node source = species2node.get(sr.getSpecies());
            if (source!=null) {
              if (splitEnzymesToOnlyOccurOnceInAnyReaction) {
                // Split enzymes to have a nicer visualization. 
                if (usedEnzymes.contains(source)) {
                  // TODO: IMPORTANT Cloned node must have a black 1/3 bottom!
                  Node oldSource = source;
                  source = oldSource.createCopy(simpleGraph);
                  NodeRealizer realizer = simpleGraph.getRealizer(source);
                  if (realizer instanceof CloneMarker) {
                    ((CloneMarker) realizer).setNodeIsCloned(true);
                    ((CloneMarker) simpleGraph.getRealizer(oldSource)).setNodeIsCloned(true);
                  }
                  unlayoutedNodes.add(source);
                  GraphElement2SBMLid.put(source, sr.getSpecies());
                }
              }
              Edge e = simpleGraph.createEdge(source, rNode);
              GraphElement2SBMLid.put(e, r.getId());
              EdgeRealizer er = simpleGraph.getRealizer(e);
              er.setArrow(Arrow.TRANSPARENT_CIRCLE);
              er.setLineType(LineType.LINE_1);
              er.setSourceArrow(Arrow.NONE);
              usedEnzymes.add(source);
            }
          }
          
          
        }
      }
    }
    
    // Fix layout of reaction nodes.
    /* TODO: These reaction nodes are not nice and require still massive improvements!
     * 
     */
    if (nodesWithoutCoordinates>0) {
      TranslatorTools tools = new TranslatorTools(simpleGraph);
      if (useLayoutExtension) {
        // Only layout nodes, that had no coords in the layout extension
        tools.layoutNodeSubset(unlayoutedNodes, true);
      } else {
        // Apply Hierarchic layout if no layoutExtension is available.
        tools.layout(SmartOrganicLayouter.class);
      }
      simpleGraph.unselectAll();
    }
        
    // Fix ReactionNode edges (determines 90° rotatable node orientation)
    for (Map.Entry<Reaction,ReactionNodeRealizer> en : reaction2node.entrySet()) {
      Set<Node> reactants = new HashSet<Node>();
      Set<Node> products = new HashSet<Node>();
      Set<Node> modifier = new HashSet<Node>();
      for (SimpleSpeciesReference sr : en.getKey().getListOfReactants()) {
        reactants.add(species2node.get(sr.getSpecies()));
      }
      for (SimpleSpeciesReference sr : en.getKey().getListOfProducts()) {
        products.add(species2node.get(sr.getSpecies()));
      }
      for (SimpleSpeciesReference sr : en.getKey().getListOfModifiers()) {
        modifier.add(species2node.get(sr.getSpecies()));
      }
      en.getValue().fixLayout(reactants, products, modifier);
    }

    
    return simpleGraph;
  }

  /**
   * Create a list of species (by identifier) that 
   * show an enzymatic activity. 
   * @param document
   * @return List of species identifiers that are in the
   * list list of {@link ModifierSpeciesReference}s.
   */
  public static Set<String> getListOfEnzymes(SBMLDocument document) {
    // Create a list of all species IDs that act as enzymes.
    Set<String> enzymeSpeciesIDs = new HashSet<String>();
    {
      Set<ModifierSpeciesReference> ref = document.getModel().getModifierSpeciesReferences();
      for (ModifierSpeciesReference msr : ref) {
        if ( msr.isSetSpecies() && msr.getSpecies().length()>0) {
          enzymeSpeciesIDs.add(msr.getSpecies());
        }
      }
    }
    return enzymeSpeciesIDs;
  }


  /**
   * Calculates the mean x and y coordinates.
   * @param <T>
   * @param listOfReactants
   * @param species2node
   * @param simpleGraph
   * @return
   */
  private <T extends SimpleSpeciesReference> ValuePair<Double, Double> calculateMeanCoords(Iterable<T> listOfReactants,
    Map<String, Node> species2node, Graph2D simpleGraph) {
    List<Double> xes = new ArrayList<Double>();
    List<Double> yes = new ArrayList<Double>();
    
    Iterator<T> it = listOfReactants.iterator();
    while (it.hasNext()) {
      T s = it.next();
      if (s.isSetSpecies()) {
        Node n = species2node.get(s.getSpecies());
        if (n!=null) {
          NodeRealizer nr = simpleGraph.getRealizer(n);
          xes.add(nr.getX());
          yes.add(nr.getY());
        }
      }
    }
    
    return new ValuePair<Double, Double>(MathUtils.mean(xes), MathUtils.mean(yes));
  }
  
  /**
   * First, calculates the mean of all x/y coordinates of all
   * products and substrates separately. Then, calculates
   * the mean of those means. The result should be X/Y
   * coordinates that are perfectly between the given 
   * products and substrates.
   * @param <T>
   * @param listOfSubstrates
   * @param listOfProducts
   * @param species2node
   * @param simpleGraph
   * @return
   */
  private <T extends SimpleSpeciesReference> ValuePair<Double, Double> calculateMeanCoords(Iterable<T> listOfSubstrates,
    Iterable<T> listOfProducts, Map<String, Node> species2node, Graph2D simpleGraph) {
    
    ValuePair<Double, Double> subs = calculateMeanCoords(listOfSubstrates, species2node, simpleGraph);
    ValuePair<Double, Double> prod = calculateMeanCoords(listOfProducts, species2node, simpleGraph);
    
    return new ValuePair<Double, Double>(MathUtils.mean(subs.getA(), prod.getA()), MathUtils.mean(subs.getB(), prod.getB()));
  }

  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorGraphLayerPanel#getOutputFileFilterForRealDocument()
   */
  @Override
  protected List<FileFilter> getOutputFileFilterForRealDocument() {
    List<FileFilter> ff = new LinkedList<FileFilter>();
    ff.add(SBFileFilter.createSBMLFileFilter());
    return ff;
  }


  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorGraphLayerPanel#writeRealDocumentToFileUnchecked(java.io.File, java.lang.String)
   */
  @Override
  protected boolean writeRealDocumentToFileUnchecked(File file, String format)
    throws Exception {
    if (SBFileFilter.isTeXFile(file) || SBFileFilter.isPDFFile(file) || format.equals("tex") || format.equals("pdf")) {
      TranslatorSBMLPanel.writeLaTeXReport(file, document);
      return true; // Void result... can't check.
    } else {
      return ((KEGG2jSBML)getTranslator()).writeToFile(document, file.getPath());
    }
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorGraphLayerPanel#isDetailPanelAvailable()
   */
  @Override
  public boolean isDetailPanelAvailable() {
    return true;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorGraphLayerPanel#updateDetailPanel(javax.swing.JPanel, y.view.HitInfo)
   */
  @Override
  protected void updateDetailPanel(final JScrollPane detailPanel, HitInfo clickedObjects) {
    
    if (clickedObjects==null || !clickedObjects.hasHits()) {
      synchronized (detailPanel) {
        ((JScrollPane) detailPanel).setViewportView(null);
      }
    } else {
      Set<Object> hits = TranslatorTools.getHitEdgesAndNodes(clickedObjects, true);
      JPanel p = new JPanel(); // Gridlayout makes always same height...
      LayoutHelper lh = new LayoutHelper(p);
      for (Object nodeOrEdge: hits) {
        if (Thread.currentThread().isInterrupted()) return;
        
        // Try to get actual SBML-element
        String sbmlID = GraphElement2SBMLid.get(nodeOrEdge);
        SBase base = null;
        if (sbmlID!=null) {
          if (!showQualModel) {
            base = document.getModel().getSpecies(sbmlID);
            if (base==null ){
              base = document.getModel().getReaction(sbmlID);
            }
          } else {
            SBasePlugin qm = document.getModel().getExtension(KEGG2SBMLqual.QUAL_NS);
            if (qm!=null && qm instanceof QualitativeModel) {
              QualitativeModel q = (QualitativeModel) qm;
              base = q.getQualitativeSpecies(sbmlID);
              if (base==null ){
                base = q.getTransition(sbmlID);
              }
            }
          }
        }
        
        // Add a detail panel if we have the element.
        if (base!=null) {
          try {
            lh.add(new SBasePanel(base, true), true);
          } catch (Exception e) {
            log.log(Level.WARNING, "Could not create detail panel.", e);
          }
        }
      }
      
      // Add final panel
      if (Thread.currentThread().isInterrupted()) return;
      if (p.getComponentCount()>0) {
        synchronized (detailPanel) {
          ((JScrollPane) detailPanel).setViewportView(p);
          
          // Scroll to top.
          GUITools.scrollToTop(detailPanel);
        }
      } else {
        synchronized (detailPanel) {
          ((JScrollPane) detailPanel).setViewportView(null);
        }
      }
    }
    
  }
  
}
