package de.zbit.kegg.parser.pathway;

import java.util.ArrayList;

/**
 * Main Kegg document. Corresponding to the Kegg Pathway class
 * (see {@link http://www.genome.jp/kegg/xml/docs/})
 * 
 * @author wrzodek
 */
public class Pathway {
	/* see http://www.genome.jp/kegg/xml/docs/ */
	/**
	 * keggid.type the KEGGID of this pathway map
	 */
	String name = "";
	/**
	 * maporg.type ko/ec/[org prefix]
	 */
	String org = "";
	/**
	 * mapnumber.type the map number of this pathway map (5 digit integer)
	 */
	int number = 0;
	/**
	 * string.type the title of this pathway map
	 */
	String title = "";
	/**
	 * url.type the resource location of the image file of this pathway map
	 */
	String image = "";
	/**
	 * url.type the resource location of the information about this pathway map
	 */
	String link = "";
	/**
	 * 
	 */
	ArrayList<Entry> entries = new ArrayList<Entry>();
	/**
	 * 
	 */
	ArrayList<Reaction> reactions = new ArrayList<Reaction>();
	/**
	 * 
	 */
	ArrayList<Relation> relations = new ArrayList<Relation>();

	// Custom Variables, not in the KGML specification
	String comment = null;
	double version=0;
	
	/**
	 * 
	 */
	private Pathway() {
		super();
	}

	/**
	 * 
	 * @param name
	 * @param org
	 * @param number
	 */
	public Pathway(String name, String org, int number) {
		this();
		setName(name);
		setOrg(org);
		setNumber(number);
	}

	/**
	 * 
	 * @param name
	 * @param org
	 * @param number
	 * @param title
	 * @param image
	 * @param link
	 */
	public Pathway(String name, String org, int number, String title,
			String image, String link) {
		this(name, org, number);
		setTitle(title);
		setImage(image);
		setLink(link);
	}

	/**
	 * 
	 * @param e
	 */
	public void addEntry(Entry e) {
		entries.add(e);
	}

	/**
	 * 
	 * @param r
	 */
	public void addReaction(Reaction r) {
		reactions.add(r);
	}

	/**
	 * 
	 * @param r
	 */
	public void addRelation(Relation r) {
		relations.add(r);
	}

	/**
	 * 
	 * @return
	 */
	public ArrayList<Entry> getEntries() {
		return entries;
	}

	/**
	 * 
	 * @param id
	 * @return
	 */
	public Entry getEntryForId(int id) {
		for (int i = 0; i < entries.size(); i++)
			if (entries.get(i).getId() == id)
				return entries.get(i);
		return null;
	}

	/**
	 * 
	 * @param name
	 * @return
	 */
	public Entry getEntryForName(String name) {
		for (int i = 0; i < entries.size(); i++)
			if (entries.get(i).getName().equalsIgnoreCase(name))
				return entries.get(i);
		return null;
	}

	/**
	 * 
	 * @return
	 */
	public String getImage() {
		return image;
	}

	/**
	 * 
	 * @return
	 */
	public String getLink() {
		return link;
	}

	/**
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Remember: Pathway number is an 5-digit integer. So if size < 5 you need
	 * to make a prefix of 0's. You'd better use getNumberReal() instead.
	 */
	public int getNumber() {
		return number;
	}

	/**
	 * 
	 * @return
	 */
	public String getNumberReal() {
		String ret = Integer.toString(number);
		while (ret.length() < 5)
			ret = "0" + ret;
		return ret;
	}

	/**
	 * 
	 * @return
	 */
	public String getOrg() {
		return org;
	}

	/**
	 * 
	 * @return
	 */
	public ArrayList<Reaction> getReactions() {
		return reactions;
	}

	/**
	 * 
	 * @return
	 */
	public ArrayList<Relation> getRelations() {
		return relations;
	}

	/**
	 * 
	 * @return
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * 
	 * @param image
	 */
	public void setImage(String image) {
		this.image = image;
	}

	/**
	 * 
	 * @param link
	 */
	public void setLink(String link) {
		this.link = link;
	}

	/**
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 
	 * @param number
	 */
	public void setNumber(int number) {
		this.number = number;
	}

	/**
	 * 
	 * @param org
	 */
	public void setOrg(String org) {
		this.org = org;
	}

	/**
	 * 
	 * @param title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public double getVersion() {
    return version;
  }

  public void setVersion(double version) {
    this.version = version;
  }

	
}
