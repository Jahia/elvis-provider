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
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dgaillard
 */
public class ElvisSession {
    private static Logger logger = LoggerFactory.getLogger(ElvisSession.class);

    private String encodedCredential;
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

        String stringToEncode = userName + ":" + password;
        this.encodedCredential = Base64.encodeBase64String(stringToEncode.getBytes());

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
            httpPostCall("/logout");
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
                List<NameValuePair> nameValuePairs = new ArrayList<>();
                nameValuePairs.add(new BasicNameValuePair("cred", encodedCredential));
                CloseableHttpResponse response = httpPostCall("/login", nameValuePairs);
                return !context.getCookieStore().getCookies().isEmpty();
            } catch(IOException e) {
                logger.error("Could not login to the ELVIS API !", e.getMessage());
                return false;
            }
        } else {
            return true;
        }
    }

    public JSONArray getChildrenFolders(String folderPath) throws Exception {
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("path", folderPath));
        return getBrowseResponse(httpPostCall("/browse", nameValuePairs));
    }

    public JSONArray getChildrenFiles(String folderPath) throws Exception {
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("q", "folderPath:\"" + folderPath + "\""));
        nameValuePairs.add(new BasicNameValuePair("num", fileLimit));
        return getHitsInSearchResponse(httpPostCall("/search", nameValuePairs));
    }

    public JSONArray getFile(String filePath) throws Exception {
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("q", "assetPath:\"" + filePath + "\""));
        return getHitsInSearchResponse(httpPostCall("/search", nameValuePairs));
    }

    public JSONArray search(String query, String queryLimit) throws Exception {
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("q", query));
        nameValuePairs.add(new BasicNameValuePair("num", queryLimit));
        return getHitsInSearchResponse(httpPostCall("/search", nameValuePairs));
    }

    public boolean updateBulk(String filePath, String pageUrl, boolean add) throws Exception {
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("q", "assetPath:\"" + filePath + "\""));
        nameValuePairs.add(new BasicNameValuePair(fieldToWriteUsage, (add?"+":"-") + pageUrl));
        return checkResponse(httpPostCall("/updatebulk", nameValuePairs));
    }

    public CloseableHttpResponse getFileStream(String url) throws IOException {
        HttpGet get = new HttpGet(url);
        get.setHeader("Accept", MediaType.MULTIPART_FORM_DATA_VALUE);
        return httpClient.execute(get, context);
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

    private CloseableHttpResponse httpPostCall(String endOfUri) throws IOException {
        HttpPost post = new HttpPost(baseUrl + "/services" + endOfUri);
        post.setHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
        return httpClient.execute(post, context);
    }

    private CloseableHttpResponse httpPostCall(String endOfUri, List<NameValuePair> nameValuePairs) throws IOException {
        HttpPost post = new HttpPost(baseUrl + "/services" + endOfUri);
        post.setHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
        // Add your data
        post.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
        return httpClient.execute(post, context);
    }

    private JSONArray getHitsInSearchResponse(CloseableHttpResponse searchResponse) throws Exception {
        if (searchResponse.getStatusLine().getStatusCode() == 200) {
            String jsonString = EntityUtils.toString(searchResponse.getEntity());
            JSONObject jsonObject = new JSONObject(jsonString);
            if (!jsonObject.has("errorcode")) {
                if (jsonObject.has("hits")) {
                    return jsonObject.getJSONArray("hits");
                }
            } else {
                throw new JSONException(jsonString);
            }
        }
        throw new RepositoryException("The request was not correctly executed please check your Elvis API Server");
    }

    private JSONArray getBrowseResponse(CloseableHttpResponse browseResponse) throws Exception {
        if (browseResponse.getStatusLine().getStatusCode() == 200) {
            String jsonString = EntityUtils.toString(browseResponse.getEntity());
            try {
                return new JSONArray(jsonString);
            } catch (JSONException e) {
                throw new JSONException(jsonString);
            }
        }
        throw new RepositoryException("The request was not correctly executed please check your Elvis API Server");
    }

    private Boolean checkResponse(CloseableHttpResponse response) throws IOException, JSONException, RepositoryException {
        if (response.getStatusLine().getStatusCode() == 200) {
            String jsonString = EntityUtils.toString(response.getEntity());
            JSONObject jsonObject = new JSONObject(jsonString);
            if (jsonObject.has("errorcode")) {
                throw new JSONException(jsonString);
            }
            return jsonObject.has("processedCount") && jsonObject.getInt("processedCount") == 1
                    && jsonObject.has("errorCount") && jsonObject.getInt("errorCount") == 0;
        } else {
            throw new RepositoryException("The request was not correctly executed please check your Elvis API Server");
        }
    }
}
