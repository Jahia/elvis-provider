package org.jahia.modules.external.elvis.decorator;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.LazyPropertyIterator;
import org.jahia.services.content.decorator.JCRMountPointNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import java.util.Map;

/**
 * @author dgaillard
 */
public class ElvisMountPointNode extends JCRMountPointNode {
    private class FilteredIterator extends LazyPropertyIterator {
        public FilteredIterator() throws RepositoryException {
            super(ElvisMountPointNode.this, ElvisMountPointNode.this.getSession().getLocale());
        }

        public FilteredIterator(String singlePattern) throws RepositoryException {
            super(ElvisMountPointNode.this, ElvisMountPointNode.this.getSession().getLocale(),
                    singlePattern);
        }

        public FilteredIterator(String[] patternArray) throws RepositoryException {
            super(ElvisMountPointNode.this, ElvisMountPointNode.this.getSession().getLocale(),
                    patternArray);
        }

        @Override
        public boolean hasNext() {
            while (super.hasNext()) {
                try {
                    if (!canGetProperty(tempNext.getName())) {
                        tempNext = null;
                    } else {
                        return true;
                    }
                } catch (RepositoryException e) {
                    tempNext = null;
                    logger.error("Cannot read property", e);
                }
            }
            return false;
        }
    }

    private transient static Logger logger = LoggerFactory.getLogger(ElvisMountPointNode.class);

    private static final String PROTECTED_PROPERTY = "password";

    public ElvisMountPointNode(JCRNodeWrapper node) {
        super(node);
    }

    private boolean canGetProperty(String propertyName) throws RepositoryException {
        return getSession().isSystem() || !PROTECTED_PROPERTY.equals(propertyName);
    }

    @Override
    public PropertyIterator getProperties() throws RepositoryException {
        // no filtering for system sessions
        return getSession().isSystem() ? super.getProperties() : new FilteredIterator();
    }

    @Override
    public PropertyIterator getProperties(String s) throws RepositoryException {
        return getSession().isSystem() ? super.getProperties(s) : new FilteredIterator(s);
    }

    @Override
    public PropertyIterator getProperties(String[] strings) throws RepositoryException {
        return getSession().isSystem() ? super.getProperties(strings) : new FilteredIterator(strings);
    }

    @Override
    public Map<String, String> getPropertiesAsString() throws RepositoryException {
        return getSession().isSystem() ? super.getPropertiesAsString() : Maps.filterKeys(super.getPropertiesAsString(),
                new Predicate<String>() {
                    @Override
                    public boolean apply(String input) {
                        try {
                            return canGetProperty(input);
                        } catch (RepositoryException e) {
                            return false;
                        }
                    }
                });
    }

    @Override
    public JCRPropertyWrapper getProperty(String s) throws PathNotFoundException, RepositoryException {
        if (!canGetProperty(s)) {
            throw new PathNotFoundException(s);
        }

        return super.getProperty(s);
    }

    @Override
    public String getPropertyAsString(String name) {
        try {
            return canGetProperty(name) ? super.getPropertyAsString(name) : null;
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean hasProperty(String s) throws RepositoryException {
        return super.hasProperty(s) && canGetProperty(s);
    }
}
