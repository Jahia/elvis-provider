package org.jahia.modules.external.elvis.communication;

import javax.jcr.PathNotFoundException;

/**
 * @author dgaillard
 */
public interface ElvisSessionCallback <T> {
    T doInElvis() throws Exception;

    T onError(Exception e) throws PathNotFoundException;
}
