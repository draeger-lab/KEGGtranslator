package de.zbit.kegg.parser.pathway;

/**
 * Corresponding to the Kegg SubType class (see {@link http
 * ://www.genome.jp/kegg/xml/docs/})
 * 
 * @author wrzodek
 */
public class SubType {
	// SubType is for RELATION
	/*
	 * name and value attributes
	 * 
	 * The name attribute specifies the subcategory and/or the additional
	 * information in each of the three types of the generalized protein
	 * interactions. The correspondence between the type attribute of the
	 * relation element (ECrel, PPrel or GErel) and the name and value
	 * attributes of the subtype element is shown below. name value ECrel PPrel
	 * GErel Explanation compound Entry element id attribute value for compound.
	 * * * shared with two successive reactions (ECrel) or intermediate of two
	 * interacting proteins (PPrel) hidden compound Entry element id attribute
	 * value for hidden compound. * shared with two successive reactions but not
	 * displayed in the pathway map activation --> * positive and negative
	 * effects which may be associated with molecular information below
	 * inhibition --| * expression --> * interactions via DNA binding repression
	 * --| * indirect effect ..> * * indirect effect without molecular details
	 * state change ... * state transition binding/association --- * association
	 * and dissociation dissociation -+- * missing interaction -/- * * missing
	 * interaction due to mutation, etc. phosphorylation +p * molecular events
	 * dephosphorylation -p * glycosylation +g * ubiquitination +u * methylation
	 * +m *
	 */

	/**
	 * <ul>
	 *   <li>compound</li>
	 *   <li>hidden compound</li>
	 *   <li>activation</li>
	 *   <li>inhibition</li>
	 *   <li>expression</li>
	 *   <li>repression</li>
	 *   <li>indirect effect</li>
	 *   <li>state change</li>
	 *   <li>binding/association</li>
	 *   <li>dissociation</li>
	 *   <li>missing interaction</li>
	 *   <li>phosphorylation</li>
	 *   <li>dephosphorylation</li>
	 *   <li>glycosylation</li>
	 *   <li>ubiquitination</li>
	 *   <li>methylation</li>
	 * </ul>
	 */
	String name;
	/**
	 * 
	 */
	String value;

	/**
	 * 
	 * @param name
	 */
	public SubType(String name) {
		super();
		this.name = name.trim();

		// Values according to http://www.genome.jp/kegg/xml/docs/
		// compound and hidden compound default to
		// "Entry element id attribute for (hidden) compound".
		if (name.equalsIgnoreCase("activation")) {
			value = "-->";
		} else if (name.equalsIgnoreCase("inhibition")) {
			value = "--|";
		} else if (name.equalsIgnoreCase("expression")) {
			value = "-->";
		} else if (name.equalsIgnoreCase("repression")) {
			value = "--|";
		} else if (name.equalsIgnoreCase("indirect effect")) {
			value = "..>";
		} else if (name.equalsIgnoreCase("state change")) {
			value = "...";
		} else if (name.equalsIgnoreCase("binding/association")) {
			value = "---";
		} else if (name.equalsIgnoreCase("binding")) {
			value = "---";
		} else if (name.equalsIgnoreCase("association")) {
			value = "---";
		} else if (name.equalsIgnoreCase("dissociation")) {
			value = "-+-";
		} else if (name.equalsIgnoreCase("missing interaction")) {
			value = "-/-";
		} else if (name.equalsIgnoreCase("phosphorylation")) {
			value = "+p";
		} else if (name.equalsIgnoreCase("dephosphorylation")) {
			value = "-p";
		} else if (name.equalsIgnoreCase("glycosylation")) {
			value = "+g";
		} else if (name.equalsIgnoreCase("ubiquitination")) {
			value = "+u";
		} else if (name.equalsIgnoreCase("methylation")) {
			value = "+m";
		}
	}

	/**
	 * 
	 * @param name
	 * @param value
	 */
	public SubType(String name, String value) {
		this(name);
		if (value != null && value.length() != 0)
			setValue(value);
	}

	/**
	 * 
	 * @return
	 */
	public String getName() {
		return (name != null) ? name : "";
	}

	/**
	 * 
	 * @return
	 */
	public String getValue() {
		return value;
	}

	/**
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 
	 * @param value
	 */
	public void setValue(String value) {
		this.value = value.replace("&gt;", ">").trim(); // + HTML Code
														// korrekturen
	}

}
