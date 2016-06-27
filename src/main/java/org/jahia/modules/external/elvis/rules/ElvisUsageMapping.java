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

    private String assetPath;

    private String mountPointIdentifier;

    public ElvisUsageMapping() {}

    public ElvisUsageMapping(String componentIdentifier, String propertyName, String assetPath, String mountPointIdentifier) {
        this.componentIdentifier = componentIdentifier;
        this.propertyName = propertyName;
        this.assetPath = assetPath;
        this.mountPointIdentifier = mountPointIdentifier;
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

    @Column(name = "propertyName")
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

    @Column()
    @Index(name = "mountPointIdentifier")
    public String getMountPointIdentifier() {
        return mountPointIdentifier;
    }

    public void setMountPointIdentifier(String mountPointIdentifier) {
        this.mountPointIdentifier = mountPointIdentifier;
    }
}
