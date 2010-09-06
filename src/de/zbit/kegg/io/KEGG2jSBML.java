package de.zbit.kegg.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.InvalidPropertiesFormatException;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Creator;
import org.sbml.jsbml.History;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.CVTerm.Qualifier;
import org.sbml.jsbml.CVTerm.Type;
import org.sbml.jsbml.xml.stax.SBMLWriter;

import de.zbit.kegg.KeggAdaptor;
import de.zbit.kegg.KeggInfoManagement;
import de.zbit.kegg.KeggInfos;
import de.zbit.kegg.parser.KeggParser;
import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.EntryType;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.kegg.parser.pathway.Reaction;
import de.zbit.kegg.parser.pathway.ReactionComponent;
import de.zbit.kegg.parser.pathway.ReactionType;
import de.zbit.kegg.parser.pathway.Relation;
import de.zbit.kegg.parser.pathway.SubType;
import de.zbit.util.EscapeChars;
import de.zbit.util.Info;
import de.zbit.util.ProgressBar;
import de.zbit.util.SortedArrayList;

/**
 * 
 * @author wrzodek
 */
public class KEGG2jSBML implements KeggConverter {
	/**
	 * Retrieve annotations from Kegg or use purely information available in the
	 * document.
	 */
	public static boolean retrieveKeggAnnots = true;
	/**
	 * Generate pure SBML or do you want to add CellDesigner annotations?
	 */
	public static boolean addCellDesignerAnnots = true;
	/**
	 * Remove single, not linked nodes
	 */
	public static boolean removeOrphans = true;
	/**
	 * If false, all relations in the document will be skipped. Just like moste
	 * of the other very-basic converters.
	 */
	public static boolean considerRelations = true;
	/**
	 * If true, all nodes in white color (except for small molecules/ compounds)
	 * will be removed from the graph. Kegg colours all nodes, which do NOT
	 * occur in the current species in white. Removing these nodes is HEAVILY
	 * recommended if you want to use the SBML document for simulations.
	 * 
	 * Set this node to false if you convert generic pathways (not species
	 * specific), since they ONLY contain white nodes.
	 */
	public static boolean removeWhiteNodes = true;
	/*
	 * MIRIAM Kegg IDs: urn:miriam:kegg.pathway (hsa00620)
	 * urn:miriam:kegg.compound (C12345) urn:miriam:kegg.reaction (R00100)
	 * urn:miriam:kegg.drug (D00123) urn:miriam:kegg.glycan (G00123)
	 * urn:miriam:kegg.genes (syn:ssr3451)
	 */
	private static String quotStart = "&#8220;"; // "\u201C";//"&#8220;"; //
	// &ldquo;
	/**
   * 
   */
	private static String quotEnd = "&#8221;"; // "\u201D";//"&#8221;"; //
	// &rdquo;
	/**
   * 
   */
	public static int ET_GeneralModifier2SBO = 13; // 13=catalyst
	// 460="enzymatic catalyst"

	/**
   * 
   */
	public static int ET_EnzymaticModifier2SBO = 460;

	/**
   * 
   */
	public static boolean treatEntrysWithReactionDifferent = true;

	/**
   * 
   */
	public static int ET_Ortholog2SBO = 243; // 243="gene",
	// 404="unit of genetic information"

	/**
   * 
   */
	public static int ET_Enzyme2SBO = 14; // 14="Enzyme",
	// 252="polypeptide chain"

	/**
   * 
   */
	public static int ET_Gene2SBO = 243; // 243="gene"

	/**
   * 
   */
	public static int ET_Group2SBO = 253; // 253="non-covalent complex"

	/**
   * 
   */
	public static int ET_Compound2SBO = 247; // 247="Simple Molecule"

	/**
   * 
   */
	public static int ET_Map2SBO = 291; // 291="Empty set"
	/**
   * 
   */
	public static int ET_Other2SBO = 285; // 285="material entity of unspecified nature"

	/**
	 * 
	 * @param goIDs
	 * @param mtGoID
	 */
	private static void appendAllGOids(String goIDs, CVTerm mtGoID) {
		for (String go_id : goIDs.split(" ")) {
			if (go_id.length() != 7 || !containsOnlyDigits(go_id))
				continue; // Invalid GO id.
			mtGoID.addResource(KeggInfos.getGo_id_with_MiriamURN(go_id));
		}
	}

	/**
	 * Append all IDs with Miriam URNs to a CV term. Multiple IDs are separated
	 * by a space. Only the part behind the ":" will be added (if an ID contains
	 * a ":").
	 * 
	 * @param IDs
	 * @param myCVterm
	 * @param miriam_URNPrefix
	 */
	private static void appendAllIds(String IDs, CVTerm myCVterm,
			String miriam_URNPrefix) {
		for (String id : IDs.split(" ")) {
			myCVterm.addResource(miriam_URNPrefix + KeggInfos.suffix(id));
		}
	}

	/**
	 * Append all IDs with Miriam URNs to a CV term. Multiple IDs are separated
	 * by a space. All ids are required to contain a ":". If not,
	 * mayContainDoublePointButAppendThisStringIfNot will be used. E.g.
	 * "[mayContainDoublePointButAppendThisStringIfNot]:[ID]" or [ID] if it
	 * contains ":".
	 * 
	 * @param IDs
	 * @param myCVterm
	 * @param miriam_URNPrefix
	 * @param mayContainDoublePointButAppendThisStringIfNot
	 */
	private static void appendAllIds(String IDs, CVTerm myCVterm,
			String miriam_URNPrefix,
			String mayContainDoublePointButAppendThisStringIfNot) {
		// Trim double point from
		// 'mayContainDoublePointButAppendThisStringIfNot' eventually.
		if (mayContainDoublePointButAppendThisStringIfNot.endsWith(":"))
			mayContainDoublePointButAppendThisStringIfNot = mayContainDoublePointButAppendThisStringIfNot
					.substring(0, mayContainDoublePointButAppendThisStringIfNot
							.length() - 1);

		for (String id : IDs.split(" ")) {
			myCVterm.addResource(miriam_URNPrefix
					+ (id.contains(":") ? id.trim()
							: mayContainDoublePointButAppendThisStringIfNot
									+ ":" + id.trim()));
		}
	}

	/**
	 * 
	 * @param myString
	 * @return
	 */
	private static boolean containsOnlyDigits(String myString) {
		char[] ch = myString.toCharArray();
		for (char c : ch)
			if (!Character.isDigit(c))
				return false;
		return true;
	}

	/**
	 * Parses a Kegg Pathway and returns the maximum x and y coordinates
	 * 
	 * @param p
	 *            - Kegg Pathway Object
	 * @return int[]{x,y}
	 */
	public static int[] getMaxCoords(Pathway p) {
		int x = 0, y = 0;
		for (Entry e : p.getEntries()) {
			if (e.hasGraphics()) {
				x = Math.max(x, e.getGraphics().getX()
						+ e.getGraphics().getWidth());
				y = Math.max(y, e.getGraphics().getY()
						+ e.getGraphics().getHeight());
			}
		}
		return new int[] { x, y };
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	private static int getSBOTerm(EntryType type) {
		if (type.equals(EntryType.compound))
			return ET_Compound2SBO;
		if (type.equals(EntryType.enzyme))
			return ET_Enzyme2SBO;
		if (type.equals(EntryType.gene))
			return ET_Gene2SBO;
		if (type.equals(EntryType.group))
			return ET_Group2SBO;
		if (type.equals(EntryType.genes))
			return ET_Group2SBO;
		if (type.equals(EntryType.map))
			return ET_Map2SBO;
		if (type.equals(EntryType.ortholog))
			return ET_Ortholog2SBO;

		if (type.equals(EntryType.other))
			return ET_Compound2SBO;
		return ET_Compound2SBO;
	}

	/**
	 * @param args
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws XMLStreamException
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 * @throws InvalidPropertiesFormatException 
	 * @throws SBMLException 
	 */
	public static void main(String[] args) throws XMLStreamException,
			InstantiationException, IllegalAccessException, InvalidPropertiesFormatException, IOException, ClassNotFoundException, SBMLException {
		// Speedup Kegg2SBML by loading alredy queried objects. Reduces network
		// load and heavily reduces computation time.
		KEGG2jSBML k2s;
		if (new File("keggdb.dat").exists()
				&& new File("keggdb.dat").length() > 0) {
			KeggInfoManagement manager = (KeggInfoManagement) KeggInfoManagement
					.loadFromFilesystem("keggdb.dat");
			k2s = new KEGG2jSBML(manager);
		} else {
			k2s = new KEGG2jSBML();
		}
		// ---

		if (args != null && args.length > 0) {
			File f = new File(args[0]);
			if (f.isDirectory()) {
				BatchConvertKegg batch = new BatchConvertKegg();
				batch.setOrgOutdir(args[0]);
				if (args.length > 1)
					batch.setChangeOutdirTo(args[1]);
				batch.setConverter(k2s);
				batch.setOutFormat("sbml");
				batch.parseDirAndSubDir();

			} else {
				// Single file mode.
				String outfile = args[0].substring(0,
						args[0].contains(".") ? args[0].lastIndexOf(".")
								: args[0].length())
						+ ".sbml.xml";
				if (args.length > 1)
					outfile = args[1];
				Pathway p = KeggParser.parse(args[0]).get(0);
				k2s.Convert(p, outfile);

			}

			// Remember already queried objects
			if (k2s.getKeggInfoManager().hasChanged()) {
				KeggInfoManagement.saveToFilesystem("keggdb.dat", k2s
						.getKeggInfoManager());
			}

			return;
		}
		System.out.println("Demo mode.");

		long start = System.currentTimeMillis();
		try {
			k2s.Convert("resources/de/zbit/kegg/samplefiles/map04010hsa.xml",
					"resources/de/zbit/kegg/samplefiles/map04010hsa.sbml.xml");
			// k2s.Kegg2jSBML("resources/de/zbit/kegg/samplefiles/hsa00010.xml");

			// Remember already queried objects
			if (k2s.getKeggInfoManager().hasChanged()) {
				KeggInfoManagement.saveToFilesystem("keggdb.dat", k2s
						.getKeggInfoManager());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Conversion took "
				+ ((System.currentTimeMillis() - start) / 1000 / 60)
				+ " minutes and "
				+ ((System.currentTimeMillis() - start) / 1000 % 60)
				+ " seconds.");

	}

	/**
	 * This manager uses a cache and retrieved informations from the KeggDB. By
	 * using the cache, it is very fast in retrieving informations.
	 */
	private KeggInfoManagement manager;

	/**
	 * Contains all ids already assigned to an element in the sbml document.
	 * Used for avoiding giving the same id to two or more different elements.
	 */
	private ArrayList<String> SIds = new ArrayList<String>();

	/**
	 * A flag, if the last sbml file that has been written by this class was
	 * overwritten. This variable is used by the BatchConverter.
	 */
	private boolean lastFileWasOverwritten = false;

	/**
	 * Temporary Stringbuffers, needed to write CellDesigner annotations. Clear
	 * thos before converting another document!
	 */
	StringBuffer CDloSpeciesAliases = new StringBuffer();
	StringBuffer CDloComplexSpeciesAliases = new StringBuffer();
	StringBuffer CDloProteins = new StringBuffer();

	/**
   * 
   */
	public KEGG2jSBML() {
		this(new KeggInfoManagement(1000, new KeggAdaptor()));
	}

	/**
	 * 
	 * @param manager
	 */
	public KEGG2jSBML(KeggInfoManagement manager) {
		this.manager = manager;
	}

	/**
	 * 
	 * @param p
	 * @param annot
	 * @param defaultC
	 */
	private void addCellDesignerAnnotationToModel(Pathway p, Annotation annot,
			Compartment defaultC) {
		annot.appendNoRDFAnnotation("<celldesigner:extension>\n");
		annot
				.appendNoRDFAnnotation("<celldesigner:modelVersion>4.0</celldesigner:modelVersion>\n");
		int[] maxCoords = getMaxCoords(p);
		annot.appendNoRDFAnnotation("<celldesigner:modelDisplay sizeX=\""
				+ (maxCoords[0] + 22) + "\" sizeY=\"" + (maxCoords[1] + 22)
				+ "\"/>\n");
		annot
				.appendNoRDFAnnotation("<celldesigner:listOfCompartmentAliases>\n");

		annot
				.appendNoRDFAnnotation(String
						.format(
								"<celldesigner:compartmentAlias id=\"cd_ca%s\" compartment=\"%s\">\n",
								defaultC.getId(), defaultC.getId()));
		annot
				.appendNoRDFAnnotation("<celldesigner:class>SQUARE</celldesigner:class>\n");
		annot
				.appendNoRDFAnnotation(String
						.format(
								"<celldesigner:bounds x=\"10.0\" y=\"10.0\" w=\"%d\" h=\"%d\" />\n",
								(maxCoords[0] + 2), (maxCoords[1] + 2)));
		// <celldesigner:namePoint x="WIDTH HALBE - TEXT_WIDHT HALB"
		// y="COMPARTMENT_HEIGHT-25"/>
		annot.appendNoRDFAnnotation(String.format(
				"<celldesigner:namePoint x=\"%d\" y=\"%d\"/>\n",
				((maxCoords[0] + 22) / 2 - (3 * defaultC.getName().length())),
				maxCoords[1] - 22));
		annot
				.appendNoRDFAnnotation("<celldesigner:doubleLine thickness=\"10.0\" outerWidth=\"2.0\" innerWidth=\"1.0\"/>\n");
		annot
				.appendNoRDFAnnotation("<celldesigner:paint color=\"ffcccc00\" scheme=\"Color\" />\n");
		annot
				.appendNoRDFAnnotation("<celldesigner:info state=\"empty\" angle=\"0.0\"/>\n");
		annot.appendNoRDFAnnotation("</celldesigner:compartmentAlias>\n");

		annot
				.appendNoRDFAnnotation("</celldesigner:listOfCompartmentAliases>\n");

		if (CDloComplexSpeciesAliases.length() > 0) {
			annot
					.appendNoRDFAnnotation("<celldesigner:listOfComplexSpeciesAliases>\n");
			annot.appendNoRDFAnnotation(CDloComplexSpeciesAliases.toString());
			annot
					.appendNoRDFAnnotation("</celldesigner:listOfComplexSpeciesAliases>\n");
		} else {
			annot
					.appendNoRDFAnnotation("<celldesigner:listOfComplexSpeciesAliases/>\n");
		}
		if (CDloSpeciesAliases.length() > 0) {
			annot
					.appendNoRDFAnnotation("<celldesigner:listOfSpeciesAliases>\n");
			annot.appendNoRDFAnnotation(CDloSpeciesAliases.toString());
			annot
					.appendNoRDFAnnotation("</celldesigner:listOfSpeciesAliases>\n");
		} else {
			annot
					.appendNoRDFAnnotation("<celldesigner:listOfSpeciesAliases/>\n");
		}
		if (CDloProteins.length() > 0) {
			annot.appendNoRDFAnnotation("<celldesigner:listOfProteins>\n");
			annot.appendNoRDFAnnotation(CDloProteins.toString());
			annot.appendNoRDFAnnotation("</celldesigner:listOfProteins>\n");
		} else {
			annot.appendNoRDFAnnotation("<celldesigner:listOfProteins/>\n");
		}
		annot.appendNoRDFAnnotation("<celldesigner:listOfGroups/>\n");
		annot.appendNoRDFAnnotation("<celldesigner:listOfGenes/>\n");
		annot.appendNoRDFAnnotation("<celldesigner:listOfRNAs/>\n");
		annot.appendNoRDFAnnotation("<celldesigner:listOfAntisenseRNAs/>\n");
		annot.appendNoRDFAnnotation("<celldesigner:listOfLayers/>\n");
		annot.appendNoRDFAnnotation("<celldesigner:listOfBlockDiagrams/>\n");
		annot.appendNoRDFAnnotation("</celldesigner:extension>\n");
	}

	/**
	 * Call me only on final/completely configured reactions!
	 * 
	 * @param sbReaction
	 * @param r
	 */
	private void addCellDesignerAnnotationToReaction(
			org.sbml.jsbml.Reaction sbReaction, Reaction r) {
		sbReaction.getAnnotation().addAnnotationNamespace("xmlns:celldesigner",
				"", "http://www.sbml.org/2001/ns/celldesigner");
		sbReaction
				.addNamespace("xmlns:celldesigner=http://www.sbml.org/2001/ns/celldesigner");

		// Add Reaction Annotation
		sbReaction.getAnnotation().appendNoRDFAnnotation(
				"<celldesigner:extension>\n");
		sbReaction.getAnnotation().appendNoRDFAnnotation(
				String.format("<celldesigner:name>%s</celldesigner:name>\n",
						sbReaction.getName()));
		// TODO: STATE_TRANSITION or UNKNOWN_TRANSITION ? Ersteres in anderen
		// releases.
		sbReaction
				.getAnnotation()
				.appendNoRDFAnnotation(
						"<celldesigner:reactionType>STATE_TRANSITION</celldesigner:reactionType>\n");

		sbReaction.getAnnotation().appendNoRDFAnnotation(
				"<celldesigner:baseReactants>\n");
		for (SpeciesReference s : sbReaction.getListOfReactants()) {
			sbReaction
					.getAnnotation()
					.appendNoRDFAnnotation(
							String
									.format(
											"<celldesigner:baseReactant species=\"%s\" alias=\"%s\"/>\n",
											s.getSpeciesInstance().getId(),
											"cd_sa"
													+ s.getSpeciesInstance()
															.getId()));
			// Write annotation for SpeciesReference
			if (!s.isSetAnnotation()) {
				Annotation rAnnot = new Annotation("");
				rAnnot.setAbout("");
				s.setAnnotation(rAnnot);
				s.getAnnotation().addAnnotationNamespace("xmlns:celldesigner",
						"", "http://www.sbml.org/2001/ns/celldesigner");
				s
						.addNamespace("xmlns:celldesigner=http://www.sbml.org/2001/ns/celldesigner");
			}
			s
					.getAnnotation()
					.appendNoRDFAnnotation(
							String
									.format(
											"<celldesigner:extension>\n<celldesigner:alias>%s</celldesigner:alias>\n</celldesigner:extension>\n",
											"cd_sa"
													+ s.getSpeciesInstance()
															.getId()));
		}
		sbReaction.getAnnotation().appendNoRDFAnnotation(
				"</celldesigner:baseReactants>\n");

		sbReaction.getAnnotation().appendNoRDFAnnotation(
				"<celldesigner:baseProducts>\n");
		for (SpeciesReference s : sbReaction.getListOfProducts()) {
			sbReaction
					.getAnnotation()
					.appendNoRDFAnnotation(
							String
									.format(
											"<celldesigner:baseProduct species=\"%s\" alias=\"%s\"/>\n",
											s.getSpeciesInstance().getId(),
											"cd_sa"
													+ s.getSpeciesInstance()
															.getId()));
			// Write annotation for SpeciesReference
			if (!s.isSetAnnotation()) {
				Annotation rAnnot = new Annotation("");
				rAnnot.setAbout("");
				s.setAnnotation(rAnnot);
				s.getAnnotation().addAnnotationNamespace("xmlns:celldesigner",
						"", "http://www.sbml.org/2001/ns/celldesigner");
				s
						.addNamespace("xmlns:celldesigner=http://www.sbml.org/2001/ns/celldesigner");
			}
			s
					.getAnnotation()
					.appendNoRDFAnnotation(
							String
									.format(
											"<celldesigner:extension>\n<celldesigner:alias>%s</celldesigner:alias>\n</celldesigner:extension>\n",
											"cd_sa"
													+ s.getSpeciesInstance()
															.getId()));
		}
		sbReaction.getAnnotation().appendNoRDFAnnotation(
				"</celldesigner:baseProducts>\n");

		sbReaction
				.getAnnotation()
				.appendNoRDFAnnotation(
						"<celldesigner:connectScheme connectPolicy=\"direct\" rectangleIndex=\"0\">\n");
		sbReaction.getAnnotation().appendNoRDFAnnotation(
				"<celldesigner:listOfLineDirection>\n");
		sbReaction
				.getAnnotation()
				.appendNoRDFAnnotation(
						"<celldesigner:lineDirection index=\"0\" value=\"unknown\"/>\n");
		sbReaction.getAnnotation().appendNoRDFAnnotation(
				"</celldesigner:listOfLineDirection>\n");
		sbReaction.getAnnotation().appendNoRDFAnnotation(
				"</celldesigner:connectScheme>\n");
		sbReaction.getAnnotation().appendNoRDFAnnotation(
				"<celldesigner:line width=\"1.0\" color=\"ff000000\"/>\n");

		sbReaction.getAnnotation().appendNoRDFAnnotation(
				"<celldesigner:listOfModification>\n");
		for (ModifierSpeciesReference s : sbReaction.getListOfModifiers()) {
			sbReaction
					.getAnnotation()
					.appendNoRDFAnnotation(
							String
									.format(
											"<celldesigner:modification type=\"CATALYSIS\" modifiers=\"%s\" aliases=\"%s\" targetLineIndex=\"-1,0\">\n", // original:
											// -1,2
											s.getSpeciesInstance().getId(),
											"cd_sa"
													+ s.getSpeciesInstance()
															.getId()));
			sbReaction.getAnnotation().appendNoRDFAnnotation(
					"<celldesigner:connectScheme connectPolicy=\"direct\">\n");
			sbReaction.getAnnotation().appendNoRDFAnnotation(
					"<celldesigner:listOfLineDirection>\n");
			sbReaction
					.getAnnotation()
					.appendNoRDFAnnotation(
							"<celldesigner:lineDirection index=\"0\" value=\"unknown\"/>\n");
			sbReaction.getAnnotation().appendNoRDFAnnotation(
					"</celldesigner:listOfLineDirection>\n");
			sbReaction.getAnnotation().appendNoRDFAnnotation(
					"</celldesigner:connectScheme>\n");
			sbReaction.getAnnotation().appendNoRDFAnnotation(
					"<celldesigner:line width=\"1.0\" color=\"ff000000\"/>\n");
			sbReaction.getAnnotation().appendNoRDFAnnotation(
					"</celldesigner:modification>\n");
			// Write annotation for ModifierSpeciesReference
			s
					.getAnnotation()
					.appendNoRDFAnnotation(
							String
									.format(
											"<celldesigner:extension>\n<celldesigner:alias>%s</celldesigner:alias>\n</celldesigner:extension>\n",
											"cd_sa"
													+ s.getSpeciesInstance()
															.getId()));
			// Write further annotations for the Modifying species.
			s
					.getSpeciesInstance()
					.getAnnotation()
					.appendNoRDFAnnotation(
							String
									.format(
											"<celldesigner:listOfCatalyzedReactions>\n<celldesigner:catalyzed reaction=\"%s\"/>\n</celldesigner:listOfCatalyzedReactions>\n",
											sbReaction.getId()));
		}
		sbReaction.getAnnotation().appendNoRDFAnnotation(
				"</celldesigner:listOfModification>\n");

		sbReaction.getAnnotation().appendNoRDFAnnotation(
				"</celldesigner:extension>\n");
	}

	/**
	 * Uses spec.getName() ! Be careful, the species CD Extension tag is NOT
	 * closed.
	 */
	private void addCellDesignerAnnotationToSpecies(Species spec, Entry e) {
		// TODO: Sind die defaults so richtig? was bedeutet z.B. cd:activity?
		EntryType t = e.getType();
		boolean isGroupNode = (t.equals(EntryType.group) || t
				.equals(EntryType.genes)); // genes = group in kgml v<0.7

		spec.getAnnotation().addAnnotationNamespace("xmlns:celldesigner", "",
				"http://www.sbml.org/2001/ns/celldesigner");
		spec
				.addNamespace("xmlns:celldesigner=http://www.sbml.org/2001/ns/celldesigner");

		// Add to Species Annotation list
		StringBuffer target;
		if (isGroupNode)
			target = CDloComplexSpeciesAliases;
		else
			target = CDloSpeciesAliases;
		// Warning: prefix "cd_sa" is also hardcoded in addCDAtoReaction!
		target.append("<celldesigner:"
				+ (isGroupNode ? "complexSpeciesAlias" : "speciesAlias")
				+ " id=\"cd_sa" + spec.getId() + "\" species=\"" + spec.getId()
				+ "\">\n");
		target
				.append("<celldesigner:activity>inactive</celldesigner:activity>\n");
		if (e.hasGraphics())
			target
					.append(String
							.format(
									"<celldesigner:bounds x=\"%d\" y=\"%d\" w=\"%d\" h=\"%d\"/>\n",
									e.getGraphics().getX(), e.getGraphics()
											.getY(),
									e.getGraphics().getWidth(), e.getGraphics()
											.getHeight()));
		target.append("<celldesigner:view state=\"usual\"/>\n");

		if (isGroupNode) {
			target.append("<celldesigner:backupSize w=\"0.0\" h=\"0.0\"/>\n");
			target.append("<celldesigner:backupView state=\"none\"/>\n");
		}

		// Add usual- and brief view
		for (int i = 1; i <= 2; i++) {
			if (i == 1)
				target.append("<celldesigner:usualView>\n");
			else
				target.append("<celldesigner:briefView>\n");
			target
					.append("<celldesigner:innerPosition x=\"0.0\" y=\"0.0\"/>\n");
			target.append(String.format(
					"<celldesigner:boxSize width=\"%d\" height=\"%d\"/>\n", e
							.hasGraphics() ? e.getGraphics().getWidth() : 90, e
							.hasGraphics() ? e.getGraphics().getHeight() : 25));
			target.append("<celldesigner:singleLine width=\""
					+ (isGroupNode ? "2.0" : (i == 1 ? "1.0" : "0.0"))
					+ "\"/>\n");
			target
					.append(String.format("<celldesigner:paint color=\""
							+ (i == 1 ? "ff" : "3f")
							+ "%s\" scheme=\"Color\"/>\n", e.getGraphics()
							.getBgcolor().replace("#", "").toLowerCase()));
			if (i == 1) {
				target.append("</celldesigner:usualView>\n");
			} else {
				target.append("</celldesigner:briefView>\n");
			}
		}

		target.append("<celldesigner:info state=\"empty\" angle=\"0.0\"/>\n");
		target.append("</celldesigner:"
				+ (isGroupNode ? "complexSpeciesAlias" : "speciesAlias")
				+ ">\n");

		// Add to type specific annotation
		String type = "";
		String reference = "";
		if (t.equals(EntryType.ortholog) || t.equals(EntryType.enzyme)
				|| t.equals(EntryType.gene)) {
			// A Protein. (EntryType.gene => KeggDoc says
			// "the node is a gene PRODUCT (mostly a protein)")
			CDloProteins
					.append(String
							.format(
									"<celldesigner:protein id=\"cd_pr%s\" name=\"%s\" type=\"GENERIC\"/>\n",
									spec.getId(), spec.getId()));
			type = "PROTEIN";
			reference = "<celldesigner:proteinReference>cd_pr" + spec.getId()
					+ "</celldesigner:proteinReference>";
		} else if (isGroupNode) { // t.equals(EntryType.group)
			type = "COMPLEX";
			reference = "<celldesigner:name>"
					+ NameToCellDesignerName(spec.getName())
					+ "</celldesigner:name>";
		} else if (t.equals(EntryType.compound)) {
			type = "SIMPLE_MOLECULE";
			reference = "<celldesigner:name>"
					+ NameToCellDesignerName(spec.getName())
					+ "</celldesigner:name>";
		} else if (t.equals(EntryType.map) || t.equals(EntryType.other)) {
			type = "UNKNOWN";
			reference = "<celldesigner:name>"
					+ NameToCellDesignerName(spec.getName())
					+ "</celldesigner:name>";
		}

		// Add Species Annotation
		spec.getAnnotation()
				.appendNoRDFAnnotation("<celldesigner:extension>\n");
		spec
				.getAnnotation()
				.appendNoRDFAnnotation(
						"<celldesigner:positionToCompartment>inside</celldesigner:positionToCompartment>\n");
		spec.getAnnotation().appendNoRDFAnnotation(
				"<celldesigner:speciesIdentity>\n");
		spec.getAnnotation().appendNoRDFAnnotation(
				String.format("<celldesigner:class>%s</celldesigner:class>\n",
						type));
		spec.getAnnotation().appendNoRDFAnnotation(reference + "\n");
		spec.getAnnotation().appendNoRDFAnnotation(
				"</celldesigner:speciesIdentity>\n");
		/*
		 * DON'T WRITE END TAG HERE. Catalysts write additional data in
		 * "addCellDesignerAnnotationToReaction".
		 * spec.getAnnotation().appendNoRDFAnnotation
		 * ("</celldesigner:extension>\n");
		 */
	}

	/**
	 * 
	 * @param p
	 * @param rc
	 * @param sr
	 * @param SBO
	 */
	private void configureReactionComponent(Pathway p, ReactionComponent rc,
			SpeciesReference sr, int SBO) {
		if (rc.getName() == null || rc.getName().trim().length() == 0) {
			rc = rc.getAlt();
			if (rc.getName() == null || rc.getName().trim().length() == 0)
				return;
		}
		sr.setName(rc.getName());
		sr.setId(NameToSId(sr.getName()));
		sr.setMetaId("meta_" + sr.getId());

		Entry spec = p.getEntryForName(rc.getName());
		if ((spec != null) && (spec.getCustom() != null)) {
			sr.setSpecies((Species) spec.getCustom());
		}

		if (sr.getSpeciesInstance() != null
				&& sr.getSpeciesInstance().getSBOTerm() <= 0) {
			sr.getSpeciesInstance().setSBOTerm(SBO); // should be
			// Product/Substrate
			sr.setSBOTerm(SBO);
		}
	}

	/**
	 * 
	 */
	public boolean Convert(Pathway p, String outfile) {
		SBMLDocument doc = Kegg2jSBML(p);

		// JSBML IO => write doc to outfile.
		if (new File(outfile).exists()) {
			// Remember that file was already
			lastFileWasOverwritten = true;
		}
		// there.
		try {
			SBMLWriter.write(doc, outfile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (XMLStreamException e) {
			e.printStackTrace();
			return false;
		} catch (SBMLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.kegg.io.KeggConverter#Convert()
	 */
	public void Convert(String infile, String outfile)
			throws XMLStreamException,
			InstantiationException, IllegalAccessException, InvalidPropertiesFormatException, IOException, ClassNotFoundException, SBMLException {
		SBMLDocument doc = Kegg2jSBML(infile);

		// JSBML IO => write doc to outfile.
		if (new File(outfile).exists())
			lastFileWasOverwritten = true; // Remember that file was already
		// there.
		SBMLWriter.write(doc, outfile);
	}

	/**
	 * This method converts a given KGML file into an SBMLDocument.
	 * 
	 * @param f
	 * @return
	 * @throws IOException
	 */
	public SBMLDocument convert(File f) throws IOException {
		if (f.exists() && f.isFile() && f.canRead()) {
			SBMLDocument doc = Kegg2jSBML(f.getAbsolutePath());

			// Remember already queried objects
			if (getKeggInfoManager().hasChanged()) {
				KeggInfoManagement.saveToFilesystem("keggdb.dat",
						getKeggInfoManager());
			}
			return doc;
		}
		throw new IOException("Cannot read input file " + f.getAbsolutePath());
	}

	/**
	 * 
	 * @return
	 */
	public KeggInfoManagement getKeggInfoManager() {
		return manager;
	}

	/**
	 * Appends "_<Number>" to a given String. <Number> is being set to the next
	 * free number, so that this sID is unique in this sbml document. Should
	 * only be called from "NameToSId".
	 * 
	 * @return
	 */
	private String incrementSIdSuffix(String prefix) {
		int i = 1;
		String aktString = prefix + "_" + i;
		while (SIds.contains(aktString)) {
			aktString = prefix + "_" + (++i);
		}
		return aktString;
	}

	/**
	 * 
	 * @param filepath
	 * @return
	 */
	public SBMLDocument Kegg2jSBML(String filepath) {
		// System.out.println("Reading kegg pathway...");
		Pathway p = KeggParser.parse(filepath).get(0);

		// System.out.println("Converting to SBML");
		SBMLDocument doc = Kegg2jSBML(p);

		return doc;
	}

	/**
	 * 
	 * @param p
	 * @return
	 */
	public SBMLDocument Kegg2jSBML(Pathway p) {
		int level = 2;
		int version = 4;
		SBMLDocument doc = new SBMLDocument(level, version);
		ArrayList<Entry> entries = p.getEntries();

		// ArrayList<String> PWReferenceNodeTexts = new ArrayList<String>();
		if (!retrieveKeggAnnots) {
			KeggInfoManagement.offlineMode = true;
		} else {
			KeggInfoManagement.offlineMode = false;

			// PreFetch infos. Enormous performance improvement!
			ArrayList<String> preFetchIDs = new ArrayList<String>();
			preFetchIDs.add("GN:" + p.getOrg());
			preFetchIDs.add(p.getName());
			for (Entry entry : entries) {
				for (String ko_id : entry.getName().split(" ")) {
					if (ko_id.trim().equalsIgnoreCase("undefined")
							|| entry.hasComponents())
						continue; // "undefined" = group node, which contains
					// "Components"
					preFetchIDs.add(ko_id);
				}
			}
			for (Reaction r : p.getReactions()) {
				for (String ko_id : r.getName().split(" ")) {
					preFetchIDs.add(ko_id);
				}
			}
			manager.precacheIDs(preFetchIDs.toArray(new String[preFetchIDs
					.size()]));
			// TODO: Add relations?
			// -------------------------
		}
		SIds = new ArrayList<String>(); // Reset list of given SIDs. These are
		// being remembered to avoid double ids.

		// Initialize a progress bar.
		int aufrufeGesamt = p.getEntries().size(); // +p.getRelations().size();
		// // Relations gehen sehr
		// schnell.
		// if (adap==null) aufrufeGesamt+=p.getRelations().size(); // TODO: noch
		// ausloten wann klasse aufgerufen wird.
		ProgressBar progress = new ProgressBar(aufrufeGesamt + 1);
		progress.DisplayBar();

		// new Model with Kegg id as id.
		Model model = doc.createModel(NameToSId(p.getName().replace(":", "_")));
		model.setMetaId("meta_" + model.getId());
		model.setName(p.getTitle());
		Compartment compartment = model.createCompartment("default");
		// Create neccessary default compartment
		// TODO: provide a parameter for this value.
		compartment.setSize(1d);
		compartment.setUnits(model.getUnitDefinition("volume"));
		// Be careful: compartment ID ant other compartment stuff are HARDCODED
		// in cellDesigner extension code generation!

		// Create Model History
		History hist = new History();
		Creator creator = new Creator();
		creator.setOrganisation("ZBIT, University of T\u00fcbingen, WSI-RA");
		hist.addCreator(creator);
		hist.addModifiedDate(Calendar.getInstance().getTime());
		Annotation annot = new Annotation();
		annot.setAbout("#" + model.getMetaId());
		model.setAnnotation(annot);
		model.setHistory(hist);
		model.appendNotes("<body xmlns=\"http://www.w3.org/1999/xhtml\">");

		// CellDesigner Annotations
		if (addCellDesignerAnnots) {
			// XXX: Probably the more correct way. But currently, extensions get
			// completely ignored.
			// Annotation cdAnnot = new Annotation();
			// cdAnnot.setAbout("");
			// addCellDesignerAnnotationPrefixToModel(p, cdAnnot);
			// annot.addExtension("xmlns:celldesigner=http://www.sbml.org/2001/ns/celldesigner",
			// cdAnnot);

			String cellDesignerNameSpace = "xmlns:celldesigner=http://www.sbml.org/2001/ns/celldesigner";
			model.addNamespace(cellDesignerNameSpace);
			annot.addAnnotationNamespace("xmlns:celldesigner", "",
					"http://www.sbml.org/2001/ns/celldesigner");
			doc.addNamespace("xmlns:celldesigner", "",
					"http://www.sbml.org/2001/ns/celldesigner"); // xmlns:celldesigner
		}
		model.getAnnotation().addRDFAnnotationNamespace("bqbiol", "",
				"http://biomodels.net/biology-qualifiers/");
		model.getAnnotation().addRDFAnnotationNamespace("bqmodel", "",
				"http://biomodels.net/model-qualifiers/");

		// Parse Kegg Pathway information
		CVTerm mtPwID = new CVTerm();
		mtPwID.setQualifierType(Type.MODEL_QUALIFIER);
		mtPwID.setModelQualifierType(Qualifier.BQM_IS);
		mtPwID.addResource(KeggInfos.getMiriamURIforKeggID(p.getName())); // same
		// as
		// "urn:miriam:kegg.pathway"
		// +
		// p.getName().substring(p.getName().indexOf(":"))
		model.addCVTerm(mtPwID);

		// Retrieve further information via Kegg Adaptor
		KeggInfos orgInfos = new KeggInfos("GN:" + p.getOrg(), manager); // Retrieve
		// all
		// organism
		// information
		// via
		// KeggAdaptor
		if (orgInfos.queryWasSuccessfull()) {
			CVTerm mtOrgID = new CVTerm();
			mtOrgID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
			mtOrgID.setBiologicalQualifierType(Qualifier.BQB_OCCURS_IN);
			appendAllIds(orgInfos.getTaxonomy(), mtOrgID,
					KeggInfos.miriam_urn_taxonomy);
			model.addCVTerm(mtOrgID);

			model.appendNotes(String.format(
					"<h1>Model of %s%s%s in %s%s%s</h1>\n", quotStart, p
							.getTitle(), quotEnd, quotStart, orgInfos
							.getDefinition(), quotEnd));
		} else {
			model.appendNotes(String.format("<h1>Model of " + quotStart + "%s"
					+ quotEnd + "</h1>\n", p.getTitle()));
		}

		// Get PW infos from KEGG Api for Description and GO ids.
		KeggInfos pwInfos = new KeggInfos(p.getName(), manager); // NAME,
		// DESCRIPTION,
		// DBLINKS
		// verwertbar
		if (pwInfos.queryWasSuccessfull()) {
			model.appendNotes(String.format("%s<br/>\n", pwInfos
					.getDescription()));

			// GO IDs
			if (pwInfos.getGo_id() != null) {
				CVTerm mtGoID = new CVTerm();
				mtGoID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
				mtGoID.setBiologicalQualifierType(Qualifier.BQB_IS_VERSION_OF);
				appendAllGOids(pwInfos.getGo_id(), mtGoID);
				if (mtGoID.getNumResources() > 0)
					model.addCVTerm(mtGoID);
			}
		}
		model.appendNotes(String.format(
				"<a href=\"%s\"><img src=\"%s\" alt=\"%s\"/></a><br/>\n", p
						.getImage(), p.getImage(), p.getImage()));
		model.appendNotes(String.format(
				"<a href=\"%s\">Original Entry</a><br/>\n", p.getLink()));

		// Write model version and creation date, if available, into model
		// notes.
		if (p.getVersion() > 0
				|| (p.getComment() != null && p.getComment().length() > 0)) {
			model.appendNotes("<p>");
			if (p.getComment() != null && p.getComment().length() > 0) {
				model.appendNotes(String.format("KGML Comment: %s%s%s<br/>\n",
						quotStart, p.getComment(), quotEnd));
			}
			if (p.getVersion() > 0) {
				model.appendNotes(String.format("KGML Version was: %s<br/>\n",
						Double.toString(p.getVersion())));
			}
			model.appendNotes("</p>");
		}

		// Save all reaction modifiers in a list. String = reaction id.
		SortedArrayList<Info<String, ModifierSpeciesReference>> reactionModifiers = new SortedArrayList<Info<String, ModifierSpeciesReference>>();

		// Create species
		for (Entry entry : entries) {
			progress.DisplayBar();
			/*
			 * <entry id="1" name="ko:K00128" type="ortholog"
			 * reaction="rn:R00710"
			 * link="http://www.genome.jp/dbget-bin/www_bget?ko+K00128">
			 * <graphics name="K00128" fgcolor="#000000" bgcolor="#BFBFFF"
			 * type="rectangle" x="170" y="1018" width="45" height="17"/>
			 * </entry>
			 */

			boolean isPathwayReference = false;
			String name = entry.getName().trim();
			if ((name != null)
					&& (name.toLowerCase().startsWith("path:") || entry
							.getType().equals(EntryType.map))) {
				isPathwayReference = true;
			}
			// Eventually skip this node. It's just a label for the current
			// pathway.
			if (isPathwayReference
					&& (entry.hasGraphics() && entry.getGraphics().getName()
							.toLowerCase().startsWith("title:"))) {
				compartment.setName(entry.getGraphics().getName().substring(6)
						.trim());
				continue;
			}

			// Skip it, if it's white
			if (removeWhiteNodes
					&& entry.hasGraphics()
					&& entry.getGraphics().getBgcolor().toLowerCase().trim()
							.endsWith("ffffff")
					&& (entry.getType() == EntryType.gene || entry.getType() == EntryType.ortholog))
				continue;

			// Look if not is an orphan
			if (removeOrphans) {
				if (entry.getReaction() == null
						|| entry.getReaction().length() < 1) {
					boolean found = false;
					for (Reaction r : p.getReactions()) {
						for (ReactionComponent rc : r.getProducts())
							if (rc.getName().equalsIgnoreCase(entry.getName())) {
								found = true;
								break;
							}
						if (!found) {
							for (ReactionComponent rc : r.getSubstrates())
								if (rc.getName().equalsIgnoreCase(
										entry.getName())) {
									found = true;
									break;
								}
						}
						if (found) {
							break;
						}
					}

					if (considerRelations && !found) {
						for (Relation r : p.getRelations()) {
							if (r.getEntry1() == entry.getId()
									|| r.getEntry2() == entry.getId()) {
								found = true;
								break;
							}
							for (SubType st : r.getSubtypes()) {
								try {
									if (Integer.parseInt(st.getValue()) == entry
											.getId()) {
										found = true;
										break;
									}
								} catch (Exception e) {
								}
							}
							if (found)
								break;
						}
					}
					if (!found)
						continue;
				}
			}
			/*
			 * TODO: Gruppenknoten erstellen. Gibt es sowas in SBML? InCD -> ja,
			 * aber umsetzung ist ungenügend (nur zur visualisierung, keine SBML
			 * Species für alle species).
			 * 
			 * Beispiel (aus map04010hsa.xml): <entry id="141"
			 * name="group:"type="genes"> <graphics fgcolor="#000000"
			 * bgcolor="#FFFFFF" type="rectangle" x="945" y="310" width="129"
			 * height="59"/> <component id="131"/> <component id="133"/>
			 * <component id="134"/> </entry> Beispiel aktuell (kg0.7
			 * map04010.xml): <entry id="138" name="undefined" type="group">
			 * <graphics fgcolor="#000000" bgcolor="#FFFFFF" type="rectangle"
			 * x="945" y="310" width="129" height="59"/> <component id="131"/>
			 * <component id="133"/> <component id="134"/> </entry>
			 */

			// Get a good name for the node
			boolean hasMultipleIDs = false;
			if (entry.getName().trim().contains(" ")) {
				hasMultipleIDs = true;
			}
			if (entry.hasGraphics()
					&& entry.getGraphics().getName().length() > 0) {
				name = entry.getGraphics().getName(); // + " (" + name + ")"; //
				// Append ko Id(s)
				// possible!
			}
			// Set name to real and human-readable name (from Inet data - Kegg
			// API).
			if (!hasMultipleIDs) {
				// Be careful: very slow, uses Cache - so doesn't matter to
				// query the same id one or more times.
				KeggInfos infos = new KeggInfos(entry.getName().trim(), manager);
				if (infos.queryWasSuccessfull() && (infos.getName() != null)) {
					name = infos.getName();
				}
			}
			// ---

			// Initialize species object
			Species spec = model.createSpecies();
			spec.setCompartment(compartment); // spec.setId("s_" +
			// entry.getId());
			// TODO: introduce a parameter for this quantity.
			spec.setInitialAmount(1d);
			spec.setUnits(model.getUnitDefinition("substance"));

			/*
			 * ID has to be at this place, because other refer to it by id and
			 * if id is not set. refenreces go to null.
			 * spec.setId(NameToSId(entry.getName().replace(' ', '_')));
			 */
			spec.setId(NameToSId(name.replace(' ', '_')));

			spec.setMetaId("meta_" + spec.getId());

			Annotation specAnnot = new Annotation("");
			specAnnot.setAbout("");
			/*
			 * manchmal ist jSBML schon bescheurt...
			 */
			spec.setAnnotation(specAnnot);
			spec.appendNotes("<body xmlns=\"http://www.w3.org/1999/xhtml\">");
			spec.appendNotes(String.format(
					"<a href=\"%s\">Original Kegg Entry</a><br/>\n", entry
							.getLink()));

			// Set SBO Term
			if (treatEntrysWithReactionDifferent && entry.getReaction() != null
					&& entry.getReaction().trim().length() != 0) {
				// Q: Ist es richtig, sowohl dem Modifier als auch der species
				// eine neue id zu geben? A: Nein, ist nicht richtig.
				// spec.setSBOTerm(ET_SpecialReactionCase2SBO);
				ModifierSpeciesReference modifier = new ModifierSpeciesReference(
						spec);

				Annotation tempAnnot = new Annotation("");
				tempAnnot.setAbout("");
				modifier.setAnnotation(tempAnnot);
				if (addCellDesignerAnnots) {
					modifier.getAnnotation().addAnnotationNamespace(
							"xmlns:celldesigner", "",
							"http://www.sbml.org/2001/ns/celldesigner");
					modifier
							.addNamespace("xmlns:celldesigner=http://www.sbml.org/2001/ns/celldesigner");
				}
				modifier.setId(this.NameToSId("mod_" + entry.getReaction()));
				modifier.setMetaId("meta_" + modifier.getId());
				modifier.setName(modifier.getId());
				if (entry.getType().equals(EntryType.enzyme)
						|| entry.getType().equals(EntryType.gene)
						|| entry.getType().equals(EntryType.group)
						|| entry.getType().equals(EntryType.ortholog)
						|| entry.getType().equals(EntryType.genes)) {
					// 1 & 2: klar. 3 (group): Doku sagt
					// "MOSTLY a protein complex". 4 (ortholog): Kommen in
					// nicht-spezies spezifischen PWs vor und sind quasi otholog
					// geclusterte gene.
					// 5. (genes) ist group in kgml versionen <0.7.
					modifier.setSBOTerm(ET_EnzymaticModifier2SBO); // 460 =
					// Enzymatic
					// catalyst
				} else { // "Metall oder etwas anderes, was definitiv nicht enzymatisch wirkt"
					modifier.setSBOTerm(ET_GeneralModifier2SBO); // 13 =
					// Catalyst
				}

				// Remember modifier for later association with reaction.
				reactionModifiers
						.add(new Info<String, ModifierSpeciesReference>(entry
								.getReaction().toLowerCase().trim(), modifier));

			} else {
				spec.setSBOTerm(getSBOTerm(entry.getType()));
			}

			// Process graphics information
			if (entry.hasGraphics()) {
				/*
				 * <entry id="16" name="ko:K04467 ko:K07209 ko:K07210"
				 * type="ortholog"> <graphics name="IKBKA..." fgcolor="#000000"
				 * bgcolor="#FFFFFF" type="rectangle" x="785" y="141" width="45"
				 * height="17"/>
				 * 
				 * ... is actually a compund!?!?
				 */

				// Get name, description and other annotations via api (organism
				// specific) possible!!
				// Graphics g = entry.getGraphics();
			}

			// Process Component information
			/*
			 * // No need to do that! if (entry.getComponents()!=null &&
			 * entry.getComponents().size()>0) { for (int
			 * c:entry.getComponents()) {
			 * 
			 * } }
			 */

			CVTerm cvtKGID = new CVTerm();
			cvtKGID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
			cvtKGID.setBiologicalQualifierType(Qualifier.BQB_IS);
			CVTerm cvtEntrezID = new CVTerm();
			cvtEntrezID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
			cvtEntrezID.setBiologicalQualifierType(Qualifier.BQB_IS);
			CVTerm cvtOmimID = new CVTerm();
			cvtOmimID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
			cvtOmimID.setBiologicalQualifierType(Qualifier.BQB_IS);
			CVTerm cvtHgncID = new CVTerm();
			cvtHgncID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
			cvtHgncID.setBiologicalQualifierType(Qualifier.BQB_IS);
			CVTerm cvtEnsemblID = new CVTerm();
			cvtEnsemblID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
			cvtEnsemblID.setBiologicalQualifierType(Qualifier.BQB_IS);
			CVTerm cvtUniprotID = new CVTerm();
			cvtUniprotID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
			cvtUniprotID.setBiologicalQualifierType(Qualifier.BQB_IS);
			CVTerm cvtChebiID = new CVTerm();
			cvtChebiID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
			cvtChebiID.setBiologicalQualifierType(Qualifier.BQB_IS);
			CVTerm cvtDrugbankID = new CVTerm();
			cvtDrugbankID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
			cvtDrugbankID.setBiologicalQualifierType(Qualifier.BQB_IS);
			CVTerm cvtGoID = new CVTerm();
			cvtGoID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
			cvtGoID.setBiologicalQualifierType(Qualifier.BQB_IS_VERSION_OF);
			CVTerm cvtHGNCID = new CVTerm();
			cvtHGNCID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
			cvtHGNCID.setBiologicalQualifierType(Qualifier.BQB_IS);
			CVTerm cvtPubchemID = new CVTerm();
			cvtPubchemID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
			cvtPubchemID.setBiologicalQualifierType(Qualifier.BQB_IS);
			CVTerm cvt3dmetID = new CVTerm();
			cvt3dmetID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
			cvt3dmetID.setBiologicalQualifierType(Qualifier.BQB_IS);
			CVTerm cvtReactionID = new CVTerm();
			cvtReactionID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
			cvtReactionID
					.setBiologicalQualifierType(Qualifier.BQB_IS_DESCRIBED_BY);
			CVTerm cvtTaxonomyID = new CVTerm();
			cvtTaxonomyID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
			cvtTaxonomyID.setBiologicalQualifierType(Qualifier.BQB_OCCURS_IN);
			// TODO: Seit neustem noch mehr in MIRIAM verfügbar.

			// Parse every gene/object in this node.
			for (String ko_id : entry.getName().split(" ")) {
				if (ko_id.trim().equalsIgnoreCase("undefined")
						|| entry.hasComponents())
					continue; // "undefined" = group node, which contains
				// "Components"

				// Add Kegg-id Miriam identifier
				String kgMiriamEntry = KeggInfos.getMiriamURIforKeggID(ko_id,
						entry.getType());
				if (kgMiriamEntry != null)
					cvtKGID.addResource(kgMiriamEntry);

				// Retrieve further information via Kegg API -- Be careful: very
				// slow!
				KeggInfos infos = new KeggInfos(ko_id, manager);
				if (infos.queryWasSuccessfull()) {

					// HTML Information
					if ((infos.getDefinition() != null)
							&& (infos.getName() != null)) {
						spec.appendNotes(String.format(
								"<p><b>Description for %s%s%s:</b> %s</p>\n",
								quotStart,
								EscapeChars.forHTML(infos.getName()), quotEnd,
								EscapeChars.forHTML(infos.getDefinition()
										.replace("\n", " "))));
					} else if (infos.getName() != null) {
						spec.appendNotes(String.format("<p><b>%s</b></p>\n",
								EscapeChars.forHTML(infos.getName())));
					}
					if (infos.containsMultipleNames())
						spec.appendNotes(String.format(
								"<p><b>All given names:</b><br/>%s</p>\n",
								EscapeChars.forHTML(infos.getNames().replace(
										";", ""))));
					if (infos.getCas() != null)
						spec.appendNotes(String.format(
								"<p><b>CAS number:</b> %s</p>\n", infos
										.getCas()));
					if (infos.getFormula() != null)
						spec.appendNotes(String.format(
								"<p><b>Formula:</b> %s</p>\n", EscapeChars
										.forHTML(infos.getFormula())));
					if (infos.getMass() != null)
						spec.appendNotes(String.format(
								"<p><b>Mass:</b> %s</p>\n", infos.getMass()));

					// Parse "NCBI-GeneID:","UniProt:", "Ensembl:", ...
					if (infos.getEnsembl_id() != null)
						appendAllIds(infos.getEnsembl_id(), cvtEnsemblID,
								KeggInfos.miriam_urn_ensembl);
					if (infos.getChebi() != null)
						appendAllIds(infos.getChebi(), cvtChebiID,
								KeggInfos.miriam_urn_chebi, "CHEBI:");
					if (infos.getDrugbank() != null)
						appendAllIds(infos.getDrugbank(), cvtDrugbankID,
								KeggInfos.miriam_urn_drugbank);
					if (infos.getEntrez_id() != null)
						appendAllIds(infos.getEntrez_id(), cvtEntrezID,
								KeggInfos.miriam_urn_entrezGene);
					if (infos.getGo_id() != null)
						appendAllGOids(infos.getGo_id(), cvtGoID);
					if (infos.getHgnc_id() != null)
						appendAllIds(infos.getHgnc_id(), cvtHGNCID,
								KeggInfos.miriam_urn_hgnc, "HGNC:");

					if (infos.getOmim_id() != null)
						appendAllIds(infos.getOmim_id(), cvtOmimID,
								KeggInfos.miriam_urn_omim);
					if (infos.getPubchem() != null)
						appendAllIds(infos.getPubchem(), cvtPubchemID,
								KeggInfos.miriam_urn_PubChem_Substance);

					if (infos.getThree_dmet() != null)
						appendAllIds(infos.getThree_dmet(), cvt3dmetID,
								KeggInfos.miriam_urn_3dmet);
					if (infos.getUniprot_id() != null)
						appendAllIds(infos.getUniprot_id(), cvtUniprotID,
								KeggInfos.miriam_urn_uniprot);

					if (infos.getReaction_id() != null)
						appendAllIds(infos.getReaction_id(), cvtReactionID,
								KeggInfos.miriam_urn_kgReaction);
					if (infos.getTaxonomy() != null)
						appendAllIds(infos.getTaxonomy(), cvtTaxonomyID,
								KeggInfos.miriam_urn_taxonomy);
				}

			}
			// Add all non-empty ressources.
			specAnnot.addRDFAnnotationNamespace("bqbiol", "",
					"http://biomodels.net/biology-qualifiers/");
			if (cvtKGID.getNumResources() > 0)
				spec.addCVTerm(cvtKGID);
			if (cvtEntrezID.getNumResources() > 0)
				spec.addCVTerm(cvtEntrezID);
			if (cvtOmimID.getNumResources() > 0)
				spec.addCVTerm(cvtOmimID);
			if (cvtHgncID.getNumResources() > 0)
				spec.addCVTerm(cvtHgncID);
			if (cvtEnsemblID.getNumResources() > 0)
				spec.addCVTerm(cvtEnsemblID);
			if (cvtUniprotID.getNumResources() > 0)
				spec.addCVTerm(cvtUniprotID);
			if (cvtChebiID.getNumResources() > 0)
				spec.addCVTerm(cvtChebiID);
			if (cvtDrugbankID.getNumResources() > 0)
				spec.addCVTerm(cvtDrugbankID);
			if (cvtGoID.getNumResources() > 0)
				spec.addCVTerm(cvtGoID);
			if (cvtHGNCID.getNumResources() > 0)
				spec.addCVTerm(cvtHGNCID);
			if (cvtPubchemID.getNumResources() > 0)
				spec.addCVTerm(cvtPubchemID);
			if (cvt3dmetID.getNumResources() > 0)
				spec.addCVTerm(cvt3dmetID);
			if (cvtReactionID.getNumResources() > 0)
				spec.addCVTerm(cvtReactionID);
			if (cvtTaxonomyID.getNumResources() > 0)
				spec.addCVTerm(cvtTaxonomyID);

			// Finally, add the fully configured species.
			spec.setName(name);
			spec.appendNotes("</body>");
			specAnnot.setAbout("#" + spec.getMetaId());
			entry.setCustom(spec); // Remember node in KEGG Structure for
			// further references.
			if (addCellDesignerAnnots)
				addCellDesignerAnnotationToSpecies(spec, entry);
			// Not neccessary to add species to model, due to call in
			// "model.createSpecies()".
		}

		// ------------------------------------------------------------------

		// All species added. Parse reactions and relations.
		for (Reaction r : p.getReactions()) {
			// Skip reaction if it has either no reactants or no products.
			boolean hasAtLeastOneReactantAndProduct = false;
			for (ReactionComponent rc : r.getSubstrates()) {
				Entry spec = p.getEntryForName(rc.getName());
				if (spec == null || spec.getCustom() == null)
					continue;
				hasAtLeastOneReactantAndProduct = true;
				break;
			}
			if (!hasAtLeastOneReactantAndProduct)
				continue;
			hasAtLeastOneReactantAndProduct = false;
			for (ReactionComponent rc : r.getProducts()) {
				Entry spec = p.getEntryForName(rc.getName());
				if (spec == null || spec.getCustom() == null)
					continue;
				hasAtLeastOneReactantAndProduct = true;
				break;
			}
			if (!hasAtLeastOneReactantAndProduct)
				continue;

			org.sbml.jsbml.Reaction sbReaction = model.createReaction();
			sbReaction.initDefaults();
			sbReaction.setCompartment(compartment);
			Annotation rAnnot = new Annotation("");
			rAnnot.setAbout("");
			sbReaction.setAnnotation(rAnnot); // manchmal ist jSBML schon
			// bescheuert... (Annotation
			// darf nicht null sein, ist
			// aber default null).
			sbReaction
					.appendNotes("<body xmlns=\"http://www.w3.org/1999/xhtml\">");

			// Pro/ Edukte
			sbReaction.setReversible(r.getType()
					.equals(ReactionType.reversible));
			for (ReactionComponent rc : r.getSubstrates()) {
				SpeciesReference sr = sbReaction.createReactant();
				configureReactionComponent(p, rc, sr, 15); // 15 =Substrate
			}
			for (ReactionComponent rc : r.getProducts()) {
				SpeciesReference sr = sbReaction.createProduct();
				configureReactionComponent(p, rc, sr, 11); // 11 =Product
			}

			// Eventually add modifier
			int pos = reactionModifiers.indexOf(r.getName().toLowerCase()
					.trim());
			while (pos >= 0) { // Multiple modifiers possible
				sbReaction.addModifier(reactionModifiers.get(pos)
						.getInformation());
				reactionModifiers.remove(pos);
				pos = reactionModifiers.indexOf(r.getName().toLowerCase()
						.trim());
			}

			// Add Kegg-id Miriam identifier
			CVTerm reID = new CVTerm();
			reID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
			reID.setBiologicalQualifierType(Qualifier.BQB_IS);
			CVTerm rePWs = new CVTerm();
			reID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
			reID.setBiologicalQualifierType(Qualifier.BQB_OCCURS_IN);

			for (String ko_id : r.getName().split(" ")) {
				reID.addResource(KeggInfos.getMiriamURIforKeggID(r.getName()));

				// Retrieve further information via Kegg API -- Be careful: very
				// slow!
				KeggInfos infos = new KeggInfos(ko_id, manager);
				if (infos.queryWasSuccessfull()) {
					sbReaction.appendNotes("<p>");
					if (infos.getDefinition() != null) {
						sbReaction.appendNotes(String.format(
								"<b>Definition of %s%s%s:</b> %s<br/>\n",
								quotStart, ko_id.toUpperCase(), quotEnd,
								EscapeChars.forHTML(infos.getDefinition()
										.replace("\n", " "))));
						// System.out.println(sbReaction.getNotesString());
						// notes="<body xmlns=\"http://www.w3.org/1999/xhtml\"><p><b>&#8220;TEST&#8221;</b> A &lt;&#061;&gt;&#62;&#x3e;\u003E B<br/></p></body>";
					} else
						sbReaction.appendNotes(String.format(
								"<b>%s</b><br/>\n", ko_id.toUpperCase()));
					if (infos.getEquation() != null) {
						sbReaction.appendNotes(String.format(
								"<b>Equation for %s%s%s:</b> %s<br/>\n",
								quotStart, ko_id.toUpperCase(), quotEnd,
								EscapeChars.forHTML(infos.getEquation())));
					}
					String prefix = "http://www.genome.jp/Fig/reaction/";
					String suffix = KeggInfos.suffix(ko_id.toUpperCase())
							+ ".gif";
					sbReaction.appendNotes(String.format("<a href=\"%s%s\">",
							prefix, suffix));
					sbReaction.appendNotes(String.format(
							"<img src=\"%s%s\"/></a>\n", prefix, suffix));
					if (infos.getPathwayDescriptions() != null) {
						sbReaction.appendNotes("<b>Occurs in:</b><br/>\n");
						for (String desc : infos.getPathwayDescriptions()
								.split(",")) {
							// e.g.
							// ",Glycolysis / Gluconeogenesis,Metabolic pathways"
							sbReaction.appendNotes(desc + "<br/>\n");
						}
						sbReaction.appendNotes("<br/>\n");
					}
					sbReaction.appendNotes("</p>");

					if (rePWs != null && infos.getPathways() != null) {
						for (String pwId : infos.getPathways().split(",")) {
							rePWs.addResource(KeggInfos.miriam_urn_kgPathway
									+ KeggInfos.suffix(pwId));
						}
					}
				}
			}
			if ((reID.getNumResources() > 0) || (rePWs.getNumResources() > 0)) {
				sbReaction.getAnnotation().addRDFAnnotationNamespace("bqbiol",
						"", "http://biomodels.net/biology-qualifiers/");
			}
			if (reID.getNumResources() > 0) {
				sbReaction.addCVTerm(reID);
			}
			if (rePWs.getNumResources() > 0) {
				sbReaction.addCVTerm(rePWs);
			}

			// Finally, add the fully configured reaction.
			sbReaction.setName(r.getName());
			sbReaction.setId(NameToSId(r.getName()));
			sbReaction.appendNotes("</body>");
			sbReaction.setMetaId("meta_" + sbReaction.getId());
			sbReaction.setSBOTerm(231); // interaction. Most generic SBO Term
			// possible, for a reaction.
			rAnnot.setAbout("#" + sbReaction.getMetaId());
			if (addCellDesignerAnnots)
				addCellDesignerAnnotationToReaction(sbReaction, r);
		}

		// ------------------------------------------------------------------

		// TODO: Special reactions / relations.

		model.appendNotes("</body>");

		/*
		 * Removing nodes here does not work, because all CellDesigner
		 * annotations are static and don't get removed! if (removeOrphans) {
		 * ArrayList<Species> containedSpecies = new ArrayList<Species>(); for
		 * (org.sbml.jsbml.Reaction r: model.getListOfReactions()) { for
		 * (SpeciesReference s:r.getListOfProducts())
		 * containedSpecies.add(s.getSpeciesInstance()); for (SpeciesReference
		 * s:r.getListOfReactants())
		 * containedSpecies.add(s.getSpeciesInstance()); for
		 * (ModifierSpeciesReference s:r.getListOfModifiers())
		 * containedSpecies.add(s.getSpeciesInstance()); } for (int i=0;
		 * i<model.getListOfSpecies().size(); i++) { Species s =
		 * model.getListOfSpecies().get(i); if (!containedSpecies.contains(s))
		 * {model.removeSpecies(s); i--;} } }
		 */

		if (addCellDesignerAnnots) {
			addCellDesignerAnnotationToModel(p, annot, compartment);

			// Close open species CD tags.
			for (Species s : model.getListOfSpecies()) {
				String a = s.getAnnotation().getNoRDFAnnotation();
				if (a != null && a.length() > 0 && a.contains("celldesigner")) {
					s.getAnnotation().appendNoRDFAnnotation(
							"</celldesigner:extension>\n");
				}
			}
		}

		return doc;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.kegg.io.KeggConverter#lastFileWasOverwritten()
	 */
	public boolean lastFileWasOverwritten() {
		return lastFileWasOverwritten;
	}

	/**
	 * CellDesigner has special encodings for space, minus, alpha, etc. in the
	 * id attribute.
	 * 
	 * @param name
	 * @param forCellDesigner
	 * @return
	 */
	private String NameToCellDesignerName(String name) {
		name = name.trim().replace(" ", "_space_").replace("-", "_minus_")
				.replace("alpha", "_alpha_").replace("beta", "_beta_").replace(
						"gamma", "_gamma_").replace("delta", "_delta_")
				.replace("epsilon  ", "_epsilon_").replace("ALPHA", "_ALPHA_")
				.replace("BETA", "_BETA_").replace("GAMMA", "_GAMMA_").replace(
						"DELTA", "_DELTA_").replace("EPSILON  ", "_EPSILON_");

		return (name);
	}

	/**
	 * Generates a valid SId from a given name. If the name already is a valid
	 * SId, the name is returned. If the SId already exists in this document,
	 * "_<number>" will be appended and the next free number is being assigned.
	 * => See SBML L2V4 document for the Definition of SId. (Page 12/13)
	 * 
	 * @param name
	 * @return SId
	 */
	private String NameToSId(String name) {
		/*
		 * letter ::= �a�..�z�,�A�..�Z� digit ::= �0�..�9� idChar ::= letter |
		 * digit | �_� SId ::= ( letter | �_� ) idChar*
		 */
		String ret = "";
		if (name == null || name.trim().length() == 0) {
			ret = incrementSIdSuffix("SId");
			SIds.add(ret);
		} else {
			name = name.trim();
			char c = name.charAt(0);
			if (!(Character.isLetter(c) || c == '_'))
				ret = "SId_";
			else
				ret = Character.toString(c);
			for (int i = 1; i < name.length(); i++) {
				c = name.charAt(i);
				if (Character.isLetter(c) || Character.isDigit(c) || c == '_')
					ret += Character.toString(c);
			}
			if (SIds.contains(ret))
				ret = incrementSIdSuffix(ret);
			SIds.add(ret);
		}

		return ret;
	}

}
