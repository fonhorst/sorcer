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
package sorcer.ex1.requestor;

import sorcer.core.SorcerEnv;
import sorcer.core.context.ServiceContext;
import sorcer.core.exertion.NetTask;
import sorcer.core.requestor.ServiceRequestor;
import sorcer.core.signature.NetSignature;
import sorcer.service.Context;
import sorcer.service.Exertion;
import sorcer.service.Signature;
import sorcer.service.Signature.Type;
import sorcer.service.Task;
import sorcer.util.Log;
import sorcer.util.Sorcer;

import java.net.InetAddress;
import java.rmi.RMISecurityManager;
import java.util.logging.Logger;

public class WhoIsItBatchApp {

	private static Logger logger = Log.getTestLog();

	public static void main(String... args) throws Exception {
		System.setSecurityManager(new RMISecurityManager());
        ServiceRequestor.prepareCodebase();
		// initialize system environment from configs/sorcer.env
		Sorcer.getEnvProperties();
		// get the queried provider name
		String providerName = Sorcer.getSuffixedName(args[0]);
		logger.info("Who is provider \"" + providerName + "\"?");
		
		
		Exertion result = new WhoIsItBatchApp().getExertion(providerName)
				.exert();
		logger.info("Exceptions: \n" + result.getExceptions());
		logger.info("Trace: \n" + result.getTrace());
		logger.info("Ouptut dataContext: \n" + result.getDataContext());
	}
	
	public Exertion getExertion(String providername) throws Exception {
		String hostname, ipAddress;
		InetAddress inetAddress = SorcerEnv.getLocalHost();
		hostname = inetAddress.getHostName();
		ipAddress = inetAddress.getHostAddress();

		Context context = new ServiceContext("Who Is It?");
		context.putValue("requestor/message", new RequestorMessage("SORCER"));
		context.putValue("requestor/hostname", hostname);
		context.putValue("requestor/address", ipAddress);
		
		Signature signature1 = new NetSignature("getHostAddress",
				sorcer.ex1.WhoIsIt.class, providername, Type.PRE);
		Signature signature2 = new NetSignature("getHostName",
				sorcer.ex1.WhoIsIt.class, providername, Type.SRV);
		Signature signature3 = new NetSignature("getCanonicalHostName",
				sorcer.ex1.WhoIsIt.class, providername, Type.POST);
		Signature signature4 = new NetSignature("getTimestamp",
				sorcer.ex1.WhoIsIt.class, providername, Type.POST);
		
		Task task = new NetTask("Who Is It?",
                new Signature[] { signature1, signature2, signature3, signature4 },
                context);

		return task;
	}
}
