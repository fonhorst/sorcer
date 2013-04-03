package sorcer.ex2.requestor;

import java.net.InetAddress;
import java.rmi.RMISecurityManager;
import java.util.logging.Logger;

import sorcer.core.context.ServiceContext;
import sorcer.core.exertion.NetJob;
import sorcer.core.exertion.NetTask;
import sorcer.core.signature.NetSignature;
import sorcer.service.Context;
import sorcer.service.Exertion;
import sorcer.service.Job;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Flow;
import sorcer.service.Task;
import sorcer.util.Log;
import sorcer.util.Sorcer;

public class WorkerJobParRequestor {

	private static Logger logger = Log.getTestLog();

	public static void main(String[] args) throws Exception {
		System.setSecurityManager(new RMISecurityManager());
		// initialize system properties
		Sorcer.getEnvProperties();

		Exertion result = new WorkerJobParRequestor()
			.getExertion().exert();
		// get contexts of component exertions - in this case tasks
		logger.info("Output context1: \n" + result.getContext("work1"));
		logger.info("Output context2: \n" + result.getContext("work2"));
		logger.info("Output context3: \n" + result.getContext("work3"));
	}

	private Exertion getExertion() throws Exception {
		String hostname = InetAddress.getLocalHost().getHostName();

		Context context1 = new ServiceContext("work1");
		context1.putValue("requstor/name", hostname);
		context1.putValue("requestor/operand/1", 1);
		context1.putValue("requestor/operand/2", 1);
		
		Context context2 = new ServiceContext("work2");
		context2.putValue("requstor/name", hostname);
		context2.putValue("requestor/operand/1", 2);
		context2.putValue("requestor/operand/2", 2);
		
		Context context3 = new ServiceContext("work3");
		context3.putValue("requstor/name", hostname);
		context3.putValue("requestor/operand/1", 3);
		context3.putValue("requestor/operand/2", 3);
		
		NetSignature signature1 = new NetSignature("doWork",
				sorcer.ex2.provider.Worker.class);
		NetSignature signature2 = new NetSignature("doWork",
				sorcer.ex2.provider.Worker.class);
		NetSignature signature3 = new NetSignature("doWork",
				sorcer.ex2.provider.Worker.class);
		
		Task task1 = new NetTask("work1", signature1, context1);
		Task task2 = new NetTask("work2", signature2, context2);
		Task task3 = new NetTask("work3", signature3, context3);
		Job job = new NetJob();
		job.addExertion(task1);
		job.addExertion(task2);
		job.addExertion(task3);
		job.setFlow(Flow.PAR);
		job.setAccess(Access.PULL);
		return job;
	}
}
