package DU3;

import java.net.MalformedURLException;
import java.rmi.AccessException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Serverb  {// UnicastRemoteObject implements Rozposli {

    /**
     * lokalne prijatie mennej sluzby.
     */
    private Registry reg_s;

    /**
     * instancia rozhrania Zobraz, pomocou kt. komunikuje s klientom.
     */
    private Zobraz service_cl;

    
    /**
     * menna sluzba pre vsetky vzdialene objekty.
     */
    public Registry reg_A;// = LocateRegistry.createRegistry(1099);

    /**
     * zoznam zoznamov mien klientov. Poradie zoznamov mien urcuje chatovaciu
     * miestnost tj. 0, 1, 2, 3, ect...
     */
    public List<List<String>> zoz_klientov = new ArrayList<>();

    /**
     * pocet vytvorenych klientov. kvoli prideleniu jednoznacneho mena.
     */
    private int pocet_klientov = 0;

    /**
     * IP adresa a cislo portu na ktorych bezi menny server.
     */
    public String nameS_IP;// = "localhost";
    public int nameS_cPortu;// = 1099;

    //Hlavna:
    public static void main(String[] args) throws RemoteException, MalformedURLException, NotBoundException {
        Serverb sb = new Serverb();
        sb.initServ("localhost", 1099, 3);

        System.out.println("Skoncil som inicializaciu Chat miestnosti");
        //1.klient:

        while (true);

    }

    //Konstruktor:
    public Serverb() throws RemoteException {
        super();
    }

    //1.
    /**
     * Ma za ulohu vytvorit napojenie na mennu sluzbu, ktora bude odosielat
     * poziadavky klientom.
     *
     * @param menoCl meno daneho klienta(instancie) pod ktorym je zaregistrovany
     * na mennej sluzbe.
     * @throws java.rmi.RemoteException
     * @throws java.net.MalformedURLException
     * @throws java.rmi.NotBoundException
     */
    public void initServToClient(String menoCl) throws RemoteException, MalformedURLException, NotBoundException {
        //musi byt napojeny na ten isty menny server ako chat miestnosti.
        if (reg_s == null) {
            reg_s = LocateRegistry.getRegistry(nameS_IP, nameS_cPortu);
        }
        //vytvaranie Stub-u:
        service_cl = (Zobraz) reg_s.lookup(menoCl); // vrací objekt Remote - musím pøety                       
    }

    //RUN ROOMS:
    //1R.
    /**
     * Ma za ulohu vytvorit mennu sluzbu, ktora bude osluhovat vs. ucastnikov.
     *
     * @param IP IP adresa kde ma byt spusteny Name server.
     * @param port cislo portu.
     * @param pocetM pocet chatovacich miestnosti
     * @throws java.rmi.RemoteException
     * @throws java.net.MalformedURLException
     */
    public void initServ(String IP, int port, int pocetM) throws RemoteException, MalformedURLException {
        nameS_IP = IP;
        nameS_cPortu = port;

        //toto staci vytvorit len raz, a bude to platit pre vsetky vlakna.:
        reg_A = LocateRegistry.createRegistry(port);
        this.zaregistujMiestnosti(pocetM);//nakrmZoznamMiestnosti(pocetM);
    }
    
    //2R.
    /**
     * Ma za ulohu zaregistrovat meno Serverb v mennej sluzbe, ktora bude osluhovat mestnosti.
     *
     * @param pocetM pocet chatovacich miestnosti
     * @throws java.rmi.RemoteException
     */
    private void zaregistujMiestnosti(int pocetM) throws RemoteException {
        String url;
        String pol_url = "rmi://" + nameS_IP + ":" + nameS_cPortu + "/";
        url = pol_url + "M";
        //iniciallizacia danych miestnosti:
        try {
            //registracia danych miestnosti: 
            Naming.rebind(url, new Serverb.RozposliImpl());
        } catch (MalformedURLException ex) {
            System.out.println("nepodarilo sa zaregistrovat miestnosti ");
        }

        //iniciallizacia danych miestnosti:
        for (int i = 0; i < pocetM; i++) {
            List<String> zoz = new ArrayList();
            this.zoz_klientov.add(zoz);
        }
    }

    /**
     * Vnutorna trieda implementujuca rozhranie 'Rozposli'. Ta je tu kvoli tomu, 
     * aby bolo mozne vytvorit viac instancii Sreverb naraz a vsetky vnutorne data boli separovane.
     */
    private class RozposliImpl extends UnicastRemoteObject implements Rozposli {

        public RozposliImpl() throws RemoteException {
            super();
        }

        @Override
        public void rozposli(String sprava, int kam) throws RemoteException, AccessException {
            
            Iterator itr = zoz_klientov.get(kam).iterator(); //chcem vybrat cisla miestnosti.
            
            String meno_cl;
            
            while (itr.hasNext()) {
                meno_cl = (String) itr.next();
                System.out.println(kam + " : " + meno_cl);
                try {
                    initServToClient(meno_cl);
                    //pozn.: vysiela aj do zavrenych okien, ale to neva.
                    service_cl.zobraz(sprava);
                } catch (MalformedURLException ex) {
                    System.out.println("MalformedURLException, rozposli 1.");
                } catch (NotBoundException ex) {
                    System.out.println("NotBoundException, rozposli 2.");
                }
            }
        }

        @Override
        public void zapisDoZoznamu(String menoCl, int room) throws RemoteException {
            for (int i = 0; i < zoz_klientov.size(); i++) {
                if (i == room) {
                    if ((zoz_klientov.get(i)).contains(menoCl) == false) {
                        (zoz_klientov.get(i)).add(menoCl);
                    }
                } else { //dany clovek moze byt naraz len v 1 miestnosti:
                    if ((zoz_klientov.get(i)).contains(menoCl) == true) {
                        (zoz_klientov.get(i)).remove(menoCl);
                    }
                }
            }
        }

        @Override
        public int ziskajCisloCl() throws RemoteException {
            int room = pocet_klientov;
            pocet_klientov++;
            return room;
        }

        @Override
        public int upravRoomId(int room_i) throws RemoteException {
            int r_i = room_i;
        
            if (room_i < 0) {
                r_i = 0;
            } else if (room_i > zoz_klientov.size() - 1) {
                r_i = zoz_klientov.size() - 1;
            }
            return r_i;
        }
    }

}
