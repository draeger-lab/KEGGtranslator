package de.zbit.kegg.io;

import java.util.HashMap;
import java.util.logging.Logger;

import org.sbgn.bindings.Glyph;

import de.zbit.kegg.parser.pathway.EntryType;

/**
 * Properties for 2SBGN translations.
 * @author Manuel Ruff
 * @author Clemens Wrzodek
 * @date 2011-12-29
 * @version $Rev: 253 $
 */

public class KEGG2SBGNProperties {
	
  public static final Logger log = Logger.getLogger(KEGG2SBGNProperties.class.getName());
  
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
	    	return this.name().replace('_', ' ');
	    }
	    
	    /**
	     * @param arg0
	     * @return reverse of {@link #toString()}
	     */
	    public static GlyphType valueOfString(String arg0) {
	      return valueOf(arg0.replace(' ', '_'));
	    }
	    
	    public int getSBOterm() {
	      switch(this) {
          case association:
            return 177;//=non-covalent binding
          case compartment:
            return 290;//=physical compartment
          case complex:
            return 253;//=non-covalent complex
          case complex_multimer:
            return 418;//=multimer of complexes
          case dissociation:
            return 180;//=dissociation.
          case macromolecule:
            return 245; //=macromolecule
          case nucleic_acid_feature:
            return 354; // = informational molecule segment
          case omitted_process:
            return 397;//=omitted process.
          case perturbing_agent:
            return 405;//=perturbing agent
          case phenotype:
            return 358;//=phenotype
          case process:
            return 375;//=process
          case simple_chemical:
            return 247; //=simple chemical
          case simple_chemical_multimer:
            return 421;//=multimer of simple chemicals
          case source_and_sink:
            return 291;//=empty set
          case submap:
            return 395;//=encapsulating process

          case uncertain_process:
            return 396;//=uncertain process.
            
          default:
            return 285; //=material entity of unspecified nature
	        
	      }
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
	
	/**
	 * A collection of enum states for {@link Glyph}s, according
	 * to Table 2.4 of the SBGN Specification.
	 * 
	 * @author Clemens Wrzodek
	 * @version $Rev$
	 */
	public static enum GlyphState {
	  Acetylation,// Ac SBO:0000215
	  Glycosylation, // G SBO:0000217
	  Hydroxylation, // OH SBO:0000233
	  Methylation, // Me SBO:0000214
	  Myristoylation, // My SBO:0000219
	  Palmytoylation, // Pa SBO:0000218
	  Phosphorylation, // P SBO:0000216
	  Prenylation, // Pr SBO:0000221
	  Protonation, // H SBO:0000212
	  Sulfation, // S SBO:0000220
	  Ubiquitination;// Ub SBO:0000224
	  
	  public int getSBOterm() {
	    switch(this) {
        case Acetylation:
          return 215;
        case Glycosylation:
          return 217;
        case Hydroxylation:
          return 233;
        case Methylation:
          return 214;
        case Myristoylation:
          return 219;
        case Palmytoylation:
          return 218;
        case Phosphorylation:
          return 216;
        case Prenylation:
          return 221;
        case Protonation:
          return 212;
        case Sulfation:
          return 220;
        case Ubiquitination:
          return 224;
	    }
	    // This should actually never get returned!
	    log.warning(String.format("Please set an sbo term for GlyphState '%s'.",toString()));
	    return 0;
	  }
	  
	  public String getLabel() {
	    switch(this) {
	      // Special cases
        case Hydroxylation:
          return "OH";
        case Protonation:
          return "H";
          
          // First-letter returns
        case Phosphorylation:
        case Sulfation:
        case Glycosylation:
          return Character.toString(toString().charAt(0));
          
          // All First-two-letter returns (default)
        default:
          return toString().substring(0, 2);
	      
	    }
	  }
	  
	  public String getName() {
	    return toString();
	  }
	}
	
}
