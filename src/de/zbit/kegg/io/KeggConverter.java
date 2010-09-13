package de.zbit.kegg.io;

import de.zbit.kegg.parser.pathway.Pathway;

/**
 * Generic interface for KeggConverters.
 * @author wrzodek 
 */
public interface KeggConverter {

  /**
   * Convert a given Kegg Pathway and write it
   * in the new format to outfile. 
   * @param p
   * @param outFile
   * @return true if and only if the conversion was
   * successful and the outFile has been written.
   */
	public boolean convert(Pathway p, String outFile);

	/**
	 * Convert a Kegg Pathway from the KGML formatted infile and write it
	 * in the new format to outfile.
	 * @param infile - input file in KGML format.
	 * @param outfile
	 * @throws Exception
	 */
	public void convert(String infile, String outfile) throws Exception;

	/**
	 * Tells you, if the last outFile that has been written was already there
	 * and has been overwritten. 
	 * 
	 * @return true or false.
	 */
	public boolean isLastFileWasOverwritten();

}
