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

import javax.jcr.RepositoryException;

/**
 * @author Damien GAILLARD
 */
public class NotMappedElvisProperty extends RepositoryException {
    public NotMappedElvisProperty() {}

    public NotMappedElvisProperty(String name) {
        this(name, null);
    }

    public NotMappedElvisProperty(String name, Throwable rootCause) {
        super("Property " + name + " not mapped", rootCause);
    }

    public NotMappedElvisProperty(Throwable rootCause) {
        super(rootCause);
    }
}
