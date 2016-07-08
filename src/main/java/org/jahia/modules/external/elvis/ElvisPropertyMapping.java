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

/**
 * Configuration class to map Elvis <-> JCR type attributes.
 * Any attribute may be mapped in list of modes. Depends on mode attribute may support reade, write and be used on create.
 *
 * @author Damien GAILLARD
 */
public class ElvisPropertyMapping {
    private String jcrName;
    private String elvisName;
    private String elvisType;

    public ElvisPropertyMapping() {
    }

    public ElvisPropertyMapping(String jcrName, String elvisName, String elvisType) {
        this.jcrName = jcrName;
        this.elvisName = elvisName;
        this.elvisType = elvisType;
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

    public String getElvisType() {
        return elvisType;
    }

    public void setElvisType(String type) {
        this.elvisType = type;
    }
}
