package de.zbit.kegg.io;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.sbgn.SbgnUtil;
import org.sbgn.bindings.Arc;
import org.sbgn.bindings.Arc.Start;
import org.sbgn.bindings.Arc.End;
import org.sbgn.bindings.Bbox;
import org.sbgn.bindings.Glyph;
import org.sbgn.bindings.Label;
import org.sbgn.bindings.Map;
import org.sbgn.bindings.Sbgn;

import y.io.GMLIOHandler;
import y.io.GraphMLIOHandler;
import y.io.TGFIOHandler;
import y.io.YGFIOHandler;
import y.view.Graph2D;
import de.zbit.kegg.KeggInfoManagement;
import de.zbit.kegg.KeggInfos;
import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.kegg.parser.pathway.Reaction;
import de.zbit.kegg.parser.pathway.ReactionComponent;
import de.zbit.kegg.parser.pathway.Relation;
import de.zbit.kegg.parser.pathway.SubType;
import de.zbit.kegg.Translator;

import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.kegg.parser.KeggParser;
import de.zbit.util.Utils;

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
// * Copyright (C) 2011 by the University of Tuebingen, Germany.
// *
// * KEGGtranslator is free software; you can redistribute it and/or 
// * modify it under the terms of the GNU Lesser General Public License
// * as published by the Free Software Foundation. A copy of the license
// * agreement is provided in the file named "LICENSE.txt" included with
// * this software distribution and also available online as
// * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
// * ---------------------------------------------------------------------
// */
//package de.zbit.kegg.io;
//
//import java.io.File;
//
//import javax.xml.bind.JAXBException;
//
//import org.sbgn.SbgnUtil;
//import org.sbgn.bindings.Bbox;
//import org.sbgn.bindings.Glyph;
//import org.sbgn.bindings.Label;
//import org.sbgn.bindings.Map;
//import org.sbgn.bindings.Sbgn;
//
//import de.zbit.kegg.KeggInfoManagement;
//import de.zbit.kegg.parser.pathway.Pathway;
//
///**
// * A (not yet fully implemented) implementation of KEGG2SBGN.
// * 
// * <p> Note:<br/>
// * Martijn should be mentioned in 'Acknowledgments', in case of a publication
// * of this method.</p>
// * 
// * @author Martijn van Iersel
// * @author Andreas Dr&auml;ger
// * @date 2011-04-22
// * @version $Rev$
// */
public class KEGG2SBGN extends AbstractKEGGtranslator {
	
	/**
	 * KEGG Entry Types
	 */
	private final String KEGG_ENTRY_TYPE_ORTHOLOG					= "ortholog"; // the node is a KO (ortholog group)
	private final String KEGG_ENTRY_TYPE_ENZYME						= "enzyme"; // the node is an enzyme
	private final String KEGG_ENTRY_TYPE_REACTION					= "reaction"; // the node is a reaction
	private final String KEGG_ENTRY_TYPE_GENE						= "gene"; // the node is a gene product (mostly a protein)
	private final String KEGG_ENTRY_TYPE_GROUP						= "group"; // the node is a complex of gene products (mostly a protein complex)
	private final String KEGG_ENTRY_TYPE_COMPOUND					= "compound"; // the node is a chemical compound (including a glycan)
	private final String KEGG_ENTRY_TYPE_MAP						= "map"; // the node is a linked pathway map
	
	
	/**
	 * KEGG Relation Types
	 */
	private final String KEGG_RELATION_ECREL						= "ECrel"; // enzyme-enzyme relation, indicating two enzymes catalyzing successive reaction steps
	private final String KEGG_RELATION_PPREL						= "PPrel"; // protein-protein interaction, such as binding and modification
	private final String KEGG_RELATION_GEREL						= "GErel"; // gene expression interaction, indicating relation of transcription factor and target gene product
	private final String KEGG_RELATION_PCREL						= "PCrel"; // protein-compound interaction
	private final String KEGG_RELATION_MAPLINK						= "maplink"; // link to another map
	
	/**
	 * KEGG Relation Subtypes
	 */
	private final String KEGG_RELATION_SUBTYPE_COMPOUND				= "compound"; // shared with two successive reactions (ECrel) or intermediate of two interacting proteins (PPrel)
	private final String KEGG_RELATION_SUBTYPE_HIDDEN_COMPOUND		= "hidden compound"; // shared with two successive reactions but not displayed in the pathway map
	private final String KEGG_RELATION_SUBTYPE_ACTIVATION			= "activation"; // positive effects which may be associated with molecular information below
	private final String KEGG_RELATION_SUBTYPE_INHIBITION			= "inhibition"; // negative effects which may be associated with molecular information below
	private final String KEGG_RELATION_SUBTYPE_EXPRESSION			= "expression"; // interactions via DNA binding
	private final String KEGG_RELATION_SUBTYPE_REPRESSION			= "repression"; // interactions via DNA binding
	private final String KEGG_RELATION_SUBTYPE_INDIRECT_EFFECT		= "indirect effect"; // indirect effect without molecular details
	private final String KEGG_RELATION_SUBTYPE_STATE_CHANGE			= "state change"; // state transition
	private final String KEGG_RELATION_SUBTYPE_ASSOCIATION			= "association"; // association
	private final String KEGG_RELATION_SUBTYPE_DISSOCIATION			= "dissociation"; // dissociation
	private final String KEGG_RELATION_SUBTYPE_MISSING_INTERACTION	= "missing interaction"; // missing interaction due to mutation, etc.
	private final String KEGG_RELATION_SUBTYPE_PHOSPHORYLATION		= "phosphorylation"; // molecular event
	private final String KEGG_RELATION_SUBTYPE_DEPHOSPHORYLATION	= "dephosphorylation"; // molecular event
	private final String KEGG_RELATION_SUBTYPE_GLYCOSYLATION		= "glycosylation"; // molecular event
	private final String KEGG_RELATION_SUBTYPE_UBIQUITINATION		= "ubiquitination"; // molecular event
	private final String KEGG_RELATION_SUBTYPE_METHYLATION			= "methylation"; // molecular event
	
	/**
	 * KEGG Reaction Types
	 */
	private final String KEGG_REACTION_REVERSIBLE					= "reversible"; // 	reversible reaction
	private final String KEGG_REACTION_IRREVERSIBLE					= "irreversible"; // irreversible reaction
	
	/**
	 * Entity Pool Node (EPN) Types
	 */
	private final String GLYPH_TYPE_UNSPECIFIED_ENTITY 				= "unspecified entity"; // no direct biological relevance
	private final String GLYPH_TYPE_SIMPLE_CHEMICAL					= "simple chemical"; // atoms, monoatomic ions, a salt, a radical, a solid metal, a crytsal, etc.
	private final String GLYPH_TYPE_MACROMOLECULE					= "macromolecule"; // proteins, nucleic acids (RNA, DNA) and polysaccharides (glycogen, cellulose, starch, etc.)
	private final String GLYPH_TYPE_NUCLEIC_ACID_FEATURE				= "nucleic acid feature"; // genes or transcripts
	private final String GLYPH_TYPE_SIMPLE_CHEMICAL_MULTIMER			= "simple chemical multimer"; 		// a multimer is an aggregation of multiple identical 
	private final String GLYPH_TYPE_MACROMOLECULE_MULTIMER			= "macromolecule multimer";			// or pseudo-identical entities held together by
	private final String GLYPH_TYPE_NUCLEIC_ACID_FEATURE_MULTIMER 	= "nucleic acid feature multimer";	// non-covalent bonds
	private final String GLYPH_TYPE_COMPLEX							= "complex"; // biochemical entity composed of other biochemical entities for example macromolecules, simple chemicals, multimers, etc.
	private final String GLYPH_TYPE_COMPLEX_MULTIMER					= "complex multimer";
	private final String GLYPH_TYPE_SOURCE_AND_SINK					= "source and sink"; // creation of a entity or a state from an unspecified source
	private final String GLYPH_TYPE_PERTURBING_AGENT					= "perturbing agent"; // external influces which affect biochemical networks
	
	/**
	 * Process Node (PN) Types
	 */
	private final String GLYPH_TYPE_PROCESS							= "process"; // a process transforms a set of entitiy pools
	private final String GLYPH_TYPE_OMITTED_PROCESS					= "omitted process"; // existing processes which are omitted from the map
	private final String GLYPH_TYPE_UNCERTAIN_PROCESS				= "uncertain process"; // processes that may not exist
	private final String GLYPH_TYPE_ASSOCIATION						= "association"; // association betwee one or more EPNs (non-covalent binding)
	private final String GLYPH_TYPE_DISSOCIATION						= "dissociation"; // dissociation of an EPN int one or more EPNs
	private final String GLYPH_TYPE_PHENOTYPE						= "phenotype"; // generated phenotypes or affects by biological processes
	
	/**
	 * Logic Operator Node Types
	 */
	private final String GLYPH_TYPE_AND								= "and"; // all EPNs linkes as input are necessary to produce the output
	private final String GLYPH_TYPE_OR								= "or"; // any of the EPNs linked as input is sufficient to produce the output
	private final String GLYPH_TYPE_NOT								= "not"; // the EPN linked as input cannot produce the output
	
	/**
	 * Sub-Glyphs on Node Types
	 */
	private final String GLYPH_TYPE_STATE_VARIABLE					= "state variable"; // existance of molecules in different states for example amino acids, nucleosides or glucid residues
	private final String GLYPH_TYPE_UNIT_OF_INFORMATION				= "unit of information"; // binding domain, catalytic site, promoter, etc.
	
	/**
	 * Sub-Glyphs on Arc Types
	 */
	private final String GLYPH_TYPE_STOICHIOMETRY					= "stoichiometry";
	
	/**
	 * Other Glyph Types
	 */
	private final String GLYPH_TYPE_COMPARTMENT						= "compartment"; // logical or physical structure that contains entity pool nodes
	private final String GLYPH_TYPE_SUBMAP							= "submap"; // encapsulate processes within one glyph
	private final String GLYPH_TYPE_TAG								= "tag"; // links or relationships between elements of a map and sub-map
	
	/**
	 * Glyph Orientation Types
	 */
	private final String GLYPH_ORIENTATION_HORIZONTAL				= "horizontal";
	private final String GLYPH_ORIENTATION_VERTICAL					= "vertical";
	private final String GLYPH_ORIENTATION_LEFT						= "left";
	private final String GLYPH_ORIENTATION_RIGHT					= "right";
	private final String GLYPH_ORIENTATION_UP						= "up";
	private final String GLYPH_ORIENTATION_DOWN						= "down";
	
	/**
	 * Production and Consumption Arc Types
	 */
	private final String ARC_TYPE_PRODUCTION						= "production"; // Production of an entity pool by a process
	private final String ARC_TYPE_CONSUMPTION						= "consumption"; // Consumption of an entity pool by a process
	
	/**
	 * Arc Modification Types
	 */
	private final String ARC_TYPE_MODULATION						= "modulation"; // affects the flux of a process (positively, negatively or both)
	private final String ARC_TYPE_STIMULATION						= "stimulation"; // positive affect of the flux of a process
	private final String ARC_TYPE_CATALYSIS							= "catalysis"; // the effector affects positively the flux of a process
	private final String ARC_TYPE_INHIBITION						= "inhibition"; // negative affect of the flux of a process
	private final String ARC_TYPE_NECESSARY_STIMULATION				= "necessary stimulation"; // necessary for a process to take place
	
	/**
	 * Other Arc Types
	 */
	private final String ARC_TYPE_LOGIC								= "logic arc"; // entity influences the outcome of a logic operator
	private final String ARC_TYPE_EQUIVALENCE						= "equivalence arc"; // all entities marked by a tag are equivalent
	
	/**
	 * Constructor
	 * @param manager
	 */
	public KEGG2SBGN(KeggInfoManagement manager) {
		super(manager);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * transform the glyph type of a KEGG Entry into a SBGN glyph type
	 * @param glyphType	String of a KEGG glyph type
	 * @return SBGN glyph type
	 */
	protected String determineGlyphType(String glyphType){
		String result = GLYPH_TYPE_UNSPECIFIED_ENTITY;
		
		if(glyphType.equalsIgnoreCase(KEGG_ENTRY_TYPE_COMPOUND))
			result = GLYPH_TYPE_COMPLEX;
		if(glyphType.equalsIgnoreCase(KEGG_ENTRY_TYPE_ENZYME))
			result = GLYPH_TYPE_MACROMOLECULE;
		if(glyphType.equalsIgnoreCase(KEGG_ENTRY_TYPE_GENE))
			result = GLYPH_TYPE_NUCLEIC_ACID_FEATURE; // GLYP_TYPE_MACROMOLECULE
		if(glyphType.equalsIgnoreCase(KEGG_ENTRY_TYPE_GROUP))
			result = GLYPH_TYPE_NUCLEIC_ACID_FEATURE_MULTIMER;
		if(glyphType.equalsIgnoreCase(KEGG_ENTRY_TYPE_MAP))
			result = GLYPH_TYPE_TAG; // TODO: check this one
		if(glyphType.equalsIgnoreCase(KEGG_ENTRY_TYPE_ORTHOLOG))
			result = GLYPH_TYPE_SUBMAP;
		if(glyphType.equalsIgnoreCase(KEGG_ENTRY_TYPE_REACTION))
			result = GLYPH_TYPE_PROCESS;
			
		return result;
	}
	
	/**
	 * transform the KEGG relation or reaction into a SBGN arc
	 * @param arcType	String of a KEGG relation or reaction type
	 * @return SBGN arc type
	 */
	protected String determineArcType(String arcType){
		String result = ARC_TYPE_CONSUMPTION;
		
		if(arcType.equalsIgnoreCase(KEGG_REACTION_IRREVERSIBLE))
			result = "";
		if(arcType.equalsIgnoreCase(KEGG_REACTION_REVERSIBLE))
			result = "";
		if(arcType.equalsIgnoreCase(KEGG_RELATION_ECREL))
			result = "";
		if(arcType.equalsIgnoreCase(KEGG_RELATION_GEREL))
			result = "";
		if(arcType.equalsIgnoreCase(KEGG_RELATION_MAPLINK))
			result = "";
		if(arcType.equalsIgnoreCase(KEGG_RELATION_PCREL))
			result = "";
		if(arcType.equalsIgnoreCase(KEGG_RELATION_PPREL))
			result = "";
		return result;
	}
	
	/**
	 * transform the KEGG relation subtype into a SBGN arc
	 * @param arcSubtype String of a KEGG relation subtype
	 * @return SBGN arc type
	 */
	protected String determineArcSubType(String arcSubtype){
		String result = ARC_TYPE_CONSUMPTION;
		
		if(arcSubtype.equalsIgnoreCase(KEGG_RELATION_SUBTYPE_ACTIVATION))
			result = "";
		if(arcSubtype.equalsIgnoreCase(KEGG_RELATION_SUBTYPE_ASSOCIATION))
			result = "";
		if(arcSubtype.equalsIgnoreCase(KEGG_RELATION_SUBTYPE_COMPOUND))
			result = "";
		if(arcSubtype.equalsIgnoreCase(KEGG_RELATION_SUBTYPE_DEPHOSPHORYLATION))
			result = "";
		if(arcSubtype.equalsIgnoreCase(KEGG_RELATION_SUBTYPE_DISSOCIATION))
			result = "";
		if(arcSubtype.equalsIgnoreCase(KEGG_RELATION_SUBTYPE_EXPRESSION))
			result = "";
		if(arcSubtype.equalsIgnoreCase(KEGG_RELATION_SUBTYPE_GLYCOSYLATION))
			result = "";
		if(arcSubtype.equalsIgnoreCase(KEGG_RELATION_SUBTYPE_HIDDEN_COMPOUND))
			result = "";
		if(arcSubtype.equalsIgnoreCase(KEGG_RELATION_SUBTYPE_INDIRECT_EFFECT))
			result = "";
		if(arcSubtype.equalsIgnoreCase(KEGG_RELATION_SUBTYPE_INHIBITION))
			result = "";
		if(arcSubtype.equalsIgnoreCase(KEGG_RELATION_SUBTYPE_METHYLATION))
			result = "";
		if(arcSubtype.equalsIgnoreCase(KEGG_RELATION_SUBTYPE_MISSING_INTERACTION))
			result = "";
		if(arcSubtype.equalsIgnoreCase(KEGG_RELATION_SUBTYPE_PHOSPHORYLATION))
			result = "";
		if(arcSubtype.equalsIgnoreCase(KEGG_RELATION_SUBTYPE_REPRESSION))
			result = "";
		if(arcSubtype.equalsIgnoreCase(KEGG_RELATION_SUBTYPE_STATE_CHANGE))
			result = "";
		if(arcSubtype.equalsIgnoreCase(KEGG_RELATION_SUBTYPE_UBIQUITINATION))
			result = "";
		
		return result;
	}

	@Override
	protected Object translateWithoutPreprocessing(Pathway p) {
		
		Sbgn sbgn = new Sbgn();
		Map map = new Map();
		sbgn.setMap (map);
		
//		List<KeggInfos> keggInfos = new LinkedList<KeggInfos>();
		// TODO : check if needed for additional informations
		
		//////////////////////////////////
		// for every entry in the pathway
		////////////////////////////////
		for (Entry e : p.getEntries())
		{	
			// initiate
			Glyph g = new Glyph();
			Bbox bb = new Bbox();
			Label l = new Label();
		
			// call KeggInfos for the correct name and additional informations
			KeggInfos infos = KeggInfos.get(e.getName(), manager);
			
			// define the bounding box
			bb.setX(e.getGraphics().getX());
			bb.setY(e.getGraphics().getY());
			bb.setW(e.getGraphics().getWidth());
			bb.setH(e.getGraphics().getHeight());
			
			// set the label name according to the KeggInfos fetched
			l.setText(infos.getName());
			
			// set the values for the glyph
			// TODO: check whats happening if there are submaps etc.
			g.setClazz(this.determineGlyphType(e.getType().toString()));
			g.setBbox(bb);
			g.setId("glyph" + String.valueOf(e.getId()));
			g.setLabel(l);
			g.setOrientation(GLYPH_ORIENTATION_HORIZONTAL);
			
			// put the glyph into the map
			sbgn.getMap().getGlyph().add(g);
		}
		
		/////////////////////////////////////
		// for every relation in the pathway
		///////////////////////////////////
		for (Relation relation : p.getRelations())
		{
			// initiate
			Arc a = new Arc();
			Start start = new Start();
			End end = new End();
			
			// grab the first and second entry
			Glyph entry1 = map.getGlyph().get(relation.getEntry1());
			Glyph entry2 = map.getGlyph().get(relation.getEntry2());
			
			// set start and end
			// TODO: check for the direction
			start.setX(entry1.getBbox().getX());
			start.setY(entry1.getBbox().getY());
			end.setX(entry2.getBbox().getX());
			end.setY(entry2.getBbox().getY());
			
			// set arc attributes
			// TODO: check for the subtype element
			a.setSource(entry1);
			a.setTarget(entry2);
			a.setClazz(relation.getType().toString());
			a.setStart(start);
			a.setEnd(end);
			
			// put the arc into the map
			sbgn.getMap().getArc().add(a);
		}
		
		/////////////////////////////////////
		// for every reaction in the pathway
		///////////////////////////////////
		for(Reaction reaction : p.getReactions())
		{
			// initiate
			Arc a = new Arc();
			Start start = new Start();
			End end = new End();
			
			// set start and end points
			// TODO: this
			
			// set the reaction type
			a.setClazz(reaction.getType().toString());
			
			// TODO: check other values and complete the arc part
			// TODO: check for subtrates / products / reactants
			
			// put the arc into the map
			sbgn.getMap().getArc().add(a);
		}
	
		return sbgn;
	}
	
	@Override
	public boolean writeToFile(Object doc, String outFile) {
		// TODO Auto-generated method stub
		try {
			SbgnUtil.writeToFile((Sbgn)doc, new File (outFile));
			return true;
		} catch (JAXBException e) {
			return false;
		}
	}
	
	public static void main(String[] args) throws Exception {
	    KeggInfoManagement manager;
	    if (new File(Translator.cacheFileName).exists()
	        && new File(Translator.cacheFileName).length() > 1) {
	      manager = (KeggInfoManagement) KeggInfoManagement.loadFromFilesystem(Translator.cacheFileName);
	    } else {
	      manager = new KeggInfoManagement();
	    }
	    
		KEGG2SBGN sbgn = new KEGG2SBGN(manager);
		
		if(args.length == 1){
			Pathway p = KeggParser.parse(args[0]).get(0);
//			sbgn.translateWithoutPreprocessing(p);
			sbgn.writeToFile(sbgn.translateWithoutPreprocessing(p), "sbgn.xml");
		}
	}

	@Override
	protected boolean considerRelations() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean considerReactions() {
		// TODO Auto-generated method stub
		return false;
	}
}
