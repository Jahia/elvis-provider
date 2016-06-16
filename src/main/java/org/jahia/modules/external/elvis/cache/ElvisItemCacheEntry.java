package org.jahia.modules.external.elvis.cache;

import org.jahia.api.Constants;
import org.jahia.modules.external.elvis.FilesDataSource;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author dgaillard
 */
public class ElvisItemCacheEntry implements Serializable {
    private static final long serialVersionUID = 3149043234199575492L;

    private String path;
    private String type;
    private String downloadUrl;
    private String contentType;
    private String thumbnailUrl;
    private List<String> mixin;
    private Map<String,String[]> properties;
    private long fileSize;
    private boolean hasThumbnail;
    private boolean calculateFileSize;

    public ElvisItemCacheEntry(FilesDataSource.ExternalFile externalFile) {
        this.path = externalFile.getPath();
        this.type = externalFile.getType();

        if (externalFile.getType().equals(Constants.JAHIANT_FILE)) {
            this.contentType = externalFile.getContentType();
            this.hasThumbnail = externalFile.isHasThumbnail();
            if (externalFile.getProperties() != null && !externalFile.getProperties().isEmpty()) {
                this.properties = externalFile.getProperties();
            }
            if (externalFile.getMixin() != null && !externalFile.getMixin().isEmpty()) {
                this.mixin = externalFile.getMixin();
            }
        }
    }

    public String getPath() {
        return path;
    }

    public String getType() {
        return type;
    }

    public String getContentType() {
        return contentType;
    }

    public boolean isHasThumbnail() {
        return hasThumbnail;
    }

    public List<String> getMixin() {
        return mixin;
    }

    public Map<String, String[]> getProperties() {
        return properties;
    }

    public FilesDataSource.ExternalFile getAsExternalFile() {
        if (getType().equals(Constants.JAHIANT_FOLDER)) {
            return new FilesDataSource.ExternalFile(FilesDataSource.ExternalFile.FileType.FOLDER, getPath(), null, null);
        }

        Date created = getProperties().containsKey(Constants.JCR_CREATED)?new DateTime(getProperties().get(Constants.JCR_CREATED)[0]).toDate():null;
        Date modified = getProperties().containsKey(Constants.JCR_LASTMODIFIED)?new DateTime(getProperties().get(Constants.JCR_LASTMODIFIED)[0]).toDate():null;
        FilesDataSource.ExternalFile externalFile = new FilesDataSource.ExternalFile(FilesDataSource.ExternalFile.FileType.FILE, getPath(), created, modified);

        externalFile.setContentType(getContentType());
        externalFile.setHasThumbnail(isHasThumbnail());
        if (getMixin() != null && !getMixin().isEmpty()) {
            externalFile.setMixin(getMixin());
        }

        if (getProperties() != null && !getProperties().isEmpty()) {
            for (Map.Entry<String, String[]> entry : getProperties().entrySet()) {
                if (!entry.getKey().equals(Constants.JCR_CREATED) && !entry.getKey().equals(Constants.JCR_LASTMODIFIED)) {
                    externalFile.getProperties().put(entry.getKey(), entry.getValue());
                }
            }
        }

        return externalFile;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public boolean isCalculateFileSize() {
        return calculateFileSize;
    }

    public void setCalculateFileSize(boolean calculateFileSize) {
        this.calculateFileSize = calculateFileSize;
    }
}
