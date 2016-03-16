package org.jahia.modules.external.elvis.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;

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
    public T onError(Exception e) throws PathNotFoundException {
        if (e instanceof org.json.JSONException) {
            if (e.getMessage().contains("errorcode") && e.getMessage().contains("401")) {
                elvisSession.closeHttp();
                elvisSession.initHttp();
                if (elvisSession.isSessionAvailable()) {
                    try {
                        return doInElvis();
                    } catch (Exception ex) {
                        throw new PathNotFoundException("The request was not correctly executed please check your Elvis API Server", ex);
                    }
                }
            }
        } else if (e instanceof javax.jcr.PathNotFoundException) {
            throw (PathNotFoundException) e;
        }

        throw new PathNotFoundException("The request was not correctly executed please check your Elvis API Server", e);
    }
}
