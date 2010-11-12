package de.zbit.gui.cfg;

import java.io.IOException;
import java.util.InvalidPropertiesFormatException;

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
