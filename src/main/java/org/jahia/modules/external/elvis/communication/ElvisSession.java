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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dgaillard
 */
public class ElvisSession {
    private static Logger logger = LoggerFactory.getLogger(ElvisSession.class);

    private String userName;
    private String password;
    private String baseUrl;
    private String fileLimit;
    private String fieldToWriteUsage;
    private boolean usePreview;
    private boolean trustAllCertificate;
    private Map<String, List<Map<String, String>>> previewSettings;
    private CloseableHttpClient httpClient;
    private HttpClientContext context;

    public ElvisSession(String baseUrl, String userName, String password, String fileLimit, boolean usePreview,
                        String previewSettings, String fieldToWriteUsage, boolean trustAllCertificate) {
        if (baseUrl.endsWith("/")) {
            baseUrl = StringUtils.substringBeforeLast(baseUrl, "/");
        }
        this.baseUrl = baseUrl;
        this.userName = userName;
        this.password = password;
        this.fileLimit = fileLimit;
        this.usePreview = usePreview;
        this.previewSettings = convertJSONtoMap(previewSettings);
        this.fieldToWriteUsage = fieldToWriteUsage;
        this.trustAllCertificate = trustAllCertificate;
    }

    public <X> X execute(ElvisSessionCallback<X> callback) throws RepositoryException {
        try {
            return callback.doInElvis();
        } catch (Exception e) {
            return callback.onError(e);
        }
    }

    public void initHttp() {
        if (trustAllCertificate) {
            try {
                SSLContextBuilder builder = new SSLContextBuilder();
                builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
                SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build());
                context = HttpClientContext.create();
                CookieStore cookieStore = new BasicCookieStore();
                context.setCookieStore(cookieStore);
                httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).setSSLSocketFactory(sslsf).build();
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        } else {
            context = HttpClientContext.create();
            CookieStore cookieStore = new BasicCookieStore();
            context.setCookieStore(cookieStore);
            httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();
        }
    }

    public void logout() {
        try {
            getDataFromApi("/logout");
            httpClient.close();
        } catch (IOException e) {
            logger.error("Could not logout from the ELVIS API !", e.getMessage());
        }
    }

    public void closeHttp() {
        try {
            httpClient.close();
        } catch (IOException e) {
            logger.error("Could not logout from the ELVIS API !", e.getMessage());
        }
    }

    public boolean isSessionAvailable() {
        if (this.context.getCookieStore().getCookies().isEmpty()) {
            try {
                // Execute get request to connect to the Elvis API
                connectToApi();
                return !context.getCookieStore().getCookies().isEmpty();
            } catch(IOException e) {
                logger.error("Could not login to the ELVIS API !", e.getMessage());
                return false;
            }
        } else {
            return true;
        }
    }

    public CloseableHttpResponse getDataFromApi(String endOfUri) throws IOException {
        HttpGet get = new HttpGet(this.baseUrl + "/services" + endOfUri);
        get.setHeader("Accept", "Application/Json");
        return httpClient.execute(get, context);
    }

    public CloseableHttpResponse getFileStream(String url) throws IOException {
        HttpGet get = new HttpGet(url);
        get.setHeader("Accept", "*/*");
        return httpClient.execute(get, context);
    }

    public CloseableHttpResponse writeAssetUsageInElvis(String assetPath, String pageUrl, boolean add) throws IOException{
        HttpPost post = new HttpPost(this.baseUrl + "/services/updatebulk");
        String parameters = "q=assetPath:\"" + assetPath + "\"&" + this.fieldToWriteUsage + (add?"=%2B":"=-") + pageUrl;
        StringEntity stringEntity = new StringEntity(parameters, ContentType.APPLICATION_FORM_URLENCODED);
        post.setEntity(stringEntity);
        return httpClient.execute(post, context);
    }

    public String getFileLimit() {
        return fileLimit;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public boolean usePreview() {
        return usePreview;
    }

    public Map<String, List<Map<String, String>>> getPreviewSettings() {
        return previewSettings;
    }

    private CloseableHttpResponse connectToApi() throws IOException {
        String stringToEncode = userName + ":" + password;
        String encodedString = Base64.encodeBase64String(stringToEncode.getBytes());
        HttpGet get = new HttpGet(this.baseUrl + "/services/login?cred=" + encodedString);
        get.setHeader("Accept", "Application/Json");
        return httpClient.execute(get, context);
    }

    private Map<String, List<Map<String, String>>> convertJSONtoMap(String previewSettings) {
        Map<String, List<Map<String, String>>> previewSettingsMap = new HashMap<>();

        if (StringUtils.isNotEmpty(previewSettings)) {
            try {
                previewSettingsMap = new ObjectMapper().readValue(previewSettings, HashMap.class);
            } catch (IOException e) {
                logger.error("Error when parsing previewSettings JSON", e.getMessage());
            }
        }

        return previewSettingsMap;
    }
}
