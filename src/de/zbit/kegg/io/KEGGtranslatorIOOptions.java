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

import java.io.File;

import de.zbit.kegg.gui.FileFilterKGML;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;
import de.zbit.util.prefs.Range;

/**
 * Command-line options, not for graphical user interface.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2011-01-07
 */
public interface KEGGtranslatorIOOptions extends KeyProvider {

	/**
	 * Possible output file formats.
	 * 
	 * @author Andreas Dr&auml;ger
	 * @date 2011-01-07
	 */
	public static enum Format {
		/**
		 * 
		 */
		SBML,
		/**
		 * 
		 */
		LaTeX,
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
		YGF;
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
			new Range<File>(File.class, new FileFilterKGML()), (short) 2, "-i",
			new File(System.getProperty("user.dir")));

	/**
	 * Path and name, where the translated file should be put.
	 */
	public static final Option<File> OUTPUT = new Option<File>("OUTPUT",
			File.class,
			"Path and name, where the translated file should be put.",
			(short) 2, "-o", new File(System.getProperty("user.dir")));

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
