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
package org.jahia.modules.external.elvis.rules;

import org.hibernate.annotations.Index;

import javax.persistence.*;

/**
 * @author dgaillard
 */
@Entity
@Table(name = "elvis_usage_mapping")
public class ElvisUsageMapping {

    private int id;
    private String componentIdentifier;
    private String propertyName;
    private String pageUrl;

    private String assetPath;

    private String mountPointIdentifier;

    public ElvisUsageMapping() {}

    public ElvisUsageMapping(String componentIdentifier, String propertyName, String assetPath, String mountPointIdentifier, String pageUrl) {
        this.componentIdentifier = componentIdentifier;
        this.propertyName = propertyName;
        this.assetPath = assetPath;
        this.mountPointIdentifier = mountPointIdentifier;
        this.pageUrl = pageUrl;
    }

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column()
    @Index(name = "componentIdentifier")
    public String getComponentIdentifier() {
        return componentIdentifier;
    }

    public void setComponentIdentifier(String componentIdentifier) {
        this.componentIdentifier = componentIdentifier;
    }

    @Column()
    @Index(name = "propertyName")
    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    @Column(name = "assetPath")
    public String getAssetPath() {
        return assetPath;
    }

    public void setAssetPath(String assetPath) {
        this.assetPath = assetPath;
    }

    @Column(name = "mountPointIdentifier")
    public String getMountPointIdentifier() {
        return mountPointIdentifier;
    }

    public void setMountPointIdentifier(String mountPointIdentifier) {
        this.mountPointIdentifier = mountPointIdentifier;
    }

    @Column(name = "pageUrl")
    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }
}
