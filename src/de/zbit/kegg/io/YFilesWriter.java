/**
 *
 * @author wrzodek
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
		toReplace.put("yFiles", KEGGtranslator.appName);
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
