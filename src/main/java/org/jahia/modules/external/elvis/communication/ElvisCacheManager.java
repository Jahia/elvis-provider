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
package org.jahia.modules.external.elvis.communication;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import org.jahia.services.cache.CacheHelper;
import org.jahia.services.cache.ModuleClassLoaderAwareCacheEntry;
import org.jahia.services.cache.ehcache.EhCacheProvider;
import org.json.JSONObject;

/**
 * @author dgaillard
 */
public class ElvisCacheManager {
    private static final String ELVIS_SEARCH_RESULT_CACHE = "ElvisSearchResultCache";

    private EhCacheProvider bigEhCacheProvider;
    private CacheManager cacheManager;
    private Ehcache lastSearchResultCache;

    public ElvisCacheManager() {}

    public void start() {
        cacheManager = bigEhCacheProvider.getCacheManager();
        lastSearchResultCache = cacheManager.getCache(ELVIS_SEARCH_RESULT_CACHE);

        if (lastSearchResultCache == null) {
            CacheConfiguration cacheConfiguration = new CacheConfiguration();
            cacheConfiguration.setName(ELVIS_SEARCH_RESULT_CACHE);
            cacheConfiguration.setTimeToLiveSeconds(30);
            // Create a new cache with the configuration
            Ehcache cache = new Cache(cacheConfiguration);
            cache.setName(ELVIS_SEARCH_RESULT_CACHE);
            // Cache name has been set now we can initialize it by putting it in the manager.
            // Only Cache manager is initializing caches.
            lastSearchResultCache = cacheManager.addCacheIfAbsent(cache);
        }
    }

    public void stop() {
        // flush
        if (lastSearchResultCache != null) {
            lastSearchResultCache.removeAll();
        }

        cacheManager.removeCache(ELVIS_SEARCH_RESULT_CACHE);
    }

    public void cacheLastSearchResult(JSONObject jsonObject, String cacheKey) {
        ModuleClassLoaderAwareCacheEntry cacheEntry = new ModuleClassLoaderAwareCacheEntry(jsonObject, "elvis-provider");
        lastSearchResultCache.put(new Element(cacheKey, cacheEntry));
    }

    public JSONObject getLastSearchResultCacheEntry(String cacheKey) {
        return (JSONObject) CacheHelper.getObjectValue(lastSearchResultCache, cacheKey);
    }

    public void setBigEhCacheProvider(EhCacheProvider bigEhCacheProvider) {
        this.bigEhCacheProvider = bigEhCacheProvider;
    }
}
