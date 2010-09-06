package de.zbit.kegg.parser.pathway;

/**
 * Corresponding to the possible Kegg Relation Types (see {@link http
 * ://www.genome.jp/kegg/xml/docs/})
 * 
 * @author wrzodek
 */
public enum RelationType {
	/**
	 * enzyme-enzyme relation, indicating two enzymes catalyzing successive
	 * reaction steps
	 */
	ECrel,
	/**
	 * protein-protein interaction, such as binding and modification
	 */
	PPrel,
	/**
	 * gene expression interaction, indicating relation of transcription factor
	 * and target gene product
	 */
	GErel,
	/**
	 * protein-compound interaction
	 */
	PCrel,
	/**
	 * link to another map
	 */
	maplink,
	/**
	 * 
	 */
	other
}
