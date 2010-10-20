package de.zbit.kegg.gui;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-10-20
 */
public class GUITools extends de.zbit.gui.GUITools {

	/**
	 * 
	 * @param prefix
	 *            Prefix of the directory containing the icons used in the
	 *            program.
	 */
	public static void initIcons(String prefix) {
		UIManager.put("ICON_LATEX_TINY", loadIcon(prefix
				+ "SBML2LaTeX_vertical_tiny.png"));
		UIManager.put("ICON_LATEX_SMALL", loadIcon(prefix
				+ "SBML2LaTeX_vertical_small.png"));
	}

	/**
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static Icon loadIcon(String path) {
		Image img = loadImage(path);
		return img != null ? new ImageIcon(img) : null;
	}

	/**
	 * 
	 * @param path
	 * @return
	 */
	public static Image loadImage(String path) {
		try {
			String p = path.substring(path.indexOf("img"));
			URL url = GUITools.class.getResource(p);
			return url != null ? ImageIO.read(GUITools.class.getResource(path
					.substring(path.indexOf("img")))) : ImageIO.read(new File(
					path));
		} catch (IOException exc) {
			System.err.printf("Could not load image %s.\n", path);
			return null;
		}
	}

}
