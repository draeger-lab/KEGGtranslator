package de.zbit.kegg;

import de.zbit.kegg.parser.pathway.EntryType;

/**
 * Simplyfies parsing KeggInfos from the Adaptor. Stores and handles all kegg
 * infos very conveniently.
 * 
 * @author wrzodek
 */
public class KeggInfos {
	/**
	 * 
	 * @param go_id
	 * @return
	 */
	public static String getGo_id_with_MiriamURN(String go_id) {
		// So aufgebaut, da GO_id mehrere enth�lt! Eine funktion muss also
		// dr�ber iterieren und diese aufrugen.
		return miriam_urn_geneOntology
				+ (go_id.contains(":") ? go_id.trim() : "GO:" + go_id.trim());
	}

	/**
	 * @param keggId
	 * @return Complete Miriam URN Including the given ID.
	 */
	public static String getMiriamURIforKeggID(String keggId, EntryType et) {
		String prefix = keggId.toLowerCase().trim();
		int pos = keggId.indexOf(':');
		if (pos <= 0) {
			System.err
					.println("Invalid Kegg ID submitted. Please submit the full id e.g. 'cpd:12345'. You submitted:"
							+ keggId);
			return null;
		}
		String suffix = keggId.substring(pos + 1).trim();
		String ret = "";

		// Add Kegg-id Miriam identifier
		if (prefix.startsWith("cpd:")) {
			ret = miriam_urn_kgCompound + suffix;
		} else if (prefix.startsWith("glycan:")) {
			ret = miriam_urn_kgGlycan + suffix;
		} else if (prefix.startsWith("ec:")) {
			ret = miriam_urn_ezymeECcode + suffix;
		} else if (prefix.startsWith("dr:")) {
			ret = miriam_urn_kgDrug + suffix;
		} else if (prefix.startsWith("rn:")) {
			ret = miriam_urn_kgReaction + suffix;
		} else if (prefix.startsWith("path:")) { // Link to another pathway
			ret = miriam_urn_kgPathway + suffix;
		} else if (et == null && prefix.startsWith("ko:") || et != null
				&& (et.equals(EntryType.gene) || et.equals(EntryType.ortholog))) {// z.B.
			// hsa:00123,
			// ko:00123
			ret = miriam_urn_kgGenes + keggId.trim(); // Be careful here: Don't
			// trim to ':'! (Don't
			// use suffix)
		} else {
			System.err.println("Please implement MIRIAM urn for: '" + keggId
					+ ((et != null) ? "' (" + et.toString() + ")." : "."));
			return null;
		}
		return ret;
	}

	/**
	 * If s contains ":" => return values is all chars behind the ":". Else =>
	 * return value is s.
	 * 
	 * @param s
	 *            - any String.
	 * @return see above. Result is always trimmed.
	 */
	public static String suffix(String s) {
		if (!s.contains(":")){
			return s.trim();
		}
			return (s.substring(s.indexOf(':') + 1)).trim();
	}

	// private KeggAdaptor adap;
	/**
   * 
   */
	private String Kegg_ID;
	/**
   * 
   */
	private String informationFromKeggAdaptor;
	/**
   * 
   */
	private String taxonomy = null;
	/**
   * 
   */
	private String definition = null;
	/**
   * 
   */
	private String description = null;
	/**
	 * multiple synonyms separated by ;
	 */
	private String names = null;
	/**
	 * Last entry (mostly the most meaningful) in names.
	 */
	private String name = null;
	/**
	 * Be careful: multiple numbers without "GO:". e.g., "0006096 0006094"
	 * instead of "GO:0006096"
	 */
	private String go_id = null;
	/**
	 * Be careful: multiple numbers without "RN:", e.g., "R05966 R05967"
	 */
	private String reaction_id = null;
	/**
   * 
   */
	private String ensembl_id = null;
	/**
   * 
   */
	private String uniprot_id = null;
	/**
   * 
   */
	private String hgnc_id = null;
	/**
   * 
   */
	private String omim_id = null;
	/**
   * 
   */
	private String entrez_id = null;
	/**
   * 
   */
	private String formula = null;
	/**
   * 
   */
	private String mass = null;
	/**
	 * <ul>
	 * <li>urn:miriam:pubchem.compound</li>
	 * <li>urn:miriam:pubchem.substance</li>
	 * <li>urn:miriam:pubchem.bioassay</li>
	 * </ul>
	 */
	private String pubchem = null;
	/**
	 * urn:miriam:obo.chebi, be careful! E.g., CHEBI:36927
	 */
	private String chebi = null;
	/**
	 * urn:miriam:3dmet
	 */
	private String three_dmet = null;
	/**
	 * NOT IN MIRIAM :(
	 * "CAS registry number, unique numerical identifiers for chemical substances"
	 */
	private String cas = null;
	/**
	 * urn:miriam:drugbank
	 */
	private String drugbank = null;
	/**
	 * e.g., "G10609 + G00094 <=> G10619 + G00097"
	 */
	private String equation = null;
	/**
	 * Referenced "PATHWAY" ids, without PATH: and comma separated.
	 */
	private String pathways = null;
	/**
	 * Description of referenced "PATHWAY" ids, comma separated. Comments behind
	 * URNs are examples.
	 */
	private String pathwayDescs = null;
	/**
	 * 9606
	 */
	public static final String miriam_urn_taxonomy = "urn:miriam:taxonomy:";
	/**
	 * GO:0006915
	 */
	public static final String miriam_urn_geneOntology = "urn:miriam:obo.go:";
	/**
	 * hsa00620
	 */
	public static final String miriam_urn_kgPathway = "urn:miriam:kegg.pathway:";
	/**
	 * C12345
	 */
	public static final String miriam_urn_kgCompound = "urn:miriam:kegg.compound:";
	/**
	 * G00123
	 */
	public static final String miriam_urn_kgGlycan = "urn:miriam:kegg.glycan:";
	/**
	 * D00123
	 */
	public static final String miriam_urn_kgDrug = "urn:miriam:kegg.drug:";
	/**
	 * syn:ssr3451
	 */
	public static final String miriam_urn_kgGenes = "urn:miriam:kegg.genes:";
	/**
	 * R00100
	 */
	public static final String miriam_urn_kgReaction = "urn:miriam:kegg.reaction:";
	/**
	 * 1.1.1.1
	 */
	public static final String miriam_urn_ezymeECcode = "urn:miriam:ec-code:";
	/**
	 * ENSG00000139618
	 */
	public static final String miriam_urn_ensembl = "urn:miriam:ensembl:";
	/**
	 * P62158
	 */
	public static final String miriam_urn_uniprot = "urn:miriam:uniprot:";
	/**
	 * HGNC:2674
	 */
	public static final String miriam_urn_hgnc = "urn:miriam:hgnc:";
	/**
	 * 603903
	 */
	public static final String miriam_urn_omim = "urn:miriam:omim:";
	/**
	 * 100010
	 */
	public static final String miriam_urn_entrezGene = "urn:miriam:entrez.gene:";
	/**
	 * 100101
	 */
	public static final String miriam_urn_PubChem_Compound = "urn:miriam:pubchem.compound:";
	/**
	 * 100101
	 */
	public static final String miriam_urn_PubChem_Substance = "urn:miriam:pubchem.substance:";
	/**
	 * 1018
	 */
	public static final String miriam_urn_PubChem_Bioassay = "urn:miriam:pubchem.bioassay:";
	/**
	 * CHEBI:36927
	 */
	public static final String miriam_urn_chebi = "urn:miriam:obo.chebi:";
	/**
	 * B00162
	 */
	public static final String miriam_urn_3dmet = "urn:miriam:3dmet:";
	/**
	 * DB00001
	 */
	public static final String miriam_urn_drugbank = "urn:miriam:drugbank:";

	// GO, kgGenes, hgnc, chebi - require extra prefixes!

	/**
	 * Same as 'getMiriamURIforKeggID(keggId, null)'. Please submit EntryTyp
	 * when available.
	 * 
	 * @param keggId
	 * @return Complete Miriam URN Including the given ID.
	 */
	public static String getMiriamURIforKeggID(String keggId) {
		return getMiriamURIforKeggID(keggId, null);
	}

	/**
	 * Please use 'KeggInfos(String Kegg_ID, KeggInfoManagement info)' if
	 * possible.
	 */
	public KeggInfos(String Kegg_ID) {
		this(Kegg_ID, new KeggAdaptor());
	}

	/**
	 * Please use 'KeggInfos(String Kegg_ID, KeggInfoManagement info)' if
	 * possible.
	 */
	public KeggInfos(String Kegg_ID, KeggAdaptor adap) {
		this.Kegg_ID = Kegg_ID;
		// this.adap = adap;

		informationFromKeggAdaptor = adap.get(Kegg_ID);
		parseInfos();
	}

	/**
	 * 
	 * @param Kegg_ID
	 * @param info
	 */
	public KeggInfos(String Kegg_ID, KeggInfoManagement info) {
		this.Kegg_ID = Kegg_ID;

		informationFromKeggAdaptor = info.getInformation(Kegg_ID);
		// this.adap = info.getKeggAdaptor();
		parseInfos();
	}

	/**
	 * 
	 * @return
	 */
	public boolean containsMultipleNames() {
		return names != null ? names.contains(";") : false;
	}

	/**
	 * 
	 * @return
	 */
	public String getCas() {
		return cas;
	}

	/**
	 * 
	 * @return
	 */
	public String getChebi() {
		return chebi;
	}

	/**
	 * 
	 * @return
	 */
	public String getChebi_with_MiriamURN() {
		return miriam_urn_chebi
				+ (chebi.contains(":") ? chebi.trim() : "CHEBI:" + chebi.trim());
	}

	/**
	 * 
	 * @return
	 */
	public String getDefinition() {
		return definition;
	}

	/**
	 * 
	 * @return
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * 
	 * @return
	 */
	public String getDrugbank() {
		return drugbank;
	}

	/**
	 * 
	 * @return
	 */
	public String getDrugbank_with_MiriamURN() {
		return miriam_urn_drugbank + suffix(drugbank);
	}

	/**
	 * 
	 * @return
	 */
	public String getEnsembl_id() {
		return ensembl_id;
	}

	/**
	 * 
	 * @return
	 */
	public String getEnsembl_id_with_MiriamURN() {
		return miriam_urn_ensembl + suffix(ensembl_id);
	}

	/**
	 * 
	 * @return
	 */
	public String getEntrez_id() {
		return entrez_id;
	}

	/**
	 * 
	 * @return
	 */
	public String getEntrez_id_with_MiriamURN() {
		return miriam_urn_entrezGene + suffix(entrez_id);
	}

	/**
	 * 
	 * @return
	 */
	public String getEquation() {
		return equation;
	}

	/**
	 * 
	 * @return
	 */
	public String getFormula() {
		return formula;
	}

	/**
	 * Maybe multiple values separated by space!
	 */
	public String getGo_id() {
		return go_id;
	}

	/**
	 * 
	 * @return
	 */
	public String getHgnc_id() {
		return hgnc_id;
	}

	/**
	 * 
	 * @return
	 */
	public String getHgnc_id_with_MiriamURN() {
		return miriam_urn_hgnc
				+ (hgnc_id.contains(":") ? hgnc_id.trim() : "HGNC:"
						+ hgnc_id.trim());
	}

	/**
	 * 
	 * @return
	 */
	public String getInformationFromKeggAdaptor() {
		return informationFromKeggAdaptor;
	}

	/**
	 * 
	 * @return
	 */
	public String getKegg_ID() {
		return Kegg_ID;
	}

	/**
	 * 
	 * @return
	 */
	public String getKegg_ID_with_MiriamURN() {
		return getMiriamURIforKeggID(this.Kegg_ID);
	}

	/**
	 * 
	 * @return
	 */
	public String getMass() {
		return mass;
	}

	/**
	 * Hopefully the most meaningfull name (last in the list of getNames() ).
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * All names, separated by ;
	 * 
	 * @return
	 */
	public String getNames() {
		return names;
	}

	/**
	 * 
	 * @return
	 */
	public String getOmim_id() {
		return omim_id;
	}

	/**
	 * 
	 * @return
	 */
	public String getOmim_id_with_MiriamURN() {
		return miriam_urn_omim + suffix(omim_id);
	}

	/**
	 * 
	 * @return
	 */
	public String getPathwayDescriptions() {
		// Of REFENRECED pathways. not the actual queried one (if one queries a
		// PW).
		return pathwayDescs;
	}

	/**
	 * 
	 * @return
	 */
	public String getPathways() {
		return pathways;
	}

	/**
	 * 
	 * @return
	 */
	public String getPubchem() {
		return pubchem;
	}

	/**
	 * 
	 * @return
	 */
	public String getPubchem_with_MiriamURN() {
		// Pubchem occurs in compounds and drugs, according to
		// http://www.genome.jp/kegg/kegg3.html
		// Even Kegg Compounds refer to Pubchem Substance unnits. NOT to Pubchem
		// compounds. Strange... but true.
		return miriam_urn_PubChem_Substance + suffix(pubchem);
	}

	/**
	 * 
	 * @return
	 */
	public String getReaction_id() {
		return reaction_id;
	}

	/**
	 * 
	 * @return
	 */
	public String getReaction_id_with_MiriamURN() {
		return miriam_urn_kgReaction + suffix(reaction_id);
	}

	/**
	 * 
	 * @return
	 */
	public String getTaxonomy() {
		return taxonomy;
	}

	/**
	 * 
	 * @return
	 */
	public String getTaxonomy_with_MiriamURN() {
		return miriam_urn_taxonomy + suffix(taxonomy);
	}

	/**
	 * 
	 * @return
	 */
	public String getThree_dmet() {
		return three_dmet;
	}

	/**
	 * 
	 * @return
	 */
	public String getThree_dmet_with_MiriamURN() {
		return miriam_urn_3dmet + suffix(three_dmet);
	}

	/**
	 * 
	 * @return
	 */
	public String getUniprot_id() {
		return uniprot_id;
	}

	/**
	 * 
	 * @return
	 */
	public String getUniprot_id_with_MiriamURN() {
		return miriam_urn_uniprot + suffix(uniprot_id);
	}

	/**
   * 
   */
	private void parseInfos() {
		String infos = informationFromKeggAdaptor; // Create a shorter variable
		// name ;-)
		if (infos != null && infos.trim().length() == 0)
			infos = null;
		if (infos == null) {
			informationFromKeggAdaptor = null;
			return;
		}

		// General
		names = KeggAdaptor.extractInfo(infos, "NAME");
		if (names != null && names.length() != 0) {
			int pos = names.lastIndexOf(";");
			if (pos > 0 && pos < (names.length() - 1))
				name = names.substring(pos + 1, names.length()).replace("\n",
						"").trim();
			else
				name = names;
		}
		definition = KeggAdaptor.extractInfo(infos, "DEFINITION");
		description = KeggAdaptor.extractInfo(infos, "DESCRIPTION");

		// Mainly Pathway specific (eg. "path:map00603")
		go_id = KeggAdaptor.extractInfo(infos, " GO:", "\n"); // DBLINKS GO:
		// 0006096
		// 0006094

		// Mainly Organism specific (eg. "GN:hsa")
		taxonomy = KeggAdaptor.extractInfo(infos, "TAXONOMY", "\n"); // e.g.
		// "TAXONOMY    TAX:9606"
		// =>
		// "TAX:9606".

		// Mainly Gene specific (eg. "hsa:12313")
		ensembl_id = KeggAdaptor.extractInfo(infos, "Ensembl:", "\n");
		uniprot_id = KeggAdaptor.extractInfo(infos, "UniProt:", "\n");
		hgnc_id = KeggAdaptor.extractInfo(infos, "HGNC:", "\n");
		omim_id = KeggAdaptor.extractInfo(infos, "OMIM:", "\n");
		entrez_id = KeggAdaptor.extractInfo(infos, "NCBI-GeneID:", "\n");

		// Mainly Glycan specific (eg. "glycan:G00181")
		/*
		 * Sadly non of the following has a miriam URN: CCSD (CarbBank) : 1303
		 * GlycomeDB: 12885 JCGGDB: JCGG-STR003128 LipidBank: GSG1005
		 */

		// Mainly Enzyme specific (eg. "ec:2.4.1.-  ")
		/*
		 * Mostly uninteresting... Product, Substrate, REACTION, Class
		 */

		// Ortholog (e.g. "ko:K01204")
		// DBLINKS (RN, GO); GENES (actual orthologous genes)
		// //urn:miriam:kegg.reaction (R00100)
		reaction_id = KeggAdaptor.extractInfo(infos, " RN:", "\n"); // DBLINKS
		// RN:
		// R05966

		// in small molecules (compound eg. "cpd:C00031")
		// KNApSAcK, NIKKAJI, (CAS) missing
		formula = KeggAdaptor.extractInfo(infos, "FORMULA"); // FORMULA C6H12O6
		mass = KeggAdaptor.extractInfo(infos, "MASS"); // MASS 180.0634

		pubchem = KeggAdaptor.extractInfo(infos, "PubChem:", "\n");
		chebi = KeggAdaptor.extractInfo(infos, "ChEBI:", "\n");
		three_dmet = KeggAdaptor.extractInfo(infos, "3DMET:", "\n");
		cas = KeggAdaptor.extractInfo(infos, " CAS:", "\n");

		// Mainly drg (eg. "dr:D00694")
		// missing: NIKKAJI, LigandBox (CAS)
		drugbank = KeggAdaptor.extractInfo(infos, "DrugBank:", "\n");

		// in reactions:
		equation = KeggAdaptor.extractInfo(infos, "EQUATION", "\n");
		String pathwaysTemp = KeggAdaptor.extractInfo(infos, "PATHWAY");
		if ((pathwaysTemp != null) && (pathwaysTemp.trim().length() != 0)) {
			pathwaysTemp = pathwaysTemp.replace("PATH:", "");
			String[] splitt = pathwaysTemp.split("\n");
			pathways = "";
			pathwayDescs = "";
			for (String s : splitt) {
				if (!s.startsWith(" ") && (pathwayDescs.length() != 0)) {
					/*
					 * Continuation of last line.
					 */
					pathwayDescs += " " + s;
					continue;
				}
				s = s.trim();
				pathways += "," + s.substring(0, s.indexOf(" "));
				pathwayDescs += ","
						+ s.substring(s.indexOf(" ")).trim().replace(",", "");
			}
		}
		if ((pathways != null) && pathways.startsWith(",")) {
			pathways = pathways.substring(1);
		}
		if ((pathwayDescs != null) && pathwayDescs.startsWith(",")) {
			pathwayDescs = pathwayDescs.substring(1);
		}

		// Free Memory instead of storing empty Strings.
		if (taxonomy != null && taxonomy.trim().length() == 0)
			taxonomy = null;
		if (equation != null && equation.trim().length() == 0)
			equation = null;
		if (pathways != null && pathways.trim().length() == 0)
			pathways = null;
		if (pathwayDescs != null && pathwayDescs.trim().length() == 0)
			pathwayDescs = null;
		if (definition != null && definition.trim().length() == 0)
			definition = null;
		if (description != null && description.trim().length() == 0)
			description = null;
		if (names != null && names.trim().length() == 0)
			names = null;
		if (name != null && name.trim().length() == 0)
			name = null;
		if (go_id != null && go_id.trim().length() == 0)
			go_id = null;
		if (reaction_id != null && reaction_id.trim().length() == 0)
			reaction_id = null;
		if (ensembl_id != null && ensembl_id.trim().length() == 0)
			ensembl_id = null;
		if (uniprot_id != null && uniprot_id.trim().length() == 0)
			uniprot_id = null;
		if (hgnc_id != null && hgnc_id.trim().length() == 0)
			hgnc_id = null;
		if (omim_id != null && omim_id.trim().length() == 0)
			omim_id = null;
		if (entrez_id != null && entrez_id.trim().length() == 0)
			entrez_id = null;
		if (formula != null && formula.trim().length() == 0)
			formula = null;
		if (mass != null && mass.trim().length() == 0)
			mass = null;
		if (chebi != null && chebi.trim().length() == 0)
			chebi = null;
		if (three_dmet != null && three_dmet.trim().length() == 0)
			three_dmet = null;
		if (cas != null && cas.trim().length() == 0)
			cas = null;
		if (drugbank != null && drugbank.trim().length() == 0)
			drugbank = null;
		if (pubchem != null && pubchem.trim().length() == 0)
			pubchem = null;
	}

	/**
	 * 
	 * @return
	 */
	public boolean queryWasSuccessfull() {
		return (informationFromKeggAdaptor != null);
	}

	/**
   * 
   */
	public void test() {
		String infos = informationFromKeggAdaptor;
		taxonomy = KeggAdaptor.extractInfo(infos, "TAXONOMY", "\n");
		// e.g., "TAXONOMY    TAX:9606" => "TAX:9606".
		System.out.println(infos + "\n=================================");
		System.out.println(KeggAdaptor.extractInfo(infos, " CAS:", "\n")
				+ " \t=> " + cas);
		System.out.println(KeggAdaptor.extractInfo(infos, "FORMULA", "\n")
				+ " \t=> " + formula);
		System.out.println(KeggAdaptor.extractInfo(infos, "MASS", "\n")
				+ " \t=> " + mass);
	}

}
