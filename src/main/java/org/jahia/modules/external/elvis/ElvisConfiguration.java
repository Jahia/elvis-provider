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
 * This is the Configuration file for Elvis API
 *
 * @author Damien GAILLARD
 */
public class ElvisConfiguration {
    private static String DEFAULT_ELVIS_TYPE_NAME = "file";
    private static List<String> JCR_TYPES_LIST;
    private static List<String> ELVIS_TYPES_LIST;
    private static List<ElvisTypeMapping> DEFAULT_ELVIS_TYPE_MAPPING;

    private List<ElvisTypeMapping> typeMapping;
    private Map<List<String>, List<ElvisTypeMapping>> elvisTypes;
    private Map<List<String>, List<ElvisTypeMapping>> jcrTypes;

    public void onStart() {
        elvisTypes = new HashMap<>();
        jcrTypes = new HashMap<>();
        if (typeMapping != null) {
            JCR_TYPES_LIST = new ArrayList<>();
            ELVIS_TYPES_LIST = new ArrayList<>();
            DEFAULT_ELVIS_TYPE_MAPPING = new ArrayList<>();
            for (ElvisTypeMapping elvisTypeMapping : typeMapping) {

                List<String> elvisName = elvisTypeMapping.getElvisName();
                ELVIS_TYPES_LIST.addAll(elvisName);
                buildMap(elvisTypeMapping, elvisTypes, elvisName);

                List<String> jcrName = elvisTypeMapping.getJcrName();
                JCR_TYPES_LIST.addAll(jcrName);
                buildMap(elvisTypeMapping, jcrTypes, jcrName);

                elvisTypeMapping.initProperties();
            }
        }
    }

    public void setTypeMapping(List<ElvisTypeMapping> typeMapping) {
        this.typeMapping = typeMapping;
    }

    public Map<List<String>, List<ElvisTypeMapping>> getElvisTypes() {
        return elvisTypes;
    }

    public Map<List<String>, List<ElvisTypeMapping>> getJcrTypes() {
        return jcrTypes;
    }

    public List<ElvisTypeMapping> getTypeByJCRName(String name) {
        List<ElvisTypeMapping> list = new ArrayList<>();
        for (Map.Entry<List<String>, List<ElvisTypeMapping>> entry : jcrTypes.entrySet()) {
            if (entry.getKey().contains(name)) {
                list = entry.getValue();
            }
        }
        return list;
    }

    /**
     * This method return the mapping for the current type if not in map it will return default mapping
     * @param name  : name of the desired type
     * @return
     */
    public List<ElvisTypeMapping> getTypeByElvisName(String name) {
        List<ElvisTypeMapping> list = new ArrayList<>();

        for (Map.Entry<List<String>, List<ElvisTypeMapping>> entry : elvisTypes.entrySet()) {
            if (entry.getKey().contains(name)) {
                list = entry.getValue();
            }
        }

        if (list.isEmpty()) {
            return DEFAULT_ELVIS_TYPE_MAPPING;
        }

        return list;
    }

    public List<String> getSupportedNodeTypes() {
        return JCR_TYPES_LIST;
    }

    private void buildMap(ElvisTypeMapping elvisTypeMapping, Map<List<String>, List<ElvisTypeMapping>> map, List<String> name) {
        if (map.containsKey(name)) {
            map.get(name).add(elvisTypeMapping);
        } else {
            List<ElvisTypeMapping> list = new ArrayList<>();
            list.add(elvisTypeMapping);
            map.put(name, list);
        }

        if (name.contains("file")) {
            DEFAULT_ELVIS_TYPE_MAPPING.add(elvisTypeMapping);
        }
    }
}
