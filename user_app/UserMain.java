import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;
import java.security.*;
import java.security.spec.ECGenParameterSpec;

import sun.misc.BASE64Encoder;

public class UserMain {


    public static final int MINPUBKEY = 36;
    public static final int MAXPUBKEY = 51;

    private static final int PORT = 1234; //default port for nodes
    private static String IPNODE = "127.0.0.1";
    private static Socket connection;

    // input sources
    private static Scanner keySelectMenu = new Scanner(System.in);
    private static Scanner keySelectWallet = new Scanner(System.in);
    private static Scanner keySelectValue = new Scanner(System.in);
    private static Scanner specifyIP = new Scanner(System.in);

    public static boolean isConnect = false;
    public static boolean walletsArrive = false;
    public static ArrayList<PublicKey> wallets;
    public static boolean BalanceArrive = false;
    public static float myBalance;

    public static PrintWriter output;
    public static ObjectOutputStream outputStreamObj;
    public static ObjectInputStream inStream;

    public static PrivateKey privateKey;
    public static PublicKey publicKey;
    public static final String keysPathUser = ".//userappKeys.dat";

    public static void main(String[] argv) {

        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        if (!readKeysFromFile()) {                        //tento di leggere le chiai, se non esiste il file con la coppia di chiavi
            generateKeyPair();                                //le genero
            saveKeysToFile(publicKey, privateKey);            //e le salvo
        }

        try {

            if (argv.length != 0)
                IPNODE = argv[0];
            else {
                System.out.println("Insert IP of the node you want to connect to: ");
                IPNODE = specifyIP.nextLine();
            }

            System.out.println("This user's publicKey is " + StringUtil.getStringFromKey(publicKey).substring(MINPUBKEY, MAXPUBKEY));

            System.out.println("Trying to connect to: " + IPNODE + " in corso...");
            connection = new Socket(IPNODE, PORT);

            outputStreamObj = new ObjectOutputStream(connection.getOutputStream());
            inStream = new ObjectInputStream(connection.getInputStream());

            output = new PrintWriter(connection.getOutputStream(), true);
            Scanner input = new Scanner(connection.getInputStream());

            UserListener clientListen = new UserListener(input, inStream); //thread in ascolto
            clientListen.start();

            output.println("?-CONNECTION");

            while (true) {

                if (isConnect) {  // managed by the thread

                    // send my pub key
                    outputStreamObj.writeObject(publicKey);
                    outputStreamObj.flush();

                    while (isConnect) {

                        System.out.println("-----------------------------");
                        System.out.println("Press 1 to make a transaction");
                        System.out.println("Press 2 to see your balance");
                        System.out.println("Press 3 to see your transaction");
                        System.out.println("Press 0 to disconnect ");
                        System.out.println("-----------------------------");
                        int inp = keySelectMenu.nextInt();
                        if (inp == 0) {
                            output.println("?-DISCONNECTION");
                            connection.close();
                            System.exit(0);
                        }
                        if (inp == 1) {

                            getBalance();

                            output.println("?-WALLETS"); // ask wallets
                            Thread.sleep(350);
                            if (walletsArrive) {
                                walletsArrive = false;
                                System.out.println("Choose recipient");

                                for (int i = 0; i < wallets.size(); i++) {
                                    System.out.println(i + ") " + StringUtil.getStringFromKey(wallets.get(i)).substring(MINPUBKEY, MAXPUBKEY));
                                }
                                int inp2;
                                do {
                                    System.out.println("Insert a correct value:");
                                    inp2 = keySelectWallet.nextInt();
                                } while (inp2 < 0 || inp2 >= wallets.size());

                                System.out.println("Insert import:");
                                float valueTrans = keySelectValue.nextFloat();

                                System.out.println("Transaction info: ");
                                System.out.println("Send to " + StringUtil.getStringFromKey(wallets.get(inp2)).substring(MINPUBKEY, MAXPUBKEY) + " a value of: " + valueTrans);

                                Transaction transaction = generateTransaction(wallets.get(inp2), valueTrans);

                                if (transaction != null) {

                                    output.println("?-TRANSACTION");
                                    Thread.sleep(500);
                                    outputStreamObj.writeObject(transaction);
                                    outputStreamObj.flush();
                                }
                            } else {
                                System.out.println("Error... No Wallet arrived");
                            }
                        }//inp==1

                        if (inp == 2) {
                            System.out.println("Balance: " + getBalance());
                        }

                        if (inp == 3) {
                            output.println("?-INFOTRANS");
                            //intanto ha stampato a schermo le transazioni
                            Thread.sleep(800);
                        }
                    }//while (isConnect)
                }//if(isConnect)
            }//while(true)
        }//try
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Unexpected Error...");
        } finally {
            try {
                connection.close();
            } catch (Exception e) {
                System.out.println("Impossible to close the connection");
            }
        }//finally
    }//main


    public static void generateKeyPair() {
        try {
            //EDSCA Elliptic Curve Digital Signature Algorithm
            //BC Bouncy Castle apis for cryptography
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDH", "BC");
            //SHA-1 Pseudo-Random Number Generation
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");
            // Initializes the key pair generator for a certain keysize with the given source of randomness
            keyGen.initialize(ecSpec, random); //256 bit
            //genera coppia di chiavi
            KeyPair keyPair = keyGen.generateKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static float getBalance() {
        output.println("?-BALANCE");
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (BalanceArrive) {
            BalanceArrive = false;
            return myBalance;
        } else {
            System.out.println("No Balance arrive - Retry");
            return 0;
        }
    }

    public static Transaction generateTransaction(PublicKey _recipient, float value) {

        if (myBalance < value) {
            System.out.println("Not enough money for the transaction or something wrong");
            return null;
        }
        ArrayList<TransactionInput> voidVector = new ArrayList<TransactionInput>();
        Transaction newTransaction = new Transaction(publicKey, _recipient, value, voidVector);
        newTransaction.generateSignature(privateKey);
        return newTransaction;
    }

    public static void saveKeysToFile(PublicKey pub, PrivateKey priv) {
        try {
            FileOutputStream fileOut = new FileOutputStream(keysPathUser);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(pub);
            objectOut.writeObject(priv);
            objectOut.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static boolean readKeysFromFile() {
        File f = new File(keysPathUser);
        if (f.exists()) {
            FileInputStream fis;
            try {
                fis = new FileInputStream(keysPathUser);
                ObjectInputStream ois = new ObjectInputStream(fis);

                PublicKey pub = (PublicKey) ois.readObject();
                PrivateKey priv = (PrivateKey) ois.readObject();
                System.out.println(StringUtil.getStringFromKey(pub).substring(MINPUBKEY, MAXPUBKEY));

                publicKey = pub;
                privateKey = priv;

                ois.close();

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            return true;
        } else {
            return false;
        }
    }
}
