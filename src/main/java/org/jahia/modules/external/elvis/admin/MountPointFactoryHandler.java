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

import org.jahia.modules.external.admin.mount.AbstractMountPointFactoryHandler;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.utils.i18n.Messages;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.webflow.execution.RequestContext;

import javax.jcr.RepositoryException;
import java.util.Locale;

/**
 * @author Damien GAILLARD
 */
public class MountPointFactoryHandler extends AbstractMountPointFactoryHandler<MountPointFactory> {
    private static final Logger logger = LoggerFactory.getLogger(MountPointFactoryHandler.class);
    private static final String BUNDLE = "resources.elvis-provider";

    private MountPointFactory mountPointFactory;

    public void init(RequestContext requestContext) {
        mountPointFactory = new MountPointFactory();
        try {
            super.init(requestContext, mountPointFactory);
        } catch (RepositoryException e) {
            logger.error("Error retrieving mount point", e);
        }
        requestContext.getFlowScope().put("elvisFactory", mountPointFactory);
    }

    public String getFolderList() {
        JSONObject result = new JSONObject();
        try {
            JSONArray folders = JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<JSONArray>() {
                @Override
                public JSONArray doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    return getSiteFolders(session.getWorkspace());
                }
            });
            result.put("folders", folders);
        } catch (RepositoryException e) {
            logger.error("Error trying to retrieve local folders", e);
        } catch (JSONException e) {
            logger.error("Error trying to construct JSON from local folders", e);
        }

        return result.toString();
    }

    public Boolean save(MessageContext messageContext) {
        Locale locale = LocaleContextHolder.getLocale();
        try {
            boolean available = super.save(mountPointFactory);
            if (available) {
                return true;
            } else {
                MessageBuilder messageBuilder = new MessageBuilder().warning().defaultText(Messages.get(BUNDLE, "serverSettings.elvis.mountPointFactory.save.unavailable", locale));
                messageContext.addMessage(messageBuilder.build());
            }
        } catch (RepositoryException e) {
            logger.error("Error saving mount point " + mountPointFactory.getName(), e);
            MessageBuilder messageBuilder = new MessageBuilder().error().defaultText(Messages.get(BUNDLE, "serverSettings.elvis.mountPointFactory.save.error", locale));
            messageContext.addMessage(messageBuilder.build());
        }
        return false;
    }
}
