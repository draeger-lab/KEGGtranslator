/*
 *  SBMLsqueezer creates rate equations for reactions in SBML files
 *  (http://sbml.org).
 *  Copyright (C) 2009 ZBIT, University of Tübingen, Andreas Dräger
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.zbit.kegg.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.Vector;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.Event;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.StoichiometryMath;
import org.sbml.jsbml.Unit;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.jsbml.util.compilers.ASTNodeValue;
import org.sbml.jsbml.util.compilers.LaTeX;

/**
 * This class is used to export a SBML model as LaTeX file.
 * 
 * @since 1.0
 * @version
 * @author Dieudonne Motsou Wouamba
 * @author Andreas Dr&auml;ger
 * @date 2007-12-04
 */
@SuppressWarnings("deprecation")
public class LaTeXExport extends LaTeX {

	/**
	 * Masks all special characters used by LaTeX with a backslash including
	 * hyphen symbols.
	 * 
	 * @param string
	 * @return
	 */
	public static String maskSpecialChars(String string) {
		return maskSpecialChars(string, true);
	}

	/**
	 * 
	 * @param string
	 * @param hyphen
	 *            if true a hyphen symbol is introduced at each position where a
	 *            special character has to be masked anyway.
	 * @return
	 */
	public static String maskSpecialChars(String string, boolean hyphen) {
		StringBuffer masked = new StringBuffer();
		for (int i = 0; i < string.length(); i++) {
			char atI = string.charAt(i);
			if (atI == '<')
				masked.append("$<$");
			else if (atI == '>')
				masked.append("$>$");
			else {
				if ((atI == '_') || (atI == '\\') || (atI == '$')
						|| (atI == '&') || (atI == '#') || (atI == '{')
						|| (atI == '}') || (atI == '~') || (atI == '%')
						|| (atI == '^')) {
					if ((i == 0) || (!hyphen))
						masked.append('\\');
					else if (hyphen && (string.charAt(i - 1) != '\\'))
						masked.append("\\-\\"); // masked.append('\\');
					// } else if ((atI == '[') || (atI == ']')) {
				}
				masked.append(atI);
			}
		}
		return masked.toString().trim();
	}

	/**
	 * Writes a LaTeX report for the given model with the given settings into
	 * the given TeX-file.
	 * 
	 * @param model
	 * @param texFile
	 * @param settings
	 * @throws IOException
	 * @throws SBMLException 
	 */
	public static void writeLaTeX(Model model, File texFile,
			boolean namesInEquations, boolean landscape,
			boolean typeWriterFont, boolean titlePage, short fontSize,
			String paperSize) throws IOException, SBMLException {
		BufferedWriter buffer = new BufferedWriter(new FileWriter(texFile));
		LaTeXExport exporter = new LaTeXExport(namesInEquations, landscape,
				typeWriterFont, titlePage, fontSize, paperSize);
		buffer.write(exporter.toLaTeX(model).toString());
		buffer.close();
	}

	/**
	 * This is the font size to be used in this document. Allowed values are:
	 * <ul>
	 * <li>8</li>
	 * <li>9</li>
	 * <li>10</li>
	 * <li>11</li>
	 * <li>12</li>
	 * <li>14</li>
	 * <li>16</li>
	 * <li>17</li>
	 * </ul>
	 * Other values are set to the default of 11.
	 */
	private short fontSize;

	/**
	 * If true this will produce LaTeX files for for entirely landscape
	 * documents
	 */
	private boolean landscape;

	private LaTeX latex;

	/**
	 * New line separator of this operating system
	 */
	private final String newLine = System.getProperty("line.separator");

	/**
	 * Allowed are
	 * <ul>
	 * <li>letter</li>
	 * <li>legal</li>
	 * <li>executive</li>
	 * <li>a* where * stands for values from 0 thru 9</li>
	 * <li>b*</li>
	 * <li>c*</li>
	 * <li>d*</li>
	 * </ul>
	 * The default is a4.
	 */
	private String paperSize;

	/**
	 * If true a title page will be created by LaTeX for the resulting document.
	 * Otherwise there will only be a title on top of the first page.
	 */
	private boolean titlepage;

	/**
	 * If true ids are set in typewriter font (default).
	 */
	private boolean typeWriter = true;

	// private boolean numberEquations = true;

	/**
	 * Constructs a new instance of LaTeX export. For each document to be
	 * translated a new instance has to be created. Here default values are used
	 * (A4 paper, 11pt, portrait, fancy headings, no titlepage).
	 */
	public LaTeXExport(boolean namesInEquations, boolean landscape,
			boolean typeWriterFont, boolean titlePage, short fontSize,
			String paperSize) {
		latex = new LaTeX(namesInEquations);
		setLandscape(landscape);
		setTypeWriter(typeWriterFont);
		setFontSize(fontSize);
		setPaperSize(paperSize);
		setTitlepage(titlePage);
	}

	/**
	 * Returns a unit.
	 * 
	 * @param u
	 * @return
	 */
	public StringBuilder format(Unit u) {
		StringBuilder buffer = new StringBuilder();
		boolean standardScale = (u.getScale() == 18) || (u.getScale() == 12)
				|| (u.getScale() == 9) || (u.getScale() == 6)
				|| (u.getScale() == 3) || (u.getScale() == 2)
				|| (u.getScale() == 1) || (u.getScale() == 0)
				|| (u.getScale() == -1) || (u.getScale() == -2)
				|| (u.getScale() == -3) || (u.getScale() == -6)
				|| (u.getScale() == -9) || (u.getScale() == -12)
				|| (u.getScale() == -15) || (u.getScale() == -18);
		if (u.getOffset() != 0d) {
			buffer.append(format(u.getOffset()).toString()
					.replaceAll("\\$", ""));
			if ((u.getMultiplier() != 0) || (!standardScale))
				buffer.append('+');
		}
		if (u.getMultiplier() != 1d) {
			if (u.getMultiplier() == -1d)
				buffer.append('-');
			else {
				buffer.append(format(u.getMultiplier()).toString().replaceAll(
						"\\$", ""));
				buffer.append(!standardScale ? "\\cdot " : "\\;");
			}
		}
		if (u.isKilogram()) {
			u.setScale(u.getScale() + 3);
			u.setKind(Unit.Kind.GRAM);
		}
		if (!u.isDimensionless()) {
			switch (u.getScale()) {
			case 18:
				buffer.append(mathrm('E'));
				break;
			case 15:
				buffer.append(mathrm('P'));
				break;
			case 12:
				buffer.append(mathrm('T'));
				break;
			case 9:
				buffer.append(mathrm('G'));
				break;
			case 6:
				buffer.append(mathrm('M'));
				break;
			case 3:
				buffer.append(mathrm('k'));
				break;
			case 2:
				buffer.append(mathrm('h'));
				break;
			case 1:
				buffer.append(mathrm("da"));
				break;
			case 0:
				break;
			case -1:
				buffer.append(mathrm('d'));
				break;
			case -2:
				buffer.append(mathrm('c'));
				break;
			case -3:
				buffer.append(mathrm('m'));
				break;
			case -6:
				buffer.append("\\upmu");
				break;
			case -9:
				buffer.append(mathrm('n'));
				break;
			case -12:
				buffer.append(mathrm('p'));
				break;
			case -15:
				buffer.append(mathrm('f'));
				break;
			case -18:
				buffer.append(mathrm('a'));
				break;
			default:
				buffer.append("10^{");
				buffer.append(Integer.toString(u.getScale()));
				buffer.append("}\\cdot ");
				break;
			}
			switch (u.getKind()) {
			case AMPERE:
				buffer.append(mathrm('A'));
				break;
			case BECQUEREL:
				buffer.append(mathrm("Bq"));
				break;
			case CANDELA:
				buffer.append(mathrm("cd"));
				break;
			case CELSIUS:
				buffer.append("\\text{\\textcelsius}");
				break;
			case COULOMB:
				buffer.append(mathrm('C'));
				break;
			case DIMENSIONLESS:
				break;
			case FARAD:
				buffer.append(mathrm('F'));
				break;
			case GRAM:
				buffer.append(mathrm('g'));
				break;
			case GRAY:
				buffer.append(mathrm("Gy"));
				break;
			case HENRY:
				buffer.append(mathrm('H'));
				break;
			case HERTZ:
				buffer.append(mathrm("Hz"));
				break;
			case INVALID:
				buffer.append(mathrm("invalid"));
				break;
			case ITEM:
				buffer.append(mathrm("item"));
				break;
			case JOULE:
				buffer.append(mathrm('J'));
				break;
			case KATAL:
				buffer.append(mathrm("kat"));
				break;
			case KELVIN:
				buffer.append(mathrm('K'));
				break;
			// case KILOGRAM:
			// buffer.append("\\mathrm{kg}");
			// break;
			case LITER:
				buffer.append(mathrm('l'));
				break;
			case LITRE:
				buffer.append(mathrm('l'));
				break;
			case LUMEN:
				buffer.append(mathrm("lm"));
				break;
			case LUX:
				buffer.append(mathrm("lx"));
				break;
			case METER:
				buffer.append(mathrm('m'));
				break;
			case METRE:
				buffer.append(mathrm('m'));
				break;
			case MOLE:
				buffer.append(mathrm("mol"));
				break;
			case NEWTON:
				buffer.append(mathrm('N'));
				break;
			case OHM:
				buffer.append("\\upOmega");
				break;
			case PASCAL:
				buffer.append(mathrm("Pa"));
				break;
			case RADIAN:
				buffer.append(mathrm("rad"));
				break;
			case SECOND:
				buffer.append(mathrm('s'));
				break;
			case SIEMENS:
				buffer.append(mathrm('S'));
				break;
			case SIEVERT:
				buffer.append(mathrm("Sv"));
				break;
			case STERADIAN:
				buffer.append(mathrm("sr"));
				break;
			case TESLA:
				buffer.append(mathrm('T'));
				break;
			case VOLT:
				buffer.append(mathrm('V'));
				break;
			case WATT:
				buffer.append(mathrm('W'));
				break;
			case WEBER:
				buffer.append(mathrm("Wb"));
				break;
			}
		} else {
			if (u.getScale() != 0) {
				buffer.append("10^{");
				buffer.append(Integer.toString(u.getScale()));
				buffer.append("}\\;");
			}
			buffer.append(mathrm("dimensionless"));
		}
		if (((u.getOffset() != 0d) || (u.getMultiplier() != 1d) || !standardScale)
				&& (u.getExponent() != 1d))
			buffer = brackets(buffer);
		if (u.getExponent() != 1) {
			buffer.append("^{");
			buffer.append(Double.toString(u.getExponent()));
			buffer.append('}');
		}
		return buffer;
	}

	/**
	 * Returns a properly readable unit definition.
	 * 
	 * @param def
	 * @return
	 */
	public StringBuffer format(UnitDefinition def) {
		StringBuffer buffer = new StringBuffer();
		for (int j = 0; j < def.getNumUnits(); j++) {
			buffer.append(format(def.getListOfUnits().get(j)));
			if (j < def.getListOfUnits().size() - 1)
				buffer.append("\\cdot ");
		}
		return buffer;
	}

	private StringBuffer getDocumentHead(String title) {
		StringBuffer head = new StringBuffer("\\documentclass[" + fontSize
				+ "pt");
		if (titlepage) {
			head.append(",titlepage");
		}
		if (landscape) {
			head.append(",landscape");
		}
		head.append("," + paperSize + "paper]{scrartcl}");
		head.append(newLine + "\\usepackage[scaled=.9]{helvet}" + newLine
				+ "\\usepackage{amsmath}" + newLine + "\\usepackage{courier}"
				+ newLine + "\\usepackage{times}" + newLine
				+ "\\usepackage[english]{babel}" + newLine
				+ "\\usepackage{a4wide}" + newLine + "\\usepackage{longtable}"
				+ newLine + "\\usepackage{booktabs}" + newLine);
		head.append("\\usepackage{url}" + newLine);
		if (landscape)
			head.append("\\usepackage[landscape]{geometry}" + newLine);
		head
				.append("\\title{\\textsc{SBMLsqueezer}: Differential Equation System ``"
						+ title
						+ "\"}"
						+ newLine
						+ "\\date{\\today}"
						+ newLine
						+ "\\begin{document}"
						+ newLine
						+ "\\author{}"
						+ newLine + "\\maketitle" + newLine);
		return head;
	}

	/**
	 * Writing laTeX code of a string id
	 * 
	 * @param pluginSpecies
	 * @return String
	 */
	public String idToTeX(Species pluginSpecies) {
		return nameToTeX(pluginSpecies.getId());
	}

	public boolean isTypeWriter() {
		return typeWriter;
	}

	private String name_idToLaTex(String s) {
		return "$" + toTeX(s) + "$";
	}

	/**
	 * Writing laTeX code of a string name
	 * 
	 * @param name
	 * @return String
	 */
	public String nameToTeX(String name) {
		String speciesTeX = name;
		int numUnderscore = (new StringTokenizer(speciesTeX, "_"))
				.countTokens() - 1;
		if (numUnderscore > 1)
			speciesTeX = replaceAll("_", speciesTeX, "\\_");
		else if ((numUnderscore == 0) && (0 < speciesTeX.length())) {
			int index = -1;
			while (index != (name.length() - 1)
					&& !Character.isDigit(name.charAt(index + 1)))
				index++;
			if ((-1 < index) && (index < name.length())) {
				String num = name.substring(++index);
				speciesTeX = speciesTeX.substring(0, index++) + "_";
				speciesTeX += (num.length() == 1) ? num : "{" + num + "}";
			}
		}
		return speciesTeX;
	}

	/**
	 * 
	 * @param reaction
	 * @return
	 * @throws SBMLException 
	 */
	public String reactionEquation(Reaction reaction) throws SBMLException {
		StringBuffer reactionEqn = new StringBuffer();
		reactionEqn.append(LaTeX.eqBegin);
		LaTeX latex = new LaTeX();
		int count = 0;
		for (SpeciesReference specRef : reaction.getListOfReactants()) {
			if (count > 0)
				reactionEqn.append(" + ");
			if (specRef.isSetStoichiometryMath())
				reactionEqn.append(specRef.getStoichiometryMath().getMath()
						.compile(latex));
			else if (specRef.getStoichiometry() != 1d)
				reactionEqn.append(specRef.getStoichiometry());
			reactionEqn.append(' ');
			reactionEqn.append(latex.mbox(LaTeX.maskSpecialChars(specRef
					.getSpecies())));
			count++;
		}
		if (reaction.getNumReactants() == 0)
			reactionEqn.append("\\emptyset");
		reactionEqn.append(reaction.getReversible() ? " \\leftrightarrow"
				: " \\rightarrow");
		// if (reaction.getNumModifiers() > 0) {
		// reactionEqn.append('{');
		// count = 0;
		// for (ModifierSpeciesReference modRef : reaction
		// .getListOfModifiers()) {
		// reactionEqn
		// .append(LaTeX.mbox(
		// LaTeX.maskSpecialChars(modRef.getSpecies()))
		// .toString());
		// if (count < reaction.getNumModifiers() - 1)
		// reactionEqn.append(", ");
		// count++;
		// }
		// reactionEqn.append('}');
		// }
		reactionEqn.append(' ');
		count = 0;
		for (SpeciesReference specRef : reaction.getListOfProducts()) {
			if (count > 0)
				reactionEqn.append(" + ");
			if (specRef.isSetStoichiometryMath())
				reactionEqn.append(specRef.getStoichiometryMath().getMath()
						.compile(latex));
			else if (specRef.getStoichiometry() != 1d)
				reactionEqn.append(specRef.getStoichiometry());
			reactionEqn.append(' ');
			reactionEqn.append(latex.mbox(LaTeX.maskSpecialChars(specRef
					.getSpecies())));
			count++;
		}
		if (reaction.getNumProducts() == 0)
			reactionEqn.append("\\emptyset");
		reactionEqn.append(LaTeX.eqEnd);
		return reactionEqn.toString();
	}

	/**
	 * a methode for string replacement
	 * 
	 * @param what
	 * @param inString
	 * @param replacement
	 * @return string
	 */
	public String replaceAll(String what, String inString, String replacement) {
		StringTokenizer st = new StringTokenizer(inString, what);
		String end = st.nextElement().toString();
		while (st.hasMoreElements())
			end += replacement + st.nextElement().toString();
		return end;
	}

	/**
	 * This is the font size to be used in this document.
	 * 
	 * @param Allowed
	 *            values are:
	 *            <ul>
	 *            <li>8</li>
	 *            <li>9</li>
	 *            <li>10</li>
	 *            <li>11</li>
	 *            <li>12</li>
	 *            <li>14</li>
	 *            <li>16</li>
	 *            <li>17</li>
	 *            </ul>
	 *            Other values are set to the default of 11.
	 * 
	 */
	public void setFontSize(short fontSize) {
		if ((fontSize < 8) || (fontSize == 13) || (17 < fontSize))
			this.fontSize = 11;
		this.fontSize = fontSize;
	}

	/**
	 * If true is given the whole document will be created in landscape mode.
	 * Default is portrait.
	 * 
	 * @param landscape
	 */
	public void setLandscape(boolean landscape) {
		this.landscape = landscape;
	}

	/**
	 * Allowed are
	 * <ul>
	 * <li>letter</li>
	 * <li>legal</li>
	 * <li>executive</li>
	 * <li>a* where * stands for values from 0 thru 9</li>
	 * <li>b*</li>
	 * <li>c*</li>
	 * <li>d*</li>
	 * </ul>
	 * The default is a4.
	 */
	public void setPaperSize(String paperSize) {
		paperSize = paperSize.toLowerCase();
		if (paperSize.equals("letter") || paperSize.equals("legal")
				|| paperSize.equals("executive"))
			this.paperSize = paperSize;
		else if (paperSize.length() == 2) {
			if (!Character.isDigit(paperSize.charAt(1))
					|| ((paperSize.charAt(0) != 'a')
							&& (paperSize.charAt(0) != 'b')
							&& (paperSize.charAt(0) != 'c') && (paperSize
							.charAt(0) != 'd')))
				this.paperSize = "a4";
			else {
				short size = Short.parseShort(Character.toString(paperSize
						.charAt(1)));
				if ((0 <= size) && (size < 10))
					this.paperSize = paperSize;
				else
					this.paperSize = "a4";
			}
		} else
			this.paperSize = "a4";
		this.paperSize = paperSize;
	}

	/**
	 * If true an extra title page is created. Default false.
	 * 
	 * @param titlepage
	 */
	public void setTitlepage(boolean titlepage) {
		this.titlepage = titlepage;
	}

	/**
	 * If true ids are set in typewriter font (default).
	 * 
	 * @param typeWriter
	 */
	public void setTypeWriter(boolean typeWriter) {
		this.typeWriter = typeWriter;
	}

	/**
	 * This is a method to write the latex file
	 * 
	 * @param astnode
	 * @param file
	 * @throws IOException
	 * @throws SBMLException 
	 */
	public StringBuffer toLaTeX(Model model) throws IOException, SBMLException {
		StringBuffer laTeX;
		String newLine = System.getProperty("line.separator");
		String title = model.getName().length() > 0 ? model.getName()
				.replaceAll("_", " ") : model.getId().replaceAll("_", " ");
		StringBuffer head = getDocumentHead(title);

		String rateHead = newLine + "\\section{Rate Laws}" + newLine;
		String speciesHead = newLine + "\\section{Equations}";
		String begin = /* (numberEquations) ? */newLine + "\\begin{equation}"
				+ newLine/* : newLine + "\\begin{equation*}" + newLine */;
		String end = /* (numberEquations) ? */newLine + "\\end{equation}"
				+ newLine
		/* : newLine + "\\end{equation*}" + newLine */;
		String tail = newLine
				+ "\\begin{center}"
				+ newLine
				+ "For a more comprehensive \\LaTeX{} export, see "
				+ "\\url{http://www.ra.cs.uni-tuebingen.de/software/SBML2LaTeX}"
				+ newLine + "\\end{center}" + newLine + "\\end{document}"
				+ newLine + newLine;

		String rateLaws[] = new String[(int) model.getNumReactions()];
		String sp[] = new String[(int) model.getNumSpecies()];
		int reactionIndex, speciesIndex, sReferenceIndex;
		Species species;
		SpeciesReference speciesRef;
		HashMap<String, Integer> speciesIDandIndex = new HashMap<String, Integer>();
		for (reactionIndex = 0; reactionIndex < model.getNumReactions(); reactionIndex++) {
			Reaction r = model.getReaction(reactionIndex);
			int latexReactionIndex = reactionIndex + 1;

			rateLaws[reactionIndex] = (!r.getName().equals("") && !r.getName()
					.equals(r.getId())) ? "\\subsection{Reaction: \\texttt{"
					+ replaceAll("_", r.getId(), "\\_") + "}" + " ("
					+ replaceAll("_", r.getName(), "\\_") + ")}" + newLine
					+ begin + "v_{" + latexReactionIndex + "}="
					: "\\subsection{Reaction: \\texttt{"
							+ replaceAll("_", r.getId(), "\\_") + "}}"
							+ newLine + begin + "v_{" + latexReactionIndex
							+ "}=";
			if (r.getKineticLaw() != null) {
				if (r.getKineticLaw().getMath() != null)
					rateLaws[reactionIndex] += r.getKineticLaw().getMath()
							.compile(latex);
				else
					rateLaws[reactionIndex] += "\\text{no mathematics specified}";
			} else
				rateLaws[reactionIndex] += "\\text{no kinetic law specified}";
			for (speciesIndex = 0; speciesIndex < model.getNumSpecies(); speciesIndex++) {
				speciesIDandIndex.put(model.getSpecies(speciesIndex).getId(),
						Integer.valueOf(speciesIndex));
			}
		}

		Vector<Species> reactants = new Vector<Species>();
		Vector<Species> products = new Vector<Species>();
		Vector<Integer> reactantsReaction = new Vector<Integer>();
		Vector<Integer> productsReaction = new Vector<Integer>();
		Vector<SpeciesReference> reactantsStochiometric = new Vector<SpeciesReference>();
		Vector<SpeciesReference> productsStochiometric = new Vector<SpeciesReference>();

		for (reactionIndex = 0; reactionIndex < model.getNumReactions(); reactionIndex++) {
			Reaction r = model.getReaction(reactionIndex);
			int latexReactionIndex = reactionIndex + 1;
			int reactant = 0;
			int product = 0;
			for (sReferenceIndex = 0; sReferenceIndex < r.getNumReactants(); sReferenceIndex++) {
				speciesRef = r.getReactant(sReferenceIndex);
				speciesIndex = (int) speciesIDandIndex.get(
						speciesRef.getSpecies()).longValue();
				species = model.getSpecies(speciesIndex);
				reactants.add(reactant, species);
				reactantsReaction.add(reactant, latexReactionIndex);
				reactantsStochiometric.add(reactant, speciesRef);
				reactant++;
			}

			for (sReferenceIndex = 0; sReferenceIndex < r.getNumProducts(); sReferenceIndex++) {
				speciesRef = r.getProduct(sReferenceIndex);
				speciesIndex = (int) speciesIDandIndex.get(
						speciesRef.getSpecies()).longValue();
				species = model.getSpecies(speciesIndex);
				products.add(product, species);
				productsReaction.add(product, latexReactionIndex);
				productsStochiometric.add(product, speciesRef);
				product++;
			}
		}
		for (speciesIndex = 0; speciesIndex < model.getNumSpecies(); speciesIndex++) {
			String sEquation = "";
			ASTNode stoch = null;
			StoichiometryMath stochMath;
			SpeciesReference ref;
			species = model.getSpecies(speciesIndex);
			for (int k = 0; k < reactants.size(); k++) {
				if (species.getId().equals(reactants.get(k).getId())) {
					ref = reactantsStochiometric.get(k);
					if (ref != null) {
						stochMath = ref.getStoichiometryMath();
						if (stochMath != null && stochMath.isSetMath()) {
							stoch = stochMath.getMath();
							sEquation += (stoch.getType() == ASTNode.Type.PLUS || stoch
									.getType() == ASTNode.Type.MINUS) ? sEquation += "-\\left("
									+ stoch.compile(latex)
									+ "\\right)v_{"
									+ reactantsReaction.get(k) + "}"
									: "-" + stoch.compile(latex) + "v_{"
											+ reactantsReaction.get(k) + "}";
						} else {
							double doubleStoch = reactantsStochiometric.get(k)
									.getStoichiometry();
							if (doubleStoch == 1.0)
								sEquation += "-v_{" + reactantsReaction.get(k)
										+ "}";
							else {
								int intStoch = (int) doubleStoch;
								if ((doubleStoch - intStoch) == 0.0)
									sEquation += "-" + intStoch + "v_{"
											+ reactantsReaction.get(k) + "}";
								else
									sEquation += "-" + doubleStoch + "v_{"
											+ reactantsReaction.get(k) + "}";
							}
						}
					}
				}
			}

			for (int k = 0; k < products.size(); k++) {
				if (species.getId().equals(products.get(k).getId())) {
					ref = productsStochiometric.get(k);
					if (ref != null) {
						stochMath = ref.getStoichiometryMath();
						if (stochMath != null) {
							if (stochMath.isSetMath())
								stoch = stochMath.getMath();
							if (sEquation == "") {
								if (stoch != null) {
									sEquation += (stoch.getType() == ASTNode.Type.PLUS || stoch
											.getType() == ASTNode.Type.MINUS) ? sEquation += "\\left("
											+ stoch.compile(latex)
											+ "\\right)v_{"
											+ productsReaction.get(k) + "}"
											: stoch.compile(latex) + "v_{"
													+ productsReaction.get(k)
													+ "}";
								} else {
									double doubleStoch = productsStochiometric
											.get(k).getStoichiometry();
									if (doubleStoch == 1.0)
										sEquation += "v_{"
												+ productsReaction.get(k) + "}";
									else {
										int intStoch = (int) doubleStoch;
										if ((doubleStoch - intStoch) == 0.0)
											sEquation += intStoch + "v_{"
													+ productsReaction.get(k)
													+ "}";
										else
											sEquation += doubleStoch + "v_{"
													+ productsReaction.get(k)
													+ "}";
									}
								}

							} else {
								if (stoch != null) {
									sEquation += (stoch.getType() == ASTNode.Type.PLUS || stoch
											.getType() == ASTNode.Type.MINUS) ? sEquation += "+\\left("
											+ stoch.compile(latex)
											+ "\\right)v_{"
											+ productsReaction.get(k) + "}"
											: "+" + stoch.compile(latex)
													+ "v_{"
													+ productsReaction.get(k)
													+ "}";
								} else {
									double doubleStoch = productsStochiometric
											.get(k).getStoichiometry();
									if (doubleStoch == 1.0)
										sEquation += "+v_{"
												+ productsReaction.get(k) + "}";
									else {
										int intStoch = (int) doubleStoch;
										if ((doubleStoch - intStoch) == 0.0)
											sEquation += "+" + intStoch + "v_{"
													+ productsReaction.get(k)
													+ "}";
										else
											sEquation += "+" + doubleStoch
													+ "v_{"
													+ productsReaction.get(k)
													+ "}";
									}
								}
							}
						}
					}
				}
			}

			if (sEquation.equals("")) {
				sp[speciesIndex] = (!species.getName().equals("") && !species
						.getName().equals(species.getId())) ? "\\subsection{Species: \\texttt{"
						+ replaceAll("_", species.getId(), "\\_")
						+ "}"
						+ " ("
						+ replaceAll("_", species.getName(), "\\_")
						+ ")}"
						+ begin
						+ "\\frac{\\mathrm {d["
						+ idToTeX(species)
						+ "]}}{\\mathrm dt}= 0"
						: "\\subsection{Species: \\texttt{"
								+ replaceAll("_", species.getId(), "\\_")
								+ "}}" + begin + "\\frac{\\mathrm{d["
								+ idToTeX(species) + "]}}{\\mathrm dt}= 0";
			} else if (!species.getBoundaryCondition()
					&& !species.getConstant()) {
				sp[speciesIndex] = (!species.getName().equals("") && !species
						.getName().equals(species.getId())) ? "\\subsection{Species: \\texttt{"
						+ replaceAll("_", species.getId(), "\\_")
						+ "}"
						+ " ("
						+ replaceAll("_", species.getName(), "\\_")
						+ ")}"
						+ begin
						+ "\\frac{\\mathrm{d["
						+ idToTeX(species)
						+ "]}}{\\mathrm dt}= " + sEquation
						: "\\subsection{Species: \\texttt{"
								+ replaceAll("_", species.getId(), "\\_")
								+ "}}" + begin + "\\frac{\\mathrm{d["
								+ idToTeX(species) + "]}}{\\mathrm {dt}}= "
								+ sEquation;
			} else {
				sp[speciesIndex] = (!species.getName().equals("") && !species
						.getName().equals(species.getId())) ? "\\subsection{Species: \\texttt{"
						+ replaceAll("_", species.getId(), "\\_")
						+ "}"
						+ " ("
						+ replaceAll("_", species.getName(), "\\_")
						+ ")}"
						+ begin
						+ "\\frac{\\mathrm {d["
						+ idToTeX(species)
						+ "]}}{\\mathrm {dt}}= 0"
						: "\\subsection{Species: \\texttt{"
								+ replaceAll("_", species.getId(), "\\_")
								+ "}}" + begin + "\\frac{\\mathrm {d["
								+ idToTeX(species) + "]}}{\\mathrm {dt}}= 0";
			}
		}
		// String rulesHead = newLine + "\\section{Rules}" + newLine;
		String eventsHead = newLine + "\\section{Events}";
		// String constraintsHead = newLine + "\\section{Constraints}";
		LinkedList<?> events[] = new LinkedList[(int) model.getNumEvents()];
		int i;
		// writing latex
		laTeX = head;
		// writing Rate Laws
		laTeX.append(rateHead);
		for (i = 0; i < rateLaws.length; i++) {
			laTeX.append(rateLaws[i] + end);
		}
		// writing Equations
		laTeX.append(speciesHead);
		for (i = 0; i < sp.length; i++) {
			laTeX.append(sp[i] + end);
		}
		// writing Rules

		// writing Events
		if (model.getNumEvents() > 0) {
			Event ev;
			for (i = 0; i < model.getNumEvents(); i++) {
				ev = model.getEvent(i);
				LinkedList<ASTNodeValue> assignments = new LinkedList<ASTNodeValue>();
				assignments.add(ev.getTrigger().getMath().compile(latex));
				for (int j = 0; j < ev.getNumEventAssignments(); j++)
					assignments.add(ev.getEventAssignment(j).getMath().compile(
							latex));
				events[i] = assignments;
			}
			laTeX.append(eventsHead);
			String var;
			for (i = 0; i < events.length; i++) {
				ev = model.getEvent(i);
				if (ev.getName() == null)
					laTeX.append("\\subsection{Event:}");
				else
					laTeX.append("\\subsection{Event: " + ev.getName() + "}");
				if (ev.getNumEventAssignments() > 1) {
					laTeX.append("\\texttt{Triggers if: }" + newLine);
					laTeX.append(/* (numberEquations) ? */"\\begin{equation}"
							+ events[i].get(0) + "\\end{equation}" + newLine);
					/*
					 * : "\\begin{equation*}" + events[i].get(0) +
					 * "\\end{equation*}" + newLine
					 */;
					if (ev.getDelay() == null)
						laTeX.append(newLine
								+ "\\texttt{and assigns the following rule: }"
								+ newLine);
					else {
						laTeX.append(newLine
								+ "\\texttt{and assigns after a delay of "
								+ ev.getDelay().getMath().compile(latex));
						if (!ev.getTimeUnits().equals(null))
							laTeX.append(ev.getTimeUnits()
									+ " the following rules: }" + newLine);
						else
							laTeX.append(" s the following rules: }" + newLine);
					}
				} else {
					laTeX.append("\\texttt{Triggers if: }" + newLine);
					laTeX.append(/* (numberEquations) ? */"\\begin{equation}"
							+ events[i].get(0) + "\\end{equation}" + newLine)
					/*
					 * : "\\begin{equation*}" + events[i].get(0) +
					 * "\\end{equation*}" + newLine
					 */;
					if (ev.getDelay() == null)
						laTeX.append(newLine
								+ "\\texttt{and assigns the following rule: }"
								+ newLine);
					else {
						laTeX.append(newLine
								+ "\\texttt{and assigns after a delay of "
								+ ev.getDelay().getMath().compile(latex));
						if (!ev.getTimeUnits().equals(null))
							laTeX.append(ev.getTimeUnits()
									+ " the following rule: }" + newLine);
						else
							laTeX.append(" s the following rule: }" + newLine);
					}
				}
				if (events[i].size() > 1)
					for (int j = 0; j < events[i].size() - 1; j++) {
						var = ev.getEventAssignment(j).getVariable();
						if (model.getSpecies(var) != null)
							laTeX.append(begin + "["
									+ idToTeX(model.getSpecies(var)) + "]"
									+ " = " + events[i].get(j + 1) + end
									+ newLine);
						else if (model.getParameter(var) != null)
							laTeX.append(begin
									+ toTeX(model.getParameter(var).getId())
									+ " = " + events[i].get(j + 1) + end
									+ newLine);
						else
							laTeX.append(begin + events[i].get(j + 1) + end
									+ newLine);
					}
				else
					for (int j = 0; j < events[i].size() - 1; j++) {
						var = ev.getEventAssignment(j).getVariable();
						if (model.getSpecies(var) != null)
							laTeX.append(begin + "["
									+ idToTeX(model.getSpecies(var)) + "]"
									+ " = " + events[i].get(j + 1) + end
									+ newLine);
						else if (model.getParameter(var) != null)
							laTeX.append(begin
									+ toTeX(model.getParameter(var).getId())
									+ " = " + events[i].get(j + 1) + end
									+ newLine);
						else
							laTeX.append(begin + events[i].get(j + 1) + end
									+ newLine);
					}
			}
		}

		// writing Constraints

		// writing parameters
		if (model.getNumParameters() > 0) {
			laTeX.append(newLine + "\\section{Parameters}");
			laTeX.append("\\begin{longtable}{@{}llr@{}}" + newLine
					+ "\\toprule " + newLine + "Parameter & Value \\\\  "
					+ newLine + "\\midrule" + newLine);
			for (i = 0; i < model.getNumParameters(); i++) {
				laTeX.append(name_idToLaTex(model.getParameter(i).getId())
						+ "&" + model.getParameter(i).getValue() + "\\\\"
						+ newLine);
			}
			laTeX.append("\\bottomrule " + newLine + "\\end{longtable}");
		}
		// writing species list and compartment.
		if (model.getNumSpecies() > 0) {
			laTeX.append(newLine + "\\section{Species}" + newLine);
			laTeX.append("\\begin{longtable}{@{}llr@{}} " + newLine
					+ "\\toprule " + newLine
					+ "Species & Initial concentration & compartment \\\\  "
					+ newLine + "\\midrule" + newLine);
			for (i = 0; i < model.getNumSpecies(); i++) {
				laTeX.append(name_idToLaTex(model.getSpecies(i).getId()) + "&"
						+ model.getSpecies(i).getInitialConcentration() + "&"
						+ model.getSpecies(i).getCompartment() + "\\\\"
						+ newLine);
			}
			laTeX.append("\\bottomrule " + newLine + "\\end{longtable}");
		}
		if (model.getNumCompartments() > 0) {
			laTeX.append(newLine + "\\section{Compartments}");
			laTeX.append("\\begin{longtable}{@{}llr@{}}" + newLine
					+ "\\toprule " + newLine + "Compartment & Volume \\\\  "
					+ newLine + "\\midrule" + newLine);
			for (i = 0; i < model.getNumCompartments(); i++) {
				laTeX.append(name_idToLaTex(model.getCompartment(i).getId())
						+ "&" + model.getCompartment(i).getVolume() + "\\\\"
						+ newLine);
			}
			laTeX.append("\\bottomrule " + newLine + "\\end{longtable}");
		}
		laTeX.append(newLine + tail);
		return laTeX;
	}

	/*
	 * public void setNumberEquations(boolean numberEquations) {
	 * this.numberEquations = numberEquations; }
	 */

	/**
	 * Writing a laTeX file
	 * 
	 * @param model
	 * @param file
	 * @throws IOException
	 * @throws SBMLException 
	 */
	public void toLaTeX(Model model, File file) throws IOException, SBMLException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		bw.append(toLaTeX(model));
		bw.close();
	}

	public StringBuffer toLaTeX(Reaction reaction) throws IOException, SBMLException {
		Model model = reaction.getModel();
		String title = model.getName().length() > 0 ? model.getName()
				.replaceAll("_", " ") : model.getId().replaceAll("_", " ");
		StringBuffer laTeX = getDocumentHead(title);
		String name = maskSpecialChars(reaction.getId());
		laTeX.append("\\begin{equation*}");
		laTeX.append(newLine);
		laTeX.append("v_\\mathtt{");
		laTeX.append(name);
		laTeX.append("}= ");
		if ((reaction.getKineticLaw() != null)
				&& (reaction.getKineticLaw().getMath() != null))
			laTeX.append(reaction.getKineticLaw().getMath().compile(latex));
		else
			laTeX.append(" \\mathrm{undefined} ");
		laTeX.append(newLine + "\\end{equation*}");
		laTeX
				.append(newLine
						+ "\\begin{center} For a more comprehensive \\LaTeX{} "
						+ "export, see \\url{http://www.ra.cs.uni-tuebingen.de/software/SBML2LaTeX}"
						+ "\\end{center}");
		laTeX.append(newLine + "\\end{document}");
		return laTeX;
	}

	/**
	 * Writing laTeX code of a string name
	 * 
	 * @param name
	 * @return String
	 */
	private String toTeX(String name) {
		String tex = "";
		String help = "";
		String sign = "";
		if (name.toLowerCase().startsWith("kass")) {
			tex += "k^\\mathrm{ass}";
			name = name.substring(4, name.length());
		} else if (name.toLowerCase().startsWith("kcatp")) {
			tex += "k^\\mathrm{cat}";
			name = name.substring(5, name.length());
			sign = "+";
		} else if (name.toLowerCase().startsWith("kcatn")) {
			tex += "k^\\mathrm{cat}";
			name = name.substring(5, name.length());
			sign = "-";
		} else if (name.toLowerCase().startsWith("kdiss")) {
			tex += "k^\\mathrm{diss}";
			name = name.substring(5, name.length());
		} else if (name.toLowerCase().startsWith("km")) {
			tex += "k^\\mathrm{m}";
			name = name.substring(2, name.length());
		} else if (name.toLowerCase().startsWith("ki")) {
			tex += "k^\\mathrm{i}";
			name = name.substring(2, name.length());
		} else {
			int j = 0;
			while (j < name.length() && !(name.substring(j, j + 1).equals("_"))
					&& !(Character.isDigit(name.charAt(j)))) {
				tex += name.substring(j, j + 1);
				j++;
			}
			name = name.substring(j - 1, name.length());
		}
		String s = "_{" + sign;
		String nameIndex = "";
		for (int i = 0; i < name.length(); i++) {
			if (i > 0) {
				nameIndex = name.substring(i, i + 1);
				if (Character.isDigit(name.charAt(i))) {
					int k = i;
					while (i < name.length()) {
						if (Character.isDigit(name.charAt(i)))
							i++;
						else
							break;
					}
					nameIndex = name.substring(k, i);
					if (name.substring(k - 1, k).equals("_")) {
						if (s.endsWith("{") || s.endsWith("+")
								|| s.endsWith("-"))
							s += nameIndex;
						else if (!s.endsWith(","))
							s += ", " + nameIndex;
					} else {
						if (s.endsWith("{")) {
							s += help + "_{" + nameIndex + "}";
							help = "";
						} else {
							s += ", " + help + "_{" + nameIndex + "}";
							help = "";
						}
					}
				} else if (!nameIndex.equals("_"))
					help += nameIndex;
			}
		}
		s += "}";
		return tex + s;
	}
}
