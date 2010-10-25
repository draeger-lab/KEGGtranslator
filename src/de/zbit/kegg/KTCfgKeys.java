/**
 * 
 */
package de.zbit.kegg;

import java.io.File;

import de.zbit.util.Option;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-10-25
 */
public interface KTCfgKeys {

	/**
	 * The default directory to open files.
	 */
	public static final Option OPEN_DIR = new Option("OPEN_DIR", File.class,
			"The default directory to open files.");

	/**
	 * The default directory where to save generated files.
	 */
	public static final Option SAVE_DIR = new Option("SAVE_DIR", File.class,
			"The default directory where to save generated files.");

}
