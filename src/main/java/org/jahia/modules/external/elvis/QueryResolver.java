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
package org.jahia.modules.external.elvis;

import org.apache.jackrabbit.commons.query.qom.Operator;
import org.jahia.modules.external.ExternalQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.query.qom.*;

/**
 * @author Damien GAILLARD
 */
public class QueryResolver {
    private static final Logger logger = LoggerFactory.getLogger(QueryResolver.class);

    private final StringBuffer TRUE = new StringBuffer("true");
    private final StringBuffer FALSE = new StringBuffer("false");

    ElvisDataSource dataSource;
    ExternalQuery query;

    public QueryResolver(ElvisDataSource dataSource, ExternalQuery query) {
        this.dataSource = dataSource;
        this.query = query;
    }

    public String resolve() throws RepositoryException {
        StringBuffer buff = new StringBuffer();

        Source source = query.getSource();
        if (source instanceof Join) {
            logger.debug("Join not supported in Elvis queries");
            return null;
        }

        if (query.getConstraint() != null) {
            StringBuffer buffer = addConstraint(query.getConstraint());
            if (buffer == FALSE) {
                return null;
            } else if (buffer != TRUE) {
//                buff.append(" WHERE ");
                buff.append(buffer);
            }
        }

        if (query.getOrderings() != null) {
            boolean isFirst = true;
            StringBuffer tmpBuf = new StringBuffer();
            for (Ordering ordering : query.getOrderings()) {
                tmpBuf.setLength(0);
                try {
                    addOperand(tmpBuf, ordering.getOperand());
                    if (isFirst) {
                        buff.append("&sort=");
                        isFirst = false;
                    } else {
                        buff.append(",");
                    }
                    buff.append(tmpBuf);
                    String order = ordering.getOrder();
                    if (QueryObjectModelConstants.JCR_ORDER_ASCENDING.equals(order)) {
                        buff.append("-").append("asc");
                    } else if (QueryObjectModelConstants.JCR_ORDER_DESCENDING.equals(order)) {
                        buff.append("-").append("desc");
                    }
//                    if (tmpBuf.toString().equals(" myscore ")) {
//                        buff.insert(buff.indexOf(" FROM"), ", SCORE() as myscore ");
//                    }
                } catch (NotMappedElvisProperty ignore) { //ignore ordering by not mapped properties
                }
            }
        }
        return buff.toString();
    }

    private StringBuffer addConstraint(Constraint constraint) throws RepositoryException {
        StringBuffer buff = new StringBuffer();
        if (constraint instanceof Or) {
            Or c = (Or) constraint;
            StringBuffer constraint1 = addConstraint(c.getConstraint1());
            StringBuffer constraint2 = addConstraint(c.getConstraint2());
            if (constraint1 == TRUE || constraint2 == TRUE) {
                return TRUE;
            }
            if (constraint1 == FALSE) {
                return constraint2;
            }
            if (constraint2 == FALSE) {
                return constraint1;
            }
            buff.append("(");
            buff.append(constraint1);
            buff.append("||");
            buff.append(constraint2);
            buff.append(")");
        } else if (constraint instanceof And) {
            And c = (And) constraint;
            StringBuffer constraint1 = addConstraint(c.getConstraint1());
            StringBuffer constraint2 = addConstraint(c.getConstraint2());
            if (constraint1 == FALSE || constraint2 == FALSE) {
                return FALSE;
            }
            if (constraint1 == TRUE) {
                return constraint2;
            }
            if (constraint2 == TRUE) {
                return constraint1;
            }
            buff.append("(");
            buff.append(constraint1);
            buff.append("AND");
            buff.append(constraint2);
            buff.append(")");
        } else if (constraint instanceof Comparison) {
            Comparison c = (Comparison) constraint;
            buff.append("(");
            try {
                int pos = buff.length();
                addOperand(buff, c.getOperand1());
                String op1 = buff.substring(pos);
                buff.setLength(pos);

                pos = buff.length();
                addOperand(buff, c.getOperand2());
                String op2 = buff.substring(pos);
                buff.setLength(pos);

                Operator operator = Operator.getOperatorByName(c.getOperator());
                buff.append(operator.formatSql(op1, op2));
            } catch (NotMappedElvisProperty e) {
                return FALSE;
            }
            buff.append(")");
        } else if (constraint instanceof PropertyExistence) {
//            PropertyExistence c = (PropertyExistence) constraint;
//            CmisPropertyMapping propertyMapping = cmisType.getPropertyByJCR(c.getPropertyName());
//            if (propertyMapping == null)
                return FALSE;
//            else
//                buff.append(" (").append(propertyMapping.getQueryName()).append(" IS NOT NULL) ");
        } else if (constraint instanceof SameNode) {
//            try {
//                SameNode c = (SameNode) constraint;
//                String path = c.getPath();
//                CmisObject object = dataSource.getCmisSession().getObjectByPath(path);
//                buff.append(" (cmis:objectId='").append(object.getId()).append("') ");
//            } catch (CmisObjectNotFoundException e) {
                return FALSE;
//            }
        } else if (constraint instanceof Not) {
            Not c = (Not) constraint;
            StringBuffer constraint1 = addConstraint(c.getConstraint());
            if (constraint1 == FALSE) {
                return TRUE;
            }
            if (constraint1 == TRUE) {
                return FALSE;
            }
            buff.append(" NOT(");
            buff.append(constraint1);
            buff.append(")");
        } else if (constraint instanceof ChildNode) {
            ChildNode c = (ChildNode) constraint;
            String parentPath = c.getParentPath();
            buff.append("folderPath:'").append(parentPath).append("'");
        } else if (constraint instanceof DescendantNode) {
//            ChildNode c = (ChildNode) constraint;
//            String parentPath = c.getParentPath();
//            buff.append("folderPath:'").append(parentPath).append("') ");
            return FALSE;
        } else if (constraint instanceof FullTextSearch) {
            FullTextSearch c = (FullTextSearch) constraint;
            buff.append("textContent:'");
            addOperand(buff, c.getFullTextSearchExpression());
            buff.append("'");
        }
        return buff;
    }

    private void addOperand(StringBuffer buff, DynamicOperand operand) throws RepositoryException {
        if (operand instanceof LowerCase) {
            throw new UnsupportedRepositoryOperationException("Unsupported operand type LowerCase");
        } else if (operand instanceof UpperCase) {
            throw new UnsupportedRepositoryOperationException("Unsupported operand type UpperCase");
        } else if (operand instanceof Length) {
            throw new UnsupportedRepositoryOperationException("Unsupported operand type Length");
        } else if (operand instanceof NodeName) {
            buff.append("name");
        } else if (operand instanceof NodeLocalName) {
            buff.append("filename");
        } else if (operand instanceof PropertyValue) {
            PropertyValue o = (PropertyValue) operand;
            throw new NotMappedElvisProperty(o.getPropertyName());
//            CmisPropertyMapping propertyByJCR = cmisType.getPropertyByJCR(o.getPropertyName());
//            if (propertyByJCR == null)
//                throw new NotMappedCmisProperty(o.getPropertyName());
//            buff.append(propertyByJCR.getQueryName());
        } else if (operand instanceof FullTextSearchScore) {
            throw new NotMappedElvisProperty();
//            buff.append(" myscore ");
        }
    }

    private void addOperand(StringBuffer buff, StaticOperand operand) throws RepositoryException {
        if (operand instanceof Literal) {
            Value val = ((Literal) operand).getLiteralValue();
            switch (val.getType()) {
                case PropertyType.BINARY:
                case PropertyType.DOUBLE:
                case PropertyType.DECIMAL:
                case PropertyType.LONG:
                case PropertyType.BOOLEAN:
                    buff.append(val.getString());
                    break;
                case PropertyType.STRING:
                    buff.append("'").append(escapeString(val.getString())).append("'");
                    break;
                case PropertyType.DATE:
                    buff.append(" TIMESTAMP '").append(val.getString()).append("'");
                    break;
                case PropertyType.NAME:
                case PropertyType.PATH:
                case PropertyType.REFERENCE:
                case PropertyType.WEAKREFERENCE:
                case PropertyType.URI:
                    // TODO implement valid support for this operand types
                    buff.append("'").append(val.getString()).append("'");
                    break;
                default:
                    throw new UnsupportedRepositoryOperationException("Unsupported operand value type " + val.getType());
            }
        } else {
            throw new UnsupportedRepositoryOperationException("Unsupported operand type " + operand.getClass());
        }
    }

    private String escapeString(String string) {
        return string.replace("\\", "\\\\").replace("'", "\\'");
    }
}
