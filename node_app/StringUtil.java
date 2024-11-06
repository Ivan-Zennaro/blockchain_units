import java.security.*;
import java.util.ArrayList;
import java.util.Base64;

import com.google.gson.GsonBuilder;

import java.util.List;

public class StringUtil {

    //Applies Sha256 to a string and returns the result.
    public static String applySha256(String input) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            //Applies sha256 to our input,
            byte[] hash = digest.digest(input.getBytes("UTF-8"));

            StringBuffer hexString = new StringBuffer(); // This will contain hash as hexidecimal
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //Applies ECDSA Signature and returns the result ( as bytes ).
    // Sign with private key of the sender
    public static byte[] applyECDSASig(PrivateKey privateKey, String input) {
        Signature dsa;
        byte[] output;
        try {
            dsa = Signature.getInstance("ECDSA", "BC");
            dsa.initSign(privateKey);
            byte[] strByte = input.getBytes();
            dsa.update(strByte);
            byte[] realSig = dsa.sign();
            output = realSig;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return output;
    }

    //Verifies a String signature con la chiave pubblica del mittente
    public static boolean verifyECDSASig(PublicKey publicKey, String data, byte[] signature) {
        try {
            Signature ecdsaVerify = Signature.getInstance("ECDSA", "BC");
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(data.getBytes());
            return ecdsaVerify.verify(signature);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //Short hand helper to turn Object into a json string
    public static String getJson(Object o) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(o);
    }

    //Returns difficulty string target, to compare to hash. eg difficulty of 5 will return "00000"
    public static String getDificultyString(int difficulty) {
        return new String(new char[difficulty]).replace('\0', '0');
    }

    public static String getStringFromKey(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
	
	/* Merkle tree
				    aaaaa1							LAYER 3
		  bbbbb1              bbbbb2				LAYER 2
	dasdsa    dasdas  	fssddad		fsdfdsfsd		LAYER 1
	 */

    // calculate the merkleroot of the transaction tree
    public static String getMerkleRoot(ArrayList<Transaction> transactions) {

        //tengo conto della quantit� di transazioni del layer in cui sono arrivato
        int count = transactions.size();

        List<String> previousTreeLayer = new ArrayList<String>();
        for (Transaction transaction : transactions) {
            previousTreeLayer.add(transaction.transactionId);
        }

        // Initialize treeLayer which would be one of the upper levels of transactions of the tree
        // Initialize it immediately because I will use it at the end, in case there was only one transaction
        // Then treelayer=previousTreeLayer therefore returns us the hash of the single transaction
        List<String> treeLayer = previousTreeLayer;
        while (count > 1) {    // until current layer has >1 transactions
            treeLayer = new ArrayList<String>();
            // create upper layer
            for (int i = 1; i < previousTreeLayer.size(); i = i + 2) {
                treeLayer.add(applySha256(previousTreeLayer.get(i - 1) + previousTreeLayer.get(i)));
            }
            count = treeLayer.size();
            previousTreeLayer = treeLayer;
        }

        // if the last layer is made up of only one element it returns it otherwise it is empty
        if (treeLayer.size() == 1) return treeLayer.getFirst();
        else return "";
    }
}