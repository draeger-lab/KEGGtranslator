/**
 *
 */
package de.zbit.kegg.io;

import de.zbit.kegg.parser.pathway.Pathway;

/**
 * @author wrzodek
 * 
 */
public interface KeggConverter {

	/**
	 * 
	 * @param p
	 * @param outFile
	 * @throws Exception
	 */
	public boolean Convert(Pathway p, String outFile) throws Exception;

	/**
	 * 
	 * @param infile
	 * @param outfile
	 * @throws Exception
	 */
	public void Convert(String infile, String outfile) throws Exception;

	/**
	 * Gibt an, ob das letzte geschriebene outFile bereits vorhanden war und
	 * deshalb ueberschrieben wurde.
	 * 
	 * @return
	 */
	public boolean lastFileWasOverwritten();

}
