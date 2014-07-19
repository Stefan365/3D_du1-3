package DU2;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
public class Clienta extends JPanel {// implements Runnable {

    public ExecutorService SPRAVCE_C = Executors.newCachedThreadPool();

    /**
     * Indikator pripojenia so serverom.
     */
    private int conn = 0;
    /**
     * Textové pole pro zadání zprávy od uživatele.
     */
    private final String meno_klienta;
    /**
     * Textové pole pro zadání zprávy od uživatele.
     */
    private JTextField enterField;
    /**
     * Textová oblast zobrazující zprávy a další informace uživateli.
     */
    private final JTextArea displayArea;

    /**
     * Výstupní TCP proud směrem k serveru.
     */
    private DataOutputStream output;

    /**
     * Vstupní TCP proud směrem od serveru. bude slouzit jenom na prijem
     * informace o chatovacich mistnostech.
     */
    private DataInputStream input;

    /**
     * Nazov serveru.
     */
    private String chatServer;

    /**
     * Soket, pomocí kterého komunikuje klient se serverem.
     */
    private Socket client;
    private final JLabel room_label;//Label, ktory ukazuje v akej miestnosti sa klient momentalne nachadza.
    //Multicast:
    private MulticastSocket socketCR;    // vytvoření soketu pro prijimanie paketu
    private InetAddress mGroup;    // příprava adresy multicastové skupiny
    private DatagramPacket packetCR; //paket na UDP spojeni
    //pripojenie na multicast:
    private int cisloClPortu;
    private String multicastAddress;
    private int room_id;

    //Hlavna.
    /**
     * Spusti vlakno klienta.
     * @param args
     */
    public static void main(String[] args) {
        Clienta client = new Clienta();
        //System.out.println("inst_counter:" + inst_counter);

        JFrame frame = new JFrame(client.getMeno());
        //frame.setLayout(new GridLayout(2,1));
        frame.setLayout(new BorderLayout());

        frame.add(client);//.add(client);
        //frame.add(new JLabel());
        frame.setSize(300, 460);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    //0.Konstruktor
    /**
     * Vytvoří novou instanci třídy TCPClient a inicializuje prvky uživatelského
     * rozhraní.
     *
     */
    public Clienta() {
        super();
        this.setLayout(new BorderLayout());

        //nacti meno klienta:
        this.meno_klienta = this.ctiMeno();

        enterField = new JTextField();
        enterField.setEditable(false);
        enterField.addActionListener(new ActionListener() {
            // zaslání zprávy serveru
            public void actionPerformed(ActionEvent e) {
                sendData(e.getActionCommand());
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
                //try {
                if (conn == 0) {
                    connectToServerTCP();//tato vola zadavacie okna na miesto pripojenia.
                    displayMessage("\nPripojil som sa na server "
                        + client.getInetAddress().getHostName());

                    //nacitanieCisloRoomu:
                    doTheConnectionUDP(ctiCisloRoom());
                    updateLabel("" + room_id);
                    conn = 1;

                    Clienta.ServisC_receive servCR = new Clienta.ServisC_receive();

                    //Spustenie vlakna:
                    SPRAVCE_C.execute(servCR);
                    setTextFieldEditable(true);
                }
                //} //catch (java.net.UnknownHostException uhex) {
                // displayMessage("\nPokusas sa pripojit k neexistujucemu miestu");
                //} catch (IOException ex) {
                //    displayMessage("\n2.Pokusas sa pripojit k neexistujucemu miestu");
                //}
            }
        });

        //2.tlacitko:
        JButton odpojSa = new JButton("DISC");
        odpojSa.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (conn == 1) {
                    closeConnectionUDP();
                    closeConnectionTCP();
                    updateLabel("NA");
                    conn = 0;
                }
            }
        });

        //3.tlacitko:
        JButton changeRoom = new JButton("CH.ROOM");
        changeRoom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (conn == 1) {
                    changeRoom();
                    updateLabel("" + room_id);
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

    //C.1.
    /**
     * Zmeni chatovaciu miestnost pri stavajucom TCP pripojeni.
     *
     */
    private void changeRoom() {

        //uzavrie stare multicast spojenie so serverom.
        setTextFieldEditable(false);
        closeConnectionUDP();

        //vytvori nove multicast connection
        doTheConnectionUDP(ctiCisloRoom());

        //upravi label:
        updateLabel("" + room_id);

        //vytvorenie a spustenie noveho vlakna na multicast.
        Clienta.ServisC_receive nova_rec = new Clienta.ServisC_receive();
        SPRAVCE_C.execute(nova_rec);

        //odblokovanie okna na pisanie sprav.
        setTextFieldEditable(true);
    }

    //C.A. 
    /**
     * Druha vnutorna trieda, Multicast.
     */
    private class ServisC_receive implements Runnable {

        //C.A.1.
        @Override
        public void run() {
            receiveC();
        }
    }

    //C.3. ...A.
    /**
     * Vytvori multicastovy soket s pripojenim na server.
     *
     * @param r_id cislo vysielaciej miestnosti
     */
    public void doTheConnectionUDP(int r_id) {
        try {
            String str;
            output.writeUTF(r_id + "");
            output.flush();
            //System.out.println("poslal som spravu serveru s poziadavkou na UDP udaje.");
            str = input.readUTF();
            
            String[] ooo = str.split("x");
            //System.out.println("9.str:" + ooo[0]);
            //System.out.println("10.str:" + ooo[1]);
            
            //napojenie sa na danu miestnost:
            multicastAddress = ooo[0];
            cisloClPortu = Integer.parseInt(ooo[1]);
            room_id = Integer.parseInt(ooo[2]);

            mGroup = InetAddress.getByName(multicastAddress);//napr: "239.0.0.17";
            socketCR = new MulticastSocket(cisloClPortu);//2060
            socketCR.joinGroup(mGroup);

        } catch (UnknownHostException ex) {
            System.out.println("chyba. UnknownHostException doTheconnectionB, 1.");
        } catch (IOException ex) {
            System.out.println("chyba. IOEx doTheconnectionB, 2.");
        }
    }

//C.4......B
    /**
     * Připojí se k serveru na adrese předané do konstruktoru.
     *
     * @throws IOException pokud dojde během připojování k chybě.
     */
    public void connectToServerTCP() {
        String[] str = new String[2];
        //str[0] = "127.0.0.1";
        //str[1] = "8081";
        str = this.ctiIPport();

        displayMessage("Zkouším spojení.\n");
        try {

            client = new Socket(InetAddress.getByName(str[0]), Integer.parseInt(str[1]));
            // zobrazení informace o spojení
            displayMessage("Připojeno k: " + client.getInetAddress().getHostName());
            getStreams();
            this.chatServer = str[0];
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Zle si zadal kombinaciu IP/port, opakuj prosim zadanie");
            connectToServerTCP();//str = this.ctiIPport();
        }

    }

    //C.5.....A
    /**
     * Prijme spravu z multicastu a vypise jej obsah
     *
     */
    public void receiveC() {
        try {
            while (true) {
                byte[] buffer = new byte[32];
                packetCR = new DatagramPacket(buffer, 32);
                socketCR.receive(packetCR);

                //vypise to, co prijalo:
                String msg = new String(packetCR.getData());
                displayMessage("\n" + msg);
            }

        } catch (IOException ex) {
            System.out.println("prijimanie soketu zlyhalo, ServisC_receive 1.");
        }
    }

    //C.6.....B
    /**
     * Odešle zprávu serveru.
     *
     * @param message zpráva, která se odešle na server.
     */
    private void sendData(String message) {
        try {
            output.writeUTF(this.meno_klienta + ">>> " + message + room_id);
            output.flush();
            System.out.println(" 1. message:" + message);
        } catch (IOException e) {
            displayArea.append("\nChyba při odesílání: " + e.toString());
        }
    }

    //C.7.....B
    /**
     * Získá ze soketu proudy pro odesílání a příjem dat.
     *
     * @throws IOException pokud dojde při získávání proudů k chybě.
     */
    public void getStreams() throws IOException {
        input = new DataInputStream(client.getInputStream());
        output = new DataOutputStream(client.getOutputStream());

        //displayMessage("\nZískány vstupní a výstupní proud.\n");
    }

    //C.8.....B
    /**
     * Uzavře proudy a soket TCP.
     */
    private void closeConnectionTCP() {
        if (conn == 1) {
            displayMessage("\nUzavírám spojení se serverom.");

            // zrušení možnosti editovat vstupní pole
            setTextFieldEditable(false);

            try {
                output.close();
                client.close();
                conn = 0;
                displayMessage("\nUkoncil som spojení se serverom.");
            } catch (IOException e) {
                displayMessage("\nNepodatilo sa uzavriet TCP soket. clostConnectionTCP, 1.");
            }
        }
    }

    //C.9.....A
    /**
     * Uzavře proudy a soket Multicastu.
     */
    private void closeConnectionUDP() {
        //if (conn == 1) {
        displayMessage("\nUzavírám UDP spojení se serverom.");

        // zrušení možnosti editovat vstupní pole
        setTextFieldEditable(false);

        try {
            socketCR.leaveGroup(mGroup);
            socketCR.close();
            displayMessage("\nUkoncil som UDP spojení se serverom.");
        } catch (IOException ex) {
            displayMessage("\nNepodarilo sa zavriet UDP spojení se serverom.");
        }
    }

    //C.10.
    /**
     * Přidá daný řetězec do textové oblasti. K úpravě dochází v event dispatch
     * vlákně.
     *
     * @param messageToDisplay zpráva, která se má přidat do textové oblasti.
     */
    private void displayMessage(final String messageToDisplay) {
        // final je tam proto, aby se k danému parametru dalo
        // přistoupit z metody vnitřní třídy
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                displayArea.append(messageToDisplay);
            }
        });
    }

    //C.11.
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

    //C.12.
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

    //C.13.
    /**
     * Načíta IP adresu servera a cislo portu
     *
     * @return načítané meno
     */
    public final String[] ctiIPport() {
        String[] zoz = new String[2];
        zoz[0] = JOptionPane.showInputDialog(null, "Zadaj IP adresu serveru: ",
            "IP SERVERU", 1);
        zoz[1] = JOptionPane.showInputDialog(null, "Zadaj cislo portu: ",
            "CISLO PORTU", 1);
        return zoz;
    }

    //C.14.
    /**
     * Načíta cislo chatovacieho roomu
     *
     * @return načítané cislo roomu
     */
    public final int ctiCisloRoom() {
        //opakuje sa, dokym sa nenapoji.
        String str;
        try {
            str = JOptionPane.showInputDialog(null, "Zadaj cislo chat room : ",
                "CISLO CHAT ROOMu", 1);

            return Integer.parseInt(str);
        } catch (java.lang.NumberFormatException ex) {
            return ctiCisloRoom();
        }
    }

//    private String getFirstChar(String message_reca) {
    //   char room_i = message_reca.charAt(0);
    //     return room_i + "";
    //   }
    //C.15.
    /**
     * Vrati meno klienta
     *
     * @return meno klienta
     */
    public String getMeno() {
        return this.meno_klienta;
    }

    //C.16.
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
}
