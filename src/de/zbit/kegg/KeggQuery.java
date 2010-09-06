package de.zbit.kegg;

import java.io.Serializable;

/**
 * This class is used by KeggFunctionManagement. It determines which function
 * should be executed and what are the parameters for this function.
 * 
 * @author wrzodek
 */
public class KeggQuery implements Comparable<KeggQuery>, Serializable {
	private static final long serialVersionUID = -2970366298298913439L;

	/**
	 * Input: Organism id (e.g. "hsa")
	 */
	public final static int getPathways = 0; // returns: Definition[] (alt:
	// ArrayList<String>)
	/**
	 * Input: Pathway id (e.g. "path:hsa04010")
	 */
	public final static int getGenesByPathway = 1; // returns: String[]
	/**
	 * Input: KG-Gene-ids, separated by space (e.g. "hsa:123 hsa:142")
	 */
	public final static int getIdentifier = 2; // returns: String (each entry
	// separated by new line)

	/**
	 * 
	 */
	private int jobToDo; // Required
	/**
	 * 
	 */
	private String query; // Required

	/**
	 * 
	 * @param jobToDo
	 * @param query
	 */
	public KeggQuery(int jobToDo, String query) {
		this.jobToDo = jobToDo;
		this.query = query;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public KeggQuery clone() {
		KeggQuery clone = new KeggQuery(this.jobToDo, new String(query));
		return clone;
	}

	/**
	 * 
	 */
	public int compareTo(KeggQuery o) {
		if (jobToDo < o.getJobToDo())
			return -1;
		else if (jobToDo > o.getJobToDo())
			return 1;
		else { // Same job to do
			return query.compareTo(o.getQuery());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		boolean equal = super.equals(o);
		if (o instanceof KeggQuery) {
			KeggQuery e = (KeggQuery) o;
			equal &= e.jobToDo == this.jobToDo && this.query.equals(e.query);
		}
		return equal;
	}

	/**
	 * 
	 * @return
	 */
	public int getJobToDo() {
		return jobToDo;
	}

	/**
	 * 
	 * @return
	 */
	public String getQuery() {
		return query;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int hc = (int) (jobToDo + query.hashCode());
		return (hc);
	}

	/**
	 * 
	 * @param jobToDo
	 */
	public void setJobToDo(int jobToDo) {
		this.jobToDo = jobToDo;
	}

	/**
	 * 
	 * @param query
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Job:" + jobToDo + " Query:" + query;
	}

}
