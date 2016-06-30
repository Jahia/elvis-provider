/**
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms & Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.external.elvis;

import java.util.*;

/**
 * TypeMapping class to map the Elvis metadata with JCR
 *
 * @author Damien GAILLARD
 */
public class ElvisTypeMapping implements Cloneable {
    private List<String> jcrName;
    private List<String> elvisName;
    private List<ElvisPropertyMapping> properties;
    private Map<String, ElvisPropertyMapping> propertiesMapJCR;
    private Map<String, ElvisPropertyMapping> propertiesMapElvis;

    public ElvisTypeMapping() {}

    public ElvisTypeMapping(List<String> jcrName, List<String> elvisName) {
        this.jcrName = jcrName;
        this.elvisName = elvisName;
    }

    public List<String> getJcrName() {
        return jcrName;
    }

    public void setJcrName(String jcrName) {
        this.jcrName = Arrays.asList(jcrName.split("\\s*,\\s*"));
    }

    public List<String> getElvisName() {
        return elvisName;
    }

    public String getElvisNameAsQueryString() {
        String elvisNameAsString = "";
        for (int i = 0 ; i < elvisName.size() ; i++) {
            if (i != 0) {
                elvisNameAsString += "%20OR%20";
            }
            elvisNameAsString += elvisName.get(i);
        }
        return elvisNameAsString;
    }

    public void setElvisName(String elvisName) {
        this.elvisName = Arrays.asList(elvisName.split("\\s*,\\s*"));
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
        HashMap<String, ElvisPropertyMapping> mapJCR = new HashMap<>();
        HashMap<String, ElvisPropertyMapping> mapElvis = new HashMap<>();
        if (properties != null) {
            for (ElvisPropertyMapping property : properties) {
                mapJCR.put(property.getJcrName(), property);
                mapElvis.put(property.getElvisName(), property);
            }
        }
        propertiesMapElvis = mapElvis.size() == 0 ? Collections.<String, ElvisPropertyMapping>emptyMap() : Collections.unmodifiableMap(mapElvis);
        propertiesMapJCR = mapJCR.size() == 0 ? Collections.<String, ElvisPropertyMapping>emptyMap() : Collections.unmodifiableMap(mapJCR);
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
}
