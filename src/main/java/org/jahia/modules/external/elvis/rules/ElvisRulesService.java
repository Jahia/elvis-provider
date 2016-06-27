package org.jahia.modules.external.elvis.rules;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jahia.api.Constants;
import org.jahia.modules.external.elvis.admin.MountPointFactory;
import org.jahia.modules.external.elvis.communication.BaseElvisActionCallback;
import org.jahia.modules.external.elvis.communication.ElvisSession;
import org.jahia.modules.external.elvis.decorator.ElvisMountPointNode;
import org.jahia.services.content.*;
import org.jahia.settings.SettingsBean;
import org.jahia.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * @author dgaillard
 */
public class ElvisRulesService {
    private static final Logger logger = LoggerFactory.getLogger(ElvisRulesService.class);

    private static ElvisRulesService instance;
    private JCRTemplate jcrTemplate;
    private SessionFactory hibernateSessionFactory;

    public ElvisRulesService() {
        super();
    }

    public static synchronized ElvisRulesService getInstance() {
        if (instance == null) {
            instance = new ElvisRulesService();
        }
        return instance;
    }

    public void writeUsageInElvis(final JCRNodeWrapper node, final String propertyName) throws RepositoryException {
        jcrTemplate.doExecuteWithSystemSession(new JCRCallback<Object>() {
            @Override
            public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                JCRNodeWrapper referencedNode = (JCRNodeWrapper) node.getProperty(propertyName).getNode();
                final String assetPath = WebUtils.escapePath(encodeDecodeSpecialCharacters(StringUtils.substringAfter(referencedNode.getPath(), referencedNode.getProvider().getMountPoint()), false));
                ElvisMountPointNode elvisMountPointNode = (ElvisMountPointNode) session.getNodeByIdentifier(referencedNode.getProvider().getKey());
                if (elvisMountPointNode.hasProperty(MountPointFactory.WRITE_USAGE_IN_ELVIS) && elvisMountPointNode.getProperty(MountPointFactory.WRITE_USAGE_IN_ELVIS).getBoolean()) {
                    addEntryToDB(new ElvisUsageMapping(node.getIdentifier(), propertyName, assetPath, elvisMountPointNode.getIdentifier()));
                    final int siteURLPortOverride = SettingsBean.getInstance().getSiteURLPortOverride();
                    String baseUrl = "http" + (siteURLPortOverride == 443 ? "s" : "") + "://" + node.getResolveSite().getServerName() +
                                    ((siteURLPortOverride != 0 && siteURLPortOverride != 80 && siteURLPortOverride != 443) ? ":" + siteURLPortOverride : "");
                    final String pageUrl = baseUrl + JCRContentUtils.getParentOfType(node, Constants.JAHIANT_PAGE).getUrl();
                    final ElvisSession elvisSession = new ElvisSession(elvisMountPointNode.getPropertyAsString(MountPointFactory.URL),
                                                                elvisMountPointNode.getPropertyAsString(MountPointFactory.USER_NAME),
                                                                elvisMountPointNode.getPropertyAsString(MountPointFactory.PASSWORD),
                                                                elvisMountPointNode.getPropertyAsString(MountPointFactory.FILE_LIMIT),
                                                                elvisMountPointNode.getProperty(MountPointFactory.USE_PREVIEW).getBoolean(),
                                                                elvisMountPointNode.getPropertyAsString(MountPointFactory.PREVIEW_SETTINGS),
                                                                elvisMountPointNode.getPropertyAsString(MountPointFactory.FIELD_TO_WRITE_USAGE));

                    elvisSession.initHttp();
                    if (elvisSession.isSessionAvailable()) {
                        elvisSession.execute(new BaseElvisActionCallback<Object>(elvisSession) {
                            @Override
                            public Object doInElvis() throws Exception {
                                elvisSession.writeUsageOnAsset(assetPath, pageUrl);
                                return null;
                            }
                        });
                    }
                    elvisSession.logout();
                }
                return null;
            }
        });
    }

    public void writeUsageInElvis(String deletedNodePath) {
        logger.info(deletedNodePath);

    }

    /**
     * Check is the property set on the node is a type weakreference/reference,
     * then check if the referenced node is type "elvismix:file".
     * @param node
     * @param propertyName
     * @return boolean
     * @throws RepositoryException
     */
    public boolean checkCondition(JCRNodeWrapper node, String propertyName) throws RepositoryException {
        if (node.hasProperty(propertyName)) {
            JCRPropertyWrapper property = node.getProperty(propertyName);
            if (property.getType() == PropertyType.REFERENCE || property.getType() == PropertyType.WEAKREFERENCE) {
                final JCRNodeWrapper referencedNode = (JCRNodeWrapper) property.getNode();
                if (referencedNode.isNodeType("elvismix:file")) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkCondition(String deletedNodeIdentifier) throws RepositoryException {
        logger.info(deletedNodeIdentifier);
        return false;
    }

    public void setJcrTemplate(JCRTemplate jcrTemplate) {
        this.jcrTemplate = jcrTemplate;
    }

    public void setHibernateSessionFactory(SessionFactory hibernateSessionFactory) {
        this.hibernateSessionFactory = hibernateSessionFactory;
    }

    private void addEntryToDB(ElvisUsageMapping elvisUsageMapping) {
        Session session = hibernateSessionFactory.openSession();
        session.beginTransaction();
        session.save(elvisUsageMapping);
        session.getTransaction().commit();
    }

    /**
     * To encode and decode characters not allowed by DXM and allowed by Elvis
     * @param path      : path to encode/decode
     * @param encode    : true if you want to encode
     * @return encoded/decoded path
     */
    private String encodeDecodeSpecialCharacters(String path, boolean encode) {
        if (encode) {
            return (path!=null)?StringUtils.replaceEachRepeatedly(path.replace("%", "%25"), new String[]{"[","]"}, new String[]{"%5B","%5D"}):null;
        } else {
            return (path!=null && path.contains("%"))?StringUtils.replaceEachRepeatedly(path, new String[]{"%5B","%5D"}, new String[]{"[","]"}).replace("%25", "%"):path;
        }
    }
}
