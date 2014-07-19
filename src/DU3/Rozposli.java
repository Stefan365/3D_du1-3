/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package DU3;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author User
 */
public interface Rozposli extends Remote {
    
        //pre server:
        public void rozposli(String sprava, int kam) throws RemoteException;
        
        //pre server:
        public void zapisDoZoznamu(String menoCl, int room) throws RemoteException;
        
        //pre server:
        public int ziskajCisloCl () throws RemoteException;
        
        //pre server:
        public int upravRoomId (int room_i) throws RemoteException;
}
