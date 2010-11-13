package de.zbit.gui.prefs;

import java.io.IOException;
import java.util.InvalidPropertiesFormatException;

import de.zbit.gui.prefs.PreferencesPanelForKeyProvider;
import de.zbit.kegg.TranslatorOptions;

/**
 * @author wrzodek
 */
public class GeneralOptionPanel extends PreferencesPanelForKeyProvider {
	private static final long serialVersionUID = 6273038303582557299L;

	/**
	 * @throws InvalidPropertiesFormatException
	 * @throws IOException
	 */
	public GeneralOptionPanel() throws IOException {
		super(TranslatorOptions.class);
	}
}
