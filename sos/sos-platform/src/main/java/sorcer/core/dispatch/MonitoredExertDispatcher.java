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
package sorcer.core.dispatch;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.Lease;
import net.jini.core.transaction.TransactionException;
import net.jini.export.Exporter;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import net.jini.lease.LeaseRenewalManager;
import sorcer.core.Monitorable;
import sorcer.core.Provider;
import sorcer.core.SorcerConstants;
import sorcer.core.exertion.NetJob;
import sorcer.core.exertion.NetTask;
import sorcer.core.monitor.MonitorEvent;
import sorcer.core.monitor.MonitorSessionManagement;
import sorcer.core.provider.ProviderDelegate.ExertionSessionInfo;
import sorcer.core.provider.exertmonitor.MonitoringManagement;
import sorcer.core.signature.NetSignature;
import sorcer.service.*;
import sorcer.util.ProviderAccessor;
import sorcer.util.Sorcer;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.logging.Level;

public abstract class MonitoredExertDispatcher extends ExertDispatcher
		implements SorcerConstants {

	public static MonitorSessionManagement sessionMonitor;

	public static LeaseRenewalManager lrm;

	public static final long LEASE_RENEWAL_PERIOD = 1 * 1000 * 30L;

	public static final long DEFAULT_TIMEOUT_PERIOD = 1 * 1000 * 60L;

	protected int doneExertionIndex = 0;

	public MonitoredExertDispatcher(Exertion exertion,
                                    Set<Context> sharedContext, boolean isSpawned, Provider provider)
			throws Throwable {
		super(exertion, sharedContext, isSpawned, provider);
				
		if (sessionMonitor == null)
			sessionMonitor = (MonitoringManagement) ProviderAccessor
					.getProvider(Sorcer.getActualName(Sorcer.getExertMonitorName()), 
							MonitoringManagement.class);
		if (lrm == null)
			lrm = new LeaseRenewalManager();

		// get the exertion with the monitor session
		this.xrt = (ServiceExertion)register(exertion);

		// make the monitor session of this exertion active
		ExertDispatcher.logger.log(Level.FINER, "Dispatching task now: " + xrt.getName());
		MonitoringSession session = xrt.getMonitorSession();
		session.init((Monitorable) provider.getProxy(), LEASE_RENEWAL_PERIOD,
				DEFAULT_TIMEOUT_PERIOD);
		lrm.renewUntil(session.getLease(), Lease.ANY, null);
		
		dThread = new DispatchThread();
		try {
			dThread.start();
			dThread.join();
		} catch (InterruptedException ie) {
			ie.printStackTrace();
			state = ExecState.FAILED;
		}
	}

	private Exertion register(Exertion exertion) throws Exception {
		RemoteEventListener l = (RemoteEventListener) (new ResultListener()
				.export());
		ServiceExertion registeredExertion = (ServiceExertion) (sessionMonitor.register(l,
				exertion, LEASE_RENEWAL_PERIOD));

		MonitoringSession session = registeredExertion.getMonitorSession();
		ExertDispatcher.logger.info("Session for the exertion =" + session);
		ExertDispatcher.logger.info("Lease to be renewed for duration ="
				+ (session.getLease().getExpiration() - System
						.currentTimeMillis()));
		lrm.renewUntil(session.getLease(), Lease.ANY, null);
		return registeredExertion;
	}

	protected void preExecExertion(Exertion exertion) throws ExertionException {
		try {
			exertion.getControlContext().appendTrace(provider.getProviderName() 
					+ " dispatcher: " + getClass().getName());
		} catch (RemoteException e) {
			// ignore it, local call
			}
		ExertionSessionInfo.add((ServiceExertion)exertion);
		//Provider is expecting exertion field in Context to be set.
		xrt.getDataContext().setExertion(xrt);
		updateInputs(exertion);
		((ServiceExertion)exertion).startExecTime();
		((ServiceExertion) exertion).setStatus(ExecState.RUNNING);
	}

	protected void postExecExertion(Exertion result) throws ExertionException {
		ServiceExertion sxrt = (ServiceExertion)result;
		if (sxrt instanceof NetJob )
			((NetJob)xrt).setExertionAt(result, ((ServiceExertion) result).getIndex());
		else
			xrt = sxrt;
		
		if (sxrt.getStatus() > ExecState.ERROR && sxrt.getStatus() != ExecState.SUSPENDED) {
			sxrt.setStatus(ExecState.DONE);
			collectOutputs(result);
		}
		if (sxrt.getStatus() != ExecState.DONE)
			state = sxrt.getStatus();
		
		xrt.stopExecTime();
		ExertDispatcher.dispatchers.remove(xrt.getId());
		ExertionSessionInfo.removeLease();
		dThread.stop = true;
	}
	
	protected void execExertion(Exertion exertion) throws ExertionException,
			SignatureException, RemoteException {
		ServiceExertion ei = (ServiceExertion) exertion;
		preExecExertion(exertion);

		ExertDispatcher.logger.log(Level.INFO,
				"Prexec Exertion done .... noe executing exertion");
		try {
			if (ei.isTask())
				execTask((NetTask) exertion);
			else
				throw new ExertionException("Functionality not yet supported");
		} catch (Exception ex) {
			ex.printStackTrace();
			ei.reportException(ex);
			try {
				if (ei.isTask())
					((ServiceExertion) exertion).getMonitorSession()
							.changed(exertion.getDataContext(), ExecState.Category.FAILED);
			} catch (Exception ex0) {
				ex0.printStackTrace();
			} finally {
				((ServiceExertion)exertion).setStatus(ExecState.FAILED);
			}
		}

	}

	// Made private so that other classes just calls execExertion and not
	// execTask
	private void execTask(NetTask task) throws ExertionException,
			SignatureException {

		ExertDispatcher.logger.log(Level.INFO, "start executing task");
		try {
			Service provider = ProviderAccessor.getProvider(task
					.getProcessSignature().getProviderName(), task
					.getServiceType());
			ExertDispatcher.logger.log(Level.INFO, "got a provider:" + provider);

			if (provider == null) {
				String msg = null;
				// get the PROCESS Method and grab provider name + interface
				NetSignature method = (NetSignature) task
						.getProcessSignature();
				msg = "No Provider available. Provider Name: "
						+ method.getProviderName() + " Provider interface: "
						+ method.getServiceType();
				System.err.println(msg);
				throw new ExertionException(msg, task);
			} else {
				// setTaskProvider(task, provider.getProviderName());
				ExertDispatcher.logger.log(Level.INFO, "Servicing task now ..............");
				MonitoringSession session = task
						.getMonitorSession();
				session.init((Monitorable) provider, LEASE_RENEWAL_PERIOD,
						DEFAULT_TIMEOUT_PERIOD);
				lrm.renewUntil(session.getLease(), Lease.ANY, null);

				task.setServicer(provider);
				provider.service(task, null);
			}
		} catch (RemoteException re) {
			re.printStackTrace();
			ExertDispatcher.logger.log(Level.SEVERE, "dispatcher execution failed for task: "
					+ task);
			throw new ExertionException("Remote Exception while executing task");
		} catch (MonitorException mse) {
			mse.printStackTrace();
			logger.log(Level.SEVERE, "dispatcher execution failed for task: "
					+ task);
			throw new ExertionException("Remote Exception while executing task");
		} catch (TransactionException te) {
			te.printStackTrace();
			logger.log(Level.SEVERE, "dispatcher execution failed for task: "
					+ task);
			throw new ExertionException("Remote Exception while executing task");
		}
	}

	protected void postExecExertion(Exertion ex, Exertion result)
			throws ExertionException, SignatureException {
		// just over ridding from super
	}

	private class ResultListener implements RemoteEventListener, Remote {

		public ResultListener() {

		}

		public ResultListener(String type) {

		}

		public void notify(RemoteEvent re) throws RemoteException {
			try {
				logger.log(Level.INFO, "Recieved Remote event from the exert monitor:\n"
						+ re);
				if (!(((MonitorEvent) re).getExertion() instanceof Exertion))
					postExecExertion(((MonitorEvent) re)
							.getExertion());
			} catch (Exception e) {
				e.printStackTrace();
				xrt.setStatus(FAILED);
				state = FAILED;
			}
		}

		public Remote export() throws Exception {
			Exporter exp = new BasicJeriExporter(TcpServerEndpoint
					.getInstance(0), new BasicILFactory(), true, true);
			return exp.export(this);
		}

	}

}
