
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Scanner;

import sun.misc.BASE64Encoder;

public class Main {

    public static final int MINPUBKEY = 36;
    public static final int MAXPUBKEY = 51;
    public static final int PORTNODE = 1235;

    public static String myip = "";

    public static ArrayList<Block> blockchain = new ArrayList<Block>();
    public static HashMap<String, TransactionOutput> UTXOs = new HashMap<String, TransactionOutput>();
    public static HashMap<String, TransactionOutput> UTXOlocal = new HashMap<String, TransactionOutput>();
    public static ArrayList<MinedPack> minedPacks = new ArrayList<MinedPack>();

    public static int difficulty = 5;
    public static double partialMiningTime = 0; //average time to mine N blocks
    public static float minimumTransaction = 0.1f;
    private static boolean isThisTheFirstUser = true; // usefull for genesys
    public static int numberOfBLocksBetweenRewardAdjustement = 10000;

    public static float timeStampFirstPack = 0;
    public static int counterCoinbaseTrans = 0;

    public static Block currentBlock;
    public static int safeBlock = -1;
    public static Wallet coinbase;

    private static final Scanner keySelectMenu = new Scanner(System.in);
    private static final Scanner keySelectWallet = new Scanner(System.in);
    private static final Scanner keySelectValue = new Scanner(System.in);

    private static final String keysPath = ".//keyPair.dat";
    private static final String blockchainPath = ".//blockchain.dat";

    public static PrivateKey privateKey;
    public static PublicKey publicKey;
    public static byte[] publicKeyBytes;

    public static ArrayList<PublicKey> wallets = new ArrayList<PublicKey>();


    // Lab: public static String nodes [] = {"172.30.15.154", "172.30.15.153" , "172.30.15.155" , "172.30.15.152" , "172.30.15.151"}; //DA USARE IN LAB
    // Local
    public static String nodes[] = {"192.168.1.1", "192.168.1.2", "192.168.1.3", "192.168.1.4", "192.168.1.5"};

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 0)
            myip = args[0];
        else {
            try {
                myip = InetAddress.getLocalHost().getHostAddress();
                System.out.println("Running on local host: " + myip);
            } catch (UnknownHostException e) {
                System.err.println("UnknownHostException");
            }
        }
        removeMyIP();
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        if (!readKeyPair()) {
            generateKeyPair();
            writeKeysToFile(publicKey, privateKey);
        } else { // get blockchain
            try {
                ObjectOutputStream outputStreamObj;
                ObjectInputStream inStreamObj;
                PrintWriter output;
                Scanner input;

                for (String node : nodes) {

                    Socket connection = new Socket();
                    connection.connect(new InetSocketAddress(node, PORTNODE), 2000); //aspetta max 2s

                    outputStreamObj = new ObjectOutputStream(connection.getOutputStream());
                    inStreamObj = new ObjectInputStream(connection.getInputStream());

                    output = new PrintWriter(connection.getOutputStream(), true);
                    input = new Scanner(connection.getInputStream());

                    output.println("!-UPDATEBLOCKCHAIN");
                    Thread.sleep(200);

                    MinedPack minedpack = (MinedPack) inStreamObj.readObject();
                    Block b = (Block) inStreamObj.readObject();
                    ArrayList<PublicKey> w = (ArrayList<PublicKey>) inStreamObj.readObject();

                    if (isChainValid(minedpack.blockchain)) {
                        Main.blockchain = minedpack.blockchain;
                        Main.UTXOs = minedpack.UTXOs;
                        currentBlock = b;
                        wallets = w;

                        saveBlockchain();

                        input.close();
                        output.close();
                        outputStreamObj.close();
                        inStreamObj.close();

                        connection.close();

                        break;
                    }

                    input.close();
                    output.close();
                    outputStreamObj.close();
                    inStreamObj.close();

                    connection.close();
                } //end for

            } catch (Exception ignored) {
                System.err.println("Connection issue");
            }

            // alternatively, if no other node responds with a (valid) chain, restore the one it had saved locally
            readBlockchain();
        }

        coinbase = new Wallet();

        // add himself in first position
        if (wallets.isEmpty()) {
            wallets.add(publicKey);
        }

        if (currentBlock == null) { // It might not be null if I got it from someone else on reboot
            currentBlock = new Block("0", 0, difficulty);
        }

        // server process listening to other users
        NodeListenerUser nodeListenerUser = new NodeListenerUser();
        nodeListenerUser.start();

        // server process listening to other nodes
        NodeListenerNode nodeListenerNode = new NodeListenerNode();
        nodeListenerNode.start();

        // Send public key
        sendToAllNode("!-DISCOVERKEY", null);

        System.out.println("This node's publicKey is " + StringUtil.getStringFromKey(publicKey).substring(MINPUBKEY, MAXPUBKEY));
        //Debug: System.out.println("This node's privateKey is " + StringUtil.getStringFromKey(privateKey).substring(MINPRIVKEY, MAXPRIVKEY));

        // menu
        while (true) {

            System.out.println("-----------------------------");
            System.out.println("Press 1 to make a transaction");
            System.out.println("Press 2 to see your balance");
            System.out.println("Press 3 to see the chain");
            System.out.println("Press 4 to see UTXO");
            System.out.println("Press 5 to quit");
            System.out.println("-----------------------------");

            try {
                int inp = -1;
                try {
                    inp = keySelectMenu.nextInt();
                } catch (InputMismatchException e) {
                    System.out.println("Input error, repeat the operation from beginning");
                    keySelectMenu.next();
                }

                if (inp == 1) {
                    System.out.println("Choose recipient:");
                    for (int i = 0; i < wallets.size(); i++) {
                        System.out.println(i + ") " + StringUtil.getStringFromKey(wallets.get(i)).substring(MINPUBKEY, MAXPUBKEY));
                    }

                    int inp2;    //select wallet
                    do {
                        System.out.println("Insert a correct value:");
                        inp2 = keySelectWallet.nextInt();
                    } while (inp2 < 0 || inp2 >= wallets.size());  // condition for which it continues to cycle

                    System.out.println("Insert import:");
                    float valueTrans = keySelectValue.nextFloat();

                    System.out.println("Transaction info: ");
                    System.out.println("Send to " + StringUtil.getStringFromKey(wallets.get(inp2)).substring(MINPUBKEY, MAXPUBKEY) + " a value of: " + valueTrans);


                    Transaction transaction = generateTransaction(wallets.get(inp2), valueTrans);

                    if (transaction != null) {
                        Transaction copy = new Transaction(transaction.sender, transaction.reciepient, transaction.value, transaction.inputs);
                        copy.signature = transaction.signature;
                        copy.timeStamp = transaction.timeStamp;

                        sendToAllNode("!-TRANSACTION", copy);
                        currentBlock.addTransaction(transaction);
                    }
                } //inp==1
                if (inp == 2) {
                    System.out.println("Balance: " + getBalance());
                }
                if (inp == 3) {
                    showChain(currentBlock);
                }
                if (inp == 4) {
                    System.out.println(printUtxos());
                }
                if (inp == 5) {
                    System.out.println("Closing...");
                    saveBlockchain();
                    try {
                        nodeListenerUser.server.close();
                        nodeListenerNode.server.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.exit(0);
                }

            } catch (InputMismatchException e) {

                System.out.println("Input error, repeat the operation from beginning");
                keySelectMenu.reset();
            }
        }//end while
    }//end main


    public static void generateKeyPair() {
        try {
            //EDSCA Elliptic Curve Digital Signature Algorithm
            //BC Bouncy Castle apis for cryptography
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
            //SHA-1 Pseudo-Random Number Generation
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");
            // Initializes the key pair generator for a certain keysize with the given source of randomness
            keyGen.initialize(ecSpec, random); //256 bit
            KeyPair keyPair = keyGen.generateKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static float getBalance() {
        float total = 0;
        for (Map.Entry<String, TransactionOutput> item : UTXOs.entrySet()) {
            TransactionOutput UTXO = item.getValue();
            if (UTXO.isMine(publicKey) && UTXO.isTransactionConfirmed()) {
                UTXOlocal.put(UTXO.id, UTXO);
                total = total + UTXO.value;
            }
        }
        return total;
    }

    public static Transaction generateTransaction(PublicKey _recipient, float value) {
        // work on local utxos created by get balance
        if (getBalance() < value) {
            System.out.println("Not enought value");
            return null;
        }
        ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();

        float total = 0;
        for (Map.Entry<String, TransactionOutput> item : UTXOlocal.entrySet()) {
            TransactionOutput UTXO = item.getValue();
            total = total + UTXO.value;
            inputs.add(new TransactionInput(UTXO.id));
            if (total > value) break;
        }

        Transaction newTransaction = new Transaction(publicKey, _recipient, value, inputs);
        newTransaction.generateSignature(privateKey);

        for (TransactionInput input : inputs) {
            UTXOlocal.remove(input.transactionOutputId);
        }

        return newTransaction;
    }


    public static void sendToAllNode(String command, Object o) {
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] != null) {
                NodeConnectionSender ncs = new NodeConnectionSender(command, i, o);
                ncs.start();
            }
        }
    }

    public static boolean isPresentPublicKey(PublicKey pc) {
        for (PublicKey k : wallets) {
            if (k.equals(pc)) return true;
        }
        return false;
    }

    public static void showChain(Block currentBlock) {

        ArrayList<Block> blockchainCopy = new ArrayList<Block>(blockchain);
        blockchainCopy.add(currentBlock);

        // show mined blocks
        int i = 0;
        for (; i < blockchainCopy.size(); i++) {
            System.out.println("Blocco numero:" + i + "{");
            Block block = blockchainCopy.get(i);
            System.out.println("\t hash: " + block.hash);
            System.out.println("\t hash precedente: " + block.previousHash);
            System.out.println("\t merkle root: " + block.merkleRoot);
            System.out.println("\t difficulty: " + block.difficulty);
            System.out.println("\t time-stamp: " + block.timeStamp);
            System.out.println("\t nonce: " + block.nonce);
            ArrayList<Transaction> transactions = blockchainCopy.get(i).transactions;
            System.out.println("\t transazioni{");
            for (Transaction transaction : transactions) {

                System.out.println("\t \t id:" + transaction.transactionId);
                System.out.print("\t \t mittente:");

                System.out.println(StringUtil.getStringFromKey(transaction.sender).substring(MINPUBKEY, MAXPUBKEY));

                System.out.print("\t \t ricevente:");

                publicKeyBytes = transaction.reciepient.getEncoded();
                System.out.println(StringUtil.getStringFromKey(transaction.reciepient).substring(MINPUBKEY, MAXPUBKEY));


                System.out.println("\t \t valore:" + transaction.value);
                System.out.println("\t \t firma:" + transaction.signature.toString());
                System.out.println("\t \t transazioni Input {");
                try {
                    for (int inputIndex = 0; inputIndex < transaction.inputs.size(); inputIndex++) {
                        System.out.println("\t \t \t idOutput: ");
                        System.out.println("\t \t \t " + transaction.inputs.get(inputIndex).transactionOutputId);
                        System.out.print('\n');
                    }
                } catch (Exception e) {
                    System.out.println("\t \t \t inputs=null");
                }
                System.out.println("\t \t  }");

                System.out.println("\t \t transazioni Output {");

                try {
                    for (int outputIndex = 0; outputIndex < transaction.outputs.size(); outputIndex++) {
                        System.out.println("\t \t \t id: " + transaction.outputs.get(outputIndex).id);
                        System.out.print("\t \t \t ricevente: ");


                        System.out.println(StringUtil.getStringFromKey(transaction.outputs.get(outputIndex).reciepient).substring(MINPUBKEY, MAXPUBKEY));

                        System.out.println("\t \t \t valore: " + transaction.outputs.get(outputIndex).value);
                        System.out.println("\t \t \t id genitore: " + transaction.outputs.get(outputIndex).parentTransactionId);
                        System.out.print('\n');
                    }
                } catch (Exception e) {
                    System.out.println("\t \t \t outputs=null");
                }


                System.out.println("\t \t  }");
                System.out.print('\n');
            } // end transactions

            System.out.println("\t }");
            System.out.println("}");

        }// end blocks
    }//end chains


    public static Boolean isChainValid(ArrayList<Block> blockchain) {

        Block currentBlock;
        Block previousBlock;
        String hashTarget;
        HashMap<String, TransactionOutput> tempUTXOs = new HashMap<String, TransactionOutput>();

        for (int i = 0; i < blockchain.size(); i++) {

            currentBlock = blockchain.get(i);
            hashTarget = new String(new char[currentBlock.difficulty]).replace('\0', '0');

            if (i == 0) {
                if (!currentBlock.previousHash.equals("0")) {
                    System.out.println("#Error# First block must have 0 as previous block");
                    return false;
                }
            } else {
                previousBlock = blockchain.get(i - 1);
                // chack prev hash
                if (!previousBlock.hash.equals(currentBlock.previousHash)) {
                    System.out.println("#Error# Previous hash:" + currentBlock.hash + "not corresponding");
                    return false;
                }
            }

            //check sigle hash block
            if (!currentBlock.hash.equals(currentBlock.calculateHash())) {
                System.out.println("#Error# Current hash block: " + currentBlock.hash + " not correct");
                return false;
            }


            // check hash start with 0
            if (!currentBlock.hash.substring(0, currentBlock.difficulty).equals(hashTarget)) {
                System.out.println("#Error# Block " + currentBlock.hash + " is not mined");
                return false;
            }

            // check inner transactions
            TransactionOutput tempOutput;
            for (int t = 0; t < currentBlock.transactions.size(); t++) {
                Transaction currentTransaction = currentBlock.transactions.get(t);  //transazione di appoggio

                // signature
                if (!currentTransaction.verifySignature()) {
                    System.out.println("#Error# Signature of transaction(" + t + ") not valid");
                    return false;
                }

                // if no coinbase transaction check inputs
                if (!currentTransaction.transactionId.equals("0")) {

                    // check inputs match outputs
                    if (currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
                        System.out.println("#Error# Inputs not equals to outputs (" + t + ")");
                        return false;
                    }

                    // input transactions
                    for (TransactionInput input : currentTransaction.inputs) {

                        // assign the output transaction that generated that input
                        tempOutput = tempUTXOs.get(input.transactionOutputId);

                        // check whether the current input actually comes from a previous output
                        if (tempOutput == null) {
                            System.out.println("#Error# Missing the reference to the input transaction (" + t + ")");
                            return false;
                        }

                        // check whether the current input actually comes from a previous output
                        if (input.UTXO.value != tempOutput.value) {
                            System.out.println("#Error# Transaction(" + t + ") has value not correct ");
                            return false;
                        }

                        // end of the check, removed
                        tempUTXOs.remove(input.transactionOutputId);
                    }

                    // coinbase transaction has only one output
                    if (currentTransaction.outputs.get(1).reciepient != currentTransaction.sender) {
                        System.out.println("#Error# Transaction(" + t + ") has output that doesn't come back to the sender");
                        return false;
                    }
                }

                // do this for all transactions
                // load output transactions to temporary hashmap
                // loop transactions to continue chain verification
                for (TransactionOutput output : currentTransaction.outputs) {
                    tempUTXOs.put(output.id, output);
                }

                if (currentTransaction.outputs.get(0).reciepient != currentTransaction.reciepient) {
                    System.out.println("#Error# Transaction(" + t + ") output of recipient not correct");
                    return false;
                }
            }
        }
        System.out.println("Chain Verification Successful");
        return true;
    }

    public static String printUtxos() {
        String s = "\n \t UTXOs:  ";
        for (Map.Entry<String, TransactionOutput> item : UTXOs.entrySet()) {
            TransactionOutput UTXO = item.getValue();
            s += "\n \t \t id: " + UTXO.id;
            s += "\n \t \t ricevente: " + StringUtil.getStringFromKey(UTXO.reciepient).substring(Main.MINPUBKEY, Main.MAXPUBKEY);

            s += "\n \t \t valore: " + UTXO.value;
            s += "\n \t \t id genitore: " + UTXO.parentTransactionId;
            s += "\n \t \t -------------------------------------------------";
        }
        return s;
    }


    public static synchronized void addMinedPack(MinedPack mp) {

        if ((mp.blockchain.size() - 1) <= safeBlock) {
            System.out.println("It's just arrived a chain too late");
            return;
        }

        if (minedPacks.isEmpty()) {
            timeStampFirstPack = new Date().getTime();
            minedPacks.add(mp);

            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    boolean findCorrectChain = false;
                    while (!findCorrectChain) {
                        if (minedPacks.isEmpty()) {
                            System.out.println("#No chain is valid");
                            return;
                        }
                        MinedPack fasterMP = minedPacks.getFirst();

                        for (MinedPack mp1 : minedPacks) {
                            if (mp1.timestamp < fasterMP.timestamp) {
                                fasterMP = mp1;
                            }
                        }
                        if (isChainValid(fasterMP.blockchain)) {

                            findCorrectChain = true;
                            blockchain = new ArrayList<>(fasterMP.blockchain);
                            UTXOs = new HashMap<>(fasterMP.UTXOs);

                            // save current block
                            currentBlock = blockchain.getLast();
                            safeBlock = blockchain.size() - 1;

                            // transaction reward
                            Wallet coinbase = new Wallet();
                            Transaction cbTrans = new Transaction(coinbase.publicKey, fasterMP.publicKey, rewardGenerator(safeBlock), null);
                            cbTrans.generateSignature(coinbase.privateKey);

                            cbTrans.transactionId = "0";
                            cbTrans.outputs.add(new TransactionOutput(cbTrans.reciepient, cbTrans.value, cbTrans.transactionId, counterCoinbaseTrans++));

                            // creation next block using previous block hash
                            currentBlock = new Block(currentBlock.hash, currentBlock.number + 1, difficulty);

                            Main.currentBlock.addTransaction(cbTrans);
                            minedPacks.clear();
                            saveBlockchain();

                        } else {
                            minedPacks.remove(fasterMP);
                        }
                    }
                } catch (InterruptedException e) {
                    System.out.println("Error:" + e);
                }
            }).start();
        } else {
            if (mp.timestamp < timeStampFirstPack + 2000) {
                minedPacks.add(mp);
                System.out.println("Another chain arrived from " + StringUtil.getStringFromKey(mp.publicKey).substring(MINPUBKEY, MAXPUBKEY));
            }
        }
    }

    // check if it's the first user
    public static synchronized boolean firstUser() {
        boolean tem = isThisTheFirstUser;
        isThisTheFirstUser = false;
        return tem;
    }

    public static void removeMyIP() {

        String myIP = myip;
		
		/* Local
		  String myIP="";
		  try {
			myIP= InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}*/

        boolean found = false;

        int i;
        for (i = 0; i < nodes.length && !found; ++i) {
            if (nodes[i].equals(myIP)) {
                found = true;
            }
        }

        i--;   // poiche prima di uscire dal for incrementa di 1 i

        if (found) {
            String[] nodesTemp = new String[nodes.length - 1];

            for (int j = 0; j < i; j++) {
                nodesTemp[j] = nodes[j];
            }
            for (int j = i; j < nodesTemp.length; j++) {
                nodesTemp[j] = nodes[j + 1];
            }

            nodes = nodesTemp;
        }


    }

    public static void writeKeysToFile(Object pub, Object priv) {
        try {
            FileOutputStream fileOut = new FileOutputStream(keysPath);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(pub);
            objectOut.writeObject(priv);
            objectOut.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static boolean readKeyPair() {
        File f = new File(keysPath);
        if (f.exists()) {
            FileInputStream fis;
            try {
                fis = new FileInputStream(keysPath);
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
			/*
			I call it to set the first user variable to false. If I find that this node was already started,
			it means that the one connecting to me is not the first user ever and I do not have to do
			the genesis transaction for him.
			*/
            firstUser();
            return true;
        } else {
            return false;
        }
    }

    public static void saveBlockchain() { //writes the blockchain to a file
        try {
            FileOutputStream fileOut = new FileOutputStream(blockchainPath);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);

            objectOut.writeObject(blockchain);
            objectOut.writeObject(UTXOs);
            objectOut.writeObject(currentBlock);

            objectOut.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static boolean readBlockchain() {
        File f = new File(blockchainPath);
        if (f.exists()) {
            FileInputStream fis;
            try {
                fis = new FileInputStream(blockchainPath);
                ObjectInputStream ois = new ObjectInputStream(fis);

                ArrayList<Block> b = (ArrayList<Block>) ois.readObject();
                HashMap<String, TransactionOutput> u = (HashMap<String, TransactionOutput>) ois.readObject();
                Block cb = (Block) ois.readObject();

                blockchain = b;
                UTXOs = u;
                currentBlock = cb;

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

    public static float rewardGenerator(int numOfBlocks) {
        if (numOfBlocks < 2) return 500f;
        int numAdjustement = (int) Math.floor(numOfBlocks / numberOfBLocksBetweenRewardAdjustement);
        int adj = (int) Math.pow(2, numAdjustement);
        if (500f / adj >= 0.1f) return 500f / adj;
        else return 0;
    }

}
