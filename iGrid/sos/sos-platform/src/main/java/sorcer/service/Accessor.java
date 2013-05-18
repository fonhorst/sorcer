/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sorcer.service;

import net.jini.core.lookup.ServiceItem;
import sorcer.core.SorcerConstants;
import sorcer.util.*;

import java.util.logging.Logger;

/**
 * A service accessing facility that allows to find dynamically a network
 * service provider matching its {@link Signature}. This class uses the Factory
 * Method pattern with the {@link DynamicAccessor} interface.
 * 
 * @author Mike Sobolewski
 */
public class Accessor {
	
	protected final static Logger logger = Log.getTestLog();
	
	/**
	 * A factory returning instances of {@link Servicer}s.
	 */
	private static DynamicAccessor accessor;

	public final static String providerName = Sorcer.getProperty(SorcerConstants.S_SERVICE_ACCESSOR_PROVIDER_NAME);
	
	static {
		try {
			logger.fine("* SORCER DynamicAccessor provider: " + providerName);
			if (providerName == null)
				ProviderLookup.init();
			else if (providerName.equals(ProviderAccessor.class.getName()))
				ProviderAccessor.init();
		} catch (AccessorException e) {
			System.err.println("No service accessor available for: " + providerName);
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Returns a servicer matching its {@link Signature} using the particular
	 * factory <code>accessor</code> of this service accessor facility.
	 * 
	 * @param signature
	 *            the signature of requested servicer
	 * @return the requested {@link Servicer}
	 * @throws SignatureException 
	 */
	public static Servicer getServicer(Signature signature)
			throws SignatureException {
		logger.fine("using accessor: " + accessor);
		return accessor.getServicer(signature);
	}

	/**
	 * Returns a service item containing the servicer matching its {@link Signature} 
	 * using the particular factory <code>accessor</code> of this service accessor facility.
	 * 
	 * @param signature
	 *            the signature of requested servicer
	 * @return the requested {@link ServiceItem}
	 * @throws SignatureException 
	 */
	public static ServiceItem getServicerItem(Signature signature) throws SignatureException {
		logger.fine("using accessor: " + accessor);
		return accessor.getServiceItem(signature);
	}

	
	/**
	 * Assigns the provider of service accessor (factory) that implements
	 * {@link DynamicAccessor}.
	 * 
	 * @param provider
	 *            the servicer accessor
	 */
	public static void setAccessor(DynamicAccessor provider) {
		accessor = provider;
	}
	
	
	/**
	 * Returns the current servicer accessor.
	 * 
	 * @return the servicer accessor
	 */
	public static DynamicAccessor getAccessor() {
		return accessor;
	}
}