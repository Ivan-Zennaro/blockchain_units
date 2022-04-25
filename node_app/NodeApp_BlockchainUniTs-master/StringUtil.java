import java.security.*;
import java.util.ArrayList;
import java.util.Base64;
import com.google.gson.GsonBuilder;
import java.util.List;

public class StringUtil {
	
	//Applies Sha256 to a string and returns the result. 
	public static String applySha256(String input){
		
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
	        
			//Applies sha256 to our input, 
			byte[] hash = digest.digest(input.getBytes("UTF-8"));
	        
			StringBuffer hexString = new StringBuffer(); // This will contain hash as hexidecimal
			for (int i = 0; i < hash.length; i++) {
				String hex = Integer.toHexString(0xff & hash[i]);
				if(hex.length() == 1) hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString();
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	//Applies ECDSA Signature and returns the result ( as bytes ).
	//Firmo l'input con la chiave privata del mittente
	public static byte[] applyECDSASig(PrivateKey privateKey, String input) {
		Signature dsa;
		byte[] output = new byte[0];
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
		}catch(Exception e) {
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
	
	
	  // Merkle tree molto schematico
//				    fwdadwa							LAYER 3
//		dasasa                 dsasafsd				LAYER 2
//	dasdsa    dasdas  	fssddad		fsdfdsfsd		LAYER 1	
	
	
	
	//calcolo la merkleroot dell'albero delle transazioni
	public static String getMerkleRoot(ArrayList<Transaction> transactions) {
		
		//tengo conto della quantità di transazioni del layer in cui sono arrivato
		int count = transactions.size();
		
		//salvo gli id su un arrayList
		List<String> previousTreeLayer = new ArrayList<String>();
		for(Transaction transaction : transactions) {
			previousTreeLayer.add(transaction.transactionId);
		}
		
		//inzializzo treeLayer che sarebbe unio dei livelli superiori di transazioni dell'albero
		//lo inizializzo subito perchè lo uso poi alla fine, nel caso ci fosse solo una transazione
		//allora il treelayer=previousTreeLayer quindi ci restituisce hash della singola
		List<String> treeLayer = previousTreeLayer;  
		
		
		while(count > 1) {	//continua fino a quando il layer attuale ha + di 1 transazione
			treeLayer = new ArrayList<String>();
			//creo il layer superiore
			for(int i=1; i < previousTreeLayer.size(); i=i+2) {
				treeLayer.add(applySha256(previousTreeLayer.get(i-1) + previousTreeLayer.get(i)));
			}
			count = treeLayer.size();
			previousTreeLayer = treeLayer;
		}
		
		//se l'ultimo layer è formato da un solo elemento lo returna altrimenti vuota
		if (treeLayer.size() == 1) return treeLayer.get(0);
		else return "";
	}
}