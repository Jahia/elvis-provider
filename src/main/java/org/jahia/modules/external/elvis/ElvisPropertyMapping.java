
package org.jahia.modules.external.elvis;

/**
 * Configuration class to map Elvis <-> JCR type attributes.
 * Any attribute may be mapped in list of modes. Depends on mode attribute may support reade, write and be used on create.
 *
 * @author Damien GAILLARD
 */
public class ElvisPropertyMapping {
    /**
     * Property will readable
     */
    public static final String MODE_READ = "r";
    /**
     * Property will updatable
     */
    public static final String MODE_WRITE = "w";
    /**
     * Property will use on creation
     */
    public static final String MODE_CREATE = "c";
    private String jcrName;
    private String elvisName;
    private String queryName;
    /**
     * Mode of property may combination of 'r' -Read; 'w' - Write (update); 'c' - Create
     */
    String mode = "r";

    public ElvisPropertyMapping() {
    }

    public ElvisPropertyMapping(String jcrName, String elvisName) {
        this.jcrName = jcrName;
        this.elvisName = elvisName;
    }

    public String getJcrName() {
        return jcrName;
    }

    public void setJcrName(String jcrName) {
        this.jcrName = jcrName;
    }

    

    public String getElvisName() {
		return elvisName;
	}

	public void setElvisName(String elvisName) {
		this.elvisName = elvisName;
	}

	/**
     * Mode of property may combination of 'r' -Read; 'w' - Write (update); 'c' - Create
     */
    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean inMode(char _mode) {
        return mode.indexOf(_mode) != -1;
    }

    /**
     * @return name used in Queries, if not set used cmisName
     */
    public String getQueryName() {
        return queryName == null ? elvisName : queryName;
    }

    public void setQueryName(String queryName) {
        this.queryName = queryName;
    }
}
