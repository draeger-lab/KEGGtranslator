package de.zbit.kegg.gui;

import java.io.BufferedReader;
import java.io.File;

import javax.swing.filechooser.FileFilter;

import de.zbit.io.OpenFile;

/**
 * An implementation of {@link java.io.FileFilter} and extension of
 * {@link FileFilter} that recognizes KGML files. It also accepts directories.
 * 
 * @author Andreas Dr&auml;ger
 * 
 */
public class FileFilterKGML extends FileFilter implements java.io.FileFilter {

	/**
	 * The maximal number of lines to check for characteristic identifier in
	 * KEGG files. If the first {@link #MAX_LINES_TO_PARSE} do not contain the
	 * DOCTYPE entry for KEGG files including a link that start with
	 * "http://www.genome.jp/kegg/xml/KGML", the file cannot be recognized as a
	 * valid KGML file.
	 */
	private static final int MAX_LINES_TO_PARSE = 20;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
	 */
	public boolean accept(File f) {
		if (f.isDirectory()) {
			return true;
		}
		if (f.getName().toUpperCase().endsWith(".XML")) {
			try {
				BufferedReader br = OpenFile.openFile(f.getAbsolutePath());
				String line;
				for (int i = 0; br.ready() && (i < MAX_LINES_TO_PARSE); i++) {
					line = br.readLine();
					if (line.toUpperCase().startsWith("<!DOCTYPE")
							&& line
									.contains("http://www.genome.jp/kegg/xml/KGML")) {
						return true;
					}
				}
			} catch (Throwable e) {
				return false;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.filechooser.FileFilter#getDescription()
	 */
	public String getDescription() {
		return "KGML files (*.xml)";
	}

}
