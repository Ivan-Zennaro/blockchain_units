import java.io.Serializable;

public class TransactionInput implements Serializable {
	
	//La transazione di input deriva da una transazione di output
	//perchè per avere denaro disponibile da spendere dobbiamo averlo
	//ricevuto da qualcuno ,il campo id fa riferimento all'id della 
	//transazione di output che mi ha fatto avere il denaro
	
	public String transactionOutputId;  //riferimento all'id della transazione output
	public TransactionOutput UTXO; 		//UTXO = Unspent transaction output
	
	public TransactionInput(String transactionOutputId) {
		this.transactionOutputId = transactionOutputId;
	}
}