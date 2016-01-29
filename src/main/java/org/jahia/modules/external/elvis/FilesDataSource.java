/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 */
package org.jahia.modules.external.elvis;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.ISO8601;
import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalContentStoreProvider;
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
    private static final Set<String> SUPPORTED_NODE_TYPES = new HashSet<String>(Arrays.asList(Constants.JAHIANT_FILE, Constants.JAHIANT_FOLDER, Constants.JCR_CONTENT));

    private ExternalContentStoreProvider contentStoreProvider;

    private static final String THUMBNAIL_CONSTANT = "thumbnail";
    private static final String THUMBNAIL2_CONSTANT = "thumbnail2";
    private static final List<String> JCR_CONTENT_LIST = Arrays.asList(Constants.JCR_CONTENT);
    private static final List<String> JMIX_IMAGE_LIST = Arrays.asList(Constants.JCR_CONTENT, THUMBNAIL_CONSTANT, THUMBNAIL2_CONSTANT);
    private static final String JCR_CONTENT_SUFFIX = "/" + Constants.JCR_CONTENT;
    private static final String THUMBNAIL_SUFFIX = "/" + THUMBNAIL_CONSTANT;
    private static final String THUMBNAIL2_SUFFIX = "/" + THUMBNAIL2_CONSTANT;

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
            return getThumbnailContent(getExternalFile(StringUtils.substringBeforeLast(path, THUMBNAIL_SUFFIX)), true);
        } else if (path.endsWith(THUMBNAIL2_SUFFIX)) {
            return getThumbnailContent(getExternalFile(StringUtils.substringBeforeLast(path, THUMBNAIL2_SUFFIX)), false);
        } else {
            return getExternalFile(path);
        }
    }

    public abstract ExternalFile getExternalFile(String path) throws PathNotFoundException ;

    public abstract List<ExternalFile> getChildrenFiles(String path) throws RepositoryException ;

    public abstract Binary getFileBinary(String path) throws PathNotFoundException ;

    public abstract Binary getThumbnailBinary(String path) throws PathNotFoundException ;

    public List<String> getChildren(String path) throws RepositoryException {
        if (!path.endsWith(JCR_CONTENT_SUFFIX) && !path.endsWith(THUMBNAIL_SUFFIX) && !path.endsWith(THUMBNAIL2_SUFFIX)) {
            ExternalFile externalFile = getExternalFile(path);
            if (externalFile.getType().equals(Constants.JAHIANT_FILE)) {
                if (externalFile.getMixin().contains(Constants.JAHIAMIX_IMAGE))
                    return JMIX_IMAGE_LIST;

                return JCR_CONTENT_LIST;
            } else if (externalFile.getType().equals(Constants.JAHIANT_FOLDER)) {
                List<ExternalFile> files = getChildrenFiles(path);
                if (files.size() > 0) {
                    List<String> children = new LinkedList<String>();
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
        if (!path.endsWith(JCR_CONTENT_SUFFIX) && !path.endsWith(THUMBNAIL_SUFFIX) && !path.endsWith(THUMBNAIL2_SUFFIX)) {
            ExternalFile externalFile = getExternalFile(path);
            if (externalFile.getType().equals(Constants.JAHIANT_FILE)) {
                List<ExternalData> externalDatas = new ArrayList<>();
                externalDatas.add(getFileContent(externalFile));
                if (externalFile.getMixin().contains(Constants.JAHIAMIX_IMAGE)) {
                    externalDatas.add(getThumbnailContent(externalFile, true));
                    externalDatas.add(getThumbnailContent(externalFile, false));
                }
                return externalDatas;
            } else if (externalFile.getType().equals(Constants.JAHIANT_FOLDER)) {
                List<ExternalFile>  files = getChildrenFiles(externalFile.getPath());
                if (files.size() > 0) {
                    List<ExternalData> children = new LinkedList<ExternalData>();
                    for (ExternalFile object : files) {
                        children.add(object);
                        if (object.getType().equals(Constants.JAHIANT_FILE)) {
                            children.add(getFileContent(object));
                            if (object.getMixin().contains(Constants.JAHIAMIX_IMAGE)) {
                                children.add(getThumbnailContent(object, true));
                                children.add(getThumbnailContent(object, false));
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
        Map<String, String[]> properties = new HashMap<String, String[]>(1);

        Binary content = getFileBinary(file.getPath());

        properties.put(Constants.JCR_MIMETYPE, new String[]{getContentType(file)});

        String jcrContentPath = file.getPath() + "/" + Constants.JCR_CONTENT;
        ExternalData externalData = new ExternalData(jcrContentPath, jcrContentPath, Constants.JAHIANT_RESOURCE, properties);

        Map<String, Binary[]> binaryProperties = new HashMap<String, Binary[]>(1);
        binaryProperties.put(Constants.JCR_DATA, new Binary[]{content});
        externalData.setBinaryProperties(binaryProperties);

        return externalData;
    }

    protected ExternalData getThumbnailContent(ExternalFile file, boolean isFirstThumbnail) throws PathNotFoundException {
        Map<String, String[]> properties = new HashMap<String, String[]>(1);

        Binary content = getThumbnailBinary(file.getPath());

        properties.put(Constants.JCR_MIMETYPE, new String[]{getContentType(file)});

        String thumbnailContentPath = file.getPath() + ((isFirstThumbnail)?THUMBNAIL_SUFFIX:THUMBNAIL2_SUFFIX);
        ExternalData externalData = new ExternalData(thumbnailContentPath, thumbnailContentPath, Constants.JAHIANT_RESOURCE, properties);

        Map<String, Binary[]> binaryProperties = new HashMap<String, Binary[]>(1);
        binaryProperties.put(Constants.JCR_DATA, new Binary[]{content});
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
     * TODO Comment me
     *
     * @author toto
     */
    public static class ExternalFile extends ExternalData {

        public enum FileType { FILE, FOLDER }

        private String contentType;

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
    }
}
