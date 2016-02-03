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
package org.jahia.modules.external.elvis.admin;

import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;
import org.jahia.modules.external.admin.mount.AbstractMountPointFactory;
import org.jahia.modules.external.admin.mount.validator.LocalJCRFolder;
import org.jahia.services.content.JCRNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * @author Damien GAILLARD
 */
public class MountPointFactory extends AbstractMountPointFactory {
    private static final Logger logger = LoggerFactory.getLogger(MountPointFactory.class);
    public static final String URL = "url";
    public static final String USER_NAME = "userName";
    public static final String PASSWORD = "password";

    @NotEmpty
    private String name;
    @LocalJCRFolder
    private String localPath;
    @NotEmpty
    @URL
    private String url;
    @NotEmpty
    private String userName;
    @NotEmpty
    private String password;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getLocalPath() {
        return localPath;
    }

    @Override
    public String getMountNodeType() {
        return "elvisnt:mountPoint";
    }

    @Override
    public void setProperties(JCRNodeWrapper mountNode) throws RepositoryException {
        mountNode.setProperty(URL, url);
        mountNode.setProperty(USER_NAME, userName);
        mountNode.setProperty(PASSWORD, password);
    }

    @Override
    public void populate(JCRNodeWrapper nodeWrapper) throws RepositoryException {
        super.populate(nodeWrapper);
        this.name = getName(nodeWrapper.getName());
        try {
            this.localPath = nodeWrapper.getProperty("mountPoint").getNode().getPath();
        }catch (PathNotFoundException e) {
            logger.error("No local path defined for this mount point");
        }
        this.userName = nodeWrapper.getPropertyAsString(USER_NAME);
        this.password = nodeWrapper.getPropertyAsString(PASSWORD);
        this.url = nodeWrapper.getPropertyAsString(URL);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
