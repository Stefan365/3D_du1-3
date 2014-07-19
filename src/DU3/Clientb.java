package DU3;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 *
 * Vnutorna trieda predstavujuca klienta.
 */
public class Clientb extends JPanel {//implements Zobraz {

    /**
     * Indikacia spojenia.
     */
    private int conn = 0;
    /**
     * Viditelne meno klienta.
     */
    private final String meno_klienta;

    /**
     * meno klienta, pod ktorym je zaregistrovany v mennej sluzbe.
     */
    private String meno_klienta_id;

    /**
     * Textové pole pro zadání zprávy od uživatele.
     */
    private JTextField enterField;
    
    /**
     * Textová oblast zobrazující zprávy a další informace uživateli.
     */
    private final JTextArea displayArea;

    /**
     * Label, ktory ukazuje v akej miestnosti sa klient momentalne nachadza.
     */
    private final JLabel room_label;

    /**
     * Objekt zodpovedajuci vzdialenemu objektu RMI.
     */
    Rozposli service_ser;
    /**
     * lokalne prijatie mennej sluzby.
     */

    private Registry reg_c;
    /**
     * Cislo chatovacej miestnosti, kde je klient pripojeny.
     */
    public int room_id;
    /**
     * Vnutorna trieda, ktora implementuje rozhranie Zobraz.
     */
    private ZobrazImpl zobraz;// = new ZobrazImpl();

    /**
     * IP adresa a cislo portu na ktorych bezi menny server.
     */
    public String IP_c;// = "localhost";
    public int cisloClPortu_c;// = 1099;

    //3.
    /**
     * Hlavna.
     *
     * @param args
     * @throws java.rmi.RemoteException
     *
     */
    public static void main(String[] args) throws RemoteException {

        Clientb cl = new Clientb();
        JFrame frame = new JFrame(cl.getMeno());
        frame.setLayout(new BorderLayout());

        frame.add(cl);

        frame.setSize(300, 460);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

    }

    //0.Konstruktor
    /**
     * Vytvoří novou instanci třídy TCPClient a inicializuje prvky uživatelského
     * rozhraní.
     *
     * @throws java.rmi.RemoteException
     */
    public Clientb() throws RemoteException {
        super();

        this.setLayout(new BorderLayout());

        //nacti meno klienta:
        this.meno_klienta = this.ctiMeno();

        enterField = new JTextField();
        enterField.setEditable(false);
        enterField.addActionListener(new ActionListener() {
            // zaslání zprávy serveru
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (room_id != -1) {
                        service_ser.rozposli(meno_klienta_id + ">>> " + e.getActionCommand(), room_id);
                    }
                } catch (RemoteException ex) {
                    displayMessage("ahoj");
                }
                enterField.setText("");
            }
        });

        this.add(enterField, BorderLayout.NORTH);

        // vytvoření textové oblasti pro zobrazování výstupu
        displayArea = new JTextArea();
        this.add(new JScrollPane(displayArea), BorderLayout.CENTER);
        // vytvoření textového pole pro zadání zprávy

        this.setSize(300, 300);

        //1.tlacitko:
        JButton pripojSa = new JButton("CONN");
        pripojSa.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (conn == 0) {
                    try {
                        connectToNameServer();
                        vytvorZobrazRMIpreKlienta();
                        service_ser.zapisDoZoznamu(meno_klienta_id, room_id);
                        updateLabel("" + room_id);
                        conn = 1;
                        setTextFieldEditable(true);
                    } catch (RemoteException ex) {
                        displayMessage("RemoteException");
                    } catch (MalformedURLException ex) {
                        displayMessage("MalformedURLException");
                    } catch (IOException ex) {
                        displayMessage("IOException");
                    } catch (NotBoundException ex) {
                        displayMessage("NotBoundException");
                    }
                }
            }
        });

        //2.tlacitko:
        JButton odpojSa = new JButton("DISC");
        odpojSa.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (conn == 1) {
                    setTextFieldEditable(false);
                    updateLabel("NA");
                    try {
                        //vymaz zo zoznamu:
                        service_ser.zapisDoZoznamu(meno_klienta_id, -1);
                        //Naming.unbind(meno_klienta_id);
                        reg_c.unbind(meno_klienta_id);
                        conn = 0;
                        room_id = -1;
                        reg_c = null;
                        service_ser = null;
                    } catch (RemoteException ex) {
                        displayMessage("RemoteException");
                    } catch (NotBoundException ex) {
                        displayMessage("NotBoundException");
                    }

                }
            }
        });

        //3.tlacitko:
        JButton changeRoom = new JButton("CH.ROOM");
        changeRoom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (conn == 1) {
                    try {
                        changeRoom();
                        //updateLabel("" + room_id);
                    } catch (RemoteException ex) {
                        displayMessage("RemoteException, CHROOM, 1.");
                    } catch (NotBoundException ex) {
                        displayMessage("NotBountException, CHROOM, 2.");
                    } catch (MalformedURLException ex) {
                        displayMessage("MalformedURLException, CHROOM, 3.");
                    }
                }
            }
        });

        //4.room label 
        room_label = new JLabel("Room:NA");

        JPanel panel_b = new JPanel();
        panel_b.setSize(300, 100);
        panel_b.setLayout(new FlowLayout());

        panel_b.add(pripojSa);
        panel_b.add(odpojSa);
        panel_b.add(changeRoom);
        panel_b.add(room_label);

        this.add(panel_b, BorderLayout.SOUTH);

    }
    //1.A

    /**
     * inicializuje 'pripojenie sa' na danu chat miestnost.
     *
     * @param IP IP adresa kde ma byt spusteny Name server.
     * @param port cislo portu.
     * @param menoR meno chat miestnosti.
     *
     * @throws java.rmi.RemoteException
     * @throws java.net.MalformedURLException
     * @throws java.rmi.NotBoundException
     */
    public void initClientRoom(String IP, int port, String menoR) throws RemoteException, MalformedURLException, NotBoundException {
        this.IP_c = IP;
        this.cisloClPortu_c = port;

        if (reg_c == null) {
            reg_c = LocateRegistry.getRegistry(IP, port);
        }
        service_ser = (Rozposli) reg_c.lookup(menoR);
        
        //dostane nove meno.
        this.meno_klienta_id = "C" + service_ser.ziskajCisloCl();
         
    }

    //1.B
    /**
     * inicializuje 'pripojenie sa' na danu chat miestnost.
     *
     * @param menoR meno chat miestnosti.
     * @throws java.rmi.RemoteException
     * @throws java.net.MalformedURLException
     * @throws java.rmi.NotBoundException
     */
    public void initClientRoom(String menoR) throws RemoteException, MalformedURLException, NotBoundException {
        if (reg_c == null) {
            reg_c = LocateRegistry.getRegistry(this.IP_c, this.cisloClPortu_c);
        }
        service_ser = (Rozposli) reg_c.lookup(menoR);
    }

    //2.
    /**
     * Zaregistruje instanciu klienta na mennom serveri.
     */
    private void vytvorZobrazRMIpreKlienta() throws RemoteException {
        String url;
        String pol_url = "rmi://" + this.IP_c + ":" + this.cisloClPortu_c + "/";

        url = pol_url + meno_klienta_id;

        //vytvori instanciu ZobrazImpl v prislusnej instancii Clientb.
        //Musi to byt takto, inak by dana instancia ZobrazImpl nemala dosah na Clientb.displayMessage dotycne
        //instancie klienta.
        this.initZI();

        try {
            //zaregistruje danu url na Name serveri:
            Naming.rebind(url, this.zobraz);
        } catch (MalformedURLException ex) {
            System.out.println("nepodarilo sa zaregistrovat miestnost c1.:" + this.meno_klienta_id);
        }
    }

    //3.
    /**
     * Ma za ulohu updatnut label s oznacenim cisla roomu, kde je klient
     * pripojeny.
     *
     * @param lab cislo miestnosti.
     */
    private void updateLabel(String lab) {
        String str = "Room:" + lab;
        room_label.setText(str);
    }

    //4.
    /**
     * Zmeni chatovaciu miestnost pri stavajucom pripojeni.
     */
    private void changeRoom() throws RemoteException, MalformedURLException, NotBoundException {

        setTextFieldEditable(false);

        int pre_room_id = ctiCisloRoom();
        room_id = this.service_ser.upravRoomId(pre_room_id);

        System.out.println("pre_room_id: " + pre_room_id);
        this.service_ser.zapisDoZoznamu(meno_klienta_id, room_id);
        System.out.println("room_id: " + room_id);

        //upravi label:
        updateLabel("" + room_id);

        //odblokovanie okna na pisanie sprav.
        setTextFieldEditable(true);
    }

    //5.
    /**
     * Přidá daný řetězec do textové oblasti. K úpravě dochází v event dispatch
     * vlákně.
     *
     * @param messageToDisplay zpráva, která se má přidat do textové oblasti.
     */
    public void displayMessage(final String messageToDisplay) {
        // final je tam proto, aby se k danému parametru dalo
        // přistoupit z metody vnitřní třídy
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                displayArea.append(messageToDisplay);
            }
        });
    }

    //6.
    /**
     * Změní stav editable textového pole. K úpravě dochází v event dispatch
     * vlákně.
     *
     * @param editable true, pokud má být obsah textového pole upravovatelný,
     * false jinak.
     */
    private void setTextFieldEditable(final boolean editable) {
        // final je tam proto, aby se k danému parametru dalo
        // přistoupit z metody vnitřní třídy
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                enterField.setEditable(editable);
                if (editable == true) {
                    //displayMessage("\nUmoznujem Editovat!");
                } else {
                    //displayMessage("\nZnemoznujem Editovat!");
                }
            }
        });
    }

    //7.
    /**
     * Načíta Meno klienta
     *
     * @return načítané meno
     */
    public final String ctiMeno() {
        String str = JOptionPane.showInputDialog(null, "Zadaj meno noveho Klienta : ",
            "MENO KLIENTA", 1);
        return str;
    }

    //8.
    /**
     * Načíta cislo chatovacieho roomu
     *
     * @return načítané cislo roomu
     */
    public final int ctiCisloRoom() {
        //opakuje sa, dokym nenacita cele cislo.
        String str;
        try {
            str = JOptionPane.showInputDialog(null, "Zadaj cislo chat room : ",
                "CISLO CHAT ROOMu", 1);
            return Integer.parseInt(str);
        } catch (java.lang.NumberFormatException ex) {
            return ctiCisloRoom();
        }
    }

    /**
     * Vnutorna trieda implementujuca rozhranie 'Zobraz'.
     */
    private class ZobrazImpl extends UnicastRemoteObject implements Zobraz {

        public ZobrazImpl() throws RemoteException {
            super();
        }

        @Override
        public void zobraz(String sprava) throws RemoteException {
            displayMessage("\n" + sprava);
        }
    }

    //9.
    /**
     * Inicializuje zobraz.
     * @throws java.rmi.RemoteException
     */
    public void initZI() throws RemoteException {
        this.zobraz = new ZobrazImpl();
    }

    //10.
    /**
     * Vrati meno klienta
     *
     * @return meno klienta
     */
    public String getMeno() {
        return this.meno_klienta;
    }

    //11.
    /**
     * Načíta IP adresu servera a cislo portu
     *
     * @return načítany zoznam.
     */
    public final String[] ctiIPport() {
        String[] zoz = new String[2];
        zoz[0] = JOptionPane.showInputDialog(null, "Zadaj IP adresu kde bude bezat menna sluzba: ",
            "IP NAME_SERVIS", 1);
        zoz[1] = JOptionPane.showInputDialog(null, "Zadaj cislo portu: ",
            "CISLO PORTU", 1);
        return zoz;
    }

    //12.
    /**
     * Připojí se k serveru na adrese předané do konstruktoru.
     *
     * @throws IOException pokud dojde během připojování k chybě.
     * @throws java.net.UnknownHostException
     * @throws java.rmi.RemoteException
     * @throws java.net.MalformedURLException
     * @throws java.rmi.NotBoundException
     */
    public void connectToNameServer() throws IOException, java.net.UnknownHostException, RemoteException, MalformedURLException, NotBoundException {
        String[] str;// = new String[2];
        //str[0] = "localhost";
        //str[1] = "1099";
        str = this.ctiIPport();

        int pre_room_id = ctiCisloRoom();
        displayMessage("Zkouším spojení.\n");
        try {
            initClientRoom(str[0], Integer.parseInt(str[1]), "M");
            // zobrazení informace o spojení
            displayMessage("Připojeno k: " + str[0] + " na portu: " + str[1]);
            //upravenie cisla roomu podla poctu chat. miestnosti:
            room_id = this.service_ser.upravRoomId(pre_room_id);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Zle si zadal kombinaciu IP/port, opakuj prosim zadanie");
            connectToNameServer();
        }
    }
}
