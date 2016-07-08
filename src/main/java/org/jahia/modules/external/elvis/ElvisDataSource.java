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
import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.modules.external.ExternalQuery;
import org.jahia.modules.external.elvis.communication.BaseElvisActionCallback;
import org.jahia.modules.external.elvis.communication.ElvisSession;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
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

    @Override
    public Set<String> getSupportedNodeTypes() {
        Set<String> supportedNodeTypes = super.getSupportedNodeTypes();
        for (String nodeTypes : configuration.getSupportedNodeTypes()) {
            if (!supportedNodeTypes.contains(nodeTypes)) {
                supportedNodeTypes.add(nodeTypes);
            }
        }

        // Add jmix:tagged as it is out of the mapping, because conditional!
        supportedNodeTypes.add(Constants.JAHIAMIX_TAGGED);
        return supportedNodeTypes;
    }

    @Override
    public ExternalFile getExternalFile(String path) throws PathNotFoundException {
        path = ElvisUtils.encodeDecodeSpecialCharacters(path, false);
        if (path.equals("/")) {
            return new ExternalFile(ExternalFile.FileType.FOLDER, path, null, null);
        } else {
            // It's a preview file
            if (path.contains(ElvisConstants.EPF_FORMAT)) {
                return getPreviewExternalFile(path);
            }

            // It's not a preview file follow the normal pattern
            final String pathToUse = path;
            try {
                return elvisSession.execute(new BaseElvisActionCallback<ExternalFile>(elvisSession) {
                    @Override
                    public ExternalFile doInElvis() throws Exception {
                        JSONArray searchJsonArray = elvisSession.getFile(pathToUse);
                        if (searchJsonArray.length() > 0) {
                            JSONObject element = searchJsonArray.getJSONObject(0);
                            JSONObject elMetadata = element.getJSONObject(ElvisConstants.PROPERTY_METADATA);
                            // Check if it's not a collection
                            if (!elMetadata.getString(ElvisConstants.PROPERTY_ASSET_DOMAIN).equals("container")) {
                                String elPath = elMetadata.getString(ElvisConstants.PROPERTY_ASSET_PATH);
                                String fileSize = elMetadata.has(ElvisConstants.PROPERTY_FILE_SIZE) ? elMetadata.getJSONObject(ElvisConstants.PROPERTY_FILE_SIZE).getString("value") : "-1";
                                String downloadUrl = element.getString(ElvisConstants.PROPERTY_ORIGINAL_URL);
                                String assetDomain = (elMetadata.has(ElvisConstants.PROPERTY_ASSET_DOMAIN)) ? elMetadata.getString(ElvisConstants.PROPERTY_ASSET_DOMAIN) : ElvisConstants.DEFAULT_ELVIS_TYPE_NAME;
                                return createExternalFile(element, elMetadata, elPath, fileSize, downloadUrl, assetDomain);
                            } else {
                                // For now collection are display as empty folder to avoid StackTrace in the tomcat console
                                ExternalFile externalCollection = new ExternalFile(ExternalFile.FileType.FOLDER, ElvisUtils.encodeDecodeSpecialCharacters(pathToUse, true), null, null);
                                externalCollection.setMixin(Collections.singletonList(ElvisConstants.ELVISMIX_COLLECTION));
                                externalCollection.getProperties().put("collectionIdentifier", new String[]{element.getString("id")});
                                return externalCollection;
                            }
                        } else {
                            String parentPath = StringUtils.substringBeforeLast(pathToUse, "/");
                            JSONArray jsonArray = elvisSession.getChildrenFolders(parentPath);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject element = jsonArray.getJSONObject(i);
                                if (element.getString(ElvisConstants.PROPERTY_ASSET_PATH).equals(pathToUse)) {
                                    return new ExternalFile(ExternalFile.FileType.FOLDER, ElvisUtils.encodeDecodeSpecialCharacters(pathToUse, true), null, null);
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
    public List<ExternalFile> getChildrenFiles(final ExternalFile externalFile) throws RepositoryException {
        final String pathToUse = ElvisUtils.encodeDecodeSpecialCharacters(externalFile.getPath(), false);
        List<ExternalFile> childrenList = new ArrayList<>();
        // if the current file is an ElvisConstants.ELVISMIX_COLLECTION we will return an empty list
        if (externalFile.getMixin() == null || (externalFile.getMixin() != null && !externalFile.getMixin().contains(ElvisConstants.ELVISMIX_COLLECTION))) {
            List<ExternalFile> externalFolders = elvisSession.execute(new BaseElvisActionCallback<List<ExternalFile>>(elvisSession) {
                @Override
                public List<ExternalFile> doInElvis() throws Exception {
                    List<ExternalFile> folders = new ArrayList<>();
                    JSONArray jsonArray = elvisSession.getChildrenFolders(pathToUse);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject element = jsonArray.getJSONObject(i);
                        folders.add(new ExternalFile(ExternalFile.FileType.FOLDER, ElvisUtils.encodeDecodeSpecialCharacters(element.getString(ElvisConstants.PROPERTY_ASSET_PATH), true), null, null));
                    }
                    return folders;
                }
            });
            childrenList.addAll(externalFolders);

            List<ExternalFile> externalFiles = elvisSession.execute(new BaseElvisActionCallback<List<ExternalFile>>(elvisSession) {
                @Override
                public List<ExternalFile> doInElvis() throws Exception {
                    List<ExternalFile> files = new ArrayList<>();
                    JSONArray searchJsonArray = elvisSession.getChildrenFiles(pathToUse);
                    for (int i = 0; i < searchJsonArray.length(); i++) {
                        JSONObject element = searchJsonArray.getJSONObject(i);
                        JSONObject elMetadata = element.getJSONObject(ElvisConstants.PROPERTY_METADATA);
                        String elPath = elMetadata.getString(ElvisConstants.PROPERTY_ASSET_PATH);
                        String fileSize = elMetadata.has(ElvisConstants.PROPERTY_FILE_SIZE) ? elMetadata.getJSONObject(ElvisConstants.PROPERTY_FILE_SIZE).getString("value") : "-1";
                        String downloadUrl = element.getString(ElvisConstants.PROPERTY_ORIGINAL_URL);
                        String assetDomain = (elMetadata.has(ElvisConstants.PROPERTY_ASSET_DOMAIN)) ? elMetadata.getString(ElvisConstants.PROPERTY_ASSET_DOMAIN) : ElvisConstants.DEFAULT_ELVIS_TYPE_NAME;

                        files.add(createExternalFile(element, elMetadata, elPath, fileSize, downloadUrl, assetDomain));

                        if (elvisSession.usePreview() && (assetDomain.equals("image") || assetDomain.equals("video"))) {
                            addPreviewExternalFiles(files, element, elMetadata, assetDomain);
                        }
                    }
                    return files;
                }
            });
            childrenList.addAll(externalFiles);
        }

        return childrenList;
    }

    @Override
    public Binary getFileBinary(ExternalFile file) throws PathNotFoundException {
        return new ElvisBinaryImpl(file.getProperties().get(ElvisConstants.PROPERTY_DOWNLOAD_URL)[0], Long.valueOf(file.getProperties().get(ElvisConstants.PROPERTY_FILE_SIZE)[0]), elvisSession);
    }

    @Override
    public Binary getThumbnailBinary(ExternalFile file) throws PathNotFoundException {
        if (file.isHasThumbnail()) {
            return new ElvisBinaryImpl(file.getProperties().get(ElvisConstants.PROPERTY_THUMBNAIL_URL)[0], -1, elvisSession);
        }
        throw new PathNotFoundException(file.getPath() + "/thumbnail");
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
        final String elvisQuery = queryResolver.resolve();

        // Not mapped or unsupported queries treated as empty.
        if (StringUtils.isBlank(elvisQuery)) {
            return Collections.emptyList();
        }

        final Long queryLimit = query.getLimit();

        if (logger.isDebugEnabled()) {
            logger.debug("Elvis query " + elvisQuery);
        }

        return elvisSession.execute(new BaseElvisActionCallback<List<String>>(elvisSession) {
            @Override
            public List<String> doInElvis() throws Exception {
                List<String> pathList = new ArrayList<>();
                JSONArray searchJsonArray = elvisSession.search(elvisQuery, queryLimit.toString());
                for (int i = 0; i < searchJsonArray.length(); i++) {
                    JSONObject element = searchJsonArray.getJSONObject(i);
                    JSONObject elMetadata = element.getJSONObject(ElvisConstants.PROPERTY_METADATA);

                    String assetDomain = (elMetadata.has(ElvisConstants.PROPERTY_ASSET_DOMAIN)) ? elMetadata.getString(ElvisConstants.PROPERTY_ASSET_DOMAIN) : ElvisConstants.DEFAULT_ELVIS_TYPE_NAME;
                    if (elvisSession.usePreview() && (assetDomain.equals("image") || assetDomain.equals("video"))) {
                        if (elvisSession.getPreviewSettings().containsKey(assetDomain) && !elvisSession.getPreviewSettings().get(assetDomain).isEmpty()) {
                            for (Map<String, String> previewParameters : elvisSession.getPreviewSettings().get(assetDomain)) {
                                String elPath = buildPreviewElPath(elMetadata, assetDomain, false, previewParameters);
                                pathList.add(elPath);
                            }
                        } else {
                            String elPath = buildPreviewElPath(elMetadata, assetDomain, true, null);
                            pathList.add(elPath);
                        }
                    }

                    pathList.add(elMetadata.getString(ElvisConstants.PROPERTY_ASSET_PATH));
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

    public ElvisSession getElvisSession() {
        return elvisSession;
    }

    private ExternalFile createExternalFile(JSONObject element, JSONObject elMetadata, String elPath, String fileSize, String downloadUrl, String assetDomain) throws JSONException {
        Date created = elMetadata.has(ElvisConstants.PROPERTY_ASSET_CREATED) ? new Date(elMetadata.getJSONObject(ElvisConstants.PROPERTY_ASSET_CREATED).getLong("value")) : null;
        Date modified = elMetadata.has(ElvisConstants.PROPERTY_ASSET_MODIFIED) ? new Date(elMetadata.getJSONObject(ElvisConstants.PROPERTY_ASSET_MODIFIED).getLong("value")) : null;
        ExternalFile externalFile = new ExternalFile(ExternalFile.FileType.FILE, ElvisUtils.encodeDecodeSpecialCharacters(elPath, true), created, modified);

        // Set boolean to know if we need to get the thumbnail or not
        if (element.has(ElvisConstants.PROPERTY_THUMBNAIL_URL)) {
            externalFile.setHasThumbnail(true);
            externalFile.getProperties().put(ElvisConstants.PROPERTY_THUMBNAIL_URL, new String[]{element.getString(ElvisConstants.PROPERTY_THUMBNAIL_URL)});
        }

        // If different than default type jnt:file getJcrMixins which should be the mixin e.g jmix:image
        List<String> mixins = new ArrayList<>();
        if (elMetadata.has("tags")) {
            mixins.add(Constants.JAHIAMIX_TAGGED);
        }

        ElvisTypeMapping elvisTypeMapping = configuration.getTypeByElvisName(assetDomain);
        mixins.addAll(elvisTypeMapping.getJcrMixins());

        if (!assetDomain.equals(ElvisConstants.DEFAULT_ELVIS_TYPE_NAME)) {
            ElvisTypeMapping elvisDefaultTypeMapping = configuration.getTypeByElvisName(ElvisConstants.DEFAULT_ELVIS_TYPE_NAME);
            mixins.addAll(elvisDefaultTypeMapping.getJcrMixins());
        }

        for (ElvisPropertyMapping propertyMapping : elvisTypeMapping.getProperties()) {
            String elvisName = propertyMapping.getElvisName();
            String jcrName = propertyMapping.getJcrName();
            String elvisType = propertyMapping.getElvisType();
            if (elMetadata.has(elvisName) && !externalFile.getProperties().containsKey(jcrName)) {
                // Property [jcr:created] and [jcr:modified] are set at the ExternalFile instantiation, the [jcr:mymeType] is need to be set as content type also and jcr:content is not exactly a property but is used as a property for the search
                if (!jcrName.equals(Constants.JCR_CREATED) && !jcrName.equals(Constants.JCR_LASTMODIFIED)
                        && !jcrName.equals(Constants.JCR_CONTENT)) {
                    if (elvisType.equals("string")) {
                        externalFile.getProperties().put(jcrName, new String[]{elMetadata.getString(elvisName)});
                        if (jcrName.equals(Constants.JCR_MIMETYPE)) {
                            externalFile.setContentType(elMetadata.getString(elvisName));
                        }
                    } else if (elvisType.equals("array")) {
                        JSONArray jsonArray = elMetadata.getJSONArray(elvisName);
                        String[] stringArray = new String[jsonArray.length()];
                        for (int i = 0; i < jsonArray.length(); i++) {
                            stringArray[i] = jsonArray.getString(i);
                        }
                        externalFile.getProperties().put(jcrName, stringArray);
                    }
                }
            }
        }

        externalFile.getProperties().put(ElvisConstants.PROPERTY_DOWNLOAD_URL, new String[]{downloadUrl});
        externalFile.getProperties().put(ElvisConstants.PROPERTY_FILE_SIZE, new String[]{fileSize});
        externalFile.setMixin(mixins);

        return externalFile;
    }

    private ExternalFile getPreviewExternalFile(final String path) throws PathNotFoundException {
        // We need to rebuild the original path
        final String pathToUse = ElvisUtils.getOriginalFilePath(path);
        // Preview name we want to get
        final String previewName = StringUtils.substringBeforeLast(StringUtils.substringAfter(path, ElvisConstants.EPF_FORMAT), "_");

        try {
            return elvisSession.execute(new BaseElvisActionCallback<ExternalFile>(elvisSession) {
                @Override
                public ExternalFile doInElvis() throws Exception {
                    JSONArray searchJsonArray = elvisSession.getFile(pathToUse);
                    if (searchJsonArray.length() > 0) {
                        JSONObject element = searchJsonArray.getJSONObject(0);
                        JSONObject elMetadata = element.getJSONObject(ElvisConstants.PROPERTY_METADATA);
                        String assetDomain = elMetadata.getString(ElvisConstants.PROPERTY_ASSET_DOMAIN);

                        boolean isDefaultElvisPreview;
                        String previewUrl = "";
                        Map<String, String> previewParameters = new HashMap<>();
                        if (previewName.equals(ElvisConstants.EP_PREVIEW_F)) {
                            isDefaultElvisPreview = true;
                            previewUrl = element.getString(ElvisConstants.PROPERTY_PREVIEW_URL);
                        } else {
                            isDefaultElvisPreview = false;
                            for (Map<String, String> parameters : elvisSession.getPreviewSettings().get(assetDomain)) {
                                if (parameters.get(ElvisConstants.PROPERTY_NAME).equals(previewName)) {
                                    previewUrl = buildPreviewUrl(element, assetDomain, parameters);
                                    previewParameters = parameters;
                                }
                            }
                        }
                        ExternalFile previewFile = createExternalFile(element, elMetadata, path, "-1", previewUrl, assetDomain);
                        updatePreviewSpecificField(previewFile, (isDefaultElvisPreview) ? null : previewParameters, assetDomain, elMetadata.getString(ElvisConstants.PROPERTY_EXTENSION), isDefaultElvisPreview);
                        return previewFile;
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

    private void addPreviewExternalFiles(List<ExternalFile> files, JSONObject element, JSONObject elMetadata, String assetDomain) throws JSONException {
        Map<String, List<Map<String, String>>> previewSettings = elvisSession.getPreviewSettings();
        if (previewSettings.containsKey(assetDomain) && !previewSettings.get(assetDomain).isEmpty()) {
            // Get settings entered by the users and create file based on it
            for (Map<String, String> parameters : previewSettings.get(assetDomain)) {
                String previewUrl = buildPreviewUrl(element, assetDomain, parameters);
                String elPath = buildPreviewElPath(elMetadata, assetDomain, false, parameters);

                ExternalFile previewFile = createExternalFile(element, elMetadata, elPath, "-1", previewUrl, assetDomain);
                updatePreviewSpecificField(previewFile, parameters, assetDomain, elMetadata.getString(ElvisConstants.PROPERTY_EXTENSION), false);

                files.add(previewFile);
            }
        } else { // Use default preview generate by Elvis
            String elPath = buildPreviewElPath(elMetadata, assetDomain, true, null);

            ExternalFile previewFile = createExternalFile(element, elMetadata, elPath, "-1", element.getString(ElvisConstants.PROPERTY_PREVIEW_URL), assetDomain);
            updatePreviewSpecificField(previewFile, null, assetDomain, elMetadata.getString(ElvisConstants.PROPERTY_EXTENSION), true);

            files.add(previewFile);
        }
    }

    private void updatePreviewSpecificField(ExternalFile previewFile, Map<String, String> previewParameters, String assetDomain, String extension, boolean isDefaultElvisPreview) {
        previewFile.getMixin().add(ElvisConstants.ELVISMIX_PREVIEW_FILE);
        previewFile.getProperties().put("previewFormatName", new String[]{(previewParameters != null)?previewParameters.get(ElvisConstants.PROPERTY_NAME):ElvisConstants.EP_PREVIEW_F});
        previewFile.getProperties().put("previewOriginalExtension", new String[]{extension});
        if (assetDomain.equals("image")) {
            if (isDefaultElvisPreview) {
                previewFile.setContentType("image/jpeg");
                previewFile.getProperties().put("j:width", new String[]{"1600"});
                previewFile.getProperties().put("j:height", new String[]{"1600"});
            } else {
                previewFile.setContentType("image/" + (previewParameters.get(ElvisConstants.PROPERTY_EXTENSION).equals("jpg") ? "jpeg" : previewParameters.get(ElvisConstants.PROPERTY_EXTENSION)));
                elvisSession.getPreviewSettings().get("image");
                previewFile.getProperties().put("j:width", new String[]{previewParameters.get("maxWidth")});
                previewFile.getProperties().put("j:height", new String[]{previewParameters.get("maxHeight")});
            }
        } else {
            if (isDefaultElvisPreview) {
                previewFile.setContentType("video/mp4");
            } else {
                previewFile.setContentType("video/" + (previewParameters.get(ElvisConstants.PROPERTY_EXTENSION).equals("flv") ? "x-flv" : previewParameters.get(ElvisConstants.PROPERTY_EXTENSION)));
            }
        }
    }

    private String buildPreviewUrl(JSONObject element, String assetDomain, Map<String, String> parameters) throws JSONException {
        String previewUrl = elvisSession.getBaseUrl() + "/preview/" + element.getString("id") +
                            "/previews/maxWidth_" + parameters.get("maxWidth") +
                            "_maxHeight_" + parameters.get("maxHeight");
        if (assetDomain.equals("image") && !parameters.get("ppi").isEmpty()) {
            previewUrl += "_ppi_" + parameters.get("ppi");
        }
        previewUrl += "_embedMetadata_true." + parameters.get(ElvisConstants.PROPERTY_EXTENSION);
        return previewUrl;
    }

    private String buildPreviewElPath(JSONObject elMetadata, String assetDomain, boolean isElvisDefaultPreview, Map<String, String> previewParameters) throws JSONException {
        if (isElvisDefaultPreview) {
            return elMetadata.getString("folderPath") + "/" +
                    StringUtils.substringBeforeLast(elMetadata.getString("filename"), ".") +
                    ElvisConstants.EPF_FORMAT + ElvisConstants.EP_PREVIEW_F + "_" + elMetadata.getString(ElvisConstants.PROPERTY_EXTENSION) +
                    "." + (assetDomain.equals("image")?"jpg":"mp4");
        } else {
            return elMetadata.getString("folderPath") + "/" +
                    StringUtils.substringBeforeLast(elMetadata.getString("filename"), ".") +
                    ElvisConstants.EPF_FORMAT + previewParameters.get(ElvisConstants.PROPERTY_NAME) + "_" + elMetadata.getString(ElvisConstants.PROPERTY_EXTENSION) +
                    "." + previewParameters.get(ElvisConstants.PROPERTY_EXTENSION);
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
