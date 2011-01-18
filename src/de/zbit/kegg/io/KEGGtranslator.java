/*
 * Copyright (c) 2011 Center for Bioinformatics of the University of Tuebingen.
 * 
 * This file is part of KEGGtranslator, a program to convert KGML files from the
 * KEGG database into various other formats, e.g., SBML, GraphML, and many more.
 * Please visit <http://www.ra.cs.uni-tuebingen.de/software/KEGGtranslator> to
 * obtain the latest version of KEGGtranslator.
 * 
 * KEGGtranslator is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * KEGGtranslator is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with KEGGtranslator. If not, see
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package de.zbit.kegg.io;

import de.zbit.kegg.parser.pathway.Pathway;

/**
 * Generic interface for {@link KEGGtranslator}s.
 * 
 * @author Clemens Wrzodek
 */
public interface KEGGtranslator {
  
  /**
   * The name of the appplication.
   */
  public final static String APPLICATION_NAME = "KEGGtranslator";
  
  /**
   * Version number of this translator
   */
  public static final String VERSION_NUMBER = "1.0.0";
  
  /**
   * Filename of the KEGG cache file (implemented just
   * like the browser cache). Must be loded upon start
   * and saved upon exit.
   */
  public static String cacheFileName = "keggdb.dat";
  /**
   * Filename of the KEGG function cache file (implemented just
   * like the browser cache). Must be loded upon start
   * and saved upon exit.
   */  
  public static String cacheFunctionFileName = "keggfc.dat";
  
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
