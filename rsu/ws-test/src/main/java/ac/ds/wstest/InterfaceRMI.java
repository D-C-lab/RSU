package ac.ds.wstest;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface InterfaceRMI extends Remote 
{
    public String ServerContainer(String message) throws Exception;
}
