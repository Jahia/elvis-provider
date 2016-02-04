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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
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

    String url;
    long fileSize;
    HttpClientContext context;
    byte[] currentBinaryContent;
    CloseableHttpClient httpClient;

    public ElvisBinaryImpl(String url, long fileSize, HttpClientContext context, CloseableHttpClient httpClient) {
        this.url = url;
        this.fileSize = fileSize;
        this.httpClient = httpClient;
        this.context = context;
    }

    @Override
    public InputStream getStream() throws RepositoryException {
        if (currentBinaryContent != null) {
            return new ByteArrayInputStream(currentBinaryContent);
        }

        try {
            if (StringUtils.isNotBlank(this.url) && this.httpClient != null && this.context != null) {
                HttpGet get = new HttpGet(this.url);
                get.setHeader("Accept", "*/*");
                CloseableHttpResponse httpResponse = this.httpClient.execute(get, this.context);

                try (InputStream is = httpResponse.getEntity().getContent()) {
                    currentBinaryContent = IOUtils.toByteArray(is);
                }
                return new ByteArrayInputStream(currentBinaryContent);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            currentBinaryContent = new byte[0];
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
        context = null;
        httpClient = null;
        currentBinaryContent = null;
    }

    @Override
    public long getSize() throws RepositoryException {
        return fileSize;
    }
}
