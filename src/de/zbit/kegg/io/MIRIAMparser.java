/*
    SBML2LaTeX converts SBML files (http://sbml.org) into LaTeX files.
    Copyright (C) 2009 ZBIT, University of Tübingen, Andreas Dräger

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.zbit.kegg.io;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.xml.sax.SAXException;

/**
 * This class parses MIRIAM URIs given in a XML file and links them to the
 * actual internet resources.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2008-12-09
 * @since 1.3
 */
public class MIRIAMparser {

	/**
	 * An XML formatted MIRIAM resource
	 */
	private Document doc;

	/**
	 * 
	 */
	public MIRIAMparser() {
		doc = new Document();
	}

	/**
	 * 
	 * @param file
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws JDOMException
	 */
	public MIRIAMparser(File file) throws ParserConfigurationException,
			SAXException, IOException, JDOMException {
		doc = new SAXBuilder().build(file);
	}

	/**
	 * 
	 * @param path
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws JDOMException
	 */
	public MIRIAMparser(String path) throws ParserConfigurationException,
			SAXException, IOException, JDOMException {
		this(new File(path));
	}

	/**
	 * 
	 * @param uri
	 *            Some URI (urn, uri)
	 * @return An array of valid urls if these can be found from the XML file.
	 *         An empty array (length 0) otherwise.
	 */
	public String[] getLocations(String uri) {
		StringBuffer miriamURI = new StringBuffer();
		String id = determinIdentifier(uri, miriamURI);
		if (id.contains("%3A")) {
			int index = id.indexOf("%3A");
			id = id.substring(0, index) + ':' + id.substring(index + 3);
		}
		Element datatype = null;
		datatype = identifyDatatype(miriamURI.toString(), new StringBuffer());
		if (datatype == null) {
			String sub = uri.substring(0, uri.length() - id.length() - 1);
			miriamURI = new StringBuffer();
			String id1 = determinIdentifier(sub, miriamURI);
			id = id1 + ':' + id;
			datatype = identifyDatatype(miriamURI.toString(), new StringBuffer());
		}
		if (datatype != null) {
			int i = 0;
			Vector<String> locations = new Vector<String>();
			for (Iterator<?> iter = datatype.getDescendants(new ElementFilter(
					"resource")); iter.hasNext();) {
				Element resource = (Element) iter.next();
				Element entry = resource.getChild("dataEntry", resource
						.getNamespace());
				if (entry != null) {
					String location = entry.getValue().replace("$id", id);
					locations.add(location);
				}
			}
			String[] urls = new String[locations.size()];
			for (String location : locations)
				urls[i++] = location;
			return urls;
		}
		return new String[0];
	}

	/**
	 * 
	 * @return
	 */
	public Document getMIRIAMdocument() {
		return doc;
	}

	/**
	 * 
	 * @param uri
	 *            Some URI (urn, uri).
	 * @return An up-to-date MIRIAM urn string.
	 */
	public String getMiriamURI(String uri) {
		StringBuffer miriamURI = new StringBuffer();
		String id = determinIdentifier(uri, miriamURI);
		if (id.contains(":")) {
			id = id.substring(0, id.indexOf(':')) + "%3A"
					+ id.substring(id.indexOf(':') + 1);
		}
		if (id.length() > 0) {
			id = ":" + id;
		}
		StringBuffer urn = new StringBuffer();
		identifyDatatype(miriamURI.toString(), urn);
		urn.append(id);
		String urnString = urn.toString();
		urnString = urnString.length() > id.length() ? urnString : "unknown";
		return urnString;
	}

	/**
	 * 
	 * @param doc
	 */
	public void setMIRIAMdocument(Document doc) {
		this.doc = doc;
	}

	/**
	 * 
	 * @param file
	 * @throws JDOMException
	 * @throws IOException
	 */
	public void setMIRIAMfile(File file) throws JDOMException, IOException {
		doc = new SAXBuilder().build(file);
	}

	/**
	 * 
	 * @param path
	 * @throws JDOMException
	 * @throws IOException
	 */
	public void setMIRIAMfile(String path) throws JDOMException, IOException {
		doc = (new SAXBuilder()).build(new File(path));
	}

	/**
	 * 
	 * @param uri
	 * @param miriamURI
	 *            an empty String buffer for the result.
	 * @return
	 */
	private String determinIdentifier(String uri, StringBuffer miriamURI) {
		String id = "";
		if (uri.startsWith("urn:")) {
			int idx = uri.lastIndexOf(':');
			if (idx > 0) {
				char atIdx = uri.charAt(idx - 1);
				while ((idx > 0) && Character.isUpperCase(atIdx)) {
					atIdx = uri.charAt(--idx);
				}
			}
			id = uri.substring(idx);
			miriamURI.append(uri.substring(0, uri.length() - id.length()));
			id = id.substring(1);
		} else if (uri.startsWith("http://")) {
			id = uri.substring(Math.min(uri.lastIndexOf('/') + 1, uri
					.indexOf('#')));
			miriamURI.append(uri.substring(0, uri.length() - id.length()));
			if (id.startsWith("#"))
				id = id.substring(1);
		}
		return id;
	}

	/**
	 * 
	 * @param uri
	 *            an URI without an identifier
	 * @param urn
	 *            This is where the MIRIAM URN will be saved to.
	 * @return
	 */
	private Element identifyDatatype(String uri, StringBuffer urn) {
		boolean found = false;
		String urnString = "";
		for (Iterator<?> iter = doc.getDescendants(new ElementFilter("uris")); iter
				.hasNext();) {
			Element uris = (Element) iter.next();
			urnString = "";
			for (int i = 0; i < uris.getChildren().size(); i++) {
				Element child = (Element) uris.getChildren().get(i);
				if (child.getAttribute("type") != null) {
					if (child.getAttribute("type").getValue().equalsIgnoreCase(
							"urn")) {
						if (child.getAttributeValue("deprecated") != null) {
							if (!Boolean.parseBoolean(child
									.getAttributeValue("deprecated"))) {
								urnString = child.getValue();
							} else if (urnString.length() == 0) {
								urnString = child.getValue();
							}
						} else if (urnString.length() == 0)
							urnString = child.getValue();
					}
				}
				if (child.getValue().equalsIgnoreCase(uri)) {
					found = true;
				}
			}
			if (found && urnString.length() > 0) {
				urn.append(urnString);
				return uris.getParentElement();
			}
		}
		return null;
	}

}
