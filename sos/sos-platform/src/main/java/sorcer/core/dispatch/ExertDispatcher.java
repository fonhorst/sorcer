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
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import sorcer.co.tuple.Tuple2;
import sorcer.core.*;
import sorcer.core.context.Contexts;
import sorcer.core.context.ServiceContext;
import sorcer.core.exertion.Jobs;
import sorcer.core.exertion.NetJob;
import sorcer.core.exertion.NetTask;
import sorcer.core.misc.MsgRef;
import sorcer.core.signature.NetSignature;
import sorcer.falcon.base.Conditional;
import sorcer.service.*;
import sorcer.util.*;
import sorcer.util.dbac.ProxyProtocol;
import sorcer.util.dbac.ServletProtocol;

import javax.security.auth.Subject;
import java.io.File;
import java.lang.reflect.Array;
import java.rmi.RemoteException;
import java.rmi.server.UID;
import java.util.*;
import java.util.logging.Logger;

@SuppressWarnings("rawtypes")
abstract public class ExertDispatcher implements Dispatcher,
		SorcerConstants, ExecState {
	protected final static Logger logger = Log.getDispatchLog();

	protected ServiceExertion xrt;

	protected ServiceExertion masterXrt;

	protected Vector<Exertion> inputXrts;

	protected int state = INITIAL;

	protected boolean isMonitored;

	protected Set<Context> sharedContexts;

	// If it is spawned by another dispatcher.
	protected boolean isSpawned;

	// All dispatchers spawned by this one.
	protected Vector runningExertionIDs = new Vector();

	// subject for whom this dispatcher is running.
	// make sure subject is set before and after any object goes out and comes
	// in dispatcher.
	protected Subject subject;

	protected Provider provider;

	protected static Cataloger catalog; // The SORCER catalog

	protected static Hashtable<Uuid, ExertDispatcher> dispatchers
		= new Hashtable<Uuid, ExertDispatcher>();

	protected ThreadGroup disatchGroup;
	
	protected DispatchThread dThread;

	public static Hashtable<Uuid, ExertDispatcher> getDispatchers() {
		return dispatchers;
	}

	public static void setDispatchers(Hashtable<Uuid, ExertDispatcher> dispatchers) {
		ExertDispatcher.dispatchers = dispatchers;
	}

	public Provider getProvider() {
		return provider;
	}

	public void setProvider(Provider provider) {
		this.provider = provider;
	}

	public ExertDispatcher() {
	}

	public ExertDispatcher(Exertion exertion, Set<Context> sharedContexts,
                           boolean isSpawned, Provider provider) throws Throwable {
		ServiceExertion sxrt = (ServiceExertion)exertion;
		this.xrt = (ServiceExertion)sxrt;
		this.subject = sxrt.getSubject();
		this.sharedContexts = sharedContexts;
		this.isSpawned = isSpawned;
		this.isMonitored = sxrt.isMonitorable();
		this.provider = provider;
		sxrt.setStatus(RUNNING);
		initialize();
	}

	protected void initialize() {
		dispatchers.put(xrt.getId(), this);
		state = RUNNING;
		if (xrt instanceof NetJob) {
			masterXrt = (ServiceExertion) ((NetJob) xrt)
					.getMasterExertion();
		}
	}

	abstract public void dispatchExertions() throws ExertionException,
			SignatureException;

	abstract public void collectResults() throws ExertionException,
			SignatureException;

	// Pre-processing before execution/writing into space
	abstract protected void preExecExertion(Exertion ex)
			throws ExertionException, SignatureException;

	// Post Processing after execution/taking from space.
	abstract protected void postExecExertion(Exertion input, Exertion result)
			throws ExertionException, SignatureException;

	public Exertion getExertion() {
		return xrt;
	}

	public static ExertDispatcher getDispatcher(String jobID) {
		return dispatchers.get(jobID);
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	protected class DispatchThread extends Thread {
		volatile boolean stop = false;
		
		public DispatchThread() {
		}

		public DispatchThread(ThreadGroup disatchGroup) {
			super(disatchGroup, "exertionDispatcher");
		}

		public void run() {
			try {
				while (!stop) {
					dispatchExertions();
				}
			} catch (Exception e) {
				logger.finer("Exertion dispatcher thread killed by exception");
				interrupt();
				e.printStackTrace();
			}
			dispatchers.remove(xrt.getId());
		}
	}

	protected class CollectResultThread extends Thread {

		public CollectResultThread(ThreadGroup disatchGroup) {
			super(disatchGroup, "Result collector");
		}

		public void run() {
			if (xrt.isExecTimeRequested())
				xrt.startExecTime();
			try {
				collectResults();
				xrt.setStatus(DONE);
				if (dThread != null)
					dThread.stop = true;
			} catch (Exception ex) {
				xrt.setStatus(FAILED);
				xrt.reportException(ex);
				ex.printStackTrace();
			}
			if (xrt.isExecTimeRequested())
				xrt.stopExecTime();
			dispatchers.remove(xrt.getId());
		}
	}

	protected NetTask httpExportTask(Task task)
			throws ExertionException, SignatureException {
		String url = ((NetSignature) task.getProcessSignature())
				.getPortalURL();
		if (url.indexOf("servlet") < 0 && url.endsWith("/"))
			url = url + "servlet/dispatcher";
		else
			url = url + "/servlet/dispatcher";

		if (url.startsWith("https://"))
			System.setProperty("java.protocol.handler.pkgs",
					"com.sun.net.ssl.internal.www.protocol");

		ProxyProtocol protocol = new ServletProtocol(url, false);
		NetTask result = (NetTask) ((ServletProtocol) protocol)
				.doTask(task);
		if (result == null || !(result instanceof ServiceExertion)) {
			throw new SignatureException(
					"Portal task execution failed at URL '" + url + "'");
		}
		return result;
	}

	public static String createExertionID(ServiceExertion ex) {
		// create unique identifier for job
		return new UID().toString() + ex.getName();
	}

	// Recursively collect shared contexts for inner jobs
	protected void collectSharedContexts(Exertion ex) {
		for (Exertion innerEx: ex.getExertions()) { 
			if (!innerEx.isJob()) 
				collectOutputs(innerEx);
			else
				collectSharedContexts(innerEx);
		}
	}
	
	protected void collectOutputs(Exertion ex) {
		List<Context> contexts = Jobs.getTaskContexts(ex);
		for (int i = 0; i < contexts.size(); i++) {
//			if (!sharedContexts.contains(contexts.get(i)))
//				sharedContexts.add(contexts.get(i));
		if (((ServiceContext)contexts.get(i)).isShared())
			sharedContexts.add(contexts.get(i));
		}
	}

	protected void updateInputs(Exertion ex) throws ExertionException {
		List<Context> inputContexts = Jobs.getTaskContexts(ex);
		for (int i = 0; i < inputContexts.size(); i++)
			updateInputs((ServiceContext) inputContexts.get(i));
	}

	protected void updateInputs(ServiceContext toContext)
			throws ExertionException {
		ServiceContext fromContext;
		String toPath = null, newToPath = null, toPathcp, fromPath = null;
		int argIndex = -1;
		try {
			Hashtable toInMap = Contexts.getInPathsMap(toContext);
//			logger.info("**************** updating inputs in dataContext toContext = " + toContext);
//			logger.info("**************** updating based on = " + toInMap);
			for (Enumeration e = toInMap.keys(); e.hasMoreElements();) {
				toPath = (String) e.nextElement();
				// find argument for parametric dataContext
				if (toPath.endsWith("]")) {
					Tuple2<String, Integer> pair = getPathIndex(toPath);
					argIndex = pair._2;
					if	(argIndex >=0) {
						newToPath = pair._1;
					}
				}
				toPathcp = (String) toInMap.get(toPath);
//				logger.info("**************** toPathcp = " + toPathcp);
				fromPath = Contexts.getContextParameterPath(toPathcp);
//				logger.info("**************** dataContext ID = " + Contexts.getContextParameterID(toPathcp));
				fromContext = getSharedContext(fromPath, Contexts.getContextParameterID(toPathcp));
//				logger.info("**************** fromContext = " + fromContext);
//				logger.info("**************** before updating toContext: " + toContext
//						+ "\n>>> TO path: " + toPath + "\nfromContext: "
//						+ fromContext + "\n>>> FROM path: " + fromPath);
				if (fromContext != null) {
//					logger.info("**************** updating toContext: " + toContext
//							+ "\n>>> TO path: " + toPath + "\nfromContext: "
//							+ fromContext + "\n>>> FROM path: " + fromPath);
					// make parametric substitution if needed
					if (argIndex >=0 ) {
						Object args = toContext.getValue(Context.PARAMETER_VALUES);
						if (args.getClass().isArray()) {
							if (Array.getLength(args) > 0) {
								Array.set(args, argIndex, fromContext.getValue(fromPath));
							} else {
								// the parameter array is empty
								Object[] newArgs = null;
								newArgs = new Object[] { fromContext.getValue(fromPath) };
								toContext.putValue(newToPath, newArgs);
							}
						}
					} else {
						// make contextual substitution
						Contexts.copyValue(fromContext, fromPath, toContext, toPath);
					}
//					logger.info("**************** updated dataContext:\n" + toContext);
				}
			}
		} catch (Exception ex) {
			throw new ExertionException("Failed to update data dataContext: " + toContext.getName()
					+ " at: " + toPath + " from: " + fromPath, ex);
		}
	}

	private Tuple2<String, Integer> getPathIndex(String path) {
		int index = -1;
		String newPath = null;
		int i1 = path.lastIndexOf('/');
		String lastAttribute = path.substring(i1+1);
		if (lastAttribute.charAt(0) == '[' && lastAttribute.charAt(lastAttribute.length()-1) == ']') {
			index = Integer.parseInt(lastAttribute.substring(1, lastAttribute.length()-1));
			newPath = path.substring(0, i1+1);
		}
		return new Tuple2<String, Integer>(newPath, index);
	}
	
	protected ServiceContext getSharedContext(String path, String id) {
		// try to get the dataContext with particular id.
		// If not found, then find a dataContext with particular path.
		Context hc;
		if (ServiceContext.EMPTY_LEAF.equals(path) || "".equals(path))
			return null;
		if (id != null && id.length() > 0) {
			Iterator<Context> it = sharedContexts.iterator();
			while (it.hasNext()) {
				hc = it.next();
				if (UuidFactory.create(id).equals(hc.getId()))
					return (ServiceContext)hc;
			}
		}
		else {
			Iterator<Context> it = sharedContexts.iterator();
			while (it.hasNext()) {
				hc = it.next();
				if (hc.containsPath(path))
					return (ServiceContext)hc;
			}
		}
		return null;
	}

	public void sendMail(String message, String to) {
		String[] msg = new String[MSIZE];
		msg[MTO] = to;
		String admin = Sorcer.getProperty("sorcer.admin");
		if (admin == null)
			admin = "nobody@sorcer.cs.ttu.edu";
		msg[MFROM] = admin;
		msg[MSUBJECT] = "SORCER notification";
		msg[MTEXT] = message;
		Command mail = new EmailCmd(String.valueOf(SEND_MAIL), Sorcer
				.getProperty("smtp.host"));
		mail.setArgs(null, msg);
		mail.doIt();
	}

	public static void sendMailWithSubject(String message, String subject,
			String to) {
		String[] msg = new String[MSIZE];
		msg[MTO] = to;
		String admin = Sorcer.getProperty("sorcer.admin");
		if (admin == null)
			admin = "nobody@sorcer.cs.ttu.edu";
		msg[MFROM] = admin;
		msg[MSUBJECT] = "SORCER notification: " + subject;
		msg[MTEXT] = message;
		Command mail = new EmailCmd(String.valueOf(SEND_MAIL), Sorcer
				.getProperty("smtp.host"));
		mail.setArgs(null, msg);
		mail.doIt();
	}

	public void notifyExertionExecution(Exertion inex, Exertion outex) {
		notifyExertionExecution(xrt, inex, outex);
	}
	
	public void notifyExertionExecution(Exertion parent, Exertion inex, Exertion outex) {
		if (inex instanceof Conditional && outex instanceof Conditional) {
			// do nothing for now
		} else if (inex.isTask()
				&& outex.isTask())
			notifyTaskExecution(parent, (ServiceExertion) inex, (ServiceExertion) outex);
	}

	private void notifyTaskExecution(Exertion parent, ServiceExertion inTask,
			ServiceExertion outTask) {
		// notify o MASTER task completion
		Vector recipients = null;
		String notifyees = parent.getControlContext().getNotifyList(inTask);
		if (notifyees != null) {
			String[] list = StringUtils.tokenize(notifyees, MAIL_SEP);
			recipients = new Vector(list.length);
			for (int i = 0; i < list.length; i++)
				recipients.addElement(list[i]);
		}

		String to = "", admin = Sorcer.getProperty("sorcer.admin");
		if (recipients == null) {
			if (admin != null) {
				recipients = new Vector();
				recipients.addElement(admin);
			}
		} else if (admin != null && !recipients.contains(admin))
			recipients.addElement(admin);

		if (recipients == null || recipients.size() == 0)
			return;

		StringBuffer sb;
		if (inTask == masterXrt)
			sb = new StringBuffer("SORCER Master Task ");
		else
			sb = new StringBuffer("SORCER Task ");

		sb.append(outTask.getName()).append("\n\nDescription:\n").append(
				outTask.getDescription());
		if (outTask.getStatus() > 0)
			sb
					.append("\n\nTask executed sucessfully with the input dataContext:\n");
		else
			sb.append("\n\nTask execution FAILED; The input dataContext was:\n");
		sb.append(inTask.contextToString()).append(
				"\n\nincluding the following output dataContext:\n").append(
				outTask.getDataContext());
		sb.append("\n\nincluding the specific output paths:\n").append(
				Contexts.getFormattedOut(outTask.getDataContext(), true));

		for (int i = 0; i < recipients.size() - 1; i++)
			to = to + recipients.elementAt(i) + ",";

		to = to + recipients.lastElement();

		// sendMailWithSubject(sb.toString(), outTask.getName(), to);
	}

	protected String getDataURL(String filename) {
		String dataURL = Sorcer.getProperty("sorcer.dataURL");
		dataURL.replace('/', File.separatorChar);
		if (!dataURL.endsWith(File.separator))
			dataURL += File.separator;
		return dataURL + filename;
	}

	protected String getDataFilename(String filename) {
		if (filename.charAt(0) == File.separatorChar)
			return filename;

		String baseDir = Sorcer.getProperty("sorcer.baseDir");
		String dataDir = Sorcer.getProperty("sorcer.dataDir");
		baseDir.replace('/', File.separatorChar);
		dataDir.replace('/', File.separatorChar);
		if (!baseDir.endsWith(File.separator)) {
			baseDir += File.separator;
		}
		if (!dataDir.endsWith(File.separator)) {
			dataDir += File.separator;
		}
		return baseDir + dataDir + filename;
	}

	public boolean isMonitorable() {
		return isMonitored;
	}

	/*
	 * protected void setTaskProvider(RemoteServiceTask task, String name) {
	 * ServiceContext[] ctxs = task.getContexts(); String providerPath =
	 * TASK_PROVIDER + "/" + task.getName() + "/ind" + task.index;
	 * ctxs[0].putValue(OUT_PATH_PROVIDER, providerPath);
	 * ctxs[0].putValue(providerPath, name); }
	 */

	protected void notifyFailure(String msg, ServiceExertion t, long seqNum) {
		// CacheServer cs = (CacheServer) ServiceProviderAccessor.getCache();
		String msgID = null;
		String UserID = null;

		long dummy = 0;/* dummy eventID value for the remote event */

		/* persist the message to the DB and get back the MsgId */
		// msgID = cs.storeMessage(msg, String jobID, t.taskID,
		// NOTIFY_FAILEDURE);
		MsgRef mr = null;// new MsgRef(t.taskID, this.job.getID(), msgID ,
		// UserID, NOTIFY_FAILEDURE);
		RemoteEvent re = new RemoteEvent(mr, dummy, seqNum, null);
	}

	protected void notifyException(String msg, ServiceExertion t, long seqNum) {
		// CacheServer cs = (CacheServer) ServiceProviderAccessor.getCache();
		String msgID = null;
		String UserID = null;

		long dummy = 0;/* dummy eventID value for the remote event */

		/* persist the message to the DB and get back the MsgId */
		// msgID = cs.storeMessage(msg, String jobID, t.taskID,
		// NOTIFY_EXCEPTION);
		MsgRef mr = null;// new MsgRef(t.taskID, this.job.getID(), msgID ,
		// UserID, NOTIFY_EXCEPTION);
		RemoteEvent re = new RemoteEvent(mr, dummy, seqNum, null);
	}

	protected void notifyInformation(String msg, ServiceExertion t, long seqNum) {
		// CacheServer cs = (CacheServer) ServiceProviderAccessor.getCache();
		String msgID = null;
		String UserID = null;

		long dummy = 0;/* dummy eventID value for the remote event */

		/* persist the message to the DB and get back the MsgId */
		// msgID = cs.storeMessage(msg, String jobID, t.taskID,
		// NOTIFY_INFORMATION);
		MsgRef mr = null;// new MsgRef(t.taskID, this.job.getID(), msgID ,
		// UserID, NOTIFY_INFORMATION);
		RemoteEvent re = new RemoteEvent(mr, dummy, seqNum, null);
	}

	protected void notifyWarning(String msg, ServiceExertion t, long seqNum) {
		// CacheServer cs = (CacheServer) ServiceProviderAccessor.getCache();
		String msgID = null;
		String UserID = null;
		long dummy = 0;/* dummy eventID value for the remote event */

		/* persist the message to the DB and get back the MsgId */
		// msgID = cs.storeMessage(msg, String jobID, t.taskID, NOTIFY_WARNING);
		MsgRef mr = null;// new MsgRef(t.taskID, this.job.getID(), msgID ,
		// UserID, NOTIFY_WARNING);
		RemoteEvent re = new RemoteEvent(mr, dummy, seqNum, null);
	}

	/**
	 * Create GApp server proxy
	 */
	public ProxyProtocol createProtocol(String url) {
		ProxyProtocol protocol;
		String type = System.getProperty("portal.server");
		if (type != null)
			protocol = new ServletProtocol(type, false);
		else
			protocol = new ServletProtocol(url, false);

		return protocol;
	}

	public NetJob stopJob() throws RemoteException {

		// job.setStatus(HALTED);
		// for (int i=0;i<runningExertionIDs.size();i++) {
		// dispatcher = getDispatcher((String)runningExertionIDs.elementAt(i));
		// if (dispatcher!=null)
		// dispatcher.stopJob();
		// }
		return (NetJob)xrt;
	}

	public NetJob suspendJob() throws RemoteException {
		ExertDispatcher dispatcher = null;
		// job.setStatus(SUSPENDED);
		// for (int i=0;i<runningExertionIDs.size();i++) {
		// dispatcher = getDispatcher((String)runningExertionIDs.elementAt(i));
		// if (dispatcher!=null)
		// dispatcher.suspendJob();
		// }
		return (NetJob)xrt;
	}

	protected boolean isInterupted(Exertion ex) throws ExertionException,
			SignatureException {
		// if (job.getStatus() == FAILED) {
		// runtimeStore(job, UPDATE_EXERTION);
		// dispatchers.remove(job.getID());
		// state = FAILED;
		// return true;
		// }
		// else if (ex.getStatus() == SUSPENDED || job.getStatus() == SUSPENDED)
		// {
		// job.setStatus(SUSPENDED);
		// ex.setStatus(SUSPENDED);
		// runtimeStore(job, UPDATE_EXERTION);
		// dispatchers.remove(job.getID());
		// state = SUSPENDED;
		// return true;
		// }
		// if (job.getStatus() == HALTED) {
		// runtimeStore(job, REMOVE_JOB);
		// dispatchers.remove(job.getID());
		// state = HALTED;
		// return true;
		// }
		return false;
	}

	public NetJob resumeJob() throws RemoteException, ExertionException {
		return null;
	}

	public NetJob stepJob() throws RemoteException, ExertionException {
		return null;
	}

	// All these codes needs to be revisited
	private void setExertionFlags(Exertion ex) {
		ServiceExertion exi = (ServiceExertion) ex;
		if (exi.isJob()) {
			for (int j = 0; j < ((Job) ex).size(); j++) {
				setExertionFlags(((Job) ex).exertionAt(j));
			}
		}
	}

	protected Cataloger getCatalog() {
		try {
			if (catalog == null)
				catalog = ProviderAccessor.getCataloger();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return catalog;
	}

	public static void sendCheckPointEmail(ServiceExertion task, NetJob job) {
		// notify o MASTER task completion
		Vector recipents = null;
		String notifyees = job.getControlContext().getNotifyList(task);
		if (notifyees != null) {
			String[] list = StringUtils.tokenize(notifyees, MAIL_SEP);
			recipents = new Vector(list.length);
			for (int i = 0; i < list.length; i++)
				recipents.addElement(list[i]);
		}

		String to = "", admin = Sorcer.getProperty("sorcer.admin");
		if (recipents == null) {
			if (admin != null) {
				recipents = new Vector();
				recipents.addElement(admin);
			}
		} else if (admin != null && !recipents.contains(admin))
			recipents.addElement(admin);

		if (recipents == null || recipents.size() == 0)
			return;

		StringBuffer sb;

		sb = new StringBuffer("CHECKPOINT: SORCER Task ");

		sb.append(task.getName()).append("\n\nDescription:\n").append(
				task.getDescription()).append("\n" + task.contextToString());

		for (int i = 0; i < recipents.size() - 1; i++)
			to = to + recipents.elementAt(i) + ",";

		to = to + recipents.lastElement();

		if (to == null)
			to = Sorcer.getProperty("sorcer.admin");

		// sendMailWithSubject(sb.toString(), "Checkpoint, "+task.getName(),
		// to);
	}

	protected void reconcileInputExertions(Exertion ex) {
		ServiceExertion ext = (ServiceExertion)ex;
		if (ext.getStatus() == DONE) {
			collectOutputs(ex);
			if (inputXrts != null)
				inputXrts.removeElement(ex);
		} else {
			ext.setStatus(INITIAL);
			if (ex.isJob()) {
				for (int i = 0; i < ((Job) ex).size(); i++)
					reconcileInputExertions(((Job) ex).exertionAt(i));
			}
		}
	}

	protected void prepareJob() throws ExertionException {
		Jobs.removeExceptions((Job)xrt);
		if (xrt != null
				&& ((xrt.getStatus() == SUSPENDED)
						|| (xrt.getStatus() == INITIAL) || (xrt.getStatus() <= ERROR))) {
			ServiceExertion ft = null;
			for (int i = 0; i < ((Job)xrt).size() - 1; i++) {
				if (((Job)xrt).exertionAt(i).isTask()) {
					ft = (ServiceExertion) ((Job)xrt).exertionAt(i);
					if (ft.getStatus() != DONE
							&& xrt.getControlContext().isReview(ft)
							&& (ft != ((Job)xrt).getMasterExertion())) {
						((ServiceExertion) ((Job)xrt).exertionAt(i + 1))
								.setStatus(SUSPENDED);
						return;
					}
				}
			}
		}
	}

}
