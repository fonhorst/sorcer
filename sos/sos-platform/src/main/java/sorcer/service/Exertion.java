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

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.id.Uuid;
import sorcer.core.context.ControlContext;
import sorcer.core.context.ControlContext.ThrowableTrace;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Flow;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.List;

/**
 * An exertion is a federated service command (statement) communicated by a requestor -
 * {@link Service#service}, to a service provider {@link Service}. It is a
 * form of service-oriented message with an enclosed service
 * {@link sorcer.service.Context} and a collection of service
 * {@link sorcer.service.Signature}s. The service dataContext specifies service data
 * to be processed by the service providers matching exertion's signatures. An
 * exertion's signature can carry own implementation when its type is associated
 * with a provided codebase.<br>
 * Exertions are request interseptors in the form of command objects (in the
 * Command programming pattern) that keep a service requestor completely
 * separate from the actions that service providers carry out. Exertions are
 * also completely separate from each other and do not know what other exertions
 * do. However, they can pass parameters via contexts by mapping
 * {@link Context#map} expected input dataContext values in terms of expected output
 * values calculated by other exertion. The operation {@link Exertion#exert} of
 * this interface provides for execution of desired actions enclosed in
 * exertions, thus keeping the knowledge of what to do inside of the exertions,
 * instead of having another parts of SO program to make these decisions. When
 * an exertion is invoked then the exertion redirects control to a dynamically
 * bound {@link Service} matching the exertion's signature of
 * type Operate <code>PROCESS</code>. <br>
 * The <code>Exertion</code> interface also provides for the Composite design
 * pattern and defines a common elementary behavior for all exertions of
 * {@link sorcer.service.Task} type and control flow exertions (for branching
 * and looping). A composite exertion called {@link sorcer.service.Job} contains
 * a collection of elementary, flow control, and other composite exertions with
 * the same elementary behavior performed collectively by all its members. Thus
 * {@link sorcer.service.Task}s and control flow exertions are analogous to
 * programming statements and {@link sorcer.service.Job}s analogous to
 * procedures in conventional procedural programming. <br>
 * Control flow exertions allow for branching and looping operations by
 * {@link Service}s executing exertions. A job combined from
 * tasks and other jobs along with relevant control flow exertions is a
 * service-oriented procedure that can federate its execution with multiple
 * service providers bound dynamically in runtime as determined by
 * {@link Signature}s of job component exertions.
 * 
 * @see sorcer.service.AsyncExertion
 * @author Mike Sobolewski
 */
public interface Exertion extends  Service, Evaluation<Object>, Invocation<Object>, Serializable, Identifiable {

	/**
	 * Returns a name of this exertion.
	 * 
	 * @return a name
	 */
	public String getName();

	/**
	 * Returns a status of this exertion.
	 * 
	 * @return a status 
	 */
	public int getStatus();
		
	/**
	 * Returns an ID of this exertion.
	 * 
	 * @return a unique identifier 
	 */
	public Uuid getId();
	
	/**
	 * Appends the specified component exertion to the end of its list of exertions.
	 * 
	 * @return an added component exertion
	 */
	public Exertion addExertion(Exertion component);
	
	/**
	 * Returns a service dataContext (service data) of this exertion to be processed
	 * by its methods as specified by exertion's signatures.
	 * 
	 * @return a service dataContext
	 * @see #getSignatures
	 */
    public Context getContext();
    // synonym, the same as  getContext();
    public Context getDataContext();

    /**
     * Returns a service context (service data) of the component exertion.
     *
     * @return a service context
     * @see #getSignatures
     */
    public Context getContext(String componentExertionName);

	public List<String> getTrace();
	
	/**
	 * Returns a control dataContext (service control strategy) of this exertion to be
	 * realized by a tasker, jobber or spacer.
	 * 
	 * @return a control dataContext
	 * @see #getSignatures
	 */
	public ControlContext getControlContext();
	
	/**
	 * Returns a process signature, all pre-processing, post-processing, and
	 * append signatures. There is only one process signature defining late
	 * binding to the service provider processing this exertion.
	 * 
	 * @return a collection of all service signatures
	 * @see #getProcessSignature
	 */
	public List<Signature> getSignatures();

	/**
	 * Returns a signature of the <code>PROCESS</code> type for this exertion.
	 * 
	 * @return a process service signature
	 * @see #getSignatures
	 */
	public Signature getProcessSignature();

	/**
	 * Returns a flow type for this exertion execution. A flow type indicates if
	 * this exertion can be executed sequentially, in parallel, or concurrently
	 * with other component exertions within this exertion. The concurrent
	 * execution requires all mapped inputs in the exertion dataContext to be
	 * assigned before this exertion can be executed.
	 * 
	 * @return a flow type
	 * @see {@link Flow}.
	 */
	public Flow getFlowType();

	/**
	 * Returns a provider access type for this exertion execution. An access
	 * type indicates whether the receiving provider specified in the exertion's
	 * process signature is accessed directly (Acess.PUSH) or indirectly
	 * (Acess.PULL) via a shared exertion space.
	 * 
	 * @return an access type
	 * @see {@link Access}.
	 */
	public Access getAccessType();
	
	/**
	 * Sends this exertion to the assigned service provider if set. If a service
	 * provider is not set then in runtime it bounds to any available provider
	 * that matches this exersion's signature of the <code>PROCESS</code> type.
	 * 
	 * @param txn
	 *            The transaction (if any) under which to exert.
	 * @return a resulting exertion
	 * @throws TransactionException
	 *             if a transaction error occurs
	 * @throws ExertionException
	 *             if processing this exertion causes an error
	 */
	public <T extends Exertion> T exert(Transaction txn, Parameter... entries) throws TransactionException,
			ExertionException, RemoteException;

	public Exertion exert(Parameter... entries) throws TransactionException, ExertionException,
			RemoteException;
	
	/**
	 * Returns the list of direct component exertions.
	 * @return Exertion list
	 */ 
	public List<Exertion> getExertions();
	
	/**
	 * Returns the list of traces of thrown exceptions.
	 * @return ThrowableTrace list
	 */ 
	public List<ThrowableTrace> getExceptions();
	
	/**
	 * Returns a component exertion with a given name.
	 * @return Exertion list
	 */ 
	public Exertion getExertion(String name);

	/**
	 * Returns the list of all nested component exertions/
	 * @return Exertion list
	 */ 
	public List<Exertion> getAllExertions();
	
	/**
	 * Returns <code>true</code> if this exertion should be monitored for its
	 * execution, otherwise <code>false</code>.
	 * 
	 * @return <code>true</code> if this exertion requires its execution to be
	 *         monitored.
	 */
	public boolean isMonitorable();
	
	/**
	 * Returns <code>true</code> if this exertion can be provisioned for its
	 * execution, otherwise <code>false</code>.
	 * 
	 * @return <code>true</code> if this exertion can be
	 *         provisioned.
	 */
	public boolean isProvisionable();
	
	/**
	 * Returns <code>true</code> if this result exertion should be returned to
	 * its requestor synchronously, otherwise <code>false</code> when accessed
	 * asynchronously.
	 * 
	 * @return <code>true</code> if this exertion is returned synchronously.
	 */
	public boolean isWaitable();
	
	/**
	 * Returns <code>true</code> if this exertion requires execution by a
	 * {@link Jobber}, otherwise <code>false</code> if this exertion requires
	 * its execution by a {@link Tasker}. Note that control follow exertion can be
	 * processed by either a {@link Tasker} or a {@link Jobber}.
	 * 
	 * @return <code>true</code> if this exertion requires execution by a
	 *         {@link Jobber}.
	 */
	public boolean isJob();

    public boolean isTask();

    public boolean isCmd();

	/**
	 * Return true if this exertion is atop an acyclic graph in which no node
	 * has two parents (two references to it).
	 * 
	 * @return true if this exertion is atop an acyclic graph in which no node
	 *         has two parents (two references to it).
	 */
	public boolean isTree();
		
	/**
	 * The exertion format for thin exertions (no RMI and Jini classes)
	 */
	public static final int THIN = 0;

	/**
	 * The exertion format for thick exertions (with RMI and Jini classes)
	 */
	public static final int STANDARD = 1;
	
}