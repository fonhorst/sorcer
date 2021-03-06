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
package sorcer.service;

import sorcer.core.context.ControlContext.ThrowableTrace;

import java.util.List;

public interface Strategy extends Parameter {

	/**
	 * There are three flow types of {@link Flow}s that can be associated
	 * with exertions control dataContext. Flow type specify the flow of control (
	 * <code>SEQ</code>, <code>PAR</code>, or
	 * <code>CON</code>) for all component exertions at the same level.
	 */
	public enum Flow {
		SEQ, PAR, CON
	}
	
	/**
	 * When the access type of a control dataContext is set to <code>PULL</code>
	 * then the associated exertion is passed onto a {@link Spacer}, otherwise
	 * (access type is PUSH) the exertion is passed directly on to the provider
	 * specified by the <code>PROCESS</code>signature	 * 
	 */
	public enum Access {
		PUSH, PULL, SWIF, QOS_PUSH, QOS_PULL, CATALOG, DIRECT
	}
		
	/**
	 * Indicates if on-demand provisioning is used for not existing currently
	 * service but required in the overlay network of service providers..
	 */
	public enum Provision {
		YES, TRUE, NO, FALSE
	}

	/**
	 * The <code>CONCURRENT</code> flow is controlled by mutex state WAIT,
	 * NOTIFY, and NOTIFY_ALL of the control dataContext. Only one exertion can own
	 * the mutex at a time. If a second provider tries to acquire the exertion,
	 * it will block (be suspended) until the owning provider releases the
	 * mutex. An exertion access to which is guarded by a mutual-exclusion is
	 * called monitor.
	 */
	public enum Monitor {
		YES, TRUE, NO, FALSE, WAIT, NOTIFY, NOTIFY_ALL
	}
	
	/**
	 * Indicates whether the service requestor waits for results or not.
	 */
	public enum Wait {
		YES, TRUE, NO, FALSE
	}

    public boolean isWaitable();
	
	public Flow getFlowType();
	
	public Access getAccessType();

	public List<ThrowableTrace> getExceptions();
	
	public List<String> getTrace();
}
