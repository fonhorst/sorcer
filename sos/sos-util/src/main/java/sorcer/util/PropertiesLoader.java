package sorcer.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Rafał Krupiński
 */
public class PropertiesLoader {
	public Map<String, String> loadAsMap(Class c) {
		return loadAsMap(c.getName() + ".properties", c.getClassLoader());
	}

	public Map<String, String> loadAsMap(String path, ClassLoader cl) {
		return toMap(loadAsProperties(path, cl));
	}

	public Properties loadAsProperties(String path, ClassLoader cl) {
		InputStream stream = cl.getResourceAsStream(path);
		if (stream == null) {
			throw new IllegalArgumentException("Could not load file " + path);
		}
		Properties result = new Properties();
		try {
			result.load(stream);
		} catch (IOException e) {
			throw new IllegalArgumentException("Could not load file " + path, e);
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				//ignore
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> toMap(Properties properties) {
		Map<String, String> result = new HashMap<String, String>();
		result.putAll((Map) properties);
		return result;
	}
}
