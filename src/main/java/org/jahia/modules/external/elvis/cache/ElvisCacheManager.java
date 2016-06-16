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
package org.jahia.modules.external.elvis.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import org.jahia.modules.external.elvis.FilesDataSource;
import org.jahia.services.cache.CacheHelper;
import org.jahia.services.cache.ModuleClassLoaderAwareCacheEntry;
import org.jahia.services.cache.ehcache.EhCacheProvider;

/**
 * @author dgaillard
 */
public class ElvisCacheManager {
    private static final String ELVIS_ITEM_CACHE = "ElvisItemCache";
    private static final String ELVIS_FOLDER_CONTENT_CACHE = "ElvisFolderContentCache";

    private String mountPointPath;
    private Ehcache itemCache;
    private Ehcache folderContentCache;

    public ElvisCacheManager(String mountPointPath, EhCacheProvider ehCacheProvider) {
        this.mountPointPath = mountPointPath;

        final CacheManager cacheManager = ehCacheProvider.getCacheManager();
        itemCache = cacheManager.getCache(ELVIS_ITEM_CACHE);
        if (itemCache == null) {
            itemCache = createElvisCache(cacheManager, ELVIS_ITEM_CACHE);
        } else {
            // TODO to remove, for dev only
            cacheManager.removeCache(ELVIS_ITEM_CACHE);
            itemCache = createElvisCache(cacheManager, ELVIS_ITEM_CACHE);
//            itemCache.removeAll();
        }
        folderContentCache = cacheManager.getCache(ELVIS_FOLDER_CONTENT_CACHE);
        if (folderContentCache == null) {
            folderContentCache = createElvisCache(cacheManager, ELVIS_FOLDER_CONTENT_CACHE);
        } else  {
            // TODO to remove, for dev only
            cacheManager.removeCache(ELVIS_FOLDER_CONTENT_CACHE);
            folderContentCache = createElvisCache(cacheManager, ELVIS_FOLDER_CONTENT_CACHE);
//            folderContentCache.removeAll();
        }
    }

    public void flushAll(){
        // flush
        if (itemCache != null) {
            itemCache.removeAll();
        }
        if (folderContentCache != null) {
            folderContentCache.removeAll();
        }
    }

    public void cacheItem(ElvisItemCacheEntry elvisItemCacheEntry) {
        ModuleClassLoaderAwareCacheEntry cacheEntry = new ModuleClassLoaderAwareCacheEntry(elvisItemCacheEntry, "elvis-provider");
        itemCache.put(new Element(elvisItemCacheEntry.getPath(), cacheEntry));
    }

    public void cacheFolderContent(ElvisFolderContentCacheEntry elvisFolderContentCacheEntry) {
        ModuleClassLoaderAwareCacheEntry cacheEntry = new ModuleClassLoaderAwareCacheEntry(elvisFolderContentCacheEntry, "elvis-provider");
        folderContentCache.put(new Element(elvisFolderContentCacheEntry.getPath(), cacheEntry));
    }

    public ElvisItemCacheEntry getItemCacheEntry(String path) {
        return (ElvisItemCacheEntry) CacheHelper.getObjectValue(itemCache, path);
    }

    public ElvisItemCacheEntry getItemCacheEntryByExternalFile(FilesDataSource.ExternalFile externalFile) {
        return (ElvisItemCacheEntry) CacheHelper.getObjectValue(itemCache, externalFile.getPath());
    }

    public ElvisFolderContentCacheEntry getFolderContentCacheEntry(String path) {
        return (ElvisFolderContentCacheEntry) CacheHelper.getObjectValue(folderContentCache, path);
    }

    private Ehcache createElvisCache(CacheManager cacheManager, String cacheName) {
        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setName(cacheName);
        cacheConfiguration.setTimeToLiveSeconds(300);
        // Create a new cache with the configuration
        Ehcache cache = new Cache(cacheConfiguration);
        cache.setName(cacheName);
        // Cache name has been set now we can initialize it by putting it in the manager.
        // Only Cache manager is initializing caches.
        return cacheManager.addCacheIfAbsent(cache);
    }
}
