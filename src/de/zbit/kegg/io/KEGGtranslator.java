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
  public final static String appName = "KEGGtranslator";
  
  /**
   * Filename of the KEGG cache file (implemented just
   * like the browser cache). Must be loded upon start
   * and saved upon exit.
   */
  public static String cacheFileName = "keggdb.dat";
  
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
