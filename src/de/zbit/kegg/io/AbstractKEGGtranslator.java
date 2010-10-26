/**
 *
 * @author wrzodek
 */
package de.zbit.kegg.io;

import java.io.File;
import java.io.IOException;

import de.zbit.kegg.KeggInfoManagement;
import de.zbit.kegg.KeggTools;
import de.zbit.kegg.parser.KeggParser;
import de.zbit.kegg.parser.pathway.Pathway;


/**
 * @author wrzodek
 *
 */
public abstract class AbstractKEGGtranslator<OutputFormat> implements KEGGtranslator {

  /**
   * Retrieve annotations from Kegg or use purely information available in the
   * document.
   */
  private boolean retrieveKeggAnnots = true;
  
  /**
   * Remove single, not linked nodes/species
   */
  private boolean removeOrphans = false; // TODO: Set to true, by default.

  /**
   * If false, all relations in the document will be skipped. Just like most
   * of the other very-basic converters.
   */
  private boolean considerRelations = true;
  
  
  /**
   * If true, all nodes in white color (except for small molecules/ compounds)
   * will be removed from the graph. Kegg colors all nodes, which do NOT
   * occur in the current species in white. Removing these nodes is HEAVILY
   * recommended if you want to use the SBML document for simulations.
   * 
   * Set this node to false if you convert generic pathways (not species
   * specific), since they ONLY contain white nodes.
   */
  private boolean removeWhiteNodes = false; // TODO: Set to true, by default.
  
  
  /**
   * If true, missing reactants and enzymes for reactions will be retrieved
   * from the KEGG-DB and added to the result file.
   * REQUIRES: {@link #retrieveKeggAnnots}
   */
  private boolean autocompleteReactions=true;
  

  
  /**
   * This manager uses a cache and retrieved informations from the KeggDB. By
   * using the cache, it is very fast in retrieving informations.
   */
  protected KeggInfoManagement manager;
  
  /**
   * A flag, if the last sbml file that has been written by this class was
   * overwritten. This variable is used by the BatchConverter.
   */
  private boolean lastFileWasOverwritten = false;
  
  /*===========================
   * CONSTRUCTORS
   * ===========================*/
  
  /**
   * @param manager2
   */
  public AbstractKEGGtranslator(KeggInfoManagement manager) {
    this.manager = manager;
  }

  
  /*===========================
   * Getters and Setters
   * ===========================*/

  /**
   * See {@link #retrieveKeggAnnots}
   * @return
   */
  public boolean isRetrieveKeggAnnots() {
    return retrieveKeggAnnots;
  }
  /**
   * @param retrieveKeggAnnots - see {@link #retrieveKeggAnnots}.
   */
  public void setRetrieveKeggAnnots(boolean retrieveKeggAnnots) {
    this.retrieveKeggAnnots = retrieveKeggAnnots;
  }
  
  /**
   * See {@link #removeOrphans}
   * @return
   */
  public boolean isRemoveOrphans() {
    return removeOrphans;
  }
  /** 
   * @param removeOrphans - see {@link #removeOrphans}.
   */
  public void setRemoveOrphans(boolean removeOrphans) {
    this.removeOrphans = removeOrphans;
  }
  /**
   * See {@link #considerRelations}
   * @return
   */
  public boolean isConsiderRelations() {
    return considerRelations;
  }
  /**
   * @param considerRelations - see {@link #considerRelations}.
   */
  public void setConsiderRelations(boolean considerRelations) {
    this.considerRelations = considerRelations;
  }
  /**
   * See {@link #removeWhiteNodes}
   * @return
   */
  public boolean isRemoveWhiteNodes() {
    return removeWhiteNodes;
  }
  /**
   * @param removeWhiteNodes - see {@link #removeWhiteNodes}.
   */
  public void setRemoveWhiteNodes(boolean removeWhiteNodes) {
    this.removeWhiteNodes = removeWhiteNodes;
  }
  /**
   * See {@link #manager}
   * @param manager
   */
  public void setKeggInfoManager(KeggInfoManagement manager) {
    this.manager = manager;
  }
  /**
   * @return - see {@link #manager}.
   */
  public KeggInfoManagement getKeggInfoManager() {
    return manager;
  }
  /**
   * {@inheritDoc}
   */
  public boolean isLastFileWasOverwritten() {
    return lastFileWasOverwritten;
  } 
  
  /**
   * Preprocesses the given pathway:
   * - precaches all Ids,
   * - removes orphans, white nodes, etc. if the user wishes to do so
   * @param p
   */
  private void preProcessPathway(Pathway p) {
    if (!retrieveKeggAnnots) {
      KeggInfoManagement.offlineMode = true;
    } else {
      KeggInfoManagement.offlineMode = false;
      
      // Prefetch kegg information (enormas speed improvement).
      System.out.print("Fetching information from KEGG online resources... ");
      KeggTools.preFetchInformation(p,manager,autocompleteReactions);
      System.out.println("done.");
      
      // Auto-complete the reaction by adding all substrates, products and enzymes.
      if (autocompleteReactions) {
        KeggTools.autocompleteReactions(p, manager);
      }
    }
    
    // Preprocess pathway
    if (removeOrphans) {
      KeggTools.removeOrphans(p, considerRelations);
    }
    
    // Skip it, if it's white
    if (removeWhiteNodes) {
      KeggTools.removeWhiteNodes(p);
    }
  }

  /**
   * {@inheritDoc}
   */
  public boolean translate(Pathway p, String outFile) {
    preProcessPathway(p);
    OutputFormat doc = translate(p);
    
    if (new File(outFile).exists()) {
      // Remember that file was already there.
      lastFileWasOverwritten = true;
    }
    
    return writeToFile(doc, outFile);
  }
  

  /**
   * Translates the given pathway to the target document.
   * @param p - the Kegg Pathway.
   * @return OutputFormat
   */
  public OutputFormat translate(Pathway p) {
    // REMARK: This class is and must be called by all other translate functions.
    preProcessPathway(p);
    
    OutputFormat doc = translateWithoutPreprocessing(p);
    
    // Remember already queried objects
    if (getKeggInfoManager().hasChanged()) {
      KeggInfoManagement.saveToFilesystem(KEGGtranslator.cacheFileName, getKeggInfoManager());
    }
    
    return doc;
  }


  /**
   * {@inheritDoc}
   */
  public void translate(String infile, String outfile) throws Exception {
    // System.out.println("Reading kegg pathway...");
    Pathway p = KeggParser.parse(infile).get(0);
    translate(p, outfile);
  }
  
  /**
   * This method converts a given KGML file into the
   * specified OutputFormat.
   * 
   * @param f - the input file.
   * @return the generated document in OutputFormat.
   * @throws IOException - if the input file is not readable.
   */
  public OutputFormat translate(File f) throws IOException {
    if (f.exists() && f.isFile() && f.canRead()) {
      Pathway p = KeggParser.parse(f.getAbsolutePath()).get(0);
      OutputFormat doc = translate(p);
      
      return doc;
    }
    throw new IOException("Cannot read input file " + f.getAbsolutePath());
  }
  

  /**
   * Write the translated document to the given file.
   * @param doc - the translated document
   * @param outFile - the file to write
   * @return true if and only if everything went fine.
   */
  public abstract boolean writeToFile(OutputFormat doc, String outFile);

  /**
   * Translate the pathway to the new format - assumes that all preprocessing
   * (precaching of ids, autocomplete reactions, remove orphans, etc.) has
   * already been performed.
   * 
   * @param p - Pathway to translate
   * @return Translated pathway
   */
  protected abstract OutputFormat translateWithoutPreprocessing(Pathway p);
  
}
