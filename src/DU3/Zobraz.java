/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package DU3;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author Stefan
 * 
 * na vzdialenom objekte (
 */
public interface Zobraz extends Remote {
    
        //Klient: zobrazi prijatu spravu.
    	public void zobraz(String sprava) throws RemoteException;
        
        //public int sum(int[] in) throws RemoteException;
}
