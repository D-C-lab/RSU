package ac.ds.wstest.rmiTest;

import java.rmi.Remote; 
import java.rmi.RemoteException; 

public interface I_Hello extends Remote { 
	String sayHello() throws RemoteException; 
}


