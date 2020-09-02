package ac.ds.wstest.rmiTest;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class S_Hello implements I_Hello {
	public S_Hello(){}

	public String sayHello() { 
		return "Hello, world!"; 
	} 

	public static void main(String[] args) {
		S_Hello obj = new S_Hello();
		try {
			I_Hello stub = (I_Hello) UnicastRemoteObject.exportObject(obj, 0);

			// Bind the remote object's stub in the registry
			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("Hello", stub);
			System.out.println("Server ready");
		} catch (Exception e) {
			System.out.println("Server exception: " + e.toString()); 
	  	} 
	} 
}


