/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.core.requestor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import sorcer.core.SorcerConstants;
import sorcer.service.ContextException;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;
import sorcer.service.ServiceExertion;
import sorcer.service.SignatureException;
import sorcer.tools.webster.InternalWebster;
import sorcer.util.ArtifactCoordinates;
import sorcer.util.Sorcer;

abstract public class ExertionRunner implements Runner, SorcerConstants {
	/** Logger for logging information about this instance */
	protected static final Logger logger = Logger
			.getLogger(ExertionRunner.class.getName());

	protected Properties props;
	protected Exertion exertion;
	protected String jobberName;
	protected static ExertionRunner requestor = null;
	protected static boolean isWebsterInt = false;
	final static String REQUESTOR_PROPERTIES_FILENAME = "requestor.properties";
	
	public static void main(String... args) throws Exception {
		prepareCodebase();
		prepareToRun(args);
		requestor.preprocess(args);
		requestor.process(args);
		requestor.postprocess();
	}

	public static void prepareToRun(String... args) {
		System.setSecurityManager(new RMISecurityManager());

		String runnerType = null;
		if (args.length == 0) {
			System.err
					.println("Usage: Java sorcer.core.requestor.ExertionRunner  <runnerType>");
			System.exit(1);
		} else {
			runnerType = args[0];
		}
		try {
			requestor = (ExertionRunner) Class.forName(runnerType)
					.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("Not able to create service runner: " + runnerType);
			System.exit(1);
		}
		String str = System.getProperty(REQUESTOR_PROPERTIES_FILENAME);
		logger.info(REQUESTOR_PROPERTIES_FILENAME + " = " + str);
		if (str != null) {
			requestor.loadProperties(str); // search the provider package
		} else {
			requestor.loadProperties(REQUESTOR_PROPERTIES_FILENAME);
		}

		if (isWebsterInt) {
			String roots = System.getProperty(SorcerConstants.WEBSTER_ROOTS);
			String[] tokens = null;
			if (roots != null)
				tokens = toArray(roots);
			try {
				InternalWebster.startWebster(tokens);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void setExertion(Exertion exertion) {
		this.exertion = exertion;
	}

	abstract public Exertion getExertion(String... args)
			throws ExertionException, ContextException, SignatureException;

	public String getJobberName() {
		return jobberName;
	}

	public static void prepareCodebase() {
		// Initialize system properties: configs/sorcer.env
		Sorcer.getEnvProperties();
		String val = System.getProperty(SorcerConstants.SORCER_WEBSTER_INTERNAL);
		if (val != null && val.length() != 0) {
			isWebsterInt = val.equals("true");
		}
		String exertrun = System.getProperty("sorcer.exertrun.codebase");		
		StringBuilder codebase = new StringBuilder();
		if (exertrun!=null && !exertrun.isEmpty()) {
			String[] artifacts = exertrun.split(" ");
			for (String artifact : artifacts) {
				if (codebase.length() > 0)
					codebase.append(" ");
				codebase.append(resolve(ArtifactCoordinates.coords(artifact)));
			}
			// Add default codebase sos-platform and sos-env
			codebase.append(' ').append(resolve(ArtifactCoordinates.getSorcerApi()));
			codebase.append(' ').append(resolve(ArtifactCoordinates.getSorcerConst()));
			
			logger.fine("ExertionRunner generated codebase: " + codebase.toString());
			if (isWebsterInt)
				System.setProperty("sorcer.codebase.jars", codebase.toString());
			else
				System.setProperty("java.rmi.server.codebase", codebase.toString());					
		}		
	}

	private static String resolve(ArtifactCoordinates coords) {
		return isWebsterInt ? coords.getRelativePath() : coords.fromRemote(Sorcer.getWebsterUrl());
	}


	public void preprocess(String... args) {
		Exertion in = null;
		try {
			in = requestor.getExertion(args);
			if (logger.isLoggable(Level.FINE))
				logger.fine("Runner java.rmi.server.codebase: "
						+ System.getProperty("java.rmi.server.codebase"));
		} catch (ExertionException e) {
			logger.throwing("ExertionRunner", "main", e);
			System.exit(1);
		} catch (ContextException e) {
			logger.throwing("ExertionRunner", "main", e);
			System.exit(1);
		} catch (SignatureException e) {
			logger.throwing("ExertionRunner", "main", e);
			System.exit(1);
		}
		if (in != null)
			requestor.setExertion(in);
		if (exertion != null)
			logger.info(">>>>>>>>>> Input context: \n" + exertion.getContext());
	}

	public void process(String... args) throws Exception {
		try {
			exertion = ((ServiceExertion) exertion).exert(
					requestor.getTransaction(), requestor.getJobberName());
		} catch (RemoteException re) {
			logger.throwing("ExertionRunner", "main", re);
		} catch (TransactionException te) {
			logger.throwing("ExertionRunner", "main", te);
		} catch (ExertionException ee) {
			logger.throwing("ExertionRunner", "main", ee);
		}
	}
	
	public void postprocess(String... args) {
		if (exertion != null) {
			logger.info("<<<<<<<<<< Exceptions: \n" + exertion.getThrowables());
			logger.info("<<<<<<<<<< Traces: \n" + exertion.getControlContext().getTrace());
			logger.info("<<<<<<<<<< Ouput context: \n" + exertion.getContext());
		}
		// Exit webster
		if (isWebsterInt) {
			InternalWebster.stopWebster();
		}
			
	}

	public Transaction getTransaction() {
		return null;
	}

	/**
	 * Loads service requestor properties from a <code>filename</code> file. By
	 * default a service requestor loads its properties from
	 * <code>requestor.properties</code> file located in the requestor's
	 * package. Also, a service requestor properties file name can be specified
	 * as a system property when starting the requestor with
	 * <code>-DrequestorProperties=&ltfilename&gt<code>. In this case the requestor loads 
	 * properties from <code>filename</code> file. Properties are accessible
	 * calling the <code>
	 * getProperty(String)</code> method.
	 * 
	 * @param filename
	 *            the properties file name see #getProperty
	 */
	public void loadProperties(String filename) {
		logger.info("loading requestor properties:" + filename);
		String propsFile = System.getProperty("requestor.properties.file");

		try {
			if (propsFile != null) {
				props.load(new FileInputStream(propsFile));
			} else {
				// check the class resource
				InputStream is = this.getClass().getResourceAsStream(filename);
				// check local resource
				if (is == null)
					is = (InputStream) (new FileInputStream(filename));
				if (is != null) {
					props = new Properties();
					props.load(is);
				} else {
					System.err
							.println("Not able to open stream on properties: "
									+ filename);
					System.err.println("Service runner class: "
							+ this.getClass());
					return;
				}
			}
		} catch (IOException ioe) {
			logger.info("Not able to load requestor properties");
			// ioe.printStackTrace();
		}

	}

	public String getProperty(String key) {
		return props.getProperty(key);
	}

	public String getProperty(String property, String defaultValue) {
		return props.getProperty(property, defaultValue);
	}

	private static String[] toArray(String arg) {
		StringTokenizer token = new StringTokenizer(arg, " ,;");
		String[] array = new String[token.countTokens()];
		int i = 0;
		while (token.hasMoreTokens()) {
			array[i] = token.nextToken();
			i++;
		}
		return (array);
	}
}
