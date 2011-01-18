/*
 * Copyright (c) 2011 Center for Bioinformatics of the University of Tuebingen.
 * 
 * This file is part of KEGGtranslator, a program to convert KGML files from the
 * KEGG database into various other formats, e.g., SBML, GraphML, and many more.
 * Please visit <http://www.ra.cs.uni-tuebingen.de/software/KEGGtranslator> to
 * obtain the latest version of KEGGtranslator.
 * 
 * KEGGtranslator is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * KEGGtranslator is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with KEGGtranslator. If not, see
 * <http://www.gnu.org/licenses/lgpl.html>.
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
 * @author wrzodek
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
