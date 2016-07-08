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

import org.jahia.api.Constants;

import java.util.*;

/**
 * This is the Configuration file for Elvis API
 *
 * @author Damien GAILLARD
 */
public class ElvisConfiguration {
    private static List<String> JCR_TYPES_LIST;
    private static List<String> ELVIS_TYPES_LIST;
    private static ElvisTypeMapping DEFAULT_ELVIS_TYPE_MAPPING;

    private List<ElvisTypeMapping> typeMapping;
    private Map<List<String>, ElvisTypeMapping> elvisTypes;
    private Map<List<String>, ElvisTypeMapping> jcrTypes;

    public void onStart() {
        elvisTypes = new HashMap<>();
        jcrTypes = new HashMap<>();
        if (typeMapping != null) {
            JCR_TYPES_LIST = new ArrayList<>();
            ELVIS_TYPES_LIST = new ArrayList<>();
            for (ElvisTypeMapping elvisTypeMapping : typeMapping) {

                List<String> elvisName = elvisTypeMapping.getElvisName();
                ELVIS_TYPES_LIST.addAll(elvisName);
                elvisTypes.put(elvisName, elvisTypeMapping);

                List<String> jcrName = elvisTypeMapping.getJcrMixins();
                JCR_TYPES_LIST.addAll(jcrName);
                jcrTypes.put(jcrName, elvisTypeMapping);

                if (elvisName.contains(ElvisConstants.DEFAULT_ELVIS_TYPE_NAME)) {
                    DEFAULT_ELVIS_TYPE_MAPPING = elvisTypeMapping;
                }

                elvisTypeMapping.initProperties();
            }
        }
    }

    public void setTypeMapping(List<ElvisTypeMapping> typeMapping) {
        this.typeMapping = typeMapping;
    }

    public Map<List<String>, ElvisTypeMapping> getElvisTypes() {
        return elvisTypes;
    }

    public Map<List<String>, ElvisTypeMapping> getJcrTypes() {
        return jcrTypes;
    }

    public ElvisTypeMapping getTypeByJCRName(String name) {
        if (name.equals(Constants.JAHIANT_FILE)) {
            return DEFAULT_ELVIS_TYPE_MAPPING;
        }

        for (Map.Entry<List<String>, ElvisTypeMapping> entry : jcrTypes.entrySet()) {
            if (entry.getKey().contains(name)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * This method return the mapping for the current type if not in map it will return default mapping
     * @param name  : name of the desired type
     * @return
     */
    public ElvisTypeMapping getTypeByElvisName(String name) {
        if (name.equals(ElvisConstants.DEFAULT_ELVIS_TYPE_NAME)) {
            return DEFAULT_ELVIS_TYPE_MAPPING;
        }

        for (Map.Entry<List<String>, ElvisTypeMapping> entry : elvisTypes.entrySet()) {
            if (entry.getKey().contains(name)) {
                return entry.getValue();
            }
        }

        return DEFAULT_ELVIS_TYPE_MAPPING;
    }

    public List<String> getSupportedNodeTypes() {
        return JCR_TYPES_LIST;
    }
}
