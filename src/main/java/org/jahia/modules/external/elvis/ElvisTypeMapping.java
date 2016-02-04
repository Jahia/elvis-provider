package org.jahia.modules.external.elvis;

import java.util.*;

/**
 * TypeMapping class to map the Elvis metadata with JCR
 *
 * @author Damien GAILLARD
 */
public class ElvisTypeMapping implements Cloneable {
    private String jcrName;
    private List<String> jcrMixins;
    private String elvisName;
    private String queryName;
    private ElvisTypeMapping parent;
    private List<ElvisTypeMapping> children;
    private List<ElvisPropertyMapping> properties;
    private Map<String, ElvisPropertyMapping> propertiesMapJCR;
    private Map<String, ElvisPropertyMapping> propertiesMapElvis;

    public ElvisTypeMapping() {

    }

    public ElvisTypeMapping(String jcrName, String elvisName) {
        this.jcrName = jcrName;
        this.elvisName = elvisName;
    }

    public String getJcrName() {
        return jcrName;
    }

    public void setJcrName(String jcrName) {
        this.jcrName = jcrName;
    }

    public List<String> getJcrMixins() {
        return jcrMixins;
    }

    public void setJcrMixins(String jcrName) {
        this.jcrMixins = Arrays.asList(jcrName.split(" "));
    }

    public String getElvisName() {
        return elvisName;
    }

    public void setElvisName(String elvisName) {
        this.elvisName = elvisName;
    }

    /**
     * Parent mapping. Used to configure tree of inheritance.
     *
     * @return
     */
    public ElvisTypeMapping getParent() {
        return parent;
    }

    public void setParent(ElvisTypeMapping parent) {
        this.parent = parent;
    }

    /**
     * List of configured children.  Used to configure tree of inheritance.
     * Return children added directly by setChildren method only.
     * If children initialize inheritance using parent property this method will not return such children.
     * This field used for configuration purposes only.
     *
     * @return List of configured children.
     */
    public List<ElvisTypeMapping> getChildren() {
        return children;
    }

    public void setChildren(List<ElvisTypeMapping> children) {
        this.children = children;
        if (children != null) {
            for (ElvisTypeMapping child : children) {
                child.setParent(this);
            }
        }
    }

    public List<ElvisPropertyMapping> getProperties() {
        return properties;
    }

    public void setProperties(List<ElvisPropertyMapping> properties) {
        this.properties = properties;
    }

    protected Map getPropertiesMapJCR() {
        return propertiesMapJCR;
    }

    protected Map getPropertiesMapElvis() {
        return propertiesMapElvis;
    }

    protected void initProperties() {
        HashMap<String, ElvisPropertyMapping> mapJCR = new HashMap<String, ElvisPropertyMapping>();
        HashMap<String, ElvisPropertyMapping> mapElvis = new HashMap<String, ElvisPropertyMapping>();
        if (parent != null) {
            mapJCR.putAll(parent.getPropertiesMapJCR());
            mapElvis.putAll(parent.getPropertiesMapElvis());
        }
        if (properties != null) {
            for (ElvisPropertyMapping property : properties) {
                mapJCR.put(property.getJcrName(), property);
                mapElvis.put(property.getElvisName(), property);
            }
        }
        propertiesMapElvis = mapElvis.size() == 0 ? Collections.<String, ElvisPropertyMapping>emptyMap() : Collections.unmodifiableMap(mapElvis);
        propertiesMapJCR = mapJCR.size() == 0 ? Collections.<String, ElvisPropertyMapping>emptyMap() : Collections.unmodifiableMap(mapJCR);
        if (children != null) {
            for (ElvisTypeMapping child : children) {
                child.initProperties();
            }
        }
    }


    @Override
    protected ElvisTypeMapping clone() {
        try {
            return (ElvisTypeMapping) super.clone();
        } catch (CloneNotSupportedException e) {  // impossible
            throw new IllegalStateException(e);
        }
    }

    /**
     * Lookup property mapping by JCR name
     *
     * @param propertyName
     * @return
     */
    public ElvisPropertyMapping getPropertyByJCR(String propertyName) {
        return propertiesMapJCR.get(propertyName);
    }

    /**
     * Lookup property mapping by Elvis local name
     *
     * @param localName
     * @return
     */
    public ElvisPropertyMapping getPropertyByElvis(String localName) {
        return propertiesMapElvis.get(localName);
    }

    /**
     * Name of type used in Elvis queries.
     * If not set return ElvisName
     *
     * @return
     */
    public String getQueryName() {
        return queryName == null ? elvisName : queryName;
    }

    public void setQueryName(String queryName) {
        this.queryName = queryName;
    }
}
