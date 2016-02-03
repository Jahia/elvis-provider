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

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.modules.external.ExternalQuery;
import org.jahia.utils.WebUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.*;
import java.util.*;

/**
 * @author Damien GAILLARD
 */
public class ElvisDataSource extends FilesDataSource implements ExternalDataSource.Searchable {
    private static final Logger logger = LoggerFactory.getLogger(ElvisDataSource.class);

    private CookieStore cookieStore = new BasicCookieStore();
    private CloseableHttpClient httpClient;
    private ExternalContentStoreProvider externalContentStoreProvider;
    private HttpClientContext context;
    private String userName;
    private String password;
    private String url;

    @Override
    public ExternalFile getExternalFile(String path) throws PathNotFoundException {
        if (path.equals("/")) {
            return new ExternalFile(ExternalFile.FileType.FOLDER, path, null, null);
        } else {
            try {
                CloseableHttpResponse searchResponse = getDataFromApi("/search?q=assetPath:" + WebUtils.escapePath("\"" + path + "\""));
                if (searchResponse.getStatusLine().getStatusCode() == 200) {
                    JSONObject jsonObject = new JSONObject(EntityUtils.toString(searchResponse.getEntity()));
                    if (!jsonObject.has("errorcode")) {
                        if (jsonObject.has("hits")) {
                            JSONArray searchJsonArray = jsonObject.getJSONArray("hits");
                            if (searchJsonArray.length() > 0) {
                                return createExternalFile(searchJsonArray, 0);
                            } else {
                                return new ExternalFile(ExternalFile.FileType.FOLDER, path, null, null);
                            }
                        }
                    } else {
                        throw new PathNotFoundException("The request could not be executed please check your Elvis API credential");
                    }
                }
                throw new PathNotFoundException("The request was not correctly executed please check your Elvis API Server");
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return null;
            }
        }
    }

    @Override
    public List<ExternalFile> getChildrenFiles(String path) throws RepositoryException {
        List<ExternalFile> childrenList = new ArrayList<>();
        try {
            CloseableHttpResponse browseResponse = getDataFromApi("/browse?path=" + WebUtils.escapePath(path));
            if (browseResponse.getStatusLine().getStatusCode() == 200) {
                try {
                    JSONArray jsonArray = new JSONArray(EntityUtils.toString(browseResponse.getEntity()));
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject element = jsonArray.getJSONObject(i);
                        childrenList.add(new ExternalFile(ExternalFile.FileType.FOLDER, element.getString("assetPath"), null, null));
                    }
                } catch (JSONException e) {
                    logger.error("Could not process JSON response probably due a connection problem with the API");
                    return childrenList;
                }
            }

            CloseableHttpResponse searchResponse = getDataFromApi("/search?q=folderPath:" + WebUtils.escapePath(path));
            if (searchResponse.getStatusLine().getStatusCode() == 200) {
                JSONObject jsonObject = new JSONObject(EntityUtils.toString(searchResponse.getEntity()));
                if (!jsonObject.has("errorcode")) {
                    if (jsonObject.has("hits")) {
                        JSONArray searchJsonArray = jsonObject.getJSONArray("hits");
                        for (int i = 0; i < searchJsonArray.length(); i++) {
                            childrenList.add(createExternalFile(searchJsonArray, i));
                        }
                    }
                }
            }

            return childrenList;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Binary getFileBinary(String path) throws PathNotFoundException {
        try {
            String originalUrl = "";
            long fileSize = -1;
            CloseableHttpResponse searchResponse = getDataFromApi("/search?q=assetPath:" + WebUtils.escapePath("\"" + path + "\""));
            if (searchResponse.getStatusLine().getStatusCode() == 200) {
                JSONObject jsonObject = new JSONObject(EntityUtils.toString(searchResponse.getEntity()));
                if (jsonObject.has("hits")) {
                    JSONArray searchJsonArray = jsonObject.getJSONArray("hits");
                    for (int i = 0 ; i < searchJsonArray.length() ; i++) {
                        JSONObject element = searchJsonArray.getJSONObject(i);
                        originalUrl = element.getString("originalUrl");
                        JSONObject elMetadata = element.getJSONObject("metadata");
                        if (elMetadata.has("fileSize"))
                            fileSize = elMetadata.getJSONObject("fileSize").getLong("value");
                    }
                }
            }

            return new ElvisBinaryImpl(originalUrl, fileSize, this.context, this.httpClient);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Binary getThumbnailBinary(String path) throws PathNotFoundException {
        try {
            String thumbnailUrl = "";
            CloseableHttpResponse searchResponse = getDataFromApi("/search?q=assetPath:" + WebUtils.escapePath("\"" + path + "\""));
            if (searchResponse.getStatusLine().getStatusCode() == 200) {
                JSONObject jsonObject = new JSONObject(EntityUtils.toString(searchResponse.getEntity()));
                if (jsonObject.has("hits")) {
                    JSONArray searchJsonArray = jsonObject.getJSONArray("hits");
                    for (int i = 0 ; i < searchJsonArray.length() ; i++) {
                        JSONObject element = searchJsonArray.getJSONObject(i);
                        if (element.has("thumbnailUrl"))
                            thumbnailUrl = element.getString("thumbnailUrl");
                    }
                }
            }

            return new ElvisBinaryImpl(thumbnailUrl, -1, this.context, this.httpClient);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean isAvailable() throws RepositoryException {
        if (this.context.getCookieStore().getCookies().size() == 0) {
            try {
                //Execute the post to Dalim API
                getDataFromApi("/login?username=" + this.userName + "&password=" + this.password);
                return this.context.getCookieStore().getCookies().size() > 0;
            } catch(IOException e) {
                logger.error("Could not login to the ELVIS API !", e.getMessage());
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public void start() {
        this.context = HttpClientContext.create();
        this.context.setCookieStore(this.cookieStore);
        this.httpClient = HttpClientBuilder.create().setDefaultCookieStore(this.cookieStore).build();
    }

    @Override
    public void stop() {
        //Logout
        try {
            getDataFromApi("/logout");
        } catch (IOException e) {
            logger.error("Could not logout from the ELVIS API !", e.getMessage());
        }
    }

    @Override
    public List<String> search(ExternalQuery query) throws RepositoryException {
        QueryResolver queryResolver = new QueryResolver(this, query);
        queryResolver.resolve();

        return Collections.emptyList();
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUrl(String url) {
        if (url.endsWith("/"))
            url = StringUtils.substringBeforeLast(url, "/");
        this.url = url;
    }

    public void setExternalContentStoreProvider(ExternalContentStoreProvider externalContentStoreProvider) {
        this.externalContentStoreProvider = externalContentStoreProvider;
    }

    private ExternalFile createExternalFile(JSONArray searchJsonArray, int i) throws JSONException, IOException, RepositoryException {
        JSONObject element = searchJsonArray.getJSONObject(i);
        JSONObject elMetadata = element.getJSONObject("metadata");
        String elPath = elMetadata.getString("assetPath");
        long created = elMetadata.getJSONObject("assetCreated").getLong("value");
        long modified = elMetadata.getJSONObject("assetModified").getLong("value");

        ExternalFile externalFile = new ExternalFile(ExternalFile.FileType.FILE, elPath, new Date(created), new Date(modified));
        if (elMetadata.has("description"))
            externalFile.getProperties().put(Constants.JCR_DESCRIPTION, new String[] {elMetadata.getString("description")});
        if (elMetadata.has("assetCreator"))
            externalFile.getProperties().put(Constants.JCR_CREATEDBY, new String[] {elMetadata.getString("assetCreator")});
        if (elMetadata.has("assetModifier"))
            externalFile.getProperties().put(Constants.JCR_LASTMODIFIEDBY, new String[] {elMetadata.getString("assetModifier")});

        String mimeType = elMetadata.getString("mimeType");
        externalFile.setContentType(mimeType);

        // If file is an image
        externalFile.getProperties().put(Constants.JCR_MIMETYPE, new String[] {mimeType});
        if (mimeType.startsWith("image/")) {
            externalFile.setMixin(Arrays.asList(Constants.JAHIAMIX_IMAGE));
            if (elMetadata.has("width"))
                externalFile.getProperties().put("j:width", new String[] {elMetadata.getString("width")});
            if (elMetadata.has("height"))
                externalFile.getProperties().put("j:height", new String[] {elMetadata.getString("height")});
        }
        return externalFile;
    }

    private CloseableHttpResponse getDataFromApi(String endOfUri) throws IOException {
        HttpGet get = new HttpGet(this.url + "/services" + endOfUri);
        get.setHeader("Accept", "Application/Json");
        return this.httpClient.execute(get, this.context);
    }
}
