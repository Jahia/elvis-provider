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
 * ExternalDataSource implementation for Elvis
 *
 * @author Damien GAILLARD
 */
public class ElvisDataSource extends FilesDataSource {
    private static final Logger logger = LoggerFactory.getLogger(ElvisDataSource.class);

    private CookieStore cookieStore = new BasicCookieStore();
    private CloseableHttpClient httpClient;
    private HttpClientContext context;
    private String userName;
    private String password;
    private String url;
    private ElvisConfiguration configuration;

    @Override
    public ExternalFile getExternalFile(String path) throws PathNotFoundException {
        if (path.equals("/")) {
            return new ExternalFile(ExternalFile.FileType.FOLDER, path, null, null);
        } else {
            try {
                CloseableHttpResponse searchResponse = getDataFromApi("/search?q=assetPath:" + WebUtils.escapePath("\"" + path + "\""));
                JSONArray searchJsonArray = getHitsInSearchResponse(searchResponse);
                if (searchJsonArray.length() > 0) {
                    return createExternalFile(searchJsonArray, 0);
                } else {
                    String parentPath = StringUtils.substringBeforeLast(path, "/");
                    CloseableHttpResponse browseResponse = getDataFromApi("/browse?path=" + WebUtils.escapePath(parentPath));
                    JSONArray jsonArray = getBrowseResponse(browseResponse);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject element = jsonArray.getJSONObject(i);
                        if (element.getString("assetPath").equals(path)) {
                            return new ExternalFile(ExternalFile.FileType.FOLDER, path, null, null);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            throw new PathNotFoundException("The request was not correctly executed please check your Elvis API Server");
        }
    }

    @Override
    public List<ExternalFile> getChildrenFiles(String path) throws RepositoryException {
        List<ExternalFile> childrenList = new ArrayList<>();
        try {
            CloseableHttpResponse browseResponse = getDataFromApi("/browse?path=" + WebUtils.escapePath(path));
            JSONArray jsonArray = getBrowseResponse(browseResponse);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject element = jsonArray.getJSONObject(i);
                childrenList.add(new ExternalFile(ExternalFile.FileType.FOLDER, element.getString("assetPath"), null, null));
            }

            CloseableHttpResponse searchResponse = getDataFromApi("/search?q=folderPath:" + WebUtils.escapePath("\"" + path + "\"") + "&num=-1");
            JSONArray searchJsonArray = getHitsInSearchResponse(searchResponse);
            for (int i = 0; i < searchJsonArray.length(); i++) {
                childrenList.add(createExternalFile(searchJsonArray, i));
            }
            return childrenList;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RepositoryException(e);
        }
    }

    @Override
    public Binary getFileBinary(ExternalFile file) throws PathNotFoundException {
        try {
            CloseableHttpResponse searchResponse = getDataFromApi("/search?q=assetPath:" + WebUtils.escapePath("\"" + file.getPath() + "\""));
            JSONArray searchJsonArray = getHitsInSearchResponse(searchResponse);
            if (searchJsonArray.length() > 0) {
                JSONObject element = searchJsonArray.getJSONObject(0);
                JSONObject elMetadata = element.getJSONObject("metadata");
                long fileSize = elMetadata.has("fileSize")?elMetadata.getJSONObject("fileSize").getLong("value"):-1;
                return new ElvisBinaryImpl(element.getString("originalUrl"), fileSize, this.context, this.httpClient);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        throw new PathNotFoundException(file.getPath());
    }

    @Override
    public Binary getThumbnailBinary(ExternalFile file) throws PathNotFoundException {
        try {
            CloseableHttpResponse searchResponse = getDataFromApi("/search?q=assetPath:" + WebUtils.escapePath("\"" + file.getPath() + "\""));
            JSONArray searchJsonArray = getHitsInSearchResponse(searchResponse);
            if (searchJsonArray.length() > 0) {
                JSONObject element = searchJsonArray.getJSONObject(0);
                if (element.has("thumbnailUrl")) {
                    return new ElvisBinaryImpl(element.getString("thumbnailUrl"), -1, this.context, this.httpClient);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        throw new PathNotFoundException(file.getPath()+ "/thumbnail");
    }

    @Override
    public boolean isAvailable() throws RepositoryException {
        if (this.context.getCookieStore().getCookies().size() == 0) {
            try {
                // Execute get request to connect to the Elvis API
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
        try {
            httpClient.close();
        } catch (IOException e) {
            logger.error("Could not close from the ELVIS API !", e.getMessage());
        }

    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setConfiguration(ElvisConfiguration configuration) {
        this.configuration = configuration;
    }

    public void setUrl(String url) {
        if (url.endsWith("/")) {
            url = StringUtils.substringBeforeLast(url, "/");
        }
        this.url = url;
    }

    private CloseableHttpResponse getDataFromApi(String endOfUri) throws IOException {
        HttpGet get = new HttpGet(this.url + "/services" + endOfUri);
        get.setHeader("Accept", "Application/Json");
        return httpClient.execute(get, this.context);
    }

    private JSONArray getBrowseResponse(CloseableHttpResponse browseResponse) throws Exception {
        if (browseResponse.getStatusLine().getStatusCode() == 200) {
            return new JSONArray(EntityUtils.toString(browseResponse.getEntity()));
        }
        throw new PathNotFoundException("The request was not correctly executed please check your Elvis API Server");
    }

    private JSONArray getHitsInSearchResponse(CloseableHttpResponse searchResponse) throws Exception {
        if (searchResponse.getStatusLine().getStatusCode() == 200) {
            JSONObject jsonObject = new JSONObject(EntityUtils.toString(searchResponse.getEntity()));
            if (!jsonObject.has("errorcode")) {
                if (jsonObject.has("hits")) {
                    return jsonObject.getJSONArray("hits");
                }
            }
        }
        throw new PathNotFoundException("The request was not correctly executed please check your Elvis API Server");
    }

    private ExternalFile createExternalFile(JSONArray searchJsonArray, int index) throws JSONException, IOException, RepositoryException {
        JSONObject element = searchJsonArray.getJSONObject(index);
        JSONObject elMetadata = element.getJSONObject("metadata");

        // Get Basic information to create ExternalFile object
        String elPath = elMetadata.getString("assetPath");
        Date created = elMetadata.has("assetCreated")?new Date(elMetadata.getJSONObject("assetCreated").getLong("value")):null;
        Date modified = elMetadata.has("assetModified")?new Date(elMetadata.getJSONObject("assetModified").getLong("value")):null;
        ExternalFile externalFile = new ExternalFile(ExternalFile.FileType.FILE, elPath, created, modified);

        // Set boolean to know if we need to get the thumbnail or not
        if (element.has("thumbnailUrl")) {
            externalFile.setHasThumbnail(true);
        }

        // If possible use assetDomain value to map data but verify if we have mapping for current value if not use default file
        String fileType = (elMetadata.has("assetDomain"))?elMetadata.getString("assetDomain"):"file";
        List<ElvisTypeMapping> elvisTypesMapping = configuration.getTypeByElvisName(fileType);

        // If different than default type jnt:file getJcrName which should be the mixin e.g jmix:image
        if (!fileType.equals("file")) {
            List<String> mixins = new ArrayList<>();
            for (ElvisTypeMapping elvisTypeMapping : elvisTypesMapping) {
                if (!elvisTypeMapping.getJcrName().equals("jnt:file")) {
                    mixins.add(elvisTypeMapping.getJcrName());
                }
            }
            if (!mixins.isEmpty()) {
                externalFile.setMixin(mixins);
            }
        }


        for (ElvisTypeMapping elvisTypeMapping : elvisTypesMapping) {
            for (ElvisPropertyMapping propertyMapping : elvisTypeMapping.getProperties()) {
                String elvisName = propertyMapping.getElvisName();
                String jcrName = propertyMapping.getJcrName();
                if (elMetadata.has(elvisName) && !externalFile.getProperties().containsKey(jcrName)) {
                    if (!jcrName.equals(Constants.JCR_CREATED) && !jcrName.equals(Constants.JCR_LASTMODIFIED) && !jcrName.equals(Constants.JCR_MIMETYPE)) {
                        externalFile.getProperties().put(jcrName, new String[]{elMetadata.getString(elvisName)});
                    } else if (jcrName.equals(Constants.JCR_MIMETYPE)) {
                        String mimeType = elMetadata.getString(elvisName);
                        externalFile.getProperties().put(Constants.JCR_MIMETYPE, new String[] {mimeType});
                        externalFile.setContentType(mimeType);
                    }
                }
            }
        }

        return externalFile;
    }
}
