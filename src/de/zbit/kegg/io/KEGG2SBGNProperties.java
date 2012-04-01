package de.zbit.kegg.io;

import java.util.HashMap;

import de.zbit.kegg.parser.pathway.EntryType;

/**
 * @author Manuel Ruff
 * @date 2011-12-29
 * @version $Rev: 253 $
 */

public class KEGG2SBGNProperties {
	
	public static HashMap<String, String> determineGlyphType = new HashMap<String, String>();
	static
	{
		determineGlyphType.put(EntryType.compound.name(), GlyphType.simple_chemical.toString());
		determineGlyphType.put(EntryType.enzyme.name(), GlyphType.macromolecule.toString());
		determineGlyphType.put(EntryType.gene.name(), GlyphType.macromolecule.toString());
		determineGlyphType.put(EntryType.group.name(), GlyphType.complex.toString());
		determineGlyphType.put(EntryType.map.name(), GlyphType.submap.toString());
		determineGlyphType.put(EntryType.ortholog.name(), GlyphType.unspecified_entity.toString());
		determineGlyphType.put(EntryType.other.name(), GlyphType.unspecified_entity.toString());
	}

	public static enum GlyphType
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
	
	public static enum GlyphOrientation
	{
        horizontal,
        vertical,
        left,
        right,
        up,
        down,
	}
	
	public static enum ArcType
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
	
}
