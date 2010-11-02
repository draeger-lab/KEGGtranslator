/**
 * @author wrzodek
 */
package de.zbit.gui.cfg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.List;

import de.zbit.kegg.TranslatorOptions;
import de.zbit.util.SBPreferences;

/**
 * @author wrzodek
 */
public class GeneralOptionPanel extends PreferencesPanel {

	/**
   * 
   */
	private static final long serialVersionUID = 6273038303582557299L;

	/**
	 * @throws InvalidPropertiesFormatException
	 * @throws IOException
	 */
	public GeneralOptionPanel() throws IOException {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#accepts(java.lang.Object)
	 */
	@Override
	public boolean accepts(Object key) {
		boolean accept = false;
		accept |= TranslatorOptions.REMOVE_ORPHANS.equals(key.toString());
		// ....
		return accept;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		return "General Options";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#init()
	 */
	@Override
	public void init() {
		autoBuildPanel();
		getJComponentForOption(TranslatorOptions.REMOVE_ORPHANS);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#loadPreferences()
	 */
	@Override
	protected SBPreferences loadPreferences() throws IOException {
		return SBPreferences.getPreferencesFor(TranslatorOptions.class,
				TranslatorOptions.CONFIG_FILE_LOCATION);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.PreferencesPanel#checkPreferences()
	 */
	@Override
	public List<String> checkPreferences() {
		// TODO Auto-generated method stub
		return new ArrayList<String>(0);
	}

}
