/**
 *
 * @author wrzodek
 */
package de.zbit.kegg.io;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.zbit.kegg.KeggInfoManagement;
import de.zbit.kegg.KeggTools;
import de.zbit.kegg.parser.KeggParser;
import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.EntryType;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.util.AbstractProgressBar;
import de.zbit.util.ProgressBar;
import de.zbit.util.SBPreferences;


/**
 * Abstract superclass for Kegg translators. All non-ouput-format specific
 * stuff should be implemented into this class.
 * 
 * @author wrzodek
 */
public abstract class AbstractKEGGtranslator<OutputFormat> implements KEGGtranslator {

  /**
   * Retrieve annotations from Kegg or use purely information available in the
   * document.
   */
  private boolean retrieveKeggAnnots = true;
  
  /**
   * Remove single, not linked nodes/species
   * Defauls: Graphical representation: true, functional: false.
   */
  private boolean removeOrphans = false;

  /**
   * If false, all relations in the document will be skipped. Just like most
   * of the other very-basic converters.
   * NOTE: Only makes sense in KEGG2SBML (or non-graphic-based-converters).
   * Kegg2yGraph by default only considers realtions.
   * 
   * REMARK: This is optiona is currently IGNORED!
   */
  protected boolean considerRelations = true;
  
  /**
   * If true, all nodes in white color (except for small molecules/ compounds)
   * will be removed from the graph. Kegg colors all nodes, which do NOT
   * occur in the current species in white. Removing these nodes is HEAVILY
   * recommended if you want to use the SBML document for simulations.
   * 
   * Set this node to false if you convert generic pathways (not species
   * specific), since they ONLY contain white nodes.
   * 
   * Defauls: Graphical representation: false, functional: true.
   */
  private boolean removeWhiteNodes = true;
  
  /**
   * If true, missing reactants and enzymes for reactions will be retrieved
   * from the KEGG-DB and added to the result file.
   * REQUIRES: {@link #retrieveKeggAnnots}
   */
  protected boolean autocompleteReactions=true;
  
  /**
   * If set to true, all names will be shortened to one synonym.
   * Else: all synonyms will be shown e.g. only "DLAT" instead of
   * "DLAT, DLTA, PDC-E2, PDCE2"
   * XXX: Implementing classes must implement this functionality!
   * You may use the {@link #shortenName(String)} function for that.
   */
  protected boolean showShortNames = true;
  
  /**
   * This manager uses a cache and retrieved informations from the KeggDB. By
   * using the cache, it is very fast in retrieving informations.
   */
  protected KeggInfoManagement manager;
  
  /**
   * A flag, if the last sbml file that has been written by this class was
   * overwritten. This variable is used by the BatchConverter.
   */
  protected boolean lastFileWasOverwritten = false;
  
  /**
   * ProgressBar for Kegg2xConversion
   */
  protected AbstractProgressBar progress=null;
  
  
  private SBPreferences prefs;
  
  private void loadPreferences() {
    /*try {
      this.prefs = SBPreferences.getPreferencesFor(TranslatorOptions.class, 
          TranslatorOptions.CONFIG_FILE_LOCATION);
    } catch (Exception e) {
      // TODO: Defaults XML-Datei kaputt: Broken Jar File.
      e.printStackTrace();
    }
    
    removeOrphans=prefs.getBoolean(TranslatorOptions.REMOVE_ORPHANS);*/
  }
  
  /*===========================
   * CONSTRUCTORS
   * ===========================*/
  
  /**
   * @param manager2
   */
  public AbstractKEGGtranslator(KeggInfoManagement manager) {
    if (manager==null) manager = new KeggInfoManagement();
    this.manager = manager;
    
    loadPreferences();
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
   * See {@link #showShortNames}
   * @return
   */
  public boolean isShowShortNames() {
    return showShortNames;
  }
  
  /**
   * See {@link #showShortNames}
   * @param showShortNames
   */
  public void setShowShortNames(boolean showShortNames) {
    this.showShortNames = showShortNames;
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
   * Set a progressBar that should be used to display the
   * status of the conversion.
   * @param progressBarSwing
   */
  public void setProgressBar(AbstractProgressBar progressBar) {
    this.progress = progressBar;
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
    // XXX: Disabled, because writing after every conversion is very time-consuming.
    // Should be considered by calling classes when to write the cache.
    //if (getKeggInfoManager().hasChanged()) {
      //KeggInfoManagement.saveToFilesystem(KEGGtranslator.cacheFileName, getKeggInfoManager());
    //}
    
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
			List<Pathway> l = KeggParser.parse(f.getAbsolutePath());
			if (l.size() > 0) {
				Pathway p = l.get(0);
				OutputFormat doc = translate(p);
				return doc;
			}
		}
		throw new IOException(String.format("Cannot translate input file %s.", f
				.getAbsolutePath()));
	}
  
  /**
   * Initializes the given, or a new progressBar with the number of
   * entries. Optionally, the number of relations or reactions can
   * be added.
   * @param p - The KEGG Pathway to translate
   * @param addRelations - if true, also adds the number of relations
   * to the number of total calls.
   * @param addReactions - if true, also adds the number of reactions
   * to the number of total calls. 
   */
  protected void initProgressBar(Pathway p, boolean addRelations, boolean addReactions) {
    // Initialize a progress bar.
    int totalCalls = p.getEntries().size(); // +p.getRelations().size(); // Relations are very fast.
    if (addRelations) totalCalls+=p.getRelations().size();
    if (addReactions) totalCalls+=p.getReactions().size();
    // if (!retrieveKeggAnnots) aufrufeGesamt+=p.getRelations().size();
    if (progress==null) {
      progress = new ProgressBar(totalCalls + 1);
    } else {
      progress.reset();
      progress.setNumberOfTotalCalls(totalCalls + 1);
    }
    progress.DisplayBar();
  }
  
  /**
   * Returns true if and only if the given entry refers to a group node.
   * @param e
   * @return
   */
  public static boolean isGroupNode(Entry e) {
    EntryType t = e.getType();
    return (t.equals(EntryType.group) || e.getName().toLowerCase().trim().startsWith("group:") || e.hasComponents());
  }
  
  /**
   * Shorten a given Entry full-name.
   * Convert e.g. "PCK1, MGC22652, PEPCK-C, PEPCK1, PEPCKC..."
   * to "PCK1".
   * @param name
   * @return short name.
   */
  protected static String shortenName(String name) {
    if (name.contains(",")) {
      return name.substring(0, name.indexOf(",")-1);
    }
    return name;
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
