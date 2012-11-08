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

import java.io.File;

import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;
import de.zbit.util.prefs.Range;

/**
 * Command-line options, not for graphical user interface.
 * 
 * @author Andreas Dr&auml;ger
 * @author Clemens Wrzodek
 * @date 2011-01-07
 * @since 1.0
 * @version $Rev$
 */
public interface KEGGtranslatorIOOptions extends KeyProvider {

  /**
   * Possible output file formats.
   * 
   * @author Andreas Dr&auml;ger
   * @author Clemens Wrzodek
   * @date 2011-01-07
   */
  public enum Format {
    /**
     * Automatically adjusts the level as needed (e.g., extensions require L3).
     */
    SBML,
    /**
     * 
     */
    SBML_L2V4,
    /**
     * 
     */
    SBML_L3V1,
    /**
     * 
     */
    SBML_QUAL,
    /**
     * 
     */
    SBML_CORE_AND_QUAL,
    /**
     * 
     */
    SBGN,
    /**
     * 
     */
    BioPAX_level2,
    /**
     * 
     */
    BioPAX_level3,
    /**
     * Some Pathway exchange format used by Cytoscape. Base
     * is 2BioPAX and paxtools can then write SIF files. 
     */
    SIF,
    // Since the restructuring and moving large parts to sysbio, the 2LaTeX
    // part is not supported anymore.
//    /**
//     * 
//     */
//    LaTeX,
    /**
     * 
     */
    GraphML,
    /**
     * 
     */
    GML,
    /**
     * 
     */
    JPG,
    /**
     * 
     */
    GIF,
    /**
     * 
     */
    TGF,
    /**
     * 
     */
    YGF,
    /**
     * This required the corresponding ySVG extension from yFiles. It's free
     * but large and thus, by default not included. But the functionality is
     * fully included. Thus, if you want SVG, include the libraries and simply
     * uncomment the next item and the SVG part in
     * TODO: update Javadoc link 
     * {@link BatchKEGGtranslator#getTranslator(Format, KeggInfoManagement)}
     */
    //SVG
    ;
    
    public boolean isSBML() {
      return toString().contains("SBML");
    }
  }
  
	/*
	 * Most important options: input, output and file format.
	 */
	
	/**
	 * Path and name of the source, KGML formatted, XML-file.
	 */
	public static final Option<File> INPUT = new Option<File>("INPUT",
			File.class,
			"Path and name of the source, KGML formatted, XML-file.",
			new Range<File>(File.class, SBFileFilter.createKGMLFileFilter()), (short) 2, "-i" );
			//new File(System.getProperty("user.dir")));

	/**
	 * Path and name, where the translated file should be put.
	 */
	public static final Option<File> OUTPUT = new Option<File>("OUTPUT",
			File.class,
			"Path and name, where the translated file should be put.",
			(short) 2, "-o" );//, new File(System.getProperty("user.dir")));

	/**
	 * Target file format for the translation.
	 */
	public static final Option<Format> FORMAT = new Option<Format>("FORMAT",
			Format.class, "Target file format for the translation.",
			new Range<Format>(Format.class, Range.toRangeString(Format.class)),
			(short) 2, "-f", Format.SBML);

	/**
	 * Define the default input/ output files and the default output format.
	 */
	@SuppressWarnings("unchecked")
	public static final OptionGroup<Object> BASE_OPTIONS = new OptionGroup<Object>(
			"Base options",
			"Define the default input/ output files and the default output format.",
			INPUT, OUTPUT, FORMAT);

}
