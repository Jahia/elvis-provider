package org.jahia.modules.external.elvis;

import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRStoreProvider;
import org.jahia.services.content.ProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.jcr.RepositoryException;

/**
 * @author Damien GAILLARD
 */
public class ElvisProviderFactory implements ProviderFactory, ApplicationContextAware, InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(ElvisProviderFactory.class);

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {}

    @Override
    public String getNodeTypeName() {
        return "elvisnt:mountPoint";
    }

    @Override
    public JCRStoreProvider mountProvider(JCRNodeWrapper mountPoint) throws RepositoryException {
        if (logger.isDebugEnabled())
            logger.info("Elvis external provider module initialization: mountPoint.getPath() - " + mountPoint.getPath() + ", name - " + mountPoint.getName());

        // Define the provider basing on the mount point parameters
        ExternalContentStoreProvider externalContentStoreProvider = (ExternalContentStoreProvider) SpringContextSingleton.getBean("ExternalContentStoreProvider");
        externalContentStoreProvider.setKey(mountPoint.getIdentifier());
        externalContentStoreProvider.setMountPoint(mountPoint.getPath());

        // Define the datasource using the credentials defined in the mount point

        // Set provider and configuration object in the datasource

        // Start the datasource

        // Finalize the provider setup with datasource and some JCR parameters

        if (logger.isDebugEnabled())
            logger.info("Elvis external provider module initialized");
        return externalContentStoreProvider;
    }
}
