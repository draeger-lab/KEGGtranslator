/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of KEGGtranslator, a program to convert KGML files
 * from the KEGG database into various other formats, e.g., SBML, GML,
 * GraphML, and many more. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator> to
 * obtain the latest version of KEGGtranslator.
 *
 * Copyright (C) 2011 by the University of Tuebingen, Germany.
 *
 * KEGGtranslator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * 
 * There are special restrictions for this file. Each procedure that
 * is using the yFiles API must stick to their license restrictions.
 * Please see the following link for more information
 * <http://www.yworks.com/en/products_yfiles_sla.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.kegg.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import de.zbit.kegg.Translator;

/**
 * This class creates an {@link OutputStream} that can be used with
 * the yFiles Java library. It ensures that KEGGtranslator name 
 * ({@link KEGGtranslator#APPLICATION_NAME}) and version
 * number ({@link KEGGtranslator#VERSION_NUMBER}) is written to each
 * file, which allows for easier debugging of written graph files.
 * 
 * <p><i>Note:<br/>
 * Due to yFiles license requirements, we have to obfuscate this class
 * in the JAR release of this application. Thus, this class
 * can not be found by using the class name.<br/> If you can provide us
 * with a proof of possessing a yFiles license yourself, we can send you
 * an unobfuscated release of KEGGtranslator.</i></p>
 * 
 * @author Clemens Wrzodek
 * @since 1.0
 * @version $Rev$
 */
public class YFilesWriter extends OutputStream implements Closeable {
	private Map<String, String> toReplace;
	
	private OutputStream realOut;
	
	private StringBuffer current;
	
	/**
	 * 
	 * @param out
	 */
	public YFilesWriter(OutputStream out) {
		toReplace = new HashMap<String, String>();
		// IMPORTANT NOTE: this MUST BE case sensitive, because
		// Replacing occurences of yfiles will create incompatible
		// files!
		/*
     * Replaces <ul>
     * <li>yFiles with {@link KEGGtranslator#APPLICATION_NAME}</li>
     * <li>yFiles version number with {@link KEGGtranslator#VERSION_NUMBER}</li>
     * </ul>
     * such that no notes of yFiles are being written to the file.
		 */
		toReplace.put("yFiles", Translator.APPLICATION_NAME);
		toReplace.put("ySVG", Translator.APPLICATION_NAME);
		toReplace.put(y.util.YVersion.currentVersionString(), Translator.VERSION_NUMBER);
		
		realOut = out;
		
		current = new StringBuffer();
	}

	
	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public void write(int b) throws IOException {
		current.append((char)b);
		
		if (b=='\n') {
			internalFlush();
		}
	}


	private void internalFlush() throws IOException {
		// Replace everything
		String toWrite = current.toString();
		for (String key : toReplace.keySet()) {
			toWrite = toWrite.replace(key, toReplace.get(key));
		}
		
		// Write things and clear the current buffer.
		realOut.write(toWrite.getBytes());
		current = new StringBuffer();
	}
	
	public void close() throws IOException {
		internalFlush();
		realOut.close();
	}
	
}
