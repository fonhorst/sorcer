/**
 *
 * Copyright 2013 the original author or authors.
 * Copyright 2013 Sorcersoft.com S.A.
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
package sorcer.ex1.bean;

import sorcer.core.Provider;
import sorcer.core.provider.ServiceProvider;
import sorcer.ex1.Message;
import sorcer.ex1.WhoIsIt;
import sorcer.ex1.provider.ProviderMessage;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.logging.Logger;

public class WhoIsItBean2 implements WhoIsIt {

	private ServiceProvider provider;
    private Logger logger = Logger.getLogger(WhoIsItBean1.class.getName());
	
	public void init(Provider provider) {
		this.provider = (ServiceProvider)provider;
		try {
			logger = provider.getLogger();
		} catch (RemoteException e) {
			// ignore it, local call
		}
	}
	
	public Context getHostName(Context context) throws RemoteException,
			ContextException {
		logger.entering(WhoIsItBean2.class.getName(), "getHostName");
		try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            String hostname = inetAddress.getHostName();
         	context.putValue("provider/hostname", hostname);
			String rhn = (String) context.getValue("requestor/hostname");
			Message rmsg = (Message) context.getValue("requestor/message");
			context.putValue("provider/message", new ProviderMessage(rmsg
					.getMessage(), provider.getProviderName(), rhn));
			
			Thread.sleep(2000);
			context.reportException(new RuntimeException("Slept for 2 sec"));
			context.appendTrace(getClass().getName() + ":" + provider.getProviderName());
		
			logger.info("executed getHostName: " + context);

		} catch (UnknownHostException e1) {
			throw new ContextException("getHostAddress", e1);
		} catch (InterruptedException e2) {
			throw new ContextException("getHostAddress", e2);
		}
		return context;
	}

	public Context getHostAddress(Context context) throws RemoteException,
			ContextException {
		logger.entering(WhoIsItBean2.class.getName(), "getHostAddress");
		try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            String ipAddress = inetAddress.getHostAddress();
			context.putValue("provider/address", ipAddress);
			String rhn = (String) context.getValue("requestor/hostname");
			Message rmsg = (Message) context.getValue("requestor/message");
			context.putValue("provider/message", new ProviderMessage(rmsg
					.getMessage(), provider.getProviderName(), rhn));
			
			Thread.sleep(2000);
			context.reportException(new RuntimeException("Slept for 2 sec"));
			context.appendTrace(getClass().getName() + ":" + provider.getProviderName());
			
			logger.info("executed getHostAddress: " + context);

		} catch (UnknownHostException e1) {
			throw new ContextException("getHostAddress", e1);
		} catch (InterruptedException e2) {
			throw new ContextException("getHostAddress", e2);
		}
		return context;
	}

	/* (non-Javadoc)
	 * @see sorcer.ex1.provider.WhoIsIt#getCanonicalHostName(sorcer.service.Context)
	 */
	public Context getCanonicalHostName(Context context)
			throws RemoteException, ContextException {
        logger.entering(WhoIsItBean2.class.getName(), "getCanonicalHostName");
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            String fqname = inetAddress.getCanonicalHostName();
            context.putValue("provider/fqname", fqname);
			String rhn = (String) context.getValue("requestor/hostname");
			Message rmsg = (Message) context.getValue("requestor/message");
			context.putValue("provider/message", new ProviderMessage(rmsg
					.getMessage(), provider.getProviderName(), rhn));
		} catch (UnknownHostException e1) {
			context.reportException(e1);
			e1.printStackTrace();
		}
		return context;
	}

	/* (non-Javadoc)
	 * @see sorcer.ex1.provider.WhoIsIt#getTimestamp(sorcer.service.Context)
	 */
	@Override
	public Context getTimestamp(Context context) throws RemoteException,
			ContextException {
        logger.entering(WhoIsItBean2.class.getName(), "getTimestamp");
        context.putValue("provider/timestamp", StringUtils.getDateTime());
        String rhn = (String) context.getValue("requestor/hostname");
        Message rmsg = (Message) context.getValue("requestor/message");
        context.putValue("provider/message",
                new ProviderMessage(rmsg.getMessage(), provider.getProviderName(),
                        rhn));
		return context;
	}
}
