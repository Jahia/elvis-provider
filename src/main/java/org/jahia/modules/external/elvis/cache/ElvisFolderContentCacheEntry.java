package org.jahia.modules.external.elvis.cache;

import org.jahia.modules.external.elvis.FilesDataSource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author dgaillard
 */
public class ElvisFolderContentCacheEntry implements Serializable {
    private static final long serialVersionUID = 4674738316873624627L;

    private String path;
    private List<ElvisItemCacheEntry> folders;
    private List<ElvisItemCacheEntry> files;

    public ElvisFolderContentCacheEntry(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public List<FilesDataSource.ExternalFile> getFoldersAsExternalFile() {
        List<FilesDataSource.ExternalFile> folders = new ArrayList<>();
        for (ElvisItemCacheEntry elvisItemCacheEntry : this.folders) {
            folders.add(elvisItemCacheEntry.getAsExternalFile());
        }

        return folders;
    }

    public List<FilesDataSource.ExternalFile> getFilesAsExternalFile() {
        List<FilesDataSource.ExternalFile> files = new ArrayList<>();
        for (ElvisItemCacheEntry elvisItemCacheEntry : this.files) {
            files.add(elvisItemCacheEntry.getAsExternalFile());
        }

        return files;
    }

    public void setFoldersElvisItemCacheEntry(List<FilesDataSource.ExternalFile> folders) {
        this.folders = new ArrayList<>();
        for (FilesDataSource.ExternalFile externalFolder : folders) {
            this.folders.add(new ElvisItemCacheEntry(externalFolder));
        }
    }

    public void setFilesElvisItemCacheEntry(List<FilesDataSource.ExternalFile> files) {
        this.files = new ArrayList<>();
        for (FilesDataSource.ExternalFile externalFile : files) {
            this.files.add(new ElvisItemCacheEntry(externalFile));
        }
    }
}
