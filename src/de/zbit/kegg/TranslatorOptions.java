/**
 *
 * @author wrzodek
 */
package de.zbit.kegg;

import de.zbit.util.Option;

/**
 * @author wrzodek
 * @author Andreas Dr&auml;ger
 */
public abstract interface TranslatorOptions {
  
  /**
   * The path to the associated configuration file, which contains one default
   * value for each option defined in this interface.
   */
  public static final String CONFIG_FILE_LOCATION = "cfg/KEGGtranslator.xml";

  public static final Option REMOVE_ORPHANS = new Option("REMOVE_ORPHANS",Boolean.class,
      "If true, remove all nodes that have no edges, before translating the pathway.", (short) 2, "-ro");
  
  // TODO: Set all options.
  
}
