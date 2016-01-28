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
 * @author Damien GAILLARD
 */
public class ElvisBinaryImpl implements Binary {
    private static final Logger logger = LoggerFactory.getLogger(ElvisBinaryImpl.class);

    String url;
    long fileSize;
    boolean getSize;
    HttpClientContext context;
    byte[] currentBinaryContent;
    CloseableHttpClient httpClient;

    public ElvisBinaryImpl(String url, long fileSize, HttpClientContext context, CloseableHttpClient httpClient, boolean getSize) {
        this.url = url;
        this.fileSize = fileSize;
        this.httpClient = httpClient;
        this.context = context;
        this.getSize = getSize;
    }

    @Override
    public InputStream getStream() throws RepositoryException {
        if (currentBinaryContent != null)
            return new ByteArrayInputStream(currentBinaryContent);

        if (StringUtils.isNotBlank(this.url) && this.httpClient != null && this.context != null) {
            try {
                HttpGet get = new HttpGet(this.url);
                get.setHeader("Accept", "*/*");
                CloseableHttpResponse httpResponse = this.httpClient.execute(get, this.context);
                InputStream is = httpResponse.getEntity().getContent();
                currentBinaryContent = IOUtils.toByteArray(is);
                return new ByteArrayInputStream(currentBinaryContent);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    public int read(byte[] b, long position) throws IOException, RepositoryException {
        if (currentBinaryContent != null)
            return getStream().read(b, (int) position, b.length);

        return -1;
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
//        if (getSize) {
//            if (currentBinaryContent == null)
//                getStream();
//            fileSize = currentBinaryContent.length;
//        }
        return fileSize;
    }
}
