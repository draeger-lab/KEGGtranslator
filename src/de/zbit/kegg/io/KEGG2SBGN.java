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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JTable;
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
	
	public enum GlyphType
	{
	    unspecified_entity,
	    simple_chemical,
	    macromolecule,
	    nucleic_acid_feature,
	    simple_chemical_multimer,
	    macromolecule_multimer,
	    nucleic_acid_feature_multimer,
	    complex,
	    complex_multimer,
	    source_and_sink,
	    perturbation,
	    biological_activity,
	    perturbing_agent,
	    compartment,
	    submap,
	    tag,
	    terminal,
	    process,
	    omitted_process,
	    uncertain_process,
	    association,
	    dissociation,
	    phenotype,
	    and,
	    or,
	    not,
	    state_variable,
	    unit_of_information,
	    stoichiometry,
	    entity,
	    outcome,
	    observable,
	    interaction,
	    influence_target,
	    annotation,
	    variable_value,
	    implicit_xor,
	    delay,
	    existence,
	    location,
	    cardinality;
	    
	    public String toString(){
	    	return this.name().replaceAll("_", " ");
	    }
	}
	
	public enum GlyphOrientation
	{
        horizontal,
        vertical,
        left,
        right,
        up,
        down,
	}
	
	public enum ArcType
	{
        production,
        consumption,
        catalysis,
        modulation,
        stimulation,
        inhibition,
        assignment,
        interaction,
        absolute_inhibition,
        absolute_stimulation,
        positive_influence,
        negative_influence,
        unknown_influence,
        equivalence_arc,
        necessary_stimulation,
        logic_arc;
        
	    public String toString(){
	    	return this.name().replaceAll("_", " ");
	    }
	}
	
	/**
	 * Mapping of the glyphs
	 */
	private HashMap<String, String> glyphType = new HashMap<String, String>();
	private Sbgn sbgn;
	private Map map;
	private int id;
	
	/**
	 * Constructor
	 * @param manager
	 */
	public KEGG2SBGN(KeggInfoManagement manager) {
		super(manager);

		// create mapping for the glyphs
		this.glyphType.put(EntryType.compound.name(), GlyphType.simple_chemical.toString());
		this.glyphType.put(EntryType.enzyme.name(), GlyphType.macromolecule.toString());
		this.glyphType.put(EntryType.gene.name(), GlyphType.macromolecule.toString());
		this.glyphType.put(EntryType.group.name(), GlyphType.complex.toString());
		this.glyphType.put(EntryType.map.name(), GlyphType.submap.toString());
		this.glyphType.put(EntryType.ortholog.name(), GlyphType.unspecified_entity.toString());
		this.glyphType.put(EntryType.other.name(), GlyphType.unspecified_entity.toString());
	}
	
	/**
	 * transform the glyph type of a KEGG Entry into a SBGN glyph type
	 * @param glyphType	String of a KEGG glyph type
	 * @return SBGN glyph type
	 */
	protected String determineGlyphType(String glyphType){
		return this.glyphType.get(glyphType);
	}

	@Override
	protected Sbgn translateWithoutPreprocessing(Pathway p) {
		
		// initialize for a new translation
		this.id = 0;
		this.sbgn = new Sbgn();
		this.map = new Map();
		sbgn.setMap (map);
	
		//////////////////////////////////
		// for every entry in the pathway
		////////////////////////////////
		handleAllEntries(p);
		
		////////////////////////////////////////////////////////////////////
		// for every relation in the pathway if considerRealtions() is true
		//////////////////////////////////////////////////////////////////
		if(this.considerRelations())
			handleAllRelations(p);
		
		////////////////////////////////////////////////////////////////////
		// for every reaction in the pathway if considerReactions() is true
		//////////////////////////////////////////////////////////////////
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
			Glyph g = new Glyph();
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
			g.setClazz(determineGlyphType(e.getType().toString()));
			g.setBbox(bb);
			g.setId("glyph" + id++);
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

				// check what relation subtype we got
				if(currentRelation.equalsIgnoreCase(SubType.PHOSPHORYLATION))
					createPhosphorylation(source, target);
				if(currentRelation.equalsIgnoreCase(SubType.ACTIVATION))
					createActivation(source, target);
				if(currentRelation.equalsIgnoreCase(SubType.ASSOCIATION))
					createAssociation(source, target);
				if(currentRelation.equalsIgnoreCase(SubType.BINDING))
					createBinding(source, target);
				if(currentRelation.equalsIgnoreCase(SubType.BINDING_ASSOCIATION))
					createBindingAssociation(source, target);
				if(currentRelation.equalsIgnoreCase(SubType.DEPHOSPHORYLATION))
					createDephosphorylation(source, target);
				if(currentRelation.equalsIgnoreCase(SubType.DISSOCIATION))
					createDissociation(source, target);
				if(currentRelation.equalsIgnoreCase(SubType.EXPRESSION))
					createExpression(source, target);
				if(currentRelation.equalsIgnoreCase(SubType.GLYCOSYLATION))
					createGlycosylation(source, target);
				if(currentRelation.equalsIgnoreCase(SubType.INDIRECT_EFFECT))
					createIndirectEffect(source, target);
				if(currentRelation.equalsIgnoreCase(SubType.INHIBITION))
					createInhibition(source, target);
				if(currentRelation.equalsIgnoreCase(SubType.METHYLATION))
					createMethylation(source, target);
				if(currentRelation.equalsIgnoreCase(SubType.MISSING_INTERACTION))
					createMissingInteraction(source, target);
				if(currentRelation.equalsIgnoreCase(SubType.REPRESSION))
					createRepression(source, target);
				if(currentRelation.equalsIgnoreCase(SubType.STATE_CHANGE))
					createStateChange(source, target);
				if(currentRelation.equalsIgnoreCase(SubType.UBIQUITINATION))
					createUbiquitination(source, target);
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
	
	private void createPhosphorylation(Glyph source, Glyph target){
		Glyph process = new Glyph();
		process.setClazz("process");
		process.setId("glyph" + id++);
		
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
		if(isTargetInLineWithSource(source, target)) {
			if(isTargetOverSource(source, target)){
				anchorSourceX = source.getBbox().getX() + source.getBbox().getW();
				anchorSourceY = source.getBbox().getY() + source.getBbox().getH()/2;
				anchorTargetX = target.getBbox().getX();
				anchorTargetY = target.getBbox().getY() + target.getBbox().getH()/2;
			}
			if(isTargetUnderSource(source, target)){
				anchorSourceX = source.getBbox().getX() + source.getBbox().getW();
				anchorSourceY = source.getBbox().getY() + source.getBbox().getH()/2;
				anchorTargetX = target.getBbox().getX();
				anchorTargetY = target.getBbox().getY() + target.getBbox().getH()/2;
			}
		}
		
		float changeX = (anchorTargetX - anchorSourceX)/2;
		float changeY = (anchorTargetY - anchorSourceY)/2;
		
		Bbox b = new Bbox();
		b.setH(10);
		b.setW(10);
		b.setX(anchorSourceX + changeX - b.getH()/2);
		b.setY(anchorSourceY + changeY - b.getH()/2);
		process.setBbox(b);
		Port in = new Port();
		Port out = new Port();
		
		in.setId(process.getId() + "." + "1");
		out.setId(process.getId() + "." + "2");
		in.setX(process.getBbox().getX() - process.getBbox().getW()/2);
		in.setY(process.getBbox().getY() + process.getBbox().getH()/2);
		out.setX(process.getBbox().getX() + process.getBbox().getW() * 1.5f);
		out.setY(process.getBbox().getY() + process.getBbox().getH()/2);
		
		process.getPort().add(in);
		process.getPort().add(out);
		sbgn.getMap().getGlyph().add(process);
		
		Arc arc1 = new Arc();
		Arc arc2 = new Arc();
		Start start1 = new Start();
		Start start2 = new Start();
		End end1 = new End();
		End end2 = new End();
		
		start1.setX(anchorSourceX);
		start1.setY(anchorSourceY);
		end1.setX(process.getBbox().getX());
		end1.setY(process.getBbox().getY());
		start2.setX(process.getBbox().getX());
		start2.setY(process.getBbox().getY());
		end2.setX(anchorTargetX);
		end2.setY(anchorTargetY);
		
		arc1.setStart(start1);
		arc1.setEnd(end1);
		arc1.setClazz(ArcType.consumption.toString());
		arc1.setSource(source);
		arc1.setTarget(process);
		
		arc2.setStart(start2);
		arc2.setEnd(end2);
		arc2.setClazz(ArcType.production.toString());
		arc2.setSource(process);
		arc2.setTarget(target);
		
		Glyph ATP = new Glyph();
		Glyph ADP = new Glyph();
		ATP.setId("glyph" + id++);
		ADP.setId("glyph" + id++);
		Label l1 = new Label();
		Label l2 = new Label();
		l1.setText("ATP");
		l2.setText("ADP");
		ATP.setLabel(l1);
		ADP.setLabel(l2);
		Clone c1 = new Clone();
		Clone c2 = new Clone();
		Bbox b1 = new Bbox();
		Bbox b2 = new Bbox();
		ATP.setClazz(GlyphType.simple_chemical.toString());
		ADP.setClazz(GlyphType.simple_chemical.toString());
		ATP.setClone(c1);
		ADP.setClone(c2);
		b1.setH(30);
		b1.setW(30);
		b2.setH(30);
		b2.setW(30);
		b1.setX(process.getBbox().getX() - 30);
		b1.setY(process.getBbox().getY() + 30);
		b2.setX(process.getBbox().getX() + process.getBbox().getW() + 30);
		b2.setY(process.getBbox().getY() + 30);
		ATP.setBbox(b1);
		ADP.setBbox(b2);
		
		Arc arc3 = new Arc();
		Arc arc4 = new Arc();
		Start start3 = new Start();
		Start start4 = new Start();
		End end3 = new End();
		End end4 = new End();
		
		start3.setX(ATP.getBbox().getX() + ATP.getBbox().getW()/2);
		start3.setY(ATP.getBbox().getY());
		start4.setX(ADP.getBbox().getX() + ADP.getBbox().getW()/2);
		start4.setY(ADP.getBbox().getY());
		end3.setX(process.getBbox().getX() + process.getBbox().getW()/2);
		end3.setY(process.getBbox().getY());
		end4.setX(process.getBbox().getX() + process.getBbox().getW()/2);
		end4.setY(process.getBbox().getY());
		
		arc3.setClazz(ArcType.consumption.toString());
		arc4.setClazz(ArcType.production.toString());
		arc3.setStart(start3);
		arc3.setEnd(end3);
		arc4.setStart(start4);
		arc4.setEnd(end4);
		arc3.setSource(ATP);
		arc3.setTarget(in);
		arc4.setSource(out);
		arc4.setTarget(ADP);
		
		sbgn.getMap().getArc().add(arc1);
		sbgn.getMap().getArc().add(arc2);
		sbgn.getMap().getArc().add(arc3);
		sbgn.getMap().getArc().add(arc4);
		sbgn.getMap().getGlyph().add(ATP);
		sbgn.getMap().getGlyph().add(ADP);
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
	
	private boolean isTargetRightOfSource(Glyph source, Glyph target){
		return (source.getBbox().getX() + source.getBbox().getW()) < target.getBbox().getX();
	}
	
	private boolean isTargetLeftOfSource(Glyph source, Glyph target){
		return source.getBbox().getX() > target.getBbox().getX() + target.getBbox().getW();
	}
	
	private boolean isTargetOverSource(Glyph source, Glyph target){
		return source.getBbox().getY() < target.getBbox().getY();
	}
	
	private boolean isTargetUnderSource(Glyph source, Glyph target){
		return source.getBbox().getY() < target.getBbox().getY();
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
