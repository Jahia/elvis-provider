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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.jahia.modules.external.elvis.communication.BaseElvisActionCallback;
import org.jahia.modules.external.elvis.communication.ElvisSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation binary properties to access Elvis document's content
 *
 * @author Damien GAILLARD
 */
public class ElvisBinaryImpl implements Binary {
    private static final Logger logger = LoggerFactory.getLogger(ElvisBinaryImpl.class);

    private String url;
    private long fileSize;
    private byte[] currentBinaryContent;
    private ElvisSession elvisSession;

    ElvisBinaryImpl(String url, long fileSize, ElvisSession elvisSession) {
        this.url = url;
        this.fileSize = fileSize;
        this.elvisSession = elvisSession;
    }

    @Override
    public InputStream getStream() throws RepositoryException {
        if (currentBinaryContent != null) {
            return new ByteArrayInputStream(currentBinaryContent);
        }
        if (StringUtils.isNotBlank(this.url) && this.elvisSession != null) {
            logger.info("ON VA CALL LE FICHIER : " + this.url);
            final String urlToUse = this.url;
            return elvisSession.execute(new BaseElvisActionCallback<ByteArrayInputStream>(elvisSession) {
                @Override
                public ByteArrayInputStream doInElvis() throws Exception {
                    CloseableHttpResponse httpResponse = elvisSession.getFileStream(urlToUse);

                    try (InputStream is = httpResponse.getEntity().getContent()) {
                        currentBinaryContent = IOUtils.toByteArray(is);
                    } catch (IOException e) {
                        if (logger.isDebugEnabled()) {
                            logger.error(e.getMessage(), e);
                        }
                        currentBinaryContent = new byte[0];
                    }

                    if (fileSize == -1 || fileSize == 0) {
                        fileSize = currentBinaryContent.length;
                    }

                    return new ByteArrayInputStream(currentBinaryContent);
                }
            });
        }
        throw new RepositoryException("Cannot get binary");
    }

    @Override
    public int read(byte[] b, long position) throws IOException, RepositoryException {
        if (currentBinaryContent != null) {
            return getStream().read(b, (int) position, b.length);
        }

        throw new IllegalStateException();
    }

    @Override
    public void dispose() {
        url = null;
        fileSize = -1;
        elvisSession = null;
        currentBinaryContent = null;
    }

    @Override
    public long getSize() throws RepositoryException {
        logger.info("ON A BESOIN DE LA SIZE : " + this.url);
        if (currentBinaryContent != null && (fileSize == -1 || fileSize == 0)) {
            fileSize = currentBinaryContent.length;
        }
        return fileSize;
    }
}
