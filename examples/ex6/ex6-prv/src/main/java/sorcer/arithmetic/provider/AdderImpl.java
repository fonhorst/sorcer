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
package sorcer.arithmetic.provider;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import net.jini.lookup.entry.UIDescriptor;
import net.jini.lookup.ui.MainUI;
import sorcer.arithmetic.provider.ui.CalculatorUI;
import sorcer.core.Provider;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.MonitorException;
import sorcer.ui.serviceui.UIComponentFactory;
import sorcer.ui.serviceui.UIDescriptorFactory;
import sorcer.util.Sorcer;

public class AdderImpl implements Adder {
	private Arithmometer arithmometer = new Arithmometer();
	private Provider provider;
	private Logger logger = Logger.getLogger(AdderImpl.class.getName());
	
	public void init(Provider provider) {
		this.provider = provider;
		try {
			logger = provider.getLogger();
		} catch (RemoteException e) {
			// ignore it, local call
		}
	}
	
	public Context add(Context context) throws RemoteException,
			ContextException, MonitorException {
		Context out = arithmometer.add(context);
		logger.info("add result: " + out);
		
//		Logger contextLogger = provider.getContextLogger();
//		contextLogger.info("dataContext logging; add result: " + out);
//		
//		Logger providerLogger =  provider.getProviderLogger();
//		providerLogger.info("provider logging; add result: " + out);
//		try {
//			Thread.sleep(1000 * 60 * 2);
//			System.out.println("slept: " + 1000 * 60 * 2);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		out.checkpoint();
//		Logger remoteLogger =  provider.getRemoteLogger();
//		remoteLogger.info("remote logging; add result: " + out);
		
		return out;
	}
	
	public static UIDescriptor getCalculatorDescriptor() {
		UIDescriptor uiDesc = null;
		try {
			uiDesc = UIDescriptorFactory.getUIDescriptor(MainUI.ROLE,
					new UIComponentFactory(new URL[] { new URL(Sorcer
							.getWebsterUrl()
							+ "/calculator-ui.jar") }, CalculatorUI.class
							.getName()));
		} catch (Exception ex) {
			// do nothing
		}
		return uiDesc;
	}
}
