/**
 * 
 */
package de.zbit.kegg.io;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * @author draeger
 * 
 */
public class FileFilterKGML extends FileFilter implements java.io.FileFilter {

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
	 */
	public boolean accept(File f) {
		return f.isDirectory() || f.getName().toUpperCase().endsWith(".XML");
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
