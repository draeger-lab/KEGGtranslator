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

import de.zbit.kegg.parser.pathway.Pathway;

/**
 * Generic interface for {@link KEGGtranslator}s.
 * 
 * @author Clemens Wrzodek
 * @since 1.0
 * @version $Rev$
 */
public interface KEGGtranslator {
  
  
	/**
	 * Translate a given KEGG Pathway and write it in the new format to outfile.
	 * 
	 * @param p
	 * @param outFile
	 * @return true if and only if the conversion was successful and the outFile
	 *         has been written.
	 */
	public boolean translate(Pathway p, String outFile);

	/**
	 * Translate a KEGG Pathway from the KGML formatted infile and write it in
	 * the new format to outfile.
	 * 
	 * @param infile
	 *            - input file in KGML format.
	 * @param outfile
	 * @throws Exception
	 */
	public void translate(String infile, String outfile) throws Exception;

	/**
	 * Tells you, if the last outFile that has been written was already there
	 * and has been overwritten.
	 * 
	 * @return true or false.
	 */
	public boolean isLastFileWasOverwritten();

}
