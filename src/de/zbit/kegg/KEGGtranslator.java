/**
 * 
 */
package de.zbit.kegg;

import de.zbit.util.SBPreferences;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-10-25
 */
public class KEGGtranslator {

	/**
	 * 
	 */
	private static SBPreferences preferences;

	/**
	 * @return the preferences
	 */
	public static SBPreferences getPreferences() {
		return preferences;
	}

	static {
		try {
			preferences = new SBPreferences(KTCfgKeys.class,
					"cfg/KEGGtranslator.xml");
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	/**
	 * 
	 */
	public KEGGtranslator() {
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		for (Object key : System.getProperties().keySet()) {
			System.out.printf("%s:\t%s\n", key, System.getProperty(key
					.toString()));
		}

		preferences.analyzeCommandLineArguments(
				"java KEGGtranslator [options]", args);
	}

}
