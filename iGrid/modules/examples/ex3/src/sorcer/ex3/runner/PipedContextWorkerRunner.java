package sorcer.ex3.runner;

import sorcer.core.context.ControlContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.exertion.NetJob;
import sorcer.core.exertion.NetTask;
import sorcer.core.requestor.ExertionRunner;
import sorcer.core.signature.NetSignature;
import sorcer.service.Context;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;
import sorcer.service.Job;
import sorcer.service.Signature;
import sorcer.service.Task;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Flow;

public class PipedContextWorkerRunner extends ExertionRunner {

	public Exertion getExertion(String... args) throws ExertionException {
		String requestorName = getProperty("requestor.name");
		
		// define requestor data
		Job job = null;
		try {
			Context context1 = new ServiceContext("work1");

			context1.putValue("requestor/name", requestorName);

			context1.putValue("requestor/operand/1", 1);
			context1.putValue("requestor/operand/2", 1);
			context1.putOutValue("provider/result", 0);

			Context context2 = new ServiceContext("work2");
			context2.putValue("requestor/name", requestorName);
			context2.putValue("requestor/operand/1", 2);
			context2.putValue("requestor/operand/2", 2);
			context2.putOutValue("provider/result", 0);

			Context context3 = new ServiceContext("work3");
			context3.putValue("requestor/name", requestorName);
			context3.putInValue("requestor/operand/1", 0);
			context3.putInValue("requestor/operand/2", 0);

			// pass the parameters from one context to the next context
			// piping parameters should be annotated via in, out, or inout paths
			context1.connect("provider/result", "requestor/operand/1", context3);
			context2.connect("provider/result", "requestor/operand/2", context3);

			// define required services
			NetSignature signature1 = new NetSignature("doWork",
					sorcer.ex2.provider.Worker.class);
			NetSignature signature2 = new NetSignature("doWork",
					sorcer.ex2.provider.Worker.class);
			NetSignature signature3 = new NetSignature("doWork",
					sorcer.ex2.provider.Worker.class);

			// define tasks
			Task task1 = new NetTask("work1", signature1, context1);
			Task task2 = new NetTask("work2", signature2, context2);
			Task task3 = new NetTask("work3", signature3, context3);

			// define a job
			job = new NetJob("piped");
			job.addExertion(task1);
			job.addExertion(task2);
			job.addExertion(task3);
		} catch (Exception e) {
			throw new ExertionException("Failed to create exertion", e);
		}

		// define a job control strategy
		// use the catalog to delegate the tasks
		job.setAccessType(Access.PUSH);
		// either parallel or sequential
		job.setFlowType(Flow.SEQ);
		// time the job execution
		job.setExecTimeRequested(true);

		return job;
	}

}