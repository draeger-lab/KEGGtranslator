/**
 *
 * @author wrzodek
 */
package de.zbit.gui.prefs;

import java.io.IOException;

import de.zbit.kegg.ext.TranslatorPanelOptions;

/**
 * Enable an option tab for the {@link TranslatorPanelOptionPanel}.
 * @author wrzodek
 */
public class TranslatorPanelOptionPanel extends PreferencesPanelForKeyProvider {
  private static final long serialVersionUID = 4932259635453073817L;

  /**
   * @throws IOException
   */
  public TranslatorPanelOptionPanel() throws IOException {
    super(TranslatorPanelOptions.class);
  }
  
}
