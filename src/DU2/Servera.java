package DU2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Servera {//implements Runnable {

    /**
     * zoznam vnutornych tied pre multikastove vysielanie. tieto Predstavuje
     * jednotlive roomy
     */
    public List<Servera.ServisS_send> zoz_SSS = new ArrayList();

    /**
     * univerzalny pool pre vsetky vlakna.
     */
    public ExecutorService SPRAVCE_S = Executors.newCachedThreadPool();

    //TCP:
    private int id_counter = 0; //pocitac pripojenych klientov na TCP
    private ServerSocket socketSER;//Serverový soket, na kterém server čeká na spojení od klienta.
    private final List<Servera.ServisS_receive> sokety_klientov = new ArrayList();// kolekcia obsahujuca sokety p                                                            //risluchajuce jednotlivym klientom TCP komunikacie.
    //Multicast:
    //private final int id_counter_M = 0; //pocitac multicastovych miestnosti   
    // STV kolekcia obsahujuca sokety prisluchajuce jednotlivym miestnostiam Multicast komunikacie.
    //private final List<Multicat_Servera.ServisS_send> sokety_multicast = new ArrayList();

    public static void main(String[] args) {
        Servera karol = new Servera();
        karol.runServer();
    }

    public Servera() {
        super();
    }

    //S.A.
    /**
     * Prva vnutorna trieda. Predstavuje chatovacie miestnosti, rooms. je
     * tvorena srz multicasty:
     */
    public class ServisS_send {//implements Runnable { nieje potrebne, 
        //aby to bolo Runnable. Server bude len rozposielat - v zavislosti od toho, co pride, 
        //nebude produkovat spravy.

        private int room_id; //id roomu
        private DatagramSocket socketDAT;     //MulticastSocket socket na odosielanie sprav;
        private InetAddress mGroup;// příprava adresy multicastové skupiny
        private DatagramPacket packetDAT;// vytvoření UDP paketu k odeslání
        private int cisloClPortu; //cislo klientskeho portu
        private String multicastAddress; //multicastova adresa

        //S.A.1.
        /**
         * Odosiela spravy z konzoly
         *
         */
        public void sendS(String sa) {
            try {
                byte[] buf = sa.getBytes();
                packetDAT = new DatagramPacket(buf, buf.length, mGroup, cisloClPortu);
                socketDAT.send(packetDAT);
            } catch (IOException ex) {
                System.out.println("chyba sendS, 1.");
            }
        }

        //S.A.2.
        /**
         * Vytvori multicastovy soket na nastavi ho na skupinu.
         *
         */
        private void doTheConnectionA(int port, String castAddr, int clport)
            throws IOException, java.net.SocketException {
            try {
                //zapametavanie si, co prijal:
                this.multicastAddress = castAddr;
                this.cisloClPortu = clport;
                //vytvorenie soketu:
                this.socketDAT = new DatagramSocket(port);
                this.mGroup = InetAddress.getByName(castAddr);

            } catch (SocketException ex) {
                System.out.println("chyba, doTheConnectionA S,1.");
            } catch (UnknownHostException uex) {
                System.out.println("chyba, doTheConnectionA S,2.");
            }
        }

        //S.A.4.
        /**
         * Uzavře proudy a soket UDP spojeni.
         */
        private void closeConnectionUDP() {
            System.out.println("Uzavírám UDP spojení s Klientem.");
            socketDAT.close();
            System.out.println("Ukoncil som UDP spojenie s Klientom!!!.");
        }
    }

    //S.B.
    /**
     * 2.vnutorna trieda. TCP spojenia s klientom. Na prijimanie sprav od
     * klientov.
     */
    private class ServisS_receive implements Runnable {

        private Socket connection;// Soket, pomocí kterého komunikuje server s konkrétním klientem.
        private DataInputStream input;// Vstupní proud směrem od klienta.
        private DataOutputStream output;// Vstupní proud směrem od klienta.
        private String message_rec; //received message

        //S.B.1.
        /**
         * Zpracování spojení s klientem. Prijme spravu od klienta a
         * multicastovo ju odvysiela vsetkym v danom roome.
         *
         */
        public void receiveS() throws IOException {
            message_rec = input.readUTF();
            //System.out.println(" 2. message:" + message_rec);
            
            int room_i_first, room_i_last, room_i;
            try {
                room_i_first = Integer.parseInt(message_rec);//firstChar);
                room_i = upravInt(room_i_first);
                sendOnce(room_i);
            } catch (java.lang.NumberFormatException ex) {
                //pokial to cislo nieje, moze program pokracovat normalnou cestou.
                room_i_last = this.getLastChar();
                zoz_SSS.get(room_i_last).sendS(this.message_rec);
            }
        }

        public int upravInt(int room_i) {
            int r_i = room_i;
            if (room_i < 0) {
                r_i = 0;
            } else if (room_i > zoz_SSS.size() - 1) {
                r_i = zoz_SSS.size() - 1;
            }
            return r_i;
        }

        //S.3.
        /**
         * Odešle TCP zprávu klientovi s informaciami o miestnosti.
         *
         * @param room_i cislo miestnosti.
         */
        private void sendOnce(int room_i) {
            try {
                //private String multicastAddress; //multicastova adresa
                System.out.println(" 5. message:" + room_i);
                String  str = zoz_SSS.get(room_i).multicastAddress + "x" + zoz_SSS.get(room_i).cisloClPortu
                    + "x" + room_i;
                output.writeUTF(str);
                System.out.println(" 6. message:" + str);
                
                // vynucení zaslání zprávy klientovi
                output.flush();

            } catch (IOException e) {
                System.out.println("\nChyba při odeslání: " + e.toString());
            }
        }

        //S.B.2.
        /**
         * Získá ze soketu proudy pro odesílání a příjem dat.
         *
         * @throws IOException pokud dojde při získávání proudů k chybě.
         */
        private void getStreams() throws IOException {
            input = new DataInputStream(connection.getInputStream());
            output = new DataOutputStream(connection.getOutputStream());

            //System.out.println("Získány vstupní a výstupní proud., getStreams");
        }

        //S.B.3.
        /**
         * Vyčká na spojení od klienta o po přijetí spojení zobrazí zprávu.
         *
         * @throws java.net.SocketException,IOException pokud dojde při čekání
         * na spojení k chybě.
         */
        private void doTheConnectionAndRunB() throws IOException, java.net.SocketException {

            this.connection = socketSER.accept();
            System.out.println("Spojení " + id_counter + " přijato od: "
                + this.connection.getInetAddress().getCanonicalHostName());

            this.getStreams();

            //ocislovanie Servisu:
            sokety_klientov.add(this);
                //id_counter++;

            //Spustenie v sprave SPRAVCE:
            SPRAVCE_S.execute((ServisS_receive) sokety_klientov.get(sokety_klientov.size() - 1));
        }

        //S.B.4.
        /**
         * Uzavře proudy a soket.
         */
        private void closeConnectionTCP() {
            System.out.println("Uzavírám spojení s Klientem.");
            try {
                input.close();
                connection.close();
                System.out.println("Ukoncil som spojenie s Klientom!!!.");
            } catch (IOException e) {
                System.out.println("Chyba v ukoncovani spojenia s klientom!!, closeConnection, 1.");
            } finally {
            }
        }

        //S.B.5.
        /**
         * Ziska posledni znak zo Stringu;
         *
         * @return posledny znak zo stringu
         */
        public int getLastChar() {

            char room_i = this.message_rec.charAt(this.message_rec.length() - 1);
            this.message_rec = this.message_rec.replace(this.message_rec.substring(this.message_rec.length() - 1), "");
            return (int) (Character.getNumericValue(room_i));
        }

        //S.B.6.
        @Override
        public void run() {
            try {
                while (true) {
                    this.receiveS();
                }
            } catch (IOException e) {
                System.out.println("Chyba v spojeni s klientom!!, B.run, 1.");
            }
        }

     /*   private String getFirstChar(String message_reca) {
            char room_i = message_reca.charAt(0);
            return room_i + "";
        }
        */
    }

    //S.1.
    /**
     * Predstavuje beh serveru
     *
     */
    public void runServer() {
        try {
            // vytvoření serverového soketu na portu 8081
            // s frontou délky 100 požadavků
            socketSER = new ServerSocket(8081, 100);

            //UDP komunikacia:
            //1. chatovacia miestnost:            
            Servera.ServisS_send vl2 = new Servera.ServisS_send();//send
            vl2.doTheConnectionA(5060, "239.0.0.17", 2060);
            vl2.room_id = 0;
            zoz_SSS.add(vl2);

            //2. chatovacia miestnost:            
            vl2 = new Servera.ServisS_send();
            vl2.doTheConnectionA(5061, "239.0.0.18", 2061);
            vl2.room_id = 1;
            zoz_SSS.add(vl2);

            //3. chatovacia miestnost:            
            vl2 = new Servera.ServisS_send();
            vl2.doTheConnectionA(5062, "239.0.0.19", 2062);
            vl2.room_id = 2;
            zoz_SSS.add(vl2);

        } catch (java.net.SocketException e) {
            System.out.println("Klient ukončil spojení.  runServer, 3.");
        } catch (IOException ex) {
            System.out.println("Nepodarilo sa vytvorit hl. servrovy TCP soket. runServer, 1.");
        }

        //TCP komunikacia:
        while (true) {
            try {
                Servera.ServisS_receive vl1 = new Servera.ServisS_receive();//receive
                vl1.doTheConnectionAndRunB();

            } catch (EOFException e) {
                System.out.println("EOFEx. Klient ukončil spojení. runServer, 2.");
            } catch (java.net.SocketException e) {
                System.out.println("SocketEx. Klient ukončil spojení.  runServer, 3.");
            } catch (IOException ex) {
                System.out.println("IOEx. Kient ukončil spojení.  runServer, 4.");
            } finally {
                id_counter++;
            }
        }
    }
}
