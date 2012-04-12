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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.sbml.jsbml.AbstractNamedSBase;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SimpleSpeciesReference;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.ext.SBasePlugin;
import org.sbml.jsbml.ext.layout.BoundingBox;
import org.sbml.jsbml.ext.layout.ExtendedLayoutModel;
import org.sbml.jsbml.ext.layout.Layout;
import org.sbml.jsbml.ext.layout.LayoutConstant;
import org.sbml.jsbml.ext.layout.ReactionGlyph;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;
import org.sbml.jsbml.ext.qual.Input;
import org.sbml.jsbml.ext.qual.Output;
import org.sbml.jsbml.ext.qual.QualConstant;
import org.sbml.jsbml.ext.qual.QualitativeModel;
import org.sbml.jsbml.ext.qual.Sign;
import org.sbml.jsbml.ext.qual.Transition;

import y.base.Edge;
import y.base.Node;
import y.view.Arrow;
import y.view.EdgeRealizer;
import y.view.Graph2D;
import y.view.LineType;
import y.view.NodeRealizer;
import de.zbit.graph.CloneMarker;
import de.zbit.graph.ReactionNodeRealizer;
import de.zbit.kegg.ext.SBGNVisualizationProperties;
import de.zbit.math.MathUtils;
import de.zbit.util.objectwrapper.ValuePair;

/**
 * This class creates a {@link Graph2D} document (GraphML) to visualize
 * SBML documents in SBGN-style. Currently, in addition to the core, the
 * following extensions are supported:
 * <ul><li>Core</li><li>Qual</li><li>Layout</li></ul>
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class SBML2GraphML extends SB_2GraphML<SBMLDocument> {
  public static final Logger log = Logger.getLogger(SBML2GraphML.class.getName());
  
  /**
   * If false, shows the normal, quantitative SBML model.
   * If true, shows the qual model.
   */
  private boolean showQualModel=false;
  
  /**
   * This is NO option that can be set. It is used as internal
   * status variable to determine, if layout infomation is available!
   */
  private boolean useLayoutExtension;
  
  /**
   * If layout information is available (see {@link #useLayoutExtension})
   * this map, maps every id (of {@link org.sbml.jsbml.Species} or
   * {@link Reaction}) to the corresponding {@link BoundingBox}.
   */
  private Map<String, BoundingBox> id2layoutMap = null;
  
  /**
   * This map helps to enhance the reactionNode-layout, after the graph
   * is completely built and layouted.
   */
  private Map<Reaction, ReactionNodeRealizer> reaction2node=null;
  
  /**
   * The namespace URI of the qual extension.
   */
  private final static String qualNamespace = QualConstant.namespaceURI;
  /**
   * The namespace URI of the layout extension.
   */
  private final static String layoutNamespace = LayoutConstant.namespaceURI;
  
  public SBML2GraphML() {
    super();
  }
  
  public SBML2GraphML(boolean showQualModel) {
    super();
    this.showQualModel = showQualModel;
  }
  
  
  
  /**
   * @return <code>TRUE</code> if the qual model is shown.
   */
  public boolean isQualModel() {
    return showQualModel;
  }

  /**
   * @param showQualModel <code>TRUE</code> if the qual model should
   * be converted, instead of the metabolic model.
   */
  public void setShowQualModel(boolean showQualModel) {
    this.showQualModel = showQualModel;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.SB_2GraphML#createNodesAndEdges(java.lang.Object)
   */
  @Override
  protected void createNodesAndEdges(SBMLDocument document) {
    
    // Check if we have anything to visualize
    if (document==null || !document.isSetModel()) return;
    
    // Get list of species
    List<? extends AbstractNamedSBase> species;
    if (showQualModel) {
      SBasePlugin qm = document.getModel().getExtension(qualNamespace);
      if (qm!=null && qm instanceof QualitativeModel) {
        QualitativeModel q = (QualitativeModel) qm;
        if (!q.isSetListOfQualitativeSpecies()) return;
        species = q.getListOfQualitativeSpecies();
      } else {
        log.warning("SBMLDocument contains no qual-model.");
        return;
      }
    } else {
      species = document.getModel().getListOfSpecies();
    }
    
    // Eventually get the layout extension
    parseLayoutInformation(document);
    
    // Create the yFiles nodes
    addSpeciesToGraph(species, document);
    
    
    if (showQualModel) {
      /*
       * RELATIONS
       */
      QualitativeModel qm = ((QualitativeModel)document.getModel().getExtension(qualNamespace));
      for (Transition t : qm.getListOfTransitions()) {
        createRelation(t);
      }
    } else {
      /*
       * REACTIONS
       */
      reaction2node = createReactions(document);
    }
    
    return;
  }


  /**
   * Fix ReactionNode nodes (determines 90° rotatable node orientation).
   * @param reaction2node
   */
  @Override
  protected void improveReactionNodeLayout() {
    if (reaction2node==null) return; // QUAL-Model
    for (Map.Entry<Reaction,ReactionNodeRealizer> en : reaction2node.entrySet()) {
      Set<Node> reactants = new HashSet<Node>();
      Set<Node> products = new HashSet<Node>();
      Set<Node> modifier = new HashSet<Node>();
      for (SimpleSpeciesReference sr : en.getKey().getListOfReactants()) {
        reactants.add((Node) id2node.get(sr.getSpecies()));
      }
      for (SimpleSpeciesReference sr : en.getKey().getListOfProducts()) {
        products.add((Node) id2node.get(sr.getSpecies()));
      }
      for (SimpleSpeciesReference sr : en.getKey().getListOfModifiers()) {
        modifier.add((Node) id2node.get(sr.getSpecies()));
      }
      en.getValue().fixLayout(reactants, products, modifier);
    }
  }

  

  /**
   * Creates edges and pseude-reaction nodes (using {@link ReactionNodeRealizer})
   * and enzymes (also cloning them), etc. to visualize
   * metabolic reactions.
   * @param document
   * @return a map from every SBML-{@link Reaction} to the corresponding
   * {@link ReactionNodeRealizer} of the reaction node (small box).
   */
  private Map<Reaction, ReactionNodeRealizer> createReactions(SBMLDocument document) {
    Map<Reaction, ReactionNodeRealizer> reaction2node = new HashMap<Reaction, ReactionNodeRealizer>();
    
    // Add all reactions to the graph
    Set<Node> usedEnzymes = new HashSet<Node>();
    for (Reaction r : document.getModel().getListOfReactions()) {
      
      if (r.isSetListOfReactants() && r.isSetListOfProducts()) {
        
        // Create the reaction node
        NodeRealizer nr = new ReactionNodeRealizer();
        reaction2node .put(r, (ReactionNodeRealizer) nr);
        Node rNode = simpleGraph.createNode(nr);
        GraphElement2SBid.put(rNode, r.getId());
        
        // Get information from the layout extension
        double x=Double.NaN;
        double y=Double.NaN;
        if (useLayoutExtension) {
          BoundingBox g = id2layoutMap.get(r.getId());
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
          ValuePair<Double, Double> xy = calculateMeanCoords(r.getListOfReactants(), r.getListOfProducts(), id2node, simpleGraph);
          x = (xy.getA());
          y = (xy.getB());
        }
        nr.setCenterX(x);
        nr.setCenterY(y);
        
        // TODO: Add stoichiometry to edges (docked to corresponding node):
        // subtrate on substrate node
        // product on product node
        
        // Add edges to the reaction node
        for (SpeciesReference sr : r.getListOfReactants()) {
          Node source = id2node.get(sr.getSpecies());
          if (source!=null) {
            Edge e = simpleGraph.createEdge(source, rNode);
            GraphElement2SBid.put(e, r.getId());
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
          Node target = id2node.get(sr.getSpecies());
          if (target!=null) {
            Edge e = simpleGraph.createEdge(rNode, target);
            GraphElement2SBid.put(e, r.getId());
            EdgeRealizer er = simpleGraph.getRealizer(e);
            er.setArrow(Arrow.STANDARD);
            er.setSourceArrow(Arrow.NONE);
          }
        }
        
        for (ModifierSpeciesReference sr : r.getListOfModifiers()) {
          Node source = id2node.get(sr.getSpecies());
          if (source!=null) {
            if (splitEnzymesToOnlyOccurOnceInAnyReaction) {
              // Split enzymes to have a nicer visualization. 
              if (usedEnzymes.contains(source)) {
                Node oldSource = source;
                source = oldSource.createCopy(simpleGraph);
                NodeRealizer realizer = simpleGraph.getRealizer(source);
                if (realizer instanceof CloneMarker) {
                  ((CloneMarker) realizer).setNodeIsCloned(true);
                  ((CloneMarker) simpleGraph.getRealizer(oldSource)).setNodeIsCloned(true);
                } else {
                  log.warning("Can not setup clone marker on " + realizer.getClass().getSimpleName());
                }
                unlayoutedNodes.add(source);
                GraphElement2SBid.put(source, sr.getSpecies());
              }
            }
            Edge e = simpleGraph.createEdge(source, rNode);
            GraphElement2SBid.put(e, r.getId());
            EdgeRealizer er = simpleGraph.getRealizer(e);
            er.setArrow(Arrow.TRANSPARENT_CIRCLE);
            er.setLineType(LineType.LINE_1);
            er.setSourceArrow(Arrow.NONE);
            usedEnzymes.add(source);
          }
        }
        
      }
    }
    
    return reaction2node;
  }


  /**
   * @param document
   * @param species
   */
  private void addSpeciesToGraph(List<? extends AbstractNamedSBase> species, SBMLDocument document) {
    // Create a list of species with enzymatic activity.
    Set<String> enzymeSpeciesIDs = document!=null? getListOfEnzymes(document) : null;
    
    for (AbstractNamedSBase s : species) {
      // Get the SBO-term (defining the shape and color)
      int sboTerm=0;
      /*
       * IMPORTANT ReactionModifiers (referencing to species)
       * should get the enzyme shape, even if the species is a gene!
       * The picture will otherwise show genes, catalyzing reactions
       * instead of enzymes!!!!!
       */
      if (enzymeSpeciesIDs!=null && enzymeSpeciesIDs.contains(s.getId())) {
        sboTerm = SBGNVisualizationProperties.macromolecule;
      } else if (s.isSetSBOTerm()) {
        sboTerm = s.getSBOTerm();
      }

      
      // Initialize default layout variables
      double x=Double.NaN;
      double y=Double.NaN;
      double w=46;
      double h=17;
      
      // Get information from the layout extension
      if (useLayoutExtension) {
        BoundingBox g = id2layoutMap.get(s.getId());
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
            }
          }
        }
      }
      
      // Now create the real node
      createNode(s.getId(), s.getName(), sboTerm, x, y, w, h);
    }
  }


  /**
   * Parses the layout information from the given <code>document</code>.
   * This will set the {@link #useLayoutExtension} and
   * {@link #id2layoutMap} variables.
   * @param document
   */
  private void parseLayoutInformation(SBMLDocument document) {
    SBasePlugin layoutExtension = document.getModel().getExtension(layoutNamespace);
    useLayoutExtension = layoutExtension!=null;
    id2layoutMap = null;
    if (useLayoutExtension) {
      if (((ExtendedLayoutModel)layoutExtension).isSetListOfLayouts()) {
        // TODO: For generic releases, it would be nice to have a JList
        // that let's the user choose the layout.
        Layout l = ((ExtendedLayoutModel)layoutExtension).getLayout(0);
        id2layoutMap = new HashMap<String, BoundingBox>();
        for (SpeciesGlyph sg: l.getListOfSpeciesGlyphs()) {
          if (sg.isSetBoundingBox()) {
            id2layoutMap.put(sg.getSpecies(), sg.getBoundingBox());
          }
        }
        for (ReactionGlyph sg: l.getListOfReactionGlyphs()) {
          if (sg.isSetBoundingBox()) {
            id2layoutMap.put(sg.getReaction(), sg.getBoundingBox());
          }
        }
        useLayoutExtension = id2layoutMap.size()>0;
      } else {
        useLayoutExtension = false;
      }
    }
  }


  /**
   * @param t
   */
  private void createRelation(Transition t) {
    // TODO: Actually, instead of making n*m edges
    // make n edges to a fake-node, that branches off to m-nodes
    // (correct SBML/SBGN-Style).
    if (!t.isSetListOfInputs() || !t.isSetListOfOutputs()) {
      return;
    }
    
    for (Input i: t.getListOfInputs()) {
      for (Output o: t.getListOfOutputs()) {
        
        Node source = id2node.get(i.getQualitativeSpecies());
        Node target = id2node.get(o.getQualitativeSpecies());
        
        Edge e = simpleGraph.createEdge(source, target);
        GraphElement2SBid.put(e, t.getId());
        
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



  /* (non-Javadoc)
   * @see de.zbit.kegg.io.SB_2GraphML#isAnyLayoutInformationAvailable()
   */
  @Override
  protected boolean isAnyLayoutInformationAvailable() {
    return useLayoutExtension;
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
  protected <T extends SimpleSpeciesReference> ValuePair<Double, Double> calculateMeanCoords(Iterable<T> listOfSubstrates,
    Iterable<T> listOfProducts, Map<String, Node> species2node, Graph2D simpleGraph) {
    
    ValuePair<Double, Double> subs = calculateMeanCoords(listOfSubstrates, species2node, simpleGraph);
    ValuePair<Double, Double> prod = calculateMeanCoords(listOfProducts, species2node, simpleGraph);
    
    return new ValuePair<Double, Double>(MathUtils.mean(subs.getA(), prod.getA()), MathUtils.mean(subs.getB(), prod.getB()));
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
  
}
