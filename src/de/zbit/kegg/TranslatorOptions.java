/**
 *
 * @author wrzodek
 */
package de.zbit.kegg;

import java.io.File;

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

  /*
   * Most important options: input, output and file format.
   */
  
  public static final Option INPUT = new Option("INPUT",File.class,
      "Path and name of the source, KGML formatted, XML-file.", (short) 2, "-i");  

  public static final Option OUTPUT = new Option("OUTPUT",File.class,
      "Path and name, where the translated file should be put.", (short) 2, "-o");

  public static final Option FORMAT = new Option("FORMAT",String.class,
      "Target file format for the translation.","{SBML,LaTeX,GraphML,GML,JPG,GIF,TGF,YGF}" ,(short) 2, "-f");
  
  /*
   * Generic translation options
   */
  
  public static final Option REMOVE_ORPHANS = new Option("REMOVE_ORPHANS",Boolean.class,
      "If true, remove all nodes that have no edges, before translating the pathway.", (short) 2, "-ro");

  public static final Option SHORT_NAMES = new Option("SHORT_NAMES",Boolean.class,
      "If true, shows only short names of all KEGG entries.");
  
  public static final Option REMOVE_WHITE_GENE_NODES = new Option("REMOVE_WHITE_GENE_NODES",Boolean.class,
      "If true, removes all gene-nodes in the KEGG document, which are white.");

  public static final Option AUTOCOMPLETE_REACTIONS = new Option("AUTOCOMPLETE_REACTIONS",Boolean.class,
      "If true, automatically looks for missing reactants and enzymes of reactions and adds them to the document.", (short) 2, "-ar");
  
  // =! retrieveKeggAnnots
  public static final Option OFFLINE_MODE = new Option("OFFLINE_MODE",Boolean.class,
      "If true, no additional information will be retrieved from the KEGG-Server.");
  
  /*
   * Graphical, yFiles based translations
   */
  
  public static final Option MERGE_NODES_WITH_SAME_EDGES = new Option("MERGE_NODES_WITH_SAME_EDGES",Boolean.class,
      "If true, merges all nodes that have exactly the same relations (sources, targets and types).", (short) 2, "--merge");
  
  /*
   * Funcional, SBML based translations
   */
  
  public static final Option CELLDESIGNER_ANNOTATIONS = new Option("CELLDESIGNER_ANNOTATIONS",Boolean.class,
      "If true, adds celldesigner annotations to the SBML-XML document.", (short) 2, "-cd");
  
}
