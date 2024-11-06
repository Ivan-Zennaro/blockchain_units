import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import sun.misc.BASE64Encoder;

public class Wallet {

    public String id; // toString of PK

    public PrivateKey privateKey;
    public PublicKey publicKey;

    //UTXO = Unspent Transaction Output
    public HashMap<String, TransactionOutput> UTXOs = new HashMap<String, TransactionOutput>();

    public Wallet() {
        generateKeyPair();
        byte[] publicKeyBytes = publicKey.getEncoded();
        BASE64Encoder encoder = new BASE64Encoder();
        //l'id � la chiave pubblica in to String
        id = encoder.encode(publicKeyBytes);

    }

    public void generateKeyPair() {
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

    public float getBalance() {
        float total = 0;
        for (Map.Entry<String, TransactionOutput> item : Main.UTXOs.entrySet()) {
            TransactionOutput UTXO = item.getValue();
            if (UTXO.isMine(publicKey)) {
                UTXOs.put(UTXO.id, UTXO);
                total = total + UTXO.value;
            }
        }
        return total;
    }


    // analyze all the transactions that have made me receive value
    // and  create the exit transaction using various entry transactions as input
    // until at least covering the value I want to send
    public Transaction generateTransaction(PublicKey _recipient, float value) {
        // work on local utxos created by get balance
        if (getBalance() < value) {
            System.out.println("Impossible no value left");
            return null;
        }
        ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();

        float total = 0;
        for (Map.Entry<String, TransactionOutput> item : UTXOs.entrySet()) {
            TransactionOutput UTXO = item.getValue();
            total = total + UTXO.value;
            inputs.add(new TransactionInput(UTXO.id));
            if (total > value)
                break;
        }

        // signed with private key
        Transaction newTransaction = new Transaction(publicKey, _recipient, value, inputs);
        newTransaction.generateSignature(privateKey);

        // remove used outputs
        for (TransactionInput input : inputs) {
            UTXOs.remove(input.transactionOutputId);
        }

        return newTransaction;
    }

}
