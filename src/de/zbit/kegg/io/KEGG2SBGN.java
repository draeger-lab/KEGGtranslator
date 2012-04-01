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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import keggapi.KEGG;

import org.sbgn.SbgnUtil;
import org.sbgn.bindings.Arc;
import org.sbgn.bindings.Arc.End;
import org.sbgn.bindings.Arc.Next;
import org.sbgn.bindings.Arc.Start;
import org.sbgn.bindings.Bbox;
import org.sbgn.bindings.Glyph;
import org.sbgn.bindings.Glyph.Clone;
import org.sbgn.bindings.Glyph.Port;
import org.sbgn.bindings.Label;
import org.sbgn.bindings.Map;
import org.sbgn.bindings.Sbgn;

import de.zbit.kegg.KEGGtranslatorOptions;
import de.zbit.kegg.io.KEGG2SBGNProperties.GlyphType;
import de.zbit.kegg.io.KEGG2SBGNProperties.GlyphOrientation;
import de.zbit.kegg.io.KEGG2SBGNProperties.ArcType;
import de.zbit.kegg.api.KeggInfos;
import de.zbit.kegg.api.cache.KeggInfoManagement;
import de.zbit.kegg.KeggTools;
import de.zbit.kegg.Translator;
import de.zbit.kegg.api.KeggInfos;
import de.zbit.kegg.api.cache.KeggInfoManagement;
import de.zbit.kegg.api.cache.KeggQuery;
import de.zbit.kegg.parser.KeggParser;
import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.EntryType;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.kegg.parser.pathway.Reaction;
import de.zbit.kegg.parser.pathway.ReactionType;
import de.zbit.kegg.parser.pathway.Relation;
import de.zbit.kegg.parser.pathway.RelationType;
import de.zbit.kegg.parser.pathway.SubType;


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
	
	private boolean DEBUG = false;
	
	private Sbgn sbgn;
	private Map map;
	private HashMap<Glyph, Integer> glyphNames = new HashMap<Glyph, Integer>();
	private int id;
	
	private double xRightFactor = 2.5;
	private double xLeftFactor = 2.3;
	private double yLeftFactor = 0.75;
	private double yRightFactor = 1;
	
	/**
	 * Constructor
	 * @param manager
	 */
	public KEGG2SBGN(KeggInfoManagement manager) {
		super(manager);
	}

	@Override
	protected Sbgn translateWithoutPreprocessing(Pathway p) {
		
		// initialize for a new translation
		this.id = 0;
		this.sbgn = new Sbgn();
		this.map = new Map();
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
	 * Transform all the Entries from KEGG to SBGN Glyphs
	 * @param p		Pathway
	 */
	private void handleAllEntries(Pathway p) {
		for (Entry e : p.getEntries()) {
			// initiate
			Glyph g = createGlyph(KEGG2SBGNProperties.determineGlyphType.get(e.getType().toString()));
			if(g == null)
				System.out.println("null");
			Bbox bb = new Bbox();
			Label l = new Label();
		
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
			g.setOrientation(GlyphOrientation.horizontal.name());
			
			// put the glyph into the map
			e.setCustom(g);
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
				
//				if(currentRelation.equalsIgnoreCase(SubType.PHOSPHORYLATION)){
					createLink(source, target);
//				}
			}
		}
	}

	private void handleAllReactions(Pathway p){
//		for(Reaction reaction : p.getReactions()){
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
//			// put the arc into the map
//			sbgn.getMap().getArc().add(a);
//		}
	}
	
	private Glyph createGlyph(String clazz){
		Glyph glyph = new Glyph();
		glyph.setClazz(clazz);
		glyph.setId("glyph" + id++);
		glyphNames.put(glyph, 1);
		return glyph;
	}
	
	private Port createPortForGlyph(Glyph glyph){
		Port port = new Port();
		int subId = glyphNames.get(glyph);
		port.setId(glyph.getId() + "." + subId);
		glyphNames.put(glyph, subId++);
		return port;
	}
	
	private void createPhosphorylation(Glyph source, Glyph target){
		
		Glyph ATP = createGlyph(GlyphType.simple_chemical.toString());
		Glyph ADP = createGlyph(GlyphType.simple_chemical.toString());
		Glyph LastGlyph = map.getGlyph().get(map.getGlyph().size());
		
		Port in = createPortForGlyph(LastGlyph);
		Port out = createPortForGlyph(LastGlyph);
	
		Arc ATPConsumption = new Arc();
		Arc ADPProduction = new Arc();
		
		float anchorATPConsumptionX = 0f;
		float anchorATPConsumptionY = 0f;
		float anchorADPProductionX = 0f;
		float anchorADPProductionY = 0f;
		
		LastGlyph.getPort().add(in);
		LastGlyph.getPort().add(out);
		
//		if(isTargetCompletelyRightOfSource(source, target) && isTargetOverSource(source, target)){
//			if(DEBUG) System.out.println("Completely Right + Over Source");
//			processBbox.setX(anchorSourceX + changeX - processBbox.getW() * 2);
//			processBbox.setY(anchorTargetY + changeY - processBbox.getH());
//		}
//		if(isTargetCompletelyRightOfSource(source, target) && isTargetUnderSource(source, target)){
//			if(DEBUG) System.out.println("Completely Right + Under Source");
//			processBbox.setX(anchorSourceX + changeX - processBbox.getW() * 2);
//			processBbox.setY(anchorSourceY + changeY - processBbox.getH());
//		}
//		if(isTargetCompletelyRightOfSource(source, target) && changeY == 0){
//			if(DEBUG) System.out.println("Completely Right + Equals Source");
//			processBbox.setX(anchorSourceX + changeX - processBbox.getW() * 2);
//			processBbox.setY(anchorSourceY + changeY - processBbox.getH());
//		}
//		if(isTargetCompletelyLeftOfSource(source, target) && isTargetOverSource(source, target)){
//			if(DEBUG) System.out.println("Completely Left + Over Source");
//			processBbox.setX(anchorTargetX + changeX - processBbox.getW() * 2);
//			processBbox.setY(anchorTargetY + changeY - processBbox.getH());
//		}
//		if(isTargetCompletelyLeftOfSource(source, target) && isTargetUnderSource(source, target)){
//			if(DEBUG) System.out.println("Completely Left + Under Source");
//			processBbox.setX(anchorTargetX + changeX - processBbox.getW() * 2);
//			processBbox.setY(anchorSourceY + changeY - processBbox.getH());
//		}
//		if(isTargetCompletelyLeftOfSource(source, target) && changeY == 0){
//			if(DEBUG) System.out.println("Completely Left + Equals Source");
//			processBbox.setX(anchorTargetX + changeX - processBbox.getW() * 2);
//			processBbox.setY(anchorSourceY + changeY - processBbox.getH());
//		}
//		if(isTargetInLineWithSource(source, target)){
//			if(isTargetOverSource(source, target) && isTargetLeftOfSource(source, target)){
//				if(DEBUG) System.out.println("In Line With + Over Source + Left of Source");
//				processBbox.setX(anchorTargetX + changeX - processBbox.getW() * 2);
//				processBbox.setY(anchorTargetY + changeY - processBbox.getH());
//			}
//			if(isTargetOverSource(source, target) && isTargetRightOfSource(source, target)){
//				if(DEBUG) System.out.println("In Line With + Over Source + Right of Source");
//				processBbox.setX(anchorSourceX + changeX - processBbox.getW() * 2);
//				processBbox.setY(anchorTargetY + changeY - processBbox.getH());
//			}
//			if(isTargetUnderSource(source, target) && isTargetLeftOfSource(source, target)){
//				if(DEBUG) System.out.println("In Line With + Under Source + Left of Source");
//				processBbox.setX(anchorTargetX + changeX - processBbox.getW() * 2);
//				processBbox.setY(anchorSourceY + changeY - processBbox.getH());
//			}
//			if(isTargetUnderSource(source, target) && isTargetRightOfSource(source, target)){
//				if(DEBUG) System.out.println("In Line With + Under Source + Right of Source");
//				processBbox.setX(anchorSourceX + changeX - processBbox.getW() * 2);
//				processBbox.setY(anchorSourceY + changeY - processBbox.getH());
//			}
//		}
//		
//		process.setBbox(processBbox);
//		
////		System.out.println("Source Position: " + "(" + source.getBbox().getX() + "," + source.getBbox().getY() + ")");
////		System.out.println("Target Position: " + "(" + target.getBbox().getX() + "," + target.getBbox().getY() + ")");
////		System.out.println("Source Anchor Position: " + "(" + anchorSourceX + "," + anchorSourceY + ")");
////		System.out.println("Target Anchor Position: " + "(" + anchorTargetX + "," + anchorTargetY + ")");
////		System.out.println("New Position: " + "(" + processBbox.getX() + "," + processBbox.getY() + ")");
//		
////		in.setX(process.getBbox().getX() - process.getBbox().getW()/2);
////		in.setY(process.getBbox().getY() + process.getBbox().getH()/2);
////		out.setX(process.getBbox().getX() + process.getBbox().getW() * 1.5f);
////		out.setY(process.getBbox().getY() + process.getBbox().getH()/2);
////		
////		process.getPort().add(in);
////		process.getPort().add(out);
//		
//		Start startSource = new Start();
//		Start startTarget = new Start();
//		End EndSource = new End();
//		End EndTarget = new End();
//		
//		startSource.setX(anchorSourceX);
//		startSource.setY(anchorSourceY);
//		EndSource.setX(process.getBbox().getX());
//		EndSource.setY(process.getBbox().getY());
//		startTarget.setX(process.getBbox().getX());
//		startTarget.setY(process.getBbox().getY());
//		EndTarget.setX(anchorTargetX);
//		EndTarget.setY(anchorTargetY);
//		
//		SourceConsumption.setStart(startSource);
//		SourceConsumption.setEnd(EndSource);
//		SourceConsumption.setClazz(ArcType.consumption.toString());
//		SourceConsumption.setSource(source);
//		SourceConsumption.setTarget(process);
//		
//		TargetProduction.setStart(startTarget);
//		TargetProduction.setEnd(EndTarget);
//		TargetProduction.setClazz(ArcType.production.toString());
//		TargetProduction.setSource(process);
//		TargetProduction.setTarget(target);
//		
////		Label l1 = new Label();
////		Label l2 = new Label();
////		l1.setText("ATP");
////		l2.setText("ADP");
////		ATP.setLabel(l1);
////		ADP.setLabel(l2);
////		Clone c1 = new Clone();
////		Clone c2 = new Clone();
////		Bbox ATPBbox = new Bbox();
////		Bbox ADPBbox = new Bbox();
////		ATP.setClone(c1);
////		ADP.setClone(c2);
////		ATPBbox.setH(30);
////		ATPBbox.setW(30);
////		ADPBbox.setH(30);
////		ADPBbox.setW(30);
////		ATPBbox.setX(process.getBbox().getX() - 30);
////		ATPBbox.setY(process.getBbox().getY() + 30);
////		ADPBbox.setX(process.getBbox().getX() + process.getBbox().getW() + 30);
////		ADPBbox.setY(process.getBbox().getY() + 30);
////		ATP.setBbox(ATPBbox);
////		ADP.setBbox(ADPBbox);
////		
////		Start startATP = new Start();
////		Start startADP = new Start();
////		End endATP = new End();
////		End endADP = new End();
////		
////		startATP.setX(ATP.getBbox().getX() + ATP.getBbox().getW()/2);
////		startATP.setY(ATP.getBbox().getY());
////		startADP.setX(ADP.getBbox().getX() + ADP.getBbox().getW()/2);
////		startADP.setY(ADP.getBbox().getY());
////		endATP.setX(process.getBbox().getX() + process.getBbox().getW()/2);
////		endATP.setY(process.getBbox().getY());
////		endADP.setX(process.getBbox().getX() + process.getBbox().getW()/2);
////		endADP.setY(process.getBbox().getY());
////		
////		PhosphorConsumption.setClazz(ArcType.consumption.toString());
////		PhosphorProduction.setClazz(ArcType.production.toString());
////		PhosphorConsumption.setStart(startATP);
////		PhosphorConsumption.setEnd(endATP);
////		PhosphorProduction.setStart(startADP);
////		PhosphorProduction.setEnd(endADP);
////		PhosphorConsumption.setSource(ATP);
////		PhosphorConsumption.setTarget(in);
////		PhosphorProduction.setSource(out);
////		PhosphorProduction.setTarget(ADP);
//		
//		// add the glyphs and arcs to the map
//		sbgn.getMap().getArc().add(SourceConsumption);
//		sbgn.getMap().getArc().add(TargetProduction);
////		sbgn.getMap().getArc().add(PhosphorConsumption);
////		sbgn.getMap().getArc().add(PhosphorProduction);
//		sbgn.getMap().getGlyph().add(process);
////		sbgn.getMap().getGlyph().add(ATP);
////		sbgn.getMap().getGlyph().add(ADP);
	}
	
	private void createUbiquitination(Glyph source, Glyph target) {
		// TODO Auto-generated method stub
	}

	private void createStateChange(Glyph source, Glyph target) {
		// TODO Auto-generated method stub
	}

	private void createRepression(Glyph source, Glyph target) {
		// TODO Auto-generated method stub
	}

	private void createMissingInteraction(Glyph source, Glyph target) {
		// TODO Auto-generated method stub
	}

	private void createMethylation(Glyph source, Glyph target) {
		// TODO Auto-generated method stub
	}

	private void createInhibition(Glyph source, Glyph target) {
		// TODO Auto-generated method stub
	}

	private void createIndirectEffect(Glyph source, Glyph target) {
		// TODO Auto-generated method stub
	}

	private void createGlycosylation(Glyph source, Glyph target) {
		// TODO Auto-generated method stub
	}

	private void createExpression(Glyph source, Glyph target) {
		// TODO Auto-generated method stub
	}

	private void createDissociation(Glyph source, Glyph target) {
		// TODO Auto-generated method stub
	}

	private void createDephosphorylation(Glyph source, Glyph target) {
		// TODO Auto-generated method stub
	}

	private void createBindingAssociation(Glyph source, Glyph target) {
		// TODO Auto-generated method stub
	}

	private void createBinding(Glyph source, Glyph target) {
		// TODO Auto-generated method stub
	}

	private void createAssociation(Glyph source, Glyph target) {
		// TODO Auto-generated method stub
	}

	private void createActivation(Glyph source, Glyph target) {
		// TODO Auto-generated method stub
	}
	
	private void createCompound(Glyph source, Glyph target) {
		// TODO Auto-generated method stub	
	}
	
	private void createLink(Glyph source, Glyph target){

		Glyph process = createGlyph("process");
		
		Arc SourceConsumption = new Arc();
		Arc TargetProduction = new Arc();
		
		// determine the positions of the anchors
		float anchorSourceX = 0f;
		float anchorSourceY = 0f;
		float anchorTargetX = 0f;
		float anchorTargetY = 0f;
		
		if(isTargetRightOfSource(source, target)) {
			anchorSourceX = source.getBbox().getX() + source.getBbox().getW();
			anchorSourceY = source.getBbox().getY() + source.getBbox().getH()/2;
			anchorTargetX = target.getBbox().getX();
			anchorTargetY = target.getBbox().getY() + target.getBbox().getH()/2;
		}
		if(isTargetLeftOfSource(source, target)) {
			anchorSourceX = source.getBbox().getX();
			anchorSourceY = source.getBbox().getY() + source.getBbox().getH()/2;
			anchorTargetX = target.getBbox().getX() + target.getBbox().getW();
			anchorTargetY = target.getBbox().getY() + target.getBbox().getH()/2;
		}
		if(isTargetInLineWithSource(source, target)) {
			if(isTargetOverSource(source, target)){
				anchorSourceX = source.getBbox().getX() + source.getBbox().getW()/2;
				anchorSourceY = source.getBbox().getY();
				anchorTargetX = target.getBbox().getX() + target.getBbox().getW()/2;
				anchorTargetY = target.getBbox().getY() + target.getBbox().getH();
			}
			if(isTargetUnderSource(source, target)){
				anchorSourceX = source.getBbox().getX() + source.getBbox().getW()/2;
				anchorSourceY = source.getBbox().getY() + source.getBbox().getH();
				anchorTargetX = target.getBbox().getX() + target.getBbox().getW()/2;
				anchorTargetY = target.getBbox().getY();
			}
		}
		
		float changeX = Math.abs((anchorTargetX - anchorSourceX)/2);
		float changeY = Math.abs((anchorTargetY - anchorSourceY)/2);
		
		// connect the parts
		Bbox processBbox = new Bbox();
		// make boxes scaleable
		processBbox.setH(10);
		processBbox.setW(10);
		
		if(isTargetCompletelyRightOfSource(source, target) && isTargetOverSource(source, target)){
			if(DEBUG) System.out.println("Completely Right + Over Source");
			processBbox.setX(anchorSourceX + changeX - (int) (processBbox.getW() * xRightFactor));
			processBbox.setY(anchorTargetY + changeY - (int) (processBbox.getH() * yRightFactor));
		}
		if(isTargetCompletelyRightOfSource(source, target) && isTargetUnderSource(source, target)){
			if(DEBUG) System.out.println("Completely Right + Under Source");
			processBbox.setX(anchorSourceX + changeX - (int) (processBbox.getW() * xRightFactor));
			processBbox.setY(anchorSourceY + changeY - (int) (processBbox.getH() * yRightFactor));
		}
		if(isTargetCompletelyRightOfSource(source, target) && changeY == 0){
			if(DEBUG) System.out.println("Completely Right + Equals Source");
			processBbox.setX(anchorSourceX + changeX - (int) (processBbox.getW() * xRightFactor));
			processBbox.setY(anchorSourceY + changeY - (int) (processBbox.getH() * yRightFactor));
		}
		if(isTargetCompletelyLeftOfSource(source, target) && isTargetOverSource(source, target)){
			if(DEBUG) System.out.println("Completely Left + Over Source");
			processBbox.setX(anchorTargetX + changeX - (int) (processBbox.getW() * xLeftFactor));
			processBbox.setY(anchorTargetY + changeY - (int) (processBbox.getH() * yLeftFactor));
		}
		if(isTargetCompletelyLeftOfSource(source, target) && isTargetUnderSource(source, target)){
			if(DEBUG) System.out.println("Completely Left + Under Source");
			processBbox.setX(anchorTargetX + changeX - (int) (processBbox.getW() * xLeftFactor));
			processBbox.setY(anchorSourceY + changeY - (int) (processBbox.getH() * yLeftFactor));
		}
		if(isTargetCompletelyLeftOfSource(source, target) && changeY == 0){
			if(DEBUG) System.out.println("Completely Left + Equals Source");
			processBbox.setX(anchorTargetX + changeX - (int) (processBbox.getW() * xLeftFactor));
			processBbox.setY(anchorSourceY + changeY - (int) (processBbox.getH() * yLeftFactor));
		}
		if(isTargetInLineWithSource(source, target)){
			if(isTargetOverSource(source, target) && isTargetLeftOfSource(source, target)){
				if(DEBUG) System.out.println("In Line With + Over Source + Left of Source");
				processBbox.setX(anchorTargetX + changeX - (int) (processBbox.getW() * xLeftFactor));
				processBbox.setY(anchorTargetY + changeY - (int) (processBbox.getH() * yLeftFactor));
			}
			if(isTargetOverSource(source, target) && isTargetRightOfSource(source, target)){
				if(DEBUG) System.out.println("In Line With + Over Source + Right of Source");
				processBbox.setX(anchorSourceX + changeX - (int) (processBbox.getW() * xRightFactor));
				processBbox.setY(anchorTargetY + changeY - (int) (processBbox.getH() * yRightFactor));
			}
			if(isTargetUnderSource(source, target) && isTargetLeftOfSource(source, target)){
				if(DEBUG) System.out.println("In Line With + Under Source + Left of Source");
				processBbox.setX(anchorTargetX + changeX - (int) (processBbox.getW() * xRightFactor));
				processBbox.setY(anchorSourceY + changeY - (int) (processBbox.getH() * yRightFactor));
			}
			if(isTargetUnderSource(source, target) && isTargetRightOfSource(source, target)){
				if(DEBUG) System.out.println("In Line With + Under Source + Right of Source");
				processBbox.setX(anchorSourceX + changeX - (int) (processBbox.getW() * xRightFactor));
				processBbox.setY(anchorSourceY + changeY - (int) (processBbox.getH() * yRightFactor));
			}
			// These cases are wired
			if(isTargetOverSource(source, target) && processBbox.getX() == 0 && processBbox.getY() == 0){
				if(DEBUG) System.out.println("In Line With");
				processBbox.setX(anchorSourceX + changeX - (int) (processBbox.getW() * xRightFactor));
				processBbox.setY(anchorSourceY - changeY - (int) (processBbox.getH() * yLeftFactor));
			}
			if(isTargetUnderSource(source, target) && processBbox.getX() == 0 && processBbox.getY() == 0){
				if(DEBUG) System.out.println("In Line With");
				processBbox.setX(anchorSourceX + changeX - (int) (processBbox.getW() * xRightFactor));
				processBbox.setY(anchorSourceY + changeY - (int) (processBbox.getH() * yLeftFactor));
			}
		}
		
		process.setBbox(processBbox);
	
		Start startSource = new Start();
		Start startTarget = new Start();
		End EndSource = new End();
		End EndTarget = new End();
		
		startSource.setX(anchorSourceX);
		startSource.setY(anchorSourceY);
		EndSource.setX(process.getBbox().getX());
		EndSource.setY(process.getBbox().getY());
		startTarget.setX(process.getBbox().getX());
		startTarget.setY(process.getBbox().getY());
		EndTarget.setX(anchorTargetX);
		EndTarget.setY(anchorTargetY);
		
		SourceConsumption.setStart(startSource);
		SourceConsumption.setEnd(EndSource);
		SourceConsumption.setClazz(ArcType.consumption.toString());
		SourceConsumption.setSource(source);
		SourceConsumption.setTarget(process);
		
		TargetProduction.setStart(startTarget);
		TargetProduction.setEnd(EndTarget);
		TargetProduction.setClazz(ArcType.production.toString());
		TargetProduction.setSource(process);
		TargetProduction.setTarget(target);
		
		sbgn.getMap().getArc().add(SourceConsumption);
		sbgn.getMap().getArc().add(TargetProduction);
		sbgn.getMap().getGlyph().add(process);
	}
	
	private boolean isTargetRightOfSource(Glyph source, Glyph target){
		return (source.getBbox().getX() < target.getBbox().getX());
	}
	
	private boolean isTargetCompletelyRightOfSource(Glyph source, Glyph target){
		return (source.getBbox().getX() + source.getBbox().getW()) < target.getBbox().getX();
	}
	
	private boolean isTargetLeftOfSource(Glyph source, Glyph target){
		return source.getBbox().getX() > target.getBbox().getX();
	}
	
	private boolean isTargetCompletelyLeftOfSource(Glyph source, Glyph target){
		return source.getBbox().getX() > target.getBbox().getX() + target.getBbox().getW();
	}
	
	private boolean isTargetOverSource(Glyph source, Glyph target){
		return source.getBbox().getY() > target.getBbox().getY();
	}
	
	private boolean isTargetCompletelyOverSource(Glyph source, Glyph target){
		return source.getBbox().getY() > target.getBbox().getY() + target.getBbox().getH();
	}
	
	private boolean isTargetUnderSource(Glyph source, Glyph target){
		return source.getBbox().getY() < target.getBbox().getY();
	}
	
	private boolean isTargetCompletelyUnderSource(Glyph source, Glyph target){
		return source.getBbox().getY() + source.getBbox().getH() < target.getBbox().getY();
	}
	
	private boolean isTargetInLineWithSource(Glyph source, Glyph target){
		return source.getBbox().getX() <= target.getBbox().getX() + target.getBbox().getW() && source.getBbox().getX() + source.getBbox().getW() >= target.getBbox().getX();
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
		return false;
	}
}
