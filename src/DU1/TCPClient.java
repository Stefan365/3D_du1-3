/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package DU1;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;

/**
 *
 * @author Stefan Veres
 */
public class TCPClient extends JPanel {

    /**
     * Indikator pripojenia.
     */
    private int conn = 0;
    /**
     * Textové pole pro zadání zprávy od uživatele.
     */
    private String meno_klienta;
    /**
     * Textové pole pro zadání zprávy od uživatele.
     */
    private JTextField enterField;
    /**
     * Textová oblast zobrazující zprávy a další informace uživateli.
     */
    private JTextArea displayArea;
    /**
     * Výstupní proud směrem k serveru.
     */
    private DataOutputStream output;
    /**
     * Vstupní proud směrem od serveru.
     */
    private DataInputStream input;
    /**
     * Nazov serveru.
     */
    private String chatServer;
    /**
     * Spravca vlakien.
     */
    public static ExecutorService SPRAVCE_K = Executors.newCachedThreadPool();
    /**
     * Soket, pomocí kterého komunikuje klient se serverem.
     */
    private Socket client;

    public static void main(String[] args) {
        // vytvoření klienta
        TCPClient client = new TCPClient();

        JFrame frame = new JFrame(client.getMeno());
        //frame.setLayout(new GridLayout(2,1));
        frame.setLayout(new BorderLayout());
        
        frame.add(client);
        
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
    public TCPClient() {
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
        JButton pripojSa = new JButton("PRIPOJ SA");
        pripojSa.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (conn == 0) {

                        connectToServer();
                        displayMessage("\nPripojil som sa na server "
                            + client.getInetAddress().getHostName()
                            //+ client.getInetAddress().getCanonicalHostName()
                            //+ client.getInetAddress().getAddress()
                            //+ client.getInetAddress().getHostAddress()
                            );

                        getStreams();
                        conn = 1;

                        ServisC servC = new ServisC();
                        //Spustenie vlakna:
                        //Spustenie v sprave SPRAVCE:
                        SPRAVCE_K.execute(servC);
                    }
                } catch (java.net.UnknownHostException uhex) {
                    displayMessage("\nPokusas sa pripojit k neexistujucemu miestu");
                } catch (IOException ex) {
                    displayMessage("\n2.Pokusas sa pripojit k neexistujucemu miestu");
                } 
            }
        });

        //2.tlacitko:
        JButton odpojSa = new JButton("ODPOJ SA");
        odpojSa.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (conn == 1) {//displayMessage("\nclose1");
                    closeConnection();
                    conn = 0;
                }
            }
        });


        JPanel panel_b = new JPanel();
        panel_b.setSize(300, 100);
        panel_b.setLayout(new FlowLayout());

        panel_b.add(pripojSa);
        panel_b.add(odpojSa);

        this.add(panel_b, BorderLayout.SOUTH);
    }

    //1.
    /**
     * Připojí se k serveru na adrese předané do konstruktoru.
     *
     * @throws IOException pokud dojde během připojování k chybě.
     */
    public void connectToServer() throws IOException, java.net.UnknownHostException {
        String[] str = new String[2];
        //str[0] = "127.0.0.1";
        //str[1] = "8080";
        str = this.ctiIPport();

        displayMessage("Zkouším spojení.\n");
        //try {

            // vytvoření soketu a navázání spojení
            client = new Socket(InetAddress.getByName(str[0]), Integer.parseInt(str[1]));
            this.chatServer = str[0];

            // zobrazení informace o spojení
            displayMessage("Připojeno k: " + client.getInetAddress().getHostName());
        //} catch (java.net.UnknownHostException uhex) {
        //    displayMessage("\nDane pripojenie na server neexistuje! ");
        //}
    }

    //2.
    /**
     * Získá ze soketu proudy pro odesílání a příjem dat.
     *
     * @throws IOException pokud dojde při získávání proudů k chybě.
     */
    public void getStreams() throws IOException {
        output = new DataOutputStream(client.getOutputStream());
        input = new DataInputStream(client.getInputStream());

        //displayMessage("\nZískány vstupní a výstupní proud.\n");
    }

    //3.
    /**
     * Zpracování spojení se serverem
     *
     * @throws IOException pokud při komunikaci se serverem dojde k chybě.
     */
    private void processConnection() throws IOException, java.net.SocketException {
        String message;

        // povolení editace vstupního pole
        setTextFieldEditable(true);

        // zpracování zpráv od serveru
        do {
            // načtení zprávy
            // blokující čekání
            message = input.readUTF();

            // zobrazení správy
            //displayMessage("\n" + message);
            displayMessage(message);

        } while (true);//(!message.equals("SERVER>>> TERMINATE"));
    }

    //4.
    /**
     * Uzavře proudy a soket.
     */
    private void closeConnection() {
        if (conn == 1) {
            displayMessage("\nUzavírám spojení se serverom.");

            // zrušení možnosti editovat vstupní pole
            setTextFieldEditable(false);

            try {
                this.conn = 0;
                output.close();
                input.close();
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            conn = 0;
        }
    }

    //6.
    /**
     * Odešle zprávu serveru.
     *
     * @param message zpráva, která se odešle na server.
     */
    private void sendData(String message) {
        try {
            output.writeUTF(this.meno_klienta + ">>> " + message);

            // vynucení poslání zprávy na server
            output.flush();
            displayMessage("\n" + this.meno_klienta + ">>> " + message);

        } catch (IOException e) {
            displayArea.append("\nChyba při odesílání: " + e.toString());
        }
    }

    //7.
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

    //8.
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

    //9.
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

    //10.
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

    public String getMeno() {
        return this.meno_klienta;
    }

    private class ServisC implements Runnable {

        @Override
        public void run() {
            try {
                processConnection();
                //displayMessage("\ndobehol som spojenieA");
            } catch (java.net.SocketException sex) {
                displayMessage("\n1. Dobehol som spojenie so serverom");
            } catch (IOException ex) {
                displayMessage("\n2. Dobehol som spojenie so serverom");
            }
            
        }
    }
}