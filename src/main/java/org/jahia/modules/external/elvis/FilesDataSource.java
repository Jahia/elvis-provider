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
import org.apache.jackrabbit.util.ISO8601;
import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.services.content.JCRContentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.*;

/**
 * Data source implementation for retrieving files.
 */
public abstract class FilesDataSource implements ExternalDataSource, ExternalDataSource.CanLoadChildrenInBatch, ExternalDataSource.CanCheckAvailability, ExternalDataSource.Initializable  {

    private static final Logger logger = LoggerFactory.getLogger(FilesDataSource.class);
    private static final Set<String> SUPPORTED_NODE_TYPES = new HashSet<>(Arrays.asList(Constants.JAHIANT_FILE, Constants.JAHIANT_FOLDER, Constants.NT_RESOURCE));

    private static final String THUMBNAIL_CONSTANT = "thumbnail";
    private static final List<String> JCR_CONTENT_LIST = Arrays.asList(Constants.JCR_CONTENT);
    private static final List<String> JMIX_IMAGE_LIST = Arrays.asList(Constants.JCR_CONTENT, THUMBNAIL_CONSTANT);
    private static final String JCR_CONTENT_SUFFIX = "/" + Constants.JCR_CONTENT;
    private static final String THUMBNAIL_SUFFIX = "/" + THUMBNAIL_CONSTANT;
    private static final String THUMBNAIL_MIME_TYPE = "image/jpeg";

    public boolean isSupportsUuid() {
        return false;
    }

    @Override
    public boolean isSupportsHierarchicalIdentifiers() {
        return true;
    }

    @Override
    public boolean itemExists(String path) {
        try {
            getItemByPath(path);
            return true;
        } catch (PathNotFoundException e) {
            return false;
        }
    }

    public Set<String> getSupportedNodeTypes() {
        return SUPPORTED_NODE_TYPES;
    }

    public ExternalData getItemByIdentifier(String identifier) throws ItemNotFoundException {
        if (identifier.startsWith("/")) {
            try {
                return getItemByPath(identifier);
            } catch (PathNotFoundException e) {
                throw new ItemNotFoundException(identifier, e);
            }
        }
        throw new ItemNotFoundException(identifier);
    }

    public ExternalData getItemByPath(String path) throws PathNotFoundException {
        if (path.endsWith(JCR_CONTENT_SUFFIX)) {
            return getFileContent(getExternalFile(StringUtils.substringBeforeLast(path, JCR_CONTENT_SUFFIX)));
        } else if (path.endsWith(THUMBNAIL_SUFFIX)) {
            return getThumbnailContent(getExternalFile(StringUtils.substringBeforeLast(path, THUMBNAIL_SUFFIX)));
        } else {
            return getExternalFile(path);
        }
    }

    public abstract ExternalFile getExternalFile(String path) throws PathNotFoundException ;

    public abstract List<ExternalFile> getChildrenFiles(String path) throws RepositoryException ;

    public abstract Binary getFileBinary(ExternalFile file) throws PathNotFoundException ;

    public abstract Binary getThumbnailBinary(ExternalFile file) throws PathNotFoundException ;

    public List<String> getChildren(String path) throws RepositoryException {
        if (!path.endsWith(JCR_CONTENT_SUFFIX) && !path.endsWith(THUMBNAIL_SUFFIX)) {
            ExternalFile externalFile = getExternalFile(path);
            if (externalFile.getType().equals(Constants.JAHIANT_FILE)) {
                if (externalFile.isHasThumbnail()) {
                    return JMIX_IMAGE_LIST;
                }
                return JCR_CONTENT_LIST;
            } else if (externalFile.getType().equals(Constants.JAHIANT_FOLDER)) {
                List<ExternalFile> files = getChildrenFiles(path);
                if (files.size() > 0) {
                    List<String> children = new LinkedList<>();
                    for (ExternalFile object : files) {
                        children.add(object.getName());
                    }
                    return children;
                } else {
                    return Collections.emptyList();
                }
            }
        }

        return Collections.emptyList();
    }

    @Override
    public List<ExternalData> getChildrenNodes(String path) throws RepositoryException {
        if (!path.endsWith(JCR_CONTENT_SUFFIX) && !path.endsWith(THUMBNAIL_SUFFIX)) {
            ExternalFile externalFile = getExternalFile(path);
            if (externalFile.getType().equals(Constants.JAHIANT_FILE)) {
                List<ExternalData> externalDatas = new ArrayList<>();
                externalDatas.add(getFileContent(externalFile));
                if (externalFile.isHasThumbnail()) {
                    externalDatas.add(getThumbnailContent(externalFile));
                }
                return externalDatas;
            } else if (externalFile.getType().equals(Constants.JAHIANT_FOLDER)) {
                List<ExternalFile>  files = getChildrenFiles(externalFile.getPath());
                if (files.size() > 0) {
                    List<ExternalData> children = new LinkedList<>();
                    for (ExternalFile object : files) {
                        children.add(object);
                        if (object.getType().equals(Constants.JAHIANT_FILE)) {
                            children.add(getFileContent(object));
                            if (object.isHasThumbnail()) {
                                children.add(getThumbnailContent(object));
                            }
                        }
                    }
                    return children;
                } else {
                    return Collections.emptyList();
                }
            }
        }

        return Collections.emptyList();
    }

    protected ExternalData getFileContent(ExternalFile file) throws PathNotFoundException {
        return getContentNode(file, getFileBinary(file), getContentType(file), JCR_CONTENT_SUFFIX);
    }

    protected ExternalData getThumbnailContent(ExternalFile file) throws PathNotFoundException {
        return getContentNode(file, getThumbnailBinary(file), THUMBNAIL_MIME_TYPE, THUMBNAIL_SUFFIX);
    }

    private ExternalData getContentNode(ExternalFile file, Binary binary, String contentType, String suffix) throws PathNotFoundException {
        Map<String, String[]> properties = new HashMap<>(1);

        properties.put(Constants.JCR_MIMETYPE, new String[]{contentType});

        String jcrContentPath = file.getPath() +  suffix;
        ExternalData externalData = new ExternalData(jcrContentPath, jcrContentPath, Constants.JAHIANT_RESOURCE, properties);

        Map<String, Binary[]> binaryProperties = new HashMap<>(1);
        binaryProperties.put(Constants.JCR_DATA, new Binary[]{binary});
        externalData.setBinaryProperties(binaryProperties);

        return externalData;
    }

    protected String getContentType(ExternalFile content) {
        String s1 = content.getContentType();
        if (s1 == null) {
            s1 = JCRContentUtils.getMimeType(content.getName());
        }
        if (s1 == null) {
            s1 = "application/octet-stream";
        }
        return s1;
    }

    /**
     * Object representation of a file or a folder
     *
     * @author toto
     */
    public static class ExternalFile extends ExternalData {

        public enum FileType { FILE, FOLDER }

        private String contentType;

        private boolean hasThumbnail;

        public ExternalFile(FileType type, String path, Date lastModified, Date created) {
            super(path, path,
                    type == FileType.FILE ? Constants.JAHIANT_FILE : Constants.JAHIANT_FOLDER,
                    new HashMap<String, String[]>());
            Map<String,String[]> properties = getProperties();

            if (lastModified != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(lastModified);
                properties.put(Constants.JCR_LASTMODIFIED, new String[]{ISO8601.format(calendar)});
            }

            if (created != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(created);
                properties.put(Constants.JCR_CREATED, new String[]{ISO8601.format(calendar)});
            }

        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public boolean isHasThumbnail() {
            return hasThumbnail;
        }

        public void setHasThumbnail(boolean hasThumbnail) {
            this.hasThumbnail = hasThumbnail;
        }
    }
}
