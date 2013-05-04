package sorcer.service;

import net.jini.core.transaction.TransactionException;

import java.rmi.RemoteException;
import java.util.concurrent.Callable;

public class ExertionCallable implements Callable<Exertion> {
	private Exertion exertion;

	public ExertionCallable(Exertion exertion) {
		this.exertion = exertion;
	}

	public Exertion call() throws RemoteException, TransactionException,
			ExertionException {
		if (exertion != null)
			return exertion.exert(null);

		return exertion;
	}

	public Exertion getExertion() {
		return exertion;
	}
}