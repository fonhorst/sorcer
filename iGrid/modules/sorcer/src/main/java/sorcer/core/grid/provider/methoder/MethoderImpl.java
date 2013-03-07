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

package sorcer.core.grid.provider.methoder;

import java.rmi.RemoteException;

import sorcer.core.Methoder;
import sorcer.core.provider.ServiceProvider;

import com.sun.jini.start.LifeCycle;

/**
 * SORCER Tasker
 */
public class MethoderImpl extends ServiceProvider implements Methoder {

	/**
	 * default constructor
	 * 
	 * @throws RemoteException
	 */
	public MethoderImpl() throws RemoteException {
		super();
	}

	/**
	 * required constructor for Jini 2 NonActivatableServiceDescriptor
	 * 
	 * @param args
	 *            String[]
	 * @param lifeCycle
	 *            LifeCycle
	 * @see LifeCycle
	 */
	public MethoderImpl(String[] args, LifeCycle lifeCycle) throws Exception {
		super(args, lifeCycle);
	}

	/**
	 * Calls init on ServiceProvider
	 * 
	 * @see ServiceExerter
	 */
	public void init() throws RemoteException {
		super.init();
	}

}
