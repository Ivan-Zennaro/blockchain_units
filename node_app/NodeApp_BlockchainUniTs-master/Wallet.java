import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map; 
import sun.misc.BASE64Encoder;

public class Wallet {
	
	public String id; //assegno un id che corrisponde alla chiave pubblica in toString
	
	//Ogni wallet ha la coppia di chiavi utile per l'invio sicuro dei dati
	public PrivateKey privateKey;
	public PublicKey publicKey;
	
	//UNXO = Unspent Transaction Output 
	//Associazione chiave-valore, tengo traccia di tutte le mie transazioni in entrata
	public HashMap<String,TransactionOutput> UTXOs = new HashMap<String,TransactionOutput>();
	
	
	public Wallet() {
		generateKeyPair(); 
		byte[] publicKeyBytes = publicKey.getEncoded();
	    BASE64Encoder encoder = new BASE64Encoder();
	    //l'id è la chiave pubblica in to String
	    id = encoder.encode(publicKeyBytes);
		
	}
		
	public void generateKeyPair() {
		try {
			//EDSCA Elliptic Curve Digital Signature Algorithm
			//BC Bouncy Castle apis for cryptography
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA","BC");
			//SHA-1 Pseudo-Random Number Generation
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			//tipologia di curva ellittica utilizzata
			ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");
			// Initializes the key pair generator for a certain keysize with the given source of randomness
			keyGen.initialize(ecSpec, random); //256 bit
			//genera coppia di chiavi
	        KeyPair keyPair = keyGen.generateKeyPair();
	        privateKey = keyPair.getPrivate();
	        publicKey = keyPair.getPublic();

	        
		}catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	//calcola saldo portafoglio 
	public float getBalance() {
		float total = 0;	
		//for che mi scorre le coppie stringa/transazioni di output del mio vettore utxo nel main
        for (Map.Entry<String, TransactionOutput> item: Main.UTXOs.entrySet()){
        	TransactionOutput UTXO = item.getValue();  //salvo la transazione output corrente
            if(UTXO.isMine(publicKey)) {      //controllo se l'output è rivolto a me 
            	UTXOs.put(UTXO.id,UTXO);      //se si lo aggiungo alle mie entrate
            	total = total + UTXO.value ; 	
            }
        }  
		return total;
	}
	
	
	//Analizzo tutte le transazioni che mi hanno fatto ricevere valore
	//e creo la transazione di uscita usando come input varie transazioni 
	//di entrata fino ad almeno ricoprire il valore che voglio inviare
	
	public Transaction generateTransaction(PublicKey _recipient,float value ) {
		//facendo il getBalance aggiorno la mia mappa delle entrate
		if(getBalance() < value) {
			System.out.println("Impossible no value left");
			return null;
		}
		ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();
		
		float total = 0;
		//scorro la mappa delle mie entrate
		for (Map.Entry<String, TransactionOutput> item: UTXOs.entrySet()){
			TransactionOutput UTXO = item.getValue(); //seleziono la singola entrata
			total = total + UTXO.value;				  // recupero il totale
			inputs.add(new TransactionInput(UTXO.id));//la aggiungo alla mappa degli input della transazione
			if(total > value) break;				  //continuo ad aggiungere transazioni fino a quando non copro il value
		}
		
		//creo la transazione e la firmo con la mia chiave privata
		Transaction newTransaction = new Transaction(publicKey, _recipient , value, inputs);
		newTransaction.generateSignature(privateKey);
		
		//rimuovo dalla mappa delle entrate quelle utilizzate
		for(TransactionInput input: inputs){
			UTXOs.remove(input.transactionOutputId);
		}
		
		return newTransaction;
	}
	
}
