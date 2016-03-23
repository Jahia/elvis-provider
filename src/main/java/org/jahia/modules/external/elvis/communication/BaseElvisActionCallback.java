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
package org.jahia.modules.external.elvis.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

/**
 * @author dgaillard
 */
public abstract class BaseElvisActionCallback<T> implements ElvisSessionCallback<T> {
    private static Logger logger = LoggerFactory.getLogger(BaseElvisActionCallback.class);

    private ElvisSession elvisSession;

    protected BaseElvisActionCallback(ElvisSession elvisSession) {
        this.elvisSession = elvisSession;
    }

    @Override
    public abstract T doInElvis() throws Exception;

    @Override
    public T onError(Exception e) throws RepositoryException {
        if (e instanceof org.json.JSONException) {
            if (e.getMessage().contains("errorcode") && e.getMessage().contains("401")) {
                elvisSession.closeHttp();
                elvisSession.initHttp();
                if (elvisSession.isSessionAvailable()) {
                    try {
                        return doInElvis();
                    } catch (Exception ex) {
                        throw new RepositoryException("The request was not correctly executed please check your Elvis API Server", ex);
                    }
                }
            }
        } else if (e instanceof javax.jcr.RepositoryException) {
            throw (RepositoryException) e;
        }

        throw new RepositoryException("The request was not correctly executed please check your Elvis API Server", e);
    }
}
