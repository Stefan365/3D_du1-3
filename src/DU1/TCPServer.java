package DU1;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;

/**
 * Serverová část pro aplikaci typu klient-server.
 *
 * @author Stefan Veres
 */
public class TCPServer extends JPanel {

    /**
     * Textové pole pro zadání zprávy od uživatele.
     */
    private JTextField enterField;
    /**
     * Textová oblast zobrazující zprávy a další informace uživateli.
     */
    private JTextArea displayArea;
    /**
     * Serverový soket, na kterém server čeká na spojení od klienta.
     */
    private ServerSocket server;
    /**
     * STV kolekcia obsahujuca sokety prisluchajuce jednotlivym klientom.
     */
    private List<Servis> sokety_klientov = new ArrayList();
    /**
     * Počet spojení.
     */
    private int counter = 1;
    /**
     * Počet servis spojení.
     */
    private int id_counter = 0;
    /**
     * Spravca vlakien.
     */
    public static ExecutorService SPRAVCE = Executors.newCachedThreadPool();

    
    
    public static void main(String[] args) {
        // vytvoření serveru
        TCPServer server = new TCPServer();

        // vytvoření a zobrazení okna
        JFrame frame = new JFrame("TCP Server");
        frame.add(server);
        frame.setSize(300, 450);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // spuštění serveru
        server.runServer();
    }
    
    
    //0.Konstruktor
    /**
     * Vytvoří novou instanci třídy TCPServer a inicializuje prvky uživatelského
     * rozhraní.
     */
    public TCPServer() {
        setLayout(new BorderLayout());

        // vytvoření textového pole pro zadání zprávy
        enterField = new JTextField();
        enterField.setEditable(false);

        enterField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendDataA(e.getActionCommand(), true);
                enterField.setText("");
            }
        });

        add(enterField, BorderLayout.NORTH);

        // vytvoření textové oblasti pro zobrazování výstupu
        displayArea = new JTextArea();
        add(new JScrollPane(displayArea), BorderLayout.CENTER);
    }

    /**
     * Hlavní funkční metoda serveru, kde dochází k čekání a obsluze spojení s
     * klienty. Obsluha není prováděna vícevláknově, takže je obsluhován pouze
     * jeden klient najednou. Ostatní čekají ve frontě na obsloužení.
     */
    public void runServer() {

        try {
            // vytvoření serverového soketu na portu 12345
            // s frontou délky 100 požadavků
            server = new ServerSocket(8081, 100);

            while (true) {


                try {
                    // čekání na spojení
                    doTheConnection();

                } catch (EOFException e) {
                    displayMessage("\nKlient ukončil spojení.");
                } catch (java.net.SocketException e) {
                    displayMessage("\nHOHOHOHOKlient ukončil spojení.");
                } finally {
                    counter++;
                }

            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (SecurityException se) {
            se.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //1.
    /**
     * Vyčká na spojení od klienta o po přijetí spojení zobrazí zprávu.
     * Synchronized kvoli prevencii v pripade pokusu o pripojenie viacerych
     * klientov naraz.
     *
     * @throws IOException pokud dojde při čekání na spojení k chybě.
     */
    private void doTheConnection() throws IOException, java.net.SocketException {
        Servis serv = new Servis();

        //wait for connection:
        //displayMessage("Čekám na spojení.\n");
        // blokující volání, čeká na spojení. 
        // Synchronizace kvuli mozemu pripojeni vicero 
        // klientu naraz.
        synchronized (this.server) {              
            serv.connection = server.accept();
        }
        
        displayMessage("\nSpojení " + counter + " přijato od: "
            + serv.connection.getInetAddress().getHostName());

        //get Stream:
        serv.getStreams();

        //new Thread(new Service(connection));

        //ocislovanie Servisu:
        serv.id = this.id_counter;
        this.sokety_klientov.add(serv);
        this.id_counter++;

        //Spustenie v sprave SPRAVCE:
        TCPServer.SPRAVCE.execute((Servis) this.sokety_klientov.get(this.sokety_klientov.size() - 1));
    }

    //2.
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

    //3.
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
            }
        });
    }

    //4.A
    /**
     * Odešle zprávu vs. klientum.
     *
     * @param message zpráva, která se odešle klientum.
     */
    private synchronized void sendDataA(String message, boolean server_zobrazenie, int id) {
        Servis ser;
        String str;
        str = server_zobrazenie ? "SERVER>>> " : "";

        Iterator it = this.sokety_klientov.iterator();

        while (it.hasNext()) {
            ser = (Servis) it.next();
            if (ser.id != id) {
                ser.sendData(message, server_zobrazenie);
       
            
            }
            //System.out.println("meno:" + it.next().getClass().getName());
        }
        displayMessage("\n" + str + message);
    }

    //4.B
    /**
     * Odešle zprávu vs. klientum.
     *
     * @param message zpráva, která se odešle klientum.
     */
    private synchronized void sendDataA(String message, boolean server_zobrazenie) {
        String str;
        str = server_zobrazenie ? "SERVER>>> " : "";

        Iterator it = this.sokety_klientov.iterator();

        while (it.hasNext()) {
            ((Servis) it.next()).sendData(message, server_zobrazenie);
        }
        displayMessage("\n" + str + message);
    }

    private class Servis implements Runnable {

        /**
         * Id daneho servisu.
         */
        private int id;
        /**
         * Soket, pomocí kterého komunikuje server s konkrétním klientem.
         */
        private Socket connection;
        /**
         * Vstupní proud směrem od klienta.
         */
        private DataInputStream input;
        /**
         * Výstupní proud směrem ke klientovi.
         */
        private DataOutputStream output;

        //S.1.
        /**
         * Zpracování spojení s klientem.
         *
         * @throws IOException pokud při komunikaci s klientem dojde k chybě.
         */
        private void processConnection() throws IOException {
            String message = "Spojení proběhlo úspěšně.";

            // odeslání zprávy o úspěšném navázání spojení
            sendData(message, true);

            // nastavení vstupního pole tak, by šlo editovat
            setTextFieldEditable(true);

            // zpracování zpráv od klienta
            do {
                // načtení zprávy
                // blokující čekání
                message = input.readUTF();

                // zobrazení zprávy
                sendDataA(message, false, this.id);

            } while (true);//!message.equals("KLIENT>>> TERMINATE"));
            //this.closeConnection();
        }

        //S.2.
        /**
         * Získá ze soketu proudy pro odesílání a příjem dat.
         *
         * @throws IOException pokud dojde při získávání proudů k chybě.
         */
        private void getStreams() throws IOException {
            output = new DataOutputStream(connection.getOutputStream());
            input = new DataInputStream(connection.getInputStream());

            //displayMessage("\nZískány vstupní a výstupní proud.\n");
        }

        //S.3.
        /**
         * Odešle zprávu klientovi.
         *
         * @param message zpráva, která se odešle klientovi.
         */
        private void sendData(String message, boolean server_zobrazenie) {
            try {
                String str;
                str = server_zobrazenie ? "\nSERVER>>> " : "\n";
                //System.out.println(str);

                output.writeUTF(str + message);

                // vynucení zaslání zprávy klientovi
                output.flush();

            } catch (IOException e) {
                displayArea.append("\nChyba při odeslání: " + e.toString());
            }
        }

        //S.4.
        /**
         * Uzavře proudy a soket.
         */
        private void closeConnection() {
            //displayMessage("\nUzavírám spojení s Klientem.");

            // zrušení možnosti editovat vstupní pole
            //setTextFieldEditable(false);

            try {
                output.close();
                input.close();
                connection.close();
                sokety_klientov.remove(this);
                displayMessage("\nUkoncil som spojenie s Klientom!!!.");
                if (sokety_klientov.isEmpty() == true) {
                    setTextFieldEditable(false);
                }
            } catch (IOException e) {
                displayMessage("\nChyba v spojeni s klientom!!.");
                //e.printStackTrace();
            } finally {
            }
        }

        @Override
        public void run() {
            try {
                this.processConnection();

            } catch (EOFException e) {
                displayMessage("\n1.Zistujem, ze Klient ukončil spojení.");
            } catch (IOException ex) {
                displayMessage("\n2.Zistujem, ze Klient ukoncil spojeni!.");
            } finally {
                //displayMessage("\nKlient ukončil spojeníA.");
                this.closeConnection();
            }

        }
    }
}
