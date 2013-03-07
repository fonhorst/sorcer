package sorcer.jini.lookup;

import net.jini.core.entry.Entry;
import sorcer.jini.lookup.entry.SorcerServiceInfo;

public class AttributesUtil {

	static public String getGroups(Entry[] attributeSets) {
		if (attributeSets != null) {
			if (attributeSets.length > 0) {
				for (int i = 0; i < attributeSets.length - 1; i++) {
					if (attributeSets[i] instanceof SorcerServiceInfo) {
						return ((SorcerServiceInfo) attributeSets[i]).groups;
					}
				}
			}
		}
		return null;
	}

	static public String getProviderName(Entry[] attributeSets) {
		if (attributeSets != null) {
			if (attributeSets.length > 0) {
				for (int i = 0; i < attributeSets.length - 1; i++) {
					if (attributeSets[i] instanceof SorcerServiceInfo) {
						return ((SorcerServiceInfo) attributeSets[i]).providerName;
					}
				}
			}
		}
		return null;
	}

	static public String getHostName(Entry[] attributeSets) {
		if (attributeSets != null) {
			if (attributeSets.length > 0) {
				for (int i = 0; i < attributeSets.length - 1; i++) {
					if (attributeSets[i] instanceof SorcerServiceInfo) {
						return ((SorcerServiceInfo) attributeSets[i]).hostName;
					}
				}
			}
		}
		return null;
	}

	static public String getHostAddress(Entry[] attributeSets) {
		if (attributeSets != null) {
			if (attributeSets.length > 0) {
				for (int i = 0; i < attributeSets.length - 1; i++) {
					if (attributeSets[i] instanceof SorcerServiceInfo) {
						return ((SorcerServiceInfo) attributeSets[i]).hostAddress;
					}
				}
			}
		}
		return null;
	}

	static public String getUserDir(Entry[] attributeSets) {
		if (attributeSets != null) {
			if (attributeSets.length > 0) {
				for (int i = 0; i < attributeSets.length - 1; i++) {
					if (attributeSets[i] instanceof SorcerServiceInfo) {
						return ((SorcerServiceInfo) attributeSets[i]).userDir;
					}
				}
			}
		}
		return null;
	}

	static public String[] getPublishedServices(Entry[] attributeSets) {
		if (attributeSets != null) {
			if (attributeSets.length > 0) {
				for (int i = 0; i < attributeSets.length - 1; i++) {
					if (attributeSets[i] instanceof SorcerServiceInfo) {
						return ((SorcerServiceInfo) attributeSets[i]).publishedServices;
					}
				}
			}
		}
		return null;
	}
}