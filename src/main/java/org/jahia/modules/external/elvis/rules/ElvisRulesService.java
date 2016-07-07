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

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.elvis.ElvisConstants;
import org.jahia.modules.external.elvis.ElvisDataSource;
import org.jahia.modules.external.elvis.ElvisUtils;
import org.jahia.modules.external.elvis.admin.MountPointFactory;
import org.jahia.modules.external.elvis.communication.BaseElvisActionCallback;
import org.jahia.modules.external.elvis.communication.ElvisSession;
import org.jahia.modules.external.elvis.decorator.ElvisMountPointNode;
import org.jahia.services.content.*;
import org.jahia.services.content.rules.ChangedPropertyFact;
import org.jahia.settings.SettingsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.List;

/**
 * @author dgaillard
 */
public class ElvisRulesService {
    private static final Logger logger = LoggerFactory.getLogger(ElvisRulesService.class);

    private JCRTemplate jcrTemplate;
    private SessionFactory hibernateSessionFactory;

    /**
     * Method call by the rule every time a node is created with a property of type weakreference or reference,
     * also call every time a property of this type is modified
     * @param changedPropertyFact   : the modified property
     * @throws RepositoryException
     */
    public void writeUsageInElvis(ChangedPropertyFact changedPropertyFact) throws RepositoryException {
        final JCRNodeWrapper node = changedPropertyFact.getNode().getNode();
        final JCRNodeWrapper referencedNode = changedPropertyFact.getNodeValue().getNode();
        final String nodeIdentifier = changedPropertyFact.getNode().getIdentifier();
        final String propertyName = changedPropertyFact.getName();

        final Session hibernateSession = hibernateSessionFactory.openSession();
        List<ElvisUsageMapping> list = hibernateSession.createQuery("from ElvisUsageMapping where componentIdentifier=:componentIdentifier and propertyName=:propertyName")
                .setString("componentIdentifier", nodeIdentifier).setString("propertyName", propertyName).list();
        if (!list.isEmpty()) {
            removeUsageFromElvis(hibernateSession, list);
        }

        if (referencedNode.isNodeType(ElvisConstants.ELVISMIX_FILE)) {
            jcrTemplate.doExecuteWithSystemSession(new JCRCallback<Object>() {
                @Override
                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    String referencedNodePath = StringUtils.substringAfter(referencedNode.getPath(), referencedNode.getProvider().getMountPoint());
                    if (referencedNode.isNodeType(ElvisConstants.ELVISMIX_PREVIEW_FILE)) {
                        referencedNodePath = ElvisUtils.getOriginalFilePath(referencedNodePath);
                    }
                    final String assetPath = ElvisUtils.encodeDecodeSpecialCharacters(referencedNodePath, false);

                    ElvisMountPointNode elvisMountPointNode = (ElvisMountPointNode) session.getNodeByIdentifier(referencedNode.getProvider().getKey());
                    if (elvisMountPointNode.hasProperty(MountPointFactory.WRITE_USAGE_IN_ELVIS) && elvisMountPointNode.getProperty(MountPointFactory.WRITE_USAGE_IN_ELVIS).getBoolean()) {
                        final int siteURLPortOverride = SettingsBean.getInstance().getSiteURLPortOverride();
                        String baseUrl = "http" + (siteURLPortOverride == 443 ? "s" : "") + "://" + node.getResolveSite().getServerName() +
                                ((siteURLPortOverride != 0 && siteURLPortOverride != 80 && siteURLPortOverride != 443) ? ":" + siteURLPortOverride : "");
                        final String pageUrl = baseUrl + JCRContentUtils.getParentOfType(node, Constants.JAHIANT_PAGE).getUrl();
                        addEntryToDB(hibernateSession, new ElvisUsageMapping(nodeIdentifier, propertyName, assetPath, elvisMountPointNode.getIdentifier(), pageUrl));

                        ExternalContentStoreProvider externalContentStoreProvider = (ExternalContentStoreProvider) elvisMountPointNode.getMountProvider();
                        ElvisDataSource elvisDataSource = (ElvisDataSource) externalContentStoreProvider.getDataSource();
                        final ElvisSession elvisSession = elvisDataSource.getElvisSession();

                        boolean updatedMetaData;
                        updatedMetaData = elvisSession.execute(new BaseElvisActionCallback<Boolean>(elvisSession) {
                            @Override
                            public Boolean doInElvis() throws Exception {
                                return elvisSession.updateBulk(assetPath, pageUrl, true);
                            }
                        });
                        if (!updatedMetaData) {
                            logger.error("Could not update information in Elvis");
                        }
                    }
                    return null;
                }
            });
        }

        hibernateSession.close();
    }

    /**
     * Method call every time a node which is registered in the database is deleted
     * @param deletedNodeIdentifier : UUID of the deleted node
     * @throws RepositoryException
     */
    public void removeUsageInElvis(String deletedNodeIdentifier) throws RepositoryException {
        Session hibernateSession = hibernateSessionFactory.openSession();
        List<ElvisUsageMapping> list = hibernateSession.createQuery("from ElvisUsageMapping where componentIdentifier=:componentIdentifier")
                .setString("componentIdentifier", deletedNodeIdentifier).list();
        removeUsageFromElvis(hibernateSession, list);
        hibernateSession.close();
    }

    /**
     * Check is the property set on the node is a type weakreference/reference.
     * @param changedPropertyFact   : the modified property
     * @return boolean
     * @throws RepositoryException
     */
    public boolean checkCondition(ChangedPropertyFact changedPropertyFact) throws RepositoryException {
        return (changedPropertyFact.getType() == PropertyType.REFERENCE || changedPropertyFact.getType() == PropertyType.WEAKREFERENCE);
    }

    /**
     * Check if the deleted node was using file(s) from Elvis
     * @param deletedNodeIdentifier : UUID of the deleted node
     * @return boolean
     * @throws RepositoryException
     */
    public boolean checkCondition(String deletedNodeIdentifier) throws RepositoryException {
        Session hibernateSession = hibernateSessionFactory.openSession();
        List<ElvisUsageMapping> list = hibernateSession.createQuery("from ElvisUsageMapping where componentIdentifier=:componentIdentifier")
                .setString("componentIdentifier", deletedNodeIdentifier).list();
        hibernateSession.close();
        return !list.isEmpty();
    }

    public void setJcrTemplate(JCRTemplate jcrTemplate) {
        this.jcrTemplate = jcrTemplate;
    }

    public void setHibernateSessionFactory(SessionFactory hibernateSessionFactory) {
        this.hibernateSessionFactory = hibernateSessionFactory;
    }

    private void addEntryToDB(Session session, ElvisUsageMapping elvisUsageMapping) {
        session.beginTransaction();
        session.save(elvisUsageMapping);
        session.getTransaction().commit();
    }

    private void removeUsageFromElvis(Session hibernateSession, List<ElvisUsageMapping> list) throws RepositoryException {
        hibernateSession.beginTransaction();
        for (ElvisUsageMapping elvisUsageMapping : list) {
            final String mountPointIdentifier = elvisUsageMapping.getMountPointIdentifier();
            final String assetPath = elvisUsageMapping.getAssetPath();
            final String pageUrl = elvisUsageMapping.getPageUrl();
            hibernateSession.delete(elvisUsageMapping);
            jcrTemplate.doExecuteWithSystemSession(new JCRCallback<Object>() {
                @Override
                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    ElvisMountPointNode elvisMountPointNode = (ElvisMountPointNode) session.getNodeByIdentifier(mountPointIdentifier);
                    if (elvisMountPointNode.hasProperty(MountPointFactory.WRITE_USAGE_IN_ELVIS) && elvisMountPointNode.getProperty(MountPointFactory.WRITE_USAGE_IN_ELVIS).getBoolean()) {
                        ExternalContentStoreProvider externalContentStoreProvider = (ExternalContentStoreProvider) elvisMountPointNode.getMountProvider();
                        ElvisDataSource elvisDataSource = (ElvisDataSource) externalContentStoreProvider.getDataSource();
                        final ElvisSession elvisSession = elvisDataSource.getElvisSession();

                        boolean updatedMetaData;
                        updatedMetaData = elvisSession.execute(new BaseElvisActionCallback<Boolean>(elvisSession) {
                            @Override
                            public Boolean doInElvis() throws Exception {
                                return elvisSession.updateBulk(assetPath, pageUrl, false);
                            }
                        });
                        if (!updatedMetaData) {
                            logger.error("Could not update information in Elvis");
                        }
                    }
                    return null;
                }
            });
        }
        hibernateSession.getTransaction().commit();
    }
}
