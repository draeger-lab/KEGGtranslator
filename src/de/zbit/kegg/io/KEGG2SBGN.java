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

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.sbgn.SbgnUtil;
import org.sbgn.bindings.Arc;
import org.sbgn.bindings.Arc.End;
import org.sbgn.bindings.Arc.Start;
import org.sbgn.bindings.Bbox;
import org.sbgn.bindings.Glyph;
import org.sbgn.bindings.Glyph.Port;
import org.sbgn.bindings.Label;
import org.sbgn.bindings.ObjectFactory;
import org.sbgn.bindings.Sbgn;

import de.zbit.graph.io.def.SBGNProperties;
import de.zbit.graph.io.def.SBGNProperties.ArcType;
import de.zbit.graph.io.def.SBGNProperties.GlyphType;
import de.zbit.kegg.api.KeggInfos;
import de.zbit.kegg.api.cache.KeggInfoManagement;
import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.kegg.parser.pathway.Reaction;
import de.zbit.kegg.parser.pathway.ReactionComponent;
import de.zbit.kegg.parser.pathway.Relation;


/**
 * A (not yet fully implemented) implementation of KEGG2SBGN.
 * 
 * <p> Note:<br/>
 * Martijn and Manuel should be mentioned at least in 'Acknowledgments', in case of a publication
 * of this method.</p>
 * 
 * @author Manuel Ruff
 * @author Clemens Wrzodek
 * @author Martijn van Iersel
 * @author Andreas Dr&auml;ger
 * @date 2011-04-22
 * @version $Rev$
 */
public class KEGG2SBGN extends AbstractKEGGtranslator<Sbgn> {
	
	private ObjectFactory objectFactory = new ObjectFactory();
	
	private Sbgn sbgn = objectFactory.createSbgn();
	private org.sbgn.bindings.Map map = objectFactory.createMap();
	private HashMap<Glyph, Integer> glyphNames = new HashMap<Glyph, Integer>();
	private int id = 0;
	
	/**
	 * Constructor
	 * @param manager
	 */
	public KEGG2SBGN(KeggInfoManagement manager) {
		super(manager);
	}

	@Override
	protected Sbgn translateWithoutPreprocessing(Pathway p) {
		
		// set the map
		sbgn.setMap (map);
		
		// for every entry in the pathway
		handleAllEntries(p);
		
		// for every relation in the pathway
		if(this.considerRelations())
			handleAllRelations(p);
		
		// for every reaction in the pathway
		if(this.considerReactions())
			handleAllReactions(p);
		
		return sbgn;
	}
	
	/**
	 * Transform all the Entries from KEGG to SBGN {@link Glyph}s
	 * @param p		Pathway
	 */
	private void handleAllEntries(Pathway p) {
		
	  Map<String, Entry> handlesEntries = new HashMap<String, Entry>();
	  
		// for every entry
		for (Entry e : p.getEntries()) {
      // create a glyph
      Glyph g = objectFactory.createGlyph();
      
		  if (handlesEntries.containsKey(e.getName())) {
		    // war schon drin
		    g.setClone(objectFactory.createGlyphClone());
		    Entry en = handlesEntries.get(e.getName());
		    if (en.getCustom()!=null && ((Glyph)en.getCustom()).getClone()==null) {
		      ((Glyph)en.getCustom()).setClone(objectFactory.createGlyphClone());
		    }
		  } else {
		    handlesEntries.put(e.getName(), e);
		  }
			
			// determine the sbgn clazz for the glyph
			g.setClazz(SBGNProperties.getGlyphType(e).toString());
			
			// create a bbox and a label
			Bbox bb = objectFactory.createBbox();
			Label l = objectFactory.createLabel();
		
			List<KeggInfos> keggInfos = new LinkedList<KeggInfos>();
			
			// call KeggInfos for the correct name and additional informations
			for (String ko_id:e.getName().split(" ")) {
				if (ko_id.trim().equalsIgnoreCase("undefined") || e.hasComponents()) continue;
				KeggInfos infos = KeggInfos.get(ko_id, manager);
				keggInfos.add(infos);
			}
        
			String name = getNameForEntry(e, keggInfos.toArray(new KeggInfos[0]));
			
			// define the bounding box
			bb.setX(e.getGraphics().getX());
			bb.setY(e.getGraphics().getY());
			bb.setW(e.getGraphics().getWidth());
			bb.setH(e.getGraphics().getHeight());
			
			// set the label name according to the KeggInfos fetched
			l.setText(name);
			
			// set the values for the glyph
			g.setBbox(bb);
			g.setLabel(l);
			
			// set the glyph as custom in the entry
			e.setCustom(g);
			// put the glyph into the map
			sbgn.getMap().getGlyph().add(g);
		}
		
	}
	
	/**
	 * Transform all the Relations from KEGG to SBGN arcs
	 * @param p		Pathway
	 */
	private void handleAllRelations(Pathway p) {
		// for every relation
		for (Relation relation : p.getRelations()) {
			// get the relation partners
			Entry one = p.getEntryForId(relation.getEntry1());
			Entry two = p.getEntryForId(relation.getEntry2());
			
			// make sure all went right
			if (one==null || two==null) {
				// This happens, e.g. when removing pathways nodes
				// or in general when removing nodes... => below
				// info, because mostly this is wanted by user.
				log.fine("Relation with unknown entry!");
				continue;
			}
			
			// grab the source and the target of the relation as glyphs
			Glyph source = (Glyph) one.getCustom();
			Glyph target = (Glyph) two.getCustom();
			
			// for every subtype of the relation
			for(int i = 0; i < relation.getSubtypes().size(); i++){
				// get the name of the relation subtype
				String currentRelation = relation.getSubtypes().get(i).getName();
				
//				// check if its needed to create a process glyph
//				if(p.getReactionsForEntry(one).size() > 0 && p.getReactionsForEntry(two).size() > 0){
//					/** TODO: whats about dissociation and association, should there be lists of sources and targets? **/
//					if(currentRelation.equalsIgnoreCase(SubType.ASSOCIATION.toString()) || currentRelation.equalsIgnoreCase(SubType.BINDING_ASSOCIATION.toString()))
//						createLinkWithProcessGlyph(source, target, GlyphType.association);
//					if(currentRelation.equalsIgnoreCase(SubType.DISSOCIATION.toString()))
//						createLinkWithProcessGlyph(source, target, GlyphType.dissociation);
//					if(currentRelation.equalsIgnoreCase(SubType.STATE_CHANGE.toString()))
//						;
//
//					createLinkWithProcessGlyph(source, target, GlyphType.process);
//				}
//				else{
//					if(currentRelation.equalsIgnoreCase(SubType.DEPHOSPHORYLATION))
//						;
//					if(currentRelation.equalsIgnoreCase(SubType.PHOSPHORYLATION))
//						;
//					if(currentRelation.equalsIgnoreCase(SubType.GLYCOSYLATION))
//						;
//					if(currentRelation.equalsIgnoreCase(SubType.METHYLATION))
//						;
//					if(currentRelation.equalsIgnoreCase(SubType.UBIQUITINATION))
//						;
//					createLink(source, target);
//				}
			}
		}
	}

	/**
	 * Transform all the Reactions from KEGG to SBGN
	 * @param p
	 */
	private void handleAllReactions(Pathway p){
		for(Reaction reaction : p.getReactions()){
		  
		  
		  // Substrates
		  for (ReactionComponent rc : reaction.getSubstrates()) {
		    Entry substrate = p.getEntryForReactionComponent(rc);
		    // TODO: Substrates
		  }
		  
      // Products
      for (ReactionComponent rc : reaction.getProducts()) {
        Entry product = p.getEntryForReactionComponent(rc);
         // TODO: Products
      }
		  
		  
		  // Enzymes;
		  Collection<Entry> enzymes = p.getReactionModifiers(reaction.getName());
		  for (Entry ec : enzymes) {
        // TODO: Enzymes
		  }
		  
//			// initiate
//			Arc a = new Arc();
//			Start start = new Start();
//			End end = new End();
//			Next next = new Next();
//			
//			// set start and end points
//			
//			// set the reaction type
//			a.setClazz(reaction.getType().toString());
//			
//			System.out.println(reaction.getType().toString());
//			
//			// put the arc into the map
//			sbgn.getMap().getArc().add(a);
		}
	}
	
	/**
	 * Create a {@link Glyph} and name them ascendingly
	 * @param clazz
	 * @return {@link Glyph}
	 */
	private Glyph createGlyph(String clazz){
		// create a new glyph
		Glyph glyph = objectFactory.createGlyph();
		// set the clazz
		glyph.setClazz(clazz);
		// name the glyph and add the id globally
		glyph.setId("glyph" + id++);
		// put the glyph in the hashmap with the number of the next subglyph
		glyphNames.put(glyph, 1);
		return glyph;
	}
	
	/**
	 * Create a {@link Port} for a {@link Glyph} with the correct name and number
	 * @param glyph
	 * @return {@link Port}
	 */
	private Port createPortForGlyph(Glyph glyph){
		// create a new port
		Port port = objectFactory.createGlyphPort();
		// get the number of the next subglyph from the hashmap
		int subId = glyphNames.get(glyph);
		// create the proper name for the port and set it
		port.setId(glyph.getId() + "." + subId);
		// increase the glyphs subglyphs
		glyphNames.put(glyph, subId++);
		return port;
	}
	
	/**
	 * Create a Connection between the source and the target {@link Glyph}s
	 * @param source
	 * @param target
	 */
	private void createLink(Glyph source, Glyph target){
		
		// create a connection
		Arc connection = objectFactory.createArc();
		
		// create start and end of the connection
		Start connectionStart = objectFactory.createArcStart();
		End connectionEnd = objectFactory.createArcEnd();
		
		// set start and end coordinations accordingly to the source and target glyphs
		connectionStart.setX(source.getBbox().getX());
		connectionStart.setY(source.getBbox().getY());
		connectionEnd.setX(target.getBbox().getX());
		connectionEnd.setY(target.getBbox().getY());
		
		// set the startand end to the connection
		connection.setStart(connectionStart);
		connection.setEnd(connectionEnd);
		// clazz is needed otherwise there will be an error
		connection.setClazz(ArcType.consumption.toString());
		// set the glyphs as source and target within the connection
		connection.setSource(source);
		connection.setTarget(target);
		
		// add the connection to the arc list
		sbgn.getMap().getArc().add(connection);
	}
	
	/**
	 * Create a Connection with a process {@link Glyph} between the source and target {@link Glyph}s
	 * @param source
	 * @param target
	 */
	private void createLinkWithProcessGlyph(Glyph source, Glyph target, GlyphType type){

		// create a process glyph
		Glyph process = createGlyph(type.toString());
		
		// create two connection (one from the source to the glyph and the other from the glyph to the target)
		Arc SourceConsumption = objectFactory.createArc();
		Arc TargetProduction = objectFactory.createArc();
		
		// create a bbox and set it
		Bbox processBbox = objectFactory.createBbox();
		processBbox.setH(10);
		processBbox.setW(10);
		processBbox.setX(0);
		processBbox.setY(0);
		process.setBbox(processBbox);
	
		// create start and end points for the two connections
		Start startSource = objectFactory.createArcStart();
		Start startTarget = objectFactory.createArcStart();
		End EndSource = objectFactory.createArcEnd();
		End EndTarget = objectFactory.createArcEnd();
		
		// specify the coordinates for these points
		startSource.setX(source.getBbox().getX());
		startSource.setY(source.getBbox().getY());
		EndSource.setX(process.getBbox().getX());
		EndSource.setY(process.getBbox().getY());
		startTarget.setX(process.getBbox().getX());
		startTarget.setY(process.getBbox().getY());
		EndTarget.setX(target.getBbox().getX());
		EndTarget.setY(target.getBbox().getY());
		
		// set the start point, the end point, the class and the source and target for the first connection
		SourceConsumption.setStart(startSource);
		SourceConsumption.setEnd(EndSource);
		SourceConsumption.setClazz(ArcType.consumption.toString());
		SourceConsumption.setSource(source);
		SourceConsumption.setTarget(process);
		
		// the same for the second connection
		TargetProduction.setStart(startTarget);
		TargetProduction.setEnd(EndTarget);
		TargetProduction.setClazz(ArcType.production.toString());
		TargetProduction.setSource(process);
		TargetProduction.setTarget(target);
		
		// add all to the map
		sbgn.getMap().getArc().add(SourceConsumption);
		sbgn.getMap().getArc().add(TargetProduction);
		sbgn.getMap().getGlyph().add(process);
	}
	
	public static void main(String[] args){
	}
	
	@Override
	public boolean writeToFile(Sbgn doc, String outFile) {
		try {
			SbgnUtil.writeToFile(doc, new File (outFile));
			return true;
		} catch (JAXBException e) {
			return false;
		}
	}

	@Override
	protected boolean considerRelations() {
		return true;
	}

	@Override
	protected boolean considerReactions() {
		return true;
	}
}
