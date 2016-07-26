package org.jahia.modules.external.elvis;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * @author dgaillard
 */
public class ElvisUtils {
    private static Logger logger = LoggerFactory.getLogger(ElvisUtils.class);

    /**
     * To encode and decode characters not allowed by DXM and allowed by Elvis
     * @param path      : path to encode/decode
     * @param encode    : true if you want to encode
     * @return encoded/decoded path
     */
    public static String encodeDecodeSpecialCharacters(String path, boolean encode) {
        if (encode) {
            return (path!=null)? StringUtils.replaceEachRepeatedly(path.replace("%", "%25"), new String[]{"[","]", ElvisConstants.EPF_FORMAT}, new String[]{"%5B","%5D", ElvisConstants.EPF_FORMAT_ENCODED}):null;
        } else {
            return (path!=null && path.contains("%"))?StringUtils.replaceEachRepeatedly(path, new String[]{"%5B","%5D", ElvisConstants.EPF_FORMAT_ENCODED}, new String[]{"[","]", ElvisConstants.EPF_FORMAT}).replace("%25", "%"):path;
        }
    }

    /**
     * To rebuild the path of the file from which is issue the preview file
     * @param path  : path of the preview file
     * @return      : return path of the original file
     */
    public static String getOriginalFilePath(String path) {
        return StringUtils.substringBefore(path, ElvisConstants.EPF_FORMAT) + "." + StringUtils.substringBeforeLast(StringUtils.substringAfterLast(path, "_"), ".");
    }
}
