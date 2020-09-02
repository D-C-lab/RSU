package ac.ds.wstest.rmiTest;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class C_Hello {
	private C_Hello() {} 

	public static void main(String[] args) {
		//String host = (args.length < 1) ? null : args[0];

		try { 
			Registry registry = LocateRegistry.getRegistry("localhost");
			I_Hello stub = (I_Hello) registry.lookup("Hello");
			String response = stub.sayHello();
			System.out.println("response: " + response);
		} catch (Exception e) { 

			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}
	}
}


