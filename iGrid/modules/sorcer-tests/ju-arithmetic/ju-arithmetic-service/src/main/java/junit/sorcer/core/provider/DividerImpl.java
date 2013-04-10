package junit.sorcer.core.provider;

import java.rmi.RemoteException;

import sorcer.service.Context;
import sorcer.service.ContextException;

public class DividerImpl implements Divider {
	Arithmometer arithmometer = new Arithmometer();

	public Context divide(Context context) throws RemoteException,
			ContextException {
		return arithmometer.divide(context);
	}
}
