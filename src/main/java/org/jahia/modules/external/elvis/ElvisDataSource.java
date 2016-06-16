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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.modules.external.ExternalQuery;
import org.jahia.modules.external.elvis.communication.BaseElvisActionCallback;
import org.jahia.modules.external.elvis.communication.ElvisSession;
import org.jahia.utils.WebUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import java.io.*;
import java.util.*;

import static javax.jcr.security.Privilege.JCR_READ;

/**
 * ExternalDataSource implementation for Elvis
 *
 * @author Damien GAILLARD
 */
public class ElvisDataSource extends FilesDataSource implements ExternalDataSource.Searchable, ExternalDataSource.AccessControllable {

    private static final Logger logger = LoggerFactory.getLogger(ElvisDataSource.class);

    protected ElvisConfiguration configuration;
    private ElvisSession elvisSession;

    private static String ELVISMIX_FILE = "elvismix:file";
    private static String ELVISMIX_PREVIEW_FILE = "elvismix:previewFile";
    private static String EPF_FORMAT = "_EPF-FORMAT_";

    @Override
    public ExternalFile getExternalFile(String path) throws PathNotFoundException {
        path = encodeDecodeSpecialCharacters(path, false);
        if (path.equals("/")) {
            return new ExternalFile(ExternalFile.FileType.FOLDER, path, null, null);
        } else {
            // It's a preview file
            if (path.contains(EPF_FORMAT)) {
                return getPreviewExternalFile(path);
            }

            // It's not a preview file follow the normal pattern
            final String pathToUse = path;
            try {
                return elvisSession.execute(new BaseElvisActionCallback<ExternalFile>(elvisSession) {
                    @Override
                    public ExternalFile doInElvis() throws Exception {
                        CloseableHttpResponse searchResponse = elvisSession.getDataFromApi("/search?q=assetPath:" + WebUtils.escapePath("\"" + pathToUse + "\""));
                        JSONArray searchJsonArray = getHitsInSearchResponse(searchResponse);
                        if (searchJsonArray.length() > 0) {
                            return createExternalFile(searchJsonArray.getJSONObject(0));
                        } else {
                            String parentPath = StringUtils.substringBeforeLast(pathToUse, "/");
                            CloseableHttpResponse browseResponse = elvisSession.getDataFromApi("/browse?path=" + WebUtils.escapePath(parentPath));
                            JSONArray jsonArray = getBrowseResponse(browseResponse);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject element = jsonArray.getJSONObject(i);
                                if (element.getString("assetPath").equals(pathToUse)) {
                                    return new ExternalFile(ExternalFile.FileType.FOLDER, encodeDecodeSpecialCharacters(pathToUse, true), null, null);
                                }
                            }
                        }
                        throw new PathNotFoundException("The request was not correctly executed please check your Elvis API Server");
                    }
                });
            } catch (PathNotFoundException e) {
                throw e;
            } catch (RepositoryException e) {
                logger.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public List<ExternalFile> getChildrenFiles(String path) throws RepositoryException {
        path = encodeDecodeSpecialCharacters(path, false);
        List<ExternalFile> childrenList = new ArrayList<>();
        final String pathToUse = path;
        List<ExternalFile> externalFolders = elvisSession.execute(new BaseElvisActionCallback<List<ExternalFile>>(elvisSession) {
            @Override
            public List<ExternalFile> doInElvis() throws Exception {
                List<ExternalFile> folders = new ArrayList<>();
                CloseableHttpResponse browseResponse = elvisSession.getDataFromApi("/browse?path=" + WebUtils.escapePath(pathToUse));
                JSONArray jsonArray = getBrowseResponse(browseResponse);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject element = jsonArray.getJSONObject(i);
                    folders.add(new ExternalFile(ExternalFile.FileType.FOLDER, encodeDecodeSpecialCharacters(element.getString("assetPath"), true), null, null));
                }
                return folders;
            }
        });
        childrenList.addAll(externalFolders);

        List<ExternalFile> externalFiles = elvisSession.execute(new BaseElvisActionCallback<List<ExternalFile>>(elvisSession) {
            @Override
            public List<ExternalFile> doInElvis() throws Exception {
                List<ExternalFile> files = new ArrayList<>();
                CloseableHttpResponse searchResponse = elvisSession.getDataFromApi("/search?q=folderPath:" + WebUtils.escapePath("\"" + pathToUse + "\"") + "&num=" + elvisSession.getFileLimit());
                JSONArray searchJsonArray = getHitsInSearchResponse(searchResponse);
                for (int i = 0; i < searchJsonArray.length(); i++) {
                    JSONObject element = searchJsonArray.getJSONObject(i);
                    files.add(createExternalFile(element));
                    JSONObject elMetadata = element.getJSONObject("metadata");

                    String assetDomain = (elMetadata.has("assetDomain")) ? elMetadata.getString("assetDomain") : "file";
                    if (elvisSession.usePreview() && (assetDomain.equals("image") || assetDomain.equals("video"))) {
                        addPreviewExternalFiles(files, element, elMetadata, assetDomain);
                    }
                }
                return files;
            }
        });
        childrenList.addAll(externalFiles);

        return childrenList;
    }

    @Override
    public Binary getFileBinary(ExternalFile file) throws PathNotFoundException {
        return new ElvisBinaryImpl(file.getProperties().get("downloadUrl")[0], Long.valueOf(file.getProperties().get("fileSize")[0]), elvisSession);
    }

    @Override
    public Binary getThumbnailBinary(ExternalFile file) throws PathNotFoundException {
        return new ElvisBinaryImpl(file.getProperties().get("thumbnailUrl")[0], -1, elvisSession);
    }

    @Override
    public boolean isAvailable() throws RepositoryException {
        return elvisSession.isSessionAvailable();
    }

    @Override
    public void start() {
        elvisSession.initHttp();
    }

    @Override
    public void stop() {
        elvisSession.logout();
    }

    @Override
    public List<String> search(ExternalQuery query) throws RepositoryException {
        QueryResolver queryResolver = new QueryResolver(this, query);
        final String sql = queryResolver.resolve();

        // Not mapped or unsupported queries treated as empty.
        if (StringUtils.isBlank(sql)) {
            return Collections.emptyList();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Elvis query " + sql);
        }

        return elvisSession.execute(new BaseElvisActionCallback<List<String>>(elvisSession) {
            @Override
            public List<String> doInElvis() throws Exception {
                List<String> pathList = new ArrayList<>();
                CloseableHttpResponse searchResponse = elvisSession.getDataFromApi("/search?q=" + sql);
                JSONArray searchJsonArray = getHitsInSearchResponse(searchResponse);
                for (int i = 0; i < searchJsonArray.length(); i++) {
                    JSONObject element = searchJsonArray.getJSONObject(i);
                    JSONObject elMetadata = element.getJSONObject("metadata");

                    if (elvisSession.usePreview()) {
                        String assetDomain = (elMetadata.has("assetDomain")) ? elMetadata.getString("assetDomain") : "file";
                        if (elvisSession.getPreviewSettings().containsKey(assetDomain)) {
                            if (!elvisSession.getPreviewSettings().get(assetDomain).isEmpty()) {
                                for (Map<String, String> previewParameters : elvisSession.getPreviewSettings().get(assetDomain)) {
                                    String elPath = buildPreviewElPath(elMetadata, assetDomain, false, previewParameters);
                                    pathList.add(elPath);
                                }
                            } else {
                                String elPath = buildPreviewElPath(elMetadata, assetDomain, true, null);
                                pathList.add(elPath);
                            }
                        }
                    }

                    pathList.add(elMetadata.getString("assetPath"));
                }
                return pathList;
            }
        });
    }

    public void setConfiguration(ElvisConfiguration configuration) {
        this.configuration = configuration;
    }

    public void setElvisSession(ElvisSession elvisSession) {
        this.elvisSession = elvisSession;
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

    private ExternalFile createExternalFile(JSONObject element) throws JSONException, IOException, RepositoryException {
        JSONObject elMetadata = element.getJSONObject("metadata");

        // Get Basic information to create ExternalFile object
        String elPath = elMetadata.getString("assetPath");
        Date created = elMetadata.has("assetCreated") ? new Date(elMetadata.getJSONObject("assetCreated").getLong("value")) : null;
        Date modified = elMetadata.has("assetModified") ? new Date(elMetadata.getJSONObject("assetModified").getLong("value")) : null;
        ExternalFile externalFile = new ExternalFile(ExternalFile.FileType.FILE, encodeDecodeSpecialCharacters(elPath, true), created, modified);

        // Set boolean to know if we need to get the thumbnail or not
        if (element.has("thumbnailUrl")) {
            externalFile.setHasThumbnail(true);
            externalFile.getProperties().put("thumbnailUrl", new String[]{element.getString("thumbnailUrl")});
        }

        // If possible use assetDomain value to map data but verify if we have mapping for current value if not use default file
        String fileType = (elMetadata.has("assetDomain")) ? elMetadata.getString("assetDomain") : "file";
        List<ElvisTypeMapping> elvisTypesMapping = configuration.getTypeByElvisName(fileType);

        // If different than default type jnt:file getJcrName which should be the mixin e.g jmix:image
        if (!fileType.equals("file")) {
            List<String> mixins = new ArrayList<>();
            mixins.add(ELVISMIX_FILE);
            for (ElvisTypeMapping elvisTypeMapping : elvisTypesMapping) {
                if (!elvisTypeMapping.getJcrName().equals("jnt:file")) {
                    mixins.add(elvisTypeMapping.getJcrName());
                }
                if (elMetadata.has("tags")) {
                    mixins.add(Constants.JAHIAMIX_TAGGED);
                }
            }
            if (!mixins.isEmpty()) {
                externalFile.setMixin(mixins);
            }
        }

        String fileSize = elMetadata.has("fileSize") ? elMetadata.getJSONObject("fileSize").getString("value") : "-1";
        String downloadUrl = element.getString("originalUrl");

        externalFile.getProperties().put("downloadUrl", new String[]{downloadUrl});
        externalFile.getProperties().put("fileSize", new String[]{fileSize});

        for (ElvisTypeMapping elvisTypeMapping : elvisTypesMapping) {
            for (ElvisPropertyMapping propertyMapping : elvisTypeMapping.getProperties()) {
                String elvisName = propertyMapping.getElvisName();
                String jcrName = propertyMapping.getJcrName();
                if (elMetadata.has(elvisName) && !externalFile.getProperties().containsKey(jcrName)) {
                    // Property [jcr:created] and [jcr:modified] are set at the ExternalFile instantiation, the [jcr:mymeType] is need to be set as content type also and jcr:content is not exactly a property but is used as a property for the search
                    if (!jcrName.equals(Constants.JCR_CREATED) && !jcrName.equals(Constants.JCR_LASTMODIFIED)
                            && !jcrName.equals(Constants.JCR_MIMETYPE) && !jcrName.equals(Constants.JCR_CONTENT)
                            && !jcrName.equals("j:tagList")) {
                        externalFile.getProperties().put(jcrName, new String[]{elMetadata.getString(elvisName)});
                    } else if (jcrName.equals(Constants.JCR_MIMETYPE)) {
                        String mimeType = elMetadata.getString(elvisName);
                        externalFile.getProperties().put(Constants.JCR_MIMETYPE, new String[]{mimeType});
                        externalFile.setContentType(mimeType);
                    } else if (jcrName.equals("j:tagList")) {
                        JSONArray tags = elMetadata.getJSONArray(elvisName);
                        String[] tagList = new String[tags.length()];
                        for (int i = 0; i < tags.length(); i++) {
                            tagList[i] = tags.getString(i);
                        }
                        externalFile.getProperties().put("j:tagList", tagList);
                    }
                }
            }
        }

        return externalFile;
    }

    private ExternalFile getPreviewExternalFile(final String path) throws PathNotFoundException {
        // We need to rebuild the original path
        final String pathToUse = StringUtils.substringBefore(path, EPF_FORMAT) + "." + StringUtils.substringBeforeLast(StringUtils.substringAfterLast(path, "_"), ".");
        // Preview name we want to get
        final String previewName = StringUtils.substringBeforeLast(StringUtils.substringAfter(path, EPF_FORMAT), "_");

        try {
            return elvisSession.execute(new BaseElvisActionCallback<ExternalFile>(elvisSession) {
                @Override
                public ExternalFile doInElvis() throws Exception {
                    CloseableHttpResponse searchResponse = elvisSession.getDataFromApi("/search?q=assetPath:" + WebUtils.escapePath("\"" + pathToUse + "\""));
                    JSONArray searchJsonArray = getHitsInSearchResponse(searchResponse);
                    if (searchJsonArray.length() > 0) {
                        JSONObject element = searchJsonArray.getJSONObject(0);
                        JSONObject elMetadata = element.getJSONObject("metadata");
                        String assetDomain = elMetadata.getString("assetDomain");

                        boolean isDefaultElvisPreview;
                        String previewUrl = "";
                        Map<String, String> previewParameters = new HashMap<>();
                        if (previewName.equals("ED-preview-F")) {
                            isDefaultElvisPreview = true;
                            previewUrl = element.getString("previewUrl");
                        } else {
                            isDefaultElvisPreview = false;
                            for (Map<String, String> parameters : elvisSession.getPreviewSettings().get(assetDomain)) {
                                if (parameters.get("name").equals(previewName)) {
                                    previewUrl = buildPreviewUrl(element, assetDomain, parameters);
                                    previewParameters = parameters;
                                }
                            }
                        }

                        return createPreviewExternalFile(element, elMetadata, path, assetDomain, previewUrl, isDefaultElvisPreview, (isDefaultElvisPreview) ? null : previewParameters);
                    }
                    throw new PathNotFoundException("Error when trying to get preview file, path was: " + pathToUse);
                }
            });
        } catch (PathNotFoundException e) {
            throw e;
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private ExternalFile createPreviewExternalFile(JSONObject element, JSONObject elMetadata, String elPath, String assetDomain, String previewUrl, boolean isDefaultElvisPreview, Map<String, String> previewParameters) throws JSONException {
        Date created = elMetadata.has("assetCreated") ? new Date(elMetadata.getJSONObject("assetCreated").getLong("value")) : null;
        Date modified = elMetadata.has("assetModified") ? new Date(elMetadata.getJSONObject("assetModified").getLong("value")) : null;
        ExternalFile previewFile = new ExternalFile(ExternalFile.FileType.FILE, encodeDecodeSpecialCharacters(elPath, true), modified, created);

        List<String> mixins = new ArrayList<>();
        mixins.add(ELVISMIX_FILE);
        mixins.add(ELVISMIX_PREVIEW_FILE);
        if (assetDomain.equals("image")) {
            mixins.add(Constants.JAHIAMIX_IMAGE);
            if (isDefaultElvisPreview) {
                previewFile.setContentType("image/jpeg");
                previewFile.getProperties().put("j:width", new String[]{"1600"});
                previewFile.getProperties().put("j:height", new String[]{"1600"});
            } else {
                previewFile.setContentType("image/" + (previewParameters.get("extension").equals("jpg") ? "jpeg" : previewParameters.get("extension")));
                elvisSession.getPreviewSettings().get("image");
                previewFile.getProperties().put("j:width", new String[]{previewParameters.get("maxWidth")});
                previewFile.getProperties().put("j:height", new String[]{previewParameters.get("maxHeight")});
            }
        } else {
            if (isDefaultElvisPreview) {
                previewFile.setContentType("video/mp4");
            } else {
                previewFile.setContentType("video/" + (previewParameters.get("extension").equals("flv") ? "x-flv" : previewParameters.get("extension")));
            }
        }
        previewFile.setMixin(mixins);
        previewFile.getProperties().put("downloadUrl", new String[]{previewUrl});
        previewFile.getProperties().put("fileSize", new String[]{"-1"});

        // Set boolean to know if we need to get the thumbnail or not
        if (element.has("thumbnailUrl")) {
            previewFile.setHasThumbnail(true);
            previewFile.getProperties().put("thumbnailUrl", new String[]{element.getString("thumbnailUrl")});
        }

        return previewFile;
    }

    private void addPreviewExternalFiles(List<ExternalFile> files, JSONObject element, JSONObject elMetadata, String assetDomain) throws JSONException {
        Map<String, List<Map<String, String>>> previewSettings = elvisSession.getPreviewSettings();
        if (previewSettings.containsKey(assetDomain) && !previewSettings.get(assetDomain).isEmpty()) {
            // Get settings entered by the users and create file based on it
            for (Map<String, String> parameters : previewSettings.get(assetDomain)) {
                String previewUrl = buildPreviewUrl(element, assetDomain, parameters);

                String elPath = buildPreviewElPath(elMetadata, assetDomain, false, parameters);

                ExternalFile previewFile = createPreviewExternalFile(element, elMetadata, elPath, assetDomain, previewUrl, false, parameters);

                files.add(previewFile);
            }
        } else { // Use default preview generate by Elvis
            String elPath = buildPreviewElPath(elMetadata, assetDomain, true, null);

            ExternalFile previewFile = createPreviewExternalFile(element, elMetadata, elPath, assetDomain, element.getString("previewUrl"), true, null);

            files.add(previewFile);
        }
    }

    private String buildPreviewUrl(JSONObject element, String assetDomain, Map<String, String> parameters) throws JSONException {
        String previewUrl = elvisSession.getBaseUrl() + "/preview/" + element.getString("id") +
                            "/previews/maxWidth_" + parameters.get("maxWidth") +
                            "_maxHeight_" + parameters.get("maxHeight");
        if (assetDomain.equals("image") && !parameters.get("ppi").isEmpty()) {
            previewUrl += "_ppi_" + parameters.get("ppi");
        }
        previewUrl += "." + parameters.get("extension");
        return previewUrl;
    }

    private String buildPreviewElPath(JSONObject elMetadata, String assetDomain, boolean isElvisDefaultPreview, Map<String, String> previewParameters) throws JSONException {
        if (isElvisDefaultPreview) {
            return elMetadata.getString("folderPath") + "/" +
                    StringUtils.substringBeforeLast(elMetadata.getString("filename"), ".") +
                    EPF_FORMAT + "ED-preview-F_" + elMetadata.getString("extension") +
                    "." + (assetDomain.equals("image")?"jpg":"mp4");
        } else {
            return elMetadata.getString("folderPath") + "/" +
                    StringUtils.substringBeforeLast(elMetadata.getString("filename"), ".") +
                    EPF_FORMAT + previewParameters.get("name") + "_" + elMetadata.getString("extension") +
                    "." + previewParameters.get("extension");
        }
    }

    /**
     * To encode and decode characters not allowed by DXM and allowed by Elvis
     * @param path      : path to encode/decode
     * @param encode    : true if you want to encode
     * @return encoded/decoded path
     */
    private String encodeDecodeSpecialCharacters(String path, boolean encode) {
        if (encode) {
            return (path!=null)?StringUtils.replaceEachRepeatedly(path.replace("%", "%25"), new String[]{"[","]"}, new String[]{"%5B","%5D"}):null;
        } else {
            return (path!=null && path.contains("%"))?StringUtils.replaceEachRepeatedly(path, new String[]{"%5B","%5D"}, new String[]{"[","]"}).replace("%25", "%"):path;
        }
    }

    @Override
    public String[] getPrivilegesNames(String username, String path) {
        Set<String> privileges = new HashSet<>();

        privileges.add(JCR_READ + "_" + Constants.EDIT_WORKSPACE);
        privileges.add(JCR_READ + "_" + Constants.LIVE_WORKSPACE);

        return privileges.toArray(new String[privileges.size()]);
    }
}
