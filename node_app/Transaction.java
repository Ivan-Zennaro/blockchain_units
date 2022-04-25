import java.io.Serializable;
import java.security.*;
import java.util.ArrayList;
import java.util.Date;

public class Transaction implements Serializable {
	
	private static final long serialVersionUID = -5399605122490343339L;
	
	public String transactionId;  	//Hash della transazione
	public PublicKey sender; 		//Mandante address/public key.
	public PublicKey reciepient; 	//Ricevente address/public key.
	public float value; 			//Valore
	public byte[] signature;        //Firma della transazione: serve per fare in modo che un
	public long timeStamp;
	
									//maleintenzionato possa reindirizzare la trasanzione nel
									//proprio wallet ed utilizzare il tuo denaro
	
	public ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();
	public ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>();
	
	
	public Transaction(PublicKey from, PublicKey to, float value,  ArrayList<TransactionInput> inputs) {
		this.sender = from;
		this.reciepient = to;
		this.value = value;
		this.inputs = inputs;
		this.timeStamp = new Date().getTime();
	}
	
	public boolean processTransaction() {
		if(verifySignature() == false) {
			System.out.println("#Wrong Signature");
			return false;
		}

		//scorro le transazioni di unput
		for(TransactionInput i : inputs) {
			//assegno all' Unspent transaction output della transazione di input la corrispondente
			//transazione di output che lo ha generato
			i.UTXO = Main.UTXOs.get(i.transactionOutputId);
			
		}

		//Controllo validità
		if(value < Main.minimumTransaction) {
			System.out.println("Input too small: " + value);
			System.out.println("Please insert a minimum value of: " + Main.minimumTransaction);
			return false;
		}
		
		//Genero gli output
		float leftOver = getInputsValue() - value; 
		transactionId = calulateHash();
		
		outputs.add(new TransactionOutput( this.reciepient, value,transactionId)); //send value to recipient
		
		if (leftOver == value) {
			// per evitare che 2 transazioni di output abbiano stesso id
			outputs.add(new TransactionOutput( this.sender, leftOver ,transactionId , 1)); 
		}else {			
			outputs.add(new TransactionOutput( this.sender, leftOver ,transactionId)); 
		}
				
		//UTXOs nel main contiene tutte le transazioni di output che arrivano ai vari user
		//i soldi di queste transazioni possono essere spesi dai singoli proprietari
		
		
		for(TransactionOutput o : outputs) {
			Main.UTXOs.put(o.id , o);
		}
		
		
		//cancello le vecchie transazioni di output che sono diventate di input per nuove transazioni
		for(TransactionInput i : inputs) {
			if(i.UTXO == null) continue; //if Transaction can't be found skip it 
			Main.UTXOs.remove(i.UTXO.id);
		}
				
		return true;
	}
	
	public float getInputsValue() {
		float total = 0;
		for(TransactionInput i : inputs) {
			if(i.UTXO == null) continue; 
			total = total + i.UTXO.value;
		}
		return total;
	}
		
	public void generateSignature(PrivateKey privateKey) {
		String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(reciepient) + Float.toString(value)	;
		signature = StringUtil.applyECDSASig(privateKey,data);		
	}
	
	public boolean verifySignature() {
		String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(reciepient) + Float.toString(value)	;
		return StringUtil.verifyECDSASig(sender, data, signature);
	}
	
	public float getOutputsValue() {
		float total = 0;
		for(TransactionOutput output : outputs) {
			total = total + output.value;
		}
		return total;
	}
	
	private String calulateHash() {

		return StringUtil.applySha256(
				StringUtil.getStringFromKey(sender) +
				StringUtil.getStringFromKey(reciepient) +
				Float.toString(value) + this.timeStamp
				);
	}
}