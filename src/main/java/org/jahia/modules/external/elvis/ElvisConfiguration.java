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
 * @author Damien GAILLARD
 */
public class ElvisConfiguration {
    private static String DEFAULT_ELVIS_TYPE_NAME = "file";

    private List<ElvisTypeMapping> typeMapping;
    private Map<String, ElvisTypeMapping> elvisTypes;
    private Map<String, ElvisTypeMapping> jcrTypes;

    public void onStart() {
        elvisTypes = new HashMap<>();
        jcrTypes = new HashMap<>();
        if (typeMapping != null) {
            Queue<ElvisTypeMapping> list = new LinkedList<>(typeMapping);
            while (!list.isEmpty()) {
                ElvisTypeMapping type = list.remove();
                elvisTypes.put(type.getElvisName(), type);
                jcrTypes.put(type.getJcrName(), type);
            }
            for (ElvisTypeMapping elvisTypeMapping : typeMapping) {
                elvisTypeMapping.initProperties();
            }
        }
    }

    public void setTypeMapping(List<ElvisTypeMapping> typeMapping) {
        this.typeMapping = typeMapping;
    }

    public Map<String, ElvisTypeMapping> getElvisTypes() {
        return elvisTypes;
    }

    public Map<String, ElvisTypeMapping> getJcrTypes() {
        return jcrTypes;
    }

    public ElvisTypeMapping getTypeByJCRName(String name) {
        return jcrTypes.get(name);
    }

    /**
     * This method return the mapping for the current type if not in map it will return default mapping
     * @param name  : name of the desired type
     * @return
     */
    public ElvisTypeMapping getTypeByElvisName(String name) {
        return elvisTypes.containsKey(name)?elvisTypes.get(name):elvisTypes.get(DEFAULT_ELVIS_TYPE_NAME);
    }
}
