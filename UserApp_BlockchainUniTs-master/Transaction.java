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
									//maleintenzionato possa reindirizzare la trasanzione nel
									//proprio wallet ed utilizzare il tuo denaro
	
	public long timeStamp;
	public ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();
	public ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>();
		
	public Transaction(PublicKey from, PublicKey to, float value,  ArrayList<TransactionInput> inputs) {
		this.sender = from;
		this.reciepient = to;
		this.value = value;
		this.inputs = inputs;
		this.timeStamp = new Date().getTime();
	}
	
	public float getInputsValue() {
		float total = 0;
		for(TransactionInput i : inputs) {
			if(i.UTXO == null) continue; 
			total = total + i.UTXO.value;
		}
		return total;
	}
	
	
	//per firmare : firmo con ECDSASig di chiave privata mittente + dati
	public void generateSignature(PrivateKey privateKey) {
		String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(reciepient) + Float.toString(value)	;
		signature = StringUtil.applyECDSASig(privateKey,data);		
	}
	
	//per controllare la firma uso chiave pubblica mittente + dati + firma vecchia
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
				Float.toString(value) + timeStamp
				);
	}
}