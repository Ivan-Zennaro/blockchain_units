import java.util.ArrayList;
import java.util.Scanner;
import java.io.*;
import java.net.*;
import java.security.PublicKey;


public class NodeConnection extends Thread {


    public ObjectOutputStream outputStreamObj;
    public ObjectInputStream inStreamObj;
    public Scanner input;
    public PrintWriter output;

    public Socket connection;

    public NodeConnection(Socket c) {
        connection = c;
    }

    public void run() {

        try {

            outputStreamObj = new ObjectOutputStream(connection.getOutputStream());
            inStreamObj = new ObjectInputStream(connection.getInputStream());

            output = new PrintWriter(this.connection.getOutputStream(), true);
            input = new Scanner(this.connection.getInputStream());

            do {
            } while (!input.hasNextLine());

            String dataFromOtherNode = input.nextLine();

            // new node connected and send public key
            switch (dataFromOtherNode) {

                case "!-DISCOVERKEY" -> {
                    PublicKey pc = (PublicKey) inStreamObj.readObject();
                    if (!Main.isPresentPublicKey(pc)) {
                        Main.wallets.add(pc);
                    }
                    outputStreamObj.writeObject(Main.publicKey);
                    outputStreamObj.flush();
                }
                case "!-TRANSACTION" -> {
                    Transaction transaction = (Transaction) inStreamObj.readObject();
                    Main.currentBlock.addTransaction(transaction);
                }

                // Suppose nodes are always online
                case "!-NEWUSER" -> {
                    Main.firstUser(); // to avoid two genesis transactions

                    PublicKey pc = (PublicKey) inStreamObj.readObject();
                    if (!Main.isPresentPublicKey(pc)) {
                        Main.wallets.add(pc);
                    }
                }
                case "!-MINED" -> {
                    MinedPack minedpack = (MinedPack) inStreamObj.readObject();
                    Main.addMinedPack(minedpack);
                }
                case "!-UPDATEBLOCKCHAIN" -> {
                    MinedPack minedpack = new MinedPack(Main.blockchain, Main.UTXOs, 0, null);

                    outputStreamObj.writeObject(minedpack);
                    outputStreamObj.flush();

                    outputStreamObj.writeObject(Main.currentBlock);
                    outputStreamObj.flush();

                    outputStreamObj.writeObject(Main.wallets);
                    outputStreamObj.flush();
                }
            }

        }//try
        catch (Exception e) {
            System.out.println("NodeConnection: " + e);
        }
    }//run

}
