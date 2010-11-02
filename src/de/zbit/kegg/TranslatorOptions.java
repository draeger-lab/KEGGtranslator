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

  /*
   * Most important options: input, output and file format.
   */
  
  public static final Option<File> INPUT = new Option<File>("INPUT",File.class,
      "Path and name of the source, KGML formatted, XML-file.", (short) 2, "-i", new File(System.getProperty("user.dir")));

  public static final Option<File> OUTPUT = new Option<File>("OUTPUT",File.class,
      "Path and name, where the translated file should be put.", (short) 2, "-o", new File(System.getProperty("user.dir")));

  public static final Option<String> FORMAT = new Option<String>("FORMAT",String.class,
      "Target file format for the translation.","{SBML,LaTeX,GraphML,GML,JPG,GIF,TGF,YGF}" ,(short) 2, "-f", "SBML");
  
  /*
   * Generic translation options
   */
  
  public static final Option<Boolean> REMOVE_ORPHANS = new Option<Boolean>("REMOVE_ORPHANS",Boolean.class,
      "If true, remove all nodes that have no edges, before translating the pathway.", (short) 2, "-ro", false);

  public static final Option<Boolean> SHORT_NAMES = new Option<Boolean>("SHORT_NAMES",Boolean.class,
      "If true, shows only short names of all KEGG entries.", true);
  
  public static final Option<Boolean> REMOVE_WHITE_GENE_NODES = new Option<Boolean>("REMOVE_WHITE_GENE_NODES",Boolean.class,
      "If true, removes all gene-nodes in the KEGG document, which are white.", true);

  public static final Option<Boolean> AUTOCOMPLETE_REACTIONS = new Option<Boolean>("AUTOCOMPLETE_REACTIONS",Boolean.class,
      "If true, automatically looks for missing reactants and enzymes of reactions and adds them to the document.", (short) 2, "-ar", true);
  
  // =! retrieveKeggAnnots
  public static final Option<Boolean> OFFLINE_MODE = new Option<Boolean>("OFFLINE_MODE",Boolean.class,
      "If true, no additional information will be retrieved from the KEGG-Server.", false);
  
  /*
   * Graphical, yFiles based translations
   */
  
  public static final Option<Boolean> MERGE_NODES_WITH_SAME_EDGES = new Option<Boolean>("MERGE_NODES_WITH_SAME_EDGES",Boolean.class,
      "If true, merges all nodes that have exactly the same relations (sources, targets and types).", (short) 2, "--merge", false);
  
  /*
   * Funcional, SBML based translations
   */
  
  public static final Option<Boolean> CELLDESIGNER_ANNOTATIONS = new Option<Boolean>("CELLDESIGNER_ANNOTATIONS",Boolean.class,
      "If true, adds celldesigner annotations to the SBML-XML document.", (short) 2, "-cd", false);
  
}
