/**
 * 
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
