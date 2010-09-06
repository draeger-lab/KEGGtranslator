/*
 * $Id: TextFormula.java 151 2010-02-11 17:23:46Z andreas-draeger $
 * $URL: https://jsbml.svn.sourceforge.net/svnroot/jsbml/trunk/src/org/sbml/jsbml/util/TextFormula.java $
 *
 *
 *==================================================================================
 * Copyright (c) 2009 the copyright is held jointly by the individual
 * authors. See the file AUTHORS for the list of authors.
 *
 * This file is part of jsbml, the pure java SBML library. Please visit
 * http://sbml.org for more information about SBML, and http://jsbml.sourceforge.net/
 * to get the latest version of jsbml.
 *
 * jsbml is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * jsbml is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with jsbml.  If not, see <http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html>.
 *
 *===================================================================================
 *
 */
package de.zbit.kegg.gui;

import java.util.List;
import java.util.Vector;

import org.sbml.jsbml.Unit;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.jsbml.util.StringTools;
import org.sbml.jsbml.util.compilers.ASTNodeValue;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-04-08
 * 
 */
public class HTMLFormula extends StringTools {

	
	/**
	 * Creates an HTML string representation of this UnitDefinition.
	 * 
	 * @return
	 */
	public static String toHTML(UnitDefinition ud) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ud.getNumUnits(); i++) {
			Unit unit = ud.getUnit(i);
			if (i > 0) {
				sb.append(" &#8901; ");
			}
			sb.append(toHTML(unit));
		}
		return sb.toString();
	}
	
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static String toHTML(Unit u) {
		StringBuffer times = new StringBuffer();
		if (u.getMultiplier() != 0) {
			if (u.getMultiplier() != 1)
				times.append(StringTools.toString(u.getMultiplier()));
			StringBuffer pow = new StringBuffer();
			pow.append(u.getKind().getSymbol());
			String prefix = u.getPrefix();
			if (prefix.length() > 0 && !prefix.startsWith("10")) {
				pow.insert(0, prefix);
			} else if (u.getScale() != 0) {
				pow.insert(0, ' ');
				pow = HTMLFormula.times(HTMLFormula.pow(Integer.valueOf(10),
						u.getScale()), pow);
			}
			times = HTMLFormula.times(times, pow);
		}
		if (u.getOffset() != 0) {
			times = HTMLFormula.sum(StringTools.toString(u.getOffset()),
					times);
		}
		return HTMLFormula.pow(times, Double.valueOf(u.getExponent())).toString();
	}

	/**
	 * Returns the basis to the power of the exponent as StringBuffer. Several
	 * special cases are treated.
	 * 
	 * @param basis
	 * @param exponent
	 * @return
	 */
	public static final StringBuffer pow(Object basis, Object exponent) {
		try {
			if (Double.parseDouble(exponent.toString()) == 0f)
				return new StringBuffer("1");
			if (Double.parseDouble(exponent.toString()) == 1f)
				return basis instanceof StringBuffer ? (StringBuffer) basis
						: new StringBuffer(basis.toString());
		} catch (NumberFormatException exc) {
		}
		String b = basis.toString();
		if (b.contains("&#8901;") || b.contains("-") || b.contains("+")
				|| b.contains("/") || b.contains("<sup>"))
			basis = brackets(basis);
		String e = exponent.toString();
		if (e.contains("&#8901;") || e.substring(1).contains("-")
				|| e.contains("+") || e.contains("/") || e.contains("<sup>"))
			exponent = brackets(e);
		return concat(basis, "<sup>", exponent, "</sup>");
	}

	/**
	 * Returns the product of the given elements as StringBuffer.
	 * 
	 * @param factors
	 * @return
	 */
	public static final StringBuffer times(Object... factors) {
		return arith("&#8901;", factors);
	}

	/**
	 * Returns the sum of the given elements as StringBuffer.
	 * 
	 * @param summands
	 * @return
	 */
	public static final StringBuffer sum(Object... summands) {
		return brackets(arith(Character.valueOf('+'), summands));
	}

	/**
	 * 
	 * @param arith
	 * @return
	 */
	private static StringBuffer brackets(Object arith) {
		return concat("(", arith, ")");
	}

	/**
	 * Basic method which links several elements with a mathematical operator.
	 * All empty StringBuffer object are excluded.
	 * 
	 * @param operator
	 * @param elements
	 * @return
	 */
	private static final StringBuffer arith(Object operator, Object... elements) {
		List<Object> vsb = new Vector<Object>();
		for (Object sb : elements)
			if (sb != null && sb.toString().length() > 0)
				vsb.add(sb);
		StringBuffer equation = new StringBuffer();
		if (vsb.size() > 0)
			equation.append(vsb.get(0));
		String op = operator.toString();
		for (int count = 1; count < vsb.size(); count++)
			append(equation, op, vsb.get(count));
		return equation;
	}

	/* (non-Javadoc)
	 * @see org.sbml.jsbml.ASTNodeCompiler#toString(org.sbml.jsbml.ASTNodeValue)
	 */
	public String toString(ASTNodeValue value) {
		return value.toString();
	}

}
