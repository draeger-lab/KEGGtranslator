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

/**
 * This class replaces <ul>
 * <li>yFiles with {@link #KEGGtranslator}.appName</li>
 * <li>yFiles version number with {@link #KEGGtranslator}.VERSION_NUMBER</li>
 * </ul>
 * such that no notes of yFiles are being written to the file.
 * @author Clemens Wrzodek
 * @since 1.0
 * @version $Rev$
 */
public class YFilesWriter extends OutputStream implements Closeable {
	private Map<String, String> toReplace;
	
	private OutputStream realOut;
	
	private StringBuffer current;
	
	public YFilesWriter(OutputStream out) {
		toReplace = new HashMap<String, String>();
		// IMPORTANT NOTE: this MUST BE case sensitive, because
		// Replacing occurences of yfiles will create incompatible
		// files!
		toReplace.put("yFiles", KEGGtranslator.APPLICATION_NAME);
		toReplace.put(y.util.YVersion.currentVersionString(), KEGGtranslator.VERSION_NUMBER);
		
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
