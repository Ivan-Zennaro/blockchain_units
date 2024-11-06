import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.io.*;
import java.net.*;
import java.security.*;

public class UserConnection extends Thread {


    public ObjectOutputStream outputStreamObj;
    public ObjectInputStream inStreamObj;
    public Scanner input;
    public PrintWriter output;

    public Socket connection;
    public PublicKey publicKey;
    public HashMap<String, TransactionOutput> UTXOs = new HashMap<String, TransactionOutput>();


    public UserConnection(Socket c) throws InterruptedException {
        this.connection = c;
    }

    public void run() {

        try {

            outputStreamObj = new ObjectOutputStream(connection.getOutputStream());
            inStreamObj = new ObjectInputStream(connection.getInputStream());

            output = new PrintWriter(this.connection.getOutputStream(), true);
            input = new Scanner(this.connection.getInputStream());

            while (true) {

                do {
                } while (!input.hasNextLine());

                String dataFromUser = input.nextLine();

                switch (dataFromUser) {
                    case "?-CONNECTION" -> {

                        output.println("OK-CONNECTION");    // accept user connection

                        // set user PK
                        this.publicKey = (PublicKey) inStreamObj.readObject();
                        if (!Main.isPresentPublicKey(this.publicKey)) {
                            Main.wallets.add(this.publicKey);
                        }
                        Main.sendToAllNode("!-NEWUSER", this.publicKey); // broadcast new user

                        if (Main.firstUser()) {  // genesis transaction

                            Transaction cbTrans = new Transaction(Main.coinbase.publicKey, publicKey, 100f, null);
                            cbTrans.generateSignature(Main.coinbase.privateKey);     //manually sign the genesis transaction

                            cbTrans.transactionId = "0";
                            cbTrans.outputs.add(new TransactionOutput(cbTrans.reciepient, cbTrans.value, cbTrans.transactionId));

                            Main.currentBlock.addTransaction(cbTrans);

                            Main.sendToAllNode("!-TRANSACTION", cbTrans);
                        }
                    }

                    case "?-DISCONNECTION" -> this.connection.close();
                    case "?-WALLETS" -> {
                        output.println("OK-WALLETS");
                        Thread.sleep(200);
                        ArrayList<PublicKey> wallets = new ArrayList<PublicKey>(Main.wallets);
                        outputStreamObj.writeObject(wallets);
                    }

                    case "?-TRANSACTION" -> {

                        // Input transaction with no inputs
                        Transaction transaction = (Transaction) inStreamObj.readObject();

                        // This is the one to be sent to other nodes
                        Transaction copy = new Transaction(transaction.sender, transaction.reciepient, transaction.value, null);
                        copy.signature = transaction.signature;
                        copy.timeStamp = transaction.timeStamp;

                        new Thread(() -> {
                            try {
                                // if input correctly added
                                if (addInputsToTransaction(transaction)) {
                                    copy.inputs = new ArrayList<TransactionInput>(transaction.inputs);

                                    Main.sendToAllNode("!-TRANSACTION", copy);

                                    if (Main.currentBlock.addTransaction(transaction)) {

                                        output.println("TRANSACTION-CONFIRMED");
                                        output.flush();

                                    } else {
                                        output.println("TRANSACTION-REFUSED");
                                    }
                                } else {
                                    System.out.println("Impossible to add Inputs");
                                }
                            } catch (Exception e) {
                                System.out.println("Error:" + e);
                            }
                        }).start();
                    }

                    case "?-BALANCE" -> {
                        output.println("OK-BALANCE");
                        Thread.sleep(200);
                        output.println(getBalance() + "");
                    }
                    case "?-INFOTRANS" -> {
                        output.println("OK-TRANSINFO");
                        Thread.sleep(200);
                        String s = "Confirmed Transactions:";
                        for (Block block : Main.blockchain) {
                            for (Transaction transaction : block.transactions) {
                                if (transaction.sender.equals(this.publicKey)) {
                                    s = s + '\n' + "Sended to " + StringUtil.getStringFromKey(transaction.reciepient).substring(Main.MINPUBKEY, Main.MAXPUBKEY) + " a value of " + transaction.value;
                                }
                                if (transaction.reciepient.equals(this.publicKey)) {
                                    s = s + '\n' + "Recieved from " + StringUtil.getStringFromKey(transaction.sender).substring(Main.MINPUBKEY, Main.MAXPUBKEY) + " a value of " + transaction.value;
                                }
                            }
                        }
                        s = s + '\n' + "-----------------------------";
                        s = s + '\n' + "Transactions not verify:";
                        for (Transaction transaction : Main.currentBlock.transactions) {
                            if (transaction.sender.equals(this.publicKey)) {
                                s = s + '\n' + "Sended to " + StringUtil.getStringFromKey(transaction.reciepient).substring(Main.MINPUBKEY, Main.MAXPUBKEY) + " a value of " + transaction.value;
                            }
                            if (transaction.reciepient.equals(this.publicKey)) {
                                s = s + '\n' + "Recieved from " + StringUtil.getStringFromKey(transaction.sender).substring(Main.MINPUBKEY, Main.MAXPUBKEY) + " a value of " + transaction.value;
                            }
                        }
                        outputStreamObj.writeObject(s);
                        outputStreamObj.flush();
                    }
                }

            }//while
        }//try
        catch (Exception e) {
            System.out.println("UserConnection: " + e.toString());
        }
    }//run


    public float getBalance() {
        float total = 0;
        for (Map.Entry<String, TransactionOutput> item : Main.UTXOs.entrySet()) {
            TransactionOutput UTXO = item.getValue();
            if (UTXO.isMine(publicKey) && UTXO.isTransactionConfirmed()) {
                UTXOs.put(UTXO.id, UTXO);
                total = total + UTXO.value;
            }
        }
        return total;
    }

    public boolean addInputsToTransaction(Transaction transaction) {

        // getBalance update local UTXO, then I can use  the local one
        if (getBalance() < transaction.value) {
            System.out.println("Impossible no value left");
            return false;
        }

        float total = 0;
        for (Map.Entry<String, TransactionOutput> item : UTXOs.entrySet()) { // local UTXO

            if (item.getValue() == null) {
                System.out.println("No expendable transaction avaiable");
            }
            TransactionOutput UTXO = item.getValue();
            total = total + UTXO.value;
            transaction.inputs.add(new TransactionInput(UTXO.id));
            if (total > transaction.value) break;
        }

        // remove used outputs
        for (TransactionInput input : transaction.inputs) {
            UTXOs.remove(input.transactionOutputId);
        }
        return true;
    }
}
