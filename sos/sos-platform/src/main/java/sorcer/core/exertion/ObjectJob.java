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
package sorcer.core.exertion;

import net.jini.core.transaction.Transaction;
import sorcer.core.context.ServiceContext;
import sorcer.core.provider.jobber.ServiceJobber;
import sorcer.core.signature.ObjectSignature;
import sorcer.service.*;

import java.rmi.RemoteException;

/**
 * The SORCER object job extending the basic job implementation {@link Job}.
 * 
 * @author Mike Sobolewski
 */
public class ObjectJob extends Job {

	static final long serialVersionUID = 1793342047789581449L;
	
	public ObjectJob(String name) {
		super(name);
		addSignature(new ObjectSignature("execute", ServiceJobber.class));
	}

	public ObjectJob(String name, Signature signature)
			throws SignatureException {
		super(name);
		if (signature instanceof ObjectSignature)
			addSignature(signature);
		else
			throw new SignatureException("ObjectJob requires ObjectSignature: "
					+ signature);
	}

	public ObjectJob(String name, Signature signature, Context context)
			throws SignatureException {
		this(name, signature);
		if (context != null)
			this.dataContext = (ServiceContext) context;
	}

	public Job doJob(Transaction txn) throws ExertionException,
			SignatureException, RemoteException {
		// return (Job) new ServiceJobber().exec(job, txn);
		Job result = null;
		try {
			Class[] paramTypes = new Class[] { Exertion.class };
			Object[] parameters = new Object[] { this };
			result = (Job)((ObjectSignature) getProcessSignature())
					.initInstance(parameters, paramTypes);
		} catch (Exception e) {
			e.printStackTrace();
			dataContext.reportException(e);
		}
		return result;
	}

	@Override
	public Object getReturnValue() throws ContextException {
		if (getContext().getReturnPath() == null) {
			return getJobContext();
		} else {
			return super.getReturnValue();
		}
	}
}
