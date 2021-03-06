package sorcer.util;

import java.util.Map;
import java.util.Properties;

/**
 * Helper class for accessing system properties. Allows to use Properties or Map<String, String> object, or the System
 * properties if the former is not provided.
 *
 * @author Rafał Krupiński
 */
public class JavaSystemProperties {
    public static final String RMI_SERVER_CODEBASE = "java.rmi.server.codebase";
    public static final String RMI_SERVER_USE_CODEBASE_ONLY = "java.rmi.server.useCodebaseOnly";
    public static final String SECURITY_POLICY = "java.security.policy";
    public static final String UTIL_LOGGING_CONFIG_FILE = "java.util.logging.config.file";
    public static final String NET_PREFER_IPV4_STACK = "java.net.preferIPv4Stack";
    public static final String PROTOCOL_HANDLER_PKGS = "java.protocol.handler.pkgs";
    public static final String CLASS_PATH = "java.class.path";
    public static final String USER_DIR = "user.dir";

    /**
     * Get the property from map or from system properties.
     *
     * @param key        property key
     * @param properties map od properties, use system properties if null
     * @return property value
     */
	public static String getProperty(String key, Map<String, String> properties) {
		return getProperty(key, null, properties);
	}

    /**
     * Get the property from Properties object or, if it's null, from the system properties.
     * @param key             property key
     * @param defaultValue    default value returned if property is not defined
     * @param properties      properties to search they key in. If null, the system properties are used
     * @return property value or default value if the property is not defined
     */
    public static String getProperty(String key, String defaultValue, Properties properties) {
		return getProperty(key, defaultValue, (Map)properties);
	}

    /**
     * Get the property from map or, if map is null, from the system properties.
     * @param key             property key
     * @param defaultValue    default value returned if property is not defined
     * @param properties      Map to search they key in. If null, the system properties are used
     * @return property value or default value if the property is not defined
     */
    public static String getProperty(String key, String defaultValue, Map<String, String> properties) {
		if (properties == null) {
			return System.getProperty(key, defaultValue);
		} else {
			String value = properties.get(key);
			return value != null ? value : defaultValue;
		}
	}
}
